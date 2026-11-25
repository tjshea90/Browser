# TEST_PLAN_alpha103: what the owner tests and when the diagnostics are sent (TEST1, 2026-11-24)
Owner, verbatim: "Give me a simple step by step guide on what to test and when to send the diagnostics so you have data to work from".
What the data is for (the report script prints each line): FOCUS REPORT sections 1 to 5, SETTING A/B (page loads with and without a switch), PAGE ANIMATION (frame cadence), bot checks, the memory-signal gate, Decorative page clips, the start screen after a long break.
Facts that shape the plan: each upload carries the newest 600 events, so SEND OFTEN, right after each test, not once a week; "Send now" is Settings > Live diagnostics (for Claude) > Send now; the browser also sends by itself at most 4 times a day; private tabs record nothing, so test in NORMAL tabs; every experiment switch applies at the NEXT START (close Kestrel from the recent-apps list, open it again).

## Day 0 (5 minutes)
1. Install alpha103 from the Release link, open it once.
2. Settings > Live diagnostics (for Claude): Live diagnostics is ON; tap Send now once (marks the start of alpha103).
3. Leave the three experiment switches in Network lab OFF.

## Day 1: the normal-use baseline (about 15 minutes of use, spread over the day)
After EACH test: Settings > Live diagnostics (for Claude) > Send now.
- Test A, the heavy page: open the chat page you use most, wait until it is usable, scroll a little. Open a second tab, switch between the two tabs 5 times, press Back on a few pages. Send now.
- Test B, the clip page (the sports-odds site): open it, scroll, tap one clip once, wait 30 s. Tell me in words: is it still laggy, is there a play bar, are some clips dark. Send now.
- Test C, a bot check: open a site that shows a "Just a moment" or "Verify you are human" check; stay until it finishes. Tell me the site and about how many seconds it took. Send now.
- Test D, video: play one ordinary video (not a clip strip) for one minute. Send now.
- Test E, the start screen after a long break (next morning): after 4 or more hours without using Kestrel, close it from the recent-apps list and open it again. Expect Home with "Continue: ..."; tap it. Tell me if it did not appear or annoyed you. Send now.

## Days 2 to 5: one experiment per day, all OFF except the one being tested
Each day: (1) Settings > Network lab: switch ON that day's experiment, leave the others OFF; (2) close Kestrel from the recent-apps list and open it again; (3) repeat Tests A and B (same pages as Day 1, about 10 page loads in all; for Day 3 open the heavy page TWICE); (4) Send now at the end of the day; (5) switch it OFF again.
- Day 2: Cheaper gradients (experiment).
- Day 3: Compile scripts on first load (experiment).
- Day 4: Parallel script clean-up (experiment).
- Day 5 (optional): Calmer redrawing (experimental); then tell me whether scrolling feels worse or better.

## What to tell me in the chat (a few words each)
Which test felt faster or slower, anything broken (missing bands of text, clips that stay dark, the Continue line in the way), and which site the bot check was.
