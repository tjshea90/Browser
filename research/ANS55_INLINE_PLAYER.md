# ANS55 (owner message 55, 2026-11-16 about 15:35Z, on alpha58): the player inside the website

## What the owner said (verbatim in INBOX.md and TASKS.md ANS55a-g)
"The video player isn't working. I opened a video on Yahoo and it showed the time of the video and it was playing but the video was black. It showed no video. When I went back and switched to another video, it simply resumed the other video which was still black. It also opened the player in full screen. Can the player be inside the website so the video plays but I still see the website around it?"

## Part 1: the black video (fixed in alpha59, ANS55e)
Reproduced from the sandbox on the real page (Playwright, scratchpad script pw_yhls.js; Yahoo uses the Shaka player and Brightcove): the page asks for ONE master list (edge.api.brightcove.com/playback/v1/accounts/<id>/videos/<video id>/master.m3u8 with a 570 character token) and then the lists of the three renditions of that film (manifest.prod.boltdns.net/manifest/v1/hls/v5/clear/<account>/<policy>/<rendition id>/6s/rendition.m3u8 for the picture and the sound, .../<id>/rendition.m3u8 for the WebVTT subtitles), every one as long as the film (163 s) and with other tokens than the master names. Alpha58 chose "the longest list of the frame, ties to the shortest address": a rendition list (sound or subtitles) went to Kestrel's player: it ran its clock, 0 pictures, 0 frames, "decoder none". Fixes: a MASTER list is the film (core `WebHandOver.filmOf`: masters first, the film-length ones, the newest the page asked for; a bare list only when it is the only HLS list and never for a list that came late); `MediaCandidate.master` (the early look read it as a master) + `lastSeenAt`; Kestrel's player gives the film back after 7 s of playing with no first picture (`NativePlayerActivity.sample`); the hand-over Health line names the list's host. The other video resumed the black one because "the film of the frame" stayed the same list and my resume rule continued it: the newest master is the film now.

## Part 2: the player inside the website (ANS55d / ANS55f)
Goal: after a press the film plays in Kestrel's own player drawn OVER THE PAGE'S OWN VIDEO BOX, the page visible and scrollable around it, with a full-screen button; the full-screen player (NativePlayerActivity) stays for that button and as the fallback.

### Facts that decide the design
- GeckoView sits in `BrowserActivity.content` (a FrameLayout); a second view added above it draws over the page. A SurfaceView with `setZOrderMediaOverlay(true)` goes above GeckoView's own surface; ordinary Views added after it (the controls) draw over both.
- GeckoView 160 has `GeckoSession.CompositorScrollDelegate.onScrollChanged(session, ScrollPositionUpdate)` (the compositor's own scroll offset, called as the page scrolls) and `ScrollDelegate`. The overlay follows the page by that, not by messages from the page's script (a starved page thread is exactly what makes the page's player lag).
- The page reports the element's box in the question it already sends (`factsOf` in gate-main.js: x, y, w, h in CSS pixels relative to the layout viewport, `visualViewport` scale and offsets, `devicePixelRatio`). The app turns that into view pixels (scale = view width / visual viewport width) and places the overlay there. An element in an iframe has no usable position (the frame's place in the page is not known to its own script): those films keep the full-screen player.
- Touches: the overlay consumes taps inside its box (a tap that reached the page's own paused element would start the page's player again); a drag that starts on it is given to GeckoView (a cached DOWN is re-dispatched when the finger moves more than the touch slop) so the page still scrolls.
- The film is stopped (not paused: nothing sounds) when the person leaves the page, switches tab, the page navigates, the app leaves the front, or the box scrolls fully out of the view; paused when only a part is out of view and the finger scrolls.

### Pieces
1. `PlayerFactory` (new, shared): builds the ExoPlayer exactly as `NativePlayerActivity.buildPlayer` does (HLS or progressive source, headers, hardware decoders first, load control, size cap). The activity uses it too.
2. `InlinePlayer` (new view group, in BrowserActivity's `content` above GeckoView): SurfaceView (media overlay) + a small Canvas control layer (centre play / pause, time + seek bar, mute, full-screen, close x). Letterboxes the picture inside the page's box.
3. `VideoFlow.start`: if the question came from the top document with a usable box, `a.showInline(tab, decision, box)`; else the existing `NativePlayerActivity.launch`. The full-screen button of the inline player launches the activity at the current position and closes the inline one (the same film, no restart).
4. The page's element stays closed and paused under the overlay (as today). Back closes the inline player first.
5. Setting: Settings > Web videos play in gets "Inside the page" as the default way (full screen player = "Full screen"); the three choices (whenever it can / only in full screen / page's own) stay.

### What is proven where (fill in as it happens)
- core: box mapping rules and the stop / pause rules as pure functions (core/media/InlineBox) with tests.
- lab (emulator): overlay rect equals the page's element rect at rest and after a scroll of a known size (Health line with both), full-screen button hands the same position to the activity, scroll keeps the page moving, Back closes the overlay first.
- NOT provable on the emulator: how smooth the overlay follows a fast fling on the tablet; the tablet's hardware decoder path with two surfaces.
