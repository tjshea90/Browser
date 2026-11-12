package app.kestrel.core.courier

import app.kestrel.core.download.Clock
import java.net.URL
import java.util.concurrent.Semaphore

/**
 * The tablet's upkeep of "pages through the phone" (research/SPEED2.md section 4): while the owner wants it, ask the phone to keep its page-load proxy open for this tablet (a signed
 * `arm` call, renewed every few minutes), and tell the browser layer through [onState] when pages may use it and when they must not. Everything slow happens on its own thread; the
 * switch, a network change or the app coming back to the front only wake it ([refresh]).
 *
 * The browser layer must treat [State.Active] as "send https traffic to host:port with a direct fallback" and everything else as "go direct": an unreachable phone, a phone with an older
 * Courier (no proxy), a wrong pairing code and a switched-off helper all end in direct loading, never in a failed page.
 */
class ProxyLink(
    private val clientFor: () -> CourierClient?,
    /** The owner's switch: the phone helper is on AND pages are to go through the phone. */
    private val wanted: () -> Boolean,
    private val onState: (State) -> Unit,
    private val clock: Clock = Clock.System,
    private val log: (String) -> Unit = {},
    private val rearmEveryMs: Long = 240_000L,
    private val armTtlSec: Int = 600,
    private val retryMs: Long = 10_000L,
    private val maxRetryMs: Long = 60_000L,
) {
    sealed class State {
        /** Switched off, or nothing to talk to (no pairing): load pages directly. */
        object Off : State()
        /** The phone's proxy is open for this tablet. */
        class Active(val host: String, val port: Int) : State() { override fun toString() = "Active($host:$port)" }
        /** Wanted but not possible right now (the phone does not answer, it is an older Courier, the code does not match): load pages directly; [why] is for the screens. */
        class Unavailable(val why: String) : State() { override fun toString() = "Unavailable($why)" }
    }

    @Volatile var state: State = State.Off
        private set
    /** What the phone's proxy has done so far (fetched with every renewal); null until the first answer or when the phone has no proxy. */
    @Volatile var phoneStats: ProxyStatus? = null
        private set
    @Volatile private var running = false
    private val wake = Semaphore(0)
    private var thread: Thread? = null
    private var armedBefore = false
    private var fails = 0

    @Synchronized fun start() {
        if (running) return
        running = true
        thread = Thread({ loop() }, "proxy-link").also { it.isDaemon = true; it.start() }
    }

    @Synchronized fun stop() {
        running = false; wake.release()
        try { thread?.join(3000) } catch (_: InterruptedException) {}
        thread = null
    }

    /** Look again now (the switch changed, the network changed, the app came back to the front). */
    fun refresh() { fails = 0; wake.release() }

    private fun loop() {
        while (running) {
            val waitMs = step()
            if (!running) break
            try { wake.tryAcquire(waitMs, java.util.concurrent.TimeUnit.MILLISECONDS) } catch (_: InterruptedException) { break }
            wake.drainPermits()
        }
    }

    /** One look at the situation; returns how long to wait before the next one. */
    private fun step(): Long {
        if (!wanted()) {
            if (armedBefore) { armedBefore = false; try { clientFor()?.disarmProxy() } catch (_: Exception) { /* the phone closes it itself after a few minutes */ } }
            set(State.Off)
            return IDLE_MS
        }
        val c = clientFor()
        if (c == null) { set(State.Unavailable("not paired with a phone")); return nextRetry() }
        return try {
            val a = c.armProxy(armTtlSec)
            val host = try { URL(c.baseUrl).host } catch (e: Exception) { "" }
            if (host.isEmpty() || a.port !in 1..65535) { set(State.Unavailable("the phone gave an unusable address")); nextRetry() }
            else {
                armedBefore = true; fails = 0
                try { phoneStats = c.status().proxy } catch (e: Exception) { /* keep the last numbers */ }
                set(State.Active(host, a.port)); rearmEveryMs
            }
        } catch (e: CourierException) {
            when (e.kind) {
                CourierException.Kind.UNREACHABLE, CourierException.Kind.TIMEOUT -> set(State.Unavailable("the phone does not answer"))
                CourierException.Kind.AUTH, CourierException.Kind.IMPOSTOR, CourierException.Kind.SKEW -> set(State.Unavailable("the pairing code does not match"))
                else -> set(State.Unavailable(if (e.status == 501) "the Courier on the phone is too old for this (update it)" else "the phone refused (${e.kind.name.lowercase()})"))
            }
            nextRetry()
        } catch (e: Exception) {
            log("proxy link: ${e.javaClass.simpleName}: ${e.message}")
            set(State.Unavailable("the phone did not answer properly")); nextRetry()
        }
    }

    private fun nextRetry(): Long { fails++; return minOf(maxRetryMs, retryMs * (1L shl minOf(fails - 1, 3))) }

    private fun set(s: State) {
        val old = state
        val same = when {
            old is State.Active && s is State.Active -> old.host == s.host && old.port == s.port
            old is State.Unavailable && s is State.Unavailable -> old.why == s.why
            else -> old === s || (old is State.Off && s is State.Off)
        }
        state = s
        if (!same) { log("proxy link: $s"); try { onState(s) } catch (e: Exception) { log("proxy link listener failed: ${e.message}") } }
    }

    companion object { const val IDLE_MS = 600_000L }
}
