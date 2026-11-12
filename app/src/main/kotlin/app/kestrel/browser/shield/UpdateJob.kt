package app.kestrel.browser.shield

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import app.kestrel.browser.data.Prefs

/** Periodic, battery-friendly filter refresh. Runs in the regular process only (the private window never does network housekeeping). */
class UpdateJob : JobService() {
    override fun onStartJob(params: JobParameters): Boolean {
        Thread({
            val r = FilterUpdater.run(applicationContext, manual = false)
            jobFinished(params, r is FilterUpdater.Result.Failed) // retry later (with the system's back-off) only after a failure
        }, "kestrel-update").apply { priority = Thread.MIN_PRIORITY; isDaemon = true }.start()
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        FilterUpdater.cancel()
        return true
    }

    companion object {
        private const val JOB_ID = 4711
        private const val DAY = 24L * 60 * 60 * 1000

        /** Idempotent: (re)schedules only when the user's policy changed. Call off the main thread. */
        fun schedule(ctx: Context) {
            val js = ctx.getSystemService(JobScheduler::class.java) ?: return
            val prefs = Prefs.get(ctx)
            if (prefs.updatePolicy == 2) { js.cancel(JOB_ID); return }
            val period = if (prefs.updatePolicy == 1) 7 * DAY else DAY // 0 = every day (owner request X2/X5), 1 = every week, 2 = only by hand
            val network = if (prefs.updateUnmeteredOnly) JobInfo.NETWORK_TYPE_UNMETERED else JobInfo.NETWORK_TYPE_ANY
            val sig = "$period/$network"
            if (js.getPendingJob(JOB_ID) != null && prefs.updateJobSig == sig) return // already scheduled with this policy
            val info = JobInfo.Builder(JOB_ID, ComponentName(ctx, UpdateJob::class.java))
                .setPeriodic(period)
                .setRequiredNetworkType(network)
                .setRequiresBatteryNotLow(true)
                .build()
            js.schedule(info)
            prefs.updateJobSig = sig
        }
    }
}
