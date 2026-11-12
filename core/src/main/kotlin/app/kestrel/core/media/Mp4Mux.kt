package app.kestrel.core.media

import java.util.BitSet

/** Where the muxer puts its bytes: the download's file, addressed by absolute position. [truncate] drops everything after [len] (stale bytes of an earlier, interrupted run). */
interface MuxOut {
    fun write(pos: Long, b: ByteArray, off: Int, len: Int)
    fun truncate(len: Long) {}
}

/** Growable big-endian byte buffer with MP4 box helpers (also used for the journal's variable-length integers). */
internal class Bytes(cap: Int = 256) {
    var a = ByteArray(cap)
    var n = 0

    private fun ensure(k: Int) { if (n + k > a.size) a = a.copyOf(maxOf(a.size * 2, n + k)) }
    fun clear() { n = 0 }
    fun u8(v: Int) { ensure(1); a[n++] = v.toByte() }
    fun u16(v: Int) { u8(v shr 8); u8(v) }
    fun u32(v: Long) { u8((v shr 24).toInt()); u8((v shr 16).toInt()); u8((v shr 8).toInt()); u8(v.toInt()) }
    fun u32(v: Int) = u32(v.toLong() and 0xFFFFFFFFL)
    fun u64(v: Long) { u32(v ushr 32); u32(v and 0xFFFFFFFFL) }
    fun str(s: String) { for (c in s) u8(c.code) }
    fun put(b: ByteArray, off: Int = 0, len: Int = b.size) { ensure(len); System.arraycopy(b, off, a, n, len); n += len }
    fun zeros(k: Int) { ensure(k); java.util.Arrays.fill(a, n, n + k, 0); n += k }
    fun patch32(at: Int, v: Int) { a[at] = (v ushr 24).toByte(); a[at + 1] = (v ushr 16).toByte(); a[at + 2] = (v ushr 8).toByte(); a[at + 3] = v.toByte() }

    inline fun box(type: String, body: () -> Unit) { val at = n; u32(0); str(type); body(); patch32(at, n - at) }
    inline fun fullBox(type: String, version: Int, flags: Int, body: () -> Unit) = box(type) { u8(version); u8(flags shr 16); u8(flags shr 8); u8(flags); body() }

    // variable-length integers for the journal (LEB128; signed values zig-zag coded)
    fun varint(v: Long) { var x = v; while (x and 0x7F.inv().toLong() != 0L) { u8(((x and 0x7F) or 0x80).toInt()); x = x ushr 7 }; u8(x.toInt()) }
    fun zigzag(v: Long) = varint((v shl 1) xor (v shr 63))
}

/** Reads what [Bytes.varint] wrote. */
internal class Reader(private val b: ByteArray, private val end: Int = b.size) {
    var pos = 0
    val more: Boolean get() = pos < end
    fun u8(): Int { check(pos < end) { "journal ends early" }; return b[pos++].toInt() and 0xFF }
    fun varint(): Long { var v = 0L; var shift = 0; while (true) { val x = u8(); v = v or ((x and 0x7F).toLong() shl shift); if (x and 0x80 == 0) return v; shift += 7; check(shift < 70) } }
    fun zigzag(): Long { val z = varint(); return (z ushr 1) xor -(z and 1) }
    fun bytes(n: Int): ByteArray { check(pos + n <= end) { "journal ends early" }; val r = b.copyOfRange(pos, pos + n); pos += n; return r }
}

/**
 * Writes a plain (progressive, not fragmented) MP4 while the samples are still arriving: `ftyp`, a `wide`+`mdat` header that is patched at the end, the samples in arrival order, and the
 * `moov` with all sample tables after them. Local files do not need the `moov` in front, and a progressive file scrubs and shares everywhere (a fragmented one does not).
 *
 * One picture track (H.264, 90 kHz) and optionally one AAC track (time scale = its sample rate). Everything it does to its tables is also recorded in a small journal ([drainJournal]) in
 * the exact order it happened, so a fresh muxer can be brought to the same state with [replay] after the process was killed (the samples are already in the file).
 * [mark] / [rollback] cut the tables and the write position back to an earlier point (an advertisement run that turned out not to belong in the file).
 */
class Mp4Mux(private val out: MuxOut, private val nowMs: () -> Long = { System.currentTimeMillis() }) {
    /** A point the muxer can go back to. Taking one closes the current chunk. */
    class Mark(val pos: Long, val v: Int, val a: Int, val c: Int, val aEnd: Long)

    var pos = HEADER_END
        private set
    private var writing = true

    // picture track: size, decode time (90 kHz, on the muxer's own timeline), composition offset, sync flag
    private var vSize = IntArray(2048)
    private var vDts = LongArray(2048)
    private var vCts = IntArray(2048)
    private val vSync = BitSet()
    var videoCount = 0
        private set
    var width = 0
        private set
    var height = 0
        private set
    private var avcC: ByteArray? = null

    // sound track: size and duration per frame (1024 ticks of the sample rate; a gap in the stream lengthens the frame before it)
    private var aSize = IntArray(2048)
    private var aDur = IntArray(2048)
    var audioCount = 0
        private set
    private var aFirstTick = 0L
    private var aEndTick = 0L
    private var asc: ByteArray? = null
    var sampleRate = 0
        private set
    private var channels = 0

    // chunks: runs of samples of one track that are contiguous in the file
    private var cOff = LongArray(1024)
    private var cTrack = ByteArray(1024)
    private var cSamples = IntArray(1024)
    private var cCount = 0
    private var open = false
    private var openTrack = 0

    private val journal = Bytes(1024)

    /** The decode time of the last picture (90 kHz) and the nominal length of a picture; the glue uses them to place the next run. */
    val lastVideoDts: Long get() = if (videoCount > 0) vDts[videoCount - 1] else 0L
    val videoDuration: Int get() = lastVideoDuration()
    /** End of the sound track in 90 kHz ticks (0 without sound). */
    val audioEnd90k: Long get() = if (sampleRate > 0 && audioCount > 0) aEndTick * 90000L / sampleRate else 0L
    val hasAudioConfig: Boolean get() = asc != null && sampleRate > 0

    /** Writes the file header; a muxer rebuilt by [replay] must not (the header is in the file already). */
    fun start() {
        val h = Bytes(HEADER_END.toInt())
        h.box("ftyp") { h.str("isom"); h.u32(512); h.str("isom"); h.str("iso2"); h.str("avc1"); h.str("mp41") }
        h.u32(8); h.str("wide")
        h.u32(0); h.str("mdat")
        check(h.n.toLong() == HEADER_END)
        out.write(0, h.a, 0, h.n)
    }

    fun setVideo(w: Int, h: Int, avcConfig: ByteArray) {
        width = w; height = h; avcC = avcConfig
        journal.u8(E_CFGV); journal.varint(w.toLong()); journal.varint(h.toLong()); journal.varint(avcConfig.size.toLong()); journal.put(avcConfig)
    }

    fun setAudio(config: ByteArray, rate: Int, channelCount: Int) {
        asc = config; sampleRate = rate; channels = channelCount
        journal.u8(E_CFGA); journal.varint(rate.toLong()); journal.varint(channelCount.toLong()); journal.varint(config.size.toLong()); journal.put(config)
    }

    fun addVideo(b: ByteArray, off: Int, len: Int, dts: Long, cts: Int, sync: Boolean) {
        require(len > 0)
        if (writing) out.write(pos, b, off, len)
        val prev = if (videoCount > 0) vDts[videoCount - 1] else 0L
        applyVideo(len, dts, cts, sync)
        journal.u8(E_V); journal.varint((len.toLong() shl 1) or (if (sync) 1L else 0L)); journal.zigzag(dts - prev); journal.zigzag(cts.toLong())
    }

    /** [n] AAC frames, [sizes] long, laid end to end in `b[off until off+total]`; [startTick] is the stream time of the first in sample-rate ticks. */
    fun addAudio(b: ByteArray, off: Int, total: Int, sizes: IntArray, n: Int, startTick: Long) {
        require(n > 0 && sampleRate > 0)
        if (writing) out.write(pos, b, off, total)
        val before = aEndTick
        applyAudio(sizes, n, total, startTick)
        journal.u8(E_A); journal.zigzag(startTick - (if (audioCount > n) before else 0L)); journal.varint(n.toLong()); for (i in 0 until n) journal.varint(sizes[i].toLong())
    }

    fun closeChunk() { open = false; journal.u8(E_CLOSE) }

    fun mark(): Mark { closeChunk(); return Mark(pos, videoCount, audioCount, cCount, aEndTick) }

    fun rollback(m: Mark) {
        applyRollback(m.pos, m.v, m.a, m.c, m.aEnd)
        journal.u8(E_ROLLBACK); journal.varint(m.pos); journal.varint(m.v.toLong()); journal.varint(m.a.toLong()); journal.varint(m.c.toLong()); journal.zigzag(m.aEnd)
    }

    /** The table changes since the last call, in order, ready to be appended to the journal file. */
    fun drainJournal(): ByteArray { val r = journal.a.copyOf(journal.n); journal.clear(); return r }

    /** Re-applies what [drainJournal] returned (in the same order); the file is not written. */
    fun replay(events: ByteArray) {
        val r = Reader(events)
        writing = false
        try {
            var prevDts = if (videoCount > 0) vDts[videoCount - 1] else 0L
            while (r.more) {
                when (val e = r.u8()) {
                    E_V -> {
                        val sz = r.varint(); val dts = prevDts + r.zigzag(); val cts = r.zigzag().toInt()
                        applyVideo((sz ushr 1).toInt(), dts, cts, sz and 1L == 1L); prevDts = dts
                    }
                    E_A -> {
                        val start = r.zigzag(); val n = r.varint().toInt()
                        val sizes = IntArray(n); var total = 0
                        for (i in 0 until n) { sizes[i] = r.varint().toInt(); total += sizes[i] }
                        applyAudio(sizes, n, total, (if (audioCount > 0) aEndTick else 0L) + start)
                    }
                    E_CLOSE -> open = false
                    E_ROLLBACK -> {
                        val p = r.varint(); val v = r.varint().toInt(); val a = r.varint().toInt(); val c = r.varint().toInt(); val ae = r.zigzag()
                        applyRollback(p, v, a, c, ae); prevDts = if (videoCount > 0) vDts[videoCount - 1] else 0L
                    }
                    E_CFGV -> { val w = r.varint().toInt(); val h = r.varint().toInt(); val l = r.varint().toInt(); width = w; height = h; avcC = r.bytes(l) }
                    E_CFGA -> { val rate = r.varint().toInt(); val ch = r.varint().toInt(); val l = r.varint().toInt(); sampleRate = rate; channels = ch; asc = r.bytes(l) }
                    else -> error("unknown journal event $e")
                }
            }
        } finally { writing = true }
    }

    // ---- the state changes, shared by the live calls and the replay

    private fun applyVideo(len: Int, dts: Long, cts: Int, sync: Boolean) {
        if (videoCount == vSize.size) { val n = videoCount * 2; vSize = vSize.copyOf(n); vDts = vDts.copyOf(n); vCts = vCts.copyOf(n) }
        vSize[videoCount] = len; vDts[videoCount] = dts; vCts[videoCount] = cts
        if (sync) vSync.set(videoCount)
        videoCount++
        chunk(0, 1, len)
    }

    private fun applyAudio(sizes: IntArray, n: Int, total: Int, startTick: Long) {
        if (audioCount == 0) { aFirstTick = startTick; aEndTick = startTick }
        else {
            val gap = startTick - aEndTick
            if (gap > sampleRate / 20) { aDur[audioCount - 1] += gap.toInt(); aEndTick = startTick }     // a real gap in the stream: the frame before it is held that much longer
        }
        for (i in 0 until n) {
            if (audioCount == aSize.size) { val m = audioCount * 2; aSize = aSize.copyOf(m); aDur = aDur.copyOf(m) }
            aSize[audioCount] = sizes[i]; aDur[audioCount] = AAC_FRAME; audioCount++
            aEndTick += AAC_FRAME
        }
        chunk(1, n, total)
    }

    private fun applyRollback(p: Long, v: Int, a: Int, c: Int, aEnd: Long) {
        if (v < videoCount) vSync.clear(v, videoCount)
        videoCount = v; audioCount = a; cCount = c; pos = p; aEndTick = aEnd; open = false
    }

    private fun chunk(track: Int, samples: Int, bytes: Int) {
        if (!(open && openTrack == track)) {
            if (cCount == cOff.size) { val m = cCount * 2; cOff = cOff.copyOf(m); cTrack = cTrack.copyOf(m); cSamples = cSamples.copyOf(m) }
            cOff[cCount] = pos; cTrack[cCount] = track.toByte(); cSamples[cCount] = 0; cCount++
            open = true; openTrack = track
        }
        cSamples[cCount - 1] += samples
        pos += bytes
    }

    private fun lastVideoDuration(): Int = when {
        videoCount >= 2 -> (vDts[videoCount - 1] - vDts[videoCount - 2]).coerceIn(1L, 450_000L).toInt()
        else -> 3000
    }

    /**
     * Ends the file: the `moov` goes after the samples, the `mdat` header in front is patched with the real size, and anything left over from an interrupted earlier run beyond the end is cut
     * off. Returns the file's length. Needs at least one picture.
     */
    fun finish(): Long {
        check(videoCount > 0 && avcC != null) { "no picture" }
        val moov = buildMoov()
        val mdatEnd = pos
        out.write(mdatEnd, moov.a, 0, moov.n)
        val mdatBox = mdatEnd - (HEADER_END - 8)
        val h = Bytes(16)
        if (mdatBox <= 0xFFFFFFFFL) {
            h.u32(mdatBox); h.str("mdat")
            out.write(HEADER_END - 8, h.a, 0, 8)
        } else {                                                                // more than 4 GB: the `wide` box makes room for the 64-bit size
            h.u32(1); h.str("mdat"); h.u64(mdatEnd - (HEADER_END - 16))
            out.write(HEADER_END - 16, h.a, 0, 16)
        }
        val total = mdatEnd + moov.n
        out.truncate(total)
        return total
    }

    // ---- moov

    private fun ms(ticks: Long, scale: Long): Long = (ticks * 1000L + scale / 2) / scale

    private fun buildMoov(): Bytes {
        val m = Bytes(64 * 1024 + videoCount * 12 + audioCount * 10)
        val hasAudio = audioCount > 0 && asc != null && sampleRate > 0
        val vFirst = vDts[0]
        val vMediaDur = vDts[videoCount - 1] - vFirst + lastVideoDuration()
        val vPts0 = vFirst + vCts[0]
        val aFirst90 = if (hasAudio) aFirstTick * 90000L / sampleRate else Long.MAX_VALUE
        val aMediaDur = if (hasAudio) aEndTick - aFirstTick else 0L
        val p0 = minOf(vPts0, aFirst90)
        val vDelta = vFirst - p0                                                // > 0: the picture starts later than the earliest sample, < 0: skip into the media
        val vLead = maxOf(vDelta, 0L); val vSkip = maxOf(-vDelta, 0L)
        val vEditDur = ms(vMediaDur - vSkip, 90000L)
        val vTrackDur = ms(vLead, 90000L) + vEditDur
        val aLead = if (hasAudio) aFirst90 - p0 else 0L
        val aEditDur = if (hasAudio) ms(aMediaDur, sampleRate.toLong()) else 0L
        val aTrackDur = if (hasAudio) ms(aLead, 90000L) + aEditDur else 0L
        val movieDur = maxOf(vTrackDur, aTrackDur)
        val created = nowMs() / 1000 + 2082844800L

        m.box("moov") {
            m.fullBox("mvhd", 0, 0) {
                m.u32(created); m.u32(created); m.u32(1000); m.u32(movieDur)
                m.u32(0x00010000); m.u16(0x0100); m.zeros(10); matrix(m); m.zeros(24); m.u32(if (hasAudio) 3 else 2)
            }
            m.box("trak") {
                m.fullBox("tkhd", 0, 7) {
                    m.u32(created); m.u32(created); m.u32(1); m.u32(0); m.u32(vTrackDur)
                    m.zeros(8); m.u16(0); m.u16(0); m.u16(0); m.u16(0); matrix(m); m.u32(width.toLong() shl 16); m.u32(height.toLong() shl 16)
                }
                edits(m, vLead, vSkip, vEditDur)
                m.box("mdia") {
                    m.fullBox("mdhd", 0, 0) { m.u32(created); m.u32(created); m.u32(90000); m.u32(vMediaDur); m.u16(0x55C4); m.u16(0) }
                    handler(m, "vide", "VideoHandler")
                    m.box("minf") {
                        m.fullBox("vmhd", 0, 1) { m.zeros(8) }
                        dinf(m)
                        m.box("stbl") { videoTables(m) }
                    }
                }
            }
            if (hasAudio) m.box("trak") {
                m.fullBox("tkhd", 0, 7) {
                    m.u32(created); m.u32(created); m.u32(2); m.u32(0); m.u32(aTrackDur)
                    m.zeros(8); m.u16(0); m.u16(0); m.u16(0x0100); m.u16(0); matrix(m); m.u32(0); m.u32(0)
                }
                edits(m, aLead, 0L, aEditDur)
                m.box("mdia") {
                    m.fullBox("mdhd", 0, 0) { m.u32(created); m.u32(created); m.u32(sampleRate.toLong()); m.u32(aMediaDur); m.u16(0x55C4); m.u16(0) }
                    handler(m, "soun", "SoundHandler")
                    m.box("minf") {
                        m.fullBox("smhd", 0, 0) { m.zeros(4) }
                        dinf(m)
                        m.box("stbl") { audioTables(m, aMediaDur) }
                    }
                }
            }
        }
        return m
    }

    private fun matrix(m: Bytes) { m.u32(0x00010000); m.u32(0); m.u32(0); m.u32(0); m.u32(0x00010000); m.u32(0); m.u32(0); m.u32(0); m.u32(0x40000000) }

    /** `edts`/`elst`: an empty edit of [lead] ticks (90 kHz) when the track starts after the earliest sample, then the media from [skip] on; written only when needed. */
    private fun edits(m: Bytes, lead: Long, skip: Long, mainMs: Long) {
        val leadMs = ms(lead, 90000L)
        if (leadMs == 0L && skip == 0L) return
        m.box("edts") {
            m.fullBox("elst", 0, 0) {
                m.u32(if (leadMs > 0) 2 else 1)
                if (leadMs > 0) { m.u32(leadMs); m.u32(-1); m.u16(1); m.u16(0) }
                m.u32(mainMs); m.u32(skip); m.u16(1); m.u16(0)
            }
        }
    }

    private fun handler(m: Bytes, type: String, name: String) {
        m.fullBox("hdlr", 0, 0) { m.u32(0); m.str(type); m.zeros(12); m.str(name); m.u8(0) }
    }

    private fun dinf(m: Bytes) {
        m.box("dinf") { m.fullBox("dref", 0, 0) { m.u32(1); m.fullBox("url ", 0, 1) {} } }
    }

    private fun videoTables(m: Bytes) {
        m.fullBox("stsd", 0, 0) {
            m.u32(1)
            m.box("avc1") {
                m.zeros(6); m.u16(1); m.zeros(16); m.u16(width); m.u16(height); m.u32(0x00480000); m.u32(0x00480000); m.u32(0); m.u16(1); m.zeros(32); m.u16(0x0018); m.u16(0xFFFF)
                m.box("avcC") { m.put(avcC!!) }
            }
        }
        // decoding times, run-length coded
        val lastDur = lastVideoDuration().toLong()
        runs(m, "stts", videoCount) { i -> if (i < videoCount - 1) vDts[i + 1] - vDts[i] else lastDur }
        // composition offsets (omitted when every picture is shown at its decode time)
        var any = false; var neg = false
        for (i in 0 until videoCount) { if (vCts[i] != 0) any = true; if (vCts[i] < 0) neg = true }
        if (any) runs(m, "ctts", videoCount, version = if (neg) 1 else 0) { i -> vCts[i].toLong() }
        // sync samples (omitted when all are)
        val syncs = vSync.cardinality()
        if (syncs in 1 until videoCount) m.fullBox("stss", 0, 0) { m.u32(syncs); var i = vSync.nextSetBit(0); while (i >= 0) { m.u32(i + 1); i = vSync.nextSetBit(i + 1) } }
        chunkTables(m, 0)
        m.fullBox("stsz", 0, 0) { m.u32(0); m.u32(videoCount); for (i in 0 until videoCount) m.u32(vSize[i]) }
    }

    private fun audioTables(m: Bytes, mediaDur: Long) {
        m.fullBox("stsd", 0, 0) {
            m.u32(1)
            m.box("mp4a") {
                m.zeros(6); m.u16(1); m.zeros(8); m.u16(channels); m.u16(16); m.u16(0); m.u16(0); m.u32(sampleRate.toLong() shl 16)
                m.fullBox("esds", 0, 0) {
                    val cfg = asc!!
                    var bytes = 0L; for (i in 0 until audioCount) bytes += aSize[i]
                    val bitrate = if (mediaDur > 0) (bytes * 8L * sampleRate / mediaDur) else 0L
                    m.u8(0x03); m.u8(3 + 2 + (13 + 2 + cfg.size) + 3); m.u16(2); m.u8(0)
                    m.u8(0x04); m.u8(13 + 2 + cfg.size); m.u8(0x40); m.u8(0x15); m.u8(0); m.u16(0); m.u32(bitrate); m.u32(bitrate)
                    m.u8(0x05); m.u8(cfg.size); m.put(cfg)
                    m.u8(0x06); m.u8(1); m.u8(2)
                }
            }
        }
        runs(m, "stts", audioCount) { i -> aDur[i].toLong() }
        chunkTables(m, 1)
        m.fullBox("stsz", 0, 0) { m.u32(0); m.u32(audioCount); for (i in 0 until audioCount) m.u32(aSize[i]) }
    }

    /** A run-length coded table of (count, value) pairs: `stts` and `ctts`. */
    private inline fun runs(m: Bytes, type: String, n: Int, version: Int = 0, value: (Int) -> Long) {
        var entries = 0
        var i = 0
        while (i < n) { val v = value(i); var j = i + 1; while (j < n && value(j) == v) j++; entries++; i = j }
        m.fullBox(type, version, 0) {
            m.u32(entries)
            i = 0
            while (i < n) { val v = value(i); var j = i + 1; while (j < n && value(j) == v) j++; m.u32(j - i); m.u32(v.toInt()); i = j }
        }
    }

    /** `stsc` (samples per chunk) and `stco` / `co64` (where each chunk starts) of one track. */
    private fun chunkTables(m: Bytes, track: Int) {
        var chunks = 0; var maxOff = 0L
        for (c in 0 until cCount) if (cTrack[c].toInt() == track) { chunks++; if (cOff[c] > maxOff) maxOff = cOff[c] }
        var entries = 0; var last = -1; var idx = 0
        for (c in 0 until cCount) if (cTrack[c].toInt() == track) { idx++; if (cSamples[c] != last) { entries++; last = cSamples[c] } }
        m.fullBox("stsc", 0, 0) {
            m.u32(entries); last = -1; idx = 0
            for (c in 0 until cCount) if (cTrack[c].toInt() == track) { idx++; if (cSamples[c] != last) { m.u32(idx); m.u32(cSamples[c]); m.u32(1); last = cSamples[c] } }
        }
        if (maxOff <= 0xFFFFFFFFL) m.fullBox("stco", 0, 0) { m.u32(chunks); for (c in 0 until cCount) if (cTrack[c].toInt() == track) m.u32(cOff[c]) }
        else m.fullBox("co64", 0, 0) { m.u32(chunks); for (c in 0 until cCount) if (cTrack[c].toInt() == track) m.u64(cOff[c]) }
    }

    companion object {
        /** `ftyp` (32 bytes) + `wide` (8) + the `mdat` header (8): the samples start here. */
        const val HEADER_END = 48L
        const val AAC_FRAME = 1024
        private const val E_V = 1
        private const val E_A = 2
        private const val E_CLOSE = 3
        private const val E_ROLLBACK = 4
        private const val E_CFGV = 5
        private const val E_CFGA = 6
    }
}
