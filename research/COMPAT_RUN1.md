# COMPAT1: the first compatibility run (lab run 43747605584, 2026-11-15, commit d288355b) - NOT usable beyond the first four sites

Owner message 45: "I want Kestrel to be a polished daily browser with web page compatibility for all sites and good speed. Run github runners with emulators matching my hardware setup, but keep in mind that most of the time I have fast wifi on ..."
Suite `compat`: about a hundred well-known sites, each loaded with Shields ON and again with Shields OFF for that site, from a cold cache, on the runner's own fast link (no shaping), Android 10 emulator 600 x 1024 at 170 dpi, 2 GB, 2 cores, software GL. Report tables: speed, pages that did not load correctly, Shields on / off verdicts, scrolling, memory, script errors.

## What came back
- 51 scenes in 74 minutes: **ok 5, timeout 39, crash 7**. Four sites loaded and were measured (google-search 0.5 s, bing-search 1.4 s, wikipedia-article 2.9 s, duckduckgo 3.1 s to finish; first paint 1.1-1.5 s). From the first Yahoo page on the emulator went under: the emulated phone's own system process (`system_server`) was killed three times (SIGKILL by the guest kernel / lmkd: `Zygote: Process ... exited due to signal 9`), the driver restarted the emulator from a clean disk each time, and every later load timed out at 50 s.
- App memory was NOT the cause: app PSS 210-300 MB (peak 458 MB on latimes), but the guest's MemAvailable fell to 166-500 MB: Android + the software GPU (SwiftShader) + 2 GB is too little for a hundred heavy pages in a row. The tablet has a real GPU; this is a limit of the lab.
- The 'java crash' and 'am_anr' issues are NOT crashes of Kestrel: `FATAL EXCEPTION` was `com.android.phone` dying with the system (`DeadSystemException`) and an `svc power stayon true` shell command without permission; the ANR on yahoo-finance-ttwo was the UI thread waiting in `HardwareRenderer.nSyncAndDrawFrame` (the software GPU drawing), 2042 ms, then input dispatching timed out. The same software-GL read-back problem as lab finding 11 in `EMU_LAB.md`.
- yahoo-finance-ttwo: 436 script errors, wikipedia-article: 237 (mostly `Cross-Origin Request Blocked`: what a content blocker produces on purpose when it refuses a cross-site request; not judged).
- Scrolling on the four pages (software GL, only for comparison): 619 frames, 6.0 % of the screen updates over 50 ms, 2.3 % over 100 ms.

## What was changed in the lab because of it (this commit)
1. `emu-lab.yml`: the emulator gets **4 GB** for suite `compat` (`EMU_RAM`); the other suites keep the tablet's 2 GB.
2. The driver closes every tab after each site (every VIEW intent opens a NEW tab: eight heavy pages in eight tabs), sets the debug switch `nolook` (Last look's picture read-back stalls the UI thread for seconds on software GL), and waits 90 s (was 50) for a page.
3. `crash_lines` counts a `FATAL EXCEPTION` only when the process is the app's.
4. Next compat run: `lab_minutes` 120.

## Not judged yet (needs the next run)
Shields false positives (the ON / OFF table), blank pages, broken pictures, script errors per site, scrolling per site, speed of the heavy sites.
