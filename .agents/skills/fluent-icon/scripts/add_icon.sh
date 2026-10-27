#!/usr/bin/env bash

set -euo pipefail

SRC_DRAWABLE="${FLUENT_ICONS_RES:-/Users/dagn/dev/android/fluentui-system-icons/android/library/src/main/res/drawable}"
DEST_DRAWABLE="${FLUENT_ICONS_DEST:-app/src/main/res/drawable}"
SRC_RES="$(dirname "$SRC_DRAWABLE")"
DEST_RES="$(dirname "$DEST_DRAWABLE")"

name="${1:-}"

if [[ -z "$name" ]]; then
  echo "usage: add_icon.sh <name>   (always vendors the 24 regular variant)" >&2
  exit 2
fi

file="ic_fluent_${name}_24_regular.xml"
src="${SRC_DRAWABLE}/${file}"

if [[ ! -f "$src" ]]; then
  echo "Not found: ${file}" >&2
  echo "Closest matches for '${name}' in the checkout:" >&2
  find "$SRC_DRAWABLE" -maxdepth 1 -iname "ic_fluent_*${name}*.xml" -exec basename {} \; | sort | sed 's/^/  /' >&2 || true
  exit 1
fi

mkdir -p "$DEST_DRAWABLE"
cp "$src" "${DEST_DRAWABLE}/${file}"
echo "Copied ${file} -> ${DEST_DRAWABLE}/"

tint_src="${SRC_RES}/color/fluent_default_icon_tint.xml"
tint_dest="${DEST_RES}/color/fluent_default_icon_tint.xml"
if [[ ! -f "$tint_dest" && -f "$tint_src" ]]; then
  mkdir -p "${DEST_RES}/color"
  cp "$tint_src" "$tint_dest"
  echo "Vendored shared color: color/fluent_default_icon_tint.xml"
fi

echo "Reference it as: R.drawable.ic_fluent_${name}_24_regular"
