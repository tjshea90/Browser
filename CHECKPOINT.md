# CHECKPOINT 1347 — read me first, then TASKS.md

**Written:** 2026-11-13T00:39:00Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `bede630` (this checkpoint is the commit after it)

## Just done
gated kestrel-g-v0.3.0-alpha25 (gecko code 124) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref ccr-a57c163e-mtq8js, inputs {"app":"gecko","full_build":"true","note":"alpha25: faster taps, lighter pages, cleaner links, and a Network lab to try things. Taps on pages wider than the screen (Desktop site, old pages) no longer wait about a quarter of a second: double-tap zoom is now off by default (Settings > Appearance brings it back; pinching always works). Video and social players inside pages (YouTube, Vimeo, Facebook, Instagram, TikTok, Spotify, SoundCloud, Disqus, Google Maps) now show a Tap to load box, so the page opens faster and nothing in them runs until you tap (Settings > Shields, or per site in the Shields panel). 2,441 more rules from the privacy lists clean tracking parts out of the links you open (for example YouTube's share tag). Three fixes: the Wi-Fi lock of background downloads really works on Android 10 now, "Warm up the top suggestion" really opens the connection while you type (the Firefox engine silently ignored it before), and the browser no longer asks websites for JPEG XL pictures, which this tablet decodes slowly. Settings > Network lab has six new switches to try one at a time, and Diagnostics has a Tap lab and a Decode lab that measure this tablet. Also: ad-hiding rules arrive sooner after a cold start, the dock follows the keyboard by event instead of polling, and a swiped-away tab keeps its newest scroll position. Ad-hiding rules that restyle a page instead of hiding a part of it are understood now (697 of the 770 in the lists; they start working at the next list refresh), and a download whose file name Android's file index refuses now tries the next name instead of failing."}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag kestrel-g-v0.3.0-alpha25), then:  bash tools/record-release.sh kestrel-g-v0.3.0-alpha25 "alpha25: faster taps, lighter pages, cleaner links, and a Network lab to try things. Taps on pages wider than the screen (Desktop site, old pages) no longer wait about a quarter of a second: double-tap zoom is now off by default (Settings > Appearance brings it back; pinching always works). Video and social players inside pages (YouTube, Vimeo, Facebook, Instagram, TikTok, Spotify, SoundCloud, Disqus, Google Maps) now show a Tap to load box, so the page opens faster and nothing in them runs until you tap (Settings > Shields, or per site in the Shields panel). 2,441 more rules from the privacy lists clean tracking parts out of the links you open (for example YouTube's share tag). Three fixes: the Wi-Fi lock of background downloads really works on Android 10 now, "Warm up the top suggestion" really opens the connection while you type (the Firefox engine silently ignored it before), and the browser no longer asks websites for JPEG XL pictures, which this tablet decodes slowly. Settings > Network lab has six new switches to try one at a time, and Diagnostics has a Tap lab and a Decode lab that measure this tablet. Also: ad-hiding rules arrive sooner after a cold start, the dock follows the keyboard by event instead of polling, and a swiped-away tab keeps its newest scroll position. Ad-hiding rules that restyle a page instead of hiding a part of it are understood now (697 of the 770 in the lists; they start working at the next list refresh), and a download whose file name Android's file index refuses now tries the next name instead of failing."
NEVER ship kestrel-g-v0.3.0-alpha25 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  bede630 ckpt 1346: EX-10 PROVEN on the emulator (Tap to load box replaced the third-party frame;
  d7c8bb0 ckpt 1341: EX-1 PROVEN on the emulator (WifiService acquireWifiLock lockMode=3 at the st
  ae14f0d ckpt 1334: :style() restyling rules in core (697 of 770 real rules accepted; CosmeticSty
  217004a ckpt 1327: EX-2 PROVEN on the emulator (Gecko's own nsHttp log + host TLS listener: old 
  59c0013 ckpt 1322: Data saver choice re-applies the engine prefs (the look-up-links lab switch f
  49dea49 ckpt 1320: Docs: docs/ARCHITECTURE.md alpha25 section, research/EX_RESULTS.md (what was 
  c560ca9 ckpt 1316: EX ladder: EX-10 embeds-wait-for-a-tap coded (Shields flag EMBEDS=128 + Prefs
  352efcb ckpt 1306: More of the EX ladder coded (compiles; unit tests green: NetProfile 12, Backu
  edaf04d ckpt 1288: EX-2/3/4/8/9/11 code done (compile green, unit tests for EX-9; emulator proof
  8a4f7fd ckpt 1283: EX-1 code done: DownloadService Wi-Fi lock = WIFI_MODE_FULL_HIGH_PERF on ever
```
