package app.kestrel.core.net

/**
 * Who may ask for a connection head start (EX-15, research/REVIEW-EXTERNAL-1.md 4.2). Opening DNS + TCP + TLS to a site before the owner opens it saves 2-3 round trips (about a second on the
 * hotspot) but tells that site "a connection from this address" even if the page is never opened, so every trigger (the typed suggestion, the page in front after a few seconds away, a link a finger
 * is on, a top search result) goes through the same few rules, kept here so they are tested:
 *  - the Warm-up switch is on, no Data saver level, the pages do not go through the phone helper's proxy (the engine skips warm-ups behind a proxy anyway),
 *  - never from a private tab: not while one is in front, and not for a page whose address a private tab holds,
 *  - a touch only counts on the page in front,
 *  - an https origin only, and the same origin at most once per [minGapMs] (the connection is still open or already failed; asking again changes nothing).
 */
class WarmPolicy(private val minGapMs: Long = 30_000L, private val maxKnown: Int = 64) {
    enum class Kind { SUGGESTION, RESUME, TOUCH, RESULT }

    class Ask(
        val kind: Kind,
        val origin: String?,
        val enabled: Boolean,
        val saver: Boolean,
        val viaProxy: Boolean,
        val frontPrivate: Boolean,
        /** a private tab holds the page that asked (two tabs can hold one address: any private one stops it) */
        val pagePrivate: Boolean = false,
        /** the page that asked is the one in front (only checked for [Kind.TOUCH]) */
        val pageIsFront: Boolean = true,
        /** a monotonic clock in ms */
        val now: Long,
    )

    private val last = HashMap<String, Long>()

    /** True when a head start for [Ask.origin] should be requested now; the origin is then remembered for [minGapMs]. */
    @Synchronized fun allow(a: Ask): Boolean {
        val o = a.origin ?: return false
        if (!a.enabled || a.saver || a.viaProxy || a.frontPrivate || a.pagePrivate) return false
        if (a.kind == Kind.TOUCH && !a.pageIsFront) return false
        if (!o.startsWith("https://", ignoreCase = true) || o.length < 11) return false
        val before = last[o]
        if (before != null && a.now - before in 0 until minGapMs) return false
        if (last.size >= maxKnown) last.clear()
        last[o] = a.now
        return true
    }
}
