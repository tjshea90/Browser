# YouTube ad blocking and skipping - notes (RX3-Y1..Y5, RX3-G7, RX4)

Rows: RX3-Y1..Y5 and RX3-G7 (research/REVIEW-EXTERNAL-3.md part Y), RX4 (the owner's screenshot of 2026-11-14). Newest finding first. Everything here is ASCII; numbers carry device, build and date.

## 1. YouTube IS reachable from the cloud sandbox (2026-11-14; corrects REVIEW-EXTERNAL-3 section 12 and Y3)
The review said real YouTube was never reached from the sandbox. It can be: Playwright Chromium started with `proxy: { server: process.env.HTTPS_PROXY }` and nothing else (the agent proxy's CA is already in the browser trust store).
Probe: `node tools/sitelab/yt_probe.js <video id>` prints the page's own `ytInitialPlayerResponse.playabilityStatus`, the count of `adPlacements` and the visible text for three user agents (mobile site with the tablet's Firefox-Android UA, mobile site with a Chrome-Android UA, desktop site).
Limits: Chromium is not Gecko. It shows what YouTube's SERVERS answer and which data and selectors exist, not how the tablet's engine behaves. A first visit without cookies sometimes gets a 302 (probably the consent or bot page): a live check must follow it or set the consent cookie (open item for RX3-Y3).

## 2. RX4 - the owner's screenshot "Video unavailable - This content isn't available on your mobile browser"
Address: `m.youtube.com/watch?v=pQ73wo2JMb8` (a YouTube Movies & TV listing, label "Preview only"). Plain Chromium, no Shields, no extension:
- mobile site, tablet UA (Firefox-Android) and mobile site, Chrome-Android UA: HTTP 200, `playabilityStatus` = UNPLAYABLE "Video unavailable", and the visible text is word for word the screenshot's ("Open App / Video unavailable / Watch on the YouTube app / This content isn't available on your mobile browser / ... Comments are turned off").
- desktop site: UNPLAYABLE, "This video is not available", "Preview only", no streaming data.
- baseline, an ordinary public video (dQw4w9WgXcQ), mobile site with the Chrome-Android UA: `playabilityStatus` OK with streaming data. (The two Firefox-UA runs of the baseline got a 302 and no player data, see section 1.)
Verdict: the message is YouTube's own answer for that listing. Our rules did not cause it, and Shields off will not change it. The owner was told in plain words and asked to try an ordinary video; open question for the owner: what the screenshot was meant to show (it came without words).
