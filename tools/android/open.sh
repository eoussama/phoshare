#!/bin/sh
# Installs the APK and opens the app, on the emulator or on the phone given (its wireless debugging address, which is
# connected first).
#
#   sh tools/android/open.sh <path/to/app.apk> [ip:port]

set -eu

apk="$1"
device="${2:-}"
activity="es.ouss.phoshare/.ui.MainActivity"

if [ -n "${device}" ]; then
  adb connect "${device}"
  adb -s "${device}" install -r "${apk}"
  adb -s "${device}" shell am start -n "${activity}"
else
  adb install -r "${apk}"
  adb shell am start -n "${activity}"
fi
