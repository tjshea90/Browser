#!/usr/bin/env bash
# Fails when the vendored keyboard engine differs from KEYBOARD_PIN (see tools/sync-keyboard-core.sh).
set -euo pipefail
here="$(cd "$(dirname "$0")/.." && pwd)"
want="$(grep '^engine_sha256=' "$here/KEYBOARD_PIN" | cut -d= -f2)"
wantlist="$(grep '^word_list_sha256=' "$here/KEYBOARD_PIN" | cut -d= -f2)"
base="$here/core/src/main/kotlin/app/kestrel/core"
got="$( ( cd "$base" && find input -type f -name '*.kt' | LC_ALL=C sort | while read -r f; do printf '%s %s\n' "$f" "$(sha256sum "$f" | cut -d' ' -f1)"; done ) | sha256sum | cut -d' ' -f1)"
gotlist="$(sha256sum "$here/geckoapp/src/main/assets/glide/en.klx" | cut -d' ' -f1)"
[ "$got" = "$want" ] || { echo "the keyboard engine in core/input differs from KEYBOARD_PIN: edit it in the Keyboard repo and run tools/sync-keyboard-core.sh" >&2; exit 1; }
[ "$gotlist" = "$wantlist" ] || { echo "the word list differs from KEYBOARD_PIN" >&2; exit 1; }
echo "keyboard engine matches the pin"
