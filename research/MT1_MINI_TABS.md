# MT1: switch, close and open tabs from the floating navigation panel (owner message 2026-11-18)

Owner, verbatim: "In the floating navigation panel, include an intuitive way with an intuitive gui to switch between, close, and open new tabs from the mini panel. There are many ways this May be built, make a good gui for it"

## Constraints (read from ui/MiniNav.kt, research/MINI_NAV.md)
- The panel is small (176 dp wide), black, drawn by a few plain views (no ripple, no animators: the owner runs with animations off), floats, is picked up by a long press (400 ms) anywhere except the keys, sits on the page without taking the page's touches outside itself.
- Rows: [Back][Home][Forward] (52 x 42 dp), the address bar (34 dp), the keys (only while typing). Height and geometry are computed from constants (`outlineH`).

## Options considered
1. A tab STRIP of tiny thumbnails in one row: too small to read on 176 dp, closing needs a tiny target. Rejected.
2. Swipe left/right on the address bar to flip tabs: fast but invisible (nobody discovers it) and no way to close or see what is open. Kept as a possible later extra, not the main design.
3. A TAB TRAY inside the panel (chosen): one extra button, a short vertical list with a thumbnail, title and host per tab, a close mark on every row, a New tab row at the bottom. Discoverable (a button with the count), one-handed (rows are 40 dp, the close mark is a 40 dp target on the right edge, away from the row's switch target), consistent with the tab grid.

## Design (chosen)
- A 4th nav button TABS at the right end of the row, with the number of tabs in a small badge. The panel becomes 208 dp wide and the buttons 48 x 42 dp so that four fit.
- Tapping TABS opens the tray between the buttons and the address bar (it pushes the bar down); tapping it again, the grip, or the address bar closes it. The tray and the keyboard exclude each other.
- The tray lists the tabs of the same kind as the tab in front (regular with regular, private with private - the same split the tab grid uses). Each row (40 dp): a 34 x 26 dp preview (the tab grid's preview: TP1 makes sure it is the right site's; a letter chip when there is none), the title (one line) and the host (smaller, muted), a x at the right edge. The tab in front has an accent outline. Up to 5 rows show; more scroll by dragging the list.
- TAP a row: switch to that tab and close the tray (the page is what the person wants to see). TAP the x: close that tab, the tray stays and the row goes (closing several is common). The last tab closing leaves a new empty tab, as everywhere in the browser. TAP "+ New tab" (last row, always visible): a new tab opens in front and the panel's keyboard opens on the address bar so that the address can be typed at once.
- A long press on the tray does NOT pick the panel up (like the keys: the list owns its touches); a long press on the buttons, the bar or the frame still does.
- No animation (rows appear and go at once). The pressed row lights in the accent for a moment (a timer, like the buttons).

## Pieces
- core `ui/TabTrayModel` (pure: row geometry, scroll, hit test: Row / Close / New / None) with tests.
- app `ui/MiniNav`: Listener `navTabs() / navSwitchTab(id) / navCloseTab(id) / navNewTab()`, the TABS button with its badge, `TrayView`, `tabsChanged()`; BrowserActivity implements the four calls from its tab list and calls `tabsChanged()` whenever the tab list, a title or a preview changes.
- Tests: core model; Robolectric for the panel (button opens the tray, a tap on a row switches, the x closes, New tab opens the keyboard, the tray closes with the panel, the geometry grows by the tray's height); lab scene on the real engine (three tabs, switch, close, new).

## Not proven until the tablet: how the 48 dp buttons and 40 dp rows feel under a thumb; the owner may want the tray to stay open after a switch (one line to change).
