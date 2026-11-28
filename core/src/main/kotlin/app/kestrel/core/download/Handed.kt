package app.kestrel.core.download

import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.io.SequenceInputStream

/**
 * PDL1 (owner, 2026-11-28: a zip from Proton Drive failed in Kestrel with "The site sent a web page instead of the file" and downloaded fine in Brave).
 *
 * Some sites make the file INSIDE the page: Proton Drive decrypts it in a script and streams it out of a service worker; other pages build a blob: file in memory; a few links
 * work once only or answer a form POST. The browser engine hands such a download to us as a response WITH a body. The download manager never read that body: it asked the site for
 * the same address again (so it can use several connections, pause and continue, and the phone can take it over), and for these downloads the second request gets the site's web
 * page, or nothing. So [HandedStreams] keeps the engine's own body for a few minutes and [HandedFetcher] saves from it when the second request does not return the file (or at once
 * when the address cannot be asked for again at all). Design notes: research/PDL1_PAGE_MADE_DOWNLOADS.md.
 */
object Handed {
    /** How long an unclaimed engine stream is kept: long enough for the "Download this file?" question and a queue, short enough not to hold a connection and a page's work for ever. */
    const val MAX_AGE_MS = 180_000L

    /**
     * True when the engine's response is something a second request cannot reproduce: no web address at all (blob:, data:), or no `Date` header. Every web server states the date
     * of its answer (RFC 9110 requires it of any server with a clock); a response a service worker made up has only the headers its script set. Wrong guesses are cheap both ways: a
     * server that leaves the date out is saved from the engine's stream (one connection, no phone); a made-up response WITH a date still falls back to the stream when the second request fails.
     */
    fun cannotBeAskedAgain(url: String, headers: Map<String, String>): Boolean {
        if (!(url.startsWith("http://", true) || url.startsWith("https://", true))) return true
        return headers.keys.none { it.equals("Date", true) }
    }

    internal fun closeQuietly(s: InputStream?) { try { s?.close() } catch (e: Exception) { /* nothing to do: closing only frees the engine's side */ } }
}

/**
 * The responses the browser engine has handed over and nobody has claimed yet, by address. An engine channel that is not read stays suspended and costs no data, so keeping one for a
 * moment is cheap; every entry is closed (which cancels the engine's side) when it is replaced, dropped, expired or pushed out by newer ones.
 */
class HandedStreams(private val clock: Clock = Clock.System, private val maxHeld: Int = 4, private val maxAgeMs: Long = Handed.MAX_AGE_MS) {
    /** One response the engine still holds. [body] is the engine's stream (null = it sent none). */
    class Entry(val url: String, val status: Int, val headers: Map<String, String>, val body: InputStream?, val at: Long) {
        fun header(name: String): String? = headers.entries.firstOrNull { it.key.equals(name, true) }?.value
        val cannotBeAskedAgain: Boolean get() = Handed.cannotBeAskedAgain(url, headers)
    }

    private val held = LinkedHashMap<String, Entry>()

    /** The engine handed a download over: keep its stream until the download manager claims it. An older offer for the same address is closed. */
    @Synchronized fun offer(url: String, status: Int, headers: Map<String, String>, body: InputStream?) {
        sweepLocked()
        held.remove(url)?.let { Handed.closeQuietly(it.body) }
        held[url] = Entry(url, status, HashMap(headers), body, clock.now())
        while (held.size > maxHeld) { val oldest = held.keys.first(); held.remove(oldest)?.let { Handed.closeQuietly(it.body) } }
    }

    @Synchronized fun has(url: String): Boolean { sweepLocked(); return held.containsKey(url) }
    /** Looks at the entry without claiming it. */
    @Synchronized fun peek(url: String): Entry? { sweepLocked(); return held[url] }
    /** Claims the stream: it leaves the registry and is the caller's to read and close. */
    @Synchronized fun take(url: String): Entry? { sweepLocked(); return held.remove(url) }
    /** Closes and forgets the stream of [url]: the question was declined, the ordinary request worked, the phone took the download, nobody came for it. */
    @Synchronized fun drop(url: String) { held.remove(url)?.let { Handed.closeQuietly(it.body) } }
    @Synchronized fun size(): Int = held.size

    private fun sweepLocked() {
        val now = clock.now()
        val it = held.values.iterator()
        while (it.hasNext()) { val e = it.next(); if (now - e.at > maxAgeMs) { it.remove(); Handed.closeQuietly(e.body) } }
    }
}

/**
 * The engine's own response, read once. The engine learns the file is not ask-again-able from [oneShot]: no ranges, no validators, nothing slows it down, and an early end of the stream
 * is a cut (never "the connection ended early, try again").
 */
class HandedResponse(private val e: HandedStreams.Entry) : FetchResponse {
    override val status: Int = if (e.status in 200..299) 200 else e.status
    override val finalUrl: String get() = e.url
    override val oneShot: Boolean get() = true
    /** What the engine sent, except what would make the download engine believe it can ask for more: ranges, validators. */
    override fun header(name: String): String? = if (HIDDEN.any { it.equals(name, true) }) null else e.header(name)
    override val body: InputStream? = e.body?.let { Guard(it, e.header("Content-Length")?.trim()?.toLongOrNull() ?: -1L, e.header("Content-Encoding")) }
    override fun abort() { Handed.closeQuietly(e.body) }
    override fun close() { Handed.closeQuietly(e.body) }

    private companion object { val HIDDEN = listOf("Accept-Ranges", "ETag", "Last-Modified", "Content-Range") }

    /**
     * Passes the stream through and tells a cut from an end: a stated length that the stream falls clearly short of (the page was closed, the engine was killed) is an error, not a finished
     * file. Two percent is tolerated: a site may state the stored size, not the size of what it sends. A stream LONGER than the stated length is read to its end (the length is a hint).
     */
    private class Guard(private val inner: InputStream, declared: Long, encoding: String?) : InputStream() {
        private val declared = if (encoding != null && !encoding.equals("identity", true)) -1L else declared
        private var count = 0L
        override fun read(): Int { val b = inner.read(); if (b >= 0) count++ else ended(); return b }
        override fun read(b: ByteArray, off: Int, len: Int): Int { val n = inner.read(b, off, len); if (n > 0) count += n else if (n < 0) ended(); return n }
        override fun available(): Int = inner.available()
        override fun close() { inner.close() }
        private fun ended() { if (declared > 0 && count < declared - declared / 50) throw IOException("the page's stream ended after $count of $declared bytes") }
    }
}

/**
 * Decorates the real fetcher (Firefox's network stack in Kestrel G) for a download the engine handed over. [onlyHanded] (kind [FETCHER_ONE_SHOT]): the engine's stream is the only source:
 * the address cannot be asked for again (a service worker's stream, blob:, data:) or the download already started from it. Otherwise (kind [FETCHER_HANDED]) the site is asked as usual,
 * which keeps several connections, pause and continue; the engine's stream is used only when that answer is not the file (an error, a web page where the engine's own answer was none,
 * an answer that cannot be reached) and is closed as soon as the ordinary answer proves good. A refusal for safety (certificate, https to http, own network) is never worked around.
 */
class HandedFetcher(private val real: Fetcher, private val handed: HandedStreams, private val onlyHanded: Boolean = false) : Fetcher {
    override val probeWindow: Long get() = real.probeWindow
    override val windowBytes: Long get() = real.windowBytes

    @Throws(FetchException::class)
    override fun open(req: FetchRequest): FetchResponse {
        if (onlyHanded) return claim(req.url)
        val own = handed.peek(req.url) ?: return real.open(req)
        if (!(req.url.startsWith("http://", true) || req.url.startsWith("https://", true))) return claim(req.url)
        val resp = try { real.open(req) } catch (e: FetchException) {
            when (e.kind) {
                FetchException.Kind.TLS, FetchException.Kind.INSECURE_REDIRECT, FetchException.Kind.LOCAL_NET, FetchException.Kind.BAD_URL -> { handed.drop(req.url); throw e }
                else -> return claim(req.url)
            }
        }
        val good = usable(resp, ContentSniff.isHtmlType(own.header("Content-Type")))
        if (good != null) { handed.drop(req.url); return good }
        try { resp.abort() } catch (e: Exception) { /* ignore */ }
        try { resp.close() } catch (e: Exception) { /* ignore */ }
        return claim(req.url)
    }

    private fun claim(url: String): FetchResponse {
        val e = handed.take(url) ?: throw FetchException(FetchException.Kind.HANDED_GONE, "the page's own stream of this file is gone")
        return HandedResponse(e)
    }

    /** The ordinary answer when it is the file (the first bytes we looked at put back in front); null when it is an error or a web page. */
    private fun usable(resp: FetchResponse, ownIsWebPage: Boolean): FetchResponse? {
        if (resp.status !in 200..299) return null
        if (ownIsWebPage) return resp // the engine's own answer was a web page too: this link IS a web page
        if (ContentSniff.isHtmlType(resp.header("Content-Type"))) return null
        val body = resp.body ?: return null
        val head = ByteArray(PEEK)
        val n = try { body.read(head, 0, head.size) } catch (e: IOException) { return null }
        if (n <= 0 || ContentSniff.looksLikeHtml(head, 0, n)) return null // a server that labels its web page "application/octet-stream"
        return Peeked(resp, head, n)
    }

    private class Peeked(private val inner: FetchResponse, head: ByteArray, n: Int) : FetchResponse by inner {
        override val body: InputStream? = SequenceInputStream(ByteArrayInputStream(head, 0, n), inner.body!!)
    }

    private companion object { const val PEEK = 512 }
}
