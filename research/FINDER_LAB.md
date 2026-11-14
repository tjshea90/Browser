# Finding the real video on a page (RX3-V1 .. V7, research/REVIEW-EXTERNAL-3.md part V) - design notes and measurements
Newest section last. ASCII only. The finder lab pages themselves are `tools/sitelab/vidlab_srv.py` (EY-21); new pages for V1-V5 are listed in section 0 when they exist.

## 0. Where things are (read 2026-11-14)
- Page side: `content.js` `report(v)` on every `loadedmetadata` sends `{k:'vid', u, w, h, d, l, p}` -> `background.js` relays -> `ShieldsBridge.onVid` -> `mediaInfo` -> `VideoFlow` -> `MediaList.applyInfo`.
- App side: `MediaList` (48 ranked entries, `usable()` = what the dock badge counts), `MediaRank.rank` (sheet: `main` and "Other files"), `MediaCandidate.touched` (somebody showed interest: never evicted, never hidden).
- The defect V1 fixes: `applyInfo` set `touched = true` for EVERY element that reported, so a hover preview, a muted looping hero video, a GIF-style loop or a 15-30 s pre-roll at 720p was "played, long or sharp" in `usableUnsorted` and stayed in the badge.

## 1. RX3-V1 - what a `<video>` is for (alpha42)
Facts the page sends with the report (all numbers, no address beyond the page the report already named): bit flags `f` = muted 1, loop 2, autoplay 4, controls 8 (the native attribute OR a known player root around it: jwplayer, video-js, plyr, mejs__container, flowplayer, shaka-video-container, html5-video-player), visible 16 (not display:none / visibility:hidden, laid out), ad 32; rendered size `rw` x `rh` in CSS pixels; `vp` = percent of its box inside the viewport.
The ad flag: an ancestor (8 levels, never body) whose class or id, split at every non-alphanumeric character, holds the token `ad`, `ads`, `advert`, `advertisement`, `adslot` or one of the ad-state classes of the players (`jw-flag-ads`, `vjs-ad-playing`, `ima-ad-container`, `plyr__ads`, `ad-showing`, `ad-interrupting`). Whole tokens only: `address`, `headline`, `download`, `add-to-cart` do not match.
`core/media/VideoRole.kt` (pure): `decide(facts, durationMs)` -> PLAYER | DECOR | AD. No facts (an old extension, a report from somewhere else) = PLAYER (nothing changes). AD = ad flag. DECOR = (loop && muted && !controls) || (muted && autoplay && !controls && 0 < duration <= 30 s) || (rendered area > 0 and under 160 x 90).
`MediaList.applyInfo(..., role)`: PLAYER sets `touched` as before; DECOR / AD record the role and do NOT set `touched`; the latest report wins; `touch(url)` (the owner long-pressed, opened or downloaded it) still sets `touched`, and `touched` always shows. `usableUnsorted` hides a DECOR / AD entry before the "played, long or sharp" exemption (a stream too). `MediaRank.junk` sends them to "Other files" (never deleted: one tap). `value()` makes them the first to be evicted from a full list.
Cost of a wrong guess: a muted autoplay clip of under 30 s with custom controls that nobody recognised is hidden from the badge and sits one tap away under "Other files on this page".

### 1.1 Emulator proof (2026-11-14; Android 10 x86_64 emulator, GeckoView 160 debug build: NOT the tablet)
Lab: `tools/sitelab/guardlab.py` page `/finderlab` (four REAL media elements, 20 s silent audio each, described differently by the page itself) + `tools/emu/playerlab_emu.py --path /finderlab --grep "video info"`; the app logs one line per report: `video info role=... length=...s counted=N of M` (no address).
| Element | What the page says | Role the app decided |
| :- | :- | :- |
| `<video muted loop autoplay>` 300x100, no controls | a hero loop | DECOR |
| `<video controls>` 300x140 | a player | PLAYER |
| `<video controls>` inside `div.sidebar.ad-slot` | an ad slot | AD |
| `<video controls>` 120x60 | thumbnail-sized | DECOR (area 7200 < 14400) |
Final line of the run: `counted=1 of 4` - only the player counts for the dock badge; the other three stay in the list for "Other files" (the sheet), nothing deleted.
Defect found by the first emulator run and fixed (with a JVM test): the element's report can arrive BEFORE the extension reported the file (loadedmetadata vs the response), and `applyInfo` then dropped it (`counted=0 of 0`, two roles lost). `MediaList` now keeps what an element said about a file it does not hold yet (32 files, forgotten with the list) and applies it when the file is added.
Tests: core `VideoRoleTest` (7), `MediaListTest` (+9), `MediaRankTest` (+4); web-tests `videofacts.test.js` (9, real Chromium), `background.test.js` (+1 relay). NOT done: a real site's hover previews and pre-rolls (the sandbox cannot reach one that plays them), the tablet.
