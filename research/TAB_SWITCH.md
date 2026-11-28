# TAB_SWITCH: "When i go between tabs it is very laggy and has to reload" (owner, 2026-11-23, PERF15)
Public repo: numbers only, no site names.

## What the alpha97 uploads say (the three uploads after the owner installed it, plus the earlier ones)
- Across all uploads 30 tabs were put to sleep (their engine session closed: the next switch to them loads the page again from its history). **16 of the 30 were put to sleep with a budget of ONE live tab**, at 289 to 402 MB free memory; the other 14 with a budget of 2 (the owner's "Few" setting, which is correct to enforce when a third tab opens).
- The budget of ONE comes from one rule: `LiveBudget.afterTrim` returned 1 for every Android memory callback of level 60 or more ("this process is a prime candidate to be killed") without looking at free memory. On this tablet Android sends level 80 (COMPLETE) within a second of EVERY time the app is left (the log shows `mem trim level=20` then `level=80` 0.7 s later, 19 to 25 times in one upload), with 400 to 540 MB free and the low-memory threshold at 144 MB. So every time the owner left Kestrel and came back, all tabs but the one in front had been closed, and the next tab switch loaded a page again (a heavy chat page takes 15 to 25 s to paint with Software drawing).
- Engine kills (`gecko onKill`): 2 in the whole log; the voluntary sleep was far more common than the real kill.

## Change (alpha98)
`afterTrim` for level 60 and up now follows the free memory: below 300 MB only the tab in front, below 450 MB at most the floor (2), otherwise the setting stands. Real numbers pinned in LiveBudgetTest: level 80 at 402 / 438 / 540 MB free with "Few" keeps both tabs. The risk, said plainly: Android may end the process while the app is away with two tabs open; the tabs then come back with their history as after any kill (the engine kill count of the next uploads shows if it happens more).
New measurement: `tabs switch-done tab= kind=live|revive first-frame=Nms` (the time from the switch to the engine's first frame of the tab in front) and the digest section TABS (sleeps by budget and free memory, switch times by kind), so "laggy" can be told apart: a switch to a live tab (swap of the engine session on the one shared view, a curtain until the first frame) or to a sleeping one (a new session and a page load).

## Not done
- The lag of switching between two LIVE tabs: no measurement existed; the new line gives it. Candidates: the session swap on the shared view re-creates the compositor frame (with Software drawing a CPU frame), the curtain waits for the first frame (1.3 to 3 s in the uploads: `curtain-up ... curtain-down why=first-paint / timeout`), the old tab's picture is not shown during the swap.
- A lighter "Many" budget by memory is not changed: a fixed setting of 3 or more tabs is still honoured whatever the memory.
