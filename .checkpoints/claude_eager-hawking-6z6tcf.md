# CHECKPOINT 3534 — read me first, then TASKS.md

**Written:** 2026-11-27T22:47:15Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/eager-hawking-6z6tcf` · **builds on:** `62e73b2d` (this checkpoint is the commit after it)

## Just done
alpha114 (code 215) gated green by ship.sh and DISPATCHED (android.yml gecko full_build=true): moveTaskToBack half a second after the installer screen (UpdateManager.stepOutOfTheWayOfTheInstaller), inst{} + install-result notes, digest FIRST START AFTER AN INSTALL; ND5 (captcha at my.nobullnetworks.com/login) under investigation: WHMCS nexus + reCAPTCHA v2 INVISIBLE (site key valid for the domain per Google's anchor page), only googletagmanager blocked, no script errors

## Do this next
wait for tag kestrel-g-v0.3.0-alpha114 (git ls-remote), verify run (list_workflow_runs perPage 1) + get_release_by_tag, then bash tools/record-release.sh kestrel-g-v0.3.0-alpha114 with the IDENTICAL note from docs/handoff/ALPHA114_RELEASE_NOTE.txt; then ND5: decide whether a form-submit/captcha probe (lengths only) is the next instrument, or a fix; answer the owner (ND4a/b/c/ND5) with the Release link

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  62e73b2d ckpt 3533: gated kestrel-g-v0.3.0-alpha114 (gecko code 215) and pushed it: handoff self
  8c29adfa ckpt 3532: owner sent a screenshot: my.nobullnetworks.com/login says Captcha verificati
  202c2cbe ckpt 3531: pre-ship: alpha114: Kestrel steps out of the way when you update, and the pa
  e1a80876 ckpt 3526: pre-ship: alpha114: Kestrel steps out of the way when you update, and the pa
  8617883a ckpt 3519: owner message after alpha113 written as TASKS ND4 (new profile; app still cr
  62f3b899 ckpt 3518: alpha113 SHIPPED and recorded (Release kestrel-g-v0.3.0-alpha113, run 460705
  f0095b38 ckpt 3513: alpha113 (code 214) gated green by ship.sh and DISPATCHED (android.yml app=g
  2012784b ckpt 3512: gated kestrel-g-v0.3.0-alpha113 (gecko code 214) and pushed it: handoff self
  5af245e5 ckpt 3511: pre-ship: alpha113: a crash fixed, and a better look inside claude.ai (alpha
  90abb06a ckpt 3499: owner message after alpha112: 'I sent a new diagnostics and a new page profi
```
