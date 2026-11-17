# CHECKPOINT 2619 — read me first, then TASKS.md

**Written:** 2026-11-17T09:21:41Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-e89b9dfe-pl3lk5` · **builds on:** `45b46ddf` (this checkpoint is the commit after it)

## Just done
LAB5c-1 (first real app change, measured by a same-APK A/B): GeckoEngine.prewarmConfig parses the engine config (files/gecko-config.yaml) with GeckoView's own DebugConfig.fromFile on a background thread from GeckoApp.onCreate (UI process only), so SnakeYAML's class loading + static init (GeckoView reads its YAML config with SnakeYAML ON THE UI THREAD inside GeckoRuntime.create; run-9 profile caught Yaml.load there) happens off the UI thread; plus permanent Health lines 'engine runtime-create-took' (UI thread) and 'engine config-warm' (background); debug kill switch debug.kestrel.nowarm=1; GeckoPrefs.writeConfig is @Synchronized. FROM THE RUN-9 PROFILE (start-up ui prof): 1.25 s of the emulator's block is Android's first-draw renderer sync (HardwareRenderer.setStopped, platform), then Yaml.load, then GeckoNetworkManager.getDhcpInfo binder call. prefsweep.py now takes '@prop=value' variants and records create_ms / warm_yaml_ms; sweep-variants.txt = base, base2, nowarm. Compiles; NOT yet measured

## Do this next
Queue order (one pending slot per ref): pack run 44053244976 running -> daily (queued, has the ART profile step) -> when the daily STARTS dispatch lab_suite=sweep lab_minutes=5 (the nowarm A/B); read PACK-REPORT.md first (omni stored / libs); agents' reports pending

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  da746ada ckpt 2618: LAB5b: more lab tooling committed (no run yet): suite 'sweep' = tools/emu/pr
  96e097e3 ckpt 2617: LAB5b: pref-sweep tooling started: GeckoPrefs.writeConfig reads debug.kestre
  97f31512 ckpt 2616: LAB5b: packaging A/B lab built and wired: geckoapp/build.gradle.kts has two 
  1cf2f4fd ckpt 2615: LAB5 (owner message 2026-11-17T06:42:30Z: improve the browser as much as pos
  cc1424d3 ckpt 2614: alpha63 SHIPPED + recorded (run 44046401335 green, release kestrel-g-v0.3.0-
  12cd7f08 ckpt 2613: alpha63 dispatched ONCE (run_workflow app=gecko full_build=true ref ccr-a10e
  d59a6ddb ckpt 2612: gated kestrel-g-v0.3.0-alpha63 (gecko code 164) and pushed it: handoff self-
  3cdec9fe ckpt 2611: run 9 read: degraded run (emulator restarted, no network, Google A/B all 60 
  ede3c889 ckpt 2610: OWNER RESCHEDULE (usage running out, 2026-11-17 04:58Z): ALL work moved to O
  8bf31a36 ckpt 2609: LAB4b: decisions recorded: ANS60e NOT done (would delay every navigation's g
```

(4 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
