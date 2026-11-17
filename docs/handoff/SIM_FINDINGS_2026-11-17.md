# What the keyboard + browser daily-use simulation found about Kestrel (2026-11-17, session ccr-5531c47b-hl1gzo)

Written for whoever works on the browser next. Nothing in this file changed any code of the other Browser session: it is a NEW file on the branch of the session that built the simulation.
The simulation lives in the Keyboard repository (`tjshea90/Keyboard`: `.github/workflows/daily-sim.yml`, `tools/sim/`). It builds Kestrel G (x86_64 debug, from a READ-ONLY checkout of a Browser branch) and Swoop,
boots an Android 10 KVM emulator shaped like the owner's tablet (600 x 1024 at 170 dpi, 2 GB, low-RAM flag), makes Swoop the keyboard and plays a person: it types, glides, opens pages and films, and reads
Kestrel's own `KestrelHealth` lines. **Emulator numbers are NOT the tablet's** (x86_64, software GPU, a fast CPU): compare them only with other runs of the same kind. The owner's link is mostly fast wifi, so the
runs are at full speed (the 5 Mbit/s hotspot imitation, `net=hotspot`, is an optional second pass).

## Page loads at full speed (run 44164818105, `Health: page loaded ...`)
| site | first paint | load finished | requests blocked |
| :- | -: | -: | -: |
| en.wikipedia.org | 552 ms | 1205 ms | 0 |
| news.ycombinator.com | 386 ms | 696 ms | 0 |
| lite.cnn.com | 195 ms | 246 ms | 0 |
| developer.mozilla.org | 441 ms | 1036 ms | 0 |
| developer.android.com | 762 ms | 1952 ms | 0 |
| docs.python.org | 661 ms | 730 ms | 0 |
| kotlinlang.org | 2773 ms | 2966 ms | 2 |
| lobste.rs | 343 ms | 701 ms | 0 |
| apnews.com | 2376 ms | 5911 ms | 54 |
| archive.org | 800 ms | 1612 ms | 0 |
All ten loaded (`ok=true`). The two slow ones to look at on the tablet: kotlinlang.org (first paint almost as late as the end of the load: a script-built page) and apnews.com (54 blocked requests, 5.9 s).

## Things seen in the Health lines of a cold start (emulator)
* `ui slow 832ms GeckoSession.<init>:1796 < BrowserActivity.createSession:1388 < BrowserActivity.ensureSession:1407 < BrowserActivity.loadUrl:1875` - the very first GeckoSession is created on the UI thread and takes about 0.8 s here
  (Choreographer: Skipped 53 frames, Davey 1 s, twice). `start open cold 1230 ms`, `start paint cold 2180 ms`. On the 4 x A53 tablet it will be slower; the creation of the first session is the one thing a person feels at start-up.
* `engine network-profile standard trr=2 http3=true` was chosen on the emulator's fast link (the fast-wifi profile works as designed).
* Memory with one page open: Kestrel 238 to 257 MB PSS (debug build, emulator), Swoop 40 to 44 MB.

## An emulator hang worth knowing about
After about 40 minutes of opening pages the emulated Android's own `system_server` watchdog fired (`android.fg waiting to lock ActivityManagerService`, `*** GOODBYE!`) and every adb call timed out. It is the emulator
host (2 vCPUs, software GL) as likely as memory pressure; the runs are now split into pieces (`suite=pages`, `suite=video`) and wait for Android to come back after a hang.

## What the simulation proved about the Swoop link (it is Kestrel code, so it belongs here)
Typing and gliding through the real Swoop keyboard into page text boxes (text, search, textarea, contenteditable, url, e-mail) is exact (7 of 7 phrases, 20 of 20 glided words per box kind), the address box gives Swoop
`kind=OMNIBOX omnibox=true`, a private tab gives `noLearn=true`. Not proven anywhere yet: the panel opening Swoop floating, the keyboard picker, the hand-over button.
