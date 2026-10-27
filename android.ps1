<#
Android without Android Studio, on Windows (android.sh is the same on macOS and Linux): Java, the Android SDK, Gradle
and adb run in the image built from tools/android/Containerfile, with Podman (or Docker: $env:CONTAINER_ENGINE =
"docker"). A phone is reached over Wi-Fi with Android's wireless debugging (Android 11 or later); without one, a headless
Android 17 emulator with 16 KB pages runs in the image (it needs /dev/kvm, which Podman's WSL2 machine has).

  ./android.ps1 build [--release]       build the debug APK, or the signed release APK and AAB
  ./android.ps1 test                    run the JVM unit tests and Android lint
  ./android.ps1 emulator [--window]     build, then run the instrumented tests and the smoke check on the emulator
                                        (screenshots in test-results/android); --window opens it with the app instead,
                                        through WSLg
  ./android.ps1 pair <ip:port> <code>   pair a phone once (Wireless debugging > Pair device with pairing code)
  ./android.ps1 [run] <ip:port>         build, install and open the app on that phone (or PHOSHARE_ANDROID_DEVICE)
  ./android.ps1 check [--release]       check the APK's 16 KB page alignment
  ./android.ps1 keystore                create the release keystore at $env:PHOSHARE_KEYSTORE, once
  ./android.ps1 image                   rebuild the images

Gradle's caches live in the phoshare-gradle volume (ANDROID_GRADLE_CACHE overrides it), adb's keys and the virtual device
in phoshare-adb, and the emulator's download (about 2 GB) in phoshare-emulator-sdk. The release keystore is a file
outside the repository: its folder is mounted read-only and its password reaches Gradle through the container's
environment, never a command line.

Runs on Windows PowerShell 5.1 and PowerShell 7. Every container command is a script in tools/android, so no argument
carries quotes (5.1 drops them on the way to a native program).
#>

# Not "Stop": Windows PowerShell 5.1 turns a native program's stderr into errors, which "Stop" would make fatal (Podman
# reports progress on stderr). Every native call's exit code is checked instead.
$ErrorActionPreference = "Continue"

$Root = $PSScriptRoot
$Containerfile = Join-Path $Root "tools/android/Containerfile"
$Engine = if ($env:CONTAINER_ENGINE) { $env:CONTAINER_ENGINE } else { "podman" }
$Platform = "linux/amd64"
$Apk = "app/build/outputs/apk/debug/app-debug.apk"
$ApkRelease = "app/build/outputs/apk/release/app-release.apk"
$AabRelease = "app/build/outputs/bundle/release/app-release.aab"
$Shots = "test-results/android"
$SigningDir = "/signing"
# The X display's sockets: WSLg's, inside Podman's machine.
$X11Sockets = "/mnt/wslg/.X11-unix"
$DevicePattern = '^[A-Za-z0-9_.-]+:[0-9]{1,5}$'

$Usage = @"
Usage: ./android.ps1 [run] <ip:port>       build, install and open the app on a phone
       ./android.ps1 pair <ip:port> <code>  pair a phone once (Wireless debugging > Pair device with pairing code)
       ./android.ps1 emulator [--window]    build, then run the device tests on an emulator
       ./android.ps1 build [--release]      build the debug APK, or the signed release APK and AAB
       ./android.ps1 test                   run the unit tests and lint
       ./android.ps1 check [--release]      check the APK's 16 KB page alignment
       ./android.ps1 keystore               create the release keystore at `$env:PHOSHARE_KEYSTORE, once
       ./android.ps1 image                  rebuild the images
"@

$NoKvm = @"
The emulator needs hardware virtualization (/dev/kvm) in the container engine, and this machine's has none. Podman's
WSL2 machine normally has it: check that virtualization is on in the firmware, or use a phone instead:
./android.ps1 pair <ip:port> <code>, then ./android.ps1 run <ip:port>.
"@

function Fail([string] $Message) {
  [Console]::Error.WriteLine($Message)
  exit 1
}

function UsageError([string] $Message) {
  Fail "$Message`n$Usage"
}

# Runs a native command with the terminal; exits with its status when it fails.
function Invoke-Native([string[]] $Arguments) {
  & $Engine @Arguments
  if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}

# The image tag: a hash of the Containerfile and the platform, the same one android.sh computes.
function Get-ImageTag([string] $Target) {
  $bytes = [IO.File]::ReadAllBytes($Containerfile) + [Text.Encoding]::ASCII.GetBytes($Platform)
  $sha = [Security.Cryptography.SHA256]::Create()
  $hash = (-join ($sha.ComputeHash($bytes) | ForEach-Object { $_.ToString("x2") })).Substring(0, 12)
  if ($Target -eq "emulator") { "phoshare-android-emulator:$hash" } else { "phoshare-android:$hash" }
}

# Builds the image's target unless it exists already (or always, with -Force); returns the tag.
function Initialize-Image([string] $Target, [switch] $Force) {
  $tag = Get-ImageTag $Target
  & $Engine image inspect $tag *> $null
  if ($Force -or $LASTEXITCODE -ne 0) {
    & $Engine build --platform $Platform --target $Target --tag $tag --file $Containerfile (Split-Path $Containerfile) | Out-Host
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
  }
  $tag
}

# Runs a shell command in a container, with the repository at /work and the volumes that outlive it.
function Invoke-Container([string] $Tag, [string] $Script, [string[]] $Extra = @()) {
  $gradleCache = if ($env:ANDROID_GRADLE_CACHE) { $env:ANDROID_GRADLE_CACHE } else { "phoshare-gradle" }
  $arguments = @("run", "--rm", "--platform", $Platform) + $Extra + @(
    "--volume", "${Root}:/work",
    "--volume", "${gradleCache}:/cache/gradle",
    "--volume", "phoshare-adb:/root/.android",
    "--workdir", "/work",
    $Tag, "sh", "-c", $Script)
  Invoke-Native $arguments
}

# The phone's address, from the argument or PHOSHARE_ANDROID_DEVICE.
function Get-Device([string] $Value) {
  $address = if ($Value) { $Value } else { $env:PHOSHARE_ANDROID_DEVICE }
  if (-not $address) { UsageError "A device address is needed, as <ip:port> (Developer options > Wireless debugging), or in PHOSHARE_ANDROID_DEVICE." }
  if ($address -notmatch $DevicePattern) { UsageError "Not a device address: $address (expected <ip:port>)." }
  $address
}

# The release keystore: its path on this machine and the container's arguments; -Write lets it create the keystore.
function Get-Signing([switch] $Write) {
  if (-not $env:PHOSHARE_KEYSTORE) {
    Fail "PHOSHARE_KEYSTORE is not set: it holds the path of the release keystore, a file outside the repository.`nCreate one with './android.ps1 keystore' (set PHOSHARE_KEYSTORE and PHOSHARE_KEYSTORE_PASSWORD first)."
  }
  if (-not $env:PHOSHARE_KEYSTORE_PASSWORD) { Fail "PHOSHARE_KEYSTORE_PASSWORD is not set: it holds the release keystore's password." }
  $alias = if ($env:PHOSHARE_KEY_ALIAS) { $env:PHOSHARE_KEY_ALIAS } else { "phoshare" }
  if ($alias -notmatch '^[A-Za-z0-9_.-]{1,64}$') { Fail "Not a key alias: $alias (letters, digits, dot, dash and underscore; PHOSHARE_KEY_ALIAS)." }
  $path = $ExecutionContext.SessionState.Path.GetUnresolvedProviderPathFromPSPath($env:PHOSHARE_KEYSTORE)
  $dir = Split-Path $path
  $file = Split-Path $path -Leaf
  $mode = if ($Write) { "" } else { ":ro" }
  @{
    File = $path
    Dir = $dir
    Arguments = @("--volume", "${dir}:$SigningDir$mode", "--env", "PHOSHARE_KEYSTORE_FILE=$SigningDir/$file", "--env", "PHOSHARE_KEY_ALIAS=$alias", "--env", "PHOSHARE_KEYSTORE_PASSWORD")
  }
}

# Builds the debug APK, or the signed release APK and AAB; returns the build image's tag.
function Invoke-Build([switch] $Release) {
  $tag = Initialize-Image "build"
  # The wrapper through sh: a Windows checkout does not keep its executable bit.
  if ($Release) {
    $signing = Get-Signing
    if (-not (Test-Path $signing.File)) { Fail "There is no release keystore at $($signing.File) (PHOSHARE_KEYSTORE): create it once with './android.ps1 keystore'." }
    Invoke-Container $tag "sh ./gradlew --no-daemon assembleRelease bundleRelease && apksigner verify --print-certs $ApkRelease" $signing.Arguments | Out-Host
    Write-Host "Release APK: $ApkRelease`nRelease AAB: $AabRelease"
  }
  else {
    Invoke-Container $tag "sh ./gradlew --no-daemon assembleDebug" | Out-Host
    Write-Host "Debug APK: $Apk"
  }
  $tag
}

# Fails on any argument but the command's one optional flag; returns whether the flag was given.
function Get-Flag([string] $Command, [string] $Flag, [string[]] $Rest) {
  foreach ($arg in $Rest) {
    if ($arg -ne $Flag) { UsageError "Unexpected argument for ${Command}: $arg" }
  }
  $Rest -contains $Flag
}

function Assert-NoArgs([string] $Command, [string[]] $Rest) {
  if ($Rest.Count -gt 0) { UsageError "Unexpected argument for ${Command}: $($Rest -join ' ')" }
}

$command = if ($args.Count -gt 0) { [string] $args[0] } else { "run" }
[string[]] $rest = if ($args.Count -gt 1) { $args[1..($args.Count - 1)] } else { @() }

if ($command -match $DevicePattern) {
  $rest = @($command) + $rest
  $command = "run"
}

switch ($command) {
  { $_ -in "help", "--help", "-h" } {
    Write-Host $Usage
  }

  "image" {
    Assert-NoArgs "image" $rest
    Initialize-Image "build" -Force | Out-Null
    Initialize-Image "emulator" -Force | Out-Null
  }

  "build" {
    $release = Get-Flag "build" "--release" $rest
    Invoke-Build -Release:$release | Out-Null
  }

  "test" {
    Assert-NoArgs "test" $rest
    Invoke-Container (Initialize-Image "build") "sh ./gradlew --no-daemon testDebugUnitTest lintDebug"
  }

  "emulator" {
    $window = Get-Flag "emulator" "--window" $rest
    & $Engine run --rm --platform $Platform --device /dev/kvm (Initialize-Image "build") true *> $null
    if ($LASTEXITCODE -ne 0) { Fail $NoKvm }
    Invoke-Build | Out-Null
    $tag = Initialize-Image "emulator"
    $extra = @("--device", "/dev/kvm", "--volume", "phoshare-emulator-sdk:/opt/emulator-sdk")
    if ($window) {
      $display = if ($env:DISPLAY) { $env:DISPLAY } else { ":0" }
      $extra += @("--volume", "${X11Sockets}:/tmp/.X11-unix", "--env", "DISPLAY=$display", "--env", "EMULATOR_WINDOW=1")
      Invoke-Container $tag "sh tools/android/emulator.sh sh tools/android/open.sh $Apk" $extra
    }
    else {
      Invoke-Container $tag "sh tools/android/emulator.sh sh tools/android/device.sh $Apk $Shots" $extra
    }
  }

  "check" {
    $release = Get-Flag "check" "--release" $rest
    $target = if ($release) { $ApkRelease } else { $Apk }
    if (-not (Test-Path (Join-Path $Root $target))) { Fail "There is no APK at ${target}: build it first with ./android.ps1 build$(if ($release) { ' --release' })." }
    Invoke-Container (Initialize-Image "build") "sh tools/android/align.sh $target"
  }

  "keystore" {
    Assert-NoArgs "keystore" $rest
    $signing = Get-Signing -Write
    if (Test-Path $signing.File) { Fail "$($signing.File) exists already: it is never overwritten. Keep it, and back it up: an app signed with another key cannot update this one." }
    New-Item -ItemType Directory -Force -Path $signing.Dir | Out-Null
    Invoke-Container (Initialize-Image "build") "sh tools/android/keystore.sh" $signing.Arguments
    Write-Host "Release keystore: $($signing.File)`nBack it up with its password: Google Play accepts updates signed with this key only."
  }

  "pair" {
    if ($rest.Count -lt 2) { UsageError "pair needs <ip:port> and the six-digit pairing code." }
    $device = Get-Device $rest[0]
    if ($rest[1] -notmatch '^[0-9]{6}$') { UsageError "The six-digit pairing code is needed (Wireless debugging > Pair device with pairing code)." }
    if ($rest.Count -gt 2) { UsageError "Unexpected argument for pair: $($rest[2..($rest.Count - 1)] -join ' ')" }
    Invoke-Container (Initialize-Image "build") "adb pair $device $($rest[1])"
  }

  "run" {
    if ($rest.Count -gt 1) { UsageError "Unexpected argument for run: $($rest[1..($rest.Count - 1)] -join ' ')" }
    $device = Get-Device $(if ($rest.Count -gt 0) { $rest[0] } else { "" })
    $tag = Invoke-Build
    Invoke-Container $tag "sh tools/android/open.sh $Apk $device"
  }

  default {
    UsageError "Unknown command: $command"
  }
}
