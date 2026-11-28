# CHECKPOINT 3569 — read me first, then TASKS.md

**Written:** 2026-11-28T08:04:23Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/elegant-darwin-h7dlay` · **builds on:** `fc35be0f` (this checkpoint is the commit after it)

## Just done
Second owner message (ND6: after PDL1, look for improvements from sent diagnostics and code) written to TASKS.md as ND6/ND6a, NOT started (waits for PDL1 shipped). The UserPromptSubmit hook DID fire for it (INBOX has an automatic entry), so the hooks are live from the second message on.

## Do this next
PDL1 design decided after reading Engine.kt/Model.kt/GeckoFetcher.kt: (1) core/download/HandedStreams.kt = one-shot registry of the engine's own WebResponse bodies keyed by address (expires after ~2 min, closes what nobody claimed, max 4); (2) core/download/HandedFetcher.kt = Fetcher decorator for kind 2: asks the real (Gecko) fetcher first, and when the answer is not a usable file (html type where the engine's own response was not html, 4xx/5xx, FetchException) or the address is blob:/data:, serves the engine's own stream instead (status 200, headers from the engine response, no range, no speed limiter); a normal link closes the engine's stream at once as today; (3) TabDelegates.onExternalResponse offers the body to the registry instead of closing it, BrowserActivity.startDownload uses fetcher kind 2 only when a stream was offered for that address, a declined/cancelled question drops it; (4) Reason/Planner text for a handed stream that was cut ('open the page and download again'); (5) JVM tests in core first (EngineTest-style fakes: whole file, cut stream, html shell, normal link unchanged), then ship gecko code 216 / alpha115. Then ND6a.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  59778a61 ckpt 3567: Owner message (Proton Drive zip download fails in Kestrel with 'The site sen
  a50908c2 ckpt 3539: alpha114 SHIPPED and recorded (Release kestrel-g-v0.3.0-alpha114, run 460786
  6ee3c3de ckpt 3537: ND5c: research/ND5_CAPTCHA_LOGIN.md has the Shields-off analysis and the bis
  fca1cfcc ckpt 3536: owner: 'Shields off fixed it' (my.nobullnetworks.com login): ND5c = find whi
  2c6e069e ckpt 3535: ND5 read: research/ND5_CAPTCHA_LOGIN.md written (reCAPTCHA v2 invisible, key
  9acfa854 ckpt 3534: alpha114 (code 215) gated green by ship.sh and DISPATCHED (android.yml gecko
  62e73b2d ckpt 3533: gated kestrel-g-v0.3.0-alpha114 (gecko code 215) and pushed it: handoff self
  8c29adfa ckpt 3532: owner sent a screenshot: my.nobullnetworks.com/login says Captcha verificati
  202c2cbe ckpt 3531: pre-ship: alpha114: Kestrel steps out of the way when you update, and the pa
  e1a80876 ckpt 3526: pre-ship: alpha114: Kestrel steps out of the way when you update, and the pa
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
