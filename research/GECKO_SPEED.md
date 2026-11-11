# GECKO_SPEED - tuning GeckoView 160 (armeabi-v7a) for the MT8168B / 2 GB / hotspot tablet (R3)

Written 2026-11-11 by a research agent. No code changed; NOTHING here ran on a tablet or emulator (no KVM in the sandbox). Method: `javap -c` of the real `classes.jar`, the real `omni.ja` (prefs + JS), `strings` of the real `libxul.so` (118 MB), plus web sources (section 6).
Tags: **[V]** verified in those 160 artifacts; **[S]** read in Mozilla source/docs (Firefox *main*, so a value may differ from 160); **[N]** not verified (section 7); **[E]** my estimate. "Exists" = exact string in libxul.so or a `pref()` line / consumer in omni.
Defaults: [G] 160 greprefs.js / geckoview-prefs.js, [J] 160 GeckoRuntimeSettings bytecode, [Y] StaticPrefList.yaml of main. Sibling reports: ADBLOCK_ENGINE.md (R5), FREEZE.md (R6).
**In plain words:** Firefox's engine already uses the GPU, hardware video decoding and several speed tricks by default; there is no "turbo" switch to flip. What we can do is (1) stop it picking video formats this CPU cannot decode (AV1), (2) keep memory small enough that Android does not kill tabs, (3) start the engine earlier and open connections before the page is requested, (4) stop wasting hotspot data, (5) remove our own per-request delay (the ad-block round trip), and (6) ship a diagnostics page so the owner can report real numbers instead of guesses.

## 0. Decisions
1. **Pref plumbing:** the app writes `filesDir/gecko-config.yaml` at every start and passes it with `GeckoRuntimeSettings.Builder.configFilePath(path)`. [V] An explicit path is read in RELEASE builds; its `prefs:` map is merged OVER GV's own settings (YAML wins) and reaches Gecko as default-branch prefs via `MOZ_DEFAULT_PREFS` before anything starts, so `once`-mirror prefs (graphics, image caches) are honoured. Live toggles: `GeckoPreferenceController.setGeckoPrefs(.., PREF_BRANCH_DEFAULT)` (no allow-list). Not user.js / `-profile`. (section 1)
2. **Video:** `media.av1.enabled=false` (software dav1d in the content process is what YouTube-class sites may pick when `isTypeSupported` says yes, Bug 1716164; this CPU is 4x Cortex-A53). Leave VP9-over-MSE at its Android default (off unless a HW VP9 decoder exists) so sites choose H.264 -> MediaCodec hardware. NEVER enable `isolatedProcessEnabled` (Java decoders are refused in isolated processes). Mirror the "cellular" media-cache values onto Wi-Fi: GV does not see Android's metered flag.
3. **GPU:** there is nothing to switch on. Hardware WebRender + GPU process + accelerated canvas are the Android defaults and nothing in 160 blocks Mali-G52 (code blocklist read on main + the same `FEATURE_FAILURE_*` ids in 160's libxul + shipped `gfx.json` with one Android rule, Vivante). WebRender even carries a Mali video-path workaround for all devices (`u_mali_workaround_dummy` in 160's shaders; Bug 1692848: `textureSize(samplerExternalOES)` crashed Mali-G72/G76 video playback, G52 not named). Do NOT set `gfx.webrender.all/software`. Verify on the tablet (section 4), add a "graphics safe mode" switch (P2).
4. **Memory (2 GB Go):** `dom.ipc.processCount=1` (A/B; GV default 2 keeps one idle spare content process after the first tab takes the startup spare), decoded-image cap 96 MB (default ceiling here ~475 MB), bfcache cap 2 (auto would be 5 on Android at 2 GB; with Fission off bfcache may be inactive [N], probe it), disk cache fixed 512 MB (smart size can reach 1 GiB). Keep `fission=false` (the static default is true), `lowMemoryDetection=true`.
5. **Startup / network:** create the runtime right after the first frame (not +600 ms) and call `runtime.warmUp()`; `GeckoWebExecutor.speculativeConnect` the search host on address-bar focus and the target on commit; keep HTTP/3 and DoH-first but MEASURE both on the hotspot; `network.prefetch-next=false`; `setFetchPriorityEnabled(true)`.
6. **Biggest controllable page-load cost is not a pref:** every sub-resource waits for a blocking `onBeforeRequest` -> `sendNativeMessage` -> Android UI thread -> reply. R5 (in-extension JS engine, P0) fixes it; until then keep the UI thread idle.
7. **Animations:** `ui.prefersReducedMotion=1`, `image.animation_mode="once"`, `general.smoothScroll=false`, plus the app's own A1. `toolkit.cosmeticAnimations.enabled` does not exist in 160.
8. **Measure before tuning further:** CI `prefs-exist` test + in-app diagnostics page (WebGL renderer, codec matrix, jank, per-process PSS, prefs in effect) so the owner can report numbers.

## 1. How an app sets Gecko prefs in GV 160
| Mechanism | Applied | Branch / persistence | Limits (verified) | Verdict |
|---|---|---|---|---|
| `GeckoRuntimeSettings.Builder` setters (`Pref<T>`) | at start through env `MOZ_DEFAULT_PREFS`; live via `GeckoView:SetDefaultPrefs` | default branch, never persisted; at every start `commit()->commitResetPrefs()` clears USER values of these names | only ~60 names (`dom.ipc.processCount`, `fission.autostart`, `network.trr.*`, `webgl.msaa-samples`, `network.fetchpriority.enabled`...) | use for the typed ones |
| `extras` Bundle: `env0..N`, `args`, `argN`; `arguments()` | at start; env also reaches every child process (GeckoProcessManager passes `getActiveExtras`) | n/a | GV overwrites `MOZ_DEFAULT_PREFS` itself, so env cannot carry prefs | env vars only (`MOZ_*`) |
| **`configFilePath` YAML** (`env`, `args`, `prefs`) | start, before Gecko; `mergeIntoPrefs`: Java map first, YAML `putAll` last = YAML wins | default branch, not persisted | read in release only if the path is set explicitly (null => only debuggable/debug-app `/data/local/tmp`); parse error => whole file ignored, one log line; `org.yaml:snakeyaml` is a GV dependency and the AAR ships the R8 rule ("Bug 1838031 - Needed for SnakeYAML"); values bool/int/string (floats as strings) | **RECOMMENDED** |
| `GeckoPreferenceController.setGeckoPref(s)` / `getGeckoPref(s)` / `clearGeckoUserPref` | live after `create()`, queued by the EventDispatcher until Gecko is up; `@HandlerThread` (call on main) | `PREF_BRANCH_USER` persists in the profile; `PREF_BRANCH_DEFAULT` does not | **no allow-list**: `GeckoViewPreferences.sys.mjs` calls `branch.setBoolPref/IntPref/StringPref` on any name; `once` prefs ignore late changes (libpref docs); wrong type returns `false` | runtime toggles, DEFAULT branch |
| `user.js` in a profile + `-profile` arg | start, user branch | persisted | `-profile` is "a backdoor ... mostly for tests" (Bug 1037504); default profile is `files/mozilla/<salt>.default` [N]; GV-managed names are reset after start; a new profile orphans cookies/permissions; geckodriver uses it | not recommended |
```kotlin
// GeckoEngine.create(), before GeckoRuntime.create(): ~1 KB, skip the write when the content is unchanged
val cfg = File(ctx.filesDir, "gecko-config.yaml")
if (runCatching { cfg.readText() }.getOrNull() != yaml) cfg.writeText(yaml)
val settings = GeckoRuntimeSettings.Builder().configFilePath(cfg.path) /* + the existing calls */.build()
val rt = GeckoRuntime.create(ctx, settings)
rt.settings.setFetchPriorityEnabled(true)   // [J] GV's Java default is false and overrides the static default (true)
rt.warmUp()                                 // @UiThread, idempotent: launches the content process early ("speeds up the first page load")
// live toggle: GeckoPreferenceController.setGeckoPrefs(listOf(SetGeckoPreference.setIntPref("ui.prefersReducedMotion", 1, PREF_BRANCH_DEFAULT)))
```
```yaml
# config v1 = everything section 2.A recommends; generated by Kotlin from Prefs, unit-tested with snakeyaml in :core
prefs:
  kestrel.config.version: 1                   # canary: the diagnostics page reads it back; missing => the file was ignored
  media.av1.enabled: false                    # P0
  media.cache_readahead_limit: 30             # P1 hotspot / media
  media.cache_resume_threshold: 10
  media.cache_size: 65536
  network.prefetch-next: false
  dom.ipc.processCount: 1                     # P1 memory (A/B against 2 with per-process PSS)
  browser.sessionhistory.max_total_viewers: 2
  browser.cache.disk.smart_size.enabled: false
  browser.cache.disk.capacity: 512000
  image.mem.surfacecache.max_size_kb: 98304   # "once" pref: only honoured from this file
  dom.script_loader.bytecode_cache.strategy: 1
  ui.prefersReducedMotion: 1                  # P1 animations
  image.animation_mode: "once"
  general.smoothScroll: false
```
Gotchas: (a) never mix branches for one name: a stale USER value from an old `setGeckoPref(USER)` shadows the YAML default, so call `clearGeckoUserPref` for managed names on a config-version bump; (b) `once` prefs (`image.mem.surfacecache.*`, `image.cache.size`, `gfx.*`, `layers.gpu-process.*`, `media.hardware-video-decoding.enabled`) only work from YAML; (c) YAML lines for GV-managed names win over the builder, so do not set the same name twice; (d) CI must fail when a name disappears (section 4): 16 popular names (+ `media.benchmark.*`) are ABSENT in 160 (list in section 2.D).

## 2. Pref table (P = priority; Ap: S = startup only (YAML), L = live ok, N = next process launch)
### 2.A Apply
| Pref | Value | Ap | Effect and evidence | Risk | In 160 | P |
|---|---|---|---|---|---|---|
| `media.av1.enabled` | false | L | [Y] true. dav1d runs in the content process (Bug 1672276: "consumes more power"), Android MediaCodec accepts AV1 only with HW decode ([S] AndroidDecoderModule), yet sites (YouTube) still pick AV1 when `isTypeSupported` says yes (Bug 1716164). Off => H.264 | AV1-only streams fail; H.264 needs more bytes at equal quality [N] | Y | P0 |
| `media.mediasource.vp9.enabled` | do not set | - | [Y] `@IS_NOT_MOBILE@`: off on Android unless HW VP9 / H.264 HW missing / benchmark passes (Bug 1614958). Forcing true would make YouTube serve software VP9 | - | Y | keep |
| `media.cache_readahead_limit` | 30 | L | [Y] 60 (cellular 30). Progressive `<video>` buffers <=30 s ahead: less data wasted when a clip is abandoned. GV has no metered flag (no "metered" string in GeckoNetworkManager/libxul) so `.cellular` never applies on hotspot Wi-Fi | stalls on very flaky links | Y | P1 |
| `media.cache_resume_threshold` | 10 | L | [Y] 30 (cellular 10) | same | Y | P1 |
| `media.cache_size` | 65536 | L | KB, file-backed [Y] 512000 (cellular 32768) | none visible | Y | P1 |
| `network.prefetch-next` | false | L | [G] true. No speculative whole-resource downloads for `rel=prefetch/next` on a metered link | none visible | Y | P1 |
| `dom.ipc.processCount` | 1 | N | [J] 2, [G] 8. [S] PreallocatedProcessManager creates a spare only while the web-process cap is not reached and refills after `Take()`: with 2 a replacement spare is made once tab 1 takes the startup spare (idle process in steady state; dropped on `memory-pressure`), with 1 there is no replacement and tab 2 reuses the process (no spawn). Saves about one content process of RSS [N: measure PSS] | both live tabs share jank/crash/LMK fate; Fission already off | Y | P1 A/B |
| `browser.sessionhistory.max_total_viewers` | 2 | L | [G] -1 = auto: [S] nsSHistory table gives 5 viewers at 2048 MB on Android (bias 15.9), i.e. 4-5 here, across all tabs. A cap of 2 bounds memory. [S] `UpdatePrefs` sets the limit to 0 unless `BFCacheInParent()` (`fission.bfcacheInParent` [Y] true + session history in parent): whether bfcache is active with Fission off in 160 is [N], so probe it (section 4); entries also expire after 360 s [G] | none if inactive; older Back steps reload from HTTP cache | Y | P1 |
| `browser.cache.disk.smart_size.enabled` + `browser.cache.disk.capacity` | false + 512000 | L | [Y] true + 256000. Smart size follows free disk (about:cache showed 1 GiB on desktop, SUMO 1434023). A fixed cap saves repeat bytes on the hotspot and keeps storage predictable | cache dir may be purged by Android on low storage | Y | P1 |
| `image.mem.surfacecache.max_size_kb` | 98304 | S | [Y] 2024*1024 but `SurfaceCache::Initialize` caps 32-bit at 1 GiB and uses RAM/`size_factor`(4) => ~475 MB ceiling on 1.9 GB; memory-pressure discards via `discard_factor`. Cap decoded images at 96 MB | re-decode when scrolling image-heavy pages | Y | P1 |
| `dom.script_loader.bytecode_cache.strategy` | 1 | L | [Y] 0 (cache after 4 loads), 1 = after 2: less JS compile time on revisited sites (claude.ai) | more disk writes | Y | P1 |
| `ui.prefersReducedMotion` | 1 | L | LookAndFeel override read before the system value ([S] nsXPLookAndFeel; change re-evaluates media queries). Android's own source is `animator_duration_scale`==0 ([V] GeckoSystemStateListener) | JS-driven animations ignore it | Y | P1 |
| `image.animation_mode` | "once" | L | [G] "normal". GIF/APNG loop once (save CPU/GPU/data); "none" = first frame | spinner GIFs stop | Y | P1 |
| `general.smoothScroll` | false | L | [G] true (sticky). Keyboard/wheel scroll instant; touch fling is APZ, unchanged | none | Y | P1 |
### 2.B Typed builder / settings (not YAML)
| Setting | Value | Why |
|---|---|---|
| `setFetchPriorityEnabled(true)` | on | [J] Java default false overrides static true: `fetchpriority=high/low` on img/script/link is ignored today. P1, low risk |
| `fissionEnabled(false)` | keep | [Y] `fission.autostart` default is TRUE (mirror never), so the call is load-bearing. `extensionsProcessEnabled(false)` already equals the static default (R5/R6: extensions then run in the parent main thread) |
| `lowMemoryDetection(true)` | keep | registers GV's own `MemoryController` (ComponentCallbacks2) -> "memory-pressure"/"low-memory" notifications (GC, CC, cache purge) [V] |
| `isolatedProcessEnabled`, `appZygoteProcessEnabled` | never true | `AndroidDecoderModule`: Java decoders only if `!IsIsolatedProcess()` [S] => no HW video |
| `glMsaaLevel` (webgl.msaa-samples=4), `largeKeepaliveFactor` (10 on Android) | keep | `webgl.default-antialias` is false on Android; tile-based Mali makes 4x cheap; do not touch |
| `setParallelMarkingEnabled(true)` | P2 | [J] false; 2 helper threads when heap >=16 MB [G]; shorter major-GC pauses vs contention on 4xA53: measure |
| `aboutConfigEnabled(true)` | diag mode only | GV ships `config.xhtml`; lets the owner read/edit prefs on the tablet without adb |
### 2.C Experiments (P2, measure with the diagnostics page first)
| Pref | Try | Note |
|---|---|---|
| `gfx.webrender.precache-shaders` | true (S) | [Y] false. Android already persists shader binaries (`gfx.webrender.program-binary-disk` true on Android [Y]); precaching trades a slower first run for no first-use shader jank |
| `ui.touch_activation.delay_ms` | 0 (L) | [Y] 100: `:active` styling appears 100 ms after touch; instant press feedback; cosmetic flashes while starting a scroll |
| `network.http.http3.enable` | false only if UDP misbehaves | [Y] true; TCP backup starts after 100 ms (`backup_timer_delay`) and `fallback_to_h2_on_error` true, so the failure cost is bounded. Compare `nextHopProtocol` |
| `network.trr.request_timeout_ms` | 1000 | [Y] 1500 per lookup in TRR-first before native fallback; alternative: Android Private DNS (DoT) system setting and TRR off. Compare `domainLookupEnd-Start` |
| `permissions.default.image` | 2 (toggle) | data saver; icon fonts stay on (`webFontsEnabled(false)` would break icon fonts) |
| `gfx.downloadable_fonts.fallback_delay` | 1000 | [G] 3000: text shows in the fallback font sooner when a web font is slow |
| `browser.cache.disk.max_chunks_memory_usage` | 16384 | [Y] 40*1024 KB of unwritten cache data held in RAM |
| `layout.css.stylo-threads`, `javascript.options.mem.nursery.max_kb` (64 MB [G]) | lower | exist; effect unknown, no evidence either way |
| `widget.android.use-surfacecontrol` | true (S) | [Y] false, "render into child SurfaceControl" (API 29+); try only with the freeze A/B (FREEZE.md) |
| `webgl.sanitize-unmasked-renderer` | false in diag mode (L) | [Y] true hides the real GPU string |
| Background traffic candidates | audit first | `network.connectivity-service.enabled` [G] true (probes firefox-portal-detection.com), `extensions.update.enabled` / `extensions.systemAddon.update.enabled` [G] true, Remote Settings poll 24 h (`services.settings.poll_interval`, also delivers ETP lists via `moz-sbrs:`, CRLite, intermediates). Conflicts with the "no background network" rule |
### 2.D Leave alone / verified absent
- Leave at default: JIT (`javascript.options.baselinejit/ion/blinterp/wasm*` exist; no evidence of a problem on arm32, any tuning needs a benchmark first), `gfx.webrender.*`, `gfx.canvas.accelerated` (true on Android), `layers.gpu-process.enabled` (true on Android: isolates Mali driver crashes), `layers.acceleration.*`, `webgl.*`, `media.hardware-video-decoding.enabled`, `media.android-media-codec.*`, `network.early-hints.*`, `network.preconnect`, `network.http.speculative-parallel-limit` (20 [G]), `network.http.tailing.*` (trackers are delayed), `security.tls.enable_0rtt_data`, `network.dnsCache*` (TTL>=60 s + 600 s grace), `dom.ipc.processPrelaunch.enabled` (true).
- ABSENT in 160 [V] (do not copy from old fork lists): `network.http.rcwn.enabled`, `network.predictor.enabled`, `image.mem.max_decoded_image_kb`, `media.ffvpx.enabled`, `media.suspend-bkgnd-video.enabled`, `toolkit.cosmeticAnimations.enabled`, `dom.image-lazy-loading.enabled`, `gfx.webrender.force-disabled`, `gfx.webrender.enabled`, `javascript.options.asmjs`, `media.mediasource.webm.enabled`, `layers.omtp.enabled`, `dom.ipc.processPreload.enabled`, `fission.disableSessionHistoryInParent`, `javascript.options.mem.gc_high_frequency_heap_growth_max`, `network.ssl_tokens_cache_enabled`, `media.benchmark.*`.

## 3. App-level techniques
0. **Expected process tree** [N, enumerate it with the diagnostics page]: app/parent (Android UI thread + Gecko main thread + extension JS, because the extensions process is off), 1-2 content (`:tab`), GPU (`layers.gpu-process.enabled` true on Android [Y]), utility for audio decode (`media.utility-process.enabled` true [Y]); RDD/GPU-process video decoding is Nightly-only on Android [Y], so video decodes in the content process through MediaCodec. A socket process may or may not exist.
1. **Start order.** Today `Io.main.postDelayed(600)` creates the runtime. Do: write YAML (IO thread, joined before use) -> first frame (`doOnPreDraw`) -> `GeckoRuntime.create` -> `warmUp()` -> `ensureBuiltIn` -> open the first `GeckoSession` on `about:blank` while the native NTP is visible. VIEW intents: start before building the UI. Mark `ProfilerController.addMarker` at each step and log the timeline into the diagnostics page.
2. **Preconnect.** `GeckoWebExecutor(runtime)` (`@AnyThread`): on address-bar focus `speculativeConnect(searchOrigin)` (Google = first engine); on Enter/suggestion tap for a URL `resolve(host)` then `speculativeConnect(url)`. Rules: normal tabs only (a private tab's origin attributes differ, so the connection would probably not be reused [N]), top-level navigations only, never for half-typed text (it leaks keystrokes to DNS), <=1 per 3 s. Unused connections are closed by Gecko ("after an unspecified amount of time", Javadoc). Gain = DNS+TCP+TLS round trips [N: measure]. GV 160 has no Predictor (`network.predictor.enabled` gone), so this is the only learned/explicit preconnect we get.
3. **View backend.** Keep `BACKEND_SURFACE_VIEW` (default): own composition layer, no extra GPU copy. TextureView only as the R6 freeze A/B. Never resize or animate the GeckoView; NTP/error as overlays (FREEZE.md).
4. **Thumbnails.** `GeckoView.capturePixels()` is a GPU read-back of the visible surface; `captureFullPage()` is far heavier. Prefer title+favicon cards; if thumbnails stay: only when leaving a tab, scale to <=160 px on the IO executor, WebP, never while typing/scrolling. [N] cost on Mali: measure.
5. **Memory.** `setActive(false)` on every hidden session ("significantly reduces its memory footprint", Javadoc; also flushes session state). `setPriorityHint(PRIORITY_HIGH)` only on the visible session (today unused). `onTrimMemory`: >=10 hibernate non-current (done), 20 (UI_HIDDEN) hibernate all non-current, >=MODERATE hibernate current when unfocused (done). GV reacts to the same callbacks itself.
6. **Hibernation.** `SessionState` + `restoreState` brings history/scroll/form data back; history-type loads should be served from the HTTP cache (hence the bigger disk cache) [N: verify on the tablet that a restored tab causes no network].
7. **Main thread.** Filter verdicts, counters and logging off the UI thread (R5/R6). Metered mode: use `ConnectivityManager.isActiveNetworkMetered()` to apply the cellular-style media prefs, deny inaudible autoplay (`PermissionDelegate`, GV asks the app because `media.geckoview.autoplay.request` is true), and show data counters.
8. **Graphics safe mode (P2).** Hidden switch writing `gfx.webrender.software: true` (or `layers.acceleration.disabled`) into the YAML and restarting the process (they are `once` prefs), for a misbehaving Mali driver.

## 4. Measuring: CI vs emulator vs tablet
| Where | What it can tell | What it cannot |
|---|---|---|
| **CI, no device (every push)** | (1) `prefs-exist`: unzip the AAR, `strings -n4 libxul.so \| sort -u`, unzip `omni.ja`, assert every key of the generated YAML is an exact string / `pref()` / JS consumer, else fail (this report found 16 dead names in popular lists). (2) YAML parse test with snakeyaml. (3) Playwright + desktop Firefox with `firefoxUserPrefs` on a local server with injected latency: requests, bytes, DCL/load before/after our lists and JS/cache prefs (TASKS D3). | GPU, MediaCodec, arm32 JIT, memory, h3 on a carrier, anything about speed in absolute terms (patched desktop Firefox is not GV 160) |
| **Emulator (x86_64 API 29 on `ubuntu-latest`, KVM udev rule per ReactiveCircus README / GitHub changelog 2024-04-02, `-gpu swiftshader_indirect`)** | functional startup timeline, per-process PSS (`dumpsys meminfo`), prefs in effect (diag page -> logcat), idle-traffic audit: `-http-proxy` to a logging proxy, 10 min idle, list hosts + bytes (finds Remote Settings, portal probes, DoH) , relative A/B of memory prefs | real GPU/video/thermal/LMK; SwiftShader WebRender is not Mali; x86 JIT |
| **Tablet only** | hardware WebRender vs software, MediaCodec HW decode and dropped frames, 2 GB memory pressure, cold start, QUIC/UDP and DoH on the hotspot, touch latency, real YouTube | - |
**Diagnostics page** (A5): asset `diag/index.html` at `resource://android/assets/diag/` (same scheme as the extension), Kotlin passes data as base64 JSON in the URL fragment (no JS bridge), output is a text box + copy button; nothing leaves the device.
- Kotlin supplies: `Build`, `ActivityManager.getMemoryInfo`, PSS per process (`getRunningAppProcesses()` + `getProcessMemoryInfo`) to expose which child processes exist, `MediaCodecList(ALL_CODECS)` for avc/vp8/vp9/av01/hevc with `isHardwareAccelerated` / `isSoftwareOnly` (API 29), prefs in effect via `GeckoPreferenceController.getGeckoPrefs` (canary, processCount, av1, animation_mode...), startup timeline, last 20 navigations: `onPageStart` -> `onFirstComposite` -> `onFirstContentfulPaint` -> `onPageStop`.
- Page measures: WebGL (`WEBGL_debug_renderer_info` renderer/vendor with `webgl.sanitize-unmasked-renderer` off, MAX_TEXTURE_SIZE, MAX_SAMPLES); `navigator.hardwareConcurrency`; `matchMedia('(prefers-reduced-motion)')`; 10 s `requestAnimationFrame` frame-time histogram while a CSS transform/opacity layer, a 2D canvas and a WebGL scene animate (GPU/compositor health: software WebRender shows up as low fps); JS kernels each >=150 ms (`privacy.reduceTimerPrecision` is true: 1 ms clamp) incl. a small wasm; codec matrix `MediaSource.isTypeSupported` / `canPlayType` / `mediaCapabilities.decodingInfo` (`supported`, `smooth`, `powerEfficient`) for avc1.64001f, vp09.00.31.08, av01.0.04M.08, hev1; playback of four bundled 10 s clips (H.264 360p/720p, VP9 720p, AV1 480p; generated in CI) reporting `getVideoPlaybackQuality()` dropped/total; three fetches (one per origin) with `PerformanceResourceTiming` dns/connect/tls/ttfb and `nextHopProtocol` (h2 vs h3).
- bfcache probe: page A links to page B; on Back, `pageshow.persisted === true` means the bfcache is alive (decides whether `max_total_viewers` matters). Also log whether a restored (hibernated) tab hits the network (`PerformanceNavigationTiming.transferSize`).
- Hidden dev entries: `about:support` (GPU decision log, HW/SW codec table), `about:memory`, `about:processes`, `about:networking` (sockets, DNS, h2/h3), `about:checkerboard`, `about:config`. The toolkit chrome pages are in libxul/omni (aboutSupport/aboutMemory/aboutProcesses/aboutNetworking/aboutCheckerboard `.xhtml/.html`) but whether GV registers/renders them is [N]: try on the emulator APK first. Also a Profiler button (`ProfilerController.startProfiler/stop`); the owner uploads a profile manually if wanted.

**What the numbers decide** (so the first report is actionable):
- No hardware `video/avc` in `MediaCodecList` => H.264 is software too: the codec plan changes (then compare VP9/AV1 software cost). Hardware `video/x-vnd.on2.vp9` present => Gecko flips VP9-MSE on by itself (Bug 1614958); keep it. `video/av01` hardware absent is expected.
- avc1 720p30 `powerEfficient=false` or drop rate >5% => decode/compositor path problem: check `onFirstComposite`, try the TextureView A/B (FREEZE.md).
- Median rAF frame >20 ms on the simple transform test => suspect software WebRender or a slow Mali driver: compare the graphics safe mode (`gfx.webrender.software`) and keep whichever is faster.
- DNS p50 >150 ms or TRR failures => compare TRR off + Android Private DNS. `nextHopProtocol` never `h3` => UDP is filtered, the TCP backup already limits the cost; no change needed.
- PSS: content process >300 MB or total >900 MB with two tabs => lower caps (surface cache 64 MB, viewers 1) or hibernate earlier; if `processCount=1` saves <40 MB revert to 2 (arbitrary thresholds [E], tune after the first report). `pageshow.persisted` false => drop `max_total_viewers` from the config.
**Owner test script** (same steps for Kestrel G, Cromite, and each config version): cold start to first page (stopwatch), claude.ai login screen, Google search + first result, a heavy news article (scroll 10 screens), YouTube 480p for 60 s with the stats-for-nerds codec line, then Diagnostics -> Copy report and paste it into the chat.

## 5. Prioritised change list (effort S/M/L, risk)
| P | Change | Effort | Risk |
|---|---|---|---|
| P0 | YAML config + canary + `prefs-exist` CI test + prefs read-back in diagnostics | S/M | low |
| P0 | `media.av1.enabled=false`; keep VP9-MSE default; never isolated processes | S | low |
| P0 | Runtime right after first frame + `warmUp()` + preconnect on focus/commit | S | low |
| P0 | Diagnostics page with timeline, codec matrix, PSS, jank (gates every P1 decision) | M | low |
| P0 | Remove the per-request UI-thread round trip (R5 in-extension engine; FREEZE.md stop-gap) | L | medium |
| P1 | `setFetchPriorityEnabled(true)`; `setPriorityHint(HIGH)` for the visible tab | S | low |
| P1 | Memory caps: processCount=1 (A/B), surfacecache 96 MB, bfcache 2, disk cache 512 MB fixed | S | low-med (A/B) |
| P1 | Hotspot media/prefetch prefs + metered-state switch + inaudible-autoplay deny | S/M | low |
| P1 | Animations: reduced-motion, `image.animation_mode`, smoothScroll (+ app A1) | S | low |
| P1 | Bytecode cache strategy 1 | S | low |
| P2 | Idle-traffic audit then cut portal probe / add-on update checks | M | low |
| P2 | HTTP/3, DoH vs Private DNS, TRR timeout: measure on the hotspot | S | low |
| P2 | Parallel marking, precache shaders, stylo threads, touch delay, SurfaceControl, graphics safe mode, thumbnails policy | S each | low-med |

## 6. Evidence and sources
- [V] artifacts: `classes.jar` (`GeckoRuntime.init`, `GeckoLoader.setupInitialPrefs`, `DebugConfig.mergeIntoPrefs`, `GeckoRuntimeSettings.<init>`/`commit`, `GeckoThread.getEnvFromExtras`, `GeckoProcessManager.start/preload`, `MemoryController`, `GeckoSystemStateListener`, `ContentBlocking$Settings`), `omni.ja` (`GeckoViewPreferences.sys.mjs`, `GeckoViewStartup.sys.mjs`, `greprefs.js`, `geckoview-prefs.js`, `defaults/settings/blocklists/gfx.json`: one Android rule, Vivante GC7000UL), `libxul.so` strings (pref names, `FEATURE_FAILURE_*` ids matching GfxInfo.cpp, `aboutSupport.xhtml`).
- Config and prefs: https://firefox-source-docs.mozilla.org/mobile/android/geckoview/consumer/automation.html (YAML keys, `configFilePath` reads in release, parse error ignores file) ; https://mozilla.github.io/geckoview/javadoc/mozilla-central/org/mozilla/geckoview/GeckoPreferenceController.html (user vs default branch, experimental) ; https://bugzilla.mozilla.org/show_bug.cgi?id=1502053 (managed prefs via default branch + env) ; https://bugzilla.mozilla.org/show_bug.cgi?id=1951985 and 1888979 (managed prefs reset at start) ; https://bugzilla.mozilla.org/show_bug.cgi?id=1037504 (`-profile` backdoor) ; https://bugzilla.mozilla.org/show_bug.cgi?id=1547717 (YAML overrides app settings) ; https://raw.githubusercontent.com/mozilla-firefox/firefox/main/modules/libpref/docs/index.md (load order, user.js read-only, `once`/`always`/`never` semantics).
- Defaults: https://raw.githubusercontent.com/mozilla-firefox/firefox/main/modules/libpref/init/StaticPrefList.yaml (all [Y] values; read in 100 kB windows).
- GPU: https://raw.githubusercontent.com/mozilla-firefox/firefox/main/widget/android/GfxInfo.cpp (WebRender blocked only for Mali-4xx, PowerVR G6110 and some driver builds, Vivante GC7000UL; Mali-G not mentioned; Mali-T lose optimized shaders only) ; https://raw.githubusercontent.com/mozilla-firefox/firefox/main/gfx/config/gfxConfigManager.cpp (WebRender enabled by default, Android uses GL) ; https://bugzilla.mozilla.org/show_bug.cgi?id=1689947 (Mali-G31 blocked in 2021; no such rule left on main) ; https://bugzilla.mozilla.org/show_bug.cgi?id=1692848 (Mali-G72/G76 textureSize workaround, WebRender re-enabled) ; https://doc.servo.org/webrender/device/gl/index.html (is_mali_bifrost/midgard/valhall).
- Video: https://raw.githubusercontent.com/mozilla-firefox/firefox/main/dom/media/platforms/android/AndroidDecoderModule.cpp ; https://bugzilla.mozilla.org/show_bug.cgi?id=1614958 (VP9 MSE) ; https://bugzilla.mozilla.org/show_bug.cgi?id=1716164 and 1672276 (AV1, dav1d in content process) ; https://bugzilla.mozilla.org/show_bug.cgi?id=1540573 (cellular media cache). MT8168 specs (decode list unclear): https://www.notebookcheck.net/MediaTek-MT8168-Processor-Benchmarks-and-Specs.483534.0.html
- Processes / bfcache: https://raw.githubusercontent.com/mozilla-firefox/firefox/main/dom/ipc/PreallocatedProcessManager.cpp (spare process rules) ; https://raw.githubusercontent.com/mozilla-firefox/firefox/main/docshell/shistory/nsSHistory.cpp (`UpdatePrefs`, `CalcMaxTotalViewers`, Android table) ; https://bugzilla.mozilla.org/show_bug.cgi?id=1689601.
- Images/cache/animations: https://raw.githubusercontent.com/mozilla-firefox/firefox/main/image/SurfaceCache.cpp ; https://raw.githubusercontent.com/mozilla-firefox/firefox/main/widget/nsXPLookAndFeel.cpp ; https://support.mozilla.org/en-US/questions/1434023 (smart size).
- API docs: https://mozilla.github.io/geckoview/javadoc/mozilla-central/org/mozilla/geckoview/GeckoRuntime.html (`warmUp`) ; .../GeckoWebExecutor.html (`speculativeConnect`, `resolve`) ; .../GeckoSession.html (`setActive`, `setPriorityHint`) ; .../GeckoView.html (backends, capture).
- CI: https://playwright.dev/docs/api/class-browsertype (`firefoxUserPrefs`) ; https://github.com/ReactiveCircus/android-emulator-runner ; https://github.blog/changelog/2024-04-02-github-actions-hardware-accelerated-android-virtualization-now-available/
- Technologies and where they stand in 160 [V names]: HTTP/3 + 0-RTT, Early Hints, preconnect, `fetchpriority`, stale-while-revalidate, request tailing for trackers, JS bytecode cache in HTTP cache, WebRender + APZ + Stylo, JPEG DCT-scaled and WebP scaled decode, downscale-during-decode, lazy image loading (always on, pref gone), MediaCodec surface decode, bfcache, program-binary shader cache on Android. All on by default; the work is configuration (above), not new engine features.

## 7. Not verified
- That the YAML is parsed and applied in a RELEASE GV 160 on a device (bytecode + docs only): the canary read-back is the test.
- [Y] values are from Firefox main, not the 160 tag; names are verified in 160, values may drift (e.g. surface cache factor, `fission.autostart`).
- Hardware WebRender actually initialising on this Mali-G52 driver (blocklist says allowed; emulator uses SwiftShader). MT8168 HW decode for VP9/HEVC/AV1 (public specs conflict): needs `MediaCodecList` on the tablet.
- Which auxiliary processes exist on a release Android build (gpu, socket, utility, rdd): measure PSS per process. Auto bfcache size and smart disk-cache size on this tablet. `restoreState` network behaviour.
- Cost numbers: `GeckoRuntime.create` on the UI thread, cold start, thumbnail read-back, preconnect gain, process-count saving, DoH/h3 effect on the hotspot. Whether `about:support/memory/processes` open in GV. `ui.prefersReducedMotion` and `ui.touch_activation.delay_ms` behaviour in GV content.
- JS GC/JIT tuning, Stylo thread count, nursery size, `SetGCParametersBasedOnAvailableMemory` use on Android: no source found; left at defaults. Fenix/Focus/Mull/IronFox pref lists: none found, none used.
