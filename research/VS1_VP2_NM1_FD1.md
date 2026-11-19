# VS1 / VP2 / NM1 / FD1: embedded players that stay on "loading", file names, the floating download window (owner messages 2026-11-18)

Owner, verbatim:
- VS1: "When i go to doraby.Com and try to download a movie from vidsrc server, it can't find the video to download, but the video downloader app i have can. try to figure out how to make it find and download"
- VS1d: "Even the vidfast server on doraby.Com can't find the video. it just stays stuck on loading"
- FD1: "make the download manager float and be resizeable and moved around the screen just like the keyboard."
- NM1: "make the video downloader smarter at automatically naming videos. for example, i downloaded ozark season 1 episode 1, but it named it just ozark. Then when i went to download episode 2, it also named it just ozark. See if the video downloader can deduce what each video should be named based on different clues such as the site or link name or if i press episode 2 on the site, etc."
- VP2: "the browser needs better support for playing videos. I selected three different video servers on doraby.Com but none if them would play. It showed no error, it just couldn't load the video."

## VS1 / VS1d / VP2: why a server frame stays on "loading" (read from the code; lab-proven only in the simulator)

- Kestrel's press-to-play gate (gate-main.js, alpha57; the owner's hard rule: nothing plays unless he presses play) keeps every media element CLOSED until a real press and HOLDS the MediaSource (blob:) a script player makes until a press in THAT frame.
- doraby.com shows its player as a cross-origin child frame (vidsrc, vidfast, ...). The person taps doraby's own big play button or server button in the TOP page. Browser user activation does not reach a cross-origin child frame, and lasts only about 5 s. So the embed's player script asked for a MediaSource, the gate held it waiting for a press that had already gone to another frame: the player shows its spinner for ever, with no error. Three servers, one cause = a cause in Kestrel, not in one site.
- The downloader pipeline itself (sniffing the playlist / file the frame asks for, the Save video sheet) was proven in the lab for four embed styles (suite vs, variants mse, auto, click, nest, worker); the finder finds nothing only when the player never starts.

### Fix (VP2)

- content.js (top frame only) sends a `ptap` message for a real tap in the page (trusted event); background.js remembers it (personTap, personFrames) and answers the frames of that tab when they ask the guard: reply field `pp` (a person tap inside the last 15 s). The gate cfg gets '1p' / '1+p'.
- gate-main.js: a frame that is told `pp` opens its gate ONCE by INHERITANCE (INHERIT_MS, `inheritUntil`, `big(el)`): only an element that is big enough to be a main player (not a 1x1 tracker, not an ad box) is opened; the 'inherit' open also lifts the preload / autoplay hold (re-checks `r.open` after adopt / setAttribute / setters). It expires after 90 s (test hook `window.__kstGateInheritMs`).
- The rule stays: no tap near the player = nothing plays. A page that starts a film on its own (no person tap in the last 15 s) is still held (lab variant msel0 must show HELD; variant msel with the tap must show PLAYING).
- Files: geckoapp assets kestrel-shields/content.js, background.js, gate-main.js; tests web-tests/gate.test.js, background.test.js, vp2.test.js (614 web tests green on 2026-11-18).
- NOT proven: any real doraby / vidsrc / vidfast embed (the sandbox cannot reach them). Unknown that only the tablet answers: whether the stuck state on the real sites is the held MediaSource (this fix) or something else (a blocked host, a codec the engine lacks, a frame sandbox, a script our shields stub). If it still happens: Diagnostics + the Health lines `gate` / `guard` and which server.

## NM1: naming a downloaded film

Before: the name came from the page title, so every episode of "Ozark" was "Ozark".

Now (core EpisodeNames, NameClues, MediaNames; geckoapp names.js, ShieldsBridge "nm", VideoFlow.stemFor):
- Clue sources, strongest first: (1) the frame / playlist / file ADDRESS of the film (`/tv/<id>/<season>/<episode>`, `?season=2&episode=3`, `s01e02`, `1x02`); (2) the ACTIVE item of the page's own episode list (the highlighted episode / season, class names active / current / selected / playing / ep); (3) JSON-LD TVEpisode / Movie, og:title, h1, title. The clue closest to what the person pressed wins over the page title.
- The show name loses a trailing "Season N" (dropSeasonTail); the result is `Show S01E02` (+ the episode title when the page has one) or `Movie (year)`; it is shown in the Save video sheet and can be edited as before.
- The embed's own frame address is carried on the sniffed item (`frameChain`), so a clue in a frame URL reaches the name even when the page title is only the show.
- names.js runs in the top frame only (document_idle) and sends its clues once per page change.
- Proven: web-tests/names.test.js, core EpisodeNames tests (release names, url marks, season tails). NOT proven: the real doraby markup (class names of its episode list are guessed from common patterns).

## FD1: the floating download window

- core `ui/FloatBox` (pure Kotlin, FloatBoxTest): geometry = (right gap, top gap, width, list height) in dp; `clamp`, `moved`, `resizedFromBottomLeft`, `store` / `load` (null = first place), `isDefault`. Same rules in spirit as the keyboard panel (MiniNav PanelLayout): kept inside the page area, the stored numbers are never changed by a small screen for a moment.
- app `ui/DownloadPopup`: the popup view now fills the page area (not clickable, so empty places let every touch through) and the window floats inside it (FloatFrame):
  - a finger that LANDS on the title bar and moves past the touch slop carries the window; a long press (400 ms, PICK_MS) anywhere on it picks it up (haptic, lit corner) like the keyboard; a quick drag in the list before that scrolls the list;
  - the corner handle at the bottom left resizes (width 200-420 dp, list height 90-640 dp); a double tap on the corner puts the window back;
  - place and size are saved after each gesture (Prefs.dlGeom, Health `downloads window-moved / window-resized / window-reset`); Settings > "Reset the downloads window";
  - at its first place it still sits below the "Download started" banner while the banner shows.
- Bug found by the new Robolectric test (DownloadPopupFloatTest, 8 tests green): the drag was judged by where the finger was at its FIRST MOVE, so a quick drag from the title bar (already far below the strip by then) was not picked up; now `downOnHeader` is taken at touch-down.
- NOT proven on the tablet: the feel of the gestures on real hardware (slop, long press time), and no lab scene yet moves the window on the engine (the Robolectric test covers the gesture logic only).

## Lab proof (2026-11-18, run 44236411726, suite new, Android 10 emulator with the real Firefox engine)

- First lab run (44233990309) looked bad on the embed variants (msel0 'started') but the lab had the press-to-play rule OFF for the whole run (Diagnostics said: Videos start only when pressed (OFF)). So it proved nothing about the gate, either way. soak.py vs_scene now sets `vpress 1` for the embed (mse) variants, `vpress 0` for the finder variants, and reads the embed's states per variant (it used to read the whole log).
- Second run, rule ON: mse = sourceopen, playing; msel (embed waits 7 s) = sourceopen, playing; msel0 (nobody pressed) = stuck, i.e. held, the rule is intact. Finder variants auto / click / nest / worker: stream listed, one pick saved a real file (404422 bytes).
- Honest limit: there is no BEFORE-fix run with the rule ON, so the lab shows the fix lets a tapped server frame play and an unpressed one stay held, not that the old code was stuck in this exact lab page.
- DM1 / DM2: 9 checks ok (banner only for a new download; cold start after front / background kill shows no banner, window or list; notification launch opens the list once, replay ignored).
