package app.kestrel.browser.shield

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import app.kestrel.browser.Io
import app.kestrel.browser.data.Prefs
import app.kestrel.core.filter.Category
import app.kestrel.core.filter.FilterSet
import app.kestrel.core.filter.FilterSetBuilder
import app.kestrel.core.filter.FilterSetCodec
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class ListSource(val id: String, val url: String, val category: Int)

/** Where the lists come from. HTTPS only. One host family where possible so a single TLS connection can serve several lists. */
object FilterSources {
    private const val UBO = "https://ublockorigin.github.io/uAssets/filters/"
    val DEFAULT = listOf(
        ListSource("easylist", "https://easylist.to/easylist/easylist.txt", Category.ADS),
        ListSource("easyprivacy", "https://easylist.to/easylist/easyprivacy.txt", Category.PRIVACY),
        ListSource("ubo-filters", UBO + "filters.min.txt", Category.ADS),
        ListSource("ubo-privacy", UBO + "privacy.min.txt", Category.PRIVACY),
        ListSource("ubo-badware", UBO + "badware.min.txt", Category.MALWARE),
        ListSource("ubo-unbreak", UBO + "unbreak.min.txt", Category.ADS),
        ListSource("easylist-cookies", "https://secure.fanboy.co.nz/fanboy-cookiemonster.txt", Category.ANNOYANCE),
    )
}

sealed class FetchResult {
    class Ok(val etag: String?, val lastModified: String?, val bytes: Long) : FetchResult()
    object NotModified : FetchResult()
    class Fail(val message: String) : FetchResult()
}

/** Network seam: the real one is [HttpFetcher]; tests inject canned responses. Implementations write the body to [out]. */
interface Fetcher {
    fun fetch(url: String, etag: String?, lastModified: String?, maxBytes: Long, out: File): FetchResult
}

class HttpFetcher : Fetcher {
    override fun fetch(url: String, etag: String?, lastModified: String?, maxBytes: Long, out: File): FetchResult {
        var conn: HttpURLConnection? = null
        try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 30_000
                instanceFollowRedirects = true // never follows https -> http
                setRequestProperty("User-Agent", "Kestrel-filter-updater")
                if (etag != null) setRequestProperty("If-None-Match", etag)
                if (lastModified != null) setRequestProperty("If-Modified-Since", lastModified)
            }
            val code = conn.responseCode
            if (code == HttpURLConnection.HTTP_NOT_MODIFIED) return FetchResult.NotModified
            if (code != HttpURLConnection.HTTP_OK) return FetchResult.Fail("HTTP $code")
            if (conn.url.protocol != "https") return FetchResult.Fail("redirected away from HTTPS")
            if (conn.contentLengthLong > maxBytes) return FetchResult.Fail("larger than ${maxBytes / 1_000_000} MB")
            var total = 0L
            conn.inputStream.use { input ->
                FileOutputStream(out).use { o ->
                    val buf = ByteArray(16 * 1024)
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        total += n
                        if (total > maxBytes) return FetchResult.Fail("larger than ${maxBytes / 1_000_000} MB")
                        o.write(buf, 0, n)
                    }
                }
            }
            return FetchResult.Ok(conn.getHeaderField("ETag"), conn.getHeaderField("Last-Modified"), total)
        } catch (e: Exception) {
            return FetchResult.Fail(e.javaClass.simpleName + (e.message?.let { ": ${it.take(80)}" } ?: ""))
        } finally {
            conn?.disconnect()
        }
    }
}

/**
 * Weekly (configurable) refresh of the filter set, built for a metered, flaky hotspot:
 *  - conditional GET (ETag / Last-Modified): an unchanged list costs a few hundred bytes;
 *  - HTTPS only, size-capped, HTML error pages rejected, nothing is ever executed;
 *  - SAFETY INVARIANT: the new compiled set only replaces the current one if every list is available, it passes a sanity
 *    check against the old rule count, and it round-trips through the codec. Any failure keeps what already works.
 */
object FilterUpdater {
    sealed class Result(val message: String) {
        class Updated(message: String) : Result(message)
        class Unchanged(message: String) : Result(message)
        class Failed(message: String) : Result(message)
        class Skipped(message: String) : Result(message)
    }

    private const val MAX_LIST_BYTES = 14_000_000L
    @Volatile private var cancelled = false
    private val lock = Any()

    fun cancel() { cancelled = true }

    /**
     * A network with INTERNET capability exists. Deliberately NOT requiring NET_CAPABILITY_VALIDATED: a working hotspot whose carrier blocks Google's
     * connectivity check never gets that flag, and the download itself fails safely (HTTPS only, HTML/oversized bodies rejected) if the link is dead.
     */
    fun isOnline(ctx: Context): Boolean {
        val cm = ctx.getSystemService(ConnectivityManager::class.java) ?: return false
        val caps = cm.getNetworkCapabilities(cm.activeNetwork ?: return false) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun isMetered(ctx: Context): Boolean {
        val cm = ctx.getSystemService(ConnectivityManager::class.java) ?: return true
        val caps = cm.getNetworkCapabilities(cm.activeNetwork ?: return true) ?: return true
        return !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
    }

    /** Blocking; call from a background thread. [manual] = the user tapped "Update now" (allowed over a metered link). */
    fun run(
        ctx: Context,
        manual: Boolean,
        sources: List<ListSource> = FilterSources.DEFAULT,
        fetcher: Fetcher = HttpFetcher(),
        online: () -> Boolean = { isOnline(ctx) },
        metered: () -> Boolean = { isMetered(ctx) },
    ): Result = synchronized(lock) {
        cancelled = false
        val prefs = Prefs.get(ctx)
        val result = try { runLocked(ctx, prefs, manual, sources, fetcher, online, metered) } catch (t: Throwable) {
            Log.w(Io.TAG, "filter update crashed", t)
            Result.Failed("Update failed: ${t.javaClass.simpleName}")
        }
        prefs.lastUpdateNote = result.message
        if (result is Result.Updated || result is Result.Unchanged) prefs.lastUpdateMs = System.currentTimeMillis()
        result
    }

    private fun runLocked(ctx: Context, prefs: Prefs, manual: Boolean, sources: List<ListSource>, fetcher: Fetcher, online: () -> Boolean, metered: () -> Boolean): Result {
        if (!online()) return Result.Skipped("No internet connection")
        if (!manual && prefs.updateUnmeteredOnly && metered()) return Result.Skipped("Waiting for an unmetered network")
        val dir = File(ctx.filesDir, "filters").apply { mkdirs() }
        val srcDir = File(dir, "src").apply { mkdirs() }
        val meta = Meta.load(File(dir, "meta.txt"))

        var changed = 0
        var bytes = 0L
        val failures = ArrayList<String>()
        for (s in sources) {
            if (cancelled) return Result.Skipped("Cancelled")
            if (!s.url.startsWith("https://")) { failures.add("${s.id}: not HTTPS"); continue }
            val dest = File(srcDir, s.id + ".txt")
            val tmp = File(srcDir, s.id + ".tmp")
            val prev = meta[s.id]
            val r = fetcher.fetch(s.url, if (dest.isFile) prev?.etag else null, if (dest.isFile) prev?.lastModified else null, MAX_LIST_BYTES, tmp)
            when (r) {
                is FetchResult.NotModified -> {}
                is FetchResult.Fail -> { tmp.delete(); failures.add("${s.id}: ${r.message}") }
                is FetchResult.Ok -> {
                    val problem = looksLikeAList(tmp)
                    if (problem != null) { tmp.delete(); failures.add("${s.id}: $problem") } else {
                        if (!tmp.renameTo(dest)) { dest.delete(); tmp.renameTo(dest) }
                        meta[s.id] = Meta.Entry(r.etag, r.lastModified)
                        changed++
                        bytes += r.bytes
                    }
                }
            }
        }
        meta.save()

        val compiled = File(dir, "current.kfs")
        val everyListPresent = sources.all { File(srcDir, it.id + ".txt").isFile }
        val summary = "${changed} of ${sources.size} lists changed" + if (bytes > 0) " (${"%.1f".format(bytes / 1_000_000.0)} MB downloaded)" else ""
        if (changed == 0 && compiled.isFile) {
            return if (failures.isEmpty()) Result.Unchanged("Up to date ($summary)") else Result.Failed("Some lists could not be checked: ${failures.joinToString("; ")}")
        }
        if (!everyListPresent) {
            return Result.Failed("Kept the current list; not every source could be downloaded: ${failures.joinToString("; ")}")
        }
        if (cancelled) return Result.Skipped("Cancelled")

        // ---- compile from the cached sources and install atomically
        val builder = FilterSetBuilder()
        try {
            for (s in sources) File(srcDir, s.id + ".txt").bufferedReader(Charsets.UTF_8).use { r -> builder.addAbpList(s.id, s.category, r.lineSequence()) }
        } catch (oom: OutOfMemoryError) {
            return Result.Failed("Not enough memory to build the lists; kept the current ones")
        }
        val fs = builder.build()
        val previous = Engine.peek()?.meta?.totalNetwork ?: 0
        if (!sane(fs, previous)) return Result.Failed("The new lists looked incomplete (${fs.meta.totalNetwork} rules); kept the current ones")
        val data = FilterSetCodec.write(fs)
        FilterSetCodec.read(data) // must round-trip, or it never ships
        val tmp = File(dir, "current.kfs.tmp")
        tmp.writeBytes(data)
        if (!tmp.renameTo(compiled)) { compiled.delete(); tmp.renameTo(compiled) }
        Engine.swap(fs, "updated list")
        return Result.Updated("Updated: $summary, ${"%,d".format(fs.meta.totalNetwork)} network rules")
    }

    /** A new set must be roughly as big as the old one: guards against a truncated or poisoned download weakening protection. */
    internal fun sane(fs: FilterSet, previousRules: Int): Boolean {
        val n = fs.meta.totalNetwork
        if (n < 20_000) return false
        return previousRules == 0 || n >= previousRules * 6 / 10
    }

    /** Null if the file plausibly is a filter list; otherwise why not (HTML error pages are the classic trap). */
    internal fun looksLikeAList(f: File): String? {
        if (!f.isFile || f.length() < 2_000) return "too small to be a list"
        val head = f.inputStream().use { i -> ByteArray(minOf(2048, f.length().toInt())).also { i.read(it) } }.toString(Charsets.ISO_8859_1).lowercase()
        if (head.contains("<html") || head.contains("<!doctype")) return "got a web page instead of a list"
        return null
    }

    /** ETag / Last-Modified per source id, one line each: `id<TAB>etag<TAB>lastModified`. */
    internal class Meta private constructor(private val file: File, private val map: MutableMap<String, Entry>) {
        class Entry(val etag: String?, val lastModified: String?)
        operator fun get(id: String): Entry? = map[id]
        operator fun set(id: String, e: Entry) { map[id] = e }
        fun save() {
            val tmp = File(file.parentFile, file.name + ".tmp")
            tmp.writeText(map.entries.joinToString("\n") { (k, v) -> "$k\t${v.etag ?: ""}\t${v.lastModified ?: ""}" })
            if (!tmp.renameTo(file)) { file.delete(); tmp.renameTo(file) }
        }

        companion object {
            fun load(file: File): Meta {
                val m = LinkedHashMap<String, Entry>()
                if (file.isFile) for (line in file.readLines()) {
                    val p = line.split('\t')
                    if (p.size == 3 && p[0].isNotEmpty()) m[p[0]] = Entry(p[1].ifEmpty { null }, p[2].ifEmpty { null })
                }
                return Meta(file, m)
            }
        }
    }
}
