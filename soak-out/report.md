# Kestrel emulator lab: gv run

Run: 2026-11-16 18:02 UTC, 3 scenes in 1 min. Device: Android 10 emulator, 600x1024, package app.kestrel.browser.gecko.debug, network unshaped (the runner's own fast link). **x86_64 + software GL is NOT the tablet: only behaviour counts, not absolute times.**

## Summary

Scene results: failed 2, ok 1
The emulated phone's own system restarted 0 time(s), the emulator itself started again from scratch 0 time(s) (an overloaded emulator, not the app).
App restarts / crashes seen: 0. Issues logged: 20 (high 19, medium 1, low 0).

## Issues (what to look at, most serious first)

- **high / video-gestures**: A: Kestrel's own player started the five-minute film and showed its first picture native lines []
- **high / video-gestures**: A: a drag UP on the right side raises the volume ([]) volume lines []
- **high / video-gestures**: A: a drag DOWN on the right side lowers it volume lines []
- **high / video-gestures**: A: a drag up on the left side changes the brightness ([]) brightness lines []
- **high / video-gestures**: A: a long press and a drag right of about 4 cm jumps forward by tens of seconds ([]) seek lines [], cmd lines []
- **high / video-gestures**: A: the film really got the command (a skip) cmd lines []
- **high / video-gestures**: A: the farther the drag the bigger the jump (6 cm: [] s against 4 cm: [] s) jumps [] []
- **high / video-gestures**: A: a long press and a drag LEFT rewinds ([]) jumps []
- **high / video-gestures**: A: a drag that also goes an inch up is finer (about a third: [] s against [] s) jumps [] []
- **high / video-gestures**: A: a double tap on the right side jumps 10 s forward cmd lines []
- **high / video-gestures**: A: and the controls are away again (the first tap showed them at once, the seek put them away): shown 0, hidden 0 shown 0 hidden 0
- **high / video-gestures**: A: Back ends the player and the brightness is the tablet's again no 'native stop' line
- **high / video-gestures**: B: the page told where its film is no layout row
- **high / private-tab**: the private tab loaded the login page and was logged out the first time (no cookie yet) cookie rows []
- **high / private-tab**: a private session anchor was opened with the first private tab ('tabs private-anchor opened') anchor lines []
- **high / private-tab**: the app was left and Android asked for memory (trim 20 / 60 / 80): the private tab (id None) was NOT put to sleep (no 'tabs hibernate' line for it) private tab id None, hibernate lines for it 0, trim lines []
- **high / private-tab**: a second private tab sees the first one's login (the same private cookie jar) cookie rows []
- **high / private-tab**: a regular tab loaded the login page no cookie rows
- **high / private-tab**: ANS59: a REGULAR tab (id None) is not put to sleep either by a trip to another app (no 'tabs hibernate' line for it) and is not reloaded on return tab id None, hibernate lines for it 0, the page loaded again: cookie rows []
- **medium / tooling**: the lab could not rest a finger on the glass (neither input motionevent nor the raw touch device reached the app): [] (touch-probe.txt)

## A private tab keeps its login when the app is left (ANS58c, owner message 58), on the real engine

| check | result | detail |
| :- | :- | :- |
| the private tab loaded the login page and was logged out the first time (no cookie yet) | **FAILED** | cookie rows [] |
| a private session anchor was opened with the first private tab ('tabs private-anchor opened') | **FAILED** | anchor lines [] |
| the app was left and Android asked for memory (trim 20 / 60 / 80): the private tab (id None) was NOT put to sleep (no 'tabs hibernate' line for it) | **FAILED** | private tab id None, hibernate lines for it 0, trim lines [] |
| back in the app the private page was not reloaded (no second load) and so still logged in | ok |  |
| a second private tab sees the first one's login (the same private cookie jar) | **FAILED** | cookie rows [] |
| a regular tab loaded the login page | **FAILED** | no cookie rows |
| ANS59: a REGULAR tab (id None) is not put to sleep either by a trip to another app (no 'tabs hibernate' line for it) and is not reloaded on return | **FAILED** | tab id None, hibernate lines for it 0, the page loaded again: cookie rows [] |

## VLC-style gestures in Kestrel's players (ANS57, owner message 57), on the real engine

| check | result | detail |
| :- | :- | :- |
| A: Kestrel's own player started the five-minute film and showed its first picture | **FAILED** | native lines [] |
| A: a drag UP on the right side raises the volume ([]) | **FAILED** | volume lines [] |
| A: a drag DOWN on the right side lowers it | **FAILED** | volume lines [] |
| A: a drag up on the left side changes the brightness ([]) | **FAILED** | brightness lines [] |
| A: a vertical drag in the middle changes nothing (VLC's dead zone) | ok |  |
| A: a sideways drag WITHOUT the long press does not seek | ok |  |
| A: a long press and a drag right of about 4 cm jumps forward by tens of seconds ([]) | **FAILED** | seek lines [], cmd lines [] |
| A: the film really got the command (a skip) | **FAILED** | cmd lines [] |
| A: the farther the drag the bigger the jump (6 cm: [] s against 4 cm: [] s) | **FAILED** | jumps [] [] |
| A: a long press and a drag LEFT rewinds ([]) | **FAILED** | jumps [] |
| A: a drag that also goes an inch up is finer (about a third: [] s against [] s) | **FAILED** | jumps [] [] |
| A: a double tap on the right side jumps 10 s forward | **FAILED** | cmd lines [] |
| A: and the controls are away again (the first tap showed them at once, the seek put them away): shown 0, hidden 0 | **FAILED** | shown 0 hidden 0 |
| A: Back ends the player and the brightness is the tablet's again | **FAILED** | no 'native stop' line |
| B: the page told where its film is | **FAILED** | no layout row |

- numbers: A volume levels []
- numbers: A brightness levels []
- numbers: touch way raw: the app saw nothing
- numbers: A hold + 4 cm right: jumps []
- numbers: A fine: plain [] s, with 1 inch up [] s

## What the app says about itself (its own Diagnostics text at the end of the run)

- Page loads: ...

Engine ready 0.0 s after the app started.

## What this run cannot say

- Software GL and an x86 JIT: scrolling smoothness, GPU memory and absolute speeds are not the tablet's. Dropped-frame counts of the player page are the emulator's.
- Sites that block data-centre addresses show as UPSTREAM or as a consent / challenge page, not as app faults. A challenge page counts as a successful load here.
- Video decoding is software only (no MediaTek decoders): 'plays' and 'no errors' are meaningful, smoothness is not.