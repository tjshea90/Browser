package app.kestrel.browser

import app.kestrel.browser.data.BackupManager
import app.kestrel.browser.data.BrowserDb
import app.kestrel.browser.data.Prefs
import app.kestrel.browser.shield.Shields
import app.kestrel.core.backup.BackupCodec
import app.kestrel.core.backup.BackupData
import app.kestrel.core.backup.Mark
import app.kestrel.core.backup.TabRef
import app.kestrel.core.backup.Visit
import app.kestrel.core.shield.ShieldFlag
import app.kestrel.core.shield.SiteShields
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Settings > Back up and restore on the JVM: what goes into the file and what an install takes back from it. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class BackupManagerTest {
    private val ctx get() = RuntimeEnvironment.getApplication()
    private lateinit var db: BrowserDb
    private lateinit var prefs: Prefs

    private fun sp() = Prefs::class.java.getDeclaredField("sp").also { it.isAccessible = true }.get(prefs) as android.content.SharedPreferences

    @Before fun fresh() {
        prefs = Prefs.get(ctx)
        sp().edit().clear().commit()
        db = BrowserDb(ctx)
        db.clearHistory()
        for (b in db.bookmarks()) db.removeBookmark(b.url)
        Shields.init(ctx)
        Shields.clearAll()
    }

    private fun choose() {
        prefs.shieldAds = false; prefs.textZoom = 130; prefs.searchId = Prefs.ENGINES.last().id; prefs.neon = 1; prefs.dlLimitKBps = 500; prefs.clearOnExit = true
        db.addBookmark("https://bm.example/", "Bookmark")
        db.recordVisit("https://h.example/a", "History A"); db.recordVisit("https://h.example/a", "History A")
        Shields.setOverride("site.example", SiteShields(ShieldFlag.ADS, 0))
    }

    @Test fun theChoicesBookmarksHistorySiteShieldsAndTabsAllMakeItIntoTheFile() {
        choose()
        val d = BackupManager.build(db, prefs, listOf(TabRef("https://t.example/", "T")), "Kestrel test", now = 1_700_000_000_000L)
        assertEquals(false, d.settings["sh_ads"]); assertEquals(130L, d.settings["text_zoom"]); assertEquals(1L, d.settings["neon"]); assertEquals(true, d.settings["clear_exit"])
        assertEquals(listOf("https://bm.example/"), d.bookmarks.map { it.url })
        assertEquals(listOf("https://h.example/a"), d.history.map { it.url }); assertEquals(2, d.history[0].visits)
        assertTrue(d.siteShields.startsWith("site.example\t"))
        assertEquals(1, d.tabs.size)
        assertEquals("Kestrel test", d.app)
    }

    @Test fun onlyChoicesThatWereReallyMadeAreCarried_anUntouchedDefaultStaysADefault() {
        prefs.textZoom = 120
        val d = BackupManager.build(db, prefs, emptyList(), "x")
        assertEquals(setOf("text_zoom"), d.settings.keys)
    }

    @Test fun aRestoreOnAFreshInstallBringsEverythingBackAndTheCountsSayWhat() {
        choose()
        val text = BackupCodec.encode(BackupManager.build(db, prefs, listOf(TabRef("https://t.example/", "T")), "x"))
        // an "uninstall first": a new install has nothing
        sp().edit().clear().commit(); db.clearHistory(); for (b in db.bookmarks()) db.removeBookmark(b.url); Shields.clearAll()
        assertEquals(100, prefs.textZoom); assertTrue(prefs.shieldAds); assertTrue(db.bookmarks().isEmpty()); assertNull(Shields.overrideFor("site.example"))
        val c = BackupManager.restore(db, prefs, BackupCodec.decode(text))
        assertEquals(1, c.bookmarks); assertEquals(1, c.history); assertEquals(1, c.sites); assertEquals(1, c.tabs); assertTrue(c.settings >= 6)
        assertFalse(prefs.shieldAds); assertEquals(130, prefs.textZoom); assertEquals(Prefs.ENGINES.last().id, prefs.searchId); assertEquals(1, prefs.neon); assertEquals(500, prefs.dlLimitKBps); assertTrue(prefs.clearOnExit)
        assertEquals(listOf("https://bm.example/"), db.bookmarks().map { it.url })
        assertEquals(2, db.history(10).single { it.url == "https://h.example/a" }.visits)
        assertEquals(SiteShields(ShieldFlag.ADS, 0), Shields.overrideFor("site.example"))
    }

    @Test fun restoringTwiceDoesNotDuplicateAndKeepsTheLargerVisitCount() {
        choose()
        val d = BackupCodec.decode(BackupCodec.encode(BackupManager.build(db, prefs, emptyList(), "x")))
        val again = BackupManager.restore(db, prefs, d)
        assertEquals("the bookmark and the history entry already exist", 0, again.bookmarks)
        assertEquals(0, again.history)
        assertEquals(1, db.bookmarks().size); assertEquals(1, db.history(10).size)
        db.recordVisit("https://h.example/a", "History A") // 3 visits now
        BackupManager.restore(db, prefs, d)
        assertEquals("a restore never lowers a count", 3, db.history(10).single().visits)
    }

    @Test fun anExistingBookmarkKeepsItsCurrentTitleAndHistoryTimeNeverGoesBack() {
        db.addBookmark("https://bm.example/", "Mine now")
        val old = BackupData(1L, "x", emptyMap(), "", listOf(Mark("https://bm.example/", "From the file", 5L)), listOf(Visit("https://h.example/", "H", 9, 100L)), emptyList())
        BackupManager.restore(db, prefs, old)
        assertEquals("Mine now", db.bookmarks().single().title)
        db.recordVisit("https://h.example/", "H")
        val last = db.history(10).single().last
        BackupManager.restore(db, prefs, old)
        assertTrue("the later time stays", db.history(10).single().last >= last)
        assertEquals("the larger count wins: 9 from the file, 10 after one more visit", 10, db.history(10).single().visits)
    }

    @Test fun aFileCanNeverSetAnythingOutsideTheListOrOutOfRange() {
        val evil = mapOf<String, Any>(
            "phone_code" to "123456", "phone_mode" to 2L, "eng_ext" to false, "blocked_total" to 5L, "upd_sig" to "x", // keys that are not carried
            "text_zoom" to 99999L, "neon" to -3L, "web_dark" to 7L, "search" to "no-such-engine", "https" to "WHATEVER", "data_saver" to "?", // out of range / not allowed values
            "sh_ads" to "yes", "dl_active" to true, // wrong types
            "net_doh" to false, // fine
        )
        val n = prefs.restoreBackup(evil)
        assertEquals(1, n)
        assertFalse(prefs.dohOn)
        assertEquals(100, prefs.textZoom); assertEquals(2, prefs.neon); assertEquals(0, prefs.webDark)
        assertEquals("", prefs.phoneCode); assertEquals(0, prefs.phoneMode); assertEquals(0L, prefs.blockedTotal)
        assertEquals(Prefs.ENGINES[0].id, prefs.searchId); assertTrue(prefs.shieldAds)
    }

    @Test fun theDescriptionIsPlainAndGetsTheGrammarRight() {
        assertEquals("1 bookmark, 1 history entry, 1 setting and 1 open tab", BackupManager.describe(1, 1, 1, 0, 1))
        assertEquals("0 bookmarks, 3,400 history entries, 18 settings, 5 site choices and 6 open tabs", BackupManager.describe(0, 3400, 18, 5, 6))
    }

    @Test fun theFileNameIsDatedAndFriendly() {
        assertEquals("Kestrel backup 2023-11-14.json", BackupManager.fileName(1_699_999_999_000L))
    }

    @Test fun historyInTheFileIsCappedToTheMostRecent() {
        for (i in 0 until BackupManager.HISTORY_LIMIT + 20) db.importHistory(listOf(Visit("https://h.example/$i", "t", 1, 1_000L + i)))
        val d = BackupManager.build(db, prefs, emptyList(), "x")
        assertEquals(BackupManager.HISTORY_LIMIT, d.history.size)
        assertEquals("the newest first", "https://h.example/${BackupManager.HISTORY_LIMIT + 19}", d.history[0].url)
    }

    private fun assertNull(o: Any?) = org.junit.Assert.assertNull(o)
}
