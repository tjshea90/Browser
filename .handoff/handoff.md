---
date: 2026-11-11T06:52:08.348Z
project: Browser
session: 6078adfc
schema_version: 1
next_steps_count: 9
has_blockers: false
keywords: kestrel browser, download manager, N6, geckoapp, alpha4, bottom dock, cere design, resume
headline: Kestrel Browser (public repo tjshea90/Browser, branch ccr-a57c163e-mtq8js). Owner request 3 (INBOX 2026-11-11T05:26Z = T
---

# Session Handoff Snapshot
> **Generated:** 11/11/2026, 6:52:08 AM

## 📌 Current State & Next Steps
* **Status:** N1-N5 and part of N7 DONE and RELEASED as alpha4 (verified on emulator only, not real tablet). NOT STARTED: N6 download manager (design exists in research/DOWNLOADS.md, no code yet). Open: N7 remainder; owner decisions SEC-1, LIC-1, UI-2, UI-3. Why N6 next: owner wants built-in downloads with pause/resume and parallel speed, system file-manager intent rejected.
* **Next Action:** bash tools/resume.sh; bash tools/claim.sh N6

### Remaining Queue
- [ ] Read research/DOWNLOADS.md
- [ ] Build pure-Kotlin download engine in core/ (package app.kestrel.core.download): adaptive parallel Range segments, strong-validator ETag/If-Range checks, pause = stop+fsync+checkpoint, ports Fetcher/Sink/Store/Clock/NetGate; JVM tests vs fake Range server
- [ ] Android shell in app/: SQLite store, MediaStore.Downloads sink with pending row, UrlConnectionFetcher, DownloadService (dataSync foreground + notification), DownloadController; hook geckoapp BrowserActivity.download()/startDownload and net/Downloads.kt
- [ ] Downloads screen (menu > Downloads; big pause/resume/cancel/open/share/delete buttons, clear finished) replacing DownloadManager file-manager intent in BrowserActivity.onMenuPick M.DOWNLOADS
- [ ] Test on emulator (adb /opt/android-sdk/platform-tools/adb; emulator-5554 may be running; write tiny Range-capable server, python http.server lacks Range); test site http://10.0.2.2:8000
- [ ] Bump geckoapp versionCode 104 / 0.2.0-alpha5; bash ship.sh gecko 'note'; dispatch android.yml; tools/record-release.sh only after green; give owner Release link on own line
- [ ] Then N7 remainder: Settings restyle, launcher icon check, tab-grid swipe-to-close, toolbar auto-hide idea, blocking engine v2 D1-D4, Firefox speed leftovers
- [ ] Delete trigger trig_01HjJLZki7r2XgUqZazC4oup when everything shipped. ckpt (bash tools/ckpt.sh 'did' 'next') after every step.

## 🛠️ Modified Files Delta
* app/src/main/res/values/colors.xml + themes.xml: dark CERE palette/theme, values-night removed
* app/src/main/res/drawable/ic_*.xml: ~28 stroke icons added (home plus mask download pause play stop check share star bookmarks history settings desktop pulse power folder file tabs update); ic_launcher_foreground.xml yellow shield on black
* app/.../ui/Chamfer.kt (new): ChamferDrawable, chamferSurface
* app/.../ui/Ui.kt: Pal private skin violet, chamfer buttons, TabsButton, ShieldChip.setPal, ProgressLine.setColor
* app/.../ui/BottomDock.kt (new): dock + DotImageButton
* app/.../ui/HomeView.kt (new): Home screen
* app/.../ui/Sheet.kt (new): SheetHost, menuGrid
* app/.../ui/TabGrid.kt (new): TabGrid, ThumbView
* app/.../ui/ThumbStore.kt (new): thumbnail cache
* app/.../ui/KeyboardProbe.kt (new): keyboard top detection
* app/.../ui/UpdatePanel.kt (new)
* app/.../ui/Panels.kt: SuggestionList restyle, MenuItemSpec icon+accent
* app/.../ui/NewTabPage.kt: LetterAvatar chamfered tile
* app/.../ui/ShieldsPanel.kt: sheet mode
* app/.../web/Tab.kt: showHome, thumb, thumbAt
* app/.../Flavor.kt: updateTagPrefix, title
* app/.../data/Prefs.kt: autoUpdateCheck
* app/.../update/UpdateManager.kt + InstallResultReceiver.kt (new)
* core/.../update/UpdateLogic.kt + UpdateLogicTest.kt (new, 10 tests)
* app/src/test/.../update/UpdateManagerTest.kt (new)
* geckoapp/.../BrowserActivity.kt: major - dock, Home, sheet menu, typing/keyboard lift, tab grid, skins, thumbnails, updater wiring, CLOSE_ALL id fix
* geckoapp/.../GeckoApp.kt: Flavor
* geckoapp/src/main/AndroidManifest.xml: REQUEST_INSTALL_PACKAGES + receiver
* geckoapp/build.gradle.kts: versionCode 103 / 0.2.0-alpha4
* extension manifest version 1.0.103
* docs/DESIGN.md (new)
* docs/ARCHITECTURE.md: UI shell + updater section
* CLAUDE.md: hotspot rule amended (daily conditional release check)
* TASKS.md: N1-N5 ticked with evidence
* BUILDLOG.md: alpha4 line

## 🚫 Failed Approaches (DO NOT RETRY)
* Approach: getWindowVisibleDisplayFrame under adjustNothing on Android 10 -> Result: never reports keyboard -> Lesson: use KeyboardProbe popup window.
* Approach: building tab-grid cards inside onSizeChanged -> Result: cards unmeasured/invisible -> Lesson: post { refresh() } after layout.
* Approach: clearUrlFocus() parking focus on root -> Result: default focus highlight grey tint over whole UI -> Lesson: root.defaultFocusHighlightEnabled=false.
* Approach: screencap of FLAG_SECURE window -> Result: empty -> Lesson: debug property override debug.kestrel.nosecure.
* Approach: two overlapping Gradle runs -> Result: stale Kotlin incremental state, :app tests could not see MainActivity -> Lesson: ./gradlew :app:compileDebugKotlin --rerun.
* Approach: foreground sleep -> Result: blocked by harness -> Lesson: Bash run_in_background with until loop.
* Approach: per-module resource override in app/res and geckoapp shared source set -> Result: not possible -> Lesson: Flavor object (Flavor.updateTagPrefix, Flavor.title).
* Approach: Robolectric with default SDK -> Result: target SDK 36 above Robolectric max -> Lesson: @Config(sdk=[29]).
* Approach: menu ids -> Result: REFRESH and CLOSE_ALL both 14, Close all never worked -> Lesson: fixed to 16; check id uniqueness.

## 🔑 Crucial Context & Implicit Rules
* Kotlin; framework Views only; NO AppCompat/Compose/Material/Room/WorkManager/coroutines/Glide; never add GMS
* minSdk 29, targetSdk 36; GeckoView 160 armeabi-v7a (x86_64 debug via -PgeckoAbi=x86_64 for emulator)
* Gradle 8.14.3, AGP 8.13.2, JDK 21, Kotlin 2.3.10, SDK /opt/android-sdk
* No animations anywhere; Google is default search
* Hotspot rules: no background network except weekly filter-list refresh + daily conditional release check; big downloads need metered confirm
* Autosave hook commits+pushes after every tool call; run bash tools/ckpt.sh 'did' 'next' after every step
* Develop only on branch ccr-a57c163e-mtq8js; never force-push; no PRs unless asked; never push main
* Repo is PUBLIC: no keys, no owner photos/personal data, never commit APKs
* Use GitHub MCP tools; no curl to api.github.com; plugin PreToolUse hook blocks curl/wget and some rm patterns
* Emulator AVD kestrel29x64 (no KVM, slow) with emu.sh in session scratchpad; test site http://10.0.2.2:8000
* Test commands: ./gradlew :core:test :app:testDebugUnitTest :geckoapp:compileDebugKotlin
* Owner rule: respond to every prompt; re-send a denied tool call once and say so

## Key Decisions
- Decision: CERE design system in docs/DESIGN.md (dark only, ink black, one accent cere-yellow F6C026, violet B794FF private mode, 45-degree chamfered corners, stroke icons, no animations, 48dp+ targets) - Reason: fresh big instant UI for owner.
- Decision: BottomDock (ui/BottomDock.kt) two rows portrait, one row at >=760dp; address row hidden on Home - Reason: owner reachability (top bar rejected).
- Decision: window uses adjustNothing while address field focused so GeckoView never resizes - Reason: root cause of typing freeze; ui/KeyboardProbe finds keyboard top (Android 11+ IME insets; Android 10 0-width adjustResize popup window); typingTick polls 100ms and lifts dock via translationY.
- Decision: Home = new-tab page (ui/HomeView.kt); Home button sets Tab.showHome overlay keeping page session alive - Reason: Back returns.
- Decision: private and regular tabs share one list but never share UI - applySkin recolours; FLAG_SECURE while private on screen; private previews RAM-only (ui/ThumbStore) - Reason: owner request item 4.
- Decision: tab previews via GeckoView.capturePixels on grid open and 2s after load, 240px RGB_565, WebP cache for regular tabs only.
- Decision: bottom sheets (ui/Sheet.kt) for menu, Shields, Updates - Reason: thumb reach.
- Decision: in-app updater (core/update/UpdateLogic + app/update/UpdateManager): Releases API with ETag, checked once a day on front (CLAUDE.md hotspot rule amended), download only after user tap + metered confirm, Range resume, SHA-256 from release body, PackageInstaller; Android enforces same signing cert (signing/CERT_SHA256).
- Decision: release flow unchanged - ship.sh, dispatch android.yml, record-release.sh only after green; GitHub builds and signs APK, never commit APKs - Reason: owner rule 'Claude codes the app, GitHub makes the apk'.

## 🙋 User Contribution (human-authored)
Owner wrote the requests (INBOX.md) and the standing rules (GitHub Actions builds APKs, checkpoint system, answer every prompt, resend denied calls). Owner tested alpha3 on the real tablet ("So far I like alpha 3"). No corrections of Claude's approach in this segment.

## 🙋 User Decisions (made by the human)
- **Whole navigation bar moves to the bottom (request item 2)**
  - Reason: Reachability with one hand on a large tablet.
  - Rejected: Top bar
- **Home screen is pure black with one large stylized search/URL field (request item 3)**
  - Reason: Owner request.
- **Private tabs separate from regular tabs (request item 4)**
  - Reason: Owner request for privacy separation.
- **Download manager built into the app (request item 6)**
  - Reason: Owner wants in-app downloads with pause/resume and speed.
  - Rejected: System file manager intent

## Summary
Kestrel Browser (public repo tjshea90/Browser, branch ccr-a57c163e-mtq8js). Owner request 3 (INBOX 2026-11-11T05:26Z = TASKS N1-N7). Done+released: N1-N5 and part of N7 as alpha4 (tag kestrel-g-v0.2.0-alpha4, Gecko versionCode 103, Actions run 42973747344 green, BUILDLOG.md, ckpt 278). Verified only on Android 10 x86_64 emulator (600x973 @170dpi, portrait+landscape), NOT on real tablet. 212 JVM/Robolectric tests green. Open: N6 download manager (design research/DOWNLOADS.md), N7 remainder, owner decisions SEC-1 (private signing key), LIC-1 (licence, YouTube switch), UI-2, UI-3. Cold-resume state lives in CLAUDE.md, CHECKPOINT.md, TASKS.md, tools/resume.sh; heartbeat trigger trig_01HjJLZki7r2XgUqZazC4oup every 2h.

---
*A short hint surfaces on session start; full context loads only if your next prompt matches a keyword above, or via manual `/handoff-resume`. The same record is available as a sibling `.json` file for external tools.*