# BRAVE_SMOOTH: "When i go to the parlayapi site in brave browser , it is smooth" (owner, 2026-11-24, PERF25)
Owner, verbatim: "When i go to the parlayapi site in brave browser , it is smooth". Sent right after alpha104 (one clip at a time, none while scrolling).
Public repo: the site is named only by kind (a sports-odds developer site: a sideways strip of 60 small looping clips; research/DECORATIVE_VIDEO.md). Tags: [M] measured on the owner's tablet, [V] verified in the engine or a primary source, [S] a secondary source, [E] estimate, [N] not found / not proven.

## 1. Which device? (asked in the reply)
Not stated. If Brave ran on THIS tablet it proves that a browser that draws on the graphics chip copes with the page here. If it ran on the owner's Android 16 phone it proves little (a far faster chip). The work below is valid either way: the tablet's own numbers (section 2) already say that CPU drawing is the main cost on every busy page.

## 2. What Brave / Chrome do and Kestrel G does not
| | Brave (Chromium) | Kestrel G today |
| :- | :- | :- |
| Drawing | GPU rasterising and a compositor thread; animations of `transform` / `opacity` run on the graphics chip [S] | **the PROCESSOR draws** (Software drawing, ON since 2026-11-23) because hardware drawing lost bands of text on this Mali-G52 (research/PAINT_GLITCH.md) |
| Video clips | hardware decoder output is composited by the graphics chip, no copy | with CPU drawing every video frame is copied and drawn by the same four weak cores that run the page's scripts [E] |
| Measured here [M] | not measured | frame callbacks a second after a page load: **median 6.4** (60 = keeps up), late frames 78.9 % over 14 pages; heavy chat app first paint **2.0 s hardware -> 14.6 s software**; typing waits for the page's next frame up to 1.2 s (alpha103 uploads, 2026-11-24) |
So the best explanation of "Brave is smooth, Kestrel G is not" is the drawing path, not Shields, not the clips alone. The clip limits of alpha102 and alpha104 only remove load from an overloaded processor; they cannot make CPU drawing as fast as a graphics chip.

## 3. What Mozilla's own bug reports say about Mali and this fault family (read 2026-11-24)
| Bug | GPU | What it says | Pref |
| :- | :- | :- | :- |
| 1689064 [V] | Mali-T628 | "optimized" shader variants give completely broken rendering; Mozilla blocked them on all Mali-T6xx | `gfx.webrender.use-optimized-shaders=false` |
| 1669960 [V] | Mali-G72 | blurry / corrupt text after bug 1669494 used glBlitFramebuffer to resize picture-cache textures; the real cause was GL_LINEAR not supported for blitting RGBAF32 (bug 1677757); fix: Mali-G goes back to glCopyImageSubData | none (code) |
| 1685276 [V] | Mali-G77 | display corruption: `eglSetDamageRegion` after offscreen passes without a driver flush; fixed in code; partial present kept for Mali-Gxx | none (code); `max-partial-present-rects` already off here |
| 1558374 [S] | Mali-G71 | black squares; scissored `glClear` avoided on Mali-Gxx | `scissored-cache-clears` already off here |
| 1607335 (wr-mali tracking) [V] | Mali family | still open: 1736299 pages corrupted on Mali-G71, 1814665 black / white tiles on Pixel 6 Pro, 1872745 pages gray or flickering | none named |
| (this fault) | Mali-G52 | **no report found** for Mali-G52 text bands [N] | - |
The engine itself carries Mali-specific code (strings in libxul 160: renderer-name checks for `Mali-T` / `Mali-G`, a `u_mali_workaround_dummy` shader uniform) and two switches that earlier sessions never tried: `gfx.webrender.use-optimized-shaders` (and `gfx.blacklist.webrender.optimized-shaders`) and `gfx.webrender.draw-calls-for-texture-copy` (the texture copy done with draw calls instead of the driver's copy call; the bug that added it was not read, the Bugzilla search was blocked [N]). Mozilla's fixes always went into code paths for ONE driver family; this tablet's driver (Android 10, patch 2021-07) may be one nobody reported. So whether hardware drawing can be made safe here is an experiment, not a certainty.

## 4. What alpha105 builds: the hardware drawing trial (core DrawTrial, Settings > Network lab > "Hardware drawing trial (experiment)")
- Off (default) = exactly what the tablet runs now. 1 = hardware drawing with `use-optimized-shaders=false`, `draw-calls-for-texture-copy=true`, `program-binary-disk=false` (the driver's saved compiled programs), on top of Safe drawing (already on). 2 = the same plus `layers.gpu-process.enabled=false` and `precache-shaders=false` (no separate renderer helper process, no compile-everything at start).
- While a trial runs Software drawing is not written (they exclude each other); the choice applies at the next start.
- It cannot trap the person: a start mark is written when a trial start begins and taken away after the engine has drawn 15 s (or the app is left after its first picture); a mark still there at the next start means that start never steadied (the driver killed the app) and the trial switches itself off, with a line in Settings and in the Diagnostics (`engine draw-trial`). A page-process crash within 3 minutes of the app's start also ends it.
- The Diagnostics `engine draw-prefs` line now reads back the five trial prefs and `software.opengl`; every upload records `draw_trial`, and the digest's drawing A/B (`drawing=hardware trial 1`, `software`, ...) compares page animation (`fm=`), first paint and typing.
- The owner's test, 3 minutes: Settings > Network lab > Hardware drawing trial > 1 > restart Kestrel (close it from recent apps) > open a chat page with a table (claude.ai) and YouTube > scroll for a minute. Bands of missing text (a thin line through some words, blocks that go away on scroll): try 2; bands again: set Off. No bands: leave it on for a day, then send diagnostics.

## 5. What it changes if it works / what is next if it does not
- Works: the digest numbers (fm=, first paint, typing) say how much; then a bisect (one switch at a time) finds the smallest set, and the heavy page, the clip strip, typing and scrolling all get the graphics chip's speed. The clip limiter stays as a safety net for CPU drawing.
- Fails (bands in both 1 and 2): the known Mali paths are ruled out; hardware drawing stays closed on this tablet and the work goes to making CPU drawing cheaper (fewer simultaneous page animations, a lower frame rate for pages with animation strips).

## 6. NOT proven
- Nothing here has run on the tablet: no GPU in this sandbox, so the trial is code-reviewed, unit-tested (the rule, the guards) and compiled, not drawn.
- Whether any of these switches touches the Mali-G52 band fault is an hypothesis from other GPUs' bug reports.
- Which device Brave ran on.
