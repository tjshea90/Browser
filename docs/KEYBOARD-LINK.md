# One keyboard engine, two apps (Swoop <-> Kestrel)

Owner, 2026-11-17: the Kestrel browser should use the Swoop keyboard's improvements, keep its own floating navigation panel (which can open the floating keyboard), and the two must not diverge.

## How they are linked
* The glide / typing engine (`app.kestrel.core.input`: decoder, word list, personal model, suggester, dictionary, emoji, clipboard-free input state machine) is **developed in `tjshea90/Keyboard`** (`core/`). This repo holds a **vendored copy** of `core/src/{main,test}/kotlin/app/kestrel/core/input/**` and of the word list `geckoapp/src/main/assets/glide/en.klx`. Never edit them here.
* `KEYBOARD_PIN` names the Keyboard commit and the fingerprint (sha256) of the engine sources and the word list.
* Take an update: `bash tools/sync-keyboard-core.sh <Keyboard checkout>` (refuses an uncommitted checkout), run the tests, commit. Check: `bash tools/check-keyboard-core.sh` fails when the copy differs from the pin (a hand edit here would diverge the engines).
* The Keyboard repo prints the same fingerprint with `bash tools/core-input-hash.sh`.
* Verified on the branch that introduced this: `:core:test` and `:app:testDebugUnitTest` green with the vendored engine, `:app:compileDebugKotlin` green. `:geckoapp` could NOT be compiled in the cloud sandbox (SDK platform 37 missing): compile it in CI before merging.

## Learned data: why it is not shared by IPC
Both apps keep the learned memory in their own private storage. Sharing it live (a ContentProvider or bound service) would have to be protected by a signature permission, but BOTH apps are signed with the PUBLIC dev key, so any app anyone signs with that key could read the owner's typing history. That is not acceptable, so there is no such channel. The learned data is shared by:
1. Using Swoop as the system keyboard everywhere on the phone / tablet: every text box, including the browser's page fields, then feeds the ONE learned memory in Swoop.
2. The existing Export / Import (same file format `KESTREL-KEYBOARD-LEARNING`, read by both apps) for the browser's own address-bar mini keyboard: export in one app, import in the other.
Owner decision needed if a live link is wanted anyway: it requires a private signing key (a repo secret) instead of the public dev key.

## The navigation panel and the floating keyboard
Not changed in this step. The panel keeps its own mini keyboard. To open Swoop's floating keyboard from the panel, focus a text field and request the system keyboard (`InputMethodManager.showSoftInput`); Swoop opens floating if it was left floating (Settings > Layout and look). This wiring needs a device test and was not done blind.
