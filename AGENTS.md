# AGENTS.md

<!-- handoff:learnings:begin -->
## Session Learnings (auto-updated by handoff)

### Implicit Rules
- Kotlin; framework Views only; NO AppCompat/Compose/Material/Room/WorkManager/coroutines/Glide; never add GMS
- minSdk 29, targetSdk 36; GeckoView 160 armeabi-v7a (x86_64 debug via -PgeckoAbi=x86_64 for emulator)
- Gradle 8.14.3, AGP 8.13.2, JDK 21, Kotlin 2.3.10, SDK /opt/android-sdk
- No animations anywhere; Google is default search
- Hotspot rules: no background network except weekly filter-list refresh + daily conditional release check; big downloads need metered confirm
- Autosave hook commits+pushes after every tool call; run bash tools/ckpt.sh 'did' 'next' after every step
- Develop only on branch ccr-a57c163e-mtq8js; never force-push; no PRs unless asked; never push main
- Repo is PUBLIC: no keys, no owner photos/personal data, never commit APKs
- Use GitHub MCP tools; no curl to api.github.com; plugin PreToolUse hook blocks curl/wget and some rm patterns
- Emulator AVD kestrel29x64 (no KVM, slow) with emu.sh in session scratchpad; test site http://10.0.2.2:8000
- Test commands: ./gradlew :core:test :app:testDebugUnitTest :geckoapp:compileDebugKotlin
- Owner rule: respond to every prompt; re-send a denied tool call once and say so

### Key Decisions
- Decision: CERE design system in docs/DESIGN.md (dark only, ink black, one accent cere-yellow F6C026, violet B794FF private mode, 45-degree chamfered corners, stroke icons, no animations, 48dp+ targets) - Reason: fresh big instant UI for owner.
- Decision: BottomDock (ui/BottomDock.kt) two rows portrait, one row at >=760dp; address row hidden on Home - Reason: owner reachability (top bar rejected).
- Decision: window uses adjustNothing while address field focused so GeckoView never resizes - Reason: root cause of typing freeze; ui/KeyboardProbe finds keyboard top (Android 11+ IME insets; Android 10 0-width adjustResize popup window); typingTick polls 100ms and lifts dock via translationY.
- Decision: Home = new-tab page (ui/HomeView.kt); Home button sets Tab.showHome overlay keeping page session alive - Reason: Back returns.
- Decision: private and regular tabs share one list but never share UI - applySkin recolours; FLAG_SECURE while private on screen; private previews RAM-only (ui/ThumbStore) - Reason: owner request item 4.
- Decision: tab previews via GeckoView.capturePixels on grid open and 2s after load, 240px RGB_565, WebP cache for regular tabs only.
- Decision: bottom sheets (ui/Sheet.kt) for menu, Shields, Updates - Reason: thumb reach.
- Decision: in-app updater (core/update/UpdateLogic + app/update/UpdateManager): Releases API with ETag, checked once a day on front (CLAUDE.md hotspot rule amended), download only after user tap + metered confirm, Range resume, SHA-256 from release body, PackageInstaller; Android enforces same signing cert (signing/CERT_SHA256).
- Decision: release flow unchanged - ship.sh, dispatch android.yml, record-release.sh only after green; GitHub builds and signs APK, never commit APKs - Reason: owner rule 'Claude codes the app, GitHub makes the apk'.

<!-- handoff:learnings:end -->
