package app.kestrel.browser.optimizer

import android.content.Context
import app.kestrel.browser.data.Prefs
import app.kestrel.core.optimizer.RunMachine
import app.kestrel.core.optimizer.Status

/**
 * Called once per process, from GeckoPrefs.writeConfig, BEFORE the engine's start-up config is written (so before the engine exists): the one place that can stop an experiment from killing the app in
 * a loop. It lets the run's state machine look at how the last process ended (core RunMachine.onProcessStart) and then writes the configuration the engine must start with: the one the run asked for
 * on a requested restart, the person's own everywhere else (a closed app, a failed start, a paused or stopped run).
 */
object OptimizerGuard {
    @Volatile private var done = false

    fun beforeEngine(ctx: Context) {
        if (done) return
        done = true
        try {
            val st = OptimizerStore.load(ctx) ?: return
            if (st.status == Status.DONE) return
            val after = RunMachine().onProcessStart(st)
            OptimizerStore.save(ctx, after)
            val cfg = after.bootConfig ?: after.base
            OptimizerApply.write(Prefs.get(ctx), cfg)
            app.kestrel.browser.diag.Health.log("opt", "guard", "status=${after.status} boot=${after.bootStarts} fail=${after.failures} cfg=${cfg.canonical().take(120)}")
        } catch (e: Exception) { app.kestrel.browser.diag.Health.log("opt", "guard-failed", e.toString().take(120)) }
    }
}
