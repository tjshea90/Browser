# CHECKPOINT 1341 — read me first, then TASKS.md

**Written:** 2026-11-13T00:20:38Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `7fb53d5` (this checkpoint is the commit after it)

## Just done
EX-1 PROVEN on the emulator (WifiService acquireWifiLock lockMode=3 at the start of a 12 MB throttled download, released at the last byte; the earlier 'no lock' runs were only the unconfirmed 'Download this file?' dialog); found+fixed MediaStoreSinks.insertRow retry (a stale MediaStore row made 'big (1).bin' fail for good; retried run finished as big (4).bin); TASKS EX-1 [x]

## Do this next
Run ex10b.sh (embed proof: start app, wait ~50 s for the built-in extension, then open http://10.0.2.2:8000/embedtest.html in the running app; screenshot should show 'Tap to load' for the third-party frame), then full suites (./gradlew :core:test :app:testDebugUnitTest :geckoapp:compileDebugKotlin; cd web-tests && npm test), ship.sh gecko with scratchpad note25.txt, dispatch android.yml app=gecko, wait, record-release.sh, reply to the owner with the Release link on its own line + re-listed open items

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  ae14f0d ckpt 1334: :style() restyling rules in core (697 of 770 real rules accepted; CosmeticSty
  217004a ckpt 1327: EX-2 PROVEN on the emulator (Gecko's own nsHttp log + host TLS listener: old 
  59c0013 ckpt 1322: Data saver choice re-applies the engine prefs (the look-up-links lab switch f
  49dea49 ckpt 1320: Docs: docs/ARCHITECTURE.md alpha25 section, research/EX_RESULTS.md (what was 
  c560ca9 ckpt 1316: EX ladder: EX-10 embeds-wait-for-a-tap coded (Shields flag EMBEDS=128 + Prefs
  352efcb ckpt 1306: More of the EX ladder coded (compiles; unit tests green: NetProfile 12, Backu
  edaf04d ckpt 1288: EX-2/3/4/8/9/11 code done (compile green, unit tests for EX-9; emulator proof
  8a4f7fd ckpt 1283: EX-1 code done: DownloadService Wi-Fi lock = WIFI_MODE_FULL_HIGH_PERF on ever
  eb2eddd ckpt 1280: Owner message 16 handled per section 0: research/REVIEW-EXTERNAL-1.md saved (
  884b8ab ckpt 1263: alpha24 SHIPPED and recorded (tag kestrel-g-v0.3.0-alpha24, run 43321730068 g
```

(6 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
