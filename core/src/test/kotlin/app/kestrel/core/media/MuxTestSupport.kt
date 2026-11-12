package app.kestrel.core.media

import java.io.File

/** An in-memory output with real file semantics: writes at any position (zero-filled gap), truncate. */
class MemOut : MuxOut {
    var data = ByteArray(1 shl 16)
    var size = 0
    var writes = 0
    override fun write(pos: Long, b: ByteArray, off: Int, len: Int) {
        val end = pos.toInt() + len
        if (end > data.size) data = data.copyOf(maxOf(data.size * 2, end))
        System.arraycopy(b, off, data, pos.toInt(), len)
        if (end > size) size = end
        writes++
    }
    override fun truncate(len: Long) { size = len.toInt() }
    fun bytes(): ByteArray = data.copyOf(size)
}

class MemJournal : MuxJournal {
    val blocks = ArrayList<ByteArray>()
    override fun append(block: ByteArray) { blocks.add(block) }
    override fun recover(): List<ByteArray> = ArrayList(blocks)
    override fun reset() { blocks.clear() }
}

object Fixtures {
    fun bytes(name: String): ByteArray = (Fixtures::class.java.getResourceAsStream("/mux/$name") ?: error("missing fixture $name")).use { it.readBytes() }

    val ffmpeg: Boolean by lazy { try { ProcessBuilder("ffmpeg", "-version").redirectErrorStream(true).start().also { it.inputStream.readBytes() }.waitFor() == 0 } catch (e: Exception) { false } }

    /** Decodes [mp4] completely with ffmpeg; returns what ffmpeg printed at error level (empty = clean). */
    fun ffmpegDecode(mp4: ByteArray): String {
        val f = File.createTempFile("mux", ".mp4")
        try {
            f.writeBytes(mp4)
            val p = ProcessBuilder("ffmpeg", "-v", "error", "-i", f.absolutePath, "-f", "null", "-").redirectErrorStream(true).start()
            val out = p.inputStream.readBytes().toString(Charsets.UTF_8)
            p.waitFor()
            return out.trim()
        } finally { f.delete() }
    }

    /** The presentation time (seconds) of the first packet of every stream, by stream index: what a player shows first, edit lists applied. */
    fun firstPacketTimes(mp4: ByteArray): Map<Int, Double> {
        val f = File.createTempFile("mux", ".mp4")
        try {
            f.writeBytes(mp4)
            val p = ProcessBuilder("ffprobe", "-v", "error", "-show_entries", "packet=stream_index,pts_time", "-of", "csv=p=0", f.absolutePath).redirectErrorStream(true).start()
            val out = p.inputStream.readBytes().toString(Charsets.UTF_8)
            p.waitFor()
            val m = LinkedHashMap<Int, Double>()
            for (l in out.lines()) { val c = l.split(','); if (c.size >= 2 && c[0].toIntOrNull() != null && c[1].toDoubleOrNull() != null && c[0].toInt() !in m) m[c[0].toInt()] = c[1].toDouble() }
            return m
        } finally { f.delete() }
    }

    /** `ffprobe` fields of the first stream of [type] ("video" / "audio") as key=value lines. */
    fun ffprobe(mp4: ByteArray, type: String, entries: String): Map<String, String> {
        val f = File.createTempFile("mux", ".mp4")
        try {
            f.writeBytes(mp4)
            val p = ProcessBuilder("ffprobe", "-v", "error", "-select_streams", if (type == "video") "v:0" else "a:0", "-count_frames", "-show_entries", "stream=$entries", "-of", "default=nw=1", f.absolutePath).redirectErrorStream(true).start()
            val out = p.inputStream.readBytes().toString(Charsets.UTF_8)
            p.waitFor()
            return out.lines().filter { it.contains('=') }.associate { it.substringBefore('=') to it.substringAfter('=') }
        } finally { f.delete() }
    }
}

/** An independent reader of MP4 files (it shares no code with the writer): boxes, then every track's samples with offset, size, decode time, composition time and sync flag. */
class Mp4Reader(private val d: ByteArray) {
    class Sample(val offset: Long, val size: Int, val dts: Long, val cts: Long, val sync: Boolean)
    class Edit(val duration: Long, val mediaTime: Long)
    class Track(val id: Int, val type: String, val timescale: Long, val mediaDuration: Long, val tkhdDuration: Long) {
        var samples: List<Sample> = emptyList()
        var edits: List<Edit> = emptyList()
        var entryType = ""
        var width = 0
        var height = 0
        var channels = 0
        var rate = 0L
        var codecConfig: ByteArray = ByteArray(0)
        var hasStss = false
        var chunkCount = 0
        var sumDurations = 0L
    }

    val tracks = ArrayList<Track>()
    var movieTimescale = 0L
    var movieDuration = 0L
    var topLevel = ArrayList<String>()
    var mdatStart = -1L
    var mdatEnd = -1L

    init { top() }

    private fun u8(p: Int) = d[p].toInt() and 0xFF
    private fun u16(p: Int) = (u8(p) shl 8) or u8(p + 1)
    private fun u32(p: Int): Long = (u16(p).toLong() shl 16) or u16(p + 2).toLong()
    private fun i32(p: Int): Long = u32(p).toInt().toLong()
    private fun u64(p: Int): Long = (u32(p) shl 32) or u32(p + 4)
    private fun type(p: Int) = String(d, p, 4, Charsets.ISO_8859_1)

    private class Box(val type: String, val start: Int, val bodyStart: Int, val end: Int)

    private fun boxes(from: Int, to: Int): List<Box> {
        val out = ArrayList<Box>()
        var p = from
        while (p + 8 <= to) {
            var size = u32(p)
            var body = p + 8
            if (size == 1L) { size = u64(p + 8); body = p + 16 } else if (size == 0L) size = (to - p).toLong()
            check(size >= (body - p) && p + size <= to) { "box ${type(p + 4)} at $p has size $size (limit $to)" }
            out.add(Box(type(p + 4), p, body, (p + size).toInt()))
            p += size.toInt()
        }
        check(p == to) { "boxes do not end exactly at $to (end at $p)" }
        return out
    }

    private fun top() {
        for (b in boxes(0, d.size)) {
            topLevel.add(b.type)
            when (b.type) {
                "mdat" -> { mdatStart = b.bodyStart.toLong(); mdatEnd = b.end.toLong() }
                "moov" -> moov(b)
            }
        }
    }

    private fun moov(m: Box) {
        for (b in boxes(m.bodyStart, m.end)) when (b.type) {
            "mvhd" -> { check(u8(b.bodyStart) == 0); movieTimescale = u32(b.bodyStart + 12); movieDuration = u32(b.bodyStart + 16) }
            "trak" -> trak(b)
        }
    }

    private fun trak(t: Box) {
        var id = 0; var tkDur = 0L; var w = 0; var h = 0
        var timescale = 0L; var mdur = 0L; var handler = ""
        var tk: Track? = null
        var edits: List<Edit> = emptyList()
        var stbl: Box? = null
        for (b in boxes(t.bodyStart, t.end)) when (b.type) {
            "tkhd" -> { id = u32(b.bodyStart + 12).toInt(); tkDur = u32(b.bodyStart + 20); w = u16(b.bodyStart + 76); h = u16(b.bodyStart + 80) }
            "edts" -> for (e in boxes(b.bodyStart, b.end)) if (e.type == "elst") {
                val n = u32(e.bodyStart + 4).toInt(); val l = ArrayList<Edit>()
                for (i in 0 until n) { val q = e.bodyStart + 8 + i * 12; l.add(Edit(u32(q), i32(q + 4))) }
                edits = l
            }
            "mdia" -> for (e in boxes(b.bodyStart, b.end)) when (e.type) {
                "mdhd" -> { timescale = u32(e.bodyStart + 12); mdur = u32(e.bodyStart + 16) }
                "hdlr" -> handler = type(e.bodyStart + 8)
                "minf" -> for (f in boxes(e.bodyStart, e.end)) if (f.type == "stbl") stbl = f
            }
        }
        tk = Track(id, handler, timescale, mdur, tkDur)
        tk.edits = edits; tk.width = w shr 0; tk.height = h shr 0
        stbl(tk, stbl!!)
        tracks.add(tk)
    }

    private fun stbl(tk: Track, s: Box) {
        var sizes = IntArray(0); var stts = ArrayList<Pair<Long, Long>>(); var ctts = ArrayList<Pair<Long, Long>>(); var syncs: Set<Int>? = null
        var stsc = ArrayList<Triple<Int, Int, Int>>(); var offs = LongArray(0)
        for (b in boxes(s.bodyStart, s.end)) when (b.type) {
            "stsd" -> {
                val e = boxes(b.bodyStart + 8, b.end)[0]
                tk.entryType = e.type
                if (tk.type == "vide") {
                    tk.width = u16(e.bodyStart + 24); tk.height = u16(e.bodyStart + 26)
                    for (c in boxes(e.bodyStart + 78, e.end)) if (c.type == "avcC") tk.codecConfig = d.copyOfRange(c.bodyStart, c.end)
                } else {
                    tk.channels = u16(e.bodyStart + 16); tk.rate = u32(e.bodyStart + 24) shr 16
                    for (c in boxes(e.bodyStart + 28, e.end)) if (c.type == "esds") tk.codecConfig = d.copyOfRange(c.bodyStart + 4, c.end)
                }
            }
            "stts" -> { val n = u32(b.bodyStart + 4).toInt(); for (i in 0 until n) stts.add(u32(b.bodyStart + 8 + i * 8) to u32(b.bodyStart + 12 + i * 8)) }
            "ctts" -> { val v = u8(b.bodyStart); val n = u32(b.bodyStart + 4).toInt(); for (i in 0 until n) ctts.add(u32(b.bodyStart + 8 + i * 8) to (if (v == 1) i32(b.bodyStart + 12 + i * 8) else u32(b.bodyStart + 12 + i * 8))) }
            "stss" -> { val n = u32(b.bodyStart + 4).toInt(); syncs = (0 until n).map { u32(b.bodyStart + 8 + it * 4).toInt() - 1 }.toSet(); tk.hasStss = true }
            "stsz" -> { val fixed = u32(b.bodyStart + 4).toInt(); val n = u32(b.bodyStart + 8).toInt(); sizes = IntArray(n) { if (fixed != 0) fixed else u32(b.bodyStart + 12 + it * 4).toInt() } }
            "stsc" -> { val n = u32(b.bodyStart + 4).toInt(); for (i in 0 until n) stsc.add(Triple(u32(b.bodyStart + 8 + i * 12).toInt(), u32(b.bodyStart + 12 + i * 12).toInt(), u32(b.bodyStart + 16 + i * 12).toInt())) }
            "stco" -> { val n = u32(b.bodyStart + 4).toInt(); offs = LongArray(n) { u32(b.bodyStart + 8 + it * 4) } }
            "co64" -> { val n = u32(b.bodyStart + 4).toInt(); offs = LongArray(n) { u64(b.bodyStart + 8 + it * 8) } }
        }
        tk.chunkCount = offs.size
        // expand: decode times and composition offsets per sample, offsets per sample from the chunk layout
        val n = sizes.size
        val dts = LongArray(n); var t = 0L; var i = 0
        for ((cnt, delta) in stts) for (k in 0 until cnt.toInt()) { dts[i++] = t; t += delta }
        check(i == n) { "stts covers $i samples, stsz has $n" }
        val cto = LongArray(n); i = 0
        for ((cnt, off) in ctts) for (k in 0 until cnt.toInt()) cto[i++] = off
        if (ctts.isNotEmpty()) check(i == n) { "ctts covers $i samples, stsz has $n" }
        val sampleOff = LongArray(n); var idx = 0
        for (c in offs.indices) {
            val per = stsc.last { it.first <= c + 1 }.second
            var o = offs[c]
            for (k in 0 until per) { if (idx >= n) break; sampleOff[idx] = o; o += sizes[idx]; idx++ }
        }
        check(idx == n) { "chunks cover $idx samples, stsz has $n" }
        val sy = syncs
        tk.samples = (0 until n).map { Sample(sampleOff[it], sizes[it], dts[it], dts[it] + cto[it], sy == null || it in sy) }
        tk.sumDurations = t
    }

    fun track(type: String): Track? = tracks.firstOrNull { it.type == type }
    fun sampleBytes(s: Sample): ByteArray = d.copyOfRange(s.offset.toInt(), s.offset.toInt() + s.size)
}
