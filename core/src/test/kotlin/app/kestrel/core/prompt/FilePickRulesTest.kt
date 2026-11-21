package app.kestrel.core.prompt

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class FilePickRulesTest {
    private fun types(vararg a: String) = FilePickRules.mimeTypes(arrayOf(*a))

    @Test fun anAcceptListWithDocumentExtensionsKeepsTheDocumentsInThePicker() {
        // ATT1: the old rule dropped every entry without a slash, so "image/*,.pdf,.docx" showed pictures only
        assertEquals(listOf("image/*", "application/pdf", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"), types("image/*", ".pdf", ".docx"))
        assertEquals(listOf("application/pdf"), types(".pdf", ".PDF"))
        assertEquals("jpg and jpeg are one kind", listOf("image/jpeg", "image/png"), types(".jpg", ".jpeg", ".png"))
        assertEquals(listOf("video/mp4", "audio/mpeg"), types(" video/mp4 ", "AUDIO/MPEG"))
    }

    @Test fun anythingNotUnderstoodMeansNoRestrictionRatherThanAWrongOne() {
        assertNull("an unknown extension: show every file", types("image/*", ".py"))
        assertNull(types("image/*", "documents"))
        assertNull(types("*/*"))
        assertNull(types("image/*", "*"))
        assertNull("nothing listed", types())
        assertNull(types("", "  "))
        assertNull(FilePickRules.mimeTypes(null))
        assertNull("a type with a space in it is not a type", types("image/ png"))
        assertNull("too many kinds", FilePickRules.mimeTypes(Array(40) { "application/x-t$it" }))
    }

    @Test fun theStatsLineSaysWhereAFileChooserStopped() {
        val s = FilePickStats()
        s.asked("SINGLE, 3 kinds"); s.opened("GET_CONTENT"); s.result(0, 0)
        assertTrue(s.line().contains("asked 1, opened 1, no picker app 0, nothing chosen 1"))
        assertTrue(s.line().endsWith("last: nothing chosen (result code 0)"))
        s.asked("SINGLE, no filter"); s.noPicker()
        assertTrue(s.line().contains("no picker app 1") && s.line().endsWith("last: no app on this device can choose a file"))
        s.asked("MULTIPLE"); s.opened("OPEN_DOCUMENT"); s.result(2, -1)
        assertTrue(s.line().contains("files given to the page 2") && s.line().endsWith("last: gave 2 file(s) to the page"))
        s.folder(); s.failed("x".repeat(200))
        assertTrue(s.line().contains("whole-folder asks refused 1, failed 1"))
        assertTrue("the reason is cut", s.line().length < 520)
    }

    @Test fun theActivityAsksTheRulesAndTheManifestLetsItSeeThePickers() {
        fun src(p: String) = listOf(p, "../$p").map { File(it) }.first { it.exists() }.readText()
        val act = src("geckoapp/src/main/kotlin/app/kestrel/browser/BrowserActivity.kt")
        assertTrue("the picker list comes from the rules", act.contains("FilePickRules.mimeTypes(mimeTypes)"))
        assertTrue("a device with no picker app is told so and the page is answered", act.contains("filePicks.noPicker()") && act.contains("no-handler"))
        assertTrue("every step is counted", act.contains("filePicks.opened(") && act.contains("filePicks.result("))
        val mf = src("geckoapp/src/main/AndroidManifest.xml")
        assertTrue("Android 11 and later: without these the check for a picker app would always say none", mf.contains("android.intent.action.GET_CONTENT") && mf.contains("android.intent.action.OPEN_DOCUMENT"))
        val del = src("geckoapp/src/main/kotlin/app/kestrel/browser/web/TabDelegates.kt")
        assertTrue("a whole-folder ask is answered, not left waiting", del.contains("FilePrompt.Type.FOLDER") && del.contains("refuseFolder"))
    }
}
