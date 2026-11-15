# VC1 + LP1 (owner message 44, 2026-11-15): a video with no controls, and a long press on a video that opened the floating panel

Owner's words (INBOX.md): "https://finance.yahoo.com/quote/TTWO/ I played a video on this site in Kestrel, and the player had no controls at all. I couldn't pause or stop the video or seek.
When I long pressed the video, the floating notification panel appeared. Anywhere I can start a video in the browser, I should be able to control it. Also, the download options should come up
for videos when I long press them instead of the floating notification panel."

## 1. What the page is (probe with headless Chromium in the sandbox, Firefox-for-Android tablet user agent, 564 x 916; the module shows in about 1 of 4 page loads, so it is probably an A/B or live-only module)
- The player is a web component: `DIV.evp-player` is a shadow HOST with an OPEN shadow root. Inside it: `DIV.evp-shaka-modern-ui` > `VIDEO#shaka-player` (blob: / MSE, `controls` false, muted, paused while the ad runs),
  `DIV.shaka-controls-container` (pointer-events none; its bottom bar is 5 px / 0 px tall while an ad plays, the play button is 0 x 0), `DIV.shaka-client-side-ad-container` with the ad's own
  `VIDEO` (a Google IMA MP4 from gcdn.2mdn.net, 10 s, muted, PLAYING) and an `IFRAME#goog_...` of 532 x 269 laid over the picture, and `DIV.evp-ad-countdown` (the "Ad" counter).
- So while the page's video ad plays there are NO controls of the page's own: Shaka hides its bar during an ad, the ad's own buttons live in the IMA frame, and a tap on the picture lands in that frame
  (click-through; in the probe a tap paused the ad and the play button's label changed to "Play", with nothing visible to press).
- `document.elementsFromPoint()` over the player returns the shadow HOST and its light-DOM ancestors, never the `<video>`; `document.querySelectorAll('video')` finds none.

## 2. Why the long press opened the floating panel (LP1)
Every video feature of Kestrel looks for videos with `document.getElementsByTagName('video')` or `elementsFromPoint`: content.js (long press on a video = "Save video" sheet, X14; the open-space classifier of
the floating panel, AY2), seekbar.js, players.js, the finder facts (`vid`). A video inside a shadow root is invisible to all of them, and the media events (`play`, `playing`: not composed) do not leave the
shadow root, so the capture listeners on `document` never see them either. The long press therefore looked like a press on empty space: panel. (If an IMA frame lies over the picture the press lands in that
frame and the frame's own classifier says "open space" too.)

## 3. Design
- `vfind.js` (content script helper, all frames, loaded first): `KestrelVF.at(x, y)` = the best video under a point, found through the elements in `elementsFromPoint` AND the shadow roots of those
  elements (open or closed: Firefox gives content scripts `openOrClosedShadowRoot`) AND the light-DOM videos that `pointer-events: none` hides from the stack, chosen by rectangle (visible, at least 80 x 45 px),
  playing first, then the larger. A scan is bounded (5000 elements, 4 shadow levels).
- LP1 (content.js): both long-press handlers use `KestrelVF.at`: a video under the finger (even under a layer, even in a shadow root) = the existing "vmenu" message = the Save video sheet (finder list, "Copy video
  address", or the toast that says why nothing was found); a video press never counts as open space. A long press inside a SUB-FRAME first asks the top frame (`ft` -> background -> `tabs.sendMessage` frame 0)
  whether a frame of that size covers a video; only when not does the frame send the open-space message.
- VC1 (`videobar.js`, all frames): a tap on a video (top frame, or relayed from a sub-frame that covers it) is judged 450 ms later: if the page shows no control of its own over that video (a visible button / slider /
  progress bar with a control-like name, native `controls`), Kestrel draws its own bar: play / pause, a seek track (tap and drag) with the time, mute, full screen. The bar is a `position: fixed` closed shadow root
  on the document element (above frames and the page's layers), hides after 4 s, never shows on a page that has controls (Settings: Automatic / Always / Off, page-config flags 64 / 128).
- Counters: Diagnostics line "Video controls".

## 4. Status
See TASKS.md VC1 / LP1. Tests: web-tests/videobar.test.js, web-tests/openspace.test.js (video cases), lab scenes in tools/emu/soak.py (page /video/shadow of tools/sitelab/soaklab.py).
