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

## 2. RX3-V2 - verify before counting (alpha42)
Problem (read in the code): an address found only in the page's text (a link to `.mp4`, an og tag, a string in a script: the extension's `dom` reports, `source "dom"`) has no length and no media type; `MediaList.usableUnsorted` treated an UNKNOWN length (-1) as usable, so a page with one link to an animated teaser showed the download badge.
Rules (`MediaCandidate.fromMarkup`, `.verified`): an entry is VERIFIED when it was really requested (seen on the wire; `fromMarkup` false) OR a player reported it / the owner touched it OR (a plain file) its length is known and at least 100 KB. `MediaList.usableCount()` (the dock badge) counts verified, non-decoy entries only. Unverified entries stay in `usable()` (the sheet) and `MediaRank` sends them to "Other files" (the sheet is never empty: the fallback keeps the best one on top). A file under 100 KB whose length is learned is dropped from the sheet as before.
The cheap check (`VideoFlow.verifySoon/verifyNow`): when a markup entry is added, 2.5 s later (and never while the page is still loading, up to 6 retries 2 s apart) a page with NOTHING verified gets up to three one-byte Range requests (`MediaList.unverifiedToCheck(3)`, newest first) with the player's own headers; the answer's `Content-Range` total goes to `MediaList.setLength` (which bumps the list version so the badge recounts). Same request the sheet already made for the entries on top of it (`sized` set shared: nobody is asked twice). Private tabs ask through the private fetcher. Cost: three one-byte requests per such page.
Wrong-guess cost: a real video that is only linked, whose server ignores Range and sends no Content-Length, stays out of the badge until it is played or long-pressed; it is still in the sheet.
Tests: core `MediaListTest` (+5), `MediaRankTest` (+2). Emulator: lab page `/teaser-link` (section 2.1 when run).

### 2.1 Emulator proof (2026-11-14; Android 10 x86_64 emulator, GeckoView 160 debug build: NOT the tablet)
Lab: `/teaser-link` (two links, no player: `/teaser.mp4` 30 KB and `/bigfile.mp4` 700 KB, both answer `Range: bytes=0-0` with `Content-Range`), driver `tools/emu/playerlab_emu.py --path /teaser-link --grep "media verif|video info"`.
Lab server log: the page loaded at 23:11:28, the app asked BOTH addresses at 23:11:32 (`Range=bytes=0-0`, 4 s later: 2.5 s idle delay + the page's own settling). App Health: `media verify asked=2 counted=0 of 2` (before: nothing counted), then `media verified length=683 KB counted=1 of 2` and `media verified length=29 KB counted=1 of 2`: only the big file counts for the badge; the teaser stays under "Other files" (and under 100 KB it is not offered at all).
