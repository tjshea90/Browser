# CHECKPOINT 2746 — read me first, then TASKS.md

**Written:** 2026-11-20T07:44:07Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-4efedacd-v3co75` · **builds on:** `76880469` (this checkpoint is the commit after it)

## Just done
gated kestrel-g-v0.3.0-alpha74 (gecko code 175) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref ccr-4efedacd-v3co75, inputs {"app":"gecko","full_build":"true","note":"alpha74: a speed, video-smoothness and security pass over the whole browser (every finding of the review was checked against the code before anything was changed). SPEED: the ad and tracker check that runs for every request no longer tries about 450 rules that cannot apply to the page; on the full list that is 33 percent less work per request on a desktop with identical verdicts (11.8 million checks compared old against new), and it should save more on the tablet, which has not been measured. VIDEO SMOOTHNESS: when the link drops in the middle of a film in Kestrel's own player (full screen or inside the page) the film is no longer ended: it waits and tries again up to four times, a failed piece is retried longer once the picture has started, and the Play button after an error now really starts it again; downloads now slow down while a film plays in Kestrel's own player (about 250 KB/s on the capped hotspot, under the same Settings switch as for loading pages, which is now called Slow downloads while a page loads or a video plays) so the film does not fall to a blurry picture or stall; the 480p limit now includes the usual 854 by 480 picture size (it used to fall to 360p); the first guess of the link speed now also applies behind the fast Wi-Fi tunnel. SECURITY: a web page can no longer aim Kestrel's own player at the router, a NAS or any other device on your network (every spelling of such an address is now recognised, and the player only opens web addresses); a web page can no longer freeze the browser with a crafted answer that hides video addresses (the address finder is bounded now); update file names that are not plain are refused and the release data is read with a size limit; private tabs no longer leave their site name in the Diagnostics. Not tested on the tablet: all of this was built and tested on a computer and in the build checks; the video retry in particular needs a real dropout on the hotspot to prove it."}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag kestrel-g-v0.3.0-alpha74), then:  bash tools/record-release.sh kestrel-g-v0.3.0-alpha74 "alpha74: a speed, video-smoothness and security pass over the whole browser (every finding of the review was checked against the code before anything was changed). SPEED: the ad and tracker check that runs for every request no longer tries about 450 rules that cannot apply to the page; on the full list that is 33 percent less work per request on a desktop with identical verdicts (11.8 million checks compared old against new), and it should save more on the tablet, which has not been measured. VIDEO SMOOTHNESS: when the link drops in the middle of a film in Kestrel's own player (full screen or inside the page) the film is no longer ended: it waits and tries again up to four times, a failed piece is retried longer once the picture has started, and the Play button after an error now really starts it again; downloads now slow down while a film plays in Kestrel's own player (about 250 KB/s on the capped hotspot, under the same Settings switch as for loading pages, which is now called Slow downloads while a page loads or a video plays) so the film does not fall to a blurry picture or stall; the 480p limit now includes the usual 854 by 480 picture size (it used to fall to 360p); the first guess of the link speed now also applies behind the fast Wi-Fi tunnel. SECURITY: a web page can no longer aim Kestrel's own player at the router, a NAS or any other device on your network (every spelling of such an address is now recognised, and the player only opens web addresses); a web page can no longer freeze the browser with a crafted answer that hides video addresses (the address finder is bounded now); update file names that are not plain are refused and the release data is read with a size limit; private tabs no longer leave their site name in the Diagnostics. Not tested on the tablet: all of this was built and tested on a computer and in the build checks; the video retry in particular needs a real dropout on the hotspot to prove it."
NEVER ship kestrel-g-v0.3.0-alpha74 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  6ab74f37 ckpt 2745: RVW3 fixes CODED + compile green (:geckoapp:compileDebugKotlin EXIT 0): secu
  7496a10a ckpt 2744: RVW3: security lane back and VERIFIED by me against the code (SC1 SC2 SC4 SC
  fe77b529 ckpt 2743: RVW3a launched: three read-only review lanes running as background agents (p
  e4b18cd0 ckpt 2742: RVW3 written into TASKS.md in the owner's words (review + improve speed, vid
  23e83be6 ckpt 2741: Owner decision recorded: keep Firefox's background traffic (P4) as it is; P3
  94c87088 ckpt 2740: alpha73 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha73 (gecko code 174, run
  7f06c310 ckpt 2739: REL73b: alpha73 DISPATCHED ONCE (android.yml app=gecko full_build=true ref c
  5bf338a5 ckpt 2738: gated kestrel-g-v0.3.0-alpha73 (gecko code 174) and pushed it: handoff self-
  f2a9d669 ckpt 2737: REL73a done: versionCode 174 / 0.3.0-alpha73, ext synced, ALPHA73 release no
  b754bfa7 ckpt 2736: STANDING RULE recorded (owner: Always ship new versions and release without 
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
