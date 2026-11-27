# OPT4: what the owner's first real Auto-tune run and the live diagnostics support (2026-11-25)

Owner, verbatim: "Whatever the tests show, and all the diagnostics sent, optimize the browser accordingly and release it".
Sources: tools/optimizer_digest.py (run 20261125-034537: 41 samples, about 30 min, alpha107, hardware drawing trial 1 ON in the baseline) and tools/diag_digest.py --days 7 (40 uploads, alpha81 to alpha107).
Rule kept: change only what a number supports; say what was NOT proven.

## 1. Changes made in alpha108 (each with its evidence)
| Change | Evidence | Where |
| :- | :- | :- |
| Hardware drawing trial 2 removed from Settings and retired from Auto-tune | processor time 25.2 s against 7.2 s per pass (3.5x), WebGL check failed (gl_ok 0) in both samples | SettingsView choice row, Prefs.drawTrial clamps to 0..1 (a saved 2 reads as 1), Catalog.RETIRED |
| Benchmark clips are allowed to start (muted) in the Auto-tune screen | clips_playing was 0 in all 37 samples: the press-to-play rule sets engine autoplay 5 and the session had no permission delegate, so the 8 clips never started and the clip arm repeated the blur arm | OptimizerRunner.openSession permissionDelegate |
| The clip arm says WHY when clips do not play (ready state, errors, refused play() names) | same | anim.html note |
| Run budget counts real work minutes (suite_s per sample + 0.4 per restart) | the plan estimated 4 min a step, 75 total; the run took 31 real minutes and skipped clips_free, fontations, jitoff as 'out of time' | core Machine.onSample / next |
| One odd sample cannot reject a setting: the loss must show in every sample | gcsmall rejected for 'processor time 30 % worse' from ONE sample (13.1 s, then 7.8 s, against 7.2 s) | core Decider.worst |
| Catalog version 2: every setting is measured again | the clip arm and the guards changed | Catalog.VERSION |
| digest counts a run sent twice once | the report arrived twice (automatic + button) | tools/optimizer_digest.py |

## 2. What the numbers say and no change was made (honest list)
- Hardware drawing trial 1 (the owner's baseline): animation arms 66 fps (the display's cap), 0.6 % late frames, scroll test 9100 px with 17 ms frames (app-driven scroll, mode 0), typing wait 30 ms, 26/26 compat, WebGL ok, 490 MB. It works well on these tests. NOT proven: that no text goes missing (the run skipped the question because it was already on). It stays an owner switch.
- Calmer redrawing (30 a second): 30 fps arms, typing wait 43 ms (+43 %), saves about 20 % processor time. Stays off.
- Inside the noise (about 5 %): scroll feel Standard / Smooth (-0.2 / -1.3 %), parallel GC marking -2.3 %, style threads 2 -3.0 %, second content process -0.4 % (start time 3.4 s against 4.3 s once, memory +14 MB: one observation, not acted on), compile-on-first-sight -1.0 %, cheaper gradients -1.3 %, no captcha watchers -0.4 %, no form fill -1.1 %, smaller nursery -1.1 %, defer decode of hidden pictures +1.3 %, new path drawing +0.8 %. No memory win anywhere (all 471 to 507 MB).
- Never measured: no limit on looping clips, Rust font reader, script compile off the main thread (the budget bug). They run in the next Auto-tune run.
- The tests are synthetic and small: script_ms 340 ms. The real pages that hurt (claude.ai: first paint 13 s, load 27 s, 'the page was too busy to answer for 90 % of its load', 827 requests) are bound by their own scripts. Nothing in the catalog can be shown to help them. A real-page probe is the missing instrument (OPT2).

## 3. Live diagnostics (7 days) - what is known and why nothing else was changed
- cold start to first paint: median 19 s (WATCH) but engine ready 1.1 s and activity 0.3 s: the time is the first page's own paint (a restored claude.ai tab), not the app.
- page animation: 61.5 % late frames on 47 pages: the sample is mostly software-drawing days; the digest's split by drawing setting has too few pages under hardware trial 1 to compare (fewer than 3 per group). Re-check after a few days with trial 1.
- memory: the engine was killed or crashed 5 times even with few tabs kept; free memory median 476 MB, 292 MB at the worst upload. No setting in the catalog reduced memory.
- Back: place missed 6 of 9, finished pages refused (PERF14 / research/INSTANT_BACK.md): open item, not touched.
- the press-to-play rule held 144 and refused 48 clips on parlay-api.com and nobody pressed: it is the owner's own rule (a per-site switch exists in the Shields panel).
- the advisor's 'turn Shields off' lines name carrier e-mail tracking links (totalwireless / tracfone mail hosts): blocked on purpose.

## 4. What was NOT run on the tablet
alpha108 itself; the clip arm with really playing clips; the three skipped experiments; the new budget (a run over 40 experiments-minutes); hardware trial 1's missing-text question.
