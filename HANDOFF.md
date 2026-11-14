# HANDOFF - read this first if you are a Claude session with NO prior context

The single entry point for a new Claude session or account. After this, in order: `CLAUDE.md` (working agreement, auto-loaded) -> `CHECKPOINT.md`
(where the last session stopped, "Do this next") -> the open `[ ]` boxes of `TASKS.md` -> `BRIEF.md` (decisions that do not change).

## 0. In one minute
- **What:** Kestrel, a very light, fast, private browser for ONE tablet (MT8168B, Android 10 Go, 2 GB RAM, no Google services, internet through a phone hotspot).
  Brave-like Shields (ads/trackers, HTTPS upgrade, cookie/fingerprint protection) and nothing else: no news, AI, rewards, wallet, VPN, sync, telemetry.
- **Two builds:** `Kestrel` (`:app`, system WebView = Chromium 74, 2.3 MB, cannot open modern sites) and `Kestrel G` (`:geckoapp`, Firefox engine, 92 MB, the one to test).
- **Who the owner is:** runs everything from a tablet/phone and a Claude chat, sends screenshots from the tablet, wants plain-language answers and a clear
  "what I did / what you must do". Prefers longer sessions and more usage if the product gets better, but wants usage spent efficiently.
- **State:** `CHECKPOINT.md` (+ `BUILDLOG.md`: the last line per app is the last real release). APKs are built and signed by GitHub Actions; this container never builds a release.
- **Repo:** the GitHub remote `origin` (public). The default branch is the first session branch; later sessions get their own `ccr-*` branch (see section 6).

## 1. First 10 minutes
1. The SessionStart hook printed the briefing (`tools/resume.sh`). Missing? `bash bootstrap.sh`, and run `bash tools/install-hooks.sh`. Verify over 2-3 edits that `git log --oneline -3` shows `auto-checkpoint:` commits.
2. Read `CHECKPOINT.md` -> do "Do this next". Do not re-plan finished work.
3. New message from the owner? Write it into `TASKS.md` in their words as `[ ]` boxes, `bash tools/ckpt.sh "did" "next"`, THEN code. Answer every message.
4. Fast local checks: `./gradlew :core:test :app:testDebugUnitTest` (JVM, no emulator), `bash tools/test_handoff.sh` (the save/release tooling), `cd web-tests && npm test` (Chromium JS tests).

## 2. Release routine (GitHub Actions builds, Claude never builds a release here)
0. Engine freshness, about monthly: read `<latest>` in https://maven.mozilla.org/maven2/org/mozilla/geckoview/geckoview-armeabi-v7a/maven-metadata.xml (the in-app daily check says the same under Menu > Updates). If it is newer than `geckoVersion` in `geckoapp/build.gradle.kts`: bump it, run `bash tools/check-gecko-prefs.sh` (every pref name in `gecko-prefs.txt` must still exist) and `./gradlew :geckoapp:compileDebugKotlin`, and ship an alpha. Checked 2026-11-12: `160.0.20261104181552` (ours) is the newest. After a bump also check the three GeckoView INTERNALS the app calls still compile and still work: `GeckoEngine.speculativeConnect` (org.mozilla.gecko.util.XPCOMEventTarget.mainThread + org.mozilla.gecko.GeckoThread.speculativeConnect; proof: set the debug property `debug.kestrel.moz_log` to `nsHttp:5` on an emulator debug build, type a bookmarked public host in the address bar and look for `SpeculativeConnect` lines of that host in files/gecko-moz.log.moz_log), the pref names of gecko-prefs.txt (tools/check-gecko-prefs.sh) and the lab pref defaults pinned in GeckoEngine.applyLab (network.buffer.cache.size 16384, gfx.downloadable_fonts.fallback_delay 3000).
1. Bump `versionCode` (strictly higher than the last line for that app in `BUILDLOG.md`) and `versionName` in `app/build.gradle.kts` or `geckoapp/build.gradle.kts`; `bash tools/ckpt.sh`.
2. `bash ship.sh <web|gecko> "what changed"`: runs the cheap local gates (handoff self-test, versionCode, unit suites), pushes, and writes the dispatch recipe into CHECKPOINT.md.
3. Dispatch `.github/workflows/android.yml`: `mcp__github__actions_run_trigger` method `run_workflow`, `workflow_id` android.yml, `ref` = the current branch, `inputs` `{"app":"gecko","full_build":"true","note":"..."}`
   (`app` = web | gecko | both; `full_build` false = gates only, about a minute). The run tests, builds, signs, checks the certificate against `signing/CERT_SHA256`, audits the APK
   (`tools/verify-apk.sh`), and creates the GitHub Release `kestrel-v<ver>` / `kestrel-g-v<ver>` with the APK + sha256 attached (the TAG is created by GitHub: a tag push from here is refused with 403).
4. A full run of BOTH apps takes about 5 minutes (first run, id 42951742468: check 16 s, test-js 1 min, test-jvm 2 min, build-web 1.5 min, build-gecko 2.5 min). Wait without spending tokens: a Bash `run_in_background` loop `until [ "$(date -u +%H%M%S)" -ge HHMMSS ]; do sleep 20; done` (a foreground `sleep` is blocked), then `actions_list list_workflow_runs`
   (`resource_id` android.yml, `workflow_runs_filter {"event":"workflow_dispatch"}`, `perPage` 1) -> `list_workflow_jobs` -> `get_job_logs` (404 until the job ENDS; `tail_lines` about 60).
5. Green + the Release shows its APK (`get_release_by_tag`): `bash tools/record-release.sh <tag> "note"`. Then give the owner the Release link as plain tappable text on its own line.
6. NEVER ship a tag twice (check BUILDLOG.md and the Releases first). Gecko APK is ~92 MB and is never committed.

## 3. Test system (what runs where)
- `core/` (about 880 JVM tests), `app/` (about 260 Robolectric tests), `web-tests/` (Playwright tests of the injected WebView scripts), lint: `scripts/verify.sh quick|full` (WebView build). The workflow repeats them on GitHub. `EngineTest.anOutageInTheMiddleWaitsAndCarriesOnInsteadOfFailing` (download engine) is timing-sensitive: it failed once while Gradle compiled and linted in parallel and passed 57/57 alone - rerun it alone before suspecting a change.
- The Gecko module has no unit tests yet (TASKS.md); its APK is audited by `tools/verify-apk.sh gecko`.
- Emulator runs (software mode, no KVM, minutes per step): recipe in the "Emulator" section of `docs/ARCHITECTURE.md` / the old notes in git history of `docs/handoff/STATE.md`
  (`sdkmanager --install emulator "system-images;android-29;default;x86_64"`, avd `kestrel29x64` pixel_c 600x973@170, `emulator -no-window -no-audio -no-snapshot -no-boot-anim -gpu swiftshader_indirect -no-accel`,
  boot 3-5 min, `adb install -r -t`, test pages via `python3 -m http.server 8000` -> `http://10.0.2.2:8000/`). The CI cannot run it either; say honestly what was not run on the real tablet.

## 4. Architecture map
`core/` pure Kotlin (filter engine, URL/omnibox, HTTPS policy, session codec) - `app/` Android UI + data + shield (+ `src/webview` engine code) - `geckoapp/` Gecko engine code + the
built-in "Kestrel Shields" WebExtension - `tools/` also holds the Gradle JVM CLI that compiles filter lists (`.kfs`) next to the shell tooling - `web-tests/` - `docs/`. Details: `docs/ARCHITECTURE.md`.

Where the owner-visible features of alpha30-33 live (each has a research note with the decisions and the test list; read it before touching the code):
- Home restored as the last page: `core/session/SessionCodec.kt` (flag `H`) + gecko `BrowserActivity.restore()` / `attachContent` guard - `research/COLD_START.md` (AQ1). Last-page picture at start: `LastLook*` - same file (EY-1).
- Blocked-page icon (1 s, press opens a new tab): `app/ui/BlockedChip.kt`, `BrowserActivity.onPopupBlocked` - `research/MINI_NAV.md` (AR1).
- One-hand top-right panel + its own keyboard: `app/ui/MiniNav.kt`, `core/input/MiniKeyboard.kt`, own-clock animation (Android animators are OFF on the owner's tablet) - `research/MINI_NAV.md` (AR2).
- Storage tidy: `app/diag/StorageTidy.kt`, `core/update/ApkFiles.kt`, `UpdateManager.sweepFiles` - `research/UPDATE_STORAGE.md` (AO4). Video size estimates: `core/media/SizeEstimate.kt` - `research/VIDEO_SIZES.md` (AO2).
- "CPU by process" Diagnostics line: `app/diag/CpuMeter.kt`, `core/diag/CpuUse.kt` - `research/VIDEO_SMOOTHNESS.md` (AO1). One-press play through fake layers: `assets/kestrel-shields/guard-main.js` (AP1).
- Glide typing on the mini keyboard (AT1): decoder + word list + simulator in `core/input/glide/` (`GlideDecoder`, `Lexicon`, `GlideSupport`), the finger/trail/strip in `MiniNav.KeysView`, the word list `geckoapp/src/main/assets/glide/en.klx` (made by `:tools:glideLexicon`, licence in `docs/DICTIONARY.md`) - `research/GLIDE_TYPING.md` has the measured accuracy (SIMULATED finger: `GlideEval`, run it with the recipe in its header) and every tuning decision; the Diagnostics "Glide typing" line is the tablet's own accuracy count.
- Invisible long-press corner (AU1): `app/ui/HotCorner.kt` (watches every touch from `BrowserActivity.dispatchTouchEvent`, never consumes until its long press fires; the page's own touch is cancelled with a synthetic ACTION_CANCEL), the panel's `MiniNav.toggle()`; the old visible tab is gone - `research/MINI_NAV.md`.
- The keyboard learns from the person (AU2): `core/input/glide/PersonalModel.kt` (word + pair weights, saved in `files/glide_personal.txt`, "Forget what the keyboard learned" in Settings > Privacy), the decoder prior in `GlideDecoder.prior`, learning rules in `MiniNav` (kept +1, picked +3, replaced-after-delete +3, typed words at Go / close) - `research/GLIDE_TYPING.md` section 8 has the simulated gain; the Diagnostics "Glide typing" line ends with what it learned.
- The mini keyboard types into the page's focused text field (AU3): `geckoapp/PageFieldInput.kt` wraps the session's TextInputDelegate (keeps the system keyboard down while the mini one is up) and gives MiniNav a `PageField` (commitText / Delete / Enter on the library's own input connection, on its handler thread); field mode in `MiniNav` (`syncField`, `sendEdit` with core `TextEdit`); lab page `tools/sitelab/fieldlab.py` - `research/MINI_NAV.md` (AU3 section).
- Emulator rig for all of the above (AVD, adb root/run-as, lab servers `tools/sitelab/`): section 3 and `research/DIAG_TOOLS.md`; the uiautomator `text` attribute of a long Diagnostics text is ONE node - grep the raw dump, do not cut it. A real multi-point finger on the emulator: on-device `sendevent` loop (`getevent -p` for the touch device and its ranges), see the AT1h note in `research/GLIDE_TYPING.md`.

## 5. Decisions already made - do not redo
Engine choice (two builds), Google default search, hotspot rules, signing with the pinned public dev key, no GMS, no AndroidX in the WebView build, the review findings S1-S13/C1-C9: `BRIEF.md`, `docs/handoff/REVIEW-2026-11-11.md`.

## 6. Taking over after a cut-off session or another account
The repo is the ONLY memory: what a session pushed is safe; what it knew only in its head (not in CHECKPOINT/TASKS) is gone. Read, verify, then continue the OLD session's "Do this next".
1. `git fetch --all --prune`; `tools/resume.sh` names any branch whose commits this one lacks. The newest commit time is the other session's heartbeat: under ~20 minutes = it MAY still run
   (do not release or touch its CI runs; ask the owner), older = cut off: merge its branch (`git merge origin/<branch>`; keep both sides of CHECKPOINT/TASKS conflicts) and continue.
2. Before dispatching anything: `actions_list list_workflow_runs` for android.yml and `list_releases`, compare with BUILDLOG.md ("RELEASED BUT NEVER RECORDED" is printed by resume.sh).
3. Branches: develop and push ONLY on the branch the session was assigned; never force-push; `tools/MIRROR_MAIN` is `off` (landing on `main` is the owner's call).

## 7. What waits on the owner
See the `OWNER` lines in TASKS.md: tablet feedback on Kestrel G, the Cromite comparison, the optional main-branch and private-key decisions.
