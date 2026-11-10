package app.kestrel.core.url

/**
 * Removes well-known click/campaign identifiers from the QUERY of a navigation URL (Brave "query filter" /
 * Firefox "query parameter stripping"). Conservative on purpose: only parameters whose sole purpose is tracking.
 * Applied to main-frame GET navigations only; never to subresources, forms or API calls.
 */
object TrackingParams {
    private val EXACT: Set<String> = hashSetOf(
        "fbclid", "gclid", "gclsrc", "dclid", "gbraid", "wbraid", "msclkid", "mc_eid", "mc_cid", "igshid", "mibextid",
        "yclid", "ysclid", "twclid", "ttclid", "li_fat_id", "epik", "_ga", "_gl",
        "_hsenc", "_hsmi", "__hstc", "__hssc", "__hsfp", "hsctatracking",
        "mkt_tok", "oly_anon_id", "oly_enc_id", "vero_id", "vero_conv", "_openstat", "s_cid", "rb_clickid",
        "wickedid", "ml_subscriber", "ml_subscriber_hash", "vgo_ee", "__s", "ref_src",
        "oft_c", "oft_ck", "oft_d", "oft_id", "oft_ids", "oft_k", "oft_lk", "oft_sk",
    )
    private val PREFIXES = arrayOf("utm_", "mtm_", "hsa_")

    /** @return the cleaned URL, or null when nothing was removed (callers can then skip any redirect). */
    @JvmStatic
    fun strip(url: String): String? {
        val q = url.indexOf('?')
        if (q < 0) return null
        val hash = url.indexOf('#', q)
        val queryEnd = if (hash >= 0) hash else url.length
        if (q + 1 >= queryEnd) return null

        val kept = StringBuilder()
        var removed = false
        var pos = q + 1
        while (pos <= queryEnd) {
            var amp = url.indexOf('&', pos)
            if (amp < 0 || amp > queryEnd) amp = queryEnd
            if (amp > pos) { // skip empty segments ("a=1&&b=2")
                val eq = url.indexOf('=', pos)
                val keyEnd = if (eq in pos until amp) eq else amp
                if (isTracking(url.substring(pos, keyEnd))) {
                    removed = true
                } else {
                    if (kept.isNotEmpty()) kept.append('&')
                    kept.append(url, pos, amp)
                }
            }
            pos = amp + 1
        }
        if (!removed) return null
        val sb = StringBuilder(url.length)
        sb.append(url, 0, q)
        if (kept.isNotEmpty()) sb.append('?').append(kept)
        if (hash >= 0) sb.append(url, hash, url.length)
        return sb.toString()
    }

    private fun isTracking(rawKey: String): Boolean {
        val key = rawKey.lowercase()
        if (key in EXACT) return true
        for (p in PREFIXES) if (key.startsWith(p)) return true
        return false
    }
}
