# What the built-in extension costs the tablet (RX3-S2 .. S5) - notes and measurements
Newest section last. ASCII only. Review text: research/REVIEW-EXTERNAL-3.md section 9.

## 1. RX3-S3 - a time budget for the class/id token observer (alpha44, 2026-11-15)
Problem (read in `content.js`, D2): on every top document whose generic-hiding CSS starts with the marker, a `MutationObserver` watches `{childList, subtree}` of the whole document; each callback walks up to 200 records x 50 nodes and `querySelectorAll('[id],[class]')` (up to 300 nodes) on every added element. It stopped only after 60 batches or 8000 names (`off`), so on a page that keeps adding nodes with already-seen names it never stopped and cost page-main-thread time for ever. The procedural observer next to it already had a budget; this one had none.
Change: `BUDGET_MS` 300 ms of the callback's own work in total, `WINDOW_MS` 20 s after the page was parsed (whichever comes first -> `off`, observer disconnected); no work while `document.hidden` (a `visibilitychange` back to visible scans the document once and sends what is new); records whose target is inside an editable region (`isContentEditable`) are skipped (typing in a rich editor). The first scan (`first`, at load) is not counted. Tests shorten the limits through `window.__kstTokCfg`.
Proof: `web-tests/observer.test.js` (5, real Chromium): the budget (forty callbacks of real work: early names sent, the last burst not), an ordinary page still sends a late name, the 20 s window (shortened), hidden page and the look when shown, an editable region.
NOT measured: how much of the 300 ms real pages use (Diagnostics has no counter for it yet); S2 (listener counts and the listener diet), S4 (blocker cost, cold/warm) and S5 (local "no cosmetic rules" answer) are still counters-first rows that need the tablet.

## 2. RX3-S2 - counters for the extension's five web request listeners (alpha45, 2026-11-15)
Why counters first: the review (section S2) says every request of every page runs through five listeners on the parent process's main thread and wants three of them thinned, but nobody has a number for what they cost
on the tablet. Thinning `onErrorOccurred` / `onCompleted` (they feed Diagnostics "Page report" and the failed-file list, XHR failures included) or `onBeforeSendHeaders` (Referer/Origin of a possible video) costs
diagnostic value and header knowledge, so it is only worth doing for the listener the numbers name.
Change (`background.js`): `LST` = per listener [calls, microseconds] for `req` (onBeforeRequest, the blocking one), `rcv` (onHeadersReceived), `snd` (onBeforeSendHeaders), `cmp` (onCompleted, shown as "done") and `err`
(onErrorOccurred); `lstTimed(slot, fn)` wraps each listener (try/finally around the real function, the return value of the blocking one is passed through unchanged, `performance.now()` with a `Date.now()` fallback);
`lstText()` -> "req 120 (5400 us), rcv 14 (200 us)" (a listener that never ran is not mentioned); sent as its OWN field `lt` of the `st` message every 5 s (the existing lazy timer). It is deliberately NOT part of `w`: the
app logs `w` into the Health ring whenever it changes, and these numbers change at every report (they would push everything else out of the 512-line ring).
App side: `ShieldsBridge.EngineStats.listeners` (`m.optString("lt").take(240)`), `statsLine()` (Diagnostics "Blocker" line) says "Extension listeners (calls, time held): req 120 (5400 us), ...".
Proof: `web-tests/background.test.js` "RX3-S2" (the engine loaded, four judged requests plus the page, one headers event: `req N` with N >= 4 and a microsecond figure, `rcv 1`, no `snd` because it never ran, and the numbers
are not in `w`); the whole background.test.js (79) is green, so the wrapped blocking listener still answers exactly as before. `:geckoapp:compileDebugKotlin` is green. The app-side text is compile-checked only (the
Gecko module has no unit tests: `ShieldsBridge` needs org.json and an Android context).
NOT measured: nothing from the tablet yet. What to read in the owner's Diagnostics after a Proton Mail session (the review's typing-lag case): the "Extension listeners" part of the Blocker line. Rule of thumb to act on:
a listener with more than about 300 calls per minute of browsing AND more than about 150 us per call (calls x us is the total it held the thread) is the diet candidate; `err`/`done` are the cheapest to thin (change their
`types`), `snd` next (only `media`, plus XHR/other whose URL has a media extension), `rcv` last (it carries the video finder). Timer precision: Firefox may round `performance.now()` to 1 ms for extension pages, so a
single call can read 0 or 1000 us; the TOTAL over many calls is the number to trust, not one call.
The diet itself is the separate open row RX3-S2b.
