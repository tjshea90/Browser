# Kestrel Browser

A very light, fast, private Android browser built for **one specific tablet**: the *Alps 711 / MediaTek MT8168B, Android 10 Go* tablet
(2 GB RAM, 600×973 at 170 dpi, **no Google Play Store and no Google services**) that reaches the internet **only through a phone hotspot**. It has Brave-style Shields (ad and tracker
blocking, HTTPS-first, cookie and fingerprint protection, private window) and none of the bloat: no news, no AI, no rewards, no wallet,
no VPN, no sync, no accounts, no telemetry.

It is **not Brave** and contains no Brave code or branding. There are **two builds** of the same browser, both built and signed by GitHub Actions and published on the [Releases](../../releases) page:

| | **Kestrel** | **Kestrel G** |
|---|---|---|
| Draws pages with | the tablet's own **Android System WebView** (Blink/V8, shared with the OS, no extra RAM) | **Firefox's engine (GeckoView)**, bundled in the app |
| Size | about 2.3 MB | about 92 MB |
| Modern sites (e.g. claude.ai) | refused on this tablet: its WebView is Chromium 74 and cannot be updated | meant to work (checked on an Android 10 emulator only) |
| Installs as | `app.kestrel.browser` | `app.kestrel.browser.gecko`, **beside** Kestrel |

Everything Brave-like (Shields, HTTPS-first, hotspot behaviour, UI) is implemented in this app and shared by both builds; only the engine under it differs.

> ### Please read: what has and has not been verified
> This app was written in a cloud build sandbox with **no Android device**. It was run **once, briefly, on an Android 10 emulator** (an AOSP image whose web engine is the very old
> Chromium 74, in a very slow software mode, against local test pages): it launched, drew its screens correctly at the tablet's 600×973 size, loaded a page, and blocked the ads and trackers
> on it. That run also reproduced a real crash (a crafted `intent:` link) that is now fixed. Everything else was verified **without a device**: the whole app compiles and passes Android Lint
> (including API-level checks for Android 10); 81 JVM tests drive the real Activity, request interceptor, tab/memory logic, SQLite store and updater on an Android 10 framework (Robolectric);
> 127 tests cover the filtering engine (including a 60 000-case comparison against an independent oracle); the page script has 16 tests in real Chromium. Two independent code reviews
> (security and correctness) were done and their confirmed findings fixed. What was **not** verified: the real tablet (touch feel, startup time, memory, **which WebView it has**), real
> websites over a real hotspot, and Google Search on an old engine. Expect rough edges on first use and please report them. See *Known limits* below.

## Install on the tablet (sideload)
1. Open the [Releases](../../releases) page on the tablet and download the APK of the build you want (`kestrel-g-<version>.apk` for the Firefox-engine build, `kestrel-<version>.apk` for the WebView build).
   Optional: check it against the `.sha256` file next to it.
2. Open the downloaded file. Android 10 will ask you to allow **Install unknown apps** for the app you opened it from (your file manager or current
   browser): allow it once, go back, tap *Install*.
3. Optional: *Settings → Apps → Default apps → Browser app → Kestrel*, so links from other apps open here.
4. Open Kestrel. Open the menu → *Settings* → *About* and read the **Browser engine** line (package name and version): that is the tablet's own web engine,
   and it decides how many modern websites will display correctly. If the new-tab page shows an "Old web engine" notice, see *Updating the web engine* below.

Updating later: install the newer APK from the Releases page over the old one. Every release is signed with the same certificate (see *Signing*), so updates install over each other.

## What you get
**Shields (per site, in the shield button on the toolbar)**
- Blocks ads and trackers with EasyList, EasyPrivacy and the uBlock Origin lists (99 000+ host rules, 21 000+ pattern rules, 22 000+ site-specific hiding rules).
- Hides empty ad boxes and cookie-consent banners; blocks pop-ups and pop-unders (including the lists' pop-up rules).
- **HTTPS-first, strict by default**: plain `http://` sites are upgraded, and you get a warning screen before any plain-HTTP page loads. Local/LAN addresses are exempt.
  This matters on a hotspot: your mobile carrier sits in the path and can read or alter cleartext pages.
- Blocks cross-site cookies; adds small per-session noise to canvas/audio readouts and reports fewer CPU cores (anti-fingerprinting).
- Strips tracking parameters (`utm_*`, `fbclid`, `gclid`, …) and skips tracking redirectors (Google/Facebook/YouTube/DuckDuckGo link wrappers).
- Blocks pages on a built-in malware/phishing list (uBlock *Badware risks*) with a warning screen. Google Safe Browsing needs Google services, which this tablet doesn't have, so this list is the only such protection here.
- Per-site switches for every one of the above, and for JavaScript.

**Made for a metered, flaky hotspot**
- *Data saver* (Standard by default): no autoplay, no media preloading, the "save data" signal for sites that honour it; *Strict* also holds back images (tap to load) and web fonts.
- Tabs are **never reloaded at startup**; only the one you look at loads. Previously visited pages open from the saved copy when the hotspot drops.
- Clear error pages that tell "tablet not on Wi-Fi" apart from "hotspot has no internet", and a page that fails because of that **reloads itself when the connection returns**.
- Asks before big downloads on a metered link; shows how much data this browser has used; filter lists refresh weekly with conditional requests (a few hundred bytes when nothing changed, about 2 MB when everything did).

**Built for a low-RAM Go device**
- The new-tab page is native (no web engine started until you open a site); at most two web views stay alive, others are frozen and restored on demand;
  the app survives the system killing the page renderer; no AndroidX/Compose/Material; the whole app is **195 KB of code** (APK 2.3 MB, mostly the filter lists).

**Private window** (menu → *New private window*): runs in its own process with a separate WebView data directory that is wiped when it starts, blocks screenshots
and the recents thumbnail, writes no history/tab list/session, and asks the keyboard not to learn from what you type.

**Also**: tabs, bookmarks, history (capped at 90 days / 5 000 entries), find in page, share, desktop-site toggle, print/save as PDF, downloads through the system downloader,
file upload, camera/microphone/location prompts (asked every time), full-screen video, dark mode that follows the system, text size, eight search engines (**Google by default**).

**Deliberately not here**: news/sponsored tiles, AI assistant, rewards/crypto/wallet, VPN, sync/accounts, Tor, extensions, telemetry of any kind.

## Privacy: what this app talks to
- The websites you open, and Google when you search (anything typed into the address bar that is not an address is searched on Google; you can pick another engine in Settings).
- Once a week (or when you tap *Update now*) the filter-list hosts: `easylist.to`, `ublockorigin.github.io`, `secure.fanboy.co.nz`. No identifiers are sent.
- Nothing else. No analytics, no crash reporting, no accounts. WebView usage metrics are opted out. Backups are disabled.
- The tablet's storage is **not encrypted** (per its hardware report), so anything stored in the app, like cookies or history, sits on flash in the clear.
  Set a screen lock; use the private window or *Settings → Clear browsing data when I quit* for sensitive browsing.

## No Google Play on this tablet: what that changes
- **Installing and updating Kestrel is manual** (sideload the APK from the Releases page; newer builds install over older ones).
- **Google Safe Browsing does not exist here** (it lives in Google Play services). Kestrel's own malware/phishing list is the only protection against known-bad sites; it refreshes weekly.
- **The web engine does not update itself.** Kestrel draws pages with the tablet's Android System WebView, and without Google Play nothing keeps that current.
- **The tablet's list of trusted certificate authorities stops at its last system update** (July 2021). A very new site can show a "connection isn't private" page even though it is fine; if
  *many* sites say "expired" or "not valid yet", the tablet's date and time are wrong. Do not tap *Proceed (unsafe)* on banking, shopping or login pages.
- Websites cannot use Google location services; there is no Google sign-in or autofill.

### Updating the web engine
1. Kestrel → menu → *Settings* → *About* → **Browser engine** shows e.g. `com.android.webview 91.0.4472.88`. A major version of 100 or more is good; below that, many current sites will misbehave.
2. Android only lets a package act as the system WebView if its **name is on the tablet's allow-list and its signature is one the manufacturer configured**. Chromium's documentation on
   [WebView providers](https://chromium.googlesource.com/chromium/src/+/bf91c9f15a735cbf40ad57504bd5f5df561fa09b/android_webview/docs/webview-providers.md) describes it: AOSP-style
   builds use `com.android.webview`, builds with Google services use `com.google.android.webview`.
   - If *Browser engine* says **`com.google.android.webview`**: install a newer *Android System WebView* APK from a trustworthy mirror such as APKMirror. Pick the **armeabi-v7a (32-bit ARM)** build whose
     minimum is **Android 10** (recent versions list it). Check the file is signed by Google LLC and matches the checksum on the page, then open it to update in place.
   - If it says **`com.android.webview`** (typical for a tablet without Google services): the system will not accept Google's build in its place, so the engine can only change with a firmware update.
     That is a hard limit of the tablet, not of Kestrel; tell the developer the version shown, because the alternative then is to bundle a different engine (GeckoView) in a separate, much larger build.
3. After changing it, fully close Kestrel and reopen it; the About page shows the new version.

## Known limits (honest)
- **Page compatibility and engine security follow the tablet's WebView version**, which is unknown and may be old. The About page and new-tab page show it.
- No uBlock-style *scriptlets* (so no YouTube-style ad stripping) and no extensions.
- Fingerprint/data-saver scripts are injected at first paint (Android's document-start hook needs AndroidX WebKit, deliberately not used), so very early inline fingerprinting can run first.
- Encrypted DNS cannot be set inside a WebView app; set *Private DNS* for the whole tablet (Settings explains where).
- Generic ad-box hiding is limited to the 1 200 most common selectors (site-specific rules are complete); `blob:` downloads aren't supported; no omnibox network suggestions yet.
- Never run on real hardware (see the notice above).

## Signing
The APK is signed with a **public development key** (`signing/dev/`, password `kestrel-dev`), chosen so every build from any machine can update the previous one.
Anyone could build a look-alike APK with the same key, so install APKs only from your own copy of this repository. To use your own key, create `keystore.properties`
(gitignored: `storeFile`, `storePassword`, `keyAlias`, `keyPassword`) and rebuild; you will need to uninstall once when switching keys.

## Build, test, develop
```
./gradlew :core:test                  # filtering engine (JVM, ~10 s)
./gradlew :app:testDebugUnitTest      # real Activity on Android 10 via Robolectric (downloads its framework jar once)
cd web-tests && npm test              # injected page script in headless Chromium (Playwright)
bash scripts/verify.sh full           # everything + lint + R8 + APK audit
bash scripts/build-apk.sh             # -> dist/kestrel-<version>.apk
```
Needs JDK 17+, the Android SDK (platform 36, build-tools 36). The bundled filter snapshot is regenerated with `scripts/make-default-lists.sh` (see `docs/FILTER-LISTS.md`).
Design and rationale: `docs/ARCHITECTURE.md`, `docs/HARDWARE.md`. Licences of bundled lists: `docs/FILTER-LISTS.md`. This project has no licence chosen yet.

## For Claude sessions
Read `CLAUDE.md`, then `docs/handoff/STATE.md`. Work is auto-saved to GitHub by hooks (`scripts/autosave.sh`); protocol in `docs/handoff/PROTOCOL.md`.
