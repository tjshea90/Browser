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
- `core/` (127 JVM tests), `app/` (81 Robolectric tests), `web-tests/` (16 Playwright tests of the injected WebView scripts), lint: `scripts/verify.sh quick|full` (WebView build). The workflow repeats them on GitHub.
- The Gecko module has no unit tests yet (TASKS.md); its APK is audited by `tools/verify-apk.sh gecko`.
- Emulator runs (software mode, no KVM, minutes per step): recipe in the "Emulator" section of `docs/ARCHITECTURE.md` / the old notes in git history of `docs/handoff/STATE.md`
  (`sdkmanager --install emulator "system-images;android-29;default;x86_64"`, avd `kestrel29x64` pixel_c 600x973@170, `emulator -no-window -no-audio -no-snapshot -no-boot-anim -gpu swiftshader_indirect -no-accel`,
  boot 3-5 min, `adb install -r -t`, test pages via `python3 -m http.server 8000` -> `http://10.0.2.2:8000/`). The CI cannot run it either; say honestly what was not run on the real tablet.

## 4. Architecture map
`core/` pure Kotlin (filter engine, URL/omnibox, HTTPS policy, session codec) - `app/` Android UI + data + shield (+ `src/webview` engine code) - `geckoapp/` Gecko engine code + the
built-in "Kestrel Shields" WebExtension - `tools/` also holds the Gradle JVM CLI that compiles filter lists (`.kfs`) next to the shell tooling - `web-tests/` - `docs/`. Details: `docs/ARCHITECTURE.md`.

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
