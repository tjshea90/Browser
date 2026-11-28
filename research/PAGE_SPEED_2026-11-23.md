# PAGE_SPEED_2026-11-23: "is there a way to speed up page loading" (owner, 2026-11-23, PERF14e)
Owner, verbatim: "I don't notice anymore drawing invisibility problems but is there a way to speed up page loading".

## 1. What the owner's own uploads say (all 28 uploads; page timing lines; medians in ms; sites with 2+ loads on each side)
Software drawing (the fix for the invisible blocks, ON since 2026-11-23) against hardware drawing:
| Site | first contentful paint hw -> sw | load finished hw -> sw |
| :- | :- | :- |
| a very heavy chat web app (about 1200 requests, 1100 scripts) | **2.0 s -> 14.6 s** | 23.9 s -> 24.6 s |
| a phone-carrier shop site | 2.3 s -> 2.4 s | 6.1 s -> 3.8 s |
| a video site's mobile pages | 5.1 s -> 1.2 s | 3.8 s -> 5.2 s |
The drawing fix costs one kind of page: the one whose scripts keep the four slow cores busy for 20 seconds (lateness 85 to 96 percent of its load): the processor that now also does the drawing cannot draw anything until the page's scripts give it a moment. The same page finishes loading just as fast; what changed is when the first picture appears. Ordinary pages are not slower. (The scroll feel was changed from Smoothest to Standard in the same hours: the three changes cannot be told apart from the uploads; the digest says so.)

## 2. What costs time on this tablet (so the levers can be ranked)
1. **The page's own scripts** (CPU-bound): 85 to 96 percent lateness on the heavy pages, 4 Cortex-A53 cores at 32 bit, ONE page process (dom.ipc.processCount = 1). Nothing in the browser makes a page's scripts shorter; what helps is not running other work next to them: other tabs asleep (Tabs kept ready: Few, already), Shields DOWN on the heavy app (its Shields A/B: 41 s with Shields up against 24 s down), Battery Saver off, no video behind.
2. **Drawing on the processor** (Software drawing): see above. Back to the graphics chip brings the invisible blocks back: the chip's driver is the fault (hardware drawing with every shortcut off still showed them; software drawing removes them). A faster safe middle way would need the faulty driver path to be named (candidates left: tile size, shader precompile, the GPU helper process).
3. **Main-thread redraws while loading** (animations of spinners and skeleton blocks at 60 a second): each costs layout + drawing work. Switch added in alpha97: Settings > Network lab > Calmer redrawing (layout.frame_rate 30), OFF by default; the digest section SETTING A/B compares page load times with it on and off.
4. **The link** (5 Mbit/s hotspot): the first byte comes in 0.1 to 0.8 s on all sites in the uploads, so it is not what makes the heavy pages slow; the page loads that are NETWORK-bound are the rare second kind (a redirect through a tracking host).
5. **Back and the first open**: see research/INSTANT_BACK.md (the engine's back/forward cache; the page's unfinished loads are cancelled when a link is tapped, alpha97).

## 3. Not done (ranked)
1. A faster safe drawing mode: bisect the faulty GPU path (tile size, GPU helper process off, shader precompile off) with one-minute A/Bs, now that the digest compares settings.
2. Per-site drawing mode (software only for the pages that showed blocks): the engine's drawing pref is global, not per page.
3. JavaScript tuning (delazification strategy, JIT thresholds): not measured on the tablet; the emulator cannot stand in for four slow cores.
