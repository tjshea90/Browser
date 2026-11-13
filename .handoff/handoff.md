---
date: 2026-11-13T05:01:56.557Z
project: Browser
session: e21ddc2b
schema_version: 1
next_steps_count: 6
has_blockers: true
keywords: 
headline: Goal: Kestrel Browser (public repo tjshea90/Browser, branch ccr-a57c163e-mtq8js): light private Android browser for owne
---

# Session Handoff Snapshot
> **Generated:** 11/13/2026, 5:01:56 AM

## 🎯 High-Level Objective
* **Goal:** Goal: Kestrel Browser (public repo tjshea90/Browser, branch ccr-a57c163e-mtq8js): light private Android browser for owner's Android 10 Go tablet (MT8168B, 2 GB RAM, 5 Mbit/s phone hotspot, no Google services). Builds: Kestrel (WebView, legacy) and Kestrel G (GeckoView 160, what owner uses). This stretch: owner msgs 16-20 = apply outside review, video audit (acceleration/codecs/choppy video/stuck loading symbol + free research implemented, paid items left to owner), doraby.com showing no photos/search in Kestrel but fine in Brave, lab numbers from tablet. Core intent: a site that works in Brave must work in Kestrel; evidence from real Gecko on emulator before claiming causes; owner cannot be asked to debug blindly so build diagnostics.

## 📌 Current State & Next Steps
* **Status:** DONE: alpha28 (versionCode 127, extension 1.0.127, GitHub run 43368098105 green, recorded). AE1 video audit answered; AF1 doraby fixed (emulator-proven; jsDelivr refusal on tablet itself NOT observed); AG1 lab numbers recorded. WAITING: owner confirmation that doraby.com photos/icons and the movie player work on alpha28; Diagnostics copy if still broken; site/link of any still-choppy video. OPEN backlog: AE2 (stand-ins for blocked ad SDK ima3.js, fMP4/CMAF defragmenter, MediaSession background audio, picture-in-picture), AD1b-5 Gradle dependency verification, SHA-pinned actions after SEC-1, EX-5/7/8/12 switches awaiting tablet numbers. Heartbeat trigger trig_01LLoVKsjuTUaDn61UGP1pBN (every 2 h) exists: stand down while real work is under 60 min old; delete after 24 h owner silence with nothing safe left.
* **Blocker:** None blocking. Unverified: jsDelivr actually refused on the owner's tablet (emulator cannot download Mozilla's lists); owner must confirm alpha28 fix.
* **Next Action:** bash tools/resume.sh, read CHECKPOINT.md and TASKS.md (AF1 and AE1 are marked [~]).

### Remaining Queue
- [ ] Await owner reply on alpha28: doraby.com photos/icons and the movie player; if still broken ask for Menu>Diagnostics>Copy and read the Page report (FAILED lines, TRACKING_URI) and the line "Firefox's own tracking protection".
- [ ] If still broken: reproduce with tools/sitelab mirror on the emulator (fail the CDN host), then test DNS/HTTP3/ECH/cache hypotheses in research/DORABY.md section 3.
- [ ] If video still choppy: get the site/link; consider AE2 backlog (ad SDK stand-ins, fMP4/CMAF defragmenter, MediaSession background audio, picture-in-picture).
- [ ] Other backlog: AD1b-5 Gradle dependency verification metadata, SHA-pinned actions after SEC-1, EX-5/7/8/12 switches awaiting tablet numbers.
- [ ] Delete heartbeat trigger trig_01LLoVKsjuTUaDn61UGP1pBN after 24 h owner silence with nothing safe left.

## 🛠️ Modified Files Delta
* geckoapp/src/main/kotlin/app/kestrel/browser/GeckoEngine.kt: antiTracking minus CONTENT, applyVideo live prefs, extra pref read-back names, debug pref hook
* geckoapp/.../DeviceVideo.kt: new MediaCodecList + Widevine probe
* geckoapp/.../web/TabDelegates.kt: autoplay/DRM permission policy, engineBlocks log
* geckoapp/.../web/ShieldsBridge.kt: rep handler, pageReports, private-tab drop
* geckoapp/.../BrowserActivity.kt: Reload without cache menu item, pageIsPrivate, Diagnostics lines
* geckoapp/src/main/assets/kestrel-shields/{background.js,content.js,manifest.json}: page report, error reporter, extension 1.0.127
* geckoapp/src/main/assets/labs/video.html + 4 clips; assets/gecko-prefs.txt v7
* core: media/VideoPolicy, diag/PageReports, shield/EtpCategories (+tests)
* app: data/Prefs, data/BackupManager, ui/SettingsView (Video section), ui/DiagnosticsView, web/Tab (engineBlocks)
* web-tests: videolab.test.js, background.test.js (+page report), content.test.js (+error reporter)
* tools/check-gecko-prefs.sh (also checks Kotlin live pref names), tools/sitelab/*
* research/VIDEO_AUDIT.md (section 9 tablet numbers), DORABY.md, EX_RESULTS.md; TASKS.md; geckoapp/build.gradle.kts (127 / 0.3.0-alpha28)

## 🚫 Failed Approaches (DO NOT RETRY)
* Approach: first doraby mirror kept protocol-relative links → Result: Gecko fetched the real site CSS, unstyled page mimicked the owner's symptom (false lead) → Lesson: rewrite // links too, check the server log.
* Approach: tested background.js changes in a Node harness without clearTimeout → Result: decide() threw, fail-open, every background test failed → Lesson: harness must provide browser globals.
* Approach: reinstalled debug APK with unchanged extension version → Result: old extension kept, Page report empty → Lesson: bump versionCode/manifest or pm clear.
* Approach: page log started only at main_frame request → Result: cold start with a link (page began before extension ready) gave no report → Lesson: also start from first sub-resource.
* Approach: assumed STRICT includes STP → Result: unit test failed (766 has no 256) → Lesson: verify constants with javap.
* Approach: Video lab ran heavy JS loops at page load → Result: slow-script prompt on slow emulator → Lesson: run on button, yield between tests.
* Approach: lab fps counted from the playing event → Result: 24.8 fps and mostly smooth for perfect clips on the tablet → Lesson: measure first to last presented picture (fixed alpha28).
* Approach: setprop with empty value to clear a debug prop → Result: adb error, old config stayed → Lesson: set 0.
* Approach: trusted emulator screenshots right after taps → Result: stale frames → Lesson: wait and confirm with uiautomator dump.
* Approach: assumed muted-autoplay refusal was the only stuck-spinner cause → Result: ETP CONTENT blocking of player CDNs found later → Lesson: check engine-level blocking (Page report) before site logic.

## 🔑 Crucial Context & Implicit Rules
* Kotlin, framework Views only, no AppCompat/Compose/Material/Room/WorkManager/coroutines; never add GMS
* minSdk 29, targetSdk 36; GeckoView 160 armeabi-v7a; JDK 21, Gradle 8.14.3, AGP 8.13.2, Kotlin 2.3.10, SDK /opt/android-sdk
* Develop only on branch ccr-a57c163e-mtq8js; never force-push; no PRs; never push main; repo is PUBLIC: no keys, owner photos, personal data or APKs
* Run bash tools/ckpt.sh 'did' 'next' after every step; write each owner request into TASKS.md BEFORE code; autosave hook commits and pushes after every tool call
* Release flow: bash ship.sh gecko note, then actions_run_trigger android.yml (app=gecko, full_build=true), wait for tag + green run, then record-release.sh; never add a push: workflow trigger
* Extension manifest version = 1.0.<versionCode>; tools/check-gecko-prefs.sh checks pref names (gecko-prefs.txt and Kotlin live names)
* Tests: ./gradlew :core:test :app:testDebugUnitTest :geckoapp:compileDebugKotlin; cd web-tests && npm test (123 tests); bash tools/test_handoff.sh
* Emulator AVD kestrel29x64: no KVM, slow, screenshots can be stale, opens only http pages; mirror sites with tools/sitelab; read Diagnostics text via uiautomator dump
* Debug-only props via DebugProps (null in release): debug.kestrel.pref1..3, etpcontent, moz_log, nosecure
* Foreground sleep is blocked: use timeout N bash -c 'until ...'
* Owner rules: answer every prompt and re-list open items; if a tool call is denied re-send once and say so
* No animations in the app UI; Google is the default search; hotspot rules: no background network except daily list/release check

## Key Decisions
- Decision: Muted autoplay allowed (inaudible permission ALLOW, audible DENY) — Reason: GeckoView asks the app for every autoplay (media.geckoview.autoplay.request); refusing muted starts left players' loading symbols stuck; proven on emulator.
- Decision: Page-side reduced-motion + GIF-once no longer fixed on: Settings>Video>Calm pages default off via USER-branch prefs — Reason: frozen site spinners.
- Decision: Codec offer rule: core/media/VideoPolicy + DeviceVideo MediaCodecList probe cached per Build.FINGERPRINT + GeckoEngine.applyVideo (USER branch, clear = engine default) for MSE VP9/AV1/HEVC; Auto leaves hardware codecs on; tablet reports hardware H.264/H.265/VP9.
- Decision: DRM: Widevine via MediaDrm; ask prompt per site (remembered per run, never in private tabs); engine default media.eme.require-app-approval=true means the app must approve.
- Decision: ROOT CAUSE doraby + player CDNs: AntiTracking.STRICT (766, includes CONTENT = Mozilla/Disconnect Content list: jsDelivr, unpkg, JW Player, Brightcove, Kaltura, Mux, Vimeo) blocked CDNs and players (error TRACKING_URI); GeckoView has no Mozilla allow-lists. Fix: STRICT minus CONTENT (750); core/shield/EtpCategories pins the numbers; debug prop debug.kestrel.etpcontent=1 restores the old set for comparison.
- Decision: Page report: extension background (webRequest errors, HTTP>=400, content.js script/load errors) pushes message rep 4 s after the page settles, max 3 per page; app PageReports keeps last 6 pages; never for private tabs (ShieldsBridge.pageIsPrivate); Diagnostics also lists Firefox-own tracking blocks (Tab.engineBlocks) — Reason: make 'site half empty' diagnosable from the owner's tablet.
- Decision: Menu Reload without cache (LOAD_FLAGS_BYPASS_CACHE) — Reason: escape hatch for stale or damaged cache entries.
- Decision: Video lab bundled with 4 synthetic ffmpeg clips (assets/labs) measures dropped frames per codec on the real tablet; resource:// playback works.
- Decision: tools/sitelab/mirror.js + serve.py mirror a page for real-Gecko emulator tests (one host can be made to fail); debug-only debug.kestrel.pref1..3 set engine prefs at start.
- Decision: Extension manifest version must equal 1.0.<versionCode> (test_handoff gate) and the extension only updates when its version changes: bump or pm clear before emulator tests.

## 🙋 User Contribution (human-authored)
Ran Video lab, Tap lab and Decode lab on the tablet and sent text + screenshots; reported doraby.com showing no photos/search in Kestrel while Brave loaded it; requested a video audit with free codec/engine research implemented.

## 🙋 User Decisions (made by the human)
- **Paid/proprietary codecs, engines or software will be handled by the owner (contact the companies, approve, pay)**
  - Reason: Stated in owner message 18
  - Rejected: none stated

## Summary
Session shipped Kestrel G alpha25 (outside-review rows), alpha26 (link warm-up, download queue controls, MP4 for H.265/AC-3/E-AC-3/MP3, procedural cosmetics), alpha27 (video audit: muted-autoplay fix, codec offer rule, Video lab, DRM ask), alpha28 (Firefox tracking-protection CONTENT category removed: doraby.com photos/icons + video players fixed; Page report; Reload without cache). All built/signed by GitHub Actions, recorded in BUILDLOG.md. Owner's tablet lab numbers recorded: hardware decode OK for H.264/VP9/H.265, 0/150 frames dropped, screen 65 fps, Widevine works, taps answered in 3 ms.

---
*A short hint surfaces on session start; full context loads only if your next prompt matches a keyword above, or via manual `/handoff-resume`. The same record is available as a sibling `.json` file for external tools.*