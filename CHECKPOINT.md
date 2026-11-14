# CHECKPOINT 2337 — read me first, then TASKS.md

**Written:** 2026-11-14T19:55:54Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `c977ba0` (this checkpoint is the commit after it)

## Just done
alpha40 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha40 (gecko code 141, run 43634029105 green, APK in the Release, BUILDLOG.md line pushed); AY1-AY5 all ticked. In the tree but NOT in alpha40: BrowserActivity SLOW_PAGE_MS 60 s -> 30 s (a page whose slow script finished is normal again after 30 s; a stuck page keeps it fresh because the engine re-asks every 20 s)

## Do this next
Owner answer for message 39 given in plain words (Release link https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.3.0-alpha40). Open for the owner: AX1g system keyboard app YES/NO; tablet feedback on alpha40 (long press feel, panel pick-up, space slide). Nothing else is claimed or half done. If the owner reports a problem, start from research/MINI_NAV.md section AY2/AY3/AY4 (what each long press delivers, the classifier, the emulator recipe tools/emu/glide_emu.py holddrag|slide + tools/sitelab/presslab.py) and fix; the next release needs versionCode 142

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  e2056d0 ckpt 2333: alpha40 (gecko code 141) gated, pushed and the android.yml run DISPATCHED (ap
  13e4374 ckpt 2325: gated kestrel-g-v0.3.0-alpha40 (gecko code 141) and pushed it: handoff self-t
  917a332 ckpt 2320: AY2 final: content.js treats only a NEW non-empty selection as text (emulator
  097f61f ckpt 2312: AY1, AY3, AY4 ticked with evidence (emulator proof of long press matrix, pick
  ca5c468 ckpt 2300: AY2+AY4 coded and unit-tested: MiniNav.showAt, PanelLayout long press pick-up
  bad447a ckpt 2287: AY1 coded+guarded; AY2 compiled (content.js contextmenu classifier -> backgro
  9a31c5a ckpt 2282: AY1 code done (askSlowScript answers CONTINUE with no dialog, Health counter 
  1eb7220 ckpt 2270: owner message 39 written into TASKS (AY1 no slow-page prompt, AY2 long press 
  44a36fb ckpt 2266: alpha39 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha39 (gecko code 140, run 
  418e420 ckpt 2260: alpha39 DISPATCHED (AX1: glide shapes + export/import): android.yml run_workf
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
