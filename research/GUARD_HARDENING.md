# Guard hardening: tap evidence and the frame assist (RX3-G2, RX3-G3)

Rows: RX3-G2, RX3-G3 (research/REVIEW-EXTERNAL-3.md part G); G4-G6 will add their sections here. Newest first. ASCII only.

## 1. G2 - the owner's tap must not be forgeable by the page
Before: the page-world guard reported every real tap with a DOM event (`__kst_pop_v1`, detail `tap|<link>`), content.js forwarded ANY such event, the app stored `lastTapAt/lastTapHref` and let a jump through when the tap was fresh and named that very address. A page script can dispatch the same event: `tap|<its ad address>` followed by `location = <its ad address>` was ALLOWED.
Now (alpha41, `content.js` + `guard-main.js`; the app is unchanged, the same message arrives):
- content.js (the isolated world) records the owner's taps itself: a trusted `click`, or a trusted primary-button `pointerup` within 500 ms and 12 px of its `pointerdown` (a page that cancels the click still gets the tap counted). A long press, a right click, a drag and a swipe are not taps. The same link twice within 700 ms is one tap. Untrusted (script) events never count.
- What the page-world guard may report through the DOM event is whitelisted: `open`, `click`, `overlay`, `form`, `redirect` (it refused something). `tap` and `vmenu` arriving through it are dropped (`vmenu` is sent directly by content.js).
- `guard-main.js` no longer reports taps at all (it keeps its own local record for `window.open`).
- Proof: `web-tests/tap.test.js` (14 tests: forged events, untrusted clicks, touch, cancelled click, swipe, long press, right click, drag, frames, hostile details) and the changed test in `guard.test.js`. The end-to-end lab "fake the tap, then jump: STOP" belongs with G5/G6 (the app-level wall and the late-jump rule).

## 2. G3 - no channel from a page into the frames it embeds
Before: every frame's guard obeyed `postMessage('__kst_assist_v1|x|y')` from `window.parent` and then played its video or dispatched pointer events at (x, y) inside the frame: ANY page could click buttons, same-site links and script-handled elements inside ANY cross-origin frame it embeds, with no action of the owner.
Now:
- The parent's guard says "go" with a parameterless DOM event (`__kst_assist_v1`) and posts nothing.
- The parent's content.js decides from ITS OWN trusted press record (G2): the press must be at most 2 s old, at most five go-signals per press, and the frame (the `<iframe>` under the press, found with `elementsFromPoint`) and the place inside it are worked out from that press, never from the event. It sends `{k:'assist', x, y, s: iframe.src}` to the background page.
- background.js relays to the ONE child frame of the sender (by its address in `frameDoc`, or its only child; two identical embeds: no guess) with `tabs.sendMessage(tabId, {k:'assist', x, y}, {frameId})`: a channel a web page cannot use.
- The frame's own content.js plays its biggest video that has a source (muted if sound is refused) or, when the player has no video yet, presses through at that place with pointer events. It never presses on a link or a form control (A, BUTTON, INPUT, SELECT, TEXTAREA, LABEL, OPTION, SUMMARY, anything inside a FORM, editable content). Same-frame help stays in guard-main.js unchanged.
- Residual risk, said plainly: a malicious page can still put a transparent layer over a frame so that the owner's own tap lands on the layer, and then fake a refusal; the assist would replay that one tap into the frame (non-control elements only). It needs a real tap of the owner on that spot, within 2 s, at most five times per press. Before this change it needed nothing.
- Proof: `web-tests/assist.test.js` (9 tests: the old tag posted into a frame does nothing, a forged go without a press sends nothing, a stale press sends nothing, the five-per-press cap and the place coming from the press, the legitimate player-in-frame cases from AP1 ported, never on a button or link, guard off, only the extension channel), `guard.test.js` (the guard says "go" and posts nothing), `background.test.js` (4 relay tests). Emulator proof of `tabs.sendMessage` in GeckoView: see section 3.

## 3. Emulator check of the relay in GeckoView 160 (2026-11-14; Android 10 x86_64 emulator, debug build; NOT the tablet)
Lab: `tools/sitelab/guardlab.py` pages `/assist-parent` (a cross-origin frame, the same server under the host name 127.0.0.1, holding a real tiny silent media element, with a transparent layer over it that opens an ad on a tap) and `/assist-child`. A REAL tap (`adb shell input tap`) on the layer:
- the layer's handler ran and called `window.open` to the other host: refused, no request reached the server, the blocked-pop-up chip was shown;
- the extension's state line (Diagnostics, and Health `shields ext-state`) read `assist 1/1`, then `assist 2/2`: the parent's content.js asked, the background found the child frame in its table and `browser.tabs.sendMessage(tabId, msg, {frameId})` RESOLVED: **GeckoView 160 supports it**;
- the frame logged `child play event: the video STARTED`: the frame's own content.js started the video with the engine's native `play()`.
Two lab lessons worth keeping: (1) the ad address of the lab layer must be cross-site, else the guard rightly allows it after a real tap (same-site windows are allowed); (2) a content script's `video.play()` goes through the engine's own wrappers, so a page script that stubs `HTMLMediaElement.prototype.play` cannot see it (the unit tests share one world, so they can): observe the media element's `play` event instead.
Not run: a real movie site with a player frame on the tablet.
