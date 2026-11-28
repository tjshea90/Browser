# CHECKPOINT 3604 — read me first, then TASKS.md

**Written:** 2026-11-28T09:11:27Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/elegant-darwin-h7dlay` · **builds on:** `58fb0020` (this checkpoint is the commit after it)

## Just done
ND6a-B/C in the tree (not released): Downloads screen 'Stopped' heading for FAILED/NEEDS_DECISION rows; alpha115's download.handed.* counters no longer count private-tab downloads (owner's private-tab rule) with a test; HandedTest 19/19, :app + :geckoapp compile green. INTERIM ANSWER with the alpha115 Release link already sent to the owner in this turn (cause, what it does, what was not run, ND4a request, small follow-up build announced).

## Do this next
Ship alpha116 (gecko code 217, 0.3.0-alpha116) = ND6a-A (download failed counters + line) + ND6a-B (Stopped heading) + ND6a-C (private-tab counters fix): bump geckoapp/build.gradle.kts versionCode 217 / 0.3.0-alpha116, bash tools/sync-ext-version.sh, write docs/handoff/ALPHA116_RELEASE_NOTE.txt (ASCII, no double quotes or backslashes), bash ship.sh gecko with that note (run it in the background, poll its log for SHIP-EXIT), ONE android.yml dispatch (app=gecko full_build=true, ref claude/elegant-darwin-h7dlay), wait for the tag (git ls-remote --tags), all jobs success, get_release_by_tag, bash tools/record-release.sh with the identical note, tick ND6a (claim.sh ND6a --done) and ND6, then the final answer: alpha116 link on its own line, what changed in plain words, what was not run, ND4a request (install the NEWEST, it contains alpha115), open items re-listed (ND4a, ND5c bisect, ND3f recording, VPS pick, PERF25a, PDL1a retry).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  ce3957d6 ckpt 3600: alpha115 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha115 (gecko code 216, r
  0ae06bdc ckpt 3594: ND6a-A DONE in the tree (not released): Engine.Listener.onFailed(DownloadVie
  439bcc1b ckpt 3589: alpha115 (gecko code 216) gated green by ship.sh and DISPATCHED: android.yml
  aae77d3c ckpt 3588: gated kestrel-g-v0.3.0-alpha115 (gecko code 216) and pushed it: handoff self
  165f5255 ckpt 3586: pre-ship: alpha115: downloads that a page makes itself, like Proton Drive, n
  f47e6119 ckpt 3582: PDL1 CODED (not yet released): core Handed.kt (HandedStreams registry, Hande
  3261ff02 ckpt 3569: Second owner message (ND6: after PDL1, look for improvements from sent diagn
  59778a61 ckpt 3567: Owner message (Proton Drive zip download fails in Kestrel with 'The site sen
  a50908c2 ckpt 3539: alpha114 SHIPPED and recorded (Release kestrel-g-v0.3.0-alpha114, run 460786
  6ee3c3de ckpt 3537: ND5c: research/ND5_CAPTCHA_LOGIN.md has the Shields-off analysis and the bis
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
