package app.kestrel.browser.net

import app.kestrel.core.download.FileNames

/** Download policy helpers for the gate dialog; the downloading itself is the engine in core/download (DownloadController). */
object Downloads {
    private val RISKY = setOf("apk", "xapk", "apks", "exe", "msi", "bat", "cmd", "scr", "sh", "jar", "dex")
    private val RISKY_MIME = setOf(
        "application/vnd.android.package-archive", "application/x-apk", "application/java-archive", "application/x-msdownload",
        "application/x-msdos-program", "application/x-sh", "application/x-shellscript", "application/x-dex",
    )

    fun fileName(url: String, contentDisposition: String?, mime: String?): String = FileNames.choose(contentDisposition, url, mime)

    fun isRisky(name: String, mime: String? = null): Boolean =
        name.substringAfterLast('.', "").lowercase() in RISKY || (mime != null && mime.substringBefore(';').trim().lowercase() in RISKY_MIME)
}
