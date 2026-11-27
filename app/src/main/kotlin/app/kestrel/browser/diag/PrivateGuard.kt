package app.kestrel.browser.diag

import android.os.SystemClock
import app.kestrel.core.livediag.PrivateScrub
import app.kestrel.core.livediag.PrivateWindow

/**
 * PV1 (owner 2026-11-21: "no data or information is ever saved at any time or sent anywhere from private tabs"): the safety net that does not depend on each log line remembering to ask "is this a private tab?".
 * While a private tab exists, and for two minutes after the last one is gone, [text] masks every site name and address in what goes into the event ring ([Health.log]); the ring feeds the saved last-run
 * notes, the crash note, the Diagnostics text and the live diagnostics, so none of them can name a site that was open in a private tab. The live diagnostics also build no snapshot and send nothing
 * during that time ([LiveDiag]). The window opens when a private tab is created (before it loads anything) and its end is counted from the moment the last one closes.
 * The counters tell it is working without telling anything private: `private.masked` (lines that had a site name masked) and `private.holds` (sends held back).
 */
object PrivateGuard {
    @Volatile private var window = PrivateWindow()
    @Volatile internal var clock: () -> Long = { SystemClock.elapsedRealtime() }

    /** BrowserActivity: is there a private tab now? Called when a private tab is created and whenever tabs go away. */
    fun setOpen(on: Boolean) { window.setOpen(on, clock()) }

    fun active(): Boolean = window.active(clock())

    /** Milliseconds until the window ends (0 = not active, -1 = a private tab is still open): how long a held-back send waits. */
    fun msLeft(): Long = window.msLeft(clock())

    /** [detail] as it may go into the ring: unchanged when no private tab is about, otherwise with every site name and address masked. */
    fun text(detail: String): String {
        if (detail.isEmpty() || !window.active(clock())) return detail
        val t = PrivateScrub.line(detail)
        if (t != detail) Health.count("private.masked")
        return t
    }

    /** The Diagnostics line: is the safety net on, and what it did (two counts, nothing from a page). */
    fun line(): String {
        val left = window.msLeft(clock())
        val state = when {
            left < 0L -> "a private tab is open: the safety net is ON (no site name goes into the event list, the live diagnostics build and send nothing)"
            left > 0L -> "the last private tab closed less than two minutes ago: the safety net is still ON for ${left / 1000} s"
            else -> "no private tab: the safety net is idle"
        }
        return "Private tabs: $state; lines that had a site name masked so far ${Health.counter("private.masked")}, live-diagnostic sends held back ${Health.counter("private.holds")}"
    }

    internal fun resetForTest() { window = PrivateWindow(); clock = { SystemClock.elapsedRealtime() } }
}
