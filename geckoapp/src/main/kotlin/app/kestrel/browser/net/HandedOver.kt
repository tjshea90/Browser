package app.kestrel.browser.net

import app.kestrel.core.download.FETCHER_HANDED
import app.kestrel.core.download.FETCHER_ONE_SHOT
import app.kestrel.core.download.Handed
import app.kestrel.core.download.HandedStreams
import org.mozilla.geckoview.WebResponse
import java.util.concurrent.atomic.AtomicInteger

/**
 * PDL1 (owner, 2026-11-28: a zip from Proton Drive failed with "The site sent a web page instead of the file" and downloaded fine in Brave).
 *
 * The browser engine hands a download over WITH its response body. A file the page makes itself (Proton Drive decrypts in a script and streams it out of a service worker; a blob:
 * file) exists only there, and the download manager's own second request gets the site's web page. This keeps the engine's body for the manager (core `HandedFetcher`) instead of
 * closing it, for at most [Handed.MAX_AGE_MS]. Design: research/PDL1_PAGE_MADE_DOWNLOADS.md.
 */
object HandedOver {
    val streams = HandedStreams()
    private val dataSeq = AtomicInteger()
    /** A page that decrypts its file block by block can be quiet for a while; the engine's default read timeout would cut it. */
    private const val READ_TIMEOUT_MS = 10 * 60_000L

    /**
     * Keeps the engine's own body of [response] and returns the address the download is listed under: the response's address, except a data: address (it can be megabytes long and
     * would be stored and shown) which gets a short stand-in. Without a body nothing is kept.
     */
    fun offer(response: WebResponse, privateTab: Boolean = false): String {
        val uri = response.uri
        val key = if (uri.startsWith("data:", true)) "data:kestrel/" + dataSeq.incrementAndGet() else uri
        val body = response.body ?: return key
        try { response.setReadTimeoutMillis(READ_TIMEOUT_MS) } catch (e: Throwable) { /* the engine's default stays */ }
        streams.offer(key, response.statusCode, HashMap(response.headers), body)
        if (!privateTab) app.kestrel.browser.diag.Health.count("download.handed.offered") // a count only, never the address, and nothing at all for a private tab (the owner's rule)
        return key
    }

    /**
     * The fetcher a download of [url] starts with: [FETCHER_ONE_SHOT] when nothing but the engine's stream can make the file (a made-up service worker response, blob:, data:),
     * [FETCHER_HANDED] when the site is asked first and the engine's stream is the fallback, and 1 (Firefox's network stack, as before) when no stream was kept (a long-pressed
     * link, a video the finder found: those downloads never came from a page's own response).
     */
    fun kindFor(url: String): Int {
        val e = streams.peek(url) ?: return 1
        return if (e.cannotBeAskedAgain) FETCHER_ONE_SHOT else FETCHER_HANDED
    }
}
