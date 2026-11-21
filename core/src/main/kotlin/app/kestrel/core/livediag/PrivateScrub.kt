package app.kestrel.core.livediag

/**
 * PV1 (owner 2026-11-21: "Confirm that if I'm using private tabs that no data or information is ever saved at any time or sent anywhere from private tabs. If this is not the case, fix it"): the last line of defence.
 * Most of the app's diagnostic lines already say "private tab" instead of a site (they know their tab). Some cannot: a line fed by the extension that names only a frame's host, a stream server's name, a
 * list the engine reports. So while a private tab exists, EVERY line that goes into the event ring (and from there into the saved last-run notes, the crash note, the Diagnostics text and the live diagnostics) is
 * passed through here first: each web address and each site name becomes `<site>`, each network address `<ip>`, each e-mail address `<email>`. Counts, times and the app's own names (`app.kestrel...`,
 * `org.mozilla...`, `java...`) stay, so the line still says what happened. Pure text; the window it applies to is [PrivateWindow].
 */
object PrivateScrub {
    const val SITE = "<site>"
    private val URL = Regex("""(?i)\b(?:https?|wss?|ftp)://[^\s"'<>)\]},;|]+""")
    private val LOCAL = Regex("""(?i)\b(?:file|content|blob|data|javascript|intent|view-source|moz-extension|resource):[^\s"'<>)\]},;|]*""")
    private val EMAIL = Regex("""[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}""")
    private val IPV4 = Regex("""(?<![\d.])(?:25[0-5]|2[0-4]\d|1?\d?\d)(?:\.(?:25[0-5]|2[0-4]\d|1?\d?\d)){3}(?::\d{1,5})?(?![\d.]*\d)""")
    private val HOSTISH = Regex("""(?<![A-Za-z0-9@./:-])((?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?\.)+[A-Za-z]{2,24})(?![A-Za-z0-9-])(?::\d{1,5})?(/[^\s,;)\]}"'<>]*)?""")
    private val CODE_PREFIX = Regex("""^(?:app|android|androidx|java|javax|kotlin|kotlinx|dalvik|libcore|sun|org\.mozilla|org\.json|com\.android|com\.google\.android|mozilla|gecko)\.""", RegexOption.IGNORE_CASE)

    fun line(raw: String): String {
        if (raw.isEmpty()) return raw
        var s = LOCAL.replace(raw, "<local>")                    // first: a blob:https://site/id is one local address, not a site with a prefix
        s = URL.replace(s, SITE)
        s = EMAIL.replace(s, "<email>")
        s = IPV4.replace(s, "<ip>")
        s = HOSTISH.replace(s) { m -> val h = m.groupValues[1]; if (CODE_PREFIX.containsMatchIn(h) || h.all { it.isDigit() || it == '.' }) m.value else SITE }
        return s
    }
}

/**
 * PV1: when the safety net applies. It applies while a private tab exists and for [graceMs] after the last one went away (a late event of a page that was just closed, a message still on its way). Pure: the caller gives
 * the time. A new private tab opens the window BEFORE it loads anything.
 */
class PrivateWindow(private val graceMs: Long = 120_000L) {
    @Volatile private var open = false
    @Volatile private var closedAt = -1L

    fun setOpen(on: Boolean, now: Long) { if (on) open = true else if (open) { open = false; closedAt = now } }
    fun active(now: Long): Boolean = open || (closedAt >= 0L && now - closedAt < graceMs)
    /** Milliseconds until the window ends when no private tab is open any more; 0 when it is over, -1 while a private tab is still open. */
    fun msLeft(now: Long): Long = if (open) -1L else if (closedAt < 0L) 0L else maxOf(0L, graceMs - (now - closedAt))
}
