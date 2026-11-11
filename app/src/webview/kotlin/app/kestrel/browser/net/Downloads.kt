package app.kestrel.browser.net

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.webkit.CookieManager
import android.webkit.URLUtil
import app.kestrel.core.filter.Psl
import app.kestrel.core.url.UrlParts

/** Hands downloads to the system DownloadManager (it survives our process being killed, shows progress, and resumes). */
object Downloads {
    private val RISKY = setOf("apk", "xapk", "apks", "exe", "msi", "bat", "cmd", "scr", "sh", "jar", "dex")

    fun fileName(url: String, contentDisposition: String?, mime: String?): String {
        val raw = URLUtil.guessFileName(url, contentDisposition, mime)
        val clean = raw.replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"), "_").trim().trimStart('.').take(120)
        return clean.ifEmpty { "download" }
    }

    /** What the SERVER says the file is: "invoice.pdf" served as an Android package is still an installer. */
    private val RISKY_MIME = setOf(
        "application/vnd.android.package-archive", "application/x-apk", "application/java-archive", "application/x-msdownload",
        "application/x-msdos-program", "application/x-sh", "application/x-shellscript", "application/x-dex",
    )

    fun isRisky(name: String, mime: String? = null): Boolean =
        name.substringAfterLast('.', "").lowercase() in RISKY || (mime != null && mime.substringBefore(';').trim().lowercase() in RISKY_MIME)

    /** @return true if queued */
    fun enqueue(ctx: Context, url: String, userAgent: String?, contentDisposition: String?, mime: String?, pageUrl: String?): Boolean {
        val u = Uri.parse(url)
        if (u.scheme != "http" && u.scheme != "https") return false
        val dm = ctx.getSystemService(DownloadManager::class.java) ?: return false
        val name = fileName(url, contentDisposition, mime)
        val req = DownloadManager.Request(u).apply {
            if (!mime.isNullOrEmpty()) setMimeType(mime)
            // Cookies only to the page's own site: forwarding them anywhere else would bypass third-party cookie blocking.
            val dlHost = u.host
            val pageHost = pageUrl?.let { UrlParts.hostOf(it) }
            val sameSite = dlHost != null && pageHost != null && try { Psl.default.isSameSite(dlHost, pageHost) } catch (e: Exception) { dlHost == pageHost }
            if (sameSite) CookieManager.getInstance().getCookie(url)?.let { addRequestHeader("Cookie", it) }
            if (!userAgent.isNullOrEmpty()) addRequestHeader("User-Agent", userAgent)
            // Referer is only the page's ORIGIN (never the full URL: it can carry tokens/search terms)
            pageUrl?.let { p -> UrlParts.parse(p)?.let { addRequestHeader("Referer", "${it.scheme}://${it.hostForAuthority}/") } }
            setTitle(name)
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        }
        return try {
            req.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name)
            dm.enqueue(req); true
        } catch (e: Exception) {
            try { req.setDestinationInExternalFilesDir(ctx, Environment.DIRECTORY_DOWNLOADS, name); dm.enqueue(req); true } catch (e2: Exception) { false }
        }
    }
}
