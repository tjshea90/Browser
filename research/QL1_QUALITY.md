# QL1: picking the video quality in full screen on as many sites as possible (owner message 2026-11-18)

Owner, verbatim: "When i put YouTube video in full screen, it doesn't let me choose the video quality. I want to be able to choose the video quality on as many sites and videos as possible"

## What was there (read from the code, 2026-11-18)

- Kestrel's full-screen bar (`PlayerControlsView`, driven by `PlayerController`) already had a **Quality** button, but only for a film the app knew a PLAYLIST of: `BrowserActivity.openPageQuality` listed the rungs of a sniffed HLS/DASH master list (`video.pageFilm(tab)`), and a pick STOPPED the page's film and handed it to Kestrel's own player from the same second (ANS53d.2).
- YouTube (and many other sites) play through Media Source: the page's own script holds the quality list and the switch; no playlist address ever reaches the app. The app declines them by design ("no safe source URL to hand over"). So `pageFilm` was null, `showQuality` false: **no button, no list**. That is the owner's report.

## What was built

The quality list is taken from the page's OWN player and a pick is passed back to it, so the film goes on in the page with no gap and without Kestrel's player (the choice a site's gear menu gives, drawn by Kestrel in the full-screen controls).

| Piece | File | What it does |
| :- | :- | :- |
| page hook (MAIN world, document_start) | `kestrel-shields/quality-main.js` | wraps the constructors of the three well-known globals (`Hls`, `dashjs.MediaPlayer().create()`, `shaka.Player`) so every player a page makes is noted (at most 6, the page's own objects). It only watches; nothing a page sees changes except that the constructor is a Proxy of the original (statics, prototype, instanceof still work) |
| adapters (isolated world, document_idle) | `kestrel-shields/quality.js` | `KestrelQ.find(video)` tries, in order: **yt** (`#movie_player` / `.html5-video-player`: `getAvailableQualityLevels`, `getAvailableQualityData` for labels like 1080p60, `setPlaybackQualityRange(id, id)` and `setPlaybackQuality`, Automatic = range `tiny..highres`), **jw** (`jwplayer(id).getQualityLevels/setCurrentQuality`), **vjs** (videojs-contrib-quality-levels: enable exactly one level), **hls** (`levels`, `currentLevel`, `autoLevelEnabled`), **dash** (`getBitrateInfoListFor('video')`, `setQualityFor`, ABR switch), **shaka** (`getVariantTracks`, `selectVariantTrack`, `configure({abr})`), **src** (`<source size|res|label>` lists: moves to the other file at the same second). A family counts only with at least two rows. The page's objects are reached through `wrappedJSObject`; only short strings and numbers are read out |
| report / command | `kestrel-shields/player.js` | the full-screen report (`vst`) carries `q` (the list, first two reports, when it changes, every 20th report), `qc` (the player's current pick) and `qf` (family); the app's answer can carry `{a:'ql', q:<id>}`, which calls the adapter's `set` |
| rules | `core/player/PageLevels.kt` | the page is untrusted: ids `[A-Za-z0-9_.:-]{1,24}`, labels cleaned and cut to 22, at most 12 rows, Automatic first, fewer than two rows = no choice, a pick can only name an id of the list the app holds |
| app | `PlayerController` (`pageQualityReport`, `hasPageLevels`, `setPageQuality`), `BrowserActivity.openPageLevels / pagePicked`, `QualitySheet` | the Quality button shows when the page's player offers a list OR a playlist is known (the old hand-over rows stay as they were when the page has no list of its own); the sheet title is "Quality", the check mark is on what the person picked this full screen, else on the player's own state; a pick closes the sheet and toasts "Quality: 720p" |
| Diagnostics | `PlayerController.pageQualityLine` | "Quality list from the page's own player (full-screen controls): lists seen N, picks N; last: yt: Automatic, 1080p ..." |

## What is proven and what is not

- Proven: `web-tests/quality.test.js` (12 tests, real headless Chromium, MOCK players with the public API method names): rows, order and labels per family, the exact call each pick makes, the Proxy hooks keep `instanceof` and statics, hostile ids/labels are cut, a command with an odd id calls nothing, a page with no list reports no `q`. `core PageLevelsTest` (7).
- On the real engine (emulator lab suite `ql`, also in `vp`): a lab page puts YouTube's player API on a `#movie_player` element (defined by the PAGE's script, so the isolated script must go through `wrappedJSObject`): the Quality button appears, the list opens with the page's rows, a pick of 1080p reaches the page as `setPlaybackQualityRange(hd1080, hd1080)`, Automatic as `(tiny, highres)`. Result: see the end of this file.
- **NOT proven: any real site.** The mocks agree with what this code believes each player's API is; the sandbox cannot reach real YouTube. Unknowns that only the tablet answers: (1) does `#movie_player` on m.youtube.com / www.youtube.com expose `getAvailableQualityLevels` and `setPlaybackQualityRange` to the content script through `wrappedJSObject` on Gecko 160; (2) does YouTube honour `setPlaybackQualityRange(q, q)` mid-play on the mobile site and `(tiny, highres)` as "automatic"; (3) the hls.js / dash.js / Shaka hooks only see players made by a page that sets the global (`window.Hls`, `window.dashjs`, `window.shaka`); a bundled player that keeps its instance in a closure is invisible, and then the old rows (sniffed playlist) are all there is.
- Not done (candidates): a family that drives a site's own gear menu through the DOM (Vimeo, Dailymotion, Twitch, Facebook ...: too fragile to write blind), remembering the pick per site, Vimeo's iframe player API.

## How the owner can help the next round

After the update: open a YouTube film, go full screen, tap the Quality button (top right, left of the exit button). If it shows no list, send Diagnostics: the line "Quality list from the page's own player" and the Health lines `player quality-levels` say whether a list ever arrived. If it shows a list and a pick changes nothing, say which site and what the film did.
