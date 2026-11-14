package app.kestrel.tools

import app.kestrel.core.input.glide.LexiconFile
import java.io.File
import kotlin.system.exitProcess

/**
 * Compiles the glide-typing word list (AT1; the sources, the licence and the steps are in docs/DICTIONARY.md):
 *
 *   gradlew :tools:glideLexicon --args="--out en.klx --words frequency_dictionary_en_82_765.txt --pairs frequency_bigramdictionary_en_243_342.txt --extra tools/glide/extra-words.txt"
 *
 *   --words  "word count" per line      --pairs  "first second count" per line
 *   --extra  "word count" per line; a word already known gets AT LEAST that count, an unknown one is added (web names, modern words, the apostrophe words)
 *   --min-count N / --max-words N  drop the rare tail
 *
 * The inputs are untrusted text: they are parsed, never executed.
 */
fun main(args: Array<String>) {
    var out: String? = null; var words: String? = null; var pairs: String? = null; val extras = ArrayList<String>()
    var minCount = 0L; var maxWords = Int.MAX_VALUE
    var i = 0
    while (i < args.size) {
        when (args[i]) {
            "--out" -> out = args[++i]
            "--words" -> words = args[++i]
            "--pairs" -> pairs = args[++i]
            "--extra" -> extras.add(args[++i])
            "--min-count" -> minCount = args[++i].toLong()
            "--max-words" -> maxWords = args[++i].toInt()
            else -> { System.err.println("unknown argument ${args[i]}"); exitProcess(2) }
        }
        i++
    }
    if (out == null || words == null) { System.err.println("usage: --out FILE --words FILE [--pairs FILE] [--extra FILE]... [--min-count N] [--max-words N]"); exitProcess(2) }

    val counts = HashMap<String, Long>()
    fun eachLine(f: String, parts: Int, block: (List<String>) -> Unit) {
        for (raw in File(f).readLines(Charsets.UTF_8)) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            val p = line.split(' ', '\t').filter { it.isNotEmpty() }
            if (p.size == parts) block(p)
        }
    }
    eachLine(words, 2) { p -> val c = p[1].toLongOrNull() ?: return@eachLine; if (c >= minCount && ok(p[0])) counts[p[0]] = maxOf(c, counts[p[0]] ?: 0L) }
    for (x in extras) eachLine(x, 2) { p -> val c = p[1].toLongOrNull() ?: return@eachLine; if (ok(p[0])) counts[p[0]] = maxOf(c, counts[p[0]] ?: 0L) }
    var entries = counts.map { LexiconFile.Entry(it.key, it.value) }.sortedByDescending { it.count }
    if (entries.size > maxWords) entries = entries.take(maxWords)
    val known = entries.map { it.text }.toHashSet()
    val ps = ArrayList<LexiconFile.Pair>()
    if (pairs != null) eachLine(pairs, 3) { p -> val c = p[2].toLongOrNull() ?: return@eachLine; if (p[0] in known && p[1] in known) ps.add(LexiconFile.Pair(p[0], p[1], c)) }

    val bytes = LexiconFile.build(entries, ps)
    File(out).parentFile?.mkdirs()
    File(out).writeBytes(bytes)
    println("glide word list: ${entries.size} words, ${ps.size} pairs, ${bytes.size} bytes -> $out")
}

/** A word is lowercase letters, with at most the apostrophes of a contraction, and short enough to glide. */
private fun ok(w: String): Boolean {
    if (w.isEmpty() || w.length > 26) return false
    var letters = 0
    for (c in w) { if (c in 'a'..'z') letters++ else if (c != '\'') return false }
    return letters in 1..24 && !w.startsWith("'") && !w.endsWith("'")
}
