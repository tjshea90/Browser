package app.kestrel.core.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class BackupCodecTest {
    private fun data(
        settings: Map<String, Any> = mapOf("sh_ads" to true, "text_zoom" to 120L, "search" to "ddg"),
        shields: String = "example.com\t1",
        marks: List<Mark> = listOf(Mark("https://a.example/", "A \u00e9\u4e2d", 5L), Mark("http://b.example/x?y=1#z", "B", 9L)),
        visits: List<Visit> = listOf(Visit("https://c.example/", "C", 3, 77L)),
        tabs: List<TabRef> = listOf(TabRef("https://t.example/1", "T1"), TabRef("https://t.example/2", "")),
    ) = BackupData(1_700_000_000_000L, "Kestrel G 0.3.0", settings, shields, marks, visits, tabs)

    private fun bad(text: String): String { try { BackupCodec.decode(text); fail("accepted: $text") } catch (e: BackupException) { return e.message ?: "" }; return "" }

    @Test fun everythingSurvivesTheRoundTripIncludingUnicode() {
        val d = BackupCodec.decode(BackupCodec.encode(data()))
        assertEquals(1_700_000_000_000L, d.createdAt); assertEquals("Kestrel G 0.3.0", d.app)
        assertEquals(mapOf<String, Any>("sh_ads" to true, "text_zoom" to 120L, "search" to "ddg"), d.settings)
        assertEquals("example.com\t1", d.siteShields)
        assertEquals(listOf("https://a.example/", "http://b.example/x?y=1#z"), d.bookmarks.map { it.url })
        assertEquals("A \u00e9\u4e2d", d.bookmarks[0].title); assertEquals(5L, d.bookmarks[0].added)
        assertEquals(3, d.history[0].visits); assertEquals(77L, d.history[0].last)
        assertEquals(listOf("T1", ""), d.tabs.map { it.title })
        assertEquals(0, d.skipped)
    }

    @Test fun anEmptyBackupIsFine() {
        val d = BackupCodec.decode(BackupCodec.encode(data(emptyMap(), "", emptyList(), emptyList(), emptyList())))
        assertTrue(d.settings.isEmpty() && d.bookmarks.isEmpty() && d.history.isEmpty() && d.tabs.isEmpty() && d.siteShields.isEmpty())
    }

    @Test fun somethingThatIsNotABackupIsRefusedInPlainWords() {
        assertTrue(bad("hello").contains("not a Kestrel backup"))
        assertTrue(bad("[1,2]").contains("not a Kestrel backup"))
        assertTrue(bad("{\"format\":\"something-else\",\"version\":1}").contains("not a Kestrel backup"))
        assertTrue(bad("{\"format\":\"kestrel-backup\"}").contains("version"))
        assertTrue(bad("{\"format\":\"kestrel-backup\",\"version\":0}").contains("damaged"))
        assertTrue(bad("{\"format\":\"kestrel-backup\",\"version\":99}").contains("newer Kestrel"))
        assertTrue(bad("{\"format\":\"kestrel-backup\",\"version\":1,\"version\":1}").contains("could not be read"))
        assertTrue(bad("").contains("could not be read"))
        assertTrue(bad("x".repeat(BackupCodec.MAX_BYTES + 1)).contains("too big"))
    }

    @Test fun onlyHttpAndHttpsAddressesComeBack() {
        val raw = BackupCodec.encode(data(marks = emptyList(), visits = emptyList(), tabs = emptyList()))
        fun withMarks(vararg urls: String): BackupData {
            val items = urls.joinToString(",") { "{\"u\":" + app.kestrel.core.courier.Json.write(it) + ",\"t\":\"x\"}" }
            return BackupCodec.decode(raw.replace("\"bookmarks\":[]", "\"bookmarks\":[$items]"))
        }
        val d = withMarks("https://ok.example/", "javascript:alert(1)", "file:///sdcard/x", "content://x/y", "intent://x#Intent;end", "data:text/html,<b>", "ftp://x.example/", "about:blank",
            "http://", "https:///x", "https://a b.example/", "https://a.example/\u0007", "HTTPS://UPPER.example/", "http://ok2.example/p?q=1")
        assertEquals(listOf("https://ok.example/", "HTTPS://UPPER.example/", "http://ok2.example/p?q=1"), d.bookmarks.map { it.url })
        assertEquals(11, d.skipped)
    }

    @Test fun duplicatesTitlesAndTheNumbersAreCleanedUp() {
        val raw = "{\"format\":\"kestrel-backup\",\"version\":1,\"bookmarks\":[{\"u\":\"https://a.example/\",\"t\":\"first\"},{\"u\":\"https://a.example/\",\"t\":\"again\"},{\"u\":\"https://b.example/\",\"t\":\"" + "T".repeat(500) + "\\u0007bell\",\"a\":-5}]," +
            "\"history\":[{\"u\":\"https://h.example/\",\"n\":99999999999,\"l\":-1},{\"u\":\"https://i.example/\",\"n\":0}],\"tabs\":[{\"u\":\"https://t.example/\"},{\"u\":\"https://t.example/\"}]}"
        val d = BackupCodec.decode(raw)
        assertEquals(listOf("first", "T".repeat(BackupCodec.MAX_TITLE)), d.bookmarks.map { it.title })
        assertEquals(0L, d.bookmarks[1].added)
        assertEquals(1_000_000, d.history[0].visits); assertEquals(0L, d.history[0].last)
        assertEquals(1, d.history[1].visits)
        assertEquals(1, d.tabs.size)
        assertEquals(2, d.skipped)
    }

    @Test fun wrongTypesAreSkippedNeverTrusted() {
        val raw = "{\"format\":\"kestrel-backup\",\"version\":1,\"settings\":{\"a\":true,\"b\":5,\"c\":\"s\",\"d\":[1],\"e\":{\"x\":1},\"f\":null,\"g\":1.5,\"\":1},\"bookmarks\":[1,\"x\",null,{\"u\":5},{\"t\":\"no url\"}]," +
            "\"history\":\"not a list\",\"tabs\":{\"u\":\"https://x.example/\"}}"
        val d = BackupCodec.decode(raw)
        assertEquals(mapOf<String, Any>("a" to true, "b" to 5L, "c" to "s", "g" to 1L), d.settings)
        assertTrue(d.bookmarks.isEmpty() && d.history.isEmpty() && d.tabs.isEmpty())
        assertEquals(9, d.skipped)
    }

    @Test fun countsAndLengthsAreCapped() {
        val sb = StringBuilder("{\"format\":\"kestrel-backup\",\"version\":1,\"tabs\":[")
        for (i in 0 until BackupCodec.MAX_TABS + 5) { if (i > 0) sb.append(','); sb.append("{\"u\":\"https://t.example/").append(i).append("\"}") }
        sb.append("],\"settings\":{")
        for (i in 0 until BackupCodec.MAX_SETTINGS + 3) { if (i > 0) sb.append(','); sb.append("\"k").append(i).append("\":1") }
        sb.append("},\"bookmarks\":[{\"u\":\"https://x.example/").append("a".repeat(BackupCodec.MAX_URL)).append("\"}]}")
        val d = BackupCodec.decode(sb.toString())
        assertEquals(BackupCodec.MAX_TABS, d.tabs.size)
        assertEquals(BackupCodec.MAX_SETTINGS, d.settings.size)
        assertTrue("an overlong address is dropped", d.bookmarks.isEmpty())
        assertEquals(5 + 3 + 1, d.skipped)
    }

    @Test fun anOverlongSettingTextAndAHugeShieldsTextAreDropped() {
        val long = "x".repeat(BackupCodec.MAX_SETTING_TEXT + 1)
        val d = BackupCodec.decode("{\"format\":\"kestrel-backup\",\"version\":1,\"settings\":{\"s\":\"$long\",\"ok\":\"fine\"},\"siteShields\":\"" + "y".repeat(BackupCodec.MAX_SHIELDS_TEXT + 1) + "\"}")
        assertEquals(mapOf<String, Any>("ok" to "fine"), d.settings)
        assertEquals("", d.siteShields)
        assertEquals(1, d.skipped)
    }

    @Test fun unknownFieldsAreIgnoredSoOlderAppsReadNewerFilesOfTheSameVersion() {
        val d = BackupCodec.decode("{\"format\":\"kestrel-backup\",\"version\":1,\"future\":{\"x\":[1,2,3]},\"bookmarks\":[{\"u\":\"https://a.example/\",\"t\":\"A\",\"extra\":true}]}")
        assertEquals(1, d.bookmarks.size)
    }

    @Test fun deepNestingIsRefusedNotFollowed() {
        val deep = "[".repeat(200) + "]".repeat(200)
        assertTrue(bad("{\"format\":\"kestrel-backup\",\"version\":1,\"x\":$deep}").contains("could not be read"))
    }

    @Test fun theFileNeverContainsAnythingElseThanWhatWasHandedIn() {
        val text = BackupCodec.encode(data())
        assertFalse(text.contains("password", ignoreCase = true) || text.contains("cookie", ignoreCase = true))
        assertTrue(text.startsWith("{\"format\":\"kestrel-backup\",\"version\":1"))
    }

    @Test fun addressCheckEdges() {
        assertTrue(BackupCodec.okUrl("https://a.b/"))
        assertTrue(BackupCodec.okUrl("http://localhost:8080/x"))
        assertTrue(BackupCodec.okUrl("https://[::1]/x"))
        assertFalse(BackupCodec.okUrl("https://"))
        assertFalse(BackupCodec.okUrl("http:/a.example/"))
        assertFalse(BackupCodec.okUrl("httpss://a.example/"))
        assertFalse(BackupCodec.okUrl(" https://a.example/"))
        assertFalse(BackupCodec.okUrl("https://a.example/\n"))
        assertFalse(BackupCodec.okUrl(""))
    }
}
