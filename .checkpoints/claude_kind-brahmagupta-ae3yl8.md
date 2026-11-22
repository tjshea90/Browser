# CHECKPOINT 3029 — read me first, then TASKS.md

**Written:** 2026-11-22T22:48:46Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/kind-brahmagupta-ae3yl8` · **builds on:** `12cc7bbd` (this checkpoint is the commit after it)

## Just done
alpha95 (gecko code 196) gated by ship.sh (core+app+geckoapp suites, handoff 84, web tests) and ONE dispatch made on claude/kind-brahmagupta-ae3yl8 (android.yml, app=gecko, full_build=true); contents: PERF10 pbo-uploads off + Software drawing switch, Photos and files permission declared + Settings row, page-side attach probe filepick page-got

## Do this next
Next: wait for tag kestrel-g-v0.3.0-alpha95 (git ls-remote --tags origin) and all jobs success (mcp__github__actions_list list_workflow_runs android.yml branch claude/kind-brahmagupta-ae3yl8); then mcp__github__get_release_by_tag shows the APK; then bash tools/record-release.sh kestrel-g-v0.3.0-alpha95 with the IDENTICAL note (saved in the scratchpad note95.txt, also in the ship.sh output); then tick PERF10a-d [~] in TASKS.md with run id; answer the owner with the Release link on its own line. NEVER dispatch alpha95 twice.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  12cc7bbd ckpt 3028: gated kestrel-g-v0.3.0-alpha95 (gecko code 196) and pushed it: handoff self-
  36dea037 ckpt 3024: pre-ship: alpha95: half-drawn text, attaching photos, and the files permissi
  a732be58 ckpt 3020: PERF10 coded (not yet gated): pbo-uploads off in Safe drawing + Software dra
  5f4fb686 ckpt 3009: PERF10a research written: paint (draw-prefs read-back proves Safe drawing re
  89edc274 ckpt 3008: PERF10 written to TASKS.md (owner after alpha94: paint still chopped, cannot
  37893d32 ckpt 2991: alpha94 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha94 (gecko code 195, run
  41643a29 ckpt 2989: alpha94 (gecko code 195) gated by ship.sh (web 683/683, core+app suites, lin
  117a4f55 ckpt 2988: gated kestrel-g-v0.3.0-alpha94 (gecko code 195) and pushed it: handoff self-
  6fcdd4e1 ckpt 2986: pre-ship: alpha94: the second film that never played. Your upload shows what
  ae98cd8b ckpt 2978: PERF9 written to TASKS.md (owner after alpha93: one video worked, another sh
```
