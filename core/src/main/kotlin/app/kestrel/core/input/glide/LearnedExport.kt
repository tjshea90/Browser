package app.kestrel.core.input.glide

/**
 * Everything the keyboard learned about THIS person, as one plain-text file they can take with them (AX1, owner message 38: "save this learned data in an exportable format because if the keyboard is
 * good I will make it my system keyboard for all apps and I want to import what it learned"). The format is documented line by line in docs/KEYBOARD-EXPORT.md; this file is its reference
 * implementation, and the unit tests pin it. Pure Kotlin, no Android types: another keyboard built on this core reads the same file.
 *
 * The file is ASCII, one entry per line, version 1:
 *
 *   KESTREL-KEYBOARD-LEARNING 1
 *   # comment lines start with a hash and are ignored
 *   habit <x> <y>              where the thumb lands beside the key it aims at, in key widths (limited to 0.6)
 *   own <word> <times>         a word the shipped list lacks that the person typed or whose site they visited
 *   w <word> <weight>          how much the person used the word
 *   p <before> <word> <weight> how often the word followed the word before it
 *   t <b2> <b1> <word> <weight> ... and the two words before it
 *   c <guessed> <meant> <weight>  the keyboard guessed the first and the person meant the second
 *   r <word> <recency>         how lately the word was used (fades)
 *   s <word> <n> u0 v0 ... u15 v15  the SHAPE of a glide the person drew for the word, 16 points in tenths of a key (u: right of the centre of Q, v: rows below it), n = times confirmed
 *   end <number of entry lines>
 *
 * A reader skips lines it does not know (a later version may add kinds), refuses a file whose first line is not this magic with a version it understands, and bounds everything it keeps.
 */
object LearnedExport {
    const val MAGIC = "KESTREL-KEYBOARD-LEARNING"
    const val VERSION = 1
    /** A file bigger than this is not one of ours (the largest the tables can hold is under 1 MB). */
    const val MAX_CHARS = 3_000_000
    const val MAX_OWN = 800
    const val MAX_BIAS = GlideDecoder.MAX_BIAS

    /** What a file holds, for the dialog that asks whether to import it. */
    class Summary(val words: Int, val pairs: Int, val triples: Int, val corrections: Int, val shapes: Int, val shapeWords: Int, val ownWords: Int, val hasHabit: Boolean, val complete: Boolean) {
        fun describe(): String {
            val parts = ArrayList<String>()
            parts.add("$words words")
            if (pairs > 0) parts.add("$pairs word pairs")
            if (triples > 0) parts.add("$triples three-word phrases")
            if (corrections > 0) parts.add("$corrections corrections")
            if (shapes > 0) parts.add("$shapes glide shapes for $shapeWords words")
            if (ownWords > 0) parts.add("$ownWords own words")
            if (hasHabit) parts.add("your thumb habit")
            return parts.joinToString(", ") + if (complete) "" else " (the file seems cut off; what could be read is used)"
        }
    }

    class Parsed(val model: PersonalModel, val habit: FloatArray?, val ownWords: List<Pair<String, Int>>, val summary: Summary)

    /** The export text for [model], with the thumb [habit] (in key widths) and the person's [own] words. */
    fun write(model: PersonalModel, habit: FloatArray?, own: UserWords?): String {
        val body = model.serialize()
        val sb = StringBuilder(body.length + 512)
        sb.append(MAGIC).append(' ').append(VERSION).append('\n')
        sb.append("# What the Kestrel keyboard learned from your typing: your words, word pairs, corrections and the shapes of your glides.\n")
        sb.append("# It lists words you typed. Keep this file private. Format: docs/KEYBOARD-EXPORT.md in the Kestrel repository.\n")
        var entries = 0
        if (habit != null && habit.size == 2 && !habit[0].isNaN() && !habit[1].isNaN()) { sb.append("habit ").append(PersonalModel.f3(habit[0])).append(' ').append(PersonalModel.f3(habit[1])).append('\n'); entries++ }
        if (own != null) for (line in own.serialize().lineSequence()) { val p = line.trim().split(' '); if (p.size == 2 && p[1].toIntOrNull() != null) { sb.append("own ").append(p[0]).append(' ').append(p[1]).append('\n'); entries++ } }
        for (line in body.lineSequence()) { if (line.isBlank()) continue; sb.append(line).append('\n'); entries++ }
        sb.append("end ").append(entries).append('\n')
        return sb.toString()
    }

    /** Reads an export; null when [text] is not one (wrong first line, a version from the future, too big). Lines it does not know are skipped; numbers must be finite and positive. */
    fun read(text: String): Parsed? {
        if (text.length > MAX_CHARS) return null
        val lines = text.lineSequence().map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }.iterator()
        if (!lines.hasNext()) return null
        val head = lines.next().removePrefix("\uFEFF").split(' ')
        if (head.size != 2 || head[0] != MAGIC) return null
        val ver = head[1].toIntOrNull() ?: return null
        if (ver < 1 || ver > VERSION) return null
        val body = StringBuilder()
        var habit: FloatArray? = null
        val own = ArrayList<Pair<String, Int>>()
        var entries = 0; var declared = -1
        var w = 0; var p = 0; var t = 0; var c = 0; var s = 0
        val shapeWords = HashSet<String>()
        while (lines.hasNext()) {
            val line = lines.next()
            val f = line.split(' ')
            when (f[0]) {
                "habit" -> if (f.size == 3) { val x = f[1].toFloatOrNull(); val y = f[2].toFloatOrNull(); if (x != null && y != null && x.isFinite() && y.isFinite()) { habit = floatArrayOf(x.coerceIn(-MAX_BIAS, MAX_BIAS), y.coerceIn(-MAX_BIAS, MAX_BIAS)); entries++ } }
                "own" -> if (f.size == 3 && own.size < MAX_OWN) { val n = f[2].toIntOrNull(); val word = f[1].lowercase(); if (n != null && n in 1..1000 && UserWords.glideable(word)) { own.add(word to n); entries++ } }
                "w", "p", "t", "c", "r" -> { body.append(line).append('\n'); entries++; when (f[0]) { "w" -> w++; "p" -> p++; "t" -> t++; "c" -> c++ } }
                "s" -> { body.append(line).append('\n'); entries++; s++; if (f.size > 1) shapeWords.add(f[1]) }
                "end" -> { declared = f.getOrNull(1)?.toIntOrNull() ?: -1 }
            }
        }
        val model = PersonalModel.parse(body.toString())
        val sum = Summary(model.wordCount, model.pairCount, model.tripleCount, model.confusionCount, model.patterns.patternCount, model.patterns.wordCount, own.size, habit != null, declared >= 0 && declared == entries)
        return Parsed(model, habit, own, sum)
    }
}
