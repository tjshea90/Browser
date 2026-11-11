# ADBLOCK_ENGINE - research R5: the best blocking engine for Kestrel G (decision first, then evidence)

Written 2026-11-11 by a research agent; no repo file other than this one was touched. Tags: **[V]** verified this session (source read, program run, page fetched), **[E]** my estimate, **[N]** not verified (section 11). Sizes are MiB (1024^2 bytes) unless a source uses its own unit.
Evidence stayed in the session scratchpad, outside the repo: shallow clones of uBlock (2bc679d), uAssets (bafdbd3), ghostery/adblocker 2.20.3, brave/adblock-rust 0.13.5, brave/adblock-resources, Hagezi, AdguardFilters; GeckoView 160 `classes.jar` (javap) and unpacked omni.ja; a JS port of our matcher; Playwright crawls. Downloaded code was read, never executed.
Map to the brief: question 1 = sections 4 and 7; 2 = section 6; 3 = sections 1, 2 and 8; 4 = sections 5 and 10; 5 = section 9; 6 = section 3 (TASKS D1 = P0, D2 = P1, D3 = P1 harness + policies, D4 = P0 tests).
**In plain words:** most of the slowness on ad-heavy sites is not the ads you see but 80-100 hidden connections per page (header-bidding auctions, cookie-sync pixels, tag managers, consent scripts). On 13 real pages, blocking them cut requests by 78%, bytes by 49% and the median load time from 14 s to 2 s (Chromium, fast link, section 5). The freeze and the slow blocking share one cause: today every request makes a round trip to the Android UI thread.

## 1. Decision

1. **Decide every request inside the extension, in plain JS over typed arrays (option b). Delete the per-request native message.** [V] Cause of the jank: `WebExtension$Listener` registers `GeckoView:WebExtension:Message|PortMessage|Connect|Disconnect` with `EventDispatcher.registerUiThreadListener`, so each `sendNativeMessage` is served on the Android UI thread while Gecko holds the request.
   - `ShieldsBridge` then does JSON, pref reads, `Log.d` and, per BLOCKED request, `updateUi()` -> `Toolbar.bind()` (2x `setImageResource`, text, layout). Ad-heavy pages block 100-1,000 requests per load (section 5).
   - research/FREEZE.md reaches the same diagnosis and files this under P2; this report says P0 (about 6.5 days, section 3). Its worker-thread bridge is the stop-gap if D1 slips; delete it when D1 lands.
2. **One compiler, two readers.** `:core` (parser, builder, canaries, PSL) stays the only list compiler. Add a little-endian, 4-byte-aligned `KFS2` writer so the extension maps sections as `Int32Array`/`Uint32Array` views (zero copy).
   - Prototype [V] (scratch, not committed): a 250-line JS port reads today's UNCHANGED `default.kfs` and returns identical verdict words (block, category, `$important`, exception) to the Kotlin engine on **156,731 requests, 0 mismatches**: 141,443 derived from the real lists (rule text turned into URLs, mutated near-misses, 18 hand-written real URLs) plus 15,288 requests recorded from real pages (real Public Suffix List).
   - Load 7 ms (big-endian byte swap; KFS2 removes it), 2.5 MiB of typed arrays for the network part, **5.2 us/request average on x86 V8** (35 us for an ad-like URL that makes the Java replica evaluate ~140 rules; <= 0.9 ms for crafted 2 KB URLs). [E] A53 + SpiderMonkey is 5-10x slower: 25-50 us average, still far below a UI-thread round trip under load.
3. **Rejected: declarativeNetRequest.** [V] Gecko 160 allows 30,000 static rules per extension (`extensions.dnr.guaranteed_minimum_static_rules`; the extra global pool is a commented-out TODO, bug 1803370), 5,000 dynamic, 1,000 regex, and matches by a linear JS scan of every rule per request (`ExtensionDNR.sys.mjs`, `#collectMatchInRuleset`). We compile ~120,000 network rules.
4. **Rejected as the default engine: bundling uBlock Origin.** It is the quality ceiling, so use it as (i) the source of scriptlets and neutered redirect resources (GPLv3, pinned release), (ii) a parser oracle in tests, (iii) a one-day "APK with uBO built in" spike to measure memory and start-up on the tablet.
   - Against: needs tab ids, `tabs.*` events, `setTabActive`, popup hosting through `ActionDelegate` (none wired in our app); seconds of compile/load with requests suspended (desktop: ~4 s first compile, ~1 s load, Mozilla bug 1651434); its own updater and UI; no hook for our Shields panel and counters; unknown memory on 2 GB (section 8).
5. **Brave's engine (adblock-rust to WASM) is a P2 spike, not P0.** Its CI builds `wasm32-unknown-unknown` but there are no maintained JS bindings; its flatbuffer engine costs "about 15 MB for ~100k filters" [E, from Brave's "-75%, ~45 MB saved"], ours 2.5 MiB for 120k. Re-open if our own cosmetic/scriptlet work misses the diff targets of section 3.
6. **Keep the Kotlin engine** for the WebView build, main-frame/malware checks (`NavPolicy`), popup checks and `isPageAllowed`.
7. **Process model.** Keep `extensionsProcessEnabled(false)` for P0: the JS then runs on the Gecko parent main thread, not the Android UI thread. Risk [V]: Mozilla calls this mode "not really supported" (bug 1880856, Rob Wu) and links blank extension pages and a file-input crash to it. Ship FREEZE.md's hidden A/B switch "extensions process ON" (the blocking callback then costs a parent<->extension IPC, [E] +0.2-1 ms, but no parent-thread JS). The engine code is identical either way; decide with tablet numbers.
8. **Lists (section 6).** Network coverage is EasyList + EasyPrivacy. Add uBO `quick-fixes`, `resource-abuse`, `annoyances-cookies`, `privacy-removeparam` (their payload is scriptlets/redirects/removeparam, not hosts), **AdGuard Tracking Protection** and **Hagezi Light**: together +4.2 points of blocked requests on 22 real pages (78.5% -> 82.7%) for 180k hosts + 30k patterns. Skip Hagezi Pro, OISD, Peter Lowe, URLhaus-URL, AdGuard Base (its exceptions unblock requests). Build the snapshot in GitHub Actions (canaries first); devices download ONE ~2-3 MiB gzip instead of fetching and compiling 7+ lists.
9. **Video-site ads (section 9).** Needs scriptlets (json-prune family, trusted-replace-*, set-constant) plus HTML filtering. Ship as a labelled Settings switch whose text says YouTube's terms forbid circumventing its ads and YouTube may limit playback. The owner decides the default (Brave and uBO ship it on).
10. **Licence.** Recommend GPLv3-or-later for the repo: uBO's scriptlets are GPLv3, EasyList is GPLv3+/CC BY-SA 3.0, uAssets, Hagezi and AdGuard are GPLv3, GeckoView/Ghostery/Brave are MPL-2.0 (GPL-compatible). Record every licence in `docs/FILTER-LISTS.md` (section 6). Owner decisions needed: licence, video-ad default, "Aggressive" mode default (off), hosting the snapshot as a GitHub Release asset (GitHub sees the device IP weekly, like any list host today).

## 2. Target architecture (Kestrel G)

```
Kotlin app (UI process)                              kestrel-shields v2 (built-in MV2 extension, persistent background page)
 Prefs + SiteShieldStore --- Port, JSON text, rare --> settings: global flags + per-host mask/values (replaces Shields.effective)
 FilterUpdater (conditional GET of CI snapshot) ----> snapshot bytes (bundled copy first; chunked Port or file:// - P0.3 spike)
 Tab counters <--- <= 1 msg/s per tab with changes --- engine.js: KFS2 views; check(url, topHost, type) -> verdict (synchronous)
 Kotlin FilterSet: NavPolicy, popups, malware          webRequest.onBeforeRequest {blocking, types without main_frame} -> {cancel} | {redirectUrl}
                                                       cosmetics: contentScripts.register({css, cssOrigin:'user', runAt:'document_start'}) per host
                                                       scriptlets: contentScripts.register({world:'MAIN', runAt:'document_start', matchOriginAsFallback})
```
- **Per-request path touches only the extension.** Page host comes from `webNavigation`/`frameAncestors` into a per-tab record; `check()` returns `{cancel:true}` synchronously (no promise, no message). Counters are typed-array increments flushed once a second; the toolbar chip updates at most 4x/s.
- **Cosmetic timing [V schema]:** Gecko 160's `contentScripts.register` accepts `css`, `cssOrigin`, `world`, `matchOriginAsFallback`, so per-host CSS and scriptlets can be pre-registered (as uBO does for scriptlets on Firefox >= 128) and run at `document_start` without a round trip. First visit to a host races the registration: uBO falls back to `tabs.executeScript` (`manuallyInject` in `vapi-scripting.js`); test first and repeat visits.
- **Main-thread discipline [E]:** the extension runs on Gecko's parent main thread (decision 7), so no task over ~8 ms: build typed-array views instead of parsing, decode cosmetic text lazily per host, assemble scriptlet code on first use per host and cache it.
- **KFS2** emits what the builder skips today: generic cosmetics as class/id hash sets, per-host scriptlet calls (name id + args), redirect and removeparam side tables, procedural selectors, HTML filters. 64-bit hashes stay (hi/lo `Int32Array` pairs sorted signed-hi/unsigned-lo; my port shows FNV-1a + murmur finaliser is exact with 32-bit limbs). Header: magic, version, section table, CRC32 verified on download only.
- **Update path.** A scheduled workflow (separate from the APK workflow) builds `kestrel-lists.kfs2.gz` + sha256 + manifest, runs `DefaultListsRegressionTest` and the differential corpus, publishes a rolling release asset; the app does a conditional GET weekly (metered rules unchanged), swaps atomically, keeps the bundled copy as fallback. [V] `default.kfs` 4.2 MiB -> 2.0 MiB gzip; the 15 raw lists are 7.5 MiB -> 2.26 MiB gzip plus ~0.8 s x86 (est. 6-10 s on the A53) of compile the tablet no longer does.
- **Pitfall [V]:** `ensureBuiltIn` reinstalls only when `manifest.json` `version` differs (`GeckoViewWebExtension.sys.mjs`); ours is a constant "1.0". Generate the version from the build or a changed `background.js` never reaches existing installs.
- **Private tabs [V]:** built-in = privileged, and `_setupStartupPermissions` auto-grants `internal:privateBrowsingAllowed`, so the extension sees private requests. Keep a test.
- **Trust model:** only lists built by our CI may call `trusted-*` scriptlets (uBO and AdGuard use the same rule); user lists never.

## 3. Staged plan, effort, tests (days = one Claude session-day, no emulator)

| Stage | Work | Days |
|---|---|---|
| **P0** | P0.0 Kotlin relief that does not wait: coalesce `shieldCountChanged`, drop per-block `Log.d`, cache `Shields.effective` per host | 0.5 |
| P0 | P0.1 `KFS2` writer + reader in `:core`/`tools`; v1 stays readable; golden test: v1 and v2 verdicts equal on the corpus | 1 |
| P0 | P0.2 `engine.js` + new `background.js`: sync listener, per-tab state, Port settings, batched counters, diagnostics (handler p50/p95 us, snapshot load ms); manifest version bump | 2 |
| P0 | P0.3 Snapshot transport spike (bundled / chunked Port / `file://` fetch) and weekly update; bundled fallback | 1 |
| P0 | P0.4 Tests (below) wired into the existing `test-js` job | 1.5 |
| P0 | P0.5 Alpha + Diagnostics page (A5); owner checks typing and claude.ai. **Exit:** no `sendNativeMessage` per request; handler p95 < 0.3 ms on the tablet; 0 mismatches; FREEZE.md stress page no worse than engine-off | 0.5 |
| **P1** | Generic cosmetics from a class/id index (94.8% of 29k selectors are plain `.class`/`#id`), survey in the content script with self-shutdown (uBO: 256 misses/5 min) | 3 |
| P1 | `$redirect`/`redirect-rule` with 16 neutered resources, `$removeparam` (simple names), `$denyallow`/`$to`/`$from`, entity domains (1,463 rules skipped today); better bucket tokens (uBO bad-token list) to cut the ~140 rule evaluations of an ad URL | 5 |
| P1 | Scriptlets MVP: vendor uBO resources at a pinned tag; the 22 scriptlets of section 7 (92.6% of rules) + YouTube pack | 6 |
| P1 | CI snapshot workflow, new lists, Shields levels (Standard / Aggressive / Strict), click-to-load for embeds | 6 |
| P1 | Real-page A/B harness in CI (section 10) | 3 |
| **P2** | Procedural cosmetics (`:has-text` 1,007, `:upward`, `:matches-css`, `:xpath`, `:remove`), HTML filtering (~450 rules, `filterResponseData`), `$csp` (101), CNAME via `dns.resolve`, `Diff-Path` updates, adblock-rust WASM spike, uBO-in-APK spike | 20 |

**Test strategy.**
1. *Differential*: a `:tools` Kotlin task emits `corpus.tsv` (url, type, page, verdict) from the real lists plus the 60k-case metamorphic generator; `web-tests/engine.test.js` (`node --test`) must match 100%. Scratch versions of both halves exist and passed.
2. *Node unit tests*: hash limbs, glob, party/type/domain, exceptions, `$important`, crafted URLs (MAX_URL, userinfo padding: my port skipped the Kotlin >2048-character handling, copy it), bounded-work assertions.
3. *Parse-level oracle*: run uBO's `AstFilterParser` (pinned, test time only) over every list line; our parser must classify it the same or skip it with a documented reason.
4. *Scriptlets*: one fixture page per scriptlet in desktop Firefox, first and repeat visit. 5. *Real pages*: section 10. 6. *On device*: A5 shows handler us, snapshot load ms, counts.

## 4. How the best engines work (evidence)

- **uBO static filtering [V source]** (`static-net-filtering.js`, 6,124 lines):
  - All filters live in one `Int32Array` arena; pure hostnames in `HNTrieContainer` (reversed hostnames, typed-array compressed trie, WASM-able matcher), plain patterns in a `BidiTrie`.
  - Each filter is indexed by the best token (max 7 chars) chosen by "badness" from the top-100 "bad tokens" histogram of 200k+ benchmark URLs; buckets with >= 3 members are optimised into tries.
  - Precedence `important` > `@@` > block; `$badfilter` removes the exact filter; `redirect`, `csp`, `removeparam`, `permissions`, `urlskip`, `replace` are separate "realms" with priorities.
  - A serialized "selfie" (LZ4) avoids recompiling at launch; requests are suspended until lists load by default (the wiki warns that disabling this lets first-page requests through). `@gorhill/ubo-core` 0.1.30 (GPL-3.0-or-later, "API subject to change at any time") packages only this engine.
- **uBO cosmetics and scriptlets [V source]**:
  - Specific rules by hostname (entity and regex allowed); "lowly generic" selectors keyed by class/id token, found by a DOM surveyor that reports new tokens in batches and shuts itself down after 256 fruitless surveys/5 min; "highly generic" ones always injected; procedural filterer loaded on demand; CSS inserted as user-origin stylesheet; HTML filtering `##^` through stream filters.
  - Scriptlets pre-registered per host with `contentScripts.register({world:'MAIN', runAt:'document_start', matchOriginAsFallback:true})` on Firefox >= 128 (fallback inline script, then blob); `trusted-*` only from trusted lists; CNAME uncloaking with `dns.resolve(host, ['canonical_name'])` (29 `$cname` exceptions in the lists).
  - Updates: `assets.js` reads `Diff-Path`/`Diff-Expires` and `diff-updater.js` (285 lines) applies patches. Defaults [V]: `autoUpdate: true`, first update 37 s after launch, then hourly checks - these must be switched off to keep our weekly-only network rule.
- **uBO syntax semantics [V wiki, source]**: `1p/3p` compare base domains (`strict1p/3p` hostnames); `domain=`/`from=` take `|` lists with `~`, entities, regex; `to=` is a superset of `denyallow=` (which needs `domain=`); `important` ignores `@@`; `csp=` needs an exception with the same text; `redirect=name:priority`; `replace`, `uritransform`, `urlskip` are trusted-only; `header=`, `method=`, `popup`, `all`.
  - Dynamic filtering rows are source/destination/type/action, `allow` > `block` > static exceptions; per-site switches `no-strict-blocking`, `no-popups`, `no-cosmetic-filtering`, `no-remote-fonts`, `no-large-media` (default 50 KB), `no-csp-reports`, `no-scripting`. "Medium mode" = `* * 3p-script block` + `* * 3p-frame block`: "significantly faster", "high likelihood of breakage", no figures given.
- **Brave adblock-rust 0.13.5 [V repo, blog]**:
  - MPL-2.0; network + cosmetic + resources (`use_resources`), `hidden_class_id_selectors`, `url_cosmetic_resources`; `$method`, `$badfilter`, `$important`, `$redirect(-rule)`, `$csp`, `$removeparam` (simple names only); procedural operators parsed; flatbuffer DAT v7; "skip flatbuffer verification for DAT files created and read by the same library version".
  - Brave posts: 69x faster than its old C++ blocker; **5.7 us average per request** on EasyList + EasyPrivacy (55k rules, 242,944 requests, 2018 MacBook Pro i7); v1.85 flatbuffers cut memory 75%, "roughly 45 MB" with ~100k default filters on all platforms, +13% matching speed from tokenizing common regexes.
  - Brave's own resources: 17 files (`brave-youtube-*`, `brave-yt-sabr-fix.js`, `vaft-ublock-origin.js`, `video-swap-new-ublock-origin.js`, `de-amp.js`, `trusted_types.js`).
- **Ghostery adblocker 2.20.3 [V repo]**: MPL-2.0 TypeScript, active (2026-10-28); `serialize()`/`deserialize()` to `Uint8Array`; `to`, `denyallow`, `domain`/`from`, `badfilter`, `important`, `redirect(-rule)`, `csp`, `removeparam` (not regex), `replace`, HTML filters, `+js` with uBO resources, extended selectors. 2019 vendor study: median 7 us/request, 1.8 MB in Chrome [old, vendor]. Source for procedural selectors if we do not write our own; not adopted wholesale because it would replace our tested matcher and canaries.
- **AdGuard [V pages]**: libraries and filters GPL-3.0 (tsurlfilter, tswebextension, Scriptlets, agtree). Extra syntax: `$cookie`, `$hls`, `$jsonprune`, `$xmlprune`, `$removeheader`, `$referrerpolicy`, `$stealth`, `$network`, `$app`, `$dnstype`, Extended CSS; scriptlets accept uBO names; trusted scriptlets limited to approved sources.
- **Mobile numbers.** Published: Brave -45 MB; Ghostery 1.8 MB (2019). uBO: no current memory figure found [N]; Mozilla bug 1651434 (desktop, 2020) ~3.5 s init; bug 1428573 (Fennec, 2018) 12.1 s start-up with uBO + HTTPS Everywhere vs 2 s bare. Mine [V]: network arrays 2.5 MiB, 5.2 us/request, 7 ms load on x86.

## 5. Real-page measurements [V, indicative only]

Headless Chromium (no Firefox in the sandbox), datacenter IP, one pass. 30 pages crawled, 8 bot-walled (< 15 requests) dropped; 22 left = 15,288 sub-resource requests, 91.4 MiB. Network rules only (no cosmetics, no scriptlets).
- **Today's bundled lists block 78.5% of requests and 36.9% of bytes; distinct hosts contacted per page fall from 103.5 to 18.5** (each host is DNS + TCP + TLS on a flaky hotspot). Blocked: images 6,996 (9.2 MiB), scripts 887 (19.3 MiB), sub-frames 2,894, fetch/xhr 1,065. Biggest pages: usatoday 3,479 requests (timed out), yahoo 2,577, weather 2,022, buzzfeed 1,958, cnet 1,391, webmd 1,132, foxnews 891.

A/B on 13 pages, same browser, `page.route` aborts what the JS port blocks (the CI job's shape):

| Site | Requests off -> on | MiB off -> on | Load event s off -> on |
|---|---|---|---|
| theguardian | 209 -> 104 | 2.7 -> 1.2 | 14.0 -> 2.2 |
| foxnews | 562 -> 159 | 15.0 -> 11.1 | 18.2 -> 5.5 |
| cnet | 945 -> 85 | 8.5 -> 2.3 | 25.0 -> 1.5 |
| buzzfeed | 907 -> 69 | 6.5 -> 3.2 | 24.1 -> 2.8 |
| usatoday | 777 -> 30 | 4.7 -> 0.3 | 16.1 -> 1.0 |
| weather | 1,183 -> 182 | 12.6 -> 5.0 | 21.4 -> 1.9 |
| yahoo / webmd | 1,358 / 611 -> 173 / 80 | 7.0 / 5.3 -> 2.6 / 2.3 | not finished in 40 s -> 3.2 / 2.1 |
| walmart (first-party heavy) | 318 -> 310 | 3.5 -> 3.3 | 5.0 -> 5.3 |
| **13 pages** | **7,540 -> 1,638 (-78%)** | **87.5 -> 44.8 (-49%)** | median 14.0 -> 2.2; last network activity x0.28 |

Blocking the first loader script prevents the cascade (cnet: 8 requests aborted, 860 fewer issued). Reddit's bytes rose (3.8 -> 5.3 MiB): run-to-run noise, expect +-30% on one pass. On a hotspot the gap should widen [E].

**What is left after the lists** (3,249 requests, 57.7 MiB): third-party 2,054 requests / 28.4 MiB; images 15.9 MiB; scripts 21.6 MiB; **media 10 requests = 7.0 MiB (12%)**; **web fonts 58 requests = 2.4 MiB (4%)**; consent managers (`cookielaw.org` 33 requests/450 KB, `ketchjs.com` 346 KB), Google IMA SDK 445 KB, Tag Manager 338 KB, JW Player 313 KB, Disqus 72 requests, `krushmedia.com` cookie-sync 222 requests (not in EasyList).

## 6. Lists: coverage per MiB, licences, update cost

Marginal effect on the same 22 pages (network rules; `.kfs` includes the lists' cosmetic data):

| List set | Rules host + pattern | .kfs MiB | Blocked requests | Blocked bytes |
|---|---|---|---|---|
| EasyList | 47.6k + 3.9k | 1.4 | 62.2% | 25.5% |
| EasyPrivacy | 47.3k + 9.1k | 1.1 | 20.4% | 11.8% |
| EasyList + EasyPrivacy | 95.0k + 13.1k | 2.6 | 76.9% | 35.8% |
| uBO lists alone (filters*, privacy, badware, unbreak) | 4.1k + 6.0k | 0.8 | 3.5% | 1.7% |
| **bundled set = today's `default.kfs`** | 99.3k + 20.9k | 4.2 | **78.5%** | **36.9%** |
| + uBO quick-fixes, resource-abuse, shorteners, annoyances | 99.5k + 21.2k | 4.2 | 78.5% | 36.9% |
| + EasyList social, annoyances, chat, newsletters, notifications, AI | 100.3k + 22.8k | 5.0 | 78.6% | 37.2% |
| + AdGuard Tracking Protection (Spyware sections) | 106.1k + 30.0k | 5.1 | 80.0% | 37.3% |
| + AdGuard Base (sections) | 100.8k + 31.3k | 6.9 | 78.2% | 36.4% |
| + AdGuard Mobile Ads / Social / Annoyances | ~100k + 21-27k | 4.5-7.0 | 78.5-78.6% | 36.9% |
| + Hagezi Light / Pro | 173.2k / 283.0k + 20.8k | 5.0 / 6.3 | 81.8% / 82.0% | 37.4% / 37.5% |
| + Peter Lowe (3,560 hosts) | 102.5k + 20.8k | 4.2 | 78.8% | 36.9% |
| **recommended: bundled + quick-fixes + abuse + AdGuard Tracking + Hagezi Light** | 180.1k + 30.1k | 6.0 | **82.7%** | **37.7%** |

- Hosts cost 12 bytes each, so size is not the issue; **value** is. Hagezi's extra hosts are DNS-level (app SDK subdomains such as `*.appsflyersdk.com`); on real pages Light's +492 requests are almost all cookie-sync pixels (krushmedia 222, `pbs.yahoo.com/setuid` 47, quantserve 42, copper6 25, plusoneads 21): tiny bytes, many new connections. Pro adds 30 more requests than Light for +110k hosts. AdGuard Base adds exceptions that unblock 0.3 points, so never mix its sections wholesale. DNS lists have no exceptions; build them into the same set so uBO `unbreak` exceptions still win.
- **Brave's default set [V `list_catalog.json`]**: uBO filters + 2020-2026 + general + badware + resource-abuse + unbreak + quick-fixes + link-shorteners, EasyList, URLhaus (online), brave-unbreak/specific/social/android-specific/sugarcoat, EasyPrivacy + uBO privacy, brave-firstparty; ON by default: Fanboy cookie notices + uBO annoyances-cookies + brave-cookie-specific, Fanboy mobile-notifications. Opt-in: annoyances, AI, newsletters, social, chat apps, YouTube Shorts/recommendations/end-screens, TrackParam URL list, regional. uBO's default-on: filters, badware, privacy, unbreak, quick-fixes, EasyList, EasyPrivacy, URLhaus, Peter Lowe.
- **Our gap vs Brave** is not hosts but `quick-fixes`, `resource-abuse`, `annoyances-cookies`, `privacy-removeparam`, the `brave-*` lists and the scriptlet/redirect engine. Cookie notices need scriptlets: `annoyances-cookies` is 2,165 scriptlet rules of ~2,400; EasyList cookies has 2,468 scriptlet vs ~23k cosmetic rules (hiding alone leaves a scroll-locked page).
- **Licences to record**: EasyList family GPLv3+ / CC BY-SA 3.0 (credit "The EasyList authors"; externally hosted subscriptions may differ); uAssets GPLv3; Hagezi GPL-3.0; AdGuard filters and libraries GPL-3.0; brave/adblock-lists MPL-2.0; URLhaus CC0; Peter Lowe "McRae General Public License", non-commercial only (fine for the owner's personal use, and redundant); OISD: its site states none [N]; "I don't care about cookies" [N]. Code: uBO GPLv3, ubo-core GPL-3.0-or-later, Ghostery and adblock-rust MPL-2.0, GeckoView MPL-2.0.
- **Update cost on the hotspot** [V sizes]: raw bundled set 7.5 MiB = 2.26 MiB gzip; CI snapshot 2.0 MiB gzip, +0.6 MiB for Hagezi Light and ~0.5 MiB for AdGuard Tracking [E], ~3 MiB total. EasyList and EasyPrivacy declare `Diff-Path` (patch every 317 min); the other bundled lists do not, and the AdGuard `diffupdates` format would make a weekly refresh cost kilobytes - P2, sizes [N]. `quick-fixes.txt` declares `Expires: 8 hours` and carries the YouTube fixes (86 KB raw): poll it daily, only while the YouTube switch is on.

## 7. Scriptlets, cosmetics, procedural: the data (29 uAssets list files, Nov 2026)

- **16,824 scriptlet rules, 100 distinct names; the 22 below cover 92.6%.** `set-constant` (alias `set`) 1,607; `abort-current-script` (`acs`) 1,538; `abort-on-property-read` (`aopr`) 1,497; `addEventListener-defuser` (`aeld`) 1,005; `no-setTimeout-if` (`nostif`) 945; `remove-node-text` (`rmnt`) 888; `no-window-open-if` (`nowoif`, popups) 813; `abort-on-property-write` (`aopw`) 477; `no-fetch-if` 405; `remove-attr` (`ra`) 277; `no-xhr-if` 245; `abort-on-stack-trace` (`aost`) 212; `no-setInterval-if` (`nosiif`) 185; `adjust-setInterval`/`adjust-setTimeout` (`nano-sib`/`nano-stb`) 168/118; `replace-node-text` (`rpnt`) 165; `noeval-if` 137; `json-prune` 130 (+ `-fetch-response` 51, `-xhr-response` 22).
- Cookie/consent group: `set-cookie` 2,203 (EasyList cookies 1,960), `trusted-set-cookie` 978, `trusted-click-element` 924, `set-local-storage-item` 593, `trusted-set-local-storage-item` 192, `remove-cookie` 73. Others: `href-sanitizer` 76 (privacy list), `trusted-replace-argument` 82, `no-webrtc` 59. About 2.4k rules (14%) use `trusted-*`.
- Scriptlet rules per file: easylist-cookies 2,468; uBO filters 2,426; annoyances-cookies 2,165; annoyances-others 2,141; filters-general 973; filters-2020..2026 543-967 each; unbreak 311; privacy 301; quick-fixes 170.
- **Generic cosmetics: 29,076 unique selectors; 94.8% are plain `.class` (54.8%) or `#id` (40.0%)**, 3.0% compound with a class/id token, only 2.2% (~650) need always-inject. A class/id index covers ~98% and fixes `.adsbygoogle`, which the 1,200-selector prefix misses today.
- **Procedural**: `:has-text` 1,007, `:upward` 173, `:remove()` 123, `:remove-attr` 105, `:matches-css` 43, `:xpath` 30, `:matches-path` 24; `:style()` 1,762 is an action (injected CSS); `:has(` 1,660 and `:not(` 389 are native CSS in Gecko 160. HTML filters `##^`: ~450 rules.
- **Network modifiers** in the same lists: `removeparam` 2,988 (2,622 plain names = 97%, 44 regex, 50 bare); `redirect` 1,001 + `redirect-rule` 472 (16 resources cover 95%: `noopjs` 572, `google-ima.js` 215, `1x1.gif` 132, `nooptext` 91, `noop.js` 89, `noopmp3-0.1s` 82, `noop.txt`, `noopframe`, `noopmp4-1s`, `32x32.png`, `2x2.png`, `fingerprint2.js`, `googlesyndication_adsbygoogle.js`, `nobab2.js`, `googletagmanager_gtm.js`, `noop.html`); `popup` 3,116; `important` 354; `badfilter` 272; `denyallow` 253; `to=` 530; `urlskip` 238; `csp` 101; `replace` 81; `method` 52; `header` 42; `cname` 29.
- Our compiler skips today 14,942 procedural/scriptlet, 2,845 redirect/csp/replace/removeparam/cname/denyallow, 1,463 entity-domain and 597 regex rules (`docs/FILTER-LISTS.md`).

## 8. Options (a)-(d) against GeckoView 160 [V unless tagged]

| | (a) Kotlin + native messaging (today) | (b) JS engine in the extension | (c) declarativeNetRequest | (d) uBlock Origin built in |
|---|---|---|---|---|
| UI-thread work per request | all of it | none | none | none |
| Fit with 120-210k rules | yes | yes | no (30k cap, linear scan) | yes |
| Scriptlets / procedural / redirects | no | yes (P1/P2) | redirects and removeparam only | yes, all |
| Our Shields switches + counters | yes | yes (Port, batched) | partial | no clean hook |
| Memory / start-up on the A53 | known | 2.5-4 MiB, ms load [E tens of ms] | JS rule objects [N] | unknown; seconds to compile/load |
| Testable in Node and desktop Firefox | no | yes | partly | yes (it is an add-on) |

- **APIs present in 160 [V]**: `webRequest` (blocking, `filterResponseData`), `webNavigation`, `dns`, `scripting`/`contentScripts` with `world:MAIN`, `publicSuffix` (no PSL file needed), `storage`, `alarms`, `cookies`, `privacy`, GeckoView `tabs` (incl. `insertCSS`, `executeScript`, `query`), `browserAction`/`pageAction`. Absent: `menus`, `commands`, `windows`, `sidebarAction` (Mozilla's Android differences page).
- **uBO on GeckoView [V manifest]**: `gecko_android` >= 128, `popup-fenix.html`, MV2 background page, permissions `dns`, `menus`, `privacy`, `webNavigation`, `webRequestBlocking`, `unlimitedStorage`. Popups need `ActionDelegate.onTogglePopup/onOpenPopup` to return a `GeckoSession` we host; built-in install via `ensureBuiltIn` needs no signature; our app calls none of `setTabActive`, `setActionDelegate`, `SessionTabDelegate`, and hibernated tabs would vanish from uBO's tab model.
- **Native messaging fine print [V]**: Mozilla's Android page lists `sendNativeMessage` as unsupported for regular extensions; it works for us as built-in + `geckoViewAddons`; replies must be plain strings.
- **Firefox keeps blocking `webRequest` in MV3** and ships DNR beside it (Mozilla add-ons blog, 2022); event pages, not service workers. Stay MV2 persistent: the engine must not be unloaded.
- FREEZE.md cites Mozilla's note that DNR "avoids holding requests". That gain is the hop to a separate extension process; with extensions in-process (our setting) a JS listener pays no hop, while Gecko's DNR is a JS linear scan with a 30k cap.
- **uBO spike recipe (1 day, P2):** copy the Firefox build (`uBlock0.firefox`, id `uBlock0@raymondhill.net`) into assets, `ensureBuiltIn`, call `setTabActive` for the visible session and `setActionDelegate` to read `badgeText`, set `autoUpdate:false`; measure with the A5 page and `adb shell dumpsys meminfo` on the tablet against engine-off and kestrel-shields v2 (cold start to first filtered request, PSS, typing stress).
- **GPL in (d)**: bundling uBO unmodified is an aggregate; modifying it means publishing the changes. The repo is public, so the cost is the licence choice (section 1.10).

## 9. YouTube and other video-site ads [V uAssets Nov 2026]

- Current uBO rules (quick-fixes + filters): `json-prune` of `playerResponse.adPlacements|playerAds|adSlots`; `json-prune-fetch-response` / `-xhr-response` on `/player` and `/playlist`; `set-constant` `ytInitialPlayerResponse.adPlacements|playerAds|adSlots = undefined`; `trusted-replace-xhr-response` / `trusted-replace-fetch-response` (`"adPlacements"` -> `"no_ads"`); `trusted-edit-inbound-object` on `JSON.stringify` (10 rules, rewrites client context such as `isInlinePlaybackNoAd`); `trusted-prevent-dom-bypass`; `adjust-setTimeout`; `remove-node-text`; `no-xhr-if` for `atr` pings; HTML filter `##^script[id]:has-text(window,"fetch")` (needs stream filters, a Firefox advantage).
- Brave adds `brave-yt-sabr-fix.js` and navigation/theater/playback-speed fixes, and Twitch scripts (`vaft-ublock-origin.js`, `video-swap-new-ublock-origin.js`).
- Needed from us: P1 scriptlets (json-prune family, trusted-replace-*, set-constant, edit-inbound-object, prevent-dom-bypass), P2 HTML filtering, the 8-hour `quick-fixes` refresh. Expect breakage: Ghostery reports detection logic changing "twice to multiple times per day", server-side ad insertion, and a warning after about three videos when logged in.
- **ToS/legal caveat, to state honestly in the UI:** YouTube's Terms (Permissions and Restrictions) forbid users to "circumvent, disable, fraudulently engage with, or otherwise interfere with any part of the Service", and YouTube told the press that ad blockers violate its terms. The consequence is contractual (warnings, playback limits, possible account action), not criminal law in most places [S, not legal advice]. Make it a labelled switch; do not hide it.

## 10. Policies and measuring "unnecessary page weight" in CI

- **Safe by default**: ads/trackers/cookie-sync (section 5), cosmetic hiding, neutered shims for GA/GTM/GPT/IMA so pages keep working, tracking-parameter stripping, cookie-notice rules with `set-cookie` scriptlets, `preload=none` media, autoplay off (GeckoView `PERMISSION_AUTOPLAY_*`; see research/GECKO_SPEED.md), HTTPS-first.
- **Opt-in "Aggressive"**: EasyList social/chat/AI/newsletter/notification lists, click-to-load for embeds (Disqus 72 requests; uBO ships `click2load.html`), `webFontsEnabled(false)` (4% of remaining bytes; icon fonts break), third-party frames blocked. **"Strict" = uBO medium mode** (3p scripts and frames blocked, relax per site); power users only.
- **Harness (P1)**: same pages, configs {off, current lists, candidate}, N >= 5 alternating, medians of requests, distinct hosts, transfer bytes, DOMContentLoaded, load, last network activity, content-process CPU. Hard gates on a local fixture site (fake ad hosts mapped with `network.dns.localDomains`); live pages as a nightly soft trend.
- **Extension in desktop Firefox**: Selenium + geckodriver (add-on install, temporary) or WebDriver BiDi `webExtension.install` (unsigned = temporary, `moz:permanent` needs a signature; Mozilla docs). Playwright and Puppeteer do not officially load Firefox extensions, hence the Chromium numbers above.

## 11. Not verified / risks

- Every on-device number: A53 + SpiderMonkey + GeckoView speed and memory of any engine; the 5-10x factor is a rule of thumb. The UI-thread mechanism is verified in bytecode and source; its share of the typing freeze is not profiled.
- Transport of a multi-MiB snapshot over native messaging, `file://` fetch from a built-in extension, `Uint8Array.fromBase64` in 160: unknown (P0.3 spike).
- Crawl: one pass, bot-walled sites dropped, headless Chromium not Firefox, no consent clicks, no cosmetic or scriptlet effect, ads rotate; treat percentages as orders of magnitude. ETP strict (Disconnect lists) may double-count with our lists: not measured.
- uBO memory/start-up on this tablet; Hagezi/AdGuard false positives on logged-in pages; real `Diff-Path` patch sizes; OISD and "I don't care about cookies" licences; CNAME `canonical_name` over DoH/TRR; adblock-rust WASM size and speed on ARM32; whether the YouTube fixes still work on the day of shipping.
- The JS port skips the Kotlin >2048-character/userinfo handling and the cosmetic tables; both must be ported before shipping.

## 12. Sources

- uBO: https://github.com/gorhill/uBlock (`src/js/static-net-filtering.js`, `hntrie.js`, `cosmetic-filtering.js`, `contentscript.js`, `assets.js`, `diff-updater.js`, `hnswitches.js`, `resources/*.js`, `platform/firefox/{manifest.json,vapi-scripting.js,vapi-background-ext.js}`, `platform/npm/README.md`, `assets/assets.json`); wiki https://github.com/gorhill/uBlock/wiki/Static-filter-syntax , /Dynamic-filtering:-rule-syntax , /Blocking-mode:-medium-mode ; https://github.com/uBlockOrigin/uBlock-issues/wiki/Launch-and-filter-lists-load-performance ; https://bugzilla.mozilla.org/show_bug.cgi?id=1651434 , ...id=1428573
- Lists: https://github.com/uBlockOrigin/uAssets (`filters/*.txt`, `thirdparties/`) ; https://easylist.to/pages/licence.html ; https://github.com/hagezi/dns-blocklists ; https://github.com/AdguardTeam/AdguardFilters ; https://adguard.com/kb/general/ad-filtering/adguard-filters/ ; https://oisd.nl/ ; https://github.com/ameshkov/diffupdates ; https://github.com/brave/adblock-resources (`filter_lists/list_catalog.json`, `resources/`) ; https://github.com/brave/adblock-lists
- Engines: https://github.com/brave/adblock-rust (README, CHANGELOG, `src/filters/network.rs`, `.github/workflows/ci.yml`) ; https://brave.com/blog/improved-ad-blocker-performance/ ; https://brave.com/privacy-updates/36-adblock-memory-reduction ; https://github.com/ghostery/adblocker (`packages/adblocker/src/filters/network.ts`) ; https://whotracks.me/blog/adblockers_performance_study.html ; https://github.com/AdguardTeam/Scriptlets ; https://github.com/AdguardTeam/tsurlfilter ; https://adguard.com/kb/general/ad-filtering/create-own-filters/
- Gecko/Mozilla: GeckoView 160 `classes.jar` (`WebExtension*`, `GeckoRuntimeSettings`) and omni.ja (`ExtensionDNRLimits.sys.mjs`, `ExtensionDNR.sys.mjs`, `GeckoViewWebExtension.sys.mjs`, `Extension.sys.mjs`, `ext-toolkit.json`, `ext-android.json`, extension schemas) ; https://firefox-source-docs.mozilla.org/mobile/android/geckoview/consumer/web-extensions.html ; https://extensionworkshop.com/documentation/develop/differences-between-desktop-and-android-extensions/ ; http://blog.mozilla.org/addons/2022/05/18/manifest-v3-in-firefox-recap-next-steps/ ; https://developer.mozilla.org/en-US/docs/Mozilla/Add-ons/WebExtensions/API/declarativeNetRequest ; https://bugzilla.mozilla.org/show_bug.cgi?id=1880856 ; https://firefox-source-docs.mozilla.org/remote/webdriver-bidi/Extensions.html
- Video: https://www.youtube.com/t/terms ; https://www.ghostery.com/blog/whats-happening-with-youtube-ads ; uAssets `filters/quick-fixes.txt`, `filters/filters.txt`
- Repo: `core/src/main/kotlin/app/kestrel/core/filter/*`, `geckoapp/src/main/kotlin/app/kestrel/browser/web/ShieldsBridge.kt`, `geckoapp/src/main/assets/kestrel-shields/*`, `BrowserActivity.kt:836-844`, `ui/Toolbar.kt:101`, `docs/FILTER-LISTS.md`, `research/FREEZE.md`
