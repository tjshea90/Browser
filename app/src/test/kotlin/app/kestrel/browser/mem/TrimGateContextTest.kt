package app.kestrel.browser.mem

import android.app.Application
import android.content.ComponentCallbacks2
import android.content.res.Configuration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** PERF20: what registers through the gate hears only the signals the gate lets pass; the system's own low-memory call and configuration changes always pass; unregistering works. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class TrimGateContextTest {
    private val app: Application get() = RuntimeEnvironment.getApplication()

    private class Engine : ComponentCallbacks2 {
        val trims = ArrayList<Int>(); var low = 0; var config = 0
        override fun onTrimMemory(level: Int) { trims.add(level) }
        override fun onLowMemory() { low++ }
        override fun onConfigurationChanged(newConfig: Configuration) { config++ }
    }

    @Test fun onlyTheSignalsTheGateLetsThroughReachTheEngine() {
        val ctx = TrimGateContext(app, object : TrimGateContext.Gate { override fun forward(level: Int) = level != 80 && level != 15 })
        val engine = Engine()
        ctx.registerComponentCallbacks(engine)
        app.onTrimMemory(80); app.onTrimMemory(15); app.onTrimMemory(40)
        assertEquals("only the level the gate passes", listOf(40), engine.trims)
        assertTrue(ctx.registered.endsWith("Engine"))
        ctx.unregisterComponentCallbacks(engine)
        app.onTrimMemory(40)
        assertEquals("nothing after unregistering", listOf(40), engine.trims)
    }

    @Test fun theSystemsOwnLowMemoryCallAndConfigurationChangesAlwaysPass() {
        val ctx = TrimGateContext(app, object : TrimGateContext.Gate { override fun forward(level: Int) = false })
        val engine = Engine()
        ctx.registerComponentCallbacks(engine)
        app.onLowMemory()
        app.onConfigurationChanged(Configuration())
        app.onTrimMemory(80)
        assertEquals(1, engine.low); assertEquals(1, engine.config); assertEquals(emptyList<Int>(), engine.trims)
        ctx.unregisterComponentCallbacks(engine)
    }

    @Test fun theApplicationContextIsTheRealOneAndOtherCallbacksAreWrappedToo() {
        val ctx = TrimGateContext(app, object : TrimGateContext.Gate { override fun forward(level: Int) = true })
        assertTrue("the engine finds the real Application through the wrapper", ctx.applicationContext === app)
        val plain = object : android.content.ComponentCallbacks {
            var n = 0
            override fun onConfigurationChanged(newConfig: Configuration) { n++ }
            @Suppress("OVERRIDE_DEPRECATION") override fun onLowMemory() {}
        }
        ctx.registerComponentCallbacks(plain)
        app.onConfigurationChanged(Configuration())
        assertEquals(1, plain.n)
        ctx.unregisterComponentCallbacks(plain)
    }
}
