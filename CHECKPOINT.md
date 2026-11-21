# CHECKPOINT 2830 — read me first, then TASKS.md

**Written:** 2026-11-21T19:44:47Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/peaceful-darwin-4sbmdz` · **builds on:** `e6ea4848` (this checkpoint is the commit after it)

## Just done
PV1 code in place and compiling: core PrivateScrub + PrivateWindow (71 core livediag tests green); app PrivateGuard + Health.log funnel (no site name in the ring while a private tab exists + 2 min after), LiveDiag holds sends and snapshot while the window is active, BrowserActivity: window opens in newTabInternal(private) / syncPrivateAnchor, engine-etp host and the Engine line's 'current <address>' no longer name a private page, tab counts regular only, liveDiagSnapshot empty while active; CrashNote masks messages and scrubEngineReports deletes crash dumps + cuts .extra to safe keys (called at start and 15 s after onCrash/onKill); MiniNav thumb habit gated; find bar / rename box IME no-learning

## Do this next
Next: (1) app tests (new PrivateTabsTest: ring + LiveDiag journal + crash note + engine reports carry no host in the window and do after it; LiveDiag.sendNow refuses; source pins) ; (2) research/PRIVATE_TABS.md audit table (every sink: guarded since earlier sessions / fixed now / relied on from the engine / cannot be closed: the site itself, DNS, the owner's phone hotspot); (3) a Diagnostics 'Private tabs' line (window state + counters private.masked/private.holds); (4) WebView flavor hook (app/src/webview BrowserActivity: PrivateGuard.setOpen where private tabs are created/closed) ; (5) CLAUDE.md standing rule; (6) full gate, alpha86, ship; (7) answer the owner

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  985f693e ckpt 2829: PV1 written into TASKS.md in the owner's words (private tabs: nothing saved 
  ede36761 ckpt 2828: alpha85 RELEASED AND RECORDED: https://github.com/tjshea90/Browser/releases/
  0f2cd29a ckpt 2827: alpha85 gated (core 1991 incl. FLK1 flake passing alone, app 478, lint 0 err
  b9f6b0b0 ckpt 2826: gated kestrel-g-v0.3.0-alpha85 (gecko code 186) and pushed it: handoff self-
  495ef4db ckpt 2825: pre-ship: alpha85: the live diagnostics now explain WHY a page is slow, whic
  4cf115af ckpt 2824: LD4 gate so far: core 1991 tests (1 failed = FLK1 PhoneBackendTest flake, pa
  a1863e08 ckpt 2823: LD4c done: diag_digest.py LOAD ANALYSIS (per site: loads, load/first paint/f
  0c525795 ckpt 2822: LD4b app side done and compiling: GeckoEngine.networkProfileCode, ShieldsBri
  ca1f0fe2 ckpt 2821: LD4b extension side done: pagetime.js (ES5, top frame, document_start, laten
  029be5ca ckpt 2820: LD4b core done: DiagScrub caps (event 480, snapshot lines 1600: the CPU/Bloc
```

(8 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
