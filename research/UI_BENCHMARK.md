# Kestrel UI benchmark and design direction (task R1) - researched 2026-11-11
Method: web search/fetch only. No emulator, no screenshots: every visual judgement here is text-based and must be re-checked on emulator screenshots (task C4) and on the tablet.
Tags: [V] read in a fetched primary page; [S] search snippet only (page blocked/empty); [U] my inference or memory, unverified. Units: dp. Tablet = 564x916 portrait, 916x564
landscape, density 1.0625.

## 1. Decisions (read this first)
1. **Pick the chrome layout at RUNTIME from window dp, never with `sw600dp` resources.** The tablet is sw564, so `sw600dp` never matches. Chromium's tablet test is exactly
   sw600dp [V], so Chrome and (being Chromium) Brave show
   their PHONE layout here [U for Brave]: the owner's "Brave look" is Brave's phone layout (address bar on top, no tab strip, optional bottom toolbar [S]). Window size classes
   [V]: portrait = compact width (<600) and expanded height (916 vs the 900 threshold); landscape = expanded width (>=840) and medium height.
2. **Touch targets: 56 dp default; 64 dp for the bottom-bar cells; 48 dp is the absolute floor** (card close, row-end buttons). On this ~170 ppi panel 48 dp = 7.6 mm, 56 = 8.9
   mm, 64 = 10.2 mm (section 2).
3. **Portrait = SPLIT chrome**: address row on top (56) + action bar at the bottom (64: Back, Forward, New tab, Tabs, Menu). **Landscape = UNIFIED**: one 56 dp row + a 48 dp tab
   strip when >= 2 tabs.
   Precedent: Vivaldi moves the bottom icons to the top bar on larger screens [V]; Samsung, Opera, Edge, DuckDuckGo, Vivaldi, Chrome and Brave all offer bottom controls [S/V].
   Setting "Toolbar: Split / All on top".
4. **Tabs**: portrait has no strip; the Tabs button opens a full-screen card grid (Firefox moved its switcher to a full-screen page [S]). Landscape has the strip (X only on the
   ACTIVE tab: Chrome hides X on narrow tabs [V]) plus the same grid. Setting "Tab bar: Auto/Always/Never".
5. **Address field** is one big target: state cell, host in bold, Shields chip (the only yellow block in the chrome), reload/stop. **Typing** hides the other chrome and shows an
   OPAQUE suggestion layer over the page, so the engine view never relayouts under the keyboard.
6. **Menu**: 4x2 tile grid (Reload, Bookmark, Share, Find, Desktop site, Text size, Save data, Print) + 5 rows (Private window, Bookmarks, History, Downloads, Settings) + Quit.
   Bottom sheet in portrait (right panel in landscape), anchored to the edge the Menu button sits on; all rows 56.
7. **Shields panel**: ON/OFF block + honest counts + "Advanced controls" accordion collapsed by default (Brave's model [V]). The lock and the shield open the same panel (no
   separate site-info popup).
8. **Identity "CERE"** (alternatives RUFOUS, STOOP in section 6): cool slate neutrals following the system light/dark theme; ONE accent, cere yellow `#F6C026`, meaning "armed /
   selected / found / under your finger"; flat surfaces + 2 dp rules, no shadows;
   6 dp controls, one 8 dp top-right chamfer on "object" containers, mitred 2 dp icons, system fonts only.
9. **No animation by construction**: zero-duration `StateListDrawable`, no `RippleDrawable`, custom 2-state toggle (not `Switch`), `PopupWindow.setAnimationStyle(0)` [V], null
   window animations, no overscroll glow, no Toast/Snackbar. With nothing moving, state is WORDS + fixed geometry (ON/OFF text; buttons never reflow).
10. **Tokens stay swappable** (`Pal` -> one `Tokens` table) so the owner can A/B concepts in one APK; ship CERE as default.
Owner decisions needed (recommendation first): (a) split chrome with a bottom bar vs all on top; (b) CERE vs the safer RUFOUS; (c) landscape tab strip (familiar) vs a 56 dp left
tab rail (fresher, saves 48 dp of height, unusual on Android: only after a trial).

## 2. Device facts and touch-target numbers
- 600x973 px @170 dpi = 564.7x915.8 dp; dump: `ro.sf.lcd_density 170`, font scale 1.2. Probably a 7" 1024x600 panel (~170 ppi) [U: show xdpi/ydpi on the A5 Diagnostics page]. 16
  sp = 19.2 dp = 3.05 mm em; bars must not clip text.
- Content after a 24 dp status bar: 892 portrait, 540 landscape. Landscape is wide-but-short: spend width, not height. Page area: split portrait 772 dp (86%), all-top portrait
  836 (94%), landscape strip+row 436 (81%), row only 484 (90%).
- Nav mode unknown (HARDWARE.md: gesture nav "may be on"). A bottom bar must pad by `getMandatorySystemGestureInsets()`; apps cannot opt out of the bottom home/quick-switch
  swipes; back-gesture edges are left/right [V]. No side-swipe controls.
- Android says low-end devices have low-res screens, so optimise UI-thread work first, but shadows, fades and stacked backgrounds still cost fill rate [V overdraw page]. Mali-G52
  MC2 at 0.58 MP: fine if we stay flat.
- Guideline numbers: Android >= 48x48 dp, >= 8 dp apart, "about 9 mm", "recommended 7-10 mm" [V]. WCAG 2.2: 24x24 CSS px AA (2.5.8), 44x44 AAA (2.5.5) [V]. Microsoft 7.5 mm now,
  9 mm ideal in older docs [V/S].
  Parhi et al. 2006 (one-thumb): 9.2 mm discrete, 9.6 mm serial [S abstract]. NN/g >= 1 cm, ~2 mm gaps [V].
- 1 dp = 1/160 in = 0.159 mm, so 48 dp is 7.6 mm although Android's page says ~9 mm: 48 is the FLOOR here, not the target. Hoober's grip study is phones only (49% one hand, 36%
  cradled, 15% two hands) [V]: tablet ergonomics are [U].
- Spacing: toolbar cells abut (each is a full 56x56 or 113x64 target); isolated small targets keep >= 12 dp (1.9 mm); destructive and safe actions >= 16 dp apart.

## 3. Layout (dp)
```
PORTRAIT 564x892 (split)                        LANDSCAPE 916x540 (unified)
+------------------------------------+          +----------------------------------------------------------+
|[lock] host.example.com  [S 14] [R] | 56       | [Tab A  x][Tab B][Tab C]                          [+]    | 48 strip
+====================================+ 4 prog.  +----------------------------------------------------------+
|                                    |          |[<][>][R] [lock host.example.com        [S 14]] [+][3][=] | 56
|        page 772 dp (86%)           |          +==========================================================+
|                                    |          |                 page 436 dp (81%)                        |
+------------------------------------+          +----------------------------------------------------------+
| [<]   [>]   [+]   [3]   [=]        | 64 (5 x 113) + gesture inset
+------------------------------------+
```
- `ChromeMode.of(widthDp, heightDp)`: SPLIT if width < 600 and height >= 700, else UNIFIED; strip if width >= 840 and tabs >= 2 (or setting); no strip if height < 480 (split
  screen). Re-evaluate in `onConfigurationChanged`.
- Chrome = vertical `LinearLayout`: [strip][row][progress][engine view, weight 1][bottom bar]. Siblings, never overlays on the engine view. Landscape field 564 dp (916 - 6x56 -
  16); all-top portrait field 388 dp; split portrait field 548 dp.
- Disabled Back/Forward stay visible in the muted colour (stable geometry). Typing mode makes the bottom bar INVISIBLE, never GONE (GONE would resize the engine view); the only
  overlay on the engine view is the opaque typing layer. Optional "Hide status bar in landscape" (+24 dp).
- No hide-on-scroll in v1: it resizes the engine view (reflow on a slow SoC) and would animate. Vivaldi [V], Samsung [V: it has a "keep bars visible" option] and Firefox [S] hide
  bars on scroll, so make it a later opt-in.

## 4. Behaviour specs
- **Address field** (48 tall in the 56 row, 6 dp radius, field fill + 2 dp outline). Idle: host bold 17 sp + path muted 15 sp (https and "www." hidden, as today). http shows the
  word "NOT SECURE" in danger colour; offline shows "SAVED COPY".
  Shield chip (56x48, inside the field, hidden on the NTP): armed = yellow fill, ink shield + count (condensed 14 sp); site switched off = outline + shield-off glyph + word OFF.
  Tap: full URL, select-all, keyboard (`IME_FLAG_NO_EXTRACT_UI`; private: no personalised learning). Long-press: Paste & go / Copy / Share. Swipe on the idle field switches tabs
  (Chrome tablets do [V]) - nice-to-have.
- **Typing mode**: the field spans the row with Clear (48) and Go; all other chrome is hidden; an opaque layer below shows rows of 56: "Search Google for ..." first, then <= 6
  local history/bookmark matches, each with a 48 dp fill-arrow.
  Rows that fit = floor((892 - 56 - keyboard)/56): ~9 portrait, ~4 landscape (keyboard 280/230 dp assumed [U]; measure). Use `SOFT_INPUT_ADJUST_NOTHING` while the field has focus
  so the engine view never resizes [U: validate with R6, the typing freeze].
- **Tab strip** (landscape): tab width 128-220, height 48, favicon-or-letter 20 + title 14 sp; active tab = chrome fill + 3 dp yellow top rule + chamfer + the only X (48x48); "+"
  48x48 after the last tab; long-press = Close / Close others / Duplicate. 6 tabs fit, then it scrolls.
- **Tab grid**: header 56 [X][N tabs][New tab = yellow][overflow: Close all]. Cards: portrait 2 cols x 264, landscape 3 x 289, gutter 12. Card = header 48 (letter tile 24, host
  bold, X 48x48) + body 112 (title 2 lines, host, age). Current tab: 3 dp yellow frame + word "OPEN".
  No thumbnails in v1 (later, on disk: 280x160 px RGB_565 ~ 89 KB each, 20 tabs ~ 1.7 MB). Close is instant with a 6 s "Undo" bar. DuckDuckGo adds swipe-to-close and long-press
  Tabs = new tab [S]: nice-to-have.
- **Menu**: tiles 72 tall, label 13 sp; rows 56. "Close all tabs" lives in the grid overflow, not here. Quit is last and asks only if downloads are active. Landscape panel 360
  wide, scrolls. Brave lets users reorder its menu (1.84) [V]: skip, keep it short instead.
- **Shields panel** (under the chip, 360 wide, <= 548 portrait): row 1 host + big ON/OFF block; row 2 "N blocked on this page" (24 sp condensed) expanding to the top blocked
  hosts (Brave: tap the count [S]);
  "Advanced controls" (collapsed): ads/trackers, cosmetic, HTTPS, cross-site cookies, fingerprinting, scripts (56 rows, ON/OFF words), then "Reset this site to defaults" and
  later "Clear this site's data"; footer "Changes reload the page".
  Brave's own panel has the same set: "Shields up for this site", "Trackers, ads, and more blocked", Advanced controls, HTTPS upgrade, Block scripts (may break sites),
  fingerprinting, cookies, "Forget me when I close this site", Block element [V strings]. Show only counts the engine measures; Brave's NTP "data/time saved" are estimates [S].
- **New tab page**: wordmark 28 sp + search field 56; favourites 4x2 tiles (133x104; 56 rounded-square letter tile) portrait, 6x2 landscape; ONE readout card of real numbers:
  BLOCKED (since install) | DATA USED (TrafficStats) | FILTERS age. Notices only when relevant. No wallpaper, feed or sponsored tiles; no network at start.
- **Downloads** (B2): rows 72: type tile 40, name 17 sp, meta in `monospace` 13 sp ("12.4 / 48 MB - 3.1 MB/s - 11 s left"), 4 dp progress under the row; active rows have
  Pause/Resume 56 + Cancel 56; finished rows open on tap, overflow = Share/Delete;
  failed = word "FAILED" + Retry; long-press = multi-select bar; active section first; > 20 MB on the hotspot asks first. Chrome's page groups by type (audio, video, images,
  documents, pages) [S]; one list with a type tile is enough here.
- **Find in page**: replaces the top row: [X 56][input][3/12][up 56][down 56]; Enter = next. Firefox keeps it at the bottom and it can vanish with the keyboard on some sites [V
  bug 1807145]; Chrome puts it in the top bar [U].
- **Long-press link/image**: bottom sheet (landscape: centred 360 dialog); header letter tile + host + 2-line URL; rows 56: new tab, private window, copy link, share, download (+
  image: save, open, copy address, share; no Lens). Chrome's is a popup with favicon + title [V].
- **Dialogs/errors**: own `Dialog` (never `AlertDialog`): full-width 56 dp stacked buttons, primary yellow, destructive in danger colour. Error pages use the same tokens with a
  state word ("NO INTERNET", "NOT SECURE", "SAVED COPY AVAILABLE") and a 64 dp Retry.
- **Settings IA** (8 rows of 64, then a page; landscape = two panes 280 + rest): Privacy & Shields | Search & address bar | Data saver | Appearance (theme, text size, page dark
  mode, tab bar, toolbar, style) | Downloads | Site settings | Filter lists | About & diagnostics.
  Brave's root list mixes in Leo, Rewards, News and Playlist [S]; ours has none of them.
- **Private window**: "Hood" chrome (`#1E1530`, lilac marker `#CDB8FF`), permanent word chip "PRIVATE" + hood glyph in the field, NTP explainer, FLAG_SECURE (exists). Never
  colour alone: Brave's purple theme drew contrast complaints [S].

## 5. What users expect
- **MUST** (every major browser): URL/search field; back, forward, reload/stop with progress; tab-count button, new/close tab, tab grid, undo close; menu with find, share,
  desktop site, bookmark; bookmarks and history with search; downloads with progress/open;
  private window; per-site Shields with a counter; long-press link/image menus; text size; page dark mode; HTTPS warning; error page with retry and saved copy; clear data; search
  engine; permissions; fullscreen video; print/PDF.
- **NICE** (later): swipe-to-close card, swipe field to switch tab, long-press Tabs = new tab, one-tap "clear data and close" (DDG Fire Button [V], Focus erase [S]), block
  element (Brave [V]), reader mode (Firefox), pinned tabs, Paste & go,
  add to home screen, hardware-keyboard shortcuts (Ctrl+T/W/L/F/R), configurable bottom slot (Brave, Firefox, Samsung [V/S]).
- **LEAVE OUT** (owner + weight): news/feed, AI, rewards, wallet/web3, VPN, sync/accounts, Tor, telemetry, sponsored tiles/wallpapers, shopping, Lens/voice/cast/translate (Google
  services), password vault (use Android Autofill),
  payment autofill, default-browser nags, promo/"what's new"/rating dialogs. Brave itself ships a page on hiding its extra features [S]
  https://support.brave.app/hc/en-us/articles/38305898674957/ .

## 6. Visual identity: CERE (recommended) and alternatives
Why a falcon palette alone is not enough. A recent catalogue of AI-template "tells" lists first-wave (purple gradients, one font, same radius and shadow everywhere, glass,
fade-ups) and second-wave looks (cream + terracotta = "the Claude palette", near-black + one neon, hairline broadsheet,
tracked caps + monospace "template chrome") [V]. Its remedy is process: derive choices from the product, write the direction down, audit against the tells. Applied here:
- **Derived from the product**: hotspot -> real byte/block readouts in three places (NTP, Shields, Downloads); no animation -> state words, fixed geometry, a loud "armed / under
  finger" marker; falcon -> one geometric cut and one yellow
  (kestrels hover; males have a blue-grey cap [V Wikipedia]; yellow cere, eye-ring and feet [U, not in the fetched text]); Shields is the product -> the yellow block in the
  chrome is always the Shield chip.
- **Self-audit**: no purple gradient, no serif, no cream/terracotta (so the rufous back colour is NOT the accent), caps and monospace only for readouts and section labels (<= 2
  places per screen), chamfer once per container, no glass.
  Honest caveat: dark theme + one accent is still the common pattern; kept because it is the most legible on a washed-out 170 ppi LCD in daylight.
- **Against stock Material** (Chromium ships tab cards radius 24, toolbar radius 16, an 18 dp close glyph in a 48 dp button [V]): no pills or large radii, no tonal elevation or
  shadows, no ripples, no circular icon buttons or avatars, no FAB, mitred icons, cells that fill the whole 56/64 dp target.

| Token | Light | Dark | Hood (private) | Contrast (computed, WCAG) |
|---|---|---|---|---|
| bg (page-level) | `#F5F7F9` | `#0E1114` | `#150F24` | - |
| bar (toolbar, strip, bottom bar) | `#DFE4E9` | `#151A1F` | `#1E1530` | text on bar 14.3 / 15.2 / 15.0 |
| field / card | `#FFFFFF`, outline `#6B7683` | `#222A32`, outline `#7B8794` | `#2A1F42` | outline 3.6 / 4.0 (>= 3 for component edges) |
| text / muted | `#12151A` / `#4F5B69` | `#ECEFF2` / `#98A2AE` | `#F1ECFA` / `#B7A9D1` | muted on bg 6.5 / 7.3; on dark bar 6.8; hood 8.0 |
| disabled | `#78838F` | `#6E7885` | `#8F83A8` [U] | ~3.0 / 3.9 (exempt, visible) |
| signal + on-signal | `#F6C026` + `#12151A` | same | lilac `#CDB8FF` + `#1E1530` | ink on signal 10.9; on dark bar 10.4; lilac 9.9 |
| danger / ok / warn | `#B42318` `#17703F` `#8A5300` | `#FF8A80` `#5AD38A` `#FFC857` | as dark | on bg 6.1 / 5.7 / 5.9; dark 7.7 / 9.2 / 11.2 |
| rule (2 dp) | `#B9C2CB` | `#2E3640` | `#3A2D57` [U] | decorative |
Yellow on white is 1.7:1, so on light surfaces it is ALWAYS a fill with ink content (never text, never the only indicator) plus a glyph or word. The current app accent `#D9480F`
is 4.3:1 on white.
- **Shape**: bars 0 radius; controls 6; tiles and avatars 8 (rounded squares); sheets, cards, active tab, Shields panel = 6 + an 8 dp top-right chamfer (custom
  `ChamferDrawable`). No shadows, no elevation; separation by value steps and 2 dp rules.
- **Type** (system only. AOSP `fonts.xml` has `sans-serif` Roboto 100-900, `sans-serif-condensed` 300-700, `monospace` Droid Sans Mono, Noto Serif, `source-sans-pro` [V: tag
  android-10.0.0_r47 requested, file does not state its release]; this OEM build may differ [U]):
  Display 28 `sans-serif-medium`; Title 20 medium; Row 17 regular; Host 17 medium; Secondary 14; Meta 13 muted; Label 12 `sans-serif-condensed` bold caps (headers and readout
  labels only); Readout 22 condensed bold; live numbers 13 `monospace`. Nothing under 12 sp (14.4 dp at 1.2). Weights: `Typeface.create(Typeface, weight, italic)` (API 28) [V].
- **Icons**: 24 dp viewport, 2-unit padding (20 live), 2-unit stroke, butt caps, MITRED joins, 3-unit 45 deg chamfers instead of corner radii, filled twin for ON states (star,
  shield, desktop), only M/L/H/V/Z plus circle arcs (< 0.5 KB each), tinted at runtime.
  Contrast: Lucide uses 2 px strokes with ROUND joins and caps [V]. Set of ~36: back fwd reload stop menu(3 square dots) search lock lock-open warn shield shield-off star share
  find download history sliders hood trash copy new-tab desktop text-size data-saver pause play folder file image video audio archive check chevrons arrow-fill. Tab-count glyph =
  24 dp square with the chamfer, count in condensed bold.
- **Feedback without animation** (`StateListDrawable` + `setEnterFadeDuration(0)`/`setExitFadeDuration(0)` [V]): buttons, tiles, primary: pressed = yellow fill + ink glyph;
  selected = yellow fill; focused (keyboard/D-pad) = 2 dp inset ring in text colour; disabled = colour, never alpha.
  List rows: pressed = `bar` fill; selected = 4 dp yellow left bar + check glyph. Toggles: ON = yellow track + ink word "ON"; OFF = outline + "OFF". Check the yellow press for
  harshness in C4.
- **Density**: 4 dp base, steps 4/8/12/16/24/32/48; gutters 16 portrait / 24 landscape; rows 56 (1 line) / 72 (2 lines); section label row 40; sheet padding 16.
- **Alternatives**: **RUFOUS** = white/grey tonal surfaces, rufous `#C2410C` accent (5.2:1 on white), 10 dp rounded squares: closest to Brave and safest, but it reads as a Brave
  / terracotta default. **STOOP** = dark-first `#0B1320`, ice accent `#7DD3FC` (11.2:1), 45 deg chamfers on every corner (HUD look):
  most distinctive, but risks the "near-black + neon" tell, gamer feel and eye strain. Recommendation: CERE.

## 7. Implementation consequences (framework Views)
1. `ui/Ui.kt`: replace `ripple()` (a `RippleDrawable`, which animates) with `Feedback.states(...)` returning cached `ConstantState` drawables; `Pal` becomes `Tokens` (+ style
   switch). Remove `elevation` on FindBar (4), SuggestionList (8), MenuPopup and ShieldsPanel (12): borders instead.
2. Overdraw/RAM: window background = `bg`, containers without backgrounds, opaque bars [V]. No `alpha` for disabled (Toolbar sets `alpha = 0.38f`); Android's own example is grey
   text instead of translucent black [V]. Zero bitmap assets (vectors, `GradientDrawable`, `onDraw`); one cached `Path` per `ChamferDrawable`.
3. Zero-animation kit: `PopupWindow.setAnimationStyle(0)` (0 = none, -1 = default [V]); `Window.setWindowAnimations(0)` on own `Dialog`s (the `AlertDialog` theme animates); theme
   `windowAnimationStyle` -> null style; `overridePendingTransition(0,0)`; `OVER_SCROLL_NEVER`;
   own `ListView.setSelector` (theme selectors are ripples); custom `Toggle` (framework `Switch` animates its thumb [U]).
4. Touch targets: set `minimumWidth/Height` 56/64 on the clickable view itself and centre a 24 dp glyph (`ScaleType.CENTER`), the way Android recommends [V]. Avoid
   `TouchDelegate` in the chrome: the ancestor must forward `onTouchEvent`, and Accessibility Scanner honours it only on Android 10+ [V].
   Add a unit test that walks the view tree and fails on any clickable under 48 dp or any `RippleDrawable`.
5. Lists: `ListView`/`GridView` only. Today `TabSwitcher`, `ListPanel` and `SuggestionList` ignore `convertView` and build a row per bind [V code]: use a view holder, fixed row
   heights, a 50-80 ms debounce for suggestions, SQLite on the IO executor.
6. Pressed state inside scroll containers is deferred by the tap timeout (~100 ms) [U]: override `shouldDelayChildPressedState()` to false in our lists/grids if instant feedback
   matters more than scroll-start flicker.
7. Font scale 1.2 up to 1.5: bars fixed in dp, rows `minHeight`, single-line ellipsis in chrome. Test 1.0/1.2/1.5 x light/dark/private x both orientations on 600x973 @170.
8. WebView and Gecko builds share all chrome (`app/src/main`). Keep the engine view a sibling of the chrome, overlays opaque and few (Gecko surface compositing cost is [U]).
9. Order: P1 tokens + Feedback + shapes + fonts + icons + Toggle + Sheet/Dialog bases (C1); P2 ChromeMode, top row, bottom bar, strip, typing mode (C2); P3 grid, menu, Shields,
   NTP, long-press, find, downloads (B2), settings, errors (C3); P4 screenshot review and owner A/B of styles (C4).

## 8. Evidence (browser by browser)
- **Chromium tablet rule**: `MINIMUM_TABLET_WIDTH_DP` "matches res/values-sw600dp" [V]
  https://raw.githubusercontent.com/chromium/chromium/main/ui/android/java/src/org/chromium/ui/base/DeviceFormFactor.java ; Brave shows the tab bar on tablets, not phones [S]
  https://community.brave.app/t/request-for-tab-bar-option-on-android-phones/646727
- **Chromium dimens** [V]: tablet `tab_strip_height=40dp` (phone 0)
  https://raw.githubusercontent.com/chromium/chromium/main/chrome/browser/ui/android/toolbar/java/res/values-sw600dp/dimens.xml ; `toolbar_icon_height=24dp`,
  `ntp_search_box_height=64dp`, `toolbar_corner_radius=16dp`, `tab_switcher_menu_width=256dp`
  https://raw.githubusercontent.com/chromium/chromium/main/chrome/browser/ui/android/toolbar/java/res/values/dimens.xml ; tab grid card margin 8, radius 24, header 40, action
  button 48, close glyph 18, strip item min width 140 https://raw.githubusercontent.com/chromium/chromium/main/chrome/android/features/tab_ui/java/res/values/dimens.xml
- **Chrome Android**: tablet tab-count button -> visual grid, swipe on omnibox switches tabs, X hidden on narrow tabs [V] https://9to5google.com/2022/10/18/chrome-android-tablet/
  ; bottom address bar via long-press, menu order unchanged, bar hides on scroll [V] https://9to5google.com/2025/07/15/chrome-android-bottom-address-bar-2/ ;
  long-press menu = popup with favicon + title and last-used share target [V] https://9to5google.com/2019/11/05/new-chrome-long-press-menu/ ; Chrome 150 menu: Back beside
  Forward, info removed [S] https://piunikaweb.com/2026/07/09/chrome-android-menu-redesign-back-button/
- **Brave**: Shields icon in the address bar; Shields > Advanced controls > Block element [V] https://brave.com/privacy-updates/34-block-page-elements-android/ ; Android Shields
  panel strings [V] https://raw.githubusercontent.com/brave/brave-core/master/browser/ui/android/strings/android_brave_strings.grd ; Customize menu 1.84 [V]
  https://brave.com/whats-new/customize-android-settings-menu/ ; settings root has Appearance, Shields & privacy, Leo, Rewards, News, Playlist [S]
  https://brave.com/whats-new/android/ ;
  bottom toolbar added 1.0.88, optional 1.0.89 [S] https://www.apkmirror.com/?p=650878 ; NTP stats are estimates [S]
  https://support.brave.app/hc/en-us/articles/360045401211-What-is-the-Privacy-Hub-in-Brave-iOS ; purple private theme, contrast complaints [S]
  https://community.brave.com/t/i-cant-see-the-tabs-in-private-mode-because-of-the-absurd-and-disastrous-lack-of-contrast-in-the-purple-theme/95409 (Brave support pages returned
  403)
- **Firefox**: toolbar Simple/Expanded, top or bottom, auto-hide, tab bar forces the address bar to the top [S] https://support.mozilla.org/kb/toolbar-firefox-android ; tablet
  tab strip top only [S] https://connect.mozilla.org/t5/discussions/tab-strip-for-firefox-android-now-available-in-firefox-nightly/m-p/60214 ; grid/list inconsistency [S]
  https://bugzilla.mozilla.org/show_bug.cgi?id=2037273 ;
  menu is a bottom sheet, tab switching moved to a full-screen fragment [S] https://bugzilla.mozilla.org/show_bug.cgi?id=1979656 ; find bar at the bottom can close with the
  keyboard [V] https://bugzilla.mozilla.org/show_bug.cgi?id=1807145 ; shield states [S] https://support.mozilla.org/en-US/kb/enhanced-tracking-protection-firefox-beta-android
  (Mozilla pages mostly failed to load)
- **Samsung Internet** [V] https://developer.samsung.com/internet/release-note/android-release-note.html : tablet Tab Manager (9.4, 22.0), tab/bookmark/URL bar to the bottom on
  tablets (21.0), grid tab manager (12.1), Customize menu (10.2), bars stay visible while scrolling (25.0); One UI 8.5 split bottom bar [S]
  https://www.sammobile.com/news/samsung-internet-new-design-one-ui-8-5-leaks/
- **Vivaldi** [V] https://help.vivaldi.com/android/android-vivaldi-mobile-interface/ : tab bar, address bar with blocker button + reload + menu, bottom nav (Panels, history
  back/forward, Start Page, Tabs), "on larger screens all bottom icons move to the top toolbar", bars hide on scroll.
- **DuckDuckGo**: softer shapes, roomier address bar, shield with live info, one-tap Fire Button [V] https://spreadprivacy.com/browser-visual-refresh/ ; grid + swipe-to-close +
  long-press Tabs = new tab (5.35) [S] https://www.apkmirror.com/apk/duckduckgo/duckduckgo-privacy-browser/duckduckgo-privacy-browser-5-35-0-release/ ; bottom address bar (5.218)
  [S] https://www.apkmirror.com/apk/duckduckgo/duckduckgo-privacy-browser/duckduckgo-privacy-browser-5-218-0-release
- **Firefox Focus** [S] https://support.mozilla.org/en-US/kb/whats-new-firefox-focus-android-version-9 : erase icon in the URL bar, tracker controls behind the shield. **Edge**
  [S] https://www.neowin.net/news/microsoft-is-redesigning-the-toolbar-in-edge-for-android/ : bottom bar, menu in the centre, Share replaces Continue-on-PC.
  **Opera** [S] https://blogs.opera.com/mobile/2019/10/opera-touch-new-navigation/ : the Fast Action Button got a standard bottom bar as an option after user demand.
- **Guidelines** [V]: https://support.google.com/accessibility/android/answer/7101858 ; https://www.w3.org/WAI/WCAG22/Understanding/target-size-minimum.html ;
  .../target-size-enhanced.html ; https://learn.microsoft.com/en-us/windows/apps/design/input/guidelines-for-targeting ; https://www.nngroup.com/articles/touch-target-size/ ;
  Parhi [S] https://www.microsoft.com/en-us/research/?p=155150 ; Hoober http://www.uxmatters.com/mt/archives/2013/02/how-do-users-really-hold-mobile-devices.php ; size classes
  https://developer.android.com/develop/adaptive-apps/guides/use-window-size-classes ; gesture nav
  https://developer.android.com/develop/ui/views/touch-and-input/gestures/gesturenav
- **Android APIs and design sources** [V]: https://developer.android.com/reference/android/widget/PopupWindow ; .../graphics/drawable/StateListDrawable ; .../view/TouchDelegate ;
  .../graphics/Typeface ; https://developer.android.com/topic/performance/rendering/overdraw ;
  fonts https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android-10.0.0_r47/data/fonts/fonts.xml ; icons https://lucide.dev/guide/design/icon-design-guide ;
  AI tells https://capitalandcompute.net/blog/fix-ai-slop-design/ ; kestrel https://en.wikipedia.org/wiki/Common_kestrel , https://en.wikipedia.org/wiki/American_kestrel

## 9. Could not verify (treat as open)
Panel physical size/ppi and nav mode; the OEM system font; IME heights; tablet grip data; current exact Brave/Firefox/Edge/Samsung/Opera toolbars (pages blocked or empty,
snippets only); Chrome's find-bar placement; Material 2/3 pages (did not load; icon and shape specs from memory);
Gecko overlay cost; the ~100 ms pressed delay; `Switch` thumb animation; and anything about how CERE actually LOOKS: it needs screenshots (C4) and the owner's eye.
