# LOW_END_PLAYBOOK: how popular browsers get speed on weak hardware, and what applies to THIS tablet and Android 10 Go (owner, 2026-11-24, PERF22)
Owner, verbatim: "After that build, do even more deep research on how popular browsers use techniques and methods and code and ram and cpu settings for the best possible speed, as well as the best practices for low ram and weak cpus. Keep in mind my hardware, and also the fact that the os is Android 10 go, which may need specific fixes or tweaks."
Method: web research 2026-11-24 (Chromium design notes and source, Mozilla bugzilla and docs, Android developer / AOSP docs, web.dev) + the engine's own library (libxul strings, javap of GeckoView 160) + 14 days of the owner's live diagnostics (194 page loads, 35 uploads, 175 CPU busy moments) + this repo's earlier audits (GECKO_TUNING_2, GECKO_SPEED, LAB5_PREFSWEEP, SPEED2, SCROLL, HW_SPEED, INSTANT_BACK, TAB_SWITCH, RAM_FOR_SPEED, BROWSER_TECHNIQUES, PAGE_SPEED, DECORATIVE_VIDEO). Tags: [M] measured on the owner's tablet, [V] verified in the engine or a primary source, [S] a secondary source or the vendor's claim, [E] estimate. Public repo: site kinds only.
The earlier audits' verdict still stands and is the frame: GeckoView 160 is already close to what settings can give on this chip; most levers popular browsers pull are already pulled here. This file adds what those audits did not have: Android 10 Go behaviour, the owner's per-thread CPU numbers, and a list of what is left.

## 1. The hardware and OS in numbers
| Fact | Value | Source |
| :- | :- | :- |
| SoC | MediaTek MT8168B: four ARM cores (public spec: Cortex-A53, in-order, up to 2 GHz); LPDDR4x per public spec | owner's file; Notebookcheck / CPU-Monkey spec pages [S] |
| GPU | Mali-G52 MC2 (the owner's file; public pages say MP1; the `mali-cmar-backend` driver thread shows in the CPU lines) | owner's file [V], uploads [M] |
| RAM | 1954 MB total, low-RAM device = true (`ro.config.low_ram`), heap class 128 MB, system low-memory threshold 144 MB, zram swap about 1.4 GB (515 to 1000 MB free in uploads) | owner's file, uploads [M] |
| MemAvailable at page loads | min 223, p10 318, median 481, p90 731, max 1110 MB | [M] |
| OS | Android 10 Go (API 29), security patch 2021-07-05, kernel 4.14.141, unencrypted storage, system image `sys_mssi_t_32_ago`: **32-bit userspace only** (armeabi-v7a, no arm64) | owner's file |
| Heat | thermal status 0 in all 35 uploads; "sustained performance mode" NOT supported by the device; Android Battery Saver on in 5 of 35 uploads | [M] |
| Storage | 2.0 to 4.2 GB free (median 2.8 GB); eMMC-class (no figure measured) | [M] |
| Who uses the four cores (CPU lines, 175 busy moments, % of one core) | parent process `Gecko` thread (engine main thread, hosts the Shields extension and network dispatch): in 121 of 175 moments, median 34, max 100; `kestrel-io`: 96 moments, median 7, max 34 (background lane); `AndroidUI` 91, median 8; `RenderThread` 63, median 10, max 78; `HeapTaskDaemon` (the app's Java garbage collector) 50, median 10, max 18; `Cache2 I/O` (engine's disk cache) 44, median 28, max 67; `Socket Thread` 14; `Classifier Upda` (tracking-list updater) 6, median 61, max 96 | [M] |

Reading: nothing is thermal-throttled, memory is usually fine, and the page's own single thread (research/PAGE_SPEED_2026-11-23.md) is the limit. What our side adds to the contention: the parent `Gecko` thread (Shields judging, 0.6 to 1.8 ms a request on this CPU, about 1500 requests on a heavy page) is the largest single item.

## 2. Android 10 Go: what it does and what it means for a browser
| Android 10 Go behaviour | Evidence | What Kestrel G does / should do |
| :- | :- | :- |
| `ro.config.low_ram=true`: `ActivityManager.isLowRamDevice()` true; fewer cached processes kept, background work limited, some features off (Google's Go guidance: trim memory, drop caches, avoid memory-heavy features; PiP and overlay windows may be unavailable) | developer.android.com Go docs [V]; the exact `low_ram` changes were not found in primary sources [N] | uses `isLowRamDevice` in the tab budget and the download rules; HAVE |
| Memory signals do NOT follow free memory: level 15 and 80 arrive with 340 to 800 MB free; 33 of 33 level-80 signals had 340 MB or more | [M] | gated since alpha101 (RAM_FOR_SPEED.md); on Android 14 and later the running-level signals are no longer sent (only 40 and up) [S] |
| Cached processes die first (lmkd, PSI-based, zram-aware); the app is often gone when the person returns: 7 "ended in the background" in 14 days | AOSP lmkd README [V], uploads [M] | cold starts are a given: lazy start after 4 h (alpha100), last-look picture, session on disk; HAVE |
| Doze and App Standby limit background work of apps NOT in use | Android docs [V] | irrelevant to foreground browsing; the daily filter refresh runs as a job and may be deferred (harmless) |
| Foreground apps run in the `top-app` cpuset (all four cores, highest weight); a bound service of a foreground app can inherit foreground priority (Chrome binds renderers with BIND_IMPORTANT) | AOSP task_profiles, Android docs, Chrome design note [V/S] | GeckoView owns its process bindings; the app's own background threads are at nice 13 (the `kestrel-io` lane, HW_SPEED H2) and downloads yield to a loading page; HAVE |
| No `PerformanceHintManager` (API 31+), no uclamp control, sustained-performance mode unsupported | [V] device report | nothing to call; SKIP |
| 32-bit userspace on a 64-bit-capable core | owner's file | a 64-bit engine would be about 4 to 8 % faster on JavaScript in Chrome's numbers [S], but the OS cannot run arm64 apps; SKIP |
| Storage 2 to 4 GB free, installs refused below about 1 GB | uploads, Update panel | "Storage tidy" and the install-space numbers exist; HAVE |

## 3. What popular browsers do, and our verdict
| Area | Technique (who) | Kestrel G today | Verdict |
| :- | :- | :- | :- |
| Loading | preload scanner, preconnect, DNS look-ahead, speculation prefetch, Early Hints, HTTP cache first, bytecode cache (all) | all present (BROWSER_TECHNIQUES.md section 1) | HAVE |
| Loading | cheaper JavaScript for weak CPUs: parse + compile cost is 2 to 5 times higher on mid-range phones (web.dev); the browser-side levers are blocking third-party script, the bytecode cache and lazy parsing | Shields blocks ads and trackers; bytecode cache strategy 1 (after 2 loads) | HAVE; strategy -1 as a lab A/B (**DO, alpha103**) |
| Loading | server-side rendering / compression proxies (Opera Mini, UC, Puffin) | needs a rendering server; privacy rules forbid | SKIP |
| CPU | keep the main thread free: yield, background priority for housekeeping (Chrome) | io lane at nice 13; downloads yield to a loading page; look / thumbnail work off the UI thread | HAVE |
| CPU | the extension / request filter must be cheap: Chrome's declarativeNetRequest evaluates rules natively, Firefox supports it too | the blocker judges each request in JavaScript on the parent `Gecko` thread: 0.6 to 1.8 ms each, the largest item in section 1 | **NEXT: PERF19** (benchmark first, then a decision cache / native rules) |
| CPU | JIT tier thresholds (SpiderMonkey 100 / 1500) | engine defaults | SKIP: Mozilla's own analysis says performance is not sensitive to the exact thresholds; refused in earlier audits |
| CPU | parallel GC marking, more helper threads | off (engine default) | **DO as a lab A/B (alpha103)**: `javascript.options.mem.gc_parallel_marking` |
| CPU | profile-guided / LTO engine builds | Mozilla's shipping armv7 build is already PGO; a custom build cannot run in CI (GECKO_TUNING_2 4.1) | SKIP |
| Memory | cache sizes by device RAM, graduated memory-pressure response, tab discard / freeze, ~100 MiB of offscreen image memory reclaimed per tab (Chrome) | fixed caches tuned for 2 GB, tab budget by free memory, offscreen images 2 screens; the new memory-signal gate | HAVE |
| Memory | second content process | skipped: first paint with 2 live tabs 1.8 s against 2.0 s with 1, +65 to 95 MB per process | SKIP (RAM_FOR_SPEED.md) |
| Graphics | GPU raster + compositor thread (all) | NOT usable: the GPU path loses bands of text on this Mali (Mozilla's own tracker has the same family on Mali-G31 (blocked), G77 (partial present off), T860, and software drawing as the working fix on an Adreno 308); software drawing is ON and fixed it | decision made (PAINT_GLITCH.md) |
| Graphics | cheaper software drawing: gradient dithering, tile sizes, extra raster pools (`gfx.webrender.dithering`, `picture-tile-width/height`, `blob-tile-size`, `enable-low-priority-pool`) exist in this engine | defaults | **DO as a lab A/B (alpha103): dithering off** (gradients are everywhere on marketing pages); the tile and pool settings stay unmeasured |
| Graphics | steady lower frame rate; reduced-motion honouring; throttle hidden frames | Calmer redrawing and Calm pages switches; decorative clip limiter (alpha102) | HAVE |
| Video | hardware decode, match decoder frame rate, ABR cap, suspend in background | all present (VIDEO_AUDIT, BROWSER_TECHNIQUES) | HAVE |
| Startup | last-page snapshot, only the tab in front loads, start screen after hours away (Chrome, Firefox) | last look, one live tab, lazy start (alpha100), baseline profile for our own code, engine warm-up | HAVE |
| Network | HTTP/3 where it helps, DoH, connection reuse | switches exist (the owner has HTTP/3 off on the hotspot) | HAVE |
| Power | Wi-Fi in high-performance mode while the browser is open | "Keep Wi-Fi awake" lab switch | HAVE |
| Android 10 Go | never answer low-memory warnings blindly | alpha101 | DONE |

## 4. What this adds: alpha103 and the list that is still open
Built in alpha103 (all OFF by default, reversible, applied at the next start, recorded in every upload's settings so the report script compares page loads with and without):
1. Settings > Network lab > **Cheaper gradients** = `gfx.webrender.dithering: false`.
2. **Compile scripts on first load** = `dom.script_loader.bytecode_cache.strategy: -1`.
3. **Parallel script clean-up** = `javascript.options.mem.gc_parallel_marking: true`.
These are EXPERIMENTS: no gain is claimed. Each is a setting the engine has, popular browsers' notes point at, and only the owner's tablet can judge.
Open, ranked by measured size, as their own tasks (TASKS.md PERF19 and PERF23): (1) the parent `Gecko` thread: Shields judging and request dispatch; (2) `Cache2 I/O` bursts (the 512 MB disk cache on eMMC; median 28 % when busy); (3) the tracking-list updater (`Classifier Upda`, rare but up to 96 % of a core; a privacy and policy decision before any change); (4) `HeapTaskDaemon` (our own Java garbage, median 10 %).

## 5. NOT proven / not found
- The exact list of what `ro.config.low_ram` changes was not found in a primary source; the table in section 2 uses only what the Android docs and the owner's own data show.
- The tablet's CPU governor, memory type and eMMC speed are not measured; the Diagnostics "Storage" line is empty in the uploads read.
- Everything in alpha103 is untested on the tablet; the three switches are for the owner to flip one at a time.
