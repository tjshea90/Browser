# ND6 - what the newest sent diagnostics say (2026-11-28, build alpha114)

Owner (verbatim, 2026-11-28): "After that is done, look for improvements to the browser from sent diagnostics and code" ("that" = PDL1, the Proton Drive download).
Read with `python3 -B -I tools/diag_digest.py` (60 uploads, 1 device, newest file `2026-11-28/075626`; five uploads of today are alpha114, the owner's own session
06:10Z to 07:56Z, the session in which the Proton download failed). All numbers are from the tablet; nothing here was re-run on a device.

## 1. The numbers (focus report of the digest; verdict words are the digest's own thresholds)

| Area | Number | Verdict |
| :- | :- | :- |
| cold start to first paint (median of all uploads) | 18.9 s; today's process 16.5 s (runtime 0.96 s, activity 0.3 s, so the rest is the engine's first page) | WATCH (OK <= 10 s) |
| engine ready after start | 1.1 s | OK |
| typing in the address bar / in page fields | 7-10 ms mean (worst 0.46 / 1.2 s) | OK |
| switch to a live tab / to a sleeping tab | 0.30 s / 0.36 s median | OK |
| first contentful paint of a page (564 loads) | 1.9 s median | OK |
| load finished (median) | 6.1 s | WATCH |
| parent process CPU average | 41 % of one core | WATCH |
| slow screen frames per upload | 84 median | WATCH |
| video dropped frames | 0.8 % | OK |
| free memory | 481 MB median; 64 tabs put to sleep, 17 engine kills; memory by process (median of the peaks): tab 183 MB (peak 418), parent 226, GPU 62 | the tablet's limit |
| engine kills | 15 content-process kills: 12 while the app was away, 3 in front, 1 while keep-ready held the process | known (KR1) |
| real load failures | 0.7 % (4 of 561); 118 loads cut short (a tap on a link, Back, Stop: not failures) | OK |

## 2. What the data does NOT show (and why that was the finding that was acted on)

- The Proton Drive failure of this morning is in NONE of the five uploads: no `download` line at all (the only one is the app-update line). A failed download wrote nothing to the diagnostics,
  so a session reading the digest could not have told that downloads were failing. Fixed in ND6a-A: every failed download now counts `download.failed.<reason>` and writes one
  `download failed <reason> http=<n> host=<site> done=<bytes> total=<bytes>` line (not for a private download; the site name goes through `Health.log`, which masks it while a private tab exists).
  Together with the PDL1 counters (`download.handed.offered|ordinary|used|gone`) the next upload shows whether the engine's stream was kept, used, or never claimed.

## 3. What the data shows that was already known and is waiting for something only the owner or the tablet can give

- claude.ai (134 loads, 94 analysed): load 25 s, first paint 12.7 s, 1,151 requests, the page's main thread too busy to answer for 93 % of the load: the page's own thousand modules
  (research/PROFILE_CLAUDE_AI.md section 9). Nothing of ours is a measurable share; the open knobs (ND2c: tracking classifier prefs, per-tab listener, the engine's script cache) need a tablet A/B.
- First start after an install ends by itself 14 to 35 s after the install in 27 of 27 cases (median 25 s). alpha114 changed how Kestrel steps out of the way during an update; the FIRST
  update on top of alpha114 is the alpha115 install: ND4a is answered by the digest's FIRST START AFTER AN INSTALL section once the owner has installed alpha115 and the app has run.
- Cold start to first paint 16-19 s: the one real lever is compile-at-install (LAB6, needs the owner); not touched.
- Shields-down sites: claude.ai x64, my.nobullnetworks.com x2: ND5c (which Shields part breaks the reCAPTCHA login) waits for the owner's two panel tests.
- File attaches (8): every one decoded and re-encoded fine on the device; claude.ai itself refused the PNGs (research/IMG1_UPLOAD.md: the page or the site, not the file or the engine).
- Video: pornhub `code 9 none` x22 and YouTube `code 99 blob` x4 are the known page-player waits (PERF7, PERF8); the press-to-play rule held 144 videos on parlay-api.com that nobody pressed.
- UI-thread stalls today: two, 0.8 s inside the engine's network-state broadcast and 0.75 s in the app's memory-trim callback while memory was short; both single events, nothing to fix from them.

## 4. Considered and NOT done (with the reason)

- ND3g (`pagetime.js` resource observer, 1.5 % of the page's main thread on a heavy page): the cost is the six property reads of each of ~1,300 resource entries, which batching does not remove;
  the saving is under 1 % of one core at the price of changing a script that every page report depends on. Left open, low priority.
- Page-load engine knobs on claude.ai: need the owner's tablet A/B (ND2c), not guessable from here.

## 5. How to read the next upload (after alpha115 is installed and a Proton file has been tried)

`python3 -B -I tools/diag_digest.py`, then in the newest file look for: counters `download.handed.*` and `download.failed.*`, lines `download failed ...`, the FIRST START AFTER AN INSTALL section
(did the alpha115 install end the app 14-35 s later or not), and the Kills section.
