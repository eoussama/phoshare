#!/bin/sh
# What `android emulator` runs once the emulator has booted (from the repository root): the instrumented tests, then the
# smoke check on the debug APK. Gradle needs the image's full SDK (platforms, build tools), not the emulator's, which
# emulator.sh put in ANDROID_HOME. The UI tests' screenshots are pulled into the screenshot folder's flow/, pass or fail.
#
#   sh tools/android/device.sh <path/to/app-debug.apk> [screenshot folder]

set -eu

shots="${2:-}"
remote="/data/local/tmp/phoshare-shots"

adb shell rm -rf "${remote}"
status=0
ANDROID_HOME=/opt/android-sdk ANDROID_SDK_ROOT=/opt/android-sdk sh ./gradlew --no-daemon connectedDebugAndroidTest || status=$?

if [ -n "${shots}" ]; then
  rm -rf "${shots}/flow"
  mkdir -p "${shots}/flow"
  adb pull "${remote}/." "${shots}/flow" > /dev/null 2>&1 || true
fi

[ "${status}" -eq 0 ] || exit "${status}"
sh tools/android/smoke.sh "$@"
