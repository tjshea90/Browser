# Full screen on the tablet: bars, rotation, controls (owner message 46, 2026-11-15)

Owner, with three screenshots (600 x 1024 portrait, 1024 x 600 landscape):
1. "the Yahoo finance video now has player controls, but when I go to full screen mode it is glitchy because the controls in full screen get covered up by the Android bottom navigation bar"
2. "when I rotate the tablet from portrait to landscape and back, it is laggy and glitchy. It may benefit from pausing the video shortly until the orientation is set"
3. "I don't see any video player controls in full screen landscape mode. They either don't exist or they are covered by the Android navigation bar."

## What the screenshots show (rows measured with a small script, not eyeballed)
| screenshot | what is there | reading |
| :- | :- | :- |
| landscape full screen (1:35) | black status bar y 0-26, picture y 26-548, navigation bar y 548-600 (52 px, dark with the three icons), the picture is cut at the bar | BOTH system bars are visible in full screen and the page extends under them |
| portrait full screen (1:36) | film 600 x 338 at y 344-683, centre y 513.5 = the middle of the WHOLE 1024 px screen (not of the visible 26-972); the player's controls peek out at y 965-972 and the navigation bar starts at 972 | the full-screen element is laid out over the whole screen, bars on top of it |
| portrait after turning back (1:37) | picture y 40-612 (600 x 575, zoomed: object-fit cover look), the page's own pause button + progress line at y 604, black 616-636, GREY 640-972 | a stale / half-laid-out page: the player box kept an old height after the turn |

## Cause of 1 and 3
`BrowserActivity.applyImmersive` (alpha48) asked Android 10 for IMMERSIVE_STICKY | HIDE_NAVIGATION | FULLSCREEN **and** LAYOUT_STABLE | LAYOUT_HIDE_NAVIGATION | LAYOUT_FULLSCREEN, and the insets listener zeroed the root padding while a page was full screen (FS1: the emulator showed a 51 px black strip where the hidden bar's space was kept). That is right ONLY where the system really hides its bars. The owner's tablet (MediaTek "Alps 711", Android 10 Go) keeps both bars, so the window was laid out under them: a player's own controls at the bottom of its box, and Kestrel's bar (which sits 10 px above the film's bottom edge), were under the navigation bar. Landscape made it worst: the box bottom is the screen bottom.

## Fix (FSB1, alpha49)
* No LAYOUT_* flags on Android 10 (the older ones are cleared); the root padding ALWAYS follows the real insets: zero when the system hid its bars (full-bleed picture), the bar sizes when it kept them (the page is laid out above the navigation bar). `View.getWindowVisibleDisplayFrame` was considered and dropped: the plain insets are the truth once the LAYOUT flags are gone.
* The bars are asked to go again when the window gets focus, 150 ms after a configuration change and 700 ms after full screen began (some systems miss the first request, made while the screen is turning). What is left is written down: Diagnostics line **Full-screen system bars: hidden | KEPT by this tablet (left/top/right/bottom px)** and the event log line `fullscreen bars`.
* Debug builds only: `adb shell setprop debug.kestrel.nohide 1` plays a tablet that keeps its bars (the emulator lab, scene `fsy`).
* Guard test: `OwnerRulesTest.aFullScreenPageIsNeverLaidOutUnderASystemBarThatIsStillShowing`.

## Fix of 2 (ROT1, alpha49)
* `core/media/TurnSettle` (pure rules, `TurnSettleTest`) + `web/TurnSettler`: while the screen turns under a full-screen page or a playing video, the app pauses the video through the engine's media session (`GeckoTabData.mediaSession`, kept by `TabDelegates.onActivated/onPlay`), covers the page area with a black view, and lets both go when the page area has stopped changing size for 650 ms (at the latest 2.2 s after the turn began). It plays again only if the APP paused it, it is still paused, the same tab is in front, the app is on screen and nobody touched the screen meanwhile.
* The engine opens a media session only for AUDIBLE media: a silent / muted video is only covered, not paused.
* NOT known: why Yahoo's player box kept a stale height after the turn (page script vs engine viewport timing). The cover hides the transition; the lab (scene `fsy`, live Yahoo player, pictures `yahoofs-*`) shows whether the final state is right. If it is not, the next step is a second `orientationChanged()` / a resize nudge after the turn settled.

## Fix of the missing Kestrel bar in landscape (FSC1)
Review finding RW2: when the full-screen element is a web component's HOST, `document.fullscreenElement` names the host and the bar was added to the host's light DOM, which a host without a slot never draws (a 0 x 0 bar). The bar's home is now `VF.rootOf(fe) || fe` (the shadow root), proven in `videobar.test.js` (RW2). A bare `<video>` taken full screen by the page gets `controls = true` for as long as it lasts.

## Sound (owner message 45, answered here because the lab run came back at the same time)
Lab run 43745675957 first said "sound kept playing" (Android's `dumpsys audio` listed started streams: 1, 3, 5, 7 ... one more per round). That count is not usable: its `state:started` entries pile up. What the PAGE saw: the clip's `currentTime` stood still while the page was hidden (5.4 s before, 5.4 s after 5 s away) and the engine's media session said `playing=false` at the stop and `playing=true` 0.1 s after the return (`suspendMediaWhenInactive`). So leaving the app by Home or screen-off does stop the sound. Added as a safety net (SND1b): `BrowserActivity.silenceStrays` pauses a playing video that is not in front 1.5 s after the app stopped or another tab came to the front, and at once when Kestrel's own Home screen covers a playing page (the page stays alive underneath: that was a real gap). The lab verdict is now the page's own clock.

## What was NOT run on the tablet
Everything above was proven in headless Chromium (the page scripts), by guard tests on the source, and (after the lab runs of suite `fsy`) on an Android 10 emulator with the real Firefox engine. Not on the owner's tablet: its ROM keeps the bars; the emulator plays that with `debug.kestrel.nohide`. The owner's next look: Yahoo Finance video, full screen, turn both ways; Menu > Diagnostics line "Full-screen system bars".

## Addendum (2026-11-15 21:00 UTC): owner message 47 - "I haven't noticed any change with the new version" (alpha50 on the tablet)
Owner: controls still under the navigation bar, no controls in full screen, the video did not pause when switching orientation, laggy, "for a few seconds the video is the wrong size in either mode"; wants the video paused while the size and orientation are set up, then resumed, controls available in both orientations and never under the navigation bar.

### What that tells (alpha48, alpha49, alpha50 look the same on the tablet)
* alpha49 removed the LAYOUT_* flags and the padding zeroing; alpha50 read the bars from the decor's insets. NOTHING changed for the owner, so the layout is not decided by those. The theory that fits: on this ROM the window manager honours HIDE_NAVIGATION / FULLSCREEN by laying the window out over the whole screen, while its System UI keeps drawing both bars on top (the insets then say "no bars": every inset-based fix reads the lie). Nothing the app can read tells the two apart.
* The picture in the owner's third screenshot (600 x 575 cover-looking picture, grey below): 575 = 600 - 25, the LANDSCAPE page height (screen height minus the status bar). The page kept the old height after the width had turned: a page that sizes its player from `innerHeight` on a `resize` event and read a stale height (or the first of two resize events). A page-level fix: one more `resize` once the viewport is still.
* The muted Yahoo video opens no engine media session (only audible media does), so the app-side pause of alpha49 (MediaSession.pause) never touched it.

### Decisions (alpha51)
1. **FB1: the system bars are not asked to hide by default.** Prefs `fs_bars` (0 Keep, default; 1 Hide), Settings row "System bars in full-screen video". In Keep the window is laid out exactly as for every ordinary page (the dock is never under a bar there), so the page, a player's own controls and Kestrel's bar are between the bars in both orientations. Hide keeps the alpha48-50 behaviour (insets, re-ask at focus / turn) for a tablet that really hides its bars. Diagnostics line "Full-screen system bars" says which is on. The lab plays both (`am start ... --es kestrel_debug bars --es kestrel_arg keep|hide`).
2. **FB2: the PAGE pauses every video for a turn** (`kestrel-shields/turn.js`, every frame, after `vfind.js`): the window's shape flips (a `resize` / `orientationchange` / `screen.orientation` change that comes with a different shape: a keyboard or a toolbar does not flip it) -> every playing `<video>` (also in shadow roots) is paused; the turn is over when no event came for 650 ms (cap 2.2 s); then ONE synthetic `resize` (FB3) and, 80 ms later, play of the videos WE paused (never one that was paused before, one the page restarted itself, or after a touch). Numbers to the app as `vbk` e = turn, a = played | skipped | none; Diagnostics "Turning, the page's own pause".
3. The app-side pause (MediaSession, audible media) and the black cover over the page area stay: they hide the transition itself; the page-side pause covers silent videos and gives the page its final-size nudge.

### Not known yet (and how it will be known)
Whether the page of the owner's Yahoo player is fixed by the one extra resize. The lab suite `fsy` takes that player full screen and turns the screen both ways (pictures `yahoofs-*`); if the emulator shows the stale size, the nudge is tuned there; if it does not, the next tablet report (Diagnostics: "Full-screen system bars", "Turning under a video", "Turning, the page's own pause", the `gecko size` events around a turn) decides.
