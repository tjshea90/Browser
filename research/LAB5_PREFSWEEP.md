# LAB5 - Gecko preferences: what is worth changing, what the emulator can judge (audit 2026-11-17)

Audit of the 160 AAR (libxul strings, greprefs.js, geckoview-prefs.js, actor JS) + the repo's pref lists. Tags: [V] read this session, [R] from repo research notes, [M] memory of Gecko source (static-pref defaults are
compiled into libxul and unreadable: any default without [V] may be off), [E] estimate. `tools/check-gecko-prefs.sh` checked 46 names (0 missing); every name below exists in 160 [V].

## (a) Existing rows that are not clearly "keep"
| Pref = value | Verdict | Why / cost |
| - | - | - |
| `network.prefetch-next` = true | obsolete | engine default is already true [V]: a no-op line |
| `browser.sessionhistory.max_total_viewers` 6 + `contentViewerTimeout` 3600 | questionable (MEM + CPU) | engine auto is 5 at 2 GB and 360 s [R/V]; a frozen page keeps DOM / frames / JS heap, 20-60 MB each [E]: 120-360 MB held up to 1 h and walked by the cycle collector. Try 3. NOT measurable by the lab ruler |
| `surfacecache.min_expiration_ms` 180000, `layout.framevisibility.numscrollportheights` 2, `dom.lazy-loading.margin.*` "1500.0" | questionable (MEM / net) | decoded cache parks at its ~95 MB cap for 3 min and uploaded textures add as much (unified RAM); about 5 screens of locked images (+10-25 MB); one extra screen of image fetches competes with critical resources at 5 Mbit [E] |
| `browser.cache.memory.capacity` 65536, `image.mem.decode_bytes_at_a_time` 65536 | unproven | 64 MB ceiling vs a smaller auto value [M]; 4x longer decode slices |
| `gfx.webrender.precache-shaders` = true | questionable | all programs reloaded at every start in the GPU process while page 1 loads (0.1-0.5 s warm, seconds cold [E]); the lab cannot judge it |
| `dom.prefetch_dns_for_anchor_https_document` (ON by default) | questionable | 2-3 DoH queries per link host; volume never measured |
| `general.smoothScroll` = false | no-op for touch | wheel and keyboard only [R] |
Worst-case pref-induced RAM above engine defaults: ~300-650 MB (bfcache 120-360, decoded images 95-190, memory cache up to 64, lazy/locked images 15-45); typical 100-250 [E]. apz "Smoother" paints 1.7-2.0x the viewport
per repaint [R]. The GPU process (~50-100 MB) and utility process (~20-40 MB) are engine defaults.

## (b) New prefs, ranked
| # | Pref: now -> try | Mechanism / effect | Risk | Lab? |
| - | - | - | - | - |
| 1 | `captchadetection.actor.enabled` true -> false | [V] a Mozilla actor that only feeds Glean counters: MutationObservers (subtree, style attributes) in the reCAPTCHA / Turnstile / hCaptcha / DataDome / Arkose frames; ANS58 excluded only OUR scripts there, this is the missing half | none | apply without a sweep |
| 2 | `signon.rememberSignons` -> false, `browser.formfill.enable` -> false | [V] LoginManagerChild returns at once when off; the app stores no logins anyway (autofill off, BrowserActivity.kt:970); cuts per-key work on password pages (ANS58b) and a formhistory write per submit | none | needs a typing scene |
| 3 | `javascript.options.mem.gc_high_frequency_small_heap_growth` 300 -> 120, `..._large_heap_growth` 150 -> 110, `gc_low_frequency_heap_growth` 150 -> 110 | [M] Mozilla's own set for devices <= 512 MB; this tablet gets the defaults [V]. Heap overshoot 3x -> 1.2x live | more GC CPU | yes (bench2) |
| 4 | `image.animated.decode-on-demand.threshold-kb` 20480 [M] -> 8192 | GIF/WebP kept fully decoded up to 20 MB each: a GIF page can hold 100+ MB | CPU while animating | needs a GIF scene |
| 5 | `media.utility-process.enabled` true -> false | one process fewer (~20-40 MB [E]) | weaker isolation | PSS only |
| 6 | `browser.sessionstore.interval` 10000 -> 60000 | fewer state batches; the app already flushes on pause (BrowserActivity.kt:643) | up to 60 s of state lost on a crash | tiny |
| 7 | `ui.touch_activation.delay_ms` 100 [M] -> 40 | `:active` tap feedback 60 ms sooner | flash at scroll start | feel only |
| 8 | `gfx.canvas.accelerated.cache-size` 256 [M] -> 64 | caps the Skia GL cache on canvas / map pages | re-upload | partial |
| 9 | `extensions.update.enabled`, `extensions.systemAddon.update.enabled`, `network.connectivity-service.enabled` -> false; `services.settings.poll_interval` 86400 -> 604800 | idle wake-ups and data (open task B14) | stale lists | request counts only |
Carried over, never run: `nursery.max_kb` 16384, `gc_parallel_marking`, bytecode strategy -1, `offthread_compilation_strategy` 0, `stylo-threads` 2, `processCount` 2, FPP off, frame-rate divisor 2, the network A/Bs (tablet only).
Not proposed: `dom.min_background_timeout_value` / `dom.timeout.*` (moot: `dom.suspend_inactive` is on for Android and hidden sessions are inactive [R]), DNS TTL / connections / pacing / JIT thresholds (refused earlier), `javascript.options.asyncstack`.

## (c) Lab sweep candidates (8) and what the ruler can read
| # | Pref | A | B | Read | Judged by the lab ruler (/bench2)? |
| - | - | - | - | - | - |
| 1 | `browser.sessionhistory.max_total_viewers` | 6 | 3 | PSS after a 10-page heavy chain; Back time | NO (needs a page chain) |
| 2 | `javascript.options.mem.nursery.max_kb` | 65536 | 16384 | peak PSS, kernel CPU | yes |
| 3 | `javascript.options.mem.gc_parallel_marking` | false | true | GC pause p95, CPU seconds | yes (2 guest cores: weak) |
| 4 | `dom.script_loader.bytecode_cache.strategy` | 1 | -1 | compile CPU on loads 2 and 3 | NO (needs repeat loads) |
| 5 | `signon.rememberSignons` (+ formfill) | true | false | CPU per key on a password field | NO |
| 6 | the GC trio of (b)3 | 300/150/150 | 120/110/110 | PSS, GC time | yes |
| 7 | `layout.framevisibility.numscrollportheights` | 2 | 1 | PSS, decode CPU in a scripted scroll | NO |
| 8 | `image.animated.decode-on-demand.threshold-kb` | 20480 | 8192 | PSS, CPU on a GIF page | NO |
Recipe: debug builds take `debug.kestrel.gp1..gp8` = `name=value` (GeckoPrefs.writeConfig), set both A and B explicitly (user values persist in the profile), force-stop between runs, rotate the variants, compare medians
of content-process utime+stime, PSS and scenario time. The lab is x86 KVM with 2 guest cores and SwiftShader on the same host cores: it cannot judge thread-count prefs (stylo, helpers, processCount), GPU prefs or network prefs;
compare only within one run. Once-prefs (surfacecache.*, precache-shaders, processCount, utility-process) would need a debug YAML hook (exists now: the same gp channel writes into the YAML config).

## Run 1 of the sweep (run 44059232336, 2026-11-17 09:54Z; CANCELLED at 10:33Z after the emulator went offline)
Variants: base, base2 (the same config twice = the noise floor), nowarm (the LAB5c-1 config warm-up thread switched off), nursery16, gcpar, gcsmall (the GC trio), canvas64. Ruler: /bench2 (8 kinds of work x 3 passes).
17 good starts (rounds 1-2 complete, round 3 for nowarm / nursery16 / gcpar), then adb said `device offline` and every later start waited five minutes for nothing (no report was written: fixed in tools/emu/prefsweep.py,
it now saves after every start, checks `adb get-state` and stops after two dead starts). Medians (n = 2-3 per variant, so only a LARGE effect could show):

| Variant | n | bench pass 1 ms | bench pass 3 (warm) ms | engine ready s | total PSS MB (all app processes) |
| :- | -: | -: | -: | -: | -: |
| base | 2 | 2220 | 1242 | 1.49 | 514 (514, 515) |
| base2 (same config) | 2 | 1974 | 1222 | 1.61 | 470 (460, 481) |
| nowarm | 3 | 2187 | 1244 | 1.50 | 488 (488, 527, 413) |
| nursery16 | 3 | 2126 | 1243 | 1.56 | 468 |
| gcpar | 3 | 2202 | 1212 | 1.45 | 460 |
| gcsmall | 2 | 2062 | 1198 | 1.50 | 456 |
| canvas64 | 2 | 2006 | 1194 | 1.48 | 485 |

Reading: the two copies of the SAME configuration differ by 45 MB of memory and 0.12 s of engine-ready time, the warm bench pass varies within +-3 %: every preference variant sits inside that noise. So (1) NO GC / nursery /
canvas preference is adopted (nothing measurable on the emulator; a memory benefit would need more rounds, and a CPU cost cannot be judged here), and (2) `nowarm` (warm-up thread off) is not slower or faster than the
other starts on x86: the theory behind GeckoEngine.prewarmConfig (SnakeYAML's class loading moves from the engine-start critical path into the idle time before the first frame) concerns the tablet's slow in-order CPU
and an interpreter-only first run; the emulator cannot show it. It stays (no cost seen, a stack sample proved the cause); the owner's Diagnostics line `engine runtime-create-took` (UI thread) next to `engine config-warm`
(background thread) lets the two be compared on the tablet.

## Decisions taken without a sweep (zero or negligible behaviour change, work removed)
- `captchadetection.actor.enabled = false` ((b)1) - stack-evidence only: the actor is Mozilla telemetry and it runs MutationObservers in captcha frames (ANS58 jank).
- `signon.rememberSignons = false`, `browser.formfill.enable = false` ((b)2) - consistent with "no logins are stored"; removes per-key and per-submit work.
