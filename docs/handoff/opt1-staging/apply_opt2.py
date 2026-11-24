
import os
R='/home/user/Browser'; S=R+'/docs/handoff/opt1-staging'
import shutil
os.makedirs(R+'/app/src/test/kotlin/app/kestrel/browser/data', exist_ok=True)
shutil.copy(S+'/tests/OptimizerPrefsTest.kt', R+'/app/src/test/kotlin/app/kestrel/browser/data/OptimizerPrefsTest.kt')
p=R+'/core/src/test/kotlin/app/kestrel/core/OwnerRulesTest.kt'
s=open(p,encoding='utf-8').read()
add = """
    /** OPT1 (owner 2026-11-24: "an automated system ... that tries all different experiments and settings ... finds the best speed"; research/OPTIMIZER.md): the wiring that makes Auto-tune safe. */
    @Test fun opt1AutoTuneIsWiredSoThatNoRunCanLeaveTheOwnerInABrokenOrTestSetting() {
        val gp = source("geckoapp/src/main/kotlin/app/kestrel/browser/GeckoPrefs.kt")
        assertTrue("the run's start guard runs before the engine config is built and before the trial guard", gp.indexOf("OptimizerGuard.beforeEngine(ctx)") in 1 until gp.indexOf("guardTrial(app.kestrel.browser.data.Prefs.get(ctx))") && gp.contains("parseEngine(p.optPrefs)"))
        val guard = source("geckoapp/src/main/kotlin/app/kestrel/browser/optimizer/OptimizerGuard.kt")
        assertTrue("the guard asks the pure machine and writes the configuration it returns", guard.contains("RunMachine().onProcessStart(st)") && guard.contains("OptimizerApply.write(Prefs.get(ctx), cfg)") && guard.contains("if (st.status == Status.DONE) return"))
        val apply = source("geckoapp/src/main/kotlin/app/kestrel/browser/optimizer/OptimizerApply.kt")
        assertTrue("only catalog names and keys can be written", apply.contains("it in Catalog.engineNames()") && apply.contains("k in Catalog.settingKeys()") && apply.contains("n in allowed"))
        val machine = source("core/src/main/kotlin/app/kestrel/core/optimizer/Machine.kt")
        assertTrue("a closed app pauses the run and boots with the person's own settings; a start that never produced a sample fails the step", machine.contains("the app was closed in the middle of the run") && machine.contains("the app died or hung while starting with this setting") && machine.contains("two starts in a row failed"))
        val runner = source("geckoapp/src/main/kotlin/app/kestrel/browser/optimizer/OptimizerRunner.kt")
        assertTrue("the run never starts with a private tab open and never uploads while one is", runner.contains("PrivateGuard.active()") && runner.contains("LiveDiag.isEnabled() && LiveDiag.hasToken()"))
        assertTrue("it stops for a low battery, heat and Battery Saver instead of measuring badly", runner.contains("isPowerSaveMode") && runner.contains("THERMAL_STATUS_SEVERE") && runner.contains("the battery is at"))
        assertTrue("the benchmark server is loopback only and takes the token first", source("geckoapp/src/main/kotlin/app/kestrel/browser/optimizer/BenchServer.kt").contains("InetAddress.getByName(\\"127.0.0.1\\")") && source("geckoapp/src/main/kotlin/app/kestrel/browser/optimizer/BenchServer.kt").contains("parts[0] != token"))
        val man = source("geckoapp/src/main/AndroidManifest.xml")
        assertTrue("the screen and the restart trampoline are declared, the trampoline in its own process and not exported", man.contains(".optimizer.OptimizerActivity") && man.contains(".optimizer.RestartActivity") && man.contains("android:process=\\":restart\\""))
        assertTrue("Settings has the row (Kestrel G only) and the activity opens it", source("app/src/main/kotlin/app/kestrel/browser/ui/SettingsView.kt").contains("Flavor.hasAutoTune") && source("geckoapp/src/main/kotlin/app/kestrel/browser/BrowserActivity.kt").contains("override fun openAutoTune()"))
        val gate = source("geckoapp/src/main/assets/kestrel-shields/gate-main.js")
        assertTrue("pages on the loopback address (the benchmark pages) are not gated: they cost no mobile data", gate.contains("location.hostname === '127.0.0.1'") && gate.contains("if (LOCAL) d = '0';"))
        assertTrue("the digest for Claude exists", source("tools/optimizer_digest.py").contains("kestrel-autotune"))
        assertTrue("the optimizer's engine names are checked against the pinned engine", source("tools/check-gecko-prefs.sh").contains("Auto-tune experiment names it"))
    }
}
"""
i=s.rstrip().rfind('}')
s=s[:i].rstrip('\n')+'\n'+add
open(p,'w',encoding='utf-8').write(s)
# the gate test
p=R+'/web-tests/gate.test.js'
t=open(p,encoding='utf-8').read()
t=t.rstrip('\n')+"""

test('a page on this machine (127.0.0.1: Auto-tune benchmark pages) is not gated, a page on the web still is', async () => {
  const context = await browser.newContext({ hasTouch: false });
  await context.route('http://127.0.0.1:**/*', (r) => r.fulfill({ contentType: 'text/html', body: '<!doctype html><html><head><title>t</title></head><body><video id="v" muted loop style="display:block;width:160px;height:90px"></video><script>setTimeout(function () { var v = document.getElementById("v"); v.play().then(function () { window.__r = "ok"; }, function (e) { window.__r = e.name + ": " + e.message.slice(0, 40); }); }, 300);</script></body></html>' }));
  const page = await context.newPage();
  await page.addInitScript({ content: GATE });
  await page.addInitScript(() => { document.dispatchEvent(new CustomEvent('__kst_gate_cfg_v1', { detail: '1' })); });   // the app says: rule ON
  await page.goto('http://127.0.0.1:8123/');
  await page.waitForFunction(() => window.__r !== undefined, null, { timeout: 5000 });
  assert.ok(!/^NotAllowedError/.test(await page.evaluate(() => window.__r)), 'play() was not refused on the local page: ' + await page.evaluate(() => window.__r));
  await context.close();
  const web = await open(VID + LATE_PLAY, '1');                                       // the same rule on a site of the web: refused
  await web.waitForFunction(() => window.__r !== undefined, null, { timeout: 5000 });
  assert.match(await web.evaluate(() => window.__r), /^NotAllowedError/);
});
"""
open(p,'w',encoding='utf-8').write(t)
print('applied2')
