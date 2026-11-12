package app.kestrel.core.media

import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.util.zip.CRC32

/**
 * The append-only record of what a [TsToMp4] has done, kept next to the staged segments so a download that was killed carries on instead of starting over. One block is written per
 * checkpoint (after the output file itself was synced), so every block describes a state the file really reached.
 */
interface MuxJournal {
    /** Appends one block; it is on disk when this returns. */
    fun append(block: ByteArray)
    /** Every intact block, oldest first. A damaged or cut-off tail (power loss mid-write) is dropped and removed, so the next [append] continues cleanly. */
    fun recover(): List<ByteArray>
    /** Forgets everything. */
    fun reset()
}

/** [MuxJournal] in one file. Block = 0xB1, payload length (4 bytes), CRC-32 of the payload (4 bytes), payload. */
class FileMuxJournal(private val file: File) : MuxJournal {
    override fun append(block: ByteArray) {
        val crc = CRC32().also { it.update(block) }.value
        val head = Bytes(9)
        head.u8(MAGIC); head.u32(block.size); head.u32(crc)
        FileOutputStream(file, true).use { o -> o.write(head.a, 0, head.n); o.write(block); o.fd.sync() }
    }

    override fun recover(): List<ByteArray> {
        val out = ArrayList<ByteArray>()
        if (!file.isFile) return out
        val all = file.readBytes()
        var p = 0
        var good = 0
        while (p + 9 <= all.size && (all[p].toInt() and 0xFF) == MAGIC) {
            val len = ((all[p + 1].toLong() and 0xFF) shl 24) or ((all[p + 2].toLong() and 0xFF) shl 16) or ((all[p + 3].toLong() and 0xFF) shl 8) or (all[p + 4].toLong() and 0xFF)
            val crc = ((all[p + 5].toLong() and 0xFF) shl 24) or ((all[p + 6].toLong() and 0xFF) shl 16) or ((all[p + 7].toLong() and 0xFF) shl 8) or (all[p + 8].toLong() and 0xFF)
            if (len < 0 || p + 9 + len > all.size) break
            val c = CRC32().also { it.update(all, p + 9, len.toInt()) }.value
            if (c != crc) break
            out.add(all.copyOfRange(p + 9, p + 9 + len.toInt()))
            p += 9 + len.toInt(); good = p
        }
        if (good < all.size) RandomAccessFile(file, "rw").use { it.setLength(good.toLong()) }
        return out
    }

    override fun reset() { file.delete() }

    private companion object { const val MAGIC = 0xB1 }
}
