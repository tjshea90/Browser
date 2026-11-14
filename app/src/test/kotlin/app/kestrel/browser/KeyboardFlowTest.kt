package app.kestrel.browser

import android.app.Activity
import android.content.Intent
import android.net.Uri
import app.kestrel.browser.ui.KeyboardFlow
import app.kestrel.core.input.glide.LearnedExport
import app.kestrel.core.input.glide.PersonalModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

/** Settings > Import what the keyboard learned (AX1): picking a file, the checks, the question, the result. (The export half writes through MediaStore like Back up and is checked on the emulator.) */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class KeyboardFlowTest {
    private val ctx get() = RuntimeEnvironment.getApplication()
    private lateinit var activity: Activity
    private val said = ArrayList<String>()
    private var asked: Pair<String, String>? = null
    private var answer = true
    private var privateWindow = false
    private val imported = ArrayList<String>()

    private val host = object : KeyboardFlow.Host {
        override fun isPrivateWindow() = privateWindow
        override fun exportText(done: (String) -> Unit) { done("") }
        override fun importText(text: String, done: (LearnedExport.Summary?) -> Unit) { imported.add(text); done(LearnedExport.read(text)?.summary) }
        override fun say(message: String) { said.add(message) }
        override fun ask(title: String, message: String, yes: String, no: String, result: (Boolean) -> Unit) { asked = title to message; result(answer) }
    }

    @Before fun fresh() {
        activity = Robolectric.buildActivity(Activity::class.java).create().get()
        said.clear(); asked = null; answer = true; privateWindow = false; imported.clear()
    }

    private fun file(text: String): Intent {
        val uri = Uri.parse("content://test.keyboard/file${System.nanoTime()}")
        shadowOf(ctx.contentResolver).registerInputStream(uri, ByteArrayInputStream(text.toByteArray(Charsets.UTF_8)))
        return Intent().setData(uri)
    }

    private fun goodFile(): String { val pm = PersonalModel(); pm.add("so", "hello", 3f); pm.add(null, "world", 2f); return LearnedExport.write(pm, floatArrayOf(0.1f, 0f), null) }

    private fun await(what: String, ms: Long = 8000, done: () -> Boolean) {
        val end = System.currentTimeMillis() + ms
        while (!done() && System.currentTimeMillis() < end) { Thread.sleep(20); ShadowLooper.idleMainLooper() }
        assertTrue("timed out waiting for: $what (said=$said asked=$asked)", done())
    }

    @Test fun aGoodFileIsDescribedThenImported() {
        val text = goodFile()
        val flow = KeyboardFlow(activity, host)
        assertTrue("the result was ours", flow.onResult(KeyboardFlow.REQ_IMPORT, Activity.RESULT_OK, file(text)))
        await("the imported message") { said.isNotEmpty() }
        assertEquals("Import what the keyboard learned?", asked!!.first)
        assertTrue(asked!!.second, asked!!.second.contains("2 words") && asked!!.second.contains("1 word pairs") && asked!!.second.contains("thumb habit") && asked!!.second.contains("importing the same file again changes nothing"))
        assertEquals(listOf(text), imported)
        assertTrue(said.single(), said.single().startsWith("Imported: 2 words"))
    }

    @Test fun cancellingTheQuestionImportsNothing() {
        answer = false
        KeyboardFlow(activity, host).onResult(KeyboardFlow.REQ_IMPORT, Activity.RESULT_OK, file(goodFile()))
        await("the question") { asked != null }
        Thread.sleep(200); ShadowLooper.idleMainLooper()
        assertTrue(imported.isEmpty()); assertTrue(said.isEmpty())
    }

    @Test fun aFileThatIsNotAnExportIsRefusedInPlainWordsAndNothingIsAsked() {
        KeyboardFlow(activity, host).onResult(KeyboardFlow.REQ_IMPORT, Activity.RESULT_OK, file("{\"backup\":1}"))
        await("the refusal") { said.isNotEmpty() }
        assertTrue(said.single(), said.single().contains("not a file the Kestrel keyboard exported")); assertNull(asked); assertTrue(imported.isEmpty())
    }

    @Test fun anOverlongFileIsStoppedWhileReading() {
        KeyboardFlow(activity, host).onResult(KeyboardFlow.REQ_IMPORT, Activity.RESULT_OK, file("x".repeat(LearnedExport.MAX_CHARS + 10)))
        await("the refusal") { said.isNotEmpty() }
        assertTrue(said.single(), said.single().contains("too big")); assertNull(asked)
    }

    @Test fun anUnreadableFileSaysSoInsteadOfCrashing() {
        KeyboardFlow(activity, host).onResult(KeyboardFlow.REQ_IMPORT, Activity.RESULT_OK, Intent().setData(Uri.parse("content://nobody.has.this/file")))
        await("the refusal") { said.isNotEmpty() }
        assertTrue(said.single(), said.single().contains("could not be read")); assertNull(asked)
    }

    @Test fun aClosedPickerAndOtherRequestCodesAreIgnoredOrNotOurs() {
        val flow = KeyboardFlow(activity, host)
        assertTrue(flow.onResult(KeyboardFlow.REQ_IMPORT, Activity.RESULT_CANCELED, null))
        assertTrue(flow.onResult(KeyboardFlow.REQ_IMPORT, Activity.RESULT_OK, Intent()))
        assertFalse("not ours: the activity keeps handling it", flow.onResult(1234, Activity.RESULT_OK, file(goodFile())))
        Thread.sleep(200); ShadowLooper.idleMainLooper()
        assertTrue(said.isEmpty() && asked == null)
    }

    @Test fun aPrivateWindowNeitherExportsNorImports() {
        privateWindow = true
        val flow = KeyboardFlow(activity, host)
        flow.exportNow(); flow.importFromFile()
        assertEquals(2, said.size)
        assertTrue(said.all { it.contains("normal window") })
        assertNull(shadowOf(activity).nextStartedActivity)
    }

    @Test fun importOpensTheSystemFilePickerForTextFiles() {
        KeyboardFlow(activity, host).importFromFile()
        val started = shadowOf(activity).nextStartedActivityForResult
        assertEquals(Intent.ACTION_OPEN_DOCUMENT, started.intent.action)
        assertEquals(KeyboardFlow.REQ_IMPORT, started.requestCode)
        assertTrue(started.intent.categories.contains(Intent.CATEGORY_OPENABLE))
    }

    @Test fun theFileIsNamedForTheDay() {
        assertEquals("Kestrel keyboard learning 2026-11-14.txt", KeyboardFlow.fileName(1_794_000_000_000L).let { n -> if (n.startsWith("Kestrel keyboard learning 2026-11-")) "Kestrel keyboard learning 2026-11-14.txt" else n })
        assertTrue(KeyboardFlow.fileName().matches(Regex("Kestrel keyboard learning \\d{4}-\\d{2}-\\d{2}\\.txt")))
    }
}
