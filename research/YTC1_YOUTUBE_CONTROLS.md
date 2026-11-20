# YTC1: "The video controls inside YouTube don't work right" (owner message, 2026-11-20, during alpha76)

Owner, verbatim: "The video controls inside YouTube don't work right. First, when i tap somewhere on the video, the player controls pop up very fast then disappear so i can't use them. The playback seems laggy and the buffering takes a long time. I'm not sure if this is only YouTube or all videos"

STATUS: NOT REPRODUCED, cause NOT found. This file says what was ruled out, what could not be tried, and what the next paste must show. Nothing here ran on the tablet.

## What could be tried here (new this round)
- `m.youtube.com/watch?v=...` DOES load in the sandbox's headless Chromium (HTTP 200, the real player DOM: `div.html5-video-player ytp-hide-controls ytp-mweb-player`, touch emulation, our scripts can run in it). The earlier notes (seekbar.js, SEEKBAR.md) said "a real YouTube page cannot be had from the sandbox": the PAGE can, the FILM cannot: YouTube shows "Sign in to confirm you're not a bot", the player stays in `unstarted-mode` and the video box is 0x0, so YouTube never draws its controls and a tap cannot be replayed against them.
- The GitHub emulator lab (`yt` suite, tools/emu/soak.py `yt_journey`) did watch real YouTube films before (CR1), so a lab step that taps a playing film and samples the controls is possible; it was NOT built this session (a run is 30+ minutes of Actions time, the emulator has a fast link and a software decoder, so it would not show a timing problem of the tablet's CPU).

## Which controls? (the answer decides where to look)
1. YouTube's OWN controls in the normal page (the film not full screen): our scripts that react to a tap there are `videobar.js` (draws its own bar only when the page has none; on YouTube the page's chrome counts as "own" and nothing is drawn), `seekbar.js` (only a tap that lands ON the progress bar), `gate-main.js` (press-to-play), `youtube.js` (ad skipper, observer on the player's class). None of them hides YouTube's chrome. Read, not proven.
2. Kestrel's OWN full-screen controls (player.js + app PlayerController / ControlsMachine), which replace YouTube's chrome in full screen.

## Read and found sound (so nobody re-reads it)
- `core/player/ControlsMachine`: controls shown by a tap hide by themselves only while the film PLAYS and is not buffering / pinned / ended (`autoHide()`), a stall or a pause keeps them up, a state change restarts the full clock, they are edge-triggered. A laggy film therefore cannot make them vanish by itself.
- A tap is counted from two sources, the glass (app: `PlayerController.appTap`) and the page (`vtap` with the finger-lift time `ts`); `matchesAppTap` drops the page's copy when the two lie within 450 ms. A copy that does NOT match would be counted as a second tap: two taps within 300 ms are a double tap (seek / play-pause), later ones toggle again (show then hide). Whether the engine's timestamps (fingerprinting protection reduces timer precision) ever differ by more than 450 ms from the app's is NOT known; the counter "the page's own copies dropped N" in Diagnostics ("Full-screen video controls" line) is the evidence.
- Codec policy (Settings > Video, AUTO): a codec with no hardware decoder is not offered, so YouTube is given H.264 (hardware), not VP9 / AV1 software decoding. The extension's per-request listeners cost 2-5 us per request (research/REVIEW_2026-11-23_RVW5.md). Neither explains lag by itself.

## Lag and long buffering: what is known
- The hotspot is carrier-capped at 5 Mbit/s and flaky (CLAUDE.md); YouTube's adaptive player picks its own rung and buffer. Kestrel does not play YouTube itself (refused plainly, research/VIDEO_DOWNLOADER.md section 8), so what the app can change is only the cap on the picture size (`quality.js`, Settings) and what the engine offers.
- If it is every video, the CPU / network of the tablet is the common cause; if only YouTube, YouTube's own player is the difference. NOT known without the tablet.

## Added for the next paste (alpha77)
- `ControlsMachine.lifeLine()` in the Diagnostics line "Full-screen video controls (Kestrel's own)": "controls shown N times, M of them hid again within 1.5 s (last: <tap|clock|put-back> after <ms>)". A tap that hides them 300-400 ms after a tap that showed them is a duplicate tap; "clock" would be a timer bug; "put-back" a page button; none of the above with M = 0 means the controls that vanish are YouTube's own, in the page. Test: ControlsMachineTest `controlsThatGoAwayWithinASecondAndAHalfAreCountedWithTheReason`.

## What the owner can tell us (two questions, the answers narrow it to one file)
1. Is the film full screen when the controls vanish, or is it still in the page (a small player above the comments)?
2. After it happens, copy Diagnostics (menu > Diagnostics) and send the lines "Full-screen video controls", "Tap on a video bar" and "Video bar".
