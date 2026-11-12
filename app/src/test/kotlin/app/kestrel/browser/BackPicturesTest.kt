package app.kestrel.browser

import android.graphics.Bitmap
import app.kestrel.browser.ui.BackPictures
import app.kestrel.browser.ui.ThumbStore
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The pictures Back shows when the engine has to load a page again (memory only, a handful, least recently used out first). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class BackPicturesTest {
    private fun bmp(w: Int = 8, h: Int = 8) = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565)
    @After fun clean() = BackPictures.clear()

    @Test fun keyedByTabAndAddressWithoutTheFragment() {
        val a = bmp()
        BackPictures.put(1, "https://site.test/page#top", a)
        assertSame(a, BackPictures.get(1, "https://site.test/page"))
        assertSame(a, BackPictures.get(1, "https://site.test/page#other"))
        assertNull("another tab has its own", BackPictures.get(2, "https://site.test/page"))
        assertNull("another page", BackPictures.get(1, "https://site.test/other"))
    }

    @Test fun onlyTheLastFewAreKeptAndTheLeastRecentlyUsedGoesFirst() {
        for (i in 1..BackPictures.MAX) BackPictures.put(1, "https://site.test/p$i", bmp())
        assertNotNull(BackPictures.get(1, "https://site.test/p1")) // p1 is now the most recently used
        BackPictures.put(1, "https://site.test/new", bmp())
        assertEquals(BackPictures.MAX, BackPictures.size())
        assertNotNull(BackPictures.get(1, "https://site.test/p1"))
        assertNull("p2 was the least recently used", BackPictures.get(1, "https://site.test/p2"))
    }

    @Test fun aClosedTabTakesItsPicturesWithIt() {
        BackPictures.put(1, "https://a.test/", bmp()); BackPictures.put(2, "https://b.test/", bmp()); BackPictures.put(1, "https://c.test/", bmp())
        BackPictures.dropTab(1)
        assertNull(BackPictures.get(1, "https://a.test/")); assertNull(BackPictures.get(1, "https://c.test/"))
        assertNotNull(BackPictures.get(2, "https://b.test/"))
        BackPictures.clear()
        assertEquals(0, BackPictures.size())
    }

    @Test fun aRecycledPictureIsNeverHandedOut() {
        val b = bmp(); BackPictures.put(1, "https://a.test/", b); b.recycle()
        assertNull(BackPictures.get(1, "https://a.test/"))
    }

    @Test fun oneCaptureMakesTheGridPreviewAndTheBackPicture() {
        val full = Bitmap.createBitmap(600, 973, Bitmap.Config.ARGB_8888)
        val (small, big) = ThumbStore.shrinkWithPicture(full)
        assertNotNull(small); assertNotNull(big)
        assertEquals(ThumbStore.WIDTH, small!!.width); assertEquals(ThumbStore.BACK_WIDTH, big!!.width)
        assertEquals(Bitmap.Config.RGB_565, big.config)
        assertTrue("same proportions", Math.abs(small.height * 2 - big.height) <= 2)
        assertTrue("the capture is recycled", full.isRecycled)
    }
}
