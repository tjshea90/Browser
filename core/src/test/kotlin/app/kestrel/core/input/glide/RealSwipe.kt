package app.kestrel.core.input.glide

import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * R24b: REAL swipes, for the first time. The FUTO Swipe corpus (MIT licence, 1.04 million swipes from thousands of donors, one JSON object per line with the touch points
 * {t, x, y} normalised to 0..1, the canvas size, the target word and its sentence). This is TEST-SIDE tooling only: nothing of it ships, the data is never committed
 * (CLAUDE.md: no replay logs in the repository; the file goes to a build/scratch folder or a runner's temp folder).
 *
 * [fetch] reads the first bytes of the file over HTTPS (a Range request, so a few MB give a few thousand swipes) and keeps whole lines only. [read] parses them.
 */
object RealSwipe {
    const val DEV_URL = "https://huggingface.co/datasets/futo-org/swipe.futo.org/resolve/main/dev.jsonl"

    class Swipe(val word: String, val sentence: String, val wordIdx: Int, val canvasW: Float, val canvasH: Float, val orientation: String, val xs: FloatArray, val ys: FloatArray, val ts: FloatArray)

    /** Downloads about [bytes] from the start of [url] into [to] (whole lines only). Returns the number of bytes kept. */
    fun fetch(url: String, bytes: Long, to: File, offset: Long = 0L): Long {
        to.parentFile?.mkdirs()
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 30_000; c.readTimeout = 60_000; c.instanceFollowRedirects = true
        c.setRequestProperty("Range", "bytes=" + offset + "-" + (offset + bytes - 1))
        c.setRequestProperty("User-Agent", "swoop-eval")
        val code = c.responseCode
        check(code == 200 || code == 206) { "HTTP $code" }
        val buf = java.io.ByteArrayOutputStream()
        c.inputStream.use { i -> val b = ByteArray(65536); while (buf.size() < bytes) { val n = i.read(b); if (n < 0) break; buf.write(b, 0, n) } }
        val all = buf.toByteArray()
        c.disconnect()
        val cut = all.lastIndexOf('\n'.code.toByte())
        check(cut > 0) { "no complete line in $bytes bytes" }
        // from the middle of the file the first line is cut off: start after the first newline (different byte ranges = different donors, for a held-out check)
        val start = if (offset > 0) all.indexOf('\n'.code.toByte()) + 1 else 0
        to.writeBytes(all.copyOfRange(start, cut + 1))
        return (cut + 1 - start).toLong()
    }

    @Suppress("UNCHECKED_CAST")
    fun read(file: File, max: Int = Int.MAX_VALUE): List<Swipe> {
        val out = ArrayList<Swipe>()
        file.bufferedReader().useLines { lines ->
            for (line in lines) {
                if (out.size >= max) break
                if (line.isBlank()) continue
                val o = try { MiniJson(line).parse() as? Map<String, Any?> ?: continue } catch (e: Exception) { continue }
                val data = o["data"] as? List<Any?> ?: continue
                if (data.size < 3) continue
                val xs = FloatArray(data.size); val ys = FloatArray(data.size); val ts = FloatArray(data.size)
                for ((i, p) in data.withIndex()) { val m = p as Map<String, Any?>; xs[i] = (m["x"] as Number).toFloat(); ys[i] = (m["y"] as Number).toFloat(); ts[i] = (m["t"] as Number).toFloat() }
                out.add(Swipe(
                    (o["word"] as? String) ?: continue, (o["sentence"] as? String) ?: "", (o["word_idx"] as? Number)?.toInt() ?: -1,
                    (o["canvas_width"] as? Number)?.toFloat() ?: 0f, (o["canvas_height"] as? Number)?.toFloat() ?: 0f, (o["orientation"] as? String) ?: "", xs, ys, ts
                ))
            }
        }
        return out
    }

    /** The word as the decoder knows it: lower case letters only (apostrophes, hyphens, digits and punctuation dropped). Null when nothing is left. */
    fun letters(word: String): String? { val s = word.lowercase().filter { it in 'a'..'z' }; return s.ifEmpty { null } }
}

/**
 * A tiny JSON reader for the corpus lines (objects, arrays, strings, numbers, true / false / null), kept INSIDE this test file so that the file compiles in every repo that carries a copy
 * of the keyboard's tests (the Kestrel browser has no translate package to borrow one from). Throws on bad input; the caller skips such a line.
 */
private class MiniJson(private val s: String) {
    private var i = 0
    fun parse(): Any? { ws(); val v = value(); ws(); check(i == s.length) { "trailing characters" }; return v }
    private fun ws() { while (i < s.length && s[i].isWhitespace()) i++ }
    private fun value(): Any? {
        ws(); check(i < s.length) { "end" }
        return when (val c = s[i]) {
            '{' -> obj(); '[' -> arr(); '"' -> str()
            't' -> lit("true", true); 'f' -> lit("false", false); 'n' -> lit("null", null)
            else -> { check(c == '-' || c.isDigit()) { "bad value" }; num() }
        }
    }
    private fun lit(w: String, v: Any?): Any? { check(s.startsWith(w, i)) { "bad literal" }; i += w.length; return v }
    private fun num(): Double { val st = i; if (s[i] == '-') i++; while (i < s.length && (s[i].isDigit() || s[i] == '.' || s[i] == 'e' || s[i] == 'E' || s[i] == '+' || s[i] == '-')) i++; return s.substring(st, i).toDouble() }
    private fun str(): String {
        i++; val b = StringBuilder()
        while (true) {
            check(i < s.length) { "unterminated string" }
            val c = s[i++]
            if (c == '"') return b.toString()
            if (c != '\\') { b.append(c); continue }
            check(i < s.length) { "bad escape" }
            when (val e = s[i++]) {
                'n' -> b.append('\n'); 't' -> b.append('\t'); 'r' -> b.append('\r'); 'b' -> b.append('\b'); 'f' -> b.append('\u000c')
                'u' -> { check(i + 4 <= s.length) { "bad unicode" }; b.append(s.substring(i, i + 4).toInt(16).toChar()); i += 4 }
                else -> b.append(e)
            }
        }
    }
    private fun arr(): List<Any?> {
        i++; val out = ArrayList<Any?>(); ws()
        if (s[i] == ']') { i++; return out }
        while (true) { out.add(value()); ws(); if (s[i] == ',') { i++; continue }; check(s[i] == ']') { "bad array" }; i++; return out }
    }
    private fun obj(): Map<String, Any?> {
        i++; val out = LinkedHashMap<String, Any?>(); ws()
        if (s[i] == '}') { i++; return out }
        while (true) { ws(); val k = str(); ws(); check(s[i] == ':') { "bad object" }; i++; out[k] = value(); ws(); if (s[i] == ',') { i++; continue }; check(s[i] == '}') { "bad object end" }; i++; return out }
    }
}
