package app.kestrel.core.prompt

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream

/** PERF14 (owner 2026-11-23: "Photos still won't attach to claude website"): the copy of a picked file the engine can always read, under a safe name, bounded, and cleaned up. */
class PickStoreTest {
    @get:Rule val tmp = TemporaryFolder()

    @Test fun aNameIsMadeSafeAndGetsItsExtensionFromTheType() {
        assertEquals("Screenshot_2026.png", PickStore.safeName("Screenshot_2026.png", "image/png"))
        assertEquals("photo.jpg", PickStore.safeName("photo", "image/jpeg"))
        assertEquals("passwd", PickStore.safeName("../../etc/passwd", null))
        assertEquals("evil.png", PickStore.safeName("..\\..\\evil.png", "image/png"))
        assertEquals("file.png", PickStore.safeName("", "image/png"))
        assertEquals("file", PickStore.safeName(null, null))
        assertEquals("hidden.txt", PickStore.safeName(".hidden.txt", "text/plain"))
        assertFalse(PickStore.safeName("a<b>c:d\"e|f?g*h.png", "image/png").any { it in "<>:\"|?*" })
        assertFalse(PickStore.safeName("tab\tand\nnewline.png", "image/png").any { it.code < 32 })
        assertEquals("a.png; name=x.png", PickStore.safeName("a.png; name=x", "image/png"))   // only a short last part counts as an extension
    }

    @Test fun aLongNameIsCutButTheExtensionStays() {
        val n = PickStore.safeName("x".repeat(300) + ".webp", "image/webp")
        assertTrue(n, n.length <= PickStore.MAX_NAME)
        assertTrue(n, n.endsWith(".webp"))
    }

    @Test fun theTypeDecidesTheExtensionOnlyWhenTheNameHasNone() {
        assertEquals("a.pdf", PickStore.safeName("a", "application/pdf"))
        assertEquals("a.bin", PickStore.safeName("a.bin", "application/pdf"))
        assertEquals("a", PickStore.safeName("a", "application/x-unknown"))
        assertEquals("png", PickStore.extFor("image/PNG; charset=binary"))
    }

    @Test fun theCopyIsByteForByteInItsOwnFolderUnderASafeName() {
        val root = tmp.newFolder("picks")
        val bytes = ByteArray(300_000) { (it * 31).toByte() }
        val f = PickStore.copy(root, "ab12cd", ByteArrayInputStream(bytes), "My Photo", "image/png")
        assertNotNull(f)
        assertEquals("ab12cd", f!!.parentFile!!.name)
        assertEquals("My Photo.png", f.name)
        assertTrue(f.readBytes().contentEquals(bytes))
    }

    @Test fun aFileThatIsTooBigIsRefusedAndNothingIsLeftBehind() {
        val root = tmp.newFolder("picks")
        val big = ByteArrayInputStream(ByteArray(10_000))
        assertNull(PickStore.copy(root, "big1", big, "big.bin", null, maxBytes = 9_000))
        assertEquals(0, root.listFiles()!!.size)
    }

    @Test fun anIdThatIsNotAPlainTokenIsRefused() {
        val root = tmp.newFolder("picks")
        assertNull(PickStore.copy(root, "../x", ByteArrayInputStream(ByteArray(3)), "a.txt", null))
        assertNull(PickStore.copy(root, "", ByteArrayInputStream(ByteArray(3)), "a.txt", null))
        assertEquals(0, root.listFiles()!!.size)
    }

    @Test fun aFailingStreamLeavesNothingBehind() {
        val root = tmp.newFolder("picks")
        val bad = object : java.io.InputStream() { override fun read(): Int = throw java.io.IOException("boom"); override fun read(b: ByteArray): Int = throw java.io.IOException("boom") }
        assertNull(PickStore.copy(root, "bad1", bad, "a.png", "image/png"))
        assertEquals(0, root.listFiles()!!.size)
    }

    @Test fun oldCopiesAreSweptAndTheNewOnesStayAndWipeRemovesAll() {
        val root = tmp.newFolder("picks")
        PickStore.copy(root, "old1", ByteArrayInputStream(ByteArray(5)), "a.txt", null)
        PickStore.copy(root, "new1", ByteArrayInputStream(ByteArray(5)), "b.txt", null)
        val now = 10_000_000L
        java.io.File(root, "old1").setLastModified(now - 7 * 3_600_000L)
        java.io.File(root, "new1").setLastModified(now - 60_000L)
        assertEquals(1, PickStore.sweep(root, 6 * 3_600_000L, now))
        assertEquals(listOf("new1"), root.listFiles()!!.map { it.name })
        assertEquals(1, PickStore.wipe(root))
        assertEquals(0, root.listFiles()!!.size)
        assertEquals(0, PickStore.wipe(java.io.File(root, "missing")))
    }
}
