# TP1 (wrong tab preview) and UC1 (copy button on the address bar), owner message 2026-11-18

## TP1: "One of my tabs has the wrong preview. It shows the tab thumbnail for a site i went to before. Make sure the tab previews are actually for the site in the tab."

Causes found by reading the code (BrowserActivity.captureThumb, ThumbStore, Tab):
1. The saved preview (`cache/thumbs/<tab id>.webp`, and `Tab.thumb` in memory) belonged to a tab NUMBER, not to a page. Nothing recorded which page it showed.
2. Nothing cleared it when the tab navigated: a tab that went from page A to page B kept A's picture until some later capture replaced it, and a tab that was left before that capture (or a restart in between) showed A for ever. After a restart the grid loaded the file by tab id.
3. `captureThumb` asked for a picture, and when it arrived (after the GPU read-back and a resize on the IO thread) it stored it on the tab without asking whether the tab was still on the page it was asked for, or was loading the next one (a page that is still loading may show the previous page's last frame under its new address).

Fix (alpha70):
- core `ThumbRule`: `pageOf(url)` (= PageIdentity: host, path, query, no fragment), `accept(...)` (same page, no new load started, page settled), `stillShows(storedFor, nowUrl)`.
- `Tab.url` setter: a new page takes the old preview away at once (`thumb`, `thumbAt`, `thumbPage`); the periodic recapture then takes a new one (it waits for 15 s from `thumbAt`, which is 0 again).
- `ThumbStore.save/load` keep a `<id>.page` record beside the picture; a preview whose record is missing (every preview written before this rule) or differs is deleted and not shown.
- `captureThumb` checks `ThumbRule.accept` when the picture arrives; a late picture is thrown away (Health `tabs thumb-dropped`, counter `tabs.thumb.dropped`).
- Diagnostics: a line "Tab previews: N tabs, K with a preview, W for the WRONG page (must be 0); late pictures thrown away ...": if a wrong preview ever shows again the owner's report names it.
- Tests: core ThumbRuleTest 4, app TabPreviewTest 6; lab scene tb (real engine).

## UC1: "On urls, include a fast way to copy any url in the address bar. For example, a copy button pops up when i press on the url bar."

- The app's address bar is the BottomDock pill (the `Toolbar` class is not used by either build). A `CopyChip` (icon + "Copy") sits at the right end of the pill, between the address text and the shield chip.
- It shows only while the bar is pressed (focused) and has text; it never takes the focus, so the keyboard stays. One tap copies exactly what the bar holds: the whole address (BrowserActivity.onUrlFocus fills the field with the full address and selects it), or what was typed or pasted. The button says "Copied" for 1.3 s (a timer) and gives a short tick.
- Tests: Robolectric DockCopyTest 6; lab scene tb (`copyurl`: press the bar, tap Copy, compare the clipboard with the tab's address).
- Not done: Copy for the Home screen's search box (it is a search box, not the address bar); Paste / Share chips (Android's own selection menu on a long press still has them).

## PV1 (new private tab flashed an old regular tab's page), owner message 2026-11-18

Cause (read from the code): the app has ONE GeckoView, attached once and never removed (a design rule: detaching it caused the typing freezes); tabs swap with `setSession`. The view keeps showing the last frame of the session it showed before until the new session's compositor has drawn. A new tab starts on the Home screen with NO session; typing an address creates the session, `attachContent` swaps it onto the view and hides Home in the same pass: for a moment the previous tab's last frame (here a regular tab's page, on a private tab: a privacy leak of the screen) is what the view shows.

Fix: core `CurtainRule` + a plain curtain view in the page background colour (the Home screen's colour, so Home -> page looks like one step), above the page view and below the restore overlay, Home and the error screens. Up when the view is handed to a session that has not painted yet (`GeckoTab.painted`, set at the first contentful paint) or to the other kind of tab (private <-> regular); down at that session's first contentful paint (or first frame for a live tab, page stop, 3 s / 0.8 s at most, tab closed, another tab shown). Health `tabs curtain-up tab= why= private=` / `curtain-down why=`, counter `tabs.curtain`.

Proof: core CurtainRuleTest 3; lab scene tb (a private tab opened over a regular tab must raise `curtain-up why=UNPAINTED private=true` and drop it at the first paint; back to a regular tab raises one too). NOT proven: the look on the tablet (that no old pixel shows) - the lab proves the mechanism engaged, a screenshot at the right instant is not reliable.
