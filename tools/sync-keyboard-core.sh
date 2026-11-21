#!/usr/bin/env bash
# One engine, two apps (owner, 2026-11-17: the Kestrel keyboard must use the Swoop keyboard's improvements and "they should not diverge").
# The glide / typing engine (package app.kestrel.core.input) is DEVELOPED in the public repo tjshea90/Keyboard (core/). This repo carries a vendored copy:
#   core/src/main/kotlin/app/kestrel/core/input/**  and  core/src/test/kotlin/app/kestrel/core/input/**   (never edit them here: edit them in the Keyboard repo)
#   geckoapp/src/main/assets/glide/en.klx + LICENSE.txt   (the shared word list)
# KEYBOARD_PIN records which Keyboard commit the copy is from and the fingerprint of the engine sources.
# Update:  bash tools/sync-keyboard-core.sh <path to a checkout of tjshea90/Keyboard at the commit to take>   then run the tests, commit.
# Check:   bash tools/check-keyboard-core.sh   (fails when the vendored copy differs from the pin: run by tools/test_handoff.sh)
set -euo pipefail
k="${1:?usage: sync-keyboard-core.sh <Keyboard checkout>}"
here="$(cd "$(dirname "$0")/.." && pwd)"
sha="$(git -C "$k" rev-parse HEAD)"
if [ -n "$(git -C "$k" status --porcelain core ime/src/main/assets/glide)" ]; then echo "the Keyboard checkout has uncommitted changes in core/ or the word list: commit and push them there first" >&2; exit 1; fi
rm -rf "$here/core/src/main/kotlin/app/kestrel/core/input" "$here/core/src/test/kotlin/app/kestrel/core/input"
cp -r "$k/core/src/main/kotlin/app/kestrel/core/input" "$here/core/src/main/kotlin/app/kestrel/core/"
cp -r "$k/core/src/test/kotlin/app/kestrel/core/input" "$here/core/src/test/kotlin/app/kestrel/core/"
cp "$k/ime/src/main/assets/glide/en.klx" "$k/ime/src/main/assets/glide/LICENSE.txt" "$here/geckoapp/src/main/assets/glide/"
printf 'keyboard_repo=tjshea90/Keyboard\nkeyboard_commit=%s\nengine_sha256=%s\nword_list_sha256=%s\n' "$sha" "$(bash "$k/tools/core-input-hash.sh" "$k")" "$(sha256sum "$here/geckoapp/src/main/assets/glide/en.klx" | cut -d' ' -f1)" > "$here/KEYBOARD_PIN"
echo "synced from $sha"; cat "$here/KEYBOARD_PIN"
