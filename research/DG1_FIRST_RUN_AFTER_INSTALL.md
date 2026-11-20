# DG1 - the first run after an install ends within seconds (owner Diagnostics 2026-11-19T05:57Z, alpha71)

Owner pasted Diagnostics with no words. Read it, wrote what it shows, changed what could be changed without the tablet.

## What the paste shows (facts, with times in UTC)

| Run | Build | Started | Last note | Lived | Ended | Evidence for "why" |
| :- | :- | :- | :- | :- | :- | :- |
| A | alpha70 (171) | 2026-11-18 07:12:14 | 07:12:18 | about 4 s | on screen, no crash note, no engine report | Release alpha70 is stamped 07:11Z: this is the FIRST start after the install |
| B | alpha70 (171) | 2026-11-19 02:23:53 | 02:51:28 | 27 min | on screen | alpha71 started 7 s later: the UPDATE ended it (a package update ends the running process) |
| C | alpha71 (172) | 2026-11-19 02:51:35 | 02:51:44 | about 9 s | on screen, no crash note, no engine report | Release alpha71 02:34Z: the FIRST start after the install |

So the three "ended while on screen" runs are not three crashes: B is the update itself, A and C are the first start after an install, each dead within seconds.
Everything else in the paste (current run: 2892 s, no crash) says the app is stable once it has been running for a while.

Not memory: `avail` 959-1146 MB at the last notes, threshold 144 MB, `low=false`. The app (main process) held 220-300 MB, normal for the Gecko parent process.
Not YouTube / doraby / a page: run A died before any page loaded (last event `start runtime cold 2861 ms`), run C was only loading mega.nz (early cover, warm start).
Not our own exit: `ProcessLife.exitIfIdle` calls `LastRun.markExit()` first and would read `state=exit`; both runs read `state=fg`.
Not proven an ANR: run C's `start paint cold 8343 ms` line was written by the screen thread at +7.96 s, so the screen thread was alive then; an ANR needs 5 s of a stuck screen thread plus a touch, so the earliest it could have shown is about +13 s. The old note could not tell (it sampled every 15 s: the process died somewhere between +9.4 s and +24 s).

## What the old note could not say (and is fixed in DG1a/DG1b, `app/diag/LastRun.kt`)

1. "ran about 0 min" hid a 4-9 s life. It prints seconds under 2 minutes now.
2. An update looked like a crash. The note now carries the NEXT start's build and the time that build was installed (`PackageInfo.lastUpdateTime`): a run that was on screen, whose last note is within 120 s before / 5 s after the install time of a DIFFERENT build, and that has no crash note / engine report / STALL, is reported as "ended by an UPDATE" and is NOT kept as an abnormal end (the three keep-slots are no longer used up by updates).
3. A first start after an install was not marked. The note now carries its own install time; a run that began within 10 minutes of it prints "FIRST START after its build was installed (N s after the install)", and when such a run ended on screen within 2 minutes with no crash note, no engine report and no stall, the cause line says to look at the install itself (compile at install, the installer, the engine's first start on an uncompiled app) instead of blaming a page or memory.
4. The first minute is sampled every 2 s (was 15 s), by the same watchdog thread, no main-thread work: the "last note" is then within 2 s of the end.
5. `start oat` and `start art` lines are kept in the note (DG1b): the next report says whether the app was compiled in the run that died.

## Hypotheses for what ends the first run (ranked; none proven; the sandbox has no device)

1. The system is busy right after an install (dexopt of the 96 MB APK with the 130 MB of unpacked libraries on a 2 GB MT8168B, the package installer, `installd`) and Android Go's low-RAM killer / a vendor policy ends the new foreground process. Fits: both deaths within 4-9 s of the start, `start paint cold 8343 ms` (8 s to first paint, normal is 4.6 s), 100 MB of growth in the main process between two notes (200 -> 300 MB).
2. The process is killed once more by the installer's own bookkeeping because the install is retried: `InstallResultReceiver` re-installs WITHOUT the start-up profile when the installer refuses the dex-metadata file (`UpdateManager.installRetryWithoutDm`). A second commit of the same package would end the new run exactly like an update. The next note will show it: two `FIRST START` lines close together, or an "ended by an UPDATE" line whose next build is the SAME code. (Check `UpdateManager` for whether the retry can run after the person already opened the app.)
3. The owner opens the app from the installer's "Open" button while the post-install work is still running and swipes it away / the launcher restarts it: would show `life pause` before the end; run A and C show none.

## What would settle it

- After the next update: open Kestrel, wait a minute, send Diagnostics. The new note says "ran N s", whether it was the first start after the install, the `start oat` size (about 12 MB base.odex = whole app compiled at install; "none" = not compiled) and `start art`.
- If the app vanishes again within a minute of an install: reopen it and send Diagnostics at once (the first report line now says first start + seconds).
- Do NOT uninstall first; the data of the run before the update is the evidence.

## Not done / not proven

Nothing here was run on the tablet. The JVM tests (Robolectric) prove the classification and the sampling rate, not what Android does after an install.

## The rest of the paste (DG1d)

- **Cut line (fixed):** `Videos start only when pressed ... (a server or pla` ended mid-word because `ShieldsBridge.EngineStats` clipped the page-gate text to 300 characters; the full text is about 340 once a player frame was started by a tap on the page around it. That trailing number is the evidence for the "tap on the server button leads to the frame" rule (EM1 / vidfast). Clip is 600 now.
- **Health ring (fixed):** `pull down ...` was logged for every touch-down: 130 of the last 400 events. One line per 8 s now; `Health.counter("pull.down")` stays exact.
- **Tab previews 0 of 3 (by design):** 9 times `tabs thumb-dropped moved=false loading=true`: the picture taken while a tab leaves a page is kept for the Back gesture (`BackPictures`) and thrown away as a tab preview, because the tab already started loading the next page (a Google result's picture would label the YouTube page). A tab that is always left by navigating gets its preview when the tab grid opens. The WRONG-page count is 0 as required.
- **Jank burst:** `frames 10s frames=191 jank=100 worst=679ms` then `368 / 102 / 115 ms`, right after `gecko size 600x418 (was 600x824)` -> `600x824` and a Google AI Mode page (54 blocked requests, 16.7 s to finish) streaming into a window that had just been resized; CPU `RenderThread 59 %`. A one-off; the other 10-s windows in the paste are 1-10 jank frames. No change.
- **`ui slow 800ms HardwareRenderer.nSetStopped`** at the resume after 47 s in the background: inside Android's renderer restart, not our code (one occurrence, `ui.late=1`). No change.
- **Page height 418 for 25 s after a resume:** nothing in the paste says why (no `win typing-mode` line). If the soft keyboard came up by itself on returning to the app, that is a real bug to chase; asked the owner.
- **Everything else is healthy:** heap 16/128 MB, MemAvailable 712 MB, storage 1.12 GB free (an update needs 887 MB: fits), blocker 970 us average per request, 0 errors, YouTube/video lines clean, current run 2892 s with no crash.
