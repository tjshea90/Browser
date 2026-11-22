package app.kestrel.browser

import java.io.InputStream

/**
 * RVW1 P11: all of a stream's bytes with ONE allocation when the stream knows its size (an asset does). `readBytes()` grows a ByteArrayOutputStream and then copies it with toByteArray(), so the peak is twice the
 * size: default.kfs (6.8 MB) held 13.6 MB at once on a 128 MB heap class, en.klx (4 MB) 8 MB. Falls back to the plain way when [available] says nothing, and never loses bytes when it says too little.
 */
fun InputStream.readAllSized(): ByteArray {
    val n = available()
    if (n <= 0) return readBytes()
    val b = ByteArray(n)
    var off = 0
    while (off < n) { val r = read(b, off, n - off); if (r < 0) break; off += r }
    if (off < n) return b.copyOf(off)
    val extra = read()                                   // available() is only a hint: more may follow
    if (extra < 0) return b
    val rest = readBytes()
    val out = ByteArray(n + 1 + rest.size)
    System.arraycopy(b, 0, out, 0, n); out[n] = extra.toByte(); System.arraycopy(rest, 0, out, n + 1, rest.size)
    return out
}

/**
 * RVW3 SC6: at most [max] bytes of a stream whose size nobody promised (an answer over the network). A longer one is an error, so an endless or hostile body can never fill the 128 MB heap
 * ([readBytes] would grow until it did). The release data of GitHub is a few hundred KB at most.
 */
fun InputStream.readCapped(max: Int): ByteArray {
    val out = java.io.ByteArrayOutputStream(minOf(max, 64 * 1024))
    val buf = ByteArray(8192)
    var total = 0
    while (true) {
        val r = read(buf)
        if (r < 0) break
        total += r
        if (total > max) throw java.io.IOException("the answer is larger than $max bytes")
        out.write(buf, 0, r)
    }
    return out.toByteArray()
}
