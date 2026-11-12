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
| AdGuard/uBO – URL Tracking Protection (`privacy-removeparam.txt`) | `$removeparam` rules for top-level navigations (AA3-3b) | uAssets (forked from AdGuard's TrackParamFilter) | GPLv3 (<https://github.com/uBlockOrigin/uAssets/blob/master/LICENSE>) |
| uBlock filters – Badware risks | malware / phishing (also blocks the page itself) | uAssets | GPLv3 |
| uBlock filters – Unbreak | exceptions (`@@`, `$badfilter`) that keep sites working | uAssets | GPLv3 |
| Public Suffix List | first/third-party decisions | publicsuffix.org | MPL-2.0 |

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
