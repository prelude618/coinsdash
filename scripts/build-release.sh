#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
properties_file="${COINSDASH_SIGNING_PROPERTIES:-/etc/coinsdash/signing/signing.properties}"

if [[ ! -r "$properties_file" ]]; then
  echo "Signing properties are not readable: $properties_file" >&2
  exit 1
fi

properties_mode="$(stat -c '%a' "$properties_file")"
if [[ "$properties_mode" != "600" ]]; then
  echo "Signing properties must have mode 600, found ${properties_mode}" >&2
  exit 1
fi

for key in storeFile storePassword keyAlias keyPassword; do
  if ! grep -q "^${key}=" "$properties_file"; then
    echo "Missing ${key} in ${properties_file}" >&2
    exit 1
  fi
done

keystore_file="$(sed -n 's/^storeFile=//p' "$properties_file" | tail -n 1)"
if [[ ! -r "$keystore_file" ]]; then
  echo "Keystore is not readable: $keystore_file" >&2
  exit 1
fi

keystore_mode="$(stat -c '%a' "$keystore_file")"
if [[ "$keystore_mode" != "600" ]]; then
  echo "Keystore must have mode 600, found ${keystore_mode}" >&2
  exit 1
fi

cd "$project_dir"
exec env COINSDASH_SIGNING_PROPERTIES="$properties_file" \
  ./gradlew --no-configuration-cache clean bundleRelease
