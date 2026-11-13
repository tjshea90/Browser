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
import app.kestrel.core.download.RateLimiter
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
    /** EY-8 (research/REVIEW-EXTERNAL-2.md section 4.5): what the download needs to find its way back when a link is refused. The address the owner picked from (a master playlist or manifest; [playlistUrl] is the chosen quality's own). */
    val masterUrl: String? = null,
    /** The document that asked for it (an embedded player's own page), null when it was the page itself. */
    val frame: String? = null,
    /** The header ladder, best first (the browser's own, the player's page, the web page, the Referer alone, nothing): when a part keeps being refused the engine tries the next set, and the one that works stays. [headers] is the first. */
    val headerSets: List<Map<String, String>> = emptyList(),
    /** When the link was handed over, and when it says it stops working (0 = unknown). */
    val madeAt: Long = 0,
    val expiresAt: Long = 0,
)

/** One stream download as stored (everything needed to carry on after the app was killed: the playlist text is kept, so the segment list is the same on resume). */
class StreamRec(@JvmField val id: Long) {
    @JvmField var state = DlState.QUEUED
    @JvmField var reason = Reason.NONE
    @JvmField var detail = 0
    @JvmField var url = ""
    @JvmField var playlistText: String? = null
    @JvmField var pageUrl: String? = null
    /** The header set in use (one worker may switch it to the next of [headerSets] while a part is refused, so every reader sees the newest). */
    @JvmField @Volatile var headers: Map<String, String> = emptyMap()
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
    /** How a transport-stream video is written: [MUX_UNDECIDED] until its first segment was looked at, [MUX_MP4] = straight into an MP4 while downloading, [MUX_RAW] = joined as it is (and converted at the end if possible). */
    @JvmField var mux = MUX_UNDECIDED
    /** One sentence for the row: why the file is what it is (see [DownloadView.note]). */
    @JvmField var note: String? = null
    /** EY-8: what it takes to find the same video again when the link is refused (see [StreamSource]). Old records have none of it and carry on as before. */
    @JvmField var masterUrl: String? = null
    @JvmField var frame: String? = null
    @JvmField var headerSets: List<Map<String, String>> = emptyList()
    /** The rendition's fingerprint (EY-11 fills and compares it); empty until then. */
    @JvmField var fp: String? = null
    @JvmField var madeAt = 0L
    @JvmField var expiresAt = 0L
    @JvmField var initDone = false
    @JvmField var total = -1L
    @JvmField var created = 0L
    @JvmField var updated = 0L
    @JvmField var finished = 0L

    fun snapshot(): StreamRec {
        val r = StreamRec(id)
        r.state = state; r.reason = reason; r.detail = detail; r.url = url; r.playlistText = playlistText; r.pageUrl = pageUrl; r.headers = headers; r.fetcher = fetcher; r.priv = priv
        r.meteredOk = meteredOk; r.nameStem = nameStem; r.name = name; r.mime = mime; r.destKind = destKind; r.destUri = destUri; r.estimate = estimate; r.repId = repId; r.ext = ext; r.companionId = companionId; r.parentId = parentId; r.appended = appended
        r.next = next; r.breaks = breaks; r.crashes = crashes; r.mux = mux; r.note = note; r.initDone = initDone; r.total = total; r.created = created; r.updated = updated; r.finished = finished
        r.masterUrl = masterUrl; r.frame = frame; r.headerSets = headerSets; r.fp = fp; r.madeAt = madeAt; r.expiresAt = expiresAt
        return r
    }
}

const val MUX_UNDECIDED = 0
const val MUX_MP4 = 1
const val MUX_RAW = 2

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
    class Joined(val uri: String, val stem: String, val container: String, val size: Long, val priv: Boolean, val destKind: Int, val parts: List<Part> = emptyList()) {
        /** Set by the finisher when it returns null: one plain phrase for why the stream stays a .ts ("there is not enough free room"). */
        @Volatile var declined: String? = null
    }
    class Converted(val uri: String, val name: String, val mime: String, val size: Long)
    /** Two finished files of one video (picture and sound, as DASH keeps them) to be made one MP4. */
    class Merge(val videoUri: String, val audioUri: String, val stem: String, val size: Long, val priv: Boolean, val destKind: Int)
    fun convert(j: Joined): Converted?
    fun merge(m: Merge): Converted? = null
    /** Reads a finished MP4 back with the platform's own parser; null = it opened fine (or this finisher cannot check), else a short description of what it could not do. */
    fun verify(uri: String): String? = null
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
    /** The one speed limit of the whole app (shared with the single-file engine); 0 = none. */
    private val limiter: RateLimiter = RateLimiter(clock),
    /**
     * What happens when picture and sound (DASH) cannot be made one MP4: true (the browser) = both stay as two downloads the owner can see; false (the phone app, which works for a tablet and has
     * no row of its own for the sound) = everything is thrown away and the job fails, so the tablet does the download itself instead of receiving a picture without its sound.
     */
    private val keepApartOnMergeFailure: Boolean = true,
) {
    interface Listener { fun onChanged(id: Long) }

    private class Stopped : RuntimeException(null, null, false, false)
    /** [restartRaw]: not a failure of the download but of the MP4 writer: the job starts over the old way (joined .ts, converted at the end if possible). */
    private class Failed(val reason: Reason, val detail: Int = 0, val restartRaw: Boolean = false, val why: String? = null) : RuntimeException(why, null, false, false)
    /** The MP4 writer broke in the middle of a segment (a bug, or data it cannot handle). */
    private class MuxBroken(why: String, cause: Throwable?) : RuntimeException(why, cause, false, false)

    private inner class Job(val rec: StreamRec) {
        @Volatile var autoResume = false   // the last run ended in the middle of this download: carry on by itself ([resumeInterrupted])
        @Volatile var stop = false
        @Volatile var stopState: DlState? = null
        @Volatile var failure: Failed? = null
        var media: Hls.Media? = null
        val staged = BitSet()
        val claimed = BitSet()
        /** Segments nobody fetched because the MP4 writer is leaving their run out (an advertisement of another format): they only move the output's pointer. */
        val skipped = BitSet()
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
        var mux: TsToMp4? = null        // the MP4 being written while the segments arrive ([MUX_MP4] jobs only)
        @Volatile var muxDirty = false  // the muxer was in the middle of a segment when something failed: its state may not be checkpointed
        var lastMuxCkpt = 0L
        @Volatile var forceRaw = false  // the MP4 writer failed once for this job: do it the old way
        @Volatile var converting = false // "Make MP4" is working on this finished download (shown as Finishing; the stored record stays COMPLETED)
        /** Workers that are trying a refused part again (guarded by [lock]): every other worker waits before it claims anything new (one connection while the server is being asked again, EY-7). */
        var recovering = 0
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
                if (r.state == DlState.CANCELED) { dropLeftover(r); store.delete(r.id); continue } // a cancel that was cut off half way: neither the output nor the staging may outlive it
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
        r.masterUrl = s.masterUrl; r.frame = s.frame; r.headerSets = s.headerSets; r.madeAt = if (s.madeAt > 0) s.madeAt else clock.now(); r.expiresAt = s.expiresAt
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

    /**
     * "Make MP4" on a finished stream that was kept as a .ts (a download from an older version, or a stream the MP4 writer could not take at the time): the file is read once through the MP4
     * writer into a new .mp4 and the .ts goes when that worked. The row shows "Finishing" meanwhile; the stored record stays COMPLETED, so a kill in the middle loses nothing.
     */
    fun makeMp4(id: Long) = control.execute {
        val job = get(id) ?: return@execute
        val rec = job.rec
        val ok = synchronized(lock) { rec.state == DlState.COMPLETED && !job.converting && rec.mux != MUX_MP4 && rec.destUri != null && rec.name.endsWith(".ts", true) && rec.companionId == 0L && rec.parentId == 0L }
        if (!ok) return@execute                                                    // (a download that has just completed may still be in its last bookkeeping step: that does not matter, the state says it is finished)
        synchronized(lock) { job.converting = true }
        changed(id)
        try { spawner.spawn("mp4-$id") { convertFinished(job) } }
        catch (e: Throwable) { synchronized(lock) { job.converting = false }; changed(id) }
    }

    private fun convertFinished(job: Job) {
        val rec = job.rec
        var out: Sink? = null
        var note: String? = null
        var swapped = false
        try {
            val uri = synchronized(lock) { rec.destUri } ?: return
            val src = sinks.reopen(uri, -1)
            if (src == null) { note = "Could not make an MP4: the .ts file is not there any more."; return }
            val size = src.length()
            if (size <= 0 || !src.readable) { note = "Could not make an MP4: this file cannot be read back."; return }
            val head = ByteArray(minOf(size, 384L * 1024).toInt())
            if (readFully(src, 0, head) < head.size) { note = "Could not make an MP4: the .ts file could not be read."; return }
            val why = TsToMp4.probe(head, 0, head.size)
            if (why != null) {                                                   // the MP4 writer cannot take it: the platform converter may (H.265, for one)
                val f = finisher
                val j = StreamFinisher.Joined(uri, rec.nameStem, "ts", size, rec.priv, rec.destKind, emptyList())
                val c = if (f != null) try { f.convert(j) } catch (e: Throwable) { log("convert failed", e); null } else null
                if (c == null) { note = "Could not make an MP4: ${j.declined ?: why}."; return }
                swap(job, uri, c.uri, c.name, c.mime, c.size, null); swapped = true
                return
            }
            val name = FileNames.sanitize(rec.nameStem + ".mp4")
            val o = sinks.create(name, "video/mp4", -1, rec.priv, rec.destKind).also { out = it }
            val sink = o
            val adapter = object : MuxOut {
                override fun write(pos: Long, b: ByteArray, off: Int, len: Int) = sink.pwrite(pos, b, off, len)
                override fun truncate(len: Long) = sink.truncate(len)
            }
            val noJournal = object : MuxJournal { override fun append(block: ByteArray) {}; override fun recover(): List<ByteArray> = emptyList(); override fun reset() {} }
            val m = TsToMp4(adapter, noJournal, { sink.sync() }, size, unitsAreMs = false)
            m.startFresh()
            // the runs between #EXT-X-DISCONTINUITY marks, as recorded while it downloaded; their weight is their length in bytes (the playlist is gone)
            val starts = ArrayList<Long>(); starts.add(0L)
            for (e in synchronized(lock) { rec.breaks }.split(';')) { val o2 = e.substringAfter(':', "").toLongOrNull() ?: continue; if (o2 > starts.last() && o2 < size) starts.add(o2) }
            val buf = ByteArray(64 * 1024)
            for ((k, from) in starts.withIndex()) {
                val to = if (k + 1 < starts.size) starts[k + 1] else size
                m.beginSegment(k, k > 0, to - from)
                var p = from
                while (p < to) {
                    val n = src.pread(p, buf, 0, minOf(buf.size.toLong(), to - p).toInt())
                    if (n <= 0) throw IOException("short read at $p")
                    m.feed(buf, 0, n); p += n
                }
                m.endSegment()
            }
            val fin = m.finish()
            sink.sync()
            val newUri = sink.publish() ?: throw SinkException(Reason.STORAGE, "no uri")
            out = null
            swap(job, uri, newUri, sink.displayName ?: name, "video/mp4", fin.length, fin.warning); swapped = true
        } catch (e: MuxException) { note = "Could not make an MP4: ${e.message}."
        } catch (e: SinkException) { note = if (e.reason == Reason.NO_SPACE || e.reason == Reason.STORAGE) "Could not make an MP4: there is not enough free room on the tablet." else "Could not make an MP4." ; log("make mp4: the output failed", e)
        } catch (e: Throwable) { note = "Could not make an MP4."; log("make mp4 failed", e)
        } finally {
            try { out?.discard() } catch (e: Exception) { /* ignore */ }
            synchronized(lock) { job.converting = false; if (!swapped && note != null) rec.note = note; rec.updated = clock.now() }
            persist(rec)
            changed(rec.id)
        }
    }

    private fun swap(job: Job, oldUri: String, newUri: String, name: String, mime: String, size: Long, warning: String?) {
        val rec = job.rec
        synchronized(lock) { rec.destUri = newUri; rec.name = name; rec.mime = mime; rec.total = size; rec.appended = size; rec.mux = MUX_MP4; rec.breaks = ""; rec.note = warning }
        try { sinks.delete(oldUri) } catch (e: Exception) { /* the .ts stays next to the .mp4: harmless */ }
    }

    private fun readFully(sink: Sink, pos: Long, b: ByteArray): Int {
        var p = 0
        while (p < b.size) { val n = sink.pread(pos + p, b, p, b.size - p); if (n <= 0) break; p += n }
        return p
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
        dropJob(rec.id)
        if (!rec.priv) store.delete(rec.id)
        changed(rec.id)
    }

    /** "Clear" forgets finished rows: a completed save keeps its file, a failed (or still listed cancelled) one takes its unfinished output and its staging with it. */
    fun clearFinished() = control.execute {
        for (j in snapshotJobs()) {
            val st = synchronized(lock) { j.rec.state }
            if (j.rec.parentId == 0L && (st == DlState.COMPLETED || st == DlState.FAILED || st == DlState.CANCELED)) {
                if (st != DlState.COMPLETED) dropLeftover(j.rec)
                wipeStaging(j); dropJob(j.rec.id); if (!j.rec.priv) store.delete(j.rec.id); changed(j.rec.id)
            }
        }
    }

    /** Delete the unfinished output a row still points at, and the staging folder of a row that has no job in memory. Never throws. */
    private fun dropLeftover(rec: StreamRec) {
        val uri = synchronized(lock) { rec.destUri }
        if (uri != null) try { sinks.delete(uri) } catch (e: Exception) { /* already gone */ }
        try { val d = store.dir(rec.id); d.listFiles()?.forEach { it.delete() }; d.delete() } catch (e: Exception) { /* ignore */ }
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
    fun needsService(): Boolean = synchronized(lock) { jobs.values.any { val s = it.rec.state; s == DlState.QUEUED || s.active || s == DlState.WAITING_NETWORK || it.converting } }
    fun counts(): IntArray = synchronized(lock) {
        var running = 0; var queued = 0; var paused = 0
        for (j in jobs.values) if (j.rec.parentId == 0L) when (if (j.converting) DlState.VERIFYING else j.rec.state) { DlState.RUNNING, DlState.STARTING, DlState.VERIFYING -> running++; DlState.QUEUED -> queued++; DlState.PAUSED_USER, DlState.PAUSED_SYSTEM, DlState.WAITING_NETWORK -> paused++; else -> {} }
        intArrayOf(running, queued, paused)
    }

    // ================================================================================================ control thread

    private fun get(id: Long): Job? = synchronized(lock) { jobs[id] }
    private fun snapshotJobs(): List<Job> = synchronized(lock) { jobs.values.toList() }
    /** Ids of jobs that were removed on purpose (a pair's merged-away sound, a cancelled or cleared download): a late checkpoint of such a job must not write its row back. Ids are never reused. */
    private val dropped = HashSet<Long>()

    private fun persist(r: StreamRec) {
        if (r.priv) return
        if (synchronized(lock) { r.id in dropped }) return
        try { store.save(r.snapshot()) } catch (e: Exception) { /* the next checkpoint retries */ }
    }

    private fun dropJob(id: Long) { synchronized(lock) { jobs.remove(id); dropped.add(id) } }

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
        job.mux = null; job.muxDirty = false
        r.destUri = null; r.appended = 0; r.next = 0; r.breaks = ""; r.initDone = false; r.total = -1; r.mux = MUX_UNDECIDED; r.note = null
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
        dropJob(rec.id)
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
            drain(job, synchronized(lock) { job.media!! })                       // segments already staged (the first one was fetched to look at it; an earlier run left others) are appended before anything else: a stream whose segments are ALL staged has no worker that would do it
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
            if (e.restartRaw) {                                                 // the MP4 writer gave up: start this download over the old way, nothing is lost but the time
                outcome = DlState.QUEUED
                synchronized(lock) { job.forceRaw = true; job.restartAfterStop = true; job.wipeAfterStop = true }
                log("stream ${rec.id}: the MP4 writer failed (${e.why}), starting over as a .ts", null)
            } else {
                outcome = DlState.FAILED
                synchronized(lock) { rec.reason = e.reason; rec.detail = e.detail }
                log("stream ${rec.id} failed: ${e.reason} ${e.detail}", null)
            }
        } catch (e: MuxBroken) {
            outcome = DlState.QUEUED
            synchronized(lock) { job.forceRaw = true; job.restartAfterStop = true; job.wipeAfterStop = true }
            log("stream ${rec.id}: the MP4 writer failed, starting over as a .ts", e)
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
        // a stopped job keeps its progress: flush the output so the checkpoint is true (and, for an MP4 being written, the journal too, unless the muxer was cut off in the middle of a segment: then the last checkpoint stands)
        if (outcome != DlState.COMPLETED && outcome != DlState.CANCELED) {
            val m = job.mux
            if (m != null && !job.muxDirty && !job.wipeAfterStop) try { job.drain.lock(); try { m.checkpoint(synchronized(lock) { rec.next }) } finally { job.drain.unlock() } } catch (e: Exception) { /* the last checkpoint still stands */ }
            else try { job.sink?.sync() } catch (e: Exception) { /* the last checkpoint still stands */ }
        }
        if (outcome != DlState.COMPLETED) job.mux = null                    // a new muxer is rebuilt from the journal when the job runs again
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
                if (media.exactBytes >= 0) rec.estimate = media.exactBytes
            }
        }
        val media = synchronized(lock) { job.media!! }
        val dir = store.dir(rec.id)
        if (media.container == "ts" && rec.ext == null) decideMux(job, media, fetcher, dir)
        applyName(rec, media)
        // the output
        var sink: Sink? = null
        val uri = synchronized(lock) { rec.destUri }
        if (uri != null) sink = try { sinks.reopen(uri, -1) } catch (e: Exception) { null }
        var reopened = false
        if (sink == null) {
            synchronized(lock) { rec.appended = 0; rec.next = 0; rec.breaks = ""; rec.initDone = false; job.written = 0; rec.destUri = null } // the output is gone: the pointer starts over (staged segments are still good)
            sink = sinks.create(rec.name, rec.mime, -1, rec.priv, rec.destKind)
            synchronized(lock) { rec.destUri = sink.uri; sink.displayName?.let { rec.name = it } }
        } else { reopened = true; synchronized(lock) { job.written = rec.appended } }
        if (synchronized(lock) { rec.mux } == MUX_MP4) startMux(job, media, sink, dir, reopened)
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

    private fun applyName(rec: StreamRec, media: Hls.Media) {
        synchronized(lock) {
            val ext = rec.ext ?: (if (rec.mux == MUX_MP4 && media.container == "ts") "mp4" else media.container)
            rec.name = FileNames.sanitize((if (rec.companionId != 0L) rec.nameStem + " (picture)" else rec.nameStem) + "." + ext) // the picture half of a pair is a stepping stone: the merged file gets the clean name
            rec.mime = when (ext) { "mp4" -> "video/mp4"; "webm" -> "video/webm"; "aac" -> "audio/aac"; "mp3" -> "audio/mpeg"; "m4a" -> "audio/mp4"; else -> "video/mp2t" }
        }
    }

    /**
     * A transport-stream video is written straight into an MP4 while it downloads when its first segment holds H.264 (and AAC sound or none): no .ts file, no second copy, no wait at the end.
     * Anything else (or a download that already began the old way) keeps the joined-.ts path and says why in the row's note.
     */
    private fun decideMux(job: Job, media: Hls.Media, fetcher: Fetcher, dir: File) {
        val rec = job.rec
        if (synchronized(lock) { rec.mux != MUX_UNDECIDED || job.forceRaw }) {
            if (job.forceRaw) synchronized(lock) { rec.mux = MUX_RAW; rec.note = "Kept as a .ts file because the MP4 writer could not handle this stream." }
            return
        }
        if (!cfg.directMp4 || synchronized(lock) { rec.next > 0 || rec.destUri != null }) { synchronized(lock) { rec.mux = MUX_RAW }; return }   // a download from an older version, or the switch is off
        val seg = media.segments[0]
        val f = File(dir, "0.seg")
        if (!f.isFile) {
            if (!net.online) throw Stopped().also { synchronized(lock) { job.stopState = DlState.WAITING_NETWORK } }
            fetchTo(job, fetcher, seg.url, seg.from, if (seg.len >= 0) seg.from + seg.len - 1 else -1, f, count = true)
        }
        val why: String? = try {
            val n = minOf(f.length(), 384L * 1024).toInt()
            val buf = ByteArray(n)
            FileInputStream(f).use { input -> var p = 0; while (p < n) { val r = input.read(buf, p, n - p); if (r < 0) break; p += r } }
            val skip = if (seg.key >= 0) SegmentPrefix.imageLength(buf, n, ts = false) else SegmentPrefix.length(buf, n, ts = true) // a picture header in front of the part (a part named ".jpg"): not media
            val plain = if (seg.key >= 0) {
                val c = Cipher.getInstance("AES/CBC/NoPadding")
                try { c.init(Cipher.DECRYPT_MODE, SecretKeySpec(keyBytes(job, media, seg.key), "AES"), IvParameterSpec(media.keys[seg.key].iv ?: Hls.sequenceIv(seg.seq))) } catch (e: Exception) { throw Failed(Reason.UNSUPPORTED) }
                c.update(buf, skip, (n - skip) and 15.inv())
            } else buf.copyOfRange(skip, n)
            TsToMp4.probe(plain, 0, plain.size)
        } catch (e: IOException) { "the first part of the stream could not be read back" }
        synchronized(lock) {
            if (why == null) { rec.mux = MUX_MP4; rec.note = null }
            else { rec.mux = MUX_RAW; rec.note = "Kept as a .ts file because $why." }
        }
        if (why != null) log("stream ${rec.id}: raw .ts: $why", null)
        persist(rec)
    }

    /** MP4 mode: the muxer over the output, its journal next to the staged segments; a restarted job is brought back to its last checkpoint. */
    private fun startMux(job: Job, media: Hls.Media, sink: Sink, dir: File, reopened: Boolean) {
        val rec = job.rec
        val out = object : MuxOut {
            override fun write(pos: Long, b: ByteArray, off: Int, len: Int) = sink.pwrite(pos, b, off, len)
            override fun truncate(len: Long) = sink.truncate(len)
        }
        val m = TsToMp4(out, FileMuxJournal(File(dir, "mux.jnl")), { sink.sync() }, media.durationMs)
        val next = if (reopened) m.recover() else null
        if (next == null) { m.startFresh(); synchronized(lock) { rec.next = 0; rec.appended = m.position; job.written = m.position; rec.breaks = "" } }
        else synchronized(lock) { rec.next = next; rec.appended = m.position; job.written = m.position }  // the journal knows better than the saved record: it is written first
        job.mux = m; job.muxDirty = false; job.lastMuxCkpt = clock.now()
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
        val mux = job.mux
        if (mux != null) {                                                      // the MP4 is already in the file: only its index (the moov) is left to write
            try { val fin = mux.finish(); synchronized(lock) { job.written = fin.length; rec.note = fin.warning } }
            catch (e: MuxException) { job.muxDirty = true; throw Failed(Reason.UNSUPPORTED, 0, restartRaw = true, why = e.message) }
            catch (e: RuntimeException) { job.muxDirty = true; throw Failed(Reason.UNSUPPORTED, 0, restartRaw = true, why = e.toString()) }
            job.mux = null
        }
        sink.sync()
        // a picture whose sound is a job of its own waits for it before anything is published
        val comp: Job? = synchronized(lock) { if (rec.companionId != 0L) jobs[rec.companionId] else null }
        if (comp != null) awaitCompanion(job, comp)
        val uri = sink.publish()
        synchronized(lock) { rec.destUri = uri ?: rec.destUri; job.sink = null }
        if (mux != null && comp == null && finisher != null && uri != null) {                // the MP4 was written by our own code: ask Android's parser whether it opens it (recorded in Diagnostics; the row says so when it does not)
            val problem = try { finisher.verify(uri) } catch (e: Throwable) { null }
            if (problem != null) synchronized(lock) { rec.note = listOfNotNull(rec.note, "Android's own video reader could not open the finished file ($problem); other players may still play it.").joinToString(" ") }
        }
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
                } else if (keepApartOnMergeFailure) synchronized(lock) { comp.rec.parentId = 0L; comp.rec.updated = clock.now() }.also { persist(comp.rec); changed(comp.rec.id) }
            }
        } else if (finisher != null && uri != null && media.container == "ts" && !job.stop && mux == null && synchronized(lock) { rec.mux } != MUX_MP4) {
            // MPEG-TS plays badly in most apps: make an MP4 of it (the .ts stays if anything goes wrong). Runs between discontinuities are handed over as parts; a stream that has
            // discontinuities but whose runs were not recorded (a download started by an older version) is left as it is, because its times cannot be laid end to end.
            val parts = partsOf(rec.breaks, media.segments, job.written)
            if (media.discontinuities == 0 || parts.size > 1) {
                val j = StreamFinisher.Joined(uri, rec.nameStem, media.container, job.written, rec.priv, rec.destKind, parts)
                converted = try { finisher.convert(j) } catch (e: Throwable) { log("convert failed", e); j.declined = "the conversion failed"; null }
                if (converted == null) synchronized(lock) { if (rec.note == null) rec.note = "Kept as a .ts file: ${j.declined ?: "it could not be converted to MP4"}." }   // the MP4 writer's own reason (set when it looked at the first segment) says more than the platform's
            } else synchronized(lock) { rec.note = "Kept as a .ts file: this download began before the MP4 writer existed and its parts were not recorded." }
            if (converted != null) try { sinks.delete(uri) } catch (e: Exception) { /* the .ts stays next to the .mp4: harmless */ }
        }
        if (comp != null && converted == null && !keepApartOnMergeFailure) {
            if (uri != null) try { sinks.delete(uri) } catch (e: Exception) { /* ignore */ }
            synchronized(lock) { rec.destUri = null }
            throw Failed(Reason.UNSUPPORTED, why = "picture and sound could not be made one file")           // the sound job goes with it (settle)
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
        dropJob(comp.rec.id)
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
                while (!job.stop && job.failure == null && synchronized(lock) { job.recovering } > 0) pause(job, 100) // another worker is asking the server again for a refused part: no new request meanwhile
                if (job.stop || job.failure != null) break
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
        } catch (e: MuxBroken) { synchronized(lock) { if (job.failure == null) job.failure = Failed(Reason.UNSUPPORTED, 0, restartRaw = true, why = e.cause?.toString() ?: e.message); job.stop = true; for (r in job.responses) try { r.abort() } catch (x: Exception) { /* ignore */ } }
        } catch (e: SinkException) { synchronized(lock) { if (job.failure == null) job.failure = Failed(e.reason); job.stop = true }
        } catch (e: Throwable) { synchronized(lock) { if (job.failure == null) job.failure = Failed(Reason.STORAGE); job.stop = true } }
    }

    /** The lowest segment nobody has and nobody is fetching: in order, so the output can grow as soon as the first ones are in. */
    private fun claim(job: Job, n: Int): Int? = synchronized(lock) {
        var i = job.rec.next
        while (i < n) { if (!job.staged.get(i) && !job.claimed.get(i) && !job.skipped.get(i)) { job.claimed.set(i); return i }; i++ }
        null
    }

    /** Append every staged segment from the output's position onward, in order. One worker does it at a time; the others carry on fetching. */
    private fun drain(job: Job, media: Hls.Media) {
        while (true) {
            if (!job.drain.tryLock()) return
            try { while (appendNext(job, media)) { /* keep going while the next one is there */ }; muxCheckpointIfDue(job) } finally { job.drain.unlock() }
            if (!nextReady(job, media)) return // else one arrived between the last look and the unlock
        }
    }

    /** MP4 mode: every few seconds (between two segments, under the drain lock) the output is synced and the journal gets a block, so a killed download carries on from here. */
    private fun muxCheckpointIfDue(job: Job) {
        val m = job.mux ?: return
        val now = clock.now()
        if (job.muxDirty || now - job.lastMuxCkpt < cfg.checkpointMs) return
        job.lastMuxCkpt = now
        try { m.checkpoint(synchronized(lock) { job.rec.next }) }
        catch (e: IOException) { throw SinkException(Reason.STORAGE, "the MP4 journal could not be written", e) }
    }

    private fun nextReady(job: Job, media: Hls.Media): Boolean = synchronized(lock) { !job.stop && job.rec.next < media.segments.size && (job.staged.get(job.rec.next) || job.skipped.get(job.rec.next)) }

    private fun appendNext(job: Job, media: Hls.Media): Boolean {
        val rec = job.rec
        val idx: Int
        synchronized(lock) {
            idx = rec.next
            if (job.stop || idx >= media.segments.size) return false
            if (!job.staged.get(idx)) {
                if (!job.skipped.get(idx)) return false
                rec.next = idx + 1; return true                          // never fetched: the MP4 writer leaves this run out, only the pointer moves
            }
        }
        val seg = media.segments[idx]
        val sink = job.sink ?: return false
        val mux = job.mux
        val f = File(store.dir(rec.id), "$idx.seg")
        val size = f.length()
        var written = synchronized(lock) { job.written }
        val startedAt = written
        // where the (decrypted) bytes go: into the output as they are, or through the MP4 writer
        val emit: (ByteArray, Int, Int) -> Unit = if (mux != null) { b, o, n -> mux.feed(b, o, n) } else { b, o, n -> sink.pwrite(written, b, o, n); written += n }
        try {
            mux?.beginSegment(idx, seg.discontinuity, runMs(media, idx))
            FileInputStream(f).use { input ->
                val buf = ByteArray(64 * 1024)
                var skip = prefixSkip(f, seg.key >= 0, media.container).toLong()
                while (skip > 0) { val k = input.skip(skip); if (k <= 0) break; skip -= k } // a picture header (or other garbage) in front of the part is not media
                if (seg.key >= 0) {
                    val key = keyBytes(job, media, seg.key)
                    val iv = media.keys[seg.key].iv ?: Hls.sequenceIv(seg.seq)
                    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
                    try { cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv)) } catch (e: Exception) { throw Failed(Reason.UNSUPPORTED) }
                    while (true) {
                        val r = input.read(buf); if (r < 0) break
                        val out = cipher.update(buf, 0, r) ?: continue
                        if (out.isNotEmpty()) emit(out, 0, out.size)
                    }
                    val tail = try { cipher.doFinal() } catch (e: Exception) { throw Failed(Reason.UNSUPPORTED) } // wrong key or damaged segment
                    if (tail.isNotEmpty()) emit(tail, 0, tail.size)
                } else {
                    while (true) { val r = input.read(buf); if (r < 0) break; emit(buf, 0, r) }
                }
            }
            if (mux != null) { mux.endSegment(); written = mux.position; if (mux.leavingRunOut) skipRestOfRun(job, media, idx) }
        } catch (e: IOException) { if (mux != null) job.muxDirty = true; throw SinkException(Reason.STORAGE, "could not read a staged segment", e) }
        catch (e: SinkException) { if (mux != null) job.muxDirty = true; throw e }
        catch (e: Failed) { if (mux != null) job.muxDirty = true; throw e }
        catch (e: Stopped) { if (mux != null) job.muxDirty = true; throw e }
        catch (e: RuntimeException) { if (mux != null) { job.muxDirty = true; throw MuxBroken("the MP4 writer failed in segment $idx", e) }; throw e }
        synchronized(lock) {
            job.written = written; rec.next = idx + 1; rec.appended = written; rec.crashes = 0
            if (mux == null && seg.discontinuity && idx > 0) rec.breaks += "$idx:$startedAt;"
            job.stagedBytes -= size; job.stagedCount--; job.staged.clear(idx)
        }
        f.delete()
        return true
    }

    /**
     * The MP4 writer judged the run [idx] belongs to (after its first segment) and is leaving it out: the rest of that run, up to the next #EXT-X-DISCONTINUITY, is not worth fetching. Segments that are
     * already being fetched or are staged are left alone (they are appended and thrown away as before); a download that is resumed after a kill fetches them again (the muxer's journal knows the run is out).
     */
    private fun skipRestOfRun(job: Job, media: Hls.Media, idx: Int) = synchronized(lock) {
        var i = idx + 1
        while (i < media.segments.size && !media.segments[i].discontinuity) { if (!job.staged.get(i) && !job.claimed.get(i)) job.skipped.set(i); i++ }
    }

    /** The playing time of the run that segment [idx] begins (it runs up to the next #EXT-X-DISCONTINUITY); 0 for a segment inside a run. */
    private fun runMs(media: Hls.Media, idx: Int): Long {
        if (idx != 0 && !media.segments[idx].discontinuity) return 0L
        var t = 0L; var i = idx
        do { t += media.segments[i].durationMs; i++ } while (i < media.segments.size && !media.segments[i].discontinuity)
        return t
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

    /** One part's run of refusals (EY-7, EY-8): how often it was asked for again, whether this worker holds the "asking again" place, and where the header ladder stands. */
    private class Refusals {
        var again = 0                      // refusals answered with a short wait and the same headers (rung L0)
        var holding = false                // this worker counts in [Job.recovering]: the others wait
        var rung = 0                       // the next set of the header ladder to try (rung L1)
        var laddered = false               // every set of the ladder was tried for this part
        var first: Map<String, String>? = null // the set in use when the ladder was entered (what a failed ladder goes back to)
        var mine: Map<String, String>? = null  // the set this worker switched to last
    }

    private fun fetchTo(job: Job, fetcher: Fetcher, url: String, from: Long, toInclusive: Long, dest: File, count: Boolean) {
        val ref = Refusals()
        try {
            fetchLoop(job, fetcher, url, from, toInclusive, dest, count, ref)
            val won = ref.mine
            if (won != null && job.rec.headers === won) { log("stream ${job.rec.id}: the part was refused with the first headers and came in with the next set of the ladder; it stays", null); persist(job.rec) }
        } catch (e: RuntimeException) {
            headersBack(job, ref) // whatever ended this part (a stop, another failure): a set that was only being tried does not stay
            throw e
        } finally { if (ref.holding) synchronized(lock) { job.recovering-- } }
    }

    private fun fetchLoop(job: Job, fetcher: Fetcher, url: String, from: Long, toInclusive: Long, dest: File, count: Boolean, ref: Refusals) {
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
            } catch (e: Denied) {
                if (count) job.inflight.addAndGet(-ctr[0])
                if (job.stop) throw Stopped()
                if (e.sent !== job.rec.headers) continue // another worker found headers that work while this request was on its way: ask again with them, nothing is used up
                if (!ref.holding) { ref.holding = true; synchronized(lock) { job.recovering++ } }
                // Two rungs, in the order that fits what is known. L1 (EY-8): the same part with the next set of the header ladder (a server that wants the player's address and was given the page's); the set that works stays
                // for the whole download. L0 (EY-7): a refused part is a hiccup (an edge that is not in sync, a token that allows few connections) until it has been refused again and again: ask again after a short, jittered
                // wait; every other worker waits meanwhile. With nothing of the download in yet the headers are the likelier cause (ladder first); once parts came in an expiry or a hiccup is (waits first).
                if (!partsFetched(job) && !ref.laddered) {
                    if (nextHeaders(job, ref)) continue
                    ref.laddered = true; headersBack(job, ref)
                }
                if (ref.again < cfg.denyDelaysMs.size) {
                    val base = cfg.denyDelaysMs[ref.again++]
                    pause(job, maxOf(base, e.retryAfterMs.coerceAtMost(30_000L)) + rnd.nextInt((base / 5).toInt() + 1))
                    continue
                }
                if (!ref.laddered) { if (nextHeaders(job, ref)) continue; ref.laddered = true }
                headersBack(job, ref)
                throw Failed(if (e.status == 404) Reason.HTTP else Reason.LINK_EXPIRED, e.status) // the last try is over: the link counts as expired
            } catch (e: IOException) {
                if (count) job.inflight.addAndGet(-ctr[0])
                if (job.stop) throw Stopped()
                if (e is java.io.FileNotFoundException || e.message?.contains("ENOSPC") == true || e.message?.contains("No space") == true) throw Failed(Reason.NO_SPACE)
                if (++tries >= MAX_TRIES) throw Failed(Reason.NETWORK)
                backoff(job, tries)
            }
        }
    }

    /** Bytes in front of a part's media that are not media (a tiny picture a server wrote before a transport stream, see [SegmentPrefix]); looks at the first 4 KB only. */
    private fun prefixSkip(f: File, encrypted: Boolean, container: String): Int = try {
        FileInputStream(f).use { i ->
            val b = ByteArray(SegmentPrefix.HEAD); var p = 0
            while (p < b.size) { val r = i.read(b, p, b.size - p); if (r < 0) break; p += r }
            if (encrypted) SegmentPrefix.imageLength(b, p, ts = false) else SegmentPrefix.length(b, p, ts = container == "ts")
        }
    } catch (e: IOException) { 0 }

    private fun looksLikeHtmlFile(f: File): Boolean = try {
        FileInputStream(f).use { i -> val b = ByteArray(512); val n = i.read(b); n > 0 && app.kestrel.core.download.ContentSniff.looksLikeHtml(b, 0, n) }
    } catch (e: IOException) { false }

    private class Retry(val status: Int, val retryAfterMs: Long = 0) : RuntimeException(null, null, false, false)

    /** A refused part: 401 / 403 / 410, and 404 once other parts of this download came in (the address was right, the edge or the token is not). */
    private class Denied(val status: Int, val sent: Map<String, String>, val retryAfterMs: Long = 0) : RuntimeException(null, null, false, false)

    /** After a ladder that found nothing: back to the set the download started with (unless somebody else has found a working one meanwhile). */
    private fun headersBack(job: Job, ref: Refusals) {
        val back = ref.first ?: return
        synchronized(lock) { if (job.rec.headers === ref.mine) job.rec.headers = back }
    }

    /** Switches the download to the next set of its header ladder that has not been tried for this part (the one in use was just refused). False when the ladder is used up. */
    private fun nextHeaders(job: Job, ref: Refusals): Boolean = synchronized(lock) {
        val sets = job.rec.headerSets
        if (ref.first == null) ref.first = job.rec.headers
        while (ref.rung < sets.size) {
            val set = sets[ref.rung++]
            if (set == ref.first || set == job.rec.headers) continue
            ref.mine = set; job.rec.headers = set
            return true
        }
        false
    }

    private fun backoff(job: Job, tries: Int, minMs: Long = 0) = pause(job, maxOf(minMs, minOf(cfg.backoffCapMs, cfg.backoffBaseMs shl minOf(tries - 1, 10))) + rnd.nextInt(250))

    /** Sleeps [ms] (looking at [Job.stop] every 100 ms); throws [Stopped] when the job was stopped meanwhile. */
    private fun pause(job: Job, ms: Long) {
        val end = clock.now() + ms
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
            val sent = job.rec.headers
            val resp = open(job, fetcher, FetchRequest(url, rf, rt, null, sent, priv = job.rec.priv))
            var got = 0L; var total = -1L; var whole = false
            try {
                val st = resp.status
                if (st == 416 && pos > 0) return          // asked past the end: the segment is whole
                if (st != 200 && st != 206) throw partFailure(job, st, resp, sent)
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
                    limiter.acquire(r) { job.stop }                // Settings > Downloads > Speed limit
                    if (job.stop) throw Stopped()
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

    /** The answer to a PART of a stream: refusals are tried again first ([Denied]); everything else is judged as before ([statusFailure]). */
    private fun partFailure(job: Job, status: Int, resp: FetchResponse?, sent: Map<String, String>): RuntimeException =
        if (status == 401 || status == 403 || status == 410 || (status == 404 && partsFetched(job)))
            Denied(status, sent, resp?.header("Retry-After")?.trim()?.toLongOrNull()?.times(1000) ?: 0)
        else statusFailure(status, resp)

    /** True once any part of this download came in (in this run or an earlier one): a refusal after that is more likely an edge hiccup or an expiry than a wrong address. */
    private fun partsFetched(job: Job): Boolean = synchronized(lock) { job.stagedCount > 0 || job.rec.next > 0 || job.written > 0 }

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
            // the average size of the segments that are COMPLETE (appended or staged) times the number of segments: bytes of half-fetched segments would make the guess jump up and down
            media != null && n > 0 && have >= 2 -> { total = ((job.written + job.stagedBytes).toDouble() / have * n).toLong(); estimated = true }
            r.estimate > 0 -> { total = r.estimate; estimated = true }
            else -> total = -1
        }
        if (total in 0 until fetched) total = fetched
        val speed = if (running) job.meter.bytesPerSec(now).toLong() else 0L
        val eta = if (running && speed > 0 && total > 0) (total - fetched).coerceAtLeast(0) / speed else -1L
        val st = if (job.converting) DlState.VERIFYING else r.state
        val actions: List<Action> = when (st) {
            DlState.QUEUED, DlState.STARTING -> listOf(Action.CANCEL)
            DlState.RUNNING, DlState.WAITING_NETWORK -> listOf(Action.PAUSE, Action.CANCEL)
            DlState.PAUSED_USER, DlState.PAUSED_SYSTEM -> listOf(Action.RESUME, Action.CANCEL)
            DlState.NEEDS_DECISION -> listOf(Action.RESTART, Action.CANCEL)
            DlState.VERIFYING -> emptyList()
            DlState.FAILED -> listOf(Action.RETRY, Action.DELETE)
            DlState.COMPLETED -> if (r.mux != MUX_MP4 && r.name.endsWith(".ts", true) && r.destUri != null && r.companionId == 0L && r.parentId == 0L) listOf(Action.OPEN, Action.SHARE, Action.MAKE_MP4, Action.DELETE) else listOf(Action.OPEN, Action.SHARE, Action.DELETE)
            DlState.CANCELED -> emptyList()
        }
        val host = try { java.net.URI(r.url).host } catch (e: Exception) { null }
        val comp = if (r.companionId != 0L && r.state != DlState.COMPLETED) jobs[r.companionId] else null
        if (comp != null) { // one row for picture + sound: sizes, speed and connections add up
            val cv = view(comp)
            val sumTotal = if (total >= 0 && cv.total >= 0) total + cv.total else -1L
            return DownloadView(r.id, r.state, r.reason, r.detail, r.name, r.mime, host, sumTotal, fetched + cv.done, speed + cv.bytesPerSec,
                if (running && speed + cv.bytesPerSec > 0 && sumTotal > 0) (sumTotal - fetched - cv.done).coerceAtLeast(0) / (speed + cv.bytesPerSec) else -1L, job.alive + cv.connections, r.priv, r.destUri, r.created, r.updated, actions, r.destKind, estimated || cv.estimated, r.note)
        }
        return DownloadView(r.id, st, r.reason, r.detail, r.name, r.mime, host, total, fetched, speed, eta, job.alive, r.priv, r.destUri, r.created, r.updated, actions, r.destKind, estimated, r.note)
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
