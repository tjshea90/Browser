# BROWSER_TECHNIQUES: what major browsers do for fast loading, smooth video, smooth animations, tab switching and Back (owner, 2026-11-24, PERF18)
Owner, verbatim: "Research online what techniques and settings and methods major browsers use for fast page loading, smooth video playback, smooth page animations, and caching for quick tab switching and back button. Implement and build any findings that would improve the browser".
Method: web searches 2026-11-24 (MDN, web.dev, Chromium blog, Mozilla support and bugzilla, Fenix performance docs) plus the repo's own earlier audits (research/GECKO_SPEED.md, GECKO_TUNING_2.md, LAB5_PREFSWEEP.md, SPEED2.md, SCROLL.md, INSTANT_BACK.md, TAB_SWITCH.md, VIDEO_*). A technique the earlier audits already decided is marked HAVE with the file, so nothing is redone. Public repo: site kinds only.

Reading the table: "gain here" is for THIS tablet (4 slow Cortex-A53 cores, 32-bit, 2 GB, one content process, hotspot, software drawing).

## 1. Fast page loading
| Technique | Who | Kestrel G today | Gain here | Verdict |
| :- | :- | :- | :- | :- |
| Preload scanner / speculative parsing (find resources before the parser gets there) | Chrome, Firefox, Safari (engine) | the engine's own | already on | HAVE (engine) |
| Preconnect and DNS look-ahead (on typing, on touch, to the top suggestion, to result links) | all | typed suggestion, search results, touch warm-up, look-ahead DNS; `engine warm` at start; MDN "Speculative loading" | high on a flaky link | HAVE (research/SPEED2.md) |
| Speculation Rules prefetch / prerender | Chrome (prerender), Firefox (prefetch, `dom.speculation_rules.enabled`) | prefetch on; whole-page prerender cannot run: 2 GB and CPU-bound pages | prerender would cost the CPU the page needs | HAVE prefetch; SKIP prerender |
| HTTP cache first for history navigations (no revalidation on Back/Forward) | all | engine does it (`kind=cache` in the Back lab); 512 MB disk cache, 64 MB memory cache | Back from cache 2.5 to 5 s mostly CPU | HAVE |
| Script bytecode cache | Firefox, Chrome code cache | `dom.script_loader.bytecode_cache.strategy = 1` (compiled after 2 loads) | 5 to 15 % on repeat loads of script-heavy pages | HAVE |
| Early Hints (103), HTTP/2, HTTP/3, Brotli, no Server Push (removed by Chrome 2022, Firefox 2024) | all | engine; HTTP/3 has a switch (off on the owner's hotspot) | link-bound pages only | HAVE (engine) |
| Image and iframe lazy loading with a margin | all | `dom.lazy-loading.margin.*` 1500 px, decode 2 screens ahead | fewer blank areas, more RAM | HAVE (alpha20s) |
| Pre-started renderer / spare content process | Chrome, Fenix (`dom.ipc.processPrelaunch`) | one content process always alive; 1 spare on Android already | new tab spawn cost avoided | HAVE |
| Start-up snapshot of the last page ("freeze-dried tabs": Chrome Android starts 13 % faster) | Chrome | the last-look picture (EY-1), sharp at the first frame | first impression at 0.7 s | HAVE (research/COLD_START.md) |
| **Open on the start screen after a long break; load the old tab only on request** | **Firefox for Android (4 h rule), Chrome start surface** | **was missing: the heavy page in front loaded at every start (12 to 27 s of CPU)** | **large: start is usable at once** | **DONE in alpha100 (research/LAZY_RESTORE.md)** |
| Defer extension late start-up (Fenix bug: ~200 ms on a Cortex-A55 start) | Fenix | the Shields engine must answer from the first request; it cannot wait | none safe | SKIP (blocker first) |
| A per-request blocking filter that is cheap | Chrome MV3 declarativeNetRequest (rules run natively), Firefox supports it too | the blocker judges each request in extension JS: 0.6 to 1.8 ms each, about 1500 requests on a heavy page; A/B on a heavy app: 41 s with Shields up against 24 s down | the largest measured cost that is OURS | NEXT (own task, see section 6) |

## 2. Smooth video playback
| Technique | Who | Kestrel G today | Verdict |
| :- | :- | :- | :- |
| Hardware decode through MediaCodec; the decoder's frame rate should match the stream (Mozilla bug: Android decoder output must run at the stream frequency or frames drop) | Chrome, Firefox | engine uses the hardware decoder; the format policy answers "no" for what the chip cannot do so sites send H.264; Kestrel's own player takes over with "at most 30 frames" | HAVE (VIDEO_AUDIT.md); dropped frames 1.6 % on the owner's tablet |
| Pick the stream by device and link (adaptive bitrate, resolution cap) | all | HLS rung cap by screen size, stall-driven picture size | HAVE |
| Suspend media and decoding of background tabs | Chrome, Firefox (`media.suspend-bkgnd-video`) | `setActive(false)`, SND1 safety net | HAVE |
| Overlay / dedicated surface for video (skip compositing) | Chrome, Firefox Android | the engine decides; the app's own player uses its own surface | HAVE |

## 3. Smooth page animations
| Technique | Who | Kestrel G today | Verdict |
| :- | :- | :- | :- |
| Animate only `transform`, `opacity` (and `filter`, `clip-path` where supported) so the compositor runs them without the main thread; `will-change` sparingly (web.dev "Stick to compositor-only properties") | ALL browsers; this is a site rule | engine's off-main-thread animations are on by default; nothing for a browser to switch | HAVE (engine) |
| Respect `prefers-reduced-motion`; stop GIF loops | all | Settings > Video > Calm pages (`ui.prefersReducedMotion`, `image.animation_mode`), OFF by default | HAVE (switch) |
| A steady lower frame rate instead of an erratic high one | Chrome (frame throttling of off-screen frames), Firefox `layout.frame_rate` | Settings > Network lab > Calmer redrawing (30, also sets the vsync, so scrolling is 30 too), OFF by default | HAVE (switch); proof needs the tablet |
| Throttle hidden / off-screen frames and timers | all | engine; our own Neon glow steps at 10 Hz and holds still while a page loads or a finger moves | HAVE |
| Draw on the GPU | all | NOT here: the tablet's GPU driver loses horizontal bands of text, so Software drawing is ON (owner confirmed the fix). Page scripts and drawing now share the four slow cores | decision made; a faster safe mode needs tablet bisection (PAGE_SPEED_2026-11-23.md section 3) |
| Measure it | Chrome / Firefox profilers | NEW in alpha100: a frame-cadence line per page (see section 6) | DONE |

## 4. Tab switching and Back
| Technique | Who | Kestrel G today | Verdict |
| :- | :- | :- | :- |
| Back/forward cache (whole page frozen in memory): Chrome Android since 96, Safari for years, Firefox; blockers = unfinished loads, open connections, some APIs; evicted on memory pressure | all | `max_total_viewers = 6`, 1 h; Instant Back (no-store rewritten), stop-on-leave so the page is "finished" when left (alpha97); engine kept 13 of 18 in the owner's last uploads | HAVE |
| Show the old page picture until the new one paints | Chrome (Android 14 predictive back), Safari | Back pictures, last look, curtain | HAVE |
| Keep recent tabs alive, discard the oldest first: Chrome discards internal pages first, then long-unused tabs, the selected tab last; freezes background renderers after about 5 minutes; "memory saver" | Chrome | LiveBudget: budget follows free memory (alpha98), least recently used sleep first, never the tab in front or a private tab; background pages inactive | HAVE; live switches measured 0.08 to 0.43 s |
| Per-tab window cost: Fenix measured 200 to 400 ms to create a new tab's engine window | Fenix | one shared engine view, sessions swapped; a revive is 0.6 s | HAVE |
| Restore only the tab in front at start | Firefox, Chrome | yes, and now not even that after a long break | DONE (see section 1) |

## 5. What the research does NOT change
- Settings that "major browsers use" but that this engine build lacks: `network.predictor.*`, `network.http.rcwn.enabled`, `media.ffvpx.enabled` are absent from GeckoView 160 (LAB5_PREFSWEEP.md, checked in libxul). Nothing to copy.
- JIT, GC and thread-count prefs: refused earlier without a tablet benchmark (LAB5_PREFSWEEP.md "Not proposed"); a change of those needs the owner's tablet, not guesses.

## 6. Built in alpha100 (this task) and what is next
1. **Lazy restore after 4 h** (above): the largest start-up saving there is for a heavy page, using the rule two major browsers use.
2. **Measure animation smoothness**: pagetime.js now samples the page's own frame callbacks twice after load (a 2.5 s window each) and reports `fm=frames/gaps over 50 ms/gaps over 100 ms/longest gap`; the digest SPEED section prints the median per drawing mode, so Software drawing against Calmer redrawing is judged on numbers, not feel.
3. **Time the bot checks**: pagetime.js recognises a CDN "checking your browser" page (title and page markers) and reports how long it stayed (`challenge host= ms=`); the digest PAGE COMPATIBILITY section prints count and median. This is the first real measurement of the slow checks.
4. NEXT, own task (PERF19): a cheaper blocker. Options ranked in section 1 last row: a decision cache by site and type in the extension; skipping requests to the page's own site for rules that cannot match first-party; later, native rules (declarativeNetRequest) for the bulk of the list. The A/B on a heavy app (41 s against 24 s) says this is where the measured time is.

Sources: MDN Speculative loading; web.dev (bfcache, compositor-only properties, animations); blog.chromium.org 2021/03 advanced memory management (freeze-dried tabs, tab discarding); Chromium OS tab discarding design doc; support.mozilla.org "Choose your opening screen for Firefox for Android"; Mozilla session restore wiki and bugzilla 648683 / 873771 / 1613227 (per-tab window cost) / 1550073 (GeckoView start-up); Mozilla bugs on Android MediaCodec output rate.
