package app.kestrel.browser.web

import android.webkit.WebResourceResponse
import app.kestrel.core.filter.RequestType
import java.io.ByteArrayInputStream

/**
 * Harmless stand-ins returned for blocked requests, so pages keep working instead of erroring: a 1x1 transparent GIF for images,
 * an empty script/stylesheet/document, 204 for everything else. CORS is allowed so a blocked cross-origin fetch fails quietly
 * instead of logging a CORS error.
 */
object Neutered {
    private val GIF = byteArrayOf(
        0x47, 0x49, 0x46, 0x38, 0x39, 0x61, 0x01, 0x00, 0x01, 0x00, 0x80.toByte(), 0x00, 0x00, 0x00, 0x00, 0x00,
        0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0x21, 0xF9.toByte(), 0x04, 0x01, 0x00, 0x00, 0x00, 0x00, 0x2C, 0x00, 0x00, 0x00, 0x00,
        0x01, 0x00, 0x01, 0x00, 0x00, 0x02, 0x02, 0x44, 0x01, 0x00, 0x3B,
    )
    private val EMPTY = ByteArray(0)
    private val BLANK_HTML = "<!doctype html><meta charset=utf-8><title>Blocked</title>".toByteArray(Charsets.UTF_8)
    private val HEADERS = mapOf("Access-Control-Allow-Origin" to "*", "Cache-Control" to "no-store")

    fun forType(typeMask: Int): WebResourceResponse = when {
        typeMask == RequestType.IMAGE -> make("image/gif", 200, GIF)
        typeMask == RequestType.STYLESHEET -> make("text/css", 200, EMPTY)
        typeMask == RequestType.SUBDOCUMENT -> make("text/html", 200, BLANK_HTML)
        typeMask and RequestType.SCRIPT != 0 -> make("application/javascript", 200, EMPTY)
        else -> make("text/plain", 204, EMPTY)
    }

    /** Placeholder document for a blocked/dangerous main-frame navigation (the native error screen is drawn on top). */
    fun blockedDocument(): WebResourceResponse = make("text/html", 403, BLANK_HTML)

    private fun make(mime: String, status: Int, body: ByteArray): WebResourceResponse =
        WebResourceResponse(mime, "utf-8", status, if (status == 200) "OK" else if (status == 204) "No Content" else "Blocked", HEADERS, ByteArrayInputStream(body))
}
