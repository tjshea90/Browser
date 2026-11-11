# SECURITY research (TASKS R4): Kestrel and Kestrel G - 2026-11-11
Owner ask: "make the browser secure by using the newest security tools and standards". Research only: no code, build, commit or CI change was made; this is the only file written. Siblings: ADBLOCK_ENGINE.md (R5), DOWNLOADS.md (R2), FREEZE.md (R6).
Tags: **[V]** verified here on the shipped bits (GeckoView 160.0.20261104181552 AAR via javap/grep, Mozilla Maven `.sha256`/`.asc`, `apksigner` on throw-away keys) | **[S]** vendor document, URL in section 7 | **[U]** NOT verified, see section 8.
Priority: P0 before the next release | P1 within two releases | P2 backlog. Effort: S < 1 h, M < 1 day, L = days. "Break" = chance of breaking sites or UX.

## 1. Decisions (read this first)
1. **Two engines, two stances.** Kestrel G (Gecko 160, release channel, build 20261104181552 = newest on maven.mozilla.org [V]) is the build to harden and the only one that can meet current standards. Kestrel (system WebView = Chromium 74, Apr 2019, unpatchable here) stays a legacy fallback: keep its app-level hardening, add nothing big, never log in with it (section 5).
2. **P0 - the PUBLIC dev signing key is the biggest hole.** Anyone can sign an APK with `signing/dev/kestrel-dev.jks` (password is in the repo) and Android installs it as a normal update that inherits cookies, sessions and permissions. Decision: private key in an Actions *environment* secret + v3 lineage so the tablet keeps its data (commands tested, section 6). Acceptance by Android 10 is UNTESTED and AOSP calls rotation "not recommended" on Android 12 and earlier [S][U]: gate on an Android 10 emulator install, fallback = new applicationId.
3. **P0 - CI trusts too much.** `android.yml` gives EVERY job `contents: write`, writes the signing key into the workspace of the job that runs Gradle/npm (third-party code), uses tag-pinned actions, has no Gradle dependency verification, and its failure step uploads a SIGNED apk as a public artifact. Fix in 4.4: per-job permissions, build (no secrets) / sign+publish split behind environment `release` with a required reviewer, SHA pins, `verification-metadata.xml`, attestations.
4. **P0 - patch cadence is the real browser security on a device without Play updates.** Firefox 160 (2026-11-10) fixed 99 CVEs, 57 rated high, about 12 sandbox escapes (MFSA 2026-125 [S]). We ship the newest GeckoView today; add `gecko-watch` (twice weekly) and an "engine age" line in About (warn above 42 days); ship Gecko bumps within days of the release.
5. **Gecko 160 already does most of "the newest standards" - pin it, do not rebuild it [V]:** HTTPS-First (default since Fx 136), TLS 1.2-1.3 only, hybrid post-quantum key exchange (X25519MLKEM768), ECH (+GREASE 100%) when an HTTPS RR is found (our DoH, or the Android 10+ native query), CRLite enforcing ("compat" channel on Android), Certificate Transparency enforcing, HSTS preload, mixed-content upgrade + active block, Safe Browsing v5, Local Network Access blocking, GPC, referrer trimming, partitioned third-party cookies. Pin them in code (4.1) so a future default change cannot silently weaken them.
6. **Real gaps in `GeckoEngine.kt` (P1):** fingerprinting protection (FPP) is off in normal tabs; Push/Notification machinery on; LNA tracker blocking off; `ShieldsBridge` answers any sender; extension holds `<all_urls>` and may not run in private tabs [U]; content processes run as the app's own UID because `isolatedProcessEnabled` defaults to false [V].
7. **Do NOT enable now:** isolated content processes (Mozilla meta-bug 1565196 open with 16 blockers incl. IME 1989379, media 1810736, crash handler [S]); Fission (RAM; Mozilla's Android rollout is contradictory [U]); RFP (timezone/dark-mode/timer breakage, FPP gives the cheap targets); engine HTTPS-Only (duplicates NavPolicy); TLS 1.3-only; TRR mode 3 or strict fallback by default (flaky hotspot -> DNS failures); enterprise roots; JIT off; WebRTC off; opt-in cookie partitioning (breaks embeds).
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
| T3 Forged / poisoned filter list | upstream compromise, or TLS MITM with a CA the 2021-07 Android store still trusts (updater uses `HttpURLConnection`, not Mozilla's roots) | block banking JS, hide warning banners, allow-list a phishing host | HTTPS only, size caps, canaries, round-trip check -> B17, B20 |
| T4 Look-alike APK / tampered release | public dev key signs a "valid" update; Release asset swapped | full takeover with existing data | CI cert pin -> B1, B11 |
| T5 CI / supply chain | malicious action, Gradle plugin, Maven artifact or npm package; a rogue or buggy session branch dispatching the workflow with secrets | signing-key theft, trojaned APK | -> B2 B3 B4 B5 B13 B15 |
| T6 Other apps / physical | task hijack of the exported singleTask activity, `intent:` with arbitrary ACTION_*, stolen unencrypted tablet, kernel LPE after a renderer exploit | data theft, root | FLAG_SECURE private tabs, allowBackup=false, no exported providers -> B10 B17, screen lock |
| T7 Stale engine | WebView 74 forever; Gecko if releases slip | any N-day | -> B6, section 5 |

## 3. Prioritized backlog (what to build, in this order)
| # | Pri | Item | Eff | Break |
|---|---|---|---|---|
| B1 | P0 | Private release key + v3 lineage + re-pin `signing/CERT_SHA256` (section 6); emulator gate first | M | none |
| B2 | P0 | Workflow `permissions: {}` at top, per job minimum; `persist-credentials: false`; delete the failure step that uploads a signed apk | S | none |
| B3 | P0 | Split build (no secrets) from sign+publish in environment `release` (required reviewer; later deployment branches = `main` + tags) | M | approval tap per release |
| B4 | P0 | Pin every `uses:` to a 40-hex SHA with a `# vX.Y.Z` comment; turn on repo policy "Require actions pinned to a full-length commit SHA"; Dependabot for actions | S | none |
| B5 | P0 | `gradle/verification-metadata.xml` (sha256 for all, PGP for `org.mozilla.geckoview`) + wrapper `distributionSha256Sum` + wrapper validation | M | none |
| B6 | P0 | `gecko-watch` workflow + engine-age line in About (4.4, 4.3) | S | none |
| B7 | P0 | Owner actions of decision 10 (Private DNS strict fails closed: if the carrier blocks port 853 set it back to Automatic) | S | low |
| B8 | P1 | `GeckoEngine.kt` pins + FPP + pref list (4.1) | S | low |
| B9 | P1 | Bridge sender check, extension host permissions/CSP, private-tab allow (4.2) | S | none |
| B10 | P1 | `intent:` action allow-list, filename format-char strip, clipboard flag (4.3); fold into DOWNLOADS.md #13 | S | none |
| B11 | P1 | `actions/attest` provenance (+ SBOM) per APK, immutable releases, verify commands in the Release text | M | none |
| B12 | P1 | CodeQL weekly (java-kotlin manual build, javascript, actions), secret scanning + push protection, SECURITY.md, OSV weekly on `verification-metadata.xml` | M | none |
| B13 | P1 | Protected `main` + CODEOWNERS for `.github/**`, `signing/**`, `gradle/verification-metadata.xml` (TASKS "main" decision); session branches stay `ccr-*` | M | sessions can no longer touch the release path |
| B14 | P1 | 24 h idle data measurement (Remote Settings, Safe Browsing, CRLite); decide `services.settings.poll_interval` / Settings switch | S | none |
| B15 | P1 | `web-tests`: lockfile, `npm ci --ignore-scripts`, pinned Playwright | S | none |
| B16 | P2 | `network.prefetch-next=false`, `network.dns.disablePrefetch=true` (privacy + hotspot data; coordinate with GECKO_SPEED) | S | low |
| B17 | P2 | NSC `cleartextTrafficPermitted="false"` for the 3 list hosts; `intentMatchingFlags`; drop `ProfileInstallReceiver`; CT in NSC only after an Android 10 run (4.3) | S | none |
| B18 | P2 | Own Google Safe Browsing key (Mozilla's embedded key is Mozilla's, ToS grey) or Settings OFF switch | M | none |
| B19 | P2 | Settings "Strict encrypted DNS" (`network.trr.strict_native_fallback=true`) + provider list (Mozilla Cloudflare / NextDNS / Quad9) | M | med on flaky links |
| B20 | P2 | CI-built list bundle signed with ECDSA P-256 (public key in the APK): independent of the stale CA store, smaller download (R5 update path) | L | none |
| B21 | P2 | Reproducibility job (unsigned APK built twice on clean runners, `cmp`), reusable-workflow build for SLSA L3 | M | none |
| B22 | P2 | Drop CAMERA / RECORD_AUDIO / location declarations if the owner never uses them (an exploited renderer cannot be granted what is not declared) | S | feature loss |
| B23 | P2 | Re-check `isolatedProcessEnabled` when bug 1565196 closes; re-check Fission "high value" strategy; retire the WebView build once G is trusted | - | - |

## 4. Exact settings and snippets
### 4.1 `GeckoEngine.kt` (all names exist in the 160 AAR [V])
```kotlin
// add to the existing GeckoRuntimeSettings.Builder():
.enterpriseRootsEnabled(false)   // static default is TRUE; GeckoView's Java default pushes false at startup. Pin. Gecko uses Mozilla's roots, not the 2021-07 Android store
.setLnaEnabled(true).setLnaBlocking(true).setLnaBlockTrackers(true)  // engine default true/true/false; the app already DENIES PERMISSION_LOCAL_NETWORK_ACCESS / _LOCAL_DEVICE_ACCESS
.translationsOfferPopup(false)   // never fetch translation models over the hotspot
// replace `return GeckoRuntime.create(ctx, settings)` with:
return GeckoRuntime.create(ctx, settings).also { rt ->
    rt.settings.setFingerprintingProtection(true)                // FPP (default only in private tabs). Android targets: CanvasRandomization, EfficientCanvasRandomization,
    rt.settings.setFingerprintingProtectionPrivateBrowsing(true) //   FontVisibilityLangPack, JSMathFdlibm, ScreenAvailToResolution, NavigatorHWConcurrencyTiered, MaxTouchPointsCollapse, IMEStyle
    rt.settings.setCertificateTransparencyMode(2)                // enforce (= engine default)
    rt.settings.setPostQuantumKeyExchangeEnabled(true)           // X25519MLKEM768 for TLS and HTTP/3 (= default)
    val u = GeckoPreferenceController.PREF_BRANCH_USER
    fun b(n: String, v: Boolean) = GeckoPreferenceController.SetGeckoPreference.setBoolPref(n, v, u)
    GeckoPreferenceController.setGeckoPrefs(listOf(              // GeckoViewPreferences.sys.mjs has NO allow-list; run before the first GeckoSession.open()
        b("dom.push.enabled", false), b("dom.webnotifications.enabled", false),        // no push websocket, no prompts (the delegate denies them anyway)
        b("security.certerrors.mitm.auto_enable_enterprise_roots", false),             // pref has no default today (NetErrorParent.primeMitm would throw): pin so a future default cannot import OS roots
        b("media.peerconnection.ice.default_address_only", true)))                     // mDNS host-candidate obfuscation is already on (greprefs.js)
}
```
Keep as is: `trustedRecursiveResolverMode(TRR_MODE_FIRST)`, `allowInsecureConnections(ALLOW_ALL)` (NavPolicy decides; engine HTTPS-First `dom.security.https_first=true` stays underneath), `fissionEnabled(false)`, `extensionsProcessEnabled(false)`, `aboutConfigEnabled(false)`, `remoteDebuggingEnabled(false)`, GPC, strict ETP, TCP (`cookieBehavior` 5), BTP. Read effective values from the A5 diagnostics page with `GeckoPreferenceController.getGeckoPrefs(listOf(...))`.
### 4.2 Native-messaging bridge and the built-in extension
```kotlin
// ShieldsBridge.onMessage, first lines: only OUR background page may ask (content scripts use runtime.sendMessage, not native messaging)
if (nativeApp != "kestrel" || sender.webExtension.id != EXTENSION_ID || sender.environmentType != WebExtension.MessageSender.ENV_TYPE_EXTENSION) return null
// ShieldsBridge.install, inside the ensureBuiltIn success callback (private tabs are otherwise unprotected by the extension [U]):
runtime.webExtensionController.setAllowedInPrivateBrowsing(ext, true)
```
```json
"permissions": ["webRequest","webRequestBlocking","nativeMessaging","geckoViewAddons","http://*/*","https://*/*","ws://*/*","wss://*/*"],
"content_security_policy": "script-src 'self'; object-src 'none'"
```
`extensionsProcessEnabled(false)` puts the background page in the PARENT process (`extensions.webextensions.remote` default false [V]): acceptable only because the extension is first-party; never expose an install UI or `extensionsWebAPIEnabled`.
### 4.3 Android layer (both builds unless noted)
```kotlin
// ExternalIntents.planUnchecked, right after Intent.parseUri(...): a web page must not fire DELETE / UNINSTALL / CALL ...
if (intent.action != null && intent.action !in setOf(Intent.ACTION_VIEW, Intent.ACTION_SENDTO, Intent.ACTION_SEND)) return null
if (intent.data?.scheme?.lowercase() in setOf("package", "android-app")) return null
// Downloads.fileName: also drop format chars (U+202E makes "gpj.exe" display as "exe.jpg")
Regex("[\\\\/:*?\"<>|\\p{Cc}\\p{Cf}]")
// BrowserActivity.copy(text, isPrivate): API 33+ hides the clip preview
if (Build.VERSION.SDK_INT >= 33 && isPrivate) clip.description.extras = PersistableBundle().apply { putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true) }
// About page (G): engine age + own signing certificate
val built = LocalDate.parse(org.mozilla.geckoview.BuildConfig.MOZ_APP_BUILDID.take(8), DateTimeFormatter.BASIC_ISO_DATE)  // 20261104
val ageDays = ChronoUnit.DAYS.between(built, LocalDate.now())  // red above 42
val cert = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES).signingInfo!!.apkContentsSigners[0].toByteArray()  // SHA-256 it, compare with README
```
```xml
<!-- both <application>: Android 16+ rejects explicit intents that match no filter (no effect on API 29) -->
android:intentMatchingFlags="enforceIntentFilter"
<!-- geckoapp manifest: the only extra exported receiver (permission DUMP) -->
<receiver android:name="androidx.profileinstaller.ProfileInstallReceiver" tools:node="remove" />
<!-- network_security_config.xml, keep base-config: NSC covers HttpURLConnection + WebView, NOT Gecko's own stack (inference [U]) -->
<domain-config cleartextTrafficPermitted="false"><domain includeSubdomains="true">easylist.to</domain><domain includeSubdomains="true">ublockorigin.github.io</domain><domain includeSubdomains="true">secure.fanboy.co.nz</domain></domain-config>
```
Manifest audit [V]: exported = `MainActivity` (MAIN/LAUNCHER, VIEW http(s) BROWSABLE, WEB_SEARCH, SEND text/plain) + AndroidX `ProfileInstallReceiver`; every GeckoView service (40 `isolatedTab*` + 40 `tab*` pairs), `GeckoClipboardContentProvider` and `InitializationProvider` are `exported=false`; no PendingIntent, no FileProvider, no `addJavascriptInterface`. `allowBackup=false` + empty extraction rules OK. Not worth doing: `taskAffinity=""` on a singleTask launcher (can spawn duplicate tasks; test first). Behaviour changes that matter at targetSdk 36 [S]: 34 implicit intents only reach exported components and mutable PendingIntents need a component; 35 TLS 1.0/1.1 refused by `HttpURLConnection`, secured background activity starts; 36 Safer Intents (above), Local Network permission (opt-in now, enforced later; WebView inherits it), orientation limits ignored from sw600dp (this tablet is ~564 dp). Android 10 itself already blocks background activity starts and background clipboard reads.
### 4.4 CI (keep the rules `tools/test_handoff.sh` greps: no branch `push:` trigger, gates first, literal `gh release create`, `--target`, `contents: write` in android.yml; new workflows are `schedule` + `workflow_dispatch` only)
```yaml
permissions: {}                       # android.yml top level (today: contents: write for every job)
jobs:
  check:    { permissions: { contents: read } }      # test-jvm and test-js identical
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
    steps:                            # download-artifact -> apksigner (section 6) -> tools/verify-apk.sh gecko signed.apk -> attest -> release
      - uses: actions/attest@<40-hex>     # v4+; attest-build-provenance v4 is a wrapper of it
        with: { subject-path: kestrel-g-*.apk }
      - run: gh release create "$TAG" --draft --target "$GITHUB_SHA" ... && gh release upload "$TAG" kestrel-g-*.apk{,.sha256} && gh release edit "$TAG" --draft=false   # immutable releases lock tag + assets at publish
# delete the 'Keep the APK for inspection (failed runs only)' steps: they publish a signed apk of a failed run. Same split for the web build.
# resolve a pin: gh api repos/actions/checkout/git/ref/tags/v5 --jq .object   (type "tag" -> gh api repos/actions/checkout/git/tags/<sha> --jq .object.sha). Or pinact / zizmor [U]. 40-hex values are NOT listed: the sandbox cannot read other repos.
```
```yaml
# .github/dependabot.yml (gradle will not see `$geckoVersion` string templates [U]; gecko-watch covers it)
version: 2
updates:
  - { package-ecosystem: github-actions, directory: /, schedule: { interval: weekly }, cooldown: { default-days: 3 } }
  - { package-ecosystem: gradle, directory: /, schedule: { interval: weekly }, cooldown: { default-days: 3 } }
# .github/workflows/gecko-watch.yml: on: { schedule: [{ cron: '41 5 * * 1,4' }], workflow_dispatch: {} }, permissions: { contents: read, issues: write }
have=$(sed -n 's/.*val geckoVersion = "\([^"]*\)".*/\1/p' geckoapp/build.gradle.kts)
new=$(curl -fsS https://maven.mozilla.org/maven2/org/mozilla/geckoview/geckoview-armeabi-v7a/maven-metadata.xml | sed -n 's|.*<release>\(.*\)</release>.*|\1|p')
[ "$have" != "$new" ] && gh issue create --title "GeckoView $new available (have $have)" --body "Advisories: https://www.mozilla.org/en-US/security/advisories/ ; bump geckoVersion, regenerate verification-metadata, ship"   # dedupe with gh issue list first
```
```bash
# B5 (run once in a session with network; commit gradle/verification-metadata.xml + gradle/verification-keyring.keys, review every "Generated by Gradle" origin)
./gradlew --write-verification-metadata pgp,sha256 --export-keys :core:test :tools:build :app:testDebugUnitTest :app:lintRelease :app:assembleRelease :geckoapp:assembleRelease :geckoapp:lintDebug
# <configuration> in the XML: <verify-metadata>true</verify-metadata> <verify-signatures>true</verify-signatures>
#   <trusted-keys><trusted-key id="827E658608679618CD349F93678E455D76767AA3" group="org.mozilla.geckoview"/></trusted-keys>   <- signer of the 160 AAR [V]; owner of the key [U]
# AAR sha256 02446a96cae04bc1425cbfd7de8ac8c96bc352fd79bb34462c59018f6d4c0c43 = Mozilla's published .aar.sha256 [V]
# gradle-wrapper.properties: distributionSha256Sum=<value for 8.14.3 from https://gradle.org/release-checksums/> ; CI: gradle/actions/setup-gradle with validate-wrappers: true
# Weekly: osv-scanner scan --lockfile gradle/verification-metadata.xml  (GeckoView advisories are probably NOT in OSV [U])
# SBOM without touching build files:  ./gradlew cyclonedxBom --init-script cyclonedx-init.gradle.kts  (init script applies org.cyclonedx.bom 3.5.1 to rootProject [S]; Android variants [U])
# CodeQL (codeql.yml, schedule only): matrix languages java-kotlin (build-mode: manual -> ./gradlew :core:compileKotlin :app:compileReleaseKotlin :geckoapp:compileReleaseKotlin), javascript-typescript, actions. build-mode none skips Kotlin [S]
# Verify a download (PC):  gh attestation verify kestrel-g-X.apk -R <owner>/<repo> ; apksigner verify --print-certs kestrel-g-X.apk (SHA-256 must equal signing/CERT_SHA256) ; sha256sum -c kestrel-g-X.apk.sha256
```

## 5. What the WebView build (Kestrel, Chromium 74) can and cannot do
- **Can (already done):** app-level HTTPS-first (`NavPolicy`), `MIXED_CONTENT_NEVER_ALLOW`, file/content access off, SSL errors never auto-proceed, third-party cookies off, request blocking, popup gating, private process + `FLAG_SECURE`, sanitised `intent:`, download checks, no JS bridges [V]. NSC (cleartext off for list hosts) applies to it.
- **Cannot (engine frozen at Apr 2019):** DoH, ECH, post-quantum TLS, HTTPS-First mode, CT, CRLSets/revocation (trust = Android store of 2021-07), Safe Browsing (needs Play services; the manifest flag is a no-op [S]), SameSite-Lax-by-default, CHIPS, site isolation, Private Network Access; engine CVEs fixed after M74 stay open (e.g. CVE-2019-13720, CVE-2020-6418, both exploited in the wild [S]).
- **Correction:** ARCHITECTURE.md section 5 says Chromium's default referrer policy is strict-origin-when-cross-origin; that changed in Chrome 85 (Aug 2020), so WebView 74 sends the FULL URL cross-origin over https [S]. It cannot be fixed for subresources (no header hook); `shouldInterceptRequest` cannot add headers.
- **Mitigations in order:** owner's Private DNS (Quad9), use Kestrel G for anything with a login, in-app "outdated engine" banner (exists on the NTP), eventually retire it (B23).

## 6. Private key and v3 rotation (commands run on throw-away keys with build-tools 36.0.0 [V]; platform install behaviour [U])
```bash
# OWNER, on his own PC. Never inside this repo tree or a Claude session (autosave pushes everything not ignored). Back the .p12 and passwords up OFFLINE twice.
keytool -genkeypair -keystore kestrel-release.p12 -storetype PKCS12 -alias kestrel -keyalg RSA -keysize 3072 -sigalg SHA256withRSA -validity 36500 -dname "CN=Kestrel release"
apksigner rotate --out signing/kestrel.lineage \
  --old-signer --ks signing/dev/kestrel-dev.jks --ks-key-alias kestrel-dev --ks-pass pass:kestrel-dev \
       --set-installed-data true --set-shared-uid false --set-permission false --set-auth false --set-rollback false \
  --new-signer --ks kestrel-release.p12 --ks-key-alias kestrel          # commit signing/kestrel.lineage (public data); signing/CERT_SHA256 := SHA-256 of the NEW cert
gh secret set SIGNING_KEYSTORE_BASE64 --env release < <(base64 -w0 kestrel-release.p12)   # + SIGNING_STORE_PASSWORD, SIGNING_KEY_ALIAS, SIGNING_KEY_PASSWORD
# CI sign step (key materialised under $RUNNER_TEMP, removed in an always() step):
apksigner sign --ks "$KS" --ks-key-alias "$ALIAS" --ks-pass env:STORE_PW --key-pass env:KEY_PW --lineage signing/kestrel.lineage --rotation-min-sdk-version 28 \
  --min-sdk-version 29 --v1-signing-enabled false --v2-signing-enabled false --v3-signing-enabled true --out signed.apk unsigned.apk
apksigner verify -Werr -v --print-certs --min-sdk-version 29 signed.apk    # "Verified using v3 scheme: true", Signer #1 = NEW cert, so tools/verify-apk.sh keeps working
```
- Why these flags: with only the new key `apksigner` refuses v2 ("oldest signer in the lineage is missing"); minSdk is 29 so v1/v2 are unnecessary; the default rotation targets Android 13+ (v3.1) and would leave this Android 10 tablet seeing the OLD key [S]. `--set-rollback false` is what stops the public key from retaking the app on devices that already took the rotated build; `--set-installed-data true` is required for the first in-place update [V: apksigner help].
- Emulator gate (Android 10 x86 AVD recipe: docs/handoff/REVIEW-2026-11-11.md; the 2.3 MB WebView APK is a valid proxy): install the dev-signed APK, `adb install -r` the rotated one (expect Success, `dumpsys package` shows the new cert), then `adb install -r` an old-key-only APK (expect INSTALL_FAILED_UPDATE_INCOMPATIBLE = the public key lost its power). If either step fails: new applicationId + new key, move bookmarks by hand, uninstall the old app (until then the old package stays exposed).
- Until the tablet has installed the rotated build the old key is still valid for it, so ship that build first and tell nobody to install other APKs before it.

## 7. Evidence (claim -> source)
- **Gecko defaults (160.0.1 = branch `release` of the official GitHub mirror):** StaticPrefList.yaml https://raw.githubusercontent.com/mozilla-firefox/firefox/release/modules/libpref/init/StaticPrefList.yaml (version https://raw.githubusercontent.com/mozilla-firefox/firefox/release/browser/config/version.txt): https_first/pbm/schemeless true, https_only false, tls min 3 max 4, enable_kyber true, ech grease 100, echconfig + http3_echconfig + upgrade_with_https_rr true, native_https_query true, crlite_mode 2 (`compat` on Android), CT mode 2, OCSP 2 on Android, mixed_content block_active = Android, upgrade_display true, lna enabled/blocking true and block_trackers false, referrer policy 2, FPP false / pbmode true / baseline true, RFP false, trr default provider mozilla.cloudflare-dns.com, trr request timeout 1500 ms, `fission.autostart` true but Java side overrides, `extensions.webextensions.remote` false.
- **In the AAR [V]:** `omni.ja` `armeabi-v7a/greprefs.js` (mDNS obfuscation true, services.settings.poll_interval 86400, safebrowsing prefs), `components/components.manifest` (Remote Settings timer), `chrome/geckoview/content/geckoview.js` (`RemoteSecuritySettings.init()`, `SafeBrowsing.init()`), `modules/AppConstants.sys.mjs` (release channel, Safe Browsing key present), `defaults/settings/main/doh-providers.json`, `GeckoViewPreferences.sys.mjs` (no allow-list), `NetErrorParent.sys.mjs`, javap of `GeckoRuntimeSettings`/`ContentBlocking$Settings` (Java defaults: enterprise roots false, isolated process false, processCount 2, SB malware/phishing true, providers google/google4/google5).
- FPP target list: https://raw.githubusercontent.com/mozilla-firefox/firefox/release/toolkit/components/resistfingerprinting/RFPTargetsDefault.inc . GeckoView API: https://mozilla.github.io/geckoview/javadoc/mozilla-central/org/mozilla/geckoview/GeckoRuntimeSettings.html , .../GeckoRuntimeSettings.Builder.html , .../WebExtensionController.html
- HTTPS-First shipped in 136: https://attackanddefense.dev/2025/03/31/https-first-in-firefox-136.html . HTTPS RRs through platform DNS (Android 10+): https://groups.google.com/a/mozilla.org/g/dev-platform/c/oh_Tk0iLT9A/m/k7XztM6hBgAJ . ECH = RFC 9849 (Mar 2026) https://www.rfc-editor.org/rfc/rfc9849 ; ECH needs DoH per https://support.mozilla.org/en-US/kb/understand-encrypted-client-hello (search snippet, page not fetchable). CRLite on Android (Fx 145): https://bugzilla.mozilla.org/show_bug.cgi?id=1988612 . CT enforcement (desktop 135): https://wiki.mozilla.org/SecurityEngineering/Certificate_Transparency (snippet). Safe Browsing v5 in Fx 147 (secondary): https://cyberinsider.com/firefox-147-tightens-web-security-reduces-data-sharing-with-google/ . Advisory: https://www.mozilla.org/en-US/security/advisories/mfsa2026-125/ . Maven metadata: https://maven.mozilla.org/maven2/org/mozilla/geckoview/geckoview-armeabi-v7a/maven-metadata.xml (plus `.aar.sha256`, `.aar.asc` next to the AAR).
- Isolated processes: https://firefox-source-docs.mozilla.org/mobile/android/geckoview/project/isolated-process/overview.html , bugs https://bugzilla.mozilla.org/show_bug.cgi?id=1565196 (meta), 1989379 (IME), 1810736 (media), 1988531 (media fixed in 159), 1985804 (perf). Fission on Android: https://connect.mozilla.org/t5/discussions/fission-goes-live-on-android-please-share-your-feedback/td-p/133911 (403 for me; search snippets only) and https://discuss.privacyguides.net/t/site-isolation-fission-now-appears-to-be-active-in-firefox-on-android/34899 .
- Android: https://developer.android.com/about/versions/14/behavior-changes-14 , .../15/behavior-changes-15 , .../16/behavior-changes-16 , https://developer.android.com/privacy-and-security/security-config (CT in NSC = Android 16+, ECH = 17+), https://developer.android.com/tools/apksigner , https://source.android.com/docs/security/features/apksigning/v3 ("not recommended for Android 12 and earlier") and .../v3-1 . Chrome 85 referrer: https://developer.chrome.com/blog/referrer-policy-new-chrome-default . WebView Safe Browsing needs GMS: https://chromium.googlesource.com/chromium/src/+/ef290da170d481d1ef1c9ca4e11d156cfaf09afe/android_webview/browser/safe_browsing/README.md . Chromium CVEs: https://www.helpnetsecurity.com/2020/02/26/cve-2020-6418/ , https://attackerkb.com/topics/cve-2019-13720 . Carrier tampering: https://transition.fcc.gov/bureaus/eb/News_Releases/DOC-338091A1.html . Quad9: https://docs.quad9.net/Setup_Guides/Android/Android_9%2B_%28Encrypted%29/ , https://quad9.net/service/service-addresses-and-features/ . URLhaus terms https://urlhaus.abuse.ch/api/ , OpenPhish https://openphish.com/phishing_feeds.html . OWASP MASVS groups (STORAGE, CRYPTO, AUTH, NETWORK, PLATFORM, CODE, RESILIENCE, PRIVACY): https://mas.owasp.org/MASVS/ . Standards: RFC 9460 (HTTPS RR), 8484 (DoH), 7858 (DoT), 8446 (TLS 1.3), 8996, 6797 (HSTS), 6962/9162 (CT); SLSA levels https://slsa.dev/spec/v1.0/levels [U].
- Supply chain: https://docs.gradle.org/8.14.3/userguide/dependency_verification.html , https://docs.github.com/en/actions/security-for-github-actions/security-guides/security-hardening-for-github-actions , https://github.blog/changelog/2025-08-15-github-actions-policy-now-supports-blocking-and-sha-pinning-actions/ , https://docs.github.com/en/actions/concepts/security/workflow-dependency-lockfiles (`.github/workflows/actions.lock`, `gh actions-lock`; GA date unconfirmed), https://docs.github.com/en/actions/managing-workflow-runs-and-deployments/managing-deployments/managing-environments-for-deployment (Free plan: public repos only; up to 6 reviewers; "Selected branches and tags"), https://github.com/actions/attest-build-provenance , https://docs.github.com/en/code-security/supply-chain-security/understanding-your-software-supply-chain/immutable-releases , https://docs.github.com/en/code-security/code-scanning/creating-an-advanced-setup-for-code-scanning/codeql-code-scanning-for-compiled-languages , https://codeql.github.com/docs/codeql-overview/supported-languages-and-frameworks/ (Kotlin 1.8-2.4.20, "GitHub Actions" language), https://google.github.io/osv-scanner/supported-languages-and-lockfiles/ , https://github.com/CycloneDX/cyclonedx-gradle-plugin , https://docs.github.com/en/code-security/dependabot/working-with-dependabot/dependabot-options-reference (`cooldown`).

## 8. Not verified / open
1. Nothing ran on a device or emulator (no KVM): Android 10 acceptance of the rotated APK, behaviour of every pref on the tablet, private-tab extension default, LNA/CT/CRLite actually active. Defaults above come from the mirror at 160.0.1 read through a summarising fetch tool (chunked) plus the AAR; confirm with `getGeckoPrefs` on the diagnostics page.
2. Byte cost of Remote Settings, Safe Browsing v5 and CRLite is unknown (B14). Ordering guarantee between `setGeckoPrefs` and the first session open is untested.
3. Fission on Android Release (Mozilla posts disagree; the Connect thread returned 403), `Selected branches` rule behaviour for `workflow_dispatch` from session branches, whether `ubuntu-latest` runners will keep SDK 37.
4. GitHub action SHAs (sandbox can read this repo only); Dependabot handling of `$geckoVersion`; OSV coverage of GeckoView; CycloneDX plugin on AGP variants; `actions/attest` SBOM input; `gh actions-lock` GA; SLSA L2/L3 wording.
5. Owner of PGP key 827E...7AA3 (a third-party thread ties key id 678E455D76767AA3 to release@mozilla.com): compare with Mozilla's published key before trusting. Safe Browsing key terms (Mozilla's key). Android Private DNS strict mode has no plaintext fallback (AOSP doc not fetched); the OEM may hide the setting. URLhaus/OpenPhish terms need the owner's reading before any list is added.
6. `taskAffinity=""`, CT-in-NSC on Android 10 parsers, Cloudflare `security.cloudflare-dns.com` filtering: not evaluated.
