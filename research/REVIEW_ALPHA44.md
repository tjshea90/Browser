# Independent review of alpha44's page-facing ad code (2026-11-15) and what was done about it
ASCII only. Newest section last. Written the moment the findings existed.

## 1. What was asked and by whom
After alpha44 was recorded, a fresh reviewer (a separate agent that had not seen the design) was asked to try to BREAK two scripts that run on every page: the ad-layer part of `guard-main.js` (RX3-G4) and `players.js` (RX3-G7), with
real Playwright scripts, and to report only what it could reproduce. The reviewer's scratch scripts are in the session scratchpad (not kept in the repo); the repo keeps the regression tests below.
Why this matters: a missed ad costs a tap, a page's own control that stops working costs the page. False positives are worse than missed ads.

## 2. Findings (all reproduced by the reviewer) and the decisions
G4 version 1 (alpha44: at every touch on a player, switch off an INVISIBLE layer of the page itself that lies over it):
1. The "invisible" test missed visible or working elements: img/svg roots, icon boxes made of spans or glyph fonts, role=button divs, boxes visible only by border / box-shadow / backdrop-filter, short text.
2. `pointer-events:none` is inherited, so a switched-off container killed everything inside it, and it was never given back (a scroll swipe that merely started on it was enough).
3. "The player" was any canvas or iframe of 120x80 anywhere in the stack: a page-wide background canvas made every transparent control on the page a "layer over a player".
4. The wrapper protection covered only the video's direct parent, and ended for the whole document after ONE refused pop-up that had nothing to do with the layer.
5. A transparent click-outside backdrop of a menu or dialog died on the first touch.
6. A cross-site frame that was invisible at DOMContentLoaded and faded in a moment later ended visible but dead.
7. (older rule, not G4) `isOverlay`: an empty positioned link of 35 percent of the viewport and any cross-site link that large, even with picture and title, was refused as an advertisement layer: on the tablet's 564x916 a card of 564x330 is already dead.
players.js:
8. While the film's length was unknown, ANY finite stream was taken for an ad (the film got muted, sped up to 16x and moved to its end), and an ad stream whose class came a moment late was remembered as "the film", so the real film coming back while the class lingered 700 ms was moved to its end.
9. Two visible skip buttons were pressed in turns, 31 times each in 2.5 s.
Cost: the touch handler took 0.03-0.2 ms (2-9 ms with a dirty layout on a page of 4000-16000 nodes), the sweep 0.2-2 ms; no loop, no exception; both files parse as ES5 (the functions inside players.js's one try block are block-level declarations, which Gecko 160 allows).

DECISION for G4: the guess is gone. A layer of the page's OWN document is no longer switched off before a press at all (the assist already answers such a layer from EVIDENCE: a press that made the page open an ad, then it plays
the video or presses through at the same place). What stays needs no guess: an invisible FRAME OF ANOTHER SITE (opacity under 0.05 or hidden, positioned) that covers at least 60 percent of a player (a touch inside a frame never reaches the page's script, so only a look can find it), found by the looks at 0 / 1.5 / 4 / 9 s and at DOMContentLoaded and load; a
frame that is visible at a later look (or at the slow check, every 2 s for 30 s) is given back (the property is removed, not overwritten). This costs the "first press goes straight to the player" improvement for invisible layers of the page's own document: the assist still plays the video after the first press, as before alpha44. G4 is therefore NOT
what answers the owner's "first press plays" for a same-document layer; the assist (AP1, alpha39+) is.
DECISION for the older overlay-link rule: an empty positioned link is an advertisement layer only if it is for another site or lies over a player; a link of another site that fills the screen is one only if it has no words of its own.
DECISION for players.js: the film's length is learned only from a stream of 3 minutes or more, or after 3 s of playing without an ad class (a `timeupdate` check on tracked videos only); while it is unknown, only a stream under 3 minutes counts as an ad. The stream the session began on is remembered; when the element's stream changes while the class is still there (the next ad of a pod,
or the film coming back with the class lingering) what was done is undone at once and nothing is done for 1 s; the move to the end of the stream happens only once a stream has stayed the same for 0.9 s. A visible skip button that says skippable is pressed whatever the stream (it exists only in an ad's own bar) and counts as a skipped ad; every pressed button is remembered.

## 3. Proof (all in headless Chromium; each REVIEW test was run against alpha44's file and FAILS there, passes now)
- `web-tests/fakelayer.test.js` (rewritten, 8): own invisible layer keeps its taps; eight kinds of the page's own invisible controls keep working (toggle overlay outside the wrapper, backdrop, border-only box, span icon box, svg, img, role=button, touch pad over a canvas); cross-site invisible frame switched off and one tap reaches the player; a frame added at 1 s is found; a fading-in frame is given back; five kinds of frame that are not hostile stay; guard off touches nothing.
- `web-tests/players.test.js` (19): +4 REVIEW tests (ad before its class and the film returning while the class lingers; a 3600 s stream is never touched while the film length is unknown; learned only after 3 s; two skip buttons once each).
- `web-tests/guard.test.js`: +3 REVIEW tests (a big card with words to another site is a link; the empty stretched link of the site's own card is the card's link; an empty cross-site link filling the screen and an empty link over a player are still advertisement layers).
- `web-tests/assist.test.js`: one test waited 800 ms between two real clicks inside the assist's 1.5 s window (it relied on the old over-eager link rule to keep a link from being clicked); it now waits 2 s.
NOT done: nothing of this has run on the tablet or a real site; the emulator lab `/fake-layer` now shows the same-document layer taking the first tap (the guard refuses its ad, the assist plays the video) - it was not run again; the cross-site-frame path is test-browser only.
Lesson for the next reviewer round: ask for a REVIEW of every script that runs on every page BEFORE the release, not after (a page-wide script's false positives cannot be seen by its own lab, which is written by the same hand).
