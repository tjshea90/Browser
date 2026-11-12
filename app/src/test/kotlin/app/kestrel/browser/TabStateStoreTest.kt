package app.kestrel.browser

import android.content.Context
import app.kestrel.browser.data.TabStateStore
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

/** The saved state of a tab (back stack + scroll position) that lets a swiped-away app come back where it was. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class TabStateStoreTest {
    private val ctx: Context get() = RuntimeEnvironment.getApplication()
    private val sample = """{"history":{"entries":[{"url":"https://a.example/","title":"A"},{"url":"https://b.example/x","title":"B","formdata":{"id":{"pw":"secret"}}}],"index":2},"scroll":"0,421","formdata":{"id":{"msg":"hello"},"xpath":{}},"scrolldata":{"scroll":"0,421","children":[{"formdata":{"id":{"q":"x"}}}]}}"""

    @Before fun clean() { TabStateStore.wipe(ctx); TabStateStore.dir(ctx).deleteRecursively() }

    @Test fun typedFormTextIsRemovedAtEveryDepth() {
        val out = TabStateStore.sanitize(sample)
        assertNotNull(out)
        assertFalse(out!!.contains("formdata"))
        assertFalse(out.contains("secret"))
        assertFalse(out.contains("hello"))
        val back = org.json.JSONObject(out) // org.json writes "/" as "\/": compare the parsed values, not the text
        assertEquals("0,421", back.getString("scroll"))
        assertEquals("https://b.example/x", back.getJSONObject("history").getJSONArray("entries").getJSONObject(1).getString("url"))
        assertEquals(2, back.getJSONObject("history").getInt("index"))
    }

    @Test fun notJsonTooBigOrEmptyIsNotKept() {
        assertNull(TabStateStore.sanitize(""))
        assertNull(TabStateStore.sanitize("not json"))
        assertNull(TabStateStore.sanitize("[1,2,3]"))
        assertNull("no history, nothing to restore", TabStateStore.sanitize("""{"scroll":"0,1"}"""))
        assertNull(TabStateStore.sanitize("""{"history":"${"x".repeat(TabStateStore.MAX_CHARS + 10)}"}"""))
        assertNull("far too big to parse", TabStateStore.sanitize("x".repeat(TabStateStore.MAX_CHARS * 4 + 1)))
    }

    @Test fun writeReadAndDelete() {
        TabStateStore.write(ctx, 7, "{\"history\":1}", "160.0")
        assertEquals("{\"history\":1}", TabStateStore.read(ctx, 7))
        assertNull(TabStateStore.read(ctx, 8))
        TabStateStore.delete(ctx, 7)
        assertNull(TabStateStore.read(ctx, 7))
    }

    @Test fun anotherEngineVersionEmptiesTheFolder() {
        assertFalse("nothing saved yet", TabStateStore.useEngine(ctx, "160.0"))
        TabStateStore.write(ctx, 1, "{\"history\":1}", "160.0")
        assertTrue(TabStateStore.useEngine(ctx, "160.0"))
        assertEquals("{\"history\":1}", TabStateStore.read(ctx, 1))
        assertFalse(TabStateStore.useEngine(ctx, "161.0"))
        assertNull("the old version's file is gone", TabStateStore.read(ctx, 1))
    }

    @Test fun retainKeepsOnlyTheListedTabsAndTheMarker() {
        for (id in 1L..4L) TabStateStore.write(ctx, id, "{\"history\":$id}", "160.0")
        File(TabStateStore.dir(ctx), "junk.txt").writeText("x")
        TabStateStore.retain(ctx, setOf(2L, 4L))
        assertNull(TabStateStore.read(ctx, 1)); assertNotNull(TabStateStore.read(ctx, 2)); assertNull(TabStateStore.read(ctx, 3)); assertNotNull(TabStateStore.read(ctx, 4))
        assertFalse(File(TabStateStore.dir(ctx), "junk.txt").exists())
        assertTrue("the engine marker survives", TabStateStore.useEngine(ctx, "160.0"))
    }

    @Test fun anOversizedFileOnDiskIsIgnored() {
        TabStateStore.write(ctx, 3, "{}", "160.0")
        File(TabStateStore.dir(ctx), "3.json").writeText("x".repeat(TabStateStore.MAX_CHARS * 3 + 5))
        assertNull(TabStateStore.read(ctx, 3))
    }
}
