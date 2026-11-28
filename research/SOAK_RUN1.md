# SOAK1c: what the first one-hour emulator run found (2026-11-15)

Owner message 43: "put the browser in Android 10 emulators ... running on GitHub runners ... simulating browsing on a variety of web sites and videos and downloads for an hour, and log any issues with loading, speed, finding and
downloading videos, crashing, etc. Then look for ways to improve it based on the simulated runs." The lab is described in `research/EMU_LAB.md`; the run is android.yml run 43740398387 (app = lab, suite all, 60 minutes, net = hotspot,
commit 281c48f4, 79 scenes in 70 minutes, Android 10 emulator 600 x 1024 at 170 dpi, 2 GB, 2 cores, Kestrel G debug build with GeckoView 160 for x86_64).

## How far the numbers can be trusted
- **Page loads, public videos and downloads: NOT trustworthy.** This run shaped the emulator's network to 5 Mbit/s (the owner's hotspot cap). The emulator's shaping made ordinary pages (wikipedia, bbc, mdn, hacker-news, npr-text,
  guardian) time out or end in a network error page and a 10 MB download never arrive; the earlier unshaped runs loaded the same pages in under a second. The owner's own words (message 45): fast wifi is the normal case, the cap is
  irrelevant most of the time. All main lab runs from now on use the runner's own fast link (`lab_net = full`, the default); a shaped run is an extra, and its page numbers must not be read as the app's.
- **Valid and useful:** everything that does not depend on a fast link: the full-screen turning, the download questions, the installer look-alike guard, the video controls and the long press on a video, crashes of the app's own process.

## What it showed
| # | Finding | Class | What was done |
| -: | :- | :- | :- |
| 1 | Full-screen turning (FS1): all nine steps pass, including "Back leaves full screen" (every earlier "Back does not leave full screen" was Android's one-time "Viewing full screen" box taking the first Back key) | app OK | lab taps the box away (`Phone.dismiss_cling`) |
| 2 | Downloads nobody tapped (X2): the two timer-started downloads asked with the "by itself" wording, the two tapped ones with the plain wording | app OK | proven on the real engine |
| 3 | APK look-alike guard (X1): the installed APK as `.apk` and renamed `holiday.mp4` are both refused, a plain `.mp4` is not an installer | app OK | proven |
| 4 | Video controls and long press (VC1 / LP1) on the lab pages: bar on a naked video, on a shadow-root video and under a frame; "own" where the page has controls; long press = the Save video sheet, never the floating panel | app OK | proven (the live Yahoo page was proven in runs 43738479064 and 43739450975) |
| 5 | Three "crashes" `am_proc_died` of `:media` and `:sandboxed_tab...` | LAB false alarm: helper processes end normally when a tab closes or the decoder is idle; the app logged no `onCrash` | the lab counts only the main process now |
| 6 | Lab clips "stalling: played only 1 s in 26 s" with 616 frames shown | LAB measurement artifact: the clip loops and the played-seconds read wrapped | none needed (frames and drops are right) |
| 7 | `<video src=...m3u8>` (hls-mux, hls-apple) did not play ("Failed to open media") | not judged: this run had the shaped network; HLS through hls.js / MSE (what sites use) is a different path | re-check in a fast run |
| 8 | Google's Safe Browsing test pages not always blocked | LAB: the list download was starved by the shaping; Mozilla's four pages were blocked in every round, all seven pages in the fast runs | none |
| 9 | The report table of the video-controls scene lost the live Yahoo row when the scene ran again | LAB bug | fixed |

## What the hour did NOT tell us, and what replaces it
Page speed, broken pages, script errors, video playback on real sites and scrolling smoothness on a fast link: that is the compatibility run of owner message 45 (`research/COMPAT_RUN1.md`): about a hundred well-known sites,
each loaded with Shields on and with Shields off, from a cold cache, with the page's own health numbers, the engine's script errors, the app's scroll counters and the memory after every page.
