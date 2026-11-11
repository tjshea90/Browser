# Kestrel design system - "CERE"

Decided 2026-11-11 (owner: "greatly improved GUI", "fresh, not generic AI design", "big easy-to-press buttons", "no animations", "move the navigation bar to the bottom",
"black home screen with one large stylized field", "private tabs separate, with an indicator", "tab switcher with live previews").
Benchmark notes behind the choices: `research/UI_BENCHMARK.md`. Owner decisions still open are in `TASKS.md` (UI-2, UI-3).

## Idea
A kestrel has a yellow **cere** (the patch at the base of the beak) on a dark, sharp-edged bird. The app is the same: ink black surfaces, ONE accent (cere yellow `#F6C026`),
and one shape - a rectangle with 45-degree **cut corners** (top-left and bottom-right), like a feather tip. No pills, no circles, no gradients, no shadows, no animation.
Private mode swaps the accent and the chrome to violet so it can never be mistaken for a normal tab.

## Tokens (`app/src/main/res/values/colors.xml`, `ui/Ui.kt` `Pal`)
| Token | Regular | Private | Use |
| :- | :- | :- | :- |
| window | `#000000` | `#000000` | Home screen, behind everything |
| chrome | `#0D0E10` | `#130D21` | dock, bars, sheets |
| pill / raised | `#1B1E23` | `#231939` | address field, tiles, buttons |
| card | `#15171B` | `#1A1230` | tab cards, lists |
| divider | `#272A31` | `#33264F` | hairlines, outlines |
| fg / muted | `#F2F3F5` / `#8D929C` | `#F2ECFF` / `#A797C8` | text, icons |
| accent | `#F6C026` | `#B794FF` | primary action, selection, progress, focus edge |
| danger / warn / ok | `#FF6B6B` / `#FFB454` / `#5BD38D` | same | state only |
Dark only (the owner uses the tablet dark; a light skin is a later option). System fonts only (Roboto): 16 sp body, 15 sp labels, 20 sp Home field, bold for emphasis.

## Shape and touch
- `ChamferDrawable` (`ui/Chamfer.kt`): cut corner 8-18 dp depending on size. Pressed state = the fill moves 14-16 % toward white/black **instantly** (`chamferSurface`, `ripple`).
- Targets: every button is at least 48 dp tall; the six dock buttons are ~94 dp x 56 dp on the tablet; tab-grid buttons 48-56 dp.
- Icons: hand-made 24 dp vectors, 2 dp stroke, square caps, mitred joins (`res/drawable/ic_*.xml`), tinted at runtime. Adding one: copy an existing file.
- **No animation anywhere**: window animations off, state lists with 0 fade, sheets and grids appear in one frame, the keyboard lift jumps.

## Screens
- **Dock (bottom, `ui/BottomDock.kt`)**. Portrait: address row (mask if private, lock/warning, address, Shields chip, reload/stop) over a row of six buttons: Back, Forward, Home, New tab (+),
  Tabs (count, chamfered square), Menu. Width >= 760 dp: one row. While the address is typed the activity lifts the dock above the keyboard with `translationY` (the page view is never resized:
  `research/FREEZE.md`), the address row sits on the keyboard, the suggestion list sits on the address row with the best match nearest the bar. A 2 dp accent progress line sits on top of the dock.
- **Home (`ui/HomeView.kt`)** = new-tab page and the Home button. Pure black, wordmark (+ PRIVATE badge), one 72 dp chamfered field with a yellow search block; focus turns its edge yellow.
  Words search Google, an address opens it. One faint line: what Shields blocked. While typing the field slides to the top and its suggestions sit below it. The dock hides its address row on Home.
  Home over a page (Home button) keeps the page alive underneath; Back returns to it.
- **Tab grid (`ui/TabGrid.kt`)**. Header: segmented `Tabs n | Private n` (each in its own colour), `Close all`. Cards: favicon/letter, title, close X, page preview (`ui/ThumbStore.kt`: 240 px RGB_565,
  GPU read-back when the grid opens and 2 s after a load, WebP cache for regular tabs, RAM only for private). Selected card = 3 dp accent edge. Bottom: `+ New tab` (accent) and `Done`.
  The private list is separate and violet; screenshots are blocked while it is on screen.
- **Sheets (`ui/Sheet.kt`)**: Menu = two-column tile grid (three on a wide window) with icons; Shields = the per-site switches. Both rise from the bottom (thumb reach) over a scrim.
- **Private mode**: the whole chrome turns violet (dock, Home, progress, bars), the address pill carries a mask, the tabs button a violet dot, Home a PRIVATE badge, FLAG_SECURE on.

## Rules for new UI
1. Reuse `Pal`, `chamferSurface`, `pillButton`/`outlineButton`/`textButton`, `iconButton`, `SheetHost`. No new colours outside `Pal`.
2. Nothing animated, nothing that needs a bitmap asset, nothing from AppCompat/Material.
3. Anything reachable with a thumb belongs in the bottom 40 % of the screen.
4. Check every screen on the emulator at 600x973 @170 dpi (portrait + landscape) before a release; screenshots of private tabs need `adb shell setprop debug.kestrel.nosecure 1` (debug builds only).
