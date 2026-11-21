# VS2: a found video row must not disappear (owner message 2026-11-18, doraby, vidsrc + 111movies)

Owner, verbatim: "On https://doraby.com/ozark/ Vidsrc and 111movies wouldn't find a video download, even though a video was in the player. Sometimes it would show a video file can be downloaded in the list, then it would disappear after a few seconds, almost as if kestrel thought the page changed to a site with no video. Is there a way to capture the video source when it is found so it doesn't disappear from the list"

## Every way a found row can leave the list (read from the code, 2026-11-18)

| Path | Where | When |
| :- | :- | :- |
| the page changed | `BrowserActivity.pageStarted` -> `VideoFlow.clearTab` -> `MediaList.clear()` (engine `onPageStart`, and a typed address) | every top-level load, also a RELOAD of the same page |
| a player frame was replaced | extension `frameNavigates` -> `gone` -> `VideoFlow.gone` -> `MediaList.dropFrames` | the frame (or one around it) loads another document: a server switch, but also a player that reloads itself or an ad redirect inside it |
| clip rule | `MediaCandidate.counts()` / `MediaList.clipNextToFilm` / `markEarlierAds` | a stream under a minute next to one of 5 minutes or more, a pre-roll that played on the same element before a film 3x longer, a page element that calls it an advertisement |
| small file next to a stream | `MediaList.usableUnsorted` | a file under 4 MB is hidden as soon as the page has a stream |
| full list | `MediaList.removeLeastValuable` | 48 entries |
| not saveable | `unfit` | a stream whose playlist was read: live, protected, or under 20 s |

The clip / advertisement rules are on purpose (pre-rolls and previews are noise). Which path fired on doraby is NOT known (no Diagnostics from that moment). Health already says `media frame-gone ... dropped= left=` for the frame path; the page path logged nothing (now `media page-clear kept= dropped=`).

## What was built (alpha70)

CAPTURE (core `MediaList`, `PageIdentity`): a candidate that is offered AND has film evidence is captured: a stream whose playlist was read and runs 2 minutes or more, a file of 40 MB or more (or 4 MB with 2 minutes), anything the owner opened. A captured row
- survives a frame going away: it is marked `orphan` (kept, listed LAST) until a live player offers a film of its own (a newly captured film replaces it); the same address asked for again makes it live again;
- survives the same page loading again (same host + path + query, no fragment, no `www.`): a reload no longer empties the list; another page, another query (another episode) or about:blank forgets everything as before;
- is no longer hidden by the clip rule or by an advertisement label that arrives later;
- is never kept when it cannot be saved (live, protected).
A clip, preview or advertisement is NOT captured: those rules keep working. A stream nobody has read yet is kept as an orphan when its frame goes away (it may be the film), a read clip is dropped.

Also (VS1e): the finder's own request for a text/plain answer now tries two more header sets on 404 / 406 (a server that dislikes the headers often answers 404, not 403) and skips requests the page made with POST.

## Proof and limits
- core MediaListTest / MediaListFramesTest: 8 new tests, the 3 old frame-drop tests rewritten to say the new rule; PageIdentity tests.
- Lab: vs variant `keep` (the player frame goes to another document AFTER the stream was found and saved: the finder must still list it; Health `media frame-gone ... dropped=0`).
- NOT proven: which path fired on the owner's tablet; vidsrc and 111movies themselves (VS2c: what their players do that the finder misses). To learn that, the owner sends Diagnostics right after trying them: the Health lines `media maybe`, `maybe-refused`, `maybe-hit`, `maybe-skipped`, `frame-gone`, `page-clear` say what the finder saw and what it did with it.
