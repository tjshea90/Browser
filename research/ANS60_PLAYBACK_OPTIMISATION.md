# ANS60d: making Kestrel's own player smoother, faster and lighter on THIS tablet

Owner message 60 (2026-11-16), point 4: "it works well now, but see if there are any additional ways to make playback smoother or faster or more optimized for my hardware. I want it as optimized as possible".
Status at the time: the owner's own words about alpha58/alpha60 were "very smooth", "noticeably smoother than when it was inside the website", "The video player works well. YouTube works well." So this round is
about what is left on the table, with a rule: **change only what is safe or provably useful, measure the rest, and say plainly what could not be measured here** (no tablet, no hotspot, an x86 emulator with software decoding).

## 1. What the hardware is (docs/HARDWARE.md and the owner's own Diagnostics)

- MediaTek MT8168B: four in-order Cortex-A53 cores, Mali-G52 MC2, 2 GB RAM, Android 10 Go, 32-bit userspace, 600 x 973 px at 170 dpi (about 564 x 916 dp), 60 Hz.
- Video decoders (Android MediaCodec, from the owner's Diagnostics): **hardware** H.264 `MTK.VIDEO.DECODER.AVC` up to 2048x1088@145 fps, H.265 `...HEVC`, VP9 `...VP9` up to 2048x1088@68, VP8, MPEG-4, MPEG-2.
  No hardware AV1 and no hardware AAC / Opus / Vorbis (audio is software, which is cheap).
- The engine's own measurement on the tablet (Diagnostics "video fitness" lines): H.264 720p30 and 1080p30, VP9 720p30, H.265 720p30 all played with **0 of 150 frames dropped**, a start in 300 to 410 ms.
  So the decoder is not the limit for these; what limits playback on this tablet is **the network** (a phone hotspot capped at 5 Mbit/s by the carrier, carrier-routed, flaky) and **everything else running on four slow cores**.

## 2. What Kestrel's own player already did (so none of it was touched)

Media3 / ExoPlayer 1.11.1 on a SurfaceView (zero-copy output), hardware decoders sorted first and decoder fallback on, no extension (software) decoders, HLS or a file with the page's own request headers,
buffers 15 to 30 s with a 16 MiB ceiling (the stock 50 s would eat a 128 MB heap class), 1.5 s to start and 3 s after a stall, Automatic capped to 16:9 of the picked class (480p inside a page, 720p full screen),
a track selector that already limits pictures to the physical display size (Media3's default: a 1080p rung is not chosen on a 973 x 600 screen unless it is the only one), no frame rate above 60,
audio focus, becoming-noisy handling, the engine's own user agent, and a hand-over that stops the page's own player (one decoder at a time).

## 3. What was changed in alpha61 (all in `PlayerFactory`, so both players get it)

| change | why it helps this tablet | risk |
| :- | :- | :- |
| **Back buffer of 10 s, kept from the keyframe before it** (`DefaultLoadControl.Builder.setBackBuffer(10_000, true)`) | The owner's new gestures and buttons go BACKWARDS a lot (10 s back button, double tap left, drag left). Without a back buffer every backward seek re-downloads the seconds that were just played: a stall of several seconds on a 5 Mbit/s link. With it a backward seek within 10 s is instant. Media3's own docs: with `retainBackBufferFromKeyframe = true` seeking back inside the buffer is fast, with false only when a keyframe precedes the target. About 3 MB at 2.5 Mbit/s, inside the same 16 MiB ceiling. | none known; a seek farther back than 10 s fetches as before |
| **A first guess of the link of 4 Mbit/s** (`DefaultBandwidthMeter.Builder.setInitialBitrateEstimate(NETWORK_TYPE_WIFI, 4_000_000)`), ONE meter for the whole run | Before it has measured anything ExoPlayer guesses about 5 to 6 Mbit/s for Wi-Fi; on a hotspot capped at 5 that starts one step too high (Akamai's note on ExoPlayer: a first pick the link cannot sustain gives a long start-up and rebuffering). The shared meter also carries what film 1 measured into film 2. | a faster home Wi-Fi starts one step lower for a few seconds, then adapts |
| **Connect and read timeouts 10 s (were 15 and 20)** | A connection that goes quiet on a carrier-routed hotspot is given up after 10 s and asked again; the wait before a stalled segment is tried again halves. | a server that needs more than 10 s to send the first byte of a segment fails (none seen) |
| **`WAKE_MODE_NETWORK`** (a Wi-Fi lock plus a CPU wake lock while it plays; `WAKE_LOCK` was already in the manifest) | The Wi-Fi chip does not drop into power save between two segment downloads. Android's own guidance for streaming over Wi-Fi is to hold a WifiLock. (The screen is on while a film plays, so this is a small gain, not a cure.) | a lock held while playing; released with the player |
| **Asking Android's codec list once, in the idle time** (`PlayerFactory.warm()`, 20 s after the browser is first on screen, background priority) | Media3 asks MediaCodecList for every format and keeps the answer for the run; each question builds a fresh MediaCodecList, which the Diagnostics show costs 100 to 600 ms on this tablet. The first film of a run paid that inside the press-to-picture time. | a few hundred ms of one background core, once |

## 4. What is measured now (so the next round is data, not guesses)

`PlaybackMeter` (one per film, both players) writes one `stats` line per film to Diagnostics and the emulator lab, and the sums to the "Kestrel's own player" line:
playable after N ms, stalls (count, total and longest wait, a wait caused by a seek or a pause is not counted), the player's own link estimate, the picture's bitrate, how often the picture size changed,
dropped pictures, seconds buffered ahead at the end. The owner's next Diagnostics will show whether 5 Mbit/s really is the limit, whether stalls still happen, and which rung Automatic settles on.

## 5. Considered and NOT done (with the reason)

- **Asynchronous MediaCodec queueing** (`DefaultRenderersFactory.forceEnableMediaCodecAsynchronousQueueing()`): Media3 turns it on by default only from Android 12 (API 31). Reports are mixed (helpful for 60 fps Widevine stutter on some devices) and there is no official advice to force it on Android 10; a MediaTek chip's behaviour cannot be tried here. Revisit only if the new numbers show dropped frames on the tablet.
- **Tunnelled video playback**: a known source of black pictures on cheap MediaTek devices, and nothing here can prove it safe.
- **A disk cache for segments** (`SimpleCache`): rewatching is rare; the back buffer covers the real case (going back a few seconds); writing every segment to the tablet's slow storage would add I/O jitter and wear.
- **OkHttp or Cronet for the data source**: a new dependency (the rules say none), and Android's HttpURLConnection already keeps connections alive and speaks HTTP/2 to the CDNs that offer it.
- **Bigger or smaller forward buffers**: 15 s to 30 s and 16 MiB suit a 5 Mbit/s link and a 128 MB heap; without stall numbers from the tablet any change is a guess. The new stall counter decides this next time.
- **A 30 fps cap for Automatic**: the display-size limit already keeps 60 fps rungs (they are large) out of reach on this screen; the Settings switch "At most 30 frames a second" is for the page's own players.
- **`SeekParameters.CLOSEST_SYNC`** (keyframe-only seeks, faster on long GOPs): it would make the owner's small precise jumps (3 s, 5 s) land on a keyframe up to a segment away; exactness matters more here.
- **Baseline profile / ahead-of-time compiled player code**: `androidx.profileinstaller` 1.4.0 is already in the release graph (it arrives with another dependency), so the profile Media3 ships inside its libraries is installed at the app's first run; nothing to add.
- **Pre-fetching the playlist before the press**: forbidden by the owner's own rule (nothing loads until play is pressed).

## 6. What cannot be shown here

The emulator decodes in software on an x86 CPU with a fast network: it proves that nothing broke (the film still starts, plays, seeks, changes picture, ends), not that anything got faster.
The effects above are expected from the mechanisms, not measured. The next tablet Diagnostics ("Kestrel's own player: ... playable ... after the start, waited for the network ...") is the measurement.

Sources (read 2026-11-16): Media3 `DefaultLoadControl.Builder.setBackBuffer` and `LoadControl` reference (developer.android.com/reference/androidx/media3/exoplayer/LoadControl); `DefaultBandwidthMeter` and `InitialBitrateSupplier`
reference; Akamai, "Enhancing Video Streaming Quality for ExoPlayer, Part 2: buffering strategy"; the Media3 1.9.0 release notes (late-frame drop threshold); `C.WakeMode` reference and Android's "Play media in the background" (WifiLock advice);
the ExoPlayer team's blog on asynchronous MediaCodec operation.

## 7. A lab finding on the way (not a playback change): the first page after a Settings change can still see the old format policy

Lab run 43960432251 (alpha61 candidate, suite vp) failed 14 checks of the page-format scene (cp) in two of its four modes (h264only and auto-nochip: the page had seen the policy of the mode BEFORE, in both of its passes);
the two other modes passed in the same run and every earlier run had passed (alpha60: 101 of 101). Nothing of alpha61 touches that path. Cause (read in `kestrel-shields/background.js`, lines 28-48 and 592-616 and `guardAnswer`):
the extension asks the app for the page answer (flags + the video format policy `codecCfg`) when a page STARTS, NOT awaited ("the answer beats the document's first bytes over a hotspot"), and a page's own content script
asks `guard` at its start, which is answered at once from the cached flags with whatever `codecCfg` the extension knew before the new answer came back. On the tablet over a hotspot the app's answer (a few ms) wins; on the lab's
localhost server (first bytes in under a millisecond, a busy 2-core emulator) the page now and then wins. Effect for the owner: after changing "Video formats" in Settings, the FIRST page that starts within a few ms of the answer
can offer the old formats; the next page is right. Not fixed in the extension (a bounded wait for an in-flight ask in `guardAnswer` would do it: TASKS.md ANS60e); the lab now loads one throw-away page of the same host before the page that is judged.
