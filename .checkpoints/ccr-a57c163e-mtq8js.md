# CHECKPOINT 1346 — read me first, then TASKS.md

**Written:** 2026-11-13T00:34:37Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `18b9d9f` (this checkpoint is the commit after it)

## Just done
EX-10 PROVEN on the emulator (Tap to load box replaced the third-party frame; one tap loaded its real address = Gecko's 'trouble finding that site'; the same-site frame untouched; first try failed only because System UI was not responding right after the emulator boot); the worker restart killed the emulator + test servers, restarted them; TASKS EX-10 [x]; owner message 16 arrived twice, noted in AC1

## Do this next
Kill the emulator + test servers (CPU for the suites), run the full gates: ./gradlew :core:test :app:testDebugUnitTest :geckoapp:compileDebugKotlin (log to scratchpad full4.log) and cd web-tests && npm test; then ship.sh gecko with scratchpad note25.txt (+ the :style() / download-name sentences), dispatch android.yml app=gecko full_build=true, wait for kestrel-g-v0.3.0-alpha25 + green run, record-release.sh, reply to the owner with the Release link on its own line + re-listed open items

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  d7c8bb0 ckpt 1341: EX-1 PROVEN on the emulator (WifiService acquireWifiLock lockMode=3 at the st
  ae14f0d ckpt 1334: :style() restyling rules in core (697 of 770 real rules accepted; CosmeticSty
  217004a ckpt 1327: EX-2 PROVEN on the emulator (Gecko's own nsHttp log + host TLS listener: old 
  59c0013 ckpt 1322: Data saver choice re-applies the engine prefs (the look-up-links lab switch f
  49dea49 ckpt 1320: Docs: docs/ARCHITECTURE.md alpha25 section, research/EX_RESULTS.md (what was 
  c560ca9 ckpt 1316: EX ladder: EX-10 embeds-wait-for-a-tap coded (Shields flag EMBEDS=128 + Prefs
  352efcb ckpt 1306: More of the EX ladder coded (compiles; unit tests green: NetProfile 12, Backu
  edaf04d ckpt 1288: EX-2/3/4/8/9/11 code done (compile green, unit tests for EX-9; emulator proof
  8a4f7fd ckpt 1283: EX-1 code done: DownloadService Wi-Fi lock = WIFI_MODE_FULL_HIGH_PERF on ever
  eb2eddd ckpt 1280: Owner message 16 handled per section 0: research/REVIEW-EXTERNAL-1.md saved (
```

(4 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
