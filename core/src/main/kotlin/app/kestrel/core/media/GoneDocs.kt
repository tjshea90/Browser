package app.kestrel.core.media

/**
 * RVW2 F19: which player documents went away, and when. A first-bytes probe of a disguised playlist takes seconds; when the document that asked for it was replaced meanwhile (the person switched servers),
 * its answer must not bring the row back. A probe that STARTED after the document went away belongs to the new document with the same address and is welcome. Small and bounded.
 */
class GoneDocs(private val max: Int = 64) {
    private val at = LinkedHashMap<String, Long>()

    fun gone(key: String, now: Long) {
        at.remove(key); at[key] = now
        while (at.size > max) at.remove(at.keys.first())
    }

    /** True when [key] went away at or after [startedAt] (the probe began before the document left). */
    fun goneSince(key: String, startedAt: Long): Boolean = (at[key] ?: return false) >= startedAt
}
