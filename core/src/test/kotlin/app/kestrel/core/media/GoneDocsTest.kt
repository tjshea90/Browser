package app.kestrel.core.media

import org.junit.Assert.assertEquals
import org.junit.Test

/** RVW2 F19: a probe that started before its document left must not bring the row back. */
class GoneDocsTest {
    @Test fun aProbeThatStartedBeforeItsDocumentLeftDoesNotBringTheRowBack() {
        val g = GoneDocs(max = 3)
        g.gone("7|https://player.test/e/1", 1_000)
        assertEquals("the probe began at 900, the document left at 1000: drop its answer", true, g.goneSince("7|https://player.test/e/1", 900))
        assertEquals("a probe of a NEW document with the same address began later: welcome", false, g.goneSince("7|https://player.test/e/1", 1_001))
        assertEquals("another document", false, g.goneSince("7|https://player.test/e/2", 0))
        for (i in 2..5) g.gone("7|d$i", 2_000L + i)
        assertEquals("the oldest entry is forgotten (bounded)", false, g.goneSince("7|https://player.test/e/1", 0))
    }
}
