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
}
