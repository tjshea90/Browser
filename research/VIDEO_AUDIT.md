# Video audit (owner message 18, 2026-11-13) - TASKS AE1

> "Audit how the browser handles video within a webpage. Is it using proper acceleration and hardware? Does the browser need more built in codecs or video support? A video I'm playing is choppy,
> and the initial loading symbol which came from the site's own video player seems to be stuck over the video when it should have disappeared after loading. Figure out if the browser has proper
> support for common elements of web sites such as updated java, hardware acceleration, video and audio codecs, etc. Research [...] free [...] codecs or engines [...] that can genuinely speed anything
> up [...] without slowing down the browser or breaking anything, then implement it. For paid/proprietary [...] I will contact the owner/company for approval and payment"

Everything here was measured or read from the shipped engine (GeckoView 160, `aar2/` extraction: `omni.ja` + `libxul.so`), the app code, or the emulator (Android 10 x86_64, no GPU, no hardware decoders).
**The tablet itself was not available**: whatever depends on its decoders / GPU is decided at run time from what Android reports (Diagnostics > Video) and checked by the Video lab the owner runs.

## 1. Answers in one screen

| Question | Answer |
| :- | :- |
| Stuck loading symbol on the site's player | **Found and fixed (proven on the emulator).** `TabDelegates.onContentPermissionRequest` refused EVERY autoplay request, including MUTED ones (`PERMISSION_AUTOPLAY_INAUDIBLE`). GeckoView asks the app each time a page starts a video by itself (`media.geckoview.autoplay.request = true` in the engine's own `geckoview-prefs.js`). Result with alpha26: a `<video muted autoplay>` stayed paused, a muted `video.play()` from script was rejected (`NotAllowedError`), so the player never got its `playing` event and its loading symbol stayed up on top of the video. Every other browser allows muted autoplay. Now allowed (Settings > Video > "Muted videos may start by themselves", on). |
| Second cause of a stuck symbol | `ui.prefersReducedMotion = 1` + `image.animation_mode = "once"` were fixed on for EVERY page (alpha3, the owner's "no animations" taken to the web). A player that hides its loader from an animation event, or whose reduced-motion style freezes the spinner, can keep a frozen symbol on screen. Now only while Settings > Video > "Calm pages" is on (default OFF). The app's own screens still have no animations. |
| Choppy video: hardware acceleration | The engine decodes **H.264 / AAC with the device's own MediaCodec decoders** (hardware on this tablet: MT8168 has a 1080p H.264/H.265 decode block). It decodes **VP9 / AV1 / VP8 / Opus / Vorbis / FLAC / MP3 itself in software** (bundled libraries) unless the device offers a hardware decoder for that codec. Four Cortex-A53 cores cannot software-decode 720p30 VP9 next to a page, which looks exactly like "choppy". Sites choose the codec by asking the engine what it can play, so the lever is what the engine says: `core/media/VideoPolicy` + `GeckoEngine.applyVideo` now tell sites "no VP9 / AV1 / H.265" when the device has NO hardware decoder for it (read from `MediaCodecList`, `isHardwareAccelerated`), so YouTube-style players send H.264. Settings > Video > "Video formats offered to sites": Automatic (default), H.264 only, Everything. |
| More built-in codecs needed? | **No.** The common formats are all covered: H.264/H.265/AAC (OS decoders), VP8/VP9/AV1/Opus/Vorbis/FLAC/MP3/WAV (bundled in the engine), HLS (`media.hls.enabled` exists), MSE, WebM, MP4. Missing ones are patent-encumbered (AC-3 / E-AC-3 / DTS decode, a software H.264 decoder): the OS decoder is the legal and the fastest path, so there is nothing to add for free. See section 6. |
| Protected video (Netflix-style) | Refused outright until alpha26 (`media.eme.require-app-approval = true`: the app must approve every request and ours denied it). Now: "ask" when the tablet has Widevine (`MediaDrm.isCryptoSchemeSupported`), never otherwise; Settings > Video > "Protected video (DRM)". The security level (L1 = HD, L3 = SD only) is shown in Diagnostics > Video. |
| "Updated java" | Taken as JavaScript. Java applets and the other NPAPI plugins ended in Firefox 52 (2017), Flash in Firefox 85 (January 2021), and Firefox for Android never ran plugins at all. The engine is **SpiderMonkey of Firefox 160**: baseline interpreter, Baseline JIT, Warp (Ion) JIT, WebAssembly with the optimising compiler, SIMD, bulk memory, reference types, multi-value, tail calls, threads. None of those prefs is touched by us (Diagnostics shows them). The Video lab times five small JS tests and checks each WebAssembly feature on the tablet. The WebView build (Chromium 74) is frozen at 2019 JS: that is why Kestrel G exists. |
| GPU / compositing | WebRender (GPU) through Mali: unchanged; Diagnostics shows the GL renderer (`GlInfo`) and now the pref read-back for `gfx.webrender.*`. The Video lab's "Drawing the screen" test counts real frame gaps for 3 s. |

## 2. The emulator proof (alpha26 build, before the fix)

`scratchpad/site/vid/autoplay.html` (kept in this file's description, 40 lines): three `<video>` elements with an overlay "LOADING" that a `playing` listener hides, a 1.5 s later `play()` call on the third.
Run in alpha26 on the Android 10 emulator:

```
 11688 a loadstart            A = muted autoplay            -> stays paused, overlay LOADING A never hides
 11860 b loadstart            B = autoplay with sound       -> paused (expected: blocked everywhere)
 11911 c loadstart            C = muted, started by script
 13498 c play() REJECTED NotAllowedError     <- a muted play() is allowed in every other browser
 16869 state a paused=true b paused=true c paused=true
```
The screenshot shows all three "LOADING" overlays on top of paused videos with Firefox's own play buttons under them: the owner's description of the bug.
After the fix (alpha27) the same page is run again: see section 7 for the result.

## 3. What the engine does for video (read from GeckoView 160)

* Prefs that exist (names checked in `libxul.so`): `media.android-media-codec.enabled/preferred` (MediaCodec path, default on), `media.mediasource.vp9.enabled`, `media.hevc.enabled`, `media.av1.enabled`, `media.hls.enabled`, `media.eme.enabled`,
  `media.gmp-widevinecdm.*`, `media.mediadrm-widevinecdm.visible = true`, `media.rdd-process.enabled`, `media.utility-process.enabled`, `media.video-queue.*`, `media.suspend-background-video.enabled`, `gfx.webrender.*`,
  `javascript.options.ion/baselinejit/wasm_optimizingjit`. Missing in 160 (so setting them would be a silent no-op): `media.ffvpx.enabled`, `media.mediasource.webm.enabled`, `media.ac3.enabled`, `media.eac3.enabled`, `media.wmf.enabled`, `media.mp4.enable-fragmented-mp4`, `media.hardware-video-decoding.*` is desktop only.
* GeckoView's own `geckoview-prefs.js` sets `media.eme.require-app-approval = true`, `media.geckoview.autoplay.request = true`, `media.mediadrm-widevinecdm.visible = true`: the APP decides autoplay and DRM. Our delegate was the blocker (section 1).
* Our own prefs before alpha27 that touched video: `media.av1.enabled = false` (right: AV1 is software-only on a Cortex-A53), `media.cache_*` (64 MB, 60 s read-ahead: right for an unmetered 5 Mbit/s link), `image.animation_mode = "once"`, `ui.prefersReducedMotion = 1` (wrong for video players, section 1).
* Nothing in our extension (`content.js`, `guard-main.js`) hooks media events; `guard-main.js` only touches `video, iframe, canvas, [class*=player]` to find overlay links. Not a cause.

## 4. The device (docs/HARDWARE.md + vendor data; the run-time probe is the authority)

MT8168B: 4x Cortex-A53 @ 2.0 GHz, Mali-G52 GPU, 2 GB RAM, Android 10 Go. Vendor data sheets list hardware decode for H.264 and H.265 up to 1080p; VP9 hardware decode is not confirmed for this part (that is why the rule is run-time).
`DeviceVideo` (geckoapp) enumerates `MediaCodecList(ALL_CODECS)`: per codec the decoder names, `isHardwareAccelerated`, the largest size and the 720p frame rate, secure-playback support, and the Widevine scheme + security level.
The answer is cached per system build (`Build.FINGERPRINT`), so a normal start only reads four letters (`AHvi` = H.264 hardware, H.265 hardware, no VP9, no AV1) and applies the rule at once; the first start after install / a system update asks Android off the UI thread.

## 5. What was built (alpha27)

| Piece | Where |
| :- | :- |
| Muted autoplay allowed; audible autoplay still refused; DRM ask | `geckoapp/.../web/TabDelegates.kt` `onContentPermissionRequest` |
| Codec offer rule, pure + tested | `core/.../media/VideoPolicy.kt`, `VideoPolicyTest` |
| Decoder probe + cache + Diagnostics text | `geckoapp/.../DeviceVideo.kt` |
| Live prefs on the USER branch (clear = engine default) | `GeckoEngine.applyVideo` (`media.mediasource.vp9.enabled`, `media.av1.enabled`, `media.hevc.enabled`, `ui.prefersReducedMotion`, `image.animation_mode`) |
| Settings > Video & page animation (4 rows) | `app/.../ui/SettingsView.kt`, `Prefs.videoMutedAutoplay/videoCodecs/videoDrm/calmPages`, `BackupManager` specs |
| Diagnostics > Video lab (bundled page + 4 clips), decoder list, policy line, extra pref read-back | `labs/video.html`, `DiagnosticsView`, `BrowserActivity.engineFacts`, `GeckoEngine.readBackPrefs` |
| Pref-name guard now also covers names set from Kotlin | `tools/check-gecko-prefs.sh` |
| Tests | `VideoPolicyTest` (8), `web-tests/videolab.test.js` (2: page runs in Chromium, every WebAssembly probe is a valid module) |

## 6. Research: free things considered, and the paid / proprietary list

Implemented (free, open, genuine gain, no cost at run time): everything in section 5.

Considered and **declined** (reason):

* **Bundling a software H.264 / HEVC decoder (OpenH264 BSD + Cisco's patent licence, libde265, ffmpeg)**: slower than the tablet's hardware block, more RAM and heat, and the patent position is worse than using the OS decoder. No.
* **Ruffle (Flash emulator, MIT/Apache-2.0)**: free, but the content it plays is rare in 2026, the WebAssembly bundle is 10+ MB and it is a new attack surface. Only if the owner has Flash content.
* **Google Safe Browsing**: the owner's rule is no Google services; the open lists the blocker already uses (see `docs/FILTER-LISTS.md`) cover malicious hosts.
* **dav1d / AV1 on**: only with a hardware decoder (the rule turns it on then).
* **Forcing `media.ffvpx.*` or removing WebM**: the pref does not exist in 160; VP9 direct playback (`<video src=x.webm>`) is left alone, only the adaptive-streaming offer is limited.
* **HEVC always on**: Gecko decides; the rule only turns it off when there is no hardware decoder.

Paid / proprietary / patent-encumbered items the owner could approach (no payment is needed for what is built; these are for the day a feature needs them):

| Item | Who | What it would give | Needed now? |
| :- | :- | :- | :- |
| Widevine L1 (HD/4K protected video) | Google (Widevine) via the device maker's certification | HD instead of SD in Netflix-style services | No: Android's own MediaDrm is used, the level is a property of the tablet |
| H.264 / AVC patent pool | Via LA (formerly MPEG LA) | Needed only if the browser itself shipped a decoder; the OS decoder is already licensed by the device maker | No |
| H.265 / HEVC patent pools | Access Advance, Via LA, Avanci Video | Same: only for a bundled decoder | No |
| AAC audio | Via Licensing | Same | No |
| Dolby AC-3 / E-AC-3 / Atmos, DTS decode | Dolby Laboratories, DTS (Xperi) | Playing those soundtracks if the OS has no decoder; our MP4 writer already CARRIES AC-3 / E-AC-3 without decoding | No |
| Netflix / Prime / Disney+ SDKs | the services | none: their sites play with Widevine | No |

## 7. After the fix (emulator, alpha27 build)

* `autoplay.html`: A (muted autoplay) and C (muted script play) play and their overlays hide; B (audible autoplay) still waits for a tap. No `play() REJECTED` line any more.
* Engine defaults read back (Diagnostics): `media.mediasource.vp9.enabled = false` ON THIS ENGINE BY DEFAULT: VP9 is not offered to streaming sites unless Settings > Video formats = Everything; the rule only forces it off when a future engine flips that default and the tablet has no hardware VP9. AV1 and H.265 are switched off when there is no hardware decoder.
* Video lab on the emulator: every section prints; the clips are CHOPPY there (software decoders, no video chip: 94-98 % dropped), which is what the lab is for; the tablet's numbers are the owner's part (section 8).
* Protected video: prompt -> Allow -> `Widevine: available (com.widevine.alpha)` (the emulator image has Widevine L3).
* Full list with numbers: `research/EX_RESULTS.md` "Alpha27 additions".

## 8. What only the tablet can answer (the owner's part)

1. Menu > Diagnostics > **Video lab** > Run (about 40 s) > Copy the results; paste them to Claude. It states, per clip, smooth / choppy, dropped frames and the real frame rate of the screen.
2. Diagnostics shows the decoder list: which of H.264 / H.265 / VP9 / AV1 have a HARDWARE decoder and the Widevine level.
3. Which site and which video was choppy, and whether the loading symbol appeared when the PAGE opened or when the video started (a link is enough).
