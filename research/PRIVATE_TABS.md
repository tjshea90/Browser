# PV1: private tabs - what could be saved or sent, what was found, what was fixed (2026-11-21, alpha86)

Owner, verbatim: "Confirm that if I'm using private tabs that no data or information is ever saved at any time or sent anywhere from private tabs. If this is not the case, fix it".
Method: every place the code writes to disk, to preferences or to the network (grep of writeText / writeBytes / FileOutputStream / edit() / HttpURLConnection / the download and engine layers), plus every Health.log line that carries a site name, each followed from a private tab to its sink.
Status words: EARLIER = already safe before this session (code read, test named where one exists); FIXED = a hole found and closed in alpha86 (test named); ENGINE = relied on from GeckoView's private mode, not testable here; CANNOT = cannot be closed, said plainly.
NOT run on the tablet: everything below is code reading and unit tests; the engine's own private mode (ENGINE rows) was not observed.

## A. Saved on the tablet
| # | Sink | Result |
| :- | :- | :- |
| 1 | Browsing history (BrowserDb.recordVisit) | EARLIER: `!tab.isPrivateTab` at the one call site |
| 2 | Typed addresses, searches, suggestions | EARLIER: suggestions read history only; there is no list of typed searches |
| 3 | Open-tab list and each tab's back stack (session.txt, TabStateStore) | EARLIER: `tabs.filter { !it.isPrivateTab }` ("never written anywhere") |
| 4 | Last-look picture and tab thumbnails (LastLook, ThumbStore) | EARLIER: `lookSkip` answers "quiet" for a private tab; `ThumbStore.save` only when `!isPrivateTab` |
| 5 | Downloads list and notifications | EARLIER: a private download has a negative id, is never inserted in the database (`if (!r.priv) store.insert`), never snapshotted, never offered to the phone (`offload`: `src.priv -> null`), its notification says "A private download", video-stream records live in a cache folder that is wiped at start. The FILE is saved in Downloads because the person said OK in a dialog that says so: by design |
| 6 | Per-site Shields choices (site_shields.txt, backups) | FIXED: the Shields panel opened from a private tab WROTE that site's name to disk (the writer only skipped the WebView build's private process). Now a change from a private tab is applied in memory only (`Shields.setOverride(.., fromPrivate)`), never written and never in a backup, and taken back when the last private tab closes (`Shields.endPrivate`). Tests ShieldsPrivateTest (4) |
| 7 | Keyboard learning (glide_personal.txt, word list, thumb habit) | EARLIER: words and contexts only when `learnAllowed()` (not in a private tab, not in a password field). FIXED: the "thumb habit" (where the thumb lands) was learned in a private tab: now gated too. Test PrivateTabsTest (every `.accepted(` sits behind `learnAllowed()`) |
| 8 | The system keyboard's own learning | EARLIER: address bar, Home box and page text fields ask for `IME_FLAG_NO_PERSONALIZED_LEARNING` in a private tab; autofill is excluded. FIXED: the find-in-page box and the video rename box now always ask |
| 9 | The event ring, the saved last-run notes, the crash note, the Diagnostics text | FIXED: many lines named a site (a frame's host, a stream server, an address) and some call sites forgot to ask "is this a private tab?" (engine etp host; the Engine line printed `current <address>` of the front tab whatever its kind). Now (a) those two are fixed at the source, (b) a safety net, `PrivateGuard`, masks EVERY site name and address in every line entering the ring while a private tab exists and for two minutes after the last one closed (`core PrivateScrub`, `PrivateWindow`), (c) the crash note masks exception messages in the same window, (d) private tabs are not counted in any tab count. Tests PrivateTabsTest (canary site names searched for in the ring, the journal, the crash note), PrivateScrubTest |
| 10 | The engine's crash reports (memory dump + details with the page address, up to 7 days in the profile) | FIXED: nothing used the dump; now every dump is deleted and every details file is cut to the keys Diagnostics reads, at start and 15 s after each engine crash or kill (`CrashNote.scrubEngineReports`). Between a crash and that moment the files exist (the engine writes them): CANNOT be closed without a crash-handler service of our own. Test PrivateTabsTest |
| 11 | Live diagnostics (journal on disk, tables on disk, upload to GitHub) | FIXED: an automatic send could fire while a private tab was in front and its snapshot included the Engine line with the private page's host. Now nothing is built or sent while a private tab exists or for two minutes after the last one closed (`LiveDiag.request` and `upload` both ask), the journal receives masked lines only, and the counts leave private tabs out. Tests PrivateTabsTest, LiveDiagTest |
| 12 | The engine's own private storage (cookies, cache, local storage, permissions, history, HSTS) | ENGINE: sessions are `usePrivateMode(true)`; the engine keeps these in memory and forgets them when the last private session closes (the app keeps one hidden private "anchor" session so a replaced session is never the last: ANS58c). Not testable here |
| 13 | Android's Recents picture | EARLIER: `FLAG_SECURE` while a private tab (or the private tab grid) is in front, so the system keeps a blank card |
| 14 | The built-in extension's storage | EARLIER: only the filter-list image (IndexedDB); the extension is allowed in private windows so Shields work; nothing per page is stored (grep) |
| 15 | Back pictures, back-scroll places, Instant Back (no-store made cacheable) | EARLIER: in memory only; the extension changes nothing for a private tab (web-tests/background.test.js BK10) |
| 16 | Kestrel's own video player and the video finder | EARLIER: requests carry the private flag (`FETCH_FLAGS_PRIVATE`), the player keeps its position in memory only and logs nothing for a private film. The log lines of the finder that named stream hosts are covered by row 9 |
| 17 | Preferences (SharedPreferences) | no per-site or per-page data except rows 6 and 7 (grep: no string sets, no desktop-site list, no permission store) |

## B. Sent from the tablet
| # | Path | Result |
| :- | :- | :- |
| 18 | The page itself, its third parties, the DNS lookup of the site | CANNOT: loading a page is sending those requests; a private tab only keeps them from being recorded here |
| 19 | Pages through the phone (the Courier proxy) | CANNOT per tab: one proxy setting for the whole engine. The phone IS the hotspot that carries all the tablet's traffic; its Courier keeps no record of tunnel destinations in a release build (it logs them only in a debug build). Downloads: a private download is never handed to the phone (row 5) |
| 20 | Connection warm-ups (preconnect) | EARLIER: never for a private tab, nor for a page a private tab holds (core WarmPolicy, tested) |
| 21 | The engine's Safe Browsing (SECURITY.md item 8) asks Google about the hash prefix of an address when a local list matches it, in a private tab too | FIXED: while a private tab exists the engine's malware and phishing address checks are switched off by user-branch overrides (`GeckoEngine.pauseSafeBrowsing`), and cleared when the last private tab closes. The price: regular tabs have no malware / phishing address check in that time (the lists keep updating). Source-pinned; the preference names are in the bundled engine (tools/check-gecko-prefs.sh: 51 of 51 found); whether the engine honours them at once was NOT observed |
| 22 | Live diagnostics upload | row 11 |
| 23 | Update check, filter-list refresh | they send no page data (an ETag request, a list fetch) |
| 24 | The video finder's and the download manager's own requests for a private tab | EARLIER: made with `FETCH_FLAGS_PRIVATE` (net/GeckoFetcher) |
| 25 | Search suggestions, telemetry, remote debugging, page console | off (`consoleOutput(false)`, `remoteDebuggingEnabled(false)`, no suggestions unless the owner enables them); the app has no analytics |
| 26 | Link prefetch (`network.prefetch-next`) | CANNOT per tab: a pref for the engine; it only fetches pages the page itself names |

## C. What a person can see for themselves
Diagnostics now has a line "Private tabs: ..." (safety net ON / idle, how many lines had a site name masked, how many live-diagnostics sends were held back, and whether the engine's address checks are paused). It carries no page data.

## D. The standing rule for new code
Anything that writes a file, a preference or a log line, or sends a request, from a place a page can reach, asks whether the tab is private first; a log line that names a site goes through `Health.log` (the safety net masks it), never to a file of its own. New persisted per-site data must be skipped or session-only for a private tab and have a test in the style of PrivateTabsTest (a canary site name that must not be found).
