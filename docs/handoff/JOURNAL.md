# Journal (append-only; one line per checkpoint — written by scripts/checkpoint.sh)

- 2026-11-10 Phase 0 recon: toolchain = JDK21, Gradle 8.14.3, AGP 8.13.2, Kotlin 2.3.10, SDK platform 36, no emulator/KVM.
- 2026-11-10 Inputs: hardware_.txt (MT8168B / Android 10 Go / 128 MB heap class / 600x973@170dpi); user added: tablet uses a phone hotspot 100% of the time.
- 2026-11-10T22:54:56Z [L2] Phase 1 done: ladder checkpoint system, hooks, handoff docs, hardware+hotspot constraints
- 2026-11-10T23:00:18Z [L2] Phase 2 done: Gradle skeleton (core/app/tools), R8 release APK builds + audited, verify.sh gate
- 2026-11-10T23:20:17Z [L1] core filter engine: parser, host table, token+4-gram index, matcher, cosmetic, codec, seed; 97+ tests incl. 60k-case oracle; 1.7us/req @140k rules
