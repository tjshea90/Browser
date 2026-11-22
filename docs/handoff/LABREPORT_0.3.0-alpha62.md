# Kestrel build report: Kestrel G (Firefox engine) 0.3.0-alpha62 (code 163)

Lab run 44017705030, commit 7f6901e6, suite **daily**, device profile **tablet**, network **full**, written 2026-11-17 04:56 UTC. Findings are about THIS build; the emulator is not the tablet (section 2).

## 1. Verdict per area

| Area | Verdict | Evidence |
| :- | :- | :- |
| Stability (crash, ANR, restart) | **GREEN** | 0 scene(s) ended in a crash; app restarts seen: 0 |
| Security | **GREEN** | 17 pass, 0 FAIL, 13 info rows |
| Page loading | **GREEN (low items)** | 64 pages; paint p50 267 ms, done p50 749 ms / p90 1435 ms |
| Speed | **AMBER** | UI stalls 0 (worst -); cold start 1.8 s |
| Scrolling | **GREEN** | 4050 frames in 60 bursts, gaps over 100 ms per 1000 frames: 3.2 |
| Typing and input | **GREEN** | 21 characters in 0.2 s, UI late-beats while typing: 0 |
| Video | **GREEN (low items)** | tap to first picture 0.6 s |
| Features (address bar, tabs, downloads, menu screens) | **GREEN** | 0 feature checks failed |
| Memory | **GREEN (low items)** | peak 311 MB, lowest available 469 MB, after warning 303 MB |
| Accessibility tree | **GREEN** | controls must be findable by name (TalkBack and the lab read the same tree) |

Rule: RED = at least one high finding in the area, AMBER = a medium one, GREEN = none; NOT MEASURED = the run did not get to it (see section 7).

## 2. How closely the lab matched the tablet (LAB3a)

| What | The owner's tablet | This run | Verdict |
| :- | :- | :- | :- |
| Android version | 10 (API 29) | Android 10 (API 29) | matched |
| Userland | 32-bit (sys_mssi_t_32_ago, armeabi-v7a) | 64-bit, ABI x86_64 (x86_64,x86) | NOT matched: 64-bit x86_64 (Mozilla publishes no 32-bit x86 GeckoView for 160: lab run 43997001918 could not resolve geckoview-x86); pointers are twice as big, so memory numbers read high |
| Screen | 600x973 at 170 dpi | 600x1024, 170 | matched |
| Font scale | 1.2 | 1.2 | matched |
| RAM | 2 GB | 1992 MB | matched |
| Low-RAM (Go) flag | ro.config.low_ram=true, memory class 128 MB | low_ram=(unset), app heap limit 512 MB | NOT matched (AOSP image, not the Go edition: no Android 10 Go image exists for the emulator) - the emulator started with -prop ro.config.low_ram=true read-back after boot: ro.config.low_ram='' |
| CPU | 4 x Cortex-A53 (MT8168B), in-order, about 2 GHz | 2 cores of an x86 host CPU (model name	: Android virtual processor) | NOT matched: the host core is several times faster; only changes between builds mean something |
| GPU | Mali-G52 MC2 | SwiftShader software GL | NOT matched: smoothness and GPU memory are not the tablet's |
| Video decoders | MediaTek hardware H.264 / VP9 | software decoders | NOT matched: 'plays / does not play' counts, smoothness does not |
| Google services | none (no Play, no GMS) | GMS present: no | matched |
| Storage | unencrypted | encrypted | NOT matched (the emulator image is file-encrypted; no effect on speed worth chasing) |
| Network | phone hotspot, 5 Mbit/s cap (owner: mostly fast Wi-Fi now) | the runner's fast link (datacentre) | partly |

CPU calibration (the smallest total is used as the runner's speed, the first run is slowed by the app still warming up)

CPU calibration page (a fixed amount of JavaScript, sort, JSON, regex, DOM and canvas work, timed in the page): bench-start total 832 ms (loop 429, sort 90, json 37, dom 66, canvas 209); bench-end total 182 ms (loop 73, sort 23, json 16, dom 24, canvas 45). Compare it between runs: a faster runner makes every page look faster.

## 3. Findings, most serious first

| # | Sev | Area | Finding | Evidence | Where to look |
| -: | :- | :- | :- | :- | :- |
| F01 | medium | speed | the UI thread did not answer for 962 ms in 1 place(s) (late heartbeat, below the stall watchdog) | cold-start (2, worst 962 ms). The tablet shows the same family at start (ui slow 865 ms) | BrowserActivity (UI-thread work: a synchronous call into the engine), diag/Health.kt watchdog stack in the Diagnostics 'ui STALL' line names the call; research/FREEZE.md, research/VIDEO_ACCEL_DEVICE.m |
| F02 | low | compat | pages with many script errors: finance.yahoo.com (436), finance.yahoo.com (436), github.com (143), www.theguardian.com (141) | a Shields A/B on the same page tells whether our blocking causes them (LAB2c) | Shields lists / scriptlets (research/ADBLOCK_ENGINE.md) |
| F03 | low | memory | after Android's 'running critical' warning the app still holds 303 MB (peak 311 MB) | hibernated tabs in the run: 4 | BrowserActivity.enforceLiveLimit / hibernate / onTrimMemory, core LiveBudget (docs/HARDWARE.md memory rules) |
| F04 | low | video | wikimedia-commons: the app never saw playback start after a tap | lab issue line | VideoFlow / WebHandOver / NativePlayerActivity / PlayerFactory (research/ANS60_PLAYBACK_OPTIMISATION.md) |

## 4. The day, step by step (what a person does, what it cost)

| Step | Result | Seconds | Page loads (paint / done ms) | UI stalls (worst ms) | Script errors | Memory PSS MB | Notes |
| :- | :- | -: | :- | -: | -: | -: | :- |
| cold-start | ok | 7.9 | localhost -1/345 | 0 (962) | 0 | 191 | {"am_total_ms": 1274, "engine_ready_s": 1.8} |
| home-screen | ok | 5.5 | - | 0 (0) | 0 | 192 | {"nodes": 39} |
| search-typed-in-address-bar | ok | 5.6 | www.google.com -1/504 | 0 (0) | 0 | 222 | {"control_found": true, "host": "www.google.com", "nav_s": 1.0} |
| read-a-long-article | ok | 31.0 | www.google.com 263/4476; en.wikipedia.org 474/821; en.wikipedia.org 261/359 | 0 (0) | 15 | 297 | {"typed": true, "host": "en.wikipedia.org", "nav_s": 1.0} |
| several-tabs-and-switching | ok | 41.5 | news.ycombinator.com 268/377; developer.mozilla.org 390/714; localhost -1/47 | 0 (0) | 0 | 289 | {"opened": 4, "cards": 6, "switch_note": "no cover-hide / painted line within 6 s: the switch time is not known (lab run 44005894797 recorded the 6 s timeout as 6.25 s)"} |
| back-presses | ok | 10.9 | - | 0 (0) | 0 | 282 | {"backs": 3} |
| typing-in-a-form-field | ok | 9.1 | localhost 76/93; localhost -1/21 | 0 (0) | 1 | 284 | {"page_reported_position": true, "field_focused": false, "typing_s": 0.17, "typed_text_arrived": "Kestrel tablet test"} |
| film-in-a-page-press-play | ok | 21.6 | localhost 83/111 | 0 (0) | 0 | 294 | {"tap_to_picture_s": 0.6, "kestrel_player_took_it": true} |
| film-on-a-real-site | ok | 30.9 | commons.wikimedia.org 600/1435 | 0 (0) | 82 | 294 | {"sites": {"wikimedia-commons": "done"}} |
| download-then-downloads-screen | ok | 12.7 | localhost -1/64 | 0 (0) | 0 | 297 | {"downloads_screen": true} |
| app-screens-from-the-menu | ok | 37.4 | - | 0 (0) | 0 | 306 | {"opened": ["Settings", "Diagnostics", "History", "Bookmarks"]} |
| private-tab-and-shields-switch | ok | 72.3 | www.theguardian.com 360/926; www.theguardian.com 210/692; www.theguardian.com 233/722 | 0 (0) | 892 | 313 | {"private_indicator_on_screen": true, "shields_ab": {"on": {"event": "loaded", "done_ms": 919, "paint_ms": 233, "blocked": 10, "done_samples": [926, 722, 919]}, "off": {"event": "loaded", "done_ms": 692, "paint_ms": 210, "blocked" |
| leave-the-app-and-return | ok | 44.2 | - | 0 (0) | 0 | 300 | {"resume_s": 0.33, "page_reloaded": false, "engine_died": false, "alive_after_screen_off": true} |
| many-tabs-and-memory-warning | ok | 85.4 | www.bbc.com 209/1359; www.theguardian.com 225/749; github.com 578/1473 | 0 (0) | 387 | 302 | {"tabs": 8, "after_trim_pss": 303, "trim_lines": ["level=15 avail=507MB thr=216MB low=false trim=15@0s"], "hibernated": 4, "max_pss": 311, "min_avail": 469} |
| idle-cpu-60-seconds | ok | 49.4 | example.com 75/91 | 0 (0) | 0 | 299 | {"cpuinfo": ["12% 4032/app.kestrel.browser.gecko.debug:gpu_disable_art_image_: 1.3% user + 11% kernel / faults: 10201 minor 4 major", "11% 3914/app.kestrel.browser.gecko.debug: 5.3% user + 5.9% kernel / faults: 79723 minor 52 majo |
| security-and-feature-probes | ok | 170.6 | localhost 406/508; 127.0.0.1 48/63; localhost -1/58 | 0 (0) | 3 | 307 | {} |
| site-tour-nine-public-sites | ok | 116.3 | text.npr.org -1/135; text.npr.org -1/129; news.ycombinator.com 266/314 | 0 (0) | 422 | 345 | {"sites": ["npr-text:ok", "hacker-news:ok", "github-repo:ok", "stackoverflow:ok", "bbc-news:ok", "guardian:ok", "reddit:ok", "weather-wttr:ok", "duckduckgo:ok"], "blank_first_screen": []} |
| heavy-page-yahoo-finance | ok | 16.0 | finance.yahoo.com 1321/1959 | 0 (0) | 436 | 360 | {"sites": ["yahoo-finance:ok"], "blank_first_screen": []} |

### Page loads

| Page | Host | First paint ms | Done ms | Blocked requests | Script errors |
| :- | :- | -: | -: | -: | -: |
| read-a-long-article | www.google.com | 263 | 4476 | 9 | None |
| heavy-page-yahoo-finance | finance.yahoo.com | 1321 | 1959 | 13 | 436 |
| heavy-yahoo-finance | finance.yahoo.com | 1321 | 1959 | 13 | 436 |
| many-tabs-and-memory-warning | en.wikipedia.org | 1397 | 1578 | 0 | None |
| many-tabs-and-memory-warning | github.com | 578 | 1473 | 0 | None |
| film-on-a-real-site | commons.wikimedia.org | 600 | 1435 | 0 | 82 |
| site-tour-nine-public-sites | github.com | 679 | 1435 | 0 | None |
| wikimedia-commons | commons.wikimedia.org | 600 | 1435 | 0 | 0 |
| tour-github-repo | github.com | 679 | 1435 | 0 | 143 |
| many-tabs-and-memory-warning | www.bbc.com | 209 | 1359 | 14 | None |
| many-tabs-and-memory-warning | stackoverflow.com | 691 | 1203 | 13 | None |
| site-tour-nine-public-sites | stackoverflow.com | 578 | 1044 | 12 | None |
| tour-stackoverflow | stackoverflow.com | 578 | 1044 | 12 | 3 |
| site-tour-nine-public-sites | duckduckgo.com | 415 | 956 | 1 | None |
| tour-duckduckgo | duckduckgo.com | 415 | 956 | 1 | 12 |
| private-tab-and-shields-switch | www.theguardian.com | 360 | 926 | 0 | None |
| private-tab-and-shields-switch | www.theguardian.com | 229 | 919 | 10 | None |
| site-tour-nine-public-sites | wttr.in | 752 | 868 | 1 | None |
| tour-weather-wttr | wttr.in | 752 | 868 | 1 | 0 |
| private-tab-and-shields-switch | www.theguardian.com | 244 | 823 | 0 | None |
| read-a-long-article | en.wikipedia.org | 474 | 821 | 0 | None |
| site-tour-nine-public-sites | www.bbc.com | 217 | 799 | 0 | None |
| tour-bbc-news | www.bbc.com | 217 | 799 | 0 | 117 |
| site-tour-nine-public-sites | www.theguardian.com | 493 | 785 | 0 | None |
| tour-guardian | www.theguardian.com | 493 | 785 | 0 | 141 |
| many-tabs-and-memory-warning | www.theguardian.com | 225 | 749 | 5 | None |
| private-tab-and-shields-switch | www.theguardian.com | 233 | 722 | 0 | None |
| several-tabs-and-switching | developer.mozilla.org | 390 | 714 | 0 | None |
| private-tab-and-shields-switch | www.theguardian.com | 210 | 692 | 0 | None |
| private-tab-and-shields-switch | www.theguardian.com | 157 | 682 | 0 | None |
| site-tour-nine-public-sites | wttr.in | 521 | 567 | 1 | None |
| security-and-feature-probes | localhost | 406 | 508 | 1 | None |
| many-tabs-and-memory-warning | developer.mozilla.org | 192 | 441 | 0 | None |
| many-tabs-and-memory-warning | stackoverflow.com | -1 | 417 | 0 | None |
| security-and-feature-probes | mixed.badssl.com | 321 | 383 | 0 | None |
| several-tabs-and-switching | news.ycombinator.com | 268 | 377 | 0 | None |
| read-a-long-article | en.wikipedia.org | 261 | 359 | 0 | None |
| many-tabs-and-memory-warning | news.ycombinator.com | 225 | 354 | 0 | None |
| cold-start | localhost | -1 | 345 | 0 | 0 |
| site-tour-nine-public-sites | news.ycombinator.com | 266 | 314 | 0 | None |

### Security and feature checks

| Check | Result | Detail |
| :- | :- | :- |
| popup-without-tap | pass | window.open returned a stand-in and the app showed its blocked-page chip (open localhost) |
| notifications | pass | no Notification object |
| push-api | info | PushManager absent, serviceWorker present |
| fp-canvas | info | two reads identical e673ea6 |
| fp-webgl | info | Google (Google Inc.) / Generic Renderer |
| fp-device | info | cores 4, memory undefined, screen 560x956 dpr 1.0714285714285714, inner 560x769, tz UTC off 0, lang en-US, platform Linux armv81, plugins 5 |
| fp-useragent | info | Mozilla/5.0 (Android 10; Mobile; rv:160.0) Gecko/160.0 Firefox/160.0 |
| fp-apis | info | battery false, connection false, bluetooth false, usb false, hid false, serial false |
| fp-timer | info | smallest clock step about 1.000 ms |
| clipboard-read | pass | refused: NotAllowedError |
| third-party-cookie-frame | info | cookie readable inside the frame: no, localStorage: yes |
| referer-same-site | info | same site was told: http://localhost:8091/sec?tag=sec1 |
| header-gpc-dnt-same | info | Sec-GPC 1, DNT -, Accept-Language en-US |
| header-useragent | info | Mozilla/5.0 (Android 10; Mobile; rv:160.0) Gecko/160.0 Firefox/160.0 |
| referer-cross-site | pass | the other site was told: http://localhost:8091/ (page asked for the full address) |
| header-gpc-dnt-cross | info | Sec-GPC 1, DNT -, Accept-Language en-US |
| webrtc-address | pass | only masked (.local) candidates |
| frame-navigates-top | pass | page is still /sec |
| camera-microphone | pass | the app asked the person first (dialog: 'localhost wants to use your camera and microphone' with BLOCK / ALLOW) and nothing was granted without a tap |
| location | pass | the app asked the person first (dialog: 'localhost wants to know your location' with BLOCK / ALLOW) and nothing was granted without a tap |
| third-party-cookie-read | pass | first-party visit found the frame cookie: false, localStorage: no |
| page-cannot-launch-other-apps | pass | the app stayed in front, no other app was started by the 8 addresses |
| file-url-script-does-not-run | pass | a file:// page's script did not run (the app said: no error page) |
| bad-certificate-expired | pass | the load was refused (page loaded, ok=false, 819ms ms): no content shown |
| bad-certificate-self-signed | pass | the load was refused (page loaded, ok=false, 534ms ms): no content shown |
| bad-certificate-wrong-host | pass | the load was refused (page loaded, ok=false, 2040ms ms): no content shown |
| bad-certificate-untrusted-root | pass | the load was refused (page loaded, ok=false, 1268ms ms): no content shown |
| http-upgrade | pass | page event loaded host=example.com ok=true; console errors 0; typed http://example.com/, the padlock says 'Secure connection' (address text 'example.com') |
| mixed-content | pass | page event loaded host=mixed.badssl.com ok=true; console errors 0; the engine upgraded 1 insecure picture request(s) to https |
| https-upgrade-counter | info | Diagnostics before: [] / after: [] |
| typed-search-goes-to-google | pass | navigated to www.google.com (owner decision: anything typed that is not an address is searched on Google) |
| typed-address-without-scheme-loads | pass | en.wikipedia.org/wiki/Firefox -> en.wikipedia.org (HTTPS-first) |
| typing-reaches-the-page | pass | 19 key events typed into the page's Name field arrived intact; focus line seen: False |
| film-starts-when-pressed | pass | first picture 0.6 s after the tap |

### What the app says about itself (its own Diagnostics, end of the run)

- Web videos in Kestrel's own player (Settings > Web videos play in: Kestrel's player whenever it can): 2 questions from pages, 1 films taken over (by the page's play() 0, a press on the player 1, Kestrel's own bar 0, full screen 0, a playlist that came late 0),
- Real video: nothing played yet this run (the numbers fill in while a video plays on a page); Video: codecs offered to sites: VP9 as the engine decides, no AV1 (software only here), no H.265 (software only here) [hardware decoders ahvi]; page animations normal;
- Kestrel's own player: 1 plays, 0 errors; decoder OMX.google.h264.decoder (software); last picture 640x360, first picture 850 ms after the start; 15 s played, 377 frames shown, 2 dropped (0.5 %); quality picked 0 time(s), rotate button 0 time(s); playable 776 m
- Real scrolling: 7 bursts on 2 site(s), 919 frames, gaps over 50 ms in 3.3 %, over 100 ms in 0.5 % (worst: www.theguardian.com 1017 ms); measured by a frame callback that runs only while a page scrolls, so it shows how well the PAGE keeps up (blank patches), no
- Blocker: fast mode, list #1, 4549 KB, engine ready from the saved copy in 111 ms (no transfer from the app). 1412 requests judged in the engine (average 184 us, slowest 32000 us, 12 over 2 ms), 0 asked the app one by one, 0 errors. Extension listeners (calls, 
- Page report (the last pages: what Shields blocked, what failed to load, scripts that threw; a private tab is never listed):
- wrong.host.badssl.com/ (1 s ago): 0 requests, 0 blocked by Shields, 1 FAILED, no script errors
- self-signed.badssl.com/ (5 s ago): 0 requests, 0 blocked by Shields, 1 FAILED, no script errors
- expired.badssl.com/ (6 s ago): 0 requests, 0 blocked by Shields, 1 FAILED, no script errors
- localhost:8091/sec/scheme?tag=sec1 (29 s ago): 0 requests, 0 blocked by Shields, no failed loads, no script errors
- 127.0.0.1:8091/sec/read?tag=sec1 (36 s ago): 1 requests, 0 blocked by Shields, no failed loads, no script errors
- localhost:8091/sec?tag=sec1 (64 s ago): 21 requests, 0 blocked by Shields, no failed loads, no script errors
- Firefox's own tracking protection, this page: on; blocked nothing
- Page loads (newest last; time from tap to first paint / to load finished):

## 5. Change since the last report

No baseline yet: this run becomes the baseline when research/lab-baseline.json is written from it.

## 6. Ways to improve (especially speed)

### What the daily lab measured on build 0.3.0-alpha62 (runs 43997607118 and 44003337190, emulator numbers: read the ratios and the behaviour, not the milliseconds)

| Thing | Measured | Reading |
| :- | :- | :- |
| Cold start (force-stop, start) | `am start` total 1.67 s, engine ready 2.2 s; the UI thread did not answer for 1.18-1.25 s right at start | the same 1 s block the tablet shows (865 ms) = the start-up engine burst, candidate 3 |
| A long article, first big page after a typed address | UI late heartbeats 877-1386 ms around the first large page | the same family: synchronous engine calls on the UI thread (candidate 2 adds the read-back at every navigation) |
| UI thread blocked past the watchdog (STALL) | 0 in 15 steps | no hard freezes in this day |
| Film pressed in a page | first picture 0.7 s after the tap; Kestrel's player: 195 frames, 0 dropped (software decoder) | start path is quick; hardware decoder start-up is tablet-only (candidate 4) |
| Tab switch, resume from Home, screen off / on | 0.44-0.68 s; 0.32 s without a page reload; app alive after screen off | fine |
| Shields on vs off, one heavy page (theguardian.com), fast link | run 5, three alternating repeats per mode: ON 1226 / 1294 / 1221 ms (median 1226, 7 requests blocked), OFF 1319 / 1315 / 1150 ms (median 1315); the earlier single samples (ON slower by 4-28 %) were noise | Shields does NOT cost load time here: ON is about 7 % faster on the median. LAB3h answered: no action |
| Memory with 8 heavy tabs | peak 291 MB PSS, 7 of 8 tabs hibernated, 280 MB after Android's RUNNING_CRITICAL warning, lowest available 471-543 MB | the live budget works; little more is handed back on a trim because the hibernating already happened (F04 is low) |
| Blocking engine | 1139 requests judged in the engine, average 252 us, slowest 27 ms, 0 asked the app one by one, ready from the saved copy in 213 ms | candidate 1 is NOT needed |
| Scrolling (software GL) | 8-14 gaps over 100 ms per 1000 frames (real-page bursts: 3.7 % over 100 ms, worst 1.28 s on theguardian.com) | an emulator number; the tablet's own measurement (62-65 fps, 0 of 32 swipes with a gap) stands |
| Page paint / done on real sites | paint median 350-360 ms, done median 385-1235 ms, slowest tenth 1.9-4.4 s | emulator + datacentre link |
| Typed `http://example.com/` | the address bar says 'Secure connection' (the padlock's description is 'Connection is not secure' for http) | the https upgrade works |
| Security and privacy | 14 pass, 0 fail (run 3): bad certificates refused (4 of 4), pop-up blocked with the app's chip, mixed content upgraded to https, third-party cookie not shared, referrer trimmed cross-site, Global Privacy Control sent, WebRTC address masked, clipboard read refused, other-app launches without a tap refused, `file://` script did not run, camera / microphone / location asked with a BLOCK / ALLOW dialog | the privacy side of the brief holds on this build |

**What the lab got wrong first and why it matters for reading any future report:** four early `FAIL`s were the lab's mistakes, not the browser's (a refused certificate logs the app's 'page loaded' line with ok=false; a blocked pop-up still hands the page a stand-in window; taps missed a form field; script errors were counted per step, not per page). Each was found by reading the raw Health lines against the app's own Diagnostics. Lesson: a security `FAIL` from the lab is a question to check against the logs, not a verdict.

**Not achievable in the lab, found on the way:** a 32-bit userland (Mozilla ships no `geckoview-x86` for 160), the Go edition / low-RAM flag (the emulator accepts `-prop ro.config.low_ram=true`, Android 10 ignores it: read-back empty), the tablet's CPU / GPU / decoders.

### Speed: what is already done, what was refused, and what is new (LAB3f, 2026-11-17)

How this was made: one read-only research pass over research/SPEED2-4, GECKO_SPEED, GECKO_TUNING_2, HW_SPEED, SCROLL, COLD_START, PERF_JS, PERF_KOTLIN, EX_RESULTS, FREEZE, TYPING, VIDEO_SMOOTHNESS, ANS60, the REVIEW-EXTERNAL files, gecko-prefs.txt (config v9) and the ticked TASKS items, to build a ledger so nothing old is proposed again. Gains below are ESTIMATES until a lab run or the tablet measures them. Candidates 1 and 2 were checked against the code by reading (see "Checked"); the others are the research pass's reading only.

**Already done (do not redo):** engine prefs v9 (bfcache 6 pages / 1 h, 64 MB memory cache, 512 MB disk cache, bytecode cache, lazy-load margin); the in-extension blocking engine with a saved copy; preconnect on the Gecko thread; warm launch; scroll feel levels (measured 62-65 fps on the tablet, 0 of 32 swipes with a gap over 100 ms); decoded-image budget; only the front tab loads; tab state on disk; hardware decoders first, press-to-play, inline player, 10 s back buffer, 4 Mbit/s first guess; the UI-stall watchdog; `adjustNothing` while typing in the address bar.
**Refused, with reasons in the research files:** another engine, a rebuilt Gecko, eager delazification, Spectre mitigations off, software WebRender / no GPU process, largeHeap, a permanent foreground service, async MediaCodec / tunnelling / OkHttp / disk cache for video, ad blocking on the phone.
**Open, waiting for numbers (not new):** pref A/Bs (trr.mode, HTTP/3, nursery, stylo threads, `precache-shaders`), a baseline profile (none in the repo), the memory governor, the heavy-widget benchmark, per-key typing latency on the tablet.

**New candidates, ranked by (gain x confidence) / risk**

| # | Candidate | Why it should be faster | Est. gain | Risk | Measure in the lab |
| -: | :- | :- | :- | :- | :- |
| 1 (DOWNGRADED by lab run 43997607118: its Diagnostics say 'engine ready from the saved copy in 213 ms, 1139 requests judged in the engine, 0 asked the app one by one': the problem seen in COMPAT_RUN1 is not present now; only a first-ever start without a saved copy could still use the slow path. Keep only if a fresh-install run shows legacy decisions) | Cold-start requests wait (bounded, ~300 ms) for the SAVED blocking engine instead of being asked to the app one by one; stop deleting `cfgMode` on every main frame (stale-while-revalidate) | `decide()` falls to `legacy()` while `engine === null`; the first page's requests each wait for the UI thread, which is blocked ~0.9 s at start (COMPAT_RUN1: 70 requests asked one by one, 263 ms each) | 100-300 ms on the first page after each cold start, correct blocking from request 1 | low (bounded wait) | cold start then first page: Blocker `lg` / `lu` counts and paint / done; exact counts on the emulator |
| 2 | Take the synchronous GPU read-back off the tap path: skip it when the left page is bfcache-eligible, post it after `allow` returns otherwise; gate the settled / page-stop Last Look captures on a finger idle >= 2 s; one shared read-back for Last Look, grid thumb and Back picture | `TabDelegates.decideLoad` -> `leavingPage` -> `captureThumb` -> `readBack` -> `GeckoView.capturePixels()` is a synchronous call into the compositor inside `onLoadRequest`, before every navigation starts | 20-100 ms per link tap, long tails when the compositor is slow; the lab already saw a 2 s block | low-medium (Back shows the plain cover if the engine evicted the page) | log every `readBack` duration; tap-to-PageStart, `ui slow` count, swipe-pause-swipe with scroll-gap counters; compare with Settings 'last page' off |
| 3 (CONFIRMED by lab run 43997607118: the cold-start step shows a 1178 ms late UI heartbeat on the emulator, the tablet 865 ms; the read-a-long-article step shows 877 / 884 ms around the first big page) | Slice the start-up engine burst: one `Io.main.post` (BrowserActivity ~521-531) runs runtime create, warmUp, ShieldsBridge.install and ~25 live `setGeckoPref` calls; time each, then one call per frame, move static prefs into the YAML | the tablet shows `ui slow 865 ms` at +1.4 s, the emulator 928 ms: the same block | taps accepted 0.2-0.6 s earlier (total time unchanged); only if the hog is our code, not `GeckoRuntime.create` | low-medium | `ui slow` events in the first 3 s (a fair test on the emulator) |
| 4 | Pre-warm the vendor video decoder: `PlayerFactory.warm()` only queries `MediaCodecList`; also `createDecoderByType("video/avc")` then `release()` on the idle thread 20 s after the app is in front | first codec instantiation loads MediaTek's codec service and libraries | 100-300 ms off the first press | low (skip while a film plays) | tablet only (the emulator has software codecs); at most first-film vs second-film `playable after` |
| 5 | Idle-CPU audit per thread (the alpha33 tablet report shows the parent at 5 % idle): a lab step idles 60 s and reads `/proc/<pid>/task/*/stat` by thread name, then cut the top consumer | contention, heat, battery | small-moderate | low | shares are valid on an emulator, absolute values are not |
| 6 | Inject the video-UI scripts (`videobar.js`, `turn.js`, `player.js`, `seekbar.js`, ~85 KB of ~310 KB) only when a `<video>` exists | every http(s) frame parses them today (10-30 frames on ad pages) | 3-10 ms and 0.2-0.5 MB per frame (unmeasured) | low-medium (first-press race) | per-script `performance.now` pair, frames per page, content-process PSS on 5 heavy pages |
| 7 | Measure the keyboard-open resize handshake before touching it (`adjustResize` makes GeckoView shrink 2-3 times per keyboard open: the same handshake that blocks 0.9-2 s at rotation) | step 1 only: type into a page `<input>` and read the existing `gecko layout WxH took N ms` lines | decide after the number | high if changed | lab: that single step |
| 8 | A/B `gfx.webrender.precache-shaders` (set on judgment, never measured; loads all shader binaries at every start) | possible start-up cost on the Mali | unknown | low | cold-start `paint` mark, owner-run on the tablet only |

**Checked by reading (this session):** (1) `background.js decide()` takes the engine path only when `engine !== null`, otherwise it reaches `legacy()`; the saved copy is loaded by `cacheLoaded` but `decide` does not wait for it. (2) `TabDelegates.kt:143` calls `a.leavingPage(tab, url)` inside the navigation decision, and `leavingPage` calls `captureThumb(tab, forBack = true)` (rate-limited to one per 1.5 s) which goes through `readBack` -> `capturePixels()`. No pref name was re-verified (the GeckoView AAR is not in this sandbox): run `tools/check-gecko-prefs.sh` before adding any.

**Cannot be measured on the emulator:** anything that depends on the Mali GPU, eMMC, the MediaTek decoders or the Cortex-A53 clock. Counts, ordering, UI-thread handshake stalls and relative A/B inside one run are valid.

## 7. What this run did not measure

- The tablet's real CPU, GPU and video decoders (section 2): every absolute time and every smoothness number is an emulator number.
- Sites that block data-centre addresses show as upstream errors or as a consent / challenge page; they are not app faults.
- Android 16 behaviour (the lab image is Android 10), the phone-hotspot link (unless net=hotspot), a real touch screen (events are injected), the owner's own keyboard, real passwords and logins.
- Steps that report 'skipped' ran out of the day's time budget; steps that report 'tooling-error' are faults of the lab script, not of the app.
- The lab saved the native view tree of 7 screens (ui-*.txt in the artifact) for tuning the next run.
