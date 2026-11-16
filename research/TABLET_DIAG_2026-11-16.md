# The tablet's Diagnostics of alpha55 (owner message 51, 2026-11-16 02:10 UTC): what the numbers say

Source: the owner pasted the whole Diagnostics screen of the real tablet (alps 711, Android 10, 2 GB, app heap 18 of 128 MB) with no words. The paste is verbatim in INBOX.md (line "Kestrel diagnostics 2026-11-16 02:10:52 UTC").
The app had run 160 s. One page: a finance.yahoo.com quote page with a Boltdns HLS film (89.5 s long), watched in ONE full screen (59 s to 132 s of the run; the controls session started four times in it, three of them restarts). Nothing below is a guess: every number is a line of that paste.
This is the FIRST real-tablet measurement of the new full-screen player (VP1) and of a manual turn under a video.

## What works (on the real tablet)

| Line | Number | Meaning |
| :- | :- | :- |
| Full-screen video controls | 4 full screens, 7 single taps, 1 command to the page, 13 reports in the last session | the controls started 4 times in the ONE real full screen of the run (the 'Full-screen turning' line says 1 full screens: one start and three restarts after the watchdog, see reading 3); a tap shows or hides them (6 taps in the first full screen: shown, hidden, shown, hidden, shown, hidden), the pause button reached the page |
| the page's own controls put away | 3 elements | Yahoo's own player shell is hidden while Kestrel's controls own the film (VP10 confirmed in Firefox on the tablet) |
| Full-screen page size | 5 checks, 0 wrong (page 600x947 = view 600x947) | the film's page always had the view's size when it reported |
| System bars | kept | the controls are never under the navigation bar in the default mode |
| Turning, the page's own pause | 2 turns, videos played again 2, left paused 0 | pause and resume at a turn work (the page guard `turn.js`) |

## Timeline of the two manual turns (the owner turned the tablet by hand; seconds since the app started)

| Time | Event |
| :- | :- |
| 59.09 | full screen starts: `fullscreen turn ... (video 0x0, screen portrait, setting 0)`: the picture size was UNKNOWN, so Automatic turning did nothing |
| 61.37 | player.js first report: film 1920x1080, length 89.52, media source (2.3 s after the full screen began) |
| 65.7-73.9 | six taps; each shows or hides the controls 300 ms later (the double-tap window) |
| 76.50 | turn to landscape: view 1024x523 at 76.76; `ui slow 887 ms` 77.3; `ui late-beat 2021 ms` 78.48; memory trims level 5 and 10 at 78.4-78.9 (avail 443 MB) |
| 79.30 | `turn-nudge`: the page has not taken the new size 2538 ms after the last size change (the page had said NOTHING since the turn) |
| 79.58 | `turn-settled after 3113 ms`; the same instant `player stop: the page stopped reporting after 18 s` (the 3.5 s watchdog) |
| 80.06 | the page's FIRST report after the turn (3.56 s after the turn began); controls pop up, session restarts |
| 85.08 | `player stop` again: only 2 reports in 5 s |
| 94.99 | the page's next report: it was silent for 14 s; `videobar turn played` (the page's resume of the film) at 95.42 = 19 s after the turn began |
| 118.84 | turn back to portrait: `ui slow 898 ms`, `late-beat 933 ms`; nudge at 121.51; settled after 3086 ms at 121.89; the page's first report at 126.68 (7.8 s after the turn began) and this time the film was 640x360 (the stream stepped down from 1080p) |

## Reading

1. The new controls are right; a manual turn on this page is slow for reasons that are mostly NOT the controls: the app's UI thread is blocked 0.9-2 s right after the view changes size (`ui slow`, `ui late-beat`), which is the engine's surface / compositor resize (the same kind of 0.9-1.0 s stalls show at the first start, `+1.4 s ui slow 865 ms`, `+1.7 s ui late-beat 1012 ms`), and the page's own script thread goes quiet for 3.5 to 14 s after a turn (while the 1080p stream plays and Android trims memory).
2. Turn statistics LOST this: the Diagnostics line says "the page took 0 ms ... 0 turns measured, 0 gave up" because the page said nothing for 3 s, the old rule then ended the wait (`TurnSettle.SILENT_MS`) and nothing counted the page's late report. Needed: count silent turns and measure the late first report.
3. The 3.5 s player watchdog (`PlayerController.SILENT_END_MS`) ended the session three times in 160 s (four sessions in the one full screen); every restart shows the controls again (the pop-ups at 80.0, 94.99, 121.89, 126.68). A page that is only busy must not end the session.
4. Automatic turning never fired: the engine side did not know the film's picture (no vfind info: the film sits in a shadow root), so `FullscreenTurner.pictureKnown()` was never called. player.js knew the size 2.3 s into the full screen (1920x1080). The owner turned the tablet by hand twice and each turn cost 3.1 s of black cover.
5. Nothing says whether the film's frames were dropped: `Real video: nothing played yet this run` (that line is fed only by videos the old finder sees). The controls' own page script can read `getVideoPlaybackQuality()` of the film it controls.
6. `CPU by process` only ever sees the parent process on this tablet (isolated processes are invisible to apps on Android 10): parent average 39 %, peak 215 % while the page loaded. The page's own load can be measured from inside the page: how late its own 1 s timer fires (`lt`) and how long its own work per report takes (`pc`).
7. A tap shows or hides the controls 300 ms after the tap: that is the double-tap window of the research (YouTube does the same); it is not a fault. The counter label "0 taps on the picture" next to "7 single" is a label mix-up (taps that reached the page vs taps the app handled): fix the label.

8. `tabs look-saved ... why=settled` appears TWICE while the film was full screen (96.4 s and 131.4 s): the page's "last look" (the picture shown at the next start) was read back from the GPU and encoded during playback. That is a wrong picture (the film's frame, not the page) and a CPU / GPU burst on the tablet at exactly the wrong moment. Fixed (ANS51h): `captureLook` returns at once while a page is full screen.

## Decisions (what is changed, what deliberately is not)

- Changed (ANS51b-f and ANS51h in TASKS.md): film size from the first report into automatic turning; watchdog 3.5 -> 12 s; silent-turn and late-report statistics; page stall / own-cost / dropped-frame numbers; log lines of how long the engine's part and the app's own part of a turn take; no page-look read-back during full screen.
- NOT changed: `SILENT_MS` (3 s) and `NUDGE_AFTER_MS` (2.5 s). The data cannot say whether lifting the cover earlier or later is better (5 checks, 0 wrong after the cover lifted, but a wrong size before the page's first report would not show in a check); the new statistics decide it next time.
- NOT provable without the tablet: how smooth the film itself is (dropped frames), what stalls the page for 14 s (software decode of 1080p VP9? the page's ad scripts? memory trims), whether a light page behaves better. The owner is asked to repeat the test on a light video page and send the same Diagnostics lines.
