# DIAG_TOOLS - which diagnosis tools the app still has, and which went (owner message 27, 2026-11-13, tasks AM1)

> "Remove any of the diagnosis tools from the app that are no longer needed or already used with no further benefit"

**Restore point:** commit `bab3a84` (the last commit with every tool below still in it). Bring one back with `git show bab3a84:<path>`; nothing was lost, only taken out of the app.
Rule used: a tool goes when the question it was built for is ANSWERED (the owner's tablet numbers are recorded in `research/`) or when it never produced a number and nothing depends on it. A tool stays when an OPEN question still needs it.

## 1. Removed in alpha32

| Tool | Where it was | The question it answered | Why it can go (evidence) |
| :- | :- | :- | :- |
| **Tap lab** | Diagnostics button, `assets/labs/tap.html` | does double-tap zoom delay taps (EX-3) | measured on the emulator and on the owner's tablet (Tablet paste, alpha27); decision taken: double-tap zoom stays off (`research/EX_RESULTS.md` EX-3) |
| **Decode lab** | Diagnostics button, `assets/labs/decode.html` + 4 test pictures (394 KB) | how long the tablet takes to decode AVIF / WebP / JPEG (AG1d) | the owner pasted the numbers (AVIF 108 ms vs WebP 50 ms vs JPEG 27 ms); decision: AVIF stays, the "no AVIF on Fast Wi-Fi" switch stays off by default (`research/VIDEO_AUDIT.md` section 9) |
| **Scroll test** and **Compare 3 feels** | two Diagnostics buttons, `ScrollTest.kt`, `assets/scrolltest/` (556 KB), the runner in `BrowserActivity` | is scrolling smooth at Standard / Smoother / Smoothest (Q2e) | the owner ran all three on the tablet: SMOOTH, 64-65 frames a second, 0 of 32 swipes with a gap over 100 ms (INBOX 2026-11-13T05:02Z, `research/TABLET_DIAG_2026-11-13.md` section 4). **Settings > Appearance > Scroll feel stays** (it is a setting, not a test) |
| **Blocker mode switch** ("fast / compatibility") | Diagnostics button, `toggleBlocker`, pref `engineInExtension` | A/B for the typing lag after D1 (AA3-2) | never produced a number ("the owner's numbers never came"); the tablet's blocker line says 648 requests judged in the engine, 723 us average, 0 errors. The extension still falls back to asking the app request by request **while the lists load** (start-up, a failed transfer): that safety path is in `background.js` and stays |
| **Test the pulse (10 s)** | Settings > Appearance, `Neon.test` | does the neon pulse show on the tablet (V6) | shipped alpha9/10; the owner never mentioned the pulse again. The line "The glow right now" in Settings and "Neon glow:" in the Diagnostics report stay and say why the glow holds |
| **Keyboard events line** | Diagnostics report | does the keyboard probe deliver layout events on the real tablet (EX-11) | the tablet reported 14 events (`TABLET_DIAG` section 4). The probe itself is a working part of the layout and stays |
| **Warm-ups line** and its counter | Diagnostics report, `GeckoEngine.warmCalls` | do the connection head starts reach the engine (EX-2 / EX-15) | the tablet reported 7 (`TABLET_DIAG` section 4). Every call is still one `engine warm` line in the event list |
| **GPU line** and the EGL probe | Diagnostics report, `GlInfo.kt` (a 1x1 GL context made every time Diagnostics opened) | which GPU the engine draws with | recorded: ARM Mali-G52 MC2, OpenGL ES 3.2 (`TABLET_DIAG` section 3, `VIDEO_AUDIT.md`) |
| **Video decoders block** | Diagnostics report (12 lines), `DeviceVideo.report`, the MediaDrm property read | which decoders the tablet has | recorded: H.264 / H.265 / VP9 in hardware, no AV1, no AC-3, Widevine L3 (`TABLET_DIAG` section 3). `DeviceVideo` itself stays: the engine reads the answer to choose which codecs to offer sites; the one-line "Video:" policy text stays in the report |
| **EX-2 proof hook** | `GeckoEngine.speculativeConnect`, a debug-only `debug.kestrel.warmpath` branch | showed that GeckoView 160 drops the public warm-up call | proven 2026-11-12 (`EX_RESULTS.md` EX-2); the working call is all that is left |
| (alpha31, earlier) | Video lab and 5.3 MiB of test clips | decoding, dropped frames | owner message 25, AK2 |

Size: about 0.95 MB of assets and about 300 lines of code less in the APK; Diagnostics opens a little faster (no GL context, no MediaDrm object) and the copied report is about 14 lines shorter.

## 2. Kept, and what keeps each one

| Tool | Why it stays |
| :- | :- |
| Diagnostics screen: Refresh screen, Update, Copy, Share, and the report | the only way anyone can see the tablet from here; Refresh screen is also the manual cure for a page that stops updating |
| Health event list, the UI-thread watchdog, the frame-jank counter | cheap (a sleeping thread, a counter) and the only evidence if a freeze or jank ever returns (the owner's paste showed `frames.jank=126` in 591 s) |
| Report lines: Engine, Network, Wi-Fi lock / boost, Download windows, the engine pref read-backs, Video policy, **Storage**, **Blocker numbers**, **Page report**, **Firefox's own tracking protection, this page**, Neon glow, Page view, **Page loads**, the phone-helper lines | each backs a question that is still open: the Doraby / Cloudflare page (AI1c), the update that cannot install (AJ1), Fast Wi-Fi and the network lab, whether a config change really reached the engine |
| Settings > Network lab (DNS over HTTPS, HTTP/3, buffers, font delay, fetch next page, Wi-Fi awake, helper under Fast Wi-Fi) | real settings with real effects on slow pages, not diagnostics; "No AVIF pictures on Fast Wi-Fi" stays as a plain switch |
| Settings > Test the phone link | measures the Kestrel Courier's phone link (uses about 40 MB); a feature of the phone helper |
| `DebugProps` (`debug.kestrel.*` system properties) | read ONLY by debuggable builds (a release build always gets null), used by the emulator test rig; costs nothing on the tablet |

Anything in section 2 can go too when its question closes: tell the next session which one and it is a one-file change.

## 3. Added in alpha33 (three report lines, each answers an open question; all text, no lab, no download)

| Line | Question it answers | Cost |
| :- | :- | :- |
| `Storage tidy: last <time>: freed ...` | AO4: did the daily clean-up run, what did it remove (update files, old installers, stale engine cache, crash reports, closed tabs' pictures, stopped downloads) | none: a stored sentence; the tidy runs 25 s after the app comes to the front, at most once a day and straight after an update |
| `Video size estimates: #1 shown 210 MB, finished 188 MB (+12%)` | AO2: how far off was the size the list showed before each recent stream download (last 8, no names) | none: one note when a stream finishes |
| `CPU by process, % of ONE core ...: tab avg 31 / peak 98; ... busy moments: ... [GeckoMain 71 ...]` | AO1: when a page or video lagged, which process and which thread was working flat out (`research/VIDEO_SMOOTHNESS.md`) | five tiny `/proc` reads every 5 s while the browser is on screen, nothing in the background; one extra second of thread reads while a process is above 70 % |

Close them when answered: the Storage line when the owner reports free space stays healthy over a week; the size line after a handful of real downloads; the CPU line when a lag report has been explained.
