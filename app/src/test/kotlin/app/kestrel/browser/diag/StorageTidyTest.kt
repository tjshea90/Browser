package app.kestrel.browser.diag

import android.content.Context
import app.kestrel.browser.data.TabStateStore
import app.kestrel.browser.update.UpdateManager
import app.kestrel.core.update.UpdateOffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

/** The storage tidy (AO4): what it removes, what it must never touch. Real files in the Robolectric app folders. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class StorageTidyTest {
    private val ctx: Context get() = RuntimeEnvironment.getApplication()
    private val day = 24L * 3600 * 1000

    private fun plant(dir: File, name: String, bytes: Int, ageDays: Long): File {
        dir.mkdirs()
        return File(dir, name).also { it.writeBytes(ByteArray(bytes)); it.setLastModified(System.currentTimeMillis() - ageDays * day) }
    }

    private fun offer(code: Long, apk: String) = UpdateOffer("kestrel-v$code", "9.9.$code", code, apk, "https://x.test/$apk", 5000, null, "", "https://x.test/r")

    @Before fun clean() {
        ctx.cacheDir.deleteRecursively(); ctx.filesDir.deleteRecursively()
        ctx.getSharedPreferences("kestrel_tidy", 0).edit().clear().commit()
        ctx.getSharedPreferences("kestrel_update", 0).edit().clear().commit()
        UpdateManager.setStateForTest(UpdateManager.State(UpdateManager.Phase.IDLE))
    }

    // ---------------------------------------------------------------- the engine's own files

    @Test fun engineCacheEntriesNobodyRewroteForThreeWeeksGoAndYoungOnesStay() {
        val c2 = File(ctx.cacheDir, "profile.default/cache2")
        plant(File(c2, "entries"), "OLD", 1000, 30)
        val young = plant(File(c2, "entries"), "YOUNG", 500, 2)
        plant(File(c2, "doomed"), "D", 700, 0)       // dropped entries go whatever their age
        plant(File(c2, "trash123"), "T", 300, 0)
        val freed = StorageTidy.pruneEngineFiles(ctx)
        assertEquals(1000L + 700 + 300, freed)
        assertTrue("a recent entry is what a current page may be reusing", young.isFile)
        assertFalse(File(c2, "entries/OLD").exists())
    }

    @Test fun theEnginesProfileIsFoundWhateverItsFolderIsCalled() {
        val a = File(ctx.cacheDir, "a/b/cache2")
        plant(File(a, "entries"), "OLD", 10, 40)
        assertEquals(10L, StorageTidy.pruneEngineFiles(ctx))
        assertEquals("only cache2 folders are touched", 1, plant(File(ctx.cacheDir, "other"), "x", 5, 99).parentFile!!.listFiles()!!.size)
    }

    @Test fun crashReportsOlderThanAWeekGoButProfileDataNeverDoes() {
        val moz = File(ctx.filesDir, "mozilla")
        plant(File(moz, "Crash Reports/pending"), "a.dmp", 100, 10)
        val fresh = plant(File(moz, "Crash Reports/pending"), "b.dmp", 100, 1)
        plant(File(moz, "prof.default/minidumps"), "c.dmp", 200, 8)
        plant(File(moz, "prof.default/crashes/events"), "e", 50, 9)
        val cookies = plant(File(moz, "prof.default"), "cookies.sqlite", 4000, 200)   // old but it is the owner's logins
        val prefs = plant(File(moz, "prof.default"), "prefs.js", 40, 200)
        assertEquals(100L + 200 + 50, StorageTidy.pruneEngineFiles(ctx))
        assertTrue(fresh.isFile)
        assertTrue("site data and logins are not stale items", cookies.isFile && prefs.isFile)
    }

    @Test fun anEmptyOrMissingEngineFolderIsNotAnError() {
        assertEquals(0L, StorageTidy.pruneEngineFiles(ctx))
        File(ctx.cacheDir, "p/cache2").mkdirs()
        assertEquals(0L, StorageTidy.pruneEngineFiles(ctx))
    }

    // ---------------------------------------------------------------- the update folder

    private fun updates() = File(ctx.filesDir, "updates")

    @Test fun anUpdateStillAheadKeepsOnlyItsInstallerAndItsHalfWrittenFile() {
        UpdateManager.setStateForTest(UpdateManager.State(UpdateManager.Phase.AVAILABLE, offer(Long.MAX_VALUE / 2, "kestrel-9.9.9.apk")))
        val keep1 = plant(updates(), "kestrel-9.9.9.apk", 400, 1)
        val keep2 = plant(updates(), "kestrel-9.9.9.apk.part", 200, 1)
        plant(updates(), "kestrel-0.1.0.apk", 300, 30)
        plant(updates(), "leftover.tmp", 100, 30)
        assertEquals(400L, UpdateManager.sweepFiles(ctx))
        assertTrue(keep1.isFile && keep2.isFile)
        assertEquals(2, updates().listFiles()!!.size)
    }

    @Test fun onceTheUpdateIsInstalledTheInstallerIsDeletedAndTheEngineForgetsTheJob() {
        val sp = ctx.getSharedPreferences("kestrel_update", 0)
        sp.edit().putLong("job", 12L).putString("verified", "kestrel-v9").commit()
        UpdateManager.setStateForTest(UpdateManager.State(UpdateManager.Phase.UP_TO_DATE, null))
        plant(updates(), "kestrel-9.9.9.apk", 400, 1)
        plant(updates(), "kestrel-9.9.9.apk.part", 100, 1)
        assertEquals(500L, UpdateManager.sweepFiles(ctx))
        assertEquals(0, updates().listFiles()!!.size)
        assertEquals(0L, sp.getLong("job", 0L))
        assertFalse(sp.contains("verified"))
    }

    @Test fun anOfferTheInstalledVersionHasCaughtUpWithIsUsedUp() {
        UpdateManager.setStateForTest(UpdateManager.State(UpdateManager.Phase.AVAILABLE, offer(1L, "kestrel-0.0.1.apk")))
        plant(updates(), "kestrel-0.0.1.apk", 400, 1)
        assertEquals(400L, UpdateManager.sweepFiles(ctx))
    }

    @Test fun aDownloadOrAnInstallInFlightOwnsTheFolder() {
        for (ph in listOf(UpdateManager.Phase.DOWNLOADING, UpdateManager.Phase.INSTALLING)) {
            UpdateManager.setStateForTest(UpdateManager.State(ph, offer(Long.MAX_VALUE / 2, "kestrel-9.9.9.apk")))
            plant(updates(), "anything.part", 100, 40)
            assertEquals("$ph", 0L, UpdateManager.sweepFiles(ctx))
            assertEquals(1, updates().listFiles()!!.size)
        }
    }

    // ---------------------------------------------------------------- the whole run

    @Test fun pictureAndSavedStateOfAClosedTabGoAndTheOpenTabsStay() {
        plant(File(ctx.cacheDir, "thumbs"), "1.webp", 10, 0); plant(File(ctx.cacheDir, "thumbs"), "2.webp", 20, 0)
        TabStateStore.write(ctx, 1, "{\"history\":1}", "160.0"); TabStateStore.write(ctx, 2, "{\"history\":2}", "160.0")
        val look = File(ctx.filesDir, "lastlook")
        plant(look, "1.json", 5, 0); plant(look, "1.webp", 50, 0); plant(look, "2.json", 5, 0); plant(look, "2.webp", 60, 0)
        val text = StorageTidy.runIfDue(ctx, setOf(1L), force = true)
        assertNotNull(text)
        assertTrue(File(ctx.cacheDir, "thumbs/1.webp").isFile && File(look, "1.json").isFile && File(look, "1.webp").isFile)
        assertFalse(File(ctx.cacheDir, "thumbs/2.webp").exists())
        assertFalse(File(look, "2.json").exists() || File(look, "2.webp").exists())
        assertEquals("{\"history\":1}", TabStateStore.read(ctx, 1))
        assertEquals(null, TabStateStore.read(ctx, 2))
        assertTrue("the marker that names the engine version stays", TabStateStore.dir(ctx).listFiles()!!.any { !it.name.endsWith(".json") })
        assertTrue(text!!, text.contains("previews of closed tabs"))
    }

    @Test fun withoutTheTabListNoPictureIsTouched() {
        plant(File(ctx.cacheDir, "thumbs"), "9.webp", 10, 0)
        StorageTidy.runIfDue(ctx, null, force = true)
        assertTrue("the tabs were not all restored: a missing id may be a tab that is about to appear", File(ctx.cacheDir, "thumbs/9.webp").isFile)
    }

    @Test fun theSummaryNamesWhatWasFreedAndIsKeptForDiagnostics() {
        plant(File(ctx.cacheDir, "p/cache2/entries"), "OLD", 3_000_000, 40)
        val text = StorageTidy.runIfDue(ctx, emptySet(), force = true)!!
        assertTrue(text, text.contains("freed") && text.contains("engine files"))
        val line = StorageTidy.line(ctx)
        assertTrue(line, line.startsWith("Storage tidy: last ") && line.contains("engine files"))
        val again = StorageTidy.runIfDue(ctx, emptySet(), force = true)!!
        assertEquals("a second look finds nothing left", "nothing to remove", again)
    }

    @Test fun dueOnceADayAndStraightAfterAnUpdate() {
        assertTrue("never run", StorageTidy.due(ctx))
        StorageTidy.runIfDue(ctx, emptySet())
        assertFalse("just run", StorageTidy.due(ctx))
        assertEquals("not due: nothing happens", null, StorageTidy.runIfDue(ctx, emptySet()))
        val sp = ctx.getSharedPreferences("kestrel_tidy", 0)
        sp.edit().putLong("code", 1L).commit()   // the last tidy was made by another version: an update was installed
        assertTrue(StorageTidy.due(ctx))
        val after = StorageTidy.runIfDue(ctx, emptySet())!!
        assertTrue(after, after.startsWith("after an update, "))
        sp.edit().putLong("last", System.currentTimeMillis() - 25 * 3600 * 1000L).commit()
        assertTrue("a day has passed", StorageTidy.due(ctx))
    }

    @Test fun deleteOlderThanOnlyTouchesFilesAndRecursesOnlyWhenAsked() {
        val root = File(ctx.cacheDir, "d")
        plant(root, "old", 10, 30); plant(File(root, "sub"), "old2", 20, 30)
        assertEquals(10L, StorageTidy.deleteOlderThan(root, System.currentTimeMillis() - day))
        assertTrue(File(root, "sub/old2").isFile)
        assertEquals(20L, StorageTidy.deleteOlderThan(root, System.currentTimeMillis() - day, recurse = true))
        assertTrue("folders themselves stay (the engine owns them)", File(root, "sub").isDirectory)
    }
}
