#!/bin/sh
# Apply MCWWS_WorldSync staging before starting Paper (Docker/MCSManager).
# Usage (server stopped): ./apply-staging.sh /data
set -e
ROOT="${1:-.}"
STAGING="$ROOT/plugins/MCWWS_WorldSync/staging"
FLAG="$ROOT/plugins/MCWWS_WorldSync/apply-on-boot"
if [ ! -d "$STAGING" ]; then
  echo "No staging directory."
  exit 0
fi
find "$STAGING" -type f ! -name '*.part' | while IFS= read -r f; do
  rel="${f#"$STAGING"/}"
  dest="$ROOT/$rel"
  mkdir -p "$(dirname "$dest")"
  cp -f "$f" "$dest"
  echo "applied $rel"
done
rm -f "$FLAG"
echo "staging applied."
