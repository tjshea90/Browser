#!/usr/bin/env bash
# sync-ext-version.sh - keep the built-in extension's manifest version equal to 1.0.<Kestrel G versionCode>.
# GeckoView's ensureBuiltIn reinstalls the bundled extension ONLY when manifest.json's version differs from the installed one (research/ADBLOCK_ENGINE.md section 2),
# so a changed background.js / content.js never reaches an existing install unless the version moves with every release. ship.sh runs this; test_handoff.sh checks it.
#   bash tools/sync-ext-version.sh            rewrite the manifest if needed
#   bash tools/sync-ext-version.sh --check    exit 1 when they differ
set -uo pipefail
D="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"; cd "$D" || exit 1
. tools/appinfo.sh
M=geckoapp/src/main/assets/kestrel-shields/manifest.json
WANT="1.0.$(app_vcode gecko)"
HAVE="$(sed -n 's/.*"version": *"\([^"]*\)".*/\1/p' "$M" | head -1)"
if [ "$HAVE" = "$WANT" ]; then exit 0; fi
if [ "${1:-}" = "--check" ]; then echo "  extension manifest version is $HAVE, must be $WANT (bash tools/sync-ext-version.sh)"; exit 1; fi
sed -i "s/\"version\": *\"[^\"]*\"/\"version\": \"$WANT\"/" "$M" && echo "  extension manifest version $HAVE -> $WANT"
