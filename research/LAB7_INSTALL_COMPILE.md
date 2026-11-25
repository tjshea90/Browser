# LAB7 - "make the install and updates take even longer, but the browser run faster" (2026-11-17, after alpha65)

Owner message (2026-11-17, while alpha65 was being gated), verbatim: "If there is a way to make the install and updates take even longer, but the browser run faster, do it."
Method: evidence first (a local build for the facts of the build, one lab run on the emulator for the costs and the differences), then change only what the numbers support. Nothing here was run on the tablet.

## 1. What a longer install can buy (two levers)
1. **Compile more of the app's Java at install.** Android 10 compiles an app while it installs (`speed-profile`, the system's own choice for installs) but ONLY the methods the profile marks HOT (H). Everything else runs in the
   interpreter until the JIT has seen it thousands of times, or the idle-and-charging dexopt gets to it. A slow in-order Cortex-A53 runs the interpreter 10-30 times slower than compiled code, so the lukewarm code
   (one tab list open, one Settings screen, one menu) is exactly what feels slow.
2. **Do the first-run work of the engine before the owner opens the app** (a warm-up launch right after an update). Only worth building if the first launch after an update is really slower (LAB7b measures it).

## 2. Facts from a local release build (scratch copy of the repo, `--offline :geckoapp:assembleRelease`, armeabi-v7a, 30-100 s per build)
- The release dex is ONE file of 3.3 MB: 2,999 classes, 18,976 method ids, **14,996 methods with a rule in the final profile when everything is listed**. R8 has shrunk GeckoView's Java, media3 and androidx hard.
- The shipped profile (alpha64/65) after R8: 4,883 methods = 2,118 HSP + 435 HP (= **2,553 hot, 17 % of the code**) + 2,330 P-only (post-start-up, listed but NOT compiled at install) and 1,845 classes. Binary profile 5,736 bytes, `.dm` 6,766 bytes:
  the control build reproduced alpha65's `.dm` BYTE FOR BYTE (builds are deterministic, the scratch copy is equivalent to the shipped build).
- The wildcard route works with AGP 8.13.2: `expandReleaseArtProfileWildcards` expands `HSPLapp/kestrel/**;->**(**)**` / `HSPL**;->**(**)**` against the classes (81,598 expanded lines for the catch-all), R8's profile rewrite maps them onto the final dex, the binary
  profile follows. Rules of one method MERGE (union of flags): the catch-all `HL**;->**(**)**` gave **2,118 HSP + 2,765 HP + 10,113 H = 14,996 methods hot**, 2,805 classes, profile 3,458 bytes, `.dm` **4,131 bytes** (smaller than today's: the
  profile is deflated and a run of consecutive hot ids compresses to nothing). So "compile everything at install" is ONE extra line in `geckoapp/src/main/baseline-prof.txt`, and the file the updater fetches gets smaller, not bigger.
- Variants built and measured by count only: `exercised` (every method line gets H: P -> HP, S -> HSP; 4,880 hot, `.dm` 7,113 bytes) and `all` (the wildcard above).
- Why the flags stay: the S / P flags also decide the ORDER of the compiled code in the oat file (start-up methods first, then post-start-up, then the rest; art OatWriter), so the start-up path stays dense in the file and a cold start pages in as little as before.
  That is why the catch-all is `H` only and not `HSP`, and why P-only lines must become HP, not HSP.

## 3. What the lab run measures (tools/emu/aotlab.py `--phases update,breadth`, emu-lab.yml suite `aot`; android.yml `app=lab lab_suite=aot`)
Three RELEASE builds for x86_64 that differ ONLY in the profile rules (tools/emu/profvariant.py): `base` (shipped), `exercised`, `all`. Variants: `verify` (nothing compiled), `sp-current`, `sp-exercised`, `sp-all`, `speed` (everything, the ceiling).
Per variant and round: compile seconds, compiled code and the files in the oat folder, 4 timed starts (2 warm, 2 after a dropped file cache) = `am start -W` TotalTime plus the host-clock time until the lab page reports its own load (the release build writes no
Health lines), and one memory snapshot (PSS of the app process and of all processes). Decision rule for shipping the catch-all (`sp-all`): compile time on the emulator under about 40 s (the tablet's 4 x A53 are slower by an estimated 2-3 x on 2 emulator vCPUs),
no start-up regression against `sp-current` beyond the +-10 % noise, PSS not higher, oat folder under about 25 MB.
LAB7b: two DEBUG builds of the same code (the second with the next versionCode and extension version = what an update looks like to the engine): install the old build, launch twice, `adb install -r` the new one, then three launches (scenario `plain`),
and the same with one extra warm-up launch right after the update (scenario `warmup`); Health lines give engine ready and page loaded per launch; Gecko's start-up cache files and compatibility.ini are listed before and after.
If the first launch after the update is not clearly slower than the steady state (more than 15 %), a warm-up job is not built.

## 4. Results
(filled in when the lab run is read: see research/LAB7_RESULTS.md)
