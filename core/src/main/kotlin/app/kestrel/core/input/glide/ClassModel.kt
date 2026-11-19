package app.kestrel.core.input.glide

/**
 * R27b (owner 2026-11-19: "learn context and grammar for better word prediction"): a small WORD-CLASS language model. Every common word of the shipped list belongs to one of about 48 classes (found
 * by clustering the words by which words precede them, so the classes are things like "what follows to" = verbs, "what follows the" = nouns and adjectives: grammar without a grammar book). The model
 * knows how often a word of class k follows a word of class j ([trans]) and how likely a word is inside its class ([mass] = the class's total share). Where the exact word pair is not in the pair list
 * (most pairs are not), "the NEXT WORD is of a class that follows the PREVIOUS word's class" still says something. P(word | previous word) ~= trans[class(prev)][class(word)] x share(word) / mass[class(word)].
 * [alpha] scales how much of it is used. Ids are those of the shipped word list ([size] is checked: a different list means no classes). Words without a class (rare ones, the person's own) get nothing from it.
 */
class ClassModel(val classOf: ByteArray, val k: Int, val trans: FloatArray, val mass: FloatArray, var alpha: Float = 1f) {
    val size: Int get() = classOf.size

    fun classOfId(id: Int): Int = if (id < 0 || id >= classOf.size) -1 else { val c = classOf[id].toInt() and 0xFF; if (c >= k) -1 else c }

    /** P(word [id] | the word before it is [prevId]) by classes, 0 when either has no class. [unigram] is the word's own probability. */
    fun prob(prevId: Int, id: Int, unigram: Double): Double {
        val cp = classOfId(prevId); val cw = classOfId(id)
        if (cp < 0 || cw < 0 || mass[cw] <= 0f) return 0.0
        return alpha * trans[cp * k + cw].toDouble() * unigram / mass[cw]
    }

    /** The bytes of the asset file: magic, k, size, classes, transitions, class masses. */
    fun toBytes(): ByteArray {
        val b = java.io.ByteArrayOutputStream(); val o = java.io.DataOutputStream(b)
        o.writeInt(MAGIC); o.writeInt(k); o.writeInt(classOf.size)
        o.write(classOf)
        for (f in trans) o.writeFloat(f)
        for (f in mass) o.writeFloat(f)
        o.flush(); return b.toByteArray()
    }

    companion object {
        const val MAGIC = 0x4B434C53          // "KCLS"
        /** Reads an asset; null when it is damaged or not for a list of [expectedSize] words. */
        fun parse(bytes: ByteArray, expectedSize: Int): ClassModel? = try {
            val i = java.io.DataInputStream(java.io.ByteArrayInputStream(bytes))
            if (i.readInt() != MAGIC) null else {
                val k = i.readInt(); val n = i.readInt()
                if (k !in 1..255 || n != expectedSize) null else {
                    val cls = ByteArray(n); i.readFully(cls)
                    val t = FloatArray(k * k) { i.readFloat() }; val m = FloatArray(k) { i.readFloat() }
                    ClassModel(cls, k, t, m)
                }
            }
        } catch (e: Exception) { null }
    }
}
