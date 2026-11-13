package app.kestrel.browser.ui

import android.content.Context
import android.graphics.Bitmap
import app.kestrel.core.session.LastLookMeta
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

/** The files of the last look (EY-1): a picture and its record, kept apart from the cache, replaced whole, trimmed to the pages worth keeping, and wiped on request. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class LastLookStoreTest {
    private val ctx: Context get() = RuntimeEnvironment.getApplication()
    private fun meta(url: String = "https://news.test/story", sy: Int = 640) = LastLookMeta(url, sy, 8, 12, 1_700_000_000_000L, "160.0", "reaper1:0:16", false, false)
    private fun bmp(): Bitmap = Bitmap.createBitmap(8, 12, Bitmap.Config.ARGB_8888)
    private val dir: File get() = File(ctx.filesDir, "lastlook")

    @Before fun clean() { LastLook.clearAll(ctx) }

    @Test fun aSavedLookComesBackWithItsRecord() {
        assertTrue(LastLook.save(ctx, 7, bmp(), meta()))
        val m = LastLook.meta(ctx, 7)!!
        assertEquals("https://news.test/story", m.url); assertEquals(640, m.scrollY)
        assertNotNull(LastLook.load(ctx, 7, m))
        assertEquals(1, LastLook.count(ctx))
    }

    @Test fun noRecordMeansNoLook() {
        assertNull(LastLook.meta(ctx, 99))
        assertEquals(0, LastLook.count(ctx))
    }

    @Test fun aNewSaveReplacesTheOldOneForTheSameTab() {
        assertTrue(LastLook.save(ctx, 7, bmp(), meta("https://news.test/one", 10)))
        assertTrue(LastLook.save(ctx, 7, bmp(), meta("https://news.test/two", 20)))
        val m = LastLook.meta(ctx, 7)!!
        assertEquals("https://news.test/two", m.url); assertEquals(20, m.scrollY)
        assertEquals(1, LastLook.count(ctx))
    }

    @Test fun onlyTheNamedTabsStayAndStrayFilesGo() {
        for (id in listOf(1L, 2L, 3L)) assertTrue(LastLook.save(ctx, id, bmp(), meta("https://news.test/$id")))
        File(dir, "notes.txt").writeText("not a picture")
        LastLook.retain(ctx, setOf(2L))
        assertNull(LastLook.meta(ctx, 1)); assertNotNull(LastLook.meta(ctx, 2)); assertNull(LastLook.meta(ctx, 3))
        assertFalse(File(dir, "notes.txt").exists())
        assertFalse("the picture of a dropped tab is gone too", File(dir, "1.webp").exists())
    }

    @Test fun deleteAndClearAllRemoveBothFiles() {
        assertTrue(LastLook.save(ctx, 4, bmp(), meta())); assertTrue(LastLook.save(ctx, 5, bmp(), meta()))
        LastLook.delete(ctx, 4)
        assertNull(LastLook.meta(ctx, 4)); assertFalse(File(dir, "4.webp").exists()); assertNotNull(LastLook.meta(ctx, 5))
        LastLook.clearAll(ctx)
        assertNull(LastLook.meta(ctx, 5)); assertEquals(0, LastLook.count(ctx))
    }

    @Test fun aRecordThatIsNotWholeIsNeverUsed() {
        dir.mkdirs()
        File(dir, "8.json").writeText("""{"v":1,"url":"https://news.test/story","w":8""")   // cut off in the middle of the write
        File(dir, "8.webp").writeBytes(ByteArray(10))
        assertNull(LastLook.meta(ctx, 8))
        assertNull("a picture with no record is not a look", LastLook.meta(ctx, 9))
    }

    @Test fun theLookIsKeptInTheAppsOwnFilesNeverInTheCache() {
        assertTrue(LastLook.save(ctx, 1, bmp(), meta()))
        assertTrue(File(ctx.filesDir, "lastlook/1.json").isFile)
        assertFalse(File(ctx.cacheDir, "lastlook").exists())
    }
}
