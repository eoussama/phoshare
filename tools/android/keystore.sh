#!/bin/sh
# Creates the release keystore: a PKCS12 file with one RSA 4096-bit key, valid for 10000 days (Google Play wants a key
# valid past 2033). The keystore's file, the key's name and the password come from the environment the host script sets
# (PHOSHARE_KEYSTORE_FILE, PHOSHARE_KEY_ALIAS, PHOSHARE_KEYSTORE_PASSWORD), never from a command line. keytool refuses
# to add to an existing file, and the host script checks first that there is none.
#
#   sh tools/android/keystore.sh

set -eu

keytool -genkeypair -storetype PKCS12 -keystore "${PHOSHARE_KEYSTORE_FILE}" -alias "${PHOSHARE_KEY_ALIAS}" \
  -keyalg RSA -keysize 4096 -validity 10000 -dname "CN=PhoShare, O=eoussama" -storepass:env PHOSHARE_KEYSTORE_PASSWORD
