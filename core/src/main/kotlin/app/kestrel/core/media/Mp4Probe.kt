package app.kestrel.core.media

/**
 * Reads the picture size, length and codec of an MP4 / MOV / M4V from its `moov` box, so a bare file address ("https://cdn.example/v/84721.mp4") can be listed as "720p" without playing it.
 * The caller feeds the first bytes of the file; if the `moov` box is not inside them (files not prepared for streaming keep it at the END, after the big `mdat`), [probe] says where to read
 * next. Pure and tolerant: anything that does not look like an MP4 is [Result.NotMp4].
 */
object Mp4Probe {
    class Info(val width: Int, val height: Int, val durationMs: Long, val videoCodec: String)

    sealed class Result {
        class Found(val info: Info) : Result()
        /** Read [length] bytes from file offset [offset] and call [probe] again with `base = offset`. */
        class NeedMore(val offset: Long, val length: Int) : Result()
        object NotMp4 : Result()
    }

    /** [b] holds file bytes `[base, base + len)`. */
    fun probe(b: ByteArray, len: Int, base: Long = 0): Result {
        var pos = 0
        var first = base == 0L
        while (true) {
            if (pos + 8 > len) return Result.NeedMore(base + pos, 16 * 1024)
            var size = u32(b, pos)
            val type = fourcc(b, pos + 4)
            var header = 8
            if (size == 1L) { if (pos + 16 > len) return Result.NeedMore(base + pos, 16 * 1024); size = u64(b, pos + 8); header = 16 }
            if (first && type !in LEADING) return Result.NotMp4
            first = false
            if (type == "moov") {
                if (size < header || size > MAX_MOOV) return Result.NotMp4
                if (pos + size > len) return Result.NeedMore(base + pos, size.toInt())
                return parseMoov(b, pos + header, pos + size.toInt())?.let { Result.Found(it) } ?: Result.NotMp4
            }
            if (size == 0L || size < header) return Result.NotMp4                    // "to the end of the file" and nothing before it was the moov
            if (pos + size > Int.MAX_VALUE.toLong()) return Result.NeedMore(base + pos + size, 16 * 1024) // a big mdat: the next box is far away
            if (pos + size > len) return Result.NeedMore(base + pos + size, 16 * 1024)
            pos += size.toInt()
        }
    }

    private val LEADING = setOf("ftyp", "moov", "mdat", "free", "skip", "wide", "styp", "pnot")
    private const val MAX_MOOV = 32L * 1024 * 1024

    private fun parseMoov(b: ByteArray, from: Int, to: Int): Info? {
        var timescale = 0L; var duration = 0L
        var width = 0; var height = 0; var codec = ""; var found = false
        forEachBox(b, from, to) { type, s, e ->
            when (type) {
                "mvhd" -> if (e - s >= 20) {
                    val v = b[s].toInt()
                    if (v == 1 && e - s >= 32) { timescale = u32(b, s + 20); duration = u64(b, s + 24) } else { timescale = u32(b, s + 12); duration = u32(b, s + 16) }
                }
                "trak" -> if (!found) {
                    var handler = ""; var w = 0; var h = 0; var c = ""
                    forEachBox(b, s, e) { t2, s2, e2 ->
                        when (t2) {
                            "tkhd" -> if (e2 - s2 >= 8) { w = (u32(b, e2 - 8) ushr 16).toInt(); h = (u32(b, e2 - 4) ushr 16).toInt() }
                            "mdia" -> forEachBox(b, s2, e2) { t3, s3, e3 ->
                                when (t3) {
                                    "hdlr" -> if (e3 - s3 >= 12) handler = fourcc(b, s3 + 8)
                                    "minf" -> forEachBox(b, s3, e3) { t4, s4, e4 ->
                                        if (t4 == "stbl") forEachBox(b, s4, e4) { t5, s5, e5 -> if (t5 == "stsd" && e5 - s5 >= 16) c = fourcc(b, s5 + 12) }
                                    }
                                }
                            }
                        }
                    }
                    if (handler == "vide") { width = w; height = h; codec = name(c); found = true }
                }
            }
        }
        if (!found) return null
        val ms = if (timescale > 0) duration * 1000 / timescale else 0L
        return Info(width, height, ms, codec)
    }

    private fun name(c: String): String = when {
        c.startsWith("avc") -> "H.264"
        c.startsWith("hvc") || c.startsWith("hev") -> "H.265"
        c.startsWith("vp09") -> "VP9"
        c.startsWith("av01") -> "AV1"
        else -> c
    }

    private inline fun forEachBox(b: ByteArray, from: Int, to: Int, visit: (String, Int, Int) -> Unit) {
        var pos = from
        while (pos + 8 <= to) {
            var size = u32(b, pos)
            var header = 8
            if (size == 1L) { if (pos + 16 > to) return; size = u64(b, pos + 8); header = 16 }
            if (size == 0L) size = (to - pos).toLong()
            if (size < header || pos + size > to) return
            visit(fourcc(b, pos + 4), pos + header, pos + size.toInt())
            pos += size.toInt()
        }
    }

    private fun u32(b: ByteArray, i: Int): Long = ((b[i].toLong() and 0xFF) shl 24) or ((b[i + 1].toLong() and 0xFF) shl 16) or ((b[i + 2].toLong() and 0xFF) shl 8) or (b[i + 3].toLong() and 0xFF)
    private fun u64(b: ByteArray, i: Int): Long = (u32(b, i) shl 32) or u32(b, i + 4)
    private fun fourcc(b: ByteArray, i: Int): String = String(charArrayOf((b[i].toInt() and 0xFF).toChar(), (b[i + 1].toInt() and 0xFF).toChar(), (b[i + 2].toInt() and 0xFF).toChar(), (b[i + 3].toInt() and 0xFF).toChar()))
}
