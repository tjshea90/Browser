# VD1: why Instagram Reels and some website videos do not load (2026-11-21)

Owner, verbatim: "Also diagnose why Instagram reels don't load and some videos don't load on websites".
Method: read every layer that can stop a video from starting in Kestrel G, from the code, the earlier research and the four real uploads of 2026-11-21 (none of them is an Instagram session: no uploads since alpha85 exist yet). NOTHING here was observed on the tablet; the sandbox cannot load Instagram as a logged-in tablet would. Each cause says what it would look like and how to tell it.

## Ranked causes
### 1. The press-to-play rule (ANS53a / ANS53b, owner message 53) - the strongest suspect for Reels and for feed / preview / carousel videos
What the code does (geckoapp/.../kestrel-shields/gate-main.js, page config bit 1024 in ShieldsBridge.pageConfig, TabDelegates.onContentPermissionRequest):
- every `<video>` is CLOSED until the person presses it: `preload` is forced to none, `autoplay` is not applied, a `blob:` source made from a MediaSource (every script-written player: hls.js, dash.js, Shaka, YouTube, Meta's players) is HELD, so the player waits for `sourceopen` and fetches no segment, and `play()` is refused (NotAllowedError) unless it comes with a tap;
- the engine's own answer to a MUTED autoplay is also "no" while the rule is on (`PERMISSION_AUTOPLAY_INAUDIBLE -> DENY`; the older Settings row "muted videos may start" counts only with the rule off); sound without a tap is always refused;
- the rule is GLOBAL ("not a Shields feature, every site has it"); there was no per-site exception, so the only way to let a Reels feed run was to turn the rule off for every site.
What a Reels feed does: each reel is a script-written player that starts itself when it scrolls into view (a muted `play()` without a tap). Under the rule every reel is held and refused; a tap on the picture opens THAT reel, but the next reel is a new element and is closed again; a player that waited for `sourceopen` (held) or whose `play()` was refused may have shown its error or loading state long before the tap. Same for any site whose videos start by script: autoplay-on-scroll cards, hover previews, auto-advance playlists, "click to load the player" that plays seconds later.
What shows it: Diagnostics "Videos start only when pressed ...: held back N, M script players kept from starting to fetch, refused K plays that nobody pressed". In the real files of 2026-11-21 (YouTube only) it reads 0 / 1 / 0 with 1 opened by the play button: the gate works for an ordinary press. The numbers are summed over all pages, so they could not say WHICH SITE was held back: fixed in alpha87 (per-site lines).
A one-minute test the owner can do: Settings > Videos start only when I press play = off, reload Instagram. If the reels load, this is the cause. (alpha87 adds a per-site switch so only that site is exempt.)

### 2. Embedded players wait for a tap (EX-10, Shields "Embeds", default on)
A THIRD-PARTY iframe of YouTube, Vimeo, Dailymotion, Facebook `/plugins/`, Instagram `/embed`, TikTok, Spotify, SoundCloud, Disqus, Google Maps is not loaded until the person taps its placeholder. A news page with an embedded video therefore shows a placeholder, not a video. Not Reels on instagram.com itself.

### 3. The format policy says "no" (ANS52g, codec-main.js)
The page-side answers to `MediaSource.isTypeSupported`, `canPlayType` and `mediaCapabilities.decodingInfo` refuse: any stated frame rate above 30 (the owner's "At most 30 frames a second" switch is ON in the real settings: `vid_fps30=true`), a codec above its hardware decoder's size or macroblock rate, AV1 (no hardware), and with the H.264-only mode every other codec. Orientation is handled (long side / short side), so a vertical 1080x1920 Reel is not refused for its shape. A site that offers only 60 fps or only AV1 and asks WITH the numbers can get "no" for everything. The counters (`vcx`) were global too.

### 4. Protected video (DRM)
Widevine is used only when Android's MediaDrm has it and the owner allows it, asked per site; a site that needs it on a tablet without it simply cannot play. Not Instagram.

### 5. Shields breaking one site's player (filter rule, scriptlet, cosmetic rule)
Site specific; the per-site Shields switch is the A/B (and the LD4 timing lines record `shields=up|down`).

### 6. The hotspot
5 Mbit/s: a held-then-opened video starts from nothing, so the first picture after the tap takes the time of a cold fetch. Slow, not "does not load".

### 7. Instagram itself
Reels for a logged-out visitor stop behind a login prompt after a few; mobile web may offer an "open the app" panel. Not Kestrel's fault and not testable without the owner's session.

### Ruled out
- `Save-Data` header: not sent in Kestrel G (grep: no such header in the extension or the app).
- Orientation in the format policy (above), HLS cap (Instagram web does not use HLS).

## What alpha87 adds
1. Per-site lines (never a private tab; host only): `video gate <host> held=.. kept=.. refused=.. opened=..` when the gate acted on that page, `video codec-no <host> size=.. mode=.. fps=.. last=<short word>` when the format policy said no, `video error <host> code=.. net=.. ready=.. src=blob|http|none frame=top|sub` for every media element error and for an element that never got data; they reach the live diagnostics and the digest ("VIDEO PROBLEMS by site").
2. Shields panel (the shield button) > "Videos may start by themselves" per site, default OFF, the global rule unchanged: for that site the gate is open and a muted autoplay is allowed (sound still needs a tap). A change from a private tab is session-only (PV1). This is the fix for Reels AND the test of cause 1: turn it on for instagram.com, reload.
3. The digest prints which of the causes above each site's lines point to.

## Not proven / not built
- No tablet session of Instagram was seen; cause 1 is read from the code and consistent with the symptom, not observed.
- A player that gave up waiting for `sourceopen` before the tap may need a reload even after the exception; the per-site switch avoids that by never holding.
- No change to the format policy or the DRM path without a per-site line that names them.

## VD2: pornhub.com videos spin in the page but play in Kestrel's own player (owner, 2026-11-21)
Owner, verbatim: "pornhub.com videos don't load in the webpage (loading nonstop), but they load in the external kestrel player".
What the second half proves: the film's address is found (the video finder saw it), the tablet's hardware decodes it, the hotspot carries it, and the site serves it to a plain request. So the network, the chip and the film are fine; what differs is the PAGE's own player inside the engine. Everything below the gate/format layer in VD1 applies; nothing here was observed on the tablet.
Ranked for this symptom (a spinner that never ends = a player that waits for something that never arrives):
1. The press-to-play rule (cause 1 above): a script player that waits for `sourceopen` or a response to its first `play()` stays on its loading symbol when the gate holds the blob source or refuses the play. Test: Shields panel > "Videos may start by themselves" for the site, reload.
2. A request the player needs is blocked by Shields (an ad / tracker call that the player waits for before it starts, or a CDN host on a list). Test: Shields off for the site (the per-site switch), reload; the `page timing` lines say `shields=up|down`.
3. The format policy (cause 3): if the player asks `isTypeSupported` WITH a frame rate or AV1 and gets "no" for every rung it stays empty. alpha87's `video codec-no <host>` line says it.
4. The site's own age check (the player loads after a click-through that needs storage or a cookie Kestrel's protections block), or its player frame being an embed (EX-10 waits for a tap there).
What alpha87 records for the site after one load of a pornhub page: `video gate <host> ...` (what the rule held), `video codec-no <host> ...` (what the format policy refused), `video error <host> code=.. src=..` (the element's own failure), `page timing` (shields up / down); `tools/diag_digest.py` prints them as "VIDEO PROBLEMS by site" with the cause each points to. Private tabs record nothing (PV1).
