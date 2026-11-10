# Journal (append-only; one line per checkpoint — written by scripts/checkpoint.sh)

- 2026-11-10 Phase 0 recon: toolchain = JDK21, Gradle 8.14.3, AGP 8.13.2, Kotlin 2.3.10, SDK platform 36, no emulator/KVM.
- 2026-11-10 Inputs: hardware_.txt (MT8168B / Android 10 Go / 128 MB heap class / 600x973@170dpi); user added: tablet uses a phone hotspot 100% of the time.
- 2026-11-10T22:54:56Z [L2] Phase 1 done: ladder checkpoint system, hooks, handoff docs, hardware+hotspot constraints
- 2026-11-10T23:00:18Z [L2] Phase 2 done: Gradle skeleton (core/app/tools), R8 release APK builds + audited, verify.sh gate
- 2026-11-10T23:20:17Z [L1] core filter engine: parser, host table, token+4-gram index, matcher, cosmetic, codec, seed; 97+ tests incl. 60k-case oracle; 1.7us/req @140k rules
- 2026-11-10T23:22:50Z [L1] oracle test strengthened with overlapping URL words; 7/7 matcher mutants killed
- 2026-11-10T23:27:40Z [L1] real EasyList/EasyPrivacy/uBO/PGL snapshot compiled (4.3MB, 102k host + 21k pattern rules), $popup support, include resolution, quote-aware cosmetic sanitizer, real-list regression test (no false positives)
- 2026-11-10T23:29:53Z [L2] Phase 3 done: core engine complete + real-list snapshot; 114 tests, oracle+mutation validated, no false positives on major sites
- 2026-11-10T23:40:23Z [L1] app: manifest, themes, icons, Prefs, Engine, Shields, Interceptor, WebView factory/clients (compiles clean); JsEscape fix
- 2026-11-10T23:50:02Z [L1] app UI complete: BrowserActivity (tabs, hibernation, session, navigation, panels, permissions, dialogs, fullscreen, downloads), toolbar, NTP, error screens, settings, shields panel; verify full PASS (lint clean, 2.3MB APK)
- 2026-11-10T23:57:18Z [L1] 40 Robolectric app tests + 117 core tests green; fixed: JsEscape doubled slash, PING in ambiguous mask (blocked Stripe etc), live-WebView limit not enforced at creation
