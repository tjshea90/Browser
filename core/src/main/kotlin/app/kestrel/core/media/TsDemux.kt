package app.kestrel.core.media

/**
 * Streaming MPEG-TS demultiplexer for what HLS carries: one picture stream (H.264 is what [Mp4Mux] can store) and optionally one sound stream (AAC in ADTS). Bytes arrive in pieces of any
 * size ([feed]); a packet cut in two by a piece boundary is completed by the next piece. Every complete PES packet of the two chosen streams is handed to [Out] with its time stamps.
 *
 * Tolerant by design: packets with the error flag, duplicates, unknown PIDs and stray bytes are skipped; a PES with a lost packet is dropped whole (one picture is cheaper than a corrupt
 * one). Scrambled streams set [problem] and deliver nothing. Segments end on PES boundaries in every HLS stream that follows the spec, so [endSegment] flushes the open PES of each stream.
 */
class TsDemux(private val out: Out) {
    interface Out {
        /** One PES payload of the picture stream (Annex-B). [pts] / [dts] are 90 kHz ticks, unwrapped past 2^33; [pts] is -1 when the packet carried none, [dts] equals [pts] when it carried only a PTS. The array is reused. */
        fun video(es: ByteArray, len: Int, pts: Long, dts: Long)
        /** One PES payload of the sound stream: one or more ADTS frames; [pts] is the time of the first. The array is reused. */
        fun audio(es: ByteArray, len: Int, pts: Long)
    }

    /** PMT stream type of the chosen picture stream (0x1B = H.264, 0x24 = H.265, ...) or 0 until a PMT has been read / when there is none. */
    var videoType = 0
        private set
    /** PMT stream type of the chosen sound stream (0x0F = AAC in ADTS, 0x81 = AC-3, ...) or 0 when the program has none. */
    var audioType = 0
        private set
    /** True once a PMT has been read (the stream types above are final for this segment). */
    var programKnown = false
        private set
    /** Why the data cannot be demultiplexed ("scrambled"), or null. */
    var problem: String? = null
        private set
    /** Bytes skipped while looking for the next packet boundary (0 for a clean stream). */
    var junkBytes = 0L
        private set

    private var pmtPid = -1
    private var videoPid = -1
    private var audioPid = -1
    private val carry = ByteArray(188)
    private var carryLen = 0
    private val lastCc = IntArray(8192) { -1 }
    private val pat = Section()
    private val pmt = Section()
    private val vid = Pes()
    private val aud = Pes()

    /** Appends bytes of the stream (any length, packets may be cut anywhere). */
    fun feed(b: ByteArray, off: Int, len: Int) {
        var i = off
        val end = off + len
        if (carryLen > 0) {
            val take = minOf(188 - carryLen, end - i)
            System.arraycopy(b, i, carry, carryLen, take)
            carryLen += take; i += take
            if (carryLen < 188) return
            if (carry[0].toInt() == 0x47) packet(carry, 0) else junkBytes += 188
            carryLen = 0
        }
        while (i + 188 <= end) {
            if (b[i].toInt() != 0x47) {
                val j = findSync(b, i, end)
                if (j < 0) { junkBytes += end - i; i = end; break }
                junkBytes += j - i; i = j
                if (i + 188 > end) break
            }
            packet(b, i)
            i += 188
        }
        if (i < end) { System.arraycopy(b, i, carry, 0, end - i); carryLen = end - i }
    }

    /** The segment is over: the pictures and sound frames still being collected are complete (segments end on PES boundaries); a half packet at the end is dropped. */
    fun endSegment() {
        flush(vid); flush(aud)
        carryLen = 0
        java.util.Arrays.fill(lastCc, -1)
    }

    private fun findSync(b: ByteArray, from: Int, end: Int): Int {
        var j = from
        while (j < end) {
            if (b[j].toInt() == 0x47 && (j + 188 >= end || b[j + 188].toInt() == 0x47)) return j
            j++
        }
        return -1
    }

    private fun packet(b: ByteArray, p: Int) {
        val b1 = b[p + 1].toInt() and 0xFF; val b2 = b[p + 2].toInt() and 0xFF; val b3 = b[p + 3].toInt() and 0xFF
        if (b1 and 0x80 != 0) return                                         // transport error indicator
        val pid = ((b1 and 0x1F) shl 8) or b2
        val interesting = pid == 0 || pid == pmtPid || pid == videoPid || pid == audioPid
        if (!interesting) return
        if ((b3 shr 6) and 3 != 0) { problem = "scrambled"; return }
        val afc = (b3 shr 4) and 3
        val cc = b3 and 0xF
        var off = p + 4
        if (afc and 2 != 0) { off += 1 + (b[off].toInt() and 0xFF); if (off > p + 188) return }
        if (afc and 1 == 0) return                                           // no payload
        val last = lastCc[pid]
        if (last == cc) return                                               // a duplicate (allowed once by the standard)
        val lost = last >= 0 && cc != ((last + 1) and 0xF)
        lastCc[pid] = cc
        val pusi = b1 and 0x40 != 0
        val end = p + 188
        when (pid) {
            0 -> section(pat, b, off, end, pusi, lost) { parsePat(pat) }
            pmtPid -> section(pmt, b, off, end, pusi, lost) { parsePmt(pmt) }
            videoPid -> pes(vid, b, off, end, pusi, lost)
            audioPid -> pes(aud, b, off, end, pusi, lost)
        }
    }

    // ---- PSI

    private class Section { var buf = ByteArray(1024); var len = 0; var need = 0; fun reset() { len = 0; need = 0 } }

    private inline fun section(s: Section, b: ByteArray, off: Int, end: Int, pusi: Boolean, lost: Boolean, done: () -> Unit) {
        var o = off
        if (pusi) {
            val pointer = b[o].toInt() and 0xFF
            o += 1 + pointer
            if (o >= end) { s.reset(); return }
            s.reset()
        } else if (s.len == 0 || lost) { s.reset(); return }
        val n = end - o
        if (s.len + n > s.buf.size) s.buf = s.buf.copyOf(maxOf(s.buf.size * 2, s.len + n))
        System.arraycopy(b, o, s.buf, s.len, n); s.len += n
        if (s.need == 0 && s.len >= 3) s.need = 3 + (((s.buf[1].toInt() and 0xF) shl 8) or (s.buf[2].toInt() and 0xFF))
        if (s.need in 1..s.len) { done(); s.reset() }
    }

    private fun parsePat(s: Section) {
        if (s.buf[0].toInt() != 0) return
        var i = 8
        val end = s.need - 4
        while (i + 4 <= end) {
            val program = ((s.buf[i].toInt() and 0xFF) shl 8) or (s.buf[i + 1].toInt() and 0xFF)
            val pid = ((s.buf[i + 2].toInt() and 0x1F) shl 8) or (s.buf[i + 3].toInt() and 0xFF)
            if (program != 0) { pmtPid = pid; return }
            i += 4
        }
    }

    private fun parsePmt(s: Section) {
        if (s.buf[0].toInt() != 2 || s.need < 16) return
        val progInfo = ((s.buf[10].toInt() and 0xF) shl 8) or (s.buf[11].toInt() and 0xFF)
        var i = 12 + progInfo
        val end = s.need - 4
        var vt = 0; var vp = -1; var at = 0; var ap = -1
        while (i + 5 <= end) {
            val type = s.buf[i].toInt() and 0xFF
            val pid = ((s.buf[i + 1].toInt() and 0x1F) shl 8) or (s.buf[i + 2].toInt() and 0xFF)
            val infoLen = ((s.buf[i + 3].toInt() and 0xF) shl 8) or (s.buf[i + 4].toInt() and 0xFF)
            when {
                vp < 0 && isVideo(type) -> { vt = type; vp = pid }
                ap < 0 && isAudio(type) -> { at = type; ap = pid }
                ap < 0 && type == 0x06 && hasDescriptor(s.buf, i + 5, minOf(i + 5 + infoLen, end), 0x6A) -> { at = 0x81; ap = pid }  // AC-3 carried as private data
            }
            i += 5 + infoLen
        }
        if (vp != videoPid) { flush(vid); vid.reset() }
        if (ap != audioPid) { flush(aud); aud.reset() }
        videoPid = vp; audioPid = ap; videoType = vt; audioType = at
        programKnown = true
    }

    private fun hasDescriptor(b: ByteArray, from: Int, to: Int, tag: Int): Boolean {
        var i = from
        while (i + 2 <= to) { if ((b[i].toInt() and 0xFF) == tag) return true; i += 2 + (b[i + 1].toInt() and 0xFF) }
        return false
    }

    private fun isVideo(t: Int) = t == 0x1B || t == 0x24 || t == 0x02 || t == 0x01 || t == 0x10 || t == 0xEA
    private fun isAudio(t: Int) = t == 0x0F || t == 0x11 || t == 0x03 || t == 0x04 || t == 0x81 || t == 0x87 || t == 0x82 || t == 0x8A

    // ---- PES

    private class Pes {
        var buf = ByteArray(64 * 1024)
        var len = 0
        var expected = 0
        var corrupt = false
        var lastRef = -1L
        fun reset() { len = 0; expected = 0; corrupt = false }
    }

    private fun pes(p: Pes, b: ByteArray, off: Int, end: Int, pusi: Boolean, lost: Boolean) {
        if (pusi) {
            flush(p); p.reset()
        } else if (p.len == 0) return                                        // the tail of a packet whose start we never saw
        if (lost) p.corrupt = true
        val n = end - off
        if (p.len + n > p.buf.size) p.buf = p.buf.copyOf(maxOf(p.buf.size * 2, p.len + n))
        System.arraycopy(b, off, p.buf, p.len, n); p.len += n
        if (p.expected == 0 && p.len >= 6) {
            if (p.buf[0].toInt() != 0 || p.buf[1].toInt() != 0 || p.buf[2].toInt() != 1) { p.reset(); return }
            val l = ((p.buf[4].toInt() and 0xFF) shl 8) or (p.buf[5].toInt() and 0xFF)
            p.expected = if (l == 0) -1 else 6 + l
        }
        if (p.expected > 0 && p.len >= p.expected) { flush(p); p.reset() }
    }

    private fun flush(p: Pes) {
        val n = p.len
        val expected = p.expected
        val corrupt = p.corrupt
        if (n < 9) { p.reset(); return }
        val isVideo = p === vid
        if (corrupt || (expected > 0 && n < expected)) { p.reset(); return }  // a lost packet or a cut-off packet: drop it rather than hand over damage
        val b = p.buf
        val flags = b[7].toInt() and 0xFF
        val hdr = b[8].toInt() and 0xFF
        val start = 9 + hdr
        val endData = if (expected > 0) minOf(expected, n) else n
        if (start > endData) { p.reset(); return }
        var pts = -1L; var dts = -1L
        if (flags and 0x80 != 0 && hdr >= 5) pts = timestamp(b, 9)
        if (flags and 0x40 != 0 && hdr >= 10) dts = timestamp(b, 14)
        if (pts >= 0) pts = unwrap(pts, p.lastRef)
        if (dts >= 0) dts = unwrap(dts, p.lastRef)
        if (dts < 0) dts = pts
        p.lastRef = if (dts >= 0) dts else p.lastRef
        if (isVideo) out.video(shiftDown(b, start, endData - start), endData - start, pts, dts)
        else out.audio(shiftDown(b, start, endData - start), endData - start, pts)
        p.reset()
    }

    /** The payload starts at [start] inside the PES buffer: hand over a view that begins at 0 without copying by moving it down (the buffer is reset right after). */
    private fun shiftDown(b: ByteArray, start: Int, len: Int): ByteArray {
        if (start > 0 && len > 0) System.arraycopy(b, start, b, 0, len)
        return b
    }

    private fun timestamp(b: ByteArray, o: Int): Long =
        (((b[o].toLong() shr 1) and 7) shl 30) or ((b[o + 1].toLong() and 0xFF) shl 22) or (((b[o + 2].toLong() and 0xFF) shr 1) shl 15) or ((b[o + 3].toLong() and 0xFF) shl 7) or ((b[o + 4].toLong() and 0xFF) shr 1)

    /** The representative of [raw] (33 bits) that lies closest to [last] (an earlier unwrapped stamp of the same stream), so a stream that runs past 2^33 keeps counting up. */
    private fun unwrap(raw: Long, last: Long): Long {
        if (last < 0) return raw
        val full = 1L shl 33; val half = 1L shl 32
        val base = last - (last and (full - 1))
        var v = base + raw
        if (v - last > half) v -= full else if (last - v > half) v += full
        return v
    }
}
