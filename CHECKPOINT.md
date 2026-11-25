# CHECKPOINT 2707 — read me first, then TASKS.md

**Written:** 2026-11-19T02:35:09Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/kestrel-browser-optimization-h6y8u2` · **builds on:** `0b5754bb` (this checkpoint is the commit after it)

## Just done
alpha71 RELEASED and RECORDED: https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.3.0-alpha71 (run 44443158040 green, APK 96 MB, certificate verified, BUILDLOG line written). TASKS EM1b-EM1j statuses written with evidence. Labs still going: compat run 44441687717 (Shields on/off, Yahoo last), dr v3 queued (title page + tap on the server button).

## Do this next
Read compat run 44441687717 (artifact emulator-lab-text: events.jsonl kind page, shields_mode on/off, report.md) into research/EM1_RESULTS.md (EM1f/EM1h: sites worse with Shields on); read dr v3 -> if Vidfast with press-to-play ON and a real tap on the server still finds nothing while rule-OFF finds 2 HLS streams, fix the gate inheritance (background.js frameNavigates PTAP_MS 8 s / PCHILD_MS 12 s, gate-main.js inheritUntil) for alpha72 with a lab proof; answer the owner (EM1l); when everything is recorded delete heartbeat trig_018ZFUWQpV3dBUPXUvKDiXwQ.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  1aa1cf0c ckpt 2706: Lab dr v2 (run 44439418098) READ: wrapper-direct rows are NOT the owner's fl
  5b20c111 ckpt 2705: alpha71 DISPATCHED ONCE: android.yml app=gecko full_build=true on branch tip
  f314d5ed ckpt 2704: gated kestrel-g-v0.3.0-alpha71 (gecko code 172) and pushed it: handoff self-
  c1463b83 ckpt 2703: alpha71 gates: first ship.sh run stopped on two documented timing-flaky down
  434b5ea7 ckpt 2702: alpha71 PREPARED: versionCode 172 / 0.3.0-alpha71, extension manifest 1.0.17
  ab9af2b4 ckpt 2701: dr scene: Vidfast also tried with press-to-play OFF (tells the rule from the
  378e887d ckpt 2700: Lab run 44435517079 (em) READ: reopen 3/3 drew the picture again; dr INCONCL
  1476784b ckpt 2699: EM1i naming: the REAL rendered doraby title page (headless Chromium) gives O
  abb85ece ckpt 2698: Lab run 44434241145 (suite ro, commit 609d22a7) READ: the 3 reopens that ran
  0853cfea ckpt 2697: EM1k step 2 CODED + tests green + Gecko module compiles: a page that painted
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
