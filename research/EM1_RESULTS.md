# EM1 results (owner message 2026-11-19: GitHub emulator labs on the newest build + eleven things to improve)

Every lab number here is an x86 emulator with a software GPU (see "What the emulator cannot prove" in research/EMU_LAB.md): read presence / absence / order of events and the CHANGE between two runs, never the milliseconds as the tablet's.
The one source of REAL tablet facts is the owner's Diagnostics pastes (private INBOX.md); this file quotes them where they decide something.

## EM1a - the baseline: daily suite on the tablet profile, alpha70 code (lab run 44432470135, commit de41f802, 2026-11-19)
Device: Android 10, 600x1024 at 170 dpi, font scale 1.2, 2 GB, 2 x86 cores, SwiftShader. NOT matched (cannot be): 32-bit userland (Mozilla ships no x86 GeckoView 160), the Go low-RAM flag, Cortex-A53 speed, Mali, hardware decoders.

| Area | Verdict | Numbers |
| :- | :- | :- |
| Stability | GREEN | 0 crashes, 0 restarts in 72 scenes |
| Security | GREEN | 17 pass, 0 fail, 13 info (popup, camera/mic/location asked, clipboard refused, third-party cookies blocked, WebRTC masked, 4 bad-certificate kinds refused, HTTPS upgrade, mixed content upgraded, no app launched by tel:/intent:/market:, file: script does not run) |
| Page loading | GREEN | 68 pages: first paint median 578 ms, done median 2.1 s, p90 4.9 s |
| Speed | AMBER | the UI thread did not answer for 1.47 s in 3 places (cold start 1465 ms, a long article 1205 ms, the private-tab switch 954 ms); cold start engine-ready 3.9 s |
| Scrolling | GREEN | 28.8 gaps over 100 ms per 1000 frames (software GL: not the tablet's; the tablet measured 62-65 fps) |
| Video | GREEN | tap to first picture 1.3 s; Kestrel's own player 1.3 % frames dropped; one real-site film (Wikimedia Commons) never reported a start |
| Memory | GREEN | peak 296 MB, after the memory warning 284 MB, 4 hibernated tabs of 8 |
| Idle CPU | GREEN | 0.9 % busy in the 60 s `top` sample (the `dumpsys cpuinfo` 14 % line is an average over the minutes BEFORE the step, not idle) |

Findings kept for later rows: F01 (UI late-beats, see below), F02 script errors on Yahoo Finance (438) / Guardian (130) / GitHub (129) - the Shields A/B is the way to tell whether our blocking causes them (LAB2c), F03 284 MB held after the critical warning, F05 Commons film start.

### F01 - the cold-start UI block is Android's own first draw, and the tablet shows it too
Health on the emulator: `ui slow 892 ms` at +1.8 s with the stack `HardwareRenderer.nSetStopped < setStopped < ViewRootImpl.performDraw < performTraversals`, then `ui late-beat 1243 ms`. The same family on the REAL tablet (owner paste 2026-11-18, alpha70): `start activity 255 ms`, `start frame 481 ms`, runtime-ready 866 ms, `ui slow 865 ms` in the earlier alpha58 paste.
So the first hardware draw of the window costs about 0.9 s on both; it is the platform's renderer start (research/LAB5_STARTUP.md: "Android's own first-draw renderer sync"), not our code. What can still help: put the picture of the last page in that first frame (the last look, EM1k), so the 0.9 s is spent showing the page, not a placeholder.

## EM1k - what the owner's tablet shows at a reopen (from the pasted Diagnostics, alpha70, 2026-11-18 ~05:15Z)
```
+0.167s start activity cold 255 ms        +0.749s start open cold 837 ms         +2.091s start stop cold 2179 ms
+0.333s tabs early-cover doraby.com       +0.749s tabs state-mismatch tab=4      +4.092s start paint cold 4180 ms
+0.393s start frame cold 481 ms           +0.753s tabs cover-show tab=4 thumb=true picture=false   +4.092s tabs cover-hide why=first-paint
+0.778s engine runtime-ready (866 ms)     page loaded host=doraby.com paint=1747ms done=6358ms
```
Reading: (1) the plain "Opening doraby.com" cover was used (`picture=false`, no `look-shown`): no saved page picture was usable at that start; (2) the engine's saved state was thrown away (`state-mismatch`), so the page loaded from scratch; (3) first paint of the live page 4.2 s after the process started: about 0.9 s before the engine exists, then 1.6 s until the page load starts, then 1.7 s to paint.
What this build now records so the NEXT paste says why (EM1k step 1, shipped in the next alpha): `tabs look-none why=<off|not-a-page|no-file|page|engine|skin|dark|desktop|unreadable>` at the cover, `tabs look-skip pause why=<...>` when leaving, counters `look.none.*` and `look.skip.*`.
Lab: suite `ro` / `em` (soak.py `reopen_round`, writes reopen.md): load, scroll, leave three ways (Home + Android ends the app 5 s later / 0.4 s later = a swipe in Recents / the system kills it in front), start cold, read the app's own Health lines.

## EM1i - the six real doraby servers: research/EM1_DORABY_SERVERS.md (measured from the sandbox) and lab suite `dr` / `em` (soak.py `doraby_round`, writes doraby.md).

## Runs
| Run | Suite | Commit | Result |
| :- | :- | :- | :- |
| 44432470135 | daily, tablet, full net | de41f802 (alpha70 code) | green; report above |
| 44434241145 | ro (reopen) | 609d22a7 | running when written |
| 44435517079 | em (ro + dr) | 45da7919 | pending behind it |
