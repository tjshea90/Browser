package app.kestrel.core.courier

import app.kestrel.core.download.Action
import app.kestrel.core.download.Clock
import app.kestrel.core.download.DEST_PUBLIC
import app.kestrel.core.download.DlState
import app.kestrel.core.download.DownloadView
import app.kestrel.core.download.Reason
import app.kestrel.core.download.Source
import java.io.File
import java.io.IOException
import java.net.URL
import java.security.SecureRandom
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

enum class PhoneMode {
    OFF,
    /** Use the phone when it answers; otherwise the tablet downloads by itself. */
    WHEN_THERE,
    /** Wait for the phone; never use the tablet's own connection while it is away (a phone that refuses a download still falls back). */
    ALWAYS,
}

/** The tablet's own download engine, as the phone backend needs it. */
interface LocalDownloads {
    /** Starts a download on the tablet itself and returns its id in the tablet's engine. */
    fun enqueue(src: Source): Long
    fun view(id: Long): DownloadView?
}

/** `Source.fetcher` value of the engine's fetcher that reads files from the phone ([LanFetcher]). */
const val FETCHER_LAN = 2

class PhoneDiag(val reachable: Boolean, val lastHelloAt: Long, val rttMs: Long, val skewMs: Long, val lastError: String?, val activeJobs: Int)

/**
 * The tablet side of "download on my phone" (research/PHONE_OFFLOAD.md sections 3, 7, 8). A download the owner starts becomes a row at once (a placeholder with its own id);
 * a background thread asks the phone to do it, mirrors the phone's live state into that row (same bar, speed, buttons), and when the phone has the whole file the tablet's
 * ordinary download engine copies it over the LAN ([LocalDownloads.enqueue] with [FETCHER_LAN]): pause, resume and kill-safety come from that engine. From then on the engine's
 * row is the download. Whenever the phone cannot or will not do it, the tablet downloads it itself with the original request and the row says why.
 *
 * Network calls happen only on the backend thread; the methods the UI calls just record what was asked and wake it.
 */
class PhoneBackend(
    private val clientFor: () -> CourierClient?,
    private val local: LocalDownloads,
    private val stateFile: File,
    firstId: Long,
    private val modeOf: () -> PhoneMode,
    private val onChange: () -> Unit = {},
    private val clock: Clock = Clock.System,
    private val rng: SecureRandom = SecureRandom(),
    private val log: (String) -> Unit = {},
) {
    companion object {
        const val DOWN_MS = 30_000L
        const val HELLO_EVERY_MS = 60_000L
        const val OFFER_LOCAL_AFTER_MS = 60_000L
        const val RETRY_MS = 3_000L
        /** The tablet's engine registers a new download a moment after enqueue() returns its id (on a slow tablet, noticeably later): until then a missing row is not "the owner removed it". */
        const val ROW_GRACE_MS = 20_000L
        /** Headers worth replaying on the phone (the phone filters again). */
        private val REPLAY = setOf("user-agent", "referer", "origin", "cookie", "accept", "accept-language")
    }

    private enum class Phase { PHONE, PULL, LOCAL, CANCELED, DONE }

    private class P(val lid: Long, val cid: String, val src: Source, val created: Long) {
        var phase = Phase.PHONE
        var remoteId: String? = null
        var engineId = 0L
        var last: RemoteJob? = null
        var note: String? = null
        var hidden = false
        var delRemote = false
        var unreachableSince = 0L
        var updated = created
        var nextTryAt = 0L
        var nextDelAt = 0L
        /** The tablet's engine row for [engineId] has been seen at least once. */
        var sawRow = false
    }

    private val lock = Any()
    private val items = LinkedHashMap<Long, P>()
    private var nextLid = firstId
    private val ops = ConcurrentLinkedQueue<() -> Unit>()
    private val kicks = Semaphore(0)
    @Volatile private var running = false
    private var thread: Thread? = null
    @Volatile private var downUntil = 0L
    private var lastClient: CourierClient? = null
    @Volatile private var lastHelloAt = 0L
    @Volatile private var rttMs = 0L
    @Volatile private var skewMs = 0L
    @Volatile private var lastError: String? = null
    @Volatile private var reachableNow = false

    // ---------------------------------------------------------------------------------------------------------------- lifecycle
    fun restore() { synchronized(lock) { load() }; kick() }

    fun start() {
        if (running) return
        running = true
        thread = Thread({ loop() }, "phone-backend").also { it.isDaemon = true; it.start() }
    }

    fun stop() { running = false; kick(); try { thread?.join(3000) } catch (_: InterruptedException) {}; thread = null }

    private fun kick() { kicks.release() }

    // ---------------------------------------------------------------------------------------------------------------- what the controller asks
    /** Takes a download over if the phone should do it: returns the row's id, or null (the caller then downloads it on the tablet). */
    fun offload(src: Source): Long? {
        val mode = modeOf()
        if (mode == PhoneMode.OFF || src.priv || src.destKind != DEST_PUBLIC) return null
        val u = try { URL(src.url) } catch (e: Exception) { return null }
        if (u.protocol != "http" && u.protocol != "https") return null
        if (clientFor() == null) return null
        val p: P
        synchronized(lock) {
            if (mode == PhoneMode.WHEN_THERE && clock.now() < downUntil) return null                  // known to be away a moment ago: no flash of a row that would fall back
            val lid = nextLid++
            p = P(lid, newCid(), src, clock.now())
            p.note = "Asking your phone"
            items[lid] = p
            save()
        }
        kick(); onChange()
        return p.lid
    }

    fun owns(id: Long): Boolean = synchronized(lock) { items[id]?.let { visible(it, clock.now()) } == true }

    fun views(): List<DownloadView> = synchronized(lock) { val now = clock.now(); items.values.filter { visible(it, now) }.map { view(it) } }

    fun view(id: Long): DownloadView? = synchronized(lock) { items[id]?.takeIf { visible(it, clock.now()) }?.let { view(it) } }

    /** Which records are a row of their own: the placeholder while the phone works (and when canceled), and for a moment after the hand-over until the tablet's engine row appears (no gap, no double row). */
    private fun visible(p: P, now: Long): Boolean = !p.hidden && when (p.phase) {
        Phase.PHONE, Phase.CANCELED -> true
        Phase.PULL, Phase.LOCAL -> !p.sawRow && now - p.updated < ROW_GRACE_MS && local.view(p.engineId) == null
        Phase.DONE -> false
    }

    fun pause(id: Long) = remoteOp(id, "pause")
    fun resume(id: Long) = remoteOp(id, "resume")

    /** "Download here" / "Restart" on a row of the phone: download it on the tablet instead when the phone is away or gave up, otherwise start it again on the phone. */
    fun restart(id: Long) {
        val p = synchronized(lock) { items[id] } ?: return
        val away = synchronized(lock) { p.unreachableSince > 0 || p.remoteId == null }
        if (away) { ops.add { fallback(p, "you chose to download it here") }; kick() } else remoteOp(id, "restart")
    }

    fun cancel(id: Long) {
        val p = synchronized(lock) { items[id]?.takeIf { it.phase == Phase.PHONE } ?: return }
        synchronized(lock) { p.phase = Phase.CANCELED; p.updated = clock.now(); p.delRemote = p.remoteId != null; save() }
        val rid = p.remoteId
        if (rid != null) ops.add { clientFor()?.let { c -> try { c.action(rid, "cancel") } catch (e: CourierException) { note(e) } } }
        kick(); onChange()
    }

    /** Removes the row (and the phone's copy of the job). */
    fun remove(id: Long) {
        val p = synchronized(lock) { items[id] } ?: return
        synchronized(lock) { if (p.phase == Phase.PHONE) p.phase = Phase.CANCELED; p.hidden = true; p.delRemote = p.remoteId != null; save() }
        kick(); onChange()
    }

    fun clearFinished() {
        synchronized(lock) { for (p in items.values) if (p.phase == Phase.CANCELED) { p.hidden = true; p.delRemote = p.remoteId != null }; save() }
        kick(); onChange()
    }

    /** A note for a row of the tablet's own engine that came from a phone download (copying, or the fallback reason); null for every other row. */
    fun noteFor(engineId: Long): String? = synchronized(lock) { items.values.firstOrNull { it.engineId == engineId && it.phase != Phase.PHONE && it.phase != Phase.CANCELED }?.note }

    /** The site the owner downloaded from (the engine's own row only knows the phone's address). */
    fun originHostFor(engineId: Long): String? = synchronized(lock) { items.values.firstOrNull { it.engineId == engineId && it.phase != Phase.PHONE && it.phase != Phase.CANCELED }?.let { hostOf(it.src.url) } }

    /** When the owner started that download (the engine's own row is younger). */
    fun createdFor(engineId: Long): Long? = synchronized(lock) { items.values.firstOrNull { it.engineId == engineId && it.phase != Phase.PHONE && it.phase != Phase.CANCELED }?.created }

    fun needsService(): Boolean = synchronized(lock) { items.values.any { (it.phase == Phase.PHONE) || (it.phase == Phase.PULL) || it.delRemote } }

    fun reachable(): Boolean = reachableNow

    fun diag(): PhoneDiag = PhoneDiag(reachableNow, lastHelloAt, rttMs, skewMs, lastError, synchronized(lock) { items.values.count { it.phase == Phase.PHONE } })

    /** The network changed (the hotspot came or went): look again now instead of waiting for the next round. */
    fun onNetwork() { downUntil = 0L; lastHelloAt = 0L; kick() }                  // a new network: whoever answers there must prove the pairing code again before it gets a URL

    private fun remoteOp(id: Long, action: String) {
        val rid = synchronized(lock) { items[id]?.takeIf { it.phase == Phase.PHONE }?.remoteId } ?: return
        ops.add { val c = clientFor(); if (c != null) try { c.action(rid, action) } catch (e: CourierException) { note(e) } }
        kick()
    }

    // ---------------------------------------------------------------------------------------------------------------- the backend thread
    private fun loop() {
        while (running) {
            try { step() } catch (e: Exception) { log("phone backend: ${e.javaClass.simpleName}: ${e.message}") }
            val wait = synchronized(lock) { if (items.values.any { it.phase == Phase.PHONE || it.phase == Phase.PULL || it.delRemote }) 1000L else 30_000L }
            try { if (kicks.tryAcquire(wait, TimeUnit.MILLISECONDS)) kicks.drainPermits() } catch (_: InterruptedException) { return }
        }
    }

    internal fun step() {
        while (true) { val op = ops.poll() ?: break; try { op() } catch (e: Exception) { log("phone op failed: ${e.javaClass.simpleName}") } }
        val work = synchronized(lock) { items.values.toList() }
        if (work.isEmpty()) return
        for (p in work) if (synchronized(lock) { p.phase == Phase.PHONE && p.remoteId == null && clock.now() >= p.nextTryAt }) createRemote(p)
        poll()
        for (p in work) {
            val phase = synchronized(lock) { p.phase }
            if (phase == Phase.PULL) checkPull(p)
            else if (phase == Phase.LOCAL || phase == Phase.DONE) {
                if (local.view(p.engineId) != null) p.sawRow = true
                else if (rowGone(p) && !synchronized(lock) { p.delRemote }) forget(p)
            }
        }
        for (p in work) if (synchronized(lock) { p.delRemote }) tryDelete(p)
        for (p in work) if (synchronized(lock) { p.hidden && !p.delRemote }) forget(p)
        onChange()
    }

    private fun forget(p: P) { synchronized(lock) { items.remove(p.lid); save() } }

    private fun ensureHello(c: CourierClient) {
        val now = clock.now()
        if (c === lastClient && now - lastHelloAt < HELLO_EVERY_MS) return
        try {
            rttMs = c.hello(); skewMs = c.skewMs; lastClient = c; lastHelloAt = now; lastError = null
        } catch (e: CourierException) { lastClient = null; throw e }
    }

    private fun note(e: CourierException) { lastError = e.kind.name.lowercase() }

    private fun createRemote(p: P) {
        val mode = modeOf()
        val c = clientFor()
        if (c == null) { if (mode == PhoneMode.ALWAYS) waiting(p, "Waiting for your phone") else fallback(p, "no phone is paired"); return }
        try {
            ensureHello(c)
            val spec = JobSpec(p.cid, p.src.url, p.src.nameHint, p.src.nameFixed, p.src.mime, -1L, p.src.headers.filter { it.key.lowercase() in REPLAY })
            val r = c.create(spec)
            synchronized(lock) { p.remoteId = r.id; p.last = r; p.unreachableSince = 0; p.note = null; p.updated = clock.now(); save() }
            reachableNow = true
        } catch (e: CourierException) {
            note(e)
            when (e.kind) {
                CourierException.Kind.UNREACHABLE, CourierException.Kind.TIMEOUT -> {
                    reachableNow = false; downUntil = clock.now() + DOWN_MS
                    if (mode == PhoneMode.ALWAYS) waiting(p, "Waiting for your phone") else fallback(p, "your phone is not reachable")
                }
                else -> fallback(p, reasonText(e))
            }
        }
    }

    private fun waiting(p: P, text: String) { synchronized(lock) { if (p.unreachableSince == 0L) p.unreachableSince = clock.now(); p.note = text; p.nextTryAt = clock.now() + RETRY_MS } }

    private fun poll() {
        val active = synchronized(lock) { items.values.filter { it.phase == Phase.PHONE && it.remoteId != null } }
        if (active.isEmpty()) return
        val c = clientFor()
        if (c == null) { away(active, "no phone is paired"); return }
        val res = try { ensureHello(c); c.jobs(active.map { it.remoteId!! }) } catch (e: CourierException) { note(e); away(active, e.kind.name); return }
        reachableNow = true
        val now = clock.now()
        for (r in res) {
            val p = active.firstOrNull { it.remoteId == r.id } ?: continue
            synchronized(lock) { p.last = r; p.unreachableSince = 0; p.updated = now; if (p.phase == Phase.PHONE) p.note = null }
            when (r.state) {
                "COMPLETED" -> startPull(p, r, c)
                "FAILED" -> fallback(p, failedText(r))
                "GONE" -> fallback(p, "your phone lost the download")
                "CANCELED" -> synchronized(lock) { p.phase = Phase.CANCELED; p.delRemote = true; save() }                // stopped on the phone itself
            }
        }
    }

    private fun away(list: List<P>, why: String) {
        reachableNow = false; downUntil = clock.now() + DOWN_MS
        val now = clock.now()
        synchronized(lock) { for (p in list) if (p.unreachableSince == 0L) p.unreachableSince = now }
        log("phone away: $why")
    }

    private fun startPull(p: P, r: RemoteJob, c: CourierClient) {
        val path = r.fileUrl
        if (path == null) { fallback(p, "your phone could not hand over the file"); return }
        val name = if (p.src.nameFixed && p.src.nameHint != null) p.src.nameHint else (r.file ?: p.src.nameHint ?: r.name)
        val src = Source(c.baseUrl + path, p.src.pageUrl, name, r.mime ?: p.src.mime, emptyMap(), FETCHER_LAN, false, true, 0, DEST_PUBLIC, true)
        val id = local.enqueue(src)
        synchronized(lock) { p.phase = Phase.PULL; p.engineId = id; p.note = "Copied from your phone"; p.updated = clock.now(); save() }
    }

    /** The engine row is missing and was either seen before (the owner removed it) or has had time enough to appear. */
    private fun rowGone(p: P): Boolean = synchronized(lock) { p.sawRow || clock.now() - p.updated > ROW_GRACE_MS }

    private fun checkPull(p: P) {
        val v = local.view(p.engineId)
        if (v != null) p.sawRow = true
        val retry = v != null && v.state == DlState.FAILED && ((v.reason == Reason.HTTP && (v.detail == 404 || v.detail == 410)) || v.reason == Reason.LINK_EXPIRED)
        if (retry) { fallback(p, "your phone no longer has the file"); return }
        if ((v == null && rowGone(p)) || v?.state == DlState.COMPLETED) synchronized(lock) { p.phase = Phase.DONE; p.delRemote = p.remoteId != null; p.updated = clock.now(); save() }
    }

    private fun tryDelete(p: P) {
        val now = clock.now()
        val rid = synchronized(lock) { if (now < p.nextDelAt) null else p.remoteId } ?: return
        val c = clientFor() ?: return
        try {
            c.delete(rid)
            synchronized(lock) { p.delRemote = false; p.remoteId = null; save() }
        } catch (e: CourierException) {
            if (e.kind == CourierException.Kind.NOT_FOUND) synchronized(lock) { p.delRemote = false; p.remoteId = null; save() }
            else synchronized(lock) { p.nextDelAt = now + 15_000 }
        }
    }

    /** The tablet downloads it itself, with the request the owner made. */
    private fun fallback(p: P, why: String) {
        if (synchronized(lock) { p.phase != Phase.PHONE && p.phase != Phase.PULL }) return
        val id = local.enqueue(p.src)
        synchronized(lock) { p.phase = Phase.LOCAL; p.engineId = id; p.note = "Downloading here: $why"; p.delRemote = p.remoteId != null; p.last = null; p.updated = clock.now(); save() }
        onChange()
    }

    // ---------------------------------------------------------------------------------------------------------------- wording
    private fun reasonText(e: CourierException): String = when (e.kind) {
        CourierException.Kind.LOCAL_NET -> "that link points into a private network"
        CourierException.Kind.NO_SPACE -> "your phone is short of space"
        CourierException.Kind.UNSUPPORTED -> "your phone's Courier cannot do this kind of download yet"
        CourierException.Kind.AUTH, CourierException.Kind.IMPOSTOR -> "the pairing code does not match"
        CourierException.Kind.TOO_MANY -> "your phone has too many downloads"
        CourierException.Kind.THROTTLED -> "your phone is busy"
        else -> "your phone could not take it"
    }

    private fun failedText(r: RemoteJob): String = when (Reason.valueOf(r.reason.takeIf { x -> Reason.values().any { it.name == x } } ?: "NONE")) {
        Reason.HTTP -> "the website refused your phone"
        Reason.LINK_EXPIRED -> "the link had expired when your phone tried it"
        Reason.NO_SPACE, Reason.STORAGE, Reason.FS_LIMIT -> "your phone ran out of space"
        Reason.BAD_CONTENT -> "the website sent a page instead of the file"
        Reason.NETWORK, Reason.STALLED -> "your phone lost its connection"
        Reason.TLS -> "your phone could not make a secure connection"
        else -> "your phone could not download it"
    }

    // ---------------------------------------------------------------------------------------------------------------- the row
    private fun view(p: P): DownloadView {
        val now = clock.now()
        if (p.phase == Phase.PULL || p.phase == Phase.LOCAL) {                      // the hand-over moment: the engine row is about to appear
            val name = p.src.nameHint ?: p.last?.file?.takeIf { it.isNotEmpty() } ?: lastSegment(p.src.url)
            val total = p.last?.total ?: -1L
            return DownloadView(p.lid, if (p.phase == Phase.PULL) DlState.RUNNING else DlState.STARTING, Reason.NONE, 0, name, p.src.mime, hostOf(p.src.url), total, if (p.phase == Phase.PULL) maxOf(total, 0L) else 0L,
                0L, -1L, 0, false, null, p.created, p.updated, emptyList(), DEST_PUBLIC, false, p.note)
        }
        val r = p.last
        val away = p.unreachableSince > 0 && p.phase == Phase.PHONE
        var state: DlState
        var noteText: String?
        if (p.phase == Phase.CANCELED) { state = DlState.CANCELED; noteText = null }
        else if (away) {
            state = DlState.WAITING_NETWORK
            noteText = p.note ?: "Can't reach your phone"
        } else if (r == null) { state = DlState.STARTING; noteText = p.note ?: "Asking your phone" }
        else {
            state = when (r.state) {
                "QUEUED" -> DlState.QUEUED; "STARTING" -> DlState.STARTING; "PAUSED_USER" -> DlState.PAUSED_USER; "PAUSED_SYSTEM" -> DlState.PAUSED_SYSTEM
                "WAITING_NETWORK" -> DlState.WAITING_NETWORK; "NEEDS_DECISION" -> DlState.NEEDS_DECISION; "VERIFYING" -> DlState.VERIFYING
                else -> DlState.RUNNING                                                                                      // RUNNING, and COMPLETED for the moment before the copy starts
            }
            noteText = "On your phone" + (r.note?.let { ": $it" } ?: "")
        }
        val total = r?.total ?: -1L
        val done = if (r?.state == "COMPLETED") maxOf(total, 0L) else (r?.done ?: 0L)
        val acts = ArrayList<Action>()
        if (p.phase == Phase.CANCELED) acts.add(Action.DELETE)
        else {
            for (a in r?.actions ?: emptyList()) when (a) { "PAUSE" -> acts.add(Action.PAUSE); "RESUME" -> acts.add(Action.RESUME); "RETRY" -> acts.add(Action.RETRY) }
            acts.add(Action.CANCEL)
            if (away && now - p.unreachableSince >= OFFER_LOCAL_AFTER_MS) acts.add(Action.DOWNLOAD_HERE)
        }
        val name = p.src.nameHint ?: r?.file?.takeIf { it.isNotEmpty() } ?: r?.name?.takeIf { it.isNotEmpty() } ?: lastSegment(p.src.url)
        return DownloadView(
            p.lid, state, Reason.NONE, 0, name, r?.mime ?: p.src.mime, hostOf(p.src.url), total, done, if (state == DlState.RUNNING) (r?.bps ?: 0L) else 0L,
            if (state == DlState.RUNNING) (r?.etaSec ?: -1L) else -1L, r?.conns ?: 0, false, null, p.created, p.updated, acts, DEST_PUBLIC, r?.estimated ?: false, noteText,
        )
    }

    private fun hostOf(url: String): String? = try { URL(url).host } catch (e: Exception) { null }
    private fun lastSegment(url: String): String = try { URL(url).path.substringAfterLast('/').ifEmpty { "download" } } catch (e: Exception) { "download" }

    private fun newCid(): String { val b = ByteArray(12); rng.nextBytes(b); return "t-" + Sign.b64(b) }

    // ---------------------------------------------------------------------------------------------------------------- persistence
    private fun save() {
        val list = items.values.map { p ->
            linkedMapOf<String, Any?>(
                "lid" to p.lid, "cid" to p.cid, "phase" to p.phase.name, "remoteId" to p.remoteId, "engineId" to p.engineId, "created" to p.created, "updated" to p.updated,
                "note" to p.note, "hidden" to p.hidden, "del" to p.delRemote,
                "src" to linkedMapOf<String, Any?>(
                    "url" to p.src.url, "pageUrl" to p.src.pageUrl, "nameHint" to p.src.nameHint, "mime" to p.src.mime, "headers" to LinkedHashMap<String, Any?>(p.src.headers),
                    "fetcher" to p.src.fetcher, "nameFixed" to p.src.nameFixed, "meteredOk" to p.src.meteredOk,
                ),
                "last" to p.last?.let { r -> linkedMapOf<String, Any?>("id" to r.id, "state" to r.state, "name" to r.name, "file" to r.file, "mime" to r.mime, "total" to r.total, "done" to r.done, "conns" to r.conns) },
            )
        }
        try { AtomicFiles.write(stateFile, Json.write(mapOf("v" to 1, "next" to nextLid, "items" to list))) } catch (e: IOException) { log("phone state write failed: ${e.message}") }
    }

    private fun load() {
        items.clear()
        for (text in AtomicFiles.candidates(stateFile)) {
            try {
                val root = Json.parse(text) as? Map<*, *> ?: continue
                nextLid = maxOf(nextLid, root.long("next") ?: 0L)
                for (x in root.list("items") ?: emptyList<Any?>()) {
                    val m = x as? Map<*, *> ?: continue
                    val s = m.obj("src") ?: continue
                    val h = LinkedHashMap<String, String>()
                    s.obj("headers")?.forEach { (k, v) -> if (k is String && v is String) h[k] = v }
                    val src = Source(s.str("url") ?: continue, s.str("pageUrl"), s.str("nameHint"), s.str("mime"), h, s.int("fetcher") ?: 0, false, s.bool("meteredOk") ?: false, 0, DEST_PUBLIC, s.bool("nameFixed") ?: false)
                    val p = P(m.long("lid") ?: continue, m.str("cid") ?: continue, src, m.long("created") ?: 0L)
                    p.phase = try { Phase.valueOf(m.str("phase") ?: "PHONE") } catch (e: IllegalArgumentException) { Phase.PHONE }
                    p.remoteId = m.str("remoteId"); p.engineId = m.long("engineId") ?: 0L; p.updated = m.long("updated") ?: p.created
                    p.note = m.str("note"); p.hidden = m.bool("hidden") ?: false; p.delRemote = m.bool("del") ?: false
                    m.obj("last")?.let { l ->
                        p.last = RemoteJob(l.str("id") ?: "", p.cid, "file", l.str("state") ?: "RUNNING", "NONE", 0, l.str("name") ?: "", l.str("file"), l.str("mime"), l.long("total") ?: -1L, l.long("done") ?: 0L, 0L, -1L,
                            l.int("conns") ?: 0, false, null, emptyList(), p.created, p.updated, null, -1L)
                    }
                    if (p.phase == Phase.PHONE) p.unreachableSince = 0L
                    items[p.lid] = p
                }
                break
            } catch (e: Exception) { log("phone state unreadable: ${e.javaClass.simpleName}"); items.clear() }
        }
    }
}
