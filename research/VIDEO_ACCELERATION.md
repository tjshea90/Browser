# Smooth video and rotation on the MT8168B tablet: what was found and the plan (owner message 52, 2026-11-16)

Three research agents read real sources (Chromium, Gecko / GeckoView 160, AOSP Android 10, Media3, MediaTek decoder limits); their full reports with every source URL are research/VIDEO_ACCEL_BROWSERS.md, research/VIDEO_ACCEL_THIRDPARTY.md and research/VIDEO_ACCEL_DEVICE.md. The tablet's own numbers are research/TABLET_DIAG_2026-11-16.md. This file is the decision file: what is true, what is changed, what comes next, in order.

## The owner's three symptoms and what explains them

1. "Status bar icons stuck in the middle of the screen, the video cropped at about the same point for several moments." Android freezes the display at a rotation and shows a SCREENSHOT until every window has drawn at the new size, or for at most 2000 ms (WINDOW_FREEZE_TIMEOUT_DURATION). Our UI thread was blocked about 2 s in GeckoView's synchronous surface handshake (SurfaceView.surfaceChanged -> GeckoSession.onSurfaceChanged -> sync IPC ResumeAndResize) BEFORE our window's first frame, so the freeze ran into its 2 s timeout (our `ui late-beat 2021 ms` is exactly that), and after it the system showed the old 600 px buffers (status bar icons at the old right edge, the video cut at the old width) until each window redrew. FIX (alpha56, lab-proven on the real engine): keep the engine's view at its old size for the first traversal, draw the cover and the bars at the new size at once, give the engine its new size right after that first frame (behind the cover). Lab run 43796258034: first frame at the new size 76 to 480 ms after the turn, hold and release in every turn.
2. "Resizes to the wrong size for a long pause." A second, independent delay: the page's own reflow. The page's script thread was silent 3.6 s and 7.8 s after the two turns (and 13 s once). The cover lifted at 3 s onto a stale layout. CHANGED (alpha56): the cover waits up to 6 s for a silent page (was 3 s), and the 1 px size nudge is made only for a page that spoke with a wrong size (both nudges on the tablet were for busy pages: more reflows of a heavy page). NOT fixable by window tricks: the engine's content process must lay the page out again; the next Diagnostics lines (`page script stalls`, `turn first word`, FrameMetrics) say what it is busy with.
3. "The video itself was very laggy in full screen." Not the decoder: the tablet has HARDWARE decoders for H.264, H.265 and VP9 (AHVi), a Mali-G52 MC2 GPU, and the lab clips played with 0 of 150 frames dropped. The suspects, in the order the evidence supports them:
   a. The page's script thread starved by a heavy page (Yahoo's player runs hls.js / VHS style script plus ad and analytics code in the ONE content process; the page timers ran 3.5 to 14 s late): the film's segments are appended by that thread.
   b. Gecko on Android draws each film frame as a SurfaceTexture image through the compositor, one frame at a time (media.video-queue.send-to-compositor-size = 1); Chrome puts the film on its own SurfaceView layer that the hardware composer shows as an overlay without GPU composition. Gecko has no overlay path on Android (bug 1529629 has been open since 2019).
   c. Gecko's vsync is the app's UI-thread Choreographer: any UI-thread work (our overlay, glows, the surface handshake) delays the frames the content process draws.
   d. On this tablet VP9 reaches MSE sites whatever `media.mediasource.vp9.enabled` says (Gecko forces VP9 when a hardware decoder exists), so Settings > Video "H.264 only" cannot remove VP9 here; the hardware VP9 decoder declares exactly 1080p30, so a 1080p60 VP9 stream (YouTube serves those) is beyond it.
   e. The link: the Boltdns film fell from 1080p to 360p on the 5 Mbit/s hotspot; a 1080p start on that link must stall.
   Measured next (alpha56 Diagnostics): frames shown / dropped of the full-screen film (also in "Real video"), how late the page's timers run, what Kestrel's own script costs, FrameMetrics around turns, the decoders' declared and measured frame rates, heat.

## What other browsers do (short)

- Chrome: film on its own SurfaceView / SurfaceControl layer (overlay, no GPU composition), orientation lock by the film's shape at full screen, rotation batched into ONE update with a black fallback and a 1 s cap, the UI thread never waits for the compositor. Samsung's popup player: a native Android player fed the film's address. Opera: server-side compression (impossible here). No browser freezes the page behind a full-screen element.
- Firefox / GeckoView (our engine): SurfaceTexture frames, synchronous resize handshake, no overlay, no asynchronous resize switch, no pref that changes either; its own HLS path moved to Media3 in Firefox 151 but only for a `<video src=m3u8>`.

## Third-party code

- Media3 ExoPlayer 1.11.1 (Apache-2.0): its core, HLS, datasource, decoder, extractor modules are ALREADY inside Kestrel G (GeckoView 160's POM pulls them in; they are in the Gradle cache); using them costs no new dependency, DASH would add 0.15 MB. libVLC (92.9 MB AAR), ijkplayer (dead since 2021), mpv-android (not embeddable), external players by intent (headers only in MX Player), NewPipeExtractor (GPL-3, YouTube terms): rejected. The framework MediaPlayer plays MPEG-TS HLS only and has no rendition control: fallback only.
- A native player can play only what has an address: not MSE streams without a manifest, not DRM, not one-shot signed links; the page's own player stays the fallback. Its gain: the film leaves the page (no page script in the video path, a SurfaceView overlay the hardware composer shows, the page can be frozen with GeckoSession.setActive(false)).

## The plan, in order

DONE in alpha56 (code, unit and web tests, lab kp proof except where noted):
1. Rotation: the engine view held at its old size for the first frame behind the cover (ANS52a); cover waits 6 s for a silent page, nudge only for a page that spoke (ANS52a2); `turn-first-frame`, `turn-frame` (FrameMetrics, 8 s from each turn), `gecko layout WxH took N ms` (the real handshake timing: onLayout of KestrelGeckoView, since GeckoView 160 has no onSizeChanged of its own).
2. Measurement of the film: frames shown / dropped, page timer lateness, the controls script's own cost (ANS51e), the decoders' declared sizes and frame rates and what the vendor measured (DeviceVideo), thermal status.
3. Auto-sideways turning from the player's first report (ANS51b), no page-look read-back during full screen (ANS51h), watchdog 12 s (ANS51d).

NEXT, cheapest and safest first (each behind a Settings row, each with tests and a lab proof):
4. A page-world codec policy like the h264ify family: answer `MediaSource.isTypeSupported`, `canPlayType` and `navigator.mediaCapabilities.decodingInfo` with "no" for VP9 above the hardware's declared 1080p30 (and, as an option, for anything above 30 fps), so a site picks H.264 (hardware 1080p60 class) or a lower VP9. This is the only way to remove VP9 here (the prefs cannot).
5. A picture-size cap: rewrite HLS master playlists (drop variants above the cap, default 720p on this tablet) in the extension with `webRequest.filterResponseData` (Firefox Android lists it from 57; our manifest already has webRequest + webRequestBlocking). A 720p picture has 0.44 of the pixels of 1080p and fits the 5 Mbit/s link; a page-world `mediaCapabilities` answer `smooth:false` above the cap steers hls.js players too. To be tested on the real engine: whether the filter sees gzip bytes (disconnect() on doubt).
6. THE THREE-WAY TEST and the native player: play one film (a) in the page's player, (b) as a bare `<video src=m3u8>` in Gecko's own Media3 path, (c) in our own Media3 SurfaceView (separate Activity, hardware-only decoder selector, buffers 15 to 30 s / 16 MiB, rendition cap, Kestrel's own controls), and compare dropped frames and the page's timer lateness. Offer (c) as "Play in Kestrel's player" in the media sheet (manual, experimental); if it wins, make it the automatic hand-off for sniffed direct URLs with the page's player as the fallback.
7. Experiments to A/B on the tablet only when the numbers ask for them: GeckoSession.setPriorityHint(PRIORITY_HIGH) for the full-screen tab, layers.gpu-process.enabled=false (one process less, no IPC hop, a driver crash would kill the app), gfx.video.aimage-reader-gpu-process-android (GeckoView 161 and later).

NOT recommended (reasons in the research files): TextureView backend (slower, still synchronous), seamless rotation (refused here: the navigation bar moves), rotationAnimation JUMPCUT (the owner has system animations off already), libVLC / ijkplayer / mpv / external players / extractors, server-side compression, setFixedSize tricks.

## ANS52g design: the page codec policy (written 2026-11-16 before the code, so a cut-off session can resume)

What exists: `core/media/VideoPolicy` + Settings "Video formats offered to sites" (Automatic / H.264 only / Everything) set only the engine prefs `media.mediasource.vp9.enabled`, `media.av1.enabled`, `media.hevc.enabled`. On this tablet VP9 has a hardware decoder, and Gecko then offers VP9 whatever the pref says (G2 / G3), so "H.264 only" cannot hide VP9 and nothing stops a site from asking for VP9 above what the chip declares (1080p30).

The policy, decided in the page (MAIN world, before the page's own scripts) for the three questions a player asks:
- `MediaSource.isTypeSupported(type)` (+ `ManagedMediaSource` when present), `HTMLMediaElement.prototype.canPlayType(type)`: "no" ("" for canPlayType) when the type names a video codec the policy refuses;
- `navigator.mediaCapabilities.decodingInfo(config)`: resolves `{supported:false, smooth:false, powerEfficient:false}` for a refused video config, otherwise the engine's own answer;
- the size and frame rate come from the HINTS a site puts in the type string (`width=`, `height=`, `framerate=`, the YouTube / TV convention that Enhanced h264ify also reads) or from the mediaCapabilities `video.width / height / framerate`. No hints = no size known = never refused for size (so a VP9-only site at an unknown size keeps working).

Modes (the existing row, now really enforced; the app sends them, the script never guesses):
- 0 Automatic: a codec is refused ABOVE what this tablet's HARDWARE decoder declares for it (maximum size, and macroblocks per second = ceil(w/16) * ceil(h/16) * fps, from `MediaCodecInfo.VideoCapabilities`: here VP9 = 2048x1088 at 244800 blocks/s = 1080p30, H.264 = 522000 = 1080p64). A codec with NO hardware decoder is left to the engine prefs (they work then).
- 1 H.264 only: every other video codec is refused (the pref could not do it for VP9).
- 2 Everything: the script does nothing.
- new switch "Frame rate offered to sites" (default: whatever the decoder can do; option: at most 30 a second): any video config with an explicit frame rate above 30 is refused, whatever the codec.

Transport: `ShieldsBridge.pageConfig` appends a 5th field to the cfg answer (`<mode>:<gen>:<id>:<flags>:<policy>`, policy = `<mode><fps30>|<avc>|<hevc>|<vp9>|<av1>`, each codec `-` or `<long>x<short>@<blocks per s>`); background.js keeps the last one and returns it as `cx` in the `guard` answer; content.js hands it to `codec-main.js` through a DOM event (like guard-main.js); the page script reports what it refused (counts, one short description, throttled) back the same way (`vcx` message), and the Diagnostics line "Video formats offered to sites" shows the policy, the hardware limits and the counts. Config arrives a few tens of ms after document_start: until then the script answers like the engine (a policy that is not known is never applied).

Why not more: no per-site list, no rewriting of what a site sends (that is ANS52h for HLS playlists), no change to `<video src>` / `<source type>` selection (the engine decides those itself).

## ANS52h design: the picture size cap for HLS pages (written 2026-11-16 before the code)

Why it matters most: the owner's own diagnostics (research/TABLET_DIAG_2026-11-16.md) show the Yahoo film is a Boltdns HLS stream and the page's player picked 1920x1080 in a 600x947 view: the site's default, no cap (hls.js: capLevelToPlayerSize and capLevelOnFPSDrop are false). The screen is 973x600 in landscape, so a 1080p picture is decoded, copied by the GPU and drawn at 1.9 times the pixels the glass can show, over a 5 Mbit/s hotspot that a 1080p rung can fill by itself.

The lever: the extension answers the player's request for the MASTER playlist with the rungs above the cap removed (webRequest.filterResponseData). The player then never sees them and its own ABR picks among the rest. Rules:
- only HLS master playlists: a request whose address path ends in .m3u8 (or whose content type says mpegurl) of type xmlhttprequest / media / other, in `onHeadersReceived` (the latest point filterResponseData may be called), status 200, and only when the cap is on;
- the filter is attached ONLY when the response has no content-encoding (or identity). Gecko may hand the filter decoded or raw bytes: until the lab proves which, a compressed response is left alone and counted ("compressed, left alone"); `onBeforeSendHeaders` asks for `Accept-Encoding: identity` for exactly these requests so that most servers answer plain (a playlist is a few KB);
- the body is buffered (at most 512 KB, otherwise written back unchanged), a text that is not a master playlist (no `#EXT-X-STREAM-INF`) is written back byte for byte;
- a rung is dropped when its RESOLUTION's longer side is above the cap (`#EXT-X-STREAM-INF` + its address line, and `#EXT-X-I-FRAME-STREAM-INF` lines with RESOLUTION); a rung without RESOLUTION is kept; at least the smallest rung with a RESOLUTION always stays; every other line, the line endings and the order stay as they were;
- never an error for the page: on any doubt (a throw, a stream error, a body that is not text) the original bytes are written back, or `disconnect()` is called before anything was changed.

The cap: Settings "Picture size from sites": Automatic (default) = no picture whose longer side is more than 1.35 times the screen's longer side (973 px -> 1314 px: 720p stays, 1080p goes; the phone's 2400 px -> no cap), Off, 480p (854), 720p (1280), 1080p (1920). The app sends the number of pixels (0 = off) as the 6th field of the page answer; the extension needs no list and no per-site setting. Diagnostics: playlists seen, rewritten, rungs dropped, left alone (compressed, too big, not a master), the last cap and rung counts.

Not done on purpose: no DASH (.mpd) rewriting, no `mediaCapabilities` answer by size (a site with ONE 1080p rendition would then refuse to play), no change for progressive files (nothing to choose) or for Gecko's own HLS player (it fetches in Java, past webRequest).

## What to ask the owner for next

The same four Diagnostics lines of message 51 plus, new in alpha56: `Full-screen video controls` (page stalls, own cost, frames), `Real video`, `Turning under a video` (first frame, silent pages), `Frames around turns`, `Video decoders`, `Heat and speed`. Test once on the Yahoo page and once on a light video page.
