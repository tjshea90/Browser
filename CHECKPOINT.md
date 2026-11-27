# CHECKPOINT 3536 — read me first, then TASKS.md

**Written:** 2026-11-27T22:54:34Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/eager-hawking-6z6tcf` · **builds on:** `2c6e069e` (this checkpoint is the commit after it)

## Just done
owner: 'Shields off fixed it' (my.nobullnetworks.com login): ND5c = find which Shields protection breaks reCAPTCHA invisible and fix with Shields ON; alpha114 build still running (waiter bs2p8bgw1)

## Do this next
ND5c: read what Shields-off changes (TabDelegates / ShieldsBridge / GeckoEngine ETP exception, FPP, cookie isolation, query stripping) and which of them reCAPTCHA invisible needs (Google cookies in the anchor frame, canvas/fingerprint, referrer); design a captcha allowance (per captcha host: ETP/FPP/cookie exception only for the captcha frame, or per-site exception auto-applied on pages that embed a captcha); then alpha114 release steps when the tag appears

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  2c6e069e ckpt 3535: ND5 read: research/ND5_CAPTCHA_LOGIN.md written (reCAPTCHA v2 invisible, key
  9acfa854 ckpt 3534: alpha114 (code 215) gated green by ship.sh and DISPATCHED (android.yml gecko
  62e73b2d ckpt 3533: gated kestrel-g-v0.3.0-alpha114 (gecko code 215) and pushed it: handoff self
  8c29adfa ckpt 3532: owner sent a screenshot: my.nobullnetworks.com/login says Captcha verificati
  202c2cbe ckpt 3531: pre-ship: alpha114: Kestrel steps out of the way when you update, and the pa
  e1a80876 ckpt 3526: pre-ship: alpha114: Kestrel steps out of the way when you update, and the pa
  8617883a ckpt 3519: owner message after alpha113 written as TASKS ND4 (new profile; app still cr
  62f3b899 ckpt 3518: alpha113 SHIPPED and recorded (Release kestrel-g-v0.3.0-alpha113, run 460705
  f0095b38 ckpt 3513: alpha113 (code 214) gated green by ship.sh and DISPATCHED (android.yml app=g
  2012784b ckpt 3512: gated kestrel-g-v0.3.0-alpha113 (gecko code 214) and pushed it: handoff self
```
