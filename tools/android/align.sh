#!/bin/sh
# Checks the APK's 16 KB page alignment, which Google Play requires: every uncompressed native library must start on a
# 16 KB boundary. zipalign's own status decides; a pipe would hide it (POSIX sh has no pipefail), so its report is kept
# and shortened afterwards.
#
#   sh tools/android/align.sh <path/to/app.apk>

set -eu

report="$(zipalign -c -P 16 -v 4 "$1")" || { echo "${report}" | grep -v " (OK" | tail -20; exit 1; }
echo "${report}" | tail -1
