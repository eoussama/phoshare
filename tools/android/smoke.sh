#!/bin/sh
# Device smoke check: on a device with 16 KB memory pages, the debug APK installs and the app survives its lifecycle:
# launch, rotate, background and resume, kill and relaunch. Each step checks that the app is running and in the
# foreground, and still is a few seconds later, and keeps a screenshot of it. At the end, the log must hold no crash of
# the app. `android emulator` runs it on the API 37 google_apis_ps16k emulator, as CI does.
#
#   sh tools/android/smoke.sh <path/to/app-debug.apk> [screenshot folder]

set -eu

apk="$1"
shots="${2:-}"
app="es.ouss.phoshare"
activity="${app}/.ui.MainActivity"

# Whether the app is running and its activity is on top.
alive() {
  adb shell pidof "${app}" > /dev/null 2>&1 || return 1
  adb shell dumpsys activity activities 2> /dev/null | grep -qE "(topResumedActivity|mResumedActivity).*${app}"
}

# What went wrong, without the noise: the app's process, the activity on top, its crashes and its own last log lines.
diagnose() {
  echo "${app} is not running in the foreground after: $1"
  echo "--- process: $(adb shell pidof "${app}" 2> /dev/null || echo none)"
  echo "--- activity on top:"
  adb shell dumpsys activity activities 2> /dev/null | grep -E "topResumedActivity|mResumedActivity" | head -3
  echo "--- crashes:"
  adb logcat -d -b crash 2> /dev/null | tail -30
  echo "--- the app and the activity manager:"
  adb logcat -d 2> /dev/null | grep -E "${app}|AndroidRuntime" | tail -40
}

# Waits until the app is running and on top (a slow emulator may take a while to bring it back), checks it is still
# there a few seconds later (a crash just after launch), and keeps a screenshot.
check() {
  waited=0

  until alive; do
    waited=$((waited + 1))
    [ "${waited}" -le 30 ] || { diagnose "$1"; exit 1; }
    sleep 1
  done

  sleep 3
  alive || { diagnose "$1 (it stopped right after starting)"; exit 1; }

  if [ -n "${shots}" ]; then
    mkdir -p "${shots}"
    adb exec-out screencap -p > "${shots}/$2.png"
  fi

  echo "ok: $1 (${waited} s)"
}

page_size="$(adb shell getconf PAGE_SIZE | tr -d '\r')"
[ "${page_size}" = "16384" ] || { echo "Expected 16 KB pages, got ${page_size}"; exit 1; }
echo "ok: 16 KB memory pages"

# A build signed with another key (debug after release, or the reverse) cannot update the installed one: replace it.
if ! adb install -r "${apk}" > /dev/null 2>&1; then
  adb uninstall "${app}" > /dev/null 2>&1 || true
  adb install "${apk}" > /dev/null
fi
echo "ok: install"
# Start from a new device: the emulator keeps the app's data between runs (and `--window` sessions change it).
cleared="$(adb shell pm clear "${app}" 2>&1)" || { echo "Could not clear the app's data: ${cleared}"; exit 1; }
# A fresh log, so the checks at the end read only this run.
adb logcat -c 2> /dev/null || true
adb logcat -c -b crash 2> /dev/null || true
adb shell am start -W -n "${activity}" > /dev/null
check "launch" 1-launch

adb shell settings put system accelerometer_rotation 0
adb shell settings put system user_rotation 1
check "rotate to landscape" 2-landscape
adb shell settings put system user_rotation 0
check "rotate back to portrait" 3-portrait

adb shell input keyevent KEYCODE_HOME
sleep 2
adb shell am start -W -n "${activity}" > /dev/null
check "background and resume" 4-resumed

adb shell am force-stop "${app}"
waited=0

until ! adb shell pidof "${app}" > /dev/null 2>&1; do
  waited=$((waited + 1))
  [ "${waited}" -le 10 ] || { echo "${app} survived force-stop"; exit 1; }
  sleep 1
done

adb shell am start -W -n "${activity}" > /dev/null
check "kill and relaunch" 5-relaunched

# A crash the steps above did not see (a background thread): Android's crash log names the process, and a native crash
# is reported as a fatal signal in the main log.
crashes="$( (adb logcat -d -b crash 2> /dev/null; adb logcat -d 2> /dev/null | grep -E "Fatal signal|FATAL EXCEPTION") | grep -F "${app}" || true)"

if [ -n "${crashes}" ]; then
  echo "The app crashed during the run:"
  echo "${crashes}" | tail -30
  exit 1
fi

echo "ok: no crash"
echo "PhoShare passed the device smoke check on a 16 KB page-size device."
