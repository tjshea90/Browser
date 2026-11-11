package app.kestrel.browser.download

import app.kestrel.core.download.DEST_APP_PRIVATE
import app.kestrel.core.download.DEST_PUBLIC
import app.kestrel.core.download.Sink
import app.kestrel.core.download.SinkFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** Where a download goes: the public Downloads folder (MediaStore) or the app's private staging folder (update APKs); and that the private folder cannot be escaped. */
class AppSinksTest {
    @get:Rule val tmp = TemporaryFolder()

    private class Recorder(val tag: String) : SinkFactory {
        val log = ArrayList<String>()
        override fun create(name: String, mime: String?, total: Long, priv: Boolean): Sink { log.add("$tag.create4 $name"); throw IllegalStateException("not needed") }
        override fun create(name: String, mime: String?, total: Long, priv: Boolean, kind: Int): Sink { log.add("$tag.create $name kind=$kind"); throw IllegalStateException("recorded") }
        override fun reopen(uri: String, total: Long): Sink? { log.add("$tag.reopen $uri"); return null }
        override fun delete(uri: String): Boolean { log.add("$tag.delete $uri"); return true }
    }

    private fun create(f: SinkFactory, kind: Int) { try { f.create("x.apk", null, 10, false, kind) } catch (e: IllegalStateException) { /* the recorder always throws after logging */ } }

    @Test fun theKindPicksTheFactory() {
        val media = Recorder("media"); val files = Recorder("files")
        val sinks = AppSinks(media, files)
        create(sinks, DEST_APP_PRIVATE); create(sinks, DEST_PUBLIC)
        assertEquals(listOf("files.create x.apk kind=2"), files.log)
        assertEquals(listOf("media.create x.apk kind=0"), media.log)
    }

    @Test fun reopenAndDeleteFollowTheUriScheme() {
        val media = Recorder("media"); val files = Recorder("files")
        val sinks = AppSinks(media, files)
        sinks.reopen("file:/data/updates/a.apk.part", 1); sinks.delete("file:/data/updates/a.apk")
        sinks.reopen("content://media/external/downloads/42", 1); sinks.delete("content://media/external/downloads/42")
        assertEquals(listOf("files.reopen file:/data/updates/a.apk.part", "files.delete file:/data/updates/a.apk"), files.log)
        assertEquals(listOf("media.reopen content://media/external/downloads/42", "media.delete content://media/external/downloads/42"), media.log)
    }

    @Test fun deleteNeverLeavesTheStagingFolder() {
        val dir = tmp.newFolder("updates")
        val outside = tmp.newFile("precious.txt")
        val inside = java.io.File(dir, "a.apk").also { it.writeText("x") }
        val f = FileSinks(dir)
        assertFalse(f.delete(outside.toURI().toString()))
        assertTrue(outside.exists())
        assertFalse(f.delete(java.io.File(dir, "../precious.txt").toURI().toString()))
        assertTrue(outside.exists())
        assertNull(f.reopen(outside.toURI().toString(), 1))
        assertTrue(f.delete(inside.toURI().toString()))
        assertFalse(inside.exists())
        assertFalse(f.delete("content://media/external/downloads/1")) // not ours
    }
}
