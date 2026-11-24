package app.kestrel.browser.optimizer

import app.kestrel.browser.data.Prefs
import app.kestrel.core.optimizer.Catalog
import app.kestrel.core.optimizer.Config

/** Between a [Config] of the optimizer and the app's real settings. Only names and keys of the catalog can pass: nothing else is ever written. */
object OptimizerApply {
    fun parseEngine(text: String): Map<String, String> {
        val allowed = Catalog.engineNames().toSet(); val out = LinkedHashMap<String, String>()
        for (l in text.lineSequence()) { val i = l.indexOf('='); if (i <= 0) continue; val n = l.substring(0, i).trim(); val v = l.substring(i + 1).trim(); if (n in allowed && v.isNotEmpty() && v.length < 40 && v.all { it.isLetterOrDigit() || it == '.' || it == '-' }) out[n] = v }
        return out
    }
    fun encodeEngine(m: Map<String, String>): String = m.toSortedMap().entries.joinToString("\n") { "${it.key}=${it.value}" }

    /** The person's configuration right now, for every key the catalog can change. */
    fun snapshot(p: Prefs): Config {
        val s = HashMap<String, Int>()
        for (k in Catalog.settingKeys().keys) p.optGet(k)?.let { s[k] = it }
        return Config(parseEngine(p.optPrefs), s)
    }

    /** Puts [c] into the app's settings. Engine preferences are only WRITTEN here (they are read by the engine when it starts); app settings take effect through the callers. */
    fun write(p: Prefs, c: Config) {
        for ((k, v) in c.settings) if (k in Catalog.settingKeys()) p.optSet(k, v)
        p.optPrefs = encodeEngine(c.engine.filterKeys { it in Catalog.engineNames() })
    }
}
