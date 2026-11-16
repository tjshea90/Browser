# Video smoothness on the MT8168B tablet: third-party and platform options

Research only, 2026-11-16. No code changed. Tags: [D#] DOCUMENTED (I read source #, list at the end), [S] seen only as a search-result summary (page not opened), [I] INFERRED, [M] MEASURED by me (method in the last section).
Local files: [L1] research/VIDEO_AUDIT.md section 9, [L2] research/TABLET_DIAG_2026-11-16.md, [L3] research/VIDEO_DOWNLOADER.md, DOWNLOADS.md, PHONE_OFFLOAD.md, [L4] geckoapp/build.gradle.kts, the extension manifest, the Gradle cache and the cached GeckoView AAR, [L5] research/VIDEO_SMOOTHNESS.md section 1, [L6] core/.../media/MediaList.kt.

## Summary

1. The decoder is not the weak spot. Inside Gecko the tablet decodes H.264 1080p30, VP9 720p30 and H.265 720p30 in hardware with 0 of 150 frames dropped [L1]. The lag shows on heavy pages: the page script goes quiet 3.5 to 14 s after a turn, a 1080p stream runs over a 5 Mbit/s link [L2].
2. No other player library decodes faster on this chip. VLC uses MediaCodec directly for hardware decoding [D22]; ijkplayer and mpv add MediaCodec or software decoding, and software 1080p on 4 x Cortex-A53 is not realistic [I].
3. Media3 ExoPlayer is already inside Kestrel G. The GeckoView 160 POM declares media3 common, datasource, decoder, exoplayer and exoplayer-hls 1.11.1 [D4]; Gecko moved its `<video src=m3u8>` path to Media3 (target Firefox 151) [D5]. Using it ourselves adds no new dependency.
4. If it had to be added from nothing, a Media3 HLS player shrinks to 0.93 MB dex (0.52 MB gzipped), with DASH and progressive 1.17 MB (0.66 MB) [M]. libVLC is a 92.9 MB AAR [D20]; mpv-android is a 117 MiB APK [D26].
5. Cheapest safe step: cap the picture (720p default) by rewriting HLS master playlists and DASH manifests in the existing extension; Firefox Android lists filterResponseData from version 57 [D30]. No dependency, easy to switch off.
6. Next cheap step: play one film three ways (page's player, bare `<video src>` in Gecko's Media3 HLS, our own Media3 SurfaceView) and compare dropped frames.
7. Best expected gain: hand the film to Media3 in a SurfaceView and freeze the page. It cannot take blob/MSE streams without a manifest, DRM or one-shot signed URLs; the page's player stays the fallback.
8. Reject: libVLC, ijkplayer (last commit 2021-04-26), mpv-android (not a library), external players, NewPipeExtractor (YouTube terms, GPL-3.0, breaks often).
9. Memory is the trap: ExoPlayer buffers live on the Java heap and the defaults allow 50 s [D12]; Mozilla cut its own buffer to avoid out-of-memory kills [D6].
10. Not run on the tablet: dropped frames, decoder instance limit, filterResponseData in GeckoView 160, cookie sharing through GeckoWebExecutor.

## Recommendation (cheapest safe step first)

1. Read the next Diagnostics (dropped frames, page stalls) before building anything large [L2]. No code.
2. Rewrite manifests to cap the picture (section 8a): about 150 lines of extension JS, no dependency. A 720p picture has 0.44 of the pixels of 1080p [I]; it also fits the 5 Mbit/s link better.
3. Add to Diagnostics, for the MTK decoder: max instances, adaptive playback, tunneled playback [D37]. Add the three-way test (leg 2 is section 9, leg 3 a debug-only Media3 SurfaceView). Cheap because Media3 is in the APK.
4. If leg 3 beats leg 1: ship the hand-off below behind Settings > Video, default off until proven. Compile against media3-exoplayer and -exoplayer-hls (optionally -dash) at exactly GeckoView's version (1.11.1), so no second copy arrives [I].
5. Do not add libVLC, ijkplayer, mpv, external players or extractors.
6. Later, optional: Kestrel Courier on the phone could serve segments over the LAN, which the carrier cap does not see [L3, I].

## Comparison

| Candidate | Size | Licence | Dependencies | Decode | Fit | Verdict |
| :- | :- | :- | :- | :- | :- | :- |
| MediaPlayer | 0 | OS | none | MediaCodec | HLS TS-only, no DASH, no rendition control | fallback only |
| Media3 1.11.1 | already in APK; own use at most +0.5 MB gzipped [M] | Apache-2.0 [D14] | Guava, annotation, exifinterface; no androidx.core, no coroutines [D2] | MediaCodec | HLS, DASH, MP4, WebM, headers, caps | GO |
| libVLC 3.7.7 | 92.9 MB AAR, 4 ABIs [D20] | LGPL-2.1 lib, GPL app [D21, D22] | annotation, legacy-support-v4, kotlin-stdlib [D21] | MediaCodec or software | huge, no gain | no |
| ijkplayer 0.8.8 | not on Maven Central [D25] | LGPL-2.1+ [D25] | native FFmpeg n3.4 | MediaCodec or software | dead since 2021 | no |
| mpv-android | APK 117 MiB [D26] | MIT app [D26] | native | MediaCodec or copy | not embeddable [D26] | no |
| VLC / MX by intent | 0 | n/a | apps must be sideloaded [I] | their own | leaves browser, headers only in MX | no |
| NewPipeExtractor | + Rhino, desugaring [D27] | GPL-3.0 [D27] | none decodes | n/a | YouTube terms | no |
| Manifest cap, hints | 0 | n/a | extension JS | n/a | any page player | do first |

## 1. Android MediaPlayer
- HLS: "MPEG-2 TS media files only", protocol version 3 [D18]; DASH is not listed [D18]. fMP4/CMAF HLS therefore fails [I].
- Headers: `setDataSource(Context, Uri, Map)` since API 14; cookies list overload since API 26, and "headers must not include cookies" [D19].
- No rendition control: `selectTrack` only takes timed text, subtitle or audio tracks [D19]. The only cap is to hand it a chosen media-playlist URL, and a separate audio group is then lost [I].
- Surface and seek: `setDisplay/setSurface` work in any state; `seekTo(msec, SEEK_CLOSEST)` since API 26 [D19]; HLS lands on segment starts [I]. A second SurfaceView sits above Gecko's only with `setZOrderMediaOverlay(true)` [D36].
- Verdict: Media3 is present and better; keep MediaPlayer out.

## 2. AndroidX Media3 ExoPlayer
- Latest stable 1.11.1 (2026-09-10); 1.12.0-beta01 listed 2026-11-11. minSdk 23 since 1.9.0 [D1].
- Dependencies (POMs): common needs Guava 33.3.1-android, annotation-experimental, annotation; exoplayer adds container, datasource, decoder, extractor, database, exifinterface (needs only annotation); hls adds concurrent-futures (annotation, listenablefuture); dash adds nothing [D2]. No androidx.core, Kotlin or coroutines. All but dash already arrive through GeckoView [D4, L4].
- Size: AARs before shrinking: exoplayer 1.6 MB, extractor 0.80, common 0.63, hls 0.26, datasource 0.16, dash 0.15 [D3]. The docs say shrinking cuts about 40 percent and advise own renderer and extractor factories [D7]. My R8 run is in [M]. Kestrel G is about 92 MB.
- Decoding: platform MediaCodec; software extensions are optional and not included [D9]. The default selector may choose software when a stream exceeds the hardware decoder [D9]; `isHardwareAccelerated` is exact from API 29 [D14], so a hardware-only selector is easy [I].
- Formats: HLS TS, fMP4/CMAF, ADTS, AES-128 (not SAMPLE-AES), Widevine; DASH fMP4/WebM/MKV, demuxed only; progressive MP4, WebM, MKV, TS, FLV (not seekable) [D8, D9].
- Headers and cookies: `setRequestProperty`, or `ResolvingDataSource` for per-request headers [D11]. DefaultHttpDataSource is the OS stack, usually HTTP/1.1 only; OkHttp adds under 1 MB; Cronet needs Play services or adds 8 MB [D17]. Cookie sharing is not covered in what I read; plan a DataSource over GeckoWebExecutor [L3, I].
- Rendition cap: constraints on width, height, frame rate and bitrate; the default already limits resolution to the display size; `setMaxVideoSizeSd()` is the documented example [D10].
- Tunneling: `setTunnelingEnabled(true)` is aimed at TVs, "performance and reliability can vary greatly" [D10, D15]. Skip. Async MediaCodec queueing: off before API 31, can be forced on from API 23, advised where frames drop [D11]; try it on the tablet.
- Low RAM: min and max buffer 50 s, start 1 s, after rebuffer 2 s, video target 2000 segments, allocator on the heap [D12]. Mozilla's own HLS player once cut its buffer from 30 s to 15 s to stop out-of-memory kills, and Mozilla calls ExoPlayer a source of content-process OOMs [D6]. Set min 15 s, max 30 s, target 16 MiB [I]. SurfaceView draws less power than TextureView [D15, D16].
- MediaTek: Media3's only MTK video workaround is "release instead of flush" for c2.mtk avc/hevc/vp9 decoders on SurfaceTexture [D13]; the OMX.MTK entries in MediaCodecUtil are audio only [D14]. Reports: H.264 artifacts with TextureView but not SurfaceView on old devices (ExoPlayer #58), a green line on Dimensity after a Media3 upgrade (Flutter #174026), Fire TV MTK parts that could not do 1080p AVC so software was chosen [S]. I found no MT8168 defect. This tablet reports AVC up to 2048x1088 [L1]. Adaptive playback is a capability flag whose value here is unknown [D37].

## 3. libVLC
Stable 3.7.7 (2026-10-01), 4.0.0-eap29 in preview [D20]. The 3.7.6 AAR is 92,876,086 bytes with every ABI, so about 23 MB for armeabi-v7a [D20, I]. LGPL-2.1 [D21]; dependencies annotation 1.7.1, legacy-support-v4 1.0.0, kotlin-stdlib 2.2.10 [D21]; minSdk not found. Hardware decoding is MediaCodec directly [D22], so it cannot beat Media3. A 2017 report shows OMX.MTK HEVC choppy at 4K with "buffer deadlock prevented" [D22]. No headers or cookies by intent [D23]. Verdict: no.

## 4. ijkplayer / FFmpeg players
README: LGPLv2.1 or later, FFmpeg n3.4, MediaCodec API 16+, Android API 9 to 23, tag k0.8.8; last commit 2021-04-26; the Gradle snippet is not on Maven Central (404) [D25]. Unmaintained, old FFmpeg, no benefit over Media3. Verdict: no.

## 5. mpv-android
MIT app, "not a library/module (AAR) you can import" [D26]. F-Droid APK 117 MiB (2026-09-17), 51 MiB in April; Android 6.0 or newer [D26]. Verdict: no.

## 6. External player by intent
VLC: ACTION_VIEW with title, position, from_start; returns extra_position when closed; no header or cookie extra documented [D23]. MX Player: `headers` String[] pairs, `position`, `return_result`; for http(s) the class name must be set [S, D24]. Costs: the film address (often a token) leaves the browser; the apps are not on a tablet without Play Store [I]; the app switch is slow and position comes back only on close. Verdict: no.

## 7. NewPipeExtractor / yt-dlp style extractors
GPL-3.0; needs core-library desugaring below minSdk 33 and a Rhino keep rule [D27]. YouTube's terms forbid accessing or downloading except as the Service allows, circumventing it, and automated access [D29]. YouTube enforces PO tokens per client and says it is "continuously updating the implementation and enforcement" [D28]. Plainly: do not ship this. YouTube keeps playing in its own page; use its quality menu.

## 8. Software levers without a new player
a. Manifest cap. filterResponseData: Firefox 57, Firefox Android "mirror" [D30]; needs webRequest, webRequestBlocking and host permission, all in our manifest [D31, L4]; "only works for small requests that aren't chunked or streamed", a playlist is small [D31]. Whether the filter sees gzip bytes is unsettled [S, D32]: test for 1f 8b, and call `disconnect()` on any doubt. HLS: drop `#EXT-X-STREAM-INF` lines above the cap with their URI line; DASH: drop Representations; keep one [I].
b. hls.js: `capLevelToPlayerSize` default false, `capLevelOnFPSDrop` false, `autoLevelCapping` -1, `useMediaCapabilities` true; ABR removes levels with `supported: false` and skips levels with `smooth: false` [D33]. A page-world wrapper of `navigator.mediaCapabilities.decodingInfo` answering `smooth:false` above 720p would steer hls.js players only [I]. The engine calls 1080p60 smooth today [L1].
c. YouTube `yt-player-quality`: scripts report YouTube wraps localStorage and the old trick stopped working [S, D38]. Do not rely on it.
d. H.264-first: already built (Settings > Video formats); the tablet has hardware VP9 and H.265, so Automatic keeps them [L1].
e. Freeze the page: `GeckoSession.setActive(false)` "will significantly reduce its memory footprint" and "flush the session state" [D34]; Kestrel already uses it for covered tabs [L4]. Pause the film first, then deactivate [I].

## 9. Gecko's own Media3 HLS (new finding)
The cached GeckoView AAR holds GeckoHlsPlayer and its renderers, not Media3 classes [M, L4]. The tablet answered "maybe" for HLS in `<video>` and no for MSE [L1]. Mozilla's data source for it is a temporary copy of the default HTTP source [D5], so Referer and cookies are probably missing [I]. Bugs seen: a crash in BaseTrackSelection, NYT videos (fixed) [D5]. Use as test leg 2, and possibly as a bare player page for token-in-URL streams.

## Hand-off design: Kestrel plays the film in a SurfaceView
Inputs
- URL: the candidate the page really plays (`MediaList.primary()` or the existing blob duration match, tolerance 3 percent) [L4, I]. Add: HLS EXTINF sum or MPD duration within max(2 s, 3 percent) of `video.duration`; picture size equals a listed variant; not live, not DRM, playlist looked at.
- Headers: first working set of `MediaHeaders.sets` (Referer, Origin) plus the engine's exact User-Agent. Nothing else [I].
- Cookies: a DataSource over `GeckoWebExecutor` (profile cookies unless anonymous, private tabs use the private jar) [L3]; never copy Cookie values into app storage [I].
- Start: position, paused, muted, rate from player.js reports [I].

Cannot hand off [I]: blob/MSE streams with no manifest (custom segment URLs); DRM (license URL and headers belong to the page); signed short-lived or single-use URLs (a second fetch gives 403); IP- or cookie-bound CDNs; ads stitched into the manifest play as ads and the page's ad beacons do not fire; client-side ad players that swap the element.

Flow [I]
1. Pause the page film (existing command); keep the page active until the first native frame, then `setActive(false)`.
2. Build ExoPlayer: hardware-only MediaCodecSelector, load control 15/30 s and 16 MiB, `setMaxVideoSizeSd()` or 720p cap, SurfaceView with `setZOrderMediaOverlay(true)` [D36]. GeckoView's default backend (Surface or Texture) is not stated in the page I read [D35]: test the z-order.
3. Reuse PlayerControlsView and the core player models; an adapter maps play, pause, seek, skip, mute and rate to ExoPlayer. Count dropped frames with the same Diagnostics line.
4. Free the decoder first: `getMaxSupportedInstances()` is only an upper-bound hint [D37]; if the codec cannot start, release at once.
5. Exit: `setActive(true)`, then `currentTime = position`, then `play()` if it was playing. Stitched-ad positions agree because both play the same manifest.
6. Fallback: on player error, 401/403/410, no first frame in 8 s, or codec failure: release, reactivate, restore the position, remember the host for an hour.
Rotation: the 0.9 to 2 s stall at a turn is the engine's surface resize [L2]; with the page inactive and only the SurfaceView resizing it should vanish [I].

## What I could not verify
- Anything on the tablet: dropped frames, decoder instance limit, adaptive and tunneled flags for OMX.MTK.VIDEO.DECODER.AVC.
- filterResponseData in GeckoView 160 itself (BCD only), and gzip or chunked bodies.
- The true APK delta after Gecko's keep rules (my figure is a standalone upper bound); libVLC minSdk; ijkplayer CVEs.
- Whether Gecko's HLS sends Referer or cookies; whether GeckoWebExecutor sends profile cookies [L3 unproven].
- Seek accuracy of ExoPlayer versus MediaPlayer on this chip; whether the Yahoo page's player honours mediaCapabilities.
- MX Player, Greasy Fork and tracker reports: summaries only [S].

## Method [M]
R8 8.10.9 from build-tools 36.0.0, `--release --min-api 29`, inputs: Media3 1.11.1 classes.jar from the Gradle cache (dash from the downloaded AAR), Guava 33.3.1-android, annotation, exifinterface, concurrent-futures; a probe class building ExoPlayer with HlsMediaSource (default renderers: 1,209,840 B dex, 679,129 gzipped; own video and audio MediaCodec renderers: 934,728 B, 523,568 gzipped; plus Dash and Progressive: 1,169,348 B, 659,735 gzipped); unshrunk D8 of everything: 5,356,856 B. A toy keep rule; a real app differs.

## Sources
D1 https://raw.githubusercontent.com/androidx/media/release/RELEASENOTES.md and https://dl.google.com/android/maven2/androidx/media3/media3-exoplayer/maven-metadata.xml
D2 POMs at https://dl.google.com/android/maven2/androidx/media3/media3-common/1.11.1/media3-common-1.11.1.pom (same pattern for media3-exoplayer, -exoplayer-hls, -exoplayer-dash, -datasource; androidx/exifinterface/exifinterface/1.3.6; androidx/concurrent/concurrent-futures/1.2.0)
D3 AAR sizes from https://dl.google.com/android/maven2/androidx/media3/media3-exoplayer/1.11.1/media3-exoplayer-1.11.1.aar (same pattern)
D4 https://maven.mozilla.org/maven2/org/mozilla/geckoview/geckoview-armeabi-v7a/160.0.20261104181552/geckoview-armeabi-v7a-160.0.20261104181552.pom
D5 https://bugzilla.mozilla.org/show_bug.cgi?id=1896203
D6 https://bugzilla.mozilla.org/show_bug.cgi?id=1896931 and https://bugzilla.mozilla.org/show_bug.cgi?id=1424168
D7 https://developer.android.com/media/media3/exoplayer/shrinking
D8 https://developer.android.com/media/media3/exoplayer/hls
D9 https://developer.android.com/media/media3/exoplayer/supported-formats
D10 https://developer.android.com/media/media3/exoplayer/track-selection
D11 https://developer.android.com/media/media3/exoplayer/customization
D12 https://raw.githubusercontent.com/androidx/media/release/libraries/exoplayer/src/main/java/androidx/media3/exoplayer/DefaultLoadControl.java
D13 https://raw.githubusercontent.com/androidx/media/release/libraries/exoplayer/src/main/java/androidx/media3/exoplayer/video/MediaCodecVideoRenderer.java
D14 https://raw.githubusercontent.com/androidx/media/release/libraries/exoplayer/src/main/java/androidx/media3/exoplayer/mediacodec/MediaCodecUtil.java
D15 https://developer.android.com/media/media3/exoplayer/battery-consumption
D16 https://developer.android.com/media/media3/ui/surface
D17 https://developer.android.com/media/media3/exoplayer/network-stacks
D18 https://developer.android.com/media/platform/supported-formats
D19 https://developer.android.com/reference/android/media/MediaPlayer
D20 https://repo1.maven.org/maven2/org/videolan/android/libvlc-all/3.7.6/ and https://repo1.maven.org/maven2/org/videolan/android/libvlc-all/
D21 https://central.sonatype.com/artifact/org.videolan.android/libvlc-all/3.7.6
D22 https://github.com/videolan/vlc-android, https://mailman.videolan.org/pipermail/vlc-devel/2017-March/112132.html, https://mailman.videolan.org/pipermail/vlc-devel/2017-March/112129.html
D23 https://wiki.videolan.org/Android_Player_Intents/
D24 https://www.sites.google.com/site/mxvpen/api
D25 https://github.com/bilibili/ijkplayer, https://github.com/bilibili/ijkplayer/commits/master, https://repo1.maven.org/maven2/tv/danmaku/ijk/media/ijkplayer-armv7a/
D26 https://github.com/mpv-android/mpv-android and https://f-droid.org/packages/is.xyz.mpv/
D27 https://github.com/TeamNewPipe/NewPipeExtractor
D28 https://github.com/yt-dlp/yt-dlp/wiki/PO-Token-Guide
D29 https://www.youtube.com/t/terms
D30 https://raw.githubusercontent.com/mdn/browser-compat-data/main/webextensions/api/webRequest.json
D31 https://developer.mozilla.org/en-US/docs/Mozilla/Add-ons/WebExtensions/API/webRequest/filterResponseData
D32 https://bugzilla.mozilla.org/show_bug.cgi?id=1590898 and https://bugzilla.mozilla.org/show_bug.cgi?id=1787119
D33 https://raw.githubusercontent.com/video-dev/hls.js/master/docs/API.md and https://raw.githubusercontent.com/video-dev/hls.js/master/src/controller/abr-controller.ts
D34 https://mozilla.github.io/geckoview/javadoc/mozilla-central/org/mozilla/geckoview/GeckoSession.html
D35 https://mozilla.github.io/geckoview/javadoc/mozilla-central/org/mozilla/geckoview/GeckoView.html
D36 https://developer.android.com/reference/android/view/SurfaceView
D37 https://developer.android.com/reference/android/media/MediaCodecInfo.CodecCapabilities
D38 https://www.greasyfork.org/scripts/469953-youtube-fix-auto-quality-issue/code and https://greasyfork.org/el/scripts/6034-youtube-hd-override/discussions/236823
Search-summary only [S]: https://github.com/google/ExoPlayer/issues/58, https://github.com/flutter/flutter/issues/174026, https://lightrun.com/answers/google-exoplayer-significant-frame-drop-while-playing-1920x1080-hls-video-tracks-on-aftsss-and-aftss
