# CHECKPOINT 1403 — read me first, then TASKS.md

**Written:** 2026-11-13T02:38:07Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `49ee0be` (this checkpoint is the commit after it)

## Just done
gated kestrel-g-v0.3.0-alpha26 (gecko code 125) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref ccr-a57c163e-mtq8js, inputs {"app":"gecko","full_build":"true","note":"alpha26: the rest of the outside review's speed ideas, a better download manager, MP4 for more videos, deeper ad hiding and a calmer look. Faster: the browser now opens the connection to a link the moment your finger touches it, and to the first few search results after a search, so the page starts sooner (Settings > Network lab > Warm up connections turns it off; never in private tabs, never with Data saver). Firefox also looks up the addresses of links ahead of your tap and shows text in a plain font after half a second while a web font loads (both on, both switchable in the Network lab). Downloads: a download that is waiting says Waiting in line and has Start now, Move up and Move down; a paused one can also Start now. A file whose server states a checksum is checked when it finishes, files are written in bigger pieces, and a file name Android refuses makes the download try the next name instead of failing. Videos: more streams end as one MP4 instead of a .ts file (H.265 pictures and AC-3, E-AC-3 and MP3 sound now work), and the rest of an advertisement that the MP4 writer leaves out is no longer downloaded. Ad blocking: the bundled lists now include restyling rules and procedural rules (hide a box that contains the word Sponsored, remove a blur overlay) for the sites that need them. Look: Settings sections are cards like the menu and the Downloads list, and the app shows the shield on black the moment you tap its icon instead of an empty black screen while the engine starts. New switch, off by default: No AVIF pictures on Fast Wi-Fi (the Decode lab tells you when to use it)."}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag kestrel-g-v0.3.0-alpha26), then:  bash tools/record-release.sh kestrel-g-v0.3.0-alpha26 "alpha26: the rest of the outside review's speed ideas, a better download manager, MP4 for more videos, deeper ad hiding and a calmer look. Faster: the browser now opens the connection to a link the moment your finger touches it, and to the first few search results after a search, so the page starts sooner (Settings > Network lab > Warm up connections turns it off; never in private tabs, never with Data saver). Firefox also looks up the addresses of links ahead of your tap and shows text in a plain font after half a second while a web font loads (both on, both switchable in the Network lab). Downloads: a download that is waiting says Waiting in line and has Start now, Move up and Move down; a paused one can also Start now. A file whose server states a checksum is checked when it finishes, files are written in bigger pieces, and a file name Android refuses makes the download try the next name instead of failing. Videos: more streams end as one MP4 instead of a .ts file (H.265 pictures and AC-3, E-AC-3 and MP3 sound now work), and the rest of an advertisement that the MP4 writer leaves out is no longer downloaded. Ad blocking: the bundled lists now include restyling rules and procedural rules (hide a box that contains the word Sponsored, remove a blur overlay) for the sites that need them. Look: Settings sections are cards like the menu and the Downloads list, and the app shows the shield on black the moment you tap its icon instead of an empty black screen while the engine starts. New switch, off by default: No AVIF pictures on Fast Wi-Fi (the Decode lab tells you when to use it)."
NEVER ship kestrel-g-v0.3.0-alpha26 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  bc3596c ckpt 1401: AD1b-3/4 + AD1a-3 DONE: Settings cards + launch screen + Downloads queue UI v
  af1938b ckpt 1388: AD1b-3 UI polish: Settings sections are chamfered cards with dividers (like t
  7cb96e0 ckpt 1384: AD1b-2 mp4 always DONE in core: Hevc.kt (hvc1/hvcC), AudioFrames.kt (AC-3/E-A
  7919d0f ckpt 1373: AD1b-1 download manager DONE in core + UI wiring: Start now / Move up / Move 
  7b80ff7 ckpt 1365: AD1a-1 DONE: link warm-up triggers (finger down + top three search results) i
  ac0f553 ckpt 1353: Owner message 17 written into TASKS as AD1 (AD1a review rows, AD1b earlier AA
  16fc889 ckpt 1350: alpha25 SHIPPED and RECORDED: kestrel-g-v0.3.0-alpha25 (gecko code 124), run 
  62b643d ckpt 1348: alpha25 (code 124) gated + pushed (ckpt 1347) and DISPATCHED: android.yml run
  e95b292 ckpt 1347: gated kestrel-g-v0.3.0-alpha25 (gecko code 124) and pushed it: handoff self-t
  bede630 ckpt 1346: EX-10 PROVEN on the emulator (Tap to load box replaced the third-party frame;
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
