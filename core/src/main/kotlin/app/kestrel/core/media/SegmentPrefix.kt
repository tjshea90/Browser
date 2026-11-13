package app.kestrel.core.media

/**
 * Some stream servers hide their video parts as pictures: a tiny REAL picture (PNG, GIF or JPEG) is written in front of the transport stream and the part is named ".jpg" / ".png" (a cache or a blocker then
 * takes it for an image). A player skips that header; a saver has to as well, or every part starts with garbage and the joined file does not play.
 * Only the first few KB of a part are looked at ([HEAD]); nothing here reads a whole part.
 */
object SegmentPrefix {
    /** How much of a part's start is enough to find the end of a fake picture header or a sync run. */
    const val HEAD = 4096
    private const val TS_PACKET = 188

    /**
     * The length of a picture at the very start of [b] (PNG up to the end of its IEND chunk, GIF up to its trailer, JPEG up to its first end marker), or 0 when [b] does not start with a picture or the picture
     * does not end inside the first [n] bytes. When [ts] says the part should be a transport stream, the answer is only kept if a transport stream really follows (three sync bytes 188 apart) or there is not
     * enough data left to tell.
     */
    fun imageLength(b: ByteArray, n: Int = b.size, ts: Boolean = true): Int {
        val len = minOf(n, b.size)
        val end = pngEnd(b, len).takeIf { it > 0 } ?: gifEnd(b, len).takeIf { it > 0 } ?: jpegEnd(b, len).takeIf { it > 0 } ?: return 0
        if (ts && end + 2 * TS_PACKET + 1 <= len && !syncAt(b, len, end)) return 0
        return end
    }

    /** [imageLength], and for a transport stream also a run of garbage of any kind: the offset of the first sync byte that is followed by two more 188 bytes apart. 0 = the part starts with its media. */
    fun length(b: ByteArray, n: Int = b.size, ts: Boolean = true): Int {
        val len = minOf(n, b.size)
        val img = imageLength(b, len, ts)
        if (img > 0) return img
        if (!ts) return 0
        if (len > 2 * TS_PACKET && syncAt(b, len, 0)) return 0
        var k = 1
        val last = minOf(len - 2 * TS_PACKET - 1, 2048)
        while (k <= last) { if (syncAt(b, len, k)) return k; k++ }
        return 0
    }

    private fun syncAt(b: ByteArray, len: Int, k: Int): Boolean = k + 2 * TS_PACKET < len && b[k] == 0x47.toByte() && b[k + TS_PACKET] == 0x47.toByte() && b[k + 2 * TS_PACKET] == 0x47.toByte()

    private fun be32(b: ByteArray, o: Int): Long = ((b[o].toLong() and 0xFF) shl 24) or ((b[o + 1].toLong() and 0xFF) shl 16) or ((b[o + 2].toLong() and 0xFF) shl 8) or (b[o + 3].toLong() and 0xFF)

    private fun pngEnd(b: ByteArray, len: Int): Int {
        val sig = intArrayOf(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
        if (len < 20) return 0
        for (i in sig.indices) if ((b[i].toInt() and 0xFF) != sig[i]) return 0
        var pos = 8
        while (pos + 12 <= len) {
            val size = be32(b, pos)
            if (size < 0 || size > len) return 0
            val iend = b[pos + 4] == 'I'.code.toByte() && b[pos + 5] == 'E'.code.toByte() && b[pos + 6] == 'N'.code.toByte() && b[pos + 7] == 'D'.code.toByte()
            val next = pos + 12 + size.toInt()
            if (iend) return if (next <= len) next else 0
            pos = next
        }
        return 0
    }

    private fun gifEnd(b: ByteArray, len: Int): Int {
        if (len < 14 || b[0] != 'G'.code.toByte() || b[1] != 'I'.code.toByte() || b[2] != 'F'.code.toByte() || b[3] != '8'.code.toByte()) return 0
        var pos = 13
        val flags = b[10].toInt() and 0xFF
        if (flags and 0x80 != 0) pos += 3 * (1 shl ((flags and 7) + 1)) // global colour table
        while (pos < len) {
            when (b[pos].toInt() and 0xFF) {
                0x3B -> return pos + 1                                       // trailer
                0x21 -> { pos += 2; pos = skipBlocks(b, len, pos); if (pos < 0) return 0 }  // extension: label, then sub-blocks
                0x2C -> {                                                    // image: descriptor, local colour table, LZW size, sub-blocks
                    if (pos + 10 > len) return 0
                    val lf = b[pos + 9].toInt() and 0xFF
                    pos += 10
                    if (lf and 0x80 != 0) pos += 3 * (1 shl ((lf and 7) + 1))
                    pos += 1
                    pos = skipBlocks(b, len, pos); if (pos < 0) return 0
                }
                else -> return 0
            }
        }
        return 0
    }

    /** From [from] (the first sub-block length byte): past the sub-blocks up to and including the zero-length terminator; -1 if they run past [len]. */
    private fun skipBlocks(b: ByteArray, len: Int, from: Int): Int {
        var pos = from
        while (pos < len) {
            val size = b[pos].toInt() and 0xFF
            pos += 1 + size
            if (size == 0) return pos
        }
        return -1
    }

    private fun jpegEnd(b: ByteArray, len: Int): Int {
        if (len < 8 || b[0] != 0xFF.toByte() || b[1] != 0xD8.toByte() || b[2] != 0xFF.toByte()) return 0
        var i = 2
        while (i + 1 < len) { if (b[i] == 0xFF.toByte() && b[i + 1] == 0xD9.toByte()) return i + 2; i++ }
        return 0
    }
}
