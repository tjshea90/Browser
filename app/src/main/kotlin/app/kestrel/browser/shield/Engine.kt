package app.kestrel.browser.shield

import android.content.Context
import android.util.Log
import app.kestrel.browser.Io
import app.kestrel.core.filter.FilterSet
import app.kestrel.core.filter.FilterSetCodec
import app.kestrel.core.filter.Psl
import app.kestrel.core.filter.SeedLists
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Holds the active [FilterSet]. Loaded on a background thread at process start; request interception waits up to
 * [WAIT_MS] for it and otherwise uses the tiny built-in seed set (so a slow cold start never lets trackers through
 * unfiltered, and never stalls a page either).
 *
 * Source order: downloaded set (filesDir/filters/current.kfs, atomically replaced by the updater) -> bundled snapshot
 * (assets/filters/default.kfs) -> seed. Any unreadable/corrupt file silently falls through to the next one.
 */
object Engine {
    private const val WAIT_MS = 1500L
    private val latch = CountDownLatch(1)
    @Volatile private var started = false

    /** A loaded set together with its generation (1, 2, ...: bumped at every publish), read as ONE reference so a caller never pairs a set with another set's number. */
    class Snap(@JvmField val fs: FilterSet, @JvmField val generation: Int)
    @Volatile private var snap: Snap? = null
    private var counter = 0
    private fun publish(fs: FilterSet) { synchronized(this) { counter++; snap = Snap(fs, counter) } }
    @Volatile var sourceDescription: String = "loading"
        private set
    @Volatile private var seed: FilterSet? = null

    fun start(ctx: Context) {
        if (started) return
        started = true
        val app = ctx.applicationContext
        Thread({
            try { load(app); warmCosmetics() } catch (t: Throwable) { Log.w(Io.TAG, "engine load failed", t) } finally { latch.countDown() }
        }, "kestrel-engine").apply { priority = Thread.NORM_PRIORITY - 2; isDaemon = true }.start()
    }

    private fun load(app: Context) {
        try {
            app.assets.open("psl.txt").bufferedReader().use { r -> Psl.default = Psl.parse(r.lineSequence()) }
        } catch (t: Throwable) { Log.w(Io.TAG, "PSL asset unreadable, using fallback subset", t) }

        val downloaded = File(File(app.filesDir, "filters"), "current.kfs")
        if (downloaded.isFile) {
            try {
                publish(FilterSetCodec.read(downloaded.readBytes()))
                sourceDescription = "updated list"
                return
            } catch (t: Throwable) { Log.w(Io.TAG, "downloaded filter set rejected: ${t.message}") }
        }
        try {
            publish(app.assets.open("filters/default.kfs").use { FilterSetCodec.read(it.readBytes()) })
            sourceDescription = "bundled list"
            return
        } catch (t: Throwable) { Log.w(Io.TAG, "bundled filter set unreadable", t) }
        publish(seedSet())
        sourceDescription = "built-in minimal list"
    }

    /** The element-hiding tables are built lazily on first use (about 2 s on the emulator): do it here, off every UI/request thread, so the first page does not pay for it. */
    private fun warmCosmetics() {
        try { snap?.fs?.let { it.cosmeticCss("warmup.invalid"); it.warmCosmetic(); it.warmQuery() } } catch (t: Throwable) { Log.w(Io.TAG, "cosmetic warm-up failed", t) }
    }

    private fun seedSet(): FilterSet = seed ?: SeedLists.build().also { seed = it }

    /** The active filter set; blocks the calling (WebView IO) thread for at most [WAIT_MS] during cold start. */
    fun filters(): FilterSet {
        snap?.let { return it.fs }
        latch.await(WAIT_MS, TimeUnit.MILLISECONDS)
        return snap?.fs ?: seedSet()
    }

    /** Non-blocking peek (UI thread safe): the loaded set or null while still loading. */
    fun peek(): FilterSet? = snap?.fs

    /** The loaded set and its generation, or null while loading. The extension's engine image is built from exactly this pair (research/D1-DESIGN.md). */
    fun snapshot(): Snap? = snap

    /** Called by the updater after atomically installing a new file. */
    fun swap(fs: FilterSet, description: String) {
        try { fs.cosmeticCss("warmup.invalid"); fs.warmCosmetic(); fs.warmQuery() } catch (t: Throwable) { /* warm-up only */ }
        publish(fs)
        sourceDescription = description
    }
}
