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

    // Java priority 3 is nice 13: Android puts such a thread in the BACKGROUND scheduler group (a sliver of one core). Right for purges, list compiles and thumbnails,
    // wrong for what the user is waiting on, so that gets its own lane at normal priority (research/HW_SPEED.md H2).
    private val fastPool = ThreadPoolExecutor(
        1, 1, 20, TimeUnit.SECONDS, LinkedBlockingQueue(),
        ThreadFactory { r -> Thread(r, "kestrel-io-fast").apply { isDaemon = true } },
    ).also { it.allowCoreThreadTimeOut(true) }

    /** Like [exec] but on the interactive lane: URL-bar suggestions, list reads, the launch-time session read. Keep it short; long work belongs on [exec]. */
    fun fast(block: () -> Unit) {
        fastPool.execute {
            try { block() } catch (t: Throwable) { Log.w(TAG, "io task failed", t) }
        }
    }

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
