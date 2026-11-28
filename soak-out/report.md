# Kestrel emulator lab: gp run

Run: 2026-11-16 21:13 UTC, 3 scenes in 2 min. Device: Android 10 emulator, 600x1024, package app.kestrel.browser.gecko.debug, network unshaped (the runner's own fast link). **x86_64 + software GL is NOT the tablet: only behaviour counts, not absolute times.**

## Summary

Scene results: failed 2, ok 1
The emulated phone's own system restarted 0 time(s), the emulator itself started again from scratch 0 time(s) (an overloaded emulator, not the app).
App restarts / crashes seen: 0. Issues logged: 19 (high 19, medium 0, low 0).

## Issues (what to look at, most serious first)

- **high / video-gestures**: A: Kestrel's own player started the five-minute film and showed its first picture native lines []
- **high / video-gestures**: A: a drag UP on the right side raises the volume ([]) volume lines []
- **high / video-gestures**: A: a drag DOWN on the right side lowers it volume lines []
- **high / video-gestures**: A: a drag up on the left side changes the brightness ([]) brightness lines []
- **high / video-gestures**: A: a plain sideways drag (no long press at all) seeks forward ([]) seek lines [], cmd lines []
- **high / video-gestures**: A: a finger resting on the left side is told 'Slide up or down for brightness' (never 'slide left or right to seek' on a side) hint lines []
- **high / video-gestures**: A: a finger resting on the right side is told 'Slide up or down for volume' (never 'slide left or right to seek' on a side) hint lines []
- **high / video-gestures**: A: a finger resting on the middle side is told 'Slide left or right to seek' (never 'slide left or right to seek' on a side) hint lines []
- **high / video-gestures**: A: after a rest, a drag right of about 4 cm still jumps forward by tens of seconds ([]) seek lines [], cmd lines []
- **high / video-gestures**: A: the film really got the command (a skip) cmd lines []
- **high / video-gestures**: A: and with no rest at all the same drag jumps the same ([] s against [] s) jumps [] []
- **high / video-gestures**: A: the farther the drag the bigger the jump (6 cm: [] s against 4 cm: [] s) jumps [] []
- **high / video-gestures**: A: a drag LEFT rewinds ([]) jumps []
- **high / video-gestures**: A: a drag that also goes an inch up is finer (about a third: [] s against [] s) jumps [] []
- **high / video-gestures**: A: a double tap on the right side jumps 10 s forward cmd lines []
- **high / video-gestures**: A: and the controls are away again (the first tap showed them at once, the seek put them away): shown 0, hidden 0 shown 0 hidden 0
- **high / video-gestures**: A: Back ends the player and the brightness is the tablet's again no 'native stop' line
- **high / video-gestures**: B: the page told where its film is no layout row
- **high / inline-quality**: the page told where its film is (layout row) no row

## The quality list inside the page (ANS60c, owner message 60), on the real engine

| check | result | detail |
| :- | :- | :- |
| the page told where its film is (layout row) | **FAILED** | no row |


## VLC-style gestures in Kestrel's players (ANS57, owner message 57), on the real engine

| check | result | detail |
| :- | :- | :- |
| A: Kestrel's own player started the five-minute film and showed its first picture | **FAILED** | native lines [] |
| A: a drag UP on the right side raises the volume ([]) | **FAILED** | volume lines [] |
| A: a drag DOWN on the right side lowers it | **FAILED** | volume lines [] |
| A: a drag up on the left side changes the brightness ([]) | **FAILED** | brightness lines [] |
| A: a vertical drag in the middle changes nothing (VLC's dead zone) | ok |  |
| A: a plain sideways drag (no long press at all) seeks forward ([]) | **FAILED** | seek lines [], cmd lines [] |
| A: a finger resting on the left side is told 'Slide up or down for brightness' (never 'slide left or right to seek' on a side) | **FAILED** | hint lines [] |
| A: a finger resting on the right side is told 'Slide up or down for volume' (never 'slide left or right to seek' on a side) | **FAILED** | hint lines [] |
| A: a finger resting on the middle side is told 'Slide left or right to seek' (never 'slide left or right to seek' on a side) | **FAILED** | hint lines [] |
| A: after a rest, a drag right of about 4 cm still jumps forward by tens of seconds ([]) | **FAILED** | seek lines [], cmd lines [] |
| A: the film really got the command (a skip) | **FAILED** | cmd lines [] |
| A: and with no rest at all the same drag jumps the same ([] s against [] s) | **FAILED** | jumps [] [] |
| A: the farther the drag the bigger the jump (6 cm: [] s against 4 cm: [] s) | **FAILED** | jumps [] [] |
| A: a drag LEFT rewinds ([]) | **FAILED** | jumps [] |
| A: a drag that also goes an inch up is finer (about a third: [] s against [] s) | **FAILED** | jumps [] [] |
| A: a double tap on the right side jumps 10 s forward | **FAILED** | cmd lines [] |
| A: and the controls are away again (the first tap showed them at once, the seek put them away): shown 0, hidden 0 | **FAILED** | shown 0 hidden 0 |
| A: Back ends the player and the brightness is the tablet's again | **FAILED** | no 'native stop' line |
| B: the page told where its film is | **FAILED** | no layout row |

- numbers: A volume levels []
- numbers: A brightness levels []
- numbers: A rest + 4 cm right: jumps []
- numbers: A no rest + 4 cm right: jumps []
- numbers: A fine: plain [] s, with 1 inch up [] s

## What the app says about itself (its own Diagnostics text at the end of the run)

- Page loads: ...

Engine ready 0.0 s after the app started.

## What this run cannot say

- Software GL and an x86 JIT: scrolling smoothness, GPU memory and absolute speeds are not the tablet's. Dropped-frame counts of the player page are the emulator's.
- Sites that block data-centre addresses show as UPSTREAM or as a consent / challenge page, not as app faults. A challenge page counts as a successful load here.
- Video decoding is software only (no MediaTek decoders): 'plays' and 'no errors' are meaningful, smoothness is not.