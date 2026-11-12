package app.kestrel.core.shield

import app.kestrel.core.url.HttpsPolicy

/** The effective per-page Shields configuration (Brave "Shields" panel). */
data class ShieldSettings(
    val shieldsUp: Boolean = true,
    /** Network ad/tracker blocking. */
    val blockAds: Boolean = true,
    /** Element hiding of ad containers / cookie banners. */
    val cosmetic: Boolean = true,
    val https: HttpsPolicy.Mode = HttpsPolicy.Mode.STRICT,
    val blockThirdPartyCookies: Boolean = true,
    val blockFingerprinting: Boolean = true,
    /** JavaScript off for this site. */
    val blockScripts: Boolean = false,
    /** Kestrel G (EX-10): video and social players embedded in the page (YouTube, Vimeo, Facebook...) wait until they are tapped, so their scripts never run for a page you only read. */
    val waitEmbeds: Boolean = true,
)

/** Individual switches a user can override per site. */
object ShieldFlag {
    const val SHIELDS = 1
    const val ADS = 2
    const val COSMETIC = 4
    const val HTTPS = 8 // value true = upgrade (mode from global, minimum UPGRADE); false = OFF
    const val COOKIES = 16
    const val FINGERPRINT = 32
    const val SCRIPTS = 64
    const val EMBEDS = 128
}

/** A site's explicit choices: bit set in [mask] = overridden; its value is the matching bit in [values]. */
data class SiteShields(val mask: Int, val values: Int) {
    fun applyTo(g: ShieldSettings): ShieldSettings {
        fun on(flag: Int, global: Boolean) = if (mask and flag != 0) (values and flag != 0) else global
        val httpsOn = if (mask and ShieldFlag.HTTPS != 0) (values and ShieldFlag.HTTPS != 0) else g.https != HttpsPolicy.Mode.OFF
        return ShieldSettings(
            shieldsUp = on(ShieldFlag.SHIELDS, g.shieldsUp),
            blockAds = on(ShieldFlag.ADS, g.blockAds),
            cosmetic = on(ShieldFlag.COSMETIC, g.cosmetic),
            https = if (!httpsOn) HttpsPolicy.Mode.OFF else if (g.https == HttpsPolicy.Mode.OFF) HttpsPolicy.Mode.UPGRADE else g.https,
            blockThirdPartyCookies = on(ShieldFlag.COOKIES, g.blockThirdPartyCookies),
            blockFingerprinting = on(ShieldFlag.FINGERPRINT, g.blockFingerprinting),
            blockScripts = on(ShieldFlag.SCRIPTS, g.blockScripts),
            waitEmbeds = on(ShieldFlag.EMBEDS, g.waitEmbeds),
        )
    }

    fun with(flag: Int, value: Boolean): SiteShields =
        SiteShields(mask or flag, if (value) values or flag else values and flag.inv())

    fun without(flag: Int): SiteShields = SiteShields(mask and flag.inv(), values and flag.inv())

    val isDefault: Boolean get() = mask == 0
}

/**
 * "Shields down" for a site = the user chose plain-browser behaviour there: ad/tracker/cookie/fingerprint/script blocking
 * all off. HTTPS-first deliberately stays as configured (it has its own per-site switch): on a carrier hotspot, cleartext is the
 * thing we least want to re-enable just because a page's ads were turned back on.
 */
fun ShieldSettings.effective(): ShieldSettings =
    if (shieldsUp) this else ShieldSettings(
        shieldsUp = false, blockAds = false, cosmetic = false, https = this.https,
        blockThirdPartyCookies = false, blockFingerprinting = false, blockScripts = false, waitEmbeds = false,
    )

/**
 * Per-site overrides keyed by lower-case host (a site setting also applies to its subdomains' pages? No: exact host, like
 * Brave's per-origin Shields). Text format, one line per site: `host<TAB>mask<TAB>values`. Tolerant of damage: bad lines
 * are skipped, the cap protects memory.
 */
class SiteShieldStore(initial: Map<String, SiteShields> = emptyMap()) {
    private val map = LinkedHashMap(initial)

    operator fun get(host: String): SiteShields? = map[host]

    fun set(host: String, s: SiteShields) {
        if (s.isDefault) map.remove(host) else {
            map.remove(host) // keep most-recently-changed last, so the oldest are evicted first
            map[host] = s
            while (map.size > MAX_SITES) map.remove(map.keys.first())
        }
    }

    fun size(): Int = map.size

    /** A copy of every site's choices (Settings > Back up and restore). */
    fun entries(): Map<String, SiteShields> = LinkedHashMap(map)

    fun encode(): String = buildString {
        for ((h, s) in map) append(h).append('\t').append(s.mask).append('\t').append(s.values).append('\n')
    }

    companion object {
        const val MAX_SITES = 2000
        /** Every per-site flag bit that exists (ShieldFlag.SHIELDS .. EMBEDS). The decoder once capped this at 127 and would have dropped a site's Embeds choice as damage. */
        const val ALL_FLAGS = ShieldFlag.SHIELDS or ShieldFlag.ADS or ShieldFlag.COSMETIC or ShieldFlag.HTTPS or ShieldFlag.COOKIES or ShieldFlag.FINGERPRINT or ShieldFlag.SCRIPTS or ShieldFlag.EMBEDS

        fun decode(text: String): SiteShieldStore {
            val out = LinkedHashMap<String, SiteShields>()
            for (line in text.lineSequence()) {
                val p = line.split('\t')
                if (p.size != 3) continue
                val host = p[0]
                if (host.isEmpty() || host.length > 253) continue
                val mask = p[1].toIntOrNull() ?: continue
                val values = p[2].toIntOrNull() ?: continue
                if (mask <= 0 || mask > ALL_FLAGS || values < 0 || values > ALL_FLAGS) continue // only bits that exist: a damaged or hostile line cannot smuggle others in
                if (out.size < MAX_SITES) out[host] = SiteShields(mask, values and mask)
            }
            return SiteShieldStore(out)
        }
    }
}

/** Data-frugal browsing for a metered / flaky hotspot link. */
enum class DataSaver {
    OFF,

    /** Default: tracker blocking, no autoplay, preload=none, save-data signal, confirm big downloads. */
    STANDARD,

    /** Also block images (tap to load) and web fonts. */
    STRICT;

    val blockImages: Boolean get() = this == STRICT
    val blockFonts: Boolean get() = this == STRICT
    val mediaPreloadNone: Boolean get() = this != OFF
    val saveDataSignal: Boolean get() = this != OFF

    /** Ask before downloading files at least this big (or of unknown size) on a metered link; -1 = never ask. */
    fun confirmDownloadBytes(metered: Boolean): Long = when {
        this == OFF || !metered -> -1L
        this == STANDARD -> 20L * 1024 * 1024
        else -> 5L * 1024 * 1024
    }

    companion object {
        fun parse(s: String?): DataSaver = values().firstOrNull { it.name.equals(s, ignoreCase = true) } ?: STANDARD
    }
}
