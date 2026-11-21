# CHECKPOINT 2849 — read me first, then TASKS.md

**Written:** 2026-11-21T23:09:05Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/peaceful-darwin-4sbmdz` · **builds on:** `86c9fea9` (this checkpoint is the commit after it)

## Just done
alpha87 first dispatch (run 44958961031) FAILED in test-js on GitHub: pagetime.test.js LD4 'assert.ok(m.fb >= 0 && m.ttfb >= 0)' (the CI browser gave the route-faked page no requestStart so ttfb=-1; passed locally and in the alpha85/86 runs): assertion made robust (ttfb -1 allowed, never above fb), pagetime 5/5 locally; no build job ran, no tag or Release exists for alpha87. Scratch probe test removed. Pornhub evidence found (research below)

## Do this next
Next: re-dispatch android.yml ONCE more for alpha87 (same inputs/note as docs/handoff/ALPHA87_RELEASE_NOTE.txt, app=gecko full_build=true, ref claude/peaceful-darwin-4sbmdz); wait; get_release_by_tag kestrel-g-v0.3.0-alpha87; tools/record-release.sh. THEN alpha88 for VD2c: (a) videoprobe stall report (element sits under readyState 3 for 15 s with no error), (b) pornhub cosmetic rules from the real lists: .mgp_overlayContainer, .video-wrapper > #player ~ .hd.clear, div.container margin-top, img[srcset], img[width=300] are suspects for the cut-off spinner; Shields blocked 5+ first-party XHR www.pornhub.com in the owner's alpha86 upload (page report); a source element at www.pornhub.com failed

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  3811af55 ckpt 2848: Owner follow-up on pornhub (VD2c, in TASKS.md): Shields ON cuts off the load
  7579817e ckpt 2847: alpha87 (code 188) gated and pushed, ONE android.yml dispatch made at about 
  85221da4 ckpt 2846: gated kestrel-g-v0.3.0-alpha87 (gecko code 188) and pushed it: handoff self-
  bf9a7857 ckpt 2845: pre-ship: alpha87: why videos do not load, and a test you can run today. I c
  a3480ef8 ckpt 2844: Digest FILE PICKS section (+2 checks, handoff 88/88); OwnerRulesTest pins up
  0a054723 ckpt 2843: IMG1 picked-file facts written (core FilePickFacts + FilePickStats.lastPick 
  39f5dc40 ckpt 2842: Research notes written for the 3 new owner requests (IMG1_UPLOAD.md, VD2 sec
  a0c4325f ckpt 2841: Owner sent 3 more requests mid-gate (IMG1 screenshots 'can't process images'
  6ed63137 ckpt 2840: VD1c/d part 2 written (gate running): core VideoSites + test (4 pass), Shiel
  418ba1cd ckpt 2839: VD1c/d part 1 green: per-site flag ShieldFlag.AUTOSTART (core + Shields pane
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
