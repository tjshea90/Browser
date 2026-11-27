# Compat lab run 1 (GitHub run 43784927532, code of alpha55, 2026-11-16 01:13 to 03:01 UTC)

What was run: `android.yml app=lab lab_suite=compat` (every site twice, Shields on and off, then the soak), emulator Android 10 with 2 cores and a 576 MB heap limit (a good boot), runner link unshaped.

## Result in one paragraph

Inconclusive for compatibility. 53 scenes in 105 minutes: 5 ok, 4 crash, 44 timeout, 29 issues (7 high). The emulated phone's own system (system_server) died 3 times and the emulator was started again from a clean disk 3 times: the 2-core runner was overloaded. Only 4 of 26 sites loaded at all (wikipedia-article, google-search, bing-search, duckduckgo: identical with Shields on and off, no script-error difference, no broken pictures). 22 sites timed out or crashed with Shields on AND off (the runner's own probe got http 403 / 402 for apnews, usatoday, politico, axios: data-centre addresses are blocked; the rest timed out after the first Yahoo scenes had overloaded the emulator). No site was found worse with Shields on ("Suspects: none"), but 22 of 26 sites are simply unmeasured.

## What is useful anyway

1. The heavy Yahoo Finance quote page on the EMULATOR showed what the tablet shows after a turn: `yahoo-finance-ttwo: the UI thread was blocked 1 time(s), worst 2051 ms`, and 28 s into the page an ANR (`Input dispatching timed out ... Wait queue head age 20240 ms`, logged at +110 s of the run): the app's UI thread does not process input for many seconds on a heavy page. The same family as the tablet's `ui slow 887 ms` / `ui late-beat 2021 ms` at a turn: the UI thread waits on the engine's synchronous handshakes (research/VIDEO_ACCEL_DEVICE.md: SurfaceView.surfaceChanged -> GeckoSession.onSurfaceChanged -> sync IPC ResumeAndResize). alpha56 writes `gecko layout WxH took N ms` and the FrameMetrics of the frames around a turn, so the next report names the phase.
2. Cold start on the emulator: `ui slow 928 ms`, `ui late-beat 1370 ms` at +1.3 to +1.9 s: the same ~1 s block at the start as on the tablet (865 / 1012 ms): the engine runtime's creation.
3. `Real video: 119 looks at 3 site(s), 14 min watched, 22801 frames, dropped 28.9 %`: software decoding on the emulator (no MediaTek decoders), not a statement about the tablet.
4. App memory stayed between 227 and 380 MB PSS over 105 minutes (no leak seen), lowest MemAvailable 1660 MB.
5. Blocker numbers on the Yahoo page: 991 requests judged in the engine (average 437 us, slowest 71 ms), 70 asked the app one by one (each held 263 ms on average): the one-by-one asks are the slow ones.

## Lessons for the lab (LAB2 / PD1)

- The compat suite is too heavy for a 2-core runner: 26 sites x 2 modes x up to 90 s. After the first ANR everything times out. Next design: at most about 8 sites per run, the heaviest page (Yahoo) LAST, restart the emulator after any ANR or system restart BEFORE the next scene (the lab restarted it but later scenes still failed: look at the order and the wait after a restart), skip sites whose runner probe says 403 / 402 (they are not measurable from a data centre), and keep Shields-off runs only for sites that loaded.
- A second kind of measurement is needed for compatibility: the owner's tablet itself (Diagnostics 'Page report' lines list blocked requests, failed loads and script errors per page).

TASKS: LAB2e stays open with this note; a smaller second compat run is LAB2f.
