package app.kestrel.core.download

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentSniffTest {
    private fun html(s: String) = ContentSniff.looksLikeHtml(s.toByteArray(), 0, s.length)

    @Test fun webPagesAreRecognisedWhateverTheyStartWith() {
        assertTrue(html("<!DOCTYPE html><html><body>log in</body></html>"))
        assertTrue(html("  \n\t<html lang=en>"))
        assertTrue(html("<HTML><HEAD>"))
        assertTrue(html("\uFEFF<!doctype html>".replace("\uFEFF", "")))   // a BOM is skipped (tested below with real bytes)
        assertTrue(html("<head><title>Blocked</title>"))
        assertTrue(html("<!-- cached --><html>"))
        assertTrue(html("<script>location='/login'</script>"))
    }

    @Test fun aByteOrderMarkIsSkipped() {
        val b = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + "<!doctype html>".toByteArray()
        assertTrue(ContentSniff.looksLikeHtml(b, 0, b.size))
    }

    @Test fun mediaAndArchivesAreNotWebPages() {
        assertFalse(ContentSniff.looksLikeHtml(byteArrayOf(0x47, 0x40, 0x00, 0x10, 0x00, 0x00), 0, 6))                    // MPEG-TS sync byte
        assertFalse(ContentSniff.looksLikeHtml(byteArrayOf(0, 0, 0, 0x18, 'f'.code.toByte(), 't'.code.toByte(), 'y'.code.toByte(), 'p'.code.toByte()), 0, 8)) // ISO base media
        assertFalse(ContentSniff.looksLikeHtml("PK\u0003\u0004".toByteArray(), 0, 4))                                      // zip
        assertFalse(ContentSniff.looksLikeHtml("%PDF-1.7".toByteArray(), 0, 8))
        assertFalse(ContentSniff.looksLikeHtml("<?xml version=\"1.0\"?><MPD>".toByteArray(), 0, 28))                         // a DASH manifest is not an HTML page
        assertFalse(ContentSniff.looksLikeHtml(ByteArray(0), 0, 0))
        assertFalse(html("a plain text file about <html> tags"))
    }

    @Test fun onlyAClearBinaryExpectationCountsAndWebAddressesAreLeftAlone() {
        assertTrue(ContentSniff.expectsBinary("Movie - 720p.mp4", null))
        assertTrue(ContentSniff.expectsBinary("https://cdn.test/a/b/c.MKV?token=1#x", null))
        assertTrue(ContentSniff.expectsBinary("download", "video/mp4"))
        assertTrue(ContentSniff.expectsBinary("report", "application/pdf; charset=binary"))
        assertFalse(ContentSniff.expectsBinary("page.html", null))
        assertFalse(ContentSniff.expectsBinary("https://site.test/download.php?id=3", null))
        assertFalse(ContentSniff.expectsBinary("https://site.test/get", "text/plain"))
        assertFalse(ContentSniff.expectsBinary("https://site.test/v1.2/file", null))      // a dot in a folder is not an extension
        assertFalse(ContentSniff.expectsBinary(null, null))
        assertTrue(ContentSniff.isHtmlType("text/html; charset=utf-8")); assertTrue(ContentSniff.isHtmlType("application/xhtml+xml")); assertFalse(ContentSniff.isHtmlType("video/mp4")); assertFalse(ContentSniff.isHtmlType(null))
    }
}
