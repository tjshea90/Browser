package app.kestrel.core.courier

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.SecureRandom

class SignTest {
    private val key = Sign.deriveKey(ByteArray(10) { (it * 7 + 1).toByte() })
    private val now = 1_800_000_000_000L

    @Test fun knownHmacAndShaVectors() {
        // RFC 4231 test case 2 and the SHA-256 of "abc"
        assertEquals("5bdcc146bf60754e6a042426089575c75a003f089d2739839dec58b964ec3843", Sign.hex(Sign.hmac("Jefe".toByteArray(), "what do ya want for nothing?".toByteArray())))
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", Sign.hex(Sign.sha256("abc".toByteArray())))
    }

    @Test fun base64UrlRoundTripsAndRefusesGarbage() {
        val b = ByteArray(33) { (it * 31).toByte() }
        assertArrayEquals(b, Sign.unb64(Sign.b64(b)))
        assertNull(Sign.unb64("***"))
        assertTrue(!Sign.b64(b).contains('=') && !Sign.b64(b).contains('+') && !Sign.b64(b).contains('/'))
    }

    @Test fun aGoodRequestPasses() {
        val body = """{"url":"https://example.org/a.bin"}""".toByteArray()
        val sig = Sign.sign(key, "POST", "/v1/jobs", now, body)
        assertEquals(Sign.Result.OK, Sign.check(key, "POST", "/v1/jobs", now.toString(), sig, body, now))
        assertEquals(Sign.Result.OK, Sign.check(key, "post", "/v1/jobs", now.toString(), sig, body, now))             // method case does not matter
        assertEquals(Sign.Result.OK, Sign.check(key, "POST", "/v1/jobs", now.toString(), sig, body, now + 59_000))
        assertEquals(Sign.Result.OK, Sign.check(key, "POST", "/v1/jobs", now.toString(), sig, body, now - 59_000))
    }

    @Test fun everyPartOfTheRequestIsCovered() {
        val body = "{}".toByteArray()
        val sig = Sign.sign(key, "POST", "/v1/jobs/j1/cancel", now, body)
        fun r(m: String = "POST", t: String = "/v1/jobs/j1/cancel", ts: Long = now, b: ByteArray = body, k: ByteArray = key) = Sign.check(k, m, t, ts.toString(), sig, b, now)
        assertEquals(Sign.Result.OK, r())
        assertEquals(Sign.Result.BAD, r(m = "DELETE"))
        assertEquals(Sign.Result.BAD, r(t = "/v1/jobs/j2/cancel"))
        assertEquals(Sign.Result.BAD, r(t = "/v1/jobs/j1/cancel?x=1"))
        assertEquals(Sign.Result.BAD, r(ts = now + 1))
        assertEquals(Sign.Result.BAD, r(b = "{ }".toByteArray()))
        assertEquals(Sign.Result.BAD, r(k = Sign.deriveKey(ByteArray(10))))
    }

    @Test fun timeWindowAndMissingParts() {
        val sig = Sign.sign(key, "GET", "/v1/jobs?ids=a", now)
        assertEquals(Sign.Result.SKEW, Sign.check(key, "GET", "/v1/jobs?ids=a", now.toString(), sig, ByteArray(0), now + 61_000))
        assertEquals(Sign.Result.SKEW, Sign.check(key, "GET", "/v1/jobs?ids=a", now.toString(), sig, ByteArray(0), now - 61_000))
        assertEquals(Sign.Result.MISSING, Sign.check(key, "GET", "/x", null, sig, ByteArray(0), now))
        assertEquals(Sign.Result.MISSING, Sign.check(key, "GET", "/x", now.toString(), null, ByteArray(0), now))
        assertEquals(Sign.Result.BAD, Sign.check(key, "GET", "/x", "soon", sig, ByteArray(0), now))
        assertEquals(Sign.Result.BAD, Sign.check(key, "GET", "/x", now.toString(), "!!!", ByteArray(0), now))
        assertEquals(Sign.Result.BAD, Sign.check(key, "GET", "/x", now.toString(), "", ByteArray(0), now))
        // a bad signature never reveals whether the clock was also off
        assertEquals(Sign.Result.BAD, Sign.check(key, "GET", "/v1/other", now.toString(), sig, ByteArray(0), now + 600_000))
    }

    @Test fun replayGuardRejectsTheSameSignatureTwice() {
        val g = ReplayGuard()
        val sig = Sign.sign(key, "POST", "/v1/jobs", now, "a".toByteArray())
        assertEquals(Sign.Result.OK, Sign.check(key, "POST", "/v1/jobs", now.toString(), sig, "a".toByteArray(), now, g))
        assertEquals(Sign.Result.REPLAY, Sign.check(key, "POST", "/v1/jobs", now.toString(), sig, "a".toByteArray(), now + 5, g))
        val sig2 = Sign.sign(key, "POST", "/v1/jobs", now + 1, "a".toByteArray())
        assertEquals(Sign.Result.OK, Sign.check(key, "POST", "/v1/jobs", (now + 1).toString(), sig2, "a".toByteArray(), now + 6, g))
    }

    @Test fun replayGuardForgetsOldSignaturesAndStaysBounded() {
        val g = ReplayGuard(keepMs = 1000, max = 3)
        assertTrue(g.firstTime("a", 0)); assertFalse(g.firstTime("a", 500))
        assertTrue("old entries are dropped", g.firstTime("a", 2000))
        assertTrue(g.firstTime("b", 2001)); assertTrue(g.firstTime("c", 2002)); assertTrue(g.firstTime("d", 2003))   // evicts "a"
        assertTrue("the oldest was evicted to stay within the limit", g.firstTime("a", 2004))
    }

    @Test fun helloProofsProveTheKey() {
        val p = Sign.helloProof(key, "abc", 1234)
        assertTrue(Sign.helloProofOk(key, "abc", 1234, p))
        assertFalse(Sign.helloProofOk(key, "abd", 1234, p))
        assertFalse(Sign.helloProofOk(key, "abc", 1235, p))
        assertFalse(Sign.helloProofOk(Sign.deriveKey(ByteArray(10)), "abc", 1234, p))
        assertFalse(Sign.helloProofOk(key, "abc", 1234, null))
        assertFalse(Sign.helloProofOk(key, "abc", 1234, "???"))
    }

    @Test fun keysDependOnTheSecret() {
        val a = Sign.deriveKey(ByteArray(10)); val b = Sign.deriveKey(ByteArray(10) { 1 })
        assertEquals(32, a.size); assertNotEquals(Sign.hex(a), Sign.hex(b)); assertArrayEquals(a, Sign.deriveKey(ByteArray(10)))
    }

    @Test fun throttleBlocksAfterFiveFailuresAndForgives() {
        val t = AuthThrottle()
        for (i in 1..4) { t.failed("10.0.0.2", 1000L * i); assertFalse(t.blocked("10.0.0.2", 1000L * i)) }
        t.failed("10.0.0.2", 5000)
        assertTrue(t.blocked("10.0.0.2", 5001)); assertTrue(t.blocked("10.0.0.2", 64_999)); assertFalse(t.blocked("10.0.0.2", 65_001))
        assertFalse("another address is not affected", t.blocked("10.0.0.3", 5001))
        t.failed("10.0.0.4", 0); t.failed("10.0.0.4", 1); t.ok("10.0.0.4")
        for (i in 1..4) t.failed("10.0.0.4", 10L + i)
        assertFalse("a success clears the count", t.blocked("10.0.0.4", 20))
        for (i in 1..4) t.failed("10.0.0.5", 100_000L * i)                                  // slow guesses are forgotten
        assertFalse(t.blocked("10.0.0.5", 400_001))
    }

    // ---------------------------------------------------------------------------------------------------- pairing
    @Test fun pairingCodeShape() {
        assertEquals("AAAA-AAAA-AAAA-AAAA", Pairing.format(ByteArray(10)))
        assertEquals("9999-9999-9999-9999", Pairing.format(ByteArray(10) { 0xFF.toByte() }))
        val rng = SecureRandom()
        val seen = HashSet<String>()
        repeat(300) {
            val s = Pairing.newSecret(rng)
            val c = Pairing.format(s)
            assertTrue(c, Regex("[A-HJ-NP-Z2-9]{4}(-[A-HJ-NP-Z2-9]{4}){3}").matches(c))
            assertArrayEquals(c, s, Pairing.parse(c))
            seen.add(c)
        }
        assertEquals("random secrets do not repeat", 300, seen.size)
    }

    @Test fun pairingCodeIsForgivingButNotSloppy() {
        val s = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
        val c = Pairing.format(s)
        assertArrayEquals(s, Pairing.parse(c.lowercase()))
        assertArrayEquals(s, Pairing.parse(c.replace("-", "")))
        assertArrayEquals(s, Pairing.parse("  " + c.replace("-", " ") + " \n"))
        assertNotNull(Pairing.parse(c))
        assertNull(Pairing.parse(""))
        assertNull(Pairing.parse(c.dropLast(1)))
        assertNull(Pairing.parse(c + "A"))
        for (ch in listOf("I", "O", "0", "1", "!", "é")) assertNull("a code with $ch is not valid", Pairing.parse(ch + c.drop(1)))
    }
}
