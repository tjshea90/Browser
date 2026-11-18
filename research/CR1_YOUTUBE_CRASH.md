# CR1: "The app crashed when i went to youtube" (owner message 2026-11-18T01:05Z)

Owner, verbatim: "The app crashed when i went to youtube, but i think it is working now. i'm just concerned that it crashed in the first place. Investigate."
Sent about 2.5 hours after the alpha68 Release (2026-11-17T22:25Z), so the tablet most likely ran alpha68 (gecko code 169). No Diagnostics paste, no time, no screenshot, no crash text came with it.

## What is known (facts, not guesses)

1. **Nothing from the tablet about this crash exists in the repo or in INBOX/OUTBOX.** The last Diagnostics the owner sent is from 2026-11-16 (alpha58); it shows no crash lines.
2. **"The app crashed" can mean three different things on this tablet**, and each leaves a different trace (Android 10 has no `ApplicationExitInfo`, that API starts at Android 11):

   | What happened | What the owner sees | What is left on the tablet |
   | :- | :- | :- |
   | the engine's PAGE process (`:sandboxed_tab`) died (crash or killed for memory) | the page goes away and "The page stopped - bringing it back" / "Page freed to save memory - bringing it back", then the page loads again | Health lines `gecko onCrash` / `onKill` (RAM only: gone at the next start); for a crash also an engine report (`.extra`) in Diagnostics "Engine crash reports on disk" |
   | an uncaught Java/Kotlin exception in the app process | the whole app closes, Android may show "Kestrel keeps stopping" | `files/last-crash.txt`, shown in Diagnostics as "EARLIER CRASH of this app" (kept 7 days) |
   | the app's MAIN process vanished with no Java exception: Android's low-memory killer, a native crash in the main process that left no report, Force stop | the whole app closes | **nothing** before CR1c (this was the visibility gap) |

3. The first row recovers by itself (AX2/Q2a, emulator-proven: kill -9 of the content process brings the page back at the same scroll place). If the owner saw the page come back by itself, that was this row: a content-process death, not the app closing.
4. Memory on this tablet (owner's Diagnostics 2026-11-16): total 1954 MB, MemAvailable 684-716 MB with ONE finance page open, 6 app processes (main, utility, gpu, one sandboxed tab, crashhelper, media), app heap 24/128 MB. YouTube's watch page plus a decoder is far heavier than a finance page; ~650 MB free is the whole safety margin of a 2 GB Go device.
5. Real YouTube pages (`m.youtube.com` home, results, two watch pages) were in the old `soak` pools, but the `daily` suite (the one run on every build since alpha62) never opens YouTube. The last build-keyed lab (alpha64, run 44077888907) had 0 crashes, but it did not visit YouTube. No lab on alpha65-alpha68 visited it either.
6. What changed between the last "YouTube works well" (owner message 60, alpha60) and alpha68: alpha61-62 (gestures, preroll hand-over), alpha64 (engine libs stored uncompressed in the APK, a start-up profile, ONE message listener in the extension, engine prefs v10 without login/form-history machinery), alpha65-67 (the updater hands a profile file to the installer; the whole app is compiled at install), alpha66-68 (Swoop keyboard link: SwoopLink, HandOverProvider, address box `privateImeOptions`). Nothing in these touches a YouTube page directly; the alpha64 engine change and the install-time compile are the two with the widest reach.

## Code read for CR1b (nothing found that explains a crash by itself)

- `ShieldsBridge.onMessage` (every message the page-side scripts send, including the YouTube ad counters `yts`): the whole dispatch is inside `try { } catch (Exception)` and a separate `catch (OutOfMemoryError)`; hand-over runs through a second guarded call. A bad message cannot end the app.
- `SwoopLink` (alpha66-68): every call is in `try/catch`; `HandOverProvider` is not exported and only serves a file in `cache/handover`.
- `BrowserActivity.sessionGone` (engine process death): recovers the first death with state, reloads the URL on the second within 15 s, shows the error screen on the third; a recovery that throws would be an uncaught exception, which CrashNote writes down.
- Nothing in the app keeps a big heap: 24/128 MB on the tablet.

## What this session built so that the cause is findable (CR1c)

`app/diag/LastRun.kt`: a small note (about 2 KB, `files/last-run.txt`) written only when something notable happens (the screen comes or goes, a page finished loading, Android sent a memory trim, the engine's page process died), never on a timer; the next start reads it and keeps it 7 days if the run ended in front of the person. Diagnostics line **"Last run ..."** says:

- ended on purpose / in the background (Android takes back background apps: ordinary) / **while on screen (not ordinary)**;
- and for the third case the cause by elimination: an uncaught Java exception (the crash note), the tablet itself restarting, an engine crash report written during the run, or **none of these = the process was ended from outside (memory killer / Force stop)**, with the last free-memory figure and the last 12 events (pages visited, memory trims, engine deaths).

Tests: `LastRunTest` (7), `OwnerRulesTest.cr1TheLastRunNoteIsWiredIntoTheRealStartAndEnd`.

## Lab: can YouTube be made to die on the emulator? (suite `yt`, run 44214894368)

`tools/emu/soak.py yt_journey`: tablet-shaped emulator (2 GB, low-RAM flag, font scale 1.2), real `m.youtube.com` home / two result pages / `www.youtube.com` desktop home, four watch pages (a tap to play, 40-60 s watched, scrolled under the playing film, app left with Home for 20 s, returned, Back); every second round with 150-450 MB of the emulated RAM taken by a file in the RAM-backed /dev. Result: see the section below (filled when the run is read).

## What to ask the owner (one thing)

Open Menu > Diagnostics right after it happens (or now: the crash note is kept 7 days) and send the text. The lines that answer it: `EARLIER CRASH of this app`, `Engine crash reports on disk`, and (from the next release) `Last run ...`.
