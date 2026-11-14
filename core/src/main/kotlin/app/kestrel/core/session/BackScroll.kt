package app.kestrel.core.session

/**
 * RX3-BK2 (research/REVIEW-EXTERNAL-3.md section 3, research/BACK_SCROLL.md): Back and Forward put a page back where the owner left it.
 *
 * The engine's back/forward cache does that by itself for a page it kept. For every other page (still loading when left, `Cache-Control: no-store`, a video player, a cache flushed under memory
 * pressure) the page is LOADED again and nothing guaranteed its place. So the app owns the place: it remembers where each page was left ([ScrollMemory]), arms that place for the page a press
 * leads to ([BackTarget]; the content script asks for it and scrolls there when the engine did not), and keeps the saved picture up until the page is painted AND back at that place
 * ([BackScrollRules.coverGoes]). No Android imports: the rules are tested on the JVM.
 */

/**
 * Where each page of a tab was scrolled when the owner left it (CSS pixels, as the page itself reports them), least recently used out. Keyed by the address without its #fragment.
 * RAM only, private tabs included: nothing here is ever written to disk. UI thread only.
 */
class ScrollMemory(private val cap: Int = BackScrollRules.MEMORY) {
    private val map = object : LinkedHashMap<String, Int>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Int>?): Boolean = size > cap
    }

    val size: Int get() = map.size

    /** A negative place means "unknown" and never replaces a real one. */
    fun put(url: String, y: Int) {
        if (y < 0 || url.isEmpty()) return
        map[BackScrollRules.keyOf(url)] = y
    }

    fun get(url: String): Int? = map[BackScrollRules.keyOf(url)]

    fun clear() = map.clear()
}

/**
 * The place armed for the page a Back / Forward press leads to. The bridge answers the content script's question from ANOTHER thread, so this is immutable and published through a volatile field.
 * It answers only for that page (the #fragment aside) and only until [until] (uptime millis): a page that asks later is a new visit, not the page the press asked for.
 */
class BackTarget(url: String, val y: Int, val until: Long) {
    val key: String = BackScrollRules.keyOf(url)

    /** The place for [url] at time [now], or -1 when this is not the armed page or the arming has run out. */
    fun placeFor(url: String, now: Long): Int = if (now <= until && BackScrollRules.keyOf(url) == key) y else -1
}

object BackScrollRules {
    /** Pages whose place is remembered per tab. */
    const val MEMORY = 32
    /** How long the armed place may be asked for (a slow page on the capped hotspot can take several seconds to load and ask). */
    const val TARGET_TTL_MS = 10_000L
    /** The engine's own cache answers within this long; a page not painted by then is loading, so its picture covers the page area. */
    const val PICTURE_DELAY_MS = 150L

    /** The same page: the address without its #fragment (the same key BackPictures uses). */
    fun keyOf(url: String): String = url.substringBefore('#')

    /** The target a press to [url] arms: the remembered place, or the top (0) when nothing is known (then the cover goes at the first paint, as it always did). */
    fun arm(memory: ScrollMemory, url: String, now: Long): BackTarget = BackTarget(url, memory.get(url) ?: 0, now + TARGET_TTL_MS)

    /**
     * The saved picture goes when the live page has painted AND shows what the picture shows: the same rule as the cold-start picture ([LastLookRules.hideNow]) with `stopped = false` on purpose.
     * The end of the load alone must not uncover a page that still sits at the top, so the exits are: the place is reached (within 4 px), the page was at the top, or 1.5 s after the first paint.
     * The caller's 20 s timeout is the last resort. Before the first paint it never goes.
     */
    fun coverGoes(painted: Boolean, savedY: Int, currentY: Int, msSincePaint: Long): Boolean = LastLookRules.hideNow(painted, savedY, currentY, false, msSincePaint)

    /** 150 ms after the press for a page the engine may have kept; at once for one it cannot have (it was still loading, sent no-store or held a stream player when it was left). */
    fun pictureDelayMs(notKept: Boolean): Long = if (notKept) 0L else PICTURE_DELAY_MS
}
