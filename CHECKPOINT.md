# CHECKPOINT 1439 — read me first, then TASKS.md

**Written:** 2026-11-13T03:38:14Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `091fee3` (this checkpoint is the commit after it)

## Just done
gated kestrel-g-v0.3.0-alpha27 (gecko code 126) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref ccr-a57c163e-mtq8js, inputs {"app":"gecko","full_build":"true","note":"alpha27: video fixes. The loading symbol stuck on top of a site's video player: the browser refused every request by a page to start a video by itself, even a muted one, so a player that starts muted waited for ever. Muted videos may now start by themselves (Settings > Video turns it off); a video with sound still waits for your tap. Page animations had been switched off for every site since alpha3, which can freeze a site's own loading symbol: they are back to normal, and Settings > Video > Calm pages turns the quiet mode back on. Choppy video: Settings > Video > Video formats offered to sites (Automatic keeps VP9, AV1 and H.265 away from sites when this tablet would decode them in software, so sites send H.264, which the tablet decodes with its video chip). New Menu > Diagnostics > Video lab: it plays short clips of each kind on the tablet and says smooth or choppy with the pictures it dropped, and it checks the screen's frame rate, JavaScript, WebAssembly and graphics; Diagnostics also lists the tablet's real video decoders (hardware or software) and Widevine. Protected (DRM) video was refused outright; it now asks "wants to play protected video" when the tablet supports it (Settings > Video turns it off)."}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag kestrel-g-v0.3.0-alpha27), then:  bash tools/record-release.sh kestrel-g-v0.3.0-alpha27 "alpha27: video fixes. The loading symbol stuck on top of a site's video player: the browser refused every request by a page to start a video by itself, even a muted one, so a player that starts muted waited for ever. Muted videos may now start by themselves (Settings > Video turns it off); a video with sound still waits for your tap. Page animations had been switched off for every site since alpha3, which can freeze a site's own loading symbol: they are back to normal, and Settings > Video > Calm pages turns the quiet mode back on. Choppy video: Settings > Video > Video formats offered to sites (Automatic keeps VP9, AV1 and H.265 away from sites when this tablet would decode them in software, so sites send H.264, which the tablet decodes with its video chip). New Menu > Diagnostics > Video lab: it plays short clips of each kind on the tablet and says smooth or choppy with the pictures it dropped, and it checks the screen's frame rate, JavaScript, WebAssembly and graphics; Diagnostics also lists the tablet's real video decoders (hardware or software) and Widevine. Protected (DRM) video was refused outright; it now asks "wants to play protected video" when the tablet supports it (Settings > Video turns it off)."
NEVER ship kestrel-g-v0.3.0-alpha27 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  6b35128 ckpt 1437: AE1 alpha27 built+tested: autoplay fix PROVEN on emulator, DRM prompt proven,
  fd0f98f ckpt 1426: AE1: CAUSE OF STUCK LOADING SYMBOL PROVEN on emulator with alpha26 (muted aut
  08a5ebd ckpt 1410: AE1 research phase done (evidence in summary); re-oriented after compaction; 
  9b93232 ckpt 1409: Owner message 18 (video audit + research + implement) written into TASKS as A
  b79c4dd ckpt 1406: alpha26 SHIPPED and RECORDED: kestrel-g-v0.3.0-alpha26 (gecko code 125), run 
  5e34d92 ckpt 1404: alpha26 (code 125) gated + pushed (ckpt 1403) and DISPATCHED: android.yml run
  33c7277 ckpt 1403: gated kestrel-g-v0.3.0-alpha26 (gecko code 125) and pushed it: handoff self-t
  bc3596c ckpt 1401: AD1b-3/4 + AD1a-3 DONE: Settings cards + launch screen + Downloads queue UI v
  af1938b ckpt 1388: AD1b-3 UI polish: Settings sections are chamfered cards with dividers (like t
  7cb96e0 ckpt 1384: AD1b-2 mp4 always DONE in core: Hevc.kt (hvc1/hvcC), AudioFrames.kt (AC-3/E-A
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
