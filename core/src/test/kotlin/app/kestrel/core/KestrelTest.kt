package app.kestrel.core

import org.junit.Assert.assertEquals
import org.junit.Test

class KestrelTest {
    @Test
    fun pipelineWorks() {
        assertEquals("Kestrel", Kestrel.NAME)
    }
}
