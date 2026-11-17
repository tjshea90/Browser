# CHECKPOINT 2603 — read me first, then TASKS.md

**Written:** 2026-11-17T03:30:41Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-e89b9dfe-pl3lk5` · **builds on:** `4dc88c0d` (this checkpoint is the commit after it)

## Just done
LAB3/LAB4a: run 5 (44008297246) READ: all lab fixes held (Features + Security GREEN 16/0, typed address passes, private indicator seen). Answers: Shields A/B with 3 repeats per mode: ON median 1226 ms vs OFF 1315 ms on theguardian.com = no cost (the n=2 hint was noise, LAB3h answered); typed http://example.com/ shows the padlock 'Secure connection' = the https upgrade works (my regex looked for the wrong words, fixed). Run 6 (44010165696, baseline with tour, old app) running; run 7 (instrumented app, hotspot, 75 min budget) queued

## Do this next
When run 6 ends: download emulator-lab-text, regenerate the report (buildreport --notes research/LAB3_IMPROVEMENTS.md), read the idle-cpu raw text and the site tour; write research/lab-baseline.json from RUN 6 ONLY (--write-baseline) and docs/handoff/LABREPORT_0.3.0-alpha62.md. When run 7 ends: grep 'ui slow-at' and 'engine burst' and 'look read-back' in its health-lines.txt, name the start-up hog, THEN fix it (LAB4b step 2). Meanwhile run the JVM + web test suites

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M research/LAB3_IMPROVEMENTS.md
     M tools/emu/daily.py

## Last ten checkpoints
```
  4dc88c0d ckpt 2602: LAB4b step 1 (measure before fixing, evidence rule): app instrumentation onl
  7a95ed47 ckpt 2601: LAB4a: daily.py got two steps: idle-cpu-60-seconds (top -H + dumpsys cpuinfo
  0f3bfce1 ckpt 2600: LAB4 (owner message 63: do the other runs that should be done, then implemen
  dd595cbe ckpt 2599: LAB3e: lab run 4 (44005894797) READ: Security GREEN 16 pass / 0 fail, camera
  025bf31d ckpt 2598: LAB3f: research/LAB3_IMPROVEMENTS.md now opens with the measured table of la
  2d31626a ckpt 2597: LAB3e: lab run 3 (44003337190) READ: all four lab false positives gone, Secu
  d06f3696 ckpt 2596: LAB3e: lab run 3 DISPATCHED ONCE: android.yml run 44003337190 (daily, head b
  b52ed4c3 ckpt 2595: LAB3e: lab run 2 (43997607118) READ. The whole pipeline works on the real em
  e61155d2 ckpt 2594: LAB3f: the speed ledger + 8 NEW ranked candidates are saved in research/LAB3
  a3b36287 ckpt 2593: LAB3 docs: research/LAB3_DAILY.md (how to start, what is matched / not, the 
```
