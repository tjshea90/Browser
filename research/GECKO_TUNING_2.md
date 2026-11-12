# GECKO_TUNING_2 - typing lag, heavy single-page apps and "modify the engine?" for GeckoView 160 on the MT8168B tablet (TASKS X12)

Written 2026-11-12 by a research agent. READ-ONLY study: no code, pref or build file changed; NOTHING ran on the tablet or an emulator (no device, no KVM). Owner report: Proton Mail "very slow" to load, "lagged when I was typing", "consider ... modifying [the Firefox engine] for maximum speed". Builds on GECKO_SPEED.md, HW_SPEED.md, FREEZE.md, SCROLL.md, D1-DESIGN.md (not repeated).
Tags: [V] verified = read in the bundled 160 AAR (bytes, strings, readelf, javap, omni.ja JS) or in a source I opened (keys S1.. in section 7); [E] my estimate or inference; [N] not verified (search snippet only, or not checked). 160 pref defaults are exact when taken from the AAR's greprefs.js / geckoview-prefs.js; otherwise they come from StaticPrefList.yaml at tag FIREFOX_160_0_RELEASE read through a summarising fetch tool (S2: values as that tool reported them).
EXISTS/MISSING method (stricter than tools/check-gecko-prefs.sh, which accepts any substring): unpack libxul.so and omni.ja into a fresh temp dir; `strings -a -n4 libxul.so | sort -u`; EXISTS = whole-line match, or a `pref("name"` line / quoted consumer in omni JS; else MISSING. About 440 names checked (369 EXISTS, 71 MISSING; a nonsense control name was MISSING). Section 3 lists every name I recommend, test or reject.

## Summary (12 lines): the 10 changes I would make first, in order
0. Verdict: GeckoView 160 is already close to what prefs can give on 4xA53 / 1.9 GB (Android defaults, PGO build, JitHints, off-thread Baseline, no socket/RDD process, the IME re-attach bug fixed in 147); no pref is justified by evidence alone, so the first moves are measurement and removing stalls we or the network add, not engine surgery.
1. DO measure first: `ProfilerController.startProfiler/stopProfiler` (exists in 160, returns gzip JSON) + typing / JS / net labs + per-navigation timing in Diagnostics. Value: gates every decision below. Risk: none.
2. DO owner checks with zero code (accessibility service, autofill service, keyboard app, battery saver, free storage, hotspot band and RTT) and A/B Shields OFF for proton.me (splits our hook + cosmetic CSS from the engine). Risk: none.
3. A/B DNS path: `network.trr.mode` 2 (now) vs 0 plus Android Private DNS (DoT); `network.trr.request_timeout_ms` 1500 vs 800. Risk: plain DNS visible to the carrier if Private DNS is off.
4. DO app preconnect: `GeckoWebExecutor.resolve()` + `speculativeConnect()` for the typed host at commit, `setPriorityHint(HIGH)` on the visible tab (still not in code). Risk: low. Gain: 1-3 RTT per cold navigation [E].
5. A/B `network.http.http3.enable` true vs false on the carrier hotspot (TCP backup starts after 100 ms). Risk: none.
6. A/B `dom.script_loader.bytecode_cache.strategy` 1 (now) vs -1 (save bytecode when a script is first seen). Risk: CPU and disk writes after load; gain only on repeat visits.
7. A/B `dom.ipc.processCount` 1 (now) vs 2 with per-process PSS and typing p95; never kill the content process needlessly (JitHints live in it). Risk: +RAM.
8. A/B JS, only if the profile shows GeckoMain GC/JS-bound: `javascript.options.mem.gc_parallel_marking` true; `javascript.options.baselinejit.offthread_compilation_strategy` 2 vs 0. Risk: low.
9. DO hygiene: `browser.translations.enable=false` (unused Translations actor is registered in every http/https frame in 160); keep gecko-prefs.txt v2 otherwise. Risk: low; gain unmeasured.
10. DO NOT rebuild Gecko (4 vCPU / 14 GB runner, 6 h cap, Mozilla's own build is PGO; a non-PGO build would be slower). The only no-compile engine change worth a Gradle step is trimming omni.ja (-9.1 MB deflated, about -10 % of the 92 MB APK), for update size only; bump the BuildID or clear the startup cache when you do.
11. Rejected on evidence: eager delazification below 2 GB, Spectre mitigations off (Fission is off), JIT thresholds, the script navigation cache (Nightly-only, broke Discord), beta/nightly AAR in production, copied "Fastfox" lists (most names are MISSING in 160).

## 1. New verified facts about GeckoView 160 (they close open items in GECKO_SPEED / HW_SPEED)
| Fact | Tag, source |
|---|---|
| No socket process on Android: `network.process.enabled` is true on desktop and false on Android / Thunderbird / iOS; `media.rdd-process.enabled` is desktop-only; `media.utility-process.enabled` true; `dom.ipc.processPrelaunch.fission.number` = 1 on Android. Expected tree: parent (app + Gecko main + extension JS, because the extensions process is off) + 1 content (+1 prelaunched spare at start) + GPU [N, GECKO_SPEED Y] + utility. All network I/O, DoH, URL classifier and our webRequest hook share the parent. | [V S2 summarised], [E] tree |
| `dom.script_loader.delazification.strategy` default is 255 ("parse everything eagerly") but `delazification.min_mem` = 2 GB: below it the on-demand strategy is used. The tablet reports about 1.9 GB, so it runs on-demand. | [V S2], [E] the integer-GB comparison |
| JitHints on (`javascript.options.jithints` true), off-thread Baseline strategy 2 ("eager only"), blinterp / baseline / Ion thresholds 10 / 100 / 1500, `ion.offthread_compilation` true, `parallel_parsing` true, `external_thread_pool` true. JitHints are an in-memory per-process cache keyed by script filename + start offset: 5000 Ion hints (LRU), Baseline bloom filter cleared above 4281 entries. | [V S2, S8] |
| The shipped libxul.so targets ARMv7-A, Thumb-2, VFPv3 + NEON (`readelf -A`); .text 80.8 MB, .rodata 26.0 MB, .ARM.exidx 1.8 MB; 118,043,120 bytes; no `.llvm.`/`_GLOBAL__sub_I_` marker strings (inconclusive for LTO). | [V S1] |
| omni.ja is already Mozilla's optimized jar: 4-byte preload size (1,859,033), central directory FIRST, entries in startup-access order, 4086 files, 47.7 MB raw / 14.5 MB deflated. No baseline profile in the AAR. No hunspell in libxul; GV sets `layout.spellcheckDefault` 0, so no inline spellcheck cost. | [V S1, S14] |
| Android-specific defaults: `security.OCSP.enabled` 2 (EV only, so no OCSP round trip for DV certs), `security.pki.crlite_channel` "compat", `gfx.webrender.enable-low-priority-pool` false, `enable-subpixel-aa` false, `batched-upload-threshold` 262144, `program-binary-disk` true, `javascript.options.gc_on_memory_pressure` off, `network.http.active_tab_priority` false, `largeKeepaliveFactor` 10. | [V S2] |
| Spectre JIT mitigations: `index_masking`, `object_mitigations`, `string_mitigations`, `value_masking` all true; `disable_for_isolated_content` true but only helps Fission-isolated processes (shipped in 119 for Fission only). Fenix on Android runs with the mitigations on too. | [V S2, S10] |
| The Rust Happy Eyeballs (`network.http.happy_eyeballs_enabled`) is Nightly-only; `retry_with_another_half_open` and `http3.retry_different_ip_family` are early-beta-or-earlier only; the script "navigation cache" is `@IS_NIGHTLY_BUILD@`. All off in release. | [V S2] |

## 2. A. Typing lag and heavy single-page apps
### 2.1 Proton Mail: what is known and what is not
- [V S16] The Mail web app is a proton-pack/webpack React + Redux Toolkit + React Router app with `comlink` (worker RPC), `@protontech/crypto` (OpenPGP, so crypto workers) and `@proton/proton-foundation-search` (a search engine, probably wasm [N]); its package.json has no workbox. A service worker, bundle sizes, KDF type (SRP/bcrypt/argon2) and whether encrypted-search indexing runs on mobile UAs are [N]: measure with the labs (section 5).
- [E] A cold load is DNS + TLS to at least 5 proton.me hosts, MBs of JS parse/compile, a login KDF and OpenPGP key decrypt (CPU, in workers that share 4 A53 cores with the page), then dozens of API round trips at 150-600 ms each on the hotspot [N]. Typing in the composer is React re-rendering plus debounced draft encryption in workers [N]: JS on one content main thread, so Gecko prefs have little leverage and the profile decides.

### 2.2 Typing: causes ranked by evidence
| # | Cause | Evidence | In 160 / lever |
|---|---|---|---|
| 1 | Page JS on the single content main thread (React render per key, autosave encryption, indexing) on in-order A53 | mechanism [V S8: tiers 10/100/1500, Ion off-thread]; magnitude [E] several times slower than a phone core (HW_SPEED) | profile (5.3). Levers are thin: JitHints + off-thread Baseline are already on; do not move thresholds without kernels |
| 2 | IME re-attach per child document: every new iframe reset the keyboard (3-4 s input delay on rakuten.co.jp; dropped keys on genius.com) | [V S5: 1995911, 1933696] | FIXED in 147, so present in 160. Proton pages with many iframes were the trigger class; if lag persists it is not this |
| 3 | Unresolved upstream: keyboard lag/hang on some pages (1909993, open, no analysis), Bluesky 1-2 chars/s (1939678, open), wrong cursor position (1916381) | [V S5] bug pages; no pref workaround named | none; attach a profile if reproducible on the lab page |
| 4 | Accessibility service, autofill service or a heavy keyboard app on the tablet: Gecko switches to an accessibility input mode when it detects a service, and Mozilla notes slowdowns with screen readers / password managers | [N S6: snippets of 961540, 1479039, 1499163] | owner check (5.1); `setAutofillEnabled(false)` already set [V repo]; `accessibility.force_disabled` (EXISTS, default 0) only to diagnose |
| 5 | CPU contention: Stylo 3 threads, JS helpers, WebRender 2 backend + 4 workers, TaskController 4, crypto workers (hardwareConcurrency 4), plus our extension on the parent main thread | counts [V HW_SPEED], `render-backend-thread-count` 2 [V S2]; effect [E] | A/B `layout.css.stylo-threads` 2; reduce our work (D1 coded, tablet numbers pending [V repo]) |
| 6 | Memory pressure: code pages of the 118 MB libxul evicted and re-read from eMMC, GC and cache flushes | [E] HW_SPEED; `gc_on_memory_pressure` off on Android [V S2] | processCount 1, surfacecache cap (done); log `MemAvailable` during the typing lab |
| 7 | Not a factor: spellcheck (absent, section 1); event coalescing already on (`dom.events.coalesce.mousemove/touchmove`, `dom.events.compress.touchmove` true [V S2]); `beforeinput` is not pref-gated in 160 | [V] | no change |
Typing path for reference [V FREEZE.md]: Android IME -> `GeckoEditable` (Java, IC thread) -> Gecko main thread -> content process; each key is an action Gecko must acknowledge, and Gecko's vsync is the app's UI-thread Choreographer, so any UI-thread stall delays the echo.

### 2.3 Loading: causes ranked by evidence
| # | Cause | Evidence | In 160 / lever |
|---|---|---|---|
| 1 | Hotspot network chain: DoH-first lookup, HTTPS-RR (SVCB) wait, TLS, RTT-bound API waterfall | Mozilla measured +10-20 % `page_open_to_first_sent` on Android from waiting for a native HTTPS-RR answer, and 1 s extra first byte at 1000 ms RTT (1895908); fix in 128 skips the wait only when DoH is disabled (1898191) [V S7]; a resolver that ignores type 65 stalled ~10 s (1907982) [N snippet] | with TRR first the wait stays on the DoH path [E]; A/B TRR off + Private DNS, `native_https_query` false, H3 off |
| 2 | JS download + parse + compile of MBs | bytecode cache: 0 = after 4 reloads, 1 = after 2, -1 = first sight, -2 read-only [V S2 comment]; eager delazification gated off below 2 GB (section 1) | strategy 1 set; A/B -1. Do NOT force delazification: concurrent delazification gave 20+ s loads (1779940) and a 200 ms GC (1872221) [N snippets] |
| 3 | Our per-request hook and cosmetic CSS in every frame | [V SCROLL Q6, D1-DESIGN]: D1 in-extension engine coded (alpha13), no tablet numbers | A/B Shields OFF on the site; then Diagnostics engine row |
| 4 | OpenPGP / KDF in workers competing for 4 cores | [E] | profile "DOM Worker" threads |
| 5 | HTTP cache misses on repeat visits | disk cache 512 MB fixed, stale-while-revalidate on [V S2] | compare `fromCache` ratio, 1st vs 3rd load (net timing log) |
| 6 | Process kill (lmkd) then reload | [V SCROLL Q3] | Health log `onKill`, trim counters |

### 2.4 Process model and memory (RAM per process is unmeasured: read PSS per process from Diagnostics)
| Item | Value in 160 | Verdict |
|---|---|---|
| Fission | `fission.autostart` default true; the app forces false; with it off, Spectre mitigations stay on and one content process serves every site | keep off (RAM) |
| Content processes | `dom.ipc.processCount` greprefs 8, GeckoRuntimeSettings default 2, app 1; `keepProcessesAlive.web` 1; prelaunch spare 1 on Android; `processPrelaunch.lowmem_mb` 4096 (limits preallocation on small-RAM devices) | 1 is right for RAM; A/B 2 for typing |
| Socket / RDD | not used on Android (section 1) | nothing to disable |
| GPU / utility | GPU process (crash isolation of the Mali driver), utility (audio decode) | keep; `media.utility-process.enabled=false` would save one process but weakens isolation, no evidence |
| bfcache / images / disk cache | `max_total_viewers` 2, `surfacecache.size_factor` 20, 512 MB cache (all set) | keep |

## 3. B. Pref table (EXISTS / MISSING checked against the bundled 160)
Apply = edit gecko-prefs.txt (YAML at start) and restart the engine process. Live switching of the A/B rows via `GeckoPreferenceController` is not assumed [N]; the Speed lab should restart the engine.
### 3.1 Change now (gecko-prefs.txt v3)
| Pref | Value | Does | Expected effect here | Risk | Ev | In 160 |
|---|---|---|---|---|---|---|
| `browser.translations.enable` | false | unregisters the `Translations` JSWindowActor (`enablePreference`, allFrames on http/https/about:blank/srcdoc/file) and the translations engine; the app already hides the offer popup and never wants models over the hotspot | fewer actors per frame, no model/Remote-Settings work; size unmeasured | none for browsing; manual translate gone | [V] actor registration in omni, [N] gain | EXISTS (GV default true, greprefs false) |

### 3.2 A/B switches for the Speed lab (default = today; decide on tablet numbers)
| Pref | Today -> try | Does / evidence | Expected effect here | Risk | Ev | In 160 |
|---|---|---|---|---|---|---|
| `network.trr.mode` | 2 -> 0 (+ Android Private DNS) | removes the DoH setup, HTTPS-RR-over-DoH wait and 1500 ms timeouts; Private DNS keeps encryption at OS level (DoT, TCP 853) | shortest DNS critical path [E] | carrier sees DNS if Private DNS off; 853 may be filtered [N] | [V] defaults, [E] | EXISTS (default 0) |
| `network.trr.request_timeout_ms` | 1500 -> 800 | shorter stall per failed DoH lookup | fewer 1.5 s stalls on a flaky link [E] | more fallbacks to plain DNS | [E] | EXISTS (1500) |
| `network.dns.native_https_query` | true -> false | skips the native HTTPS-RR query; 1898191 already stops the wait when DoH is off | about 0 [E]; kept as a cheap control | none | [V] bugs, [E] | EXISTS (true) |
| `network.http.http3.enable` | true -> false | UDP/QUIC through carrier NAT may be filtered or stall; TCP backup after 100 ms, `fallback_to_h2_on_error` true | <= 100 ms per new origin if UDP blocked; mid-transfer stalls would show in TTFB [N] | loses H3 / 0-RTT | [V] defaults | EXISTS |
| `dom.script_loader.bytecode_cache.strategy` | 1 -> -1 | save bytecode at first sight; bundles are re-hashed per release [N] so "after 2 reloads" may never trigger | faster 2nd load of a new Proton release [E] | CPU + eMMC writes after load (`disk_cache_delay_ms` 10000) | [V] semantics, [E] | EXISTS (default 0) |
| `javascript.options.mem.gc_parallel_marking` | false -> true | 2 marking threads when heap >= 16 MB (`gc_parallel_marking_threshold_mb`, `gc_max_parallel_marking_threads`); Fenix flips it by Nimbus, no Android result published; 1943810: threshold counts GC heap only | shorter major-GC pauses [E] | contention on 4 cores | [N S20] | EXISTS (false) |
| `javascript.options.baselinejit.offthread_compilation_strategy` | 2 -> 0 | 0 none, 1 on-demand, 2 eager only, 3 eager + on-demand, 4 aggressive; 1943901 saw ~500 ms spikes with strategy 1; 1935289: late Baseline can skip trial inlining | only if the profile shows main-thread spikes | slower warm-up | [V] values, [N] effect | EXISTS (2) |
| `dom.ipc.processCount` | 1 -> 2 | second content process | smoother typing if one process starves, +RAM | RAM | [E] | EXISTS (ALREADY SET 1) |
| `layout.css.stylo-threads` | -1 (3) -> 2 | fewer style workers leave a core for the main thread | none known [N] | slower restyle on big DOM | [N] | EXISTS (-1) |
| `privacy.fingerprintingProtection` | true (app) -> false | cost of FPP in normal tabs | unknown | privacy | [N] | EXISTS (false; pbmode true) |
| `javascript.options.mem.nursery.max_kb` | 65536 (greprefs) -> 16384 | lower peak RSS, more minor GCs | RAM spike control [N] | more GC | [N] | EXISTS |
| `accessibility.force_disabled` | 0 -> 1 | never start the a11y engine; diagnostic only | tells whether a service is the cause | breaks screen readers | [N S6] | EXISTS (0) |

### 3.3 ALREADY SET (gecko-prefs.txt v2) and whether the value should change
`media.av1.enabled` false, `media.cache_*` 30 / 10 / 65536, `network.prefetch-next` false, `image.mem.surfacecache.size_factor` 20, `browser.sessionhistory.max_total_viewers` 2, `browser.cache.disk.smart_size.enabled` false + `capacity` 512000, `ui.prefersReducedMotion` 1, `image.animation_mode` "once", `general.smoothScroll` false, `dom.push.enabled` false, `dom.webnotifications.enabled` false, `media.peerconnection.ice.default_address_only` true, `security.certerrors.mitm.auto_enable_enterprise_roots` false: all EXISTS (the last one only as a JS consumer, no static definition), KEEP. `dom.ipc.processCount` 1: keep, A/B above. `dom.script_loader.bytecode_cache.strategy` 1: keep, A/B above (the file comment "after 2 loads" matches the 160 source comment [V S2]). Runtime settings in GeckoEngine.kt that matter here (keep): `fissionEnabled(false)`, `extensionsProcessEnabled(false)`, ETP strict + FPP, TRR first, `setFetchPriorityEnabled(true)` (`network.fetchpriority.enabled` true), `lowMemoryDetection(true)`.

### 3.4 Considered and rejected (all EXISTS in 160 unless noted)
- `javascript.options.spectre.{index_masking,object_mitigations,string_mitigations,value_masking}` = false: price unknown (no Firefox number found, S10), and with Fission off third-party iframes share the process: keep true.
- `dom.script_loader.delazification.{strategy,min_mem,max_size}`: forcing eager parse costs CPU and RAM and has regressed loads (S9); the 2 GB gate is Mozilla's own low-memory choice.
- `dom.script_loader.experimental.navigation_cache` (+`.check_memory_pressure`): Nightly-only; cached scripts broke Discord after refresh (2043993, fixed in 153/154) and sfgate (2042605) [V S11]. Revisit when it ships in release.
- `javascript.options.{blinterp,baselinejit,ion}.threshold` (10/100/1500), `ion`/`baselinejit` off: no evidence; JIT-less is much slower (ENGINES.md, V8 jitless Speedometer ~40 % slower [N]).
- GC knobs (`gc_incremental_slice_ms` 5, `gc_delay*`, `gc_helper_thread_ratio` 50, `gc_max_helper_threads` 8, `gc_compacting`), `wasm_*` tiering levels (`wasm_lazy_tiering_level` 5, `wasm_inlining_level` 5): no evidence either way.
- `browser.cache.check_doc_frequency` 2 ("never revalidate"): serves stale pages. `network.dns.disablePrefetch` / `network.http.speculative-parallel-limit` / `max-connections` (128): no gain on H2/H3 + carrier NAT.
- `network.dns.use_https_rr_as_altsvc` false: bug 1899841 title reports sites loading endlessly with it false under DoH [N snippet].
- `gfx.webrender.*`, `layers.*`, software WebRender, GPU process off: SCROLL.md / HW_SPEED traps stand. `layout.css.backdrop-filter.enabled` false: only helps the Mali-G78 class bug (1891831), no G52 report.
- `javascript.options.use_fdlibm_for_sin_cos_tan`, `memory.phc.*` (min_ram_mb 8000, so off here), `memory.lazypurge.enable`, `image.jxl.enabled`: defaults are fine.

### 3.5 MISSING in 160 (do not copy from popular lists; they are silent no-ops)
`javascript.options.asmjs` (asm.js removed in 2026, S19), `javascript.options.wasm_simd`, `wasm_gc`, `off_thread_parsing`, `jit_hints` (real name `jithints`), `mem.high_water_mark`, `mem.gc_high_frequency_heap_growth_min/max`, `dom.script_loader.delazification.enabled`, `dom.script_loader.navigation_cache` (real: `.experimental.navigation_cache`), `dom.serviceWorkers.parent_intercept`, `dom.caches.enabled`, `dom.indexedDB.enabled`, `dom.storage.next_gen`, `dom.input_events.beforeinput.enabled`, `dom.requestIdleCallback.enabled`, `fission.sessionHistoryInParent`, `layers.gpu-process.allow-software`, `layers.offmainthreadcomposition.enabled`, `extensions.webextensions.background-delayed-startup`, `network.http.rcwn.enabled`, `network.predictor.enabled`, `network.preload`, `network.tcp.tcp_fastopen_enable`, `network.ssl_tokens_cache_enabled` (real: `_capacity`), `network.trr.skip-AAAA-when-not-supported`, `wait-for-portal`, `early-AAAA`, `gfx.webrender.picture-caching`, `gfx.downloadable_fonts.woff2.enabled`, `content.notify.*`, `content.max.tokenizing.time`, `content.switch.threshold`, `html5.offmainthread`, `memory.free_dirty_pages`, `memory.lowmem_threshold`, `image.avif.enabled`, `intl.ime.hack.*fire_key_events_for_composition`, `dom.input.skip_cursor_move_for_same_value_set`, `dom.event.highrestimestamp.enabled`, `editor.use_div_for_default_newlines`, `editor.undo.driven_by_ime`, `accessibility.accessfu.activate`, `browser.tabs.remote.autostart`, `toolkit.cosmeticAnimations.enabled`, `extensions.manifestV3.enabled`, `browser.safebrowsing.provider.google4.updateURL` (no Google SB provider in GV), `widget.android.composite-directly-to-surface-control`. Most "Fastfox/Betterfox" lines are in this list or are desktop-era; I found no Fenix / IronFox / Mull performance pref list (S21 agrees).

## 4. C. Can the engine itself be modified?
### 4.1 A custom GeckoView build with device flags (-mcpu=cortex-a53, PGO/LTO, features off)
| | Mozilla CI (shipping armv7) | GitHub-hosted, public repo | This sandbox |
|---|---|---|---|
| CPU / RAM | PGO tasks use worker `b-linux-docker-xlarge-amd` (size not found [N]) | 4 vCPU / 16 GB [V S13] | 4 vCPU / 15 GB [V measured] |
| Disk | docs: at least 30 GB free, 4 GB RAM minimum, 8+ recommended [V S12] | 14 GB SSD [V S13] | 2.5 GB free of 252 GB [V measured] |
| Time | every android-arm build task capped at 7200 s; shippable ones use PGO (instrumented build + profile run + final build) [V S12] | 6 h per job [V S13]; cache 10 GB per repo [V S13] | n/a |
| Size | source + objdir about 2 GB + 15-25 GB [E] (no figure found) | does not fit the 14 GB SSD [E] | does not fit (free space [V], need [E]) |
- Estimate [E]: a clean armv7 opt build on 4 vCPU takes 4-8 h (Mozilla caps the job at 2 h on a larger worker; backing LTO out of one Android build cut its time from 1545 to 758, units not stated, bug 1480006 [V S12]); a PGO build is 2-3 builds, so it cannot fit one 6 h job, and a 10 GB cache split across jobs is fragile.
- What the flags could buy: Mozilla's shippable armv7 builds are ALREADY PGO ([V S12]: `use-pgo` on `android-arm-shippable/opt`, bug 632954 fixed in 68); LTO gave +2.5 % Speedometer on ARMv7 Fennec (Moto G5, Pixel 2) when it landed in Firefox 66 [V S12 bug 1480006]. The shipped libxul is generic ARMv7-A + VFPv3 + NEON, so `-mcpu=cortex-a53` (idiv, VFPv4 FMA, in-order scheduling) would touch only C++/Rust code, not SpiderMonkey's JIT output, GPU shaders or hand-written NEON codecs: [E] at most 3-5 % overall, less than losing PGO (10-20 % [E]). A non-PGO custom build would be SLOWER than Mozilla's.
- Artifact builds cannot help: they download prebuilt C++/Rust, are typically from mozilla-central, and "changes in Gecko or GeckoView may not be reflected" [V S12].
- Verdict: possible only on rented 16-32 vCPU hardware, with a rebase every 2 weeks (Mozilla ships a release roughly every 2 weeks, S3); not worth it. Partial approach that is realistic: configuration (done), omni.ja / JS / default-pref edits without compiling (4.2), and tracking beta for single fixes (4.3).

### 4.2 Patching the shipped AAR (omni.ja, assets, locales, unused modules)
| Part of omni.ja (14.49 MB deflated, 4086 files) | Deflated | Safe? |
|---|---|---|
| non-en-US locales (`chrome/<xx>`, `localization/<xx>`, 1764 files) | 2.62 MB | yes if the app stays English; Gecko-internal UI falls back to en-US [N] |
| pdf.js viewer (`chrome/pdfjs`) | 2.48 MB | functional loss: no inline PDFs |
| devtools + remote agent (`chrome/devtools`, `chrome/remote`) | 1.34 + 0.57 MB | yes with `remoteDebuggingEnabled(false)` (set); the Java ProfilerController is native, not devtools [E] |
| Translations (modules/translations, actors) | 0.96 MB | yes with `browser.translations.enable=false` |
| hyphenation dictionaries (46 files) | 1.16 MB | yes (`hyphens:auto` stops hyphenating) |
| Total removable | 9.12 MB (63 % of omni) | APK about 92 MB -> about 83 MB, i.e. about -10 % [E from the x86_64 build's ratios: libxul 59 %, omni.ja 4 %] |
- Keep: `libcrashhelper.so` + `libcrashtools.so` (7.5 MB raw, about 3 MB deflated; the child-process crash path [N]), `libgkcodecs.so`, `libmozavcodec.so`. libxul is stripped; nothing to remove without recompiling.
- Benefit is size (about 9 MB less per update over the hotspot), not speed: omni entries are demand-read, so start-up and RSS change by about 0 [E]. Measure with APK size and `am start -W`.
- What breaks if done carelessly: (1) the optimized layout (preload header, central directory first, startup order): a plain `zip` repack loses it, so use `mozpack.mozjar` (`JarWriter.preload()` reorders members to a given list; `MOZ_JAR_LOG_FILE` logs first accesses) [V S14]; (2) the startup cache is keyed by BuildID / `compatibility.ini` and bug 1535181 shows stale caches surviving changed omnijar content [N snippet]: bump the BuildID or clear the profile's startupCache on our version change; (3) Gradle dependency verification (if enabled) and the AAR checksum: do it as a build-time transform of the AAR, not by editing the cached file; (4) APK signature: re-signed by our key anyway; Gecko itself does not verify omni.ja, only add-ons (signature checks) [E].

### 4.3 Newer or different prebuilt engines
- Maven (S3, read 2026-11-12): release `geckoview-armeabi-v7a` latest 160.0.20261104181552 (updated 2026-11-10; majors 155 to 160 ship at about 2-week steps, 151 was 2026-06-08); beta `geckoview-beta-armeabi-v7a` latest 161.0.20261111090329 (2026-11-11); nightly `geckoview-nightly-armeabi-v7a` latest 162.0.20261111213251. MDN lists Firefox 161 for 2026-11-24 [N snippet].
- ABI: armeabi-v7a is published in all three channels [V S3]; Mozilla: "32-bit ARM devices will continue to be supported", Android 8.0 minimum from 144, 32-bit x86 dropped [V S4]; no later announcement found [N].
- Performance wins for ARM32 / low-end in 161 or 162: none documented. The Android 160 notes list no performance items [V S17]; the SpiderMonkey blog has no release newsletter after Firefox 135-137 [V S19]. Already inside 160: eager off-thread Baseline with hints (144, 1980830 [N snippet]), the IME re-attach fix (147). Not in 160: the script navigation cache (Nightly), NDK AChoreographer vsync (backed out, 2021258 [V S18]).
- Recommendation: stay on release; use beta only as an A/B for one named fix (the Gradle coordinate `geckoview-beta-armeabi-v7a`; compileSdk 37 / androidx.core constraints from ENGINES.md apply). Other prebuilt GeckoView AARs do not exist (forks ship apps, not AARs; ENGINES.md).

### 4.4 Other realistic ideas
- Optimized omni.ja layout: already done by Mozilla (section 1); a re-order from our own `MOZ_JAR_LOG_FILE` access log could only help if we repack anyway [E].
- Startup-cache priming: not realistic; it is BuildID-bound, written by the first run, and portability of the cached bytecode across ISAs is unverified [N]. Pre-warmed profile: nothing worth seeding; prefs come from YAML, Remote Settings dumps are already inside omni (`defaults/settings`) [V S1], an HTTP cache cannot be shipped in a public repo.
- Java side: a baseline profile for our dex (GeckoView's Java IME code runs in our process; the AAR ships none [V S1]) takes effect only after the idle+charging dexopt (HW_SPEED H9): P2.
- GeckoView env/args via YAML: nothing speed-related found.

## 5. D. Measurement plan (cheap first; nothing here was run)
### 5.1 Facts to collect with no adb (Diagnostics report)
Prefs in effect (`GeckoPreferenceController.getGeckoPrefs`) for every name in section 3; BuildID 20261104181552; per-process PSS (`getRunningAppProcesses` + `getProcessMemoryInfo`) to enumerate content / GPU / utility; enabled accessibility services (`AccessibilityManager.getEnabledAccessibilityServiceList`), `AutofillManager.hasEnabledAutofillServices()`, `Settings.Secure.DEFAULT_INPUT_METHOD`; `/proc/cpuinfo` Features (aes / sha2 decide crypto speed); thermal status (API 29); `MemoryInfo` headroom; battery-saver state; free storage; RTT to the hotspot gateway and one CDN (HEAD) plus a 1 MB fetch for link speed (a 2.4 GHz-only hotspot link, packet loss or MSS black holes would explain "slow" everywhere [E]). [V] all are public Android APIs (FREEZE.md, HW_SPEED).
### 5.2 Three tiny lab pages (assets/diag, offline, no network except net lab)
- Typing lab: `<input>`, `<textarea>`, contenteditable and a hidden-iframe contenteditable, with 0 / 20 / 60 / 120 ms of synthetic per-key work (a React stand-in). Per key: `beforeinput` -> `input` -> next rAF + MessageChannel tick, plus `PerformanceObserver({type:'event', durationThreshold:16})` entries; report p50 / p95 / max and gaps > 300 ms between keys. Event Timing is on in 160 (`dom.enable_event_timing`, `event_timing.enable_interactionid` true [V S2]); durations round to 8 ms. A native Android `EditText` next to it separates Gecko from the IME.
- JS lab: 6 kernels of at least 150 ms (JSON 2 MB, regex, sort 200k, Map/Set churn for GC, typed arrays, SHA-256 over 64 MB via WebCrypto as an S2K stand-in, a small wasm) x 5 runs; `privacy.reduceTimerPrecision` is 1 ms so kernels must be long. Used for the bytecode, parallel-marking, off-thread-Baseline, stylo-threads, FPP and nursery rows of 3.2.
- Net lab: per-navigation Navigation Timing from the built-in extension's content script (top frame, after `load`): `domainLookupEnd-Start`, `connectEnd-Start`, TLS, TTFB, `nextHopProtocol`, request count, `fromCache` ratio via `webRequest.onCompleted`; log to the ring buffer. Cross-origin Resource Timing is zeroed without Timing-Allow-Origin, so use the navigation entry and webRequest timestamps.
### 5.3 Profiler (the one tool that answers "JS, GC, layout, IPC or network?")
`ProfilerController.startProfiler(String[] filters, String[] features)` and `stopProfiler()` -> `GeckoResult<byte[]>` (gzip JSON, asynchronous), `addMarker`, `isProfilerActive` [V S1 javap, S15 Javadoc]. Filters are case-insensitive substrings of thread names. Start with `GeckoMain, Compositor, Renderer, DOM Worker, StyleThread` and features `js, stackwalk, cpu` [N: strings not verified on 160; the Firefox Profiler's Android presets use similar sets]. Capture 20 s of typing in the Proton composer and 20 s of a cold login load. In-app summary [E]: count samples per thread x category (JS, GC/CC, Layout, Graphics, Network, Idle) so the owner needs no PC; the file can also be opened in profiler.firefox.com ("from file", no upload) [N]. Reading it: GeckoMain busy > 80 % in JS during typing = JS-bound (prefs cannot fix it; page work and cores can); GC share > 15 % = memory / GC rows; Idle high but latency high = IME / Java / UI thread (Health log, native EditText comparison); Compositor / Renderer busy = GPU path. With adb (if ever): `dumpsys meminfo`, `dumpsys gfxinfo framestats`, `ps -T`, `dumpsys package dexopt` (is the dex `speed-profile` or `verify`?), a Perfetto trace with the categories in FREEZE.md section 6, simpleperf (Mozilla has a page for it, S15) [N].
### 5.4 Recommended A/B order (same pages every time: Proton login cold + warm, Wikipedia article, heavy news page, typing lab 0 / 60 ms; 5 runs, median and spread; owner time about 10 min per row)
1. Baseline = current build. 2. Shields OFF for proton.me (our hook vs engine). 3. Environment: other keyboard, accessibility / autofill services off, battery saver off, hotspot on 5 GHz if offered. 4. DNS: `network.trr.mode` 0 + Private DNS, then `request_timeout_ms` 800, then `native_https_query` false. 5. H3 off. 6. Bytecode strategy -1 (needs a 1st, 2nd and 3rd load). 7. processCount 2 (with PSS). 8. Only if the profile is JS/GC-bound: `gc_parallel_marking` true, `offthread_compilation_strategy` 0, `stylo-threads` 2, nursery 16 MB. 9. FPP off, translations off (cost check). Keep a change only if the median improves by at least 10 % and exceeds the spread, nothing else regresses by more than 5 %, and the privacy cost is accepted by the owner (steps 4 and 9 need an explicit yes).
### 5.5 What the numbers decide [E thresholds]
- Net lab DNS p50 > 150 ms or any TTFB > 1 s with TLS fine: DNS path first (row 4). `nextHopProtocol` never h3 or h3 slower: row 5. Gateway RTT > 20 ms or loss: it is the Wi-Fi link, not the browser.
- Shields OFF cuts load or typing p95 by > 30 %: the extension (D1, cosmetic CSS) is the lever, not the engine. No change: engine / page / CPU.
- Typing lab 0 ms fine but 60 ms bad: JS-bound page work, as expected; Proton composer much worse than the 60 ms lab: profile the composer. Native EditText also laggy: IME / system.
- PSS content > 300 MB or `onKill` during Proton: memory prefs / hibernation before any speed pref.

## 6. Not verified
- Anything measured on the tablet: every effect size above is [E] or [N]. The 160 StaticPrefList values came through a summarising fetch tool (paraphrase risk; the AAR-derived defaults are exact). Whether the integer-GB gate treats 1.9 GB as below 2 GB. Whether HTTPS-RR waits still apply under TRR first (inferred from 1895908 / 1898191). Whether Mozilla's armv7 release uses LTO (PGO is documented). GPU process default on Android (GECKO_SPEED says true).
- Proton internals: service worker, bundle sizes, wasm use, KDF, encrypted-search indexing on mobile UAs. Accessibility bug details (S6 are search snippets). Profiler thread-name filters and feature strings on 160. Whether Private DNS (TCP 853) works through this carrier hotspot. Cost of the Translations actor per frame. Bug status of 1943810, 2027803, 1670403, 1530057, 1935289, 1943901, 1980830, 1779940, 1872221, 1535181 (snippets only).
- Build-size numbers for Android objdir and source tree (no figure found); `b-linux-docker-xlarge-amd` machine size.

## 7. Sources
- S1 Local, Gradle cache: org.mozilla.geckoview:geckoview-armeabi-v7a:160.0.20261104181552 (libxul.so strings + `readelf -A -S`, omni.ja `unzip -v` + pref files + JS, classes.jar `javap`), the repo's built release APK (x86_64 variant, `unzip -v`), repo files geckoapp/src/main/assets/gecko-prefs.txt, GeckoEngine.kt, BrowserActivity.kt.
- S2 https://raw.githubusercontent.com/mozilla-firefox/firefox/FIREFOX_160_0_RELEASE/modules/libpref/init/StaticPrefList.yaml (windows 50k-600k chars).
- S3 https://maven.mozilla.org/maven2/org/mozilla/geckoview/geckoview-armeabi-v7a/maven-metadata.xml ; .../geckoview-beta-armeabi-v7a/maven-metadata.xml ; .../geckoview-nightly-armeabi-v7a/maven-metadata.xml
- S4 https://blog.mozilla.org/futurereleases/2025/09/15/raising-the-minimum-android-version-for-firefox/
- S5 https://bugzilla.mozilla.org/show_bug.cgi?id=1995911 , =1933696 , =1909993 , =1939678 (read); =1916381 (snippet)
- S6 https://bugzilla.mozilla.org/show_bug.cgi?id=961540 , =1479039 , =1499163 (snippets)
- S7 https://bugzilla.mozilla.org/show_bug.cgi?id=1895908 , =1897462 , =1898191 (read); =1907982 , =1664492 (snippets)
- S8 https://firefox-source-docs.mozilla.org/js/how-we-optimize.html ; https://searchfox.org/firefox-main/source/js/src/jit/JitHints.h (read); bugs 1935289 , 1943901 , 1980830 (snippets)
- S9 https://bugzilla.mozilla.org/show_bug.cgi?id=1779940 , =1872221 (snippets)
- S10 https://bugzilla.mozilla.org/show_bug.cgi?id=1851162 (read); =1774178 (snippet)
- S11 https://bugzilla.mozilla.org/show_bug.cgi?id=2043993 (read); =2027803 , =1670403 , =2042605 (snippets)
- S12 https://firefox-source-docs.mozilla.org/setup/linux_build.html ; https://firefox-source-docs.mozilla.org/contributing/build/artifact_builds.html ; https://firefox-source-docs.mozilla.org/mobile/android/geckoview/contributor/geckoview-quick-start.html ; https://raw.githubusercontent.com/mozilla-firefox/firefox/main/taskcluster/kinds/build/android.yml ; https://raw.githubusercontent.com/mozilla-firefox/firefox/main/build/moz.configure/lto-pgo.configure ; bugs https://bugzilla.mozilla.org/show_bug.cgi?id=1480006 , =632954 (read), =1530057 (snippet)
- S13 https://docs.github.com/en/actions/reference/runners/github-hosted-runners ; https://docs.github.com/en/actions/reference/limits
- S14 https://raw.githubusercontent.com/mozilla-firefox/firefox/main/python/mozbuild/mozpack/mozjar.py (read); https://bugzilla.mozilla.org/show_bug.cgi?id=1535181 (snippet)
- S15 https://mozilla.github.io/geckoview/javadoc/mozilla-central/org/mozilla/geckoview/ProfilerController.html (read); https://firefox-source-docs.mozilla.org/tools/profiler/instrumenting-android.html (snippet)
- S16 https://raw.githubusercontent.com/ProtonMail/WebClients/main/applications/mail/package.json
- S17 https://www.firefox.com/en-US/firefox/android/160.0/releasenotes/ ; https://developer.mozilla.org/en-US/docs/Mozilla/Firefox/Releases/161 (snippet)
- S18 https://bugzilla.mozilla.org/show_bug.cgi?id=2021258 (read)
- S19 https://spidermonkey.dev/blog/ (list read; "Saying goodbye to asm.js", 2026-05-20)
- S20 https://bugzilla.mozilla.org/show_bug.cgi?id=1943810 , =1817741 , =1908182 (snippets)
- S21 Repo: research/GECKO_SPEED.md, HW_SPEED.md, FREEZE.md, SCROLL.md, ENGINES.md, D1-DESIGN.md, docs/HARDWARE.md, tools/check-gecko-prefs.sh.
