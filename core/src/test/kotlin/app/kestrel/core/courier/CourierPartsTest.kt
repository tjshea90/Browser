package app.kestrel.core.courier

import app.kestrel.core.download.DlState
import app.kestrel.core.download.FetchException
import app.kestrel.core.download.FetchRequest
import app.kestrel.core.download.Reason
import app.kestrel.core.download.Rec
import app.kestrel.core.download.Seg
import app.kestrel.core.download.UrlConnectionFetcher
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File
import java.net.InetAddress
import java.nio.file.Files

class CourierPartsTest {
    private val root: File = Files.createTempDirectory("courier-parts").toFile()
    @After fun cleanUp() { root.deleteRecursively() }
    private fun ip(s: String): ByteArray = InetAddress.getByName(s).address

    // ------------------------------------------------------------------------------------------------------------------ staging files
    @Test fun aStagedFileIsWrittenInPlaceAndPublishedUnderItsName() {
        val sinks = StagingSinks(File(root, "s"))
        val s = sinks.create("movie.mp4", "video/mp4", 100, false)
        assertEquals("movie.mp4", s.displayName); assertTrue(s.uri!!.endsWith("movie.mp4.part")); assertEquals(100L, s.length())
        val a = ByteArray(40) { it.toByte() }; val b = ByteArray(60) { (100 + it).toByte() }
        s.pwrite(40, b, 0, 60); s.pwrite(0, a, 0, 40)                                            // out of order, as parallel segments are
        s.sync()
        val back = ByteArray(100); assertEquals(100, s.pread(0, back, 0, 100)); assertArrayEquals(a + b, back)
        assertEquals(0, sinks.fileOf(s.uri)!!.name.compareTo("movie.mp4.part"))
        val done = s.publish()!!
        assertTrue(done.endsWith("movie.mp4")); val f = sinks.fileOf(done)!!
        assertTrue(f.isFile); assertArrayEquals(a + b, f.readBytes()); assertFalse(File(f.parentFile, "movie.mp4.part").exists())
        assertTrue(sinks.delete(done)); assertFalse(f.exists()); assertFalse("the folder goes with it", f.parentFile.exists())
    }

    @Test fun twoFilesWithOneNameNeverShareAFile() {
        val sinks = StagingSinks(File(root, "s"))
        val a = sinks.create("video.mp4", null, 10, false); val b = sinks.create("video.mp4", null, 10, false)
        assertEquals("video.mp4", a.displayName); assertEquals("video.mp4", b.displayName)
        a.pwrite(0, byteArrayOf(1, 1, 1), 0, 3); b.pwrite(0, byteArrayOf(2, 2, 2), 0, 3)
        val fa = sinks.fileOf(a.publish())!!; val fb = sinks.fileOf(b.publish())!!
        assertTrue(fa.absolutePath != fb.absolutePath); assertEquals(1, fa.readBytes()[0].toInt()); assertEquals(2, fb.readBytes()[0].toInt())
    }

    @Test fun nastyNamesStayInsideTheFolder() {
        val sinks = StagingSinks(File(root, "s"))
        for (n in listOf("../../etc/passwd", "..", "a/b/c.bin", "..\\x", "", "   ", ".hidden", "a:b*c?.bin", "x\u0000y")) {
            val s = sinks.create(n, null, 1, false)
            val f = sinks.fileOf(s.uri)!!
            assertTrue("$n -> ${f.path}", f.canonicalPath.startsWith(File(root, "s").canonicalPath + File.separator))
            assertFalse(s.displayName!!.contains('/') || s.displayName!!.contains('\\') || s.displayName!!.isEmpty())
            s.discard()
        }
        assertEquals("long names are cut but keep their extension", true, sinks.create("a".repeat(300) + ".mp4", null, 1, false).displayName!!.let { it.length <= 120 && it.endsWith(".mp4") })
    }

    @Test fun onlyItsOwnUrisAreAccepted() {
        val sinks = StagingSinks(File(root, "s"))
        val outside = File(root, "outside.bin").also { it.writeText("x") }
        assertNull(sinks.fileOf(outside.toURI().toString())); assertFalse(sinks.delete(outside.toURI().toString())); assertNull(sinks.reopen(outside.toURI().toString(), 1))
        assertNull(sinks.fileOf("content://media/external/downloads/5")); assertNull(sinks.fileOf("file:///etc/passwd")); assertNull(sinks.fileOf(null)); assertNull(sinks.fileOf("file:not a uri"))
        assertTrue(outside.exists())
        val s = sinks.create("ok.bin", null, 1, false)
        assertNull("a path that climbs out of the folder", sinks.fileOf(File(File(root, "s"), "../outside.bin").toURI().toString()))
        s.discard()
    }

    @Test fun anInterruptedFileCanBeReopenedAndCutBack() {
        val sinks = StagingSinks(File(root, "s"))
        val s = sinks.create("part.bin", null, 1000, false)
        s.pwrite(0, ByteArray(300) { 7 }, 0, 300); s.sync()
        val uri = s.uri!!                                                                           // (the process dies here: nothing is closed or published)
        val again = sinks.reopen(uri, 1000)!!
        val back = ByteArray(300); assertEquals(300, again.pread(0, back, 0, 300)); assertTrue(back.all { it == 7.toByte() })
        again.truncate(200); assertEquals(200L, again.length())
        again.truncate(500); assertEquals("truncate never grows a file", 200L, again.length())
        assertNull(sinks.reopen(File(File(root, "s"), "nope/none.part").toURI().toString(), 1))
        again.discard(); assertFalse(File(java.net.URI(uri).path).exists())
    }

    @Test fun folderSweepRemovesOnlyStrays() {
        val dir = File(root, "s"); val sinks = StagingSinks(dir)
        val keep = sinks.create("keep.bin", null, 1, false); val stray = File(dir, "deadbeef").also { it.mkdirs(); File(it, "x.part").writeText("x") }
        val young = File(dir, "cafe").also { it.mkdirs() }
        stray.setLastModified(1_000); keep.sync()
        sinks.sweep(setOf(keep.uri!!), 60_000, 10_000_000)
        assertFalse("an old folder nobody refers to is removed", stray.exists()); assertTrue("a young one may still be in the middle of being created", young.exists())
        assertTrue("a folder in use is kept", sinks.fileOf(keep.uri)!!.exists())
    }

    // ------------------------------------------------------------------------------------------------------------------ the engine's records as a file
    private fun fullRec(): Rec {
        val r = Rec(42)
        r.state = DlState.PAUSED_SYSTEM; r.reason = Reason.NETWORK; r.detail = 503; r.prio = 3; r.url = "https://a.test/x?y=1&z=é"; r.finalUrl = "https://cdn.test/x"; r.finalAt = 12345L; r.pageUrl = "https://a.test/p"
        r.headers = linkedMapOf("Referer" to "https://a.test/", "Cookie" to "a=b; c=\"d\"")
        r.name = "café \"1\".mp4"; r.nameFixed = true; r.mime = "video/mp4"; r.destKind = 2; r.destUri = "file:/x/y.part"
        r.total = 9_000_000_000L; r.etag = "\"abc\""; r.lastMod = "Wed, 21 Oct 2026 07:28:00 GMT"; r.dateHdr = "Thu, 22 Oct 2026 07:28:00 GMT"; r.ranges = 1; r.fetcher = 1; r.risk = 2; r.meteredOk = true
        r.done = 4_500_000_000L; r.fails = 3; r.connsPeak = 5; r.crashes = 1; r.priv = false; r.created = 1L; r.updated = 2L; r.lastProgress = 3L; r.finished = 4L
        r.segs.add(Seg(0, 0, 4_500_000_000L, 4_500_000_000L)); r.segs.add(Seg(1, 4_500_000_000L, 9_000_000_000L, 4_500_000_000L))
        return r
    }

    private fun same(a: Rec, b: Rec) {
        assertEquals(a.id, b.id); assertEquals(a.state, b.state); assertEquals(a.reason, b.reason); assertEquals(a.detail, b.detail); assertEquals(a.prio, b.prio)
        assertEquals(a.url, b.url); assertEquals(a.finalUrl, b.finalUrl); assertEquals(a.finalAt, b.finalAt); assertEquals(a.pageUrl, b.pageUrl); assertEquals(a.headers, b.headers)
        assertEquals(a.name, b.name); assertEquals(a.nameFixed, b.nameFixed); assertEquals(a.mime, b.mime); assertEquals(a.destKind, b.destKind); assertEquals(a.destUri, b.destUri)
        assertEquals(a.total, b.total); assertEquals(a.etag, b.etag); assertEquals(a.lastMod, b.lastMod); assertEquals(a.dateHdr, b.dateHdr); assertEquals(a.ranges, b.ranges)
        assertEquals(a.fetcher, b.fetcher); assertEquals(a.risk, b.risk); assertEquals(a.meteredOk, b.meteredOk); assertEquals(a.done, b.done); assertEquals(a.fails, b.fails)
        assertEquals(a.connsPeak, b.connsPeak); assertEquals(a.crashes, b.crashes); assertEquals(a.priv, b.priv)
        assertEquals(a.created, b.created); assertEquals(a.updated, b.updated); assertEquals(a.lastProgress, b.lastProgress); assertEquals(a.finished, b.finished)
        assertEquals(a.segs.size, b.segs.size)
        for (i in a.segs.indices) { assertEquals(a.segs[i].idx, b.segs[i].idx); assertEquals(a.segs[i].start, b.segs[i].start); assertEquals(a.segs[i].end, b.segs[i].end); assertEquals(a.segs[i].cursor, b.segs[i].cursor) }
    }

    @Test fun everyFieldOfARecSurvivesTheJson() {
        val r = fullRec()
        same(r, RecJson.decode(Json.parse(Json.write(RecJson.encode(r))) as Map<*, *>)!!)
        val bare = Rec(7).also { it.url = "https://b.test/" }
        same(bare, RecJson.decode(Json.parse(Json.write(RecJson.encode(bare))) as Map<*, *>)!!)
    }

    @Test fun damagedRecsAreSkippedNotFatal() {
        assertNull(RecJson.decode(emptyMap<String, Any?>()))
        assertNull(RecJson.decode(mapOf("id" to 1L)))
        assertNull(RecJson.decode(mapOf("id" to 1L, "state" to 2L)))                         // no url
        assertNull(RecJson.decode(mapOf("id" to 1L, "state" to 2L, "url" to "u", "segs" to listOf(listOf("a", 1L, 2L, 3L)))))
        val ok = RecJson.decode(mapOf("id" to 1L, "state" to 2L, "url" to "u", "segs" to listOf(listOf(1L, 2L), "junk")))!!
        assertEquals(0, ok.segs.size)
    }

    @Test fun theStoreKeepsWhatTheEngineSavesAcrossRestarts() {
        val file = File(root, "d/downloads.json")
        val s1 = JsonRecStore(file)
        assertTrue(s1.loadAll().isEmpty())
        s1.insert(fullRec()); s1.insert(Rec(43).also { it.url = "https://c.test/"; it.state = DlState.COMPLETED })
        val moved = fullRec().also { it.done = 6_000_000_000L; it.state = DlState.RUNNING }
        s1.save(moved); s1.delete(43); s1.delete(999)
        val s2 = JsonRecStore(file)
        val loaded = s2.loadAll()
        assertEquals(1, loaded.size); same(moved, loaded[0])
    }

    @Test fun aKilledWriteLeavesAWholeFile() {
        val file = File(root, "d/downloads.json")
        val s = JsonRecStore(file)
        s.insert(fullRec())
        s.save(fullRec().also { it.done = 7L })                                                    // now there is a current file and a .bak
        assertTrue(File(root, "d/downloads.json.bak").isFile)
        file.writeText("{\"v\":1,\"downloads\":[{\"id\":4")                                         // the process died halfway through a write of the main file
        val back = JsonRecStore(file).loadAll()
        assertEquals("the previous complete version is used", 1, back.size); assertEquals(4_500_000_000L, back[0].done)
        file.delete()                                                                              // died between the two renames
        assertEquals(1, JsonRecStore(file).loadAll().size)
        File(root, "d/downloads.json.bak").writeText("garbage"); file.writeText("also garbage")
        assertTrue("two bad files: start empty, do not crash", JsonRecStore(file).loadAll().isEmpty())
    }

    // ------------------------------------------------------------------------------------------------------------------ private addresses
    @Test fun addressesAPhoneMustNotFetchFrom() {
        for (s in listOf("127.0.0.1", "127.8.8.8", "0.0.0.0", "10.0.0.1", "172.16.5.5", "172.31.0.1", "192.168.43.1", "169.254.169.254", "100.64.0.1", "100.100.100.100", "198.18.0.1", "224.0.0.1", "255.255.255.255",
            "::1", "::", "fe80::1", "fd12:3456::1", "fec0::1", "ff02::1", "64:ff9b::7f00:1", "64:ff9b::a00:5")) assertTrue(s, Ssrf.blocked(ip(s)))
        for (s in listOf("8.8.8.8", "93.184.216.34", "172.15.0.1", "172.32.0.1", "100.63.0.1", "100.128.0.1", "198.17.0.1", "223.255.255.255", "2606:4700::1111", "2001:db8::1", "64:ff9b::808:808")) assertFalse(s, Ssrf.blocked(ip(s)))
        val mapped = ByteArray(16).also { it[10] = 0xFF.toByte(); it[11] = 0xFF.toByte(); it[12] = 127; it[15] = 1 }
        assertTrue("::ffff:127.0.0.1", Ssrf.blocked(mapped))
    }

    @Test fun oneBadAnswerAmongGoodOnesRefusesTheHost() {
        fun check(vararg a: String) = Ssrf.check("h.test") { a.map { ip(it) } }
        check("8.8.8.8", "2606:4700::1111")
        for (bad in listOf(arrayOf("192.168.1.1"), arrayOf("8.8.8.8", "10.1.1.1"), arrayOf("2606:4700::1111", "::1"))) {
            try { check(*bad); fail("should refuse ${bad.toList()}") } catch (e: FetchException) { assertEquals(FetchException.Kind.LOCAL_NET, e.kind) }
        }
        Ssrf.check("unresolvable.test") { throw java.net.UnknownHostException("no") }                    // left to fail in the connection itself
        Ssrf.check("empty.test") { emptyList() }
    }

    @Test fun theFetcherAsksTheGuardBeforeEveryConnection() {
        val asked = ArrayList<String>()
        val f = UrlConnectionFetcher { u -> asked.add(u.host); if (u.host == "inner.test") throw FetchException(FetchException.Kind.LOCAL_NET, "no") }
        try { f.open(FetchRequest("http://inner.test/x", -1, -1, null, emptyMap())); fail("the guard must stop the request") } catch (e: FetchException) { assertEquals(FetchException.Kind.LOCAL_NET, e.kind) }
        assertEquals(listOf("inner.test"), asked)
        // the real guard on literals (no DNS needed): loopback, private and IPv6 literals are refused, in the form a URL spells them
        for (u in listOf("http://127.0.0.1:1/x", "http://192.168.1.1/x", "http://[::1]:1/x", "http://localhost:1/x", "https://10.1.2.3/x", "http://[fe80::1]/x")) {
            try { UrlConnectionFetcher(Ssrf.guard()).open(FetchRequest(u, -1, -1, null, emptyMap())); fail("should be refused: $u") }
            catch (e: FetchException) { assertEquals(u, FetchException.Kind.LOCAL_NET, e.kind) }
        }
        try { UrlConnectionFetcher(Ssrf.guard()).open(FetchRequest("ftp://example.org/x", -1, -1, null, emptyMap())); fail() } catch (e: FetchException) { assertEquals(FetchException.Kind.BAD_URL, e.kind) }
        assertNotNull(Ssrf.guard())
    }

    // ------------------------------------------------------------------------------------------------------------------ stream records as files
    private fun streamRec(): app.kestrel.core.media.StreamRec {
        val r = app.kestrel.core.media.StreamRec(1_000_000_007)
        r.state = DlState.PAUSED_SYSTEM; r.reason = Reason.NETWORK; r.detail = 5; r.url = "https://cdn.test/v/index.m3u8?t=\u00e9"; r.playlistText = "#EXTM3U\n#EXTINF:2,\ns0.ts\n#EXT-X-ENDLIST\n"; r.pageUrl = "https://site.test/watch"
        r.headers = linkedMapOf("Referer" to "https://site.test/"); r.fetcher = 1; r.meteredOk = true; r.nameStem = "My clip - 720p"; r.name = "My clip - 720p.mp4"; r.mime = "video/mp4"; r.destKind = 2; r.destUri = "file:/x/y.part"
        r.estimate = 123456789L; r.repId = "v1"; r.ext = "mp4"; r.companionId = 5; r.parentId = 6; r.appended = 987654321L; r.next = 42; r.breaks = "10:100;20:200;"; r.crashes = 1; r.mux = 1; r.note = "1 part was left out"
        r.initDone = true; r.total = 2_000_000_000L; r.created = 1; r.updated = 2; r.finished = 3
        return r
    }

    @Test fun aStreamRecordSurvivesTheFilesAndAKilledWrite() {
        val dir = File(root, "streams")
        val st = JsonStreamStore(dir)
        assertTrue(st.loadAll().isEmpty())
        val r = streamRec()
        st.save(r)
        val back = JsonStreamStore(dir).loadAll().single()
        assertEquals(r.id, back.id); assertEquals(r.state, back.state); assertEquals(r.reason, back.reason); assertEquals(r.url, back.url); assertEquals(r.playlistText, back.playlistText); assertEquals(r.headers, back.headers)
        assertEquals(r.nameStem, back.nameStem); assertEquals(r.name, back.name); assertEquals(r.destUri, back.destUri); assertEquals(r.estimate, back.estimate); assertEquals(r.repId, back.repId); assertEquals(r.ext, back.ext)
        assertEquals(r.companionId, back.companionId); assertEquals(r.parentId, back.parentId); assertEquals(r.appended, back.appended); assertEquals(r.next, back.next); assertEquals(r.breaks, back.breaks)
        assertEquals(r.crashes, back.crashes); assertEquals(r.mux, back.mux); assertEquals(r.note, back.note); assertEquals(r.initDone, back.initDone); assertEquals(r.total, back.total)
        assertEquals(r.pageUrl, back.pageUrl); assertEquals(r.fetcher, back.fetcher); assertEquals(r.meteredOk, back.meteredOk); assertEquals(r.mime, back.mime); assertEquals(r.destKind, back.destKind)
        r.next = 43; st.save(r)                                                               // a second save leaves a .bak
        File(dir, "${r.id}.json").writeText("{\"id\":10000000")                              // the process died halfway through the next write
        assertEquals("the previous complete record is used", 42, JsonStreamStore(dir).loadAll().single().next)
        st.dir(r.id).let { File(it, "seg0.bin").writeText("x") }
        st.delete(r.id)
        assertTrue(JsonStreamStore(dir).loadAll().isEmpty()); assertFalse(File(dir, "${r.id}.d").exists()); assertFalse(File(dir, "${r.id}.m3u8").exists())
    }
}

