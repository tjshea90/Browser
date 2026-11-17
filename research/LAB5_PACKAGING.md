# LAB5 - what the APK / packaging does to start-up, memory and storage (2026-11-17)

Sources: a read-only audit of the GeckoView 160 AAR and the released alpha63 APK (tags: [L] read locally, [M] Mozilla source from memory/summary, [D] developer docs), and the measured pack run
(run 44053244976, `lab_suite=pack`, four builds of the SAME code, x86_64 debug APKs, KVM emulator; read the DIFFERENCES between variants, not the milliseconds; n = 4 starts per cell, 2 rounds
(round 3 of every variant failed to install: the emulator disk filled with 220 MB APKs, harmless)).

## 1. omni.ja stored instead of deflated  -> DONE (default ON from alpha64)
- Mechanism [M]: GeckoView passes `-greomni <apk>`; Gecko opens `assets/omni.ja` (14.5 MB, itself a zip of 4,005 already-deflated entries). A DEFLATED entry is inflated whole into a private
  heap buffer in EVERY process that opens it (parent + each content process); a STORED entry is mapped straight from the APK (shared, evictable file pages). Mozilla's own Fenix and
  geckoview_example use `noCompress 'ja'` for this reason (bug 1428128).
- Cost: +0.5 MB APK (14.0 -> 14.5 MB; deflate gained nothing on this data).
- Measured (pack run, B vs A): warm start `am start -W` 1442 -> 1280 ms (-11 %), engine ready 1.42 -> 1.25 s (-12 %), page loaded 2.56 -> 2.28 s (-11 %), worst UI block 918 -> 753 ms (-18 %);
  after a cold file cache -4 % / -5 % / -2 % / -1 % (same direction). Memory after the page set (PSS, n=2): total 590 -> 562 MB (-28 MB): app process 378 -> 329, content process 120 -> 102.
  The direction matches the mechanism (about 14.5 MB of private heap per opening process); the size of the effect on the 4 x A53 tablet is an estimate (inflate of 14.5 MB took 59 ms on a 2.1 GHz Xeon core; x4-6 on an A53).
- Change: `geckoapp/build.gradle.kts` `omniStored` is now ON unless `-PomniStored=false`; `tools/verify-apk.sh` asserts the entry is stored (a regression fails the build).

## 2. Native libraries in the APK (extractNativeLibs=false, `-PlibsInApk=true`)  -> NOT done (opt-in stays)
- Supported by GeckoView 160 [M] (bug 1610018; Fenix bundles use it), about 75 % confident on API 29 and untestable for every edge on the tablet.
- Measured (C, D): start-up -4..-6 % warm (inside the noise of this run), memory not lower (C 594 MB vs A 590 MB), APK +103 MB on x86_64 (+65 MB on armeabi-v7a: 95 -> 160 MB), installed footprint -73 MB,
  update free-space need unchanged (2 x APK + libs). A big download on a 5 Mbit hotspot for no start-up gain: refused. Documented opt-in only.

## 3. Native library trimming
- `libcrashtools.so` (5.0 MB raw, 2.6 MB deflated) belongs to Fenix's crash library, no reference in GeckoView / libxul / our code [L]: safe to drop, saves 2.6 MB of download and 5 MB installed, no start effect.
- `libcrashhelper.so` MUST stay (`GeckoRuntime.init` binds the service that loads it) [L].
- The `:crashhelper` PROCESS (29 MB PSS in the pack run, the same size as a bare Android process) was looked at and REFUSED: GeckoRuntime.init -> startCrashHelper() (read with javap, 160 AAR) creates the crash IPC pipes
  for the native side and calls bindService(CrashHelper, BIND_AUTO_CREATE) ignoring its result; a disabled or missing service would leave every Gecko process writing its crash request into a pipe nobody reads (a crashed
  tab could hang for the timeout instead of dying at once), and running the service inside the main process is an unsupported layout (its native side may exit the process it lives in). Kestrel never reads Mozilla's minidumps
  (no telemetry; StorageTidy prunes them), but the saving is not worth a hung crash path that no lab can fully exercise. Not done.
- Resources are tiny (res 52 KB, arsc 57 KB): locale filters save nothing. `classes.dex` and `baseline.prof` are already stored. `default.kfs` / `en.klx` stay deflated (5.5 MB saving not worth it).

## 4. Dex baseline profile (ART)  -> pipeline in LAB5c
- Today `assets/dexopt/baseline.prof` is 246 B (from androidx libraries) and `.profm` 71 B: none of OUR code or GeckoView's Java is precompiled. profileinstaller 1.4.0 is already in the dependency
  graph (ProfileInstallerInitializer runs 5 s after the first frame, writes `/data/misc/profiles/cur/0/<pkg>/primary.prof`; no effect on debuggable apps).
- On Android 10 the profile only turns into compiled code when BackgroundDexOptService runs (idle + charging, about once a day, skipped when storage is low) or `cmd package compile -f -m speed-profile`.
  `adb install-multiple app.apk app.dm` would compile at install on API 28-30 but needs the installer's cooperation: not usable for our self-update path (unverified).
- Generation recipe on a rooted API-29 emulator: install a NON-debuggable, UNMINIFIED build, `cmd package compile --reset`, drive the flows, `killall -s SIGUSR1 <pkg>`, then
  `profman --dump-classes-and-methods --profile-file=/data/misc/profiles/cur/0/<pkg>/primary.prof --apk=<path>`; keep lines starting with `[` or containing `+`... (androidx filters the same way), `sort -u`
  into `geckoapp/src/main/baseline-prof.txt`. AGP 8.2+ rewrites the rules through R8 (so collect from the unobfuscated build). The compiled profile must stay under 1.5 MB.
- The lab collects the raw material at the end of every lab run (emu-lab.yml step "Keep the ART profile of this run": `lab-out/profile/`). Gain is estimated at 20-35 % of the Java-side start-up once compiled; unmeasured here.

## Ranked build changes (as audited)
1. `noCompress += "ja"` default ON - DONE, measured. 2. drop `libcrashtools.so` (optional, small). 3. baseline profile + verify-apk assert. 4. libs in APK (owner's call; not recommended). 5. startup profile only after 3.
