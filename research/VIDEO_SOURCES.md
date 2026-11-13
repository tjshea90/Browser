# VIDEO_SOURCES - finding more videos and saving more of them (owner message 25, 2026-11-13, tasks AK4)

> "a movie from doraby.com on the vidsrc server couldn't be downloaded in kestrel, but video downloader app can download it. See if there are additional ways to find video sources for the app and make the app better at finding and downloading videos"

Design of the downloader itself: `research/VIDEO_DOWNLOADER.md` (V4/V5). This file is what was found and changed for embedded players. The real doraby.com / vidsrc server could NOT be reached from the build sandbox and the tablet was not available:
everything below is proven on a LOCAL LAB (`tools/sitelab/vidlab_srv.py`, section 5) that behaves like such a player, and by unit and web tests. What the real site does is NOT known; the new `media` lines in Diagnostics (section 6) will say.

## 1. The most likely cause (found by reading our own code; proven on the lab)

Doraby shows its movie in an iframe from another site (the "server" buttons: vidfast, vidsrc ...). The stream's CDN (the host behind that player) answers **403 unless the request names the PLAYER as the asker**: `Referer: https://<player site>/` and, for
script requests, `Origin: https://<player site>`. A "video downloader" app built on a WebView sees every request header the page sent and repeats them. Kestrel repeated the **web page's** (doraby.com) origin instead:
`VideoFlow.requestHeaders(tab)` built Referer and Origin from `tab.url` (the top page) for the playlist read, the size probes, every segment and key, and the plain-file download. The extension already told the app which frame asked
(`MediaCandidate.frame`, the `f` field), but nothing used it. So: sniffed fine, listed fine, then "The stream's list could not be read (the site may have refused it)", or the download failed on its first part.

## 2. What changed (alpha31)

| # | Change | Where |
| :- | :- | :- |
| 1 | The extension records the **Referer and Origin the browser really sent** with each script / media request (`webRequest.onBeforeSendHeaders`, types media / xmlhttprequest / other / object, kept 160 request ids) and sends them (`rf`, `og`) with every `media` message. The app keeps only their ORIGINS (a path or query can carry a token and the replay goes to other hosts too). | `background.js`, `ShieldsBridge.onMedia`, `MediaHeaders.refererOf/originOf` |
| 2 | Header sets tried **in order**: what the browser sent -> the player's page (the frame) -> the web page -> the Referer alone -> nothing. The playlist read (HLS and DASH) tries them until the site answers with a real playlist, and **remembers the set that worked** (`MediaCandidate.viaHeaders`); the qualities, size probes, segments, AES key and companion sound all repeat it. A plain file brings the headers its player used. | `MediaHeaders.sets`, `VideoFlow.fetchPlaylist/requestHeaders/pick`, `StreamSource.headers` |
| 3 | **Honest reasons**: "The site refused to show the stream's list (HTTP 403), even with the player's own headers. Reload the page, press play and try again: the link may have expired", "...is gone (HTTP 404)", "(no answer from the site)". | `VideoFlow.whyNotRead` |
| 4 | **Playlists and videos in disguise**: stream sites serve playlists as `text/plain` / `application/octet-stream` under `.txt`, `.css`, no extension; videos under `.bin`. A response that says nothing about being media is sent to the app as a `maybe` (script-made requests only; not for pages, JSON, scripts, fonts, tiny or huge replies, `<img>`; twelve per page, once per address). The app reads its **first 4 KB with the same headers** and decides by the bytes: `#EXTM3U` = HLS, `<MPD` = DASH, `ftyp` / EBML / OggS / FLV / AVI / ID3 / MPEG sync = a file. Nothing is offered unless the bytes say so. | `background.js maybeMedia`, `VideoFlow.maybe`, `MediaSniff.classifyBytes` |
| 5 | **Parts disguised as pictures**: a tiny real PNG / GIF / JPEG written in front of every transport-stream part (named `.jpg` / `.png`). The picture header is skipped (clear parts: also any garbage before the first run of three sync bytes 188 apart; encrypted parts: only a recognised picture header, since cipher text has no sync). | `SegmentPrefix`, `StreamEngine.appendNext/decideMux` |

## 3. All the ways Kestrel now finds a video source (existing + new)

| Signal | Status |
| :- | :- |
| Network: response Content-Type video/*, audio/*, mpegurl, dash+xml; or URL extension mp4 m4v mov webm mkv avi wmv flv 3gp mpg m4a mp3 aac ogg opus wav flac m3u8 m3u mpd | existing (`onHeadersReceived`, any frame, `MediaSniff.classify`) |
| Network: **the same by the first bytes** when type and name say nothing | **new** (`maybe`) |
| Page markup: `<video>`, `<audio>`, `<source>`, `og:video`, `twitter:player:stream`, `<link rel=preload as=video>`, links to media files, media addresses inside inline scripts / JSON (plain, `\/` and `/` escapes) | existing (`content.js`, all frames, found before play) |
| What the page's own player says (picture size, length, the label next to a `<source>`) | existing (`vid` message) |
| The long-pressed `<video>` / `<audio>` element's own address | existing (`VideoFlow.longPress`) |
| Master playlists: every quality and the separate audio; media playlists; DASH static (SegmentTemplate / Timeline / List / Base) | existing (`Hls`, `Dash`) |
| The player's own request headers, per candidate | **new** |

## 4. What cannot be done (said plainly in the app)

* **A player that keeps its stream out of the network**: MSE (`blob:`) with the segments decrypted or assembled in JavaScript, WebSocket or WebRTC video. No address exists to save.
* **DRM** (Widevine / PlayReady / FairPlay, SAMPLE-AES, `ContentProtection`): refused on purpose, never circumvented.
* **Live streams** (no `#EXT-X-ENDLIST`, dynamic MPD).
* **Short-lived or one-time signed links**: the link may be dead by the time Download is tapped; the message says reload and press play. A server that refuses even the player's own headers (cookie-bound, IP-bound) cannot be replayed.
* Requests made by a **service worker** or a worker carry no tab: a stream fetched only from one is not attributed (not seen on any page yet; test first).

## 5. The lab (reproduce without the real site)

`bash tools/sitelab/vidlab_gen.sh <folder outside the repo>` makes two real 12-second H.264 + AAC HLS streams with ffmpeg; `VIDLAB_GEN=<folder> python3 -I tools/sitelab/vidlab_srv.py` serves
* a movie page `http://10.0.2.2:8000/watch` with an iframe to
* a player page `http://10.0.2.2:8003/embed/movie` whose script fetches the playlist, a variant playlist, the key and two parts from
* a CDN on port 8004 that answers **403 unless `Referer` starts with the player's origin (and `Origin`, when sent, equals it)**; the playlists are `text/plain` under `.txt`, the parts are `image/jpeg` named `.jpg`, each a real PNG header followed by the AES-128 encrypted transport stream, the key is a 16-byte octet-stream.
`lab.log` lists every request with its Referer / Origin / status: that is the proof of which headers the app's own reads and downloads used. Open the page in the debug build on the emulator (`am start -a android.intent.action.VIEW -d http://10.0.2.2:8000/watch`).

## 6. Where to look when a video is still not found or will not save (Diagnostics > Copy)

`Health` lines (layer `media`): `maybe <host> type= len=` (a disguised candidate was looked at), `maybe-hit <host> HLS ext= set=n/m` (it was a playlist), `maybe-no` (its bytes were not media), `maybe-refused <host> http=403` (the site refused even the other header sets),
`list-ok <host> http= set=n/m referer=<host>` (a playlist was read; which header set worked and the Referer host it carried), `list-failed <host> http=N tried=m`, `file <host> referer=<host>` (a plain file download started).
No line at all for a page = the extension saw no media-looking request (an MSE-only player or one that starts only after play).
