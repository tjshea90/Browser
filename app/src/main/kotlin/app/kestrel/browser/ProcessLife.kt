package app.kestrel.browser

import android.os.Handler
import android.os.Looper
import android.os.Process
import app.kestrel.browser.download.DownloadController
import app.kestrel.browser.update.UpdateManager

/**
 * When the process may go. The user quits by swiping the app away; Android then kills the process by itself unless a foreground service (a download) is still running.
 * This covers the rest: a process that was started WITHOUT a screen (the daily update check, a finished download's service, an installer result) must not linger once its work is done,
 * and a swiped-away app whose downloads have just finished must not keep the engine's memory. Everything that finishes work calls [maybeExit]; it only exits when nothing needs the process.
 */
object ProcessLife {
    /** An Activity exists (or is being re-created for a configuration change). */
    @Volatile var uiAlive = false
    /** The Activity is started and not yet stopped: on screen, screen on. Download threads lower their priority only then. */
    @Volatile var uiVisible = false
    /** The first Activity of this process has been created (decides "clear browsing data when the app is closed": a cold start means it was closed). */
    @Volatile var coldStartHandled = false

    private val main = Handler(Looper.getMainLooper())
    private val check = Runnable { exitIfIdle() }

    /** Cancel a pending exit (a screen appeared). */
    fun cancel() { main.removeCallbacks(check) }

    /** Exit the process after [delayMs] unless something needs it by then (the delay lets the engine flush its profile and the notification disappear). */
    fun maybeExit(delayMs: Long = 2000) { main.removeCallbacks(check); main.postDelayed(check, delayMs) }

    private fun exitIfIdle() {
        if (uiAlive) return
        if (DownloadController.needsService() || UpdateManager.busy()) return
        Process.killProcess(Process.myPid())
    }
}
