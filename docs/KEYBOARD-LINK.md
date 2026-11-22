# One keyboard, two apps (Swoop <-> Kestrel)

Owner, 2026-11-17 (request 16): the Kestrel browser should use the Swoop keyboard's improvements, keep its own floating navigation panel (which can open the floating keyboard), and the two must not diverge, learning included.

## The design in one paragraph
Swoop is THE keyboard. When Swoop is the chosen system keyboard, everything typed in Kestrel (the address bar, the page's text fields) goes through Android's normal input path into Swoop, so there is one learned memory and one dictionary, and nothing can diverge. The Kestrel navigation panel opens Swoop FLOATING over the page when its bar is tapped. When Swoop is not installed or not chosen, the panel falls back to its own small keyboard, which runs the SAME engine code (a pinned, checked copy) so its guesses, rare-word filter and learning behave the same; only its learned file is separate (see "Learned data").

## 1. The shared engine (code that must not diverge)
* The glide / typing engine (`app.kestrel.core.input`: layout, decoder, word list, personal model, suggester, dictionary, emoji, input state machine) is DEVELOPED in `tjshea90/Keyboard` (`core/`). The Kestrel repo holds a vendored copy of `core/src/{main,test}/kotlin/app/kestrel/core/input/**` and of the word list `geckoapp/src/main/assets/glide/en.klx` + `LICENSE.txt`. Never edit them in Kestrel.
* Kestrel's `KEYBOARD_PIN` names the Keyboard commit and the sha256 of the engine sources and of the word list. Take an update: `bash tools/sync-keyboard-core.sh <Keyboard checkout>` (it refuses an uncommitted checkout), run the tests, commit. `bash tools/check-keyboard-core.sh` fails when the copy differs from the pin, and `tools/test_handoff.sh` runs it, so a hand edit there cannot go unnoticed.
* This repo prints the same fingerprint with `bash tools/core-input-hash.sh`.
* After ANY change under `core/.../input` here: commit it, then run the sync in the Kestrel repo (on its own branch, never main) and its tests.

## 2. Learned data: ONE memory
* With Swoop chosen (the normal case on the tablet and the phone) there is exactly one: Swoop's private store. Kestrel adds nothing of its own to it and takes nothing from it.
* Why there is no live channel between the two apps: a ContentProvider or bound service would have to be protected by a signature permission, but BOTH apps are signed with a PUBLIC dev key, so any app anyone signs with that key could read the owner's typing history. Not acceptable; no such channel exists. (A live link would need a private signing key kept as a repo secret: the owner's decision, not taken.)
* The panel's own small keyboard (the fallback) keeps its learned file in Kestrel's own storage in the same text format (`KESTREL-KEYBOARD-LEARNING`); Export in one app and Import in the other moves what was learned.

## 3. The navigation panel opens Swoop floating
* Kestrel: tapping the panel's bar calls `MiniNav.Listener.navSwoop(true)`. `BrowserActivity` (Gecko build) answers true only when Settings > "Use the Swoop keyboard from the panel" is on (default) AND Swoop is the chosen keyboard (`SwoopLink.isChosen`: Android's `default_input_method` is `app.swoop.keyboard/...`). It then focuses the page's text field (or the address bar), asks Android to show the keyboard, and sends Swoop one private command: `InputMethodManager.sendAppPrivateCommand(view, "app.swoop.keyboard.FLOAT", null)`. A second tap on the bar puts the keyboard away (`ACTION_DOCK` exists too). When the answer is false the panel's own keys open as before.
* Swoop: `KeysService.onAppPrivateCommand` floats or docks the keyboard (`KeyboardRoot.requestFloating`). Android delivers such a command only to the CURRENT keyboard and only from the app whose text box is being edited; it carries no data and reads nothing, so no other app gains anything by sending it.
* Tested here: `KeysServiceTest.theKestrelNavigationPanelCanFloatAndDockTheKeyboardWithAPrivateCommand`, Kestrel's `MiniNavTest` (the three Swoop tests) and `SwoopLinkTest`. NOT proven on a device: the real round trip (Kestrel field focus -> Swoop shown -> floating) needs the tablet; the emulator smoke test covers Swoop's floating on its own.

## 4. Improvements copied into Kestrel's own mini keyboard (the fallback)
* Engine (automatic through the copy): the 140,000-word list with the rare-word floor ("tui" no longer beats a common word), the learner, the suggester, the user dictionary code, emoji map.
* Glide versus tap (M9): a quick swipe to the next key is a glide, not one letter (`MIN_GLIDE_PITCHES` 1.2 -> 0.45, a glide starts after 5 dp, a path that stays on its key is a tap).
* One special character brings the letters back (M5), in `MiniKeyboardModel` (shared engine file).
* Not copied (they live in Swoop, which the panel opens): the clipboard, translate, the hide-the-letters option, the squeeze / floating window, the dictionary screen and the long-press removal of a guess. Reason: they are screens and window behaviour of a system keyboard, and Kestrel now opens Swoop for them.
