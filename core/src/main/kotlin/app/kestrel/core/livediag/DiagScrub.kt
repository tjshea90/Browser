package app.kestrel.core.livediag

/**
 * LD1 (owner 2026-11-21: "a smart live diagnostics engine ... that logs and sends regular diagnostic data to github for claude to review ... only sending website data to the private repo and not logging
 * passwords or payment methods etc ... an off switch that turns all logging completely off"): everything that may leave the tablet passes through here first. Pure, no Android, tested with real-looking lines.
 *
 * Rules, in the order they act on one line:
 *  1. a link is reduced to its HOST (no path, query, fragment, port or user name); a link to a sensitive site (mail, bank, payment, sign-in, account, health) is not named at all;
 *  2. an e-mail address, a card number (13-19 digits that pass the Luhn check), a phone number, a key-shaped string (GitHub / AWS / Google / OpenAI / Slack token, JWT, PEM header, a long opaque token),
 *     `Bearer ...`, and the value after password / token / secret / key / cookie / session / auth is removed;
 *  3. a bare host name (`to=news.example.org`) is kept unless it is a sensitive site;
 *  4. the line is cut at [MAX_LINE] characters, and a line that STILL looks like a secret after all this is dropped whole (null).
 * Nothing here can add information: it only removes or shortens.
 */
object DiagScrub {
    const val MAX_LINE = 400

    private val URL = Regex("""(?i)\b(?:https?|wss?|ftp)://[^\s"'<>)\]},;|]+""")
    private val LOCAL = Regex("""(?i)\b(?:file|content|blob|data|javascript|intent|view-source|moz-extension|resource):[^\s"'<>)\]},;|]*""")
    /** A dotted-quad address (the phone's address on the hotspot, the tablet's own): it names a person's network, so it is never kept. Version numbers have three parts and are not touched. */
    private val IPV4 = Regex("""(?<![\d.])(?:25[0-5]|2[0-4]\d|1?\d?\d)(?:\.(?:25[0-5]|2[0-4]\d|1?\d?\d)){3}(?::\d{1,5})?(?![\d.]*\d)""")
    private val EMAIL = Regex("""[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}""")
    private val PHONE = Regex("""(?<![\d.])(?:\+\d{1,3}[ .-]?)?\(?\d{2,4}\)?[ .-]\d{3,4}[ .-]\d{3,4}(?!\d)""")
    private val CARDISH = Regex("""(?<!\d)\d(?:[ -]?\d){12,18}(?!\d)""")
    private val KEY_VALUE = Regex("""(?i)\b(password|passwd|pwd|passcode|pin|secret|token|api[_-]?key|apikey|auth|authorization|cookie|session|sessionid|sid)\b(\s*[:=]\s*)("[^"]*"|'[^']*'|[^\s,;&]+)""")
    private val BEARER = Regex("""(?i)\bBearer\s+[A-Za-z0-9._~+/=-]{8,}""")
    private val TOKENS = Regex("""\b(?:gh[pousr]_[A-Za-z0-9]{20,}|github_pat_[A-Za-z0-9_]{20,}|AKIA[0-9A-Z]{12,}|AIza[0-9A-Za-z_-]{20,}|sk-[A-Za-z0-9_-]{20,}|xox[abprs]-[A-Za-z0-9-]{10,}|eyJ[A-Za-z0-9_-]{8,}\.[A-Za-z0-9_-]{8,}\.[A-Za-z0-9_-]*)""")
    private val PEM = Regex("""-----BEGIN [A-Z ]+-----""")
    private val OPAQUE = Regex("""(?<![A-Za-z0-9])[A-Za-z0-9+/_=-]{40,}(?![A-Za-z0-9])""")
    private val HOSTISH = Regex("""(?<![A-Za-z0-9@./:-])((?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?\.)+[A-Za-z]{2,24})(?![A-Za-z0-9-])(/[^\s,;)\]}"'<>]*)?""")
    private val CODE_PREFIX = Regex("""^(?:app|android|androidx|java|javax|kotlin|kotlinx|dalvik|libcore|sun|org\.mozilla|org\.json|com\.android|com\.google\.android|mozilla|gecko)\.""", RegexOption.IGNORE_CASE)

    /** The host of an http(s) / ws(s) link without user name, port, path, query or fragment, lower case; null when it has none. */
    fun hostOf(link: String): String? {
        val rest = link.substringAfter("://", "")
        if (rest.isEmpty()) return null
        var h = rest.substringBefore('/').substringBefore('?').substringBefore('#')
        h = h.substringAfterLast('@')
        if (h.startsWith("[")) return null                         // an IPv6 literal: not a site name
        h = h.substringBefore(':').trim().lowercase()
        return h.takeIf { it.isNotEmpty() }
    }

    private fun path(link: String): String = "/" + link.substringAfter("://", "").substringAfter('/', "").substringBefore('#').substringBefore('?')
    private fun query(link: String): String = link.substringAfter('?', "").substringBefore('#').let { if (it.isEmpty()) "" else "?$it" }

    /** A link as it may be told: its host, or [SENSITIVE] for a sensitive site (the host is not named). */
    fun site(link: String): String {
        val h = hostOf(link) ?: return LOCAL_TAG
        return if (SensitiveSites.isSensitive(h, path(link), query(link))) SENSITIVE else h
    }

    const val SENSITIVE = "<sensitive-site>"
    const val LOCAL_TAG = "<local>"

    /** True for a digit string that passes the Luhn check and has the length of a payment card number. */
    fun looksLikeCard(digits: String): Boolean {
        if (digits.length !in 13..19 || digits.any { !it.isDigit() }) return false
        var sum = 0; var alt = false
        for (i in digits.length - 1 downTo 0) {
            var d = digits[i] - '0'
            if (alt) { d *= 2; if (d > 9) d -= 9 }
            sum += d; alt = !alt
        }
        return sum % 10 == 0
    }

    /** The line as it may leave the device, or null when it must not (it still looks like a secret). */
    fun line(raw: String): String? {
        if (raw.isBlank()) return null
        var s = raw.filter { it == '\t' || it >= ' ' }.take(MAX_LINE * 3)
        s = URL.replace(s) { m -> site(m.value) }                                   // first: a link is judged whole (a token in its query makes the site sensitive) before anything is cut out of it
        s = LOCAL.replace(s, LOCAL_TAG)
        s = EMAIL.replace(s, "<email>")
        s = IPV4.replace(s, "<ip>")
        s = PEM.replace(s, "<secret>")
        s = TOKENS.replace(s, "<secret>")
        s = BEARER.replace(s, "Bearer <secret>")
        s = KEY_VALUE.replace(s) { m -> m.groupValues[1] + m.groupValues[2] + "<redacted>" }
        s = CARDISH.replace(s) { m -> if (looksLikeCard(m.value.filter { it.isDigit() })) "<card>" else m.value }
        s = PHONE.replace(s, "<phone>")
        s = HOSTISH.replace(s) { m ->
            val h = m.groupValues[1]
            if (CODE_PREFIX.containsMatchIn(h) || h.all { it.isDigit() || it == '.' }) m.value
            else if (SensitiveSites.isSensitive(h.lowercase())) SENSITIVE else h      // a bare "host/some/path" (no https:// in front, as some app lines write it) keeps the host only
        }
        s = OPAQUE.replace(s, "<secret>")
        if (s.length > MAX_LINE) s = s.take(MAX_LINE) + "..."
        return if (stillRisky(s)) null else s
    }

    /** Last look at the result: anything that still has the shape of a secret, an address or a card makes the whole line stay home. */
    private fun stillRisky(s: String): Boolean =
        TOKENS.containsMatchIn(s) || PEM.containsMatchIn(s) || EMAIL.containsMatchIn(s) || URL.containsMatchIn(s) ||
            CARDISH.findAll(s).any { looksLikeCard(it.value.filter { c -> c.isDigit() }) }
}

/**
 * Sites where the page's own caching rules and a person's privacy both say "do not copy": mail, banking, payment, sign-in, account, health, government and school portals, and well-known finance, health-insurance and mail
 * brands. The same rule as the extension's `bkSensitive` (kestrel-shields/background.js, Instant Back): a shared test corpus keeps the two together. Best effort, not a list of every bank.
 */
object SensitiveSites {
    private val LABELS = Regex("""^(?:mail|webmail|email|bank|banking|login|signin|sign-in|signon|accounts?|myaccount|auth|sso|secure|pay|payments?|checkout|wallet|billing|id|identity|passwords?|vault|admin|portal|health|patient|medical|tax|payroll)$""")
    private val BRANDS = Regex("""^(?:chase|wellsfargo|citi|citibank|capitalone|amex|americanexpress|discover|schwab|fidelity|vanguard|etrade|robinhood|sofi|zelle|revolut|stripe|intuit|turbotax|hrblock|quickbooks|wise|kaiser|anthem|cigna|aetna|unitedhealthcare|uhc|medicare|mychart|gmail|fastmail|zoho|hey|aol|yandex|mailbox|tuta|tutanota|protonmail|proton)$""")
    private val PARTS = Regex("""(?:bank|banc|credit|loan|mortgage|insur|medic|health|patient|clinic|paypal|venmo|crypto|coinbase|binance|proton|tutanota|outlook|hotmail|icloud|cashapp|payroll)""")
    private val PATH = Regex("""(?i)(?:^|/)(?:log-?in|log-?out|log-?on|sign-?in|sign-?out|sign-?up|register|account|accounts|myaccount|checkout|cart|payment|payments|billing|password|passwd|oauth2?|authorize|auth|sso|verify|2fa|otp|callback|settings|profile|admin|wp-admin|inbox|mail|banking|wallet|transfer|security)(?:/|$|\.|\?)""")
    private val QUERY = Regex("""(?i)[?&](?:password|passwd|token|code|otp|access_token|id_token|session|sid|auth)=""")

    fun isSensitive(host: String, path: String = "", query: String = ""): Boolean {
        val h = host.lowercase().trim('.')
        val labels = h.split('.')
        if (labels.any { LABELS.matches(it) || BRANDS.matches(it) }) return true
        val last = labels.last(); val second = if (labels.size > 1) labels[labels.size - 2] else ""
        if (last == "gov" || last == "mil" || last == "edu" || second == "gov" || second == "edu") return true
        if (PARTS.containsMatchIn(h)) return true
        return PATH.containsMatchIn(path) || QUERY.containsMatchIn(query)
    }
}

/** LD1: which Health events may be journalled at all. A whitelist by layer, minus a few events that name what the person watches or reads. */
object DiagPolicy {
    private val LAYERS = setOf(
        "page", "gecko", "mem", "life", "engine", "media", "video", "fullscreen", "inline", "player", "back", "cpu", "start", "update", "sitefixes", "guard", "storage", "scroll", "shields", "cosmetic",
        "tabs", "ui", "filepick", "seekbar", "ads", "sound", "win",
    )
    /** layer + event pairs that carry titles, show names or the like: never kept. */
    private val NEVER = setOf("media name-clues", "media name", "video name", "page title", "tabs title")

    fun allows(layer: String, event: String): Boolean = layer in LAYERS && "$layer $event" !in NEVER && !event.startsWith("title") && !event.contains("name-")
}
