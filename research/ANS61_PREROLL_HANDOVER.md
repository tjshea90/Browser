# ANS61: a pre-roll advertisement played in Kestrel's player and the real video could not be reached

Owner message 61 (2026-11-17): "I went to a site with a video and it played an advertisement video and I couldn't get to the real video". The site was not named. This file is the reading of the code (no device run), the cause that fits, and what was changed.

## How the hand-over decides (read 2026-11-17)

1. The gate (`kestrel-shields/gate-main.js`, page world) holds every `<video>` closed until the person presses. On a press (or the page's own `play()`), `factsOf` reports the element: its address (`currentSrc`), blob or not, box, position, duration.
2. `content.js` -> `background.js` ('vhand') -> `ShieldsBridge` ('vh') -> `VideoFlow.start` -> core `WebHandOver.decide`.
3. `decide`: when the element's own address is a file or a playlist (`directOf`) it is taken as it is. The only ad rule: an element that says it is under 40 s (`AD_MAX_S`) while a LONG HLS film of the same frame is already known is swapped for that film.
4. A yes starts Kestrel's player (inside the page or full screen); the page's element stays closed and its `play()` is rejected with AbortError.

## Why the real video was out of reach (fits the report; not seen on the site)

- A pre-roll from an ad plugin (Google IMA custom playback, videojs-contrib-ads, JW / Fluid VAST) plays on the SAME `<video>` element: the plugin sets `src` to the ad file and calls `play()`. The person has just pressed play, so the gate's question arrives with the AD file as the element's address.
- Nothing in `decide` knew about an ad state. The swap rule needs a long HLS film already sniffed; a site that loads its film only after the ad (or as a plain mp4, or through a script player) has none, so the ad file was handed to Kestrel's player.
- The page's ad plugin never saw its ad play (its `play()` was rejected, the element closed), so it never moved on to the film. After the ad ended in Kestrel's player, the next press asked again, the element still held the ad address, `resumeMs` saw a clip played to its end and started it from the beginning: the same ad again. An ad loop with no way to the film.

## What changed (ANS61b)

1. Page says "ad": `gate-main.js factsOf` adds `a: 1` when the element or an ancestor (8 levels, shadow hosts crossed) carries an ad-state class that a player's own source sets (`jw-flag-ads`, `jw-flag-ads-vpaid`, `jw-flag-ads-googleima`, `vjs-ad-playing`, `ad-showing`, `ad-interrupting`) or sits inside `ima-ad-container` / `plyr__ads` (same word lists as `players.js`). `content.js` and `background.js` pass the one bit through; `VideoFlow.elementOf` reads it into `WebHandOver.Element.ad`.
2. Core `WebHandOver.decide` refuses (null = the page plays it itself) when `el.ad`, when the address the element holds looks like an advertisement file (`WebHandOver.looksLikeAdAddress`: whole words ad, ads, advert*, adserv*, preroll, pre-roll, vast, vpaid, imasdk, adunit, ad_type, adid in the path or query, never the host), or when the finder already flagged that very file as an ad / decoration (`VideoFlow.start` asks `tab.media`).
3. Loop breaker, whatever the detection missed: `WebHandOver.playedThrough` - the same address that Kestrel's player played to its end (clip of at most 2 minutes, ended no more than 10 minutes ago) is NOT handed over again: the page plays it itself, so its ad plugin can finish and go on to the film. The film then plays in the page; Kestrel's own bar ('kestrel' question, bypasses the 15 s no) offers the film to Kestrel's player once the ad is gone.
4. Not done on purpose: guessing "short clip = ad" without any other sign (a legitimate 30 s clip would lose Kestrel's player); faking the page's `ended` event (the ad plugin's state is the page's business).

## What was NOT run

Nothing on the tablet and no real ad. Proof is JVM tests (WebHandOverTest), a Playwright test of the gate facts (web-tests/handover.test.js) and the source-reading guard in OwnerRulesTest. A lab scene on the real engine (pre-roll then film on one element) is ANS61c.
