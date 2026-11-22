package app.kestrel.core.input

/**
 * The crash-loop guard (plan KB-106, risk R3 "a bug locks the owner out of typing"). The service calls [onStart] when it starts and [healthy] once its keys have been drawn.
 * A start that never became healthy is a crash. Three such starts within a minute put the keyboard in SAFE MODE: the plain keys only, no glide engine, no learning, no word list
 * (nothing that could crash again), so typing always works and the owner can switch back to another keyboard. Pure logic on a tiny [Store]; the clock is injected.
 */
class CrashGuard(private val store: Store, private val now: () -> Long, private val window: Long = 60_000L, private val limit: Int = 3) {
    interface Store { fun read(): String; fun write(text: String) }

    /** Records this start and returns true when the keyboard must run in safe mode. */
    fun onStart(): Boolean {
        val t = now()
        val recent = load().filter { t - it in 0..window } + t
        save(recent.takeLast(MAX_KEPT))
        return recent.size >= limit
    }

    /** The keys were drawn: this start did not crash, so the unhealthy starts are forgotten. */
    fun healthy() { save(emptyList()) }

    private fun load(): List<Long> = try { store.read().split(' ', ',', '\n').mapNotNull { it.trim().toLongOrNull() } } catch (e: Exception) { emptyList() }
    private fun save(list: List<Long>) { try { store.write(list.joinToString(" ")) } catch (e: Exception) { /* a full disk must not break the guard's caller */ } }

    private companion object { const val MAX_KEPT = 8 }
}
