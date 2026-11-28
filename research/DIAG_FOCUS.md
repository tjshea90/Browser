# DIAG_FOCUS: "Focus future diagnostics on speed of app, page loading speed, video playback, page compatibility, and privacy and security" (owner, 2026-11-23, PERF16)
Owner, verbatim, after installing alpha98 ("It seems faster. I sent the diagnostics again."): "Focus future diagnostics on speed of app, page loading speed, video playback, page compatibility, and privacy and security".
Privacy note: this repo is PUBLIC. The owner's per-site numbers stay in the private diagnostics repo (`python3 -B -I tools/diag_digest.py` reads them); this file names site KINDS only.

## 1. What the owner's alpha98 uploads said (tab switching, PERF15 check)
- Live tab switches (the other tab was still alive): 81 to 433 ms to the first frame, median about 300 ms. That is the fix working: alpha98 stopped putting the second tab to sleep at Android's trim level 80 when 400 MB or more were free (research/TAB_SWITCH.md).
- Revives (the tab had been put to sleep): still seconds, as expected; they are rare now.
- Owner on the tablet: tab switching "seems faster". Not yet proven by a long session: the next uploads show whether the sleep rule holds when memory is low.

## 2. The five areas: what the digest now answers, what the tablet now logs
The digest (`python3 -B -I tools/diag_digest.py`) prints a FOCUS REPORT first, before the older sections. Each line carries a verdict (OK / WATCH / BAD) and the threshold it was judged against, so a line can be wrong in the open and fixed in the open.

| # | Area | Measured by (tablet) | Verdict rule (digest) |
| :- | :- | :- | :- |
| 1 | **Speed of the app** | cold start to first paint (`start paint cold`), live tab switch time (`tabs switch-done ... first-frame=`), dropped-frame share, parent-process CPU, memory trims | cold start median <= 10 s OK, <= 20 s WATCH; live tab switch <= 500 ms OK; dropped frames <= 2 % OK; CPU > 40 % average WATCH |
| 2 | **Page loading speed** | `page timing` (first contentful paint, load end), blocked requests per page, real load failures (cut-short loads are not failures: `cut=1`) | first contentful paint median <= 2.5 s OK; real failures > 5 % WATCH (counted from alpha96 uploads only: older ones counted cut-short loads) |
| 3 | **Video playback** | the existing `video ...` lines (gate, errors, stalls, decoder, quality) | stalls, element errors with no data, refused frame rates, hand-declined plays, listed with the site kind |
| 4 | **Page compatibility** | file attach probe (`filepick page-got`), Back verdicts (`back press ... left=`), pop-up and redirect blocks, the page's own errors, per-site Shields exceptions | attach decode failures, Back kept / refused, sites needing an exception |
| 5 | **Privacy and security** (NEW tablet lines) | `sec` lines: `https-upgraded`, `https-unavailable`, `cert-error`, `popup-blocked`, `perm-asked` (kinds `location`, `drm`, `other`; the autoplay questions of video pages are the video gate's) | counts printed with examples (host only); a certificate error or a permission question is always shown |

## 3. Rules kept (owner 2026-11-21, PV1; research/PRIVATE_TABS.md)
- `sec` lines are written only for NORMAL tabs (`BrowserActivity.sec` returns for a private tab, and the live diagnostics send nothing while a private tab exists). Guarded by OwnerRulesTest `perf16SecurityDecisionsAreToldInSecLinesForNormalTabsOnly`.
- `sec` is never thinned by the journal (every security decision is kept); the bookkeeping kinds (`gecko first-composite`, `win focus`, `tabs look-*`, ...) are cut to 8 examples per upload so the room goes to the five areas. Guarded by DiagJournalTest `bookkeepingLinesAreKeptToAFewExamplesAndSecurityLinesAreNeverThinned`.
- The scrubber lets the `sec` layer through (DiagScrub `LAYERS`), and still removes paths and query strings; only the host is sent.

## 4. What was NOT proven on the tablet
The new `sec` lines and the thinning were tested on the JVM and in the digest self-test only. The first upload from an alpha99 install shows whether a real certificate error, a pop-up block or an https upgrade writes its line, and whether the focus report reads right on real numbers.

## 5. Still open from the numbers (alpha98 uploads)
- Cold start median about 18 s, dominated by the restore of one very heavy chat page (about 1200 requests): owner decision pending on a lazy restore of the last tab (research/INSTANT_BACK.md section 4, item 5).
- Real load failures about 8 % since alpha96 (4 of 50): the digest names the failing site kinds; check whether they are the hotspot or a site problem before changing any rule.
- Back: the engine kept 4 of 13 pages; the rest were refused while "loading". The alpha97 stop-on-leave is the change under test.
- A kept page is where the engine left it: the saved scroll place is only a hint, so the digest no longer counts a kept page as a missed place.
