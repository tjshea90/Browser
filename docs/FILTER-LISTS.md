# Filter lists: sources, licences, how the bundled snapshot is made

The browser ships a compiled snapshot (`app/src/main/assets/filters/default.kfs`, ~4 MB) so it is protected on the very
first page load even offline, and refreshes it itself (weekly, conditional GET, HTTPS only — see ARCHITECTURE.md section 7).

| List | Category | Upstream | Licence |
|---|---|---|---|
| EasyList | ads | easylist.to | dual GPLv3+ / CC BY-SA 3.0 (<https://easylist.to/pages/licence.html>) |
| EasyPrivacy | trackers | easylist.to | same |
| EasyList – Cookie Notices | annoyance (mostly cosmetic) | easylist.to | same |
| uBlock filters (main + included 2020-2026 + general) | ads | github.com/uBlockOrigin/uAssets | GPLv3 |
| uBlock filters – Privacy (+ resource-abuse) | trackers | uAssets | GPLv3 |
| uBlock filters – Badware risks | malware / phishing (also blocks the page itself) | uAssets | GPLv3 |
| uBlock filters – Unbreak | exceptions (`@@`, `$badfilter`) that keep sites working | uAssets | GPLv3 |
| Public Suffix List | first/third-party decisions | publicsuffix.org | MPL-2.0 |

**Deliberately excluded**
- *Peter Lowe's ad-server list*: its licence encourages **noncommercial use only**; ~3 % extra hosts, mostly redundant. Add it yourself if you want it.
- *URLhaus "online" URL list*: 105 000 individual malware URLs (17 MB compiled) — wrong tool for a 128 MB-class device. Domain-level malware
  coverage comes from uBlock *Badware risks*; WebView Safe Browsing (if Google Play Services exist) adds URL-level protection.

If you **redistribute** the APK or repo, keep these attributions and honour the licences above (the lists are data under their own
licences, separate from this project's code, whose licence the owner has not chosen yet).

## What is and is not supported (measured on the real lists)
Of ~226 000 lines: 99 318 pure-host rules + 20 927 pattern rules + 885 site-level exceptions + 22 768 site-specific and 29 204 generic
cosmetic rules compiled. Skipped on purpose: regex rules (597, ReDoS risk), procedural cosmetics/scriptlets (14 942, need page-world
scripting), `redirect=`/`csp=`/`replace=`/`removeparam=`/`cname`/`denyallow` options (2 845), entity domains `site.*` (1 463).
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
