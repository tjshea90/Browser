package app.kestrel.browser.diag

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

/** LAB5: the doomed-cache purge removes what the engine left in cache2/doomed and cache2/trash*, and never walks (or touches) the live cache2/entries folder. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class StorageInfoPurgeTest {
    private val ctx get() = RuntimeEnvironment.getApplication()
    private lateinit var cache2: File

    @Before fun setUp() {
        cache2 = File(ctx.cacheDir, "cache2").apply { mkdirs() }
        File(cache2, "entries").mkdirs(); File(cache2, "doomed").mkdirs(); File(cache2, "trash123").mkdirs()
    }

    @After fun tearDown() { cache2.deleteRecursively() }

    private fun put(dir: String, name: String, bytes: Int) = File(File(cache2, dir), name).also { it.writeBytes(ByteArray(bytes) { 7 }) }

    @Test fun removesDoomedAndTrashButKeepsEveryLiveEntry() {
        val live = (1..50).map { put("entries", "E%03d".format(it), 100) }
        val d1 = put("doomed", "a", 1000); val d2 = put("doomed", "b", 2000)
        val t1 = put("trash123", "c", 3000)
        val freed = StorageInfo.purgeDoomedCache(ctx)
        assertEquals(6000L, freed)
        assertTrue(!d1.exists() && !d2.exists() && !t1.exists())
        assertTrue("a live cache entry was touched", live.all { it.exists() && it.length() == 100L })
    }

    @Test fun neverListsTheLiveEntriesFolder() {
        // a directory that cannot be listed (a regular file where the folder should be) must not matter: entries is skipped before any listing,
        // and a doomed file next to it is still purged. (A stat-per-file walk of 30k entries was the cost this removes.)
        File(cache2, "entries").deleteRecursively()
        val trap = File(cache2, "entries").also { it.createNewFile() }
        val d = put("doomed", "x", 500)
        assertEquals(500L, StorageInfo.purgeDoomedCache(ctx))
        assertTrue(!d.exists())
        assertTrue(trap.exists())
    }

    @Test fun otherFoldersNamedEntriesStillGetWalked() {
        // only cache2/entries is the live cache; a folder with that name elsewhere (the image previews, say) holds no doomed files either way, but the walk stays generic
        val other = File(ctx.cacheDir, "previews/entries").apply { mkdirs() }
        File(other, "keep").writeBytes(ByteArray(10))
        val d = put("doomed", "y", 40)
        assertEquals(40L, StorageInfo.purgeDoomedCache(ctx))
        assertTrue(!d.exists())
        assertTrue(File(other, "keep").exists())
    }
}
