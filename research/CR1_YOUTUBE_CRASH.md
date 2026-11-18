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

## CR1e: the Diagnostics the owner pasted (2026-11-18 01:00:31 UTC, alpha68, app running 24 s)

Taken 24 s after a COLD start, i.e. the first run after the crash. What it says and does not say:

- **No `EARLIER CRASH of this app` line and no `Engine crash reports on disk` line.** CrashNote catches every uncaught Java/Kotlin exception on any thread of the main process (including OutOfMemoryError), and the engine's crash helper writes a report for a native crash. The previous run left neither. So it was NOT a Java crash and NOT a native crash the engine could report. What is left: the process was ended from outside (Android's low-memory killer, Force stop, the tablet restarting) or died so hard that nothing could write.
- **`tabs state-mismatch tab=3` / `tabstate.mismatch=1`**: the tab list (session.txt, written 600 ms after a page change) and the engine's own saved state of that tab (written separately) disagreed about the page. The code comment says "a cut-off can leave them apart": the previous run ended abruptly between two writes, not through a normal close. This supports "ended abruptly", not a cause.
- The early cover on the first frame was `www.google.com` (the last saved address): the run ended before YouTube was saved as the tab's address, i.e. soon after the page change (the owner's words: "when I went to YouTube").
- No `gecko.kill` / `gecko.crash` counter: those count THIS run only (the new process), so they say nothing about the last one.
- Memory right after the start: MemAvailable 646-709 MB, low=false, threshold 144 MB, app heap 16/128 MB. The tablet was not short of memory at that moment; YouTube had not loaded yet.
- **`Cold start ... activity 2591 ms`** is new: every earlier paste has 223 / 293 / 300 ms for the same number (time from process start until the activity was created); `frame 2719`, `runtime 3004` follow it, while `open 2970` and `paint 6310` are much better than before (paint 15-19 s in alpha58). So the 2.3 s sit BEFORE the activity exists (process start, class loading of the activity, Application work). Two readings: (1) a one-off, the system was busy reviving the app right after it had been taken away (memory pressure on a 2 GB Go device: the same cause as the crash); (2) a cost of alpha64-68 (uncompressed libraries, whole-app compile at install, `start oat` line would show what was compiled: it is not in this paste yet, the line comes later in the run). Not decidable from one paste: the next Diagnostics (a normal start, not one right after a crash) answers it. If `activity` is still above 1 s on a normal start, that is a regression to chase.
- Keyboard is Swoop (`app.swoop.keyboard/.KeysService`): the alpha68 typing link was in use. The crash note would have caught a failure in it (it is all Java); it did not.

**Conclusion so far (honest):** the evidence the app keeps points at "the process was ended without a trace", most likely Android's memory killer on the 2 GB tablet while the YouTube page was loading, but this is a conclusion by elimination, not a proof. `LastRun` (CR1c) turns the next occurrence into a proof: it records free memory and the last events, and says "ended while on screen, no crash note, no engine report".

## What this session built so that the cause is findable (CR1c)

`app/diag/LastRun.kt`: a small note (about 2 KB, `files/last-run.txt`) written only when something notable happens (the screen comes or goes, a page finished loading, Android sent a memory trim, the engine's page process died), never on a timer; the next start reads it and keeps it 7 days if the run ended in front of the person. Diagnostics line **"Last run ..."** says:

- ended on purpose / in the background (Android takes back background apps: ordinary) / **while on screen (not ordinary)**;
- and for the third case the cause by elimination: an uncaught Java exception (the crash note), the tablet itself restarting, an engine crash report written during the run, or **none of these = the process was ended from outside (memory killer / Force stop)**, with the last free-memory figure and the last 12 events (pages visited, memory trims, engine deaths).

Tests: `LastRunTest` (7), `OwnerRulesTest.cr1TheLastRunNoteIsWiredIntoTheRealStartAndEnd`.

## Lab: can YouTube be made to die on the emulator? (suite `yt`, run 44214894368)

`tools/emu/soak.py yt_journey`: tablet-shaped emulator (2 GB, low-RAM flag, font scale 1.2), real `m.youtube.com` home / two result pages / `www.youtube.com` desktop home, four watch pages (a tap to play, 40-60 s watched, scrolled under the playing film, app left with Home for 20 s, returned, Back); every second round with 150-450 MB of the emulated RAM taken by a file in the RAM-backed /dev. **Result (run 44214894368, commit f346b9f1 = alpha68 code + the new suite, read 2026-11-18T02:25Z): INCONCLUSIVE, and honest about why.** 11 scenes in 34 minutes: the emulated Android's own system died TWICE (system_server killed by its watchdog, then the emulator was started again from a clean disk: the known software-GL limit, research/EMU_LAB.md item 9, same as the Keyboard simulation's pypi.org and 1080p runs), no YouTube page reached "page loaded" within 90 s (yt-home: an ANR with the UI thread stuck in `HardwareRenderer.nSyncAndDrawFrame`, i.e. the emulator's software renderer, 2.3 s), and the low-RAM flag could not be set (`lowram-attempt.txt`: read back empty). The "RAM pressure" rounds were never reached. What it does say: **no app restart, no Java crash, no engine page-process death, 3 memory trims (level 20 = the app left, ordinary), app PSS 190-197 MB, MemAvailable 542-701 MB** while the three leave-and-return steps passed. It does NOT say YouTube is safe on the tablet: the emulator could not load the pages at all. Another run of the same shape would repeat the emulator's limit, so none was started; the proof for the owner's case has to come from the tablet's own `Last run` line.

## What to ask the owner (one thing)

Open Menu > Diagnostics right after it happens (or now: the crash note is kept 7 days) and send the text. The lines that answer it: `EARLIER CRASH of this app`, `Engine crash reports on disk`, and (from the next release) `Last run ...`.
