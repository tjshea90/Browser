# CHECKPOINT 3588 — read me first, then TASKS.md

**Written:** 2026-11-28T08:45:59Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/elegant-darwin-h7dlay` · **builds on:** `03d7737b` (this checkpoint is the commit after it)

## Just done
gated kestrel-g-v0.3.0-alpha115 (gecko code 216) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref claude/elegant-darwin-h7dlay, inputs {"app":"gecko","full_build":"true","note":"alpha115: downloads that a page makes itself, like Proton Drive, now save. WHAT YOU SAW: a zip from Proton Drive failed in Kestrel G with The site sent a web page instead of the file, and the same file downloaded fine in Brave. WHY: Proton Drive does not keep your file on a web address. Its page decrypts the file inside the browser and hands it out of a small background program (a service worker) that exists only inside the engine. Kestrel's download manager used to throw away what the engine offered and ask the website for the same address again, and the website answers that with its start page, not your file. Brave saves what its engine hands over, so it worked there. WHAT CHANGED: Kestrel now keeps the engine's own stream for up to three minutes. When the address cannot be asked for again (a Proton Drive stream, a blob address), or the website's answer is an error or a web page, the file is saved straight from the engine's stream. Ordinary links work as before, with several connections and pause and resume; the engine's copy is simply closed unused. A file a page makes itself cannot be paused or resumed: its row offers Cancel only while it runs, it starts at once ahead of other waiting downloads, it is never sent to the phone, it ignores the speed limit, and its tab is not put to sleep while it runs. If it is cut (the page was closed, the app stopped) the row says the page made this file itself and to go back to the page and download it again, and the half file is removed. Downloads from blob: addresses now work too (they used to fail with This address cannot be downloaded). New counters download.handed.offered, ordinary, used and gone show in the next diagnostics. NOT RUN: there is no Proton Drive account and no Firefox engine in the build sandbox, so the proof here is 17 new tests of the download engine (with a stand-in for the service worker stream, a cut stream, a blob file, an ordinary link and a one-time link) plus the full unit test suites. Please try the same zip again in Kestrel G on the tablet. The Kestrel WebView build is not changed and cannot save such files."}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag kestrel-g-v0.3.0-alpha115), then:  bash tools/record-release.sh kestrel-g-v0.3.0-alpha115 "alpha115: downloads that a page makes itself, like Proton Drive, now save. WHAT YOU SAW: a zip from Proton Drive failed in Kestrel G with The site sent a web page instead of the file, and the same file downloaded fine in Brave. WHY: Proton Drive does not keep your file on a web address. Its page decrypts the file inside the browser and hands it out of a small background program (a service worker) that exists only inside the engine. Kestrel's download manager used to throw away what the engine offered and ask the website for the same address again, and the website answers that with its start page, not your file. Brave saves what its engine hands over, so it worked there. WHAT CHANGED: Kestrel now keeps the engine's own stream for up to three minutes. When the address cannot be asked for again (a Proton Drive stream, a blob address), or the website's answer is an error or a web page, the file is saved straight from the engine's stream. Ordinary links work as before, with several connections and pause and resume; the engine's copy is simply closed unused. A file a page makes itself cannot be paused or resumed: its row offers Cancel only while it runs, it starts at once ahead of other waiting downloads, it is never sent to the phone, it ignores the speed limit, and its tab is not put to sleep while it runs. If it is cut (the page was closed, the app stopped) the row says the page made this file itself and to go back to the page and download it again, and the half file is removed. Downloads from blob: addresses now work too (they used to fail with This address cannot be downloaded). New counters download.handed.offered, ordinary, used and gone show in the next diagnostics. NOT RUN: there is no Proton Drive account and no Firefox engine in the build sandbox, so the proof here is 17 new tests of the download engine (with a stand-in for the service worker stream, a cut stream, a blob file, an ordinary link and a one-time link) plus the full unit test suites. Please try the same zip again in Kestrel G on the tablet. The Kestrel WebView build is not changed and cannot save such files."
NEVER ship kestrel-g-v0.3.0-alpha115 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  165f5255 ckpt 3586: pre-ship: alpha115: downloads that a page makes itself, like Proton Drive, n
  f47e6119 ckpt 3582: PDL1 CODED (not yet released): core Handed.kt (HandedStreams registry, Hande
  3261ff02 ckpt 3569: Second owner message (ND6: after PDL1, look for improvements from sent diagn
  59778a61 ckpt 3567: Owner message (Proton Drive zip download fails in Kestrel with 'The site sen
  a50908c2 ckpt 3539: alpha114 SHIPPED and recorded (Release kestrel-g-v0.3.0-alpha114, run 460786
  6ee3c3de ckpt 3537: ND5c: research/ND5_CAPTCHA_LOGIN.md has the Shields-off analysis and the bis
  fca1cfcc ckpt 3536: owner: 'Shields off fixed it' (my.nobullnetworks.com login): ND5c = find whi
  2c6e069e ckpt 3535: ND5 read: research/ND5_CAPTCHA_LOGIN.md written (reCAPTCHA v2 invisible, key
  9acfa854 ckpt 3534: alpha114 (code 215) gated green by ship.sh and DISPATCHED (android.yml gecko
  62e73b2d ckpt 3533: gated kestrel-g-v0.3.0-alpha114 (gecko code 215) and pushed it: handoff self
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
