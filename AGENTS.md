# AGENTS.md

<!-- handoff:learnings:begin -->
## Session Learnings (auto-updated by handoff)

### Implicit Rules
- Kotlin framework Views only; no AppCompat/Compose/Material/Room/WorkManager/coroutines; never GMS; minSdk 29 targetSdk 36; GeckoView 160 armeabi-v7a
- Develop only on ccr-a57c163e-mtq8js; never force-push; no PRs; repo PUBLIC: no keys, owner photos, personal data, APKs
- ckpt after every step; write each owner request into TASKS before code; autosave commits+pushes after each tool call
- Release: bash ship.sh gecko note -> actions_run_trigger android.yml app=gecko full_build=true ref branch -> wait tag (git ls-remote) + run success -> get_release_by_tag -> bash tools/record-release.sh; never add push: trigger; never ship a tag twice
- Extension manifest version = 1.0.<versionCode> (ship.sh syncs; test_handoff gates)
- Engine pref calls (GeckoPreferenceController set/clear) only on main thread; getGeckoPrefs/setGeckoPrefs assert handler thread
- Emulator AVD kestrel29x64 slow: run flows in background with logs, waitfor loops; adb at /opt/android-sdk/platform-tools/adb; fill storage with fallocate on /data/local/tmp/filler (remove before installs); debug props: debug.kestrel.feed/pref1..3/notp/etpcontent; Updates menu label is Update available when an offer exists
- Hooks: foreground sleep >~60 s blocked; no curl/wget; no absolute home paths in written files
- Owner rules: answer every prompt, re-list open items; plain words; say what was not run on the tablet

### Key Decisions
- Decision: per-site Shields off switches Firefox own ETP via GeckoSessionSettings.useTrackingProtection synced from tab.shields (shieldsUp && blockAds) before every load — Reason: GV160 ContentBlockingController has no exception API; flag proven on emulator with urlclassifier.trackingTable.testEntries probe
- Decision: Install the other way offered only when UpdateLogic.otherWayPlan fits (free + own footprint - saved copy >= needKeep) — Reason: owner tablet would be left with no browser and Android still refusing
- Decision: Updater keeps owner alpha18 rule (installer always tried); only HINTS before download — Reason: earlier inflated estimates blocked updates that would have worked
- Decision: Clear cache = StorageController ALL_CACHES then unlink cache2/doomed and cache2/trash* (also at every engine start) — Reason: engine only unlinks doomed entries lazily; emulator showed 48 MB lingering, no process held fds
- Decision: space maths measure filesDir partition not files/updates — Reason: missing folder answers 0 total/free so reserve was 0 on a fresh install
- Decision: all engine pref calls hop to main thread (hopToMain) and apply text is written before prefs — Reason: GeckoResult.map needs a Looper; first pref sent then exception aborted the rest
- Decision: ship alpha29 instead of alpha28 for the owner (one install covers both) — Reason: owner cannot install anything until ~1.5 GB freed; extension manifest version 1.0.128
- Decision: no extractNativeLibs=false and no engine cache cap change — Reason: GeckoView needs libs on disk (research/UPDATE_STORAGE.md); cache is clearable on demand

<!-- handoff:learnings:end -->
