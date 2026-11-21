# CHECKPOINT 2826 — read me first, then TASKS.md

**Written:** 2026-11-21T18:11:17Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/peaceful-darwin-4sbmdz` · **builds on:** `495ef4db` (this checkpoint is the commit after it)

## Just done
gated kestrel-g-v0.3.0-alpha85 (gecko code 186) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref claude/peaceful-darwin-4sbmdz, inputs {"app":"gecko","full_build":"true","note":"alpha85: the live diagnostics now explain WHY a page is slow, which you asked for after Claude.ai and YouTube felt slow even on fast Wi-Fi. What the tablet's own numbers already said: a Claude.ai load took 28 to 35 seconds, in one earlier visit about nine in ten of its requests (over a thousand) came from the cache, so the link is probably not what is slow; the ad blocker costs about 1.6 milliseconds per request, which would be roughly 6 percent of a thousand-request load, so it is probably not the main cause either. What was missing was where the rest goes, so each page load now sends one small scrubbed line from the page itself: time to the first byte, first and largest paint, how many requests and how many were slow, which hosts were slowest (names only, never an address path), and how long the page was too busy to answer, which tells a page busy with its own scripts from one that is waiting for the network. Also sent now as numbers instead of cut-off text: the blocker's cost, processor use of the browser and of the page engine, memory of every process, start-up times and slow-frame counts. The suggestions in Settings, Live diagnostics, now say when a slow site is busy with its own scripts (a faster link will not help) or when the blocker is a large part of its load. Claude reads all of it with one command and sorts every slow site into busy, waiting or blocker-heavy. Nothing about the engine settings was changed because the cause is not proven yet; after a day of normal browsing the next files will name it. Same privacy as before: only your private repository, only your token, never a private tab, sensitive sites never named, and the master switch off means nothing is recorded, kept or sent. NOT run on the tablet: the new timing has only been tested in a desktop browser."}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag kestrel-g-v0.3.0-alpha85), then:  bash tools/record-release.sh kestrel-g-v0.3.0-alpha85 "alpha85: the live diagnostics now explain WHY a page is slow, which you asked for after Claude.ai and YouTube felt slow even on fast Wi-Fi. What the tablet's own numbers already said: a Claude.ai load took 28 to 35 seconds, in one earlier visit about nine in ten of its requests (over a thousand) came from the cache, so the link is probably not what is slow; the ad blocker costs about 1.6 milliseconds per request, which would be roughly 6 percent of a thousand-request load, so it is probably not the main cause either. What was missing was where the rest goes, so each page load now sends one small scrubbed line from the page itself: time to the first byte, first and largest paint, how many requests and how many were slow, which hosts were slowest (names only, never an address path), and how long the page was too busy to answer, which tells a page busy with its own scripts from one that is waiting for the network. Also sent now as numbers instead of cut-off text: the blocker's cost, processor use of the browser and of the page engine, memory of every process, start-up times and slow-frame counts. The suggestions in Settings, Live diagnostics, now say when a slow site is busy with its own scripts (a faster link will not help) or when the blocker is a large part of its load. Claude reads all of it with one command and sorts every slow site into busy, waiting or blocker-heavy. Nothing about the engine settings was changed because the cause is not proven yet; after a day of normal browsing the next files will name it. Same privacy as before: only your private repository, only your token, never a private tab, sensitive sites never named, and the master switch off means nothing is recorded, kept or sent. NOT run on the tablet: the new timing has only been tested in a desktop browser."
NEVER ship kestrel-g-v0.3.0-alpha85 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  495ef4db ckpt 2825: pre-ship: alpha85: the live diagnostics now explain WHY a page is slow, whic
  4cf115af ckpt 2824: LD4 gate so far: core 1991 tests (1 failed = FLK1 PhoneBackendTest flake, pa
  a1863e08 ckpt 2823: LD4c done: diag_digest.py LOAD ANALYSIS (per site: loads, load/first paint/f
  0c525795 ckpt 2822: LD4b app side done and compiling: GeckoEngine.networkProfileCode, ShieldsBri
  ca1f0fe2 ckpt 2821: LD4b extension side done: pagetime.js (ES5, top frame, document_start, laten
  029be5ca ckpt 2820: LD4b core done: DiagScrub caps (event 480, snapshot lines 1600: the CPU/Bloc
  0b7be92b ckpt 2819: LD4a done: research/SLOW_LOADS.md written from the real uploads: claude.ai 1
  a6b43548 ckpt 2818: LD4 written into TASKS.md in the owner's words (richer live diagnostics + cl
  db4c9602 ckpt 2817: alpha84 RELEASED AND RECORDED: https://github.com/tjshea90/Browser/releases/
  bcfe8a73 ckpt 2816: alpha84 build running: run id 44924979560 (run_number 265, head 2cf128f4), d
```
