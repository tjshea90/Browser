package app.kestrel.core.filter

/**
 * Which worker thread of the Gecko bridge answers a native message from the built-in extension (AA3-3b).
 *
 * Gecko holds every request until the reply arrives, so the quick page verdicts must never wait behind slow work, and the first element-hiding answer of a page must never wait behind
 * the engine image that goes out in chunks right after a cold start. The extension always writes the kind first (`{"k":"css",...}`, pinned by web-tests/background.test.js), so the lane
 * is read from the first bytes without parsing the JSON on the thread that receives the message.
 */
object BridgeLane {
    /** Page verdicts, page configuration, counters, media notes. */
    const val QUICK = 0
    /** Element-hiding CSS (`css`) and the lazy batches of generic selectors (`gen`). */
    const val COSMETIC = 1
    /** The engine image chunks (`img`). */
    const val IMAGE = 2

    private const val HEAD = "{\"k\":\""

    fun of(raw: Any?): Int {
        if (raw !is String || !raw.startsWith(HEAD)) return QUICK
        return when {
            raw.startsWith("img\"", HEAD.length) -> IMAGE
            raw.startsWith("css\"", HEAD.length) || raw.startsWith("gen\"", HEAD.length) -> COSMETIC
            else -> QUICK
        }
    }
}
