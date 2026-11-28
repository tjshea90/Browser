package app.kestrel.core.ui

/**
 * PV1 (owner 2026-11-18: "I just opened a new private tab, and once i began to load a site, an old regular tab website i visited flashed. It should have only shown the private tab home page transition to the
 * page i typed in"): the browser has ONE page view for all tabs, and it keeps showing the last frame of the tab that used it before until the new tab's session has drawn its own. So whenever the page view is
 * handed to another session, a plain curtain in the page background covers it until that session has painted: the old tab's picture is never seen, and a private tab never shows a regular tab's page (or the
 * other way round).
 */
object CurtainRule {
    enum class Why { UNPAINTED, KIND_CHANGED }

    /**
     * [newPainted]: the session being shown has already painted a page of its own (a live tab coming back). [prevPrivate]: whether the tab whose picture is still on the view was private (null = unknown or gone).
     * @return null when nothing needs hiding.
     */
    fun needed(newPainted: Boolean, prevPrivate: Boolean?, newPrivate: Boolean): Why? = when {
        !newPainted -> Why.UNPAINTED
        prevPrivate != null && prevPrivate != newPrivate -> Why.KIND_CHANGED
        else -> null
    }

    /** An unpainted session keeps the curtain until its first contentful paint; a live one only until its first frame is drawn on the view. */
    fun untilPaint(why: Why): Boolean = why == Why.UNPAINTED
    fun timeoutMs(why: Why): Long = if (untilPaint(why)) 3_000L else 800L
}
