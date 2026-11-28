# CHECKPOINT 3567 — read me first, then TASKS.md

**Written:** 2026-11-28T08:01:14Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/elegant-darwin-h7dlay` · **builds on:** `192e9835` (this checkpoint is the commit after it)

## Just done
Owner message (Proton Drive zip download fails in Kestrel with 'The site sent a web page instead of the file', fine in Brave) captured in INBOX by hand (the UserPromptSubmit hook did not fire for it) and written to TASKS.md as PDL1/PDL1a-d, claimed. Hypothesis from the code: TabDelegates.onExternalResponse closes the engine body and the download manager re-requests response.uri itself, which cannot work for a service-worker stream (Proton Drive) or blob:, so core Engine fails it BAD_CONTENT.

## Do this next
PDL1a/b: read core/download/Engine.kt (BAD_CONTENT at ~502 and ~702) + core/download Source/fetcher + geckoapp DownloadController.enqueue; add a one-shot 'engine stream' job kind that saves WebResponse.body (TabDelegates.onExternalResponse must NOT close the body when the address cannot be fetched again: service worker /sw/ path, blob:, data:, POST); write the core JVM test first (fake InputStream: whole file, cut stream, html shell), then wire Gecko, then ship (bump gecko code 216 / alpha115).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  a50908c2 ckpt 3539: alpha114 SHIPPED and recorded (Release kestrel-g-v0.3.0-alpha114, run 460786
  6ee3c3de ckpt 3537: ND5c: research/ND5_CAPTCHA_LOGIN.md has the Shields-off analysis and the bis
  fca1cfcc ckpt 3536: owner: 'Shields off fixed it' (my.nobullnetworks.com login): ND5c = find whi
  2c6e069e ckpt 3535: ND5 read: research/ND5_CAPTCHA_LOGIN.md written (reCAPTCHA v2 invisible, key
  9acfa854 ckpt 3534: alpha114 (code 215) gated green by ship.sh and DISPATCHED (android.yml gecko
  62e73b2d ckpt 3533: gated kestrel-g-v0.3.0-alpha114 (gecko code 215) and pushed it: handoff self
  8c29adfa ckpt 3532: owner sent a screenshot: my.nobullnetworks.com/login says Captcha verificati
  202c2cbe ckpt 3531: pre-ship: alpha114: Kestrel steps out of the way when you update, and the pa
  e1a80876 ckpt 3526: pre-ship: alpha114: Kestrel steps out of the way when you update, and the pa
  8617883a ckpt 3519: owner message after alpha113 written as TASKS ND4 (new profile; app still cr
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
