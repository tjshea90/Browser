package app.kestrel.browser.optimizer

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Process

/**
 * The trampoline of a restart (its own process, `:restart`): the old main process asks the system to start this activity and ends itself; this one makes sure the old process is gone, starts the real target
 * (Auto-tune's screen) as a fresh task and ends. The system lets an app start an activity from a visible window, and this activity has one, so no background-start rule gets in the way.
 */
class RestartActivity : Activity() {
    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val oldPid = intent.getIntExtra("pid", -1)
        val target = intent.getParcelableExtra<Intent>("target")
        if (oldPid > 0 && oldPid != Process.myPid()) try { Process.killProcess(oldPid) } catch (e: Exception) { /* already gone */ }
        try { Thread.sleep(300) } catch (e: InterruptedException) { /* go on */ }
        if (target != null) { target.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK); try { startActivity(target) } catch (e: Exception) { /* the notification fallback is still there */ } }
        finish()
        Process.killProcess(Process.myPid())
    }
}
