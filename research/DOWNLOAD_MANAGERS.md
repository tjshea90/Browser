# Download managers: what the best ones do, what Kestrel lacks, what to build (research for TASKS X3, 2026-11-12)
Read with `research/DOWNLOADS.md` (design, still valid) and `research/VIDEO_DOWNLOADER.md`. Code READ: `core/.../download/*`, `core/.../media/StreamEngine.kt`, `app/.../download/*`, `GeckoFetcher`, `core/src/test/.../download/*`. Nothing was run or measured on the tablet.
Tags: [V] verified (S# = URL in section 8, or `file:function` read in this repo) - [E] my estimate or inference - [N] not verified (secondary source, or needs a measurement). Hours = engineer hours including tests.

## 0. Decisions I recommend (10 lines)
1. Keep the architecture: own pure-Kotlin engine, bounded Range windows, data-then-cursor checkpoints, If-Match / If-Unmodified-Since on every range request, NEEDS_DECISION when the file changed. For validator-backed downloads that already matches or beats Chrome and Android's DownloadManager on resume safety [V code, S7, S8, S17]; the validator-less path is the exception (M1).
2. Do NOT raise connection counts: 3 per download on this tablet (HTTP/1.1) and 3 H2 streams. Extra flows share one radio, H2 streams share one congestion window [V S27], servers may refuse them [V S26, S30]; the only published gain is Chrome's unmeasured 3-way split [N S36].
3. Safety first (M1): a validator-less download is continued after every drop with NO check (`FetchRequest.cond` is null); add Chrome Android's trailing-window compare (Chrome: 1024 B [V S8], use 4 KiB), compare the reply's Last-Modified, verify file length and segment table on reopen.
4. Hotspot outages (M2): transport errors while Android still says "online" end in FAILED after 8 tries (about 1-3 min [E]); make them WAITING_NETWORK with probe backoff, FAILED(STALLED) only after 30 min, and abort sockets at once when the default network changes.
5. A refused extra connection (M3): 403/429/503/reset on connection #2 must retire only that connection and lower a per-host cap; today it can fail the whole job while connection #1 is healthy [V `Engine.openWindow`, `transient`].
6. Auto-resume after a process kill (M4), at most 2 crashes per download: interrupted downloads now wait in PAUSED_SYSTEM for a tap, DownloadManager and Chrome resume by themselves [V S19, S7].
7. Speed (M5): bounded probe window for the HttpURLConnection build (a cut wastes nothing), 2 connections at once for files >= 8 MiB, 2.5 s evaluations, recycle connections below 25 % of the median, cut in-flight remainders at the end; benchmark 1/2/3 connections on the tablet before touching any cap.
8. Integrity and sanity (M6, M7): SHA-256 against a pasted checksum or `Repr-Digest` (ask with `Want-Repr-Digest`) in the existing VERIFYING state; sniff HTML/portal pages saved as binaries and HLS segments.
9. Control and IO (M8-M10): priorities (`Rec.prio` is stored, unused), Auto/1/2/3 connections, polite mode, token-bucket limit; checkpoint fsync off the control thread; mirror-flip retry and "continue earlier download".
10. Order and cost [E]: T0 raw-socket fake server 6 h, M1 10, M2 7, M3 6, M4 4, M5 11, M6 8, M7 5, M8 12, M9 5, M10 4 = about 78 h (UI of M10 +6). Start with the 3 h hot-fix subset of M1 (reopen checks + Last-Modified compare). Skip list: section 6.

## 1. What the reference downloaders do (numbers from sources)
| Tool | Connections and splitting | Resume and failure rules | Other defaults | Src |
|---|---|---|---|---|
| aria2 | `-s` 5 splits but `-x` 1 connection per server by default; `-k` 20M ("does not split less than 2*SIZE"); `-j` 5 downloads; `--piece-length` 1M | `--always-resume` true; `.aria2` control file (lose it = cannot resume); `--max-tries` 5, `--retry-wait` 0; `--lowest-speed-limit` closes slow connections; `--timeout` 60 | `--file-allocation` prealloc (falloc best on ext4), none under 5M; keep-alive on, pipelining off ("usually no performance gain"); global and per-download speed limits; `--checksum`; `--conditional-get`; `--optimize-concurrent-downloads` N = 5 + 25*log10(Mbps) | S1 |
| Chromium (parallel on by default on Android only) | 3 requests; min slice 1,365,333 B; slice = max(total/3, min); last slice open-ended; delay 0 ms; extra requests only if remaining time > 2 s; split ONCE (`BuildParallelRequests`), a stream ends when it reaches data another stream wrote; needs ETag or Last-Modified + known length + GET; H2 and QUIC allowed (flags on) | resume `Range: bytes=N-` with If-Range; workers use If-Match / If-Unmodified-Since (`use_if_range` false); no strong validator: Android re-fetches `validation_length` bytes (default 1024) before the offset and compares, restart at 0 if the offset is not larger; prefix hash check (`FILE_HASH_MISMATCH`); `kMaxAutoResumeAttempts` 5; a failed stream whose start offset a neighbour already covers lets the download finish | `download_file_impl.cc` calls no fsync/Flush; cross-origin redirects on worker requests are errors | S2-S8 |
| IDM | "up to 8 times"; dynamic: "When new connection becomes available IDM finds the largest segment to download and divide it in half"; reuses connections "without additional connect and login stages" | "Refresh download address" opens the page the link came from, takes the new link, resumes the same file | queues + scheduler, hourly quota, antivirus hook; no checksum or limiter wording on the features page | S9, S10 |
| 1DM+ / ADM (Android) | 1DM+ listing: up to 32 parts per download, 30 downloads; ADM numbers differ between listings [N] | "Pause and Resume feature with supported links" | speed limiter (global and individual), scheduler, proxies | S11 |
| Android DownloadManager | 1 connection per download; `Range: bytes=N-` + `If-Match: <ETag>`; `Accept-Encoding: identity` (HttpURLConnection path); `Connection: close` | 200 on resume, 412, 416 = cannot resume; `MAX_RETRIES` 5, first delay 30 s then doubling; Retry-After clamped 30 s..24 h; survives reboots (docs) | fsync before every progress save, saved after >= 64 KiB and >= 2 s; 8 KiB buffer; max 5 redirects | S17-S19 |
| Firefox | desktop `nsIResumableChannel.resumeAt(startPos, entityID)` -> `NS_ERROR_ENTITY_CHANGED` / `NS_ERROR_NOT_RESUMABLE`; Fenix: one request per file, 32 KiB chunks | Fenix resume sends only `Range: bytes=N-` (no validator); a 200 reply deletes the partial and restarts | Necko: http2 and http3 on, 100 concurrent streams | S31-S33 |
| OkDownload, PRDownloader, Fetch2, Ketch | OkDownload blocks by size: about <1 MiB 1, <5 MiB 2, <50 MiB 3, <100 MiB 4, else 5; the others: 1 connection per file | OkDownload: ETag change, 412, 201/205 with range not from 0 -> restart, needs a seekable output; PRDownloader: compares response ETag with the stored one, 416 -> restart; Ketch: ETag stored only, rest not stated | Fetch2: per-download priority, concurrency limit, network type, retry; Ketch: WorkManager | S12-S15 |
| DownThemAll, JDownloader, FDM | DTA defaults: 4 concurrent downloads, 5 retries, retry-time 10 (unit not stated), per-domain limit list; JDownloader (community): 1 chunk by default, more than 5 triggers hoster blocks [N]; FDM: per-server connection setting, 3 traffic modes [N] | - | - | S34, S39 |
| OkHttp / Android HttpURLConnection | OkHttp adds `Accept-Encoding: gzip` only if the request has neither Accept-Encoding nor Range; Android's HttpURLConnection is an OkHttp fork pinned to HTTP/1.1 | it gunzips transparently unless Accept-Encoding is set and then clears Content-Length/Encoding; may pool sockets, `disconnect()` "may return the socket to a pool" | - | S16, S20, S24 |

## 2. Technique notes (what to copy, what to skip)
- **Segmentation:** only IDM (halve the largest segment) and Kestrel (`Planner.nextWork`) re-split while running; Chrome splits once, aria2 hands out pieces. Minimum split: Chrome 1.3 MiB, Kestrel 2 MiB (tail >= 4 MiB), aria2 20 MiB [V S1-S3, S9, code].
- **How many connections:** published defaults sit at 1 (aria2 per server, Android DM, Firefox) to 3-5 (Chrome 3, OkDownload up to 5, DTA 4 downloads); 16-32 are listing claims. No primary data shows a gain on a shared cellular radio; Chrome shipped 3 without published numbers and an informal Android test saw 1 GB go 1.39 -> 1.21 min [N S36]. Per-flow TCP rate ~ MSS/(RTT*sqrt(loss)) [V S37], so extra flows help on lossy or per-flow-capped paths, not when the radio is the bottleneck [E].
- **Why 403/429/503:** a client "ought to limit the number of simultaneous open connections ... to a given server", a server "might reject traffic that it deems abusive" [V S26]; nginx `limit_conn` answers 503 by default [V S30].
- **H2 vs several TCP flows:** "Clients SHOULD NOT open more than one HTTP/2 connection to a given host and port pair" [V S27], so N range streams = one connection and one congestion window [E]; Gecko cannot be forced onto a second connection [N]. The hill-climb (keep +1 only if +15 %) is the right control for both fetchers.
- **Warm-up:** a new TCP connection starts at IW10 (<= 14,600 B) [V S29] plus a TLS handshake [E]; the cure is reuse, not more connections. Reuse over windows in our HttpURLConnection path is unproven (G7).
- **Range rules [V S25]:** a server MAY ignore Range (14.2); If-Range MUST NOT be sent without Range, MUST NOT carry a weak tag, a date only when no ETag exists (13.1.5); a false If-Range gives a full 200 (13.2.2); ranges address the ENCODED bytes (14.1.2); a client MUST inspect Content-Range of a 206 (15.3.7). Hence identity encoding plus If-Match (a clean 412 without body, never a surprise full re-download on metered data) [E].
- **Changed file:** every product restarts or asks (Chrome, Android DM, OkDownload, Firefox ENTITY_CHANGED). Kestrel's NEEDS_DECISION with the partial kept is the most careful variant.
- **Resume after drop or kill:** Android DM and Chrome do it by themselves (Chrome <= 5 times) [V S19, S7]; Chrome Android also resumes validator-less files via the 1024-byte compare [V S5, S7, S8]; IDM repairs expired links by re-capturing from the page [V S10].
- **Temp files:** Chrome `.crdownload` (visible; out-of-order slices go into one sparse file, `IsSparseFile()` [V S6]), aria2 control file; Kestrel's hidden pending MediaStore row ("Only your app can view the file until ... IS_PENDING back to 0" [V S22]) + SQLite is equivalent and safer. `allocateBytes(fd)` "will cause the system to delete any cached files necessary", may take seconds, worker thread only [V S21]; aria2 skips allocation under 5M [V S1]. Android DM fsyncs before saving progress [V S17]; Kestrel fsyncs before every cursor save.
- **Integrity [V S28]:** `Repr-Digest` covers the whole representation even in a 206, `Content-Digest` only the bytes sent; sha-256/512 active, md5/sha/crc32c deprecated (accidental corruption only); no protection against an on-path attacker without TLS; `Want-Repr-Digest` weights 0-10 are a hint. Content-MD5 was dropped as inconsistent with partial responses [V S35]. Chrome also hash-validates the on-disk prefix on resume [V S6]. Few servers send digests [E], so a pasted checksum is the main source.
- **Limit and queue:** aria2 and 1DM+ have global + per-download limits [V S1, S11]; Fetch2 per-download priority [V S14]; Kestrel has neither (G33, G34).
- **Threads:** with <= 6 blocking sockets and 64 KiB buffers, threads are not the bottleneck on 4 A53 cores [E]; the hot spots are fsync on the control thread and one syscall per read (G17, G28).

## 3. Gap analysis (Kestrel today vs the techniques above)
| # | Technique (who) | Kestrel today (file:function) | Status |
|---|---|---|---|
| G1 | Re-split the largest remaining range (IDM S9, aria2 S1) | `Planner.nextWork`: idle segment first, else upper half of the biggest unrequested tail, 64 KiB aligned, tail >= 2 x MIN_SPLIT (4 MiB) [V] | HAVE |
| G2 | Parallel start at once (Chrome 3, delay 0 S2; aria2 all `-s`) | `HillClimb.update`: 1 connection, 2nd at 1.5 s, then one step per two 4 s evaluations: 3rd connection at about 10 s, confirmed at 14 s [V arithmetic]; files under about 30 MB at 2 MB/s end before the ramp [E] | PARTIAL |
| G3 | Cut a stream without wasting bytes | Gecko probe and windows are bounded (`GeckoFetcher.probeWindow` 4 MiB) [V]; HttpURLConnection: `Fetcher.probeWindow` is 0, so segment 0 is an open-ended GET and a cut discards what sits in socket buffers (size unknown [N]); `EngineTest.parallelConnectionsFillTheFileExactly` only bounds waste at 8 MiB [V] | PARTIAL |
| G4 | Drop slow connections (aria2 `--lowest-speed-limit` S1); end-game steal | `Engine.doTick` watchdog is job-level (15 s without bytes on ANY worker); per-socket 20 s read timeout (`FetchRequest.readMs`); `nextWork` never cuts a window already requested [V] | PARTIAL |
| G5 | One connection budget | `Config` 4 per download / 6 total, `DownloadController` sets 3/3 on low-RAM; `StreamEngine.supervise` takes up to 3 workers per job (2 for the sound half) outside that budget [V] | PARTIAL |
| G6 | Per-host limits; a refused extra connection retires only itself | none: `Engine.openWindow` GONE -> one re-resolve -> `fail(LINK_EXPIRED)` stops ALL workers; `transient` fails the job when ONE segment reaches 8 failures [V] | MISSING |
| G7 | Connection reuse across windows; H2 multiplexing | HttpURLConnection is HTTP/1.1 only [V S24] and pools sockets [V S20]; `Engine.quiet` calls `disconnect()` after every window, reuse unproven [N]. Gecko: H2/H3 streams, one congestion window [V S27] | PARTIAL |
| G8 | Window size, shrinking tail | `Planner.window` 8 MiB (Gecko 4 MiB) down to 512 KiB [V] | HAVE |
| G9 | Servers without Range | `Engine.fullReply`, `noRange`; `Rec.resumable` hides Pause [V] | HAVE |
| G10 | Strong validator + If-Match / If-Unmodified-Since on every range request (Android DM S17, Chrome workers S8) | `Engine.openWindow` `cond`; `HttpSemantics.isStrongEtag`, `isStrongLastModified` (>= 1 s older than Date) [V] | HAVE |
| G11 | Validate every reply: first byte, total, ETag, Last-Modified | `HttpSemantics.classify` checks first byte, total, ETag; the reply's Last-Modified is never compared, so a server that ignores If-Unmodified-Since can splice a new file [V] | PARTIAL |
| G12 | Changed file: ask, keep partial (412 has no body) | `Engine.needDecision(FILE_CHANGED)`, Restart/Cancel [V] | HAVE |
| G13 | Resume without validators (Chrome Android: re-fetch 1024 B before the offset, compare; S5, S7, S8) | none. `Rec.resumable` is false (no Pause) yet every retry and network resume still continues with `cond == null`: an unchecked splice [V `openWindow`, `segmentLoop`] | MISSING |
| G14 | Mirror pools / per-mirror ETags | every start goes to the clicked URL first (`Engine.start`: `useOriginal = true`), a pool can hand out another mirror; CHANGED then asks the user [V] | PARTIAL |
| G15 | Unknown length / close-delimited body | EOF means complete (`Engine.pump`, `seg.end < 0`); a cut chunked body throws in the HTTP stack, a plain close cannot be told apart [E]; AOSP DM accepts it only if chunked or `Connection: close` [V S17] | PARTIAL |
| G16 | Data first, cursor second | `Engine.checkpoint`: snapshot -> `sink.sync()` -> `store.save` (WAL, synchronous=NORMAL); Android DM does the same [V S17] | HAVE |
| G17 | Cheap checkpoints | on the single control thread, `Os.fsync` not `fdatasync`, every 5 s / 8 MiB [V]; cost on this eMMC unmeasured [N] | PARTIAL |
| G18 | Verify the partial on reopen | `MediaStoreSinks.reopen` / `FileSinks.reopen` only open; no length check; `Planner.check` is called by tests only [V grep] | MISSING |
| G19 | Resume after network loss | `Engine.onNetwork` + `doTick`: WAITING_NETWORK, 1-3 s jitter [V] | HAVE |
| G20 | Dead upstream while Wi-Fi stays up (hotspot; no callback) | `DownloadController.refreshNet` counts the INTERNET capability only, so `Engine.transient` keeps retrying and ends in FAILED(NETWORK) after 8 failures (1-3 min [E]); `Reason.STALLED` is never set [V grep] | MISSING |
| G21 | Drop sockets when the default network changes | `DownloadController.startNetWatch` -> `Engine.onNetwork` acts only when offline [V]; stale sockets wait for the 15-20 s timeouts [E] | MISSING |
| G22 | Resume after kill or reboot (Android DM S19; Chrome <= 5 tries S7) | `Engine.restore` -> PAUSED_SYSTEM until a tap (DOWNLOADS.md section 11 "not built yet") [V] | MISSING |
| G23 | Low-memory and FGS-timeout callbacks | no `onTimeout` / `onTrimMemory` in `download/*` [V grep]; irrelevant on API 29, needed for the Android 15+ promise | MISSING |
| G24 | Backoff + Retry-After | `Backoff.delayMs` full jitter 1-60 s, `retryAfterMs` cap 10 min; Android DM: 30 s doubling x5, Retry-After 30 s..24 h [V S18] | HAVE |
| G25 | Failure taxonomy | busy / link dead / changed / TLS / no space are separate `Reason`s [V]; "extra connection refused" and "upstream dead" are not (G6, G20) | PARTIAL |
| G26 | Preallocate + hidden partial + atomic publish | `MediaStoreSinks` (IS_PENDING, `allocateBytes` -> `posix_fallocate` -> `ftruncate`), `FileSinks` (.part + rename) [V] | HAVE |
| G27 | Skip preallocation for small files (aria2 5M S1) | always allocates; `allocateBytes` "may take several seconds" [V S21] | PARTIAL |
| G28 | Write coalescing | `Engine.pump`: one `pwrite` per read (platform streams often return <= 8 KiB [E]); DOWNLOADS.md planned 128 KiB batches [V] | PARTIAL |
| G29 | Length check | `Engine.completeJob`: sum of cursors == total [V] (file length proves nothing: it is preallocated) | HAVE |
| G30 | Digest verification | `HttpSemantics.reprDigest`, `Reason.DIGEST`, `DlState.VERIFYING` exist; nothing hashes; `Source` has no digest field [V grep] | MISSING |
| G31 | Content sanity (HTML/portal page, HLS segment magic) | none; `StreamEngine.fetchTo` renames any bytes to `.seg`, `appendNext` writes them [V] | MISSING |
| G32 | Max simultaneous downloads | `Config.maxActive` 2, FIFO by `created` (`Engine.startQueued`), not user-settable [V] | HAVE |
| G33 | Priorities, reorder, start now (Fetch2 S14) | `Rec.prio` persisted, never read by the scheduler [V] | MISSING |
| G34 | Speed limit, polite mode (aria2 S1, 1DM+ S11) | none (DOWNLOADS.md section 11 "not built yet") [V] | MISSING |
| G35 | Expired signed link | `Engine.openWindow` GONE: one re-resolve through the clicked URL, then LINK_EXPIRED [V]; no refresh-from-page (IDM S10) | PARTIAL |
| G36 | Identity encoding | `Engine.headersFor`, `HttpSemantics.isEncoded` [V]; OkHttp skips gzip when Range is set [V S16], Android DM sets identity [V S17]; Gecko honouring it unverified [N] | HAVE |
| G37 | Redirects | `UrlConnectionFetcher.open`: <= 10 hops by hand, never https -> http, Cookie/Authorization dropped cross-host [V]; Chrome treats cross-origin redirects of worker requests as errors [V S3] | HAVE |
| G38 | File names | `FileNames.choose/sanitize` (RFC 6266/8187, 255 UTF-8 bytes, reserved names) [V]; sniffing = G31 | HAVE |
| G39 | Test fidelity | `FakeServer` is an in-memory `Fetcher`; no raw-socket server, `UrlConnectionFetcher` has no test; the kill test is one scenario, the 1000-seed fuzz of DOWNLOADS.md section 8 is not built [V grep] | PARTIAL |

## 4. Ranking of the open items (value for THIS device x data-corruption risk)
Value = how often it bites on a metered, flaky hotspot, 2 GB RAM, 4 slow cores, eMMC, times the cost of the failure (taps, metered MB, bad file) [E]. "Exposure" = what can corrupt or waste data TODAY. "Change risk" = risk of the fix itself.
| Rank | ID (closes) | Item | Value | Exposure today | Change risk | Hours |
|---|---|---|---|---|---|---|
| 1 | M1 (G11, G13, G18) | Resume validation bundle | High; also gives validator-less files a Pause button | silent splice or zero-filled holes; the main path to a silently corrupt file today | Medium (resume path) | 10 |
| 2 | M2 (G20, G21, G25) | Outage ladder | High: the hotspot case | none (partial is safe); user must tap Retry | Low | 7 |
| 3 | M3 (G6, G25) | Refused extra connection | High: servers cap per IP | none; wasted retries, failed job | Low | 6 |
| 4 | M4 (G22) | Auto-resume after kill | Medium-high on 2 GB | none | Low | 4 |
| 5 | M5 (G2, G3, G4, G5, G7) | Planner v2 | Medium-high: speed and metered bytes; size of gain unknown [N] | cut waste of 1-4 MB per split [E] | Medium (planner invariants) | 11 |
| 6 | M6 (G30) | Digest verification | Medium: the only end-to-end check, sources rare | undetectable corruption class | Low | 8 |
| 7 | M7 (G31) | Content sanity | Medium: carrier/portal pages, HLS | HTML saved as a file or inside a video | Low | 5 |
| 8 | M8 (G32-G34) | Queue, polite, limiter, settings | Medium: owner asked for multiple downloads; browsing stays smooth | none | Low | 12 |
| 9 | M9 (G17, G27, G28) | IO path | Low-medium | none, but touches durability order | Medium | 5 |
| 10 | M10 (G14, G35) | Mirror flip, link refresh | Low-medium: depends on the hosts used | false "file changed" asks | Low | 4 (+6 UI) |
| - | T0 (G39) | Raw-socket fake server | Enabler for M1-M5 on the real fetcher | untested wire code | - | 6 |
Not ranked (accepted as is): G5 beyond M5, G15 (inherent), G23 (API 29 device), G7 until measured.

## 5. Implementation sketches
### T0 Raw-socket fake server (6 h)
`FakeServer` is a `Fetcher`, so the WebView build's only network path, `UrlConnectionFetcher`, has no test [V]. Add `FakeHttpServer` in `app/src/test`: `ServerSocket`, keep-alive, Range, If-Match, ETag, chunked, `Connection: close`, cut after N bytes, RST via SO_LINGER 0, 429/503 + Retry-After, redirects, accepted-connection counter. Drive `UrlConnectionFetcher`: header spelling, Content-Range parsing, status mapping, https -> http refusal, gzip trap, `abort()` unblocks a reader. The JVM's HttpURLConnection is not Android's OkHttp fork [V S20, S24], so reuse counts still need the emulator job. Failure mode: green on the JVM, red on the tablet; keep the emulator run.

### M1 Resume validation bundle (10 h; M1a = reopen + Last-Modified, 3 h)
Change: (a) `Sink.pread(pos, buf, off, len)` (`Os.pread` in `MediaStoreSinks` / `FileSinks`, array copy in `FakeSink`); (b) `Engine.openWindow`: when `rec.etag == null && rec.lastMod == null && cursor > 0` request `[max(0, cursor-4096), reqEnd)`; `pump` compares the first bytes with `sink.pread` before writing, mismatch -> `needDecision(FILE_CHANGED)`; cursor <= 4096 restarts at 0 (Chrome rule [V S7]); (c) `Rec.resumable` also true when `ranges == 1 && total > 0` (Pause for validator-less files); (d) `HttpSemantics.classify(..., storedLastMod)` compares the reply's Last-Modified; (e) `Engine.start`: reopen must give `sink.length() >= total` and `Planner.check(segs, total) == null`, else wipe and restart.
Test: `FakeServer(etag = null, lastModified = null)`: pause at 30 %, resume -> first request `from == cursor - 4096`, output identical, extra bytes <= 4 KiB per window; new `mutateAt(offset)` inside the last 4 KiB -> NEEDS_DECISION(FILE_CHANGED) with the partial kept; a change beyond the cursor with equal length passes (documented residual, only M6 catches it); 200 to the validation request -> NO_RANGE; Last-Modified-only server that ignores If-Unmodified-Since and serves a new file -> CHANGED; truncated sink on reopen -> restart, never zero holes.
Failure modes: off-by-one at the window start; validation request refused (403) must take the GONE path; +4 KiB per 8 MiB window (0.05 % [E]); never applied to parallel segments (they require validators).

### M2 Outage ladder (7 h)
Change: `Engine.transient`: for transport failures (`status == 0`) never `fail()`; after `cfg.outageFails` (3) consecutive failures and `cfg.outageMs` (25 s) without a byte, `requestStop(job, WAITING_NETWORK)` with `Reason.NETWORK`; `doTick` restarts a WAITING_NETWORK job at `job.resumeAt = now + fullJitter(5 s, cap 60 s)`, growing per failed probe and reset on progress; FAILED(STALLED) when `now - rec.lastProgress > 30 min`; HTTP 5xx keep the 8-try limit. `DownloadController.startNetWatch`: keep the default `Network`; when `onAvailable` / `onLost` changes it while online call `Engine.onNetworkSwitched()` = `abort()` every in-flight response, no state change (workers retry in about 1 s).
Test: `FakeNet.online` stays true; `FakeServer.down = true` for N ms after 30 % (throws `FetchException(TIMEOUT)`) -> state passes WAITING_NETWORK, never FAILED, bytes identical, probe count bounded (assert on `seen`); `ManualClock` +31 min -> FAILED(STALLED), partial kept, Retry resumes; `onNetworkSwitched` -> `abort()` called, recovery within 3 s of fake time.
Failure modes: probe storm (cap + jitter); `DownloadService.keepAwake` releases the wake lock while waiting, so with the screen off a probe waits for a radio wake-up, acceptable; each probe about 5 KB metered [E]; the user sees "Waiting for the connection..." instead of an error, intended.

### M3 Refused extra connection (6 h)
Change: in `Engine.openWindow`, 401/403/429/503/RESET/TIMEOUT before the first byte of a NON-primary connection (`job.alive > 1` and another worker moved bytes within 10 s) = "refused": `seg.worker = false`, new runtime `Seg.coolUntil = now + 30 s` (skipped by `Planner.nextWork`), `HillClimb.capDown(alive - 1)` with `noGrowthUntil = now + 10 min`, a learned per-host cap in a small map keyed by the host of `finalUrl` (expiry 24 h, memory only), worker leaves via `Exit` without `fail()`; Retry-After applies to that segment only. If no other worker moves, today's path (re-resolve, LINK_EXPIRED) stays.
Test: `FakeServer.connLimit = 1` with `overLimit` = 503 + Retry-After, 429, 403, RESET (reject when `concurrent` would exceed it): COMPLETED, bytes identical, never FAILED, `maxConcurrent` after learning == 1, <= 3 rejected requests per download; a second download from the same host starts at cap 1; `connLimit = 2` settles at 2; all connections 403 -> still LINK_EXPIRED.
Failure modes: thrash (cooldown), over-learning from one transient 503 (expiry), `plan()` relaunching the returned segment at once, an expired signed link mistaken for a limit (only when another worker is alive and moving).

### M4 Auto-resume after kill (4 h)
Change: `Rec.crashes` and `Rec.auto` (`SqliteStore` version 3 `ALTER TABLE`); `restore()` keeps PAUSED_SYSTEM, notes that the row was RUNNING/STARTING/WAITING (unclean exit) and increments `crashes`; new `Engine.resumeInterrupted(maxCrashes = 2)` called by `DownloadController.init` after `restore()` when `DownloadService.onStartCommand` gets a null intent (a START_STICKY restart redelivers no intent [E]; docs: "leave it in the started state" [V S23]) and from `BrowserActivity.onStart`; never for PAUSED_USER, NEEDS_DECISION or `!meteredOk`; `crashes` cleared after 60 s of progress.
Test: extend `EngineTest.aKilledProcessLosesAtMostOneCheckpointOfBytes`: after `restore()` + `resumeInterrupted()` it completes without `resume(id)` and refetches < 1 window; `crashes = 3` stays PAUSED_SYSTEM; PAUSED_USER stays paused; add the 1000-seed kill fuzz (kill at random instants, assert byte-identical output and `durable >= cursors`).
Failure modes: crash loop burning metered data (cap 2), resurrecting a paused download (flag), FGS start refused on Android 12+ (already caught in `DownloadService.start`), a URL that crashes the app on every start (the counter).

### M5 Planner v2: zero-waste cut, fast ramp, stragglers (11 h)
Change: (a) `UrlConnectionFetcher.probeWindow = 8 MiB` (Gecko already uses 4 MiB) so segment 0 is a bounded window and a cut costs nothing; a plain 200 still works via `fullReply`; (b) `HillClimb`: start at target 2 when `ranges != 0`, validator present, `total >= 8 MiB`; `evalMs` 2500 and grow in the same step that confirms the previous one (3 connections at about 2.5 s instead of 10 s [E]); (c) per-worker `Meter`; in `doTick` a worker below 25 % of the median of its siblings for 10 s gets `abort()` (cursor kept, `plan()` relaunches, at most once per 20 s per segment); (d) `Planner.nextWork` end-game: a worker idle > 2 s and no tail >= 4 MiB -> cut the slowest window's remainder at `cursor + 3 s x speed`; (e) `StreamEngine` draws its workers from the same `maxConnsTotal`.
Test: extend `FakeServer` with `perConnBps`, `totalBps` (sleep per chunk) and `slowConn = n` (n-th connection at 10 %): ramp reaches 3 connections within 4 s (`HillClimb` unit test with explicit `now`); one slow connection -> completion <= 1.3 x ideal; `servedBytes <= total + 256 KiB` for 24 MiB with 3 connections; `PlannerTest.invariantsHoldUnderRandomWork` gains the end-game cut; connection count changes <= 4 per 60 s (no flapping).
Failure modes: planner invariants (fuzz them), recycling a connection that is slow because the link is slow (needs >= 2 siblings), more flows -> more 429 (M3), abort waste on metered data (rate-limit aborts).

### M6 Digest verification (8 h)
Change: `Source.digest` ("sha-256:hex", from a paste field in the details sheet), `Rec.digestWant/digestSrc/digestGot` (+ columns); the probe request adds `Want-Repr-Digest: sha-256=10, sha-512=5` (free, honoured rarely [E]) and stores `HttpSemantics.reprDigest` of the first 200/206 when the user gave none; `completeJob`: if a digest exists -> VERIFYING (`DownloadService` already holds the wake lock for it), hash on a BACKGROUND thread through `Sink.pread` in 256 KiB steps, progress as `done`, cancellable; mismatch -> FAILED(DIGEST) with the file kept ("Keep / Delete"); "Check checksum" also on COMPLETED items; md5/sha1/crc32c accepted as "accidental corruption only" [V S28].
Test: `FakeServer.reprDigest` (right, wrong, absent, only `Content-Digest` = ignored, sent only on the 206); `FakeSink.corruptByte(offset)` after a parallel run: size check passes, digest catches it; cancel during VERIFYING; verify-after-the-fact on a COMPLETED item.
Failure modes: SHA-256 speed on this CPU unknown, maybe 30-50 MB/s -> 20-35 s per GB [N]: BACKGROUND priority, cancel, user-set size ceiling; pasted text with upper case, spaces or a `sha256:` prefix; file changed while verifying; never auto-delete.

### M7 Content sanity (5 h)
Change: `core/download/Sniff.kt` (pure): first 512 bytes -> HTML/XML page vs binary vs media (TS sync 0x47 at 0 and 188, ISO-BMFF `ftyp/styp/moof/sidx`, ZIP/PDF/ELF magic); `Engine.pump` checks the first buffer of segment 0 once: URL or MIME says binary but the bytes are HTML -> `needDecision(WRONG_CONTENT)` ("This looks like a web page, not a .zip. Keep / Cancel"); `StreamEngine.fetchTo`: check before `.part` -> `.seg`, retry twice (a portal page can disappear), then fail with a new `Reason.BAD_CONTENT` instead of appending HTML to the video; AES-128 segments are checked after decrypt.
Test: `FakeServer.bodyOverride = "<html>log in</html>"`, `contentType = text/html`, URL `.zip` -> NEEDS_DECISION; `.html` URL completes; HLS fake with segment 3 = HTML -> retries then BAD_CONTENT, `rec.next` unchanged.
Failure modes: false positives (legit HTML under a binary name: Keep is offered), gzip'd text; only plain-http portals are visible, TLS portals fail the certificate check first [E].

### M8 Queue, polite mode, limiter, settings (12 h; engine 6, UI 6)
Change: `Engine.startQueued` orders by `(-prio, created)`; `Engine.setPriority/startNow`; `Config` from `Prefs`: active 1-3 (default 2), connections Auto/1/2/3, limit off/1/2/5 MB/s; `TokenBucket(rate, burst 256 KiB)` in core with injected `Clock`, `take(n)` after each read in `Engine.pump` (TCP back-pressure on URLConnection; Gecko duty-cycles 1 MiB windows because its queue is unbounded [V DOWNLOADS.md 3.5]); limiter wait counts as progress for the stall watchdog; `Engine.setPolite(true)` while a tab loads -> `Planner.cap` returns 1.
Test: ManualClock token-bucket exactness (10 MiB at 1 MiB/s takes 9.5-10.5 s); A, B, C with maxActive 1 and C promoted -> A, C, B; polite -> `maxConcurrent == 1` while set; limiter + `stallMs` never trips; settings round-trip.
Failure modes: starvation of low priority, watchdog vs limiter, UI clutter (owner wants big simple buttons: one Downloads settings group).

### M9 IO path (5 h)
Change: `Engine.checkpoint` runs `sink.sync()` on a dedicated single-thread IO executor (today on the control thread that also serves pause, plan and exit events), one pending request per job; `Os.fdatasync` (API 21 [V S38]) when the file is preallocated, fsync at publish; per-worker 128 KiB write buffer flushed when full, at EOF/stop or after 250 ms, cursors advance only after the flush (invariant "cursor <= bytes written" kept); skip `allocateBytes` below 5 MiB.
Test: `FakeSink` counts `pwrite` calls (<= 1 per 128 KiB) and records the thread calling `sync()` (never `test-control`); kill fuzz as in M4; pause mid-buffer loses no byte.
Failure modes: buffered bytes lost on stop (flush before counting), `fdatasync` weaker when the size changes (use fsync if not preallocated), "paused" feedback latency.

### M10 Mirror flip and link refresh (4 h + 6 h UI)
Change: `Engine.openWindow` on CHANGED while `job.useOriginal` was true and `rec.finalUrl` exists: retry once on `rec.finalUrl` before NEEDS_DECISION (every resume starts at the clicked URL [V `Engine.start`], so a mirror pool with per-mirror ETags looks like "file changed"); IDM-style refresh [V S10]: a new download whose name + total + validator equal a FAILED/LINK_EXPIRED partial offers "Continue earlier download" (`Engine.adopt(oldId, newUrl)` keeps segments, swaps `url` / `finalUrl`, If-Match decides).
Test: `FakeServer.etagFor(url)` (mirror A/B) with alternating `redirectTo` -> completes, no decision; adopt with the same ETag resumes, with another ETag -> NEEDS_DECISION.
Failure modes: URL ping-pong (one retry only), Cookie/Authorization leaking across hosts (already dropped [V `UrlConnectionFetcher`]), adopting the wrong partial (needs equal total AND validator, user confirms).

## 6. Not worth building here
- More than 4 connections per file (IDM "up to 8 times", 1DM+ 32 parts): one radio, 403/429 risk [E, V S26].
- Metalink, BitTorrent, multi-source mirrors, chunk checksums (aria2 `--realtime-chunk-checksum` needs hashes from Metalink/torrent [V S1]); pipelining (aria2: "usually no performance gain" [V S1]); hedged duplicate tail requests (cost metered bytes); a user-space disk cache (aria2 `--disk-cache` 16M [V S1]; the kernel page cache does it on 2 GB); clock scheduler and hourly quotas (IDM); proxies, antivirus hooks, site grabbers, FTP/SFTP.
- Own HTTP/2 client, OkHttp, Cronet (dependency rule in CLAUDE.md).

## 7. Open measurements [N] (nothing above depends on them except M5's defaults)
1. 1/2/3/4 connections on the real hotspot, H1.1 host vs H3 CDN, screen on and off (Diagnostics A5): decides M5's constants and whether parallelism helps at all.
2. Does Android's HttpURLConnection reuse a socket across windows (count TLS handshakes on the emulator)? G7.
3. fsync / fdatasync cost on this eMMC; write speed on API 29 sdcardfs vs FUSE (direct-path random writes "up to twice as slow" on Android 11+ [V S22]).
4. SHA-256 and AES-GCM MB/s on the MT8168B (crypto extensions unknown).
5. Does `GeckoWebExecutor` honour `Accept-Encoding: identity`, send the profile cookies, report the final `response.uri`?
6. How often Android kills the download process on this 2 GB tablet (M4's real value); how often the owner's hosts hand out per-mirror ETags (M10).

## 8. Sources (all fetched or searched 2026-11-12)
S1 aria2 manual https://aria2.github.io/manual/en/html/aria2c.html (first 100k of 179k characters read)
S2 https://raw.githubusercontent.com/chromium/chromium/main/components/download/internal/common/parallel_download_utils.cc
S3 https://raw.githubusercontent.com/chromium/chromium/main/components/download/internal/common/parallel_download_job.cc
S4 https://raw.githubusercontent.com/chromium/chromium/main/components/download/internal/common/download_job_factory.cc
S5 https://raw.githubusercontent.com/chromium/chromium/main/components/download/public/common/download_features.cc
S6 https://raw.githubusercontent.com/chromium/chromium/main/components/download/internal/common/download_file_impl.cc
S7 https://raw.githubusercontent.com/chromium/chromium/main/components/download/internal/common/download_item_impl.cc (first 100k of 106k read)
S8 https://raw.githubusercontent.com/chromium/chromium/main/components/download/internal/common/download_utils.cc
S9 IDM features https://www.internetdownloadmanager.com/features.html
S10 IDM FAQ "Refresh download address" https://www.internetdownloadmanager.com/register/new_faq/sites2_3.html (search excerpt, page not fetched)
S11 1DM+ listing mirror https://appgoblin.info/apps/idm.internet.download.manager.plus (vendor text, third-party mirror)
S12 https://raw.githubusercontent.com/lingochamp/okdownload/master/okdownload/src/main/java/com/liulishuo/okdownload/core/download/DownloadStrategy.java
S13 https://raw.githubusercontent.com/MindorksOpenSource/PRDownloader/master/prdownloader/src/main/java/com/downloader/internal/DownloadTask.java
S14 https://github.com/tonyofrancis/Fetch   S15 https://github.com/khushpanchal/Ketch
S16 https://raw.githubusercontent.com/square/okhttp/master/okhttp/src/commonJvmAndroid/kotlin/okhttp3/internal/http/BridgeInterceptor.kt
S17 AOSP DownloadThread https://android.googlesource.com/platform/packages/providers/DownloadProvider/+/refs/heads/main/src/com/android/providers/downloads/DownloadThread.java
S18 AOSP Constants https://android.googlesource.com/platform/packages/providers/DownloadProvider/+/refs/heads/main/src/com/android/providers/downloads/Constants.java
S19 https://developer.android.com/reference/android/app/DownloadManager   S20 https://developer.android.com/reference/java/net/HttpURLConnection
S21 https://developer.android.com/reference/android/os/storage/StorageManager   S22 https://developer.android.com/training/data-storage/shared/media
S23 https://developer.android.com/reference/android/app/Service
S24 https://raw.githubusercontent.com/Reginer/aosp-android-jar/main/android-36/src/com/android/okhttp/HttpsHandler.java
S25 RFC 9110 https://www.rfc-editor.org/rfc/rfc9110.html (13.1.5, 13.2.2, 14.1.2, 14.2, 14.4, 15.3.7)   S26 RFC 9112 9.4 https://www.rfc-editor.org/rfc/rfc9112.html
S27 RFC 9113 9.1 https://www.rfc-editor.org/rfc/rfc9113.html   S28 RFC 9530 https://www.rfc-editor.org/rfc/rfc9530.html   S29 RFC 6928 https://www.rfc-editor.org/rfc/rfc6928
S30 nginx limit_conn https://nginx.org/r/limit_conn (search excerpt)
S31 https://raw.githubusercontent.com/mozilla-firefox/firefox/main/netwerk/base/nsIResumableChannel.idl
S32 https://raw.githubusercontent.com/mozilla-firefox/firefox/main/mobile/android/android-components/components/feature/downloads/src/main/java/mozilla/components/feature/downloads/AbstractFetchDownloadService.kt
S33 https://raw.githubusercontent.com/mozilla-firefox/firefox/main/modules/libpref/init/StaticPrefList.yaml (http2 and http3 prefs)
S34 https://raw.githubusercontent.com/downthemall/downthemall/master/data/prefs.json
S35 Digest Headers draft-05 (Content-MD5 note) https://datatracker.ietf.org/doc/html/draft-ietf-httpbis-digest-headers-05 (search excerpt)
S36 informal Chrome test https://techwiser.com/how-to-enable-parallel-downloading-in-chrome/ [N]   S37 Mathis et al. 1997 https://courses.cs.duke.edu/fall24/compsci514/readings/mathis-tcpmodel-ccr97.pdf (formula via search excerpt)
S38 local Android SDK, platform 37.0 `api-versions.xml`: `Os.fdatasync`, `Os.posix_fallocate`, `StorageManager.allocateBytes` (since 26)
S39 community notes [N] https://www.brainbound.blog/jdownloader2-guide-speed-optimization and https://techwalla.com/articles/how-to-speed-up-free-download-manager
