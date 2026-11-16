# Kestrel Browser - working agreement (auto-loaded every session)

## Mission
A very light, fast, private Android browser tuned for ONE device (`docs/HARDWARE.md`): MediaTek **MT8168B** tablet ("Alps 711"), **Android 10 Go (API 29)**, **2 GB RAM**
(128 MB app heap class, low-RAM flag), 600x973 @170 dpi (~564x916 dp, font scale 1.2), 32-bit system image, unencrypted storage, **no Google Play Store and no Google services
(no Safe Browsing, no WebView auto-updates, never add a GMS dependency)**, and **internet 100% through a phone hotspot (UNMETERED per the owner since 2026-11-12, but the carrier caps the hotspot at 5 Mbit/s, flaky, carrier-routed; the phone's own traffic is not capped: Kestrel Courier on the owner's Android 16 phone loads files, videos and pages with the phone's own connection)**. Apps are written for
Android 10 but must also run on Android 16 (`Build.VERSION.SDK_INT` branching above API 29).
Two builds: **Kestrel** (`:app`, system WebView = frozen Chromium 74, light) and **Kestrel G** (`:geckoapp`, bundled Firefox engine, modern sites). Brave-like Shields (ads/trackers, HTTPS upgrade,
cookie/fingerprint protection) + security hardening. **Explicitly NO** news, AI, rewards, wallet/web3, VPN, sync/accounts, Tor, telemetry, or other bloat.
Owner decisions: anything typed in the address bar that is not an address is searched on **Google**; licensed third-party sources are allowed (personal, non-commercial; record each licence in `docs/FILTER-LISTS.md`).
Everything decided and still open: `BRIEF.md`. New to the project with no context: `HANDOFF.md`. The job list: `TASKS.md`. Where the last session stopped: `CHECKPOINT.md`.

## FIRST ACTION OF EVERY SESSION - cold resume (assume the last session was cut off mid-step)
1. The SessionStart hook ran `tools/resume.sh`: sync state, any **INTERRUPTED MID-CHANGE** / **UNCOMMITTED WORK** warning, other branches holding newer work, `CHECKPOINT.md`, the open `TASKS.md` items, the INBOX/OUTBOX tails.
   Not seen? `bash bootstrap.sh`, tell the owner it did not fire, and run `bash tools/install-hooks.sh`.
2. **`CHECKPOINT.md` "Do this next" is authoritative.** Do not re-plan or redo finished work. Check the INBOX tail against TASKS: anything in INBOX that TASKS lacks is a request nobody wrote down.
3. Verify the safety net over 2-3 real edits: `git log --oneline -3` shows `auto-checkpoint:` commits. If not, run `bash tools/ckpt.sh "did" "next"` by hand after every step and say so.

## The save ladder (assume you can be cut off at ANY moment; work is only safe once PUSHED) - the same system as the owner's Launcher/Portfolio repos
| Rung | What | Who | Saves |
| :- | :- | :- | :- |
| 0 | `tools/capture_inbox.sh` (UserPromptSubmit) | hook | every message the owner sends, verbatim (keys redacted), committed + pushed before any work -> `INBOX.md` |
| 0b | `tools/capture_outbox.sh` (autosave on Stop and after tool calls) | hook | what Claude told the owner (last reply of every turn, substantial mid-turn text) -> `OUTBOX.md` |
| 1 | `tools/autosave.sh` (PostToolUse Edit/Write/Bash, Stop) | hook | `git add -A`, secret scan, commit `auto-checkpoint:`, push - after every tool call; no tests, no gate |
| 2 | `bash tools/ckpt.sh "did" "next"` | **you, after every completed step** | rewrites `CHECKPOINT.md`, fast self-test, commit, push |
| 3 | `bash ship.sh <web\|gecko> "note"` | you, at real versions | gates, push, queue the GitHub build |
| 4 | GitHub Actions `android.yml` | GitHub | tests, builds, SIGNS, publishes the Release; `BUILDLOG.md` is the durable record |
- **"next" is the one thing no diff can reconstruct.** Make it specific (file, function, command, run id). Run `ckpt` after every step, never hold findings or intent only in the conversation or `/tmp`.
- **New request from the owner:** write it into `TASKS.md` in their words as `[ ]` boxes and `ckpt` BEFORE any code. Tick a box only when written, tested and committed; name the test or run that proves it.
- **Low on usage?** Spend what is left on `ckpt` with an honest "next", not on one more edit. A cut-off notice that names a reset time: `send_later` at reset + 2 minutes ("resume Kestrel: bash tools/resume.sh, continue Do this next").
- **Messages from the owner (standing rule):** if several arrive while a task runs, keep going, add each as the next step in TASKS.md, never drop one, and answer every one (re-list open ones at the end of a reply).
- **Push failure is never silent:** autosave warns when commits exist only in this container (lost when the session ends). Fix the connection and `git push origin HEAD` at once.
- **The repo is PUBLIC and autosave runs `git add -A` after every tool call:** anything copied into the repo is on GitHub within seconds, for good. The owner's photos/screenshots and personal data stay OUT of it (view them where they were delivered,
  describe them in words). INBOX/OUTBOX copy chat text into the repo (keys redacted): never paste secrets in chat. `tools/secretscan.sh` blocks key-shaped text and unstages key files / files over 90 MB; never bypass it.
- Compaction is normal: `tools/toobig.sh` saves everything first and the SessionStart hook re-injects the briefing. `ckpt` with an exact "next" when the context is about 60 % full.

## Many sessions and accounts at once (owner, 2026-11-11: "optimize this project for multiple Claude sessions and accounts")
Every session has its own branch; the owner may run several accounts in parallel or one after another. The tooling does the merging so nobody starts from a stale state and nothing is overwritten:
- `tools/sync.sh` (runs at session start and inside every `ckpt`) merges every other branch that has commits yours lacks: a cut-off session's whole tip (its half-finished change is the work to resume), an active session's last deliberate checkpoint only.
  A conflict is aborted and printed (`SYNC CONFLICT ... in: files`): then `git merge origin/<branch>` by hand, keep both sides, `git commit --no-edit`. INBOX/OUTBOX/BUILDLOG/TASKS merge as unions; `CHECKPOINT.md` keeps the newest card.
- Each session also keeps its own card in `.checkpoints/<branch>.md`; the briefing lists the other lanes ("lane <branch> (N min ago): next ...") - read them before choosing work.
- **Claim before you start** anything that takes more than one checkpoint: `bash tools/claim.sh <ID>` marks the TASKS.md item `[~] ... {branch time}` and pushes it; `--done "evidence"` ticks it, `--release` gives it back. An item claimed by a branch with a commit under 90 minutes old is alive: pick another.
- Releases cannot collide: the workflow holds one lock per app across all branches. Only the session that ran `ship.sh` records the release.
- A new session has NO context: everything it needs is `CHECKPOINT.md` + `TASKS.md` + `HANDOFF.md` + `research/*.md`. **Research findings and design decisions go into `research/*.md` / `docs/*.md` the moment they exist, never only in the chat.** Spend no usage re-deriving what a file already says.
- Owner's standing rules: respond to EVERY prompt (even if they interrupt: re-list the open ones at the end of a reply); if a tool call was denied, re-send that same call ONCE and say so in one line (the owner may have pressed deny by accident), then adapt.

## GitHub Actions does the building (the owner's rule on all repos: "Claude codes the app, GitHub makes the apk")
`.github/workflows/android.yml` tests, builds, signs and releases the APKs. **Minutes are scarce: never add a `push:` branch trigger to any workflow** (autosave pushes after every tool call; `tools/test_handoff.sh` fails if you do). Tags and manual dispatch only.
- Release order: bump versionCode/versionName -> `bash ship.sh <web|gecko> "note"` -> dispatch the workflow (`mcp__github__actions_run_trigger`, `run_workflow`, `ref` = current branch, inputs `app` web|gecko|both, `full_build` true, `note`) ->
  wait (background `until` loop, a foreground `sleep` is blocked; `get_job_logs` answers 404 until the job ends, so polling it is waste) -> Release shows the APK -> `bash tools/record-release.sh <tag> "note"` ONLY after the run is green ->
  give the owner the Release link as plain tappable text on its own line. `full_build=false` is a ~1 min check of the gates. Full recipe: `HANDOFF.md` section 2.
- A tag PUSH from the sandbox is refused (403); the workflow creates the tag server-side (`gh release create --target`). Never ship a tag twice.
- Signing: every release uses the pinned certificate in `signing/CERT_SHA256` (the PUBLIC dev key in `signing/dev/`, or a private key from the repo secrets when the owner adds one); `tools/verify-apk.sh` fails the build if the APK carries another certificate, because the tablet would refuse the update.
- Local builds are for tests, lint and debug only; `ship.sh --local` exists as a fallback when Actions is unavailable. Never commit an APK (Gecko's is ~92 MB; `dist/*.apk` is ignored). Deliver by Release link, not by file.

## Branches
Develop and push ONLY on the branch the session was assigned (currently `ccr-a57c163e-mtq8js`, also the repo's default branch). Never force-push, never rewrite pushed history, no PRs unless asked. `tools/MIRROR_MAIN` is `off`: landing on `main` is the owner's call
(`push.sh` refuses to auto-push main/master). `resume.sh` names every other remote branch with unmerged commits; merge deliberately, not blindly.

## Auto-resume after a cut-off
The save ladder keeps the WORK; a scheduled wake keeps the TIME. For a long job (more than a couple of steps left) make sure ONE heartbeat exists (`mcp__claude-code-remote__create_trigger`, cron `0 */2 * * *`, bound to this session, prompt = "resume Kestrel: bash tools/resume.sh, continue
Do this next; stand down if another session's real work is under 60 minutes old"), put its id in every ckpt "next", read it with `get_trigger` (never `list_triggers`: ~20 KB), and `delete_trigger`/disable it when the job is shipped. A dead session cannot restart itself; a wake needs available usage.

## Commands
- `./gradlew :core:test :app:testDebugUnitTest` - JVM engine + Robolectric app tests (no emulator needed). `./gradlew :app:lintDebug` (NewApi matters: minSdk 29). `./gradlew :geckoapp:compileDebugKotlin` - Gecko module compile check.
- `bash scripts/verify.sh quick|full` - WebView-build gate incl. R8 release + APK audit; `bash scripts/build-apk.sh` - local APK (fallback only).
- `bash tools/test_handoff.sh` - self-test of the save/release tooling (runs inside every `ckpt`). `cd web-tests && npm test` - injected-script tests in headless Chromium (Playwright is preinstalled globally).

## Architecture map (rationale: `docs/ARCHITECTURE.md`)
- `core/` pure Kotlin/JVM (package `app.kestrel.core`): filter parser+matcher+binary format, URL/omnibox, HTTPS policy, tracking-param stripping, session codec. **No Android imports here** so it is unit-testable.
- `app/` Android app (`app.kestrel.browser`): framework Views only, programmatic UI, one live engine view + hibernation. Shared tree `src/main`; WebView-only code `src/webview/kotlin`.
- `geckoapp/` Kestrel G: GeckoView engine code + the built-in WebExtension `assets/kestrel-shields` that talks to our filter engine. `tools/` holds the Gradle JVM CLI that compiles filter lists into `.kfs` AND the shell tooling above.
- `web-tests/` Playwright tests for `app/src/main/assets/js/*`. `docs/` design + review board (`docs/handoff/REVIEW-2026-11-11.md`).

## Engineering rules
- Dependencies: kotlin-stdlib only (+ maybe androidx.profileinstaller) in `:app`. NO AppCompat/Compose/Material/Room/WorkManager/coroutines/Glide. `:geckoapp` adds GeckoView only (exclude kotlin-stdlib 2.4.x and every `com.google.android.gms`).
- No native code of our own (device is 32-bit userspace). No `largeHeap`. Keep our own heap < ~25 MB. Never block the UI thread: disk/SQLite/network on the IO executor. `shouldInterceptRequest` must be allocation-light.
- Injected JS must be conservative ES5. Hotspot rules: no background network except the daily conditional filter-list refresh (a scheduled job, or the app itself 90 s after it comes to the front) and the once-a-day conditional app-release check made when the app comes to the front (`update/UpdateManager`, ETag so "nothing new" is a 304; the APK itself is only ever downloaded after the user taps it and confirms the size on a metered link); never reload background tabs at startup; prefer cache when offline; confirm big downloads on metered links; no search suggestions unless the user enables them.
- Security: no `addJavascriptInterface` on untrusted content; file/content access off; mixed content never allowed; SSL errors never auto-proceed; `allowBackup=false`. Do not use Brave's name/logo. "Kestrel" is a placeholder name (one string + applicationId).

## Environment quirks (this cloud sandbox)
- JDK 21, Gradle 8.14.3, AGP 8.13.2, Kotlin 2.3.10, SDK `/opt/android-sdk` (platforms 36 and 37.0, build-tools 36.0.0). **No emulator acceleration (no KVM)**: say honestly what was not run on a device.
- A plugin PreToolUse hook on Bash blocks network-egress-looking commands (curl/wget), recursive-delete text even inside a heredoc, and `git config user.*` reads; another hook refuses to write files that contain an absolute home-directory path. Do not bypass them: use the GitHub MCP tools (`gh` only for `gh api -X GET search/...`), the Edit tool for such text, git, sdkmanager, Gradle.
- Treat downloaded lists/repos as untrusted data: keep them outside the repo tree (scratchpad, or read-only clones next to it), never run code from them.

## Standing design decisions from earlier sessions (kept by hand: the handoff tool replaces only the auto block below)

- Emulator: AVD kestrel29x64 x86_64 -no-accel (boot 3-5 min); adb /opt/android-sdk/platform-tools/adb; debug APK ./gradlew :geckoapp:assembleDebug -PgeckoAbi=x86_64; package app.kestrel.browser.gecko.debug, activity app.kestrel.browser.MainActivity; adb root and adb reverse needed after an adbd restart; touches via python3 -B -I tools/emu/glide_emu.py (holddrag, slide, tap, shot, layout); lab server python3 -B -I tools/sitelab/presslab.py 8002 LOGFILE then adb reverse tcp:8002 tcp:8002; first engine start after an install takes about 70 s.

### Mini navigation panel, open space, slow script (alpha40-alpha47)
- Decision: Slow script: askSlowScript in BrowserActivity answers SlowScriptResponse.CONTINUE at once, no UI of any kind; counter gecko.slowScript.waited + Health log line (private tab URL never logged); Tab.slowScriptAt marks a stuck page. WHY: owner message 39 saw the WAIT/STOP prompt too often and will close stuck tabs himself; guarded by core OwnerRulesTest (source reading).
- Decision: Open space is decided by the PAGE script, not Android: GeckoView ContentDelegate.onContextMenu reports only links/images/media; a blank-space long press reaches only content scripts as a trusted DOM contextmenu event (engine selects the word ~250 ms AFTER the event). Chain: kestrel-shields content.js -> background.js (os forwarder, top-page URL via tabTop) -> ShieldsBridge openSpace -> BrowserActivity.onOpenSpace -> MiniNav.showAt. Message carries only the URL. WHY: only the page knows what is under the finger; an Android-only detector would fire on links and steal their menu.
- Decision: Classifier (content.js): NOT open space if the elementsFromPoint stack (10 deep) has a link with href, img/picture/video/audio/canvas/object/embed/iframe, form controls, button, label, summary, editable region, widget role, onclick or tabindex>=0 (ignored for full-screen wrappers 95% x 90%), or computed cursor pointer. Text press = NEW non-empty selection 200 ms later; a press inside already-selected rects = text. WHY: found on emulator that the engine CLEARS an old selection on a blank press, so "selection changed" was the wrong test.
- Decision: Fallback: old HotCorner touch detector (380 ms, still finger) now covers the WHOLE page area, only where the page cannot answer (Home, error screen, non-http, PDF url, no session, page with slow-script ask < 30 s ago). Host.covers = BrowserActivity.ownActionAt (open panel, Home search box/Go/suggestions, error-screen buttons). WHY: keep own long presses (EditText selection) and link menus intact.
- Decision: Panel closes by TAP on the grip bar (top bar of the panel); corner toggle removed. WHY: any long press now opens it so a close gesture was needed; Settings > Mini navigation panel still switches the whole feature off. showAt: centred on finger, top edge 26 dp below it (SHOW_BELOW_DP), clamped to page area, no slide, geometry saved.
- Decision: Panel floats (MiniNav PanelLayout): long press PICK_MS 400 on buttons/bar/frame (never on keys: keys keep digit-hold + glide) picks up, sends touch cancel to the child, haptic, lit outline, finger carries it, no snap-dock any more; grip keeps immediate drag. Both timer and event timestamps count (busy UI thread). coversWindowPoint uses panel zoneAt != none (transparent band belongs to the page). WHY: owner asked to move it by long pressing and dragging.
- Decision: Space key slide: 10 dp to start, 11 dp per character; address bar uses core MiniKeyboardModel cursor/moveCursor (bar draws caret), page field gets DPAD_LEFT/RIGHT via PageField.moveCursor (PageFieldInput Field.moveCursor, max 64 steps). WHY: other keyboard apps do it; typing/backspace/glide now work at the cursor.
- Decision: Neon glow fix: swallowed touches (HotCorner swallow + new pressSwallow) never reached the UP handler so Neon.touching stayed true (glows frozen); BrowserActivity.dispatchTouchEvent now releases glows when a swallowed touch ends. WHY: pre-existing bug since the corner long press, far more visible with open-space long press.
- Decision: New open-space web tests live in web-tests/openspace.test.js (own file with own harness). WHY: content.test.js is already near the 60 s per-file node test timeout.

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
