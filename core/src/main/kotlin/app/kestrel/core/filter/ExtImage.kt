package app.kestrel.core.filter

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * "KEX1": the network half of a [FilterSet] plus the Public Suffix List, laid out for the JavaScript engine of the built-in Gecko extension
 * (geckoapp/src/main/assets/kestrel-shields/engine.js) so it can run the SAME matcher without a message to the app per request (research/D1-DESIGN.md).
 *
 * Little-endian, every section 4-byte aligned, so the extension maps each one as a zero-copy `Int32Array`. 64-bit hashes are two ints `[lo, hi]` in
 * the same sorted order as the Kotlin arrays (signed 64-bit order = hi signed, then lo unsigned). Cosmetic and site-flag tables are NOT included:
 * element hiding stays a message per frame, and the app answers "does blocking apply to this page" itself.
 *
 * Layout: `[magic][version][generation][sectionCount]` then `sectionCount x [byteOffset][elementCount]`, then the sections, then a trailer word
 * holding the wrapping sum of every earlier 32-bit word (the extension verifies it once after the transfer).
 * Element count means: bytes for [PAT_BYTES], hashes (pairs of ints) for the hash sections, ints for the rest.
 */
object ExtImage {
    const val MAGIC = 0x3158454B // bytes 'K' 'E' 'X' '1' read as a little-endian int
    const val VERSION = 1

    // section ids (the extension indexes the table with these numbers: never reorder, only append)
    const val HOST_HASH = 0
    const val HOST_FLAGS = 1
    const val EXC_HASH = 2
    const val EXC_FLAGS = 3
    const val PAT_BYTES = 4
    const val PAT_OFF = 5
    const val PAT_META = 6
    const val PAT_DOM = 7
    const val DOM_POOL = 8
    const val TAB_KEY = 9
    const val TAB_START = 10
    const val TAB_LEN = 11
    const val RULE_IDS = 12
    const val GRAM_KEY = 13
    const val GRAM_START = 14
    const val GRAM_LEN = 15
    const val GRAM_IDS = 16
    const val NO_TOKEN = 17
    const val PSL_RULES = 18
    const val PSL_WILD = 19
    const val PSL_EXC = 20
    const val SECTIONS = 21

    private const val HEADER_INTS = 4 + SECTIONS * 2

    /** Chunk size the extension pulls the image in (raw bytes, before base64). */
    const val CHUNK = 256 * 1024

    fun encode(fs: FilterSet, psl: Psl, generation: Int): ByteArray {
        val p = fs.patterns
        val pslTables = psl.tables()
        // (kind, payload): 0 = ints, 1 = longs, 2 = bytes
        val kinds = IntArray(SECTIONS)
        val data = arrayOfNulls<Any>(SECTIONS)
        fun ints(id: Int, a: IntArray) { kinds[id] = 0; data[id] = a }
        fun longs(id: Int, a: LongArray) { kinds[id] = 1; data[id] = a }
        longs(HOST_HASH, fs.hosts.hashes); ints(HOST_FLAGS, fs.hosts.flags)
        longs(EXC_HASH, fs.hostExceptions.hashes); ints(EXC_FLAGS, fs.hostExceptions.flags)
        kinds[PAT_BYTES] = 2; data[PAT_BYTES] = p.patBytes
        ints(PAT_OFF, p.patOff); ints(PAT_META, p.meta); ints(PAT_DOM, p.dom); longs(DOM_POOL, p.domPool)
        ints(TAB_KEY, p.tabKey); ints(TAB_START, p.tabStart); ints(TAB_LEN, p.tabLen); ints(RULE_IDS, p.ruleIds)
        ints(GRAM_KEY, p.gramKey); ints(GRAM_START, p.gramStart); ints(GRAM_LEN, p.gramLen); ints(GRAM_IDS, p.gramRuleIds)
        ints(NO_TOKEN, p.noToken)
        longs(PSL_RULES, pslTables[0]); longs(PSL_WILD, pslTables[1]); longs(PSL_EXC, pslTables[2])

        val counts = IntArray(SECTIONS)
        val byteLen = IntArray(SECTIONS)
        for (id in 0 until SECTIONS) {
            when (kinds[id]) {
                0 -> { counts[id] = (data[id] as IntArray).size; byteLen[id] = counts[id] * 4 }
                1 -> { counts[id] = (data[id] as LongArray).size; byteLen[id] = counts[id] * 8 }
                else -> { counts[id] = (data[id] as ByteArray).size; byteLen[id] = (counts[id] + 3) and 3.inv() }
            }
        }
        var total = HEADER_INTS * 4L
        for (id in 0 until SECTIONS) total += byteLen[id]
        total += 4 // trailer
        require(total < Int.MAX_VALUE) { "image too large" }

        val bb = ByteBuffer.allocate(total.toInt()).order(ByteOrder.LITTLE_ENDIAN)
        bb.putInt(MAGIC); bb.putInt(VERSION); bb.putInt(generation); bb.putInt(SECTIONS)
        var offset = HEADER_INTS * 4
        for (id in 0 until SECTIONS) { bb.putInt(offset); bb.putInt(counts[id]); offset += byteLen[id] }
        for (id in 0 until SECTIONS) {
            when (kinds[id]) {
                0 -> bb.asIntBuffer().also { it.put(data[id] as IntArray) }.also { bb.position(bb.position() + byteLen[id]) }
                1 -> bb.asLongBuffer().also { it.put(data[id] as LongArray) }.also { bb.position(bb.position() + byteLen[id]) }
                else -> { bb.put(data[id] as ByteArray); repeat(byteLen[id] - counts[id]) { bb.put(0) } }
            }
        }
        // trailer: wrapping sum of every earlier word
        var sum = 0
        val words = bb.duplicate().order(ByteOrder.LITTLE_ENDIAN)
        words.position(0)
        val n = (bb.position()) / 4
        for (i in 0 until n) sum += words.getInt(i * 4)
        bb.putInt(sum)
        return bb.array()
    }

    /** The wrapping word sum the trailer must hold; used by tests and by anything that wants to validate an image on the Kotlin side. */
    fun checksum(image: ByteArray): Int {
        val b = ByteBuffer.wrap(image).order(ByteOrder.LITTLE_ENDIAN)
        var sum = 0
        for (i in 0 until (image.size / 4) - 1) sum += b.getInt(i * 4)
        return sum
    }
}
