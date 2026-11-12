package app.kestrel.core.download

import java.io.IOException
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantLock

/**
 * The download engine (design: research/DOWNLOADS.md). Pure Kotlin, no Android: everything it needs comes through ports (Fetcher, SinkFactory, Store, NetGate, Clock).
 *
 * Threads: every command (enqueue / pause / resume / tick ...) runs on [control], one ordered thread; workers are plain threads, one per running segment, that
 * only move bytes. All state lives in [Rec]/[Seg] objects guarded by ONE lock, held only for short updates, never across IO. The Android layer gets immutable
 * [DownloadView] snapshots. A kill loses at most one checkpoint interval of already-written bytes: offsets in the store are saved only AFTER the data was fsynced.
 */
class Engine(
    private val fetchers: (Int) -> Fetcher?,
    private val sinks: SinkFactory,
    private val store: Store,
    private val net: NetGate,
    private val control: Executor,
    private val clock: Clock = Clock.System,
    private val cfg: Config = Config(),
    private val spawner: Spawner = Spawner { name, task -> Thread(task, name).also { it.isDaemon = true; it.start() } },
    /** One limit for every download of the app (shared with the stream engine); 0 = none. */
    val limiter: RateLimiter = RateLimiter(clock),
) {
    interface Listener { fun onChanged(id: Long) }

    private class Stopped : RuntimeException(null, null, false, false)   // the job was paused / canceled / lost the network
    private class Exit : RuntimeException(null, null, false, false)      // this worker is done; the job's state was already set
    private class Retry : RuntimeException(null, null, false, false)     // try the request again (already waited)

    /** [vFrom]/[vLen]: the reply starts [vLen] bytes BEFORE the resume point so the engine can compare them with what the file already holds (a file the server gave no validator for). */
    private class Opened(val resp: FetchResponse, val reqEnd: Long, val vFrom: Long = -1, val vLen: Int = 0)

    private class Worker { @Volatile var resp: FetchResponse? = null; var seg: Seg? = null }

    private inner class Job(val rec: Rec) {
        @Volatile var stop = false
        var stopState: DlState? = null
        var stopReason = Reason.NONE
        var stopDetail = 0
        var restartAfterStop = false
        var sink: Sink? = null
        val workers = ArrayList<Worker>()
        var alive = 0
        val meter = Meter()
        val climb = HillClimb(cfg)
        var resumed = false
        var useOriginal = false      // the next request goes to the clicked URL again (signed links are re-issued after a pause)
        var reResolved = false
        var completing = false
        var lastBytesAt = 0L
        var lastNotify = 0L
        var lastCkpt = 0L
        var sinceCkpt = 0L
        var ckptPending = false
        var resumeAt = 0L
        var autoResume = false       // the last run ended in the middle of this download: carry on by itself ([resumeInterrupted])
        var expectBinary = false     // the caller / the file name says this is not a web page: a web page in its place is an error ([ContentSniff])
        var sniffed = false
        var outageSince = 0L         // an outage in the middle of the download began at this time (0 = none)
        var outageProbes = 0
        val mon = ReentrantLock()
        val wake = mon.newCondition()
    }

    private companion object { const val MAX_AUTO_CRASHES = 2; const val VERIFY_BYTES = 4096 }

    private val lock = Any()
    private val jobs = LinkedHashMap<Long, Job>()
    private val nextId = AtomicLong(1)
    private val nextPrivId = AtomicLong(-1)
    private val rnd = java.util.Random()
    private val listeners = CopyOnWriteArrayList<Listener>()

    fun addListener(l: Listener) { listeners.add(l) }
    fun removeListener(l: Listener) { listeners.remove(l) }
    private fun changed(id: Long) { for (l in listeners) try { l.onChanged(id) } catch (e: Exception) { /* a listener must not break the engine */ } }

    // ================================================================================================ public API

    /** Load what the last run left behind. Call once, before anything else, from any thread. Interrupted downloads come back PAUSED_SYSTEM (never started by themselves). */
    fun restore() {
        val recs = store.loadAll()
        synchronized(lock) {
            var max = 0L
            for (r in recs) {
                if (r.id > max) max = r.id
                if (r.state == DlState.CANCELED) { store.delete(r.id); continue }
                val wanted = r.state == DlState.RUNNING || r.state == DlState.STARTING || r.state == DlState.VERIFYING || r.state == DlState.QUEUED || r.state == DlState.WAITING_NETWORK || r.state == DlState.PAUSED_SYSTEM
                val unclean = r.state == DlState.RUNNING || r.state == DlState.STARTING || r.state == DlState.VERIFYING // the process ended while it was working
                if (!r.state.terminal && r.state != DlState.PAUSED_USER && r.state != DlState.NEEDS_DECISION) r.state = DlState.PAUSED_SYSTEM
                if (unclean) r.crashes++
                jobs[r.id] = Job(r).also { it.autoResume = wanted && r.crashes <= MAX_AUTO_CRASHES }
            }
            nextId.set(max + 1)
        }
    }

    /**
     * After [restore]: the downloads the last run was in the middle of carry on by themselves (Android's DownloadManager and Chrome do too). A download the user paused stays paused, one that
     * needs a decision stays asking, and one whose restarts keep ending the process before it moves a byte ([MAX_AUTO_CRASHES] in a row) is left for a tap.
     */
    fun resumeInterrupted() = control.execute {
        for (j in snapshotJobs()) {
            val go = synchronized(lock) { val g = j.autoResume && j.rec.state == DlState.PAUSED_SYSTEM; j.autoResume = false; g }
            if (go) requeue(j, wipe = false)
        }
    }

    /** Add a download; it starts when a slot is free. Returns its id at once (private downloads get negative ids and are never stored). */
    fun enqueue(src: Source): Long {
        val id = if (src.priv) nextPrivId.getAndDecrement() else nextId.getAndIncrement()
        control.execute { doEnqueue(id, src) }
        return id
    }

    fun pause(id: Long) = control.execute {
        val job = get(id) ?: return@execute
        val st = synchronized(lock) { job.rec.state }
        when {
            st == DlState.QUEUED -> { synchronized(lock) { job.rec.state = DlState.PAUSED_USER }; persist(job); changed(id) }
            (st == DlState.RUNNING || st == DlState.WAITING_NETWORK) && synchronized(lock) { job.rec.resumable } -> requestStop(job, DlState.PAUSED_USER)
        }
    }

    fun resume(id: Long) = control.execute {
        val job = get(id) ?: return@execute
        val st = synchronized(lock) { job.rec.state }
        if (st == DlState.PAUSED_USER || st == DlState.PAUSED_SYSTEM || st == DlState.WAITING_NETWORK || st == DlState.FAILED) requeue(job, wipe = st == DlState.FAILED && !synchronized(lock) { job.rec.resumable })
    }

    /** The "Restart" answer of a NEEDS_DECISION download: throw the partial file away and download again from the start. */
    fun restart(id: Long) = control.execute {
        val job = get(id) ?: return@execute
        requeue(job, wipe = true)
    }

    /** Stop and delete the partial file and the entry. */
    fun cancel(id: Long) = control.execute {
        val job = get(id) ?: return@execute
        requestStop(job, DlState.CANCELED)
    }

    /** Remove a finished / failed entry; [deleteFile] also deletes the downloaded file. */
    fun remove(id: Long, deleteFile: Boolean) = control.execute {
        val job = get(id) ?: return@execute
        val rec = job.rec
        val st = synchronized(lock) { rec.state }
        if (!st.terminal && st != DlState.PAUSED_USER && st != DlState.PAUSED_SYSTEM && st != DlState.NEEDS_DECISION) return@execute
        val uri = synchronized(lock) { rec.destUri }
        if (uri != null && (deleteFile || st != DlState.COMPLETED)) try { sinks.delete(uri) } catch (e: Exception) { /* already gone */ }
        synchronized(lock) { jobs.remove(rec.id) }
        if (!rec.priv) store.delete(rec.id)
        changed(rec.id)
    }

    fun clearFinished() = control.execute {
        val ids = synchronized(lock) { jobs.values.filter { it.rec.state == DlState.COMPLETED || it.rec.state == DlState.FAILED || it.rec.state == DlState.CANCELED }.map { it.rec.id } }
        for (id in ids) { val j = get(id) ?: continue; synchronized(lock) { jobs.remove(id) }; if (!j.rec.priv) store.delete(id); changed(id) }
    }

    fun pauseAll() = control.execute { for (j in snapshotJobs()) if (synchronized(lock) { j.rec.state == DlState.RUNNING && j.rec.resumable }) requestStop(j, DlState.PAUSED_USER) }

    fun resumeAll() = control.execute {
        for (j in snapshotJobs()) {
            val st = synchronized(lock) { j.rec.state }
            if (st == DlState.PAUSED_USER || st == DlState.PAUSED_SYSTEM || st == DlState.WAITING_NETWORK) requeue(j, wipe = false)
        }
    }

    /** The app is going away (service destroyed, memory trimmed): stop everything cleanly so nothing is lost. Entries come back PAUSED_SYSTEM. */
    fun pauseAllSystem() = control.execute { for (j in snapshotJobs()) if (synchronized(lock) { j.rec.state.active }) requestStop(j, DlState.PAUSED_SYSTEM) }

    /** Once a second from the owner (the service): planner, stall watchdog, periodic checkpoint, network resume, queue. */
    fun tick() = control.execute { doTick() }

    /** The network changed ([NetGate.online] has the new value). */
    fun onNetwork() = control.execute {
        val now = clock.now()
        for (j in snapshotJobs()) {
            val st = synchronized(lock) { j.rec.state }
            if (!net.online) { if (st == DlState.STARTING || st == DlState.RUNNING) requestStop(j, DlState.WAITING_NETWORK) }
            else if (st == DlState.WAITING_NETWORK) synchronized(lock) { j.resumeAt = now + 1000 + rnd.nextInt(2000) }
        }
    }

    fun views(): List<DownloadView> = synchronized(lock) { jobs.values.map { view(it) }.sortedByDescending { it.created } }
    fun view(id: Long): DownloadView? = synchronized(lock) { jobs[id]?.let { view(it) } }

    /** The service must stay alive while anything is queued, moving bytes or waiting for the network. */
    fun needsService(): Boolean = synchronized(lock) { jobs.values.any { val s = it.rec.state; s == DlState.QUEUED || s.active || s == DlState.WAITING_NETWORK } }

    fun counts(): IntArray = synchronized(lock) {
        var running = 0; var queued = 0; var paused = 0
        for (j in jobs.values) when (j.rec.state) { DlState.RUNNING, DlState.STARTING, DlState.VERIFYING -> running++; DlState.QUEUED -> queued++; DlState.PAUSED_USER, DlState.PAUSED_SYSTEM, DlState.WAITING_NETWORK -> paused++; else -> {} }
        intArrayOf(running, queued, paused)
    }

    // ================================================================================================ control-thread operations

    private fun get(id: Long): Job? = synchronized(lock) { jobs[id] }
    private fun snapshotJobs(): List<Job> = synchronized(lock) { jobs.values.toList() }

    private fun doEnqueue(id: Long, s: Source) {
        val r = Rec(id)
        r.url = s.url; r.pageUrl = s.pageUrl; r.headers = s.headers; r.mime = s.mime; r.fetcher = s.fetcher; r.priv = s.priv; r.meteredOk = s.meteredOk; r.risk = s.risk; r.destKind = s.destKind; r.nameFixed = s.nameFixed
        r.name = FileNames.sanitize(s.nameHint ?: FileNames.fromUrl(s.url) ?: "download")
        r.created = clock.now(); r.updated = r.created
        if (!(s.url.startsWith("http://", true) || s.url.startsWith("https://", true))) { r.state = DlState.FAILED; r.reason = Reason.BAD_URL; r.finished = r.created }
        if (!r.priv) store.insert(r.snapshot())
        synchronized(lock) { jobs[id] = Job(r) }
        changed(id)
        startQueued()
    }

    private fun startQueued() {
        val go = ArrayList<Job>()
        synchronized(lock) {
            var active = jobs.values.count { it.rec.state.active }
            for (j in jobs.values.sortedBy { it.rec.created }) {
                if (active >= cfg.maxActive) break
                if (j.rec.state == DlState.QUEUED && j.alive == 0 && j.stopState == null) { j.rec.state = DlState.STARTING; go.add(j); active++ }
            }
        }
        for (j in go) start(j)
    }

    private fun requeue(job: Job, wipe: Boolean) {
        val rec = job.rec
        val again: Boolean
        synchronized(lock) {
            rec.fails = 0; rec.reason = Reason.NONE; rec.detail = 0
            again = job.alive > 0 || job.stopState != null
            if (!again && wipe) wipePartial(job)
            rec.state = DlState.QUEUED
            if (again) { job.restartAfterStop = true; job.stop = true }
        }
        if (!again && wipe) persist(job)
        changed(rec.id)
        if (!again) startQueued() else control.execute { tryFinishStop(job) }
    }

    /** Throw the partial file and everything learned about the file away (under the lock or from a quiet job). */
    private fun wipePartial(job: Job) {
        val rec = job.rec
        val uri = rec.destUri
        try { job.sink?.discard() } catch (e: Exception) { /* ignore */ }
        if (job.sink == null && uri != null) try { sinks.delete(uri) } catch (e: Exception) { /* ignore */ }
        job.sink = null
        rec.segs.clear(); rec.done = 0; rec.destUri = null; rec.total = -1; rec.ranges = -1; rec.etag = null; rec.lastMod = null; rec.dateHdr = null
        job.resumed = false
    }

    private fun start(job: Job) {
        val rec = job.rec
        val now = clock.now()
        synchronized(lock) {
            job.stop = false; job.stopState = null; job.restartAfterStop = false; job.completing = false
            rec.state = DlState.STARTING; rec.reason = Reason.NONE; rec.detail = 0
            job.climb.reset(); job.meter.reset(); job.reResolved = false
            job.lastBytesAt = now; job.lastCkpt = now; job.sinceCkpt = 0; job.ckptPending = false
        }
        changed(rec.id)
        val partial = synchronized(lock) { rec.segs.isNotEmpty() }
        if (!partial) { spawnWorker(job, null); return }
        val uri = synchronized(lock) { rec.destUri }
        val sink = if (uri != null) try { sinks.reopen(uri, rec.total) } catch (e: Exception) { null } else null
        if (sink == null) { synchronized(lock) { wipePartial(job) }; spawnWorker(job, null); return } // the partial file is gone: start over
        val allDone: Boolean
        synchronized(lock) {
            job.sink = sink; job.resumed = true; job.useOriginal = true
            rec.state = DlState.RUNNING
            for (s in rec.segs) { s.worker = false; s.live = false; s.reqEnd = -1; s.fails = 0 }
            allDone = rec.segs.all { it.done }
        }
        if (allDone) { completeJob(job); return }
        plan(job)
        changed(rec.id)
    }

    /** Planner: how many connections, and which byte ranges they take (control thread; also after every worker exit). */
    private fun plan(job: Job) {
        val rec = job.rec
        val launches = ArrayList<Seg>()
        synchronized(lock) {
            if (rec.state != DlState.RUNNING || job.stop) return
            val now = clock.now()
            val speed = job.meter.bytesPerSec(now)
            val globalRunning = jobs.values.sumOf { it.alive }
            val cap = Planner.cap(rec, cfg, speed, (cfg.maxConnsTotal - globalRunning).coerceAtLeast(0), job.alive)
            val target = job.climb.update(now, speed, cap, rec.total)
            var free = target - job.alive
            while (free > 0) {
                val s = Planner.nextWork(rec.segs, (speed * 3).toLong()) ?: break
                s.worker = true
                launches.add(s)
                free--
            }
        }
        for (s in launches) spawnWorker(job, s)
    }

    private fun doTick() {
        val now = clock.now()
        var anyQueued = false
        for (j in snapshotJobs()) {
            val st = synchronized(lock) { j.rec.state }
            when (st) {
                DlState.RUNNING -> {
                    plan(j)
                    synchronized(lock) {
                        if (j.alive > 0 && now - j.lastBytesAt > cfg.stallMs) { // no bytes for a long time: cut the sockets, the workers retry
                            j.lastBytesAt = now
                            for (w in j.workers) try { w.resp?.abort() } catch (e: Exception) { /* ignore */ }
                        }
                    }
                    val due = synchronized(lock) { j.sinceCkpt > 0 && now - j.lastCkpt >= cfg.checkpointMs && !j.ckptPending }
                    if (due) checkpoint(j)
                    changed(j.rec.id)
                }
                DlState.WAITING_NETWORK -> {
                    val gaveUp = synchronized(lock) { j.outageSince != 0L && now - j.outageSince >= cfg.outageGiveUpMs }
                    if (gaveUp) fail(j, Reason.STALLED, 0) // half an hour without a byte: the partial file stays, Retry continues it
                    else if (net.online && synchronized(lock) { now >= j.resumeAt }) requeue(j, wipe = false)
                }
                DlState.QUEUED -> anyQueued = true
                else -> {}
            }
        }
        if (anyQueued) startQueued()
    }

    // ================================================================================================ workers

    private fun spawnWorker(job: Job, seg: Seg?) {
        val w = Worker()
        w.seg = seg
        synchronized(lock) { job.workers.add(w); job.alive++ }
        try {
            spawner.spawn("dl-${job.rec.id}") { try { runWorker(job, w) } finally { workerExit(job, w) } }
        } catch (e: Throwable) {
            synchronized(lock) { job.workers.remove(w); job.alive--; seg?.worker = false }
            fail(job, Reason.STORAGE, 0)
        }
    }

    private fun workerExit(job: Job, w: Worker) {
        synchronized(lock) { job.workers.remove(w); job.alive--; w.seg?.worker = false }
        control.execute { afterWorkerExit(job) }
    }

    private fun afterWorkerExit(job: Job) {
        tryFinishStop(job)
        if (synchronized(lock) { job.rec.state == DlState.RUNNING && !job.stop }) plan(job)
    }

    private fun runWorker(job: Job, w: Worker) {
        try {
            var seg = w.seg
            var first: Opened? = null
            if (seg == null) {
                val p = probe(job, w)
                seg = p.first; first = p.second
                w.seg = seg
            }
            segmentLoop(job, w, seg, first)
        } catch (e: Stopped) { /* the stop handler owns the state */ }
        catch (e: Exit) { /* state already set */ }
        catch (e: SinkException) { fail(job, e.reason, 0) }
        catch (e: Throwable) { fail(job, Reason.NETWORK, 0) }
    }

    private fun headersFor(rec: Rec): Map<String, String> { val m = HashMap<String, String>(rec.headers); m["Accept-Encoding"] = "identity"; return m }

    private fun quiet(r: FetchResponse?) {
        if (r == null) return
        try { r.abort() } catch (e: Exception) { /* ignore */ }
        try { r.close() } catch (e: Exception) { /* ignore */ }
    }

    private fun checkStop(job: Job) { if (job.stop) throw Stopped() }

    private fun fetcherFor(job: Job): Fetcher = fetchers(job.rec.fetcher) ?: fetchers(0) ?: run { fail(job, Reason.STORAGE, 0); throw Exit() }

    /** The first request: learns size, validators and range support. Returns the first segment and its (already open) reply. */
    private fun probe(job: Job, w: Worker): Pair<Seg, Opened> {
        val rec = job.rec
        var attempt = 0
        while (true) {
            checkStop(job)
            val f = fetcherFor(job)
            val win = f.probeWindow
            val resp = try {
                f.open(FetchRequest(rec.url, if (win > 0) 0 else -1, if (win > 0) win - 1 else -1, null, headersFor(rec), priv = rec.priv))
            } catch (e: FetchException) { fetchFailure(job, e); transient(job, null, attempt++, 0, 0); continue }
            w.resp = resp
            if (job.stop) { quiet(resp); throw Stopped() }
            val now = clock.now()
            val c = HttpSemantics.classify(resp.status, if (win > 0) 0 else -1, -1, null, now, resp::header)
            when (c.verdict) {
                HttpSemantics.Verdict.FULL_200, HttpSemantics.Verdict.OK_206 -> {}
                HttpSemantics.Verdict.RETRY -> { quiet(resp); transient(job, null, attempt++, c.retryAfterMs, resp.status); continue }
                else -> { quiet(resp); fail(job, Reason.HTTP, resp.status); throw Exit() }
            }
            val ranged = resp.status == 206
            val cr = if (ranged) HttpSemantics.parseContentRange(resp.header("Content-Range")) else null
            val encoded = HttpSemantics.isEncoded(resp.header("Content-Encoding"))
            val total = when {
                encoded -> -1L
                ranged -> cr?.total ?: -1L
                else -> resp.header("Content-Length")?.trim()?.toLongOrNull() ?: -1L
            }
            val acceptRanges = resp.header("Accept-Ranges")?.trim()?.lowercase()
            val ranges = when { encoded || total < 0 -> 0; ranged || acceptRanges == "bytes" -> 1; acceptRanges == "none" -> 0; else -> -1 }
            val ctype = resp.header("Content-Type")?.substringBefore(';')?.trim()?.takeIf { it.isNotEmpty() }
            val etag = resp.header("ETag")?.trim()?.takeIf { HttpSemantics.isStrongEtag(it) }
            val lm = resp.header("Last-Modified")?.trim()
            val date = resp.header("Date")?.trim()
            val wantsBinary = ContentSniff.expectsBinary(rec.name, rec.mime) || ContentSniff.expectsBinary(rec.url, null)
            if (wantsBinary && ContentSniff.isHtmlType(ctype)) { quiet(resp); fail(job, Reason.BAD_CONTENT, 0); throw Exit() } // an honest "text/html" where a video or file was expected: nothing is created
            synchronized(lock) { job.expectBinary = wantsBinary; job.sniffed = false }
            val name = if (rec.nameFixed) FileNames.withExtensionFor(ctype ?: rec.mime, rec.name) else FileNames.choose(resp.header("Content-Disposition"), resp.finalUrl, ctype ?: rec.mime, rec.name)
            val sink = try { sinks.create(name, ctype ?: rec.mime, total, rec.priv, rec.destKind) } catch (e: SinkException) { quiet(resp); fail(job, e.reason, 0); throw Exit() }
            val seg = Seg(0, 0, total, 0)
            seg.worker = true
            val reqEnd = if (ranged) (cr?.last ?: -2L) + 1 else -1L
            seg.live = !ranged
            seg.reqEnd = reqEnd
            synchronized(lock) {
                rec.finalUrl = resp.finalUrl; rec.finalAt = now; rec.name = sink.displayName ?: name; rec.mime = ctype ?: rec.mime; rec.total = total; rec.ranges = ranges
                rec.etag = etag
                rec.lastMod = if (etag == null && HttpSemantics.isStrongLastModified(lm, date)) lm else null
                rec.dateHdr = date
                rec.destUri = sink.uri; rec.segs.clear(); rec.segs.add(seg); rec.done = 0
                job.sink = sink; job.useOriginal = false; rec.state = DlState.RUNNING; job.lastBytesAt = now
            }
            control.execute { checkpoint(job); plan(job) } // the record now has its file and segment: durable before the first byte
            changed(rec.id)
            return seg to Opened(resp, reqEnd)
        }
    }

    private fun segmentLoop(job: Job, w: Worker, seg: Seg, first0: Opened?) {
        var first = first0
        while (true) {
            checkStop(job)
            val cursor: Long
            val end: Long
            synchronized(lock) { cursor = seg.cursor; end = seg.end }
            if (end >= 0 && cursor >= end) break
            val opened: Opened
            if (first != null) { opened = first; first = null }
            else {
                if (synchronized(lock) { job.alive > job.climb.target && !seg.live && job.rec.state == DlState.RUNNING }) return // fewer connections wanted: leave the segment for later
                val win = (fetchers(job.rec.fetcher)?.windowBytes ?: 0L).takeIf { it > 0 } ?: cfg.windowBytes
                val reqEnd = if (end >= 0) minOf(end, cursor + Planner.window(seg, win)) else -1L
                try { opened = openWindow(job, w, seg, cursor, reqEnd) } catch (r: Retry) { continue }
            }
            synchronized(lock) { seg.reqEnd = opened.reqEnd }
            w.resp = opened.resp
            try {
                if (opened.vLen > 0 && !sameTail(job, opened)) { quiet(opened.resp); needDecision(job, Reason.FILE_CHANGED); throw Exit() }
                pump(job, seg, opened)
            }
            catch (e: IOException) { quiet(opened.resp); transient(job, seg, 0, 0, 0); continue }
            quiet(opened.resp)
        }
        onSegmentDone(job)
    }

    /** A Range request for [cursor, reqEnd); checks what came back and either returns a usable 206 or decides what the job must do instead. */
    private fun openWindow(job: Job, w: Worker, seg: Seg, cursor: Long, reqEnd: Long): Opened {
        val rec = job.rec
        val url: String; val etag: String?; val lm: String?; val total: Long
        synchronized(lock) { url = if (job.useOriginal) rec.url else (rec.finalUrl ?: rec.url); etag = rec.etag; lm = rec.lastMod; total = rec.total }
        val cond = etag?.let { Cond.IfMatch(it) } ?: lm?.let { Cond.IfUnmodifiedSince(it) }
        // No validator at all (no strong ETag, no usable Last-Modified): nothing proves the server still holds the same file, so the request starts up to 4 KiB BEFORE the cursor and the bytes
        // that come back are compared with what is already in the file (Chrome on Android does the same). A mismatch asks the owner instead of splicing two different files.
        val verifyLen = if (cond == null && cursor > 0 && job.sink?.readable == true) minOf(cursor, VERIFY_BYTES.toLong()).toInt() else 0
        val reqFrom = cursor - verifyLen
        val resp = try {
            fetcherFor(job).open(FetchRequest(url, reqFrom, if (reqEnd >= 0) reqEnd - 1 else -1, cond, headersFor(rec), priv = rec.priv))
        } catch (e: FetchException) { fetchFailure(job, e); transient(job, seg, 0, 0, 0); throw Retry() }
        w.resp = resp
        if (job.stop) { quiet(resp); throw Stopped() }
        val now = clock.now()
        val c = HttpSemantics.classify(resp.status, reqFrom, total, etag, now, resp::header)
        when (c.verdict) {
            HttpSemantics.Verdict.OK_206 -> {
                // A server that ignores If-Unmodified-Since still states the date of what it sends: when the file is held by its Last-Modified and the reply names another date, the bytes
                // belong to a different file and must not be spliced onto the partial one (the owner decides: Restart or Cancel).
                val replyLm = resp.header("Last-Modified")?.trim()
                if (lm != null && replyLm != null && replyLm != lm) { quiet(resp); needDecision(job, Reason.FILE_CHANGED); throw Exit() }
                synchronized(lock) { rec.ranges = 1; if (job.useOriginal) { job.useOriginal = false; rec.finalUrl = resp.finalUrl; rec.finalAt = now } }
                return Opened(resp, reqEnd, reqFrom, verifyLen)
            }
            HttpSemantics.Verdict.FULL_200 -> return fullReply(job, seg, resp)
            HttpSemantics.Verdict.CHANGED -> { quiet(resp); needDecision(job, Reason.FILE_CHANGED); throw Exit() }
            HttpSemantics.Verdict.BAD_RANGE -> {
                quiet(resp)
                if (resp.status == 416 && total > 0 && cursor >= total) { synchronized(lock) { seg.end = seg.cursor }; throw Retry() } // nothing left to ask for
                noRange(job, seg); throw Exit()
            }
            HttpSemantics.Verdict.GONE -> {
                quiet(resp)
                val again = synchronized(lock) { !job.reResolved && !job.useOriginal }
                if (again) { synchronized(lock) { job.reResolved = true; job.useOriginal = true }; throw Retry() } // the signed link may have expired: ask the clicked URL again
                fail(job, if (resp.status == 404) Reason.HTTP else Reason.LINK_EXPIRED, resp.status); throw Exit()
            }
            HttpSemantics.Verdict.RETRY -> { quiet(resp); transient(job, seg, 0, c.retryAfterMs, resp.status); throw Retry() }
            HttpSemantics.Verdict.FATAL_HTTP -> { quiet(resp); fail(job, Reason.HTTP, resp.status); throw Exit() }
        }
    }

    /** Reads the [Opened.vLen] bytes the server sent before the resume point and compares them with the same bytes in the file. A network error is an IOException (retried); a difference is false. */
    private fun sameTail(job: Job, o: Opened): Boolean {
        val body = o.resp.body ?: throw IOException("empty reply")
        val sink = job.sink ?: throw IOException("no file")
        val theirs = ByteArray(o.vLen)
        var got = 0
        while (got < o.vLen) {
            if (job.stop) { o.resp.abort(); throw Stopped() }
            val n = try { body.read(theirs, got, o.vLen - got) } catch (e: IOException) { if (job.stop) throw Stopped(); throw e }
            if (n < 0) return false // the file got shorter than what we already hold
            got += n
        }
        val ours = ByteArray(o.vLen)
        var have = 0
        while (have < o.vLen) { val n = sink.pread(o.vFrom + have, ours, have, o.vLen - have); if (n <= 0) return true; have += n } // a sink that cannot read: no verdict, carry on as before
        return theirs.contentEquals(ours)
    }

    /** A plain 200 to a Range request: this server does not do ranges. */
    private fun fullReply(job: Job, seg: Seg, resp: FetchResponse): Opened {
        val rec = job.rec
        val onlySeg: Boolean
        synchronized(lock) { rec.ranges = 0; onlySeg = rec.segs.size == 1 && seg.cursor == 0L }
        if (onlySeg) { synchronized(lock) { seg.live = true }; return Opened(resp, -1) } // nothing written yet: the full reply is simply the download
        quiet(resp)
        noRange(job, seg)
        throw Exit()
    }

    private fun noRange(job: Job, seg: Seg) {
        val undone = synchronized(lock) { job.rec.ranges = 0; seg.idx > 0 && Planner.undoSplit(job.rec.segs, seg) }
        if (!undone) needDecision(job, Reason.NO_RANGE) // a partial file that cannot be continued: ask before spending the data again
    }

    private fun pump(job: Job, seg: Seg, o: Opened) {
        val body = o.resp.body ?: throw IOException("empty reply")
        val sink = job.sink ?: throw IOException("no file")
        val buf = ByteArray(64 * 1024)
        while (true) {
            if (job.stop) { o.resp.abort(); throw Stopped() }
            val limit: Long
            val cursor: Long
            synchronized(lock) { limit = seg.end; cursor = seg.cursor }
            if (limit >= 0 && cursor >= limit) { o.resp.abort(); return }
            val want = if (limit >= 0) minOf(buf.size.toLong(), limit - cursor).toInt() else buf.size
            val n = try { body.read(buf, 0, want) } catch (e: IOException) { if (job.stop) throw Stopped(); throw e }
            if (n < 0) {
                synchronized(lock) {
                    if (seg.end < 0) { seg.end = seg.cursor; job.rec.total = seg.cursor } // an open-ended reply ended: that was the whole file
                    else if (seg.cursor < seg.end && (o.reqEnd < 0 || seg.cursor < o.reqEnd)) throw IOException("the connection ended early")
                }
                return
            }
            if (n == 0) continue
            limiter.acquire(n) { job.stop }                                                 // Settings > Downloads > Speed limit
            if (job.stop) { o.resp.abort(); throw Stopped() }
            if (cursor == 0L && !job.sniffed) { // the very first bytes of the file: a server that sends a web page as "application/octet-stream" is still caught
                job.sniffed = true
                if (job.expectBinary && ContentSniff.looksLikeHtml(buf, 0, n)) { fail(job, Reason.BAD_CONTENT, 0); throw Exit() }
            }
            sink.pwrite(cursor, buf, 0, n)
            onBytes(job, seg, n)
        }
    }

    private fun onBytes(job: Job, seg: Seg, n: Int) {
        val now = clock.now()
        var notify = false
        var ckpt = false
        val rec = job.rec
        synchronized(lock) {
            seg.cursor += n; seg.fails = 0; seg.failedAt = 0; seg.lastAt = now; rec.done += n; rec.lastProgress = now
            if (rec.crashes != 0) rec.crashes = 0
            if (job.outageSince != 0L) { job.outageSince = 0L; job.outageProbes = 0 }
            job.meter.add(n, now); job.lastBytesAt = now; job.sinceCkpt += n
            if (now - job.lastNotify >= 250) { job.lastNotify = now; notify = true }
            if (!job.ckptPending && (now - job.lastCkpt >= cfg.checkpointMs || job.sinceCkpt >= cfg.checkpointBytes)) { job.ckptPending = true; ckpt = true }
        }
        if (notify) changed(rec.id)
        if (ckpt) control.execute { checkpoint(job) }
    }

    private fun onSegmentDone(job: Job) {
        val complete = synchronized(lock) { job.rec.state == DlState.RUNNING && !job.stop && job.rec.segs.all { it.done } }
        if (complete) completeJob(job)
    }

    private fun fetchFailure(job: Job, e: FetchException) {
        when (e.kind) {
            FetchException.Kind.TLS -> { fail(job, Reason.TLS, 0); throw Exit() }
            FetchException.Kind.INSECURE_REDIRECT -> { fail(job, Reason.INSECURE_REDIRECT, 0); throw Exit() }
            FetchException.Kind.LOCAL_NET -> { fail(job, Reason.LOCAL_NET, 0); throw Exit() }
            FetchException.Kind.BAD_URL -> { fail(job, Reason.BAD_URL, 0); throw Exit() }
            else -> {}
        }
    }

    /**
     * A failure worth retrying: wait with jittered backoff, give up after [Config.maxFails] in a row, hand over to WAITING_NETWORK when the network is gone. Two refinements for a flaky,
     * capped hotspot link: a connection that keeps failing while ANOTHER one keeps moving bytes is the extra one the server or carrier refuses, so it retires alone and the download carries on
     * with one connection fewer (it used to fail the whole download); and when the only connection keeps failing in the middle of a download that can continue, the download WAITS and looks again
     * with growing pauses instead of failing after a minute or two.
     */
    private fun transient(job: Job, seg: Seg?, attempt: Int, retryAfterMs: Long, status: Int) {
        if (!net.online) { requestStop(job, DlState.WAITING_NETWORK); throw Exit() }
        val now = clock.now()
        val n = if (seg != null) synchronized(lock) { if (seg.fails == 0) seg.failedAt = now; ++seg.fails } else attempt + 1
        if (seg != null && n >= 2 && retireExtra(job, seg, now)) throw Exit()
        if (n >= cfg.maxFails) {
            if (status == 0 && waitOutOutage(job, now)) throw Exit()
            fail(job, if (status != 0) Reason.HTTP else Reason.NETWORK, status); throw Exit()
        }
        sleep(job, Backoff.delayMs(n - 1, cfg.backoffBaseMs, cfg.backoffCapMs, retryAfterMs, rnd))
    }

    /** True when [seg]'s connection should simply stop: another connection of this job moved bytes at least 2 s AFTER this one began failing (so it is not a dip that hit them all). */
    private fun retireExtra(job: Job, seg: Seg, now: Long): Boolean = synchronized(lock) {
        if (job.alive < 2 || job.rec.state != DlState.RUNNING || seg.failedAt == 0L) return false
        val healthy = job.rec.segs.any { it !== seg && it.worker && it.lastAt > seg.failedAt + cfg.refusedEvidenceMs && now - it.lastAt < cfg.refusedFreshMs }
        if (!healthy) return false
        job.climb.refused(now, job.alive - 1, cfg.refusedNoGrowthMs)
        seg.fails = 0; seg.failedAt = 0
        true
    }

    /** The only connection of a started download keeps failing at the transport level: wait and look again later, with growing pauses (the 30-minute give-up is in [doTick]). */
    private fun waitOutOutage(job: Job, now: Long): Boolean {
        val rec = job.rec
        synchronized(lock) {
            if (!(rec.done > 0 && rec.resumable) || job.alive > 1) return false // a fresh download (a typo in the address, a dead host) fails fast as before
            if (job.outageSince == 0L) job.outageSince = now
            job.outageProbes++
            job.resumeAt = now + Backoff.delayMs(job.outageProbes - 1, cfg.outageBaseMs, cfg.outageCapMs, 0L, rnd)
        }
        requestStop(job, DlState.WAITING_NETWORK, Reason.NETWORK)
        return true
    }

    private fun sleep(job: Job, ms: Long) {
        var nanos = TimeUnit.MILLISECONDS.toNanos(ms)
        job.mon.lock()
        try { while (!job.stop) { if (nanos <= 0) return; nanos = job.wake.awaitNanos(nanos) } } finally { job.mon.unlock() }
        throw Stopped()
    }

    // ================================================================================================ stopping, checkpoints, completion

    private fun fail(job: Job, reason: Reason, detail: Int) = requestStop(job, DlState.FAILED, reason, detail)
    private fun needDecision(job: Job, reason: Reason) = requestStop(job, DlState.NEEDS_DECISION, reason)

    /** Ask every worker of the job to stop; the state is shown at once, the checkpoint happens when the last worker is gone ([tryFinishStop]). */
    private fun requestStop(job: Job, st: DlState, reason: Reason = Reason.NONE, detail: Int = 0) {
        val rec = job.rec
        synchronized(lock) {
            if (rec.state == DlState.COMPLETED || rec.state == DlState.CANCELED || job.stopState == DlState.CANCELED) return
            job.stopState = st; job.stopReason = reason; job.stopDetail = detail; job.stop = true
            rec.state = st; rec.reason = reason; rec.detail = detail
            for (w in job.workers) try { w.resp?.abort() } catch (e: Exception) { /* ignore */ }
        }
        job.mon.lock(); try { job.wake.signalAll() } finally { job.mon.unlock() }
        changed(rec.id)
        control.execute { tryFinishStop(job) }
    }

    private fun tryFinishStop(job: Job) {
        val rec = job.rec
        val st: DlState
        val reason: Reason
        val detail: Int
        synchronized(lock) {
            if (job.alive > 0) return
            st = job.stopState ?: return
            reason = job.stopReason; detail = job.stopDetail
            job.stopState = null
        }
        if (st == DlState.CANCELED) {
            val uri = synchronized(lock) { rec.destUri }
            try { if (job.sink != null) job.sink?.discard() else if (uri != null) sinks.delete(uri) } catch (e: Exception) { /* ignore */ }
            synchronized(lock) { jobs.remove(rec.id); rec.headers = emptyMap() }
            if (!rec.priv) store.delete(rec.id)
            changed(rec.id)
            startQueued()
            return
        }
        if (st == DlState.FAILED && reason == Reason.BAD_CONTENT) synchronized(lock) { wipePartial(job) } // nothing worth keeping: the "file" is a web page
        val again: Boolean
        synchronized(lock) {
            rec.state = st; rec.reason = reason; rec.detail = detail
            if (st == DlState.FAILED) rec.finished = clock.now()
            again = job.restartAfterStop
            job.restartAfterStop = false
        }
        checkpoint(job)
        if (again) synchronized(lock) { rec.state = DlState.QUEUED; rec.reason = Reason.NONE; rec.detail = 0 }
        changed(rec.id)
        startQueued()
    }

    /**
     * Every snapshot gets a number when it is taken (under [lock]); the store only ever takes a newer one than the last it saved for that download. A checkpoint snapshots, then fsyncs for a while,
     * then saves: without the number a finished download could be overwritten by that older "verifying" snapshot (the row then claimed the file was unfinished).
     */
    private val snapSeq = AtomicLong()
    private val savedSeq = HashMap<Long, Long>()
    private fun saveSnapshot(id: Long, n: Long, snap: Rec) = synchronized(savedSeq) { if (n > (savedSeq[id] ?: 0L)) { store.save(snap); savedSeq[id] = n } }

    private fun persist(job: Job) {
        if (job.rec.priv) return
        val n: Long; val snap: Rec
        synchronized(lock) { n = snapSeq.incrementAndGet(); snap = job.rec.snapshot() }
        saveSnapshot(job.rec.id, n, snap)
    }

    /** Data first, metadata second: the offsets saved here never run ahead of bytes that were fsynced. */
    private fun checkpoint(job: Job) {
        val rec = job.rec
        val n: Long
        val snap = synchronized(lock) { job.ckptPending = false; job.lastCkpt = clock.now(); job.sinceCkpt = 0; rec.updated = job.lastCkpt; n = snapSeq.incrementAndGet(); rec.snapshot() }
        try { job.sink?.sync() } catch (e: SinkException) { /* a failing disk shows up on the next write */ }
        if (!rec.priv) saveSnapshot(rec.id, n, snap)
    }

    private fun completeJob(job: Job) {
        val rec = job.rec
        synchronized(lock) { if (job.completing || job.stop) return; job.completing = true; rec.state = DlState.VERIFYING }
        changed(rec.id)
        try {
            val sink = job.sink ?: throw SinkException(Reason.STORAGE, "no file")
            sink.sync()
            val sum = synchronized(lock) { rec.segs.sumOf { it.cursor - it.start } }
            if (rec.total > 0 && sum != rec.total) throw SinkException(Reason.STORAGE, "size mismatch $sum != ${rec.total}")
            val uri = sink.publish()
            synchronized(lock) {
                rec.destUri = uri ?: rec.destUri; rec.done = sum; rec.total = if (rec.total < 0) sum else rec.total
                rec.state = DlState.COMPLETED; rec.reason = Reason.NONE; rec.detail = 0
                rec.finished = clock.now(); rec.updated = rec.finished; rec.headers = emptyMap(); rec.segs.clear()
            }
            persist(job)
            changed(rec.id)
            control.execute { startQueued() }
        } catch (e: SinkException) { fail(job, e.reason, 0) }
    }

    // ================================================================================================ views

    private fun view(job: Job): DownloadView {
        val r = job.rec
        val now = clock.now()
        val running = r.state == DlState.RUNNING
        val speed = if (running) job.meter.bytesPerSec(now).toLong() else 0L
        val eta = if (running && speed > 0 && r.total > 0) (r.total - r.done) / speed else -1L
        val actions: List<Action> = when (r.state) {
            DlState.QUEUED, DlState.STARTING -> listOf(Action.CANCEL)
            DlState.RUNNING -> if (r.resumable) listOf(Action.PAUSE, Action.CANCEL) else listOf(Action.CANCEL)
            DlState.PAUSED_USER, DlState.PAUSED_SYSTEM -> listOf(Action.RESUME, Action.CANCEL)
            DlState.WAITING_NETWORK -> if (r.resumable) listOf(Action.PAUSE, Action.CANCEL) else listOf(Action.CANCEL)
            DlState.NEEDS_DECISION -> listOf(Action.RESTART, Action.CANCEL)
            DlState.VERIFYING -> emptyList()
            DlState.FAILED -> listOf(Action.RETRY, Action.DELETE)
            DlState.COMPLETED -> listOf(Action.OPEN, Action.SHARE, Action.DELETE)
            DlState.CANCELED -> emptyList()
        }
        val host = try { java.net.URI(r.url).host } catch (e: Exception) { null }
        return DownloadView(r.id, r.state, r.reason, r.detail, r.name, r.mime, host, r.total, r.done, speed, eta, job.alive, r.priv, r.destUri, r.created, r.updated, actions, r.destKind)
    }
}
