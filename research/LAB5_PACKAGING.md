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

## Measured: the baseline profile on the release build and compile at install (aot run 44074456934, x86 emulator, Android 10)
Method (tools/emu/aotlab.py, release phase): install the R8-minified release build for the emulator's ABI; `am broadcast ... androidx.profileinstaller.action.INSTALL_PROFILE` (does what the library does itself 5 s after the first frame);
`cmd package compile -f -m speed-profile` vs `-m verify` of the SAME APK; `am start -W` TotalTime (time to the first drawn frame), 4 starts per cell, alternating warm / dropped file cache, two rounds in swapped order.
- The embedded profile works end to end: ProfileInstaller wrote 5,728 bytes into `/data/misc/profiles/cur/0/<pkg>/primary.prof`; the compile answered `status=speed-profile`; compiled code 3,753 KB (verify: 97 KB) = the ~6,700 methods of the R8-rewritten rules.
- Start (release build): warm 1898 -> 1671 ms (-12 %; verify [1697, 2062, 1770, 2027], speed-profile [1587, 1755, 1577, 1902]); after a dropped file cache 1884 -> 1884 ms (verify [1965, 1907, 1862, 1860], speed-profile [1926, 2083, 1835, 1841]).
  The spread (+-10 %) is as large as the effect: read it as "the direction is right and it is not a loss", not as a promise. The emulator's x86 cores run interpreted code much faster than the tablet's in-order A53s, and a cold start is dominated by paging libxul in.
- The DEBUG build cannot show this: a debuggable app only ever gets `quicken` (the 3-mode A/B of the same run reports `status=quicken` for speed-profile and speed): that phase is now off by default (`--debug-ab`).
- COMPILE AT INSTALL works on Android 10: a PackageInstaller session with `base.apk` + `base.dm` (`pm install-create / install-write / install-commit`, what Google Play does) compiles with the profile WHILE INSTALLING:
  plain session: `[status=speed-profile] [reason=install]` but only 158 KB of code (this Android asks for speed-profile at install and, with no profile, has nothing to compile); with AGP's `outputs/apk/release/baselineProfiles/1/geckoapp-release.dm`:
  `[reason=install-dm]`, 3,753 KB compiled, install 3.8 s vs 3.5 s on the fast emulator; the `.../baselineProfiles/0/...dm` is accepted but ignored (158 KB: the other profile format). So on API 29 the `1` file is the right one.
- What it would give: every update compiled the moment it is installed, instead of interpreter-bound until Android's idle-and-charging dexopt (which an owner who updates every day or two meets only some nights). NOT built: the in-app updater
  (app/.../update/UpdateManager.kt install(): one session, `kestrel-update.apk`) would have to (1) fetch the small `.dm` (7 KB, a release asset the workflow would publish next to the APK), (2) write it into the session as `kestrel-update.dm`,
  (3) on INSTALL_FAILED_BAD_DEX_METADATA retry the session WITHOUT it, (4) be tested on the tablet (Android Go may not honour it: then nothing is gained and nothing lost). It only helps updates FROM the build that carries the new updater, so it
  needs its own release; a blind change to the only delivery channel was judged too risky for an unattended run. Recorded as TASKS LAB6 for the owner's go.

## LAB6 measured (aot run 44118008035, the installer variants with the updater's own names) and built
Session installs with `pm install-create / install-write / install-commit`, named exactly as the updater names its files (`kestrel-update.apk` + `kestrel-update.dm`), x86 emulator, Android 10:
| Install | result | compiled code |
| :- | :- | -: |
| plain, no file | Success, `[reason=install]` | 158 KB (nothing to compile) |
| with AGP's `baselineProfiles/1/*.dm`, fresh install | Success, `[reason=install-dm]`, 3.0 s | 3,753 KB |
| the same as an UPDATE over the installed copy (what the updater does) | Success, `[reason=install-dm]`, 2.9 s | 3,753 KB |
| a `.dm` that is not a zip, as an update | `Failure [INSTALL_FAILED_BAD_DEX_METADATA: Failed parse during installPackageLI: Error opening .../base.dm: Invalid file]`; the installed copy stays intact | - |
| a zip with a garbage `primary.prof` | Success, profile ignored | 158 KB |
| `baselineProfiles/0/*.dm` (the other format) | Success, ignored | 158 KB |
So: the pairing by name works, an update is compiled while it installs, a wrong file is either ignored or refused with a text that names the dex metadata and leaves the installed copy alone.
Built (alpha65): the release script publishes `<apk name>.dm` (AGP's `baselineProfiles/1`, checked: small zip with `primary.prof`), `UpdateLogic.choose` puts the asset and GitHub's digest of it into the offer, `UpdateManager.ensureDm` fetches it after the APK is verified
(8 s to connect, 10 s to read, 1 MB at most, digest and zip checked, kept next to the APK), `install()` writes it into the session on Android 9 to 11 only, and when the installer answers with `DEX_METADATA` in its text
`InstallResultReceiver` installs ONCE more without the file and no file is attached for two weeks. No file is attached when our own copy has to be dropped early for room (that install stays what it was). Tests: core DexMetadataTest (8), app DexMetadataFetchTest (15).
It only helps updates FROM a build that carries this updater: alpha64 -> alpha65 is still compiled by the idle dexopt; alpha65 -> alpha66 is the first compiled at install. NOT run on the tablet (Android Go may not honour the file: then nothing is gained, nothing is lost).

## Ranked build changes (as audited)
1. `noCompress += "ja"` default ON - DONE, measured. 2. drop `libcrashtools.so` (optional, small). 3. baseline profile + verify-apk assert. 4. libs in APK (owner's call; not recommended). 5. startup profile only after 3.
