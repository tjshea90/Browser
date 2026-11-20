# Filter lists: sources, licences, how the bundled snapshot is made

The browser ships a compiled snapshot (`app/src/main/assets/filters/default.kfs`, ~4 MB) so it is protected on the very
first page load even offline, and refreshes it itself (daily since alpha16, conditional GET, HTTPS only — see ARCHITECTURE.md section 7).

| List | Category | Upstream | Licence |
|---|---|---|---|
| EasyList | ads | easylist.to | dual GPLv3+ / CC BY-SA 3.0 (<https://easylist.to/pages/licence.html>) |
| EasyPrivacy | trackers | easylist.to | same |
| EasyList – Cookie Notices | annoyance (mostly cosmetic) | easylist.to | same |
| uBlock filters (main + included 2020-2026 + general) | ads | github.com/uBlockOrigin/uAssets | GPLv3 |
| uBlock filters – Privacy (+ resource-abuse) | trackers | uAssets | GPLv3 |
| uBlock filters – Quick fixes (`quick-fixes.txt`, AW1e) | ads: the newest site-specific ad and anti-adblock fixes, many of them scriptlets | uAssets | GPLv3 |
| AdGuard/uBO – URL Tracking Protection (`privacy-removeparam.txt`) | `$removeparam` rules for top-level navigations (AA3-3b) | uAssets (forked from AdGuard's TrackParamFilter) | GPLv3 (<https://github.com/uBlockOrigin/uAssets/blob/master/LICENSE>) |
| uBlock filters – Badware risks | malware / phishing (also blocks the page itself) | uAssets | GPLv3 |
| uBlock filters – Unbreak | exceptions (`@@`, `$badfilter`) that keep sites working | uAssets | GPLv3 |
| HaGeZi's Multi LIGHT (`adblock/light.txt`, RX3-B2) | trackers / ads at host level (mostly cookie-sync and tracker hosts); on-device update source only, switchable ("Extra tracker lists") | github.com/hagezi/dns-blocklists | GPL-3.0 (<https://github.com/hagezi/dns-blocklists/blob/main/LICENSE>) |
| AdGuard Tracking Protection - sections `tracking_servers`, `tracking_servers_firstparty`, `general_url`, `specific` (RX3-B2) | trackers; on-device update source only, switchable (the composite `filters.adtidy.org/.../3.txt` is 15 MB and is NOT used) | github.com/AdguardTeam/AdguardFilters (`SpywareFilter/sections/`) | GPL-3.0 (<https://github.com/AdguardTeam/AdguardFilters/blob/master/LICENSE>) |
| Public Suffix List | first/third-party decisions | publicsuffix.org | MPL-2.0 |
| Web Compatibility Interventions (NL1, 2026-11-20; NOT a filter list: site-specific User-Agent overrides and small CSS / script patches) | site fixes, Kestrel G only; fetched at build time from Mozilla's Maven (`org.mozilla.components:feature-webcompat`, same major as the engine), the Remote Settings listener replaced by an empty one so it adds no background traffic | maven.mozilla.org, source: mozilla-central browser/extensions/webcompat | MPL-2.0 (<https://www.mozilla.org/MPL/2.0/>); the files are packed unchanged except lib/remote_settings_update_check.js |

**Licensing stance (owner, 2026-11-11):** this build is for the owner's *personal, non-commercial* use and the owner will obtain any permission a list or product requires, so
non-commercial-only data (e.g. Peter Lowe's list, CC BY-NC lists) MAY be added. Keep this table accurate and keep attributions if the APK or repo is ever shared.

**Deliberately excluded (a choice of value, no longer of licence)**
- *Peter Lowe's ad-server list*: licence is non-commercial only (now permitted), but it is ~3 % extra hosts, mostly redundant with EasyList/uBlock; not worth another host to contact.
- *URLhaus "online" URL list*: 105 000 individual malware URLs (17 MB compiled) — wrong tool for a 128 MB-class device. Domain-level malware
  coverage comes from uBlock *Badware risks*; WebView Safe Browsing (if Google Play Services exist) adds URL-level protection.

If you **redistribute** the APK or repo, keep these attributions and honour the licences above (the lists are data under their own
licences, separate from this project's code, whose licence the owner has not chosen yet).

## What is and is not supported (measured on the real lists)
Of ~226 000 lines: 99 318 pure-host rules + 20 927 pattern rules + 885 site-level exceptions + 22 768 site-specific and 29 204 generic
cosmetic rules compiled. Skipped on purpose: regex rules (597, ReDoS risk), procedural cosmetics/scriptlets (14 942, need page-world
scripting), `redirect=`/`csp=`/`replace=`/`removeparam=`/`cname`/`denyallow` options (2 845), entity domains `site.*` (1 463).
`$removeparam` (AA3-3b, alpha25): plain parameter names are applied to the address of every top-level navigation (host rules, `||host/path` rules, generic rules, `domain=` / `to=` lists, `site.*` entity hosts, `@@` exceptions, `$badfilter`):
2 441 of the 2 990 rules of the uBO Privacy list and the AdGuard/uBO "URL Tracking Protection" list (`privacy-removeparam.txt`, added to the sources). Left out: regular-expression values (49), rules for sub-resources only
or with wildcards in the path or the query part (297). A generic rule can never name a parameter that sites need (`q`, `id`, `v`, `code`, `state`, `token`...). Measured on the real lists: 27 us per address, 61 KB in the snapshot.
`$popup` (3 032 rules) IS supported: the first URL of a window opened by a page is checked with type `POPUP`.

## Regenerating the snapshot
`scripts/make-default-lists.sh` (header explains how to obtain the uAssets checkout). It compiles with the same `:core` code the tablet
uses, installs the assets, and runs `DefaultListsRegressionTest` (must block known ad/tracker URLs, must NOT block 16 major sites' pages
or 18 essential third-party resources such as fonts, CDNs, reCAPTCHA, Stripe, PayPal, Maps).

## On-device update sources
Defined in code (`FilterSources`), HTTPS only. Verified reachable on 2026-11-10 via the `web-fetch` agent (curl is blocked in the sandbox):
EasyList (2.0 MB), EasyPrivacy (1.5 MB), uBlock `filters.min` (1.9 MB), `privacy.min` (141 KB), `badware.min` (165 KB), `unbreak.min` (178 KB),
and the EasyList cookie list at `secure.fanboy.co.nz/fanboy-cookiemonster.txt` (0.9 MB). A first guess at a uAssets mirror URL for the cookie list was a
404 and was replaced. Total about 6.8 MB uncompressed, roughly 2 MB gzipped; unchanged lists cost a conditional-GET round trip (a few hundred bytes).
`FilterUpdater` keeps the SAFETY INVARIANT: the compiled set only replaces the active one if every list is available, the new rule count is >= 60 % of the old
and >= 20 000, and it round-trips through the codec. A failing source is reported in Settings and is never fatal.

## Which lists are trusted (RX3-B1, 2026-11-14)
A list is "trusted" when its maintainers also write the scriptlet semantics: only then may its `+js(trusted-*)` lines run. Those scriptlets rewrite what a site's own API answers
(`trusted-replace-fetch-response`, `trusted-replace-xhr-response`, alias `trusted-rpfr`) and set cookies or storage by script (`trusted-set-cookie`, `trusted-set-local-storage-item`).
uBlock Origin itself honours them only from its own lists. In code: `ListSource.trusted` (default false) for the downloaded lists, `abp-trusted:` in `scripts/make-default-lists.sh` for the
bundled snapshot, `FilterSetBuilder.addAbpList(..., trusted)` (default false) and `ScriptletRules.encode(inner, trusted)`. Trusted: the six `ubo-*` sources and our own seed lists.
Not trusted: EasyList, EasyPrivacy, `easylist-cookies` (Fanboy), and every list added later until someone says otherwise (`FilterSourcesTrustTest` pins it).
A `trusted-*` line in an untrusted list is skipped as `SCRIPTLET_UNTRUSTED` (counted per list in `BuildReport.untrustedByList`), also as an exception line (an untrusted list cannot switch a trusted call off).
Measured the same day on the real files (EasyList 2.06 MB, EasyPrivacy 1.52 MB, Fanboy cookie list 0.91 MB): none of the three holds a single `trusted-*` scriptlet line, so nothing they do today is lost.

## Extra tracker lists (RX3-B2, owner 2026-11-14: "Yes to all four")
Settings > Filter lists > "Extra tracker lists", default ON (`Prefs.extraTrackerLists`): HaGeZi Light and four AdGuard Tracking Protection sections are downloaded and compiled together with the standard lists (`FilterSources.EXTRA`, all from raw.githubusercontent.com, never trusted: no `trusted-*` scriptlets from them). They are NOT in the bundled snapshot (`default.kfs`); until the first update after install the standard lists are what blocks. Switching the row starts an update; the set of lists that went into the installed compile is remembered (`files/filters/sources.txt`), so a changed set recompiles even when every list answers 304, and the "not smaller than 60 % of the old set" check is skipped for that one deliberate change.
Measured 2026-11-14 with the real files (JVM, desktop CPU): EasyList + EasyPrivacy + Cookie list = 111,988 network rules; with the extras 210,598 (+98,610, 90k of them HaGeZi hosts); compile time about the same (0.5 s warm); the update canaries (resources no list may block, well-known trackers that must stay blocked) pass for both. Raw sizes: HaGeZi 2.2 MB, AdGuard sections 127 + 155 + 19 + 264 KB.
Why not AdGuard's composite file: `filters.adtidy.org/extension/ublock/filters/3.txt` (and its `_optimized` edition) is 15 MB / 440k lines because it also carries the cosmetic, scriptlet and removeparam parts, more than the updater's 14 MB cap (a list over the cap would stop EVERY update) and far too much for the hotspot and the tablet. research/ADBLOCK_ENGINE.md's "+ AdGuard Tracking Protection (Spyware sections)" measurement was of the section files, which is what is used now.
NOT done: a per-list block count in the Shields panel (the compiled format does not remember which list a rule came from; "Shields off for this site" is the one-tap diagnosis that exists), a regression run on the owner's own sites (needs the tablet).
