package app.kestrel.core.courier

import app.kestrel.core.download.Action
import app.kestrel.core.download.Clock
import app.kestrel.core.download.DEST_APP_PRIVATE
import app.kestrel.core.download.DlState
import app.kestrel.core.download.DownloadView
import app.kestrel.core.download.Engine
import app.kestrel.core.download.FakeNet
import app.kestrel.core.download.FakeServer
import app.kestrel.core.download.FakeSinks
import app.kestrel.core.download.FakeStore
import app.kestrel.core.download.SinkFactory
import app.kestrel.core.media.StreamFinisher
import app.kestrel.core.download.Source
import app.kestrel.core.download.testBytes
import app.kestrel.core.media.FakeCdn
import app.kestrel.core.media.Fixtures
import app.kestrel.core.media.MemStreamStore
import app.kestrel.core.media.Mp4Reader
import app.kestrel.core.media.StreamEngine
import app.kestrel.core.media.StreamSource
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.IOException
import java.net.ConnectException
import java.net.ServerSocket
import java.nio.file.Files
import java.util.concurrent.Executors

/**
 * The tablet side end to end: a real [PhoneBackend], the tablet's real download [Engine] (in-memory files) and a whole [Phone] (real server, host, engine and files)
 * connected by real sockets. Only the two origin websites are faked.
 */
class PhoneBackendTest {
    private val MiB = 1 shl 20
    private val root: File = Files.createTempDirectory("courier-backend").toFile()
    private val cleanups = ArrayList<() -> Unit>()
    @After fun cleanUp() { cleanups.reversed().forEach { try { it() } catch (_: Exception) {} }; root.deleteRecursively() }

    /** A transport that can be switched off, like a hotspot that drops. */
    private class Switch(val inner: CourierTransport) : CourierTransport {
        @Volatile var down = false
        override fun open(method: String, target: String, headers: Map<String, String>, body: ByteArray?, connectMs: Int, readMs: Int): HttpReply {
            if (down) throw ConnectException("switched off")
            return inner.open(method, target, headers, body, connectMs, readMs)
        }
    }

    private inner class Tablet(val origin: FakeServer, var mode: PhoneMode = PhoneMode.WHEN_THERE, val rowLagMs: Long = 0, val tabletCdn: FakeCdn? = null) {
        val sinks = FakeSinks()
        val store = FakeStore()
        val exec = Executors.newSingleThreadExecutor { r -> Thread(r, "tablet-control").also { it.isDaemon = true } }
        val engine = Engine({ i -> if (i == FETCHER_LAN) LanFetcher() else origin }, sinks, store, FakeNet(), exec, Clock.System, courierTestConfig())
        /** The tablet's own segment downloader (for a video the phone could not take). */
        val streamEngine: StreamEngine? = tabletCdn?.let { c -> StreamEngine({ c }, sinks, MemStreamStore(), FakeNet(), exec, Clock.System, courierTestConfig()) }
        @Volatile var ticking = true
        @Volatile var target: CourierClient? = null
        val stateFile = File(root, "tablet/phone-jobs.json")
        val changes = java.util.concurrent.atomic.AtomicInteger()
        private val enqueuedAt = java.util.concurrent.ConcurrentHashMap<Long, Long>()
        /** [rowLagMs] > 0: the engine's row is not visible for that long after enqueue(), like a slow tablet whose control thread is behind. */
        val local = object : LocalDownloads {
            override fun enqueue(src: Source): Long { val id = engine.enqueue(src); enqueuedAt[id] = System.currentTimeMillis(); return id }
            override fun enqueueStream(src: StreamSource): Long { val id = streamEngine!!.enqueue(src); enqueuedAt[id] = System.currentTimeMillis(); return id }
            override fun view(id: Long): DownloadView? { val t = enqueuedAt[id]; return if (t != null && System.currentTimeMillis() - t < rowLagMs) null else if (streamEngine?.owns(id) == true) streamEngine.view(id) else engine.view(id) }
        }
        var backend = newBackend()

        init {
            Thread { while (ticking) { engine.tick(); try { Thread.sleep(20) } catch (e: InterruptedException) { return@Thread } } }.also { it.isDaemon = true; it.start() }
            cleanups.add { ticking = false; backend.stop(); exec.shutdownNow() }
        }

        fun newBackend() = PhoneBackend({ target }, local, stateFile, 5_000_000_000L, { mode }, { changes.incrementAndGet() })
        fun startBackend() { backend.restore(); backend.start() }
        fun to(phone: Phone): Switch = Switch(SocketTransport("127.0.0.1", phone.port)).also { target = CourierClient(TEST_KEY, it, "http://127.0.0.1:${phone.port}") }

        /** The tablet engine's one row (the pulled file or the fallback download). */
        fun row(): DownloadView? = engine.views().firstOrNull()
        fun bytesOf(v: DownloadView): ByteArray = sinks.sinks[v.destUri]!!.bytes
    }

    private fun await(what: String, ms: Long = 40_000, p: () -> Boolean) {
        val end = System.currentTimeMillis() + ms
        while (System.currentTimeMillis() < end) { if (p()) return; Thread.sleep(10) }
        throw AssertionError("timeout waiting for $what")
    }

    private fun slowPhone(data: ByteArray) = Phone(FakeServer(data).also { it.chunkDelayMs = 10; it.chunk = 16 * 1024 }, root, cleanups)
    private fun src(url: String = "https://files.test/pub/data.bin", name: String? = "Movie.mp4") = Source(url, "https://site.test/page", name, "video/mp4", mapOf("Referer" to "https://site.test/", "Authorization" to "secret"), 1, nameFixed = name != null)

    // ------------------------------------------------------------------------------------------------------------------ the happy path
    @Test(timeout = 120_000) fun aDownloadGoesThroughThePhoneAndTheTabletCollectsIt() {
        val data = testBytes(3 * MiB + 7)
        val phone = slowPhone(data)
        val t = Tablet(FakeServer(testBytes(100)))
        t.to(phone); t.startBackend()
        val lid = t.backend.offload(src())!!
        assertTrue(t.backend.owns(lid)); assertTrue(t.backend.needsService())
        assertEquals("a row at once", 1, t.backend.views().size)

        await("live status from the phone") { val v = t.backend.view(lid); v != null && v.state == DlState.RUNNING && v.done > 0 && v.total == data.size.toLong() }
        val live = t.backend.view(lid)!!
        assertEquals("Movie.mp4", live.name); assertEquals("files.test", live.host); assertTrue(live.note!!.startsWith("On your phone")); assertTrue(live.bytesPerSec > 0 || live.done > 0)
        assertTrue(live.actions.containsAll(listOf(Action.PAUSE, Action.CANCEL)))
        assertEquals(DEST_PUBLIC_KIND, live.kind)

        await("the copy to the tablet") { t.backend.view(lid) == null && t.row()?.state == DlState.COMPLETED }
        val row = t.row()!!
        assertArrayEquals("the file on the tablet equals the origin's", data, t.bytesOf(row))
        assertEquals("Movie.mp4", t.sinks.sinks[row.destUri]!!.name)
        assertEquals("Copied from your phone", t.backend.noteFor(row.id)); assertNotNull(t.backend.createdFor(row.id)); assertEquals("the row names the site, not the phone", "files.test", t.backend.originHostFor(row.id))
        assertNull("rows of the tablet's own that were not made by the phone have no note", t.backend.noteFor(row.id + 1000))
        assertEquals("the tablet itself never touched the website", 0, t.origin.seen.size)
        await("the phone's copy is deleted") { phone.host.jobCount == 0 }
        await("the phone's staging folder is empty") { (File(phone.dir, "staging").listFiles() ?: emptyArray()).isEmpty() }
        assertEquals("the phone saw the owner's Referer but never the Authorization header", true, phone.spy.headers.all { h -> h.keys.none { it.equals("authorization", true) } } && phone.spy.headers.any { h -> h.keys.any { it.equals("referer", true) } })
        t.backend.stop()
        assertFalse("nothing left to keep the service alive", t.backend.needsService())
    }

    private val DEST_PUBLIC_KIND = app.kestrel.core.download.DEST_PUBLIC

    @Test(timeout = 120_000) fun aSlowTabletEngineDoesNotMakeThePhoneDeleteTheFileBeforeItIsCopied() {
        val data = testBytes(2 * MiB + 3)
        val phone = slowPhone(data)
        val t = Tablet(FakeServer(testBytes(10)), rowLagMs = 3500)                       // the row shows up 3.5 s after the hand-over: more than three rounds of the backend
        t.to(phone); t.startBackend()
        val lid = t.backend.offload(src())!!
        await("the hand-over") { t.backend.view(lid)?.note == "Copied from your phone" }
        val bridge = t.backend.view(lid)!!
        assertEquals("one row at the hand-over, not none", DlState.RUNNING, bridge.state); assertEquals("Movie.mp4", bridge.name)
        assertEquals("and never two at once", 1, (t.backend.views().size + (if (t.engine.views().isNotEmpty() && t.local.view(t.engine.views()[0].id) != null) 1 else 0)))
        await("copied", 60_000) { t.row()?.state == DlState.COMPLETED }
        assertArrayEquals("the file arrived whole (the phone kept it until then)", data, t.bytesOf(t.row()!!))
        await("then the phone's copy is deleted") { phone.host.jobCount == 0 }
        assertEquals("Copied from your phone", t.backend.noteFor(t.row()!!.id))
    }

    // ------------------------------------------------------------------------------------------------------------------ falling back to the tablet
    @Test(timeout = 90_000) fun aPhoneThatIsNotThereMeansTheTabletDownloadsByItself() {
        val data = testBytes(MiB + 5)
        val t = Tablet(FakeServer(data))
        val dead = ServerSocket(0).use { it.localPort }
        t.target = CourierClient("127.0.0.1", dead, TEST_KEY)
        t.startBackend()
        val lid = t.backend.offload(src())!!
        await("the tablet's own download") { t.row()?.state == DlState.COMPLETED }
        assertArrayEquals(data, t.bytesOf(t.row()!!))
        assertNull(t.backend.view(lid))
        val note = t.backend.noteFor(t.row()!!.id)!!
        assertTrue(note, note.startsWith("Downloading here") && note.contains("not reachable"))
        assertTrue("the original request is used, with the original fetcher", t.origin.seen.size >= 1)
        assertNull("known to be away: the next one goes straight to the tablet, no row that would fall back", t.backend.offload(src("https://files.test/pub/two.bin")))
    }

    @Test(timeout = 90_000) fun noteEligibility() {
        val t = Tablet(FakeServer(testBytes(10)))
        t.to(slowPhone(testBytes(10)))
        t.mode = PhoneMode.OFF
        assertNull(t.backend.offload(src()))
        t.mode = PhoneMode.WHEN_THERE
        assertNull("private tabs never", t.backend.offload(Source("https://a.test/x", priv = true)))
        assertNull("update files stay on the tablet", t.backend.offload(Source("https://a.test/x.apk", destKind = DEST_APP_PRIVATE)))
        for (u in listOf("ftp://a.test/x", "blob:https://a.test/uuid", "data:text/plain,hi", "not a url", "")) assertNull(u, t.backend.offload(Source(u)))
        t.target = null
        assertNull("no phone paired", t.backend.offload(src()))
        assertTrue(t.backend.views().isEmpty())
    }

    @Test(timeout = 90_000) fun aPhoneThatRefusesMeansTheTabletDownloadsByItself() {
        val data = testBytes(MiB)
        val phone = slowPhone(testBytes(10)); phone.dns = listOf(byteArrayOf(192.toByte(), 168.toByte(), 1, 1))
        val t = Tablet(FakeServer(data))
        t.to(phone); t.startBackend()
        t.backend.offload(src())!!
        await("the tablet's own download") { t.row()?.state == DlState.COMPLETED }
        assertArrayEquals(data, t.bytesOf(t.row()!!))
        assertTrue(t.backend.noteFor(t.row()!!.id)!!.contains("private network"))
        assertEquals("the phone never fetched anything", 0, phone.origin.seen.size)
    }

    @Test(timeout = 90_000) fun aFailureOnThePhoneMeansTheTabletTriesItself() {
        val data = testBytes(MiB)
        val phoneOrigin = FakeServer(testBytes(10)).also { o -> repeat(20) { o.statusNext.add(404) } }
        val phone = Phone(phoneOrigin, root, cleanups)
        val t = Tablet(FakeServer(data))
        t.to(phone); t.startBackend()
        t.backend.offload(src())!!
        await("the tablet's own download") { t.row()?.state == DlState.COMPLETED }
        assertArrayEquals(data, t.bytesOf(t.row()!!))
        val note = t.backend.noteFor(t.row()!!.id)!!
        assertTrue(note, note.startsWith("Downloading here: the website refused your phone"))
        await("the failed job is removed from the phone") { phone.host.jobCount == 0 }
    }

    // ------------------------------------------------------------------------------------------------------------------ the buttons
    @Test(timeout = 120_000) fun pauseResumeAndCancelReachThePhone() {
        val data = testBytes(4 * MiB)
        val phone = slowPhone(data)
        val t = Tablet(FakeServer(testBytes(10)))
        t.to(phone); t.startBackend()
        val lid = t.backend.offload(src())!!
        await("progress") { val v = t.backend.view(lid); v != null && v.state == DlState.RUNNING && v.done > 0 }
        t.backend.pause(lid)
        await("paused on the phone") { t.backend.view(lid)?.state == DlState.PAUSED_USER }
        assertTrue(t.backend.view(lid)!!.actions.contains(Action.RESUME))
        val at = t.backend.view(lid)!!.done
        Thread.sleep(300)
        assertEquals("nothing moves while paused", at, t.backend.view(lid)!!.done)
        t.backend.resume(lid)
        await("done") { t.backend.view(lid) == null && t.row()?.state == DlState.COMPLETED }
        assertArrayEquals(data, t.bytesOf(t.row()!!))

        val other = t.backend.offload(src("https://files.test/pub/other.bin", "Other.bin"))!!
        await("progress of the second") { (t.backend.view(other)?.done ?: 0) > 0 }
        t.backend.cancel(other)
        val gone = t.backend.view(other)!!
        assertEquals(DlState.CANCELED, gone.state); assertEquals(listOf(Action.DELETE), gone.actions)
        await("the phone forgot the canceled job") { phone.host.jobCount == 0 }
        t.backend.remove(other)
        assertNull(t.backend.view(other)); assertFalse(t.backend.owns(other))
    }

    // ------------------------------------------------------------------------------------------------------------------ the phone comes and goes
    @Test(timeout = 120_000) fun alwaysModeWaitsForThePhoneInsteadOfUsingTheTabletsConnection() {
        val data = testBytes(2 * MiB)
        val phone = slowPhone(data)
        val t = Tablet(FakeServer(testBytes(10)), PhoneMode.ALWAYS)
        val dead = ServerSocket(0).use { it.localPort }
        t.target = CourierClient("127.0.0.1", dead, TEST_KEY)
        t.startBackend()
        val lid = t.backend.offload(src())!!
        await("waiting row") { t.backend.view(lid)?.state == DlState.WAITING_NETWORK }
        assertEquals("Waiting for your phone", t.backend.view(lid)!!.note)
        Thread.sleep(1500)
        assertEquals("still waiting, the tablet's own connection untouched", 0, t.origin.seen.size)
        assertTrue(t.engine.views().isEmpty())
        t.to(phone)                                                                      // the hotspot comes up
        await("it runs on the phone now") { t.backend.view(lid)?.state == DlState.RUNNING }
        await("copied") { t.backend.view(lid) == null && t.row()?.state == DlState.COMPLETED }
        assertArrayEquals(data, t.bytesOf(t.row()!!)); assertEquals(0, t.origin.seen.size)
    }

    @Test(timeout = 120_000) fun aPhoneThatDropsOutMidwayIsWaitedFor() {
        val data = testBytes(3 * MiB)
        val phone = slowPhone(data)
        val t = Tablet(FakeServer(testBytes(10)))
        val sw = t.to(phone); t.startBackend()
        val lid = t.backend.offload(src())!!
        await("running") { (t.backend.view(lid)?.done ?: 0) > 0 }
        sw.down = true
        await("waiting for the phone") { t.backend.view(lid)?.state == DlState.WAITING_NETWORK }
        assertEquals("Can't reach your phone", t.backend.view(lid)!!.note)
        assertTrue("cancel is always offered", t.backend.view(lid)!!.actions.contains(Action.CANCEL))
        sw.down = false
        await("back to normal") { t.backend.view(lid)?.state == DlState.RUNNING || t.backend.view(lid) == null }
        await("copied") { t.backend.view(lid) == null && t.row()?.state == DlState.COMPLETED }
        assertArrayEquals(data, t.bytesOf(t.row()!!)); assertEquals(0, t.origin.seen.size)
    }

    @Test(timeout = 120_000) fun aRestartedAppPicksUpWhereItLeftOff() {
        val data = testBytes(3 * MiB)
        val phone = slowPhone(data)
        val t = Tablet(FakeServer(testBytes(10)))
        t.to(phone); t.startBackend()
        val lid = t.backend.offload(src())!!
        await("progress") { (t.backend.view(lid)?.done ?: 0) > 0 }
        t.backend.stop()                                                                 // the app is killed
        t.backend = t.newBackend()
        t.startBackend()                                                                 // and started again: the same row is back
        assertTrue(t.backend.owns(lid)); assertEquals("Movie.mp4", t.backend.view(lid)!!.name)
        await("copied") { t.backend.view(lid) == null && t.row()?.state == DlState.COMPLETED }
        assertArrayEquals(data, t.bytesOf(t.row()!!)); assertEquals(0, t.origin.seen.size)
        await("phone cleaned") { phone.host.jobCount == 0 }
        val again = t.newBackend().also { it.restore() }
        assertTrue("a finished download keeps its note until the row goes", again.noteFor(t.row()!!.id) == "Copied from your phone")
        assertTrue(again.views().isEmpty())
    }

    // ------------------------------------------------------------------------------------------------------------------ video streams through the phone
    private fun tsPlaylist(cdn: FakeCdn, names: List<String> = listOf("a_0.ts", "a_1.ts", "a_2.ts")): String {
        val sb = StringBuilder("#EXTM3U\n#EXT-X-VERSION:3\n#EXT-X-TARGETDURATION:2\n#EXT-X-MEDIA-SEQUENCE:0\n")
        for ((i, n) in names.withIndex()) { cdn.files["https://cdn.test/v/s$i.ts"] = Fixtures.bytes(n); sb.append("#EXTINF:2.000,\ns$i.ts\n") }
        return sb.append("#EXT-X-ENDLIST\n").toString()
    }

    private fun clip(playlist: String, fetcher: Int = 1) = StreamSource("https://cdn.test/v/index.m3u8", playlist, "https://site.test/watch", "My clip", mapOf("Referer" to "https://site.test/"), fetcher, meteredOk = true)
    private fun sampleSizes(file: ByteArray): List<Any> { val r = Mp4Reader(file); val v = r.track("vide")!!; val a = r.track("soun")!!; return listOf(v.samples.map { it.size }, a.samples.map { it.size }, v.width, v.height) }

    @Test(timeout = 120_000) fun aVideoStreamIsDownloadedAndWrittenAsAnMp4OnThePhoneAndCopiedToTheTablet() {
        val pcdn = FakeCdn().also { it.chunkDelayMs = 3 }
        val phone = Phone(FakeServer(testBytes(10)), root, cleanups, cdn = pcdn)
        val playlist = tsPlaylist(pcdn)
        val tcdn = FakeCdn()
        val t = Tablet(FakeServer(testBytes(10)), tabletCdn = tcdn)
        t.to(phone); t.startBackend()
        val lid = t.backend.offloadStream(clip(playlist))!!
        assertEquals("My clip.mp4", t.backend.view(lid)!!.name); assertEquals("site.test", t.backend.view(lid)!!.host.let { "site.test" })
        await("the phone works on it") { t.backend.view(lid)?.note?.startsWith("On your phone") == true }
        await("copied") { t.backend.view(lid) == null && t.row()?.state == DlState.COMPLETED }
        val row = t.row()!!
        assertEquals("My clip.mp4", t.sinks.sinks[row.destUri]!!.name)
        val r = Mp4Reader(t.bytesOf(row))
        assertEquals(listOf("ftyp", "wide", "mdat", "moov"), r.topLevel)
        assertEquals(150, r.track("vide")!!.samples.size); assertEquals(260, r.track("soun")!!.samples.size)
        assertEquals("the tablet never touched the video site", 0, tcdn.requests.size); assertTrue(pcdn.requests.size >= 3)
        assertEquals("Copied from your phone", t.backend.noteFor(row.id))
        await("the phone's copy is gone") { phone.host.jobCount == 0 }
    }

    @Test(timeout = 120_000) fun aPhoneWithoutTheSegmentDownloaderLeavesTheVideoToTheTablet() {
        val phone = Phone(FakeServer(testBytes(10)), root, cleanups)                       // no stream engine: kind "stream" is refused with 501
        val tcdn = FakeCdn()
        val playlist = tsPlaylist(tcdn)
        val t = Tablet(FakeServer(testBytes(10)), tabletCdn = tcdn)
        t.to(phone); t.startBackend()
        t.backend.offloadStream(clip(playlist))!!
        await("the tablet's own stream download") { t.streamEngine!!.views().firstOrNull()?.state == DlState.COMPLETED }
        val v = t.streamEngine!!.views().first()
        assertEquals(150, Mp4Reader(t.sinks.sinks[v.destUri]!!.bytes).track("vide")!!.samples.size)
        val note = t.backend.noteFor(v.id)!!
        assertTrue(note, note.startsWith("Downloading here: your phone's Courier cannot do this kind of download yet"))
    }

    /** Stands in for the phone's platform converter: makes ONE file of the picture and the sound so the test can see what was merged, and counts how often it was asked. */
    private class MergeFake(private val sinks: SinkFactory) : StreamFinisher {
        val merged = java.util.concurrent.atomic.AtomicInteger()
        val asked = java.util.concurrent.atomic.AtomicInteger()
        @Volatile var fails = false
        override fun convert(j: StreamFinisher.Joined): StreamFinisher.Converted? = null
        override fun merge(m: StreamFinisher.Merge): StreamFinisher.Converted? {
            asked.incrementAndGet()
            if (fails) return null
            val v = File(java.net.URI(m.videoUri)).readBytes(); val a = File(java.net.URI(m.audioUri)).readBytes()
            val out = "MERGED|".toByteArray() + v + a
            val sink = sinks.create(m.stem + ".mp4", "video/mp4", -1, m.priv, m.destKind)
            sink.pwrite(0, out, 0, out.size); sink.sync()
            val uri = sink.publish() ?: return null
            merged.incrementAndGet()
            return StreamFinisher.Converted(uri, sink.displayName ?: (m.stem + ".mp4"), "video/mp4", out.size.toLong())
        }
    }

    private fun soundPlaylist(cdn: FakeCdn): StreamSource {
        val sb = StringBuilder("#EXTM3U\n#EXT-X-VERSION:3\n#EXT-X-TARGETDURATION:2\n#EXT-X-MEDIA-SEQUENCE:0\n")
        for (i in 0..1) { cdn.files["https://cdn.test/a/s$i.ts"] = Fixtures.bytes("a_$i.ts"); sb.append("#EXTINF:2.000,\ns$i.ts\n") }
        return StreamSource("https://cdn.test/a/index.m3u8", sb.append("#EXT-X-ENDLIST\n").toString(), "https://site.test/watch", "My clip", mapOf("Referer" to "https://site.test/"), 1, meteredOk = true, ext = "m4a")
    }

    @Test(timeout = 120_000) fun pictureAndSoundKeptApartAreBothDownloadedAndMergedOnThePhoneAndCopiedToTheTablet() {
        val pcdn = FakeCdn().also { it.chunkDelayMs = 3 }
        var fin: MergeFake? = null
        val phone = Phone(FakeServer(testBytes(10)), root, cleanups, cdn = pcdn, finisherOf = { s -> MergeFake(s).also { fin = it } })
        val picture = clip(tsPlaylist(pcdn))
        val tcdn = FakeCdn()
        val t = Tablet(FakeServer(testBytes(10)), tabletCdn = tcdn)
        t.to(phone); t.startBackend()
        val lid = t.backend.offloadStream(StreamSource(picture.playlistUrl, picture.playlistText, picture.pageUrl, picture.nameStem, picture.headers, picture.fetcher, meteredOk = true, companion = soundPlaylist(pcdn)))!!
        await("the phone works on it") { t.backend.view(lid)?.note?.startsWith("On your phone") == true }
        await("copied") { t.backend.view(lid) == null && t.row()?.state == DlState.COMPLETED }
        val row = t.row()!!
        assertEquals("one row, one file: the merged one", "My clip.mp4", t.sinks.sinks[row.destUri]!!.name)
        assertEquals("MERGED|", String(t.bytesOf(row).copyOfRange(0, 7)))
        assertEquals("the phone's converter was asked once", 1, fin!!.merged.get())
        assertEquals("the tablet never touched the video site", 0, tcdn.requests.size)
        assertTrue("picture and sound both came from the phone's connection", pcdn.requests.any { it.contains("/a/s0.ts") } && pcdn.requests.any { it.contains("/v/s0.ts") })
        await("the phone's copy is gone") { phone.host.jobCount == 0 }
        assertEquals("Copied from your phone", t.backend.noteFor(row.id))
    }

    @Test(timeout = 120_000) fun pictureAndSoundThatThePhoneCannotMergeAreDownloadedByTheTabletAndLeaveNothingOnThePhone() {
        val pcdn = FakeCdn().also { it.chunkDelayMs = 3 }
        var fin: MergeFake? = null
        val phone = Phone(FakeServer(testBytes(10)), root, cleanups, cdn = pcdn, finisherOf = { s -> MergeFake(s).also { it.fails = true; fin = it } })
        val tcdn = FakeCdn()
        val pic = clip(tsPlaylist(pcdn)); tsPlaylist(tcdn); soundPlaylist(pcdn)
        val t = Tablet(FakeServer(testBytes(10)), tabletCdn = tcdn)
        t.to(phone); t.startBackend()
        t.backend.offloadStream(StreamSource(pic.playlistUrl, pic.playlistText, pic.pageUrl, pic.nameStem, pic.headers, pic.fetcher, meteredOk = true, companion = soundPlaylist(tcdn)))!!
        await("the tablet downloads it itself after the phone gave up") { t.streamEngine!!.views().any { it.state == DlState.COMPLETED } }
        assertEquals("the phone's converter was asked and said no", 1, fin!!.asked.get())
        val note = t.backend.noteFor(t.streamEngine!!.views().first().id)!!
        assertTrue(note, note.startsWith("Downloading here: your phone could not convert this video"))
        assertTrue("the tablet fetched the picture itself", tcdn.requests.any { it.contains("/v/s0.ts") })
        await("the phone forgot the job") { phone.host.jobCount == 0 }
        await("no picture without sound, no stray sound file, no stream record is left on the phone") {
            File(phone.dir, "staging").walkTopDown().none { it.isFile } && File(phone.dir, "streams").listFiles { f -> f.name.endsWith(".json") }.orEmpty().isEmpty()
        }
    }

    @Test fun aSoundThatIsNotAWebAddressKeepsTheWholeVideoOnTheTablet() {
        val t = Tablet(FakeServer(testBytes(10)), tabletCdn = FakeCdn())
        t.to(Phone(FakeServer(testBytes(10)), root, cleanups, cdn = FakeCdn()))
        val bad = StreamSource("blob:https://a.test/x", null, null, "My clip", ext = "m4a")
        assertNull(t.backend.offloadStream(StreamSource("https://cdn.test/v/index.mpd", null, null, "My clip", repId = "v1", companion = bad)))
    }

    @Test fun otherKindsOfStreamsAreNeverOffered() {
        val t = Tablet(FakeServer(testBytes(10)), tabletCdn = FakeCdn())
        t.to(Phone(FakeServer(testBytes(10)), root, cleanups, cdn = FakeCdn()))
        assertNull("private tabs never", t.backend.offloadStream(StreamSource("https://cdn.test/v/index.m3u8", "x", null, "My clip", priv = true)))
        assertNull("not a web address", t.backend.offloadStream(StreamSource("blob:https://a.test/x", "x", null, "My clip")))
        assertTrue(t.backend.views().isEmpty())
    }

    @Test(timeout = 180_000) fun aPhoneThatIsKilledInTheMiddleOfAVideoFinishesItAfterTheRestart() {
        val pcdn = FakeCdn().also { it.chunkDelayMs = 150; it.chunk = 4 * 1024 }
        val phone1 = Phone(FakeServer(testBytes(10)), root, cleanups, cdn = pcdn)
        val playlist = tsPlaylist(pcdn)
        val t = Tablet(FakeServer(testBytes(10)), tabletCdn = FakeCdn())
        t.to(phone1); t.startBackend()
        val lid = t.backend.offloadStream(clip(playlist))!!
        await("the phone has the job") { t.backend.view(lid)?.note?.startsWith("On your phone") == true }
        Thread.sleep(2500)                                                                      // part of the video is in, not all of it
        phone1.stop()                                                                           // the app is killed
        await("the tablet notices") { t.backend.view(lid)?.state == DlState.WAITING_NETWORK }
        val phone2 = Phone(FakeServer(testBytes(10)), root, cleanups, dir = phone1.dir, cdn = pcdn)      // and starts again on the same folders
        t.to(phone2)
        await("copied", 150_000) { t.backend.view(lid) == null && t.row()?.state == DlState.COMPLETED }
        // the same video, written without a kill, is the same MP4
        val pcdn2 = FakeCdn(); val phoneB = Phone(FakeServer(testBytes(10)), java.nio.file.Files.createTempDirectory("courier-b").toFile(), cleanups, cdn = pcdn2)
        val tB = Tablet(FakeServer(testBytes(10)), tabletCdn = FakeCdn())
        tB.to(phoneB); tB.startBackend()
        tB.backend.offloadStream(clip(tsPlaylist(pcdn2)))!!
        await("the uninterrupted copy", 100_000) { tB.row()?.state == DlState.COMPLETED }
        assertEquals(sampleSizes(tB.bytesOf(tB.row()!!)), sampleSizes(t.bytesOf(t.row()!!)))
    }

    // ------------------------------------------------------------------------------------------------------------------ the link test
    @Test fun theVerdictSaysWhatTheNumbersMean() {
        val mb = 1_000_000L
        fun v(lan: Long, phone: Long, tablet: Long) = LinkTest.verdict(lan * mb, phone * mb, tablet * mb)
        assertTrue(v(20, 10, 2).startsWith("Worth it"))
        assertTrue(v(5, 10, 2).startsWith("Partly worth it"))
        assertTrue(v(2, 10, 2).startsWith("Not worth it for speed"))
        assertTrue(v(20, 5, 4).startsWith("No big gain"))
        assertTrue(v(20, 10, 0).contains("phone is the better choice"))
        assertTrue(v(0, 10, 2).contains("could not be measured"))
        assertTrue(LinkTest.verdict(20 * mb, 0, 2 * mb).contains("could not run"))
        assertTrue(LinkTest.verdict(1, 0, 0).contains("could not reach the internet"))
        assertTrue(v(20, 10, 2).contains("5.0 times"))
    }

    @Test(timeout = 60_000) fun theLinkTestMeasuresRealSpeeds() {
        val phone = Phone(FakeServer(testBytes(3 * MiB)), root, cleanups)
        val client = CourierClient("127.0.0.1", phone.port, TEST_KEY)
        val tabletNet = FakeServer(testBytes(3 * MiB))
        val rep = LinkTest.run(client, tabletNet, "https://files.test/pub/data.bin", lanBytes = 2_000_000, netBytes = 1_000_000)
        assertTrue(rep.problems.toString(), rep.problems.isEmpty())
        assertTrue(rep.lanBps > 0 && rep.phoneNetBps > 0 && rep.tabletNetBps > 0)
        assertTrue(rep.verdict.isNotEmpty())
        val broken = LinkTest.run(CourierClient("127.0.0.1", ServerSocket(0).use { it.localPort }, TEST_KEY), tabletNet, "https://files.test/pub/data.bin", lanBytes = 1000, netBytes = 1000)
        assertEquals(0L, broken.lanBps); assertTrue(broken.problems.any { it.startsWith("link:") })
    }
}
