package app.kestrel.core.session

import app.kestrel.core.courier.Json

/**
 * The "last look" (research/REVIEW-EXTERNAL-2.md EY-1): a sharp picture of the page the owner left, kept together with what it takes to know whether it still shows what the engine is about to show.
 * This file holds the PURE rules (nothing from Android): what is stored beside the picture, when the picture may be used, how it fits the screen, and when it must go. The picture itself and the
 * drawing are in the app (`LastLook`, `RestoreOverlay`).
 */
class LastLookMeta(
    /** The page the picture belongs to. */
    val url: String,
    /** How far the page was scrolled (pixels of the engine's view) when the picture was taken: the live page has to be there before the picture goes. */
    val scrollY: Int,
    /** The size of the picture in pixels. */
    val w: Int,
    val h: Int,
    /** When it was taken (ms since 1970). */
    val at: Long,
    /** The engine version, the skin and the dark-pages / desktop-site choices it was taken under: a different one shows a different page. */
    val engine: String,
    val skin: String,
    val dark: Boolean,
    val desktop: Boolean,
) {
    fun encode(): String = Json.write(linkedMapOf<String, Any?>("v" to 1, "url" to url, "sy" to scrollY, "w" to w, "h" to h, "at" to at, "eng" to engine, "pal" to skin, "dark" to dark, "desk" to desktop))

    companion object {
        /** null for anything that is not a usable record (a cut-off write, another version, an empty address). */
        fun decode(text: String?): LastLookMeta? {
            if (text.isNullOrBlank() || text.length > 8192) return null
            val m = try { Json.parse(text) as? Map<*, *> } catch (e: Exception) { null } ?: return null
            if ((m["v"] as? Number)?.toInt() != 1) return null
            val url = m["url"] as? String ?: return null
            val w = (m["w"] as? Number)?.toInt() ?: return null
            val h = (m["h"] as? Number)?.toInt() ?: return null
            if (url.isEmpty() || w <= 0 || h <= 0) return null
            return LastLookMeta(url, (m["sy"] as? Number)?.toInt() ?: 0, w, h, (m["at"] as? Number)?.toLong() ?: 0L, m["eng"] as? String ?: "", m["pal"] as? String ?: "", m["dark"] as? Boolean ?: false, m["desk"] as? Boolean ?: false)
        }
    }
}

/** How a saved picture sits in the page area: 1:1, scaled to the width (still sharp), or not at all (today's cover then). */
enum class LookFit { EXACT, SCALE, NONE }

object LastLookRules {
    /** A width this close to the saved one counts as the same (rounding, a scroll bar). */
    const val SAME_WIDTH_PX = 2
    /** A picture of another width is still used, scaled, when the height-to-width ratio is within this share of the screen's. */
    const val ASPECT_SLOP = 0.05
    /** The live page counts as being where the saved one was when its scroll is this close. */
    const val SCROLL_SLOP_PX = 4
    /** The picture goes this long after the first paint at the latest (the engine may paint before it has scrolled to the restored place). */
    const val AFTER_PAINT_MS = 1500
    /** After this long without a first paint a slim note says the picture is the saved view, and after [CAP_MS] (the report says 12 s; this hotspot is slow, and the plain cover already waits 20 s) it goes and the engine's own state shows. */
    const val NOTE_AFTER_MS = 4000
    const val CAP_MS = 20_000
    /** A new picture is taken at most this often while reading, 700 ms after the last scroll; leaving the page asks for one if the last is older than [LEAVE_MIN_AGE_MS]. */
    const val SETTLE_MS = 700
    /** The page's own scroll report waits this long after the last scroll before it is sent (content.js). */
    const val PAGE_DEBOUNCE_MS = 300
    const val MIN_GAP_MS = 4000
    const val LEAVE_MIN_AGE_MS = 1500
    const val AFTER_STOP_MS = 1500
    /** Pictures kept: the page in front and this many most recently used pages. */
    const val KEEP_EXTRA = 2

    /**
     * Found by the emulator lab (research/EMU_LAB.md, 2026-11-15): GeckoView's `capturePixels()` is a SYNCHRONOUS call into the compositor. On a heavy page it held the UI thread for 2 s in the app's own watchdog
     * (stack: `GeckoSession$Compositor.requestScreenPixels` <- `captureLook`) and, with the compositor starved, for more than 20 s: an Android "not responding". A read-back that took [SLOW_READBACK_MS] or more is
     * followed by no read-back at all for this long (the tab grid's preview, which the person is waiting for, is the one exception). 0 = fast, nothing to do.
     */
    const val SLOW_READBACK_MS = 400L
    fun readBackBackoffMs(tookMs: Long): Long = when { tookMs < SLOW_READBACK_MS -> 0L; tookMs < 2_000L -> 120_000L; else -> 600_000L }

    private fun bare(url: String): String = url.substringBefore('#')

    /** The same page (the #fragment aside). */
    fun samePage(a: String, b: String): Boolean = bare(a) == bare(b)

    /**
     * EM1k: a page that never reports "load finished" (ads, streams and trackers keep it busy) was never photographed, so at the next start there was no picture of it and the plain cover showed. At the
     * moment of LEAVING ([why] = "pause") a page that is still loading may be photographed when it has painted AS THIS ADDRESS: [paintedFor] is the address of the engine's last first contentful paint,
     * and a navigation that has started but not painted yet still shows the old page under the new address (that picture would be stored under the wrong address, TP1), so then it stays a no.
     * Every other trigger keeps the old rule (a loading page is not photographed while the person reads: a read-back during a heavy load costs frames).
     */
    fun mayTakeWhileLoading(why: String, paintedFor: String, tabUrl: String): Boolean =
        why == "pause" && paintedFor.isNotEmpty() && samePage(paintedFor, tabUrl)

    /** Only real web pages: not about:, data:, a blank page, the new-tab page or an error page (the caller has already left those out), and no address with a password in it. */
    fun worthKeeping(url: String): Boolean {
        val u = url.trim()
        if (!(u.startsWith("http://", true) || u.startsWith("https://", true))) return false
        val authority = u.substringAfter("://").substringBefore('/').substringBefore('?').substringBefore('#')
        return !authority.contains('@')
    }

    /** True when the saved picture may be shown for a tab that is on [tabUrl] now: the same page (the #fragment aside), engine, skin, dark-pages and desktop-site choice. */
    fun usable(meta: LastLookMeta, tabUrl: String, engine: String, skin: String, dark: Boolean, desktop: Boolean): Boolean =
        whyNot(meta, tabUrl, engine, skin, dark, desktop) == null

    /**
     * EM1k (owner 2026-11-19: at the next start he still sees the plain "Opening <host>" cover): null when the saved picture may be used, else the ONE word that says what is different
     * ("page" = the picture shows another address than the tab's, "engine", "skin", "dark", "desktop"). The start-up path writes the word into the Health list, so the next Diagnostics
     * names the cause instead of leaving a guess. Same order as the old rule: address first.
     */
    fun whyNot(meta: LastLookMeta, tabUrl: String, engine: String, skin: String, dark: Boolean, desktop: Boolean): String? = when {
        !samePage(meta.url, tabUrl) -> "page"
        meta.engine != engine -> "engine"
        meta.skin != skin -> "skin"
        meta.dark != dark -> "dark"
        meta.desktop != desktop -> "desktop"
        else -> null
    }

    /** [metaW] x [metaH] is the picture, [viewW] x [viewH] the page area it has to fill. */
    fun fit(metaW: Int, metaH: Int, viewW: Int, viewH: Int): LookFit {
        if (metaW <= 0 || metaH <= 0 || viewW <= 0 || viewH <= 0) return LookFit.NONE
        if (Math.abs(metaW - viewW) <= SAME_WIDTH_PX) return LookFit.EXACT
        val a = metaH.toDouble() / metaW; val b = viewH.toDouble() / viewW
        return if (Math.abs(a - b) <= b * ASPECT_SLOP) LookFit.SCALE else LookFit.NONE
    }

    /**
     * The picture goes when the live page has painted AND shows what the picture shows: it was at the top, or it is at the saved place (within [SCROLL_SLOP_PX]), or its load has ended, or
     * [AFTER_PAINT_MS] have passed since the first paint. Before the first paint it never goes (the caller's timeouts are the only other way out).
     */
    fun hideNow(paintSeen: Boolean, savedScrollY: Int, currentScrollY: Int, stopped: Boolean, msSincePaint: Long): Boolean =
        paintSeen && (savedScrollY <= 0 || Math.abs(currentScrollY - savedScrollY) <= SCROLL_SLOP_PX || stopped || msSincePaint >= AFTER_PAINT_MS)

    /** The ids whose pictures stay: the page in front and the [KEEP_EXTRA] most recently used others ([lastUsed]: id -> when it was last in front). */
    fun keep(selected: Long, lastUsed: Map<Long, Long>): Set<Long> {
        val out = LinkedHashSet<Long>()
        out.add(selected)
        for ((id, _) in lastUsed.entries.filter { it.key != selected }.sortedByDescending { it.value }.take(KEEP_EXTRA)) out.add(id)
        return out
    }
}
