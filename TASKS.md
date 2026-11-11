# TASKS - the job list. Tick `[x]` only when written, tested, committed (name the test / the run that proves it).

New request from the owner => write it here in their words as `[ ]` boxes and `ckpt` BEFORE any code. Review findings of 2026-11-11
(S1-S13, C1-C9) live in `docs/handoff/REVIEW-2026-11-11.md` (all done except S12 = WONTFIX, public dev signing key).

Owner's request, 2026-11-11 (verbatim): "From now on, use github actions to build and release apks and release them on github.
Use the checkpoint and save system from my other repos"

## Build + save infrastructure (2026-11-11)
- [x] Study the owner's save system (Launcher, Portfolio): ladder = capture_inbox / autosave / ckpt / ship / Actions - `tools/test_handoff.sh`
- [x] Port it: `tools/{ckpt,autosave,push,unpushed,resume,install-hooks,posttool,capture_inbox,capture_outbox,secretscan,toobig,record-release,verify-apk,appinfo}.sh`, `ship.sh`, CHECKPOINT/TASKS/INBOX/OUTBOX/HANDOFF/BRIEF/BUILDLOG - `tools/test_handoff.sh`
- [x] `android.yml` builds, signs and releases BOTH APKs on GitHub (no push trigger; tags are created by GitHub) - run 42951742468: check, test-jvm, test-js, build-web, build-gecko all green in 4m42s; Releases `kestrel-v0.1.1` and `kestrel-g-v0.2.0-alpha2` carry their APK + sha256
- [x] First GitHub-built releases published and recorded in BUILDLOG.md (certificate = the pin, so they install over the delivered ones)
- [ ] OWNER: install `kestrel-g-0.2.0-alpha2.apk` over alpha1 and say whether the update installs in place (proves the certificate path on the real tablet)
- [x] Old checkpoint files removed (`scripts/{lib,checkpoint}.sh`, `docs/handoff/{STATE,PROTOCOL}.md`); nothing references them
- [ ] Delete the two compat shims `scripts/autosave.sh` and `scripts/resume.sh` (kept so a session that started before the move, whose hooks still point at them, keeps saving)
- [ ] Owner decision (optional): create `main` and make it the default branch + `tools/MIRROR_MAIN` = on, so a fresh session always starts from the newest work (today the default branch IS the first session branch; `resume.sh` names any branch with newer commits)
- [ ] Owner decision (optional): a PRIVATE release key in the repo secrets (`SIGNING_KEYSTORE_BASE64`, `SIGNING_STORE_PASSWORD`, `SIGNING_KEY_ALIAS`, `SIGNING_KEY_PASSWORD`) instead of the public dev key in `signing/dev/`; the workflow supports it, then `signing/CERT_SHA256` must be re-pinned (v3 signing allows the rotation without uninstalling)
- [ ] Gecko update routine: bump `geckoVersion` in `geckoapp/build.gradle.kts` about monthly (browser-engine security fixes), ship a new alpha through Actions
- [ ] Optional: `ci.yml` (unit tests + lint on pull requests) once PRs are used

## Kestrel G (Firefox engine) - waits on real-tablet feedback
- [ ] OWNER: install `kestrel-g` from the Releases page; report claude.ai, Google search, YouTube, speed and memory on the 1.9 GB tablet; compare with Cromite (arm32 APK) on the same sites
- [ ] Generic cosmetic hiding: class/id token index (content script reports ids/classes, the bridge returns matching selectors from all ~29k; today an alphabetical prefix of 1200 is applied, `.adsbygoogle` is missed)
- [ ] Gecko gaps: data-saver image/font blocking, cache-only "Open saved copy", print/PDF, find-in-page counter semantics, date/colour prompts, SSL "proceed anyway" (engine's own page), per-site JS off
- [ ] Gecko tests: unit tests for pure logic (`ShieldsBridge.requestType`, message parsing); the workflow already audits the Gecko APK with `tools/verify-apk.sh` (package, version, certificate, no GMS, size)
- [ ] README / ARCHITECTURE: a Kestrel G section (two builds, shared tree `app/src/main`, WebView code in `app/src/webview`, Gecko code in `geckoapp/`)
- [ ] Settings toggle for encrypted DNS (Gecko build uses Cloudflare DoH, TRR first)

## Low priority
- [ ] `BrowserDb` is a new `SQLiteOpenHelper` per Activity and never closed (CloseGuard warnings in tests): make it a process singleton
- [ ] "Reset filter lists" action in Settings
- [ ] Backlog (value order): omnibox network suggestions (off until built); baseline profile for cold start; per-site permission memory; blob: downloads; scriptlets/redirect resources for YouTube-style ads (see D2)

## Owner request 2, 2026-11-11 03:24Z (verbatim in INBOX.md) - THE BIG ONE
Owner: "The new version of the browser loads Claude.ai." (Kestrel G on the real tablet: the first real-device success.)
Standing rules in that message: frequently checkpoint and save ALL progress to GitHub; optimize the project for MULTIPLE Claude sessions and ACCOUNTS; a resumed session has NO context and must continue from where the last one was
interrupted by usage or finished its last task; no progress/data loss, no wasted Claude usage; "Always keep my prompts and respond to each one even if I interrupt. Don't ignore any prompt."; "Send me the prompt again if I pressed
deny on accident" (a denied tool call is re-sent ONCE, say so in one line); the Brave problem - "it freezes a lot especially when I'm typing and I have to switch apps and go back to brave for it to refresh. I don't want this
problem with kestrel".
Work order: M (process) and R (research, in parallel) -> A (speed, freeze, security quick wins) -> B (downloads) -> C (UI) -> D (blocking engine v2). Every step: ckpt. Every release: ship.sh + Actions + record-release.
- [ ] M1 Many sessions/accounts: a session start merges newer sibling-branch work automatically (tools/sync.sh), every session keeps its OWN checkpoint card (.checkpoints/<branch>.md) so two sessions never overwrite each other's "Do this next", append-only files merge without conflicts, items can be claimed ([~]) - proven by tools/test_handoff.sh with two simulated sessions
- [ ] M2 Releases cannot collide: one build/publish per app at a time across all branches (job-level concurrency in android.yml)
- [ ] M3 Cold-start drill: a fresh clone with no context runs resume.sh and is told the exact next step (checked in test_handoff.sh)
- [ ] M4 Standing rules above written into CLAUDE.md; research findings go into research/*.md at once (a cut-off session must not lose them)
- [ ] R1 UI benchmark: Brave (tablet) + Chrome/Firefox/Samsung/Edge/Opera/Vivaldi/Focus - what users expect, touch sizes -> research/UI_BENCHMARK.md
- [ ] R2 Download-manager techniques (segments, pause/resume, scoped storage, services, Gecko network stack) -> research/DOWNLOADS.md
- [ ] R3 Gecko speed for a 32-bit Android 10 Go 2 GB tablet (prefs, GPU, video decode, memory, startup, network) -> research/GECKO_SPEED.md
- [ ] R4 Newest security standards/tools (browser, Android app, CI supply chain) -> research/SECURITY.md
- [ ] R5 Blocking-engine state of the art + our design (uBlock Origin, Brave adblock-rust, AdGuard, lists, JS-in-extension, DNR, scriptlets) -> research/ADBLOCK_ENGINE.md
- [ ] R6 The typing freeze (Brave/Chromium/Gecko on Android): causes, prevention, recovery -> research/FREEZE.md
- [x] A1 "Have no animations. Everything should feel snappy and instant": native UI animations off (instant pressed state instead of ripples, no popup/dialog/window animation) + page side: ui.prefersReducedMotion=1, GIFs once, no smooth scroll (assets/gecko-prefs.txt) - alpha3; the redesign (C) must keep it
- [~] A2 Firefox engine tuned for the tablet - DONE in alpha3: YAML config (gecko-prefs.txt, 18 prefs verified to exist: AV1 off, 1 content process, 96 MB image cap, 512 MB disk cache, page-cache cap, media read-ahead, no prefetch), runtime after first frame + warmUp, fetch priority; GPU/WebRender/MediaCodec are already the Android defaults (R3: nothing to switch on). OPEN: preconnect with GeckoWebExecutor.speculativeConnect on URL focus/commit; graphics safe-mode switch; measure on the tablet (Diagnostics)
- [~] A3 No freezes - DONE in alpha3: ad-block decisions on a worker thread (stop-gap until D1), cosmetic tables pre-built off the UI thread (the emulator log caught a 2 s UI stall there), ONE GeckoView attached for ever (tabs swap with setSession, NTP/error = overlays), toolbar updates coalesced + diffed, FLAG_SECURE only on change, autofill off, address-bar typing no longer resizes the page view (adjustNothing until the keyboard is gone), watchdog + frame-jank + timeline ring + Diagnostics + manual Refresh screen (R0+R1+R2). OPEN (research/FREEZE.md P1): echo probes, automatic recovery ladder (log-only first), TextureView/extensions-process A/B switches, suggestion debounce, NetworkMonitor.start off the UI thread (stalled 2 s at start on the emulator), stress page web-tests/freeze-stress.html
- [~] A4 Security per research/SECURITY.md - DONE in alpha3: Gecko pins (FPP everywhere, LNA, CT, PQ, enterprise roots off, no push/notifications, WebRTC LAN leak off), bridge sender check, extension CSP + host list + private tabs allowed. OPEN (priority order): B1 private release key (needs the owner: GitHub secrets), B2-B5 CI hardening (per-job permissions, build/sign split + environment, SHA-pinned actions, Gradle dependency verification), B6 gecko-watch + engine-age line, B10 intent action allow-list, B11 provenance, owner actions (Private DNS dns.quad9.net, GitHub 2FA, screen lock)
- [~] A5 Diagnostics screen (menu > Diagnostics) - DONE in alpha3: device/memory/IME/processes, engine version, engine prefs read back (proves the config was applied), counters, 150-event timeline, Copy/Share. OPEN: WebGL renderer + codec matrix page, per-process memory (PSS), startup timeline
- [ ] B1 Download manager: parallel segmented downloads, proper pause/resume (survives app switch and process death), queue, speed + time left, retries, notifications, saved straight into Downloads (MediaStore), integrity checks; pure-Kotlin engine with unit tests; both builds
- [ ] B2 Downloads screen with large buttons: progress, pause/resume/cancel/open/share/delete, clear finished
- [ ] C1 Design system: a FRESH identity (not generic Material/AI look), light + dark palette, shape language, system fonts only, hand-made vector icons, buttons 52-56dp, no animations -> docs/DESIGN.md
- [ ] C2 Toolbar, address bar, tab strip + tab switcher for the 564x916dp tablet (portrait and landscape)
- [ ] C3 Menu, Shields panel, new-tab page, Settings, dialogs, error pages in the new style
- [ ] C4 Claude looks at emulator screenshots (600x973 @170dpi) of every screen before each release; the owner's feedback is collected in TASKS
- [ ] D1 Blocking engine inside the extension (no round trip to Kotlin per request), same lists + per-site switches, counters batched
- [ ] D2 Cosmetic filtering for all ~29k generic selectors (class/id token index), procedural :has()/:has-text(), common scriptlets (YouTube-style ads), redirect neutering
- [ ] D3 "Anything unnecessary that slows down page loading": heavy third-party widgets/trackers, autoplay/preload policy, annoyance lists - measured by a requests/bytes/time benchmark on real pages (desktop Firefox in CI) before vs after
- [ ] D4 Differential tests: the new engine gives the same verdicts as the Kotlin engine on the 60k-case corpus + real-list samples
