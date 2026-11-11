package app.kestrel.core.download

import java.io.Closeable
import java.io.InputStream

/** Where a download is in its life (persisted as [code]). Design: research/DOWNLOADS.md section 4. */
enum class DlState(val code: Int) {
    QUEUED(0), STARTING(1), RUNNING(2), PAUSED_USER(3), PAUSED_SYSTEM(4), WAITING_NETWORK(5), NEEDS_DECISION(6), VERIFYING(7),
    COMPLETED(8), FAILED(9), CANCELED(10);

    val terminal: Boolean get() = this == COMPLETED || this == FAILED || this == CANCELED
    /** Workers are (or may be) moving bytes. */
    val active: Boolean get() = this == STARTING || this == RUNNING || this == VERIFYING

    companion object { fun of(code: Int): DlState = values().firstOrNull { it.code == code } ?: FAILED }
}

/** Why a download is failed / needs a decision (persisted as [code]; HTTP statuses go in `detail`). */
enum class Reason(val code: Int) {
    NONE(0), HTTP(1), LINK_EXPIRED(2), NO_SPACE(3), STORAGE(4), FS_LIMIT(5), INSECURE_REDIRECT(6), TLS(7), LOCAL_NET(8), DIGEST(9),
    STALLED(10), FILE_CHANGED(11), NO_RANGE(12), NETWORK(13), BAD_URL(14),
    /** Streams (video): a live stream, copy protection (DRM), a format that cannot be saved, a playlist that could not be read. */
    LIVE(15), DRM(16), UNSUPPORTED(17), BAD_PLAYLIST(18);

    companion object { fun of(code: Int): Reason = values().firstOrNull { it.code == code } ?: NONE }
}

/**
 * A byte range of the file and how far it is written: [start, end) with a durable [cursor]. end = -1: open-ended (length unknown).
 * Invariants (tested): segments are disjoint, their union is [0, total), start <= cursor <= end.
 */
class Seg(@JvmField val idx: Int, @JvmField var start: Long, @JvmField var end: Long, @JvmField var cursor: Long) {
    // ---- runtime only (never persisted)
    @JvmField var worker = false      // a worker owns this segment right now
    @JvmField var reqEnd = -1L        // exclusive end of the request in flight; -1 = open-ended reply
    @JvmField var live = false        // the first reply (not a Range request) feeds this segment
    @JvmField var fails = 0           // consecutive failures without progress

    val done: Boolean get() = end >= 0 && cursor >= end
    fun copy() = Seg(idx, start, end, cursor)
}

/** One download as stored. All fields are guarded by the engine's lock; the Android layer only ever sees [DownloadView]. */
class Rec(@JvmField val id: Long) {
    @JvmField var state = DlState.QUEUED
    @JvmField var reason = Reason.NONE
    @JvmField var detail = 0                  // HTTP status or errno-like detail for the reason
    @JvmField var prio = 0
    @JvmField var url = ""                    // as clicked
    @JvmField var finalUrl: String? = null    // after redirects
    @JvmField var finalAt = 0L
    @JvmField var pageUrl: String? = null
    @JvmField var headers: Map<String, String> = emptyMap() // request headers to replay (User-Agent, Referer origin, Cookie for the URLConnection fetcher); scrubbed on terminal states
    @JvmField var name = ""
    @JvmField var nameFixed = false          // the caller's name wins over Content-Disposition / the URL (videos: "Page title - 720p.mp4")
    @JvmField var mime: String? = null
    @JvmField var destKind = 0                // 0 MediaStore, 1 SAF, 2 staged file
    @JvmField var destUri: String? = null
    @JvmField var total = -1L
    @JvmField var etag: String? = null        // strong ETag only
    @JvmField var lastMod: String? = null     // strong Last-Modified only
    @JvmField var dateHdr: String? = null
    @JvmField var ranges = -1                 // -1 unknown, 0 no, 1 yes
    @JvmField var fetcher = 0                 // which Fetcher implementation (0 URLConnection, 1 Gecko)
    @JvmField var risk = 0
    @JvmField var meteredOk = false
    @JvmField var done = 0L
    @JvmField var fails = 0
    @JvmField var connsPeak = 0
    @JvmField var priv = false                // private tab: never written to the store, generic notification
    @JvmField var created = 0L
    @JvmField var updated = 0L
    @JvmField var lastProgress = 0L
    @JvmField var finished = 0L
    @JvmField val segs = ArrayList<Seg>()

    /** A strong validator exists, so a partial file can be continued without risking a spliced, corrupt file. */
    val hasValidator: Boolean get() = etag != null || lastMod != null
    /** Pause / resume makes sense: the server did not say no to ranges, the length is known, a validator exists. */
    val resumable: Boolean get() = ranges != 0 && total > 0 && hasValidator

    fun snapshot(): Rec {
        val r = Rec(id)
        r.state = state; r.reason = reason; r.detail = detail; r.prio = prio; r.url = url; r.finalUrl = finalUrl; r.finalAt = finalAt; r.pageUrl = pageUrl
        r.headers = headers; r.name = name; r.nameFixed = nameFixed; r.mime = mime; r.destKind = destKind; r.destUri = destUri; r.total = total; r.etag = etag; r.lastMod = lastMod
        r.dateHdr = dateHdr; r.ranges = ranges; r.fetcher = fetcher; r.risk = risk; r.meteredOk = meteredOk; r.done = done; r.fails = fails; r.connsPeak = connsPeak
        r.priv = priv; r.created = created; r.updated = updated; r.lastProgress = lastProgress; r.finished = finished
        for (s in segs) r.segs.add(s.copy())
        return r
    }
}

/** What the caller hands to [Engine.enqueue]. */
class Source(
    val url: String,
    val pageUrl: String? = null,
    val nameHint: String? = null,
    val mime: String? = null,
    val headers: Map<String, String> = emptyMap(),
    val fetcher: Int = 0,
    val priv: Boolean = false,
    val meteredOk: Boolean = false,
    val risk: Int = 0,
    /** Where the file goes: [DEST_PUBLIC] = the public Downloads folder, [DEST_APP_PRIVATE] = the app's own staging folder (update APKs: not listed, not shown to other apps). */
    val destKind: Int = DEST_PUBLIC,
    /** True: [nameHint] is the file name, whatever the server or the URL says (the extension is still added from the content type if missing). */
    val nameFixed: Boolean = false,
)

const val DEST_PUBLIC = 0
const val DEST_APP_PRIVATE = 2

/** Buttons a row can show, first = primary. */
enum class Action { PAUSE, RESUME, CANCEL, RETRY, RESTART, OPEN, SHARE, DELETE }

/** Immutable snapshot for the UI and the notification (research/DOWNLOADS.md section 5 table). */
class DownloadView(
    val id: Long,
    val state: DlState,
    val reason: Reason,
    val detail: Int,
    val name: String,
    val mime: String?,
    val host: String?,
    val total: Long,
    val done: Long,
    val bytesPerSec: Long,
    val etaSec: Long,
    val connections: Int,
    val priv: Boolean,
    val destUri: String?,
    val created: Long,
    val updated: Long,
    val actions: List<Action>,
    /** [DEST_PUBLIC] or [DEST_APP_PRIVATE]. */
    val kind: Int = DEST_PUBLIC,
    /** [total] is a guess (a stream whose size is only known from its bit rate): shown as "about". */
    val estimated: Boolean = false,
) {
    val fraction: Float get() = if (total > 0) (done.toDouble() / total).coerceIn(0.0, 1.0).toFloat() else 0f
}

// ------------------------------------------------------------------------------------------------ ports (implemented by the Android layer; faked in tests)

interface Clock {
    fun now(): Long
    object System : Clock { override fun now() = java.lang.System.currentTimeMillis() }
}

sealed class Cond {
    class IfMatch(val etag: String) : Cond()
    class IfUnmodifiedSince(val date: String) : Cond()
}

/** One HTTP GET. [from] / [toInclusive] = -1 for "no Range header" / open end. */
class FetchRequest(
    val url: String,
    val from: Long,
    val toInclusive: Long,
    val cond: Cond?,
    val headers: Map<String, String>,
    val connectMs: Int = 15_000,
    val readMs: Int = 20_000,
    val priv: Boolean = false,
)

interface FetchResponse : Closeable {
    val status: Int
    val finalUrl: String
    fun header(name: String): String?
    val body: InputStream?
    /** Must unblock a thread that is reading [body] (that thread then gets an IOException). */
    fun abort()
}

class FetchException(val kind: Kind, message: String?, cause: Throwable? = null) : Exception(message, cause) {
    enum class Kind { TIMEOUT, RESET, OFFLINE, DNS, TLS, LOCAL_NET, PROTOCOL, INSECURE_REDIRECT, BAD_URL }
}

/** Blocking; called on worker threads. Follows redirects itself (<= 10, never https -> http) and reports the final URL. */
interface Fetcher {
    /** > 0: the first request asks for only this many bytes (a Range request) so the reply is bounded; the engine learns the total from Content-Range. 0: plain GET, streamed. */
    val probeWindow: Long get() = 0L
    /** > 0: bytes per Range request for this engine (0 = [Config.windowBytes]). Gecko buffers a whole reply in memory, so it asks for smaller windows. */
    val windowBytes: Long get() = 0L
    @Throws(FetchException::class) fun open(req: FetchRequest): FetchResponse
}

class SinkException(val reason: Reason, message: String?, cause: Throwable? = null) : Exception(message, cause)

/** The destination file. [pwrite] must be safe to call from several threads at different offsets. */
interface Sink {
    val uri: String?
    /** The name the file really got (the provider may have renamed it to avoid a clash); null = as requested. */
    val displayName: String? get() = null
    @Throws(SinkException::class) fun pwrite(pos: Long, b: ByteArray, off: Int, len: Int)
    @Throws(SinkException::class) fun sync()
    fun length(): Long
    /** Make the file visible (clear the pending flag); returns its final URI. */
    @Throws(SinkException::class) fun publish(): String?
    fun discard()
}

interface SinkFactory {
    @Throws(SinkException::class) fun create(name: String, mime: String?, total: Long, priv: Boolean): Sink
    /** [kind] = [DEST_PUBLIC] / [DEST_APP_PRIVATE]; factories that know only one place ignore it. */
    @Throws(SinkException::class) fun create(name: String, mime: String?, total: Long, priv: Boolean, kind: Int): Sink = create(name, mime, total, priv)
    /** Re-open a partial file after a restart; null = it is gone (the download starts again). */
    fun reopen(uri: String, total: Long): Sink?
    /** Delete a finished file (the Delete button). */
    fun delete(uri: String): Boolean
}

interface Store {
    fun loadAll(): List<Rec>
    fun insert(r: Rec)
    fun save(r: Rec)
    fun delete(id: Long)
}

interface NetGate {
    val online: Boolean
    val metered: Boolean
}

/** Starts worker threads (the Android layer lowers their priority). */
fun interface Spawner { fun spawn(name: String, task: Runnable): Thread }
