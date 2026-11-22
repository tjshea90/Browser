# CHECKPOINT 3020 — read me first, then TASKS.md

**Written:** 2026-11-22T22:29:17Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/kind-brahmagupta-ae3yl8` · **builds on:** `8ed08de5` (this checkpoint is the commit after it)

## Just done
PERF10 coded (not yet gated): pbo-uploads off in Safe drawing + Software drawing switch (labSoft, GeckoPrefs, Settings, backup), Storage/Photos permission declared + Settings row opening Android app page, page-side attach probe (pagetime.js k=fc -> background.js -> ShieldsBridge fc -> BrowserActivity.onFileCheck -> Health filepick page-got + FilePickFacts.pageLine/pageVerdict + Diagnostics sentence); web pagetime tests 8/8 green

## Do this next
Next: run web-tests background.test.js (new fc test) + whole npm test; update tools/diag_digest.py FILE PICKS to print page-got lines + verdict (and its test if any); run ./gradlew --offline -q :core:test :app:testDebugUnitTest :geckoapp:testDebugUnitTest in background (poll log for EXIT); bump versionCode 196 / 0.3.0-alpha95 + extension manifest version sync (tools/sync-ext-version.sh); ship.sh gecko; dispatch android.yml; record-release; answer owner (all-files answer, what to try: Software drawing, attach test then Send now)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  5f4fb686 ckpt 3009: PERF10a research written: paint (draw-prefs read-back proves Safe drawing re
  89edc274 ckpt 3008: PERF10 written to TASKS.md (owner after alpha94: paint still chopped, cannot
  37893d32 ckpt 2991: alpha94 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha94 (gecko code 195, run
  41643a29 ckpt 2989: alpha94 (gecko code 195) gated by ship.sh (web 683/683, core+app suites, lin
  117a4f55 ckpt 2988: gated kestrel-g-v0.3.0-alpha94 (gecko code 195) and pushed it: handoff self-
  6fcdd4e1 ckpt 2986: pre-ship: alpha94: the second film that never played. Your upload shows what
  ae98cd8b ckpt 2978: PERF9 written to TASKS.md (owner after alpha93: one video worked, another sh
  c97f7f67 ckpt 2977: alpha93 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha93 (gecko code 194, run
  a882ac50 ckpt 2974: alpha93 (gecko code 194) gated by ship.sh and ONE dispatch made on ccr-144ae
  5488bb85 ckpt 2973: gated kestrel-g-v0.3.0-alpha93 (gecko code 194) and pushed it: handoff self-
```

(10 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
