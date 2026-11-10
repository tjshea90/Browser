package app.kestrel.core.url

/**
 * Builds the User-Agent we present. Goals:
 *  - look like mainstream Chrome, NOT an embedded WebView (the `; wv` token and `Version/4.0` make sites serve degraded
 *    pages and make Google refuse sign-in), and
 *  - leak nothing device-specific: model/build id are replaced by `K`, the Chrome minor/build numbers by `0.0.0`
 *    (exactly what Chrome's own "UA reduction" does), so every Kestrel user looks the same.
 */
object UserAgents {
    private val CHROME_MAJOR = Regex("""Chrome/(\d+)\.""")

    @JvmStatic
    fun chromeMajor(ua: String): Int? = CHROME_MAJOR.find(ua)?.groupValues?.get(1)?.toIntOrNull()

    /** Phone-style UA (cheaper pages, right for a 7" screen). Falls back to a de-WebView'd copy of [webViewUa]. */
    @JvmStatic
    fun mobile(webViewUa: String): String {
        val major = chromeMajor(webViewUa) ?: return stripWebViewMarkers(webViewUa)
        return "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/$major.0.0.0 Mobile Safari/537.36"
    }

    /** "Desktop site" UA (same shape Chrome-for-Android sends for desktop mode). */
    @JvmStatic
    fun desktop(webViewUa: String): String {
        val major = chromeMajor(webViewUa) ?: return stripWebViewMarkers(webViewUa).replace(" Mobile", "")
        return "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/$major.0.0.0 Safari/537.36"
    }

    private fun stripWebViewMarkers(ua: String): String =
        ua.replace("; wv)", ")").replace(" Version/4.0", "")
}
