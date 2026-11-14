package app.kestrel.core.guard

/**
 * RX3-G5 (research/REVIEW-EXTERNAL-3.md part G): the app's own wall for NEW windows, for what the page-side guard (guard-main.js, [allowOpen] there) misses (a script that opens a window from an iframe it just created,
 * before anything could be injected into it). Until now a window that reached the engine within 2.5 s of ANY finger touch (the play press!) was allowed. Now it needs the same evidence the page guard wants: the same
 * site, a sign-in / payment host, or a real tap on a link to exactly this address. Anything else stops and shows the blocked-window icon (one tap opens it). Pure, no Android: PopupGuardTest.
 */
object PopupGuard {
    enum class Verdict { ALLOW, STOP, WAIT }

    /** A new window needs a finger touch this recent (the rule the app has had since AR1). */
    const val TOUCH_WINDOW_MS = 2500L
    /** A tap report this old may still explain the window. */
    const val TAP_WINDOW_MS = 2500L
    /** The longest a window is held for the tap report to arrive (it travels through the extension). */
    const val WAIT_MS = 150L

    /** Sign-in and payment windows the owner asked for with a tap: allowed from any site. KEEP EQUAL to `AUTH` in guard-main.js (PopupGuardTest reads that file and compares). */
    const val AUTH_SOURCE = "(^|\\.)(accounts\\.google\\.com|appleid\\.apple\\.com|login\\.microsoftonline\\.com|login\\.live\\.com|www\\.facebook\\.com|m\\.facebook\\.com|github\\.com|api\\.twitter\\.com|twitter\\.com|x\\.com|discord\\.com|www\\.paypal\\.com|paypal\\.com|checkout\\.stripe\\.com|pay\\.google\\.com|accounts\\.spotify\\.com|id\\.twitch\\.tv|www\\.linkedin\\.com)$"
    private val AUTH = Regex(AUTH_SOURCE)

    fun isAuthHost(host: String): Boolean = AUTH.containsMatchIn(host)

    /**
     * @param sameSite the window's address is on the same site as the page
     * @param touchAgeMs time since the last finger touch on the screen
     * @param tapArrived the tap report for that touch has arrived
     * @param tapAgeMs age of the last tap report
     * @param tapHref address of the link the tap was on, "" when it was not on a link
     * @param uri the window's address
     * @param authHost the window's host is a sign-in / payment host ([isAuthHost])
     * @param guardReady the page-side guard answered for this page; when it never did, windows on another site stop
     */
    fun decide(sameSite: Boolean, touchAgeMs: Long, tapArrived: Boolean, tapAgeMs: Long, tapHref: String, uri: String, authHost: Boolean, guardReady: Boolean = true): Verdict {
        if (touchAgeMs > TOUCH_WINDOW_MS) return Verdict.STOP        // nobody touched the screen lately: timers, page load, background code
        if (sameSite) return Verdict.ALLOW                           // part of the same site
        if (authHost) return Verdict.ALLOW                           // sign-in / payment
        if (!guardReady) return Verdict.STOP
        if (!tapArrived) return Verdict.WAIT
        if (tapAgeMs <= TAP_WINDOW_MS && tapHref.isNotEmpty() && JumpGuard.sameAddress(tapHref, uri)) return Verdict.ALLOW   // the very link that was tapped
        return Verdict.STOP
    }
}
