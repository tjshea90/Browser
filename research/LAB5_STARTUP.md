# LAB5 - what the app does on the UI thread from process start to the first usable frame (audit 2026-11-17)

Read-only audit of the start path (tags: [R] read in code, [V] checked in the AAR / POM / manifest, [G] guess). EVERY A53 millisecond figure is an estimate scaled from lab reasoning; nothing here was
measured on the tablet. Paths: G = geckoapp/src/main/kotlin/app/kestrel/browser, A = app/src/main/kotlin/app/kestrel/browser.

## Facts that shape the list
- `GeckoRuntime.create` cannot leave the UI thread [V] (`attachTo` starts with `ThreadUtils.assertOnUiThread`; then startCrashHelper bindService, font-scale listener, ProcessLifecycleOwner observer).
  Creating it in `Application.onCreate` only reorders (first pixels ~0.9 s later): do NOT.
- `restore()` (G/BrowserActivity.kt ~558) is a normal message held behind ViewRootImpl's sync barrier, so `create` runs right after the first draw. `StartTimeline frame` is taken before that traversal:
  use `look` as the first-pixels mark. Everything before `restore()` delays first pixels; `create` delays touch response.
- Emulator profile (run 9, `ui prof`): 1.25 s of the block is Android's own first-draw renderer sync (HardwareRenderer.setStopped, platform), then SnakeYAML `Yaml.load` (GeckoView reads its YAML config on the UI
  thread inside create), then a `GeckoNetworkManager.getDhcpInfo` binder call. DONE: LAB5c-1 `GeckoEngine.prewarmConfig` (config parse on a background thread; kill switch `debug.kestrel.nowarm=1`; sweep A/B pending).

## Ranked changes
| # | Change and where | Cost now (A53 guess) / fix | Gain, risk | Lab check |
| - | - | - | - | - |
| 1 | Warm thread part 2 (G/GeckoApp.kt:34, GeckoEngine.kt:83) | prefs XML `kestrel`, `kestrel_update`, `kestrel_video` parsed on the UI thread (A/data/Prefs.kt:13, UpdateManager.kt:130, G/DeviceVideo.kt:43; 5-30 ms); last-look WebP decode on the UI thread in `earlyCover` (G/BrowserActivity.kt:545,1101-1129; A/ui/LastLook.kt:62; 25-80 ms); `SessionState.fromString` (:1394) pure JSON 3-15 ms | -40..-150 ms before first pixels; low (skip when clearOnExit; fallback = today's path) | nowarm kill switch, >= 5 interleaved cold starts, compare look / frame / runtime / runtime-create-took |
| 2 | Stop walking `cache2/entries` (A/diag/StorageInfo.kt:103-117 `purgeDoomedCache`, called at G/GeckoEngine.kt:106) | every cold start the nice-13 lane lists the whole engine cache (owner's was 228 MB, up to 512 MB allowed = 10-40k files) while libxul/omni page in from eMMC; only `cache2/doomed` and `trash*` are needed | 0..several hundred ms of start I/O, 5-10 MB garbage; no risk | 30k files in cache2/entries, A/B the runtime/paint marks; Health line with visited count + ms |
| 3 | Baseline profile (geckoapp/src/main/baseline-prof.txt) | none exists (see LAB5_PACKAGING.md section 4) | ~20-35 % of Java-side start after the next dexopt | `cmd package compile -m verify -f` vs `-m speed-profile -f` |
| 4 | First-frame diet (G/BrowserActivity.kt:510-513,521,582; A/ui/DownloadPopup.kt:98-104; GeckoApp.kt:28) | `DownloadController.init` (HandlerThread, 2 engines, SqliteStore, Remuxer, PhoneBridge, ProxyLink thread, directory scan, 2 binder calls; 20-70 ms; `DownloadPopup.attach` calls it again in onStart), `UpdateManager.init` (second XML file + 2 getPackageInfo; 10-40 ms), `setRecentsLook` inside the pre-traversal frame callback (192 px bitmap + setTaskDescription binder; 5-15 ms), `setFetcher` 3-8 ms: run after `runtime` mark + 300 ms | -40..-130 ms to first pixels; low-medium (attach must move with init; menu dot re-set by the listener at :479) | activity -> frame/look marks |
| 5 | Defer the daily update check + interrupted-download resume (G/BrowserActivity.kt:602; A/download/DownloadController.kt:186) | 5-7 TLS requests (GitHub, Mozilla) begin the moment the app is in front, on the 5 Mbit link the first page needs; wait for first page stop or +20 s | -100..-300 ms first page of each day; no risk | shaped-net profile, force a due check with `debug.kestrel.feed` |
| 6 | Filter loader at normal priority until published (A/shield/Engine.kt:42) | `kestrel-engine` is Java priority 3 = nice 13 (background cgroup); until `Engine.peek()` is non-null the app returns no cosmetic CSS / scriptlets / malware check / query-strip (ShieldsBridge.kt:219,321,337,346; BrowserActivity.kt:3265; NavPolicy.kt:24); request blocking is unaffected (extension's saved copy) | shrinks that window (seconds); low-medium (4 cores busy in the first 2 s) | `filters-ready` Health line; count `cosmetic css len=0` on the first page |
| 7 | Micro bundle (each < 15 ms): `getRequestedOrientation` binder call every start (G/BrowserActivity.kt:489; needed only when savedInstanceState != null); `getPackageInfo` for salt (ShieldsBridge.kt:431) and twice in UpdateManager.kt:133-134; KeyboardProbe 0-width PopupWindow = a second window on API 29 (KeyboardProbe.kt:41-53, attached at BrowserActivity.kt:846): attach at first address-bar focus or +3 s; child-process init (GeckoApp.kt:9-27 runs in EVERY Gecko child: move under the UI-process guard) | 20-70 ms total | no risk, except the popup change (first keyboard measure) | per-call SystemClock Health lines; typing step |
| 8 | Read-backs only while the finger is idle (G/BrowserActivity.kt:1975-1985,2883-2889,2999-3010; TabDelegates.kt:143) | `capturePixels()` is a synchronous compositor call (2 s on a heavy page in the lab); runs at navigation, +1.5 s and +4 s after every page stop, 0.7 s after each scroll pause; back-off starts at 400 ms | possible 20-100 ms hitches when reading resumes (Mali unmeasured); low | NOT judgeable on software GL (1-10 ms there): add a read-back histogram, read the owner's Diagnostics |
| 9 | Build hidden views on first use (G/BrowserActivity.kt:978-980,1003-1069) | MiniNav (1.6k lines), PlayerControlsView, 2x QualitySheet, InlinePlayer, TabGrid, FindBar, SuggestionList, DownloadPopup, turn cover all built before the first frame while GONE | -20..-80 ms; MEDIUM risk (z-order, isInitialized guards) | activity -> frame |

## Reviewed, small, no action
InitializationProvider before Application.onCreate (3-10 ms; needed for ProcessLifecycleOwner), field initialisers, Pal x2, Shields.init, LaunchLogo, `buildUi` itself (80-250 ms, the biggest single piece: see row 9),
onResume's 3 thread starts, restoring 26 Tab objects, ShieldsBridge.install (3 HandlerThreads, 10-40 ms), writeConfig (~2 ms).

## Already handled
Engine after the first frame (BrowserActivity.kt:519-538; but `ensureSession` at :1355 creates it first); session / tab-state / thumbnail reads, NetworkMonitor, purge and job scheduling, lists at +90 s, decoder warm at +20 s,
tidy at +25 s, DeviceVideo probe are all off the UI thread; filter loader and 3 shield HandlerThreads are own threads; UI-process guard for init; extension not reloaded each start; Health.log without String.format; WAL.

## Steady state
UI thread per extension message ~20-50 us (GeckoResult + post); the real idle cost is Neon at 10 Hz (RenderThread 3.8 % of the 5.4 % idle in LAB3; owner's call, kept) and the watchdog (4 wakes/s) + CpuMeter
(re-reads /proc/<pid>/cmdline of every process each 5 s: negative-cache non-app pids). Together under 1-2 % CPU.

## Dependencies
profileinstaller 1.4.0 and startup-runtime 1.1.1 come through GeckoView's POM (lifecycle-process 2.11.0). media3 1.11.1 and GeckoView ship no baseline profile (research/ANS60_PLAYBACK_OPTIMISATION.md:47 claims otherwise: wrong).

## Where this audit guesses
Every A53 number; how the ~865 ms block splits between create / first-draw sync / compositor handshake at setSession (BrowserActivity.kt:1295; the new `runtime-create-took` and `gecko layout ... took` lines will say);
how many files the owner's cache2/entries holds; whether the tablet's oat is verified (`dumpsys package dexopt`); Mali read-back cost; wildcard support in the profile compiler.
