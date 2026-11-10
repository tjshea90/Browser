package app.kestrel.browser

import android.os.Handler
import android.os.Looper
import android.util.Log
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/** Threading helpers: one low-priority IO thread (SQLite, files, list compile) and the main-thread handler. */
object Io {
    const val TAG = "Kestrel"

    @JvmField val main = Handler(Looper.getMainLooper())

    private val pool = ThreadPoolExecutor(
        1, 1, 20, TimeUnit.SECONDS, LinkedBlockingQueue(),
        ThreadFactory { r -> Thread(r, "kestrel-io").apply { priority = Thread.NORM_PRIORITY - 2; isDaemon = true } },
    ).also { it.allowCoreThreadTimeOut(true) }

    /** Run [block] on the IO thread; exceptions are logged, never crash the app. */
    fun exec(block: () -> Unit) {
        pool.execute {
            try { block() } catch (t: Throwable) { Log.w(TAG, "io task failed", t) }
        }
    }

    /** Run [block] on the main thread (immediately if already there). */
    fun ui(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block() else main.post(block)
    }

    val isMain: Boolean get() = Looper.myLooper() == Looper.getMainLooper()
}
