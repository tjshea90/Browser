# RAM_FOR_SPEED: "as fast as possible utilizing as much ram as possible for speed within reason for a low ram tablet" (owner, 2026-11-24, PERF20)
Owner, verbatim: "Do even more research how the browser can be as fast as possible utilizing as much ram as possible for speed within reason for a low ram tablet".
Method: 14 days of the owner's live diagnostics (194 page loads, 27 uploads; python3 -B -I tools/diag_digest.py loaders), the GeckoView 160 classes (javap), the repo's earlier audits (LAB5_PREFSWEEP.md, GECKO_SPEED.md, SPEED2.md, TAB_SWITCH.md, INSTANT_BACK.md) and web searches 2026-11-24 (Mozilla bugzilla and support pages, Chromium design docs and source, AOSP lmkd docs, web.dev). Public repo: site kinds only.

## 1. What this tablet's memory really does (measured)
| Fact | Value |
| :- | :- |
| Total RAM, flags | 1954 MB, low-RAM device = true, app heap class 128 MB, the system's own low-memory threshold 144 MB; zram swap about 1.4 GB free |
| Memory AVAILABLE at the moment of a page load (MemAvailable, includes reclaimable cache) | min 223, p10 318, **median 481**, p90 731, max 1110 MB |
| Engine kills / crashes in 14 days | **2** (both after the app was left, with 358 to 453 MB available) |
| Android's trim signals in 14 days | level 20 (screen hidden) 241; level 5: 35; 10: 36; **level 15 (running critical): 53; level 80 (this process is next): 33** |
| Memory available when the CRITICAL signals came | **level 80: min 340, median 442, p90 571, none under 300 MB.** Level 15: min 195, median 369, 16 of 53 under 300 MB (those may be real), 37 of 53 at 300 MB or more |
| Page loads with 1, 2, 3 live tabs (median first paint) | 2.0 s (63 loads), 1.8 s (103), 1.3 s (11): no sign that a second live tab slows the one in front |
| Processes | one page process, the GPU process (software drawing), a utility and a media process, the crash helper, the app's own process (about 310 MB at one reading, the engine's parent included) |

Reading: memory is NOT the main shortage on this tablet (kills are rare, half a gigabyte is usually free); the single page thread's CPU is (research/PAGE_SPEED_2026-11-23.md). But Android's "critical" signals do not follow free memory here: they come with 340 to 800 MB free, level 80 within a second of every time the app is left.

## 2. What the engine does with those signals (read from GeckoView 160 with javap)
- `GeckoRuntime.create(context, settings)` makes a `MemoryController` (a ComponentCallbacks2) and registers it on THE CONTEXT IT WAS CREATED WITH. It maps level 15 and 80 and up to CRITICAL ("low-memory") and 40 and up to MODERATE ("low-memory-ongoing"), and onLowMemory to CRITICAL; levels 5, 10 and 20 do nothing.
- Gecko answers a memory-pressure notification by purging caches (decoded images, texture / memory caches), evicting frozen back/forward pages and running a shrinking garbage collection (`javascript.options.gc_on_memory_pressure`), which also discards compiled script code. Mozilla's own bug tracker says the same and says the APP is expected to decide (bug 1587853: "the application can implement onLowMemory / onTrimMemory on its own"; bug 1454752: memory-pressure events purge caches and jettison images; Fennec turned the bfcache off under pressure).
- Chrome's (desktop) policy is graduated: moderate pressure trims the back/forward cache to a small count for background tabs, only critical pressure clears it (Chromium `back_forward_cache_impl.h` and `bfcache_policy.cc`; Android's policy not read); Chrome's own planning notes: on low-RAM devices memory is traded against speed, and 60 to 70 % of foreground-renderer crashes on low-RAM Android are out-of-memory (Chromium design doc): "within reason" means a safety floor.
- Android 14 and later no longer send the running-level signals (15) to apps (Android API reference as quoted by third-party SDK notes; not re-read at the source); levels of the background family (40 and up) still come. So on an Android 16 device only the second kind exists.

## 3. Where the browser already spends RAM for speed (sizes are estimates unless measured)
| What | Setting | Size |
| :- | :- | :- |
| Frozen back/forward pages | `max_total_viewers` 6, kept 1 hour | 20 to 100+ MB each; evicted by a critical signal |
| Decoded images | `surfacecache.size_factor` 20 (cap about 95 MB), kept 3 min, decoded 2 screens ahead, lazy margin 1500 px | up to 95 MB plus the same again as textures in software drawing |
| Memory cache of page files | 64 MB | 64 MB |
| Disk cache (the kernel keeps its hot parts in free RAM) | 512 MB | 0 of ours; page cache is free RAM |
| Compiled script cache | bytecode strategy 1 (after 2 loads) | disk |
| Live tabs | Settings "Tabs kept ready" (the owner: Few, 2) and the free-memory rule (alpha98: 1 tab under 300 MB, 2 under 450) | one page 100 to 350 MB |
| The app's own pictures | 8 Back pictures, last-look pictures of the front tab and two more, grid thumbnails | a few MB |
| Spare content process | 1 on Android | not seen in the process list |

## 4. Findings and verdicts (what I did and did not do, and why)
| Technique | Who | Kestrel G | RAM | Gain here | Verdict |
| :- | :- | :- | :- | :- | :- |
| Do not drop warm caches and compiled code on a false alarm; respond to real memory shortage (graduated, by evidence) | Chrome (graduated bfcache policy), Mozilla (apps decide) | the engine took every signal as real | frees nothing, KEEPS ~100 to 300 MB warm | the heavy pages' compiled script code and decoded images survive leaving the app; returning is a warm start | **DONE in alpha101: `TrimGate` (core) + `TrimGateContext` (app) + `EngineTrimGate` (Gecko)** |
| A cap that follows memory, not a fixed one | Firefox (memory cache auto-sized from RAM: -1 = by RAM; disk smart size), Chrome low-end mode | fixed values chosen for 2 GB (section 3) | - | the fixed values already assume "usually 450 MB free" | HAVE; the gate adds the missing "give back when short" |
| More content processes | Firefox desktop (default up to 8; 10 to 20 % more memory with 1, about double with 8 in Mozilla's early measurement; Tails cut to 2 below 3 GB) | 1 | +65 to 95 MB per process (old bug) | my own data: first paint with 2 live tabs 1.8 s against 2.0 s with 1: no interference to remove | SKIP (no evidence; costs the memory the gate is protecting) |
| Keep the app process alive after the system destroys its screen | Chrome (cached process), Android lmkd (cached processes die first, PSI-based) | 2 minutes (already) | 300+ MB while cached | about 1 to 2 s (engine ready 1.1 s) because the tabs' sessions are closed with the screen | SKIP (small gain, large RAM) |
| Keep decoded images longer than 3 min | Firefox (min expiration 60 s default) | 3 min | bounded by the 95 MB cap | pictures of the page you return to | NEXT, only after the gate's numbers (the gate already stops the flush on leaving) |
| Bigger bfcache (more than 6) | Chrome (entry counts, 1 background entry under moderate pressure) | 6, 1 hour | 20 to 100+ MB each | LAB5 flagged 6 as already questionable | SKIP |
| Bigger JS garbage-collection growth factors, bigger nursery | Firefox (smaller sets for 512 MB devices only) | engine defaults | more | unmeasured, refused earlier without a tablet benchmark | SKIP |
| Tabs kept ready: Automatic when memory is plentiful | Chrome | owner chose Few (2); the advisor suggests Automatic when there are no kills and 450+ MB | one page per extra tab | switching tabs stays instant | HAVE (advisor), the owner's choice |

## 5. What alpha101 does and how it is judged
- Every Android trim signal now passes the gate before the engine hears it. A signal the engine would react to is HELD only when: the switch is on, the system's own low-memory flag is off, at least 350 MB are available, the engine was not killed or crashed in the last 6 hours, and fewer than 6 signals were already held in the last 30 minutes. Otherwise it is passed on exactly as before. `onLowMemory` (the system's own call) and configuration changes always pass. If anything in the gate throws, the signal is passed on.
- Settings > "Keep the engine's memory when it is not short" turns it off. Diagnostics shows the gate line (registered on `org.mozilla.gecko.process.MemoryController`, held and passed-on counts, the last decision). The live diagnostics carry `mem trim-held` / `mem trim-sent` lines and the digest prints "memory signals" and "the gate" lines in section 1.
- The result to read in the next uploads: (1) held count above 0 and the reasons of the passed-on ones; (2) engine kills not more than before (2 in 14 days); (3) first paint of a heavy page after leaving and returning, split by the gate switch (not yet in the digest; add when there are uploads from both settings); (4) free memory at page loads not dropping under 300 MB more often.
- Risk, said plainly: when Android really needs memory the engine now flushes later or not at all if the system's numbers look fine; Android may then end the app's processes instead (it ends cached processes first). That is why a kill switches the gate off for 6 hours and why the gate holds only above 350 MB.

## 6. NOT proven on the tablet
The gate is tested on the JVM (the rule; the wrapper registering and unregistering a callback through a Robolectric Application) and compiled into the Gecko build; whether the real engine's `MemoryController` is registered through the wrapper on a device is shown by the Diagnostics line "gate registered on:" and by the first `mem trim-held` line. The speed gain (a warm start after leaving the app) is a hypothesis from the engine's documented behaviour and the owner's memory numbers, not a measurement.
