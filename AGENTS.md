# AGENTS.md

<!-- handoff:learnings:begin -->
## Session Learnings (auto-updated by handoff)

### Implicit Rules
- Repo is PUBLIC: no secrets, owner photos, APKs; ASCII text only; private INBOX/OUTBOX live in tjshea90/Kestrel-Private
- Develop and push ONLY on branch ccr-587430ac-royssd; never force-push; no PRs unless asked; never add a push: trigger to workflows
- Cold resume: bash tools/resume.sh then CHECKPOINT.md 'Do this next' is authoritative; write owner requests into TASKS.md in his words before code; run bash tools/ckpt.sh after each step
- Release: bump geckoapp/build.gradle.kts versionCode/versionName, bash tools/sync-ext-version.sh, bash ship.sh gecko with note (ASCII, no double quotes/backslashes, kept in docs/handoff/ALPHA<N>_RELEASE_NOTE.txt), ONE android.yml dispatch app=gecko full_build=true, verify tag via git ls-remote, list_workflow_jobs, get_release_by_tag, then tools/record-release.sh with identical note
- Lab: android.yml app=lab lab_suite=<tb|new|new2|lr|vs|ql|...> lab_profile=standard lab_net=full lab_minutes=5; ONE lab at a time per branch (concurrency cancels); ffmpeg step takes ~12 min; poll job with get_workflow_job; fetch text artifact with python3 -B -I tools/emu/fetch_artifact.py <url> <empty dir>
- Never run two gradle commands at once; foreground sleep > 55 s is blocked, use run_in_background
- Stack: :app kotlin-stdlib only, :geckoapp adds GeckoView 160 armeabi-v7a, :core pure Kotlin JVM, no native code, injected JS conservative ES5, minSdk 29
- Answer every owner message in plain words, say what was NOT proven on the tablet, re-list open items; never use Workflow without owner opt-in

### Key Decisions
- Decision: capture only film-evidence rows (read stream >= 2 min, file >= 40 MB, owner interest) — Reason: clips/previews/ads must stay hidden; kept row becomes orphan listed last until live player's captured film replaces it
- Decision: Last-run note written from dedicated daemon thread, not main looper — Reason: hung UI thread silenced the old note so crash vs freeze was indistinguishable
- Decision: Copy button lives in ui/BottomDock via ui/CopyChip — Reason: ui/Toolbar is unused by both builds
- Decision: tab tray lists tabs of same kind (regular/private) as tab in front; row tap switches and closes tray, x closes tab and keeps tray, New tab opens tab + panel keyboard — Reason: mirrors tab grid split, fast one-hand use
- Decision: curtain (pal.windowBg) over the single shared GeckoView on session swap to unpainted session or other tab kind, down at first contentful paint, 3 s cap — Reason: GeckoView keeps previous session last frame; privacy leak of regular tab into private tab
- Decision: release recipe = ship.sh gecko with note, ONE dispatch, verify tag + all jobs + release, then tools/record-release.sh identical note — Reason: workflow minutes scarce, never ship a tag twice, never add push trigger

<!-- handoff:learnings:end -->
