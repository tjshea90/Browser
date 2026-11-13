# COLD_START - where the time goes between the tap on the icon and the page on the screen (EY-2, task AN1)

Source of the work: `research/REVIEW-EXTERNAL-2.md` section 3.4 (EY-2 "do this FIRST": every later row of Part A (EY-1 last look, EY-3 first-frame diet, EY-4 engine prefetch, EY-5 stay ready, EY-6 cache-first load) is judged by these numbers, not by feel).

## What was built (alpha32)

`app/src/main/kotlin/app/kestrel/browser/diag/StartTimeline.kt`: eight marks, each taken once per run:

| mark | taken where | meaning |
| :- | :- | :- |
| app | `GeckoApp.onCreate` (UI process only) | the process is up |
| activity | `BrowserActivity.onCreate` | the browser's screen was created |
| frame | first frame after that | something is on the screen |
| look | (EY-1, not built yet) | the saved picture of the last page was drawn |
| runtime | engine reports it is up | the engine is ready |
| open | the current tab's session was opened | the page's session exists |
| paint | `firstPaint(tab)` for the current tab | the page's first paint |
| stop | the current tab's page load ended | the page is loaded |

A COLD start (first activity of the process) counts milliseconds since the process started (`Process.getStartElapsedRealtime`), a WARM start (a later activity in the same process) since that activity was created. Every mark is also one `start` event in the Health list (`adb logcat -s KestrelHealth` in debug builds), and **Menu > Diagnostics > Copy** carries one line:

`Cold start (ms since the process started): app 1434 | activity 4642 | frame 11915 | look - | runtime 23161 | open 21863 | paint 63412 | stop 48691`

## Emulator reading (2026-11-13, AVD kestrel29x64, x86_64, no KVM, software GL: NOT representative of the tablet)

The line above is that reading. It only proves the marks fire in the right places and the line reaches the report. The order of runtime / open / paint / stop on the emulator is not what the tablet will show (its compositor lags behind the page load).

## What the tablet must say (the owner's smallest step)

Fresh start of the browser after a restart or after swiping it away (so it is a COLD start), wait until the page is on the screen, then Menu > Diagnostics > Copy and paste the report. The `Cold start` line is what is needed. Five cold starts give a usable middle value.

| Date | Build | Device | app | activity | frame | look | runtime | open | paint | stop | Note |
| :- | :- | :- | -: | -: | -: | -: | -: | -: | -: | -: | :- |
| 2026-11-13 | alpha32 debug | emulator | 1434 | 4642 | 11915 | - | 23161 | 21863 | 63412 | 48691 | not representative |
| (tablet) | | MT8168B | | | | | | | | | waiting for the owner's paste |

Rule for later rows: a change that is meant to make the last page appear sooner (EY-1, EY-3, EY-4, EY-5) is kept only if the `frame`, `look` or `paint` mark moves the right way on the tablet.
