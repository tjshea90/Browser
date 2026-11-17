# LAB7 results: "make the install and updates take even longer, but the browser run faster" (2026-11-17)

Design and the local facts: `research/LAB7_INSTALL_COMPILE.md`. Every lab number is an x86_64 Android 10 emulator on a GitHub runner (KVM, 2 vCPUs, software GPU): read the DIFFERENCES between variants of one run, never the milliseconds. Nothing here was run on the tablet.

## Run 1 (aot lab run 44126148207, commit e40a5b70; lasted 30 minutes, the emulator's system server died in round 2 of the breadth test, `cmd: Can't find service`)

### LAB7b - is the first launch after an update slower? NO. A warm-up after the update is NOT built.
Two debug builds of the same code, the second with the next versionCode and extension version; old build launched twice (70 s each), `adb install -r` of the new one (= an update), then three launches. n = 2 rounds per row (medians).
| Scenario | Launch | am start -W ms | engine ready s | page loaded s | worst UI block ms |
| :- | :- | -: | -: | -: | -: |
| plain | old build, 1st launch ever | 1910 | 1.81 | 3.05 | 1412 |
| plain | old build, 2nd launch (steady state) | 1822 | 1.74 | 2.95 | 1224 |
| plain | new build, launch 1 (first after the update) | 1676 (-8 %) | 1.56 (-10 %) | 2.80 (-5 %) | 1141 (-7 %) |
| plain | new build, launch 2 | 1696 (-7 %) | 1.62 (-7 %) | 2.86 (-3 %) | 1156 (-5 %) |
| plain | new build, launch 3 | 1628 (-11 %) | 1.54 (-11 %) | 2.73 (-7 %) | 1127 (-8 %) |
| warmup | warm-up launch (not counted) | 1584 | 1.49 | 2.71 | 970 |
| warmup | new build, launch 1 (after the warm-up) | 1846 (-1 %) | 1.79 (+7 %) | 3.17 (+4 %) | 1290 (+14 %) |
| warmup | new build, launch 2 | 1865 (+0 %) | 1.77 (+6 %) | 3.06 (+0 %) | 1237 (+9 %) |
- The first launch after an update is no slower than the steady state (it is 5-10 % FASTER in this run, which is the noise of n = 2). The warm-up scenario is not better than the plain one. The decision rule (build a warm-up only if the first launch is more than 15 % slower AND the warm-up removes it) is not met by a wide margin.
- Why (read from the files the lab listed): Gecko keeps its caches across the update. `compatibility.ini` is rewritten on the first launch of the new build (`LastPlatformDir` = the NEW install path, `LastVersion` unchanged = the engine build ID), `extensions.json` is rewritten (the built-in extension's new version, 1747 -> 2735 bytes), the start-up caches (`startupCache.8`, `scriptCache.bin`, `scriptCache-child.bin`, 0.2-3.4 MB) stay and are refreshed while the app runs: they are already complete 20 s after the page loaded and are rewritten as `scriptCache-current.bin` during later runs. So the engine does not start from nothing after an update, and a headless launch would only repeat what the first user launch does.
- The update itself (`adb install -r` of the debug build) took 1.6 s on the emulator.

### LAB7a - how much of the app does Android compile at install, and what does it cost? (round 1; round 2 partly lost)
Four release builds (x86_64) that differ only in the profile rules; per variant: `cmd package compile --reset`, the embedded profile put in place (INSTALL_PROFILE), `cmd package compile -f -m MODE`, 4 timed starts (2 warm, 2 after a dropped file cache), one memory snapshot. Percentages against `sp-current` (what alpha65 ships).
| Variant | status | compile s | compiled code MB | files in oat/ | start -W warm / cold ms | page loaded warm / cold ms | PSS app / all MB |
| :- | :- | -: | -: | :- | -: | -: | -: |
| verify | verify | 0.4 | 0.1 | odex 72 KB, vdex 24 KB | 1076 / 1290 | 2467 / 2598 | 163 / 290 |
| sp-current (2553 of 14996 methods hot) | speed-profile | 1.3 | 3.7 | art 668 KB, odex 3069 KB, vdex 24 KB | 1027 / 1186 | 2485 / 2903 | 176 / 310 |
| sp-exercised (4880 hot) | speed-profile | 1.5 | 6.0 | art 672 KB, odex 5411 KB | 1105 / 1103 | 2314 / 2769 | 175 / 309 |
| sp-all (14996 hot: one wildcard rule) | speed-profile | 2.2 | 12.9 | art 936 KB, odex 12252 KB | 1039 / 1268 | 2266 / 2742 | 186 / 323 |
| speed (the ceiling) | speed | 2.1 | 12.0 | odex 12250 KB | 986 / 1110 | 2384 / 2520 | 178 / 303 |
- COST: compiling the WHOLE app takes 2.2 s on the emulator (1.3 s for today's profile): even if the tablet's cores are 10 times slower that is about 20 s on top of an install. Compiled code 13 MB (3.7 MB today), plus an app image `base.art` of 0.9 MB. `.dm` for the updater: 4.3 KB (today 6.9 KB).
- The profile classes also feed an APP IMAGE (`base.art`): `speed-profile` writes one, `verify` and `speed` do not. The catch-all adds a class line for every class (2805 instead of 1845) and the image grows from 668 to 936 KB: more classes are ready at start-up (the image is mapped, not computed).
- Start times and memory: inside the noise. n = 2 starts per cell; `am start -W` varies +-10 %. The two PSS snapshots of `sp-all` (round 1 and 2) were 335 and 311 MB: the snapshot noise (24 MB) is larger than every difference between variants. So this run proves "no big regression" and nothing finer; the confirmation run (below) takes six rounds, links opened in the running app and memory by kind.
- Mechanism that is NOT measured by start-up time: Android compiled only 2553 of the app's 14996 methods at install. Every other method (the Settings screens, tab list, menus, downloads, anything the lab's flows ran but only marked "post-start-up") runs in the interpreter until the JIT has seen it thousands of times or the idle-and-charging dexopt gets to it: on in-order Cortex-A53 cores that is 10-30 times slower than compiled code. The catch-all makes all of it compiled when the update finishes installing.

## Decision (so far)
- Ship the catch-all: `geckoapp/src/main/baseline-prof.txt` ends with `HL**;->**(**)**` (committed, with the gate `tools/check-profile-coverage.sh`, the source guard `OwnerRulesTest.lab7WholeAppIsCompiledAtInstall`, and `tools/profile-to-baseline.py` keeping the rule). It is the only change that the numbers support at a negligible cost; it needs the confirmation run below to rule out a memory or start-up regression, and it ships as alpha67 (alpha66 belongs to the lane that links the Swoop keyboard).
- Do NOT build the post-update warm-up (no penalty to remove).
- Update path: the updater's `.dm` fetch already handles any size up to 1 MB; the new file is smaller. alpha65 -> alpha66 is the first update compiled at install; alpha66 -> alpha67 is the first compiled WHOLE.

## Run 2 (confirmation; android.yml run 44137227924, `lab_minutes=breadth`): sp-current vs sp-all, 6 rounds, 4 starts per cache state, 4 links opened in the running app, 2 memory snapshots with `dumpsys meminfo` by kind
(filled in when the run is read)
