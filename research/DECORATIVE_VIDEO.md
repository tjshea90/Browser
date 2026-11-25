# DECORATIVE_VIDEO: "The animations on this page are very laggy and make the entire site laggy. They also each have a play button, which is incorrect for in site animations" (owner, 2026-11-24, PERF21)
Owner, verbatim, with a screenshot of a developer-tools site whose hero is a strip of small clips: "The animations on this page are very laggy and make the entire site laggy. They also each have a play button, which is incorrect for in site animations".
Privacy note: this repo is PUBLIC; the site is named only by kind (a sports-odds developer site). The probe pages and numbers were kept in the scratchpad, not the repo.

## 1. What the page does (read in headless Chromium, 2026-11-24)
- The hero is a sideways-moving strip of **60 `<video>` elements** (24 of them sized, about 6 on screen at once, each 180 x 128 px), every one `muted loop playsinline`, `preload` none or metadata, NO `controls`, NO `autoplay` attribute: the page's own script plays the ones in view. In plain Chromium 4 of them play at once.
- The owner's diagnostics for this site: `video gate held=24 refused=4 opened=0` (the press-to-play rule holds them, as designed for films), later three clips decoding at once (480x270, 480x854, 480x270, 24 frames a second, 7 to 8 s) after a tap, and page lateness up to 1.9 s in 5 s.

## 2. The two faults
1. **A play / seek / mute / full-screen bar over each clip.** Kestrel's bar (VC1, videobar.js) comes up when the person taps a video whose page shows no controls of its own. For a film that is right ("anywhere I can start a video I must be able to control it"). A silent looping clip with no controls is not a film: it is a page animation (a GIF replaced by a video), and no browser puts controls over one.
2. **Lag.** Measured on the real page in headless Chromium, which also draws on the processor, with the processor slowed 6x to stand in for the tablet's four weak cores: frame rate with the clips paused about **60 a second**; with even ONE clip playing about **36**; with four **32** (and every playing clip also costs decoding on a processor that has little to spare, and on this tablet the hardware decoder handles only a few streams, the rest fall back to software). The page's drawing is tied to the clip's own frame rate, so a row of playing clips slows everything, including the scrolling text above them.

## 3. What alpha102 does
- **No bar on a decorative clip** (silent, looping, no `controls`): `VF.decorative()` in vfind.js, checked in videobar.js `judge()`; not even where the Settings row says "Always". A film (sound, or no loop, or controls) is unchanged. web-tests/videobar.test.js proves the four cases.
- **Limit looping page clips** (decoclips.js, Settings > "Limit looping page clips", on by default; the page script asks the app through the `guard` answer, flag 4096): a decorative clip that is PLAYING is paused when it leaves the screen, and only the two most visible play at once; the others are parked and played again, by the script, when they are visible and there is room. Rules: only clips already playing are touched (a clip the press-to-play gate holds never started: nothing changes); a clip the page itself pauses is not resumed; a page that keeps starting a parked clip is left alone after 4 parkings in 20 s; one 500 ms timer, only while such a clip exists; nothing runs, and the app is not even asked, for a page without one. web-tests/decoclips.test.js (7 tests, real Chromium) proves each rule.
- Counters (`videoclips.deco.bar / cap / off / back`) go to the Diagnostics line and the digest ("decorative page clips").

## 4. NOT proven on the tablet
The rules are tested in headless Chromium and on the JVM; the page on the tablet (Gecko, software drawing) is the proof. Said plainly: the cap makes the strip show still pictures (or dark boxes, for clips that never loaded) for the clips beyond two, and a page's own logic that waits for every clip may look odd; the Settings switch turns the limiter off. The press-to-play rule (the owner's own, ANS53) still decides whether a clip may start at all; this change does not touch it.

## 5. Open
- Dark boxes: a held clip with no poster shows a dark rectangle (the middle clip of the owner's screenshot). Showing its first frame needs the clip's data, which the owner's rule forbids before a press: only a page-supplied poster can be shown. Not changed.
- The strip itself moves with a script or CSS animation: Settings > Video > Calm pages stops it on pages that honour reduced motion; Settings > Network lab > Calmer redrawing halves the frame work for every page.
