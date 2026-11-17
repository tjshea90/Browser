# CHECKPOINT 2597 — read me first, then TASKS.md

**Written:** 2026-11-17T02:50:22Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-e89b9dfe-pl3lk5` · **builds on:** `54c2b1d6` (this checkpoint is the commit after it)

## Just done
LAB3e: lab run 3 (44003337190) READ: all four lab false positives gone, Security GREEN 14 pass / 0 fail (bad certs refused ok=false, pop-up blocked with the app's chip, mixed content upgraded, 19 typed key events arrived intact, referer trimmed, GPC sent, WebRTC masked, 3rd-party cookie not shared, clipboard refused), Features GREEN, Stability GREEN, Speed AMBER (1245 ms late UI heartbeat at cold start, 1386 ms in the long article), low-RAM: emulator accepts -prop ro.config.low_ram=true but Android 10 ignores it (read-back empty) = not achievable that way. New facts from ui-home.txt: the address bar is a clickable EditText whose text is the full URL at the bottom of the page area (y 859-906), nav bar 913-973 (Back, Forward, Home, New tab, Tabs, Download video, Menu); the app raises a real BLOCK/ALLOW dialog for camera/microphone (correct). daily.py now finds the address control by class, goes Home in the home step, dismisses permission dialogs and records their titles, proves http->https from the address control

## Do this next
Dispatch lab run 4 ONCE (same inputs, ref ccr-e89b9dfe-pl3lk5, workflow_runs_filter branch + event workflow_dispatch to get the id), read it, then FINALISE: buildreport.py --notes research/LAB3_IMPROVEMENTS.md --write-baseline research/lab-baseline.json on the run-4 artifact dir, copy BUILD-REPORT.md to docs/handoff/LABREPORT_0.3.0-alpha62.md, tick LAB3a-g with evidence, answer the owner, delete heartbeat trig_01K9rvGwTaZNy2NbUQeBeWMg and the send_later trig_01TfmPuNRp4XY2gZATmURc6Z

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  d06f3696 ckpt 2596: LAB3e: lab run 3 DISPATCHED ONCE: android.yml run 44003337190 (daily, head b
  b52ed4c3 ckpt 2595: LAB3e: lab run 2 (43997607118) READ. The whole pipeline works on the real em
  e61155d2 ckpt 2594: LAB3f: the speed ledger + 8 NEW ranked candidates are saved in research/LAB3
  a3b36287 ckpt 2593: LAB3 docs: research/LAB3_DAILY.md (how to start, what is matched / not, the 
  a1a955e8 ckpt 2592: LAB3e: lab run 2 DISPATCHED ONCE: android.yml run 43997607118 (app=lab lab_s
  c59a2c9a ckpt 2591: LAB3 run 1 (43997001918) failed at the Gradle step: Mozilla publishes NO gec
  3b71b73b ckpt 2590: LAB3d done: tools/emu/buildreport.py (build identity from geckoapp/build.gra
  a198ecf8 ckpt 2589: LAB3e: first daily-suite run DISPATCHED ONCE: android.yml run 43997001918 (a
  f1cb9443 ckpt 2588: LAB3a/b/c code written: emu-lab.yml (profile input: tablet = 32-bit x86 Andr
  2d9c59f4 ckpt 2587: LAB3 (owner message 62: github runner + hardware-matched emulator + daily-us
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
