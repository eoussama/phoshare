#!/bin/sh
# Boots a headless Android 17 emulator with 16 KB memory pages in the emulator image, runs the given command once it has
# booted, then stops it. The emulator, its command-line tools and the system image are downloaded once into the volume
# mounted at /opt/emulator-sdk (phoshare-emulator-sdk); the virtual device lives in /root/.android (phoshare-adb).
# With EMULATOR_WINDOW=1 (`android emulator --window`) the emulator opens on the X display instead and stays open until
# its window is closed.
#
#   sh tools/android/emulator.sh <command> [arguments...]

set -eu

image="system-images;android-37.0;google_apis_ps16k;x86_64"
avd="phoshare"
sdk="/opt/emulator-sdk"

if [ ! -x "${sdk}/emulator/emulator" ] || [ ! -x "${sdk}/cmdline-tools/latest/bin/avdmanager" ] || [ ! -d "${sdk}/system-images/android-37.0/google_apis_ps16k/x86_64" ]; then
  echo "Downloading the emulator and the Android 17 (16 KB pages) system image, once..."
  yes | sdkmanager --sdk_root="${sdk}" --licenses > /dev/null
  sdkmanager --sdk_root="${sdk}" --install "emulator" "cmdline-tools;latest" "${image}" > /dev/null
fi

# The emulator only accepts an SDK folder with platform-tools: link the image's.
ln -sfn "${ANDROID_HOME}/platform-tools" "${sdk}/platform-tools"

export ANDROID_HOME="${sdk}" ANDROID_SDK_ROOT="${sdk}"
avdmanager="${sdk}/cmdline-tools/latest/bin/avdmanager"

if ! "${avdmanager}" list avd -c 2> /dev/null | grep -qx "${avd}"; then
  echo no | "${avdmanager}" create avd --name "${avd}" --package "${image}" --device pixel_6 > /dev/null
fi

# A container that was killed leaves its locks behind, and the emulator then refuses to start the device.
find "${HOME}/.android/avd/${avd}.avd" -maxdepth 1 -name "*.lock" -exec rm -rf {} + 2> /dev/null || true

window="-no-window"
[ "${EMULATOR_WINDOW:-0}" = "1" ] && window=""

# shellcheck disable=SC2086 # ${window} is empty or one word.
"${sdk}/emulator/emulator" -avd "${avd}" ${window} -no-audio -no-boot-anim -no-snapshot \
  -gpu swiftshader_indirect -accel on -memory 4096 > /tmp/emulator.log 2>&1 &
emulator=$!

waited=0
until [ "$(adb shell getprop sys.boot_completed 2> /dev/null | tr -d '\r')" = "1" ]; do
  kill -0 "${emulator}" 2> /dev/null || { echo "The emulator stopped while booting:"; tail -40 /tmp/emulator.log; exit 1; }
  waited=$((waited + 5))
  [ "${waited}" -le 600 ] || { echo "The emulator did not boot within 10 minutes:"; tail -40 /tmp/emulator.log; exit 1; }
  sleep 5
done
echo "Emulator booted in about ${waited} s."

# Espresso wants no animations; a person in front of the window does.
if [ "${EMULATOR_WINDOW:-0}" != "1" ]; then
  for scale in window_animation_scale transition_animation_scale animator_duration_scale; do
    adb shell settings put global "${scale}" 0
  done
fi

status=0
"$@" || status=$?

if [ "${EMULATOR_WINDOW:-0}" = "1" ] && [ "${status}" -eq 0 ]; then
  echo "The emulator stays open: close its window to stop."
  wait "${emulator}" || true
  exit 0
fi

adb emu kill > /dev/null 2>&1 || true
exit "${status}"
