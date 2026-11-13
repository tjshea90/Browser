# CHECKPOINT 1540 — read me first, then TASKS.md

**Written:** 2026-11-13T09:37:08Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `ff56881` (this checkpoint is the commit after it)

## Just done
alpha30 CI run 43399774749 FAILED in test-js (not the app build): web-tests/background.test.js 'page report: what Shields blocked...' was flaky (the plain-request count depended on how often ready() polled: n=2 vs >=3; and the first matched request can still be a promise): test made deterministic (count a plain request on purpose, await the first matched request), 14/14 standalone and 2x full 123/123 locally; no extension code changed, no tag was created

## Do this next
re-dispatch android.yml for alpha30 on the new commit (same inputs: app=gecko full_build=true note from scratchpad note30.txt; the tag kestrel-g-v0.3.0-alpha30 does not exist yet and BUILDLOG has no alpha30 line, so this is not a second shipment), wait for tag + success, get_release_by_tag, bash tools/record-release.sh kestrel-g-v0.3.0-alpha30 with note30.txt, then answer the owner (all three items + the AI1d question + open items) and save the handoff with the unanswered-prompt list

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  ea9b9f4 ckpt 1536: alpha30 DISPATCHED: android.yml run 43399774749 (run_number 80) queued 09:15:
  ab7b158 ckpt 1535: gated kestrel-g-v0.3.0-alpha30 (gecko code 129) and pushed it: handoff self-t
  ac7766e ckpt 1534: pre-ship: alpha30 (includes alpha29 and alpha28): your three requests. 1) Set
  613fb9e ckpt 1531: AI1a + AI1b EMULATOR-PROVEN (Settings in the private-window menu; long-press 
  53ae8c1 ckpt 1525: new context window (session restart): the owner re-sent items 1-3 of message 
  5930d95 ckpt 1519: handoff saved (.handoff/handoff.md; CLAUDE.md/AGENTS.md Session Learnings upd
  5c37df1 ckpt 1518: alpha29 SHIPPED and RECORDED (run 43383744581 green, BUILDLOG line written, R
  01ea47c ckpt 1515: alpha29 DISPATCHED: android.yml run 43383744581 (run_number 79) started 07:09
  78c9506 ckpt 1514: gated kestrel-g-v0.3.0-alpha29 (gecko code 128) and pushed it: handoff self-t
  f2230eb ckpt 1511: AH1b DONE and emulator-proven with the real engine (heads-up before a refused
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
