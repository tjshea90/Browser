package app.kestrel.core.download

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FileNamesTest {
    @Test fun contentDisposition() {
        assertEquals("report.pdf", FileNames.fromContentDisposition("attachment; filename=\"report.pdf\""))
        assertEquals("plain.bin", FileNames.fromContentDisposition("inline; filename=plain.bin"))
        assertEquals("naïve file.txt", FileNames.fromContentDisposition("attachment; filename*=UTF-8''na%C3%AFve%20file.txt"))
        assertEquals("real.txt", FileNames.fromContentDisposition("attachment; filename=\"fallback.txt\"; filename*=UTF-8''real.txt")) // filename* wins
        assertEquals("a;b.txt", FileNames.fromContentDisposition("attachment; filename=\"a;b.txt\"; size=3"))
        assertEquals("q\"uote.txt", FileNames.fromContentDisposition("attachment; filename=\"q\\\"uote.txt\""))
        assertEquals("café.txt", FileNames.fromContentDisposition("attachment; filename*=ISO-8859-1''caf%E9.txt"))
        assertNull(FileNames.fromContentDisposition("attachment"))
        assertNull(FileNames.fromContentDisposition(null))
        assertNull(FileNames.fromContentDisposition("attachment; filename=\"\""))
    }

    @Test fun urls() {
        assertEquals("file name.zip", FileNames.fromUrl("https://example.com/a/b/file%20name.zip?x=1#frag"))
        assertEquals("a+b.txt", FileNames.fromUrl("https://example.com/a+b.txt"))
        assertNull(FileNames.fromUrl("https://example.com/"))
        assertNull(FileNames.fromUrl("https://example.com"))
    }

    @Test fun sanitizing() {
        val t = FileNames.sanitize("../../etc/passwd")
        assertFalse(t.contains("/")); assertFalse(t.startsWith("."))
        assertEquals("evilfdp.exe", FileNames.sanitize("evil‮fdp.exe"))          // bidi override removed
        assertEquals("ab.txt", FileNames.sanitize("a​b.txt"))                      // zero width removed
        assertEquals("a_b_c_d.txt", FileNames.sanitize("a:b*c?d.txt"))
        assertEquals("_CON.txt", FileNames.sanitize("CON.txt"))
        assertEquals("download", FileNames.sanitize("..."))
        assertEquals("download", FileNames.sanitize(""))
        assertEquals("name", FileNames.sanitize("name. . "))
        assertEquals("hidden", FileNames.sanitize(".hidden"))
        val cjk = "日".repeat(200) + ".pdf"
        val s = FileNames.sanitize(cjk)
        assertTrue(s.toByteArray(Charsets.UTF_8).size <= 255); assertTrue(s.endsWith(".pdf"))
    }

    @Test fun extensions() {
        assertEquals("report.pdf", FileNames.withExtensionFor("application/pdf", "report"))
        assertEquals("report.pdf", FileNames.withExtensionFor("application/pdf", "report.pdf"))
        assertEquals("pic.jpg", FileNames.withExtensionFor("image/jpeg; charset=x", "pic"))
        assertEquals("blob", FileNames.withExtensionFor("application/x-unknown", "blob"))
        assertEquals("v1.2", FileNames.withExtensionFor(null, "v1.2"))
    }

    @Test fun choosingAndDedupe() {
        assertEquals("from-header.pdf", FileNames.choose("attachment; filename=\"from-header.pdf\"", "https://x.test/other.zip", "application/pdf", "hint.bin"))
        assertEquals("other.zip", FileNames.choose(null, "https://x.test/dl/other.zip", null, "hint.bin"))
        assertEquals("hint.bin", FileNames.choose(null, "https://x.test/", null, "hint.bin"))
        assertEquals("download", FileNames.choose(null, "https://x.test/", null, null))
        assertEquals("a (2).txt", FileNames.dedupe("a.txt") { it == "a.txt" || it == "a (1).txt" })
        assertEquals("b.txt", FileNames.dedupe("b.txt") { false })
    }
}
