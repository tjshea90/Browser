package app.kestrel.browser.ui

import android.content.Context
import android.graphics.Bitmap
import app.kestrel.browser.web.Tab
import app.kestrel.core.ui.ThumbRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

/** TP1 (owner 2026-11-18): a tab's preview is the picture of the page the tab is on, never of a site it visited before. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class TabPreviewTest {
    private val ctx: Context get() = RuntimeEnvironment.getApplication()
    private fun bmp() = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)

    private fun tabWithPreviewOf(url: String): Tab {
        val t = Tab(7); t.url = url
        t.thumb = bmp(); t.thumbPage = ThumbRule.pageOf(url); t.thumbAt = 123L
        return t
    }

    @Test fun aNewPageTakesThePreviewOfTheOldOneAwayAtOnce() {
        val t = tabWithPreviewOf("https://news.example/story/1")
        t.url = "https://shop.example/cart"
        assertNull(t.thumb); assertEquals("", t.thumbPage); assertEquals(0L, t.thumbAt)
    }

    @Test fun aFragmentOrTheSameAddressKeepsThePreview() {
        val t = tabWithPreviewOf("https://news.example/story/1")
        t.url = "https://news.example/story/1#comments"; assertNotNull(t.thumb)
        t.url = "https://news.example/story/1"; assertNotNull(t.thumb)
    }

    @Test fun anotherQueryIsAnotherPageAndGoingBlankClearsToo() {
        val t = tabWithPreviewOf("https://tube.example/watch?v=1")
        t.url = "https://tube.example/watch?v=2"; assertNull(t.thumb)
        val u = tabWithPreviewOf("https://news.example/a"); u.url = ""; assertNull(u.thumb)
    }

    @Test fun aStoredPreviewIsLoadedOnlyForThePageItWasTakenFor() {
        val a = "https://news.example/story/1"; val b = "https://shop.example/cart"
        ThumbStore.save(ctx, 7, bmp(), ThumbRule.pageOf(a))
        val webp = File(File(ctx.cacheDir, "thumbs"), "7.webp"); val rec = File(File(ctx.cacheDir, "thumbs"), "7.page")
        assertTrue(webp.isFile && rec.isFile)
        ThumbStore.load(ctx, 7, a)                                  // the right page: the files stay
        assertTrue("kept for the right page", webp.isFile && rec.isFile)
        assertNull("the tab is on another page now", ThumbStore.load(ctx, 7, b))
        assertFalse("the stale files are deleted", webp.isFile || rec.isFile)
    }

    @Test fun aPreviewStoredBeforeThisRuleHasNoRecordOfItsPageAndIsNotTrusted() {
        val d = File(ctx.cacheDir, "thumbs"); d.mkdirs()
        val old = File(d, "9.webp"); old.writeBytes(byteArrayOf(1, 2, 3))
        assertNull(ThumbStore.load(ctx, 9, "https://news.example/story/1"))
        assertFalse(old.isFile)
    }

    @Test fun noPreviewIsStoredForTheNewTabPage() {
        ThumbStore.save(ctx, 11, bmp(), "")
        assertFalse(File(File(ctx.cacheDir, "thumbs"), "11.webp").isFile)
    }
}
