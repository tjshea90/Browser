# Applies the OPT1 integration edits to the repo (run from /home/user/Browser after the ship gate has finished) and copies the new sources in.
import os, shutil, sys
R = '/home/user/Browser'
S = os.path.join(R, 'docs/handoff/opt1-staging')

def sub(p, old, new, count=1):
    p = os.path.join(R, p)
    s = open(p, encoding='utf-8').read()
    assert s.count(old) == count, (p, old[:70], s.count(old))
    open(p, 'w', encoding='utf-8').write(s.replace(old, new))

# ---- new files
os.makedirs(os.path.join(R, 'geckoapp/src/main/kotlin/app/kestrel/browser/optimizer'), exist_ok=True)
for f in os.listdir(S + '/gecko'):
    shutil.copy(os.path.join(S, 'gecko', f), os.path.join(R, 'geckoapp/src/main/kotlin/app/kestrel/browser/optimizer', f))
shutil.copy(S + '/core/Report.kt', os.path.join(R, 'core/src/main/kotlin/app/kestrel/core/optimizer/Report.kt'))

# ---- core: the engine's own values already read stay (the first reading, in a process with the person's own settings, is the true default)
sub('core/src/main/kotlin/app/kestrel/core/optimizer/Machine.kt', 'defaults = s.defaults + defaults)', 'defaults = defaults + s.defaults)')

# ---- Prefs
sub('app/src/main/kotlin/app/kestrel/browser/data/Prefs.kt', '    var drawTrialWhy: String get() = sp.getString("draw_trial_why", "") ?: ""; set(v) = put("draw_trial_why", v)\n',
'''    var drawTrialWhy: String get() = sp.getString("draw_trial_why", "") ?: ""; set(v) = put("draw_trial_why", v)
    /**
     * OPT1 (research/OPTIMIZER.md, Auto-tune): the engine preferences a run asked for, `name=value` per line (only names of core Catalog; written into the engine's start-up config by GeckoPrefs.labOverrides),
     * and typed access to the app settings the catalog can change, by key. Nothing outside the catalog's keys can be read or written through [optGet] / [optSet].
     */
    var optPrefs: String get() = sp.getString("opt_prefs", "") ?: ""; set(v) = put("opt_prefs", v)
    fun optGet(key: String): Int? = when (key) {
        "draw_trial" -> drawTrial; "scroll_feel" -> scrollFeel
        "lab_fps30" -> if (labFps30) 1 else 0; "lab_dither" -> if (labDither) 1 else 0; "lab_bytecode" -> if (labBytecode) 1 else 0; "lab_gcpar" -> if (labGcPar) 1 else 0
        "limit_clips" -> if (limitClips) 1 else 0
        else -> null
    }
    fun optSet(key: String, v: Int): Boolean {
        when (key) {
            "draw_trial" -> drawTrial = v; "scroll_feel" -> scrollFeel = v
            "lab_fps30" -> labFps30 = v != 0; "lab_dither" -> labDither = v != 0; "lab_bytecode" -> labBytecode = v != 0; "lab_gcpar" -> labGcPar = v != 0
            "limit_clips" -> limitClips = v != 0
            else -> return false
        }
        return true
    }
''')

# ---- GeckoPrefs: the run's engine prefs are overrides, the guard runs first
sub('geckoapp/src/main/kotlin/app/kestrel/browser/GeckoPrefs.kt', '        for ((name, value) in app.kestrel.core.gfx.DrawTrial.overrides(p.drawTrial)) add("$name=$value")',
'''        for ((name, value) in app.kestrel.browser.optimizer.OptimizerApply.parseEngine(p.optPrefs)) add("$name=$value")   // OPT1: what an Auto-tune run asked for (only names of its catalog)
        for ((name, value) in app.kestrel.core.gfx.DrawTrial.overrides(p.drawTrial)) add("$name=$value")''')
sub('geckoapp/src/main/kotlin/app/kestrel/browser/GeckoPrefs.kt', '        guardTrial(app.kestrel.browser.data.Prefs.get(ctx))',
'''        app.kestrel.browser.optimizer.OptimizerGuard.beforeEngine(ctx)      // OPT1: a run that is active decides the start configuration (and puts the person's own back after a failed start or a closed app)
        guardTrial(app.kestrel.browser.data.Prefs.get(ctx))''')

# ---- OptimizerApply: the run manages the trial's safety itself, the DrawTrial start mark must not revert it behind the run's back
sub('geckoapp/src/main/kotlin/app/kestrel/browser/optimizer/OptimizerApply.kt', '        p.optPrefs = encodeEngine(c.engine.filterKeys { it in Catalog.engineNames() })',
'''        p.optPrefs = encodeEngine(c.engine.filterKeys { it in Catalog.engineNames() })
        p.drawTrialStart = 0L       // the run has its own start guard (OptimizerGuard); the trial's own mark must not switch a tested setting off between two of its steps''')

# ---- LiveDiag.sendFile
sub('app/src/main/kotlin/app/kestrel/browser/diag/LiveDiag.kt', '    private fun fail(p: SharedPreferences, now: Long, why: String)',
'''    /**
     * OPT1: sends ONE file to the private repository under [path] (a new name each time: GitHub refuses to replace one). Same rules as the diagnostics upload: the owner's switch must be on, the token
     * set, the repository private, and nothing while a private tab exists. [done] gets a sentence for the person.
     */
    fun sendFile(path: String, json: String, done: (String) -> Unit) {
        Io.exec {
            val p = sp
            val token = p?.getString("token", "") ?: ""
            if (p == null || !enabled || token.isEmpty()) { done("Not sent: diagnostics sending is off or has no token (Settings > Diagnostics)."); return@exec }
            if (PrivateGuard.active()) { done("A private tab is open: nothing was sent."); return@exec }
            try {
                when (repoState(token)) {
                    RepoState.PRIVATE -> {}
                    RepoState.PUBLIC -> { done("The repository is public: nothing was sent."); return@exec }
                    RepoState.NO_ACCESS -> { done("GitHub says the token cannot see the repository."); return@exec }
                    RepoState.ERROR -> { done("Could not reach GitHub: the results stay on this tablet, tap Send again later."); return@exec }
                }
                var code: Int; var attempt = 0
                while (true) {
                    val name = if (attempt == 0) path else path.removeSuffix(".json") + "-" + attempt + ".json"
                    code = put(name, json, token)
                    if (code == 422 && attempt < 2) { attempt++; continue }
                    break
                }
                done(if (code in 200..299) "Sent the Auto-tune results to the private repository." else "GitHub answered $code: not sent.")
            } catch (e: Exception) { done("Could not send (no connection?). The results stay on this tablet.") }
        }
    }

    private fun fail(p: SharedPreferences, now: Long, why: String)''')

# ---- Flavor flag, Settings row, listener default
sub('app/src/main/kotlin/app/kestrel/browser/Flavor.kt', '    @JvmField var hasNetworkLab = false', '    @JvmField var hasNetworkLab = false\n    /** OPT1: the Auto-tune screen exists (Kestrel G only: it drives the Firefox engine). */\n    @JvmField var hasAutoTune = false')
sub('geckoapp/src/main/kotlin/app/kestrel/browser/GeckoApp.kt', '        Flavor.hasNetworkLab = true', '        Flavor.hasNetworkLab = true\n        Flavor.hasAutoTune = true')
sub('app/src/main/kotlin/app/kestrel/browser/ui/SettingsView.kt', 'interface SettingsListener {\n    fun closeSettings()', 'interface SettingsListener {\n    /** OPT1: Settings > Auto-tune (Kestrel G): open the automatic optimizer screen. */\n    fun openAutoTune() {}\n    fun closeSettings()')
sub('app/src/main/kotlin/app/kestrel/browser/ui/SettingsView.kt', '        if (app.kestrel.browser.Flavor.hasNetworkLab) {\n            section("Network lab (for slow pages)")',
'''        if (app.kestrel.browser.Flavor.hasAutoTune) {
            section("Auto-tune")
            button("Auto-tune: find the best settings for this tablet", "Tries each speed setting one by one with fixed test pages, keeps only what is proven better and tells you what it kept. About an hour; plug the tablet in. You can pause or stop, and your own settings come back. The results can go to Claude if diagnostics sending is on.") { l.openAutoTune() }
        }

        if (app.kestrel.browser.Flavor.hasNetworkLab) {
            section("Network lab (for slow pages)")''')
sub('geckoapp/src/main/kotlin/app/kestrel/browser/BrowserActivity.kt', '    private val settingsListener = object : SettingsListener {\n        override fun closeSettings() { this@BrowserActivity.closeSettings() }',
'''    private val settingsListener = object : SettingsListener {
        override fun openAutoTune() { startActivity(Intent(this@BrowserActivity, app.kestrel.browser.optimizer.OptimizerActivity::class.java)) }
        override fun closeSettings() { this@BrowserActivity.closeSettings() }''')

# ---- manifest
sub('geckoapp/src/main/AndroidManifest.xml', '        <activity\n            android:name=".MainActivity"',
'''        <!-- OPT1: the Auto-tune screen (own GeckoView, own session) and the trampoline that restarts the whole app for it (own process, ends itself) -->
        <activity
            android:name=".optimizer.OptimizerActivity"
            android:configChanges="orientation|screenSize|smallestScreenSize|screenLayout|keyboard|keyboardHidden|navigation|uiMode|locale|layoutDirection"
            android:exported="false"
            android:launchMode="singleTask" />
        <activity
            android:name=".optimizer.RestartActivity"
            android:exported="false"
            android:excludeFromRecents="true"
            android:noHistory="true"
            android:process=":restart"
            android:taskAffinity=""
            android:theme="@android:style/Theme.Translucent.NoTitleBar" />

        <activity
            android:name=".MainActivity"''')

# ---- the gate: pages on 127.0.0.1 are the app's own benchmark pages and local development: they use no mobile data
sub('geckoapp/src/main/assets/kestrel-shields/gate-main.js', "  var on = true;                                         // closed until the person's setting says the rule is off",
"""  var LOCAL = false; try { LOCAL = location.hostname === '127.0.0.1'; } catch (e0) { /* no location */ }   // OPT1: a page on this machine (Auto-tune's benchmark pages: their clips must play) costs no mobile data: the rule is about data
  var on = !LOCAL;                                       // closed until the person's setting says the rule is off""")
sub('geckoapp/src/main/assets/kestrel-shields/gate-main.js', "        var d = e && e.detail;\n        if (d === '0')", "        var d = e && e.detail;\n        if (LOCAL) d = '0';\n        if (d === '0')")
sub('core/src/main/kotlin/app/kestrel/core/optimizer/Machine.kt', 'fun start(runId: String, now: Long, priority: Priority, budgetMin: Int, base: Config, skip: Set<String>, engineVersion: String, appVersion: String): RunState =\n        RunState(runId, now, priority, budgetMin, PlanBuilder.build(catalog, skip), base,',
    'fun start(runId: String, now: Long, priority: Priority, budgetMin: Int, base: Config, skip: Set<String>, engineVersion: String, appVersion: String, only: Set<String> = emptySet()): RunState =\n        RunState(runId, now, priority, budgetMin, PlanBuilder.build(catalog.filter { only.isEmpty() || it.id in only }, skip), base,')
print('applied')
