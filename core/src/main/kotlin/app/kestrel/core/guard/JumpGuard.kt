package app.kestrel.core.guard

/**
 * The decision half of the forced-redirect guard (owner request X6: "when I press play it redirects to an advertisement page").
 *
 * A page that sends its own tab to ANOTHER SITE right after the user's tap, where that tap was not on a link to that address, is a hijack (the transparent "play" layer of a movie site). The rule
 * needs positive evidence: the page-side script reports every real tap with the link it was on ("" = not on a link); only when such a report exists, is recent, and names a different address is the
 * jump stopped. No report (a scroll, a tap that was not on the page, a slow message) never stops anything. The report travels through the extension, so it can arrive a few milliseconds after the
 * navigation request: [Verdict.WAIT] says "decide again when it has arrived, or after [WAIT_MS]".
 */
object JumpGuard {
    enum class Verdict { ALLOW, STOP, WAIT }

    /** How long after a real tap a page-initiated jump is still treated as caused by that tap. */
    const val TAP_WINDOW_MS = 2500L
    /** The longest a navigation is held for the tap report to arrive. */
    const val WAIT_MS = 150L

    /**
     * @param crossSite the target is on another site than the page the tab shows
     * @param touchAgeMs time since the last finger touch on the screen
     * @param tapArrived the tap report for that touch has arrived (the report's time is not older than the touch)
     * @param tapAgeMs age of the last tap report
     * @param tapHref address of the link the tap was on, "" when it was not on a link
     * @param uri where the page wants to go
     */
    fun decide(crossSite: Boolean, touchAgeMs: Long, tapArrived: Boolean, tapAgeMs: Long, tapHref: String, uri: String): Verdict {
        if (!crossSite) return Verdict.ALLOW
        if (touchAgeMs > TAP_WINDOW_MS) return Verdict.ALLOW // nobody touched the screen lately: not a tap-triggered jump (other rules are not this guard's business)
        if (!tapArrived) return Verdict.WAIT
        if (tapAgeMs > TAP_WINDOW_MS) return Verdict.ALLOW
        return if (tapHref.isNotEmpty() && sameAddress(tapHref, uri)) Verdict.ALLOW else Verdict.STOP
    }

    /** Equal addresses, a fragment aside. */
    fun sameAddress(a: String, b: String): Boolean = a == b || a.substringBefore('#') == b.substringBefore('#')
}
