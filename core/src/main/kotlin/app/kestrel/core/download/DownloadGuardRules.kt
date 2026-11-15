package app.kestrel.core.download

/**
 * RX4-X2 (research/REVIEW-EXTERNAL-4.md): a download nobody tapped asks first.
 *
 * Until now only a big, risky (APK, script) or plain-http download asked; an ordinary .zip or .pdf that a page started by itself (a timer that clicks a hidden link, a redirect to a file,
 * a hidden frame) arrived silently, and that is how fake "update your player" installers reach a tablet. A download is "by itself" when nobody touched the screen in the last
 * [TOUCH_WINDOW_MS] and the owner did not just ask for the address (typed it, opened it from another app) and it is not one of the owner's own buttons (the Downloads screen, the video
 * sheet). Such a download asks, with the sentence [BY_ITSELF]; when the owner says Cancel, the same page's next self-started downloads are refused quietly for [REPEAT_WINDOW_MS]
 * (the pop-up dialog guard's rule: a page must not be able to nag). Everything else behaves as before.
 */
object DownloadGuardRules {
    /** A finger that lifted this recently counts as "the person started it" (a slow server answers later than that: one extra question, acceptable). */
    const val TOUCH_WINDOW_MS = 8_000L

    /** After a Cancel on a download the page started itself, more of them from the same tab are refused without a question for this long. */
    const val REPEAT_WINDOW_MS = 60_000L

    const val BY_ITSELF = "This page started the download by itself, without a tap."
    const val BLOB_NOTE = "The page made this file itself; it is not a file from a server."

    enum class Verdict { GO, ASK, REFUSE_QUIETLY }

    /**
     * [touchAgeMs] = time since the last touch of the screen ([Long.MAX_VALUE] = never), [ownerNavigation] = the address was asked for by the owner (typed, opened from another app),
     * [ownRequest] = the owner's own button (the video sheet, the Downloads screen), [declinedAgoMs] = time since the owner said Cancel to a self-started download of this tab.
     */
    class Input(
        val touchAgeMs: Long,
        val ownerNavigation: Boolean,
        val risky: Boolean,
        val big: Boolean,
        val cleartext: Boolean,
        val fromBlob: Boolean = false,
        val ownRequest: Boolean = false,
        val declinedAgoMs: Long = Long.MAX_VALUE,
    )

    fun byItself(i: Input): Boolean = !i.ownRequest && !i.ownerNavigation && !(i.touchAgeMs in 0..TOUCH_WINDOW_MS)

    fun decide(i: Input): Verdict {
        val itself = byItself(i)
        if (itself && i.declinedAgoMs in 0 until REPEAT_WINDOW_MS) return Verdict.REFUSE_QUIETLY
        return if (itself || i.risky || i.big || i.cleartext) Verdict.ASK else Verdict.GO
    }

    /** The sentences that go in front of the file's name in the question ("" = none). */
    fun note(i: Input): String = buildString {
        if (byItself(i)) append(BY_ITSELF)
        if (i.fromBlob) { if (isNotEmpty()) append(' '); append(BLOB_NOTE) }
    }
}
