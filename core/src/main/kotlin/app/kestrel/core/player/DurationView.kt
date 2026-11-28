package app.kestrel.core.player

/**
 * VP5 (owner message 49: "the video player first falsely reported that the video was 4 seconds long when it was over one minute"): what length the controls may show.
 *
 * The page's `<video>` says `duration` as it is, and it is wrong early on in two ways: a Media Source stream (a `blob:` address, which is how almost every big site plays) has, until the page sets the
 * whole length, only the length of what has been appended so far (4 s, then 8 s ...), and an ad that plays in the same element has its own short length. So a length is shown only when
 *  - it is a real number (not NaN or 0; infinite, or 2^32 s and more, = a live stream, which shows LIVE and no track),
 *  - it is not smaller than what has been played or buffered (a film cannot be shorter than that: the number is still growing),
 *  - and, for a Media Source stream, it has stayed the same for [PlayerRules.STABLE_MS].
 * Until then only the position shows (no end time, no track to drag). A plain file (`https://...mp4`) knows its length at once and is believed at once.
 */
class DurationView {
    enum class Kind { KNOWN, UNKNOWN, LIVE }
    class Shown(val kind: Kind, val seconds: Double)

    private var lastD = Double.NaN
    private var changedAt = 0L

    /** [d] = the element's duration as the page reports it, [t] = the position, [buffered] = the end of the buffered range, [mse] = a Media Source stream, [live] = the page says the stream is live. */
    fun feed(d: Double, t: Double, buffered: Double, mse: Boolean, live: Boolean, now: Long): Shown {
        if (live || d.isInfinite() || d >= PlayerRules.LIVE_SECONDS) { lastD = Double.NaN; return Shown(Kind.LIVE, 0.0) }
        if (d.isNaN() || d <= 0.0) { lastD = Double.NaN; return Shown(Kind.UNKNOWN, 0.0) }
        if (d != lastD) { lastD = d; changedAt = now }
        val floor = maxOf(t, buffered)
        if (d + PlayerRules.FLOOR_SLACK_S < floor) return Shown(Kind.UNKNOWN, 0.0)
        if (mse && now - changedAt < PlayerRules.STABLE_MS) return Shown(Kind.UNKNOWN, 0.0)
        return Shown(Kind.KNOWN, d)
    }

    /** A new film (the element was emptied or its source changed): nothing is believed yet. */
    fun reset() { lastD = Double.NaN; changedAt = 0L }
}
