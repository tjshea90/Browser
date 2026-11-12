# ADBLOCK D2 - all ~29,000 generic cosmetic rules, applied by the class / id a page really has

Owner request (AA3-3, "figure out if it is using the best ad and tracker blocking software"). Tags: **[V]** verified here (JVM tests on the REAL bundled list `app/src/main/assets/filters/default.kfs`, web tests in headless Chromium, Android 10 x86_64 emulator), **[N]** not verified (no real tablet, no real-world site corpus).

## 1. What was wrong
The lists carry **29,204 generic element-hiding selectors** (they apply on every site). Only the first 1,200 were ever injected (`CosmeticIndex.DEFAULT_GENERIC_LIMIT`), all of them `#id` selectors (research/PERF_JS.md). Injecting all 29k as one style sheet would cost a few MB and a few hundred ms of style parsing per page on this tablet, so the other 28,000 selectors (every `.class` one, most of the compound ones) did nothing: ad boxes left behind after their requests were blocked, first-party ad containers, cookie banners.

## 2. The design (Firefox build; the WebView build keeps the old path)
A generic selector can only match on a page that HAS an element with the class or id it names. So:
1. **Index** (`core/filter/CosmeticTokens.kt`, `CosmeticIndex`): each selector is filed under the **rarest** class/id it requires (rarest over the whole list, so a common class such as `.container` never pulls in hundreds of rules). Built once on first use or at engine warm-up. Class/id tokens inside `[...]`, `(...)` (`:not(.x)`, `:is(...)`, `:has(...)`) and quotes are never used, and a selector list (`.a, .b`) is not filed under one part: it stays in the up-front set. Keys are lower-cased on both sides (quirks-mode documents match class names case-insensitively; lower-casing can only return too much, never too little). **Soundness:** a selector is never withheld from a page it could match.
2. **Up front** (`cosmeticCss(..., tokenMode = true)`): the site's own rules + the generic selectors that name no class/id and are led by a tag name (`a[href^=...]`, `ins[...]`: the browser tests only elements of that tag), plus the marker `/*kg*/`. Selectors led by `[`, `*` or `:` would be tested against EVERY element of EVERY page and stay unused, as before.
3. **Lazily** (`gen` message): the top document with the marker collects its classes and ids (`querySelectorAll('[id],[class]')` at DOMContentLoaded and load, then a `MutationObserver` for added nodes), lower-cases them, and sends the NEW ones in batches of at most 300 every 350 ms; the app returns the CSS of the selectors filed under them (at most 500 per batch, minus `#@#` exceptions of that site, nothing when `$generichide` / `$elemhide` / `$document` applies or the page's Shields switches are off) and the page injects one `<style data-kestrel="g">` per batch. Limits per page: 60 batches or 8,000 distinct names, then it stops asking (a page that invents names, e.g. generated ids). Iframes ask nothing (they never got the generic set).
4. **Where the code lives:** `core/.../CosmeticTokens.kt` (pure), `CosmeticIndex.kt` (`tokens()`, `looseCss`, `genericCssForTokens`, `warm`), `FilterSet.genericCssForTokens`, `ShieldsBridge` (`gen` on the cosmetic worker thread, `MAX_TOKENS` 400), `background.js` (forwards `gen`, not cached), `content.js` (watcher). Warm-up at engine load/swap (`Engine.warmCosmetics`).

## 3. Numbers on the real list [V]
- 29,204 generic selectors: **28,555 (97.8 %) are filed under a class/id**, 27,323 distinct keys, **largest bucket 9 selectors**; 649 keyless (478 tag-led: injected up front; 171 led by `[` `*` `:`: not used).
- Index build: **52 to 91 ms** on this x86_64 sandbox (JVM); the tablet is several times slower, which is why it is built at start-up, off every request thread.
- Up-front CSS for a normal site: **17,280 bytes** (was 18,579 bytes for the 1,200 `#id` selectors): the page pays no more than before and now reaches 28,555 more selectors.
- A typical page's batch of ~40 common class/id names (plus a few ad names) pulled **18 selectors, 1,080 bytes** of CSS.
- Real examples: `.ad-unit-wrapper`, `#siteAdHeader`, `.advert__leaderboard`, `#google_ads`, `.Ad-Container:not(.adsbygoogle)`, `ins.adsbygoogle[data-ad-client]` are each returned only for a page that holds that name.

## 4. Tests
- `CosmeticTokensTest` (20): which tokens a selector requires (brackets / parentheses / quotes / lists / case / length), hostile page tokens never reach the CSS, only matching selectors returned, rarest-token filing, exceptions and `$generichide` / `$elemhide` on the lazy path, marker only when it matters, the old non-token path unchanged (the WebView build), binary round trip, cap per batch, empty set.
- `RealCosmeticTokenIndexTest`: the numbers above on the real list.
- `web-tests/content.test.js` +6 (87 in all): names sent lower-cased and once, applied CSS hides only the matching element, no marker = no messages, late nodes are sent, batches of at most 300, an iframe asks nothing, an app that cannot answer never breaks the page. Found on the way: `inject` and the observer ran before the page's root element existed when the answer came back at once (a cached answer): the CSS would have been lost; both now wait for it.
- Emulator: see section 5.

## 5. Emulator probe (`d2.html`, a page with the real selectors above, kept in the scratchpad) [V when filled in below]
RESULT: (filled in after the run)

## 6. Not done here (AA3-3 remainder, in the order of value)
1. Procedural cosmetics (`:has-text()`, `:upward()`), 1,730 `:has(` site rules need a recent engine (Firefox 160 has `:has`), scriptlets (YouTube-style ad skipping), `$removeparam`, `redirect=` neutering, D3: unchanged, each needs the same kind of measured step.
2. A real-world corpus (false positives): generic class rules such as `.promo` or `.sponsor` can hide something a site really needs. Shields > "Hide page ads" per site (the `cosmetic` switch) turns the whole mechanism off for that site; a list of reported breakages is the way to find rules to except.
3. The verdict that was never reported to the owner (research/AUDIT-2026-11.md section 5): the engine is the right technology (the lists are EasyList-family, matched by a hash/token engine in the extension, same approach as uBlock Origin and Brave's adblock-rust); the generic cosmetic gap above was its largest hole, and is now closed.
