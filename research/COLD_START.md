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

## EY-1 - the last look (alpha33): the page you left, on screen in the first frame

What it does: the browser keeps a sharp picture of the page in front (at the full size of the page area) together with the scroll place. At the next start - the first frame, before the engine exists - that picture is drawn 1:1 from the top left, undimmed, with a thin static line along the top edge (the live page is loading) and, after 4 s without a first paint, a slim note at the bottom ("Saved view - still loading"). The live page replaces it when it has painted AND is where the picture was (or 1.5 s after its first paint at the latest). The picture cannot be scrolled; a tap on it only makes the line brighter.

Where it lives: `core/.../session/LastLook.kt` (pure rules: record, when a picture may be used, how it fits, when it goes: `LastLookTest`), `app/.../ui/LastLook.kt` (files in `filesDir/lastlook/<tabId>.webp` + `.json`, never the cache folder, never a private tab: `LastLookStoreTest`), `RestoreOverlay` look mode (`RestoreOverlayLookTest` draws it and checks pixels), `BrowserActivity` (capture and hide, `captureLook`, `lookFor`, `lookCheck`). Settings > Privacy & security > "Show the last page right away" (default on; off deletes the pictures).

When a picture is taken (a regular tab on a real web page, not loading, not typing, nothing laid over it): on leaving the page (first line of `onPause`), 700 ms after the last scroll (at most every 4 s), 1.5 s after a page finished loading. Only the page in front and the two most recently used tabs keep a picture (about 100-300 KB together). Wiped with: clear browsing data, the "clear on exit" start, closing the tab, the switch turned off.

When it is shown: only for the same page (address without #fragment), engine version, appearance (skin, Dark pages choice, night mode) and desktop-site choice. A picture of another screen shape (rotation) is scaled to the width when the shape is within 5 %, else today's plain "Opening <host>" cover. A hibernated or crashed tab that comes back uses it too (the note then carries the "bringing it back" message at once).

Emulator proof (2026-11-13, AVD kestrel29x64, no KVM, slow): scroll a long page to y=1366, press Home (picture taken at pause), kill the app, start it again: the first screen is the page exactly as left, the line at the top, the note after 4 s (`lk_3_a_t26.png`, not kept in the repo: it is the test page). Health events: `look-saved` after page stop, after each scroll and at pause; `look-shown`, `look-adopted`, `look-hide why=...`. Diagnostics shows "Last look ...: on, N stored, shown at start, taken, hidden".

Found on the way: GeckoView's UI-thread scroll callback does not report the place the engine restores a page to by itself, so a restored page stayed at "y=0" in the record; the compositor callback (`CompositorScrollDelegate`, source OTHER) is used instead.

What only the tablet can say: how long the picture takes to appear (the `look` mark in the Cold start line, next to `frame`), whether a pause capture finishes before the owner swipes the app away (it took about 5 s on the emulator), and whether the hand-over to the live page is invisible. Owner's smallest step: after alpha33, use the browser as usual for a few pages, swipe it away, open it again, and paste Menu > Diagnostics > Copy.

## AQ1 - left on the Home screen: the app comes back to Home (alpha33; owner 2026-11-13: "I pressed home ... closed and opened it again and it loaded doraby.com")

Cause: the Home button lays the Home screen OVER the tab's page (`Tab.showHome`, the page's session stays alive underneath; a tab on Home keeps its address for the tab switcher, AK3). The saved session (`session.txt`) had only the address, so after the process died the tab was restored as that page.

Fix: `TabRecord.homeOverPage`, saved in the existing flags field of the session line (`0` / `1` = desktop site, plus `H` when Home was over the page: still six fields, so older files load as "not on Home" and an older app still finds every tab; `SessionCodec`, tests in `ShieldAndSessionTest`). On restore a tab flagged Home gets `showHome = true` and NO engine session: `attachContent` makes none for a Home tab that has none, so nothing loads behind the Home screen at start (the address and the saved Back stack wait; Back, or typing an address, brings the page). Also skipped for a tab left on Home: the early cover and its last-look picture (`earlyCover`), the start-up connection warm-up of its site, the resume warm-up (`warmFrontOrigin`). The session file is written at once when Home is pressed or left (`scheduleSave`), not only when the app goes to the background.

Rules that stay: switching to a tab shows its PAGE, not Home (`show()` resets the flag of the tab it switches to); only the tab that was in front when the app was closed comes back on Home.
