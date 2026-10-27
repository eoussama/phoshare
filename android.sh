#!/usr/bin/env bash
# Android without Android Studio, on macOS and Linux (android.ps1 is the same on Windows): Java, the Android SDK, Gradle
# and adb run in the image built from tools/android/Containerfile, with Podman (or Docker: CONTAINER_ENGINE=docker). A
# phone is reached over Wi-Fi with Android's wireless debugging (Android 11 or later); without one, a headless Android 17
# emulator with 16 KB pages runs in the image (it needs /dev/kvm: Linux and Windows' WSL2 have it, macOS does not).
#
#   ./android.sh build [--release]       build the debug APK, or the signed release APK and AAB
#   ./android.sh test                    run the JVM unit tests and Android lint
#   ./android.sh emulator [--window]     build, then run the instrumented tests and the smoke check on the emulator
#                                        (screenshots in test-results/android); --window opens it with the app instead
#   ./android.sh pair <ip:port> <code>   pair a phone once (Wireless debugging > Pair device with pairing code)
#   ./android.sh [run] <ip:port>         build, install and open the app on that phone (or PHOSHARE_ANDROID_DEVICE)
#   ./android.sh check [--release]       check the APK's 16 KB page alignment
#   ./android.sh keystore                create the release keystore at $PHOSHARE_KEYSTORE, once
#   ./android.sh image                   rebuild the images
#
# Gradle's caches live in the phoshare-gradle volume (ANDROID_GRADLE_CACHE overrides it, e.g. a folder CI caches), adb's
# keys and the virtual device in phoshare-adb, and the emulator's download (about 2 GB) in phoshare-emulator-sdk. The
# release keystore is a file outside the repository: its folder is mounted read-only and its password reaches Gradle
# through the container's environment, never a command line.

set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# The repository as the container engine sees it (a Windows path under Git Bash, see below).
ROOT="${HERE}"
CONTAINERFILE="${HERE}/tools/android/Containerfile"
ENGINE="${CONTAINER_ENGINE:-podman}"
PLATFORM="linux/amd64"
APK="app/build/outputs/apk/debug/app-debug.apk"
APK_RELEASE="app/build/outputs/apk/release/app-release.apk"
AAB_RELEASE="app/build/outputs/bundle/release/app-release.aab"
SHOTS="test-results/android"
SIGNING_DIR="/signing"
X11_SOCKETS="/tmp/.X11-unix"

# Git Bash on Windows: Podman wants a Windows path for the repository, and MSYS must not rewrite /work and friends.
case "$(uname -s)" in
  MINGW* | MSYS* | CYGWIN*)
    export MSYS_NO_PATHCONV=1
    ROOT="$(cd "${HERE}" && pwd -W)"
    X11_SOCKETS="/mnt/wslg/.X11-unix"
    ;;
esac

USAGE="Usage: ./android.sh [run] <ip:port>       build, install and open the app on a phone
       ./android.sh pair <ip:port> <code>  pair a phone once (Wireless debugging > Pair device with pairing code)
       ./android.sh emulator [--window]    build, then run the device tests on an emulator
       ./android.sh build [--release]      build the debug APK, or the signed release APK and AAB
       ./android.sh test                   run the unit tests and lint
       ./android.sh check [--release]      check the APK's 16 KB page alignment
       ./android.sh keystore               create the release keystore at \$PHOSHARE_KEYSTORE, once
       ./android.sh image                  rebuild the images"

NO_KVM="The emulator needs hardware virtualization (/dev/kvm) in the container engine, and this machine's has none:
Podman's machine on macOS usually has not, while Windows' (WSL2) and Linux hosts do. Use a phone instead:
./android.sh pair <ip:port> <code>, then ./android.sh run <ip:port>."

fail() {
  printf '%s\n' "$1" >&2
  exit 1
}

usage_error() {
  fail "$1
${USAGE}"
}

# The image tag: a hash of the Containerfile and the platform, the same one android.ps1 computes.
image_tag() {
  local hash
  if command -v sha256sum > /dev/null 2>&1; then
    hash="$({ cat "${CONTAINERFILE}"; printf '%s' "${PLATFORM}"; } | sha256sum)"
  else
    hash="$({ cat "${CONTAINERFILE}"; printf '%s' "${PLATFORM}"; } | shasum -a 256)"
  fi
  hash="${hash:0:12}"
  if [ "$1" = "emulator" ]; then echo "phoshare-android-emulator:${hash}"; else echo "phoshare-android:${hash}"; fi
}

# Builds the image's target unless it exists already (or always, with "force"); prints the tag.
ensure_image() {
  local target="$1" force="${2:-}" tag
  tag="$(image_tag "${target}")"
  if [ -n "${force}" ] || ! "${ENGINE}" image inspect "${tag}" > /dev/null 2>&1; then
    "${ENGINE}" build --platform "${PLATFORM}" --target "${target}" --tag "${tag}" --file "${CONTAINERFILE}" "$(dirname "${CONTAINERFILE}")" >&2
  fi
  echo "${tag}"
}

# Runs a shell command in a container: in_container <tag> <script> [extra run arguments...]
in_container() {
  local tag="$1" script="$2"
  shift 2
  "${ENGINE}" run --rm --platform "${PLATFORM}" "$@" \
    --volume "${ROOT}:/work" \
    --volume "${ANDROID_GRADLE_CACHE:-phoshare-gradle}:/cache/gradle" \
    --volume "phoshare-adb:/root/.android" \
    --workdir /work \
    "${tag}" sh -c "${script}"
}

DEVICE_RE='^[A-Za-z0-9_.-]+:[0-9]{1,5}$'

# The phone's address, from the argument or PHOSHARE_ANDROID_DEVICE.
device_from() {
  local address="${1:-${PHOSHARE_ANDROID_DEVICE:-}}"
  [ -n "${address}" ] || usage_error "A device address is needed, as <ip:port> (Developer options > Wireless debugging), or in PHOSHARE_ANDROID_DEVICE."
  [[ "${address}" =~ ${DEVICE_RE} ]] || usage_error "Not a device address: ${address} (expected <ip:port>)."
  echo "${address}"
}

# The release keystore's arguments for the container; "write" lets it create the keystore.
signing_args() {
  local path="${PHOSHARE_KEYSTORE:-}" alias="${PHOSHARE_KEY_ALIAS:-phoshare}" mode=":ro"
  [ -n "${path}" ] || fail "PHOSHARE_KEYSTORE is not set: it holds the path of the release keystore, a file outside the repository.
Create one with './android.sh keystore' (set PHOSHARE_KEYSTORE and PHOSHARE_KEYSTORE_PASSWORD first)."
  [ -n "${PHOSHARE_KEYSTORE_PASSWORD:-}" ] || fail "PHOSHARE_KEYSTORE_PASSWORD is not set: it holds the release keystore's password."
  [[ "${alias}" =~ ^[A-Za-z0-9_.-]{1,64}$ ]] || fail "Not a key alias: ${alias} (letters, digits, dot, dash and underscore; PHOSHARE_KEY_ALIAS)."
  [ "${1:-}" = "write" ] && mode=""
  local dir file
  dir="$(cd "$(dirname "${path}")" 2> /dev/null && pwd)" || fail "The keystore's folder does not exist: $(dirname "${path}")"
  file="$(basename "${path}")"
  case "$(uname -s)" in MINGW* | MSYS* | CYGWIN*) dir="$(cd "${dir}" && pwd -W)" ;; esac
  SIGNING=(--volume "${dir}:${SIGNING_DIR}${mode}" --env "PHOSHARE_KEYSTORE_FILE=${SIGNING_DIR}/${file}" --env "PHOSHARE_KEY_ALIAS=${alias}" --env PHOSHARE_KEYSTORE_PASSWORD)
  KEYSTORE_FILE="${dir}/${file}"
}

# Builds the debug APK, or the signed release APK and AAB; prints the build image's tag.
build() {
  local tag
  tag="$(ensure_image build)"
  # The wrapper through sh: a Windows checkout does not keep its executable bit.
  if [ "${1:-}" = "--release" ]; then
    signing_args
    [ -f "${KEYSTORE_FILE}" ] || fail "There is no release keystore at ${KEYSTORE_FILE} (PHOSHARE_KEYSTORE): create it once with './android.sh keystore'."
    in_container "${tag}" "sh ./gradlew --no-daemon assembleRelease bundleRelease && apksigner verify --print-certs ${APK_RELEASE}" "${SIGNING[@]}" >&2
    printf 'Release APK: %s\nRelease AAB: %s\n' "${APK_RELEASE}" "${AAB_RELEASE}" >&2
  else
    in_container "${tag}" "sh ./gradlew --no-daemon assembleDebug" >&2
    printf 'Debug APK: %s\n' "${APK}" >&2
  fi
  echo "${tag}"
}

flag_only() { # flag_only <command> <flag> [args...]: fails on anything but the flag
  local command="$1" flag="$2"
  shift 2
  for arg in "$@"; do
    [ "${arg}" = "${flag}" ] || usage_error "Unexpected argument for ${command}: ${arg}"
  done
}

no_args() { # no_args <command> [args...]
  local command="$1"
  shift
  [ "$#" -eq 0 ] || usage_error "Unexpected argument for ${command}: $*"
}

command="${1:-run}"
[ "$#" -gt 0 ] && shift

if [[ "${command}" =~ ${DEVICE_RE} ]]; then
  set -- "${command}" "$@"
  command="run"
fi

case "${command}" in
  help | --help | -h)
    echo "${USAGE}"
    ;;

  image)
    no_args image "$@"
    ensure_image build force > /dev/null
    ensure_image emulator force > /dev/null
    ;;

  build)
    flag_only build --release "$@"
    build "${1:-}" > /dev/null
    ;;

  test)
    no_args test "$@"
    in_container "$(ensure_image build)" "sh ./gradlew --no-daemon testDebugUnitTest lintDebug"
    ;;

  emulator)
    flag_only emulator --window "$@"
    "${ENGINE}" run --rm --platform "${PLATFORM}" --device /dev/kvm "$(ensure_image build)" true > /dev/null 2>&1 || fail "${NO_KVM}"
    build > /dev/null
    tag="$(ensure_image emulator)"
    emulator_args=(--device /dev/kvm --volume "phoshare-emulator-sdk:/opt/emulator-sdk")
    if [ "${1:-}" = "--window" ]; then
      emulator_args+=(--volume "${X11_SOCKETS}:/tmp/.X11-unix" --env "DISPLAY=${DISPLAY:-:0}" --env EMULATOR_WINDOW=1)
      in_container "${tag}" "sh tools/android/emulator.sh sh tools/android/open.sh ${APK}" "${emulator_args[@]}"
    else
      in_container "${tag}" "sh tools/android/emulator.sh sh tools/android/device.sh ${APK} ${SHOTS}" "${emulator_args[@]}"
    fi
    ;;

  check)
    flag_only check --release "$@"
    apk="${APK}"
    [ "${1:-}" = "--release" ] && apk="${APK_RELEASE}"
    [ -f "${HERE}/${apk}" ] || fail "There is no APK at ${apk}: build it first with ./android.sh build ${1:-}"
    in_container "$(ensure_image build)" "sh tools/android/align.sh ${apk}"
    ;;

  keystore)
    no_args keystore "$@"
    signing_args write
    [ ! -e "${KEYSTORE_FILE}" ] || fail "${KEYSTORE_FILE} exists already: it is never overwritten. Keep it, and back it up: an app signed with another key cannot update this one."
    in_container "$(ensure_image build)" "sh tools/android/keystore.sh" "${SIGNING[@]}"
    printf 'Release keystore: %s\nBack it up with its password: Google Play accepts updates signed with this key only.\n' "${KEYSTORE_FILE}"
    ;;

  pair)
    [ "$#" -ge 2 ] || usage_error "pair needs <ip:port> and the six-digit pairing code."
    device="$(device_from "$1")"
    [[ "$2" =~ ^[0-9]{6}$ ]] || usage_error "The six-digit pairing code is needed (Wireless debugging > Pair device with pairing code)."
    [ "$#" -eq 2 ] || usage_error "Unexpected argument for pair: ${*:3}"
    in_container "$(ensure_image build)" "adb pair ${device} $2"
    ;;

  run)
    [ "$#" -le 1 ] || usage_error "Unexpected argument for run: ${*:2}"
    device="$(device_from "${1:-}")"
    tag="$(build)"
    in_container "${tag}" "sh tools/android/open.sh ${APK} ${device}"
    ;;

  *)
    usage_error "Unknown command: ${command}"
    ;;
esac
