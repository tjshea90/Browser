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
- [ ] Backlog (value order): omnibox network suggestions (off until built); baseline profile for cold start; per-site permission memory; blob: downloads; scriptlets/redirect resources for YouTube-style ads
