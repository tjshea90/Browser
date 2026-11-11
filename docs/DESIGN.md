# Kestrel design system - "CERE" shape, "REAPER" colour

Decided 2026-11-11. Request 3 (owner): "greatly improved GUI", "fresh, not generic AI design", "big easy-to-press buttons", "no animations", "move the navigation bar to the bottom",
"black home screen with one large stylized field", "private tabs separate, with an indicator", "tab switcher with live previews".
Request 4 (owner, with a photo of a crowned skeletal reaper in a pitch-black void, neon-cyan glow, ghostly blue flames, a glowing scythe arc): "instead of yellow highlights, make them blue",
"I love the dark black glossy black look with the neon blue color", "very subtle pulsing blue animation ... like the pulsating neon colors around the modern smartphone screens when the phone is talking or using AI",
"make the colors of the animated accents look like the contrast between the very dark black and the blue colors in the attached photo".
(The photo itself is the owner's and stays out of this public repo; this file describes what was taken from it.)
Benchmark notes behind the choices: `research/UI_BENCHMARK.md`. Owner decisions still open are in `TASKS.md` (UI-2, UI-3).

## Idea
Two layers, kept apart so either can change without the other:
- **CERE = shape.** Ink-black surfaces, sharp edges, one shape: a rectangle with 45-degree **cut corners** (top-left and bottom-right), like a feather tip. No pills, no circles, no shadows.
- **REAPER = colour and light.** Pure black glossy surfaces (a faint blue-black sheen at the top of every panel fading to black) and ONE accent, **neon cyan-blue `#14D4F8`**, with an ice-white-cyan
  `#36E6F2` for the brightest glow cores and a mid blue `#0284B0` for hairlines. The contrast between the black void and the blue glow is the whole look.
Private mode swaps accent and chrome to violet (`#B07CFF` on `#0B0716`) so it can never be mistaken for a normal tab; its glows are violet too.

## Tokens (`app/src/main/res/values/colors.xml`, `ui/Ui.kt` `Pal`)
| Token | Regular | Private | Use |
| :- | :- | :- | :- |
| window | `#000000` | `#000000` | Home screen, behind everything |
| chrome | `#04090C` | `#0B0716` | dock, bars, sheets |
| gloss top -> bottom | `#0B1B25` -> `#010305` | `#170F2A` -> `#05030B` | the sheen of dock, sheets, cards, buttons (`GlossDrawable`, `ChamferDrawable(gloss=true)`) |
| pill / raised | `#0A1820` | `#1A1230` | address field, tiles, buttons |
| card | `#07121A` | `#120B22` | tab cards, lists |
| divider | `#12384A` | `#33264F` | hairlines, outlines |
| fg / muted | `#E6F8FF` / `#7FA3B3` | `#F2ECFF` / `#A797C8` | text, icons |
| accent | `#14D4F8` | `#B07CFF` | primary action, selection, progress, focus edge, glow |
| accentHi / deep | `#36E6F2` / `#0284B0` | `#D9C2FF` / `#5B35B8` | glow core, active icon / quiet hairlines |
| danger / warn / ok | `#FF5C7A` / `#FFB84D` / `#4DE3A5` | same | state only |
Dark only (the owner uses the tablet dark; a light skin is a later option). System fonts only (Roboto): 16 sp body, 15 sp labels, 20 sp Home field, bold for emphasis.

## Shape and touch
- `ChamferDrawable` (`ui/Chamfer.kt`): cut corner 8-18 dp depending on size, optional `gloss`. Pressed state = the fill moves 14-16 % toward white/black **instantly** (`chamferSurface`, `ripple`).
- Targets: every button is at least 48 dp tall; the six dock buttons are ~94 dp x 56 dp on the tablet; tab-grid buttons 48-56 dp.
- Icons: hand-made 24 dp vectors, 2 dp stroke, square caps, mitred joins (`res/drawable/ic_*.xml`), tinted at runtime. Adding one: copy an existing file.

## Motion: instant everywhere, except the neon (`ui/Neon.kt`)
Window animations are off, state lists have 0 fade, sheets and grids appear in one frame, the keyboard lift jumps. **The only motion is the neon breathing**, in exactly three places:
1. the **halo above the dock** (a 16 dp blue bloom rising from the 2 dp progress line into the page),
2. the **Home field's glow** (a 10 dp bloom around the chamfered field),
3. the **selected tab card's glow** in the tab grid.
Each is a static, pre-built glow drawable on a `GlowView`; "pulsing" is only an alpha change of that view (a RenderNode property: no bitmap redraw, no layout, no off-screen layer, no blur, no
`RenderEffect`), a cosine breath of 4.2 s between alpha 0.45-0.5 and 1.0 on ONE shared clock (all glows breathe together). It is **stepped at 10 Hz** (100 ms) instead of animated at 60 Hz: a slow breath moves 1-2 % of alpha per step, the eye cannot tell it from a smooth ramp, and each step is one tiny frame
(6x fewer than a RenderThread animation). While it runs each glow is a small hardware layer (strokes drawn once, a step only re-blends a texture); at rest the layer is dropped.
It **holds still** (a steady glow at ~70 %) while typing or while a page loads (the screen is already changing), in battery-saver, when the system's animations are off, when the app is not
on screen, and after **25 s without a touch** (so a tablet left on a page lets the screen go quiet; the next touch resumes it). Settings > Appearance > **Neon glow**: Off / Steady / Subtle pulse (default).
Rules for a new glow: a `GlowView` + `Neon.pulse(view)`, small area only (never a full-screen layer), never behind text that is being read, never in the page area except the dock halo.
The Home screen backdrop (`HomeBackdrop`) is the reaper photo's mood and is **static**: faint vignette, two ghostly blue flames rising from the bottom corners (a 40 KB procedural mask, generated once on
a background thread), a thin glowing scythe arc in the top-left.

## Screens
- **Dock (bottom, `ui/BottomDock.kt`)**. Portrait: address row (mask if private, lock/warning, address, Shields chip, reload/stop) over a row of six buttons: Back, Forward, Home, New tab (+),
  Tabs (count, chamfered square), Menu. Width >= 760 dp: one row. While the address is typed the activity lifts the dock above the keyboard with `translationY` (the page view is never resized:
  `research/FREEZE.md`), the address row sits on the keyboard, the suggestion list sits on the address row with the best match nearest the bar. The glossy dock carries a blue edge; a 2 dp neon
  progress line (`NeonBar`) with its halo sits on top of it.
- **Home (`ui/HomeView.kt`)** = new-tab page and the Home button. Pure black with the reaper backdrop, wordmark (+ PRIVATE badge), one 72 dp glossy chamfered field with a blue search block and a
  breathing glow; focus turns its edge ice-bright. Words search Google, an address opens it. One faint line: what Shields blocked. While typing the field slides to the top and its suggestions sit below it.
  The dock hides its address row on Home. Home over a page (Home button) keeps the page alive underneath; Back returns to it.
- **Tab grid (`ui/TabGrid.kt`)**. Header: segmented `Tabs n | Private n` (each in its own colour), `Close all`. Cards: favicon/letter, title, close X, page preview (`ui/ThumbStore.kt`: 240 px RGB_565,
  GPU read-back when the grid opens and 2 s after a load, WebP cache for regular tabs, RAM only for private). Selected card = 3 dp accent edge + breathing glow. Bottom: `+ New tab` (accent) and `Done`.
  The private list is separate and violet; screenshots are blocked while it is on screen.
- **Sheets (`ui/Sheet.kt`)**: Menu = two-column tile grid (three on a wide window) with icons; Shields = the per-site switches; Updates. They rise from the bottom (thumb reach) over a scrim.
- **Downloads (`ui/DownloadsScreen.kt`)**: one glossy card per download with a progress bar, speed/ETA line and big Pause / Resume / Cancel / Open / Share / Delete buttons.
- **Private mode**: the whole chrome turns violet (dock, Home, progress, glows, bars), the address pill carries a mask, the tabs button a violet dot, Home a PRIVATE badge, FLAG_SECURE on.

## Rules for new UI
1. Reuse `Pal`, `chamferSurface`, `pillButton`/`outlineButton`/`textButton`, `iconButton`, `SheetHost`, `GlowView`. No new colours outside `Pal`.
2. Nothing animated except `Neon.pulse` on a small `GlowView`; nothing that needs a bitmap asset; nothing from AppCompat/Material.
3. Anything reachable with a thumb belongs in the bottom 40 % of the screen.
4. Check every screen on the emulator at 600x973 @170 dpi (portrait + landscape) before a release; screenshots of private tabs need `adb shell setprop debug.kestrel.nosecure 1` (debug builds only).
