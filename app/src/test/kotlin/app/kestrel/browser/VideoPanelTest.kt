package app.kestrel.browser

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import app.kestrel.browser.ui.Pal
import app.kestrel.browser.ui.VideoPanel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** The "Save video" sheet: the source's name shown (tap to rename), one chosen format, one Download button, small files tucked away. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class VideoPanelTest {
    private val ctx: Context get() = RuntimeEnvironment.getApplication()
    private val downloads = ArrayList<Pair<String, String>>()

    private fun panel() = VideoPanel(ctx, Pal(ctx, false), object : VideoPanel.Listener { override fun download(choice: VideoPanel.Choice, stem: String) { downloads.add(choice.id to stem) } })

    private fun all(v: View, out: MutableList<View> = ArrayList()): List<View> { out.add(v); if (v is ViewGroup) for (i in 0 until v.childCount) all(v.getChildAt(i), out); return out }
    private fun texts(p: View): List<String> = all(p).filterIsInstance<TextView>().map { it.text.toString() }.filter { it.isNotEmpty() }
    private fun byText(p: View, startsWith: String): View = all(p).first { it is TextView && it.text.toString().startsWith(startsWith) }
    private fun clickAncestor(v: View) { var x: View? = v; while (x != null && !x.isClickable) x = x.parent as? View; assertNotNull("something clickable around '${(v as? TextView)?.text}'", x); x!!.performClick() }

    private fun choice(id: String, title: String, detail: String, stem: String, ext: String = "mp4", badge: String? = null) = VideoPanel.Choice(id, title, detail, stem, ext, id, badge = badge)

    private val rows = listOf(
        choice("f|1080", "1080p", "MP4  ·  148 MB  ·  2:31", "Holiday_Clip_1080p", badge = "THIS VIDEO"),
        choice("f|720", "720p", "MP4  ·  82 MB  ·  2:31", "Holiday_Clip_720p"),
        choice("f|aud", "Audio only", "M4A  ·  3 MB", "Holiday_Clip", ext = "m4a"),
    )
    private val other = listOf(choice("f|trk", "Video file", "MP4  ·  300 KB", "get", badge = null), choice("f|ad", "Video file", "MP4  ·  2 MB", "pre"))
    private fun model(extras: List<Pair<String, () -> Unit>> = emptyList()) = VideoPanel.Model("Great Holiday - site.example", rows, other, null, null, extras, sizeText = { c -> if (c.id == "f|1080") "148 MB" else "82 MB" })

    @Test fun showsTheSourcesNameTheBestFormatChosenAndOneDownloadButton() {
        val p = panel(); p.show(model())
        val t = texts(p)
        assertTrue(t.contains("Holiday_Clip_1080p.mp4")); assertTrue(t.contains("RENAME")); assertTrue(t.contains("1080p")); assertTrue(t.contains("THIS VIDEO"))
        assertTrue("the size is on the button", t.any { it.startsWith("Download") && it.contains("148 MB") })
    }

    @Test fun smallFilesAreTuckedAwayUntilAskedFor() {
        val p = panel(); p.show(model())
        assertFalse("closed: its rows are not there", texts(p).any { it.contains("300 KB") })
        clickAncestor(byText(p, "▸  Other files on this page (2)"))
        assertTrue("open now", texts(p).any { it.contains("300 KB") })
        assertTrue(texts(p).any { it.startsWith("▾  Other files") })
    }

    @Test fun choosingAnotherFormatChangesTheNameAndTheButtonAndDownloadUsesIt() {
        val p = panel(); p.show(model())
        clickAncestor(byText(p, "720p"))
        val t = texts(p)
        assertTrue(t.contains("Holiday_Clip_720p.mp4")); assertTrue(t.any { it.startsWith("Download") && it.contains("82 MB") })
        clickAncestor(byText(p, "Download"))
        assertEquals(listOf("f|720" to "Holiday_Clip_720p"), downloads)
    }

    @Test fun theChoiceSurvivesTheSheetBeingRedrawn() {
        val p = panel(); p.show(model())
        clickAncestor(byText(p, "720p"))
        p.show(model()) // a stream's quality list arrived: the sheet is redrawn with the same rows
        assertTrue(texts(p).contains("Holiday_Clip_720p.mp4"))
        // a row that disappears falls back to the first one
        p.show(VideoPanel.Model("x", rows.filter { it.id != "f|720" }, emptyList()))
        assertTrue(texts(p).contains("Holiday_Clip_1080p.mp4"))
    }

    @Test fun extraButtonsAreThereAndWork() {
        var copied = 0
        val p = panel(); p.show(model(listOf("Copy video address" to { copied++ })))
        clickAncestor(byText(p, "Copy video address"))
        assertEquals(1, copied)
    }

    @Test fun anEmptySheetDoesNotOfferADownload() {
        val p = panel(); p.show(VideoPanel.Model("x", emptyList(), emptyList(), busy = "Reading the quality list…"))
        val b = byText(p, "Download")
        assertFalse(b.isEnabled)
        assertTrue(texts(p).contains("Reading the quality list…"))
    }
}
