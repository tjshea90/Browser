# CHECKPOINT 1238 — read me first, then TASKS.md

**Written:** 2026-11-12T22:12:41Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `e059eb8` (this checkpoint is the commit after it)

## Just done
AA3-7 Back up/Restore, part 1 done and tested: core BackupCodec (strict untrusted-file reader: caps, http(s) only, dedupe, version check; 13 tests), app BackupManager (settings whitelist with types/ranges, build/restore/describe/fileName; phone pairing, counters, debug switches deliberately excluded), Prefs.backupSnapshot/restoreBackup, BrowserDb.importBookmarks/importHistory, Shields.exportText/importText, SiteShieldStore.entries; BackupManagerTest 9 (Robolectric round trip incl. 'a file can never set anything outside the list'). NOT yet: the Settings screen section, the flow (Back up now writes Downloads/Kestrel backup <date>.json via MediaStore, Restore uses ACTION_OPEN_DOCUMENT), tab restore, both activities' wiring, emulator check.

## Do this next
continue AA3-7 part 2: 1) SettingsListener.backupNow()/restoreFromFile() default no-ops + Settings section 'Back up & restore' (info line, two buttons) in SettingsView.rebuild before About; 2) app/.../ui/BackupFlow.kt: backup = IO thread MediaStore.Downloads insert (name BackupManager.fileName(), mime application/json, RELATIVE_PATH Downloads) + toast with BackupManager.describe; restore = ACTION_OPEN_DOCUMENT (application/json, */* fallback) -> read <= BackupCodec.MAX_BYTES -> decode (BackupException message to the owner) -> confirm dialog with describe() -> BackupManager.restore on IO -> host.openTabs(urls) + host.afterRestore(); refuse in a private window; 3) wire both BrowserActivity files (gecko geckoapp/.../BrowserActivity.kt settingsListener ~line 1454, webview app/src/webview/.../BrowserActivity.kt ~711) incl. onActivityResult forwarding and a Host for tabs (non-private tabs: url+title) and openTabs (hibernated/background tabs, not all loaded at once); 4) Robolectric/Flow test where possible, then emulator: Back up now -> file in /sdcard/Download; wipe app data; restore through the picker; 5) then ship alpha24 with D2 + typing fix + backup

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  b630671 ckpt 1227: AA3-3 D2 DONE and verified: all ~29,204 generic cosmetic rules now apply (was
  15b71c1 ckpt 1196: AA3-2 typing/keyboard: closed the 'keyboard resizes the page' hypothesis on G
  81f5a25 ckpt 1187: SHIPPED + RECORDED: Kestrel G 0.3.0-alpha23 (code 122), tag kestrel-g-v0.3.0-
  05062dd ckpt 1185: alpha23 CI run 43305054932 failed in test-jvm: BrowserFlowTest.newTabPageWarn
  65bbb1c ckpt 1183: gated kestrel-g-v0.3.0-alpha23 (gecko code 122) and pushed it: handoff self-t
  8e8c9bc ckpt 1176: AB3 code done + unit-tested: NetProfile (core) now has isFastTunnel (10.111.2
  3a65bfe ckpt 1151: AB2 verified on the Android 10 emulator: long press opens the new Save video 
  d9db0b5 ckpt 1139: AB1 done: ONE master switch for the phone helper: PhoneBridge.setMaster/maste
  98cbc10 ckpt 1135: Owner message 15 written into TASKS.md as AB (AB1 one master switch for the p
  b420dda ckpt 1132: alpha22 SHIPPED and recorded (run 43272414589 green incl. the first publish u
```

(10 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
