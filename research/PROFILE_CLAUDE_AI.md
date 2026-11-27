# PROFILE_CLAUDE_AI: the first real recording of a page (owner, 2026-11-27, alpha111)

Owner, verbatim: "I sent a recording". File: `profiles/2026-11-27/071135-852b0843cb06.json.gz` (+ `.meta.json`) in the PRIVATE repo (Kestrel-Private, origin/main); 3.6 MB; a reload of claude.ai, 44 s long, taken with Settings > Live diagnostics > "Record a profile of this page" on alpha111.
Read with `python3 -B -I tools/profile_digest.py` (newest file) or `... FILE`; `--shape` prints the layout. Tags: [M] measured in this file, [V] verified in code or in the earlier research files, [E] estimate, not measured.

## 1. What the file is (and is not)
- GeckoView returned the GPU process and the PARENT process. The PAGE process (where claude.ai's own scripts run) is NOT in it: it ran out of memory while it serialised its part. alpha112 records a shorter window and fewer threads (section 6); a second recording is the open owner step (ND2b).
- Kestrel G runs its extension in the parent process (`extensionsProcessEnabled(false)`, research/ADBLOCK_ENGINE.md item 7, FREEZE.md): the Shields JavaScript, Firefox's webRequest plumbing and the engine's own network handling all run on ONE thread, the parent's `GeckoMain` [V].
- Layout of the file (v36 processed profile): per thread `samples`, `stackTable`, `frameTable`, `markers`, `stringTable` as `{schema, data}` tables whose rows can be shorter than the schema; markers have a `phase`: 0 instant, 1 interval (both times), 2 interval START (startTime only, endTime 0), 3 interval END (startTime 0, endTime only). A network request is TWO phase-1 rows: a START row (cache "Unresolved": channel created to first answer) and a STOP/CANCEL row (the whole load, with cache verdict and bytes). The first digest counted START and END rows as separate things and so printed nonsense for the interval markers: fixed in alpha112 (`marker_intervals`, `network_report`, `extension_report`; `tools/test_profile_digest.py`).

## 2. The parent main thread is the bottleneck of this page load [M]
| Number | Value |
| :- | :- |
| Parent `GeckoMain`, whole 44 s | 7,775 samples, 19.5 s of processor = 44 % of one core: Idle 49 %, Other 15 %, JavaScript 14 %, Network 9 %, DOM 8 % |
| Parent `GeckoMain`, 8 s to 23 s of the load | 99 % busy (saturated) |
| Requests the parent handled | 1,301 (1,264 to assets-proxy.anthropic.com, 36 to claude.ai), 14.35 MB, 1,217 of them cache hits, 80 misses |
| What they are | JavaScript modules: claude.ai asks for its whole module graph at load (`modulepreload`, 139 bytes to a few hundred KB each) |
| Opened | 907 of them between 8 s and 16 s of the profile (109 / 306 / 211 / 281 per 2 s) |
| Ended | 899 between 16 s and 24 s (85 / 255 / 213 / 346 per 2 s): only 32 had ended before 16 s |
| Open to end per request | median 7.9 s, 90 % 8.3 s, longest 19.9 s: the SAME wait for a 139-byte file as for a 300 KB file |
| Open to first answer (START rows) | median 1.5 s, 90 % 2.1 s |
| Share of the busy window | Firefox's webRequest plumbing around the extension's listeners 34 to 38 % (whole run about 17 %), HTTP channel handling about 20 %, native code without symbols about 40 %; our own JavaScript (`moz-extension://`) about 1 % |

Reading: this is not the page's JavaScript running slowly in the parent, it is a QUEUE. About 900 requests arrive in a burst of 8 s, and every one of them costs the parent's main thread roughly 9 ms on a Cortex-A53 (about 5 ms of webRequest serialisation per listener call, 2 ms of Firefox's own tracking classifier, the channel itself); 900 x 9 ms is 8 s, which is the wait every request shows [E from [M] parts]. Nothing about the size of the file matters.

## 3. The extension's share, from Firefox's own markers [M]
| Marker (parent) | Count | Wait |
| :- | :- | :- |
| "Extension Suspend": a request held until the extension's BLOCKING `onBeforeRequest` answered | 1,254 | median 3.9 s, 90 % 4.4 s, longest 4.6 s (the first requests of the load, at 0.6 s with an idle thread, were held 45 to 73 ms) |
| "ExtensionParent" `webRequest.onBeforeRequest` event on its way | 1,254 | median 3.9 s |
| "ExtensionParent" `webRequest.onCompleted` event on its way | 1,326 | median 1.3 s, 90 % 4.1 s, longest 5.5 s |
| `onHeadersReceived`, `onBeforeSendHeaders`, `tabs.onUpdated` | 42, 36, 4 | milliseconds |
| `AntiTrackingChannelClassifier::CheckChannelHelper` (Firefox's own, per request) | 1,254 | median 2 ms, 90 % 8 ms (about 2.5 s of the main thread in the burst) |

These are wall-clock waits that overlap; they are not processor time. They say: every one of the ~1,250 module requests sat suspended for about 4 s while the parent worked through a queue, and the extension's `onCompleted` events (diagnostics bookkeeping that Shields does not need to block anything) doubled the number of events in that queue.

## 4. What alpha112 changes (ND2) and what is expected
- The Shields `onCompleted` listener (bookkeeping: pending requests when a page is left, cache / 304 counts, the page report's HTTP errors) now registers with `types: ['main_frame', 'sub_frame', 'stylesheet', 'xmlhttprequest']`. Firefox evaluates the type filter in the parent BEFORE it serialises the request for the extension, so a script, picture or font request no longer costs a second dispatch. For this recording: 1,326 of the 2,665 extension events disappear (about half), about 4 to 5 ms of the parent's main thread each [E] = roughly 5 to 6 s of the 15 s the main thread was saturated. `onErrorOccurred` keeps EVERY type: it fires only for a request that failed (3 in this load: it costs nothing per request), so a script or picture that failed to load stays in the page report. Only requests of the counted types are counted as started (`reqsT`) and as ended, and `rvStart` (the start time kept for a 304) follows, so nothing is left half counted. What the Diagnostics lose: the cache / 304 counts and the HTTP 4xx / 5xx answers of scripts, pictures and fonts (a 404 on a script is no longer in the page report; a network failure of one still is).
- NOT run on the tablet: the shorter load of claude.ai [E: first paint maybe 3 to 5 s earlier]. The second recording (ND2b) measures it: "Extension Suspend" median (3.9 s now), the count of `ExtensionParent` events (2,665 now), open-to-end median (7.9 s now).

## 5. What was considered and NOT done (ranked, with reasons)
1. `declarativeNetRequest` instead of the blocking `webRequest` listener: REJECTED for now. research/ADBLOCK_ENGINE.md (about line 167) already found that Gecko's DNR is a JavaScript linear scan with a 30,000 rule cap, evaluated in the same parent thread: it would not remove the per-request cost and cannot hold our lists.
2. Registering the blocking listener per tab so Shields-off sites pay nothing: the app and the extension have no common tab id (grep of ShieldsBridge / GeckoEngine found none), a new tab races the registration, requests with tabId -1 would escape. Needs a design first (ND2c).
3. Filtering the blocking listener by host: match patterns only INCLUDE; there is no exclude, so the blocker cannot skip first-party CDNs.
4. `extensionsProcessEnabled(true)`: moves about 1 % of JavaScript but not the plumbing (serialisation happens in the parent) and costs 50 to 80 MB of RAM on a 2 GB tablet. No.
5. Firefox's own tracking classifier (`AntiTrackingChannelClassifier`, about 2 ms per request, 1,254 times): candidate prefs (`privacy.trackingprotection.annotate_channels`, the classifier tables) change what Firefox does for cookies and fingerprinting; needs an A/B in the Network lab and the owner's say (ND2c).
6. `pagetime.js kind()` maps `initiatorType 'link'` to "css", so the page report's "css=982" for claude.ai was really 982 `modulepreload` JavaScript modules. A wrong label, not a cost: ND2d.

## 6. The recorder after this recording (ND1d changes in alpha112)
- The load window is 22 s (was 100 s), `NOW_MS` 15 s, four thread names (`GeckoMain`, `JS Helper`, `DOM Worker`, `StyleThread`), features `js` + `cpu` only, so the page process has a chance to hand over its part. The toast says "first 22 seconds". `window_ms` is in the meta file.
- If the page process is still missing, `tools/profile_digest.py` says so at the top ("THE PAGE PROCESS IS NOT IN THIS RECORDING") instead of showing the busiest thread of another process.

## 7. How to use this next time
`python3 -B -I tools/profile_digest.py` prints: gathering log and settings, share per category per thread, busy seconds, script files and functions, label frames, markers (intervals joined), NETWORK (requests once each, opened / ended per 2 s) and EXTENSION PLUMBING (held requests, events). A number that looks wrong: `--shape`, fix the reader, keep the raw file.
