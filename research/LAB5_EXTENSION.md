# LAB5 - what the built-in shields extension costs per page and per frame (audit 2026-11-17)

Read-only audit. Every ms / MB figure is an ESTIMATE (the repo has no per-script or per-frame timing). A normal frame executes 313 KB across 12 script runs (vfind.js twice), not 450 KB; the 458 KB is the whole
folder incl. background.js (83 KB), engine.js (25 KB), redirects.js (24 KB). Files: geckoapp/src/main/assets/kestrel-shields/.

## Content-script entries (all http(s) frames, 9 captcha-host exclusions unless noted)
| Files (KB) | run_at / world | Load-time work |
| - | - | - |
| vfind 11 + content 61 | start / isolated, no about:blank | 12 IIFEs (8 run in a sub-frame): `css` + `guard` messages, ~31 listeners, DOM media scan twice; top frame also 2 class/id scans + an observer |
| guard-main 24 | start / MAIN, about:blank too | patches window.open + form.submit, 8 listeners, `sweepFrames` 6 times |
| gate-main 37 | same | 10 media-prototype patches, a MutationObserver on the document for the page's life, 6 listeners |
| codec-main 12 | same | 4 patches, 1 listener |
| scriptlets-main 57 | same | defines ~212 functions + 1 listener; idle unless the site has calls |
| youtube 11 | end / YouTube hosts only | lazy |
| players 14 | start / isolated | 2 listeners. BUG below |
| seekbar 18 | start / isolated | 4 listeners |
| vfind again + videobar 33 + turn 8 + player 26 | idle / isolated | 4 + 7 + 1 listeners; top frame only: a 1.2 s `guard` ask, a 1 s `vpt` timer, an onMessage listener |

BUG (real, found by the audit): the players.js entry has TWO `exclude_matches` keys (manifest.json ~:176 and ~:187). JSON keeps the last, so only the YouTube list applies and the script runs inside captcha frames
(the ANS58 intent was to keep our scripts out of human-check frames). OwnerRulesTest.kt:559 passes anyway; background.test.js:1162 pins the YouTube-only list.

## Sub-frames (~5 ms per frame, range 3-8, and 0.3-0.8 MB; 20 frames = 60-160 ms of content-process main thread + 6-16 MB; the top document adds 20-100 ms)
Needed in every frame: guard-main, content's guard relay (pop-up report, tap evidence, flags to the page-world scripts), `css` (site rules, almost always empty). Only with a film: vfind, videobar, turn, player, seekbar,
players, content's video-facts / long-press / media-scan code, codec-main, the gate's real work (its observer must still watch every frame). Top frame only: 4 IIFEs, the token scan, turn's `vpt`, videobar's `ask`.
Only hosts with scriptlet calls: scriptlets-main. Do NOT size-gate frames with `innerWidth` (forces a layout flush of the parent).

## Request and cosmetic path
6 webRequest listeners: `onBeforeRequest` (blocking, every request; `engine.check` averages 184-252 us in the lab, 723 us on the tablet in alpha27; no app message once the engine is loaded), `onCompleted` + `onErrorOccurred`
(every request, diagnostics only), `onBeforeSendHeaders` (media, XHR, other, object), `onHeadersReceived` (those + frames, for the sniffer), a second blocking `onHeadersReceived` for `*.m3u8*` only.
Per main frame ~12 Map operations, a `cfg` message, `pend`; hosts with early scriptlet rules add an `sc` message and a wait of up to 250 ms; up to 3 `rep` messages 4 s later; an `st` message every 5 s while requests flow.
CSS: the top document gets ~17 KB (marker, site rules, 478 tag-led generic selectors); frames get site rules only; the other 28,555 generic selectors arrive lazily through `gen` batches.
Budgets: token observer 300 ms / 20 s, procedural rules 1500 ms / 40 runs, css cache 200 entries / 60 s with in-flight dedupe.

## Ranked changes
| # | Change, edit point | Est. gain per page | Risk | Proof |
| - | - | - | - | - |
| 1 | ONE `runtime.onMessage` dispatcher keyed on `m.k`, replacing the 24 listeners at background.js:243,320,375,538,632,655,673,901,915,925,943,950,957,969,980,992,1002,1016,1029,1040,1052,1061,1070,1078 (as recalled, Gecko deserialises the message and clones `sender` once per registered listener: unverified) | 15-45 ms parent thread on a 20-frame page; ~5 ms on a light one | very low | background.test.js (100) + an assertion of ONE listener; lab: 20-frame scene, parent-process CPU |
| 2 | Manifest hygiene: merge the two exclude lists (:176-191), drop the second vfind.js (:239), optionally merge entries 9 -> 4 | closes the captcha hole; 0.3-0.8 ms per frame | low | a JSON test that rejects duplicate keys; update background.test.js:1162-1164 and OwnerRulesTest.kt:123,228 |
| 3 | Token scan (content.js:42-45, 56-58): two synchronous passes (DOMContentLoaded + load) over up to 6000 elements, 3 Xray crossings each, OUTSIDE the 300 ms budget: skip repeated raw class strings, run the load pass in `requestIdleCallback`, count it in the budget | 20-50 ms per 3,000-element page | low | content.test.js "tokens:" (6), observer.test.js; new 5,000-node case with a getAttribute spy |
| 4 | scriptlets-main lazy body: wrap :29-1017 in `build()` called from the event handler at :1063 (code stays in the page: no first-visit race) | 0.4-1.2 ms + 0.1-0.3 MB per frame incl. about:blank | low | scriptlets.test.js + "no event, nothing built" |
| 5 | Video group boots only when a film can exist (manifest.json:238-243, content.js:195-326, 604-646, 651-674; stub in sub-frames boots at idle if the frame has video/audio, else on a one-shot play / loadedmetadata / fullscreenchange / first trusted pointerdown replayed; keep the overlay-frame `ft` relay; top frame unchanged) | 1-2.5 ms + 0.2-0.5 MB per film-less frame = 20-45 ms per page | MEDIUM (first tap, players.js first play, shadow-DOM films) | videobar / turn / player / seekbar / players / handover tests + "film created late"; lab vp + ho + an IMA-overlay scene |
| 6 | `sweepFrames` pre-filter (guard-main.js:304-316): check `getElementsByTagName('iframe')` first; the `[class*="player" i]` query only when a frame is positioned and hidden (runs 6x per frame, each a whole-DOM walk) | 3-10 ms on iframe pages in the busiest second | low | fakelayer.test.js, guard.test.js (36) |
| 7 | Fewer frame `css` hops (background.js:629,636,642-646): keep empty `f|host` answers 10 min while the list generation is unchanged | ~5-15 UI-thread tasks avoided (0.3-5 ms each) | low | count Health `cosmetic css` lines per page |
| 8 | Verdict LRU (background.js:566-578): key host + type mask + url, raw verdict, cleared when the engine is replaced, ~2000 entries | hit rate x 184-723 us (40 % of 150 requests = 10-40 ms); hit rate unknown: ADD hit/miss counters first | low | cached == uncached over the engine.test.js corpus |
| 9 | Procedural budget 1500 ms / 40 runs -> ~400 ms / 15 (content.js:140,146); drop `characterData:true` (:151); read `textContent` only for small nodes (:107) | caps a 1.5 s worst case; typically 10-50 ms | low-medium (a late ad may stay) | content.test.js "procedural:" (8) + a budget test |
| 10 | Listener diet (RX3-S2b): restrict `onCompleted` / `onErrorOccurred` types, thin `snd` (:759-773) | 3-10 ms parent thread | medium (Page report, Back accounting) | act only after the owner's `lt` line shows > 300 calls/min and > 150 us |

Measure first (small, no risk): a debug-only `performance.now()` pair around each script's top level; `fr`, `msg`, `cssN` counters in `lstText()` (background.js:157, the `lt` field is capped at 240 chars);
a lab scene with 20 cross-origin frames; the page-health probe already reports iframe counts per top page in debug builds.

Not recommended yet: splitting content.js only for the top-only IIFEs (they return at once, < 0.1 ms per frame); the 17 KB top-only stylesheet (parse 1-3 ms; Stylo matching unmeasured); merging `css` and `guard` (J1.1).
Checked, already fine: the engine check path (pageHashes memo, zero-copy views, bounded Maps, no per-request message), passive touch/pointer listeners, the DOM media scan, the token observer's budget / hidden-page / editable skips,
the css cache, no generic CSS in frames, lazy one-shot timers (no setInterval or rAF at load), header loops, engine start from the saved copy in 111-213 ms, early scriptlet registrations (LRU of 40);
the gate's document MutationObserver cannot be thinned without breaking "nothing loads until pressed".
