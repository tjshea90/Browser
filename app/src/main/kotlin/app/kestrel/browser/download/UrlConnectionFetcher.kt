package app.kestrel.browser.download

import app.kestrel.core.download.Cond
import app.kestrel.core.download.FetchException
import app.kestrel.core.download.FetchRequest
import app.kestrel.core.download.FetchResponse
import app.kestrel.core.download.Fetcher
import java.io.IOException
import java.io.InputStream
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * Plain HttpURLConnection (HTTP/1.1 on Android: every connection is its own TCP+TLS flow, so splitting a download really adds a flow). Redirects are followed by
 * hand: at most 10, never https -> http, Cookie / Authorization dropped when the host changes.
 */
class UrlConnectionFetcher : Fetcher {
    override fun open(req: FetchRequest): FetchResponse {
        var url = req.url
        val firstHost = try { URL(url).host } catch (e: Exception) { throw FetchException(FetchException.Kind.BAD_URL, "bad address") }
        var hops = 0
        while (true) {
            val u = try { URL(url) } catch (e: Exception) { throw FetchException(FetchException.Kind.BAD_URL, "bad address") }
            if (u.protocol != "http" && u.protocol != "https") throw FetchException(FetchException.Kind.BAD_URL, "not a web address")
            val c = u.openConnection() as HttpURLConnection
            try {
                c.instanceFollowRedirects = false
                c.connectTimeout = req.connectMs
                c.readTimeout = req.readMs
                c.useCaches = false
                c.requestMethod = "GET"
                val sameHost = u.host.equals(firstHost, ignoreCase = true)
                for ((k, v) in req.headers) {
                    if (!sameHost && (k.equals("Cookie", true) || k.equals("Authorization", true))) continue
                    c.setRequestProperty(k, v)
                }
                if (req.from >= 0) c.setRequestProperty("Range", "bytes=${req.from}-" + (if (req.toInclusive >= 0) req.toInclusive.toString() else ""))
                when (val cd = req.cond) {
                    is Cond.IfMatch -> c.setRequestProperty("If-Match", cd.etag)
                    is Cond.IfUnmodifiedSince -> c.setRequestProperty("If-Unmodified-Since", cd.date)
                    null -> {}
                }
                val code = c.responseCode
                if (code in 301..303 || code == 307 || code == 308) {
                    val loc = c.getHeaderField("Location") ?: throw FetchException(FetchException.Kind.PROTOCOL, "redirect without a target")
                    val next = URL(u, loc).toString()
                    if (u.protocol == "https" && next.startsWith("http://", ignoreCase = true)) throw FetchException(FetchException.Kind.INSECURE_REDIRECT, "https redirected to http")
                    c.disconnect()
                    if (++hops > 10) throw FetchException(FetchException.Kind.PROTOCOL, "too many redirects")
                    url = next
                    continue
                }
                return Resp(c, code, url)
            } catch (e: FetchException) { c.disconnect(); throw e }
            catch (e: SSLException) { c.disconnect(); throw FetchException(FetchException.Kind.TLS, e.message, e) }
            catch (e: SocketTimeoutException) { c.disconnect(); throw FetchException(FetchException.Kind.TIMEOUT, e.message, e) }
            catch (e: UnknownHostException) { c.disconnect(); throw FetchException(FetchException.Kind.DNS, e.message, e) }
            catch (e: ConnectException) { c.disconnect(); throw FetchException(FetchException.Kind.OFFLINE, e.message, e) }
            catch (e: NoRouteToHostException) { c.disconnect(); throw FetchException(FetchException.Kind.OFFLINE, e.message, e) }
            catch (e: IOException) { c.disconnect(); throw FetchException(FetchException.Kind.RESET, e.message, e) }
        }
    }

    private class Resp(val c: HttpURLConnection, override val status: Int, override val finalUrl: String) : FetchResponse {
        override fun header(name: String): String? = c.getHeaderField(name)
        override val body: InputStream? get() = try { if (status >= 400) c.errorStream else c.inputStream } catch (e: IOException) { null }
        override fun abort() { try { c.disconnect() } catch (e: Exception) { /* ignore */ } }
        override fun close() { try { body?.close() } catch (e: Exception) { /* ignore */ }; abort() }
    }
}
