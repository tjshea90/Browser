# CHECKPOINT 3028 — read me first, then TASKS.md

**Written:** 2026-11-22T22:48:07Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/kind-brahmagupta-ae3yl8` · **builds on:** `ecc1f889` (this checkpoint is the commit after it)

## Just done
gated kestrel-g-v0.3.0-alpha95 (gecko code 196) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref claude/kind-brahmagupta-ae3yl8, inputs {"app":"gecko","full_build":"true","note":"alpha95: half-drawn text, attaching photos, and the files permission. Your newest uploads prove the first Safe drawing settings reached the engine and the bands still came, so Safe drawing now also turns off the engine's texture upload shortcut that can leave some rows of a letter unwritten while a busy page repaints, and Settings > Network lab has a new switch, Software drawing (slower, off by default, restart Kestrel after changing it): the page is then drawn by the processor, which a graphics driver fault cannot break, so you can tell in a minute whether the chip is the cause. Files permission: Android 10 has no All files access switch at all (it begins with Android 11), and Kestrel declared no storage permission so the Permissions screen had nothing to offer; Kestrel now declares the Storage and Photos permission (never asked for by itself, off until you turn it on) and Settings > Privacy has a new Photos and files permission row that opens Android's page for it. Attaching a picture does not need it: Android's own picker hands Kestrel exactly the file you choose, and your diagnostics show the picked screenshots were whole (about 100 KB, readable). To find where claude.ai stops, the page now measures what it received when you attach a file: its type and size, the first bytes, whether the picture decodes and whether a canvas can draw and re-encode it (no file name, nothing from private tabs), and the next diagnostics upload names which side is at fault. Not run on the tablet."}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag kestrel-g-v0.3.0-alpha95), then:  bash tools/record-release.sh kestrel-g-v0.3.0-alpha95 "alpha95: half-drawn text, attaching photos, and the files permission. Your newest uploads prove the first Safe drawing settings reached the engine and the bands still came, so Safe drawing now also turns off the engine's texture upload shortcut that can leave some rows of a letter unwritten while a busy page repaints, and Settings > Network lab has a new switch, Software drawing (slower, off by default, restart Kestrel after changing it): the page is then drawn by the processor, which a graphics driver fault cannot break, so you can tell in a minute whether the chip is the cause. Files permission: Android 10 has no All files access switch at all (it begins with Android 11), and Kestrel declared no storage permission so the Permissions screen had nothing to offer; Kestrel now declares the Storage and Photos permission (never asked for by itself, off until you turn it on) and Settings > Privacy has a new Photos and files permission row that opens Android's page for it. Attaching a picture does not need it: Android's own picker hands Kestrel exactly the file you choose, and your diagnostics show the picked screenshots were whole (about 100 KB, readable). To find where claude.ai stops, the page now measures what it received when you attach a file: its type and size, the first bytes, whether the picture decodes and whether a canvas can draw and re-encode it (no file name, nothing from private tabs), and the next diagnostics upload names which side is at fault. Not run on the tablet."
NEVER ship kestrel-g-v0.3.0-alpha95 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  36dea037 ckpt 3024: pre-ship: alpha95: half-drawn text, attaching photos, and the files permissi
  a732be58 ckpt 3020: PERF10 coded (not yet gated): pbo-uploads off in Safe drawing + Software dra
  5f4fb686 ckpt 3009: PERF10a research written: paint (draw-prefs read-back proves Safe drawing re
  89edc274 ckpt 3008: PERF10 written to TASKS.md (owner after alpha94: paint still chopped, cannot
  37893d32 ckpt 2991: alpha94 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha94 (gecko code 195, run
  41643a29 ckpt 2989: alpha94 (gecko code 195) gated by ship.sh (web 683/683, core+app suites, lin
  117a4f55 ckpt 2988: gated kestrel-g-v0.3.0-alpha94 (gecko code 195) and pushed it: handoff self-
  6fcdd4e1 ckpt 2986: pre-ship: alpha94: the second film that never played. Your upload shows what
  ae98cd8b ckpt 2978: PERF9 written to TASKS.md (owner after alpha93: one video worked, another sh
  c97f7f67 ckpt 2977: alpha93 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha93 (gecko code 194, run
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
