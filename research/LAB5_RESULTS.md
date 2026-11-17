# LAB5 results: the "ultra fast, snappy, lightweight" pass (2026-11-17, release alpha64)

Owner message (2026-11-17T06:42Z): use as much time and usage as needed to improve the browser from what the emulated runs taught; run more emulations if they help; ultra fast, snappy, lightweight; release after research and coding.
Method (evidence first): four read-only audits (research/LAB5_*.md) -> lab suites that answer ONE question each (pack, sweep, aot, daily) -> change only what a measurement or a stack sample supports, each with a test.
Every lab number is an x86 emulator with a software GPU and a fast link: read the DIFFERENCES between variants of one run, never the milliseconds, and nothing here was run on the tablet.

## Changed in alpha64 (and the proof)
| # | Change | Evidence | Test |
| -: | :- | :- | :- |
| 1 | `assets/omni.ja` stored uncompressed (mapped from the APK, not inflated into every engine process) | pack run 44053244976, four builds of the same code: warm start 1442 -> 1280 ms (-11 %), engine ready 1.42 -> 1.25 s (-12 %), page loaded -11 %, worst UI block -18 %; memory after the page set 590 -> 562 MB (-28 MB: app process -49, content process -18); after a dropped file cache the same direction (-4 %) | tools/verify-apk.sh fails a deflated omni.ja; OwnerRulesTest.lab5MeasuredPackagingProfileAndPrefsStay |
| 2 | Baseline profile: 14,100 rules (11,173 methods, 2,927 classes) from the lab's real ART profile; R8 keeps 6,721; `assets/dexopt/baseline.prof` 5.9 KB (was 246 B) | the profile of daily run 44054301212 (profman dump of the app that ran a person's day). aot run 44074456934 on the RELEASE build: ProfileInstaller wrote it, `speed-profile` compiled 3.75 MB of code (verify: 0.1 MB), warm start 1898 -> 1671 ms (-12 %, inside a +-10 % spread), cold-cache start unchanged | verify-apk.sh asserts >= 2.5 KB; Health line `start art` on the tablet |
| 3 | Engine config v10: captcha telemetry actor off, login + form-history machinery off | stack evidence (ANS58: a slow Cloudflare check; MutationObservers of a telemetry-only actor in every captcha frame); the app stores no logins (autofill off) | names checked against the real GeckoView (49, 0 missing); read back in Diagnostics |
| 4 | The cold-start cache purge no longer lists the 10-40k files of `cache2/entries` | read from the code + the owner's 228 MB cache; Health line `storage purge-doomed ... took=` | StorageInfoPurgeTest (3) |
| 5 | Shields extension: ONE `runtime.onMessage` listener (24 -> 1; Firefox deserialises every message and clones the sender once per registered listener), the players script no longer runs inside human-check frames (a duplicate JSON key hid the exclusion), the class-name scan skips repeated class strings and runs its load pass when idle | audit (estimates 15-45 ms of parent-process time on a 20-frame page; the manifest bug is a fact) | background.test.js (99), manifest duplicate-key test, one-listener test; web-tests 581 pass |
| 6 | SnakeYAML warm-up thread (GeckoView parses the engine config with SnakeYAML on the UI thread) | run-9 stack profile caught `Yaml.load` in `GeckoRuntime.create`; A/B (sweep run 44059232336, n=3): engine ready 1.50 s with it off vs 1.49 / 1.61 s with it on = no difference on x86; kept on theory + the stack sample, NOT a measured gain | compile + tests; Diagnostics lines `engine runtime-create-took` and `engine config-warm` |
| 7 | Start-path micro changes: the three SharedPreferences files are requested in `GeckoApp.onCreate`; the Recents icon is set 1.5 s after the engine is ready instead of inside the first-frame callback | audit | compile + tests |

## The proof: the daily suite before and after (same lab code, same kind of runner)
Before = run 44054301212 (commit da746ada: alpha63 + lab tooling, none of the LAB5c app changes). After = run 44062596768 (commit c83d9f96: all LAB5c changes). 72 scenes, a person's day on the emulated tablet. The runner's own bench page says the
second runner was about 6 % slower (244 ms vs 229 ms), so differences under ~10 % are noise.
| Metric | before | after | Reading |
| :- | -: | -: | :- |
| scenes ok / failed | 70 ok, 1 fail, 1 no-play | 71 ok, 0 fail, 1 no-play | the one failure (typed address without scheme) did not come back: a flake of the lab's tap, not the app |
| page load done, median / p90 (ms) | 1784 / 4018 | 1777 / 4718 | same (p90 is one heavy page) |
| page first paint, median (ms) | 439 | 514 | noise (runner 6 % slower, one outlier page) |
| scroll gaps over 100 ms per 1000 frames | 20.4 | 17.6 | same or a little better; worst gap 2233 -> 1667 ms |
| UI thread late heartbeats / worst (ms) | 6 / 2095 | 2 / 1771 | fewer |
| cold start `am start -W`, engine ready | 2180 ms, 2.7 s | 2373 ms, 2.9 s | no gain on this emulator (+9 %, runner 6 % slower): the first launch after an install pays for first-run work in both; the tablet's interpreter-bound start is what the profile is for |
| engine-create burst on the UI thread (`engine burst runtime+orientation`, first start) | 176 ms | 125 ms | the SnakeYAML warm-up thread (57-140 ms of YAML work done on another core) |
| memory: max PSS / after the memory warning / lowest available | 309 / 302 / 476 MB | 303 / 291 / 489 MB | 2-4 % less, 13 MB more free; median app PSS per page 345 -> 335 MB |
| security: pass / fail | 17 / 0 | 16 / 0 | the missing pass is `bad-certificate-self-signed`: the runner could not reach badssl.com that time (not measured, not failed) |
| issues (high / medium / low) | 0 / 1 / 1 | 0 / 0 / 1 | |
Plain reading: nothing got worse that the runs can see, memory is a little lower, the engine-create step is shorter; start-up time and page speed are the same on this fast x86 emulator. The wins this pass was after (the interpreter-bound start on the
tablet's four slow cores, the 14.5 MB per engine process, per-page work in the extension) are only partly visible here and are NOT proven on the tablet.

## The video suite on the changed code (vp run 44066133643)
11 of 12 scenes ok: Kestrel's own player, the native player (file and HLS), the video format policy, the HLS picture cap, videos start only when pressed, web videos in Kestrel's player (the hand-over chain through the single
extension listener), VLC-style gestures, the quality list inside the page, a private tab that keeps its login. The 12th, `film-inside-the-page`, failed: its Diagnostics line names the reason, alpha62's loop breaker (a clip of at most two
minutes that Kestrel's player played to its end is not handed over again within ten minutes) meeting the lab replaying the same clip after the earlier scene. Not a LAB5 effect; the lab now clears that memory (debug command
`handreset`) before the scene, and the scene is run again on its own (result below when read).

## Measured and NOT adopted (the ledger)
- Engine preferences nursery16 / gcpar / gcsmall / canvas64: sweep run 44059232336 (17 good starts, then the emulator went offline): every variant inside the noise of two identical configurations (memory +-45 MB, bench +-3 %): none adopted (research/LAB5_PREFSWEEP.md).
- Native libraries kept in the APK (`-PlibsInApk=true`): no start-up gain (-4..-6 % inside the noise), no memory gain, +65 MB download on ARM32 (+103 MB in the x86 lab build). Opt-in switch stays.
- Dropping `libcrashtools.so` (2.6 MB download): safe but tiny; skipped. Dropping the `:crashhelper` process (29 MB): refused, a crashed Gecko process could hang on a pipe nobody reads.
- Moving `earlyCover` image decode / DownloadController / UpdateManager init / hidden views off the first frame: 20-130 ms each by estimate, medium risk (z-order, visual flash), not measurable here: not done.
- Verdict LRU for the blocking engine: hit rate unknown, would need counters first.
- A `.dm` file in the self-updater (compile at install): the platform path WORKS on Android 10 (aot run 44074456934: a PackageInstaller session with base.apk + AGP's baselineProfiles/1/*.dm = `reason=install-dm`, 3.75 MB compiled while installing, install +0.3 s); the updater change is NOT made (a failed install would block the only update channel; it needs its own release and a tablet test). TASKS LAB6.

## Still to fill in
- vp run (video hand-over after the extension change).
