package app.kestrel.core.prompt

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** IMG1 (owner 2026-11-21): the facts about a picked file say whether the file or the page is to blame, and carry no name. */
class FilePickFactsTest {
    private fun p(type: String = "image/png", bytes: Long = 830_000, w: Int = 600, h: Int = 973, ext: Boolean = true, free: Long = 300) = FilePickFacts.Picked(type, bytes, w, h, ext, free)

    @Test fun anOrdinaryScreenshotIsDescribedAsOneAndBlamesNoOne() {
        assertEquals("type=image/png kb=811 size=600x973 decodes=1 ext=1 freeMb=300", FilePickFacts.logLine(p()))
        assertTrue(FilePickFacts.verdict(p()), FilePickFacts.verdict(p()).startsWith("an ordinary picture (image/png, 600x973, 811 KB)"))
    }

    @Test fun anEmptyFileAFullCacheAndAPictureWithoutAReadableHeaderAreEachNamed() {
        assertTrue(FilePickFacts.verdict(p(bytes = 0, w = 0, h = 0)).contains("EMPTY file"))
        assertTrue(FilePickFacts.verdict(p(free = 12)).contains("only 12 MB free"))
        assertTrue(FilePickFacts.verdict(p(w = 0, h = 0)).contains("cannot read this picture's header"))
        assertTrue(FilePickFacts.verdict(p(ext = false)).contains("no file extension"))
    }

    @Test fun theLineCarriesOnlyNumbersAndATypeNeverAPathOrAName() {
        val l = FilePickFacts.logLine(p(type = "image/png; name=/storage/emulated/0/Pictures/Secret Name.png"))
        assertFalse(l, l.contains("Secret") || l.contains("Pictures") || l.contains(' ').let { false })
        assertTrue(l, l.startsWith("type=image/pngnamestorage"))      // the filter drops spaces, semicolons and equal signs; it is cut at 40 characters
        assertTrue(FilePickFacts.logLine(p(type = "")).startsWith("type=none "))
        assertTrue(FilePickFacts.logLine(p(bytes = -1)).contains("kb=-1"))
    }
}
