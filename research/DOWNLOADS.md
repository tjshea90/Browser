# Kestrel download manager: research and design (task R2 / B1) - researched 2026-11-11
Method: fetched primary sources, `javap` on the GeckoView 160 jar, reading this repo. NOTHING was run (no emulator, no tablet, no Gradle). Tags: [V] read in a primary source, [S] search snippet / secondary only, [U] my inference or memory, UNVERIFIED. `E#` = evidence list (section 9).
Today (both builds) `net/Downloads.kt` hands the URL to Android's DownloadManager: no pause (its pause/resume are `@hide` [E1]), one connection, no integrity check, private-tab downloads land in Android's own list.

## 1. Decisions (read this first)
1. **Own engine, not DownloadManager.** Pure Kotlin in `core/` (Java-8 API only: state machine, planner, HTTP semantics, file names, digests) + a thin Android shell shared by both builds + one `Fetcher` per engine. DownloadManager only has enqueue/query/remove and its docs now
   steer apps to `MediaStore.Downloads` [E1]. No new dependency (no OkHttp / WorkManager / coroutines / Cronet).
2. **Parallelism is adaptive and starts at ONE connection.** Segment 0 is the connection we already have (Gecko: the live `onExternalResponse` body; WebView: the first GET). Add one connection at a time (cap 4 on HTTP/1.1, 3 on H2/H3, 6 over all downloads, 3 if
   `isLowRamDevice`) only if ranges are proven, a strong validator exists, the total is known, remaining >= 4 MiB and ETA >= 6 s; keep it only if aggregate speed gains >= 15 %, else step back and freeze growth for 30 s.
   For: Chromium on Android splits 3-way by default (min slice 1,365,333 B, strong validator + known length + > 2 s left) [E5,E6]; aria2 offers 5 splits but defaults to 1 connection per server and a 20 MiB minimum split [E4].
   Against blind parallelism: H2/H3 streams share one connection and one congestion window [E3], RFC 9112 tells clients to limit connections per server [E3], every extra flow takes bandwidth from page loads on the shared hotspot.
   Whether it helps THIS hotspot cannot be known here [U] (carrier per-flow shaping vs per-subscriber is unverified): the engine measures, the detail view shows "3 connections, +40 % vs 1", Settings has Auto (default) / Off / Max N.
3. **Segments = byte ranges `[start,end)` + a durable cursor; requests are bounded windows** (W = 8 MiB URLConnection, 4 MiB Gecko, shrinking to 512 KiB near the end, 1 MiB under a speed limit). A new connection takes the upper half of the biggest stealable tail: the
   not-yet-requested tail of a windowed segment (zero waste, no overlap, keep-alive works) or, only for the first open-ended response, the range beyond ~3 s of data ahead of its cursor (that response is closed at the new boundary; waste = bytes already in flight).
   Bounded windows also bound Gecko's unbounded Java-side buffer [E10].
4. **Validators.** Store a strong ETag (never `W/`), else Last-Modified only if `Date - Last-Modified >= 1 s` [E2]; neither = single stream, resume only after an explicit "resume anyway". Extra segments send `If-Match` / `If-Unmodified-Since` (changed file = 412 without a body;
   Chromium's workers do this, `use_if_range=false` [E7]); a lone sequential resume sends `If-Range` (mismatch = 200 + new validators = "file changed"). Accept a 206 only if `Content-Range` first == requested and total == stored. A 200 to a Range request means
   "no range support": collapse to one stream, NEVER splice. Single-range requests only (multi-range is a DoS vector [E2]).
5. **Never range-download encoded bytes.** Send `Accept-Encoding: identity`; a reply with `Content-Encoding != identity` = single stream, no resume, size unknown (ranges address the ENCODED bytes [E2]; Chromium turns a decode failure on a partial request into "restart" [E7];
   HttpURLConnection silently gunzips and strips Content-Length/Encoding unless Accept-Encoding is set [E15]).
6. **Pause = cooperative stop, fsync, checkpoint, close sockets (never keep an idle socket).** Persist per-segment cursors (only AFTER the fsync), validators, original + resolved URL, request headers, destination URI; the first request after resume re-validates.
   Never silently restart a big partial on a metered link: a 200/412 on resume with `done > 20 MiB` -> NEEDS_DECISION "Restart (uses X MB again) / Cancel". Non-resumable downloads (no ranges or validator): Pause = soft pause (TCP back-pressure, <= 60 s [U]) on URLConnection
   only; on Gecko the button is disabled with a reason, because its stream has no back-pressure and only `close()` stops it [E10,E11].
7. **Write straight into a pending `MediaStore.Downloads` row** (IS_PENDING=1, `Download/`), one `ParcelFileDescriptor` ("rw") shared by all workers, `Os.pwrite` (NOT `FileChannel`: interrupting any thread closes the channel for all [E18]), pre-allocate with `StorageManager.allocateBytes(fd, total)` (API 26, may evict
   system caches to make room [U]) -> `Os.posix_fallocate` -> `ftruncate` (sparse) as fallbacks, `fsync`, then IS_PENDING=0. Re-open the stored URI after a restart; pending rows expire after ~7 days [E17] so a longer pause can lose the partial (detected -> restart). Android 11+: keep the MediaStore fd, direct-path random writes are "up to twice as slow" [E17].
   Fallbacks: SAF tree (probe seekability) -> app-private staging file + copy at the end (needs 2x space, asks first).
8. **Foreground service type `dataSync`**, notification channels, `POST_NOTIFICATIONS` asked at the first download (denied: the service still runs, visible only in the Task Manager [E20]). Android 15's 6 h per 24 h `onTimeout` -> pause all + Resume notification [E19].
   A notification tap may start an FGS from the background on 12+ [E19]. No auto-resume after reboot by default (dataSync cannot start from BOOT_COMPLETED on 15+ [E19]; BRIEF: no unattended network); a sticky restart after a kill continues on <= 11 only [U].
9. **Network:** reuse `NetworkMonitor`; WAITING_NETWORK only on OFFLINE or >= 3 consecutive failures while not ONLINE; NO_INTERNET alone never pauses (BRIEF rule: a hotspot can fail Android's check and still work); probe with backoff because the callback may never fire.
10. **Two `Fetcher`s.** `UrlConnectionFetcher` (WebView build; Android's HttpsURLConnection is OkHttp pinned to HTTP/1.1 [E14] = every segment is its own TCP+TLS 1.3 flow; trust store frozen at the tablet's July 2021 state [E16]) and `GeckoFetcher` (Kestrel G;
    `GeckoWebExecutor`: H2/H3, DoH, profile cookies [E9,E12,E13], Mozilla's own root list [U]). Kestrel G falls back to URLConnection when the Gecko runtime is not up (after a service restart).
11. **Retry:** full-jitter backoff (base 1 s, cap 60 s) [E24], `Retry-After` honoured (cap 10 min); 403/410 on a stale resolved URL -> re-resolve once from the original URL (GitHub release links redirect to short-lived signed URLs [U]); stall = < 1 KiB/s for 15 s or 20 s read
    timeout; STALLED (user taps Retry) after 30 min without progress.
12. **Integrity:** length checks always; digest check only when a source exists: user SHA-256 > `Repr-Digest` sha-256/512 [E23] > none. Never auto-probe sidecar files (extra requests on a metered link); offer "Check .sha256" on tap.
13. **Names and safety:** replace `URLUtil.guessFileName` (ignores `filename*=`, needs the `attachment;` prefix, may rewrite the extension [E23]; our `take(120)` counts chars, filesystems limit BYTES) with a core RFC 6266/8187 parser + sanitizer; sniff the first 512 B and flag
    mismatches before "Open".
14. **Polite on a shared hotspot:** worker threads at BACKGROUND priority (owner's Brave complaint was UI freezes); while a tab is loading drop to 1 connection; 2 active downloads max, the rest queued; optional token-bucket speed limit.
15. **UI contract:** core emits `DownloadView(state, fraction, speed, eta, primary, secondary[])`; one 56 dp primary button, 52 dp secondary buttons, no animation (TASKS A1/C1); table in section 5.
16. **Test everything pure on the JVM** (virtual clock, `FakeFetcher`, in-memory `Sink`/`Store`, raw-socket fake HTTP server with Range/ETag/failure injection); Android-only parts need a CI emulator job + a tablet checklist (sections 8, 10).
17. **Rejected:** OkHttp / Cronet (dependency, native code; OkHttp would negotiate H2 and fold our parallel flows into one connection [U]) · own HTTP/2 client · WorkManager or user-initiated jobs as the base (API 34+ only, tablet is 29; revisit for 14+) · multi-range requests · hedged duplicate tail
    requests (cost metered bytes) · HEAD probe (extra RTT, presigned URLs [U]) · staging file + final copy as the default (2x space, a long copy at the very end).
Owner decisions (recommendation first): Auto parallelism, max 4 | polite mode ON | no auto-resume after reboot | save into the `Download/` root (subfolder optional).

## 2. Module split and threads
- `core/` package `app.kestrel.core.download` (Java-8 API only, no Android, threads only via injected executors): `Model` (states, reasons, Segment, Record, DownloadView) · `HttpSemantics` (classify(response); parsers for Content-Range, ETag, Last-Modified/Date, Retry-After, Content-Disposition, Digest)
  · `Planner` (windows, split/steal, hill-climb, invariants) · `Engine` (coordinator, events, checkpoint, pause/resume) · `Backoff` · `Meter` (EWMA speed/ETA) · `Limiter` · `FileNames` · `Sniff` · `Digests` · ports `Fetcher Sink Store Clock NetGate`.
- `app/` (both builds): `download/` DownloadService, SqliteStore, MediaStoreSink, SafSink, StagedSink, UrlConnectionFetcher, Notifier, NetGate(NetworkMonitor), DownloadController (facade for BrowserActivity), DownloadsScreen. `app/src/webview/.../Downloads.kt`: captures the same-site cookie (existing `Psl` rule) + UA + Referer origin at enqueue.
- `geckoapp/`: `net/GeckoFetcher`, `LiveResponse` (wraps the `WebResponse` from `onExternalResponse`), `Downloads.kt` facade.
- Threads: ONE coordinator (HandlerThread, BACKGROUND) owns all state and the Store; workers = `ThreadPoolExecutor(0..6, 30 s idle)`, one blocking read loop per running segment; UI gets immutable snapshots <= 4 Hz while visible. Do not reuse `Io` (one thread: SQLite would queue behind disk).
- Memory/CPU: 64 KiB read buffer per worker (reused), 128 KiB write batches => ~1.5 MB; Gecko windows cap its queue at W per connection (12 MB worst case, inside the 25 MB budget); Android 17 adds RAM-based app memory limits [E22]. At hotspot speeds TLS for 4-6 flows should be a small share of 4 A53 cores [U]; the periodic
  fsync also bounds dirty pages on a 2 GB device [U].
- Process: nothing in the service depends on UI classes, so `android:process=":dl"` is a manifest change for the WebView build (cheaper to keep alive than the whole browser); impossible for Kestrel G (the executor lives in the Gecko process). Ship in-process, measure RSS on the tablet [U].
- Private downloads: RAM-only `Store` (no row, no cookie on disk, generic notification text, `FETCH_FLAGS_PRIVATE`); WebView build: engine instance inside `:private`; the file still lands in Downloads (UI already warns); no resume after process death.

## 3. Algorithms (core/, one coordinator thread; workers only move bytes)
### 3.1 The first request is the probe (not HEAD)
```
start(d): r = fetcher.open(GET d.url, Accept-Encoding: identity)        // Gecko: r = the live onExternalResponse stream, already open
  redirects by hand (<=10, never https->http, cookies re-evaluated per hop, Authorization dropped) -> d.finalUrl, d.finalAt
  read status, total (Content-Length | Content-Range), Accept-Ranges, ETag, Last-Modified, Date, Content-Encoding/Disposition/Type, Repr-Digest
  d.ranges = (Accept-Ranges: bytes) ? YES : UNKNOWN        // "client MAY try regardless" [E2]; UNKNOWN is settled by the first extra request (206 -> YES, 200 -> NO)
  d.validator = strongETag ?: strongLastModified ?: NONE;  sink = openSink(name, total)   // free-space check, fallocate
  segs = [[0, total|INF)], s0.live = true (open-ended reply, no Range header); worker 0 pumps THIS response, so no byte is wasted
  Content-Encoding != identity, or (no length and no validator)  =>  single stream, d.ranges = NO
```
### 3.2 Planner (every event + 1 s tick) and work stealing
```
plan(d): if d.state != RUNNING return;  speed = meter.ewma;  rem = total - done
  cap = (ranges != NO && validator && total > 0 && rem >= 4MiB && rem / max(speed, 32KiB) >= 6s && !polite) ? d.cap : 1
  d.n = hillClimb(d.n, speed, cap, now)      // 4 s windows after a 1.5 s ramp: gain >= 15% -> n+1; gain < 5% -> n-1 and no growth for 30 s; speed < 50% of baseline for 8 s -> n = 1
  while running(d) < d.n && globalFree() > 0 && (s = nextWork(d)) != null: launch(s)
nextWork(d): 1) PENDING / retry-ready segment, lowest start
  2) steal: for each RUNNING v: from = v.live ? v.cursor + max(2MiB, 3s*speed) : v.reqEnd ;  tail = v.end - from       // windowed victims win ties (zero waste); the live reply only when nothing else
     v* = argmax(tail); if tail >= 2*MIN_SPLIT(2MiB): mid = align64K(from + tail/2); new seg [mid, v*.end); v*.end = mid         // victim keeps reading until cursor == mid, then closes (waste ~ RTT*rate)
     if d.ranges == UNKNOWN the split is provisional: the thief's first reply decides (206 -> persist both rows; 200 -> v*.end restored, thief dropped, ranges = NO)
  3) else null
window(s) = clamp((s.end - s.cursor) / 4, 512KiB, W)    // guided scheduling: finer toward the end so the straggler tail is ~1 s; no hedged duplicate requests (they cost metered bytes)
```
### 3.3 Worker (one thread per running segment)
```
worker(s): while s.cursor < s.end and !stop:
    s.reqEnd = s.live ? INF : min(s.end, s.cursor + window(s))
    r = s.live ? d.liveResponse : fetcher.open(GET d.resolvedUrl, Range: bytes=s.cursor-(s.reqEnd-1), cond(s), Accept-Encoding: identity)   // cond = If-Match | If-Unmodified-Since (extra segments) | If-Range (lone resume)
    switch classify(r, s, d):                                  // pure, table-tested in core
      OK_206      -> pump(r, s)
      FULL_200    -> if s.cursor == 0 && d.segs.size == 1 { d.ranges = NO; pump(r, s) }     // plain reply to the first request: a normal download
                     else if d.resumed { r.abort(); d.ranges = NO; needDecision(NO_RANGE) } // the partial cannot be continued
                     else { r.abort(); d.ranges = NO; undoSplit(d) }                        // probe of an extra connection failed: keep seg 0, drop the thief
      CHANGED     -> 412 | 200 with other validators | Content-Range total != stored  -> needDecision(FILE_CHANGED)       // partial kept; user chooses Restart / Cancel
      BAD_RANGE   -> 416 with cursor < total | 206 whose first byte != s.cursor -> treat as NO_RANGE
      GONE        -> 401/403/404/410: if !d.reResolved { reResolve(d.url); continue } else fail(LINK_EXPIRED | HTTP_n)
      RETRY       -> 429/503 (Retry-After) | 5xx | IOException | timeout:  sleep(fullJitter(s.fails++, retryAfter)); continue          // 8 fails without progress -> segment idle, download probes
    s.fails = 0 once >= 256 KiB written
pump(r, s): while (n = r.body.read(buf64K)) > 0 { sink.pwrite(s.cursor, buf, n); s.cursor += n; meter.add(n); limiter.take(n); stall.touch();
              if stop || s.cursor >= s.end { r.abort(); return } }      // abort = Gecko body.close() (cancels) / HttpURLConnection.disconnect(); NEVER Thread.interrupt()
            if n == -1 && s.cursor < min(s.end, s.reqEnd): throw Truncated       // early EOF is never "done" (RFC 9112 incomplete message); we count bytes ourselves
```
### 3.4 Checkpoint, pause, resume, crash safety
```
checkpoint(d)   // every 5 s or 8 MiB, on pause / network loss / onTrimMemory / onTimeout / service destroy
  snap = {s.cursor for s in segs}   // sampled BEFORE the sync, so metadata can never run ahead of durable data
  sink.sync()                       // fdatasync: data first
  store.tx { UPDATE segments SET cursor = snap[s]; UPDATE downloads SET done = sum, updated = now }   // metadata second, one SQLite transaction (WAL, synchronous=NORMAL)
pause(d, why): state = PAUSING; set stop on all workers (they abort their response); join <= 3 s; checkpoint(d); state = why; release wake/Wi-Fi lock if nothing else is RUNNING
resume(d): load rows; verify dest (MediaStore row present, length == total, else restart); segments with cursor < end -> PENDING; d.resumed = true;
           first request goes to d.url (not the cached final URL when final_at is older than 60 s and the chain had a redirect) so signed links are re-issued; validators re-checked as in 3.3
crash: a kill loses at most one checkpoint interval (<= 8 MiB / 5 s) of already-written bytes, which are simply fetched again; offsets in the DB never exceed fsynced bytes, so the file can't contain a gap
```
### 3.5 Writes, ordering, limits
- Order of bytes on disk is arbitrary across segments, strictly ascending inside one; each worker coalesces <= 128 KiB per `pwrite`. Completion = all cursors == ends -> size check -> `fsync` -> optional digest -> publish (IS_PENDING=0 / close) -> COMPLETED -> notify. Publish failure (name taken) -> retry with `name (n).ext`.
- Speed limit: one token bucket shared by all workers (100 ms refill, burst 256 KiB). URLConnection throttles by reading less (TCP back-pressure); Gecko by duty-cycling bounded 1 MiB windows (its queue is unbounded, so never "just read slower").
- Progress: `Meter` = EWMA (alpha 0.3) of 1 s buckets; ETA hidden for the first 3 s; UI 4 Hz, notification <= 1 Hz, DB checkpoint as above.
- Stall: a watchdog aborts a response with < 1 KiB/s over 15 s (aria2 `--lowest-speed-limit` idea [E4]); URLConnection `setReadTimeout(20 s)`; Gecko `poll(20 s)` for headers + `setReadTimeoutMillis(20 s)` on the body (the response default is 30 s [E12]). Hotspot drops are often silent, no RST [U], so timeouts matter more than error callbacks.

## 4. State machine and SQLite
```
QUEUED -slot free-> STARTING -headers ok-> RUNNING -all bytes-> [VERIFYING if a digest is wanted] -> COMPLETED          (terminal: COMPLETED | FAILED | CANCELED -> scrub req_headers)
RUNNING -user-> PAUSED_USER -resume-> QUEUED     RUNNING -OFFLINE | 3 fails while not ONLINE-> WAITING_NETWORK -net back (+1..3 s jitter; probes 5..60 s)-> RUNNING
RUNNING -process killed / reboot / FGS timeout-> row stays RUNNING; at start: PAUSED_SYSTEM (reboot, API>=12 kill, onTimeout) or STARTING (sticky restart, API<=11)
RUNNING -412 / 200 on resume / validators differ-> NEEDS_DECISION(FILE_CHANGED | NO_RANGE) -Restart-> QUEUED (done=0) | -Cancel-> CANCELED
any -fatal-> FAILED(HTTP_n | LINK_EXPIRED | NO_SPACE | STORAGE | FS_LIMIT(FAT 4 GiB) | INSECURE_REDIRECT | TLS | LOCAL_NET | DIGEST | STALLED)   FAILED -retry-> QUEUED (partial kept when validators allow)
any non-terminal -cancel-> CANCELED (partial deleted)       Every transition = one SQLite transaction + notification refresh; replaying a persisted state is idempotent.
```
```sql
-- downloads.db: own file, WAL, synchronous=NORMAL, user_version=1. Offsets are written only after the data was fsynced.
CREATE TABLE downloads(id INTEGER PRIMARY KEY, state INTEGER NOT NULL, reason INTEGER NOT NULL DEFAULT 0, prio INTEGER NOT NULL,
  url TEXT NOT NULL, final_url TEXT, final_at INTEGER, page_url TEXT,            -- as clicked / after redirects / source page ("Open page" when a link expired)
  req_headers TEXT,                                                              -- Cookie, UA, Referer(origin only); NULL for private; scrubbed on terminal states
  name TEXT NOT NULL, mime TEXT, dest_kind INTEGER NOT NULL, dest_uri TEXT,      -- 0 MEDIASTORE, 1 SAF, 2 STAGED
  total INTEGER NOT NULL DEFAULT -1, etag TEXT, last_mod TEXT, date_hdr TEXT, ranges INTEGER NOT NULL DEFAULT -1,   -- -1 unknown, 0 no, 1 yes
  fetcher INTEGER NOT NULL, risk INTEGER NOT NULL DEFAULT 0, metered_ok INTEGER NOT NULL DEFAULT 0, done INTEGER NOT NULL DEFAULT 0,
  fails INTEGER NOT NULL DEFAULT 0, conns_peak INTEGER, digest_want TEXT, digest_src INTEGER, digest_got TEXT,      -- "sha-256:hex"
  created INTEGER NOT NULL, updated INTEGER NOT NULL, last_progress INTEGER, finished INTEGER);
CREATE TABLE segments(dl INTEGER NOT NULL REFERENCES downloads(id) ON DELETE CASCADE, idx INTEGER NOT NULL, start INTEGER NOT NULL,
  end INTEGER NOT NULL, cursor INTEGER NOT NULL, PRIMARY KEY(dl, idx));          -- end = -1: open-ended (length unknown); <= ~64 rows per download
CREATE INDEX dl_state ON downloads(state, prio);
```

## 5. Android integration and UI
- **Service:** `startForegroundService` -> `startForeground(id, n, FOREGROUND_SERVICE_TYPE_DATA_SYNC)` (3-arg form and the constant: API 29; `onTimeout(int)` 34, `onTimeout(int,int)` 35, `ForegroundServiceStartNotAllowedException` 31 [E18]). Manifest: `foregroundServiceType="dataSync"`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_DATA_SYNC`, `POST_NOTIFICATIONS`, `WAKE_LOCK`. Stop the service when nothing is RUNNING/WAITING.
  Notification: channel `dl_progress` (LOW, ongoing, only-alert-once, <= 1 update/s, actions Pause all / Resume all), channel `dl_done` for finished/failed (tap = open, action = Share). Resume actions use `PendingIntent.getForegroundService`. Catch `ForegroundServiceStartNotAllowedException` (API 31 class; `IllegalStateException` before) -> PAUSED_SYSTEM + "tap to resume".
- **Switching apps:** the FGS keeps the process; the Activity only attaches/detaches a listener (no download state in the Activity). `onTrimMemory` -> checkpoint. A swipe-away may force-stop the app on some OEM builds [U] with no callback, so the 5 s / 8 MiB checkpoint is the only protection; whole-process low-RAM kills are survivable by design (3.4).
- **Doze / locks:** AOSP lets a process with a foreground service keep network in Doze / Battery Saver [E21] while the docs say Doze "ignores wake locks" [E21]: hold a PARTIAL_WAKE_LOCK (10 min, renewed) only while RUNNING, optional Wi-Fi lock when the screen is off; check on the tablet that a screen-off download keeps its speed [U].
- **Network:** `NetGate` adapts `NetworkMonitor` (default-network callback, debounce 1 s). A new `Network` object (hotspot reconnect) => abort sockets, wait 1-3 s jitter, resume. The metered confirmation stays in the existing UI gate (`confirmDownloadBytes`); the engine stores `metered_ok` and does not re-ask.
- **Storage details:** name dedupe = our own query on (DISPLAY_NAME, RELATIVE_PATH) before insert (MediaProvider also has `ensureUniqueFileColumns` [S]); use mode "rw", never rely on "w" truncating (version-dependent [U]); free space via `StatFs` / `StorageManager.getAllocatableBytes` (API 26) before start; ENOSPC -> NO_SPACE (resumable after freeing space);
  EFBIG -> FS_LIMIT. SAF folder: `DocumentsContract.createDocument` + `openFileDescriptor("rw")`, probe `Os.lseek` (ESPIPE = not seekable -> StagedSink).
- **Integration with today's UI (both `BrowserActivity`):** keep the `download()` gate (metered size confirm, risky, cleartext) and the private-tab dialog (new text: no Android list); replace `Downloads.enqueue` by `DownloadController.enqueue(Source)`, `Source = Live(WebResponse)` (Gecko `onExternalResponse`) | `Url(url, ua, cd, mime, pageUrl)` (WebView listener, long-press image/link).
  Gecko: keep the `WebResponse` UNREAD while the confirm dialog is open (suspended = near-zero data); Cancel -> `body.close()`; if the dialog takes minutes the stream may be dead and the engine falls back to a Range fetch of the same URL. Menu "Downloads" opens `DownloadsScreen`, not `DownloadManager.ACTION_VIEW_DOWNLOADS`; open/share via the `content://` URI + `FLAG_GRANT_READ_URI_PERMISSION`.
- **Download row (core `DownloadView`)**

| State | Text | Primary (56 dp) | Secondary (52 dp) |
|---|---|---|---|
| RUNNING | 37 % - 2.1 MB/s - 3 min left (3 connections) | Pause | Cancel |
| PAUSED_USER / PAUSED_SYSTEM | Paused - 37 % | Resume | Cancel |
| WAITING_NETWORK | Waiting for the hotspot... | Pause | Cancel |
| NEEDS_DECISION | The file changed on the server; restarting uses 412 MB again | Restart | Cancel |
| FAILED | reason text (Link expired, No space, ...) | Retry (or Open source page) | Delete |
| COMPLETED | Done - 412 MB (checksum OK) | Open | Share, Delete |

- **Per Android version:**

| API | consequence for the download service |
|---|---|
| 29 (tablet) | MediaStore.Downloads + IS_PENDING; scoped storage; `startForeground` with type; FGS may start from background; HttpURLConnection HTTP/1.1, TLS 1.3 default [E16] |
| 30 | scoped storage for everyone, FUSE-backed paths; MediaStore fd I/O recommended [E17] |
| 31 | FGS start from background blocked except exemptions (notification tap OK) [E19]; sticky restart may throw [U] |
| 33 | `POST_NOTIFICATIONS` runtime permission; the FGS runs without it [E20] |
| 34 | FGS type + `FOREGROUND_SERVICE_DATA_SYNC` mandatory; user-initiated data-transfer jobs exist (schedule only while visible) [E19]: a v2 option, not for this tablet |
| 35 | dataSync: 6 h per 24 h shared, `onTimeout(startId, fgsType)` must `stopSelf()` within seconds, timer resets when the user opens the app; no dataSync from BOOT_COMPLETED [E19] |
| 36 | jobs running next to an FGS follow job runtime quotas [E22]; Local Network permission opt-in (downloads from RFC1918 hosts; Gecko reports `ERROR_LOCAL_NETWORK_ACCESS_DENIED`) [E22] |
| 37 | RAM-based app memory limits [E22] |

## 6. Where the bytes come from
### 6.1 `UrlConnectionFetcher` (the only one in the WebView build, fallback in Kestrel G)
- HttpsURLConnection on Android = OkHttp fork with `setProtocols(HTTP_1_1_ONLY)` (read in Android 9 and 31-36 sources; the API-29 file itself not read) [E14]: no H2/H3, so N segments = N independent TCP flows, the case where IDM-style splitting is most likely to pay off. TLS 1.3 is on by default since Android 10 [E16].
- Trust store = the tablet's system CAs (patch level 2021-07). Android only got updatable roots in 14 [E16]; Let's Encrypt began issuing from new roots in Jan 2026 and leans on cross-signs to ISRG X1 for older clients [E16, S]: expect more handshake failures over time. Map `SSLHandshakeException` to "this tablet's certificate list is too old" and (Kestrel G) retry with `GeckoFetcher`.
- Docs facts [E15]: auto-redirects <= 5 and never HTTP<->HTTPS (so we follow by hand), transparent gzip unless `Accept-Encoding` is set, per-read timeout only (hence our watchdog), `disconnect()` returns/aborts the socket. Use `getContentLengthLong()` (files > 2 GiB on a 32-bit device).
### 6.2 `GeckoFetcher` (Kestrel G) - how a GeckoView app receives a download [V unless marked]
- `ContentDelegate.onExternalResponse(session, WebResponse)` fires "when there is a response that cannot be handled by Gecko (e.g., a download)", `@UiThread` [E12]. `WebResponse` has `uri` (the channel URI at that moment; that it is the post-redirect URL is not confirmed [U]), all response headers, `statusCode`, `redirected`, `body: InputStream`. Since GeckoView 160 the
  filename in `Content-Disposition` is already validated by Gecko (bug 1822968) [E12]. No request cookies/UA/referrer are exposed and none are needed: Gecko made the request. Hand the stream to a worker at once; a blocking read on the UI thread would freeze the app.
- The body is a `GeckoInputStream`: the channel starts suspended and resumes on the first read; the Java queue is UNBOUNDED and never re-suspended; `close()` cancels the request (`Cancel(NS_ERROR_ABORT)`); single-byte `read()` is buggy (bulk reads only); an unclosed body holds the connection until GC; `WebResponse` applies a 30 s read timeout [E10,E11,E12].
  Fenix does the same dance: original response for the first attempt, then its Gecko-backed HTTP client with `Range: bytes=N-` on resume or IOException, without validators [E8].
- `GeckoWebExecutor.fetch(WebRequest, flags)` (any thread; `GeckoResult.poll(ms)` blocks a worker): flags ANONYMOUS=1 (no cookies), NO_REDIRECTS=2, PRIVATE=8; custom headers (`Range`, `If-Match`, `If-Range`, `Accept-Encoding`) go through `SetRequestHeader` with no blocklist in that file; `cacheMode` NO_STORE for downloads [U]; `referrer`;
  `speculativeConnect(url)` warms a connection (call when adding a stream or resuming); schemes http(s) and `blob:` (the backlog's "blob: downloads" becomes possible in this build only) [E9,E12]. Errors arrive as `WebRequestError`: NET_RESET / NET_INTERRUPT / NET_TIMEOUT -> retry, OFFLINE -> WAITING_NETWORK, SECURITY_* -> TLS (no retry), UNKNOWN_HOST -> retry then fail, LOCAL_NETWORK_ACCESS_DENIED -> LOCAL_NET.
- The channel uses the SYSTEM principal with a REGULAR cookie jar, so profile cookies are sent unless ANONYMOUS [E9]; with Kestrel's `ACCEPT_FIRST_PARTY_AND_ISOLATE_OTHERS` first-party cookies are unpartitioned, so login cookies should reach the executor [U, untested]. Necko multiplexes our Range requests onto ONE H2/H3 connection
  (http3 and http2 are on by default, 100 concurrent streams [E13]): cap 3 and let the hill-climb decide; we cannot force a second TCP connection [U].
- Open risk [U]: does an explicit `Accept-Encoding: identity` override Necko's default, and does Necko decode executor bodies (then byte offsets and Content-Length disagree)? We always check the reply's `Content-Encoding` and the delivered byte count against `Content-Length` / `Content-Range`; any mismatch => single stream + no resume.
### 6.3 The abstraction (core ports; blocking, called on worker threads)
```kotlin
interface Fetcher { fun open(req: FetchRequest): FetchResponse }          // throws FetchException(kind = TIMEOUT|RESET|OFFLINE|DNS|TLS|LOCAL_NET|PROTOCOL, cause)
class FetchRequest(val url: String, val from: Long, val toInclusive: Long /* -1 open */, val cond: Cond? /* IfMatch|IfRange|IfUnmodSince */, val headers: Map<String,String>,
                   val connectMs: Int, val readMs: Int, val priv: Boolean)
interface FetchResponse : Closeable { val status: Int; val finalUrl: String; fun header(name: String): String?; val body: InputStream?; fun abort() }   // abort() must unblock a reader
interface Sink  { fun pwrite(pos: Long, b: ByteArray, off: Int, len: Int); fun sync(); fun setLength(n: Long); fun length(): Long; fun publish(); fun discard() }
interface Store { fun load(): List<Record>; fun tx(block: (Tx) -> Unit) }    interface NetGate { val online: Boolean; val metered: Boolean; fun onChange(l: () -> Unit) }
```

## 7. Integrity and file safety
- Digest sources: RFC 9530 `Repr-Digest` covers the whole representation even in a 206 (usable with segments); `Content-Digest` covers only that payload; sha-256/sha-512 are active, md5/sha/crc32c deprecated (corruption detection only); digests do not stop on-path tampering without TLS [E23]. Legacy `Digest:` (RFC 3230) is ambiguous: ignore. Result text: "matches the checksum the server sent" vs "matches the checksum you entered".
- SHA-256 runs after completion on a BACKGROUND thread (~10-20 s per GB on in-order A53 cores [U]), cancellable, skipped unless a digest exists. A mismatch keeps the file and asks Delete / Keep.
- Names: `filename*` (RFC 8187) wins over `filename` [E23]; percent-decode; NFC; drop control, bidi (U+202A-202E, U+2066-2069) and zero-width characters; replace `\ / : * ? " < > |`; strip leading dots (no `.nomedia`) and trailing dots/spaces; reserved device names; limit 255 UTF-8 BYTES keeping the extension; empty -> `download`; add an extension from the MIME type only when missing.
  Path traversal is moot because only DISPLAY_NAME + a fixed RELATIVE_PATH reach MediaStore.
- Risk: keep `isRisky` (extension + MIME) and add sniffing of the first 512 B: `\x7fELF`, `MZ`, `dex\n`, `#!`, ZIP whose first entries are `AndroidManifest.xml` / `classes.dex` / `META-INF/`, HTML/SVG under a binary name; disagreement between server MIME, extension and sniff raises the risk level. "Open" on risky types needs a second confirmation; never auto-open, never ask for install permission.

## 8. Test plan (JVM first; no emulator exists in this sandbox)
- `HttpSemanticsTest` (table-driven, `core`): classify() over status x header combinations (206 good/bad Content-Range, 200 on Range, 416 `*/total`, 412, redirects incl. https->http refusal, Content-Encoding trap, Retry-After seconds/date, weak vs strong ETag, Last-Modified vs Date), Content-Disposition (RFC 6266/8187 vectors, traversal, quotes, `inline`), Digest parsing, `FileNames` (CJK byte limit, bidi, reserved names, dedupe), `Sniff`.
- `PlannerTest`: seeded random sequences of split / progress / fail / steal; invariants: segments disjoint, union == [0,total), cursor <= end, no steal inside a requested window, MIN_SPLIT and alignment respected, provisional split rollback restores the old end, hill-climb converges on a modelled link (per-connection cap vs total cap).
- `EngineSimTest` with `FakeFetcher` (in-memory file, virtual clock, scripted failures) + in-memory `Sink`/`Store`: happy path; no-Range server; unknown length; ETag change mid-way (FILE_CHANGED, partial kept); 412 on an extra segment; gzip trap; 429 + Retry-After; stall abort and retry; OFFLINE -> WAITING_NETWORK -> resume; pause with in-flight windows (cursors <= written bytes, resume refetches < 1 window); ENOSPC; expired signed URL re-resolve; speed-limiter exactness; global connection cap; polite mode.
- Crash fuzz: 1000 seeds; snapshot the Store (and the Sink bytes as of the last fsync) at a random instant, start a new Engine from the snapshot, require byte-identical output and a re-fetch of at most one checkpoint interval; "offset stored but data missing" must be impossible by construction and is asserted.
- `FakeHttpServer` (raw `ServerSocket`, Java-8 API, in `core/src/test`): Range, If-Range, If-Match, ETag/Last-Modified, chunked, `Connection: close`, truncation at byte N, slow trickle, resets, 429/503, redirects; drives `UrlConnectionFetcher` in `app/src/test` (the JVM's HttpURLConnection differs from Android's OkHttp fork in pooling / redirects / gzip, so also run it in the CI emulator).
- Written but unproven until CI/tablet: `MediaStoreSink` (pending lifecycle, re-open after kill), `DownloadService` (FGS, notifications, `onTimeout` via `adb shell am compat enable FGS_INTRODUCE_TIME_LIMITS` [E19]), `GeckoFetcher` (cookies, identity encoding, cancel latency, `response.uri`).

## 9. Evidence
E1 DownloadManager: public methods enqueue/query/remove, `addCompletedDownload` deprecated in 29 "contribute files to MediaStore.Downloads" [V] https://developer.android.com/reference/android/app/DownloadManager ; `pauseDownload/resumeDownload` are `@hide` [S] https://code.yawk.at/android/android-9.0.0_r35/android/app/DownloadManager.java
E2 RFC 9110 [V]: 13.1.5 If-Range, 13.2.2 (mismatch -> 200), 14.1.2 ranges are over the ENCODED bytes, 14.2 "MAY ignore", 14.3 client MAY try ranges regardless, 14.4 `bytes */len`, 15.3.7 "MUST inspect Content-Range", 8.8.1/8.8.2.2 strong validators, 17.15 multi-range DoS https://www.rfc-editor.org/rfc/rfc9110.html
E3 [V] RFC 9112 9.4 "ought to limit the number of simultaneous open connections" https://www.rfc-editor.org/rfc/rfc9112.html ; RFC 9113 9.1 "SHOULD NOT open more than one HTTP/2 connection", 6.9.2 window 65,535 https://www.rfc-editor.org/rfc/rfc9113.html ; RFC 9114 1.1 loss stalls all H2 transactions, 1.2, 3.3 https://www.rfc-editor.org/rfc/rfc9114.html
E4 aria2 defaults: split 5, max-connection-per-server 1, min-split-size 20M, lowest-speed-limit, max-tries 5, file-allocation prealloc, `.aria2` control file [V] https://aria2.github.io/manual/en/html/aria2c.html
E5 Chromium constants: min slice 1,365,333 B, 3 requests, 2 s remaining; slices = max(total/count, min) [V] https://raw.githubusercontent.com/chromium/chromium/main/components/download/internal/common/parallel_download_utils.cc
E6 Chromium: parallel only with ETag|Last-Modified, known length, GET, size >= min slice, HTTP/1.1 (H2/QUIC behind flags that default ON); `kParallelDownloading` ON on Android [V] https://raw.githubusercontent.com/chromium/chromium/main/components/download/internal/common/download_job_factory.cc and https://raw.githubusercontent.com/chromium/chromium/main/components/download/public/common/download_features.cc
E7 Chromium: workers `use_if_range=false` -> `If-Match` / `If-Unmodified-Since`; lone resume `If-Range`; 200 on resume restarts at 0; 416 and decode failure -> restart; auto-resume <= 5 [V] https://raw.githubusercontent.com/chromium/chromium/main/components/download/internal/common/parallel_download_job.cc and https://raw.githubusercontent.com/chromium/chromium/main/components/download/internal/common/download_utils.cc
E8 Fenix `AbstractFetchDownloadService`: original response first, Range resume via client, no validators, 32 KiB chunks, 750 ms progress [V] https://raw.githubusercontent.com/mozilla-firefox/firefox/main/mobile/android/android-components/components/feature/downloads/src/main/java/mozilla/components/feature/downloads/AbstractFetchDownloadService.kt
E9 Gecko `widget/android/WebExecutorSupport.cpp`: system principal, regular/private cookie jar, header set without merge, flags [V] https://raw.githubusercontent.com/mozilla-firefox/firefox/main/widget/android/WebExecutorSupport.cpp
E10 `GeckoInputStream.java`: unbounded queue, resumed once, close, read timeout [V] https://raw.githubusercontent.com/mozilla-firefox/firefox/main/mobile/android/geckoview/src/main/java/org/mozilla/geckoview/GeckoInputStream.java
E11 `GeckoViewStreamListener.cpp`: suspend at start, `Close()` = Cancel, URI = `channel->GetURI` [V] https://raw.githubusercontent.com/mozilla-firefox/firefox/main/mobile/android/components/geckoview/GeckoViewStreamListener.cpp
E12 GeckoView API [V]: local `javap` of the 160 jar (GeckoWebExecutor flags, WebRequest, WebResponse `DEFAULT_READ_TIMEOUT_MS=30000`, GeckoResult.poll, WebRequestError); `GeckoSession.java` onExternalResponse @UiThread https://raw.githubusercontent.com/mozilla-firefox/firefox/main/mobile/android/geckoview/src/main/java/org/mozilla/geckoview/GeckoSession.java ; CHANGELOG v160 filename validation https://raw.githubusercontent.com/mozilla-firefox/firefox/main/mobile/android/geckoview/src/main/java/org/mozilla/geckoview/doc-files/CHANGELOG.md ;
    Fenix `GeckoViewFetchClient.kt` (flag mapping, `poll(timeout)`) https://raw.githubusercontent.com/mozilla-firefox/firefox/main/mobile/android/android-components/components/browser/engine-gecko/src/main/java/mozilla/components/browser/engine/gecko/fetch/GeckoViewFetchClient.kt
E13 `network.http.http3.enable` = true, `network.http.http2.enabled` = true, `http2.default-concurrent` = 100 (main branch, GeckoView 160 not pinned) [V] https://raw.githubusercontent.com/mozilla-firefox/firefox/main/modules/libpref/init/StaticPrefList.yaml
E14 Android HTTPS handler `okHttpClient.setProtocols(HTTP_1_1_ONLY)` [V, android-36 + Android 9 mirrors] https://raw.githubusercontent.com/Reginer/aosp-android-jar/main/android-36/src/com/android/okhttp/HttpsHandler.java
E15 HttpURLConnection docs: 5 redirects, no HTTP<->HTTPS, transparent gzip unless Accept-Encoding set, socket pooling [V] https://developer.android.com/reference/java/net/HttpURLConnection
E16 [S] TLS 1.3 default in Android 10 https://developer.android.com/about/versions/10/behavior-changes-all ; updatable roots only from Android 14 https://source.android.com/docs/core/ota/modular-system/conscrypt ; Let's Encrypt 2026 hierarchy https://letsencrypt.org/docs/certificate-compatibility
E17 MediaStore: IS_PENDING owner-only, direct-path random I/O "up to twice as slow", SAF for others' downloads [V] https://developer.android.com/training/data-storage/shared/media ; DATE_EXPIRES ~7 days then deleted at idle [V] https://developer.android.com/reference/android/provider/MediaStore.MediaColumns
E18 `FileChannel` positional ops may run concurrently; interrupt closes the channel [V] https://developer.android.com/reference/java/nio/channels/FileChannel ; `android.system.Os` pwrite / posix_fallocate / ftruncate / fsync (API 21) [V] https://developer.android.com/reference/android/system/Os ; API levels read from the local SDK file `/opt/android-sdk/platforms/android-37.0/data/api-versions.xml` [V]: Service.startForeground(3-arg) 29, onTimeout(int) 34, onTimeout(int,int) 35, ServiceInfo DATA_SYNC 29, ForegroundServiceStartNotAllowedException 31, StorageManager.allocateBytes / getAllocatableBytes 26, MediaColumns IS_PENDING / DATE_EXPIRES / RELATIVE_PATH 29
E19 FGS [V]: timeouts https://developer.android.com/develop/background-work/services/fgs/timeout ; types https://developer.android.com/develop/background-work/services/fgs/service-types ; background-start exemptions https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start ; UIDT https://developer.android.com/develop/background-work/background-tasks/uidt
E20 POST_NOTIFICATIONS and FGS notifications [V] https://developer.android.com/develop/ui/views/notifications/notification-permission
E21 Doze doc [V] https://developer.android.com/training/monitoring-device-state/doze-standby ; AOSP `NetworkPolicyManager.isProcStateAllowedWhileIdleOrPowerSaveMode` (foreground-service states allowed; Android 8.0 and 12 sources, the Android 10 file not read) [V] https://raw.githubusercontent.com/Reginer/aosp-android-jar/main/android-31/src/android/net/NetworkPolicyManager.java
E22 [V] Android 16 job quota + Local Network permission https://developer.android.com/about/versions/16/behavior-changes-all ; Android 17 memory limits https://developer.android.com/about/versions/17/behavior-changes-all
E23 [V] RFC 9530 digest fields https://www.rfc-editor.org/rfc/rfc9530.html ; RFC 6266 4.3 https://www.rfc-editor.org/rfc/rfc6266.html ; `URLUtil.guessFileName` regex `attachment;\s*filename\s*=` https://raw.githubusercontent.com/Reginer/aosp-android-jar/main/android-31/src/android/webkit/URLUtil.java
E24 AWS "Exponential Backoff And Jitter" (full vs decorrelated: close, both far better than none) [V] https://aws.amazon.com/blogs/architecture/exponential-backoff-and-jitter/
E25 [S] Mathis et al. 1997 (per-flow TCP rate ~ MSS/(RTT*sqrt(loss)): why N flows can beat 1 on lossy paths) https://courses.cs.duke.edu/fall24/compsci514/readings/mathis-tcpmodel-ccr97.pdf ; Flach et al. 2016 (policers exist, loss ~2 % -> >20 % when policed; per-flow vs per-subscriber NOT confirmed) https://research.google/pubs/an-internet-wide-analysis-of-traffic-policing/

## 10. Not verified, and what to measure on the tablet first
- Never measured: any speed gain from parallelism; carrier shaping per flow vs per subscriber; TLS CPU cost on the MT8168B (crypto extensions unknown); fsync cost on its flash; Chromium's published gain figures (not looked up). Every threshold in section 3 (4 MiB, 6 s, 15 %, 2 MiB, window sizes, 5 s / 8 MiB checkpoints) is a starting value, not a result.
- Unverified code paths: executor really sends profile cookies; `Accept-Encoding: identity` honoured and no transparent decode; `response.uri` is the final URL; creating a `GeckoRuntime` from a restarted service (thread rule, memory); Gecko trust roots vs the `security.enterprise_roots` default; HttpURLConnection protocol on API 29 itself; MediaStore on API 29 (collision renaming, "w" truncation,
  whether DATE_EXPIRES can be extended, pending row after reboot); sticky-restart behaviour on 12+; wake lock under Doze for FGS processes; GitHub signed-URL lifetime; soft-pause survival time on HTTP/1.1.
- Android 11-17 behaviour comes from documentation only; no device above API 29 was available.
- Measure first (Diagnostics page, task A5): (1) 100 MB file from an H1.1-only host and an H3 CDN with 1/2/3/4 connections x3, screen on and off; (2) Gecko executor against a cookie-gated URL: 206 to Range, 412 to If-Match, Content-Encoding, `response.uri`, cancel latency; (3) MediaStore pending `pwrite` x4 threads MB/s, fsync ms per 8 MiB, `kill -9` + reboot survival;
  (4) RSS of the process with a running download, in-process vs `:dl` (WebView build); (5) a few sites on the new Let's Encrypt chain through URLConnection vs Gecko.

## 11. Implementation notes (alpha5, 2026-11-11) - what was built and where it deviates from sections 1-8
Code: `core/.../download/` (Model, HttpSemantics, FileNames, Planner, Engine; 43 JVM tests incl. an in-memory fake server: pause/resume, parallel ranges, If-Match, changed file, no-range, 429/503, truncation, offline,
no space, cancel, crash recovery, link refresh, queue, private) · `app/.../download/` (SqliteStore, MediaStoreSinks, UrlConnectionFetcher, DownloadController, DownloadService, Notifier) · `app/.../ui/DownloadsScreen` · `geckoapp/.../net/GeckoFetcher`.
- **Conditional requests:** every Range request carries `If-Match` (strong ETag) or `If-Unmodified-Since` - also a lone sequential resume (section 4 said If-Range there). A changed file is a 412 WITHOUT a body, so a resume can never silently spend a 400 MB re-download on the hotspot;
  If-Range would send the whole new file. A 200 to a Range request after progress = NEEDS_DECISION(NO_RANGE).
- **Probe:** the first request is a bounded Range window when the fetcher asks for it (`Fetcher.probeWindow`; Gecko 4 MiB, because GeckoInputStream's queue has no back-pressure), else a streamed GET (URLConnection). The page's own `onExternalResponse` stream is closed unread (suspended channel = no body bytes) and
  the manager makes its own request through the same network stack (same cookies); POST-initiated downloads are therefore not supported yet.
- **Not built yet (alpha5):** digest verification (`Repr-Digest`), file sniffing, speed limit, polite mode (drop to 1 connection while a tab loads), SAF/staged sinks, `onTimeout` handling for Android 15, auto-resume after a restart (interrupted downloads come back PAUSED_SYSTEM and wait for a tap).
- **Not resumable = no Pause button:** without a strong validator, a known length and range support the row offers Cancel only (a soft pause would hold a socket on a hotspot).
- **Names:** `FileNames.choose` (Content-Disposition `filename*` > `filename` > URL path > hint, sanitised, <= 255 UTF-8 bytes, extension from the MIME type); MediaProvider renames a clash and the real name is read back.
- **Control flow:** one ordered control thread (HandlerThread, background priority) + one worker thread per running segment, ONE lock for all state held only for short updates; "paused" is shown at once and the checkpoint (fsync, then SQLite) lands when the last worker is gone.
- **Verified:** JVM only until the emulator run below; the real tablet has not run it. Everything in section 10 "Not verified" still applies.
