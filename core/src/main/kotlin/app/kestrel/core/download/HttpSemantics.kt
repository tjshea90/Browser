package app.kestrel.core.download

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/** The HTTP rules the engine relies on (RFC 9110 sections 8.8, 13, 14, 15; RFC 9530), pure and table-tested. */
object HttpSemantics {
    /** first = -1 when the range part is a star (a 416 reply); total = -1 when the total is a star (unknown). */
    class ContentRange(val first: Long, val last: Long, val total: Long)

    fun parseContentRange(v: String?): ContentRange? {
        val s = v?.trim() ?: return null
        if (!s.regionMatches(0, "bytes", 0, 5, ignoreCase = true)) return null
        val rest = s.substring(5).trim()
        val slash = rest.indexOf('/')
        if (slash < 0) return null
        val range = rest.substring(0, slash).trim()
        val totalS = rest.substring(slash + 1).trim()
        val total = if (totalS == "*") -1L else (totalS.toLongOrNull() ?: return null)
        if (range == "*") return ContentRange(-1, -1, total)
        val dash = range.indexOf('-')
        if (dash <= 0) return null
        val first = range.substring(0, dash).trim().toLongOrNull() ?: return null
        val last = range.substring(dash + 1).trim().toLongOrNull() ?: return null
        if (first < 0 || last < first) return null
        return ContentRange(first, last, total)
    }

    /** A strong entity-tag: quoted, not weak ("W/"). Weak tags cannot guard a byte-range splice (RFC 9110 8.8.1). */
    fun isStrongEtag(v: String?): Boolean {
        val s = v?.trim() ?: return false
        return s.length >= 2 && s.startsWith("\"") && s.endsWith("\"")
    }

    private val DATE_FORMATS = arrayOf("EEE, dd MMM yyyy HH:mm:ss zzz", "EEEE, dd-MMM-yy HH:mm:ss zzz", "EEE MMM d HH:mm:ss yyyy")

    fun parseHttpDate(v: String?): Long? {
        val s = v?.trim() ?: return null
        for (f in DATE_FORMATS) {
            try {
                val fmt = SimpleDateFormat(f, Locale.US)
                fmt.timeZone = TimeZone.getTimeZone("GMT")
                fmt.isLenient = false
                return fmt.parse(s)?.time ?: continue
            } catch (e: Exception) { /* try the next format */ }
        }
        return null
    }

    /** Last-Modified is a strong validator only when it is at least one second older than the response's Date (RFC 9110 8.8.2.2). */
    fun isStrongLastModified(lastModified: String?, date: String?): Boolean {
        val lm = parseHttpDate(lastModified) ?: return false
        val d = parseHttpDate(date) ?: return false
        return d - lm >= 1000
    }

    /** Retry-After: delta-seconds or an HTTP-date; capped at 10 minutes. */
    fun retryAfterMs(v: String?, nowMs: Long): Long? {
        val s = v?.trim() ?: return null
        val secs = s.toLongOrNull()
        val ms = if (secs != null) secs * 1000 else (parseHttpDate(s)?.minus(nowMs) ?: return null)
        return ms.coerceIn(0, 600_000)
    }

    /** Repr-Digest "sha-256=:BASE64:" (RFC 9530) -> ("sha-256", lower-case hex). Only sha-256 / sha-512 are honoured. */
    fun reprDigest(v: String?): Pair<String, String>? {
        val s = v ?: return null
        for (part in s.split(',')) {
            val eq = part.indexOf('=')
            if (eq < 0) continue
            val alg = part.substring(0, eq).trim().lowercase(Locale.ROOT)
            if (alg != "sha-256" && alg != "sha-512") continue
            val b64 = part.substring(eq + 1).trim().removePrefix(":").removeSuffix(":")
            val bytes = try { java.util.Base64.getDecoder().decode(b64) } catch (e: Exception) { continue }
            return alg to bytes.joinToString("") { "%02x".format(it) }
        }
        return null
    }

    enum class Verdict { OK_206, FULL_200, CHANGED, BAD_RANGE, GONE, RETRY, FATAL_HTTP }

    class Classified(val verdict: Verdict, val retryAfterMs: Long = 0, val detail: Int = 0)

    /**
     * What a reply means for a request that carried `Range: bytes=[rangeFrom]-...` (or none when [rangeFrom] < 0).
     * [storedTotal] / [storedEtag] describe the file as first seen; -1 / null when unknown.
     */
    fun classify(
        status: Int, rangeFrom: Long, storedTotal: Long, storedEtag: String?, nowMs: Long, header: (String) -> String?,
    ): Classified {
        when (status) {
            206 -> {
                val cr = parseContentRange(header("Content-Range")) ?: return Classified(Verdict.BAD_RANGE, detail = status)
                if (cr.first < 0 || (rangeFrom >= 0 && cr.first != rangeFrom)) return Classified(Verdict.BAD_RANGE, detail = status)
                if (storedTotal > 0 && cr.total > 0 && cr.total != storedTotal) return Classified(Verdict.CHANGED, detail = status)
                val tag = header("ETag")
                if (storedEtag != null && isStrongEtag(tag) && tag!!.trim() != storedEtag) return Classified(Verdict.CHANGED, detail = status)
                return Classified(Verdict.OK_206)
            }
            200 -> {
                val tag = header("ETag")
                if (rangeFrom >= 0 && storedEtag != null && isStrongEtag(tag) && tag!!.trim() != storedEtag) return Classified(Verdict.CHANGED, detail = status)
                return Classified(Verdict.FULL_200)
            }
            412 -> return Classified(Verdict.CHANGED, detail = status)
            416 -> return Classified(Verdict.BAD_RANGE, detail = status)
            401, 403, 404, 410 -> return Classified(Verdict.GONE, detail = status)
            429, 503 -> return Classified(Verdict.RETRY, retryAfterMs(header("Retry-After"), nowMs) ?: 0, status)
            408, 500, 502, 504 -> return Classified(Verdict.RETRY, 0, status)
        }
        return if (status in 500..599) Classified(Verdict.RETRY, 0, status) else Classified(Verdict.FATAL_HTTP, 0, status)
    }

    /** Content-Encoding other than identity: the bytes on the wire are not the file's bytes, so ranges are meaningless (RFC 9110 14.1.2). */
    fun isEncoded(contentEncoding: String?): Boolean {
        val e = contentEncoding?.trim()?.lowercase(Locale.ROOT) ?: return false
        return e.isNotEmpty() && e != "identity"
    }
}

/** Full-jitter exponential backoff (AWS Architecture Blog; research E24). */
object Backoff {
    fun delayMs(attempt: Int, baseMs: Long, capMs: Long, retryAfterMs: Long, rnd: java.util.Random): Long {
        val exp = baseMs shl attempt.coerceIn(0, 20)
        val bound = minOf(capMs, if (exp <= 0) capMs else exp).coerceAtLeast(1)
        val jitter = (rnd.nextDouble() * bound).toLong()
        return maxOf(jitter, retryAfterMs)
    }
}

/** Throughput estimate: exponentially weighted average (alpha 0.3) of one-second buckets. */
class Meter {
    private var bucketStart = -1L
    private var bucketBytes = 0L
    private var ewma = 0.0
    private var firstAt = -1L

    private fun roll(now: Long) {
        if (bucketStart < 0) { bucketStart = now; firstAt = now; return }
        var n = 0
        while (now - bucketStart >= 1000) {
            val rate = bucketBytes.toDouble()
            ewma = if (ewma == 0.0) rate else 0.3 * rate + 0.7 * ewma
            bucketBytes = 0
            bucketStart += 1000
            if (++n >= 10) { ewma = 0.0; bucketStart = now; break } // a long gap (pause, offline): start over
        }
    }

    @Synchronized fun add(n: Int, now: Long) { roll(now); bucketBytes += n }
    @Synchronized fun bytesPerSec(now: Long): Double { roll(now); return ewma }
    @Synchronized fun reset() { bucketStart = -1; bucketBytes = 0; ewma = 0.0; firstAt = -1 }
}

/** Adaptive parallelism: start with one connection, add one at a time, keep it only if the aggregate speed gains at least [Config.growGain]. */
class HillClimb(private val cfg: Config) {
    var target = 1
        private set
    private var baseline = 0.0
    private var pending = false
    private var nextEval = -1L
    private var noGrowthUntil = 0L

    fun reset() { target = 1; baseline = 0.0; pending = false; nextEval = -1; noGrowthUntil = 0 }

    /**
     * A connection beyond the first was refused or keeps failing while another one moves bytes (many servers and carriers allow only so many connections per client): never ask for more than
     * [keep] connections for [forMs]. Growth resumes after that, so a limit that was only a busy moment is not remembered for ever.
     */
    fun refused(now: Long, keep: Int, forMs: Long) {
        target = maxOf(1, minOf(target, keep)); pending = false
        noGrowthUntil = maxOf(noGrowthUntil, now + forMs)
    }

    /**
     * @param cap the most connections allowed right now (1 = no splitting). [total] is the file size when it is known: a big file starts with two connections at once instead of
     * waiting out the ramp (a file of 30 MB at 2 MB/s is over before the ramp would have reached a third connection). Returns the desired number of connections.
     */
    fun update(now: Long, speed: Double, cap: Int, total: Long = -1L): Int {
        if (nextEval < 0) {
            nextEval = now + cfg.rampMs
            if (cfg.fastStart && cap >= 2 && target == 1 && total >= cfg.fastStartMinBytes) { target = 2; baseline = 0.0; pending = true }
        }
        if (cap <= 1) { target = 1; pending = false; return 1 }
        if (target > cap) target = cap
        if (now < nextEval) return target
        if (pending) {
            pending = false
            if (speed < baseline * cfg.growGain) { target = maxOf(1, target - 1); noGrowthUntil = now + cfg.noGrowthMs }
        } else if (target < cap && now >= noGrowthUntil) {
            baseline = speed; target++; pending = true
        }
        nextEval = now + cfg.evalMs
        return target
    }
}

/** Knobs. The defaults are starting values from research/DOWNLOADS.md, to be tuned on the tablet. */
class Config(
    val maxActive: Int = 2,
    val maxConnsPerDownload: Int = 4,
    val maxConnsTotal: Int = 6,
    val windowBytes: Long = 8L shl 20,
    val minGrowRemaining: Long = 4L shl 20,
    val minGrowEtaSec: Long = 6,
    val rampMs: Long = 1500,
    val evalMs: Long = 4000,
    val growGain: Double = 1.15,
    val noGrowthMs: Long = 30_000,
    val checkpointMs: Long = 5000,
    val checkpointBytes: Long = 8L shl 20,
    val stallMs: Long = 15_000,
    val maxFails: Int = 8,
    val backoffBaseMs: Long = 1000,
    val backoffCapMs: Long = 60_000,
    val parallel: Boolean = true,
    /** Files at least this big start with two connections at once (0 = off). */
    val fastStart: Boolean = true,
    val fastStartMinBytes: Long = 8L shl 20,
    /** A refused extra connection keeps the download at one connection fewer for this long. */
    val refusedNoGrowthMs: Long = 10 * 60_000,
    /** An outage in the middle of a download: first look again after [outageBaseMs], doubling up to [outageCapMs]; give up (STALLED) after [outageGiveUpMs] without a byte. */
    val outageBaseMs: Long = 10_000,
    val outageCapMs: Long = 60_000,
    val outageGiveUpMs: Long = 30 * 60_000,
)
