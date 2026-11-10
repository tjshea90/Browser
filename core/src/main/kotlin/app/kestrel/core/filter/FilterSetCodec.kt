package app.kestrel.core.filter

import app.kestrel.core.Kestrel
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.nio.ByteBuffer
import java.util.zip.CRC32

/**
 * Binary `.kfs` format for a compiled [FilterSet]. Loading is a handful of bulk array copies (no parsing), so a cold start
 * on the tablet costs milliseconds, not the seconds that re-parsing 150k list lines would.
 *
 * Layout: magic "KFS1" | version | createdAt | list names | counters | arrays... | CRC32 (8 bytes) of everything before it.
 * [read] verifies the CRC AND the structural invariants the matcher relies on, so a damaged file can never produce an
 * out-of-bounds read later on a WebView thread — it fails here, and the caller falls back to the previous/bundled set.
 */
object FilterSetCodec {
    private const val MAGIC = 0x4B465331 // "KFS1"

    fun write(fs: FilterSet): ByteArray {
        val bos = ByteArrayOutputStream(1 shl 20)
        val d = DataOutputStream(bos)
        d.writeInt(MAGIC)
        d.writeInt(Kestrel.FILTER_FORMAT_VERSION)
        d.writeLong(fs.meta.createdAtMillis)
        d.writeInt(fs.meta.lists.size)
        for (n in fs.meta.lists) d.writeUTF(n)
        d.writeInt(fs.meta.cosmeticSite)

        longs(d, fs.hosts.hashes); ints(d, fs.hosts.flags)
        longs(d, fs.hostExceptions.hashes); ints(d, fs.hostExceptions.flags)
        longs(d, fs.siteHash); ints(d, fs.siteFlags)

        val p = fs.patterns
        d.writeInt(p.count)
        bytes(d, p.patBytes); ints(d, p.patOff); ints(d, p.meta); ints(d, p.dom); longs(d, p.domPool)
        ints(d, p.tabKey); ints(d, p.tabStart); ints(d, p.tabLen); ints(d, p.ruleIds)
        ints(d, p.gramKey); ints(d, p.gramStart); ints(d, p.gramLen); ints(d, p.gramRuleIds); ints(d, p.noToken)

        val c = fs.cosmetic
        longs(d, c.siteHash); ints(d, c.siteOff); bytes(d, c.sitePool)
        longs(d, c.excHash); ints(d, c.excOff); bytes(d, c.excPool)
        bytes(d, c.genericPool); d.writeInt(c.genericCount); bytes(d, c.genericExcPool)
        d.flush()

        val body = bos.toByteArray()
        val crc = CRC32().also { it.update(body, 0, body.size) }.value
        val out = ByteBuffer.allocate(body.size + 8)
        out.put(body); out.putLong(crc)
        return out.array()
    }

    /** @throws IllegalArgumentException if the data is truncated, corrupt or from another format version */
    fun read(data: ByteArray): FilterSet {
        require(data.size > 16) { "too short" }
        val crcStored = ByteBuffer.wrap(data, data.size - 8, 8).long
        val crc = CRC32().also { it.update(data, 0, data.size - 8) }.value
        require(crc == crcStored) { "checksum mismatch" }

        val b = ByteBuffer.wrap(data, 0, data.size - 8)
        require(b.int == MAGIC) { "bad magic" }
        require(b.int == Kestrel.FILTER_FORMAT_VERSION) { "unsupported format version" }
        val created = b.long
        val nLists = b.int
        require(nLists in 0..1000) { "bad list count" }
        val names = ArrayList<String>(nLists)
        repeat(nLists) { names.add(utf(b)) }
        val cosmeticSite = b.int

        val hosts = HostTable(rLongs(b), rInts(b))
        val excs = HostTable(rLongs(b), rInts(b))
        val siteHash = rLongs(b); val siteFlags = rInts(b)

        val count = b.int
        val pat = PatternTable(
            count, rBytes(b), rInts(b), rInts(b), rInts(b), rLongs(b),
            rInts(b), rInts(b), rInts(b), rInts(b),
            rInts(b), rInts(b), rInts(b), rInts(b), rInts(b),
        )
        val cos = CosmeticIndex(
            rLongs(b), rInts(b), rBytes(b), rLongs(b), rInts(b), rBytes(b), rBytes(b), b.int, rBytes(b),
        )
        validate(hosts, excs, siteHash, siteFlags, pat, cos)
        val meta = FilterMeta(created, names, hosts.hashes.size, excs.hashes.size, pat.count, siteHash.size, cosmeticSite, cos.genericCount)
        return FilterSet(hosts, excs, siteHash, siteFlags, pat, cos, meta)
    }

    // ------------------------------------------------------------------ validation

    private fun validate(h: HostTable, e: HostTable, sh: LongArray, sf: IntArray, p: PatternTable, c: CosmeticIndex) {
        require(h.hashes.size == h.flags.size && e.hashes.size == e.flags.size && sh.size == sf.size) { "table size mismatch" }
        requireSorted(h.hashes); requireSorted(e.hashes); requireSorted(sh); requireSorted(c.siteHash); requireSorted(c.excHash)
        require(p.count >= 0 && p.patOff.size == p.count + 1 && p.meta.size == p.count && p.dom.size == p.count) { "pattern arrays" }
        require(p.patOff[0] == 0 && p.patOff[p.count] == p.patBytes.size) { "pattern offsets" }
        for (i in 0 until p.count) require(p.patOff[i] <= p.patOff[i + 1]) { "pattern offsets not monotonic" }
        validateIndex(p.tabKey, p.tabStart, p.tabLen, p.ruleIds, p.count)
        validateIndex(p.gramKey, p.gramStart, p.gramLen, p.gramRuleIds, p.count)
        for (id in p.noToken) require(id in 0 until p.count) { "no-token id" }
        for (i in 0 until p.count) {
            val d = p.dom[i]
            if (d == 0) continue
            val off = d - 1
            require(off >= 0 && off + 2 <= p.domPool.size) { "domain offset" }
            val ni = p.domPool[off]; val ne = p.domPool[off + 1]
            require(ni >= 0 && ne >= 0 && off + 2 + ni + ne <= p.domPool.size) { "domain counts" }
        }
        validateGroups(c.siteHash.size, c.siteOff, c.sitePool.size)
        validateGroups(c.excHash.size, c.excOff, c.excPool.size)
        require(c.genericCount >= 0) { "generic count" }
    }

    /** The probe loop only terminates if the table has an empty slot; bucket ranges and ids must be in bounds. */
    private fun validateIndex(key: IntArray, start: IntArray, len: IntArray, ids: IntArray, count: Int) {
        val cap = key.size
        require(cap >= 16 && (cap and (cap - 1)) == 0 && start.size == cap && len.size == cap) { "index table shape" }
        var anyEmpty = false
        for (i in 0 until cap) {
            val l = len[i]
            if (l == 0) { anyEmpty = true; continue }
            require(l > 0 && start[i] >= 0 && start[i].toLong() + l <= ids.size) { "bucket range" }
        }
        require(anyEmpty) { "index table has no empty slot (probe would not terminate)" }
        for (id in ids) require(id in 0 until count) { "rule id out of range" }
    }

    private fun validateGroups(n: Int, offs: IntArray, poolSize: Int) {
        require(offs.size == n + 1 && offs[0] == 0 && offs[n] == poolSize) { "cosmetic offsets" }
        for (i in 0 until n) require(offs[i] <= offs[i + 1]) { "cosmetic offsets not monotonic" }
    }

    private fun requireSorted(a: LongArray) {
        for (i in 1 until a.size) require(a[i - 1] <= a[i]) { "table not sorted" }
    }

    // ------------------------------------------------------------------ primitives

    private fun longs(d: DataOutputStream, a: LongArray) {
        d.writeInt(a.size)
        val bb = ByteBuffer.allocate(a.size * 8)
        bb.asLongBuffer().put(a)
        d.write(bb.array())
    }

    private fun ints(d: DataOutputStream, a: IntArray) {
        d.writeInt(a.size)
        val bb = ByteBuffer.allocate(a.size * 4)
        bb.asIntBuffer().put(a)
        d.write(bb.array())
    }

    private fun bytes(d: DataOutputStream, a: ByteArray) {
        d.writeInt(a.size)
        d.write(a)
    }

    private fun rLongs(b: ByteBuffer): LongArray {
        val n = b.int
        require(n >= 0 && n.toLong() * 8 <= b.remaining()) { "bad long array length" }
        val a = LongArray(n)
        b.asLongBuffer().get(a)
        b.position(b.position() + n * 8)
        return a
    }

    private fun rInts(b: ByteBuffer): IntArray {
        val n = b.int
        require(n >= 0 && n.toLong() * 4 <= b.remaining()) { "bad int array length" }
        val a = IntArray(n)
        b.asIntBuffer().get(a)
        b.position(b.position() + n * 4)
        return a
    }

    private fun rBytes(b: ByteBuffer): ByteArray {
        val n = b.int
        require(n >= 0 && n <= b.remaining()) { "bad byte array length" }
        val a = ByteArray(n)
        b.get(a)
        return a
    }

    private fun utf(b: ByteBuffer): String {
        val n = b.short.toInt() and 0xFFFF
        require(n <= b.remaining()) { "bad string length" }
        val a = ByteArray(n)
        b.get(a)
        return String(a, Charsets.UTF_8)
    }
}
