# CHECKPOINT 3271 — read me first, then TASKS.md

**Written:** 2026-11-24T20:54:31Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/kind-brahmagupta-ae3yl8` · **builds on:** `decc03f0` (this checkpoint is the commit after it)

## Just done
probe v2 result: EVERY release-creation variant works with the workflow token (API draft and real, sha / branch / default branch targets, gh release create with a file) - so the alpha106 403 is specific to the real release: next test is the tag NAME shape kestrel-g-v* (matches the workflow tag trigger) and the real notes-file shape (probe v3). The Auto-tune emulator lab run did not start the optimizer at all (no opt events in 17 min): soak.py now fails after 100 s and prints the relevant logcat lines

## Do this next
Dispatch probe v3 (app=lab lab_suite=probe), read the job log: if the kestrel-g-v0.0.0-probe tag 403s -> tag-name rule, ask the owner to check Settings > Rules / Tags; if it works -> the failure was transient or content-specific: bump to alpha107 and ship Auto-tune + Firefox 161 together after the opt lab passes. THEN re-dispatch the opt lab (app=lab lab_suite=opt) and read its console log lines 'opt:' for why the optimizer did not start.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M .github/workflows/android.yml

## Last ten checkpoints
```
  7e08a8c5 ckpt 3269: probe v2: calls POST /releases directly (draft and real, target sha / branch
  8821fc21 ckpt 3268: alpha106 release publish fails with HTTP 403 twice (token has contents: writ
  8caaa5ba ckpt 3266: OPT1: Auto-tune integrated (BenchServer, Store, Apply, Guard, restart trampo
  59502d88 ckpt 3259: alpha106 (code 207) gated (ckpt 3257, head 0f14013b) and dispatched ONCE: ru
  0f14013b ckpt 3257: gated kestrel-g-v0.3.0-alpha106 (gecko code 207) and pushed it: handoff self
  c510bc41 ckpt 3256: OPT1: the Android-side sources and patch scripts are now saved IN the repo (
  f92c5435 ckpt 3255: OPT1: core done + 25 tests green, bench pages + web test green, tools/optimi
  59efade6 ckpt 3253: FF1: alpha106 prepared (code 207, GeckoView 161): lab smoke GREEN on 161 (ru
  29576308 ckpt 3230: FF1: GeckoView 161.0.20261118180411 + webcompat 161.0 pinned, clean compile,
  e7367846 ckpt 3222: wrote FF1 (new Firefox version into Kestrel, optimised for the hardware) and
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
