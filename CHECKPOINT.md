# CHECKPOINT 3589 — read me first, then TASKS.md

**Written:** 2026-11-28T08:46:50Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/elegant-darwin-h7dlay` · **builds on:** `aae77d3c` (this checkpoint is the commit after it)

## Just done
alpha115 (gecko code 216) gated green by ship.sh and DISPATCHED: android.yml run 46127362671 (run_number 317, head aae77d3c, dispatched 08:46Z). Not built yet. PDL1 code is in that commit (HandedTest 17/17, 436 core tests in the download/courier/stream/rules slices, :geckoapp + :app compile green).

## Do this next
WAIT for run 46127362671 (mcp__github__actions_list list_workflow_jobs resource_id 46127362671; get_job_logs answers 404 until the job ends: do not poll it). When ALL jobs are success: mcp__github__get_release_by_tag tjshea90/Browser kestrel-g-v0.3.0-alpha115 shows the APK, then bash tools/record-release.sh kestrel-g-v0.3.0-alpha115 with the IDENTICAL note (file docs/handoff/ALPHA115_RELEASE_NOTE.txt, no trailing newline), then tick PDL1a-d in TASKS.md with evidence (HandedTest, run 46127362671), then answer the owner in plain words: cause read from code (Proton Drive service worker stream, not reproduced), what alpha115 does, what was NOT run (no Proton account, tablet untested, WebView build unchanged), the Release link on its own line, ND4a note (alpha115 is the first update on top of alpha114: tell me whether the app stayed open after the install), re-list open items. While the run builds (do NOT edit before the run has checked out: it has, pinned to aae77d3c): ND6a = claim ND6 (bash tools/claim.sh ND6); item A: Engine.Listener.onFailed(DownloadView) default no-op called once from tryFinishStop on FAILED, DownloadController counts download.failed.<reason> + Health.log download failed (skip priv), test in HandedTest; item B: ND3g only if cheap; digest findings so far are in the research note to write (research/ND6_DIAGNOSTICS_2026-11-28.md): cold start median 18.9 s WATCH, parent CPU 41 %, 15 content-process kills (12 while away), claude.ai CPU-bound (known, engine knobs need tablet A/B), FILE PICKS all fine (claude.ai refused the PNGs), no download lines at all for the Proton failure (why item A).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  aae77d3c ckpt 3588: gated kestrel-g-v0.3.0-alpha115 (gecko code 216) and pushed it: handoff self
  165f5255 ckpt 3586: pre-ship: alpha115: downloads that a page makes itself, like Proton Drive, n
  f47e6119 ckpt 3582: PDL1 CODED (not yet released): core Handed.kt (HandedStreams registry, Hande
  3261ff02 ckpt 3569: Second owner message (ND6: after PDL1, look for improvements from sent diagn
  59778a61 ckpt 3567: Owner message (Proton Drive zip download fails in Kestrel with 'The site sen
  a50908c2 ckpt 3539: alpha114 SHIPPED and recorded (Release kestrel-g-v0.3.0-alpha114, run 460786
  6ee3c3de ckpt 3537: ND5c: research/ND5_CAPTCHA_LOGIN.md has the Shields-off analysis and the bis
  fca1cfcc ckpt 3536: owner: 'Shields off fixed it' (my.nobullnetworks.com login): ND5c = find whi
  2c6e069e ckpt 3535: ND5 read: research/ND5_CAPTCHA_LOGIN.md written (reCAPTCHA v2 invisible, key
  9acfa854 ckpt 3534: alpha114 (code 215) gated green by ship.sh and DISPATCHED (android.yml gecko
```
