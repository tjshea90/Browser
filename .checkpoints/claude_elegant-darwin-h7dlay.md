# CHECKPOINT 3600 — read me first, then TASKS.md

**Written:** 2026-11-28T09:04:45Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/elegant-darwin-h7dlay` · **builds on:** `3df3578e` (this checkpoint is the commit after it)

## Just done
alpha115 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha115 (gecko code 216, run 46127362671 green on the first attempt incl. CI full core+app tests, lint, js tests, APK verification; APK + sha256 + dm in the Release; BUILDLOG line pushed). PDL1b/c/d ticked, PDL1a stays open until the owner retries the zip on the tablet. ND6a-A (failed-download counters + line) is committed but NOT in alpha115.

## Do this next
ND6a continues (claimed): (1) review the alpha114 'steps out of the way when you update' code against research/DG1_FIRST_RUN_AFTER_INSTALL.md section 4 BEFORE the owner installs alpha115 (the alpha115 install is its first real test); (2) pick further PROVEN improvements from code/diagnostics (see research/ND6_DIAGNOSTICS_2026-11-28.md section 3/4); (3) release them together as alpha116 only if at least one more proven change exists besides the failed-download line (every update costs the owner a first-start-after-install); (4) the answer to the owner for PDL1 + ND6 is the final message of this turn: Release link https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.3.0-alpha115 on its own line, what was NOT run, ND4a request, open items.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  0ae06bdc ckpt 3594: ND6a-A DONE in the tree (not released): Engine.Listener.onFailed(DownloadVie
  439bcc1b ckpt 3589: alpha115 (gecko code 216) gated green by ship.sh and DISPATCHED: android.yml
  aae77d3c ckpt 3588: gated kestrel-g-v0.3.0-alpha115 (gecko code 216) and pushed it: handoff self
  165f5255 ckpt 3586: pre-ship: alpha115: downloads that a page makes itself, like Proton Drive, n
  f47e6119 ckpt 3582: PDL1 CODED (not yet released): core Handed.kt (HandedStreams registry, Hande
  3261ff02 ckpt 3569: Second owner message (ND6: after PDL1, look for improvements from sent diagn
  59778a61 ckpt 3567: Owner message (Proton Drive zip download fails in Kestrel with 'The site sen
  a50908c2 ckpt 3539: alpha114 SHIPPED and recorded (Release kestrel-g-v0.3.0-alpha114, run 460786
  6ee3c3de ckpt 3537: ND5c: research/ND5_CAPTCHA_LOGIN.md has the Shields-off analysis and the bis
  fca1cfcc ckpt 3536: owner: 'Shields off fixed it' (my.nobullnetworks.com login): ND5c = find whi
```

(4 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
