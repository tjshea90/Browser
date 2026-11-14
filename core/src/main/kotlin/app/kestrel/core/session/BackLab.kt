package app.kestrel.core.session

import app.kestrel.core.url.UrlParts

/** What a Back / Forward press turned out to be. */
enum class BackKind(val label: String) {
    /** The engine's back/forward cache had the page: instant, exact place. */
    KEPT("kept"),
    /** Loaded again, answered from the HTTP cache (the page's own file cost no bytes). */
    FROM_CACHE("cache"),
    /** Loaded again over the network. */
    FROM_NETWORK("network"),
    /** No usable report (the page never answered, or the engine treated it as a new navigation). */
    OTHER("other"),
}

/** Totals of all presses since the app started (RAM only; nothing about a page address is kept here). */
class BackStats {
    var presses = 0; var kept = 0; var fromCache = 0; var fromNetwork = 0; var other = 0
    /** Presses whose place was known AND measured, and how many of those were back within 4 px. */
    var scrollKnown = 0; var scrollOk = 0
    private val paint = ArrayList<Long>()

    fun addPaint(ms: Long) { paint.add(ms); if (paint.size > 64) paint.removeAt(0) }

    /** Median milliseconds from the press to the first paint of the reloads (kept pages paint nothing new); null when there were none. */
    fun medianPaintMs(): Long? {
        if (paint.isEmpty()) return null
        val s = paint.sorted(); val n = s.size
        return if (n % 2 == 1) s[n / 2] else (s[n / 2 - 1] + s[n / 2]) / 2
    }

    fun reset() { presses = 0; kept = 0; fromCache = 0; fromNetwork = 0; other = 0; scrollKnown = 0; scrollOk = 0; paint.clear() }

    private fun pct(n: Int): Int = Math.round(100.0 * n / presses).toInt()

    /** The Diagnostics line (plain words: the owner copies and pastes it). */
    fun line(): String {
        if (presses == 0) return "Back: no presses yet"
        val sb = StringBuilder("Back: ").append(presses).append(if (presses == 1) " press" else " presses")
        sb.append(", ").append(pct(kept)).append("% kept (instant), ").append(pct(fromCache)).append("% reloaded from the cache, ").append(pct(fromNetwork)).append("% from the network")
        if (other > 0) sb.append(", ").append(pct(other)).append("% other")
        medianPaintMs()?.let { sb.append(", median ").append(it).append(" ms to first paint") }
        if (scrollKnown > 0) sb.append(", scroll kept in ").append(scrollOk).append(" of ").append(scrollKnown)
        return sb.toString()
    }
}

/**
 * RX3-BK1 (research/REVIEW-EXTERNAL-3.md section 3, BK1): the Back lab. It measures what every press turned out to be before anything is tuned: kept by the engine (instant), loaded again from the
 * cache or from the network, how long the first paint took, whether the page ended up at its place. It also remembers which pages the engine cannot have kept when they were left, so that Back to them
 * shows its picture at once (BK2).
 *
 * Time is passed in (uptime millis): every rule is a plain function of the events, so it is tested without a clock. UI thread only (the bridge posts its reports there). Holds no cookie, token or
 * address beyond the host in a log line.
 *
 * Events of one press, in the order the app sees them: [press] (Back or Forward was pressed; the place the page was left at is known or -1), [pageshow] (the content script of the target says what
 * kind of show it was), [painted] (the engine painted the target), [scrolled] (the target reports where it is scrolled). [finish] closes the press (the next press and a timer 3 s after the show do).
 */
class BackLab(private val log: (String) -> Unit = {}) {
    val stats = BackStats()

    private class Open(val key: String, val host: String, val forward: Boolean, val at: Long, val savedY: Int) {
        var kind: BackKind? = null
        var showAt = 0L
        var paintAt = 0L
        var sampleY = -1
    }

    private class KeySet(private val cap: Int) {
        private val m = object : LinkedHashMap<String, Boolean>(16, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Boolean>?): Boolean = size > cap
        }
        fun add(k: String) { m[k] = true }
        fun remove(k: String) { m.remove(k) }
        fun contains(k: String): Boolean = m.containsKey(k)
        fun clear() = m.clear()
    }

    private var cur: Open? = null
    private val notKeptKeys = KeySet(KNOWN_PAGES)
    private val noStoreKeys = KeySet(KNOWN_PAGES)

    // ---------------------------------------------------------------- one press

    /** Back ([forward] false) or Forward was pressed and leads to [url]; [savedY] is the place that page was left at (-1 = unknown). Closes the press before it. */
    fun press(now: Long, url: String, forward: Boolean, savedY: Int) {
        finish(now)
        stats.presses++
        cur = Open(BackScrollRules.keyOf(url), UrlParts.hostOf(url) ?: "?", forward, now, savedY)
    }

    /** The target's content script reported a `pageshow`: [persisted] = the engine restored it from its back/forward cache, [navType] = the navigation timing type, [transferSize] = bytes the page's file cost (0 = from cache). */
    fun pageshow(now: Long, url: String, persisted: Boolean, navType: String, transferSize: Long, y: Int) {
        val o = cur ?: return
        if (o.kind != null || BackScrollRules.keyOf(url) != o.key) return   // one answer per press; a report for another page (a redirect, the page that was left) is not this press
        o.kind = when {
            persisted -> BackKind.KEPT
            navType == "back_forward" -> if (transferSize <= 0) BackKind.FROM_CACHE else BackKind.FROM_NETWORK
            else -> BackKind.OTHER
        }
        o.showAt = now
        if (persisted) o.sampleY = maxOf(o.sampleY, y)   // a restored page is already where it will stay: its place at the show is the measurement
    }

    /** The engine painted the target (the caller checks that the tab shows the target's address). */
    fun painted(now: Long) { val o = cur ?: return; if (o.paintAt == 0L) o.paintAt = now }

    /** The target reports its place (CSS px). Only the first [SAMPLE_WINDOW_MS] after the show count: later movement is the owner reading, not Back. */
    fun scrolled(now: Long, y: Int) {
        val o = cur ?: return
        if (o.kind == null || y < 0 || now - o.showAt > SAMPLE_WINDOW_MS) return
        o.sampleY = y
    }

    /** The key of the page the open press leads to, or null when no press is open (the caller compares it with the tab's address before it reports a paint or a scroll). */
    fun openKey(): String? = cur?.key

    /** Close the open press and count it; the one Health detail line goes to the log callback. */
    fun finish(now: Long) {
        val o = cur ?: return
        cur = null
        val kind = o.kind ?: BackKind.OTHER
        when (kind) {
            BackKind.KEPT -> stats.kept++
            BackKind.FROM_CACHE -> stats.fromCache++
            BackKind.FROM_NETWORK -> stats.fromNetwork++
            BackKind.OTHER -> stats.other++
        }
        var paintMs = -1L
        if ((kind == BackKind.FROM_CACHE || kind == BackKind.FROM_NETWORK) && o.paintAt > 0L) { paintMs = o.paintAt - o.at; stats.addPaint(paintMs) }
        var place = "-"
        if (o.savedY >= 0 && o.sampleY >= 0) {
            stats.scrollKnown++
            val ok = Math.abs(o.sampleY - o.savedY) <= LastLookRules.SCROLL_SLOP_PX
            if (ok) stats.scrollOk++
            place = "${o.sampleY}/${o.savedY}" + if (ok) "" else " MISSED"
        }
        log("kind=${kind.label} dir=${if (o.forward) "fwd" else "back"} show=${if (o.kind == null) -1 else o.showAt - o.at} paint=$paintMs y=$place to=${o.host}")
    }

    // ---------------------------------------------------------------- what is known about the pages that were left

    /** The engine reported `Cache-Control: no-store` on the main response of this page: no cache of any kind may keep it. */
    fun noStore(url: String) { val k = BackScrollRules.keyOf(url); if (k.isNotEmpty()) noStoreKeys.add(k) }

    fun noStoreSeen(url: String): Boolean = noStoreKeys.contains(BackScrollRules.keyOf(url))

    /** The page in front is being LEFT: remember whether the engine can have kept it (it was still loading, forbids caching, or held a stream player: it cannot). The newest answer for a page replaces the older one. */
    fun left(url: String, loading: Boolean, stream: Boolean = false) {
        val k = BackScrollRules.keyOf(url); if (k.isEmpty()) return
        if (loading || stream || noStoreKeys.contains(k)) notKeptKeys.add(k) else notKeptKeys.remove(k)
    }

    /** True when Back to [url] will certainly have to load it again (so its picture goes up at once). */
    fun notKept(url: String): Boolean { val k = BackScrollRules.keyOf(url); return notKeptKeys.contains(k) || noStoreKeys.contains(k) }

    fun reset() { cur = null; stats.reset(); notKeptKeys.clear(); noStoreKeys.clear() }

    companion object {
        /** Pages whose "cannot be kept" state is remembered. */
        const val KNOWN_PAGES = 64
        /** A report counts as Back's work only this soon after the show. */
        const val SAMPLE_WINDOW_MS = 2_500L
        /** The app closes a press this long after its show (the next press closes it too). */
        const val CLOSE_AFTER_MS = 3_000L
    }
}
