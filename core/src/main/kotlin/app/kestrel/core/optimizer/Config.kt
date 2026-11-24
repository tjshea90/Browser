package app.kestrel.core.optimizer

/**
 * A configuration the optimizer can put the browser into: engine preferences (written into the engine's start-up config, so changing one means a restart) and app settings (Prefs keys, applied at once).
 * The canonical text is one line (`p:name=value;s:key=n;...`, sorted) so two configurations compare as strings and a run can save it.
 */
class Config(val engine: Map<String, String> = emptyMap(), val settings: Map<String, Int> = emptyMap()) {
    fun canonical(): String = (engine.toSortedMap().map { "p:${it.key}=${it.value}" } + settings.toSortedMap().map { "s:${it.key}=${it.value}" }).joinToString(";")
    /** Only the part that needs an engine restart to change. */
    fun enginePart(): String = engine.toSortedMap().map { "p:${it.key}=${it.value}" }.joinToString(";")
    override fun equals(other: Any?) = other is Config && other.canonical() == canonical()
    override fun hashCode() = canonical().hashCode()
    override fun toString() = canonical().ifEmpty { "(no changes)" }

    /**
     * This configuration with [changes] on top. A FLIP takes the opposite of the engine's own value from [defaults] (name to "true" / "false", read from the running engine); with no usable default
     * the experiment cannot be set up and the answer is null.
     */
    fun with(changes: List<Change>, defaults: Map<String, String>): Config? {
        val e = engine.toMutableMap(); val s = settings.toMutableMap()
        for (c in changes) when (c) {
            is Change.Pref -> e[c.name] = c.value
            is Change.Flip -> e[c.name] = when (defaults[c.name]) { "true" -> "false"; "false" -> "true"; else -> return null }
            is Change.Setting -> s[c.prefKey] = c.value
        }
        return Config(e, s)
    }

    companion object {
        fun parse(text: String): Config {
            val e = HashMap<String, String>(); val s = HashMap<String, Int>()
            for (part in text.split(';')) {
                if (part.length < 3) continue
                val eq = part.indexOf('=', 2); if (eq < 0) continue
                val k = part.substring(2, eq); val v = part.substring(eq + 1)
                when {
                    part.startsWith("p:") -> e[k] = v
                    part.startsWith("s:") -> v.toIntOrNull()?.let { s[k] = it }
                }
            }
            return Config(e, s)
        }
    }
}
