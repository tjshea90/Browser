---
date: 2026-11-14T20:00:32.265Z
project: Browser
session: bc5a08af
schema_version: 1
next_steps_count: 5
has_blockers: true
keywords: 
headline: Goal: Kestrel Browser (repo tjshea90/Browser, develop+push ONLY on branch ccr-a57c163e-mtq8js): very light, fast, privat
---

# Session Handoff Snapshot
> **Generated:** 11/14/2026, 8:00:32 PM

## 🎯 High-Level Objective
* **Goal:** Goal: Kestrel Browser (repo tjshea90/Browser, develop+push ONLY on branch ccr-a57c163e-mtq8js): very light, fast, private Android browser tuned for ONE device: MediaTek MT8168B tablet, Android 10 Go API 29, 2 GB RAM, 600x973 @170 dpi, 32-bit, no Google services/Play Store, internet only via phone hotspot. Two builds: Kestrel (:app, system WebView) and Kestrel G (:geckoapp, bundled GeckoView 160). Core intent: owner browses one-handed on a big tablet and wants zero interruptions (no prompts), a navigation panel that appears where his finger is and can be carried anywhere, and phone-keyboard parity (glide typing, cursor slide, learning that fixes sloppy glides, exportable learned data for a future system keyboard). Standing process: save ladder (autosave hooks, tools/ckpt.sh after every step, TASKS.md boxes before code), releases only via ship.sh + GitHub Actions android.yml, answers to owner in plain words naming what was NOT run on the tablet.

## 📌 Current State & Next Steps
* **Status:** DONE: alpha38, alpha39, alpha40 released and recorded; AY1-AY5 ticked in TASKS.md with evidence; HANDOFF.md entry + research/MINI_NAV.md section AY2/AY3/AY4 written; owner answered. Tests green: core, app, geckoapp unit suites, lint, web-tests/openspace.test.js (12), OwnerRulesTest guards. REMAINING: (1) owner feedback from tablet on alpha40 (long-press feel, 400 ms pick-up, slide distance), nothing else claimed or half done; (2) AX1g owner YES/NO: separate system keyboard app (Android input method reusing :core, starting from the export file), not started; (3) tree-only change NOT in alpha40: BrowserActivity SLOW_PAGE_MS lowered 60 s -> 30 s (ships with next release, needs versionCode 142). Emulator emulator-5554 (AVD kestrel29x64) was running with the debug build and lab server tools/sitelab/presslab.py on port 8002 (may be gone in a new container).
* **Blocker:** None. Open owner items: AX1g system keyboard YES/NO; tablet feedback on alpha40.
* **Next Action:** Wait for owner tablet feedback on alpha40 (long-press feel: PICK_MS 400, SHOW_BELOW_DP 26, SLIDE_START_DP 10 / SLIDE_STEP_DP 11 in the MiniNav companion; open-space detection on real sites); tune the constants if he reports misses or accidental pick-ups.

### Remaining Queue
- [ ] AX1g: if owner says YES build a separate system keyboard app (Android InputMethodService reusing :core, importing the export file; spec docs/KEYBOARD-EXPORT.md, research/GLIDE_TYPING.md section 11.8); if NO close the box.
- [ ] Next release carries the tree-only SLOW_PAGE_MS 30 s change; bump geckoapp/build.gradle.kts to versionCode 142 and follow the release recipe; never reuse tag kestrel-g-v0.3.0-alpha40.
- [ ] Possible follow-ups (not asked): panel inside srcdoc/blank frames and the PDF viewer (extension does not run there; fallback detector only covers pdf URLs), investigate the ProxyLinkTest flake, open items in TASKS.md.
- [ ] On session start: bash tools/resume.sh, read CHECKPOINT.md "Do this next" and TASKS.md open boxes; any new owner message goes into TASKS.md in his words before code; run ckpt after every step.

## 🛠️ Modified Files Delta
* geckoapp/src/main/kotlin/app/kestrel/browser/BrowserActivity.kt: askSlowScript auto-CONTINUE; pageAnswersLongPress; hotCornerHost whole-page zone + fired -> MiniNav.showAt; ownActionAt/ownViewAt; press tracking + onOpenSpace + pressSwallowing; dispatchTouchEvent releases neon glows after a swallowed touch; constants SLOW_PAGE_MS (30 s in tree, 60 s in alpha40), OPEN_SPACE_FRESH_MS 2 s
* geckoapp/src/main/assets/kestrel-shields/content.js: open-space contextmenu classifier (stack check, selection logic, huge() wrapper exemption); background.js: os forwarder
* geckoapp/src/main/kotlin/app/kestrel/browser/web/ShieldsBridge.kt: openSpace callback + os message kind; web/TabDelegates.kt: onSlowScript -> askSlowScript, slowScriptAt reset on page start; PageFieldInput.kt: Field.moveCursor via DPAD keys
* app/src/main/kotlin/app/kestrel/browser/ui/MiniNav.kt: showAt; PanelLayout long-press pick-up, grip tap closes, no snap-dock, lit outline; space-slide cursor; BarView caret; coversWindowPoint via zoneAt; test hooks. HotCorner.kt docs; SettingsView.kt texts; PageField.kt moveCursor; web/Tab.kt slowScriptAt
* core/src/main/kotlin/app/kestrel/core/input/MiniKeyboard.kt: model cursor/moveCursor/restartText; core tests MiniKeyboardTest + new OwnerRulesTest (source-reading guards for AY1-AY4)
* web-tests/openspace.test.js (new, 12 tests); app/src/test/.../MiniNavTest.kt (new pick-up/showAt/grip tests); tools/emu/glide_emu.py (holddrag, slide); tools/sitelab/presslab.py (lab page, /busy 50 s script, input logging)
* docs: research/MINI_NAV.md section AY2/AY3/AY4 (what each long press delivers, classifier, emulator recipe); HANDOFF.md AY entry; TASKS.md AY1-AY5 ticked; BUILDLOG.md alpha38-40 lines; geckoapp/build.gradle.kts versionCode 141 / 0.3.0-alpha40; extension manifest 1.0.141
* alpha39 (earlier this session): core/input/glide PatternMemory.kt, LearnedExport.kt, PersonalModel line kind s, KeyboardFlow Export/Import, docs/KEYBOARD-EXPORT.md

## 🚫 Failed Approaches (DO NOT RETRY)
* Approach: adb input swipe x y x y 800 as a long press on the emulator → Result: Gecko saw a tap (touchstart, touchend, click), no contextmenu → Lesson: use real evdev injection tools/emu/glide_emu.py holddrag (needs adb root; after an adbd restart re-run adb root AND adb reverse tcp:8002 tcp:8002).
* Approach: treat "selection changed between contextmenu and +200 ms" as a text press → Result: emulator: a blank press while a word was selected failed because the engine clears the old selection → Lesson: text press = NEW non-empty selection only (fixed in alpha40 content.js, web test added).
* Approach: Android-side whole-screen long-press detector for open space → Result: rejected before shipping, it would fire on links/images and steal link menus (Android cannot tell) → Lesson: classify in the page script; keep the detector only as fallback.
* Approach: add open-space tests to web-tests/content.test.js → Result: file-level 60 s node test timeout hit (whole file about 55 s) → Lesson: separate file openspace.test.js.
* Approach: MiniNavTest idle() between DOWN and UP, then UP at the old coordinates → Result: Robolectric re-lays the window out during idle, the finger lands on another key, nothing typed → Lesson: call relayout() after idle() and recompute the point.
* Approach: run ship.sh gates while the qemu emulator (about 90% of one core) and other jobs run → Result: flaky core test ProxyLinkTest.theRenewalKeepsItOpenPastTheFirstArming failed once; web-tests background.test.js flaked once under parallel load → Lesson: kill -STOP the qemu pid during gates then kill -CONT, rerun alone to confirm a flake, do not edit the tests for it.
* Approach: SLOW_PAGE_MS 60 s for "page is stuck, use fallback detector" → Result: after a recovered slow script, link long presses would open the panel for up to 60 s → Lesson: 30 s is enough (engine re-asks every 20 s while stuck); change is in the tree, not in alpha40.
* Approach: foreground sleep over about 55 s, or a backgrounded subshell inside run_in_background → Result: hook blocks long sleeps; run_in_background returns at once and its notification fires immediately → Lesson: poll log files with bounded 5 s sleep loops and EXIT/DONE markers.

## 🔑 Crucial Context & Implicit Rules
* Stack: :app kotlin-stdlib only, framework Views, no AppCompat/Compose/Material/Room/WorkManager/coroutines; :geckoapp adds GeckoView 160 armeabi-v7a only; never GMS; minSdk 29 targetSdk 36; :core pure Kotlin JVM, no Android imports; no native code; injected JS conservative ES5.
* Branch: develop and push only on ccr-a57c163e-mtq8js; never force-push; no PRs unless asked; repo is PUBLIC: no keys, owner photos, personal data, APKs; repo text ASCII only; hooks block curl/wget, absolute home paths in written files, recursive-delete text.
* Save ladder: autosave hook commits+pushes after tool calls; run bash tools/ckpt.sh "did" "next" after every step with a specific next; write owner requests into TASKS.md in his words as [ ] boxes BEFORE code; claim with bash tools/claim.sh ID, tick with --done "evidence"; cold resume: bash tools/resume.sh then CHECKPOINT.md "Do this next".
* Release: bash ship.sh gecko "note" -> mcp__github__actions_run_trigger (owner tjshea90, repo Browser, workflow android.yml, ref = branch, inputs app=gecko full_build="true" note) -> wait for tag (git ls-remote --tags) + all jobs success (actions_list list_workflow_jobs) -> get_release_by_tag shows APK -> bash tools/record-release.sh TAG with the IDENTICAL note -> answer owner with the Release link on its own line. Never ship a tag twice, never add a push: workflow trigger, versionCode must exceed last recorded (next 142).
* Tests: ./gradlew --offline -q :core:test :app:testDebugUnitTest :geckoapp:testDebugUnitTest and :app:lintDebug (run in background, poll log for EXIT); cd web-tests && npm test (60 s per-file timeout); ProxyLinkTest and background.test.js can flake under CPU load.
* Emulator: AVD kestrel29x64 x86_64 -no-accel (boot 3-5 min); adb /opt/android-sdk/platform-tools/adb; debug APK ./gradlew :geckoapp:assembleDebug -PgeckoAbi=x86_64; package app.kestrel.browser.gecko.debug, activity app.kestrel.browser.MainActivity; adb root and adb reverse needed after an adbd restart; touches via python3 -B -I tools/emu/glide_emu.py (holddrag, slide, tap, shot, layout); lab server python3 -B -I tools/sitelab/presslab.py 8002 LOGFILE then adb reverse tcp:8002 tcp:8002; first engine start after an install takes about 70 s.
* Owner rules: answer every owner prompt in plain words, re-list open items at the end, say honestly what was NOT run on the tablet; if a tool call is denied, resend once; foreground sleep over ~55 s is blocked.

## Key Decisions
- Decision: Slow script: askSlowScript in BrowserActivity answers SlowScriptResponse.CONTINUE at once, no UI of any kind; counter gecko.slowScript.waited + Health log line (private tab URL never logged); Tab.slowScriptAt marks a stuck page. WHY: owner message 39 saw the WAIT/STOP prompt too often and will close stuck tabs himself; guarded by core OwnerRulesTest (source reading).
- Decision: Open space is decided by the PAGE script, not Android: GeckoView ContentDelegate.onContextMenu reports only links/images/media; a blank-space long press reaches only content scripts as a trusted DOM contextmenu event (engine selects the word ~250 ms AFTER the event). Chain: kestrel-shields content.js -> background.js (os forwarder, top-page URL via tabTop) -> ShieldsBridge openSpace -> BrowserActivity.onOpenSpace -> MiniNav.showAt. Message carries only the URL. WHY: only the page knows what is under the finger; an Android-only detector would fire on links and steal their menu.
- Decision: Classifier (content.js): NOT open space if the elementsFromPoint stack (10 deep) has a link with href, img/picture/video/audio/canvas/object/embed/iframe, form controls, button, label, summary, editable region, widget role, onclick or tabindex>=0 (ignored for full-screen wrappers 95% x 90%), or computed cursor pointer. Text press = NEW non-empty selection 200 ms later; a press inside already-selected rects = text. WHY: found on emulator that the engine CLEARS an old selection on a blank press, so "selection changed" was the wrong test.
- Decision: Fallback: old HotCorner touch detector (380 ms, still finger) now covers the WHOLE page area, only where the page cannot answer (Home, error screen, non-http, PDF url, no session, page with slow-script ask < 30 s ago). Host.covers = BrowserActivity.ownActionAt (open panel, Home search box/Go/suggestions, error-screen buttons). WHY: keep own long presses (EditText selection) and link menus intact.
- Decision: Panel closes by TAP on the grip bar (top bar of the panel); corner toggle removed. WHY: any long press now opens it so a close gesture was needed; Settings > Mini navigation panel still switches the whole feature off. showAt: centred on finger, top edge 26 dp below it (SHOW_BELOW_DP), clamped to page area, no slide, geometry saved.
- Decision: Panel floats (MiniNav PanelLayout): long press PICK_MS 400 on buttons/bar/frame (never on keys: keys keep digit-hold + glide) picks up, sends touch cancel to the child, haptic, lit outline, finger carries it, no snap-dock any more; grip keeps immediate drag. Both timer and event timestamps count (busy UI thread). coversWindowPoint uses panel zoneAt != none (transparent band belongs to the page). WHY: owner asked to move it by long pressing and dragging.
- Decision: Space key slide: 10 dp to start, 11 dp per character; address bar uses core MiniKeyboardModel cursor/moveCursor (bar draws caret), page field gets DPAD_LEFT/RIGHT via PageField.moveCursor (PageFieldInput Field.moveCursor, max 64 steps). WHY: other keyboard apps do it; typing/backspace/glide now work at the cursor.
- Decision: Neon glow fix: swallowed touches (HotCorner swallow + new pressSwallow) never reached the UP handler so Neon.touching stayed true (glows frozen); BrowserActivity.dispatchTouchEvent now releases glows when a swallowed touch ends. WHY: pre-existing bug since the corner long press, far more visible with open-space long press.
- Decision: New open-space web tests live in web-tests/openspace.test.js (own file with own harness). WHY: content.test.js is already near the 60 s per-file node test timeout.

## 🙋 User Contribution (human-authored)
Owner reported Kestrel closing on the www.ally.com dividend-reinvestment page (message 38) and sent a tablet screenshot of the "This page is slow" dialog on live.invest.ally.com (message 39).
Owner specified every behavior shipped in alpha39/alpha40 in his own words (messages 38 and 39) and tests releases on the real tablet.

## 🙋 User Decisions (made by the human)
- **Wait for slow scripts automatically with no prompt; owner closes a stuck tab himself**
  - Reason: the This page is slow dialog appears too often (message 39)
  - Rejected: showing the prompt
- **Mini navigation panel appears under the finger on a long press of any open space, but a long press on a link gives that link's options**
  - Reason: one-handed use of a big tablet (message 39)
  - Rejected: only the top right corner
- **Panel is a floating panel moved by long pressing it and dragging**
  - Reason: easy repositioning (message 39)
- **Space key slide moves the text cursor**
  - Reason: like other keyboard apps do (message 39)
- **Keyboard learned data must be exportable**
  - Reason: owner may make this keyboard the system keyboard for all apps and import what it learned (message 38)

## Summary
Session shipped three Kestrel G releases. alpha38 (versionCode 139): Ally passkey crash fix (passkeys switched off in engine). alpha39 (140): keyboard learns glide SHAPES + corrections (PatternMemory/Fingerprint, remembered-word shortlist) + Export/Import of learned data (LearnedExport v1, docs/KEYBOARD-EXPORT.md). alpha40 (141, GitHub run 43634029105 green, tag kestrel-g-v0.3.0-alpha40, recorded in BUILDLOG.md): owner message 39 = AY1 slow-script prompt removed (engine asks -> auto CONTINUE, no UI), AY2 long press on OPEN space puts the mini navigation panel under the finger (links/images/text keep own behavior), AY3 space key slide moves text cursor (address bar + page field), AY4 panel floats: long press (400 ms) on buttons/bar/frame + drag moves it, it stays where put, tap on grip bar closes it. Emulator-proven with real touches; NOT tried on owner tablet. Owner answered in plain words with Release link.

---
*A short hint surfaces on session start; full context loads only if your next prompt matches a keyword above, or via manual `/handoff-resume`. The same record is available as a sibling `.json` file for external tools.*