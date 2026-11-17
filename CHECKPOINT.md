# CHECKPOINT 2588 — read me first, then TASKS.md

**Written:** 2026-11-17T01:37:25Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-e89b9dfe-pl3lk5` · **builds on:** `c6b2edcd` (this checkpoint is the commit after it)

## Just done
LAB3a/b/c code written: emu-lab.yml (profile input: tablet = 32-bit x86 Android 10 image + low-RAM prop attempt with fallback + font scale 1.2; 'daily' suite option; build-report step + 30-day artifact), android.yml (lab_profile, daily), tools/sitelab/soaklab.py (/sec battery + /sec/frame,read,echo,scheme,file + /bench CPU calibration; validated in headless Chromium), tools/emu/daily.py (DailyMixin: 15-step day of a person on the app's REAL defaults + 20 security/feature probes), soak.py hooks (--profile, daily suite, mixin loaded by path). Selftest on the fake phone runs all 15 steps with no Python error. NOT yet run on the real emulator.

## Do this next
LAB3d: write tools/emu/buildreport.py (reads events.jsonl, health-lines.txt, diagnostics-*.txt, device.json, summary.json from a lab run dir; writes BUILD-REPORT.md + build-report.json; compares with research/lab-baseline.json). Develop it against a REAL older artifact (fetch via mcp__github__actions_get download_workflow_run_artifact + python3 -I tools/emu/fetch_artifact.py <url> <empty dir>). Then dispatch android.yml app=lab lab_suite=daily lab_minutes=45 on this branch ccr-e89b9dfe-pl3lk5 (full_build=false).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  2d9c59f4 ckpt 2587: LAB3 (owner message 62: github runner + hardware-matched emulator + daily-us
  fe809368 ckpt 2586: alpha62 SHIPPED and RECORDED: Release kestrel-g-v0.3.0-alpha62 (code 163, ru
  a29d8d50 ckpt 2585: alpha62 build DISPATCHED ONCE: run 43989321170 (app=gecko full_build=true, h
  e7fd1dd9 ckpt 2584: gated kestrel-g-v0.3.0-alpha62 (gecko code 163) and pushed it: handoff self-
  75920d6d ckpt 2583: ANS61a-c ticked in TASKS.md with evidence: lab ho run 43986542855 = 61 check
  eb522196 ckpt 2582: alpha62 prepared but NOT shipped: geckoapp versionCode 163 / 0.3.0-alpha62, 
  241f219d ckpt 2581: lab ho run 43986542855 DISPATCHED (head 977ab6a8, app=lab lab_suite=ho): pro
  977ab6a8 ckpt 2580: ANS61c: lab variants preroll (ad-state class, ad set up before the press) + 
  25e3ced5 ckpt 2579: ANS61: :core:test 1413/0 failed (OwnerRulesTest guard added), :geckoapp:comp
  a86c1e78 ckpt 2578: ANS61b core+app+page done: WebHandOver ad rules (Element.ad, looksLikeAdAddr
```

(9 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
