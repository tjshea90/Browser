# SECURITY research (TASKS R4): Kestrel and Kestrel G - 2026-11-11
Owner ask: "make the browser secure by using the newest security tools and standards". Research only: no code, build, commit or CI change was made; this is the only file written. Siblings: ADBLOCK_ENGINE.md (R5), DOWNLOADS.md (R2), FREEZE.md (R6).
Tags: **[V]** verified here on the shipped bits (GeckoView 160.0.20261104181552 AAR via javap/grep, Mozilla Maven `.sha256`/`.asc`, `apksigner` on throw-away keys) | **[S]** vendor document, URL in section 8 | **[U]** NOT verified, see section 9.
Priority: P0 before the next release | P1 within two releases | P2 backlog. Effort: S < 1 h, M < 1 day, L = days. "Break" = chance of breaking sites or UX.

## 1. Decisions (read this first)
1. **Two engines, two stances.** Kestrel G (Gecko 160, release channel, build 20261104181552 = newest on maven.mozilla.org [V]) is the build to harden and the only one that can meet current standards. Kestrel (system WebView = Chromium 74, Apr 2019, unpatchable here) stays a legacy fallback: keep its app-level hardening, add nothing big, never log in with it (section 5).
2. **P0 - the PUBLIC dev signing key is the biggest hole.** Anyone can sign an APK with `signing/dev/kestrel-dev.jks` (password is in the repo) and Android installs it as a normal update that inherits cookies, sessions and permissions. Decision: private key in an Actions *environment* secret + v3 lineage so the tablet keeps its data (commands tested, section 6). Acceptance by Android 10 is UNTESTED and AOSP calls rotation "not recommended" on Android 12 and earlier [S][U]: gate on an Android 10 emulator install, fallback = new applicationId.
3. **P0 - CI trusts too much.** `android.yml` gives EVERY job `contents: write`, writes the signing key into the workspace of the job that runs Gradle/npm (third-party code), uses tag-pinned actions, has no Gradle dependency verification, and its failure step uploads a SIGNED apk as a public artifact. Fix in 4.4: per-job permissions, build (no secrets) / sign+publish split behind environment `release` with a required reviewer, SHA pins, `verification-metadata.xml`, attestations.
4. **P0 - patch cadence is the real browser security on a device without Play updates.** Firefox 160 (2026-11-10) fixed 99 CVEs, 57 rated high, including "sandbox escape" titles (MFSA 2026-125 [S]). We ship the newest GeckoView today; add `gecko-watch` (twice weekly) and an "engine age" line in About (warn above 42 days); ship Gecko bumps within days of the release.
5. **Gecko 160 already does most of "the newest standards" - pin it, do not rebuild it [V]:** HTTPS-First (default since Fx 136), TLS 1.2-1.3 only, hybrid post-quantum key exchange (X25519MLKEM768), ECH (+GREASE 100%) when an HTTPS RR is found (our DoH, or the Android 10+ native query), CRLite enforcing ("compat" channel on Android), Certificate Transparency (static default 2; an old bug comment says not on Android [U]), HSTS preload, mixed-content upgrade + active block, Safe Browsing v5, Local Network Access blocking, GPC, referrer trimming, partitioned third-party cookies. Pin them in code (4.1) so a future default change cannot silently weaken them.
6. **Real gaps in `GeckoEngine.kt` (P1):** fingerprinting protection (FPP) is off in normal tabs; Push/Notification machinery on; LNA tracker blocking off; `ShieldsBridge` answers any sender; extension holds `<all_urls>` and may not run in private tabs [U]; content processes run as the app's own UID because `isolatedProcessEnabled` defaults to false [V].
7. **Do NOT enable now:** isolated content processes (Mozilla meta-bug 1565196 open with 16 blockers incl. IME 1989379, media 1810736, crash handler [S]); Fission (RAM; Mozilla's Android rollout is contradictory [U]); RFP; engine HTTPS-Only; TLS 1.3-only; TRR mode 3 or strict fallback by default (flaky hotspot -> DNS failures); enterprise roots; JIT off; WebRTC off; opt-in cookie partitioning. Reasons in the matrix (4.0).
8. **Safe Browsing already works in Gecko without Play services [V]:** `SafeBrowsing.init()` runs at startup, Mozilla's Google API key is compiled in, malware+phishing prefs default true, v5 hash lists from `safebrowsing.googleapis.com` (hash prefixes only; threat-hit reporting off in the v4 provider). Decision: keep ON (best phishing coverage available; owner already uses Google search), add a Settings switch, measure its data (B14). No extra list: URLhaus now needs a free Auth-Key plus fair-use terms, OpenPhish's feed terms are not stated [S]. Resolver-level blocking (Quad9) is the small legal add-on and the only one for the WebView build.
9. **DNS and ECH:** keep TRR mode 2 -> `mozilla.cloudflare-dns.com` (Mozilla's own provider entry, http3First [V]). ECH only helps with encrypted DNS, otherwise the carrier reads the host name from the DNS query. Zero-code owner action: Android *Private DNS* = `dns.quad9.net` (covers the WebView build and Gecko's native fallback, blocks known-malware domains [S]).
10. **Owner actions, no code:** Private DNS above; GitHub passkey/2FA and a review of authorized apps/tokens; screen lock (storage is unencrypted, patch level 2021-07, kernel 4.14.141); install each Kestrel G update within a week; create GitHub environment `release` (you as required reviewer, "Prevent self-review" OFF because you trigger the runs).
11. **Data-cost warning [V]:** besides the weekly lists, Gecko polls Mozilla Remote Settings every 24 h (`components.manifest` update-timer + `services.settings.poll_interval` 86400), updates Safe Browsing lists and CRLite/intermediate certificates. That breaks the "no background network" rule in CLAUDE.md. Measure 24 h idle bytes with DataMeter before throttling anything (B14).
12. **R5 interplay:** moving the blocker into the extension (D1) and scriptlets (D2) turn list poisoning into code injection in every page: B20 (signature on the list bundle, public key pinned in the APK) becomes a prerequisite for scriptlets; user lists must never reach trusted scriptlets.

## 2. Threat model (this tablet, this network)
| Actor | Can | Worst case | Controls: existing -> new |
|---|---|---|---|
| T1 Carrier / hotspot | read + rewrite cleartext HTTP and DNS, inject headers (Verizon UIDH [S]), block or throttle DoH/QUIC | profiling, ad injection, downgrade to http | NavPolicy + engine HTTPS-First, DoH, ECH, mixed-content block -> B7 |
| T2 Malicious site | engine bug, phishing, drive-by download, `intent:` abuse, LAN probing, push spam, fingerprinting | renderer exploit = app-UID access to cookies and any camera/mic/location grant | patch cadence, Safe Browsing, LNA, `ExternalIntents` -> B6 B8 B10 |
| T3 Forged / poisoned filter list | upstream compromise, or TLS MITM with a CA the 2021-07 Android store still trusts (updater = `HttpURLConnection`, not Mozilla's roots) | block banking JS, hide warning banners, allow-list a phishing host | HTTPS only, size caps, canaries, round-trip check -> B17, B20 |
| T4 Look-alike APK / tampered release | public dev key signs a "valid" update; Release asset swapped | full takeover with existing data | CI cert pin -> B1, B11 |
| T5 CI / supply chain | malicious action, Gradle plugin, Maven artifact or npm package; rogue or buggy session branch dispatching the workflow with secrets | signing-key theft, trojaned APK | -> B2 B3 B4 B5 B13 B15 |
| T6 Other apps / physical | task hijack of the exported singleTask activity, `intent:` with arbitrary ACTION_*, stolen unencrypted tablet, kernel LPE after a renderer exploit | data theft, root | FLAG_SECURE private tabs, allowBackup=false, no exported providers -> B10 B17, screen lock |
| T7 Stale engine | WebView 74 forever; Gecko if releases slip | any N-day | -> B6, section 5 |

## 3. Prioritized backlog (build in this order)
| # | Pri | Item | Eff | Break |
|---|---|---|---|---|
| B1 | P0 | Private release key + v3 lineage + re-pin `signing/CERT_SHA256` (section 6); emulator gate first | M | none |
| B2 | P0 | Workflow `permissions: {}` at top, per-job minimum; `persist-credentials: false`; delete the failure step that uploads a signed apk | S | none |
| B3 | P0 | Split build (no secrets) from sign+publish in environment `release` (required reviewer; later deployment branches = `main` + tags) | M | approval tap per release |
| B4 | P0 | Pin every `uses:` to a 40-hex SHA with `# vX.Y.Z`; repo policy "Require actions pinned to a full-length commit SHA"; Dependabot for actions | S | none |
| B5 | P0 | `gradle/verification-metadata.xml` (sha256 for all, PGP for `org.mozilla.geckoview`) + wrapper `distributionSha256Sum` + wrapper validation | M | none |
| B6 | P0 | `gecko-watch` workflow + engine-age line in About (4.3, 4.4) | S | none |
| B7 | P0 | Owner actions of decision 10 (Private DNS strict fails closed: if the carrier blocks port 853 set it back to Automatic) | S | low |
| B8 | P1 | `GeckoEngine.kt` pins + FPP + pref list (4.1) | S | low |
| B9 | P1 | Bridge sender check, extension host permissions/CSP, private-tab allow (4.2) | S | none |
| B10 | P1 | `intent:` action allow-list, filename format-char strip, clipboard flag (4.3); fold into DOWNLOADS.md #13 | S | none |
| B11 | P1 | `actions/attest` provenance (+ SBOM) per APK, immutable releases, verify commands in the Release text | M | none |
| B12 | P1 | CodeQL weekly (java-kotlin manual build, javascript, actions), secret scanning + push protection, SECURITY.md, OSV weekly on `verification-metadata.xml`, optional OpenSSF Scorecard [U] | M | none |
| B13 | P1 | Protected `main` + CODEOWNERS for `.github/**`, `signing/**`, `gradle/verification-metadata.xml` (TASKS "main" decision); sessions stay on `ccr-*` | M | sessions can no longer touch the release path |
| B14 | P1 | 24 h idle data measurement (Remote Settings, Safe Browsing, CRLite); decide `services.settings.poll_interval` / Settings switch | S | none |
| B15 | P1 | `web-tests`: lockfile, `npm ci --ignore-scripts`, pinned Playwright | S | none |
| B16 | P2 | `network.prefetch-next=false`, `network.dns.disablePrefetch=true` (privacy + hotspot data; coordinate with GECKO_SPEED) | S | low |
| B17 | P2 | NSC `cleartextTrafficPermitted="false"` for the 3 list hosts; `intentMatchingFlags`; drop `ProfileInstallReceiver`; CT in NSC only after an Android 10 run (4.3) | S | none |
| B18 | P2 | Own Google Safe Browsing key (Mozilla's embedded key is Mozilla's, ToS grey) or a Settings OFF switch | M | none |
| B19 | P2 | Settings "Strict encrypted DNS" (`network.trr.strict_native_fallback=true`) + provider list (Mozilla Cloudflare / NextDNS / Quad9) | M | med on flaky links |
| B20 | P2 | CI-built list bundle signed with ECDSA P-256 (public key in the APK): independent of the stale CA store, smaller download (R5 update path) | L | none |
| B21 | P2 | Reproducibility job (unsigned APK built twice on clean runners, `cmp`); reusable-workflow build for SLSA L3 | M | none |
| B22 | P2 | Drop CAMERA / RECORD_AUDIO / location declarations if the owner never uses them (an exploited renderer cannot be granted what is not declared) | S | feature loss |
| B23 | P2 | Re-check `isolatedProcessEnabled` when bug 1565196 closes, Fission "high value" strategy, retire the WebView build once G is trusted | - | - |

Suggested sessions: (1) owner items B7 + GitHub settings (SHA-pin policy, secret scanning + push protection, immutable releases, environment `release`); (2) one code session B8 B9 B10 B16 with an emulator smoke test of the Gecko build (page loads, private tab, download, permission prompts); (3) one CI session B2 B3 B4 B5 B6 B11 B12 B15 on a throw-away tag before touching the real release path; (4) B1 with the emulator gate, then delete the dev key from the release path (keep it for tests only); (5) the P2 items.

## 4. Exact settings and snippets
### 4.0 Gecko settings matrix (defaults from StaticPrefList 160.0.1 + AAR [V])
| Area | Default in 160 | Decision | Pref / API | Break |
|---|---|---|---|---|
| HTTPS-First / Only | First on (`dom.security.https_first`), Only off | keep; NavPolicy stays authoritative, Only would duplicate it | `allowInsecureConnections(ALLOW_ALL)` | none |
| TLS versions | min 1.2 (3), max 1.3 (4) | keep; 1.3-only rejected (legacy servers, small gain) | `security.tls.version.min/max` | med if 1.3-only |
| Post-quantum | X25519MLKEM768 on | pin | `setPostQuantumKeyExchangeEnabled(true)` | none |
| ECH | on, GREASE 100 | keep; needs HTTPS RR via DoH or native query (Android 10+) | `network.dns.echconfig.enabled` | none |
| DoH | none by default; ours mode 2, Mozilla Cloudflare | keep; mode 3 / strict fallback = optional B19 | `trustedRecursiveResolverMode/Uri` | low |
| CRLite / OCSP | CRLite enforce, `compat` filters; OCSP EV-only on Android | keep | `security.pki.crlite_mode` | none |
| CT | enforce (2) | pin | `setCertificateTransparencyMode(2)` | low (certs without SCTs) |
| HSTS preload | list compiled into libxul | keep; refreshed only by new Gecko releases | `network.stricttransportsecurity.preloadlist` | none |
| Mixed content | active blocked (Android), display upgraded | keep | `security.mixed_content.*` | low |
| Cookies | Total Cookie Protection (5, partitioned) | keep; opt-in partitioning rejected | `cookieBehavior` | med if rejected option |
| Fingerprinting | baseline on; FPP only in private tabs; RFP off | FPP everywhere; RFP rejected (UTC timezone, prefers-color-scheme, timer clamp, letterbox) | `setFingerprintingProtection(true)` | low: canvas read-back noise |
| Referrer | strict-origin-when-cross-origin, trackers trimmed to origin | keep; XOrigin policy 1/2 rejected | `network.http.referer.*` | high if 1/2 |
| WebRTC | on, mDNS host-candidate obfuscation on | `ice.default_address_only=true` | pref | low |
| Permissions | app delegate denies all but geolocation/camera/mic prompts; audible autoplay blocked (`media.autoplay.default` 1) | keep; also push/notifications prefs off | prefs (4.1) | none |
| Local Network Access | enabled + blocking on, trackers off | all three on | `setLnaEnabled/Blocking/BlockTrackers` | low |
| Site isolation | Fission off (app) | keep off | `fissionEnabled(false)` | - |
| OS sandbox | `isolatedProcessEnabled` false, app-UID content processes | keep off until bug 1565196 closes | `isolatedProcessEnabled` | crash/IME risk |
| Enterprise roots | static true, Java pushes false | pin false | `enterpriseRootsEnabled(false)` | none |
| Extension | built-in, MV2, `<all_urls>` | host list + CSP + sender check (4.2) | manifest | none |

### 4.1 `GeckoEngine.kt` (all names exist in the 160 AAR [V])
```kotlin
// add to the existing GeckoRuntimeSettings.Builder():
.enterpriseRootsEnabled(false)   // Gecko uses Mozilla's roots, not the 2021-07 Android store
.setLnaEnabled(true).setLnaBlocking(true).setLnaBlockTrackers(true)  // the app already DENIES PERMISSION_LOCAL_NETWORK_ACCESS / _LOCAL_DEVICE_ACCESS
.translationsOfferPopup(false)   // never fetch translation models over the hotspot
// replace `return GeckoRuntime.create(ctx, settings)` with:
return GeckoRuntime.create(ctx, settings).also { rt ->
    rt.settings.setFingerprintingProtection(true)                // FPP (default only in private tabs). Android targets: CanvasRandomization, EfficientCanvasRandomization,
    rt.settings.setFingerprintingProtectionPrivateBrowsing(true) //   FontVisibilityLangPack, JSMathFdlibm, ScreenAvailToResolution, NavigatorHWConcurrencyTiered, MaxTouchPointsCollapse, IMEStyle
    rt.settings.setCertificateTransparencyMode(2)                // enforce (= engine default)
    rt.settings.setPostQuantumKeyExchangeEnabled(true)           // X25519MLKEM768 for TLS and HTTP/3 (= default)
    val u = GeckoPreferenceController.PREF_BRANCH_USER
    fun b(n: String, v: Boolean) = GeckoPreferenceController.SetGeckoPreference.setBoolPref(n, v, u)
    GeckoPreferenceController.setGeckoPrefs(listOf(              // GeckoViewPreferences.sys.mjs has NO allow-list; runs before the first GeckoSession.open()
        b("dom.push.enabled", false), b("dom.webnotifications.enabled", false),        // no push websocket, no prompts (the delegate denies them anyway)
        b("security.certerrors.mitm.auto_enable_enterprise_roots", false),             // no default today (NetErrorParent.primeMitm would throw); pin so a future default cannot import OS roots
        b("media.peerconnection.ice.default_address_only", true)))
}
```
Keep as is: TRR mode 2, `ALLOW_ALL`, `fissionEnabled(false)`, `extensionsProcessEnabled(false)`, `aboutConfigEnabled(false)`, `remoteDebuggingEnabled(false)`, GPC, strict ETP, bounce-tracking protection. Read effective values on the A5 diagnostics page with `GeckoPreferenceController.getGeckoPrefs(listOf(...))`.
### 4.2 Native-messaging bridge and the built-in extension
```kotlin
// ShieldsBridge.onMessage, first lines: only OUR background page may ask (content scripts use runtime.sendMessage, not native messaging)
if (nativeApp != "kestrel" || sender.webExtension.id != EXTENSION_ID || sender.environmentType != WebExtension.MessageSender.ENV_TYPE_EXTENSION) return null
// ShieldsBridge.install, in the ensureBuiltIn success callback (private tabs may otherwise lack the extension [U]):
runtime.webExtensionController.setAllowedInPrivateBrowsing(ext, true)
```
```json
"permissions": ["webRequest","webRequestBlocking","nativeMessaging","geckoViewAddons","http://*/*","https://*/*","ws://*/*","wss://*/*"],
"content_security_policy": "script-src 'self'; object-src 'none'"
```
`extensionsProcessEnabled(false)` puts the background page in the PARENT process (`extensions.webextensions.remote` default false [V]): acceptable only because the extension is first-party; never add an install UI or `extensionsWebAPIEnabled`.
### 4.3 Android layer (both builds unless noted)
```kotlin
// ExternalIntents.planUnchecked, right after Intent.parseUri(...): a page must not fire DELETE / UNINSTALL / CALL ...
if (intent.action != null && intent.action !in setOf(Intent.ACTION_VIEW, Intent.ACTION_SENDTO, Intent.ACTION_SEND)) return null
if (intent.data?.scheme?.lowercase() in setOf("package", "android-app")) return null
Regex("[\\\\/:*?\"<>|\\p{Cc}\\p{Cf}]")   // Downloads.fileName: U+202E would make "gpj.exe" display as "exe.jpg"
if (Build.VERSION.SDK_INT >= 33 && isPrivate) clip.description.extras = PersistableBundle().apply { putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true) }   // BrowserActivity.copy
val built = LocalDate.parse(org.mozilla.geckoview.BuildConfig.MOZ_APP_BUILDID.take(8), DateTimeFormatter.BASIC_ISO_DATE)   // About (G): "20261104"
val ageDays = ChronoUnit.DAYS.between(built, LocalDate.now())   // red above 42; also print the SHA-256 of packageManager.getPackageInfo(packageName, GET_SIGNING_CERTIFICATES).signingInfo
```
```xml
<application ... android:intentMatchingFlags="enforceIntentFilter">   <!-- both manifests: Android 16+ rejects explicit intents that match no filter; no effect on API 29 -->
<receiver android:name="androidx.profileinstaller.ProfileInstallReceiver" tools:node="remove" />   <!-- geckoapp: the only extra exported receiver (permission DUMP) -->
<!-- network_security_config.xml (keep base-config; NSC covers HttpURLConnection + WebView, not Gecko's own stack [U]): -->
<domain-config cleartextTrafficPermitted="false"><domain includeSubdomains="true">easylist.to</domain><domain includeSubdomains="true">ublockorigin.github.io</domain><domain includeSubdomains="true">secure.fanboy.co.nz</domain></domain-config>
```
Manifest audit [V]: exported = `MainActivity` (MAIN/LAUNCHER, VIEW http(s) BROWSABLE, WEB_SEARCH, SEND text/plain) + AndroidX `ProfileInstallReceiver`; all GeckoView services (40 `isolatedTab*` and 40 `tab*`), `GeckoClipboardContentProvider` and `InitializationProvider` are `exported=false`; no PendingIntent (add `FLAG_IMMUTABLE` + explicit component if one ever appears), no FileProvider, no `addJavascriptInterface`; `FLAG_SECURE` + no-autofill on private tabs, `allowBackup=false` + empty extraction rules, R8 full mode (obfuscation is not a control for an open-source app; keep line numbers). No runtime tamper check: the source is public and sideloaded, a check is removed by the attacker; use update continuity (section 6), CI pin and attestations. Untested idea: `taskAffinity=""` on a singleTask launcher can spawn duplicate tasks.
### 4.4 CI (keep what `tools/test_handoff.sh` greps: no branch `push:` trigger, gates first, literal `gh release create`, `--target`, `contents: write` in android.yml; new workflows = `schedule` + `workflow_dispatch` only)
```yaml
permissions: {}                       # android.yml top level (today: contents: write for every job)
jobs:
  check:    { permissions: { contents: read } }      # add this key to the existing check / test-jvm / test-js jobs
  build-gecko:                        # NO secrets here: Gradle plugins and npm packages run arbitrary code
    permissions: { contents: read }
    steps:
      - uses: actions/checkout@<40-hex>   # v5.x
        with: { persist-credentials: false }
      - run: ./gradlew :geckoapp:assembleRelease -Pkestrel.unsigned=true --console=plain   # build.gradle.kts: signingConfig = null when the property is set
      - uses: actions/upload-artifact@<40-hex>   # v4.x
        with: { name: unsigned-gecko, path: geckoapp/build/outputs/apk/release/*-unsigned.apk, retention-days: 1 }
  sign-publish:
    needs: [build-gecko]
    environment: release              # SIGNING_* secrets live ONLY here; required reviewer; later deployment branches = main + tags
    permissions: { contents: write, id-token: write, attestations: write }
    steps:                            # download-artifact -> apksigner (section 6) -> bash tools/verify-apk.sh gecko signed.apk -> attest -> release
      - uses: actions/attest@<40-hex>     # v4+; attest-build-provenance v4 is a wrapper of it
        with: { subject-path: kestrel-g-*.apk }
      - run: gh release create "$TAG" --draft --target "$GITHUB_SHA" ... && gh release upload "$TAG" kestrel-g-*.apk{,.sha256} && gh release edit "$TAG" --draft=false   # immutable releases lock tag + assets at publish
# Delete the 'Keep the APK for inspection (failed runs only)' steps (they publish a signed apk of a failed run). Same split for the web build.
# Resolve a pin: gh api repos/actions/checkout/git/ref/tags/v5 --jq .object  (type "tag" -> gh api repos/actions/checkout/git/tags/<sha> --jq .object.sha). 40-hex values are NOT listed: the sandbox cannot read other repos.
```
```yaml
# .github/dependabot.yml   (gradle will not see `$geckoVersion` string templates [U]; gecko-watch covers it)
version: 2
updates:
  - { package-ecosystem: github-actions, directory: /, schedule: { interval: weekly }, cooldown: { default-days: 3 } }
  - { package-ecosystem: gradle, directory: /, schedule: { interval: weekly }, cooldown: { default-days: 3 } }
```
```bash
# .github/workflows/gecko-watch.yml: on: { schedule: [{ cron: '41 5 * * 1,4' }], workflow_dispatch: {} }  permissions: { contents: read, issues: write }
have=$(sed -n 's/.*val geckoVersion = "\([^"]*\)".*/\1/p' geckoapp/build.gradle.kts)
new=$(curl -fsS https://maven.mozilla.org/maven2/org/mozilla/geckoview/geckoview-armeabi-v7a/maven-metadata.xml | sed -n 's|.*<release>\(.*\)</release>.*|\1|p')
[ "$have" != "$new" ] && gh issue create --title "GeckoView $new available (have $have)" --body "Advisories: https://www.mozilla.org/en-US/security/advisories/ - bump geckoVersion, regenerate verification-metadata, ship"   # list open issues first to dedupe
# B5, once, in a session with network; commit gradle/verification-metadata.xml + gradle/verification-keyring.keys and review every "Generated by Gradle" origin:
./gradlew --write-verification-metadata pgp,sha256 --export-keys :core:test :tools:build :app:testDebugUnitTest :app:lintRelease :app:assembleRelease :geckoapp:assembleRelease
# XML: <verify-metadata>true</verify-metadata> <verify-signatures>true</verify-signatures> <trusted-keys><trusted-key id="827E658608679618CD349F93678E455D76767AA3" group="org.mozilla.geckoview"/></trusted-keys>
#   (that is the signer of the 160 AAR [V], owner [U]; AAR sha256 02446a96cae04bc1425cbfd7de8ac8c96bc352fd79bb34462c59018f6d4c0c43 equals Mozilla's published .aar.sha256 [V])
# gradle-wrapper.properties: distributionSha256Sum=<8.14.3 value from https://gradle.org/release-checksums/>; setup-gradle with validate-wrappers: true
# weekly: osv-scanner scan --lockfile gradle/verification-metadata.xml (GeckoView advisories probably absent from OSV [U]); SBOM: ./gradlew cyclonedxBom --init-script cyclonedx-init.gradle.kts (org.cyclonedx.bom 3.5.1 [S], Android variants [U])
# CodeQL (schedule only): languages java-kotlin with build-mode manual (./gradlew :core:compileKotlin :app:compileReleaseKotlin :geckoapp:compileReleaseKotlin), javascript-typescript, actions; build-mode none would skip Kotlin [S]
# verify a download on a PC: gh attestation verify kestrel-g-X.apk -R <owner>/<repo>; apksigner verify --print-certs kestrel-g-X.apk (== signing/CERT_SHA256); sha256sum -c kestrel-g-X.apk.sha256
```

## 5. What the WebView build (Kestrel, Chromium 74) can and cannot do
- **Can (done) [V]:** app-level HTTPS-first (`NavPolicy`), `MIXED_CONTENT_NEVER_ALLOW`, file/content access off, SSL errors never auto-proceed, third-party cookies off, request blocking, popup gating, private process + `FLAG_SECURE`, sanitised `intent:`, download checks, no JS bridges. NSC (4.3) applies to it.
- **Cannot (engine frozen Apr 2019):** DoH, ECH, post-quantum TLS, HTTPS-First mode, CT, CRLSets/revocation (trust = Android store of 2021-07), Safe Browsing (needs Play services; the manifest flag is a no-op [S]), SameSite-Lax-by-default, CHIPS, site isolation, Private Network Access; engine CVEs fixed after M74 stay open (e.g. CVE-2019-13720, CVE-2020-6418, both exploited in the wild [S]).
- **Correction:** ARCHITECTURE.md section 5 claims Chromium's default referrer policy is strict-origin-when-cross-origin; that changed in Chrome 85 (Aug 2020), so WebView 74 sends the FULL URL cross-origin [S]; `shouldInterceptRequest` cannot add headers, so subresources cannot be fixed.
- **Mitigations in order:** owner's Private DNS (Quad9), Kestrel G for anything with a login, keep the NTP "outdated engine" banner, retire it eventually (B23).

## 6. Private key and v3 rotation (commands run on throw-away keys, build-tools 36.0.0 [V]; platform install behaviour [U])
```bash
# OWNER, on his own PC. Never inside this repo tree or a Claude session (autosave pushes everything not ignored). Back the .p12 and passwords up OFFLINE twice.
keytool -genkeypair -keystore kestrel-release.p12 -storetype PKCS12 -alias kestrel -keyalg RSA -keysize 3072 -sigalg SHA256withRSA -validity 36500 -dname "CN=Kestrel release"
apksigner rotate --out signing/kestrel.lineage \
  --old-signer --ks signing/dev/kestrel-dev.jks --ks-key-alias kestrel-dev --ks-pass pass:kestrel-dev \
       --set-installed-data true --set-shared-uid false --set-permission false --set-auth false --set-rollback false \
  --new-signer --ks kestrel-release.p12 --ks-key-alias kestrel          # commit signing/kestrel.lineage (public); signing/CERT_SHA256 := SHA-256 of the NEW cert
gh secret set SIGNING_KEYSTORE_BASE64 --env release < <(base64 -w0 kestrel-release.p12)   # + SIGNING_STORE_PASSWORD, SIGNING_KEY_ALIAS, SIGNING_KEY_PASSWORD
# CI sign step (key under $RUNNER_TEMP, removed by an always() step):
apksigner sign --ks "$KS" --ks-key-alias "$ALIAS" --ks-pass env:STORE_PW --key-pass env:KEY_PW --lineage signing/kestrel.lineage --rotation-min-sdk-version 28 \
  --min-sdk-version 29 --v1-signing-enabled false --v2-signing-enabled false --v3-signing-enabled true --out signed.apk unsigned.apk
apksigner verify -Werr -v --print-certs --min-sdk-version 29 signed.apk   # "Verified using v3 scheme: true", Signer #1 = NEW cert, so tools/verify-apk.sh keeps working
```
- Why these flags: with only the new key `apksigner` refuses v2 ("oldest signer in the lineage is missing"); minSdk is 29 so v1/v2 are unnecessary; the default rotation targets Android 13+ (v3.1) and would leave this Android 10 tablet seeing the OLD key [S]. `--set-rollback false` stops the public key from retaking the app on devices that took the rotated build; `--set-installed-data true` is required for the first in-place update [V: apksigner help]. `apksigner lineage --in signed.apk --print-certs` must list both certificates.
- Emulator gate (Android 10 x86 AVD recipe: docs/handoff/REVIEW-2026-11-11.md; the 2.3 MB WebView APK is a valid proxy): install the dev-signed APK, `adb install -r` the rotated one (expect Success, `dumpsys package` shows the new cert), then `adb install -r` an old-key-only APK (expect INSTALL_FAILED_UPDATE_INCOMPATIBLE = the public key lost its power). If a step fails: new applicationId + new key, move bookmarks by hand, uninstall the old app (until then the old package stays exposed).
- Until the tablet has installed the rotated build the old key still works for it: ship that build first and install nothing else before it.

## 7. Standards map and reproducible builds
| OWASP MASVS group | Status | Where |
|---|---|---|
| STORAGE | no backup, private tabs not persisted, FLAG_SECURE; OS storage unencrypted | owner screen lock, clear-on-exit |
| CRYPTO / AUTH | no app crypto at rest, no accounts, login autofill off; the signing key is the crypto asset | section 6 |
| NETWORK | HTTPS-first, DoH, CRLite/CT, system CAs only (no user CAs) | 4.0, 4.3 |
| PLATFORM | one exported activity, intent plan, permission prompts | 4.3 |
| CODE | pinned + verified dependencies, CodeQL, R8 | 4.4 |
| RESILIENCE | not a goal for open source; update continuity + attestation instead | 6, 4.4 |
| PRIVACY | no telemetry, GPC, query stripping, strict ETP; Gecko's calls to Mozilla/Google are measured first | B14 |

| API | Change that matters [S] | Effect here |
|---|---|---|
| 29 (tablet) | background activity starts and background clipboard reads blocked, scoped storage | external launches need a foreground gesture (done); DownloadManager exempt |
| 31 / 33 | `android:exported` mandatory; `EXTRA_IS_SENSITIVE`, POST_NOTIFICATIONS | present; clipboard flag (4.3) |
| 34 | implicit intents reach only exported components; mutable PendingIntent needs a component; receiver export flags; zip path traversal throws | nothing to change today |
| 35 / 36 | TLS 1.0/1.1 refused for apps targeting 35; Safer Intents (opt-in `intentMatchingFlags`), Local Network permission (opt-in now, enforced later, WebView inherits), orientation limits ignored from sw600dp | tablet is ~564 dp; add the flag |

Reproducible builds [U]: promising for `:app` (our code only) and plausible for `:geckoapp` (Mozilla's AAR is pinned by hash and copied unchanged). Remove variables: signature (compare unsigned APKs or use apksigcopier), AGP VCS info, Gradle/JDK/build-tools versions (Temurin 21.x, 36.0.0, runner image), absolute paths. Test = B21; claim nothing until it passes.

## 8. Evidence (claim -> source)
- **Gecko defaults** (branch `release` of the official GitHub mirror = 160.0.1): https://raw.githubusercontent.com/mozilla-firefox/firefox/release/modules/libpref/init/StaticPrefList.yaml and https://raw.githubusercontent.com/mozilla-firefox/firefox/release/browser/config/version.txt . FPP targets: https://raw.githubusercontent.com/mozilla-firefox/firefox/release/toolkit/components/resistfingerprinting/RFPTargetsDefault.inc . API: https://mozilla.github.io/geckoview/javadoc/mozilla-central/org/mozilla/geckoview/GeckoRuntimeSettings.html , .../GeckoRuntimeSettings.Builder.html , .../WebExtensionController.html
- **In the AAR [V]:** `omni.ja` (`armeabi-v7a/greprefs.js`, `components/components.manifest`, `chrome/geckoview/content/geckoview.js`, `modules/AppConstants.sys.mjs`, `defaults/settings/main/doh-providers.json`, `modules/GeckoViewPreferences.sys.mjs`, `moz-src/toolkit/actors/NetErrorParent.sys.mjs`) and javap of `GeckoRuntimeSettings`, `ContentBlocking$Settings`, `GeckoPreferenceController`, `WebExtension$MessageSender` (Java defaults: enterprise roots false, isolated process false, processCount 2, SB malware/phishing true, providers google/google4/google5).
- **Mozilla:**
  - HTTPS-First in 136 https://attackanddefense.dev/2025/03/31/https-first-in-firefox-136.html
  - HTTPS RRs via platform DNS, Android 10+ https://groups.google.com/a/mozilla.org/g/dev-platform/c/oh_Tk0iLT9A/m/k7XztM6hBgAJ
  - ECH = RFC 9849 https://www.rfc-editor.org/rfc/rfc9849 and https://support.mozilla.org/en-US/kb/understand-encrypted-client-hello (snippet only)
  - CRLite on Android (Fx 145) https://bugzilla.mozilla.org/show_bug.cgi?id=1988612
  - CT https://wiki.mozilla.org/SecurityEngineering/Certificate_Transparency (snippet)
  - SB v5 in 147 (secondary) https://cyberinsider.com/firefox-147-tightens-web-security-reduces-data-sharing-with-google/
  - advisory https://www.mozilla.org/en-US/security/advisories/mfsa2026-125/
  - Maven https://maven.mozilla.org/maven2/org/mozilla/geckoview/geckoview-armeabi-v7a/maven-metadata.xml (`.aar.sha256`, `.aar.asc` beside the AAR).
- **Isolation:** https://firefox-source-docs.mozilla.org/mobile/android/geckoview/project/isolated-process/overview.html ; bugs https://bugzilla.mozilla.org/show_bug.cgi?id=1565196 (meta), 1989379 (IME), 1810736 (media), 1988531 (fixed in 159), 1985804 ; Fission on Android https://connect.mozilla.org/t5/discussions/fission-goes-live-on-android-please-share-your-feedback/td-p/133911 (403 for me, search snippets) and https://discuss.privacyguides.net/t/site-isolation-fission-now-appears-to-be-active-in-firefox-on-android/34899 .
- **Android:**
  - https://developer.android.com/about/versions/14/behavior-changes-14 , .../15/behavior-changes-15 , .../16/behavior-changes-16
  - NSC https://developer.android.com/privacy-and-security/security-config (CT = Android 16+, ECH = 17+)
  - https://developer.android.com/tools/apksigner
  - https://source.android.com/docs/security/features/apksigning/v3 ("not recommended for Android 12 and earlier") and .../v3-1
  - Chrome 85 referrer https://developer.chrome.com/blog/referrer-policy-new-chrome-default
  - WebView Safe Browsing needs GMS https://chromium.googlesource.com/chromium/src/+/ef290da170d481d1ef1c9ca4e11d156cfaf09afe/android_webview/browser/safe_browsing/README.md
  - CVEs https://www.helpnetsecurity.com/2020/02/26/cve-2020-6418/ , https://attackerkb.com/topics/cve-2019-13720
  - carrier tampering https://transition.fcc.gov/bureaus/eb/News_Releases/DOC-338091A1.html
  - Quad9 https://docs.quad9.net/Setup_Guides/Android/Android_9%2B_%28Encrypted%29/ , https://quad9.net/service/service-addresses-and-features/
  - lists https://urlhaus.abuse.ch/api/ , https://openphish.com/phishing_feeds.html
  - MASVS https://mas.owasp.org/MASVS/
  - RFC 9460 (HTTPS RR), 8484 (DoH), 7858 (DoT), 8446, 8996, 6797 (HSTS), 6962/9162 (CT); SLSA https://slsa.dev/spec/v1.0/levels [U].
- **Supply chain:**
  - https://docs.gradle.org/8.14.3/userguide/dependency_verification.html
  - https://docs.github.com/en/actions/security-for-github-actions/security-guides/security-hardening-for-github-actions
  - https://github.blog/changelog/2025-08-15-github-actions-policy-now-supports-blocking-and-sha-pinning-actions/
  - https://docs.github.com/en/actions/concepts/security/workflow-dependency-lockfiles (`actions.lock`, `gh actions-lock`, GA date unconfirmed)
  - environments https://docs.github.com/en/actions/managing-workflow-runs-and-deployments/managing-deployments/managing-environments-for-deployment (Free plan: public repos; 6 reviewers; "Selected branches and tags")
  - https://github.com/actions/attest-build-provenance
  - https://docs.github.com/en/code-security/supply-chain-security/understanding-your-software-supply-chain/immutable-releases
  - CodeQL https://docs.github.com/en/code-security/code-scanning/creating-an-advanced-setup-for-code-scanning/codeql-code-scanning-for-compiled-languages and https://codeql.github.com/docs/codeql-overview/supported-languages-and-frameworks/
  - https://google.github.io/osv-scanner/supported-languages-and-lockfiles/
  - https://github.com/CycloneDX/cyclonedx-gradle-plugin
  - https://docs.github.com/en/code-security/dependabot/working-with-dependabot/dependabot-options-reference .

## 9. Not verified / open
1. Nothing ran on a device or emulator (no KVM): Android 10 acceptance of the rotated APK, every pref's effect on the tablet, private-tab extension default, LNA/CT/CRLite really active on Android. Defaults come from the mirror at 160.0.1 (read through a summarising fetch tool, chunked) plus the AAR; confirm with `getGeckoPrefs` on the diagnostics page.
2. Byte cost of Remote Settings, Safe Browsing v5 and CRLite is unknown (B14); ordering between `setGeckoPrefs` and the first session open is untested.
3. Fission on Android Release (Mozilla posts disagree; the Connect thread returned 403); `Selected branches` rules for `workflow_dispatch` from session branches; `ubuntu-latest` keeping SDK 37.
4. GitHub action SHAs (sandbox reads this repo only); Dependabot on `$geckoVersion`; OSV coverage of GeckoView; CycloneDX on AGP variants; `actions/attest` SBOM input; `gh actions-lock` GA; SLSA L2/L3 wording; Scorecard.
5. Owner of PGP key 827E...7AA3 (a third-party thread ties key id 678E455D76767AA3 to release@mozilla.com): compare with Mozilla's published key before trusting. Safe Browsing key terms. Android Private DNS strict mode has no plaintext fallback (AOSP doc not fetched); the OEM may hide the setting. URLhaus/OpenPhish terms need the owner's reading before any list is added.
6. `taskAffinity=""`, CT-in-NSC parsing on Android 10, Cloudflare `security.cloudflare-dns.com` filtering: not evaluated.
