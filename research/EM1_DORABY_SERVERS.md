# EM1i - the six doraby.com servers, measured against the REAL site (2026-11-19)

Owner request (message 2026-11-19, item 8): "finding and properly loading videos from different servers (try doraby.com various movie servers) and finding and keeping sources for video downloading and smart naming of video downloads".

How this was measured: the sandbox reaches doraby.com through its proxy (earlier sessions wrongly wrote that it cannot). Python read the markup; headless Chromium (Playwright, a 600 px wide window) loaded the player chain, clicked once on the player and logged request addresses,
content types and the first 12 bytes of each response. No media was played to the end, nothing was kept, no body was stored. The player hosts CHANGE OFTEN: treat the host names below as the state of 2026-11-19, the SHAPES as the lasting facts.

## The chain (the same for all six servers)
1. Title page `https://doraby.com/<title>/` lists the servers as `li.server[data-load-embed-host=...]`; a click sets the page's `iframe#iframe` to `https://doraby.com/?player_tv=<id>&s=<season>&e=<episode>&sv=<host>&tv=true` (movies: `player_mv`).
2. That wrapper page holds ONE iframe `./getPlayTV.php?id=<tmdb>&s=1&e=1&sv=<host>&playtv=true`.
3. `getPlayTV.php` answers a few hundred bytes of script that sets `location` to the server's own player. So the player is 3 frames below the title page, and some servers go 2 more frames deeper (Vidsrc).

| label on the page | `sv=` | player address (2026-11-19) | what the player does (headless Chromium, one click) |
| :- | :- | :- | :- |
| Vidfast | Vidfast | vidfast.vc/tv/<tmdb>/<s>/<e> | NOT reachable from the sandbox (chrome-error); the owner reports "stuck on loading" (VS1d / VP2) |
| 111movies | Openvid | embed.vidrift.net/embed/tv/.. | hls.js; `fetch /api/source/tv/..?token=<signed>` returns the stream address; telemetry POSTs; a Cloudflare challenge call |
| Vidsrc | movieclub | furniturebeast.nl/embed/tv/.. -> stellarconductornexus.com/embed/.. (2 more frames) | `vs_src.php` JSON, `api.php?..&stream_urls` JSON (`application/json`), `generate.php` (a token, text/plain), then **`/pl/<id>?token=<jwt>` as XHR, `text/plain`: a master playlist (936 B) and a media playlist (72 KB), both starting `#EXTM3U`**; then the **parts as `/content/<id>/<id>/page-N.html?token=..`, `Content-Type: text/html`, 150-730 KB each, first byte `G` (0x47 = MPEG-TS sync)**, fed by XHR into a `blob:` video (1920 wide, readyState 4) |
| Videasy | vidlink | www.vidy.st/tv/.. (Next.js) | metadata only (TMDB-style JSON); nothing started by one click: the real play button is inside the page |
| Vidzee | vidsrc2 | play.xpass.top/e/tv/.. (JW Player) | `data/tv/..` (text/plain, encrypted blob), `mdata/<id>/1/playlist.json` (JSON naming the master), then **`master.m3u8` with `application/vnd.apple.mpegurl`** on tik.1x2.space and vip.1x2.space, media playlists with the same type, **parts at `/content/<long id>` with no extension** (some answer 403 `{"code":3403}` to a data-centre client, others 200 `text/html` TS), 11 subtitle `.vtt` files |
| Vidcore | Vidsrc | vidcore.net/tv/.. | NOT reachable from the sandbox (chrome-error) |

## What this means for Kestrel's finder (read against background.js `maybeMedia` and core MediaHeaders)
- Vidzee: the master has the right content type: found by type alone. Parts with no extension are never candidates (right: they are what the playlist names).
- Vidsrc: the playlist is `text/plain`, 936 B / 72 KB, XHR, address with no extension and a `?token=` query: exactly the "maybe" path (`MAYBE_CT`, 80 B <= size), so the app must REPLAY the request to read the first bytes. A single-use or header-bound token makes the replay answer 403 / 404: the finder then sees nothing although the page plays. The parts (`.html`, `text/html`) are correctly NOT candidates (`NOT_MAYBE` skips `.html`).
- Frame depth 5: every frame is covered by `all_frames`, but the finder's "page" for a candidate must be the TOP page (doraby title page), not the wrapper, or the captured row (VS2b) and the names (NM1) lose their clues.
- Press-to-play (owner setting, ON): a tap on the page's play button does not reach the cross-origin frames as user activation; VP2 covered the MediaSource case with lab pages.

## Open decision: sniff in place instead of replaying
`webRequest.filterResponseData` needs a BLOCKING `onHeadersReceived` listener (the HLS cap already has one, narrowed to `.m3u8` addresses). A sniffer for text/plain / octet-stream XHR would run for every XHR response (blocking = the response waits for the extension): too costly on four A53 cores for all pages. A cheaper form: register it only for tabs that already showed a sign of video (an m3u8 / mpd / video request, a `<video>` report, a `blob:` media element). Not built: needs a real-engine proof first; the lab suite `dr` shows how often the replay fails on the real servers.

## Lab
`android.yml app=lab lab_suite=dr` (or `em` = `ro` + `dr`): tools/emu/soak.py `doraby_round`, writes doraby.md and events.jsonl (kind `doraby`): per server the finder's answer after the tap and its own Health lines (`media maybe-hit`, `maybe-no`, `maybe-refused http=..`).

## Result of the first real-engine run (lab run 44435517079, 2026-11-19): inconclusive, one useful fact
- vidfast.vc answers from GitHub's runners (the sandbox cannot reach it). Its player makes three `POST` requests with `text/plain` answers to its own host; the finder logs them as `media maybe ... method=POST` and `maybe-skipped (an API call, not a stream)`. After two taps no stream request followed: the player did not get as far as asking for a stream (the owner's 'stuck on loading', VS1d / VP2). What gates it (press-to-play on a cross-origin frame, an anti-bot check, a missing click on the player's own button) is NOT known yet: the next step is the same scene with press-to-play OFF to see whether a stream request appears.
- The second server's player (embed.vidrift.net, hls.js, a Cloudflare challenge) froze the emulator's software GPU and the phone's system died: the scene is restart-safe now; servers 2-6 still have no result. A software-GL emulator is a poor judge of heavy video players: for those, the owner's Diagnostics (`media maybe`, `maybe-hit`, `maybe-refused http=`) from the tablet stay the real evidence.
