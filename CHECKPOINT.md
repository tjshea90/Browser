# CHECKPOINT 2846 — read me first, then TASKS.md

**Written:** 2026-11-21T22:52:13Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/peaceful-darwin-4sbmdz` · **builds on:** `bf9a7857` (this checkpoint is the commit after it)

## Just done
gated kestrel-g-v0.3.0-alpha87 (gecko code 188) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref claude/peaceful-darwin-4sbmdz, inputs {"app":"gecko","full_build":"true","note":"alpha87: why videos do not load, and a test you can run today. I could not watch Instagram or pornhub from here, so none of this is proven on your tablet. Reading the code, the strongest suspect for Reels (and any video that a page starts by script) is your own rule that videos start only when you press play: a Reel starts itself as it scrolls into view, the rule holds or refuses it, and the page can sit on its loading circle. So there is now a switch for ONE site: tap the shield, then Videos may start by themselves, and reload. If instagram.com or pornhub.com plays after that, that was the cause, and only that site is exempt (the rule stays for every other site; sound still needs a tap; a private tab keeps it session-only). pornhub is a good test because Kestrel's own player already plays the same film, which means the film, the chip and the hotspot are fine and only the page player waits. The app also now records, per site and only outside private tabs, what the rule held back, which video formats were refused and which video elements failed, so the next live upload names the cause in the digest instead of guessing. Screenshots: the file picker in your uploads works (it opens and hands the file to the page); what happens after that is not visible to Kestrel, so alpha87 records what the picked file looks like to Android (type, size, readable, free storage; no name). To tell browser from Claude: attach the same screenshot to claude.ai in another browser; if only Kestrel fails, it is Kestrel and the new line will say why. Undrawn page parts: no cause yet; I need your screenshots, the site, what you did just before, and whether Shields down for that site fixes it. Not run on the tablet: everything above."}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag kestrel-g-v0.3.0-alpha87), then:  bash tools/record-release.sh kestrel-g-v0.3.0-alpha87 "alpha87: why videos do not load, and a test you can run today. I could not watch Instagram or pornhub from here, so none of this is proven on your tablet. Reading the code, the strongest suspect for Reels (and any video that a page starts by script) is your own rule that videos start only when you press play: a Reel starts itself as it scrolls into view, the rule holds or refuses it, and the page can sit on its loading circle. So there is now a switch for ONE site: tap the shield, then Videos may start by themselves, and reload. If instagram.com or pornhub.com plays after that, that was the cause, and only that site is exempt (the rule stays for every other site; sound still needs a tap; a private tab keeps it session-only). pornhub is a good test because Kestrel's own player already plays the same film, which means the film, the chip and the hotspot are fine and only the page player waits. The app also now records, per site and only outside private tabs, what the rule held back, which video formats were refused and which video elements failed, so the next live upload names the cause in the digest instead of guessing. Screenshots: the file picker in your uploads works (it opens and hands the file to the page); what happens after that is not visible to Kestrel, so alpha87 records what the picked file looks like to Android (type, size, readable, free storage; no name). To tell browser from Claude: attach the same screenshot to claude.ai in another browser; if only Kestrel fails, it is Kestrel and the new line will say why. Undrawn page parts: no cause yet; I need your screenshots, the site, what you did just before, and whether Shields down for that site fixes it. Not run on the tablet: everything above."
NEVER ship kestrel-g-v0.3.0-alpha87 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  bf9a7857 ckpt 2845: pre-ship: alpha87: why videos do not load, and a test you can run today. I c
  a3480ef8 ckpt 2844: Digest FILE PICKS section (+2 checks, handoff 88/88); OwnerRulesTest pins up
  0a054723 ckpt 2843: IMG1 picked-file facts written (core FilePickFacts + FilePickStats.lastPick 
  39f5dc40 ckpt 2842: Research notes written for the 3 new owner requests (IMG1_UPLOAD.md, VD2 sec
  a0c4325f ckpt 2841: Owner sent 3 more requests mid-gate (IMG1 screenshots 'can't process images'
  6ed63137 ckpt 2840: VD1c/d part 2 written (gate running): core VideoSites + test (4 pass), Shiel
  418ba1cd ckpt 2839: VD1c/d part 1 green: per-site flag ShieldFlag.AUTOSTART (core + Shields pane
  4b9a2d4a ckpt 2838: VD1a/b done: research/VIDEO_NOLOAD.md written. Cause 1 (strongest for Reels 
  ddf1d945 ckpt 2837: VD1 written into TASKS.md in the owner's words (diagnose why Instagram reels
  cb695e57 ckpt 2836: alpha86 RELEASED AND RECORDED: https://github.com/tjshea90/Browser/releases/
```
