<p align="center">
	<img src="app/src/main/res/drawable-nodpi/logo.png" alt="PhoShare logo" width="150">
	<h1 align="center">PhoShare</h1>
</p>

A simple offline photo sharing application for those who don't have friends 😥.

PhoShare is a tiny social network that lives entirely on one phone. Make an account, post photos, comment on them and
favorite them. Then make another account and congratulate yourself. There is no server and no cloud, and the app
doesn't even ask for the internet permission. The only way a photo leaves the phone is when you share it to another app
through Android's share sheet.

## Features

- **Accounts:** sign up with a username, a password and a security question.
  - Passwords and answers are hashed with PBKDF2-HMAC-SHA256 (310,000 iterations, random salt).
  - You stay logged in until you log out.
  - Forgot your password? Answer your question (case doesn't matter) and pick a new one.
- **Feed:** every post on the device, newest first, with favorites, comment counts and sharing.
- **Posts:** pick a photo from the system photo picker or take one with the camera app, then add a caption. Photos are
  turned upright and scaled to at most 2048 px.
- **Comments and favorites**, plus a Favorites tab to find them again.
- **Profiles:** avatar, bio, join date and a photo grid. Change your password, switch accounts, or delete your account
  with everything it posted.
- Material 3 in the logo's colors, light and dark, edge to edge, from Android 7 (API 24) to Android 17 (API 37).
- No permissions at all. The photo picker and the camera app need none.

## Requirements

[Podman](https://podman.io/) (or Docker) and Git. Android needs nothing else installed: no Android Studio, no JDK and no
SDK. Java 25, the Android SDK (API 37, build tools 37.0.0), Gradle 9.8 and adb all run in a container image built from
[tools/android/Containerfile](tools/android/Containerfile).

On Windows, Podman's machine runs in WSL2, which also provides the emulator's hardware acceleration (`/dev/kvm`) and its
window (WSLg).

## Commands

Run them from the repository root: `./android.ps1` on Windows (Windows PowerShell 5.1 or PowerShell 7), `./android.sh`
on macOS and Linux. Both take the same commands.

| Command | What it does |
| --- | --- |
| `build` | Builds the debug APK, `app/build/outputs/apk/debug/app-debug.apk`. |
| `test` | Runs the JVM unit tests and Android lint. |
| `emulator` | Builds, then boots a headless Android 17 emulator with 16 KB pages. It runs the instrumented tests and a smoke check (launch, rotate, background, kill and relaunch) and saves screenshots in `test-results/android/`. |
| `emulator --window` | Builds and opens the emulator in a window with the app running. Close the window to stop. |
| `pair <ip:port> <code>` | Pairs a phone over Wi-Fi, once. See [On a phone](#on-a-phone). |
| `run <ip:port>` | Builds, installs and opens the app on a paired phone. `PHOSHARE_ANDROID_DEVICE` can hold the address. |
| `check` | Checks the APK's 16 KB page alignment, which Google Play requires. |
| `build --release` | Builds the signed release APK and AAB. See [Releases](#releases). |
| `keystore` | Creates the release keystore, once. |
| `image` | Rebuilds the container images. |

The first run builds the image and downloads Gradle and the dependencies. The first emulator run also downloads the
emulator and its system image, about 2 GB. Everything is kept in Podman volumes for later runs:

| Volume | Holds |
| --- | --- |
| `phoshare-gradle` | Gradle's caches (`ANDROID_GRADLE_CACHE` can point to a folder instead) |
| `phoshare-adb` | adb's keys and the virtual device |
| `phoshare-emulator-sdk` | The emulator and its system image |

To reclaim the space:

```sh
podman rmi $(podman images --filter reference='phoshare-android*' -q)
podman volume rm phoshare-gradle phoshare-adb phoshare-emulator-sdk
```

### The emulator

The emulator needs hardware virtualization in the container engine (`/dev/kvm`). Windows (WSL2) and Linux have it.
Podman's machine on macOS usually doesn't, and there the scripts say so and suggest a phone instead. Google ships the
SDK tools and the emulator for x86-64 only, so the image always runs as `linux/amd64`, which is emulated (slowly) on
Apple Silicon.

### On a phone

1. On the phone (Android 11 or later), open **Developer options › Wireless debugging**, turn it on, and pick
   **Pair device with pairing code**.
2. Run `./android.ps1 pair <ip:port> <code>` with the address and code that screen shows. The pairing is kept.
3. Run `./android.ps1 run <ip:port>` with the address shown on the **Wireless debugging** screen itself. That address
   is not the pairing one.

### Releases

The release keystore stays out of the repository. Point `PHOSHARE_KEYSTORE` at where it should live and set
`PHOSHARE_KEYSTORE_PASSWORD`, then run `keystore` once and `build --release` afterwards. `PHOSHARE_KEY_ALIAS` is
optional and defaults to `phoshare`. The keystore's folder is mounted read-only into the container, and the password
only travels through the environment. Back the keystore up: an app signed with another key can't update this one.

## Project layout

```
app/src/main/java/es/ouss/phoshare/
  data/          Room database (users, posts, comments, favorites), repositories, photo files, session
  security/      PBKDF2, password hashing, account rules
  ui/auth/       login, sign up, password reset
  ui/main/       feed, favorites, profile, post, new post, edit profile, change password
  ui/common/     shared UI helpers
app/src/test/          JVM tests (hashing, rules)
app/src/androidTest/   device tests (database, photo import, the account flow)
tools/android/         the container image and the scripts that run in it
android.ps1, android.sh
```

The app is Java with Android Views, AndroidX, Material 3, Room, Navigation and Glide. The build uses AGP 9.4 with the
Kotlin DSL and a version catalog (`gradle/libs.versions.toml`).

## CI

[.github/workflows/android.yml](.github/workflows/android.yml) runs `test`, `emulator` and `check` on every pull
request that touches the app, on GitHub's Linux runners with Podman and KVM. It uploads the debug APK, plus the
screenshots and reports when a check fails.

## License

[Apache 2.0](LICENSE)
