# BRIEF - platform decisions, owner facts, and what is still open

Moved here from the old `docs/handoff/STATE.md` on 2026-11-11 (the save system now follows the owner's other repos: CHECKPOINT.md = where the
last session stopped, TASKS.md = the job list, this file = decisions that do not change). Design rationale: `docs/ARCHITECTURE.md`.

## The device (`docs/HARDWARE.md`, raw file `docs/hardware/hardware_.txt`)
MediaTek MT8168B "Alps 711" tablet, Mali-G52 MC2, **Android 10 Go (API 29)**, security patch 2021-07-05, kernel 4.14.141, **2 GB RAM (reports 1.9 GB), low_ram=true,
heap class 128 MB**, 600x973 @170 dpi (~564x916 dp), font scale 1.2, 32-bit userspace (`sys_mssi_t_32_ago`), unencrypted, single user.
**No Google Play Store, no Google services**: no Safe Browsing, no WebView auto-update, manual APK installs, NEVER add a GMS dependency.
**Internet is 100% through a phone hotspot** (UNMETERED per the owner since 2026-11-12, but the carrier caps it at about 5 Mbit/s; flaky, carrier-routed; LinkMeter's Fast Wi-Fi can lift the cap): HTTPS-first, no background network except the daily
conditional filter-list refresh and the daily release check, never reload background tabs at startup, prefer cache when OFFLINE.

## Owner facts (authoritative)
- Anything typed in the address bar that is not an address is searched on **Google** (default engine = first entry of `Prefs.ENGINES`).
- Licensed third-party products/sources are allowed (personal, non-commercial; the owner obtains permissions) - still record each licence in `docs/FILTER-LISTS.md`.
- Explicitly NO news, AI, rewards, wallet/web3, VPN, sync/accounts, Tor, telemetry, or other bloat. Brave-like Shields + security hardening only. No Brave name/logo.
- Wants it fast, snappy, lightweight. Prefers longer sessions and more usage if the product is better, but wants usage spent efficiently.
- **GitHub Actions builds and releases every APK** (2026-11-11: "From now on, use github actions to build and release apks and release them on github"), with the
  checkpoint/save system of the owner's other repos (Launcher, Portfolio).
- The owner switches Claude Code plugins on/off on request. Known blocker: the `simple-workflow` plugin's Bash guard denies curl/wget-style network commands,
  recursive force-deletes and `git config user.*` reads (it matches that text even inside a heredoc). Do not bypass it; use the GitHub MCP tools, `gh api -X GET search/...`,
  git, sdkmanager and Gradle instead, and write such text with the Edit tool.

## Two builds, one repo
| | Kestrel (`:app`) | Kestrel G (`:geckoapp`) |
|---|---|---|
| Engine | system WebView = Chromium 74 on this tablet (`com.android.webview 74.0.3729.186`) | Firefox's engine, GeckoView 160, bundled |
| Size | ~2.3 MB | ~92 MB |
| applicationId | `app.kestrel.browser` | `app.kestrel.browser.gecko` (installs BESIDE the other) |
| Modern sites (claude.ai...) | refused ("out of date"): the engine cannot be updated | works (smoke-tested on an x86_64 emulator) |
| Release tag / asset | `kestrel-v<ver>` / `kestrel-<ver>.apk` | `kestrel-g-v<ver>` / `kestrel-g-<ver>.apk` |
Shared code is `app/src/main` (UI, data, shield, net); WebView-only code is `app/src/webview/kotlin`; Gecko-only code is `geckoapp/src/main`. Pure logic is `core/`.
Why a bundled engine: the AOSP `com.android.webview` provider is allow-listed + signature-checked, so Google's WebView APK cannot replace it; Chromium has no embeddable
form except that system WebView (Brave/Cromite are whole browsers built from Chromium source). GeckoView is the only modern engine that can sit under our own light UI.

## Decisions (short; rationale in ARCHITECTURE.md)
- Native new-tab page (no engine at startup); one live engine view (+1 for an opener); other tabs hibernated through the engine's saved state.
- Private window: WebView build = separate process `:private` with `WebView.setDataDirectorySuffix`, FLAG_SECURE, nothing persisted; Gecko build = `usePrivateMode` sessions.
- Filter lists: pure-host rules as sorted 64-bit hashes, other rules token-indexed, compiled to the binary `.kfs`; refreshed daily (conditional GET); canaries refuse a poisoned list.
- Gecko: strict ETP, first-party-isolation cookies, query-param stripping, bounce-tracking protection, GPC, DoH (TRR first, Cloudflare), fission and the extensions process off, low-memory detection.
  Our filter engine runs as the built-in WebExtension `assets/kestrel-shields` (native messaging: **plain strings both ways**, structured replies fail with "Invalid event data").
  Gecko child processes instantiate Application: only the UI process loads the filter engine (`GeckoApp` checks `getProcessName()`).
- Suggestions OFF by default (data + privacy). HTTPS-first STRICT by default. Hotspot rule: only OFFLINE (no INTERNET capability) means cache-first; NO_INTERNET (unvalidated) only changes wording.
- Signing: the PUBLIC dev key in `signing/dev/` (pinned in `signing/CERT_SHA256`) signs every release, so any session/CI build updates the installed app. A private key may replace it later (see TASKS.md).

- 2026-11-12 (owner, message 13): the hotspot is UNMETERED ("no data saving is necessary, but the option may remain in the settings"): data saver defaults to OFF, nothing acts metered unless a saver level is on, speculative loading and Wi-Fi media buffering are allowed. Their carrier caps the hotspot at 5 Mbit/s (LinkMeter docs); the phone is an Android 16 Samsung with Shizuku, paired with the owner's LinkMeter app (tjshea90/LinkMeter: Fast Wi-Fi VPN relay on phone port 8767, control port 8765: the Courier must never use those). Owner goal: speed and snappiness first, RAM may be used liberally (800 MB to 1 GB free). One switch (main menu "Phone helper") must turn the phone off completely. Ad block: "whatever is logical" -> stays in the engine (research/SPEED2.md).

## Known issues / risks
- **First real-tablet result (owner, 2026-11-11): "The new version of the browser loads Claude.ai"** = Kestrel G works on the real tablet (claude.ai is the page the WebView build refused). Nothing else has been reported yet:
  speed, memory, UI feel and the rest are still unmeasured there. The WebView build and the Gecko build were otherwise only run on Android 10 emulators (software mode). Say so honestly.
- Generic cosmetic hiding applies an alphabetical prefix of 1200 generic selectors (site-specific rules, 22k, are complete): TASKS.md has the class/id index plan.
- The cloud sandbox has no emulator acceleration (no KVM): emulator runs take minutes per step; builds belong on GitHub Actions.
- Licences: bundled lists are GPLv3 / CC BY-SA data (`docs/FILTER-LISTS.md`); the owner has not chosen a project licence.
