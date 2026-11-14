# The blocked-page icon and the one-hand mini navigation panel (AR1, AR2; owner message 32, 2026-11-13)

> "When Kestrel blocks a pop up, instead of the notification on the bottom, put a very small icon in the bottom middle that is not distracting that symbolizes a page blocked, and have it disappear in one second. If I press the icon before it disappears, it still open the blocked page in a new tab without any further prompts."
> "My tablet screen is big, and I sometimes browse with one hand only at the top of the tablet. Make a small quick menu at the top right corner of the screen that is black ... back button on the left, home button in the middle, a forward button on the right, and a bar underneath which when I press it, it selects everything in the url bar and puts a small floating keyboard (or docked to the top right) ... There should be a small toggle at the top right ... slides down ... slides back up ... a very quick light up neon blue ..."

## AR1 - the blocked-page icon (`ui/BlockedChip.kt`, `ic_page_blocked`)

* Replaces the "Blocked a pop-up (host)  Open  Dismiss" bar (`NoticeBar`, 7 s, deleted) and the "Pop-up blocked" toast of the engine's own pop-up prompt (`TabDelegates.onPopupPrompt`, which now also counts the page like every other blocked pop-up).
* ONE icon (a window with a slash), 28 dp inside a 44 dp touch area, bottom middle of the page area, quiet colours (black 80 % disc, mid-blue hairline, muted icon). Shown by `BrowserActivity.onPopupBlocked`; gone 1000 ms after the LATEST block (a second block inside the second restarts the clock; the newest address is the one a press opens).
* A press opens the blocked address in a NEW tab at once (`newTabInternal` + `show` + `navigate`), no sheet, no question. A forced redirect (the page tried to send THIS tab away) is opened the same way, so the page the owner is on stays where it is (before: the notice's "Open" replaced the page).
* An address that is not http / https (an empty window) still shows the icon, and a press only dismisses it.
* Events for Diagnostics: `guard chip-show <kind> <host>`, `guard chip-open <host>`.
* Tests: `BlockedChipTest` (5): one second, restart on a second block, newest wins, one open per press, unopenable address.

## AR2 - the mini navigation panel (`ui/MiniNav.kt`, core `input/MiniKeyboard.kt`)

What it is: a small tab ("toggle", 44 x 26 dp) at the very top right of the page area, always there unless Settings > Appearance > "Mini navigation tab (top right)" is off. Pressing it slides a black panel (176 dp wide) down from the top: **Back** (left), **Home** (middle), **Forward** (right), each 52 x 42 dp, and under them an address bar. Pressing the bar selects the whole address and opens the app's OWN small keyboard under it (the panel grows to 304 dp, anchored at the top right). The toggle slides the panel back up. Open / closed is remembered (`Prefs.miniNavOpen`).

Decisions and why:
* **The keyboard is drawn by the app.** Android lets only the keyboard's own settings move or shrink it; an app cannot ask for a floating keyboard. So `MiniNav.KeysView` draws 4 rows (letters with `/`, `.`, `.com`, space, Go; a `123` page with the digits and `- _ : ? = & % # @ +`, `~ ! * ' ( ) , ;`; shift; backspace that repeats while held) and `core/input/MiniKeyboardModel` decides what a key does (the first key replaces the selected address, Backspace clears it, Go opens it or searches Google exactly like the normal address bar, because it goes through the same `navigate()`).
* **Own clock, not Android's animators.** The owner runs with animations turned off (the neon glow was changed for the same reason, see `Neon`): a system animator would jump to its end. The slide is stepped by `postOnAnimation` from `SystemClock.uptimeMillis()` (200 ms, decelerate in, accelerate out), the lit state of a pressed control is a 130 ms timer. `MiniNavTest` runs the slide with `ValueAnimator`'s scale set to 0 and steps the clock by hand.
* **Neon feedback.** Back / Home / Forward, the bar, the toggle and every key light up in `Pal.accent` (the app's own neon blue; violet in a private tab) for 130 ms when touched, drawn in `onDraw`, no ripple object.
* **Touches.** MiniNav is not clickable itself: only the toggle, the panel's views and (when open) the keys take touches; everything else in the corner belongs to the page. The panel is hidden in full-screen video and the Back key closes the keyboard first (`handleBack`).
* **Buttons act like the dock's:** Back = `handleBack()` (never sends the app to the background from this panel), Home = Home screen (does nothing when Home is already shown, so it does not raise the phone keyboard), Forward = `goForward`. Back and Forward dim when there is nothing to go to; Home is lit when Home is on screen. The bar shows the page's address (tidied: no scheme, no `www.`, no trailing `/`), empty on Home.
* **Gecko build only** (`Flavor.hasMiniNav`); the WebView build (Kestrel Light) has no panel.

Tests: `MiniKeyboardTest` (core, 9), `MiniNavTest` (Robolectric, 13: slide on its own clock, buttons, flash, dimming, bar selects all, first key replaces, Go, shift and symbols, Back closes the keyboard, hiding closes it, unavailable = gone).

What only the tablet can say: whether 28 x 40 px keys are comfortable for the right thumb (the key size is `KEYS_H_DP` and the panel widths `PANEL_*_DP` at the top of `MiniNav.kt`), whether the toggle in the corner gets in the way of a site's own top-right buttons (then it can be switched off in Settings), whether the slide looks smooth on the real GPU.
