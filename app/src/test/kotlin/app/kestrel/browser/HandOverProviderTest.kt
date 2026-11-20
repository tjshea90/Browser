package app.kestrel.browser

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

/** Owner request 17: ONE file leaves the app, read only, and only a file directly inside cache/handover. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class HandOverProviderTest {
    private val app get() = RuntimeEnvironment.getApplication()
    private fun provider() = Robolectric.setupContentProvider(HandOverProvider::class.java, HandOverProvider.authority(app))

    @Test fun theFileIsReadableThroughTheProviderAndNothingElseIs() {
        provider()
        val f = File(HandOverProvider.dir(app), "kestrel-learned.txt"); f.writeText("KESTREL-KEYBOARD-LEARNING 1\nw hello 3\n")
        val uri = HandOverProvider.uriFor(app, f.name)
        assertEquals("KESTREL-KEYBOARD-LEARNING 1\nw hello 3\n", app.contentResolver.openInputStream(uri)!!.use { String(it.readBytes()) })
        val c = app.contentResolver.query(uri, null, null, null, null)!!
        assertTrue(c.moveToFirst()); assertEquals("kestrel-learned.txt", c.getString(0)); c.close()
        // a file outside the folder, a path that climbs out of it, and a missing file are refused
        File(app.cacheDir, "secret.txt").writeText("no")
        for (bad in listOf("secret.txt", "..%2Fsecret.txt", "..", "missing.txt")) {
            try { app.contentResolver.openInputStream(Uri.parse("content://" + HandOverProvider.authority(app) + "/" + bad))?.close(); fail("served $bad") } catch (e: Exception) { /* refused */ }
        }
        assertNull(app.contentResolver.query(Uri.parse("content://" + HandOverProvider.authority(app) + "/missing.txt"), null, null, null, null))
    }

    @Test fun nobodyCanWriteDeleteOrInsert() {
        provider()
        val uri = HandOverProvider.uriFor(app, "kestrel-learned.txt"); File(HandOverProvider.dir(app), "kestrel-learned.txt").writeText("x")
        try { app.contentResolver.openOutputStream(uri, "w")?.close(); fail("written") } catch (e: Exception) { /* read only */ }
        try { app.contentResolver.delete(uri, null, null); fail("deleted") } catch (e: UnsupportedOperationException) { /* no */ }
        try { app.contentResolver.insert(uri, android.content.ContentValues()); fail("inserted") } catch (e: UnsupportedOperationException) { /* no */ }
    }
}
