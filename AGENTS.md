# AGENTS.md

<!-- handoff:learnings:begin -->
## Session Learnings (auto-updated by handoff)

### Implicit Rules
- Kotlin, framework Views only, no AppCompat/Compose/Material/Room/WorkManager/coroutines; never add GMS
- minSdk 29, targetSdk 36; GeckoView 160 armeabi-v7a; JDK 21, Gradle 8.14.3, AGP 8.13.2, Kotlin 2.3.10, SDK /opt/android-sdk
- Develop only on branch ccr-a57c163e-mtq8js; never force-push; no PRs; never push main; repo is PUBLIC: no keys, owner photos, personal data or APKs
- Run bash tools/ckpt.sh 'did' 'next' after every step; write each owner request into TASKS.md BEFORE code; autosave hook commits and pushes after every tool call
- Release flow: bash ship.sh gecko note, then actions_run_trigger android.yml (app=gecko, full_build=true), wait for tag + green run, then record-release.sh; never add a push: workflow trigger
- Extension manifest version = 1.0.<versionCode>; tools/check-gecko-prefs.sh checks pref names (gecko-prefs.txt and Kotlin live names)
- Tests: ./gradlew :core:test :app:testDebugUnitTest :geckoapp:compileDebugKotlin; cd web-tests && npm test (123 tests); bash tools/test_handoff.sh
- Emulator AVD kestrel29x64: no KVM, slow, screenshots can be stale, opens only http pages; mirror sites with tools/sitelab; read Diagnostics text via uiautomator dump
- Debug-only props via DebugProps (null in release): debug.kestrel.pref1..3, etpcontent, moz_log, nosecure
- Foreground sleep is blocked: use timeout N bash -c 'until ...'
- Owner rules: answer every prompt and re-list open items; if a tool call is denied re-send once and say so
- No animations in the app UI; Google is the default search; hotspot rules: no background network except daily list/release check

### Key Decisions
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

<!-- handoff:learnings:end -->
