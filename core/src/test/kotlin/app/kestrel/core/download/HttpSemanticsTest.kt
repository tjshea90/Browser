package app.kestrel.core.download

import app.kestrel.core.download.HttpSemantics.Verdict
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HttpSemanticsTest {
    private fun h(vararg kv: Pair<String, String>): (String) -> String? { val m = kv.toMap(); return { k -> m.entries.firstOrNull { it.key.equals(k, true) }?.value } }
    private val now = 1_767_225_600_000L // 2026-01-01T00:00:00Z

    @Test fun contentRange() {
        val a = HttpSemantics.parseContentRange("bytes 0-99/1000")!!
        assertEquals(0L, a.first); assertEquals(99L, a.last); assertEquals(1000L, a.total)
        assertEquals(-1L, HttpSemantics.parseContentRange("bytes 100-199/*")!!.total)
        val star = HttpSemantics.parseContentRange("bytes */1000")!!
        assertEquals(-1L, star.first); assertEquals(1000L, star.total)
        assertNull(HttpSemantics.parseContentRange("bytes 5-3/10"))
        assertNull(HttpSemantics.parseContentRange("items 0-1/2"))
        assertNull(HttpSemantics.parseContentRange("bytes 0-1"))
        assertNull(HttpSemantics.parseContentRange(null))
    }

    @Test fun validators() {
        assertTrue(HttpSemantics.isStrongEtag("\"abc\""))
        assertFalse(HttpSemantics.isStrongEtag("W/\"abc\""))
        assertFalse(HttpSemantics.isStrongEtag("abc"))
        assertFalse(HttpSemantics.isStrongEtag(null))
        assertTrue(HttpSemantics.isStrongLastModified("Wed, 31 Dec 2025 23:59:58 GMT", "Thu, 01 Jan 2026 00:00:00 GMT"))
        assertFalse(HttpSemantics.isStrongLastModified("Thu, 01 Jan 2026 00:00:00 GMT", "Thu, 01 Jan 2026 00:00:00 GMT"))
        assertFalse(HttpSemantics.isStrongLastModified("garbage", "Thu, 01 Jan 2026 00:00:00 GMT"))
        assertFalse(HttpSemantics.isStrongLastModified("Wed, 31 Dec 2025 23:59:58 GMT", null))
    }

    @Test fun retryAfter() {
        assertEquals(120_000L, HttpSemantics.retryAfterMs("120", now))
        assertEquals(600_000L, HttpSemantics.retryAfterMs("99999", now)) // capped at 10 minutes
        assertEquals(30_000L, HttpSemantics.retryAfterMs("Thu, 01 Jan 2026 00:00:30 GMT", now))
        assertEquals(0L, HttpSemantics.retryAfterMs("Wed, 31 Dec 2025 00:00:00 GMT", now)) // in the past
        assertNull(HttpSemantics.retryAfterMs("soon", now))
        assertNull(HttpSemantics.retryAfterMs(null, now))
    }

    @Test fun reprDigest() {
        val sum = ByteArray(32) { it.toByte() }
        val b64 = java.util.Base64.getEncoder().encodeToString(sum)
        val d = HttpSemantics.reprDigest("md5=:AAAA:, sha-256=:$b64:")!!
        assertEquals("sha-256", d.first)
        assertEquals(sum.joinToString("") { "%02x".format(it) }, d.second)
        assertNull(HttpSemantics.reprDigest("md5=:AAAA:"))
        assertNull(HttpSemantics.reprDigest(null))
    }

    @Test fun encodedReplies() {
        assertTrue(HttpSemantics.isEncoded("gzip")); assertTrue(HttpSemantics.isEncoded("br"))
        assertFalse(HttpSemantics.isEncoded("identity")); assertFalse(HttpSemantics.isEncoded(null)); assertFalse(HttpSemantics.isEncoded(""))
    }

    @Test fun classifyTable() {
        fun v(status: Int, from: Long, total: Long, etag: String?, vararg hs: Pair<String, String>) = HttpSemantics.classify(status, from, total, etag, now, h(*hs)).verdict
        // good 206
        assertEquals(Verdict.OK_206, v(206, 100, 1000, "\"a\"", "Content-Range" to "bytes 100-199/1000", "ETag" to "\"a\""))
        // 206 starting somewhere else is never spliced
        assertEquals(Verdict.BAD_RANGE, v(206, 100, 1000, "\"a\"", "Content-Range" to "bytes 0-99/1000"))
        assertEquals(Verdict.BAD_RANGE, v(206, 100, 1000, null))                    // no Content-Range
        // the file changed under us
        assertEquals(Verdict.CHANGED, v(206, 100, 1000, "\"a\"", "Content-Range" to "bytes 100-199/2000"))
        assertEquals(Verdict.CHANGED, v(206, 100, 1000, "\"a\"", "Content-Range" to "bytes 100-199/1000", "ETag" to "\"b\""))
        assertEquals(Verdict.CHANGED, v(412, 100, 1000, "\"a\""))
        assertEquals(Verdict.CHANGED, v(200, 100, 1000, "\"a\"", "ETag" to "\"b\""))   // 200 with other validators
        // a weak ETag in the reply cannot prove a change
        assertEquals(Verdict.OK_206, v(206, 100, 1000, "\"a\"", "Content-Range" to "bytes 100-199/1000", "ETag" to "W/\"b\""))
        // 200 to a range request = no range support; 200 to a plain request = a normal download
        assertEquals(Verdict.FULL_200, v(200, 100, 1000, "\"a\"", "ETag" to "\"a\""))
        assertEquals(Verdict.FULL_200, v(200, -1, -1, null))
        assertEquals(Verdict.BAD_RANGE, v(416, 100, 1000, "\"a\""))
        // links
        for (s in listOf(401, 403, 404, 410)) assertEquals(Verdict.GONE, v(s, 0, 10, null))
        // transient
        val r = HttpSemantics.classify(429, 0, 10, null, now, h("Retry-After" to "7"))
        assertEquals(Verdict.RETRY, r.verdict); assertEquals(7000L, r.retryAfterMs)
        for (s in listOf(408, 500, 502, 503, 504, 599)) assertEquals(Verdict.RETRY, v(s, 0, 10, null))
        assertEquals(Verdict.FATAL_HTTP, v(418, 0, 10, null))
        assertEquals(Verdict.FATAL_HTTP, v(302, 0, 10, null))
    }

    @Test fun backoffIsBoundedAndHonoursRetryAfter() {
        val rnd = java.util.Random(7)
        for (a in 0..30) { val d = Backoff.delayMs(a, 1000, 60_000, 0, rnd); assertTrue(d in 0..60_000) }
        assertEquals(5000L, Backoff.delayMs(0, 1000, 60_000, 5000, rnd))
        assertNotNull(Backoff.delayMs(100, 1000, 60_000, 0, rnd))
    }
}
