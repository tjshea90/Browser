# FIREFOX_UPGRADE_161: "a new Firefox version is out. put it in kestrel. Make sure it is optimized for my hardware." (owner, 2026-11-24, FF1)
Owner, verbatim: "a new Firefox version is out. put it in kestrel. Make sure it is optimized for my hardware."
Tags: [V] verified in a primary source or in the engine's own files, [M] measured on the owner's tablet, [E] estimate, [N] not found / not proven.

## 1. What is out [V]
- Firefox 161.0 was released on **2026-11-24** (Firefox release notes, desktop and Android pages). GeckoView `161.0.20261118180411` is the newest on Mozilla's Maven (`maven-metadata.xml`: latest = release = 161.0.20261118180411, last updated 2026-11-24). It has an armeabi-v7a build (the only one this 32-bit tablet can run).
- The Web Compatibility Interventions extension (`feature-webcompat`) has a `161.0` release: pinned `161.0` (was `160.0.1`).
- Security: 161 carries "various security fixes" (advisory mfsa2026-131); 160.0.1 (2026-11-17) had mfsa2026-130. Kestrel G renders the open web, so the engine version is a security setting: a month behind is the exposure.
- Android notes for 161 (verbatim items that matter here): "Improved performance on some devices for web pages that use blur and similar effects behind page elements" (the only performance item); others are UI of Mozilla's own app (search widgets, homepage shortcuts, extension update prompts) that GeckoView-only apps do not ship. Desktop 161 notes name nothing about WebRender, Mali, memory, startup or low-end devices.

## 2. What was changed in Kestrel
- `geckoapp/build.gradle.kts`: `geckoVersion = "161.0.20261118180411"`, `webcompatVersion = "161.0"`. The tablet build stays armeabi-v7a only. The engine version the app shows, the user agent (`Firefox/161.0`), the saved-session and last-look "same engine" checks and the in-app engine-update notice all read `GeckoView BuildConfig.MOZ_APP_VERSION` at run time, so nothing else is pinned by hand (a new engine invalidates saved page states and last-look pictures on purpose; the first start after the update opens the start page / reloads the page in front).
- Dependencies of the engine are unchanged between 160 and 161 (androidx.core 1.19.1 forced down to 1.17.0 as before, media3 1.11.1, kotlin-stdlib 2.4.21, snakeyaml 2.2, play-services-fido 21.3.1 excluded as before) [V from the two `.module` files].
- `tools/check-gecko-prefs.sh` now reads the engine the build PINS (it used to take whichever version the Gradle cache listed first, which is wrong after a bump).
- Compile: `:geckoapp:compileDebugKotlin` is clean against 161 with no source change for the engine's API.

## 3. The engine's preferences, 160 -> 161 (strings of libxul.so, 3509 vs 3518 names in the families we use) [V]
- Every one of the 71 preference names Kestrel writes or reads (gecko-prefs.txt, the Network lab, Safe drawing, the hardware drawing trial) **still exists** in 161: 0 missing.
- Removed in 161 (none is ours): `dom.ipc.processPrelaunch.lowmem_mb`, `network.process.enabled`, `network.http.network_access_on_socket_process.enabled`, `network.max_socket_process_failed_count`, `network.proxy.parse_pac_on_socket_process`, `network.send_ODA_to_content_directly`, `security.sandbox.socket.process.level`, `javascript.ionvp8vp9av1`, `dom.fs.writable_file_stream.enabled`, `mathml.font_family_math.enabled`, `extensions.sqlite`, `toolkit.telemetry.debugSlowSql`.
- Added in 161 and worth watching on THIS tablet: `gfx.webrender.paths`, `gfx.webrender.paths.swgl`, `gfx.webrender.path-aa.samples` (WebRender's own path drawing: the software path has its own switch), `gfx.font_rendering.fontations.enabled` (the Rust font reader), `image.decode.defer_if_not_visible` (do not decode pictures that are not on screen: less work and memory on a weak tablet), `network.http.close_connections_on_background`, `network.http.http2.max_output_queue_size`, `apz.pan_gesture.buffer_max_age`, `widget.swipe.touchscreen.enabled`, `webgl.workarounds.finish-before-blitframebuffer-with-multiple-attachments`.
- What I did NOT do: change any of the new defaults. Their values are compiled into the engine and are not in the strings; a guess would be a guess. Instead the Diagnostics line `engine draw-prefs` now reads back five of them (`gfx.webrender.paths`, `gfx.webrender.paths.swgl`, `gfx.webrender.path-aa.samples`, `gfx.font_rendering.fontations.enabled`, `image.decode.defer_if_not_visible`), so the first upload from 161 shows their defaults on the tablet, and they go into the automatic optimizer's experiment list (OPT1, research/OPTIMIZER.md) where the tablet itself decides.

## 4. "Optimized for my hardware": what already is, what this upgrade checked
- Already tuned for the MT8168B / 2 GB / 32-bit / Android 10 Go and kept as is (research/GECKO_TUNING_2.md, LAB5_PREFSWEEP.md, LOW_END_PLAYBOOK.md): armeabi-v7a engine only, one content process, software drawing by default for the owner (GPU driver text bands), safe-drawing flags, memory caches sized for 2 GB, image decode two screens ahead, memory-signal gate, lazy start, tab budget by free memory, stored (uncompressed) `omni.ja` and baseline profile (the build step "Verify the APK itself" still checks them for the new AAR).
- Checked in this upgrade: all 71 names still exist; nothing we set was removed; the removed socket-process prefs never applied on Android (no socket process there).
- Mozilla's armv7 build is profile-guided and link-time optimised in its release channel (research/GECKO_TUNING_2.md section 4); a custom build cannot be done in CI, so the engine binary itself is Mozilla's.
- The tablet decides the rest: the automatic optimizer (OPT1) will A/B the settings that could still help this CPU and GPU, including the new 161 ones.

## 5. NOT proven / not run
- Nothing here has run on the tablet: the new engine is compiled and unit-tested, the APK is built and signed by GitHub, first start on the tablet is the proof. The first start after the update takes longer (new engine, rebuilt start-up caches, saved pages dropped by design).
- The GeckoView changelog page was not reachable (404), so API changes are known only from a clean compile against 161, not from Mozilla's list.
- Whether 161's "blur behind elements" speed-up applies to the Mali tablet or to software drawing was not found.
