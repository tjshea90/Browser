# perf-kotlin.md - app-side (Kotlin) performance review for Kestrel G, task AA4

Read-only pass. Nothing was built, run or measured. Scope: `geckoapp/src/main/kotlin/**`, `app/src/main/kotlin/**` (ui, data, net, download, diag, update, shield), `core/` only where it sits on a hot path, the two manifests, `gecko-prefs.txt`, and a listener check of the extension JS.

**Code state read:** commit `a0a56be` (ckpt 1069). The tree moves under autosave, so every `file:line` below refers to that commit. A frozen copy of what I read is in `scratchpad/snap/` (same relative paths as the repo).
**Tags:** `[read]` = verified in the code, or in the GeckoView 160 API/bytecode (I ran `javap` on the local AAR in the Gradle cache). `[guess]` = inference or an estimate; no tablet or emulator number exists for it. Costs are for 4x Cortex-A53 (in-order), 32-bit, 2 GB, 800 MB to 1 GB MemAvailable, 5 Mbit/s (about 625 KB/s) hotspot.

## 0. Verdict

The app's own UI code is lean (coalesced `updateUi`, early-out `dock.bind`, 3 % progress steps, 4/s shield counter, touch-detail only on DOWN, adaptive keyboard poll, IO lanes split, shield worker at normal priority). I found no per-frame work on the UI thread that would hurt scrolling. What the owner experiences as "reloads", "blank pages" and "Back is not instant" comes from POLICY in `BrowserActivity`, not from slow code:

1. K1: only 2 sessions stay alive, and every memory-trim callback of level 10 or more (including "the user left the app") closes all the others. With 3 or more tabs in rotation this is a 100 % miss rate.
2. K2: after a process death, a swipe-away or an activity re-creation only the URL comes back (no Back stack, no scroll position, no form text).
3. K3: dark mode / font size / display size / language changes destroy the activity and close every session (known in FREEZE.md 4.8, still open).
4. K4: after the OS kills the engine process in the background, the app starts a new one at once, while it is not on screen.
5. K5-K7: nothing covers the page area while a revived tab loads; a link-launch loads the restored tab as well; nothing protects a page load from a running download.

## 1. Tab / memory policy as it is today (exact numbers)

| Item | Current value | Where |
| - | - | - |
| Live GeckoSessions | **2** on a low-RAM device (`isLowRamDevice != false`, true on this tablet: `ro.config.low_ram=true`), 4 otherwise | BrowserActivity.kt:630-632 |
| Which are kept | the 2 most recently used (`Tab.lastActive`, set in `show()` :534); the current tab and any tab with a live popup child are never hibernated, so popups can push the count above 2 | :633-640, `hasLiveChild` :621 |
| When it runs | `show()` :539 and the end of `ensureSession()` :617 | |
| `onTrimMemory` level >= 10 | hibernate EVERY tab except the current one (unless a popup child is live). No memory check at all. 10/15 = RUNNING_LOW/CRITICAL, **20 = UI_HIDDEN**, 40/60/80 | :366 |
| UI_HIDDEN (20) | sent when the process drops out of the visible states (Home, Recents, a full-screen picker, an external app link); probably not sent while the download foreground service keeps the process important | system behaviour [recalled from ActivityManagerService and the ComponentCallbacks2 docs; not re-read] |
| Level >= 60 and window has no focus | also hibernate the CURRENT tab (revived in `onResume` :233) | :367 |
| What "hibernate" does | `GeckoSession.close()`; regular tab keeps its `SessionState` (history, scroll, form data), **private tab drops it and sets no `pendingLoad`** | :644-651 |
| What revive does | new `GeckoSession`, `open()`, then `restoreState(state)` else `loadUri(pendingLoad)`; the page's JS, layout and image decode run again, its bfcache entries are gone with the closed session | :596-619 |
| Engine processes | one content process for ALL tabs (`dom.ipc.processCount = 1`, fission off), so hibernating a tab frees heap, not a process | gecko-prefs.txt, GeckoEngine.kt:50 |
| bfcache | `max_total_viewers = 4`, timeout 3600 s (the limit is global over all tabs [guess, nsSHistory]) | gecko-prefs.txt |
| Gecko's own reaction | only trim 15 (and `onLowMemory`) reaches Gecko while we are visible; it then wipes bfcache, decoded images and WebRender caches; no public switch filters it | SCROLL.md Q2 |
| Content process killed | `sessionGone`: first death in 15 s restores state, second reloads the URL, third shows the error page | :1591-1610 |
| What is saved to disk | `session.txt` = id, url, title, lastActive, parent, desktop per regular tab; written 600 ms after a change and in `onPause` | SessionCodec.kt:4-11, BrowserActivity.kt:1390-1402 |
| Thumbnails | 240 px RGB_565 in RAM (max 20), lossy WebP q60 in `cacheDir/thumbs` for regular tabs | ThumbStore.kt |

---

## 2. Findings, ranked

### K1 - HIGH - Two live tabs, and every trim >= 10 (incl. "user left the app") closes all the others

**Evidence [read]:** table above. `onTrimMemory` :361-368, `enforceLiveLimit` :630-641, `hibernate` :644-651, `ensureSession` :596-619.
The levels 10/15 are derived by Android from the number of cached processes, not from free RAM (SCROLL.md Q2, [recalled, unverified]), so on a Go device `level >= 10` is a poor proxy for pressure. Level 20 does not mean pressure at all.

**Why it hurts here:**
- LRU with capacity 2 under cyclic use of 3 tabs (A, B, C, A, B, C) misses on every switch; each tab is the least recently used one when its turn comes.
- Closing a tab reveals its parent (`doCloseTab` :677): in "search page, open 3 results, close them one by one" the search page is already hibernated at the third close and reloads at the moment the user expects it instantly.
- Every app switch (a picker, a link to another app, Home) hibernates the second tab even with 900 MB free.
- A revived tab loses its bfcache entries, so Back inside it is no longer instant either.
- A private tab that gets hibernated comes back **blank behind an address bar that still shows its URL**: `hibernate` :649 returns before `pendingLoad` is set, `ensureSession` then opens an empty session; Reload only reloads `about:blank`. With a limit of 2, opening a third tab silently kills one private page.

**Cost here [guess]:** a revive is a history load from the HTTP cache (GECKO_SPEED 5 says "should be served from the cache", unverified): 1-3 s CPU for a news page, 3-8 s or more for a JS app (Proton Mail), plus 5-10 s extra if the page is `no-store`. A live tab switch is under 100 ms. Per avoidable revive the user waits seconds and loses JS state.
Memory side [guess]: a light page costs 30-80 MB inside the single content process, a heavy one 150-300 MB. With 800 MB to 1 GB MemAvailable, 4-5 normal tabs fit.

**Fix (all in `BrowserActivity`):**
1. Replace the constant in `enforceLiveLimit` by a budget from real headroom. `MemAvailable` from `/proc/meminfo` (kernel 4.14 has it; `Health.meminfo` already reads that file, about 0.2 ms) is what the owner's "800 MB to 1 GB free" is. Starting table [guess]: >= 700 MB -> 6 live, >= 500 -> 5, >= 350 -> 4, >= 250 -> 3, below that or `lowMemory` -> 2. Keep 2 as the fallback if the file cannot be read.
2. Replace the unconditional `if (level >= 10) purge` (:366): at 15 and above purge down to 2 only when MemAvailable is below about 350 MB (Gecko flushes its own caches at 15 anyway); at 20 arm a 120 s timer (cancel in `onStart`), and when it fires with the UI still hidden shrink to 2 only if MemAvailable < 500 MB. Quick app switches then cost nothing. This is the opposite direction of HW_SPEED H6/H7 (hibernate earlier); the owner's AA4 asks for the reverse and the current policy is already stricter than the RAM needs.
3. Age-out: hibernate non-current tabs idle for more than about 20 min [guess] even when RAM is free, so a forgotten webmail/chat tab does not burn background timers and WebSocket work on 4 weak cores (inactive tabs are throttled, not stopped).
4. Private tabs: keep the URL in RAM (`pendingLoad = tab.url`, state still dropped) so a hibernated private tab reloads instead of going blank; it is the same string `tab.url` already holds.
5. Raise `browser.sessionhistory.max_total_viewers` together with the budget (already queued as gecko-prefs v5 "viewers 6"); the 4 viewers are shared by all live tabs.
6. Optional Settings row "Tabs kept ready: Few (2) / Auto / Many (6)" so the owner can A/B; Diagnostics already prints "live sessions N of M tabs" (:1215).

**Risk:** a bigger footprint makes the content process a more attractive victim when the user leaves for a heavy app (cached process, highest adj, largest first). Recovery is the same as today (`sessionGone` restores from `SessionState`), so the worst case equals today's typical case, but the CURRENT page can then come back as a restore instead of instantly. The 120 s timer is the compromise. Watch `gecko.kill`, `gecko.recover.*`, `mem.trim.15/20` in Diagnostics before and after.
**Verify:** emulator: open 5 tabs, `adb shell am send-trim-memory app.kestrel.browser.gecko.debug RUNNING_CRITICAL` and press Home; Diagnostics "live sessions" line must follow the table. Tablet: Diagnostics counters over a day of use.

### K2 - HIGH - Process death, swipe-away and activity re-creation restore the URL only

**Evidence [read]:** `TabRecord` has id, url, title, lastActive, parentId, desktop (SessionCodec.kt:4-11); `saveSessionNow` :1390-1402 writes only that; `restore` :461-482 sets `pendingLoad = url`, which `ensureSession` :610-615 turns into a plain `loadUri`. Meanwhile `tab.gecko.state` (a `GeckoSession.SessionState`) is already kept up to date in RAM (TabDelegates.kt:180). In GeckoView 160 `SessionState` has public `toString()` (JSON) and `static fromString(String)` [read, javap]; its keys are `history`, `scroll`, `formdata`, `scrolldata` [read, bytecode strings].
"Swipe away = quit" is deliberate (BrowserActivity.kt:305, ProcessLife.kt), and Android also kills cached apps on this device, so a cold restore is the normal way back into the browser after any other app.

**Cost [guess]:** the user lands at scroll 0 with no Back stack; a plain `loadUri` revalidates the document and every expired sub-resource (one round trip of about 100-300 ms each on the hotspot), where a history load serves from the cache first.

**Fix:**
- In `saveSessionNow` (pause + 600 ms debounce): for each regular tab with a state, write `state.toString()` to `filesDir/tabstate/<id>.json`. Call `toString()` on the UI thread (1-3 ms for about 20 KB [guess]; the state object is mutated on the UI thread), write on `Io.exec`; skip tabs whose state object did not change since the last write; cap a file at about 60 KB (drop the file, fall back to URL).
- In `restore`: try `SessionState.fromString` per tab inside try/catch, set `tab.gecko.state`, let `ensureSession` call `restoreState` (:604-609). Key the folder with `Flavor.engineVersion` and discard on mismatch (a new GeckoView may read old JSON differently).
- Delete the file in `doCloseTab`, `showNewTabPage`, `clearBrowsingData`, and wipe the folder in `coldStartClear` when `clearOnExit` is set.
- Privacy: `formdata` holds typed field text and the file is unencrypted storage. Inspect the JSON on the emulator and strip `formdata` (org.json round trip) unless the owner wants it; history titles and URLs are already in `session.txt`.
**Risk:** a restored page shows the cached copy (same as every browser's session restore; pull-to-refresh exists); a malformed or oversized state must never block the start (try/catch, size cap, fall back to the URL).
**Verify (emulator):** open a long page, scroll, open a second page, `adb shell am force-stop`, relaunch: scroll offset and Back must work; with `clearOnExit` on, the folder must be gone.

### K3 - MEDIUM (HIGH when it happens) - Dark mode, font size, display size or language change destroys the activity and closes every session

Known: FREEZE.md 4.8 and its P2; still open.
**Evidence [read]:** `geckoapp/src/main/AndroidManifest.xml:53` lists `orientation|screenSize|smallestScreenSize|screenLayout|keyboard|keyboardHidden|navigation` only (the WebView build's `PrivateActivity` has the full list). `onDestroy` :303 closes every session; with K2 unfixed the new activity restores URLs only (and only the selected tab loads). A change made while the app was stopped re-creates it on the next resume [recalled]. Battery Saver and a dark-theme schedule can switch dark theme on their own, without the owner touching the browser [recalled, Android 10 behaviour].
**New facts [read, javap/bytecode]:** `GeckoView.onConfigurationChanged(newConfig)` already forwards to `GeckoRuntime.configurationChanged(newConfig)`, which calls `GeckoSystemStateListener.updateNightMode(uiMode)`. So handling `uiMode` in the activity keeps `prefers-color-scheme` live without any code. Each re-creation also leaks two idle `HandlerThread`s: `ShieldsBridge` is constructed per activity (BrowserActivity.kt:203) and starts `kestrel-shield` and `kestrel-cosmetic` (ShieldsBridge.kt:48-52) that are never quit.
**Fix:** add `uiMode|locale|layoutDirection` to `configChanges` (no visual effect: dark-only palette, English strings). Leave `fontScale|density` out or add them knowingly: the programmatic UI converts dp/sp at construction, so it would keep the old size until the next start; they are rare and K2 makes the recreation cheap. In `ShieldsBridge` expose `quit()` and call it from `onDestroy`, or make the bridge a process singleton.
**Verify (emulator):** open two tabs, `adb shell cmd uimode night yes`; the Diagnostics "live sessions" line must stay 2 of 2 and the page must not reload.

### K4 - MEDIUM - A killed engine process is re-created immediately, even while the app is in the background

**Evidence [read]:** `onKill` (TabDelegates.kt:192) -> `sessionGone` (BrowserActivity.kt:1591-1610). For the current tab it calls `showRestoring` and `attachContent` -> `ensureSession` -> `GeckoSession.open` + `restoreState`, with no check that the activity is started. `ProcessLife.uiVisible` (set :223, cleared :265) says exactly that. `onResume` :233 already revives a tab whose session is null, so the deferred path exists.
**Why it matters here:** the kill happened because memory was short; spawning a new content process (about 150-300 MB [guess]) and loading a page right away, while another app is in front, takes memory and CPU from that app and invites the next kill. The 15 s double-death rule and the `n=` log field in `sessionGone` exist for that loop.
**Fix:** after the bookkeeping (state, `crashCount`, `pendingLoad`, log), `if (!ProcessLife.uiVisible) { restoringTab = tab; return }`; in `onResume` call `showRestoring` before `attachContent` for that tab.
**Risk:** low (one flag). **Verify:** emulator, kill the `:tab` child with the app on the home screen (needs root on the image [FREEZE.md 101]); no new `:tab` process until the app is opened.

### K5 - MEDIUM (perceived speed) - Nothing covers the page area while a revived tab, a cold start or a Back loads

**Evidence [read]:** `RestoreOverlay` is used only in `sessionGone` (:1612-1624). `ensureSession` (:596-619) and `restore` show nothing; at cold start the Home placeholder (:207-209) is replaced by an empty GeckoView. After `setSession` the SurfaceView keeps its last buffer until the new compositor draws [guess; SCROLL.md Q3 and FREEZE.md say the same], so the old tab's picture may stay under the progress line for the whole reload. `GeckoView.coverUntilFirstPaint(int)` exists in 160 (it sets the SurfaceViewWrapper background [read, bytecode]) and is not used.
**Fix:** in `ensureSession`, when `tab === current` and a state or URL is being loaded, call `showRestoring(tab, ...)` with the tab's thumbnail and a chip "Opening <host>"; it already hides on `onFirstContentfulPaint` (TabDelegates.kt:191 -> `firstPaint` :1627) and after 20 s. Also hide it in `errorChanged` and `pageFinished`. For cold start, load the tab's disk thumbnail on `Io.fast` before `restore()` posts. The 240 px preview is blurry at 2.5x; saving a 480 px copy of the current tab in `onPause` costs about 100 KB.
**Back on pages the bfcache cannot hold (no-store, unload handlers), PROMPT_AUDIT carry-over (c):** keep a small RAM-only map `url -> preview` fed by the existing post-load capture (4 entries, 480 px RGB_565 about 750 KB each); on Back read the previous entry's URL from `tab.gecko.state` (history list + `currentIndex`) and show that preview 120 ms after the Back unless the first composite already arrived. No extra GPU read-backs.
**Risk:** an overlay that never hides (timeout exists); private tabs have no preview (plain dark panel with the chip).

### K6 - MEDIUM, cheap - Launching the browser by a link also loads the restored tab

**Evidence [read]:** `restore` :461-482: `show(sel)` (:479) loads the saved selected tab immediately, then `openInNewTabOrCurrent(launch)` (:481) opens a second tab for the link (the restored tab is not a "new tab", so it is not reused). As the default browser, links from other apps are a main way in.
**Cost [guess]:** one extra full page load competing on the 5 Mbit/s pipe and in the single content process with the page the user asked for: plus 2-5 s for the intended page.
**Fix:** when `launch != null`, rebuild the tab list but do not select or attach the saved selection; select the new tab only. Care: `restore` removes the placeholder tab while `current` still points to it, and `openInNewTabOrCurrent` would reuse that dead tab (`cur.isNewTab`), so set `current = null` or add the new tab explicitly.

### K7 - MEDIUM - Nothing keeps background transfers away from the page being loaded

**Evidence [read]:** downloads default to no limit (`Prefs.dlLimitKBps = 0`, Prefs.kt:55) with up to 3 connections on low RAM (DownloadController.kt:94-96) and 4 MB Range windows through Gecko's own network stack (GeckoFetcher.kt:23-24, 28), i.e. the same connection pool as the page. SettingsView.kt:72 says "a big download makes browsing crawl" but offers only a manual limit. `DownloadController.init` runs `resumeInterrupted()` at every start (:133). `onResume` runs the update check (BrowserActivity.kt:237 -> `UpdateManager.check`: engine metadata + up to 5 GitHub requests over TLS to two hosts, daily) and posts the list refresh for +90 s (:239-249: up to 7 lists, then `Engine.swap` and a new image sent to the extension).
**Cost [guess]:** 3 download flows take about half the pipe from a page that opens 10-20 flows; a daily list refresh is about 5 MB, 8+ s of the whole link.
**Fix:**
- Adaptive cap: `DownloadController.pageLoading(on)` called from `TabDelegates.onPageStart/onPageStop` of the current tab; while on (until 1.5 s after stop) set `limiter.bytesPerSec = min(userLimit or infinity, 150 KB/s)`. The limiter is live-settable (`setSpeedLimit` :51). Add a Settings switch "Slow downloads while a page loads", default on.
- `resumeInterrupted` and `maybeAutoCheck` wait for the first `onPageStop` of the session or 20 s.
- The list refresh runs only when no tab is loading and no touch for 10 s; swap the new set in then.
**Risk:** downloads are slower while browsing (visible setting). Update checks that fail do not stamp `checked` (UpdateManager.kt:227), so a hotspot without internet retries on every resume; a failure back-off would remove that.

### K8 - LOW (startup) - Work before the first frame, and an ordering dependency (on top of REVIEW-V7 P5)

**Evidence [read]:** `onCreate` :174-219 does on the UI thread, before the first frame: `Prefs` load (:177, first SharedPreferences read waits for the XML), `buildUi` (:187), `DownloadController.init` (:188: two engines, a HandlerThread, `StreamStoreFiles.mkdirs` + `wipePrivate` directory scans, `PhoneBridge.create`, `PageProxy.init` thread, `Remuxer`; DownloadController.kt:79-138) and `UpdateManager.init` (:189: SharedPreferences, two `getPackageInfo` Binder calls, JSON; UpdateManager.kt:104-120). None is needed for the first frame (the menu reads download counts only when opened). Class loading and interpretation of this code is cold (no baseline profile, geckoapp/build.gradle.kts has no profileinstaller).
Ordering: engine creation is queued after the first frame (:197-206), but `restore()` (a plain post from the `Io.fast` thread :213-217) reaches `ensureSession` -> `GeckoEngine.runtime(this)` (:599) by itself. Which message runs first depends on timing, so in some launches `GeckoRuntime.create` can land before the first frame [guess].
**Fix:** move `DownloadController.init`, `UpdateManager.init` and `Shields.init` into the post-first-frame block (they are idempotent; `init` is `@Synchronized`); make the engine start explicit (`restore()` waits for a flag the frame callback sets); touch `getSharedPreferences("kestrel")` from `GeckoApp.onCreate` on a thread so the XML parse overlaps class loading.
**Gain [guess]:** 30-100 ms on the first frame. **Verify:** Diagnostics timeline, `life resume` versus `engine runtime-create` / `runtime-ready` stamps (they exist) tell the real gap and the real `GeckoRuntime.create` cost with no new code.

### K9 - LOW - Thumbnail work runs on the lowest lane while the user waits

**Evidence [read]:** `onTabs` :844 loads missing thumbnails with `Io.exec` (nice 13, BACKGROUND group, Io.kt:19-22) although the grid is on screen; the post-processing of a grid-triggered capture (:858-866: `createScaledBitmap` + RGB_565 copy + WebP encode + file write) is on `Io.exec` too. Cards show letter avatars until it finishes. The post-load capture (:1566-1569) still reads back a 600x973 frame (2.3 MB, a tile-GPU pipeline flush [guess]) after every load, now throttled to one per 15 s and 3 s after the last touch (HW_SPEED H3 asked to drop it).
**Fix:** grid-triggered load and save on `Io.fast`; keep the post-load capture on `Io.exec`. Optional: `GeckoDisplay.screenshot().size(240, h)` scales inside the compositor (API exists in 160 [read]), but `GeckoView.mDisplay` is a package-private type and `GeckoSession.acquireDisplay()` throws "Display already acquired", so it needs reflection; do it only if profiling shows the read-back matters.

### K10 - LOW - `MediaList.usable()` runs on every UI refresh and sorts with regex-based keys

**Evidence [read]:** `refreshDownloadButton` (BrowserActivity.kt:1451) runs inside every `applyUi` and, once a page has sniffed media, calls `t.media.usable()` (core MediaList.kt:81-92): a filter plus a sort whose tie-break `knownHeight` (:29) runs `MediaSniff.heightFromUrl` (substring allocations + two regex scans) on every comparison, and `heightOfLabel` (:35) compiles a new `Regex` per call when a label exists. `applyUi` fires on each 3 % progress step, title change and the 4/s shield counter.
**Cost [guess]:** 4-9 ms per call with 24 candidates; about 1-2 % of a core on a video page.
**Fix:** `MediaList` keeps `usableCount` and invalidates it in `add/applyInfo/clear`; `refreshDownloadButton` reads the count. Optionally cache `knownHeight` in the candidate when `add/applyInfo` changes it, and hoist the `Regex` in `heightOfLabel` into a constant.

### K11 - LOW - Diagnostics code that is always on

**Evidence [read]:** `Health.log` formats with `String.format(Locale.US, "+%.3fs ...")` (Health.kt:33-36), about 50-100 us on this CPU [guess]; it also runs for every touch-down that may start a pull (KestrelGeckoView.kt:59). `Health.startWatchdog()` and `watchFrames()` start at every resume (BrowserActivity.kt:230). HW_SPEED H5 already asks to gate the last two.
**Fix:** build the timestamp with `StringBuilder` (no `Formatter`), drop or sample the per-touch "pull down" line, gate watchdog and frame metrics behind the Diagnostics switch.

### K12 - LOW - `browser.db` is not in WAL mode

**Evidence [read]:** `BrowserDb` (data/BrowserDb.kt:15) uses the default journal; `recordVisit` after every page (BrowserActivity.kt:1561) costs 2-3 fsyncs on eMMC [guess 10-40 ms]. Non-WAL readers wait for that writer, so `suggest()` on `Io.fast` can stall behind a visit write. `SqliteStore` (downloads.db) already does `enableWriteAheadLogging()` + `synchronous=NORMAL` (SqliteStore.kt:19-22).
**Fix:** same two lines in `BrowserDb.onConfigure`. Risk: `-wal`/`-shm` files, nothing else.

### K13 - LOW, optional - A page keeps rendering under opaque full-screen panels

**Evidence [read]:** Settings, Downloads, History, Diagnostics and the tab grid cover the page with opaque views, but nothing calls `setActive(false)` for them (only `show()` :531 and `onStop` :262 do). A page with a running animation keeps the content process and compositor busy while the user changes a setting. `setActive(false)` also suspends media (`suspendMediaWhenInactive`), so audio would stop; the media-session delegate is not used to tell. Only worth doing if a pause for audio is acceptable.

---

## 3. Back / forward map (where the cache can be lost, and who controls it)

| Case | Today | Lever |
| - | - | - |
| Back in a live tab, page bfcache-eligible | instant (bfcache, 4 viewers shared by all tabs, 1 h) | viewers up with live tabs (K1.5) |
| page sends `Cache-Control: no-store`, has unload handlers | network reload | engine; K5 preview for perceived speed |
| Gecko flush at trim 15 | bfcache wiped, history load from HTTP cache | no public switch (SCROLL Q2) |
| tab was hibernated (K1) | revive; its bfcache entries are gone | K1 |
| closing a tab returns to its parent | parent is hibernated from the 3rd tab on | K1 |
| app swiped away / killed | Back stack and scroll gone | K2 |
| activity re-created (dark mode, font size) | all sessions closed | K3 |
| engine process killed in background | new process started at once | K4 |

The extension does not hurt Back or scrolling [read]: no `unload`, `pagehide`, `beforeunload` or port listeners in content.js, guard-main.js or background.js; only capture-phase `click`, `auxclick`, `touchend`, `pointerup`, `contextmenu`, `loadedmetadata`, no `touchstart`/`touchmove`/`wheel` (those would delay APZ). Counters are batched (500 ms), stats every 5 s.

## 4. Reviewed, nothing to change

- `BottomDock.bind` early-out via `Shown` (:221-234); `NeonBar.progress`, `TabsButton.count`, `DownloadButton.setCount` early-outs; `updateUi` coalescing; `shieldCountChanged` 4/s; `onProgressChange` 3 % steps.
- Io lanes (`Io.fast` normal priority, `Io.exec` nice 13), the shield worker at normal priority with its own `GeckoResult(handler)`, the cosmetic thread; `ShieldsBridge.onMessage` is a string check plus a post on the UI thread (about 20-50 us per message [guess]); `onContentBlocked` is two atomic increments.
- Session save: 600 ms debounce, atomic rename, `onPause` flush. `typingTick` adaptive (100/300 ms). `DownloadService` posts at most once a second, wake lock only while bytes move. `ProxyLink` idles at 10 min.
- Overdraw: hidden overlays are GONE/INVISIBLE; the only translucent strip over the page is the 16 dp neon halo; the theme window background sits under an opaque root (one extra fill, a nit).
- Neon: 10 Hz alpha steps, held still while touching, typing, loading and when covered; the layer churn at touch boundaries is REVIEW-V7 S7 (owner-sensitive, left alone).

## 5. Known and still open (pointers only, not re-argued)

HW_SPEED H3 (post-load capture, now throttled), H5 (watchdog and frame metrics always on), H8 (engine start timing; K8 adds the ordering race), H9 (no baseline profile), H6/H7/P1e (memory profiles; K1 is the opposite direction), REVIEW-V7 P5 (eager `buildUi`, loader thread `kestrel-engine` at nice 13, Engine.kt:42), P9 (`onLoadRequest` work on the UI thread), S3 (`SuggestionList` and `ListPanel` ignore `convertView`), S5 (tab grid cards not keyed; `TabGrid.refresh` :174 rebuilds all cards on every open and rebind), S7, `setPriorityHint(PRIORITY_HIGH)` unused (grep), FREEZE.md 4.8 (K3).

## 6. Measure first (no new code needed)

- Diagnostics counters: `mem.trim.20`, `mem.trim.15`, `mem.trim.10` (how often K1 fires), `gecko.kill`, `gecko.recover.state|url`, `gecko.firstComposite`, `ui.late`, `frames.jank`.
- Diagnostics lines "live sessions N of M tabs" and "Page loads ... visit #N": a visit number above 1 on a host the owner did not reload by hand is a revive; its "done" time is the cost of K1.
- Timeline: `life resume`, `engine runtime-create`, `engine runtime-ready`, first `gecko first-composite`, first `PageTimes` paint (K8).
- Missing and worth adding: per-process PSS (PROMPT_AUDIT carry-over d), to pick the K1 thresholds from numbers instead of the guesses above.

## 7. Could not verify

Everything marked [guess]. In particular: the real cost of `GeckoRuntime.create` on the UI thread, whether `onKill` arrives while the app is stopped on this ROM, whether a history load after `restoreState` really avoids the network, the JSON shape of `SessionState.toString()`, whether the SurfaceView shows the previous tab's frame during a revive, whether `max_total_viewers` is global across sessions, and the memory cost per live tab in the shared content process.
