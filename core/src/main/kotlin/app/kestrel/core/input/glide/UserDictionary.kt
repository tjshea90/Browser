package app.kestrel.core.input.glide

/**
 * The person's own dictionary (owner, 2026-11-17): "a dictionary, which i can add or remove words from. Any added words will be predicted if i glide their pattern. Any removed words will never be
 * predicted." Two lists, both lowercase words, kept in a small private text file:
 *  - [added]: words the keyboard did not know (a name, a place, slang). They join the glide lexicon at the level of a word the person uses a lot, and are offered while typing.
 *  - [blocked]: words that must never be offered again (a rare word such as "tui" that keeps showing, or one deleted with a long press on the strip). Taking a word back off the list ([unblock]) restores it.
 * Adding a blocked word unblocks it; blocking an added word removes it from [added]. Thread safe (the engine thread reads it for every glide; the settings screen edits it).
 */
class UserDictionary {
    private val added = LinkedHashSet<String>()
    private val blocked = LinkedHashSet<String>()

    @Synchronized fun addedWords(): List<String> = added.sorted()
    @Synchronized fun blockedWords(): List<String> = blocked.sorted()
    val blockedCount: Int @Synchronized get() = blocked.size
    val addedCount: Int @Synchronized get() = added.size

    @Synchronized fun isBlocked(word: String): Boolean = blocked.isNotEmpty() && blocked.contains(word.lowercase())
    @Synchronized fun isAdded(word: String): Boolean = added.contains(word.lowercase())

    /** Adds [word]; [Result.OK] or why not. A blocked word is taken off the blocked list by adding it. */
    @Synchronized fun add(word: String): Result {
        val w = norm(word) ?: return Result.NOT_A_WORD
        if (w.length < 2) return Result.TOO_SHORT
        if (w.length > MAX_LEN) return Result.TOO_LONG
        if (added.size >= MAX_WORDS) return Result.FULL
        blocked.remove(w)
        added.add(w)
        return Result.OK
    }

    @Synchronized fun removeAdded(word: String): Boolean = added.remove(word.lowercase())

    /** Never offer [word] again (it is also taken off the added list). */
    @Synchronized fun block(word: String): Result {
        val w = norm(word) ?: return Result.NOT_A_WORD
        if (blocked.size >= MAX_WORDS) return Result.FULL
        added.remove(w)
        blocked.add(w)
        return Result.OK
    }

    @Synchronized fun unblock(word: String): Boolean = blocked.remove(word.lowercase())

    @Synchronized fun clear() { added.clear(); blocked.clear() }

    @Synchronized fun serialize(): String {
        if (added.isEmpty() && blocked.isEmpty()) return ""
        val sb = StringBuilder(HEADER).append('\n')
        for (w in added) sb.append('+').append(w).append('\n')
        for (w in blocked) sb.append('-').append(w).append('\n')
        return sb.toString()
    }

    enum class Result { OK, NOT_A_WORD, TOO_SHORT, TOO_LONG, FULL }

    companion object {
        const val HEADER = "SWOOP-DICTIONARY 1"
        const val MAX_LEN = 24
        const val MAX_WORDS = 5000

        /** The word as stored: trimmed, lowercase, letters a-z only (a finger draws letters); an apostrophe inside is dropped ("don't" is in the shipped list already). Null when nothing usable is left. */
        fun norm(word: String): String? {
            val w = word.trim().lowercase().replace("'", "").replace("’", "")
            return if (w.isNotEmpty() && w.all { it in 'a'..'z' }) w else null
        }

        fun parse(text: String?): UserDictionary {
            val d = UserDictionary()
            if (text == null) return d
            val lines = text.lineSequence().toList()
            if (lines.firstOrNull() != HEADER) return d
            for (l in lines.drop(1)) {
                if (l.length < 2) continue
                val w = norm(l.substring(1)) ?: continue
                if (l[0] == '+') d.added.add(w) else if (l[0] == '-') d.blocked.add(w)
            }
            d.added.removeAll(d.blocked)
            return d
        }
    }
}
