# LAB5 results: the "ultra fast, snappy, lightweight" pass (2026-11-17, release alpha64)

Owner message (2026-11-17T06:42Z): use as much time and usage as needed to improve the browser from what the emulated runs taught; run more emulations if they help; ultra fast, snappy, lightweight; release after research and coding.
Method (evidence first): four read-only audits (research/LAB5_*.md) -> lab suites that answer ONE question each (pack, sweep, aot, daily) -> change only what a measurement or a stack sample supports, each with a test.
Every lab number is an x86 emulator with a software GPU and a fast link: read the DIFFERENCES between variants of one run, never the milliseconds, and nothing here was run on the tablet.

## Changed in alpha64 (and the proof)
| # | Change | Evidence | Test |
| -: | :- | :- | :- |
| 1 | `assets/omni.ja` stored uncompressed (mapped from the APK, not inflated into every engine process) | pack run 44053244976, four builds of the same code: warm start 1442 -> 1280 ms (-11 %), engine ready 1.42 -> 1.25 s (-12 %), page loaded -11 %, worst UI block -18 %; memory after the page set 590 -> 562 MB (-28 MB: app process -49, content process -18); after a dropped file cache the same direction (-4 %) | tools/verify-apk.sh fails a deflated omni.ja; OwnerRulesTest.lab5MeasuredPackagingProfileAndPrefsStay |
| 2 | Baseline profile: 14,100 rules (11,173 methods, 2,927 classes) from the lab's real ART profile; R8 keeps 6,721; `assets/dexopt/baseline.prof` 5.9 KB (was 246 B) | the profile of daily run 44054301212 (profman dump of the app that ran a person's day); local release build shows the asset; the worth is measured by the `aot` suite (below) | verify-apk.sh asserts >= 2.5 KB; Health line `start art` on the tablet |
| 3 | Engine config v10: captcha telemetry actor off, login + form-history machinery off | stack evidence (ANS58: a slow Cloudflare check; MutationObservers of a telemetry-only actor in every captcha frame); the app stores no logins (autofill off) | names checked against the real GeckoView (49, 0 missing); read back in Diagnostics |
| 4 | The cold-start cache purge no longer lists the 10-40k files of `cache2/entries` | read from the code + the owner's 228 MB cache; Health line `storage purge-doomed ... took=` | StorageInfoPurgeTest (3) |
| 5 | Shields extension: ONE `runtime.onMessage` listener (24 -> 1; Firefox deserialises every message and clones the sender once per registered listener), the players script no longer runs inside human-check frames (a duplicate JSON key hid the exclusion), the class-name scan skips repeated class strings and runs its load pass when idle | audit (estimates 15-45 ms of parent-process time on a 20-frame page; the manifest bug is a fact) | background.test.js (99), manifest duplicate-key test, one-listener test; web-tests 581 pass |
| 6 | SnakeYAML warm-up thread (GeckoView parses the engine config with SnakeYAML on the UI thread) | run-9 stack profile caught `Yaml.load` in `GeckoRuntime.create`; A/B = variant `nowarm` of sweep run 44059232336 | (result below) |
| 7 | Start-path micro changes: the three SharedPreferences files are requested in `GeckoApp.onCreate`; the Recents icon is set 1.5 s after the engine is ready instead of inside the first-frame callback | audit | compile + tests |

## Measured and NOT adopted (the ledger)
- Native libraries kept in the APK (`-PlibsInApk=true`): no start-up gain (-4..-6 % inside the noise), no memory gain, +65 MB download on ARM32 (+103 MB in the x86 lab build). Opt-in switch stays.
- Dropping `libcrashtools.so` (2.6 MB download): safe but tiny; skipped. Dropping the `:crashhelper` process (29 MB): refused, a crashed Gecko process could hang on a pipe nobody reads.
- Moving `earlyCover` image decode / DownloadController / UpdateManager init / hidden views off the first frame: 20-130 ms each by estimate, medium risk (z-order, visual flash), not measurable here: not done.
- Verdict LRU for the blocking engine: hit rate unknown, would need counters first.
- A `.dm` file in the self-updater (compile at install): the platform path is measured by the `aot` suite (below); the updater change is NOT made (a failed install would block updates).

## Still to fill in (runs in flight when this file was written)
- sweep run 44059232336: noise floor (base vs base2), `nowarm`, nursery16, gcpar, gcsmall, canvas64.
- after-run daily on the changed code vs research/lab-baseline.json and run 11.
- vp run (video hand-over after the extension change).
- aot run: verify vs speed-profile vs speed, release build with the embedded profile, compile-at-install check.
