# CHECKPOINT 2004 — read me first, then TASKS.md

**Written:** 2026-11-14T04:22:48Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `a970c01` (this checkpoint is the commit after it)

## Just done
gated kestrel-g-v0.3.0-alpha35 (gecko code 136) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref ccr-a57c163e-mtq8js, inputs {"app":"gecko","full_build":"true","note":"alpha35 (includes alpha34): 1) The small button in the top right corner is gone. Hold your finger (about half a second) on the top right corner of the page - an invisible square about 12 mm wide - and the navigation panel slides down; hold the same corner again and it slides back up. A quick tap or a drag there still goes to the page. 2) The mini keyboard now learns from you: the words you keep, the guess you pick from the strip over its first guess (counts three times) and the words you type. Over time your own words and your own word pairs come out first. It stays on the tablet (a small file in the app's storage); Settings > Privacy and security > Forget what the keyboard learned wipes it. Nothing is learned in private tabs or in password fields. 3) If a text field of a web page has the cursor (a site's search box, a form, a text area), the mini keyboard now types into THAT field instead of searching Google: letters, glided words, picking a guess, Backspace, and Go (which presses Enter: a search box submits). The system keyboard stays down while the mini keyboard is up, and the bar says type into the page with a small page tag. Passwords show as dots. With no field open the bar works as before. Menu > Diagnostics has a new line Mini keyboard in page fields, and the Glide typing line now says what it learned: please paste both after you have used them. Tested on the emulator with real touches and a test page; NOT yet tried on your tablet (your Gboard and real sites may behave differently)."}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag kestrel-g-v0.3.0-alpha35), then:  bash tools/record-release.sh kestrel-g-v0.3.0-alpha35 "alpha35 (includes alpha34): 1) The small button in the top right corner is gone. Hold your finger (about half a second) on the top right corner of the page - an invisible square about 12 mm wide - and the navigation panel slides down; hold the same corner again and it slides back up. A quick tap or a drag there still goes to the page. 2) The mini keyboard now learns from you: the words you keep, the guess you pick from the strip over its first guess (counts three times) and the words you type. Over time your own words and your own word pairs come out first. It stays on the tablet (a small file in the app's storage); Settings > Privacy and security > Forget what the keyboard learned wipes it. Nothing is learned in private tabs or in password fields. 3) If a text field of a web page has the cursor (a site's search box, a form, a text area), the mini keyboard now types into THAT field instead of searching Google: letters, glided words, picking a guess, Backspace, and Go (which presses Enter: a search box submits). The system keyboard stays down while the mini keyboard is up, and the bar says type into the page with a small page tag. Passwords show as dots. With no field open the bar works as before. Menu > Diagnostics has a new line Mini keyboard in page fields, and the Glide typing line now says what it learned: please paste both after you have used them. Tested on the emulator with real touches and a test page; NOT yet tried on your tablet (your Gboard and real sites may behave differently)."
NEVER ship kestrel-g-v0.3.0-alpha35 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  90d1fd1 ckpt 2002: version bump 136 / 0.3.0-alpha35 for the release of AU1-AU3
  21e5616 ckpt 1998: AU1+AU2+AU3 verified on the emulator (corner long press, page-field typing: l
  95be9ed ckpt 1994: AU3 verified on the JVM side: core 876/877 (the one failure is a download tim
  85e6e7a ckpt 1990: AU3 code: TextEdit (core, 7 tests), PageField interface + MiniNav field mode 
  4ad5eb4 ckpt 1979: AU3 research written (research/MINI_NAV.md): GeckoView 160 input path read fr
  543c7ef ckpt 1978: AU2 learning keyboard: PersonalModel tests (11) + MiniKeyboard typedPairs tes
  a2b40ea ckpt 1965: alpha34 SHIPPED AND RECORDED (run 43549663530 green, Release kestrel-g-v0.3.0
  937563e ckpt 1958: Owner message 35 written into TASKS (AU1 invisible long-press corner instead 
  7898087 ckpt 1954: alpha34 (gecko code 135) DISPATCHED: android.yml run 43549663530 (run #85, st
  0a7c905 ckpt 1953: gated kestrel-g-v0.3.0-alpha34 (gecko code 135) and pushed it: handoff self-t
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
