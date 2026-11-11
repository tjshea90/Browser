# ENGINES - research P2: which web engine under Kestrel (MT8168B tablet, Android 10 Go, 32-bit, 2 GB, metered hotspot)

Written 2026-11-11 by a research agent for TASKS P2 (owner: "Research online and consider other engines to use other than Firefox. Consider cromite."). No code or build file was touched. **Nothing here ran on the tablet** (no device, no KVM): every statement about speed or RAM on THIS tablet is [U] unless a source is named.
Tags: **[V:key]** read in a primary source (URL in section 9) / **[S]** secondary source or search snippet / **[U]** my inference or memory / **[E]** estimate. Pages were read through a summarising fetch tool, so numbers are as that tool reported them; load-bearing ones were cross-checked on a second page.
Related: `research/GECKO_SPEED.md` (tuning GeckoView 160), `research/ADBLOCK_ENGINE.md` (R5), `docs/HARDWARE.md`, `BRIEF.md`, `docs/hardware/hardware_.txt`.
**In plain words:** Cromite does exist for your tablet's 32-bit chip, but it is a separate browser app, not a part we can put inside Kestrel; the Firefox engine we already use is the only one that can live inside. Cromite also ships with JavaScript acceleration switched off, is 2.5x bigger (232 MB vs 92 MB) and its last update is two months old, so it is a backup for sites that break, not a replacement.

## 0. Verdict (10 lines)
1. **Stay on GeckoView (Kestrel G).** It is the only modern engine that can sit under our own UI on a 32-bit Android 10 user build. Chromium has no embeddable form we can ship, and the tablet's WebView cannot be replaced (user build, release-keys, locked bootloader; AOSP allow-list plus signature rule) [V:cr-prov,aosp-wvu,hw].
2. **Cromite does publish arm32** (`arm_ChromePublic.apk` in all four releases checked, v145-v153; Android 10+; GPLv3) but only as a whole-browser APK: no AAR, no arm32 WebView, no supported way to embed it or make it the system WebView here [V:c-rel,c-readme].
3. **Cromite fits this tablet poorly:** 232 MB APK (v153; ours 92 MB), JavaScript JIT OFF by default, strict site isolation forced even below Chromium's ~2 GB floor, one maintainer whose last stable build (15 Sep 2026, Chromium 153) is 4 milestones behind Chromium 157 [V:c-rel,c-faq,c-sip,cr-proc,brave-rel].
4. **Which engine is snappier on this tablet is unknown.** No public Gecko-vs-Chromium measurement on Cortex-A53 / 2 GB exists; Speedometer 3 on mid-range phones is mixed (Firefox +11 % on one phone, Chrome +18 % on another) [V:bench]. My lean: Gecko >= Cromite with its defaults (about 60 %, [U]); a coin flip if Cromite's JIT is switched on.
5. **Chrome's recent Android speed-ups do not reach this device class:** they came from 64-bit, -O2/-O3, PGO "premium" builds; entry-level devices get a size-constrained build [V:goog-blog]. Neither engine has a built-in edge on 4x A53 / 32-bit [U].
6. **Servo, WPE WebKit and Ladybird are not options:** Servo's Android artifacts are aarch64-only and its JIT is 64-bit-only; WPE Android lists arm64/x86_64 only; Ladybird has no Android work [V:servo-rel,servo-pr,wpe,ladybird].
7. **Self-building Chromium is not feasible on free runners:** >= 100 GB disk and 8-16 GB RAM for Chromium vs a hosted runner of 4 vCPU / 16 GB / 14 GB SSD with a 6 h job cap; Cromite's own release workflow uses dispatch-selected runners with host-mounted storage and a 24 h timeout [V:cr-build,gh-runners,gh-limits,c-wf].
8. **A cheap hybrid exists:** an "Open in Cromite" menu item (explicit-package `ACTION_VIEW`, about 0.5 day, +0 bytes in our APK) for sites where Gecko fails, labelled as a stale-patch-risk second app; Brave (Chromium 157 on 2026-11-10, 221 MB arm32 APK) is the fresher Chromium [V:brave-rel].
9. **Confidence:** "keep GeckoView" HIGH (about 90 %); "Gecko is also the faster one here" LOW-MEDIUM (about 60 %). A 30-minute A/B on the tablet (section 7) settles speed and RAM.
10. **Engine-independent wins beat any engine swap** (section 6.3): early request blocking (R5: median load 14 s to 2 s on 13 pages), JIT left on, few processes, warm start, light modes for heavy pages.

## 1. Comparison (arm32 = armeabi-v7a; tablet is 32-bit userspace, Android 10 = API 29)
| Engine | arm32 | Min Android | Size | Embeddable how | RAM on 2 GB | Speed evidence | Ad-block | Updates | Licence | Verdict |
|---|---|---|---|---|---|---|---|---|---|---|
| **GeckoView 160** (Kestrel G today) | yes: `geckoview-armeabi-v7a` 160.0.20261104181552; Mozilla: "32-bit ARM devices will continue to be supported" (2025-09-15) [V:maven,moz-android] | Firefox: 8.0+ since 144 [V:moz-android]; our minSdk 29 | APK 92 MB + ~135 MB extracted [V:repo] | AAR on maven.mozilla.org, our own UI [V:maven,gv-docs] | unmeasured; budget in GECKO_SPEED sec 4 (Fission off, 1-2 content procs) | no public A53 comparison [U]; Mozilla tracks Fenix cold start on a Moto G5 (8x Cortex-A53 class [U]) [V:moz-gpu]; claude.ai loads on the tablet [V:BRIEF] | ours: in-extension engine (R5), est. 25-50 us/request on A53 [E:R5] | we rebuild per Mozilla release (every 2 weeks since 155) [V:maven,moz-sumo] | MPL-2.0 [S:R5] | **KEEP** |
| **Cromite app** (Chromium 153) | yes: `arm_ChromePublic.apk` [V:c-rel] | Android 10+ [V:c-readme]; upstream floor is API 29 since M139 [V:cr-psa] | arm APK 232 MB (v153), 157-163 MB (v145-v148) [V:c-rel] | no: APK only, no AAR [V:c-rel] | unmeasured; JIT off, strict site isolation forced, no soft renderer cap on Android [V:c-faq,c-sip,cr-proc]; a 2 GB A53 phone user says "smooth" [S:c-3058] | V8 jitless: Speedometer 2 about 40 % slower [V:v8-jitless]; no Cromite numbers found | built-in Eyeo/ABP engine in C++, CNAME uncloaking, snippets toggle; no extensions on Android [V:c-faq,c-1395] | in-app updater (opt-in), own F-Droid repo, GitHub; irregular [V:c-readme,c-rel] | GPLv3 [V:c-readme] | **second app only, after A/B** |
| **Cromite WebView** | NO: arm64 and x64 only (v145-v153) [V:c-rel] | n/a | 275-291 MB (arm64) | system provider only; ROM allow-list or LSPosed-style module [V:cr-prov,anywebview] | n/a | n/a | n/a | n/a | GPLv3 | **impossible here** |
| **Google WebView sideload** (`com.google.android.webview`) | builds exist, need a separate Trichrome Library APK [S:apkmirror] | 10 for M139-M140; some 141 listings say 12L [S:apkmirror] | n/a | provider only: non-GMS Q allow-list is `com.android.webview` alone; user build needs a listed name AND release-key signature (or system app) [V:cr-prov,aosp-wvu] | n/a | n/a | n/a | manual APKMirror installs (no Play) | proprietary | **dead unless Developer options shows a second provider** |
| **Chromium self-build** (content_shell / AwContents embedder, Crosswalk-style) | `arm` allowed for non-WebView targets [V:cr-build] | 29 [V:cr-build] | vanilla arm APK 166 MB (Cromite test build, v147) [V:c-rel] | `content_shell_apk` wraps the content layer; no supported library [V:cr-build]; Crosswalk ended 2017 [S:crosswalk] | n/a | n/a | would need native network patches | we rebase every 2 weeks | BSD-3 [U] | **infeasible** |
| **Other Blink apps** (Brave, Vivaldi, Samsung, Opera, Edge, Kiwi, Bromite) | Brave: `BraveMonoarm.apk` 221 MB [V:brave-rel]; others unchecked | Brave arm32 min unverified | 221 MB (Brave) | none: whole apps | unmeasured | n/a | Brave Shields (Rust engine) [U] | Brave: Chromium 157.0.8103.50 on 2026-11-10 [V:brave-rel]; Kiwi archived Jan 2025, Bromite ended Dec 2022 [S:kiwi,bromite] | MPL-2.0 (Brave) [U] | **second-app candidates; Brave is fresher than Cromite** |
| **Servo 0.8.0** (2026-11-09) | no: Android assets are aarch64 only; JIT enabled only on 64-bit Android [V:servo-rel,servo-pr] | n/a | AAR 38.4 MB, APK 119 MB (arm64) | AAR exists, but "work is still ongoing to make Servo consumable as a webview library" [V:servo-book] | n/a | "prototype web browser engine" [V:servo-gh] | none | about monthly: v0.4.0 (4 Aug) to v0.8.0 (9 Nov) [V:servo-rels] | MPL-2.0 [V:servo-gh] | **no; revisit 2027** |
| **WPE WebKit Android** | no: "arm64 and x86_64" [V:wpe] | n/a | n/a | WPEView 0.3.0 on Maven Central [V:wpe] | n/a | n/a | n/a | n/a | LGPL/BSD [U] | **no (no armv7)** |
| **Ladybird** | no Android | n/a | n/a | none | n/a | n/a | n/a | n/a | BSD-2 [U] | **no**: "Mobile platforms are not a current focus" [V:ladybird] |
| **System WebView M74** (Kestrel today) | yes | 29 | 2.3 MB app | the OS WebView | shared | cannot run modern sites [V:BRIEF] | ours (Kotlin) | frozen | n/a | **fallback only** |

## 2. Cromite in detail (question 1)
- **What it is:** a Chromium fork based on Bromite with built-in ad blocking; "available for Android arm64-v8a, arm32-v7a and x86_64, Android 10 and above, Windows and Linux 64bit" [V:c-readme]. Bromite ended Dec 2022 at v108 [S:bromite]. Package id `org.cromite.cromite`; WebView id `org.cromite.webview` in the GN args, though one user reports a Cromite WebView named `com.android.webview` [V:c-gn; S:c-3109].
- **arm32 today:** yes. `arm_ChromePublic.apk` is in v145.0.7632.76/.120, v147, v148 and v153; size 157, 161, 163, then 232 MB for v153 [V:c-rel].
  - Not checked: that it installs and runs on this exact image; the real minSdk (cromite.org returned HTTP 503, so the F-Droid index was not read). Upstream `chrome_public_apk` is minSdk 29 [V:cr-build].
- **Maintainer and cadence:** the last 15 commits are all by one person (uazo) [V:c-commits].
  - Stable releases 19 Feb, 2 Mar, 10 Apr, 21 May, 15 Sep (gaps 11, 39, 41, 117 days; the list prints no year, 2026 inferred from the v145-v153 numbering) [V:c-rel].
  - Chrome went to 2-week milestones at M153 (8 Sep) [V:chrome-2wk]; the maintainer said work went into "the mechanism to keep up with the bi-weekly releases" (16 Sep) [V:c-3040].
  - As of 11 Nov there is no release after 15 Sep; users report v153 bugs (history loss, location crash) and ask whether the project is dead [V:c-3040,c-3117]; one commenter says updates "would become irregular" [S:c-3118].
  - Chromium 157 was stable by 3-10 Nov, so Cromite 153 is 4 milestones (about 8 weeks) behind [U from V:brave-rel,chrome-2wk].
- **How updates ship:** in-app updater asked on first start, a self-hosted F-Droid repo (cromite.org, fingerprint in the README) and GitHub releases [V:c-readme]. Every update is a full APK, about 232 MB over the hotspot [U].
- **Ad blocking:** Eyeo/ABP-family C++ engine with public lists, "enhanced with support for cname uncloaking and blocking even in workers" [V:c-faq]; an "Enable anti-circumvention and snippets" toggle [V:c-1395]; element hiding by a bundled user script (Bromite heritage) [S:c-347]; no extensions on Android, so no uBlock Origin [V:c-faq]. Per-request cost is unpublished.
- **Defaults that matter on a 4x A53 / 1.9 GB device:**
  - **JIT off:** "Cromite comes with JavaScript JIT disabled by default" (re-enable per site or in flags) [V:c-faq,c-patch]. V8's own jitless test: Speedometer 2.0 about 40 % slower, Web Tooling 80 % [V:v8-jitless]. Edge says disabling JIT "can also disable parts of JavaScript including WebAssembly" [V:edge-jit].
  - **Site isolation forced:** `SitePerProcess` + `StrictOriginIsolation` on with "bypass any device memory size check" (their per-site JIT setting needs it), while Chromium requires "about 2 GB of device RAM" for any site isolation on Android [V:c-sip,c-patch,cr-proc]. This tablet reports 1.9 GB [V:BRIEF], so stock Chromium would likely leave it off [U].
  - **Low-end mode:** only at <= 1024 MB total RAM (maintainer: "the threshold is set to 1Gb by default"; the flag "cannot be configured via chrome://flags") [V:cr-sysu,c-3058], so this tablet runs the normal mode [U].
  - **Build flags:** `is_high_end_android=true`, PGO on, platform (hardware) H.264/HEVC decoders only [V:c-gn]. The flag is Chromium's speed-over-size switch, defaulted for 64-bit only [V:cr-gni]; its effect on arm32 is unverified and may explain part of the 232 MB [U].
- **Embeddable as an engine? No**, in every form asked:
  - Library: release assets are APKs/zips, no AAR [V:c-rel].
  - WebView provider: arm64/x64 only. A stock user build accepts a provider only if its package name is on the image's list (non-GMS Q: `com.android.webview`) AND it is a system app or signed with the listed key; the WebView team: "There's intentionally no way to do this" [V:cr-prov,aosp-wvu,cr-torne]. The tablet is a locked `user`/`release-keys` build [V:hw].
  - Elsewhere it runs as a provider only on ROMs that ship it (for example /e/OS [S:eos]) or via an LSPosed-style module such as AnyWebView, which lists Android 10 as working [V:anywebview] but needs a root-class framework, impossible with this locked bootloader [U].
  - content-shell / AwContents: not published by Cromite.
  - Custom Tabs: Cromite keeps CCT code (ephemeral and incognito CCT patches), but whether it answers as a Custom Tabs service is unverified; plan on plain `ACTION_VIEW` [V:c-patch]. Bound service: none.
- **RAM on 2 GB: nobody has published a measurement.** Only comments: a 2 GB Redmi 9A (1.74 GB usable, Helio G25 = 8x A53) "runs Cromite smoothly... lagging only on video-heavy sites"; a request to tune for 3-4 GB devices says Chromium browsers "can feel heavy, laggy or get killed" there [S:c-3058].

## 3. Other candidates (question 2)
- **Stock Chromium / AwContents / content shell, self-built.**
  - Needs >= 8 GB RAM (16+ advised) and >= 100 GB disk [V:cr-build,cr-lin]. A free hosted runner is 4 vCPU / 16 GB / 14 GB SSD with 6 h per job; standard runners are free on public repos [V:gh-runners,gh-limits,gh-free].
  - One mailing-list report: a clean arm64 debug cross-build took 1 h 20 min on a 24-core EPYC [S:cr-time], so about 8 h on 4 vCPU [U], before official-build PGO/LTO.
  - Cromite's release workflow takes `runs-on` from a dispatch input (dev/ci), mounts host storage (`/storage/images`, a Windows SDK dir), runs Docker images with `siso ninja` and sets `timeout-minutes: 1440`: self-hosted [U from V:c-wf].
  - Even if built, `content_shell_apk` is a test shell [V:cr-build]; Crosswalk, the last embeddable Chromium, ended in Feb 2017 [S:crosswalk]. Verdict: infeasible without paid infrastructure and a 2-weekly rebase.
- **Google's WebView APK, sideloaded.** The rules in section 1 apply; the owner's earlier "it won't work" (INBOX) matches them [V:cr-prov,aosp-wvu]. Chromium M139+ needs Android 10 [V:cr-psa]. Arm32 builds exist on APKMirror but need a Trichrome Library APK, and some 141+ listings show a 12L minimum (unverified) [S:apkmirror]. Free check: Settings > Developer options > WebView implementation; one entry means dead [U].
- **Vanadium / Mulch / DivestOS.** Vanadium is GrapheneOS's WebView and browser and relies on that OS [V:vanadium]; its README gives no ABI or minimum Android (not verified). Mulch ended with DivestOS [S:mull]. Same provider rules; not usable on a stock user build.
- **Blink apps (Brave, Vivaldi, Samsung, Opera, Edge, Kiwi).**
  - Brave v1.99.56 (Chromium 157.0.8103.50, 2026-11-10) ships `BraveMonoarm.apk` 221 MB; its wiki still points 32-bit users to it [V:brave-rel,brave-wiki]. Its 2026 minSdk is unverified; APKMirror lists recent Brave 32-bit betas as Android 12L+, which would exclude this tablet [S:apkmirror]. Brave also carries Rewards/Wallet/AI code the owner excluded [U].
  - Kiwi archived Jan 2025 [S:kiwi]. Vivaldi, Samsung, Opera, Edge: closed apps, not checked. None is embeddable.
- **GeckoView and forks.**
  - GeckoView is built to be "embedded as a web engine component in any kind of app" [V:gv-docs]. Maven has every major 64 to 160; latest 160.0.20261104181552, updated 2026-11-10 [V:maven].
  - Cadence: 154 (14 Aug), 155 (26 Aug), 156 (9 Sep), 157 (24 Sep), 158 (7 Oct), 159 (22 Oct), 160 (4 Nov) [V:maven]; Mozilla: "moving to a two-week release cycle", first release Firefox 155 on 1 Sep [V:moz-sumo].
  - Android floor 8.0 from 144, 32-bit x86 dropped, 32-bit ARM stays (2025-09-15) [V:moz-android]; I found no later announcement [U].
  - No API-stability policy found. Each bump costs work: GeckoView 160 needs compileSdk 37 and androidx.core 1.19, which we pin to 1.17 on AGP 8.13 [V:repo].
  - Forks: Mull ended with DivestOS [S:mull]; IronFox is AGPLv3 [V:ironfox] and, per snippets, builds arm/arm64/x86_64 and adds HTTPS-only, Fission, memory tagging and removes sponsored content [S]; Fennec and Iceraven not verified. They are apps that rebuild Gecko; none offers a third-party GeckoView AAR that I found [U]. Useful only as pref ideas (GECKO_SPEED).
- **Servo.** v0.8.0 AAR is aarch64-only; JIT is re-enabled only on 64-bit Android because the SpiderMonkey bug "affects only 32-bit systems"; README: "prototype web browser engine", "developed on 64-bit targets"; Servo Book: embedding "still ongoing"; recent monthly posts still announce CSS Grid and `<progress>` [V:servo-rel,servo-pr,servo-gh,servo-book,servo-org].
- **WPE WebKit (Igalia)** has WPEView on Maven Central, "arm64 and x86_64", scripts "only tested in Linux" [V:wpe]. **Ladybird:** first alpha for Linux/macOS only [V:ladybird]. No other WebKit port for Android found [U].

## 4. Performance on low-end hardware (question 3)
- **Real data that exists:**
  - None comparing Gecko and Chromium on Cortex-A53 / 2 GB (searched Speedometer, JetStream, MotionMark, Fire HD 8 = MT8168, Moto G5, Redmi 9A) [U].
  - Speedometer 3.0, Feb 2025: Galaxy S23 Firefox 135 13.6 vs Chrome 132 12.2; Mi 11 Lite 5G Chrome 7.88 vs Firefox 6.69 [V:bench]. No consistent winner.
  - Chrome's "Speedometer doubled since M112" post: measured on a Pixel Tablet; build optimisations "account for more than half"; 64-bit Arm instead of 32-bit, -O2/-O3 instead of -Oz, PGO; "memory and disk size constraints on entry-level devices resulted in Chrome having to prioritize a small binary size" [V:goog-blog].
  - Chromium's low-end experiments carry a TODO to "make the feature not enable on 32-bit devices" before launch, so 32-bit is a special case there, not the tuning target [V:cr-sys; U].
  - Mozilla measures Fenix on a Moto G5 and caught a 128 ms cold-start cost from the GPU process there, fixed in Firefox 99 [V:moz-gpu].
  - The SoC is 4x A53 up to 2 GHz, 12 nm [S:nbc]. Notebookcheck's JS scores are not comparable across devices (MT8168 Octane V2 4169 vs about 37,000 listed for a Helio G25 phone), so I did not use them [S:nbc; U].
- **Process models**

| | Chromium (Cromite or Chrome-class) | GeckoView 160 as Kestrel G configures it |
|---|---|---|
| Processes | browser + GPU + utility + one renderer per site instance; Cromite's strict origin isolation adds one per origin and cross-site iframe; spare renderer while the NTP shows [V:cr-proc] | app/parent (also runs extension JS) + 1-2 content + GPU + utility; Fission off, so most content shares a process; 1 preallocated [V:moz-proc; GECKO_SPEED sec 3] |
| Cap | Android sets no soft limit and "relies on the OS to discard processes" [V:cr-proc] | `dom.ipc.processCount` (we use 1-2) [V:GECKO_SPEED] |
| Low-memory mode | only at <= 1 GB RAM; this tablet is above [V:cr-sysu] | `lowMemoryDetection`, `setActive(false)` on hidden tabs [V:GECKO_SPEED] |
| Measured on tablet | no | no |

- **Startup, scrolling, jank:** no data for either engine on this SoC. Chrome pre-spawns a renderer during the NTP; GeckoView's counterpart is `warmUp()` plus an early session (GECKO_SPEED). Hardware WebRender on this GPU is unverified. GPU identity is inconsistent: the hardware file and Notebookcheck say Mali-G52 (MC2 vs MP1) [V:hw; S:nbc], TASKS mentions PowerVR GE8320; read `GL_RENDERER` on the tablet [U].
- **Ad-block efficiency:** the engine is not the lever, the request diet is. R5 measured on 13 real pages: blocking cut requests 78 %, bytes 49 %, median load 14 s to 2 s (Chromium, fast link) [V:R5]. Cromite's C++ engine skips our per-request extension hop; ours (in-extension JS) is estimated at 25-50 us/request on an A53 once D1 lands [E:R5]. Both read EasyList/EasyPrivacy-class lists; ours is planned to add uBO-class scriptlets and cosmetics [V:R5], Cromite has snippets plus a user-script cosmetic layer [S].
- **Which engine is snappier here, and how sure** [U]:
  - Gecko >= Cromite-with-defaults, about 60 %: the JIT-off penalty (40 %-class on JS [V:v8-jitless]), forced site isolation on a 1.9 GB device, a 232 MB footprint, and no premium-build gains for 32-bit entry-level devices [V:goog-blog].
  - Cromite with JIT on vs Gecko, about 50 %: V8 is usually ahead on pure JS, but Gecko won one of two phones in the only comparison found [V:bench]. One 2 GB A53 phone owner calls Cromite smooth [S:c-3058].
  - Only the section 7 A/B settles it.

## 5. What adopting each would cost and gain (question 4)
| Option | Work | APK / disk | Risk | Update path and patch cadence | Gain |
|---|---|---|---|---|---|
| Stay on GeckoView + GECKO_SPEED | built; P0-P1 tuning about 1-2 days [E] plus a toolchain bump per Mozilla major [V:repo] | 92 MB APK, ~135 MB extracted | Mozilla API churn; 32-bit address space; no tablet numbers yet | Actions rebuild, owner taps update; Mozilla ships every 2 weeks [V:moz-sumo]; suggest monthly plus out-of-band for in-the-wild advisories [U] | works today (claude.ai) |
| Cromite as second app | 0.5 day (intent, menu, test) [E] | +0 to our APK; 232 MB disk; 232 MB per Cromite update over the hotspot | stale patches (M153 vs 157), single maintainer, JIT off, one more privacy surface | Cromite's own updater / F-Droid repo, not ours | escape hatch for Gecko-hostile sites; Chromium compatibility |
| Brave as second app | 0.5 day [E] | 221 MB | bloat (Rewards/Wallet/AI), minSdk unverified | its own updater [U]; Chromium 157 now [V:brave-rel] | fresher security patches |
| Embed Chromium (self-build) | months, a team-sized job [E]; runner >= 16 cores / 64 GB / 300 GB [E] | ~160-230 MB | rebase every 2 weeks; Cromite shows a lone maintainer already lags | none on free runners | JIT-on V8, Blink compatibility |
| Sideload Google WebView (only if a second provider is allowed) | 1 hour to test | 0 (`:app` stays 2.3 MB) | shared system component; Trichrome pair; min-SDK drift | manual APKMirror installs | would make the 2.3 MB build viable |
| Servo, WPE, Ladybird | n/a | n/a | not viable on 32-bit | n/a | n/a |

### 5.1 Cheap way to offer Cromite without embedding it
```kotlin
val i = Intent(Intent.ACTION_VIEW, uri).setPackage("org.cromite.cromite").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
try { startActivity(i) } catch (e: ActivityNotFoundException) { /* offer the download page */ }
```
- Menu entry "Open in Cromite" shown only when the package resolves; on Android 11+ add `<queries><package android:name="org.cromite.cromite"/></queries>` (package visibility, [U] not fetched). Package id [V:c-gn]. Zero-coupling variant: `Intent.createChooser` excluding Kestrel, so any installed browser works.
- Install: the owner downloads `arm_ChromePublic.apk` (232 MB) from the GitHub release or the F-Droid repo `https://www.cromite.org/fdroid/repo/?fingerprint=49F37E74DEE483DCA2B991334FB5A0200787430D0B5F9A783DD5F13695E9517B` [V:c-readme]; our downloader already confirms big files on metered links. Cromite then updates itself.
- Caveats: no shared cookies/history/Shields with Kestrel; Cromite's JIT-off default (flip per site or in flags) and the stale M153 patch level; label it "for sites that fail in Kestrel G". Per-domain auto-suggestion is optional later.

## 6. Recommendation (question 5)
1. **Stay on GeckoView; do not switch.** Embeddability is the deciding fact (nothing else can run under our UI on 32-bit Android 10); Mozilla confirms 32-bit ARM stays; Maven publishes armeabi-v7a every 2 weeks; nothing found shows another engine would be faster on this tablet. Keep the 2.3 MB WebView build only as a fallback (it cannot render claude.ai [V:BRIEF]).
2. **Hybrid, cheaply:** after the A/B, add "Open in Cromite" (5.1) if Cromite proves useful on sites where Gecko fails; offer Brave as the fresher option if the owner values patches over hardening. No embedding work.
3. **Engine-independent ways to get snappier (what the comparison revealed):**
   - (a) Request diet first: finish R5 D1, decide in the extension, no UI-thread hop (R5: 14 s to 2 s median).
   - (b) Leave JIT on; never copy "JIT off" hardening onto 4x A53 [V:v8-jitless].
   - (c) Premium-build tricks do not reach 32-bit entry-level devices [V:goog-blog], so app-side wins dominate.
   - (d) Memory is speed on a Go device: Fission off, 1-2 content processes, `setActive(false)`, cap decoded images (GECKO_SPEED); Chromium shows what happens with no cap [V:cr-proc].
   - (e) Warm start like Chrome's spare renderer: `warmUp()` after the first frame plus preconnect (GECKO_SPEED).
   - (f) Both engines use Android's MediaCodec for H.264, so video is not a differentiator; keep AV1 off [V:c-gn; GECKO_SPEED].
   - (g) A Readability-style light mode for article pages and per-site JS/image off [U].
   - (h) Delta APK updates (bsdiff applied on-device) would cut the 92 MB per update over the hotspot [U].
4. **Confidence:** engine choice HIGH (about 90 %), based on verified embedding limits; speed ranking LOW-MEDIUM (about 60 % Gecko >= Cromite-defaults) because nothing was measured on the tablet.
5. **What would change the answer:**
   - The A/B shows Gecko clearly worse on the owner's top sites (for example > 2x time to usable, or repeated low-memory kills): make the Cromite/Brave bridge prominent, still not embedded.
   - Developer options lists a second WebView provider: sideload Google WebView, and the 2.3 MB `:app` build may beat Gecko on RAM.
   - Mozilla announces the end of armeabi-v7a or a minSdk above 29: promote the Chromium bridge to plan A.
   - A maintained, embeddable Chromium for arm32 appears (none exists today), or Servo ships armv7 with a 32-bit JIT and a stable embedding API and renders the owner's sites.
   - Chrome/Cromite raise their floor past Android 10: this tablet would be stranded on the Chromium side anyway [U].

## 7. Open questions to test on the tablet (30 minutes, no adb needed unless noted)
1. Settings > Developer options > "WebView implementation": which providers are listed? (Free; decides whether the Google-WebView route is open.)
2. Install `arm_ChromePublic.apk`: does it install and start on this image? Install size in Settings > Apps? Cold start by stopwatch.
3. Run the GECKO_SPEED sec 4 owner script on Kestrel G and on Cromite: claude.ai login, Google search + first result, a heavy news article (10 screens), YouTube 480p for 60 s, tab switching. Second Cromite run with Site settings > JavaScript JIT = Allowed for those sites.
4. RAM: Developer options > Running services for Kestrel G vs Cromite with 2 tabs open; note reloads or kills when switching apps. With adb: `dumpsys meminfo` per process. In Cromite try `chrome://process-internals` to count renderers on a news page; compare with the Kestrel Diagnostics page process list.
5. Does Kestrel's `ACTION_VIEW` + package `org.cromite.cromite` open a new Cromite tab without extra prompts? Any first-run or "enable updater" dialogs?
6. Diagnostics page: `GL_RENDERER` (resolve Mali-G52 vs PowerVR), hardware WebRender on or off, codec matrix (GECKO_SPEED).
7. If Brave is considered: try `BraveMonoarm.apk`; if it refuses to install, its minSdk excludes Android 10.

## 8. Not verified (read this before relying on the report)
- Anything measured on the tablet: RAM, speed, startup, scrolling, thermal, battery, for either engine. No public Gecko-vs-Chromium numbers on A53 / 2 GB were found.
- Cromite: real minSdk and install/run on this image (cromite.org returned 503; F-Droid index not read); installed size; Custom Tabs provider behaviour; whether `is_high_end_android` changes arm32 code; RAM beyond one anecdote; cosmetic and scriptlet coverage of its engine (only a docs mirror and 2023-2024 issues were read).
- Whether the tablet's `config_webview_packages` lists anything besides `com.android.webview` (the Chromium doc says non-GMS Q lists only that name; the image is a MediaTek BSP).
- Google WebView / Brave 32-bit minimum Android in late 2026 (APKMirror 12L labels are unverified); Vivaldi, Samsung, Opera, Edge arm32 status.
- GeckoView: API-stability policy, third-party support stance, any 2026 change to armeabi-v7a; IronFox / Fennec / Iceraven maintenance; Mull's exact end date.
- Chromium 157/158 dates come from Brave's release, a Wikipedia infobox snippet and Cromite's bot issues; the Chrome milestone schedule is the announced one, not a live feed.
- Single-source items: the 24-core Chromium build time [S:cr-time]; the Redmi 9A comment [S:c-3058]; Notebookcheck SoC data [S:nbc]; licences marked [U] are from memory.

## 9. Sources (key = URL; ".../" repeats the previous base)
- Cromite: c-readme=https://raw.githubusercontent.com/uazo/cromite/master/README.md · c-rel=https://github.com/uazo/cromite/releases (assets via .../releases/expanded_assets/<tag> for v145.0.7632.120, v147.0.7727.56, v148.0.7778.168, v153.0.8010.37) · c-faq=https://raw.githubusercontent.com/uazo/cromite/master/docs/FAQ.md · c-patch=.../docs/PATCHES.md · c-gn=.../build/cromite.gn_args · c-sip=.../build/patches/Enable-StrictOriginIsolation-and-SitePerProcess.patch · c-wf=.../.github/workflows/build_cromite.yaml · c-commits=https://github.com/uazo/cromite/commits/master
- Cromite issues: c-3040=https://github.com/uazo/cromite/issues/3040 · c-3058=.../issues/3058 · c-3109=.../issues/3109 · c-3117=.../issues/3117 · c-3118=.../issues/3118 · c-347=.../issues/347 · c-1395=https://github.com/uazo/cromite/discussions/1395
- Android/Chromium: aosp-wvu=https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android10-release/services/core/java/com/android/server/webkit/WebViewUpdater.java · cr-prov=https://chromium.googlesource.com/chromium/src/+/main/android_webview/docs/webview-providers.md · cr-build=.../docs/android_build_instructions.md · cr-lin=.../docs/linux/build_instructions.md · cr-proc=.../docs/process_model_and_site_isolation.md · cr-sysu=https://chromium.googlesource.com/chromium/src.git/+/main/base/android/java/src/org/chromium/base/SysUtils.java · cr-sys=.../base/system/sys_info.cc · cr-gni=.../build/config/chrome_build.gni
- Chromium discussion: cr-torne=https://groups.google.com/a/chromium.org/g/android-webview-dev/c/apA8HXJpGr0/m/Ug_WvgktAAAJ · cr-psa=https://groups.google.com/a/chromium.org/g/chromium-dev/c/vEZz0721rUY · cr-time=https://groups.google.com/a/chromium.org/g/Chromium-dev/c/qozC-B_cips
- Speed/JIT: goog-blog=https://blog.google/chromium/doubling-speedometer-scores-android/ · chrome-2wk=https://developer.chrome.com/blog/chrome-two-week-release · v8-jitless=https://v8.dev/blog/jitless · edge-jit=https://learn.microsoft.com/en-us/deployedge/microsoft-edge-policies/defaultjavascriptjitsetting · bench=https://gracelandtoo.github.io/browser_bench/ · nbc=https://www.notebookcheck.net/MediaTek-MT8168-Processor-Benchmarks-and-Specs.483534.0.html
- GitHub limits: gh-runners=https://docs.github.com/en/actions/reference/runners/github-hosted-runners · gh-limits=https://docs.github.com/en/actions/reference/limits · gh-free=https://docs.github.com/en/actions/administering-github-actions/usage-limits-billing-and-administration
- Mozilla: maven=https://maven.mozilla.org/maven2/org/mozilla/geckoview/geckoview-armeabi-v7a/maven-metadata.xml · moz-android=https://blog.mozilla.org/futurereleases/2025/09/15/raising-the-minimum-android-version-for-firefox/ · moz-sumo=https://blog.mozilla.org/sumo/2026/08/19/firefox-new-release-cadence-and-what-to-expect/ · moz-gpu=https://bugzilla.mozilla.org/show_bug.cgi?id=1756700 · moz-proc=https://raw.githubusercontent.com/mozilla-firefox/firefox/main/dom/docs/ipc/process_model.md · gv-docs=https://firefox-source-docs.mozilla.org/mobile/android/geckoview/index.html · ironfox=https://gitlab.com/ironfox-oss/IronFox
- Others: brave-rel=https://github.com/brave/brave-browser/releases (assets .../expanded_assets/v1.99.56) · brave-wiki=https://github.com/brave/brave-browser/wiki/Installing-Brave-on-Android · servo-rel=https://github.com/servo/servo/releases/expanded_assets/v0.8.0 · servo-pr=https://github.com/servo/servo/pull/32284 · servo-gh=https://github.com/servo/servo · servo-book=https://book.servo.org/ · servo-org=https://servo.org/ · wpe=https://github.com/Igalia/wpe-android · ladybird=https://ladybird.org/ · anywebview=https://github.com/neoblackxt/AnyWebView · vanadium=https://raw.githubusercontent.com/GrapheneOS/Vanadium/main/README.md
- Secondary: apkmirror=https://www.apkmirror.com/apk/google-inc/android-system-webview/android-system-webview-140-0-7339-208-release/android-system-webview-140-0-7339-208-2-android-apk-download · crosswalk=https://medium.com/phonegap/a-fond-farewell-to-the-crosswalk-project-e8ef2b2dfcbc · kiwi=https://alternativeto.net/news/2025/1/kiwi-browser-discontinued-explore-alternatives-for-extension-support-and-security · bromite=https://alternativeto.net/software/bromite/about · mull=https://forum.f-droid.org/t/divest-os-apps-discontinued/29792 · eos=https://community.e.foundation/t/which-webview-does-e-os-use/62396
- In this repo: hw=`docs/hardware/hardware_.txt` (user build, release-keys, locked bootloader, verified boot green) · repo=`geckoapp/build.gradle.kts` · BRIEF, R5=`research/ADBLOCK_ENGINE.md`, GECKO_SPEED=`research/GECKO_SPEED.md`.
