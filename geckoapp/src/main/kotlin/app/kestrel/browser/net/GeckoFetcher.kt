package app.kestrel.browser.net

import app.kestrel.browser.GeckoEngine
import app.kestrel.core.download.Cond
import app.kestrel.core.download.FetchException
import app.kestrel.core.download.FetchRequest
import app.kestrel.core.download.FetchResponse
import app.kestrel.core.download.Fetcher
import org.mozilla.geckoview.GeckoWebExecutor
import org.mozilla.geckoview.WebRequest
import org.mozilla.geckoview.WebRequestError
import org.mozilla.geckoview.WebResponse
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.TimeoutException

/**
 * Downloads through Firefox's own network stack (Kestrel G): the same cookies as the page (login-gated downloads work), HTTP/2 + HTTP/3, encrypted DNS and
 * Mozilla's root list instead of this tablet's 2021 one. Replies are bounded Range windows because the engine's Java-side queue has no back-pressure.
 * When the engine is not running in this process (a service restart) it quietly falls back to [fallback] (plain HttpURLConnection).
 */
class GeckoFetcher(private val fallback: Fetcher) : Fetcher {
    override val probeWindow: Long get() = 4L shl 20
    override val windowBytes: Long get() = 4L shl 20

    override fun open(req: FetchRequest): FetchResponse {
        val ex: GeckoWebExecutor = GeckoEngine.executorOrNull() ?: return fallback.open(req)
        val b = WebRequest.Builder(req.url).method("GET").cacheMode(WebRequest.CACHE_MODE_NO_STORE)
        for ((k, v) in req.headers) {
            when {
                k.equals("Referer", true) -> b.referrer(v)
                k.equals("Cookie", true) || k.equals("User-Agent", true) || k.equals("Host", true) -> {} // the profile's cookies and the engine's own user agent apply
                else -> b.header(k, v)
            }
        }
        if (req.from >= 0) b.header("Range", "bytes=${req.from}-" + (if (req.toInclusive >= 0) req.toInclusive.toString() else ""))
        when (val c = req.cond) {
            is Cond.IfMatch -> b.header("If-Match", c.etag)
            is Cond.IfUnmodifiedSince -> b.header("If-Unmodified-Since", c.date)
            null -> {}
        }
        val resp: WebResponse = try {
            ex.fetch(b.build(), if (req.priv) GeckoWebExecutor.FETCH_FLAGS_PRIVATE else GeckoWebExecutor.FETCH_FLAGS_NONE).poll((req.connectMs + req.readMs).toLong())
                ?: throw FetchException(FetchException.Kind.TIMEOUT, "no answer")
        } catch (e: FetchException) { throw e }
        catch (e: WebRequestError) { throw map(e) }
        catch (e: TimeoutException) { throw FetchException(FetchException.Kind.TIMEOUT, "timeout", e) }
        catch (e: Throwable) { throw FetchException(FetchException.Kind.RESET, e.message, e) }
        resp.setReadTimeoutMillis(req.readMs.toLong())
        if (req.url.startsWith("https://", true) && resp.uri.startsWith("http://", true)) {
            try { resp.body?.close() } catch (e: Exception) { /* ignore */ }
            throw FetchException(FetchException.Kind.INSECURE_REDIRECT, "https redirected to http")
        }
        return Resp(resp)
    }

    private fun map(e: WebRequestError): FetchException {
        val kind = when {
            e.category == WebRequestError.ERROR_CATEGORY_SECURITY -> FetchException.Kind.TLS
            e.code == WebRequestError.ERROR_NET_TIMEOUT -> FetchException.Kind.TIMEOUT
            e.code == WebRequestError.ERROR_OFFLINE -> FetchException.Kind.OFFLINE
            e.code == WebRequestError.ERROR_UNKNOWN_HOST -> FetchException.Kind.DNS
            e.code == WebRequestError.ERROR_LOCAL_NETWORK_ACCESS_DENIED -> FetchException.Kind.LOCAL_NET
            e.code == WebRequestError.ERROR_MALFORMED_URI || e.code == WebRequestError.ERROR_UNKNOWN_PROTOCOL -> FetchException.Kind.BAD_URL
            else -> FetchException.Kind.RESET
        }
        return FetchException(kind, "engine error ${e.code}/${e.category}", e)
    }

    private class Resp(val r: WebResponse) : FetchResponse {
        override val status: Int get() = r.statusCode
        override val finalUrl: String get() = r.uri
        override fun header(name: String): String? = r.headers.entries.firstOrNull { it.key.equals(name, true) }?.value
        override val body: InputStream? get() = r.body
        /** Closing the body cancels the request (and wakes a reader blocked on it). */
        override fun abort() { try { r.body?.close() } catch (e: IOException) { /* ignore */ } catch (e: Exception) { /* ignore */ } }
        override fun close() = abort()
    }
}
