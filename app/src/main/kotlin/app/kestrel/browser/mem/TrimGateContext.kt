package app.kestrel.browser.mem

import android.content.ComponentCallbacks
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import java.util.WeakHashMap

/**
 * PERF20 (research/RAM_FOR_SPEED.md): a thin Context for `GeckoRuntime.create`. The engine registers its memory handler (GeckoView `MemoryController`, a ComponentCallbacks2) on the context it is created with and
 * the engine uses that context for nothing else but `getApplicationContext()`, which a ContextWrapper hands on unchanged. Registering through this wrapper puts a [Gate] between Android's onTrimMemory and the engine:
 * a signal is passed on only when [Gate.forward] says so. `onLowMemory` (the system's own low-memory call) and configuration changes are always passed on. No reflection, no engine internals: every callback registered
 * through this context is wrapped (the engine registers exactly one). If anything about this fails the engine simply gets every signal, as before.
 */
class TrimGateContext(base: Context, private val gate: Gate) : ContextWrapper(base) {
    interface Gate {
        /** Should the engine hear this trim level? Called on the UI thread. */
        fun forward(level: Int): Boolean
    }

    private class Gated(val inner: ComponentCallbacks, val gate: Gate) : ComponentCallbacks2 {
        override fun onTrimMemory(level: Int) { if (gate.forward(level)) (inner as? ComponentCallbacks2)?.onTrimMemory(level) }
        override fun onConfigurationChanged(newConfig: Configuration) { inner.onConfigurationChanged(newConfig) }
        @Suppress("OVERRIDE_DEPRECATION")
        override fun onLowMemory() { inner.onLowMemory() }
    }

    private val wrapped = WeakHashMap<ComponentCallbacks, Gated>()

    /** The class names of what was registered through this context (for the Diagnostics line: proves the gate is in place). */
    @Volatile var registered: String = "nothing yet"; private set

    override fun registerComponentCallbacks(callback: ComponentCallbacks?) {
        if (callback == null) { super.registerComponentCallbacks(callback); return }
        val g = Gated(callback, gate)
        synchronized(wrapped) { wrapped[callback] = g }
        registered = callback.javaClass.name
        super.registerComponentCallbacks(g)
    }

    override fun unregisterComponentCallbacks(callback: ComponentCallbacks?) {
        val g = if (callback == null) null else synchronized(wrapped) { wrapped.remove(callback) }
        super.unregisterComponentCallbacks(g ?: callback)
    }
}
