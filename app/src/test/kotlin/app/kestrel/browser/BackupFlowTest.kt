package app.kestrel.browser

import android.app.Activity
import android.content.Intent
import android.net.Uri
import app.kestrel.browser.data.BrowserDb
import app.kestrel.browser.data.Prefs
import app.kestrel.browser.shield.Shields
import app.kestrel.browser.ui.BackupFlow
import app.kestrel.core.backup.BackupCodec
import app.kestrel.core.backup.BackupData
import app.kestrel.core.backup.Mark
import app.kestrel.core.backup.TabRef
import app.kestrel.core.backup.Visit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import java.io.ByteArrayInputStream

/** Settings > Back up & restore, the Restore half: picking a file, the checks, the question, the result. (The Back up half writes through MediaStore and is checked on the emulator.) */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class BackupFlowTest {
    private val ctx get() = RuntimeEnvironment.getApplication()
    private lateinit var db: BrowserDb
    private lateinit var activity: Activity
    private val said = ArrayList<String>()
    private var asked: Pair<String, String>? = null
    private var answer = true
    private var openedTabs: List<TabRef> = emptyList()
    private var afterRestore = 0
    private var privateWindow = false

    private val host = object : BackupFlow.Host {
        override fun database() = db
        override fun isPrivateWindow() = privateWindow
        override fun tabsForBackup() = listOf(TabRef("https://t.example/", "T"))
        override fun openTabs(refs: List<TabRef>): Int { openedTabs = refs; return refs.size }
        override fun say(message: String) { said.add(message) }
        override fun ask(title: String, message: String, yes: String, no: String, result: (Boolean) -> Unit) { asked = title to message; result(answer) }
        override fun afterRestore() { afterRestore++ }
        override fun appName() = "Kestrel test"
    }

    @Before fun fresh() {
        Prefs::class.java.getDeclaredField("sp").also { it.isAccessible = true }.get(Prefs.get(ctx)).let { (it as android.content.SharedPreferences).edit().clear().commit() }
        db = BrowserDb(ctx); db.clearHistory(); for (b in db.bookmarks()) db.removeBookmark(b.url)
        Shields.init(ctx); Shields.clearAll()
        activity = Robolectric.buildActivity(Activity::class.java).create().get()
        said.clear(); asked = null; answer = true; openedTabs = emptyList(); afterRestore = 0; privateWindow = false
    }

    /** Prefs and the site store are process-wide singletons: leave them as the other test classes expect to find them (Google as the search engine, every Shields switch on, nothing saved). */
    @After fun leaveNothingBehind() {
        (Prefs::class.java.getDeclaredField("sp").also { it.isAccessible = true }.get(Prefs.get(ctx)) as android.content.SharedPreferences).edit().clear().commit()
        db.clearHistory(); for (b in db.bookmarks()) db.removeBookmark(b.url)
        Shields.clearAll()
    }

    private fun file(text: String): Intent {
        val uri = Uri.parse("content://test.backup/file${System.nanoTime()}")
        shadowOf(ctx.contentResolver).registerInputStream(uri, ByteArrayInputStream(text.toByteArray(Charsets.UTF_8)))
        return Intent().setData(uri)
    }

    private fun goodFile() = BackupCodec.encode(BackupData(1L, "Kestrel old", mapOf("text_zoom" to 140L, "sh_ads" to false), "",
        listOf(Mark("https://b1.example/", "B1", 5L), Mark("https://b2.example/", "B2", 6L)), listOf(Visit("https://h.example/", "H", 4, 9L)), listOf(TabRef("https://t1.example/", "T1"), TabRef("https://t2.example/", "T2"))))

    /** The picker answer is read on the IO thread and the question and result come back on the main thread: wait for them. */
    private fun await(what: String, ms: Long = 8000, done: () -> Boolean) {
        val end = System.currentTimeMillis() + ms
        while (!done() && System.currentTimeMillis() < end) { Thread.sleep(20); ShadowLooper.idleMainLooper() }
        assertTrue("timed out waiting for: $what (said=$said asked=$asked)", done())
    }

    @Test fun aGoodFileIsDescribedThenBroughtBackAndTheTabsAreOpened() {
        val flow = BackupFlow(activity, host)
        assertTrue("the result was ours", flow.onResult(BackupFlow.REQ_RESTORE, Activity.RESULT_OK, file(goodFile())))
        await("the restored message") { said.isNotEmpty() }
        assertEquals("Restore this backup?", asked!!.first)
        assertTrue(asked!!.second, asked!!.second.contains("2 bookmarks") && asked!!.second.contains("1 history entry") && asked!!.second.contains("2 open tabs"))
        assertEquals(listOf("https://b1.example/", "https://b2.example/").sorted(), db.bookmarks().map { it.url }.sorted())
        assertEquals(4, db.history(10).single().visits)
        assertEquals(140, Prefs.get(ctx).textZoom); assertFalse(Prefs.get(ctx).shieldAds)
        assertEquals(listOf("https://t1.example/", "https://t2.example/"), openedTabs.map { it.url })
        assertEquals(1, afterRestore)
        assertTrue(said.single(), said.single().startsWith("Restored: 2 bookmarks and 1 history entries added") && said.single().contains("2 tabs opened"))
    }

    @Test fun cancellingTheQuestionChangesNothing() {
        answer = false
        BackupFlow(activity, host).onResult(BackupFlow.REQ_RESTORE, Activity.RESULT_OK, file(goodFile()))
        await("the question") { asked != null }
        Thread.sleep(200); ShadowLooper.idleMainLooper()
        assertTrue(db.bookmarks().isEmpty()); assertEquals(100, Prefs.get(ctx).textZoom); assertTrue(openedTabs.isEmpty()); assertEquals(0, afterRestore); assertTrue(said.isEmpty())
    }

    @Test fun aFileThatIsNotABackupIsRefusedInPlainWordsAndNothingIsAsked() {
        BackupFlow(activity, host).onResult(BackupFlow.REQ_RESTORE, Activity.RESULT_OK, file("{\"hello\":1}"))
        await("the refusal") { said.isNotEmpty() }
        assertTrue(said.single(), said.single().contains("not a Kestrel backup")); assertEquals(null, asked); assertTrue(db.bookmarks().isEmpty())
    }

    @Test fun anOverlongFileIsStoppedWhileReading() {
        BackupFlow(activity, host).onResult(BackupFlow.REQ_RESTORE, Activity.RESULT_OK, file("x".repeat(BackupCodec.MAX_BYTES + 10)))
        await("the refusal") { said.isNotEmpty() }
        assertTrue(said.single(), said.single().contains("too big")); assertEquals(null, asked)
    }

    @Test fun anUnreadableFileSaysSoInsteadOfCrashing() {
        val intent = Intent().setData(Uri.parse("content://nobody.has.this/file"))
        BackupFlow(activity, host).onResult(BackupFlow.REQ_RESTORE, Activity.RESULT_OK, intent)
        await("the refusal") { said.isNotEmpty() }
        assertTrue(said.single(), said.single().contains("could not be read")); assertEquals(null, asked)
    }

    @Test fun aClosedPickerAndOtherRequestCodesAreIgnoredOrNotOurs() {
        val flow = BackupFlow(activity, host)
        assertTrue(flow.onResult(BackupFlow.REQ_RESTORE, Activity.RESULT_CANCELED, null))
        assertTrue(flow.onResult(BackupFlow.REQ_RESTORE, Activity.RESULT_OK, Intent()))
        assertFalse("not ours: the activity keeps handling it", flow.onResult(1234, Activity.RESULT_OK, file(goodFile())))
        Thread.sleep(200); ShadowLooper.idleMainLooper()
        assertTrue(said.isEmpty() && asked == null)
    }

    @Test fun aPrivateWindowNeitherBacksUpNorRestores() {
        privateWindow = true
        val flow = BackupFlow(activity, host)
        flow.backupNow(); flow.restoreFromFile()
        assertEquals(2, said.size)
        assertTrue(said.all { it.contains("normal window") })
        assertEquals(null, shadowOf(activity).nextStartedActivity)
    }

    @Test fun restoreOpensTheSystemFilePickerForJsonFiles() {
        BackupFlow(activity, host).restoreFromFile()
        val i = shadowOf(activity).nextStartedActivity
        assertEquals(Intent.ACTION_OPEN_DOCUMENT, i.action)
        assertTrue(i.categories.contains(Intent.CATEGORY_OPENABLE))
        assertTrue(i.getStringArrayExtra(Intent.EXTRA_MIME_TYPES)!!.contains("application/json"))
    }
}
