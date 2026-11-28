# AGENTS.md

<!-- handoff:learnings:begin -->
## Session Learnings (auto-updated by handoff)

### Implicit Rules
- Stack: :app kotlin-stdlib only (framework Views; no AppCompat/Compose/Material/Room/WorkManager/coroutines); :geckoapp adds GeckoView 160 armeabi-v7a only; never any Google/GMS dependency; minSdk 29 targetSdk 36; :core pure Kotlin JVM (no Android imports); no native code; injected JS conservative ES5.
- Branch: develop and push ONLY on ccr-b3c2484c-sbew36; never force-push; no PRs unless asked; repo is PUBLIC (no keys, owner photos, APKs; repo text ASCII only; hooks block curl/wget and absolute home paths in written files).
- Save ladder: autosave hook commits+pushes after tool calls; run bash tools/ckpt.sh "did" "next" after every step with specific next; write owner requests into TASKS.md in his words BEFORE code; cold resume = bash tools/resume.sh then CHECKPOINT.md 'Do this next'.
- Release recipe: bump geckoapp/build.gradle.kts versionCode/versionName, bash tools/sync-ext-version.sh, bash ship.sh gecko "<note>" (note ASCII, no double quotes/backslashes, kept in docs/handoff/ALPHA<N>_RELEASE_NOTE.txt), ONE dispatch of android.yml (app=gecko full_build=true note) via mcp__github__actions_run_trigger, verify tag (git ls-remote --tags origin) + all jobs green + get_release_by_tag, THEN bash tools/record-release.sh <tag> with IDENTICAL note; never ship a tag twice; never add push: workflow trigger.
- Lab: android.yml app=lab lab_suite=<kp|np|cp|ap|ho|vp...> (vp = everything video, ~15-20 min); artifacts emulator-lab-text via mcp__github__actions_get download_workflow_run_artifact then python3 -I tools/emu/fetch_artifact.py <url> <empty dir>; use get_workflow_job (small) not get_workflow_run; foreground sleep >55 s is blocked (use run_in_background).
- Tests: cd web-tests && npm test (573 tests); ./gradlew --offline -q :core:test :app:testDebugUnitTest :geckoapp:testDebugUnitTest.
- Owner rules: answer every owner message in plain words, say what was NOT run on the tablet, re-list open items at end, never use Workflow without opt-in.

### Key Decisions
- Decision: page gate (kestrel-shields/gate-main.js, page world) asks app on a press via event __kst_gate_ask_v1 -> content.js -> background.js message 'vhand' -> ShieldsBridge native 'vh' (UI thread) -> VideoFlow.handOver/start -> core WebHandOver.decide -> NativePlayerActivity(fromPage); yes = page element stays closed/paused and page play() rejected AbortError; no or 700 ms timeout = page plays itself — WHY: app knows sniffed playlists/tabs/setting, page only reports a press.
- Decision: trusted 'play' event of native <video controls> (gate nativePlay) counts as the press — WHY: lab on real engine showed taps on native controls send NO pointer/click events to page listeners, so pressAt never ran.
- Decision: page scripts can fake the ask event, so content.js relays only while navigator.userActivation (or own trusted listeners, 12 s) shows a touch; app (BrowserActivity.lastPressAt, tapInPage, keys only when not typing) requires a real tap inside page view within 15 s (30 s full screen / late playlist), used up by a launch — WHY: owner hard rule = nothing plays unless he presses play.
- Decision: review fixes — playLike only for real play controls (word play) so Mute/Share do not start films; refused press tied to its page url; Kestrel's own bar asks whenever film not playing; late yes sets yesAt and clears 30 s no; page play() interrupted until a NEW press after a yes; 3 s repeat guard cleared when player fails/ends; held MediaSource source waits for answer; frame url sent whole (2000 chars); ShieldsBridge parses each message once — WHY: independent review found 9 real problems.
- Decision: film pressed again resumes where Kestrel's player stopped (core WebHandOver.resumeMs, RAM only, never private tabs) — WHY: Back then play must not restart a half-watched film.
- Decision: setting 'Web videos play in' works only while press-to-play rule is on (gate carries it) — WHY: hand-over depends on the gate.
- Decision: decline by design — YouTube-style blob+DASH without playlist address, DRM, DASH, loop clips, tiny boxes; cookies not shared with native player so a login-only film fails then page plays it for 2 minutes — WHY: no safe source URL to hand over.

<!-- handoff:learnings:end -->
