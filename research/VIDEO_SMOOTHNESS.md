# Smooth video and page animations on the MT8168B (AO1, owner message 29 item 1, 2026-11-13)

Owner: "The video player and web page animations inside the browser are still slow and laggy. Investigate if it has the proper codecs/gpu acceleration/cpu acceleration to play videos smoothly within web sites."

## 1. The short answer

**Yes: the tablet has the right decoders, the GPU path and the CPU paths, and the browser uses them.** That was measured on the owner's tablet (alpha27 Video lab, `research/VIDEO_AUDIT.md` section 9), not assumed:

| What | Measured on the MT8168B tablet |
| :- | :- |
| H.264 720p30 / 1080p30 | hardware decoder (`MTK.VIDEO.DECODER.AVC`, 2048x1088 at 145 fps), 0 of 150 pictures dropped, started in 307 / 365 ms |
| VP9 720p30 | hardware decoder (`MTK.VIDEO.DECODER.VP9`), 0 / 150 dropped |
| H.265 720p30 | hardware decoder, 0 / 150 dropped |
| AV1 | software only, so the browser never offers it (sites send H.264) |
| Drawing the screen | 65.2 frames a second, median gap 15.1 ms, 0 gaps over 50 ms (WebRender on the Mali-G52 GPU) |
| Scrolling | 64-65 compositor frames a second, 0 of 32 swipes with a gap over 100 ms |
| JavaScript | 36-77 ms for the four small tests: 3 to 5 times slower than a recent phone (4 x Cortex-A53) |

So a video that is "just a video" plays smoothly. What the owner sees on real sites is something that lab clip does not have. The causes that remain (most likely first):

1. **The page's own JavaScript and layout.** All of a page's script, its player (hls.js and friends), its ads and its anti-adblock code run on ONE thread (the main thread of the page process), on a CPU that is 3 to 5 times slower than a phone's. The other three cores cannot help that thread. This is what "laggy animation" usually is on this class of tablet.
2. **The stream, not the player.** The hotspot carries 5 Mbit/s; a player that picks a quality above that stalls and shows the same stutter as a slow decoder. (The size lines in Diagnostics now say what a quality really weighs: `research/VIDEO_SIZES.md`.)
3. **All tabs share one page process** (`dom.ipc.processCount = 1`, chosen for the 2 GB of RAM). A background tab still runs its timers once a second; a busy one competes with the page in front. The A/B of 2 processes against 1 is listed in `research/GECKO_TUNING_2.md` and needs the tablet.
4. **Memory pressure.** When Android runs short the engine flushes its decoded images and its drawing caches; the next scroll then redraws them (visible as a hitch).

## 2. What was checked in this round

### 2a. Do Kestrel's own scripts cost frames?  No.
The built-in extension (`content.js` in every page, `guard-main.js` in every frame) hooks events and watches the page. Measured in headless Chromium with the CPU throttled (CDP `Emulation.setCPUThrottlingRate`), a page that animates 1 500 boxes (400 style writes a frame) and adds and removes 60 nodes a frame, frame gaps of `requestAnimationFrame`, 3 s per run, two runs each (mean ms per frame):

| Throttle 6x, page saturated | run 1 | run 2 |
| :- | :- | :- |
| no scripts | 54.8 | 58.3 |
| `content.js` + `guard-main.js` | 54.8 | 52.0 |
| plus a 400-selector cosmetic stylesheet answered to `content.js` | 48.0 | 53.9 |

On a lighter page (400 boxes, throttle 4x) all three ran at a locked 16.7 ms a frame, 0 to 1 frames in 277 over 33 ms in every configuration. The injected scripts add nothing measurable even when the page itself cannot keep up. Limits: Chromium, not Gecko; the real cosmetic stylesheet is token-based (only selectors that can match the page's classes and ids) and far smaller than the 400-selector one used here.

### 2b. Engine settings reviewed (nothing changed: nothing measured on the tablet supports a change)
`gecko-prefs.txt` and `GeckoEngine.applyVideo`: hardware decoding is the engine's default path (`media.android-media-codec.*` untouched), AV1 off, VP9 and H.265 offered only when the tablet reports a hardware decoder (it does), WebRender untouched with `gfx.webrender.precache-shaders = true` (no first-use hitch), `layout.framevisibility.numscrollportheights = 2`, image decode ahead, smooth scrolling off (instant), reduced motion is now a switch (Settings > Video > Calm pages, off by default, so players' spinners and animations run as the site wrote them). Background tabs: `setActive(false)` on every tab switch and when the browser stops (`BrowserActivity`), so a covered page stops rendering.

### 2c. The app's own animation is not free on a software-rendered screen
On the emulator (no GPU) the home screen's neon animation alone keeps the parent process near 100 % of a core (RenderThread 36 %, Gecko 30 %). On the tablet the animation is drawn by the GPU; the new CPU line below will say whether the parent process is ever the busy one.

## 3. Built: "CPU by process" in Diagnostics (so the next lag report carries the answer)
`diag/CpuMeter.kt` + `core/diag/CpuUse.kt` (`CpuUseTest`, 4 tests): every 5 s while the browser is on screen it reads the CPU time of each of the app's own processes from `/proc/<pid>/stat` (parent, page process `tab`, graphics, sockets: five tiny file reads, no network, nothing while the browser is in the background). A process above 70 % of a core is looked at thread by thread for one second. The Diagnostics line shows the last five minutes:

`CPU by process, % of ONE core (this tablet has 4 ...): tab avg 31 / peak 98; parent avg 9 / peak 40; gpu avg 4 / peak 22 | busy moments: 2 min 10 s ago tab 98 % [GeckoMain 71, DOM Worker 20, Compositor 5] | reading: ...`

How to read it:
* **`tab` near 100 % (GeckoMain high)** while the page lags: the page's own script or layout is the limit. Switching Shields to Strict for that site, closing other tabs, or Settings > Video > Calm pages are the levers; the A/B of two page processes is worth doing only if OTHER tabs show up busy too.
* **`tab` low, `parent` or `gpu` high**: the app's own drawing or the compositor; send the line.
* **Every process low** while the video stutters: the stream (network) or the player stalling, not the CPU.
* **A thread named like a decoder (`MediaPDecoder`, `VPXDecoder`...) busy**: a video is being decoded in software; send the page link.
* The same line is also written to the event log when a process is busy (`cpu busy tab 98 % [...]`, at most once every 30 s per process).

## 4. The owner's smallest tablet step
1. Next time a page or a video is laggy, leave it open, then Menu > Diagnostics > Copy and paste it (with the page's link). The `CPU by process` line holds the last five minutes, so a minute after the lag is fine.
2. If the page was a movie site: say whether Settings > Video > Calm pages ON helps that page (a switch, nothing to install).
3. Only after step 1 shows other tabs busy: the two-process A/B (a release with a switch).

## 5. Decisions
* No engine pref changed in this round: the tablet's own numbers show decode, drawing and scrolling are fine, and no measurement points at a pref.
* The CPU line is the measuring tool; it costs nothing while the browser is in the background and five file reads every 5 s in front (the busy-thread look is one extra second only while a process is above 70 %).
* WebView build (Kestrel Light): has no Diagnostics screen; the line is Kestrel's (GeckoView) only.
