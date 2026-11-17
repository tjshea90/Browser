# CHECKPOINT (hand-written, lane ccr-5531c47b-hl1gzo, from the Swoop keyboard session) - read me first, then TASKS.md

**Written:** 2026-11-17T16:01Z - **Branch:** `ccr-5531c47b-hl1gzo` (based on the alpha64 lane tip 20e28803; the alpha65 lane ccr-e89b9dfe-pl3lk5 is active: DO NOT release from this branch while it ships, merge its tip first)

## Just done
SW1 (TASKS.md): vendored keyboard engine re-synced to the Keyboard repo (KEYBOARD_PIN), mini keyboard glide-vs-tap + special-character return, the panel's bar can open the Swoop keyboard floating (SwoopLink, MiniNav.navSwoop, Prefs.swoopKeyboard, Settings switch), docs/KEYBOARD-LINK.md.

## Do this next
Run android.yml (app=gecko, full_build=false) on this branch to compile :geckoapp and run the gates; merge origin/ccr-e89b9dfe-pl3lk5 when it has shipped alpha65; ship the combination as the next alpha only if that lane is idle. Nothing here is proven on the tablet.
