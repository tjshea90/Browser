package app.kestrel.core.media

import app.kestrel.core.download.Action
import app.kestrel.core.download.Clock
import app.kestrel.core.download.Config
import app.kestrel.core.download.DEST_PUBLIC
import app.kestrel.core.download.DlState
import app.kestrel.core.download.DownloadView
import app.kestrel.core.download.FetchException
import app.kestrel.core.download.FetchRequest
import app.kestrel.core.download.FetchResponse
import app.kestrel.core.download.Fetcher
import app.kestrel.core.download.FileNames
import app.kestrel.core.download.Meter
import app.kestrel.core.download.NetGate
import app.kestrel.core.download.Reason
import app.kestrel.core.download.Sink
import app.kestrel.core.download.SinkException
import app.kestrel.core.download.SinkFactory
import app.kestrel.core.download.Spawner
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.BitSet
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantLock
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/** What the caller hands to [StreamEngine.enqueue]: one HLS media playlist (the quality the user picked) and how to save it. */
class StreamSource(
    val playlistUrl: String,
    /** The playlist text when the caller has just read it (saves a request); null = the engine fetches it. */
    val playlistText: String?,
    val pageUrl: String?,
    /** File name without extension ("Page title - 720p"); the extension comes from the stream's container. */
    val nameStem: String,
    val headers: Map<String, String> = emptyMap(),
    val fetcher: Int = 0,
    val priv: Boolean = false,
    val meteredOk: Boolean = false,
    val destKind: Int = DEST_PUBLIC,
    /** A size guess for the progress bar before the first segments arrive (bit rate x length), -1 = none. */
    val estimateBytes: Long = -1,
    /** DASH: which Representation of the manifest at [playlistUrl] to download. */
    val repId: String? = null,
    /** Force the file extension (DASH audio: "m4a"); null = from the stream's container. */
    val ext: String? = null,
    /** The sound of this picture, downloaded alongside it as a hidden job and merged into one MP4 at the end (DASH keeps them apart). */
    val companion: StreamSource? = null,
)

/** One stream download as stored (everything needed to carry on after the app was killed: the playlist text is kept, so the segment list is the same on resume). */
class StreamRec(@JvmField val id: Long) {
    @JvmField var state = DlState.QUEUED
    @JvmField var reason = Reason.NONE
    @JvmField var detail = 0
    @JvmField var url = ""
    @JvmField var playlistText: String? = null
    @JvmField var pageUrl: String? = null
    @JvmField var headers: Map<String, String> = emptyMap()
    @JvmField var fetcher = 0
    @JvmField var priv = false
    @JvmField var meteredOk = false
    @JvmField var nameStem = ""
    @JvmField var name = ""
    @JvmField var mime: String? = null
    @JvmField var destKind = DEST_PUBLIC
    @JvmField var destUri: String? = null
    @JvmField var estimate = -1L
    @JvmField var repId: String? = null
    @JvmField var ext: String? = null
    /** A picture job names its sound job here; a sound job names its picture job in [parentId] and is not listed. */
    @JvmField var companionId = 0L
    @JvmField var parentId = 0L
    /** Bytes of the output written (and flushed) so far, and the index of the next segment to write. */
    @JvmField var appended = 0L
    @JvmField var next = 0
    /** Where each run after a #EXT-X-DISCONTINUITY starts in the output, as "segment index:byte offset;" pairs (only for what has been written): the remuxer needs them to lay the times end to end. */
    @JvmField var breaks = ""
    /** Restarts in a row in which the process ended in the middle of this download before any segment was written (auto-resume stops after 2). */
    @JvmField var crashes = 0
    @JvmField var initDone = false
    @JvmField var total = -1L
    @JvmField var created = 0L
    @JvmField var updated = 0L
    @JvmField var finished = 0L

    fun snapshot(): StreamRec {
        val r = StreamRec(id)
        r.state = state; r.reason = reason; r.detail = detail; r.url = url; r.playlistText = playlistText; r.pageUrl = pageUrl; r.headers = headers; r.fetcher = fetcher; r.priv = priv
        r.meteredOk = meteredOk; r.nameStem = nameStem; r.name = name; r.mime = mime; r.destKind = destKind; r.destUri = destUri; r.estimate = estimate; r.repId = repId; r.ext = ext; r.companionId = companionId; r.parentId = parentId; r.appended = appended
        r.next = next; r.breaks = breaks; r.crashes = crashes; r.initDone = initDone; r.total = total; r.created = created; r.updated = updated; r.finished = finished
        return r
    }
}

/**
 * Turns the finished file of a stream into a nicer one (MPEG-TS -> MP4). Runs on the job's supervisor thread while the download shows "Finishing"; it may take a while and may refuse
 * (return null): the stream is then simply kept as it is. The Android layer implements it with MediaExtractor + MediaMuxer.
 */
interface StreamFinisher {
    /**
     * One run of the joined file between two #EXT-X-DISCONTINUITY marks: the bytes [offset, offset + length) and the length the playlist gave it. A discontinuity means the time stamps start over
     * (a new encoder run: spliced-in advertising, a re-joined movie), so each part is read on its own and the times are laid end to end.
     */
    class Part(val offset: Long, val length: Long, val durationMs: Long)
    /** [parts] has more than one entry only when the playlist marked discontinuities; empty = the file is one run. */
    class Joined(val uri: String, val stem: String, val container: String, val size: Long, val priv: Boolean, val destKind: Int, val parts: List<Part> = emptyList())
    class Converted(val uri: String, val name: String, val mime: String, val size: Long)
    /** Two finished files of one video (picture and sound, as DASH keeps them) to be made one MP4. */
    class Merge(val videoUri: String, val audioUri: String, val stem: String, val size: Long, val priv: Boolean, val destKind: Int)
    fun convert(j: Joined): Converted?
    fun merge(m: Merge): Converted? = null
}

/** Where job records and staged segments live. The Android layer keeps them in the app's own folder; tests use a temp folder. */
interface StreamStore {
    fun loadAll(): List<StreamRec>
    fun save(r: StreamRec)
    fun delete(id: Long)
    /** The folder for this job's staged segments (created on demand). */
    fun dir(id: Long): File
}

/**
 * Downloads a segmented stream (HLS) into ONE file: several segments are fetched at once into the app's staging folder, then appended to the output strictly in playlist order (decrypting AES-128
 * on the way), so the final file is the stream, whole. Pause / resume / kill-and-resume work at segment granularity: a finished segment is never fetched twice, a half-fetched one starts over.
 * Same ports, states and notification path as the single-file [app.kestrel.core.download.Engine] (it shows in the same Downloads list). Design: research/VIDEO_DOWNLOADER.md sections 4 and 7.
 *
 * Threads: commands run on [control]; each running job has one supervisor thread (plan, checkpoints, stall watchdog, finish) and up to [Config.maxConnsPerDownload] fetch workers.
 */
class StreamEngine(
    private val fetchers: (Int) -> Fetcher?,
    private val sinks: SinkFactory,
    private val store: StreamStore,
    private val net: NetGate,
    private val control: Executor,
    private val clock: Clock = Clock.System,
    private val cfg: Config = Config(),
    private val spawner: Spawner = Spawner { name, task -> Thread(task, name).also { it.isDaemon = true; it.start() } },
    private val finisher: StreamFinisher? = null,
    /** Where unexpected failures are written down (the Android layer: logcat); the engine itself only turns them into a failed download. */
    private val log: (String, Throwable?) -> Unit = { _, _ -> },
    /** The first id handed out (the Android layer keeps these apart from the single-file engine's). */
    private val idBase: Long = 1_000_000_000L,
) {
    interface Listener { fun onChanged(id: Long) }

    private class Stopped : RuntimeException(null, null, false, false)
    private class Failed(val reason: Reason, val detail: Int = 0) : RuntimeException(null, null, false, false)

    private inner class Job(val rec: StreamRec) {
        @Volatile var autoResume = false   // the last run ended in the middle of this download: carry on by itself ([resumeInterrupted])
        @Volatile var stop = false
        @Volatile var stopState: DlState? = null
        @Volatile var failure: Failed? = null
        var media: Hls.Media? = null
        val staged = BitSet()
        val claimed = BitSet()
        var stagedBytes = 0L
        var stagedCount = 0
        val inflight = AtomicLong()
        var written = 0L
        var sink: Sink? = null
        val meter = Meter()
        var alive = 0
        val responses = CopyOnWriteArrayList<FetchResponse>()
        val drain = ReentrantLock()
        val keys = HashMap<Int, ByteArray>()
        @Volatile var lastBytesAt = 0L
        var lastCkpt = 0L
        var running = false             // a supervisor thread exists for this job
        var restartAfterStop = false
        var wipeAfterStop = false
        var base = 0L                   // bytes already on the tablet when this run started (output + staged segments)
    }

    private val lock = Any()
    private val jobs = LinkedHashMap<Long, Job>()
    private val nextId = AtomicLong(idBase)
    private val nextPrivId = AtomicLong(-idBase)
    private val listeners = CopyOnWriteArrayList<Listener>()
    private val rnd = java.util.Random()

    fun addListener(l: Listener) { listeners.add(l) }
    private fun changed(id: Long) { for (l in listeners) try { l.onChanged(id) } catch (e: Exception) { /* a listener must not break the engine */ } }

    fun owns(id: Long): Boolean = id >= idBase || id <= -idBase

    // ================================================================================================ public API

    /** Load what the last run left behind (call once, before anything else). Interrupted jobs come back PAUSED_SYSTEM: never started by themselves. */
    fun restore() {
        val recs = store.loadAll()
        synchronized(lock) {
            var max = idBase - 1
            for (r in recs) {
                if (r.id > max) max = r.id
                if (r.state == DlState.CANCELED) { store.delete(r.id); continue }
                val wanted = r.state == DlState.QUEUED || r.state.active || r.state == DlState.WAITING_NETWORK || r.state == DlState.PAUSED_SYSTEM
                val unclean = r.state.active // the process ended while it was working
                if (!r.state.terminal && r.state != DlState.PAUSED_USER) r.state = DlState.PAUSED_SYSTEM
                if (unclean) r.crashes++
                jobs[r.id] = Job(r).also { it.autoResume = wanted && r.crashes <= 2 }
            }
            // a sound job whose picture job is gone is an ordinary download; a picture job whose sound job is gone has no companion
            for (j in jobs.values) {
                if (j.rec.parentId != 0L && !jobs.containsKey(j.rec.parentId)) { j.rec.parentId = 0L; persist(j.rec) }
                if (j.rec.companionId != 0L && !jobs.containsKey(j.rec.companionId)) { j.rec.companionId = 0L; persist(j.rec) }
            }
            nextId.set(max + 1)
        }
    }

    /** After [restore]: stream downloads the last run was in the middle of carry on by themselves; paused ones stay paused; a download whose restarts keep ending the process before a segment is written is left for a tap. */
    fun resumeInterrupted() = control.execute {
        val ids = synchronized(lock) { jobs.values.filter { it.autoResume && it.rec.state == DlState.PAUSED_SYSTEM }.map { it.rec.id } }
        for (id in ids) {
            synchronized(lock) { jobs[id]?.autoResume = false }
            for (job in get(id)?.let { family(it) } ?: continue) if (synchronized(lock) { job.rec.state } == DlState.PAUSED_SYSTEM) requeue(job, wipe = false)
        }
    }

    fun enqueue(s: StreamSource): Long {
        val id = if (s.priv) nextPrivId.getAndDecrement() else nextId.getAndIncrement()
        val cid = if (s.companion != null) (if (s.priv) nextPrivId.getAndDecrement() else nextId.getAndIncrement()) else 0L
        control.execute {
            val r = newRec(id, s)
            r.companionId = cid
            persist(r)
            synchronized(lock) { jobs[id] = Job(r) }
            val c = s.companion
            if (c != null) { val cr = newRec(cid, c); cr.parentId = id; persist(cr); synchronized(lock) { jobs[cid] = Job(cr) } }
            changed(id)
            startQueued()
        }
        return id
    }

    private fun newRec(id: Long, s: StreamSource): StreamRec {
        val r = StreamRec(id)
        r.url = s.playlistUrl; r.playlistText = s.playlistText; r.pageUrl = s.pageUrl; r.headers = s.headers; r.fetcher = s.fetcher; r.priv = s.priv; r.meteredOk = s.meteredOk
        r.nameStem = FileNames.sanitize(s.nameStem.ifBlank { "video" }); r.name = r.nameStem; r.destKind = s.destKind; r.estimate = s.estimateBytes; r.repId = s.repId; r.ext = s.ext
        r.created = clock.now(); r.updated = r.created
        if (!(s.playlistUrl.startsWith("http://", true) || s.playlistUrl.startsWith("https://", true))) { r.state = DlState.FAILED; r.reason = Reason.BAD_URL; r.finished = r.created }
        return r
    }

    /** The job and, when it has one, its hidden sound job: what the user sees as one download. */
    private fun family(job: Job): List<Job> = synchronized(lock) { listOfNotNull(job, job.rec.companionId.takeIf { it != 0L }?.let { jobs[it] }) }

    fun pause(id: Long) = control.execute {
        for (job in get(id)?.let { family(it) } ?: return@execute) {
            val st = synchronized(lock) { job.rec.state }
            when {
                st == DlState.QUEUED -> { synchronized(lock) { job.rec.state = DlState.PAUSED_USER }; persist(job.rec); changed(job.rec.id) }
                st.active || st == DlState.WAITING_NETWORK -> requestStop(job, DlState.PAUSED_USER)
            }
        }
    }

    fun resume(id: Long) = control.execute {
        for (job in get(id)?.let { family(it) } ?: return@execute) {
            val st = synchronized(lock) { job.rec.state }
            if (st == DlState.PAUSED_USER || st == DlState.PAUSED_SYSTEM || st == DlState.WAITING_NETWORK || st == DlState.FAILED) requeue(job, wipe = false)
        }
    }

    /** Start again from nothing (a failed or stale job): staged segments and the partial output go. */
    fun restart(id: Long) = control.execute { for (job in get(id)?.let { family(it) } ?: return@execute) requeue(job, wipe = true) }

    fun cancel(id: Long) = control.execute {
        for (job in get(id)?.let { family(it) } ?: return@execute) {
            val st = synchronized(lock) { job.rec.state }
            if (st.terminal && st != DlState.CANCELED) { if (job.rec.parentId != 0L || job.rec.companionId != 0L) finishCancel(job); continue }
            if (synchronized(lock) { job.running }) requestStop(job, DlState.CANCELED) else finishCancel(job)
        }
    }

    fun remove(id: Long, deleteFile: Boolean) = control.execute {
        val first = get(id) ?: return@execute
        for (job in family(first).reversed()) removeOne(job, deleteFile)
    }

    private fun removeOne(job: Job, deleteFile: Boolean) {
        val rec = job.rec
        val st = synchronized(lock) { rec.state }
        if (!st.terminal && st != DlState.PAUSED_USER && st != DlState.PAUSED_SYSTEM) return
        val uri = synchronized(lock) { rec.destUri }
        if (uri != null && (deleteFile || st != DlState.COMPLETED)) try { sinks.delete(uri) } catch (e: Exception) { /* already gone */ }
        wipeStaging(job)
        synchronized(lock) { jobs.remove(rec.id) }
        if (!rec.priv) store.delete(rec.id)
        changed(rec.id)
    }

    fun clearFinished() = control.execute {
        for (j in snapshotJobs()) { val st = synchronized(lock) { j.rec.state }; if (j.rec.parentId == 0L && (st == DlState.COMPLETED || st == DlState.FAILED || st == DlState.CANCELED)) { wipeStaging(j); synchronized(lock) { jobs.remove(j.rec.id) }; if (!j.rec.priv) store.delete(j.rec.id); changed(j.rec.id) } }
    }

    fun pauseAll() = control.execute { for (j in snapshotJobs()) if (synchronized(lock) { j.rec.state.active }) requestStop(j, DlState.PAUSED_USER) }
    fun resumeAll() = control.execute { for (j in snapshotJobs()) if (synchronized(lock) { j.rec.state.let { it == DlState.PAUSED_USER || it == DlState.PAUSED_SYSTEM || it == DlState.WAITING_NETWORK } }) requeue(j, wipe = false) }
    fun pauseAllSystem() = control.execute { for (j in snapshotJobs()) if (synchronized(lock) { j.rec.state.active }) requestStop(j, DlState.PAUSED_SYSTEM) }

    /** The network changed ([NetGate.online] has the new value). */
    fun onNetwork() = control.execute {
        for (j in snapshotJobs()) {
            val st = synchronized(lock) { j.rec.state }
            if (!net.online) { if (st.active) requestStop(j, DlState.WAITING_NETWORK) }
            else if (st == DlState.WAITING_NETWORK) requeue(j, wipe = false)
        }
    }

    fun views(): List<DownloadView> = synchronized(lock) { jobs.values.filter { it.rec.parentId == 0L }.map { view(it) } }
    fun view(id: Long): DownloadView? = synchronized(lock) { jobs[id]?.let { view(it) } }
    fun needsService(): Boolean = synchronized(lock) { jobs.values.any { val s = it.rec.state; s == DlState.QUEUED || s.active || s == DlState.WAITING_NETWORK } }
    fun counts(): IntArray = synchronized(lock) {
        var running = 0; var queued = 0; var paused = 0
        for (j in jobs.values) if (j.rec.parentId == 0L) when (j.rec.state) { DlState.RUNNING, DlState.STARTING, DlState.VERIFYING -> running++; DlState.QUEUED -> queued++; DlState.PAUSED_USER, DlState.PAUSED_SYSTEM, DlState.WAITING_NETWORK -> paused++; else -> {} }
        intArrayOf(running, queued, paused)
    }

    // ================================================================================================ control thread

    private fun get(id: Long): Job? = synchronized(lock) { jobs[id] }
    private fun snapshotJobs(): List<Job> = synchronized(lock) { jobs.values.toList() }
    private fun persist(r: StreamRec) { if (!r.priv) try { store.save(r.snapshot()) } catch (e: Exception) { /* the next checkpoint retries */ } }

    private fun startQueued() {
        val go = ArrayList<Job>()
        synchronized(lock) {
            var active = jobs.values.count { it.rec.state.active }
            for (j in jobs.values.sortedBy { it.rec.created }) {
                if (active >= cfg.maxActive) break
                if (j.rec.state == DlState.QUEUED && !j.running && j.stopState == null) { j.rec.state = DlState.STARTING; j.running = true; go.add(j); active++ }
            }
        }
        for (j in go) { changed(j.rec.id); spawn(j) }
    }

    private fun requeue(job: Job, wipe: Boolean) {
        val busy: Boolean
        synchronized(lock) {
            job.rec.reason = Reason.NONE; job.rec.detail = 0
            busy = job.running
            if (!busy && wipe) resetProgress(job)
            job.rec.state = DlState.QUEUED
            if (busy) { job.restartAfterStop = true; job.wipeAfterStop = wipe; job.stop = true }
        }
        if (!busy && wipe) { wipeStaging(job); persist(job.rec) }
        changed(job.rec.id)
        if (!busy) startQueued()
    }

    private fun resetProgress(job: Job) {
        val r = job.rec
        try { job.sink?.discard() } catch (e: Exception) { /* ignore */ }
        if (job.sink == null) r.destUri?.let { try { sinks.delete(it) } catch (e: Exception) { /* ignore */ } }
        job.sink = null; job.media = null; job.staged.clear(); job.claimed.clear(); job.stagedBytes = 0; job.stagedCount = 0; job.inflight.set(0); job.written = 0; job.keys.clear()
        r.destUri = null; r.appended = 0; r.next = 0; r.breaks = ""; r.initDone = false; r.total = -1
    }

    private fun requestStop(job: Job, to: DlState) {
        synchronized(lock) {
            if (!job.running) return
            job.stopState = to; job.stop = true
            for (resp in job.responses) try { resp.abort() } catch (e: Exception) { /* ignore */ }
        }
    }

    private fun finishCancel(job: Job) {
        val rec = job.rec
        val uri = synchronized(lock) { rec.destUri }
        try { job.sink?.discard() } catch (e: Exception) { /* ignore */ }
        if (job.sink == null && uri != null) try { sinks.delete(uri) } catch (e: Exception) { /* ignore */ }
        wipeStaging(job)
        synchronized(lock) { jobs.remove(rec.id) }
        if (!rec.priv) store.delete(rec.id)
        changed(rec.id)
    }

    private fun wipeStaging(job: Job) {
        try { val d = store.dir(job.rec.id); d.listFiles()?.forEach { it.delete() }; d.delete() } catch (e: Exception) { /* ignore */ }
    }

    // ================================================================================================ the supervisor (one thread per running job)

    private fun spawn(job: Job) {
        try { spawner.spawn("stream-${job.rec.id}") { supervise(job) } }
        catch (e: Throwable) { synchronized(lock) { job.running = false; job.rec.state = DlState.FAILED; job.rec.reason = Reason.STORAGE }; persist(job.rec); changed(job.rec.id) }
    }

    private fun supervise(job: Job) {
        val rec = job.rec
        var outcome: DlState = DlState.RUNNING
        try {
            prepare(job)
            if (job.stop) throw Stopped()
            synchronized(lock) { rec.state = DlState.RUNNING; job.lastBytesAt = clock.now(); job.lastCkpt = clock.now(); job.meter.reset() }
            changed(rec.id)
            val n = synchronized(lock) { job.media!!.segments.size }
            val workers = ArrayList<Thread>()
            val want = maxOf(1, minOf(if (rec.parentId != 0L) minOf(2, cfg.maxConnsPerDownload) else cfg.maxConnsPerDownload, n - rec.next))
            for (i in 0 until want) { synchronized(lock) { job.alive++ }; workers.add(spawner.spawn("stream-${rec.id}-w$i") { try { worker(job) } finally { synchronized(lock) { job.alive-- } } }) }
            while (true) {
                val alive = synchronized(lock) { job.alive }
                if (alive == 0) break
                try { Thread.sleep(250) } catch (e: InterruptedException) { /* woken to look again */ }
                val now = clock.now()
                if (job.failure == null && !job.stop && now - job.lastBytesAt > cfg.stallMs && alive > 0) { // no bytes for a long time: cut the sockets, the workers retry
                    job.lastBytesAt = now
                    for (resp in job.responses) try { resp.abort() } catch (e: Exception) { /* ignore */ }
                }
                if (now - job.lastCkpt >= cfg.checkpointMs && !job.stop) checkpoint(job)
                companionFailure(job)?.let { f -> synchronized(lock) { if (job.failure == null) job.failure = f; job.stop = true; for (r in job.responses) try { r.abort() } catch (e: Exception) { /* ignore */ } } }
                changed(rec.id)
            }
            job.failure?.let { throw it }
            if (job.stop) throw Stopped()
            outcome = finish(job)
        } catch (e: Stopped) {
            outcome = synchronized(lock) { if (job.restartAfterStop) DlState.QUEUED else job.stopState ?: DlState.PAUSED_SYSTEM }
        } catch (e: Failed) {
            outcome = DlState.FAILED
            synchronized(lock) { rec.reason = e.reason; rec.detail = e.detail }
            log("stream ${rec.id} failed: ${e.reason} ${e.detail}", null)
        } catch (e: SinkException) {
            outcome = DlState.FAILED
            synchronized(lock) { rec.reason = e.reason }
            log("stream ${rec.id}: the output failed (${e.reason})", e)
        } catch (e: Throwable) {
            outcome = DlState.FAILED
            synchronized(lock) { rec.reason = Reason.STORAGE }
            log("stream ${rec.id}: unexpected failure", e)
        }
        settle(job, outcome)
    }

    /** The job's supervisor is done: record where it ended, free what is not needed, and look at the queue. */
    private fun settle(job: Job, outcome: DlState) {
        val rec = job.rec
        // a stopped job keeps its progress: flush the output so the checkpoint is true
        if (outcome != DlState.COMPLETED && outcome != DlState.CANCELED) try { job.sink?.sync() } catch (e: Exception) { /* the last checkpoint still stands */ }
        control.execute {
            synchronized(lock) { job.running = false; job.stop = false; job.stopState = null; job.failure = null }
            if ((outcome == DlState.FAILED || outcome == DlState.CANCELED) && rec.companionId != 0L) {
                val comp = synchronized(lock) { jobs[rec.companionId] }
                if (comp != null) { if (synchronized(lock) { comp.running }) requestStop(comp, DlState.CANCELED) else finishCancel(comp) }
                synchronized(lock) { rec.companionId = 0L }
            }
            when (outcome) {
                DlState.CANCELED -> { finishCancel(job); startQueued(); return@execute }
                DlState.COMPLETED -> {}
                DlState.QUEUED -> {
                    val wipe = synchronized(lock) { val w = job.wipeAfterStop; job.restartAfterStop = false; job.wipeAfterStop = false; rec.state = DlState.QUEUED; w }
                    if (wipe) { synchronized(lock) { resetProgress(job) }; wipeStaging(job) }
                }
                else -> synchronized(lock) { rec.state = outcome; if (outcome == DlState.FAILED) rec.finished = clock.now() }
            }
            synchronized(lock) { rec.updated = clock.now() }
            persist(rec)
            changed(rec.id)
            startQueued()
        }
    }

    /** Read the playlist, make the plan, open (or reopen) the output, find what an earlier run already staged, and write the init section. */
    private fun prepare(job: Job) {
        val rec = job.rec
        val fetcher = fetchers(rec.fetcher) ?: fetchers(0) ?: throw Failed(Reason.NETWORK)
        if (synchronized(lock) { job.media == null }) {
            var text = synchronized(lock) { rec.playlistText }
            if (text == null) {
                if (!net.online) throw Stopped().also { synchronized(lock) { job.stopState = DlState.WAITING_NETWORK } }
                text = readText(job, fetcher, rec.url)
            }
            val media: Hls.Media = if (text.trimStart('\uFEFF', ' ', '\n', '\r', '\t').startsWith("<")) {
                val man = Dash.parse(text, rec.url) ?: throw Failed(Reason.BAD_PLAYLIST)
                when (man.problem) { Hls.Problem.LIVE -> throw Failed(Reason.LIVE); Hls.Problem.DRM -> throw Failed(Reason.DRM); Hls.Problem.UNSUPPORTED -> throw Failed(Reason.UNSUPPORTED); else -> {} }
                val rep = man.reps.firstOrNull { it.id == (rec.repId ?: it.id) } ?: throw Failed(Reason.BAD_PLAYLIST)
                rep.media ?: throw Failed(Reason.UNSUPPORTED)
            } else (Hls.parse(text, rec.url) as? Hls.MediaPlaylist ?: throw Failed(Reason.BAD_PLAYLIST)).media
            when (media.problem) {
                Hls.Problem.LIVE -> throw Failed(Reason.LIVE)
                Hls.Problem.DRM -> throw Failed(Reason.DRM)
                Hls.Problem.SAMPLE_AES, Hls.Problem.UNSUPPORTED -> throw Failed(Reason.UNSUPPORTED)
                Hls.Problem.EMPTY -> throw Failed(Reason.BAD_PLAYLIST)
                null -> {}
            }
            synchronized(lock) {
                job.media = media
                rec.playlistText = text
                val ext = rec.ext ?: media.container
                rec.name = FileNames.sanitize((if (rec.companionId != 0L) rec.nameStem + " (picture)" else rec.nameStem) + "." + ext) // the picture half of a pair is a stepping stone: the merged file gets the clean name
                rec.mime = when (ext) { "mp4" -> "video/mp4"; "webm" -> "video/webm"; "aac" -> "audio/aac"; "mp3" -> "audio/mpeg"; "m4a" -> "audio/mp4"; else -> "video/mp2t" }
                if (media.exactBytes >= 0) rec.estimate = media.exactBytes
            }
        }
        val media = synchronized(lock) { job.media!! }
        val dir = store.dir(rec.id)
        // the output
        var sink: Sink? = null
        val uri = synchronized(lock) { rec.destUri }
        if (uri != null) sink = try { sinks.reopen(uri, -1) } catch (e: Exception) { null }
        if (sink == null) {
            synchronized(lock) { rec.appended = 0; rec.next = 0; rec.breaks = ""; rec.initDone = false; job.written = 0; rec.destUri = null } // the output is gone: the pointer starts over (staged segments are still good)
            sink = sinks.create(rec.name, rec.mime, -1, rec.priv, rec.destKind)
            synchronized(lock) { rec.destUri = sink.uri; sink.displayName?.let { rec.name = it } }
        } else synchronized(lock) { job.written = rec.appended }
        scanStaged(job, media, dir) // what an earlier run left in the staging folder
        synchronized(lock) { job.sink = sink }
        persist(rec)
        // fragmented MP4: the init section comes first
        val init = media.init
        if (init != null && !synchronized(lock) { rec.initDone }) {
            if (!net.online) throw Stopped().also { synchronized(lock) { job.stopState = DlState.WAITING_NETWORK } }
            val f = File(dir, "init.seg")
            fetchTo(job, fetcher, init.url, init.from, if (init.len >= 0) init.from + init.len - 1 else -1, f, count = false)
            FileInputStream(f).use { input ->
                val buf = ByteArray(64 * 1024)
                while (true) { val r = input.read(buf); if (r < 0) break; sink.pwrite(synchronized(lock) { job.written }, buf, 0, r); synchronized(lock) { job.written += r } }
            }
            f.delete()
            synchronized(lock) { rec.initDone = true; rec.appended = job.written }
        }
        synchronized(lock) { job.base = job.written + job.stagedBytes; job.inflight.set(0) }
    }

    private fun scanStaged(job: Job, media: Hls.Media, dir: File) = synchronized(lock) {
        job.staged.clear(); job.claimed.clear(); job.stagedBytes = 0; job.stagedCount = 0
        for (i in job.rec.next until media.segments.size) {
            val f = File(dir, "$i.seg")
            if (f.isFile) { job.staged.set(i); job.stagedBytes += f.length(); job.stagedCount++ } else File(dir, "$i.part").delete()
        }
    }

    private fun readText(job: Job, fetcher: Fetcher, url: String): String {
        val resp = open(job, fetcher, FetchRequest(url, -1, -1, null, job.rec.headers, priv = job.rec.priv))
        try {
            if (resp.status !in 200..299) throw statusFailure(resp.status)
            val body = resp.body ?: throw Failed(Reason.BAD_PLAYLIST)
            val out = java.io.ByteArrayOutputStream()
            val buf = ByteArray(16 * 1024)
            while (true) { if (job.stop) throw Stopped(); val r = body.read(buf); if (r < 0) break; out.write(buf, 0, r); if (out.size() > 8 * 1024 * 1024) throw Failed(Reason.BAD_PLAYLIST) }
            return out.toString("UTF-8")
        } catch (e: IOException) { if (job.stop) throw Stopped(); throw Failed(Reason.NETWORK) } finally { job.responses.remove(resp); try { resp.close() } catch (e: Exception) { /* ignore */ } }
    }

    private fun finish(job: Job): DlState {
        val rec = job.rec
        synchronized(lock) { rec.state = DlState.VERIFYING }
        changed(rec.id)
        val sink = job.sink ?: throw SinkException(Reason.STORAGE, "no file")
        if (synchronized(lock) { rec.next != job.media!!.segments.size }) throw SinkException(Reason.STORAGE, "stream incomplete")
        sink.sync()
        // a picture whose sound is a job of its own waits for it before anything is published
        val comp: Job? = synchronized(lock) { if (rec.companionId != 0L) jobs[rec.companionId] else null }
        if (comp != null) awaitCompanion(job, comp)
        val uri = sink.publish()
        synchronized(lock) { rec.destUri = uri ?: rec.destUri; job.sink = null }
        val media = synchronized(lock) { job.media!! }
        var converted: StreamFinisher.Converted? = null
        if (comp != null && finisher != null && uri != null) {
            // picture + sound -> one MP4 (if the muxer says no, both files stay as they are: the sound is shown as a download of its own)
            val audioUri = synchronized(lock) { comp.rec.destUri }
            if (audioUri != null) {
                converted = try { finisher.merge(StreamFinisher.Merge(uri, audioUri, rec.nameStem, job.written + synchronized(lock) { comp.written }, rec.priv, rec.destKind)) } catch (e: Throwable) { log("merge failed", e); null }
                if (converted != null) {
                    try { sinks.delete(uri) } catch (e: Exception) { /* ignore */ }
                    try { sinks.delete(audioUri) } catch (e: Exception) { /* ignore */ }
                    dropCompanion(comp)
                } else synchronized(lock) { comp.rec.parentId = 0L; comp.rec.updated = clock.now() }.also { persist(comp.rec); changed(comp.rec.id) }
            }
        } else if (finisher != null && uri != null && media.container == "ts" && !job.stop) {
            // MPEG-TS plays badly in most apps: make an MP4 of it (the .ts stays if anything goes wrong). Runs between discontinuities are handed over as parts; a stream that has
            // discontinuities but whose runs were not recorded (a download started by an older version) is left as it is, because its times cannot be laid end to end.
            val parts = partsOf(rec.breaks, media.segments, job.written)
            if (media.discontinuities == 0 || parts.size > 1) {
                converted = try { finisher.convert(StreamFinisher.Joined(uri, rec.nameStem, media.container, job.written, rec.priv, rec.destKind, parts)) } catch (e: Throwable) { log("convert failed", e); null }
            }
            if (converted != null) try { sinks.delete(uri) } catch (e: Exception) { /* the .ts stays next to the .mp4: harmless */ }
        }
        synchronized(lock) {
            if (converted != null) { rec.destUri = converted.uri; rec.name = converted.name; rec.mime = converted.mime; job.written = converted.size }
            rec.total = job.written; rec.appended = job.written
            rec.state = DlState.COMPLETED; rec.reason = Reason.NONE; rec.detail = 0; rec.finished = clock.now(); rec.updated = rec.finished
            rec.headers = emptyMap(); rec.playlistText = null
            if (converted == null && comp != null) rec.companionId = 0L // the sound stays a download of its own
        }
        wipeStaging(job)
        return DlState.COMPLETED
    }

    /** Wait (in the supervisor's own thread) until the sound job has finished; its failure is this job's failure. */
    private fun awaitCompanion(job: Job, comp: Job) {
        while (true) {
            if (job.stop) throw Stopped()
            val st = synchronized(lock) { comp.rec.state }
            when (st) {
                DlState.COMPLETED -> return
                DlState.FAILED -> throw Failed(synchronized(lock) { comp.rec.reason }, synchronized(lock) { comp.rec.detail })
                DlState.CANCELED -> throw Failed(Reason.STORAGE)
                else -> {}
            }
            if (synchronized(lock) { !jobs.containsKey(comp.rec.id) }) throw Failed(Reason.STORAGE)
            try { Thread.sleep(200) } catch (e: InterruptedException) { /* look again */ }
        }
    }

    /** The sound job failed or vanished while the picture still downloads: the picture job fails with the same reason. */
    private fun companionFailure(job: Job): Failed? {
        val cid = job.rec.companionId
        if (cid == 0L) return null
        val c = synchronized(lock) { jobs[cid] } ?: return null
        return synchronized(lock) { if (c.rec.state == DlState.FAILED) Failed(c.rec.reason, c.rec.detail) else null }
    }

    /** The merge made the sound's own file unnecessary: its job and record go. */
    private fun dropCompanion(comp: Job) {
        wipeStaging(comp)
        synchronized(lock) { jobs.remove(comp.rec.id) }
        if (!comp.rec.priv) store.delete(comp.rec.id)
        changed(comp.rec.id)
    }

    private fun checkpoint(job: Job) {
        val rec = job.rec
        try { job.sink?.sync() } catch (e: SinkException) { return } // only what is on disk may be claimed
        synchronized(lock) { rec.appended = job.written; rec.updated = clock.now(); job.lastCkpt = clock.now() }
        persist(rec)
    }

    // ================================================================================================ workers

    private fun worker(job: Job) {
        val rec = job.rec
        val fetcher = fetchers(rec.fetcher) ?: fetchers(0) ?: return
        val media = synchronized(lock) { job.media!! }
        val dir = store.dir(rec.id)
        try {
            while (!job.stop && job.failure == null) {
                val idx = claim(job, media.segments.size) ?: break
                try {
                    val seg = media.segments[idx]
                    fetchTo(job, fetcher, seg.url, seg.from, if (seg.len >= 0) seg.from + seg.len - 1 else -1, File(dir, "$idx.seg"), count = true)
                    synchronized(lock) { job.staged.set(idx); job.stagedCount++; job.stagedBytes += File(dir, "$idx.seg").length(); job.claimed.clear(idx) }
                    drain(job, media)
                } catch (e: RuntimeException) {
                    synchronized(lock) { job.claimed.clear(idx) }
                    throw e
                }
            }
        } catch (e: Stopped) { /* the supervisor knows */
        } catch (e: Failed) { synchronized(lock) { if (job.failure == null) job.failure = e; job.stop = true; for (r in job.responses) try { r.abort() } catch (x: Exception) { /* ignore */ } }
        } catch (e: SinkException) { synchronized(lock) { if (job.failure == null) job.failure = Failed(e.reason); job.stop = true }
        } catch (e: Throwable) { synchronized(lock) { if (job.failure == null) job.failure = Failed(Reason.STORAGE); job.stop = true } }
    }

    /** The lowest segment nobody has and nobody is fetching: in order, so the output can grow as soon as the first ones are in. */
    private fun claim(job: Job, n: Int): Int? = synchronized(lock) {
        var i = job.rec.next
        while (i < n) { if (!job.staged.get(i) && !job.claimed.get(i)) { job.claimed.set(i); return i }; i++ }
        null
    }

    /** Append every staged segment from the output's position onward, in order. One worker does it at a time; the others carry on fetching. */
    private fun drain(job: Job, media: Hls.Media) {
        while (true) {
            if (!job.drain.tryLock()) return
            try { while (appendNext(job, media)) { /* keep going while the next one is there */ } } finally { job.drain.unlock() }
            if (!nextReady(job, media)) return // else one arrived between the last look and the unlock
        }
    }

    private fun nextReady(job: Job, media: Hls.Media): Boolean = synchronized(lock) { !job.stop && job.rec.next < media.segments.size && job.staged.get(job.rec.next) }

    private fun appendNext(job: Job, media: Hls.Media): Boolean {
        val rec = job.rec
        val idx: Int
        synchronized(lock) { idx = rec.next; if (job.stop || idx >= media.segments.size || !job.staged.get(idx)) return false }
        val seg = media.segments[idx]
        val sink = job.sink ?: return false
        val f = File(store.dir(rec.id), "$idx.seg")
        val size = f.length()
        var written = synchronized(lock) { job.written }
        val startedAt = written
        try {
            FileInputStream(f).use { input ->
                val buf = ByteArray(64 * 1024)
                if (seg.key >= 0) {
                    val key = keyBytes(job, media, seg.key)
                    val iv = media.keys[seg.key].iv ?: Hls.sequenceIv(seg.seq)
                    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
                    try { cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv)) } catch (e: Exception) { throw Failed(Reason.UNSUPPORTED) }
                    while (true) {
                        val r = input.read(buf); if (r < 0) break
                        val out = cipher.update(buf, 0, r) ?: continue
                        if (out.isNotEmpty()) { sink.pwrite(written, out, 0, out.size); written += out.size }
                    }
                    val tail = try { cipher.doFinal() } catch (e: Exception) { throw Failed(Reason.UNSUPPORTED) } // wrong key or damaged segment
                    if (tail.isNotEmpty()) { sink.pwrite(written, tail, 0, tail.size); written += tail.size }
                } else {
                    while (true) { val r = input.read(buf); if (r < 0) break; sink.pwrite(written, buf, 0, r); written += r }
                }
            }
        } catch (e: IOException) { throw SinkException(Reason.STORAGE, "could not read a staged segment", e) }
        synchronized(lock) {
            job.written = written; rec.next = idx + 1; rec.appended = written; rec.crashes = 0
            if (seg.discontinuity && idx > 0) rec.breaks += "$idx:$startedAt;"
            job.stagedBytes -= size; job.stagedCount--; job.staged.clear(idx)
        }
        f.delete()
        return true
    }

    private fun keyBytes(job: Job, media: Hls.Media, ki: Int): ByteArray {
        synchronized(lock) { job.keys[ki]?.let { return it } }
        val fetcher = fetchers(job.rec.fetcher) ?: fetchers(0) ?: throw Failed(Reason.NETWORK)
        val resp = open(job, fetcher, FetchRequest(media.keys[ki].url, -1, -1, null, job.rec.headers, priv = job.rec.priv))
        try {
            if (resp.status !in 200..299) throw statusFailure(resp.status)
            val b = resp.body?.readBytes() ?: throw Failed(Reason.BAD_PLAYLIST)
            if (b.size != 16) throw Failed(Reason.UNSUPPORTED) // an AES-128 key is exactly 16 bytes; anything else is a licence server's answer
            synchronized(lock) { job.keys[ki] = b }
            return b
        } catch (e: IOException) { throw Failed(Reason.NETWORK) } finally { job.responses.remove(resp); try { resp.close() } catch (e: Exception) { /* ignore */ } }
    }

    // ---- one segment: fetched into [dest] (".part" first, renamed when whole), retried with back-off

    private fun fetchTo(job: Job, fetcher: Fetcher, url: String, from: Long, toInclusive: Long, dest: File, count: Boolean) {
        val part = File(dest.parentFile, dest.name.removeSuffix(".seg") + ".part")
        var tries = 0
        while (true) {
            if (job.stop) throw Stopped()
            if (!net.online) { synchronized(lock) { if (job.stopState == null) job.stopState = DlState.WAITING_NETWORK; job.stop = true }; throw Stopped() }
            val ctr = LongArray(1)
            try {
                FileOutputStream(part).use { out -> readInto(job, fetcher, url, from, toInclusive, out, count, ctr) }
                // a web page where a video part should be (an expired or blocked link that answers 200): never let its text into the video
                if (looksLikeHtmlFile(part)) { part.delete(); if (count) job.inflight.addAndGet(-ctr[0]); throw Failed(Reason.BAD_CONTENT) }
                if (!part.renameTo(dest)) throw Failed(Reason.STORAGE)
                return
            } catch (e: FetchException) {
                if (count) job.inflight.addAndGet(-ctr[0])
                if (job.stop) throw Stopped()
                when (e.kind) {
                    FetchException.Kind.TLS -> throw Failed(Reason.TLS)
                    FetchException.Kind.LOCAL_NET -> throw Failed(Reason.LOCAL_NET)
                    FetchException.Kind.INSECURE_REDIRECT -> throw Failed(Reason.INSECURE_REDIRECT)
                    FetchException.Kind.BAD_URL -> throw Failed(Reason.BAD_URL)
                    else -> {}
                }
                if (++tries >= MAX_TRIES) throw Failed(Reason.NETWORK)
                backoff(job, tries)
            } catch (e: Retry) {
                if (count) job.inflight.addAndGet(-ctr[0])
                if (job.stop) throw Stopped()
                if (++tries >= MAX_TRIES) throw Failed(if (e.status > 0) Reason.HTTP else Reason.NETWORK, e.status)
                backoff(job, tries, e.retryAfterMs)
            } catch (e: IOException) {
                if (count) job.inflight.addAndGet(-ctr[0])
                if (job.stop) throw Stopped()
                if (e is java.io.FileNotFoundException || e.message?.contains("ENOSPC") == true || e.message?.contains("No space") == true) throw Failed(Reason.NO_SPACE)
                if (++tries >= MAX_TRIES) throw Failed(Reason.NETWORK)
                backoff(job, tries)
            }
        }
    }

    private fun looksLikeHtmlFile(f: File): Boolean = try {
        FileInputStream(f).use { i -> val b = ByteArray(512); val n = i.read(b); n > 0 && app.kestrel.core.download.ContentSniff.looksLikeHtml(b, 0, n) }
    } catch (e: IOException) { false }

    private class Retry(val status: Int, val retryAfterMs: Long = 0) : RuntimeException(null, null, false, false)

    private fun backoff(job: Job, tries: Int, minMs: Long = 0) {
        val wait = maxOf(minMs, minOf(cfg.backoffCapMs, cfg.backoffBaseMs shl minOf(tries - 1, 10))) + rnd.nextInt(250)
        val end = clock.now() + wait
        while (clock.now() < end) { if (job.stop) throw Stopped(); try { Thread.sleep(minOf(100L, maxOf(1L, end - clock.now()))) } catch (e: InterruptedException) { /* look again */ } }
    }

    /** Streams one segment (in bounded windows when the fetcher wants them, resuming after a short reply) into [out]; [ctr] counts the bytes added to the progress. */
    private fun readInto(job: Job, fetcher: Fetcher, url: String, from: Long, toInclusive: Long, out: FileOutputStream, count: Boolean, ctr: LongArray) {
        val window = fetcher.windowBytes
        val known = from >= 0 && toInclusive >= 0
        val segLen = if (known) toInclusive - from + 1 else -1L
        var pos = 0L                                   // bytes of this segment stored so far
        while (true) {
            val rf: Long; val rt: Long
            when {
                known -> { rf = from + pos; rt = if (window > 0) minOf(toInclusive, rf + window - 1) else toInclusive }
                window > 0 -> { rf = pos; rt = pos + window - 1 }
                else -> { rf = -1; rt = -1 }
            }
            val resp = open(job, fetcher, FetchRequest(url, rf, rt, null, job.rec.headers, priv = job.rec.priv))
            var got = 0L; var total = -1L; var whole = false
            try {
                val st = resp.status
                if (st == 416 && pos > 0) return          // asked past the end: the segment is whole
                if (st != 200 && st != 206) throw statusFailure(st, resp)
                whole = st == 200
                if (st == 206) total = totalOf(resp.header("Content-Range"))
                val body = resp.body ?: throw FetchException(FetchException.Kind.PROTOCOL, "no body")
                var skip = if (whole && rf > 0) rf else 0L // a server that ignored Range sent everything: drop what is before the part we asked for
                var limit = if (whole && rt >= 0) rt - maxOf(rf, 0L) + 1 else Long.MAX_VALUE // and stop after it
                val buf = ByteArray(32 * 1024)
                while (limit > 0) {
                    if (job.stop) throw Stopped()
                    val r = body.read(buf)
                    if (r < 0) break
                    var off = 0; var len = r
                    if (skip > 0) { val s = minOf(skip, len.toLong()).toInt(); skip -= s; off += s; len -= s }
                    if (len > limit) len = limit.toInt()
                    if (len > 0) {
                        out.write(buf, off, len); got += len; limit -= len
                        if (count) { job.inflight.addAndGet(len.toLong()); ctr[0] += len; job.meter.add(len, clock.now()); job.lastBytesAt = clock.now() }
                    }
                }
            } finally { job.responses.remove(resp); try { resp.close() } catch (e: Exception) { /* ignore */ } }
            pos += got
            if (whole || rf < 0) return                   // a full reply (or a plain GET) is everything there is
            if (known && pos >= segLen) return
            if (total >= 0 && rf + got >= total) return   // reached the end of the resource
            if (got == 0L) return
            if (!known && got < window) return            // a short window: that was the end
        }
    }

    private fun open(job: Job, fetcher: Fetcher, req: FetchRequest): FetchResponse {
        val resp = fetcher.open(req)
        job.responses.add(resp)
        if (job.stop) { job.responses.remove(resp); try { resp.abort(); resp.close() } catch (e: Exception) { /* ignore */ }; throw Stopped() }
        return resp
    }

    private fun statusFailure(status: Int, resp: FetchResponse? = null): RuntimeException = when {
        status == 401 || status == 403 || status == 410 -> Failed(Reason.LINK_EXPIRED, status)
        status == 404 || status in 400..499 && status != 408 && status != 429 -> Failed(Reason.HTTP, status)
        else -> Retry(status, resp?.header("Retry-After")?.trim()?.toLongOrNull()?.times(1000) ?: 0)
    }

    private fun totalOf(contentRange: String?): Long = contentRange?.substringAfterLast('/')?.trim()?.toLongOrNull() ?: -1L

    // ================================================================================================ views

    private fun view(job: Job): DownloadView {
        val r = job.rec
        val now = clock.now()
        val running = r.state == DlState.RUNNING
        val media = job.media
        val n = media?.segments?.size ?: 0
        val fetched = when {
            r.state == DlState.COMPLETED -> r.total
            r.state.active -> job.base + job.inflight.get().coerceAtLeast(0)
            else -> job.written + job.stagedBytes
        }
        var total: Long
        var estimated = false
        val have = r.next + job.stagedCount
        when {
            r.state == DlState.COMPLETED -> total = r.total
            media != null && media.exactBytes >= 0 -> total = media.exactBytes
            media != null && n > 0 && have >= 2 -> { total = (fetched.toDouble() / have * n).toLong(); estimated = true }
            r.estimate > 0 -> { total = r.estimate; estimated = true }
            else -> total = -1
        }
        if (total in 0 until fetched) total = fetched
        val speed = if (running) job.meter.bytesPerSec(now).toLong() else 0L
        val eta = if (running && speed > 0 && total > 0) (total - fetched).coerceAtLeast(0) / speed else -1L
        val actions: List<Action> = when (r.state) {
            DlState.QUEUED, DlState.STARTING -> listOf(Action.CANCEL)
            DlState.RUNNING, DlState.WAITING_NETWORK -> listOf(Action.PAUSE, Action.CANCEL)
            DlState.PAUSED_USER, DlState.PAUSED_SYSTEM -> listOf(Action.RESUME, Action.CANCEL)
            DlState.NEEDS_DECISION -> listOf(Action.RESTART, Action.CANCEL)
            DlState.VERIFYING -> emptyList()
            DlState.FAILED -> listOf(Action.RETRY, Action.DELETE)
            DlState.COMPLETED -> listOf(Action.OPEN, Action.SHARE, Action.DELETE)
            DlState.CANCELED -> emptyList()
        }
        val host = try { java.net.URI(r.url).host } catch (e: Exception) { null }
        val comp = if (r.companionId != 0L && r.state != DlState.COMPLETED) jobs[r.companionId] else null
        if (comp != null) { // one row for picture + sound: sizes, speed and connections add up
            val cv = view(comp)
            val sumTotal = if (total >= 0 && cv.total >= 0) total + cv.total else -1L
            return DownloadView(r.id, r.state, r.reason, r.detail, r.name, r.mime, host, sumTotal, fetched + cv.done, speed + cv.bytesPerSec,
                if (running && speed + cv.bytesPerSec > 0 && sumTotal > 0) (sumTotal - fetched - cv.done).coerceAtLeast(0) / (speed + cv.bytesPerSec) else -1L, job.alive + cv.connections, r.priv, r.destUri, r.created, r.updated, actions, r.destKind, estimated || cv.estimated)
        }
        return DownloadView(r.id, r.state, r.reason, r.detail, r.name, r.mime, host, total, fetched, speed, eta, job.alive, r.priv, r.destUri, r.created, r.updated, actions, r.destKind, estimated)
    }

    companion object {
        private const val MAX_TRIES = 6

        /**
         * The runs of a joined file between discontinuities, from the "index:offset;" pairs the engine kept while appending ([StreamRec.breaks]): the byte range and the playlist's own length of each.
         * Empty when the stream has no (recorded) discontinuity. Pairs that do not fit the file or are out of order are ignored.
         */
        internal fun partsOf(breaks: String, segments: List<Hls.Seg>, total: Long): List<StreamFinisher.Part> {
            val idx = ArrayList<Int>(); val off = ArrayList<Long>()
            idx.add(0); off.add(0L)
            for (e in breaks.split(';')) {
                if (e.isEmpty()) continue
                val i = e.substringBefore(':').toIntOrNull() ?: continue
                val o = e.substringAfter(':', "").toLongOrNull() ?: continue
                if (i > idx.last() && i < segments.size && o > off.last() && o < total) { idx.add(i); off.add(o) }
            }
            if (idx.size < 2) return emptyList()
            return idx.indices.map { k ->
                val endIdx = if (k + 1 < idx.size) idx[k + 1] else segments.size
                val endOff = if (k + 1 < idx.size) off[k + 1] else total
                var ms = 0L
                for (i in idx[k] until endIdx) ms += segments[i].durationMs
                StreamFinisher.Part(off[k], endOff - off[k], ms)
            }
        }
    }
}
