# Video downloader for Kestrel G - research (2026-11-11, owner request 8, tasks V4 + V5)

Question: "InShot's Video Downloader: figure out how it works and let me download videos from any page by long-pressing it; several resolutions with names and file sizes;
as many sources as possible, even sites that do not offer downloads; parallel/threaded downloads; safe pause/resume."
Target: GeckoView 160 (Firefox engine), built-in MV2 extension `kestrel-shields`, Android 10 (API 29) tablet, 2 GB RAM, no ffmpeg / no native code, metered hotspot.

Tags: [V] verified in a primary source read during the research (spec, vendor docs, source code, MDN / browser-compat-data); [S] search snippet or third-party mirror only;
[U] inference, untested (must be tried on the emulator / tablet before it is relied on). Nothing here was run on a device. No app was decompiled.

## 0. Bottom line (what we build)

1. Three source families: **direct files** (mp4/webm/mp3/m4a...), **HLS VOD** (clear or AES-128), **static non-DRM DASH**. Everything else gets an honest refusal (section 8).
2. Detection = **network sniffing in the existing extension** (`webRequest.onHeadersReceived` + `onBeforeSendHeaders`, types `media`/`xmlhttprequest`/`other`) + a `document_start` content
   script for DOM / JSON-LD / og tags + a tiny MAIN-world hook only to recognise MSE (`blob:`) and EME players [U].
3. Sizes: exact for files and byte-range/sidx streams. HLS/DASH show "~" estimates from `(AVERAGE-)BANDWIDTH x duration`, refined by probing the first segments. yt-dlp dropped such
   estimates by default for "potentially extreme inaccuracy" [V], so we label them "~" and replace them with the real size when the file is done.
4. No ffmpeg: save `.ts` (TS) or `init + fragments` as `.mp4` (fMP4) as they are. Optional MediaExtractor + MediaMuxer remux (H.264+AAC to MP4, VP9+Opus to WebM). Else two files.
5. Machinery we already have: resumable multi-connection engine (`core/.../download/Model.kt`, `Engine.kt`), `UrlConnectionFetcher` + `GeckoFetcher` (kind 1), MediaStore/File sinks,
   native-messaging extension, a long-press hook (`BrowserActivity.longPress`). Missing: sniffer, manifest parsers, segment-job type, remux step, quality sheet.
6. Refuse clearly on DRM, live streams, expired links and YouTube (section 8). Show a one-time "Only save content you have the right to save." notice.

## 1. How comparable apps work

### 1.1 InShot "Video Downloader" / "XDownloader" [S]
Only store mirrors were readable (InShot's Play pages are a JavaScript shell), so its internals are UNKNOWN [U]. Published behaviour: a built-in browser that "automatically recognizes the video
and you can download it in one click", preview then download, HD support, "optionally choose the quality", download manager (pause / resume / delete, background, several files at once),
offline player, generic file downloads ("mp3, m4a, mp4, m4v, mov, avi, wmv, doc, xls, pdf, txt"). XDownloader adds private browsing, ad blocking, WhatsApp status saving. A store listing says
YouTube cannot be downloaded "because of YouTube policies". Package ids in the mirrors disagree (video.downloader.videodownloader / com.downloader.privatebrowser): unconfirmed.
The standard technique behind such apps (WebView `shouldInterceptRequest` sniffing + DOM scan) is our extension's webRequest + content script [U].

### 1.2 Peers
- **SuperX Video Downloader** (GPL-3, F-Droid): browser + HLS/MPD sniffing + background downloads. Parsers: Media3's HLS parser with a hand-written fallback (records METHOD/URI/IV per segment,
  no SAMPLE-AES/DRM), an MPD parser, native ffmpeg (`-c copy -bsf:a aac_adtstoasc -movflags +faststart`) to merge. Needs native ffmpeg, so NOT a model for us (no native code) [V].
- **Stream Detector** (Firefox WebExtension, MPL-2): `onBeforeSendHeaders` + `onHeadersReceived` on all URLs, no type filter; matches by URL substring and exact lower-case Content-Type
  (a `; charset=` suffix breaks it); keeps UA / Referer / Cookie per request; size = passive Content-Length only [V].
- **Video DownloadHelper**: permissions tabs, downloads, webRequest, webNavigation, scripting, declarativeNetRequest, `<all_urls>`: the same toolbox as proposed here [V, permissions only].
- **yt-dlp** is the reference page scraper and segment downloader (below).

### 1.3 Detection methods in the wild
- Network sniffing by URL pattern + Content-Type [V]. yt-dlp generic: Content-Type regex `(audio|video|application(?=/(?:ogg$|(?:vnd\.apple\.|x-)?mpegurl)))/`, sniffs the first 512 bytes for
  `#EXTM3U`, sends `Accept-Encoding: identity`, parses XML roots (MPD, ISM, SMIL, XSPF, RSS, F4M).
- DOM scan [V]: `<video>/<audio>/<source>`, og tags, JSON-LD, JW Player / Plyr / video.js / Flowplayer configs, `twitter:player:stream`. yt-dlp does NOT scan `<a href>` (we add that [U]).
- JS hooks in the page [U]: wrap `MediaSource.addSourceBuffer`, `URL.createObjectURL`, `fetch`/XHR, `navigator.requestMediaKeySystemAccess` to label blob players and spot EME.

### 1.4 Sizes
Passive Content-Length [V]; for 206 replies the total is the last number of `Content-Range: bytes a-b/total` (a `Range: bytes=0-0` probe returns it) [V MDN]; JSON-LD `contentSize` /
`duration` hints; manifest bandwidth x duration = estimate only [V yt-dlp]. Exact sizes: `EXT-X-BYTERANGE` lengths, DASH `sidx`/`indexRange`, SegmentList `mediaRange`.

## 2. Detection signals on an arbitrary page

| Signal | What to read | Tag |
| --- | --- | --- |
| `<video>`/`<audio>` (+ `amp-`/`dl8-`) | `src`, `data-src`, `data-video-src`, `data-source`; nested `<source>` (`type`, `label`/`title`, `res`/`height`); yt-dlp sets `Referer` on every format | [V] |
| Live element state | `currentSrc`, `duration`, `videoWidth/Height`, `readyState`, `mediaKeys` | [U] |
| Open Graph | `og:video`, `:url`, `:secure_url`, `:type`, `:width`, `:height`; `og:audio` | [V] |
| JSON-LD `VideoObject` | `contentUrl` = actual bytes; `embedUrl` = a player page, not a file; `contentSize`, `duration`, `encodingFormat`, `bitrate` | [V] |
| Twitter card | `twitter:player:stream` (raw stream), `twitter:player` (iframe) | [V] |
| Anchors to `.mp4/.webm/.m3u8/.mpd` | not scanned by yt-dlp; add | [U] |
| JW Player / Plyr / video.js / Flowplayer | `sources[].file/type/label`, `drm` block flags DRM; Plyr `sources:[{src,type,size}]`; `player.src({src,type})`; `clip.url` | [V] |
| Manifests by header | `application/vnd.apple.mpegurl`, `audio/mpegurl`, `application/x-mpegURL`; path `.m3u8`/`.m3u` | [V] |
| Manifests by body | `#EXTM3U`; XML root `MPD` (many servers send wrong MIME types: sniff small bodies); `application/dash+xml` | [V]/[U] |
| XHR / fetch / media requests | webRequest `type`: `xmlhttprequest` (XHR + fetch), `media`, `other` | [V] |
| `blob:` sources | `video.src = URL.createObjectURL(mediaSource)`; no network address | [V] |
| Embedded players | cross-origin iframes: `all_frames` content scripts; webRequest `frameId`, `parentFrameId`, `documentUrl`, `originUrl` | [V] |
| Shadow DOM | `element.openOrClosedShadowRoot` reaches closed roots | [V partial] |

Real segment URLs behind a `blob:` player [U]: the player (hls.js, dash.js, Shaka) fetches manifest + segments with XHR/fetch, which webRequest sees as `xmlhttprequest` whatever the blob;
sniff them, group by `tabId`/`frameId`/`documentUrl`, show only the manifest; a MAIN-world wrapper on `addSourceBuffer` yields mime/codecs and tags the element as MSE; map the long-pressed
`<video>` to a candidate with `document.elementsFromPoint`; rank manifest > largest media, same frame, most recent.
GeckoView 160 may play `<video src=x.m3u8>` through its native Android HLS path (Media3); that traffic would bypass Necko and webRequest [U; test].

## 3. MV2 WebExtension in GeckoView 160 (BCD "Firefox Android" columns; GeckoView-specific behaviour is [U] until tested)

- `webRequest.onBeforeSendHeaders` (`requestHeaders`) and `onHeadersReceived` (`responseHeaders`), `onCompleted`, `onResponseStarted`: supported [V]. Non-blocking listeners suffice.
- Details: `type` (`media`, `xmlhttprequest`, `other`, `main_frame`, `sub_frame`...), `tabId` (-1 if none), `frameId` (0 = main), `parentFrameId`, `originUrl`, `documentUrl`, `fromCache`.
  NOT supported on Android: `incognito`, `cookieStoreId`, `thirdParty`, `urlClassification`; `frameAncestors` listed unsupported in `onHeadersReceived` (our `onBeforeRequest` already uses it).
  Map private tabs through `tabId` to the session in the app; confirm the extension runs in private tabs [U].
- Cookie / Referer / User-Agent come from `onBeforeSendHeaders` [V Stream Detector]. Prefer replaying through the Gecko fetcher (profile cookie jar incl. HttpOnly) instead of storing Cookie values in jobs [U].
- `filterResponseData`: listed from FF Android 57, but streaming multi-MB bodies through extension JS is a memory trap: do not use [V]/[U].
- Host permissions needed: our manifest already has `http://*/*`, `https://*/*`, `ws(s)://*/*`.
- Page world: manifest `content_scripts` with `"world": "MAIN"` + `run_at: "document_start"` (MAIN supported from Firefox 128 [V]); a static manifest entry is preferred (dynamic registrations are dropped when
  the registering page unloads, and Android can kill extension processes [V]). Whether MAIN scripts run before page scripts in GeckoView 160 is untested [U]. Page-to-content-script bridge: `window.postMessage`.
- Native messaging to the app: `runtime.sendNativeMessage` / `connectNative`; needs `nativeMessaging` + `geckoViewAddons`; content scripts need `nativeMessagingFromContent` (built-in extensions only);
  the app registers a name with `WebExtension.setMessageDelegate` (background) / `SessionController.setMessageDelegate` (content scripts; `sender.session` identifies the GeckoSession).
  Repo finding: the engine rejected structured replies ("Invalid event data for callback"), so the extension sends/receives plain JSON STRINGS (see `background.js`).
- Long-press: `ContentDelegate.onContextMenu(session, x, y, element)`: `type` (NONE/IMAGE/VIDEO/AUDIO), `srcUri` (= `currentSrc`, a `blob:` URL for MSE players), `linkUri`, `baseUri`, `title`.
  `BrowserActivity.longPress` handles only links and images today. Overlay divs often swallow the press (`TYPE_NONE`): ALSO show a persistent chip/badge with the candidate count as the primary UI [U].
- Replaying: `GeckoWebExecutor.fetch(WebRequest[, flags])` (Gecko network stack); `FETCH_FLAGS_ANONYMOUS` = no cookies, so the default sends them [U]; `FETCH_FLAGS_PRIVATE` ties it to the private
  session; `WebRequest.Builder` has `header()`, `referrer()`, `cacheMode()`. The repo already has it as fetcher kind 1 (`GeckoFetcher`, falls back to HttpURLConnection when no runtime).
- NOT available / unverified: bytes of `blob:` / MSE appends / Media3 HLS in webRequest; `downloads` and `cookies` APIs in GeckoView (unknown: do not depend on them); service-worker requests (`tabId = -1` probably).

## 4. HLS [RFC 8216, Apple authoring spec, yt-dlp hls.py]

- Playlist: path `.m3u8`/`.m3u` or the MIME types above; starts `#EXTM3U`. Master = all URIs are playlists; media = all URIs are segments (`#EXT-X-TARGETDURATION` present means media). Relative URIs resolve
  against the (post-redirect) playlist URL. Segments: MPEG-TS, fMP4, packed audio, WebVTT. HEVC/AV1 only in fMP4; fMP4 needs `EXT-X-MAP`.
- Master: `EXT-X-STREAM-INF` (`BANDWIDTH` = peak, `AVERAGE-BANDWIDTH`, `CODECS`, `RESOLUTION`, `FRAME-RATE`, `AUDIO`/`SUBTITLES` group ids) with the URI on the next line; `EXT-X-MEDIA`
  (`TYPE`, `GROUP-ID`, `NAME`, `DEFAULT`, `AUTOSELECT`, `LANGUAGE`, `URI`): no `URI` = audio muxed into the variant; with `URI` = a separate playlist (two downloads + a merge). Ignore `EXT-X-I-FRAME-STREAM-INF`.
  Label [U]: height from `RESOLUTION` (+ "60" if FRAME-RATE >= 50), codec family from `CODECS` (avc1 H.264, hvc1/hev1 H.265, vp09, av01), audio-only as "Audio ~N kbps".
- Media: `EXTINF`, `EXT-X-MEDIA-SEQUENCE` (default 0), `EXT-X-ENDLIST` = complete. **No ENDLIST = live/event: refuse.** `EXT-X-DISCONTINUITY` = timestamps/format may change (ads!): do not blindly mux as one
  file. `EXT-X-GAP`: do not fetch. Ignore `EXT-X-PART` (low latency); use whole segments.
- Encryption: `EXT-X-KEY:METHOD=NONE|AES-128|SAMPLE-AES(-CTR)`. AES-128 = whole segment AES-128-CBC, PKCS7, restarted per segment; IV = `IV` attribute or the media sequence number as a 16-byte big-endian value.
  Key = 16-byte resource at `URI` (fetch with the segment headers). `KEYFORMAT` other than `identity` (FairPlay, Widevine/PlayReady UUIDs) or METHOD SAMPLE-AES* = DRM: refuse. Android: `Cipher "AES/CBC/PKCS5Padding"`, chunked `update()`.
- `EXT-X-MAP:URI[,BYTERANGE]` init section, output order init then segments. `EXT-X-BYTERANGE:n[@o]`: without `o` it starts after the previous range of the same resource; `Range: bytes=o-(o+n-1)`.
- Size estimate [U]: `(AVERAGE-BANDWIDTH ?: BANDWIDTH) x sum(EXTINF) / 8`; `BANDWIDTH` already includes the audio rendition, do not add it again. Error: compliant VOD with AVERAGE-BANDWIDTH about +/-10 %;
  BANDWIDTH only up to ~2.2x over; typical VBR 1.2-1.6x over; non-Apple packagers no guarantee. Better, in order: (1) sum BYTERANGE lengths (exact), (2) probe init + first 3 segments and extrapolate
  bytes/second x duration (about +/-5-15 %), (3) bandwidth formula. Show "~", recompute as segments finish, show the real size at the end.

## 5. DASH [ISO 23009-1, DASH-IF, yt-dlp]

- MPD `type` default `static`; `dynamic` = live: refuse. `Period` > `AdaptationSet` (`contentType`, `lang`, `segmentAlignment`) > `Representation` (`id`, `bandwidth`, `width`, `height`, `codecs`, `mimeType`, `frameRate`,
  `audioSamplingRate`); `BaseURL` resolves representation > adaptation set > period > MPD. Sum Period durations; multi-period with different init segments: main Period only or refuse.
- Addressing: (1) SegmentTemplate + SegmentTimeline (`S@t`, `S@d`, `S@r` = ADDITIONAL repeats; negative `r` only on the last `S` = repeat to the Period end; yt-dlp does not handle `r=-1`);
  (2) SegmentTemplate with `@duration`; (3) SegmentList (`SegmentURL@media/@mediaRange`, `Initialization@sourceURL/@range`); (4) SegmentBase + `indexRange` = one CMAF file at `BaseURL`
  (plain download = the whole file, no sidx parsing needed; sidx gives exact segment sizes). Template ids: `$RepresentationID$`, `$Number$`, `$Bandwidth$`, `$Time$`, `$$`, printf widths `$Number%05d$`; `startNumber` default 1.
- DRM: any `ContentProtection` on the chosen Representation = protected (refuse). System ids: Widevine `edef8ba9-79d6-4ace-a3c8-27dcd51d21ed`, PlayReady `9a04f079-9840-4286-ab92-e65be0885f95`,
  FairPlay `94ce86fb-07ff-4f43-adb8-93d2fa968ca2`, common PSSH `1077efec-c0b2-4d02-ace3-3c1e52e2fb4b`, ClearKey `e2719d58-a985-b3c9-781a-b030af78d30e`; `urn:mpeg:dash:mp4protection:2011` carries the 4CC (cenc/cbcs).
  After download: `MediaExtractor.getPsshInfo()` / `getDrmInitData()` / `SAMPLE_FLAG_ENCRYPTED` as a second check.
- Separate audio + video: per Representation concatenate `init + segments` to one fragmented MP4/WebM; merge only combinations MediaMuxer can write (avc1+mp4a to MP4; vp09+opus to WebM); never offer
  1080p VP9 + AAC as one file; prefer one Period, `segmentAlignment`.
- Size [U]: `bandwidth x duration / 8` (peak-like, "~"); exact for SegmentBase (HEAD / sidx); `contentLength` on `BaseURL` when present.

## 6. Android 10 muxing without ffmpeg [V Android reference]

- MediaMuxer: MP4 since API 18, WebM 21, `MediaMuxer(FileDescriptor, format)` 26 (WebM needs a read-write fd), B-frames in MP4 25, one video + one audio track everywhere.
  Codecs written: AAC, AVC, H.263, MPEG-4, AMR in MP4 (16); HEVC in MP4 (24); VP8 + Vorbis in WebM (21); VP9 in WebM (24); Opus in WebM (29); AV1 in MP4 only from API 34 (branch on SDK_INT).
  On API 29: MP4 = AAC + H.264/HEVC; WebM = VP8/VP9/Vorbis/Opus. Opus or VP9 in MP4 is NOT muxable.
- MediaExtractor reads: MPEG-TS = H.264 + AAC only; MP4 = H.264, HEVC, MPEG-4, VP9, AV1; WebM/MKV = VP8, VP9, Opus, Vorbis; raw ADTS `.aac`. Platform HLS playback = MPEG-2 TS only.
- Pitfalls: (1) codec-specific data is mandatory for `addTrack` (AAC AudioSpecificConfig, AVC SPS/PPS, HEVC): pass `MediaExtractor.getTrackFormat(i)` straight in; (2) AAC from TS is ADTS-framed and
  ffmpeg's `aac_adtstoasc` has no equivalent here: whether Android's TS extractor supplies csd-0 is UNCONFIRMED, test on the tablet; (3) write samples in chronological order per track and interleave by timestamp;
  (4) subtract a common base PTS (TS PTS rarely starts at 0); after a DISCONTINUITY run one extractor per group + running offset, or keep `.ts`; (5) B-frames fine on API 29; send an end-of-stream
  buffer with the last timestamp; (6) `setOrientationHint` before `start()`; (7) refuse if `SAMPLE_FLAG_ENCRYPTED`; (8) `stop()` throws if a track has no samples, always `release()`, write to a temp file then
  rename/move into MediaStore; (9) fMP4: `init + fragments` is already a playable fragmented MP4, MediaExtractor on it is untested; (10) keep one sample buffer (`KEY_MAX_INPUT_SIZE`, about 1 MB).
- When NOT to mux: TS with HEVC/AC-3 or discontinuities = keep `.ts`; fMP4 with a single muxed track = save `init + fragments` as `.mp4`; incompatible DASH codecs = two files "video" + "audio";
  audio-only = concatenated ADTS `.aac` / `.mp3` plays directly.

## 7. Fast and safe segment downloads

Reference (yt-dlp `fragment.py`): retries per fragment; `skip_unavailable_fragments` defaults true (silent gaps: we do NOT copy that); resume via a state file with the fragment index (sequential only);
a thread pool with ordered append; byte-range fragments use `Range: bytes=start-(end-1)`.

Design for Kestrel [U unless noted]:
- Job record: page URL, manifest URL, chosen variant ids, absolute segment table (index, URL, optional range, duration, key id, IV), key bytes, scrubbed header snapshot.
- Per-segment files `seg-000123.part` renamed atomically to `seg-000123` when complete; the completion bitmap is rebuilt from a directory listing on resume (a 2 h video at 6 s = about 1200 segments).
  Resume restarts a segment from byte 0 (segments are small); `ETag` / `If-Range` only for single big files; a `200` reply to a `Range` request means the server ignored it: never append it [V MDN].
- Concatenate strictly in playlist order, `EXT-X-MAP` first; decrypt while writing; sanity-check first bytes (TS sync byte `0x47` at 0 and every 188 bytes, or an `ftyp`/`moof` box) to catch HTML error pages
  saved as segments; delete segment files as they are appended; check free space first (staging + output about 2x).
- Parallelism: 3-4 connections for segment jobs (do not starve the page being browsed), drop to 1-2 after repeated timeouts or while a page loads; single big files keep the multi-connection `Seg` engine.
- Retries: 408/429/5xx/IO: up to about 5 with exponential backoff + jitter; 401/403 mid-job: stop with "link expired", re-detect from the page (signed URLs expire, Range GETs included [V CloudFront]);
  404 on a segment: fail or ask the user to accept a gap.
- Memory: 32-64 KB buffers per connection, never a whole segment (up to ~10 MB at 1080p); one `Cipher` per segment with chunked `update()`; a single muxer sample buffer. Own heap < ~25 MB (CLAUDE.md).
- Headers: `Referer` (page URL / origin), `Origin` for CORS-checking CDNs, the GeckoView `User-Agent`, `Accept-Encoding: identity`; Cookie through the Gecko fetcher jar, not stored in the job. The engine's
  `Rec.headers` already replays UA/Referer and scrubs on terminal states; `UrlConnectionFetcher` drops Cookie/Authorization on cross-host redirects.
- Metered hotspot: reuse the existing metered confirm, pause on network loss and `WAITING_NETWORK`.

## 8. Where it cannot work (and what the app says)

| Case | Why | Message |
| --- | --- | --- |
| DRM / EME (Netflix, Disney+, Prime...) | keys live only in the CDM; EME is a common API to it [V W3C]. Detected by `ContentProtection`, non-identity KEYFORMAT / SAMPLE-AES, `encrypted` event, `requestMediaKeySystemAccess` hook, JW `drm` block | "This video is copy-protected (DRM). Kestrel can't save it." |
| Short-lived signed / IP-bound URLs | expiry blocks restarts and Range GETs; a hotspot IP change breaks IP-bound ones [V]/[U]. 401/403 mid-job | "The site's link expired. Reload the page and tap Download again." |
| Referer / cookie / UA gates, one-time tokens | server checks request context; 403 on replay | "The site refused the download request." |
| YouTube | `n`/signature JS challenges need a JS runtime, PO tokens, SABR-only clients, some DRM; its terms forbid downloading [V] | "Kestrel doesn't download from YouTube." |
| Live (no ENDLIST, dynamic MPD) | sliding window [V] | "Live streams can't be saved." |
| MSE with JS-side decryption / WebRTC / WebSocket video | no manifest or file URL on the wire | "No downloadable file found for this player." |
| Native Media3 HLS in GeckoView | requests may bypass webRequest [U] | same as above; test first |
| Codec / muxer mismatch | MediaMuxer API 29 limits | "Saved as separate video and audio files." |
| Multi-period / discontinuities | timestamp resets, different init segments | "Saved as .ts" or main Period only |
| Disk | FAT32 4 GB limit on some cards; engine already has NO_SPACE / FS_LIMIT | existing messages |

Terms and responsibility (not legal advice): personal, non-commercial tool; the person saving is responsible for having the right to. YouTube's terms forbid downloading except where authorised and forbid
circumventing the service [V]. Behaviour: never defeat DRM, tokens or access controls; one-time notice "Only save content you have the right to save."; do not market YouTube support; no telemetry, never upload
page URLs. Peers ship similar disclaimers (SuperX: "research and educational purposes").

## 9. Build plan (most coverage per unit of work)

- **P0 direct files (about 2 days).** Extension: non-blocking `onHeadersReceived` (types `media`, `xmlhttprequest`, `other`) + `onBeforeSendHeaders`; classify by Content-Type or extension (first bytes for manifests);
  dedupe by URL without Range and collapse a flurry of Range requests into one candidate with the `Content-Range` total; send compact JSON STRINGS to the app
  (`{"k":"media","tab":..,"fr":..,"page":"..","u":"..","ct":"..","len":..,"ref":".."}`); `document_start` content script for `<video>/<source>/og/JSON-LD`.
  App: candidate list per tab; badge/chip; `longPress` for `TYPE_VIDEO`/`TYPE_AUDIO`; quality sheet with sizes; `DownloadController.enqueue(Source(url, pageUrl, headers, fetcher = 1))`;
  hide candidates Shields blocks; drop candidates under about 30 s [U].
- **P1 HLS VOD (about 4-5 days).** Kotlin parser (master + media, MAP, BYTERANGE, KEY AES-128, ENDLIST) in `:core` with tests; quality sheet with "~" sizes; segment job on the engine; output `.ts` or `.mp4` (fMP4);
  hard refusals for SAMPLE-AES, live, non-identity KEYFORMAT.
- **P2 optional remux (about 2-3 days).** MediaExtractor + MediaMuxer for TS (H.264+AAC) to MP4; skip with discontinuities; test csd-0 / timestamps on the tablet first.
- **P3 DASH static non-DRM (about 4 days).** SegmentTemplate/Timeline/List/Base; video + audio combos merged with MediaMuxer when the codecs allow, else two files.
- **P4 MAIN-world hook (1-2 days).** Wrap `addSourceBuffer`, `createObjectURL`, `requestMediaKeySystemAccess`; flag blob players, detect EME early, hide the badge for protected streams.
- **P5 polish.** Size probing, error messages, private tabs (extension in private; RAM-only / private flag), local test server (the repo already uses 10.0.2.2:8000).

Verify before building each stage (emulator first, tablet when the owner can): (1) webRequest reports `media` and `xmlhttprequest` with `tabId >= 0` for a GeckoSession page; (2) a MAIN-world content script runs before page
scripts in GeckoView 160; (3) whether `<video src=*.m3u8>` traffic is visible to webRequest (the Media3 question); (4) `GeckoWebExecutor` sends cookies by default and honours `referrer()`; (5) MediaExtractor on a
concatenated `.ts` yields AAC csd-0 and handles a fragmented MP4 file; (6) the extension works in private tabs and shows `incognito` as absent.

## 10. Sources

InShot mirrors: apkvision.org, modyolo.com, appgoblin.info [S]. Peers: github.com/alexch33/super-video-downloader (+ F-Droid page), github.com/54ac/stream-detector, addons.mozilla.org Video DownloadHelper.
yt-dlp: README, `YoutubeDL.py`, `extractor/{generic,common,genericembeds}.py`, `downloader/{hls,fragment}.py`, wiki EJS and PO-Token pages. Specs: RFC 8216 (+ the 8216bis draft), Apple HLS authoring spec,
ISO DASH XSD, DASH-IF timing model and content-protection identifiers, W3C EME, schema.org VideoObject, ogp.me, JW Player / Plyr docs. Browser: MDN (webRequest, MediaSource, Content scripts, Range requests),
mdn/browser-compat-data (webRequest, contentScripts, scripting), GeckoView docs (web-extensions, ContextElement, GeckoWebExecutor, WebRequest.Builder; javadoc is 162.0a1), Extension Workshop (Android differences),
Bugzilla 1896931 / 1911651 (Media3 HLS). Android: MediaMuxer, MediaExtractor, supported-formats, Cipher. Google Play policy 9888379; YouTube terms; CloudFront signed URLs.
