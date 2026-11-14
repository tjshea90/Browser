# The pop-up guard on the very first page after a cold start (RX3-G1)

Row: RX3-G1 (research/REVIEW-EXTERNAL-3.md part G). Newest first. ASCII only.

## 1. Problem (read in the code, 2026-11-14)
`guard-main.js` (the pop-up / forced-redirect guard, runs in the PAGE world) and `scriptlets-main.js` (our scriptlet library, page world) were registered at RUN TIME by `background.js` with `browser.contentScripts.register(...)`. A script registered from the background page exists only after that page has started and the promise has resolved. A document that begins before that (the first page after a cold start, opened from a link or restored) has no `window.open` guard and no scriptlet library. AP1's own note had seen it on the emulator: "that page opened a pop-up tab and had no guard".

## 2. What changed (alpha41)
- `manifest.json` DECLARES both scripts in `content_scripts`: `world: "MAIN"`, `run_at: document_start`, `all_frames`, `match_about_blank`, `match_origin_as_fallback`, after `content.js` (which listens for the guard's ready event). Declared scripts exist from the moment the extension starts, before the background page runs a line.
- `background.js` no longer registers either of them (it only keeps the per-host scriptlet calls, registered after the library: scripts declared in the manifest always run first). `guardState` / `slState` (Diagnostics texts) now say "declared in the manifest".
- Both files return at once in any world that has the extension API (`browser.runtime`): they belong in the page world only. They also keep their idempotence markers (`window.__kstGuard`, `window.__kstSL`).
- Gates: `tools/test_handoff.sh` fails if the manifest stops declaring them (world MAIN, document_start, all frames); `web-tests/background.test.js` pins the same and the old "library registered first" test now says nothing is registered at run time.
- The manifest version moves with the release (1.0.142 for alpha41): GeckoView re-installs the built-in extension only when the version differs.

## 3. Measured on the emulator (2026-11-14; Android 10 x86_64 emulator, GeckoView 160 debug build; NOT the tablet)
Lab: `tools/sitelab/guardlab.py <port> <log>` page `/fakeplay` (tries `window.open` by script, no tap, at 0.3 s, 3 s, 8 s, 15 s; logs whether the guard's marker is in the page world at its very first script and whether a window object came back) and `tools/emu/firstpage_emu.py` (force-stop, cold start from a link, report).
| Build | Guard marker at the page's first script | The page's own about:blank frame | Pop-ups stopped |
| :- | :- | :- | :- |
| before (run-time registration only) | not recorded (the probe did not exist yet) | - | 4 of 4 (one run; the race did not show) |
| with ONLY the manifest declaration (run-time registration switched off for the experiment) | **YES** (guard and library) | **YES** (marker in a frame the page created itself, its `window.open` stopped) | 4 of 4 |
Findings: (1) GeckoView 160 honours `world: "MAIN"` for manifest content scripts, and `match_about_blank` + `match_origin_as_fallback` for the frames a page makes itself: so the run-time registrations are redundant and were removed (keeping both would make every document parse the scripts twice). (2) The old code's race could NOT be reproduced in one cold start; the proof that the guard is there from the first script rests on the marker, not on a before/after difference. (3) A run directly after `pm clear` (the extension's very first installation) let the first pop-ups through: no registration style can help while the extension itself is not installed yet; it happens once per installation.
Not run: the tablet.

## 4. Rerun
`python3 tools/sitelab/guardlab.py 8090 <log>`, boot the emulator, install the debug APK (`./gradlew :geckoapp:assembleDebug -PgeckoAbi=x86_64`), `python3 -u -I tools/emu/firstpage_emu.py --port 8090 --log <log>`; the log shows `first-script guard in the page world: YES|no`.
