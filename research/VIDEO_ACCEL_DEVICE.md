# Video acceleration and rotation on the MT8168B tablet (research, 2026-11-16)

Method: web sources read 2026-11-16; no code run, no device. DOCUMENTED = read in the cited source; INFERRED = my reasoning from documented facts (table: DOC, INF). [D] = the Diagnostics pastes in the repo (INBOX.md, research/TABLET_DIAG_2026-11-16.md, docs/hardware/hardware_.txt). Keys like [A1] are listed under Sources (URL = prefix + path). Android = tag android-10.0.0_r47; Gecko = tag FIREFOX_160_0_RELEASE unless marked main.

## Summary (12 lines)

1. Verdict on our rotation diagnosis: PARTLY CONFIRMED, with three corrections (lines 3-5).
2. Confirmed (DOCUMENTED): the UI thread blocks in a synchronous compositor handshake. SurfaceView calls surfaceChanged inside setFrame() / pre-draw [A1]; GeckoView forwards it to GeckoSession.onSurfaceChanged, whose native part runs on the UI thread and sends the sync IPDL message ResumeAndResize, waiting for its bool reply [G13 G16 G17 G18]. The window's first draw and its finishDrawing report wait behind it [A3].
3. Correction 1: it is not GeckoView.onSizeChanged. GeckoView 160 defines no onSizeChanged, onLayout, onMeasure or setFrame [G13], so KestrelGeckoView times an empty super call and "the engine's part took N ms" can practically never print. Time layout() and use FrameMetrics instead.
4. Correction 2: during the window-manager freeze the screen shows a screenshot, not old buffers [A7]. Old buffers show only after the freeze ends: all windows drawn, or WINDOW_FREEZE_TIMEOUT_DURATION = 2000 ms [A4 A5]. Our "ui late-beat 2021 ms" is within 1 percent of that timeout, so it probably fired (INFERRED).
5. Correction 3: a second, independent delay. The cropped video lasts until the content process re-lays-out and sends a new display list (page silent 3.5-14 s [D]; resize path in Bug 1766127 [G21]); no window trick fixes that.
6. Hardware is not the bottleneck: H.264, H.265 and VP9 all have MediaTek hardware decoders (AHVi = AVC, HEVC, VP9 hardware; AV1 not) [D]. Declared limits (INFERRED from the reported numbers): H.264 level 4.2 (1080p about 64 fps), HEVC level 4.1 (1080p60), VP9 exactly 1080p30 [D H4].
7. New Gecko finding: on this tablet VP9 reaches MSE sites whatever media.mediasource.vp9.enabled says (IsVP9Forced + UseVP9HwDecode from the MediaCodec list) [G2 G3]. Settings > Video "H.264 only" therefore cannot remove VP9 (INFERRED); a 1080p60 VP9 stream exceeds the declared 1080p30.
8. No Gecko pref makes the resize asynchronous; the TextureView backend resizes synchronously too and is slower [G13 G14]. Best lever is app side: hold GeckoView at its old frame for the first traversal, cover, resize after the first draw (test in lab); Chromium also hides rotation jank with black [X1].
9. Seamless rotation is probably refused here (nav bar stays at the bottom) [A6]; JUMPCUT saves only the rotate animation; animation scale 0 does not remove the freeze (INFERRED) [A4 A5].
10. Gecko's vsync is the app's UI-thread Choreographer [G10]: any UI-thread work (overlay, glows, resize) delays video frames.
11. "mem trim level 5/10" is not a memory fact: Gecko acts only on trim level 15 and on 40 and up [G11].
12. Not verifiable without the tablet: DVFS, thermal, zram, vendor overlay values, real decoder throughput (commands at the end).

## A. The hardware

- DOCUMENTED [H1, a spec aggregator; I found no public MediaTek datasheet]: MT8168 = 4x Cortex-A53 up to 2.0 GHz, 12 nm, LPDDR4x controller (speed not stated; a search snippet said about 14.9 GB/s, unverified), 1080p decode and encode. GPU: aggregators say Mali-G52 MP1 (up to 800 MHz); the tablet reports "ARM Mali-G52 MC2, OpenGL ES 3.2 v1.r20p0" [D]. It is not a PowerVR; the MC1/MC2 conflict is unresolved, the device report wins.
- DOCUMENTED [D]: decoders listed by Android (names printed without "OMX."; INFERRED real names OMX.MTK.VIDEO.DECODER.*, not c2.mtk.*, on this Android 10 build): AVC 2048x1088 and 145 fps at 720p; HEVC the same; VP9 2048x1088 and 68 fps at 720p; VP8 ("VPX") 2048x1088, 960 fps (no cap declared); MPEG-4 896x896; MPEG-2 1920x1088@60. Software: android/google AVC, HEVC, VP9, VP8 and AV1. Widevine security level L3 (SD only).
- DOCUMENTED [H3]: getSupportedFrameRatesFor "is not a performance indicator", only a coding-standard limit. AOSP maps AVC level 4.2 to 522,240 macroblocks/s and 8,704 macroblocks (= 2048x1088, 145 fps at 720p) and HEVC main level 4.1 to 60 fps at 2048x1088 [H4], so those two decoders claim 1080p60 class. INFERRED: VP9 68 fps at 720p = 62,668,800 luma samples/s = exactly 1920x1088 at 30 fps; VP9 level 4 is 83,558,400 [H4], so the vendor declared 1080p30. Measured figures (getAchievableFrameRatesFor, getSupportedPerformancePoints, API 29) are not logged yet [H3].
- DOCUMENTED: isHardwareAccelerated (API 29) "is provided by the device manufacturer ... cannot be tested for correctness" [H2]; Gecko uses the same call on API 29 and up [G1].
- DOCUMENTED [VideoPolicy.kt, DeviceVideo.kt, D]: AHVi = four letters A = H.264, H = H.265, V = VP9, I = AV1; capital = at least one decoder of that MIME type has isHardwareAccelerated true, lower case = none. "26 decoder(s)" counts (decoder, MIME) pairs over 14 probed audio and video types, not 26 codecs; the probe took 99-613 ms.
- Implication. Smooth (lab, [D]): H.264 720p30 and 1080p30, VP9 720p30, H.265 720p30, all 0 of 150 frames dropped; H.264 1080p60 is claimed, not lab-tested. At the limit: VP9 1080p30. Not smooth (INFERRED): VP9 1080p60, any AV1 (software on four A53), anything above 2048x1088. Unknown: 10-bit (HEVC Main10, VP9 profile 2), green frames, adaptive playback of this decoder; I found no MT8168-specific bug, and Gecko's adaptive-playback block-list names only Samsung and LG models [G1]. Gecko keeps only a hardware maximum size (HWMAX), no frame rate that I could see [G24].

## B. Gecko settings (defaults are Firefox 160 on Android; the tablet's read-back [D] agrees; names Kestrel already sets are not repeated)

- DOCUMENTED: the name "media.suspend-bkgnd-video.*" does not exist; the real ones are media.suspend-background-video.enabled = true and .delay-ms = 10000 (background tabs only) [G4].
- DOCUMENTED: Java MediaCodec decoders are tried first (media.android-media-codec.enabled and .preferred = true on release; preferred inserts the Java module before ffvpx) [G5 G7]. RDD (media.rdd-process.enabled), GPU decoding (media.gpu-process-decoder) and the utility MediaCodec path are off on release Android; the utility process does audio only [G5]. The ":media" process is the Java MediaManager service [G9].
- DOCUMENTED: a decoded frame is a SurfaceTexture; WebRender's render thread (GPU process) calls UpdateTexImage and samples the external texture without a copy [G8]; media.video-queue.send-to-compositor-size = 1 on Android because "SurfaceTexture contains only one" frame [G5]. A late render thread therefore delays or drops video directly.
- DOCUMENTED [G4]: gfx.webrender.compositor = false and layer-compositor = false (native compositors are Windows, macOS, GTK only); gfx.webrender.software = false; program-binary-disk = true; max-partial-present-rects = 1; layers.gpu-process.enabled = true (read once at start); layout.frame_rate = -1 (vsync).
- DOCUMENTED [G4 G6 G11 G12]: javascript.options.gc_on_memory_pressure = false on Android; dom.ipc.processPriorityManager.enabled = true with 3000 ms background grace; dom.ipc.keepProcessesAlive.web = 1; GeckoSession.setPriorityHint(PRIORITY_HIGH) is the app's own priority switch; dom.min_background_timeout_value = 1000 with budget throttling is background-only.

## C. Android rotation internals (Android 10, app with configChanges)

1. DOCUMENTED: DisplayContent.updateRotationUnchecked picks seamless or frozen [A5]. startFreezingDisplayLocked returns early if the screen is off, the display not ready or !okToAnimate; otherwise ScreenRotationAnimation takes a screenshot of the old screen, places it above all windows, keeps the old content where it was on the panel, and holds it until dismiss(); it has no timeout of its own [A4 A7].
2. DOCUMENTED: windows whose frame changes become orientation-changing; stopFreezingDisplayLocked does not unfreeze while any still is; WINDOW_FREEZE_TIMEOUT_DURATION = 2000 ms, then onWindowFreezeTimeout forces the clear [A4 A5]. Timeline [D]: turn 76.50 s, "ui slow 887 ms" 77.3, "late-beat 2021 ms" 78.48, trims at 78.4-78.9: the stall ends where a 2 s timeout would (INFERRED).
3. DOCUMENTED: app side. The window manager sends MSG_RESIZED_REPORT; ViewRootImpl calls reportNextDraw and requestLayout; performTraversals does relayoutWindow, measure, layout, dispatchOnPreDraw, performDraw; the frame-complete callback runs pendingDrawFinished, then reportDrawFinished calls finishDrawing [A3]. SurfaceView.updateSurface runs from setFrame() and the pre-draw listener, calls surfaceChanged synchronously, calls setBufferSize on its child SurfaceControl, and holds the window's draw report only for Callback2 listeners; plain Callbacks (GeckoView) count as finished at once [A1 A2]. So the block delays the draw, GeckoView adds no further hold. INFERRED: the 0.9-2 s may also include a relayout Binder wait while the window manager takes the screenshot.
4. DOCUMENTED: rotationAnimation: ROTATE, CROSSFADE, JUMPCUT since API 18, SEAMLESS since API 26 ("works like JUMPCUT but falls back to CROSSFADE") [A8]. Android 10 selectRotationAnimationLw maps SEAMLESS to the crossfade resource and forces JUMPCUT when the screen is not fully on; shouldRotateSeamlessly needs: not upside-down, nav bar able to move (config_navBarCanMove) or config_allowSeamlessRotationDespiteNavBarMoving, focused window = top opaque fullscreen window, matching app bounds, not animating, attribute SEAMLESS [A6]. INFERRED: landscape height 523 px = 600 - 25 (status) - 51 (nav) at 170 dpi, so the nav bar stays at the bottom and seamless is refused.
5. DOCUMENTED: okToAnimate() ignores animation scales [A5]; only mAnimationsDisabled (battery saver) forces scales to 0 [A4]. INFERRED: scale 0 shortens the rotate animation, not the screenshot or the wait.
6. INFERRED: after the freeze ends, a window that has not redrawn is composed with its previous buffer: the old 600-px status bar (SystemUI is another process that must redraw its own window) in a 1024-px display puts the icons mid-screen, and the video keeps its old size until Gecko produces a new-size frame; SurfaceFlinger buffer-size semantics were not verified.
7. DOCUMENTED: SurfaceView is composed by SurfaceFlinger, as a hardware overlay when supported; TextureView is composed by the GPU, "every visible pixel is composited twice" [A12]; Media3 prefers SurfaceView for lower power and "more accurate frame timing" [A11]. GeckoView has setViewBackend(BACKEND_SURFACE_VIEW or BACKEND_TEXTURE_VIEW), no setSurfaceBackend; its Javadoc: TextureView "worse performance" [G13]; TextureView's onSurfaceTextureSizeChanged calls the same listener synchronously [G14]. GeckoDisplay.surfaceChanged "may block" [G15]. Moving the compositor into the UI thread was closed WONTFIX; a SurfaceView does not move "in lockstep" with the UI [G20]. No asynchronous-resize switch exists.
8. DOCUMENTED, other apps: Chromium "hides fullscreen rotation jank" by evicting the frame and showing black while the frame size differs from the Surface size [X1]; the ExoPlayer demo activity declares configChanges and handles rotation itself [X2], and Media3 recommends SurfaceView [A11]. Firefox for Android: not verified.
9. DOCUMENTED: FrameMetrics (API 24) gives FIRST_DRAW_FRAME, LAYOUT_MEASURE_DURATION and UNKNOWN_DELAY_DURATION (UI thread not responsive), per frame [A9]. INFERRED: the resize cost is the handshake (0.9-1.0 s also at first start [D]), not a few MB of buffers; gralloc pressure is unverified.
10. INFERRED, technique T1 in steps: (a) on the configuration change show the cover; (b) make KestrelGeckoView report its old size from onMeasure/layout for exactly one traversal, so setFrame sees no size change and the window draws and reports at once; (c) from a ViewTreeObserver.OnDrawListener (fires in that frame's draw) post a Choreographer callback, then requestLayout() to release the clamp, so the synchronous resize runs after finishDrawing was sent; (d) lift the cover when the page's first new-size report arrives (TurnSettler). The UI thread still blocks 1-2 s, but behind the cover, after the freeze.

## D. Vendor, Android Go, laggy video

- DOCUMENTED [A13]: AOSP Go defaults: ro.config.low_ram = true, ro.lmk.critical_upgrade, upgrade_pressure 40, downgrade_pressure 60, kill_heaviest_task = false, heapgrowthlimit 128m (= memory class 128 [D]); no zram property is set there, so swap is vendor-defined and unknown. I found nothing saying the flag disables hardware layers.
- INFERRED: the tablet runs parent, :gpu, :tab, :utility, :media and :crashhelper [D G9]; process churn changes the cached-process count Android uses for trim levels 5/10/15 (the app's LiveBudget.kt says the same); Gecko ignores levels 5 and 10 [G11].
- DOCUMENTED [G10]: Gecko's vsync is Choreographer on the main Looper. INFERRED: a full-screen overlay, glows or Shields work on the UI thread directly cost video frames. Lab: one file with Kestrel's controls hidden and shown, FrameMetrics and dumpsys gfxinfo; also Developer options "Disable HW overlays".
- DOCUMENTED [A10]: API 29 has PowerManager.getCurrentThermalStatus() and a listener; isSustainedPerformanceModeSupported (API 24). I found nothing MT8168-specific on governor or thermal limits.
- INFERRED [D]: the Boltdns stream fell from 1080p to 640x360 on the 5 Mbit/s hotspot, so the link, not the decoder, limits quality.

## Table

| setting or technique | what it does | risk on 2 GB | evidence | recommend |
| :- | :- | :- | :- | :- |
| media.android-media-codec.enabled/.preferred (true) | Java MediaCodec decoder first | none | DOC G5 G7 D | yes (keep) |
| media.mediasource.vp9.enabled | default false on Android but bypassed when a hardware VP9 decoder exists | none | DOC G2 G3 G19 | no (cannot hide VP9 here) |
| page script like h264ify: isTypeSupported and canPlayType answer no to webm, vp8, vp9, av01 (optionally framerate over 30) | sites pick H.264 | low; loses VP9-only 1440p and 4K | DOC X3 X4 | test in lab |
| media.hevc.enabled / media.av1.enabled | codec offer; keep AV1 off | none | DOC G4 | yes (keep policy) |
| media.rdd-process.enabled, media.gpu-process-decoder, media.utility-android-media-codec.enabled | decoder process placement (off on release Android) | extra process, no gain | DOC G5 | no |
| media.video-queue.default-size 10, send-to-compositor-size 1 | frame queue | none | DOC G5 | no |
| layers.gpu-process.enabled = false (start-up only) | compositor in parent: one process less, no IPC hop; driver crash kills app | medium | DOC G4 G22 | test in lab |
| gfx.webrender.* defaults, layout.frame_rate -1 | see B | none | DOC G4 | no |
| GeckoSession.setPriorityHint(PRIORITY_HIGH) for the full-screen tab | app-chosen process priority | none | DOC G12 | test in lab |
| dom.ipc.processCount 1, processPriorityManager, gc_on_memory_pressure, suspend-background-video | already right or background-only | none | DOC G4 | no change |
| GeckoView.setViewBackend(TEXTURE_VIEW) | extra GPU pass, still synchronous | more GPU and RAM | DOC G13 G14 A12 | no |
| T1: keep GeckoView frame at the old size for the first traversal, cover, resize after the first draw (OnDrawListener, post or Choreographer) | the first frame is cheap, the freeze ends early, the 1-2 s block hides behind the cover | medium (layout hack) | INF A1 A3 G17 X1 | test in lab |
| window rotationAnimation = JUMPCUT in full screen | skips the rotate animation only | none | DOC A8 A6 | yes |
| ROTATION_ANIMATION_SEAMLESS | falls back to crossfade | none | DOC A6 A8 | no |
| SENSOR_LANDSCAPE while a wide video is full screen | 180 degree flips keep the size, no resize | owner wants portrait turns | INF A1 G17 | test in lab |
| transition_animation_scale 0 (adb) | measurement only | none | INF A4 A5 | test in lab |
| FrameMetrics listener plus timing layout() | finds the real stall | tiny | DOC A9 | yes |
| thermal status and sustained-performance support in Diagnostics | tests throttling | none | DOC A10 | yes |
| setFixedSize on GeckoView's SurfaceView, requestNewSurface | stretches the picture or recreates the surface | flicker | DOC A1 G13 | no |

## What I could not verify

- A primary MT8168 datasheet; Mali MC1 versus MC2; the memory type on this board.
- Real decoder throughput (VP9 1080p30 and 1080p60, H.264 1080p60), 10-bit support, green-frame bugs; getAchievableFrameRatesFor and performance points.
- The vendor overlay (config_navBarCanMove, allowSeamlessRotationDespiteNavBarMoving) and whether this modified build really shows the screenshot freeze. Test: adb shell dumpsys window (mDisplayFrozen, nav bar), adb shell screenrecord during a turn.
- Where the 0.9-2 s goes (relayout Binder wait, sync IPC wait, render thread): needs Perfetto (sched, gfx, view, wm, binder) or timing around layout(). Whether WebRender re-renders the old display list at the new size.
- The AOSP source of the trim-level computation (updateLowMemStateLocked was not opened); Fenix rotation handling; whether Chrome sets rotationAnimation.
- DVFS, thermal trips, zram, memory pressure, per-process memory of the six processes: cat /proc/swaps, /proc/pressure/memory, the cpufreq scaling_governor, dumpsys thermalservice, dumpsys meminfo.
- media.* defaults for android-media-codec, rdd, video-queue came from main, not the 160 tag; the tablet's read-back matches.

## Sources

Prefixes: AOSP = https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android-10.0.0_r47/ ; G160 = https://raw.githubusercontent.com/mozilla-firefox/firefox/FIREFOX_160_0_RELEASE/ ; Gmain = https://raw.githubusercontent.com/mozilla-firefox/firefox/main/

- H1 https://www.notebookcheck.net/MediaTek-MT8168-Processor-Benchmarks-and-Specs.483534.0.html
- H2 https://developer.android.com/reference/android/media/MediaCodecInfo
- H3 https://developer.android.com/reference/android/media/MediaCodecInfo.VideoCapabilities
- H4 AOSP media/java/android/media/MediaCodecInfo.java
- A1 AOSP core/java/android/view/SurfaceView.java ; A2 AOSP core/java/com/android/internal/view/SurfaceCallbackHelper.java ; A3 AOSP core/java/android/view/ViewRootImpl.java
- A4 AOSP services/core/java/com/android/server/wm/WindowManagerService.java ; A5 AOSP services/core/java/com/android/server/wm/DisplayContent.java ; A6 AOSP services/core/java/com/android/server/wm/DisplayPolicy.java ; A7 AOSP services/core/java/com/android/server/wm/ScreenRotationAnimation.java
- A8 https://developer.android.com/reference/android/view/WindowManager.LayoutParams
- A9 https://developer.android.com/reference/android/view/FrameMetrics
- A10 https://developer.android.com/reference/android/os/PowerManager
- A11 https://developer.android.com/media/media3/ui/surface
- A12 https://source.android.com/docs/core/graphics/arch-tv
- A13 https://android.googlesource.com/platform/build/+/3021218d2a/target/product/go_defaults_common.mk
- G1 G160 mobile/android/geckoview/src/main/java/org/mozilla/gecko/util/HardwareCodecCapabilityUtils.java
- G2 G160 dom/media/mediasource/MediaSource.cpp (IsVP9Forced)
- G3 G160 gfx/thebes/gfxPlatform.cpp (CODEC_HW_FEATURE_SETUP) ; G160 widget/android/GfxInfo.cpp
- G4 G160 modules/libpref/init/StaticPrefList.yaml
- G5 Gmain modules/libpref/init/StaticPrefList.yaml ; G6 Gmain mobile/android/app/geckoview-prefs.js ; G7 Gmain dom/media/platforms/PDMFactory.cpp
- G8 G160 gfx/webrender_bindings/RenderAndroidSurfaceTextureHost.cpp ; G9 G160 mobile/android/geckoview/src/main/AndroidManifest_overlay.jinja
- G10 G160 mobile/android/geckoview/src/main/java/org/mozilla/gecko/gfx/AndroidVsync.java ; G11 G160 .../org/mozilla/gecko/process/MemoryController.java
- G12 https://firefox-source-docs.mozilla.org/mobile/android/geckoview/design/priority-hint.html
- G13 G160 mobile/android/geckoview/src/main/java/org/mozilla/geckoview/GeckoView.java ; G14 Gmain mobile/android/geckoview/src/main/java/org/mozilla/gecko/SurfaceViewWrapper.java ; G15 Gmain .../org/mozilla/geckoview/GeckoDisplay.java ; G16 G160 .../org/mozilla/geckoview/GeckoSession.java
- G17 Gmain widget/android/nsWindow.cpp ; G18 Gmain gfx/layers/ipc/PUiCompositorController.ipdl and G160 gfx/layers/ipc/UiCompositorControllerParent.cpp
- G19 https://bugzilla.mozilla.org/show_bug.cgi?id=1614958 ; G20 https://bugzilla.mozilla.org/show_bug.cgi?id=1594860 ; G21 https://bugzilla.mozilla.org/show_bug.cgi?id=1766127 ; G22 https://bugzilla.mozilla.org/show_bug.cgi?id=1331109
- G24 G160 gfx/thebes/gfxAndroidPlatform.cpp
- X1 https://codereview.chromium.org/2810813004
- X2 https://raw.githubusercontent.com/androidx/media/release/demos/main/src/main/AndroidManifest.xml
- X3 https://raw.githubusercontent.com/erkserkserks/h264ify/master/src/inject/inject.js ; X4 https://raw.githubusercontent.com/erkserkserks/h264ify/master/README.md
