package app.kestrel.core.session

/**
 * RX3-BK2 (research/REVIEW-EXTERNAL-3.md section 3, research/BACK_SCROLL.md): Back and Forward put a page back where the owner left it.
 *
 * The engine's back/forward cache does that by itself for a page it kept. For every other page (`Cache-Control: no-store`, a video player, a cache flushed under memory pressure, a page that grew after
 * its load) the page is LOADED again and nothing guaranteed its place. So the app owns the place: it remembers where each page was left ([ScrollMemory]), answers the page's own question after a
 * history load (the content script asks, then scrolls there when the engine did not), and keeps the saved picture up until the page is painted AND back at that place ([BackScrollRules.coverGoes]).
 * No Android imports: the rules are tested on the JVM.
 */

/**
 * Where each page of a tab was scrolled when the owner left it (CSS pixels, as the page itself reports them), least recently used out. Keyed by the address without its #fragment.
 * RAM only, private tabs included: nothing here is ever written to disk. Written on the UI thread, read on the UI thread AND on the extension bridge's worker thread (the page's question), so every
 * method is synchronized (the least-recently-used order changes on every read).
 */
class ScrollMemory(private val cap: Int = BackScrollRules.MEMORY) {
    private val map = object : LinkedHashMap<String, Int>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Int>?): Boolean = size > cap
    }

    val size: Int @Synchronized get() = map.size

    /** A negative place means "unknown" and never replaces a real one. */
    @Synchronized fun put(url: String, y: Int) {
        if (y < 0 || url.isEmpty()) return
        map[BackScrollRules.keyOf(url)] = y
    }

    @Synchronized fun get(url: String): Int? = map[BackScrollRules.keyOf(url)]

    @Synchronized fun clear() = map.clear()
}

object BackScrollRules {
    /** Pages whose place is remembered per tab. */
    const val MEMORY = 32
    /** The engine's own cache answers within this long; a page not painted by then is loading, so its picture covers the page area. */
    const val PICTURE_DELAY_MS = 150L

    /** The same page: the address without its #fragment (the same key BackPictures uses). */
    fun keyOf(url: String): String = url.substringBefore('#')

    /**
     * BK10 (owner: "One time it glitched"): the app's copy of the engine's history (`SessionState`) arrives a moment AFTER a page commits, so a quick Back can meet a copy that is one page behind. The picture of the page
     * "before" would then be the wrong one (two steps back instead of one) and sit over the page the engine really goes to. The copy is only believed when the entry it calls current IS the page on screen:
     * the same address apart from the scheme, a #fragment and a trailing slash (an http page that was upgraded to https, a page that moved its fragment). False for no entry or no address.
     */
    fun sameAddress(stateUri: String?, tabUrl: String): Boolean {
        if (stateUri.isNullOrEmpty() || tabUrl.isEmpty()) return false
        fun norm(u: String) = u.substringBefore('#').substringAfter("://").trimEnd('/').lowercase()
        return norm(stateUri) == norm(tabUrl)
    }

    /**
     * The saved picture goes when the live page has painted AND shows what the picture shows: the same rule as the cold-start picture ([LastLookRules.hideNow]) with `stopped = false` on purpose.
     * The end of the load alone must not uncover a page that still sits at the top, so the exits are: the place is reached (within 4 px), the page was at the top, or 1.5 s after the first paint.
     * The caller's 20 s timeout is the last resort. Before the first paint it never goes.
     */
    fun coverGoes(painted: Boolean, savedY: Int, currentY: Int, msSincePaint: Long): Boolean = LastLookRules.hideNow(painted, savedY, currentY, false, msSincePaint)

    /** 150 ms after the press for a page the engine may have kept; at once for one it cannot have (it was still loading, sent no-store or held a stream player when it was left). */
    fun pictureDelayMs(notKept: Boolean): Long = if (notKept) 0L else PICTURE_DELAY_MS
}
