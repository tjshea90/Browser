# CHECKPOINT 3594 — read me first, then TASKS.md

**Written:** 2026-11-28T08:49:48Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/elegant-darwin-h7dlay` · **builds on:** `47ea8cea` (this checkpoint is the commit after it)

## Just done
ND6a-A DONE in the tree (not released): Engine.Listener.onFailed(DownloadView) called once from tryFinishStop on FAILED, DownloadController.noteFailed (skips private) counts download.failed.<reason> and writes one 'download failed' Health line; HandedTest 18/18 + app compile green; research/ND6_DIAGNOSTICS_2026-11-28.md written (digest read: nothing new beyond the known WATCH items; the Proton failure left NO line in the uploads, which is why A); ND6 claimed; alpha115 run 46127362671 still building (check, then test-js and test-jvm in progress at 08:50Z).

## Do this next
WAIT for run 46127362671 to finish (mcp__github__actions_list list_workflow_jobs resource_id 46127362671; all jobs success), then get_release_by_tag kestrel-g-v0.3.0-alpha115, then bash tools/record-release.sh kestrel-g-v0.3.0-alpha115 with the identical note (docs/handoff/ALPHA115_RELEASE_NOTE.txt), tick PDL1a-d (HandedTest + run id), answer the owner in plain words incl. the Release link on its own line, ND4a request (tell me whether the app stayed open after the alpha115 install), what was NOT run, and re-list open items. The ND6a-A observability is committed but NOT in alpha115 (run is pinned to aae77d3c): ship it only with the next proven change; do not cut a release for it alone (every update costs the owner a first-start-after-install).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  439bcc1b ckpt 3589: alpha115 (gecko code 216) gated green by ship.sh and DISPATCHED: android.yml
  aae77d3c ckpt 3588: gated kestrel-g-v0.3.0-alpha115 (gecko code 216) and pushed it: handoff self
  165f5255 ckpt 3586: pre-ship: alpha115: downloads that a page makes itself, like Proton Drive, n
  f47e6119 ckpt 3582: PDL1 CODED (not yet released): core Handed.kt (HandedStreams registry, Hande
  3261ff02 ckpt 3569: Second owner message (ND6: after PDL1, look for improvements from sent diagn
  59778a61 ckpt 3567: Owner message (Proton Drive zip download fails in Kestrel with 'The site sen
  a50908c2 ckpt 3539: alpha114 SHIPPED and recorded (Release kestrel-g-v0.3.0-alpha114, run 460786
  6ee3c3de ckpt 3537: ND5c: research/ND5_CAPTCHA_LOGIN.md has the Shields-off analysis and the bis
  fca1cfcc ckpt 3536: owner: 'Shields off fixed it' (my.nobullnetworks.com login): ND5c = find whi
  2c6e069e ckpt 3535: ND5 read: research/ND5_CAPTCHA_LOGIN.md written (reCAPTCHA v2 invisible, key
```

(4 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
