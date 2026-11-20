package app.kestrel.browser

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.FilterInputStream
import java.io.InputStream

/** RVW1 P11: the sized read gives the same bytes as the plain one, whatever available() says. */
class StreamBytesTest {
    private fun data(n: Int) = ByteArray(n) { (it * 31 + 7).toByte() }

    /** A stream that reports [claim] bytes available whatever it really holds, and hands out at most [chunk] bytes per read. */
    private class Liar(src: InputStream, val claim: Int, val chunk: Int) : FilterInputStream(src) {
        override fun available(): Int = claim
        override fun read(b: ByteArray, off: Int, len: Int): Int = super.read(b, off, minOf(len, chunk))
    }

    @Test fun anExactSizeIsOneAllocationWithTheRightBytes() {
        val d = data(100_000)
        assertArrayEquals(d, ByteArrayInputStream(d).readAllSized())
    }

    @Test fun anEmptyStreamAndAStreamThatSaysZeroStillWork() {
        assertEquals(0, ByteArrayInputStream(ByteArray(0)).readAllSized().size)
        val d = data(5_000)
        assertArrayEquals(d, Liar(ByteArrayInputStream(d), 0, 777).readAllSized())
    }

    @Test fun aStreamThatPromisesLessThanItHoldsLosesNothing() {
        val d = data(50_000)
        assertArrayEquals(d, Liar(ByteArrayInputStream(d), 1_000, 4096).readAllSized())
    }

    @Test fun aStreamThatPromisesMoreThanItHoldsGivesWhatItHas() {
        val d = data(3_000)
        assertArrayEquals(d, Liar(ByteArrayInputStream(d), 100_000, 512).readAllSized())
    }

    // RVW3 SC6: the release API answer is read with a cap
    @Test fun aCappedReadGivesAllBytesUpToTheCapAndRefusesMore() {
        val d = data(10_000)
        assertArrayEquals(d, ByteArrayInputStream(d).readCapped(10_000))
        assertArrayEquals(d, ByteArrayInputStream(d).readCapped(1_000_000))
        assertEquals(0, ByteArrayInputStream(ByteArray(0)).readCapped(10).size)
        try { ByteArrayInputStream(data(10_001)).readCapped(10_000); throw AssertionError("a body one byte over the cap must be refused") } catch (e: java.io.IOException) { /* expected */ }
    }

    @Test fun anEndlessBodyIsRefusedWithoutFillingTheHeap() {
        val endless = object : InputStream() {
            var served = 0L
            override fun read(): Int { served++; return 65 }
            override fun read(b: ByteArray, off: Int, len: Int): Int { java.util.Arrays.fill(b, off, off + len, 65); served += len; return len }
        }
        try { endless.readCapped(2_000_000); throw AssertionError("an endless body must be refused") } catch (e: java.io.IOException) { /* expected */ }
        assertEquals("it stopped soon after the cap (one buffer over)", true, endless.served <= 2_000_000L + 8192L)
    }
}
