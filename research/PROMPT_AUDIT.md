# Audit of every owner prompt against TASKS / OUTBOX / BUILDLOG / code (read-only)

Snapshot: repo HEAD `ac00a1f` (2026-11-12T15:38:57Z) when the audit started. The AA-worker kept committing while I read (`a0a56be` ckpt 1069 = "AA2 + AA1 core done"); AA/Z rows are as of the snapshot and are NOT counted as gaps.
Method: read in full INBOX.md (25 entries), TASKS.md (208 lines), OUTBOX.md (24 replies), BUILDLOG.md (26 releases), BRIEF.md, HANDOFF.md, CHECKPOINT.md, JOURNAL.md, research/AUDIT-2026-11.md, SPEED2.md, ENGINES.md, HW_SPEED.md (head), MP4_MUX.md (notes), REVIEW-V7.md, docs/FILTER-LISTS.md, docs/DESIGN.md. Code was checked by grep/read only (no build, no test, no network). A TASKS box was never trusted on its own: shipped-ness was checked against BUILDLOG notes and named tests/symbols exist in the tree.

Conventions: `alphaN` = Release `kestrel-g-v0.2.0/0.3.0-alphaN` in BUILDLOG.md. "OUTBOX 11-11T06:49Z" = the reply headed `## 2026-11-11T06:49:36Z` in OUTBOX.md. X/Y/Z/AA/V/R/... ids are TASKS.md ids. "emulator only" = verified on the Android 10 x86_64 emulator, never on the owner's tablet (the repo says so itself for almost everything). Statuses: DONE / PARTIAL / NOT DONE / UNANSWERED / OWNER-ONLY / SUPERSEDED / UNCLEAR. A re-send row (id `Mn.r`) adds no new request; its status cell lists the original rows.

COUNTS_PLACEHOLDER

## 0. How the message stream looks (read this first)
- 25 entries in INBOX.md; the first two were seeded by hand (written before the hook existed). **7 of the 25 are re-sends**: M5 and M7 = M4 verbatim; M8 and M9 = M6 verbatim; M11 = first sentence of M10; M18 = M17 verbatim; M24 = first paragraph of M23 (checked by hashing the normalised text).
- The re-sends coincide with **dead-session windows**: on 11-11 no commit exists between 08:50Z and 14:50Z, while the owner re-sent M7 (10:49Z), M8 (10:50Z) and M9 (12:51Z); on 11-12 no commit between 09:42Z and 12:20Z (M24 at 12:20Z), work resumed 13:55Z. OUTBOX has no reply between 11-11T06:49Z and 15:27Z. So "past prompts not responded to" are mostly these: every request in them WAS eventually answered by the later replies, but the owner had to ask 2-3 times.
- Older, pre-capture messages: the original build brief of 2026-11-10 and the follow-ups until 2026-11-11 ~01:50Z are not stored anywhere verbatim (INBOX starts at 03:18Z on 11-11). Their facts survive in BRIEF.md "Owner facts", docs/HARDWARE.md, JOURNAL.md. They are listed as M0.
- Numbering = INBOX order (M1..M25), M0 = pre-capture. TASKS labels: M2 = "Owner's request" at the top; M3 = Owner request 2; M4 = request 3; M6 = request 4; M10 = request 5; M12 = request 6; M13 = request 7; M15 = request 8; M16 = request 9; M17/M18 = request 10; M19 = message 11; M22 = message 12; M23 = message 13 (Z0-Z10); M25 = message 14 (AA1-AA4). **Not in TASKS at all: M1 (pre-capture), M20 ("Start building the next step").**

| M# | UTC | Notes |
| :- | :- | :- |
| M0 | 2026-11-10 ~22:00Z to 2026-11-11 ~01:50Z | pre-capture brief, reconstructed |
| M1 | 2026-11-11 ~01:50-02:00Z (seeded) | between the 0.1.0 delivery (01:48Z) and the engine decision commit (02:01Z) |
| M2 | 2026-11-11 ~02:40-03:04Z (seeded) | before the Actions workflow landed (03:04Z) |
| M3 | 2026-11-11T03:24:20Z | the big one |
| M4 | 2026-11-11T05:26:26Z | re-sent as M5, M7 |
| M5 | 2026-11-11T06:48:46Z | re-send of M4 |
| M6 | 2026-11-11T07:44:45Z | re-sent as M8, M9 |
| M7 | 2026-11-11T10:49:29Z | re-send of M4 (session was dead) |
| M8 | 2026-11-11T10:50:18Z | re-send of M6 (session was dead) |
| M9 | 2026-11-11T12:51:04Z | re-send of M6 (session was dead) |
| M10 | 2026-11-11T14:50:48Z | |
| M11 | 2026-11-11T16:17:31Z | sentence 1 of M10 |
| M12 | 2026-11-11T16:18:33Z | |
| M13 | 2026-11-11T16:26:31Z | had a screenshot attached (TASKS says 16:22Z) |
| M14 | 2026-11-11T18:00:56Z | |
| M15 | 2026-11-11T19:15:24Z | |
| M16 | 2026-11-12T00:07:18Z | |
| M17 | 2026-11-12T00:37:05Z | re-sent as M18 |
| M18 | 2026-11-12T02:56:07Z | re-send of M17 |
| M19 | 2026-11-12T03:04:40Z | |
| M20 | 2026-11-12T03:08:57Z | |
| M21 | 2026-11-12T04:27:21Z | |
| M22 | 2026-11-12T05:31:21Z | |
| M23 | 2026-11-12T09:37:25Z | being worked on elsewhere (Z0-Z10; shipped alpha21 + Courier alpha3) |
| M24 | 2026-11-12T12:20:42Z | paragraph 1 of M23 (session was dead) |
| M25 | 2026-11-12T15:35:36Z | being worked on elsewhere (AA1-AA4) |

---

## M0 - pre-capture brief (2026-11-10 ~22:00Z to 2026-11-11 ~01:50Z) - text not stored
> (no verbatim text exists; reconstructed from BRIEF.md "Owner facts", docs/HARDWARE.md, JOURNAL.md 2026-11-10 "Inputs" and 2026-11-11T00:26Z, README.md)

| Request | Status | Evidence | Gap |
| :- | :- | :- | :- |
| M0.1 A very light, fast, private Android browser with Brave-like Shields and security hardening, for ONE tablet (MT8168B, Android 10 Go, 2 GB; hardware_.txt supplied) | DONE | Kestrel 0.1.0/0.1.1 and Kestrel G alpha1-21 in BUILDLOG; core filter engine (JOURNAL: 127 core tests); README.md; docs/HARDWARE.md | none (speed itself: see M3.7, M25.4) |
| M0.2 The tablet reaches the internet only through a phone hotspot; 2 GB RAM; no Play Store, no Google services | DONE | CLAUDE.md Mission; docs/HARDWARE.md; tools/verify-apk.sh audits "no GMS" on every release | the "metered" half was replaced by M23.2 (unmetered); text in BRIEF.md/CLAUDE.md/FILTER-LISTS.md still says metered/weekly in places (G10) |
| M0.3 Anything typed in the address bar that is not an address is searched on Google | DONE | BRIEF.md Owner facts; JOURNAL 2026-11-11T00:26Z; first entry of `Prefs.ENGINES` | none |
| M0.4 Licensed third-party sources are allowed (personal, non-commercial) | DONE | docs/FILTER-LISTS.md "Licensing stance" | project licence still undecided (LIC-1, owner) |
| M0.5 No news, AI, rewards, wallet/web3, VPN, sync/accounts, Tor, telemetry | DONE | BRIEF.md; none of these exist in the tree | none |
| M0.6 Anything else the original brief asked | UNCLEAR | the prompt text was never captured | cannot be audited |

## M1 - 2026-11-11 (seeded, ~01:50Z)
> "Is the Firefox engine good because I tried to update android system webview but it won't work. Can it be injected into the browser you are making? Should it? Sh"

| Request | Status | Evidence | Gap |
| :- | :- | :- | :- |
| M1.1 "Is the Firefox engine good because I tried to update android system webview but it won't work." | UNCLEAR | the decision is in the repo: BRIEF.md "Two builds" (system WebView cannot be replaced; Gecko is the only embeddable modern engine), research/ENGINES.md; Kestrel G exists and M3 says it loads claude.ai | the chat reply pre-dates OUTBOX capture, so whether the question was answered to the owner cannot be established |
| M1.2 "Can it be injected into the browser you are making? Should it?" | UNCLEAR | BRIEF.md "Why a bundled engine ... allow-listed + signature-checked"; ENGINES.md row "Google WebView sideload: dead" | same: no captured reply |
| M1.3 "Should we use the Firefox engine or the chromium engine that brave uses?" | DONE | built as Kestrel G; Chromium side answered in detail at M10.1 (OUTBOX 11-11T15:27Z, 16:18Z) | none |
| M1.4 "I want fast, snappy, lightweight" | PARTIAL | the standing goal, see M3.7, M6.1, M23.3, M25.4 | speed never measured on the tablet |

## M2 - 2026-11-11 (seeded, ~02:40-03:04Z)
> "From now on, use github actions to build and release apks and release them on github. Use the checkpoint and save system from my other repos"

| Request | Status | Evidence | Gap |
| :- | :- | :- | :- |
| M2.1 "use github actions to build and release apks and release them on github" | DONE | `.github/workflows/android.yml` (tags + manual dispatch only), run 42951742468; BUILDLOG: 21 Kestrel G + 2 Kestrel + 3 Courier releases; OUTBOX 11-11T03:24Z | none |
| M2.2 "Use the checkpoint and save system from my other repos" | DONE | tools/{autosave,ckpt,resume,sync,claim,capture_inbox,capture_outbox,record-release}.sh, tools/test_handoff.sh; OUTBOX 11-11T03:24Z | leftovers: TASKS line 16 (delete shims scripts/autosave.sh, scripts/resume.sh) and line 14 (stale "OWNER: install alpha2 over alpha1": the owner has updated in place since, M15.1) |

## M3 - 2026-11-11T03:24:20Z (TASKS "Owner request 2 - THE BIG ONE")
> "From now on, frequently checkpoint and save all progress to github and optimize this project for multiple Claude sessions and accounts. Assume a resumed session"

| Request | Status | Evidence | Gap |
| :- | :- | :- | :- |
| M3.1 "frequently checkpoint and save all progress to github" | DONE | tools/autosave.sh after every tool call; 1,070 commits (819 auto-checkpoint, 147 ckpt); tools/test_handoff.sh | none |
| M3.2 "optimize this project for multiple Claude sessions and accounts. Assume a resumed session will have no context ... no progress or data loss or Claude usage wasted" | DONE | TASKS M1-M4 [x]: tools/sync.sh, .checkpoints/<branch>.md, tools/claim.sh, per-app lock in android.yml; test_handoff.sh simulates two sessions; sessions did resume from CHECKPOINT (11-12 13:55Z) | cut-offs by usage still happen (see section 0); one heartbeat trigger exists |
| M3.3 "The new version of the browser loads Claude.ai." (statement) | DONE | acknowledged OUTBOX 11-11T04:54Z; recorded in BRIEF.md Known issues | none |
| M3.4 "1) study gui and ui of brave browser (which I love) and the popular elements of other major browsers" | DONE | R1: research/UI_BENCHMARK.md | none |
| M3.5 "Implement a greatly improved gui ... simple and intuitive ... professional and polished ... decently large, easy to press buttons. Don't use boring generic Ai design. Come up with something fresh." | PARTIAL | docs/DESIGN.md (CERE shape + REAPER colour); BottomDock, HomeView, TabGrid, Sheet (alpha4/5); TASKS N2-N5 | polish backlog open (M4.7: Settings restyle, tab-grid swipe-to-close, cold-start glitch P5); boxes C1-C4 still `[ ]` although built; UI-3 landscape-tabs decision unanswered |
| M3.6 "and download manager that can pause and use the latest technologies and methods for fast downloading such as parallel and proper pausing" | DONE | N6/B1/B2 alpha5: pure-Kotlin engine (parallel Range segments, pause/resume, kill-resume), DownloadsScreen; EngineTest 22, PlannerTest 9, HttpSemantics 7 | "full fledged" remainder: M16.3 |
| M3.7 "2) make the browser as lightweight and snappy and fast as possible. Research ways to load websites extremely fast and snappy." | PARTIAL | research/GECKO_SPEED.md, HW_SPEED.md, SPEED2.md, AUDIT-2026-11.md; gecko-prefs v4 (26 prefs checked by tools/check-gecko-prefs.sh); D1 in-engine blocker (alpha13); bfcache/caches (alpha21) | nothing measured on the tablet; the same ask returns in M6, M10, M12, M15, M16, M17, M23, M25 (AA4 is in progress elsewhere) |
| M3.8 "Have no animations. Everything should feel snappy and instant." | DONE | A1: native UI animations off, ui.prefersReducedMotion=1, image.animation_mode once, general.smoothScroll=false (gecko-prefs.txt) | M6.5 deliberately re-allowed ONE animation (neon pulse); that exception supersedes this rule only for the pulse |
| M3.9 "Research online what methods or technologies allow for fast page and video loading and implement the best engine and tweak the Firefox engine as needed to optimize for speed" | PARTIAL | R3 GECKO_SPEED.md; engine choice ENGINES.md (stay on GeckoView); A2 [~]: AV1 off, 1 content process, bounded caches, YAML config proven by read-back; "modify Firefox" answered at M17.5 | A2 open: graphics safe-mode switch, preconnect only for the top suggestion, no tablet numbers |
| M3.10 "Keep in mind the specs of the tablet ... 32 bit android 10 go 2gb ram system. The GPU should be able to be used to speed things up when needed." | PARTIAL | HW_SPEED.md; Q2c: WebRender/GPU process/MediaCodec are Android defaults (told to the owner, OUTBOX 11-11T04:54Z); Diagnostics GL strings (GlInfo.kt) | no code for P1d "Speed lab" A/B switches or P1e Lean/Balanced/Roomy memory profiles (grep finds none); only onTrimMemory + 2 live sessions; P1f tablet measurement is the owner's |
| M3.11 "3) make the browser secure by using the newest security tools and standards" | PARTIAL | R4 research/SECURITY.md; A4 alpha3: FPP, LNA, CT, PQ, enterprise roots off, no push, bridge sender check, extension CSP | not done (owner-independent): B2 per-job `permissions:` (top level is `contents: write` for all jobs), B4 SHA-pinned actions (`actions/checkout@v5` etc.), B5 Gradle verification (no gradle/verification-metadata.xml), B6 gecko-watch, B10 intent-action allow-list (ExternalIntents has none), B11 provenance; owner-only: B1 private key, 2FA, Private DNS |
| M3.12 "4) make the best engine possible to block unwanted ads and trackers and anything that is unnecessary that slows down page loading" | PARTIAL | R5 ADBLOCK_ENGINE.md; D1 alpha13 (in-extension engine, identical verdicts on 63k+ requests, web-tests/engine.test.js, ExtImageTest, RealListsExtImageTest); HTTPS upgrade; daily lists; pop-up guard | D2: only 1,200 of 29,204 generic cosmetic rules apply (CosmeticIndex.DEFAULT_GENERIC_LIMIT); 14,942 procedural/scriptlet rules and 2,845 redirect/csp/removeparam rules skipped (docs/FILTER-LISTS.md); D3 annoyance lists / heavy widgets not started; D4 differential tests are effectively done under D1 but the box is `[ ]` |
| M3.13 "Send me the prompt again if I pressed deny on accident" | DONE | CLAUDE.md standing rule | none |
| M3.14 "Always keep my prompts and respond to each one even if I interrupt. Don't ignore any prompt." | PARTIAL | tools/capture_inbox.sh (25 entries) + capture_outbox.sh (24); every distinct request except M20 (and pre-capture M1) is in TASKS | 7 of 25 entries are re-sends after cut-off sessions; M20 never written down; ticks lag reality (section G10); this audit is the repair |
| M3.15 "keep in mind an issue I have with brave: it freezes a lot especially when I'm typing and I have to switch apps and go back ... I don't want this problem with kestrel" | PARTIAL | A3 alpha3: verdict worker off the UI thread, one GeckoView kept, adjustNothing while typing in the address bar, watchdog, Menu > Diagnostics > Refresh screen; M4 "So far I like alpha 3" (no freeze reported) | TEST-1 `[~]`: never explicitly confirmed; in-page typing lag on Proton Mail (M17.3) unresolved; A3 open: echo probes, recovery ladder, NetworkMonitor.start still called on the UI thread (BrowserActivity.kt:224), stress page |

## M4 - 2026-11-11T05:26:26Z (TASKS "Owner request 3")
> "So far I like alpha 3. 1) make it so when github makes a new release the app itself will see it and I can update the browser inside of the app 2) move the entir"

| Request | Status | Evidence | Gap |
| :- | :- | :- | :- |
| M4.1 "1) make it so when github makes a new release the app itself will see it and I can update the browser inside of the app" | DONE | N1 alpha4: UpdateManager/UpdatePanel/InstallResultReceiver, UpdateLogicTest, UpdateManagerTest; later bugs fixed (X1 alpha14 "up to date", Y1 alpha18); the owner reached the install step in M22 | none |
| M4.2 "2) move the entire top navigation bar to the bottom, including url bar, refresh, new tab button, etc." | DONE | N2 alpha4: ui/BottomDock.kt; OUTBOX 11-11T06:49Z | none |
| M4.3 "3) add a home button ... home page a very simple black screen with a large stylized text field in the middle which does a Google search ... or goes to that url" | DONE | N3 alpha4: ui/HomeView.kt | Home later gained the reaper flames/glow per M6 |
| M4.4 "4) separate private tabs from regular tabs, make sure there is an indicator for private tabs" | DONE | N4 alpha4: separate list in TabGrid, violet skin, mask icon, FLAG_SECURE | none |
| M4.5 "5) make the tab switching easier and with a good gui like major browsers, with a small thumbnail preview of a live view of every website tab open" | DONE | N5 alpha4: TabGrid + ThumbStore (capturePixels when the grid opens and 4 s after load) | thumbnails are snapshots, not live; swipe-to-close in the grid was promised in OUTBOX three times and is not built (TabGrid.kt has no swipe code) - see M4.7 |
| M4.6 "6) when I go to downloads, it tries to open my file manager. I want ... a full featured smart and fast download manager with pause and resume and fast features" | DONE | N6 alpha5: own Downloads screen, parallel Range segments, pause/resume/kill-resume, notifications; BUILDLOG alpha5 | remainder of "full fledged": M16.3 |
| M4.7 "7) begin work on the other things I already asked for. Show me what Claude can do, make the gui amazing and intuitive. Make the entire app snappy and fast. I want to be impressed" | PARTIAL | N7 `[ ]`; REAPER redesign alpha5; alpha10-21 | open polish: Settings restyle (P6), tab-grid swipe-to-close, cold-start glitch P5 (restored page tab shows Home overlay), toolbar auto-hide idea; C4 screenshot pass of every screen; speed (AA4) |

## M5 - 2026-11-11T06:48:46Z
> "So far I like alpha 3. 1) make it so when github makes a new release the app itself will see it and I can update the browser inside of the app 2) move the entir"

| Request | Status | Evidence | Gap |
| :- | :- | :- | :- |
| M5.r (re-send) identical to M4, sent 50 s before the reply | M4.1-M4.6 DONE; M4.7 PARTIAL | answered by OUTBOX 11-11T06:49Z (alpha4: items 1-5 and part of 7 done, 6 next) | none beyond M4 |

## M6 - 2026-11-11T07:44:45Z (TASKS "Owner request 4")
> "The goal for this browser is to be as lightweight and fast as possible, snappy. Research the hardware I have and make sure the browser is well optimized and tak"

| Request | Status | Evidence | Gap |
| :- | :- | :- | :- |
| M6.1 "Research the hardware I have and make sure the browser is well optimized and takes advantage of the ram and cpu and GPU to their full power for maximum speed and snappiness" | PARTIAL | research/HW_SPEED.md (GPU is Mali-G52 MC2, not PowerVR; nothing to switch on); P1a-c shipped (shield thread out of the background group, Io.fast lane, preview throttle); RAM prefs alpha21 | P1d Speed lab, P1e memory profiles, P1f measurement not built/done; the research result was never reported in chat (U2); AA4 |
| M6.2 "Research online and consider other engines to use other than Firefox. Consider cromite." | DONE | P2 research/ENGINES.md; chat answers OUTBOX 11-11T15:27Z and 16:18Z | optional P2a/P2b are the owner's |
| M6.3 "Keep the gui except instead of yellow highlights, make them blue." | DONE | P3 alpha5: colors.xml accent 14D4F8; no yellow left (one stale comment at TabGrid.kt:126) | none |
| M6.4 "Get design ideas based on the attached grim reaper photo ... incorporate the visuals and colors of the attached photo into the browser theme" | DONE | P3 alpha5: REAPER palette, ghost-flame Home, glossy dock; docs/DESIGN.md (photo kept out of the public repo) | owner never commented on the look; UI-2 is de facto answered |
| M6.5 "add very subtle pulsing blue animation to the gui in logical places such as button edges ... like the pulsating neon colors around the modern smartphone screens ... colors ... look like the contrast between the very dark black and the blue" | DONE | P4 alpha5, R1 alpha7 (pulse no longer gated on the system animation scale, ring on the address pill), V6 alpha10 (Settings "The glow right now" + "Test the pulse (10 s)"); ui/Neon.kt; M19.4 says "pulses like the other animations" | the owner complained twice (M12.1, M15.9) and has not repeated it since alpha10, but never confirmed it shows (O1); real CPU/battery cost unmeasured |

## M7 - 2026-11-11T10:49:29Z
> "So far I like alpha 3. 1) make it so when github makes a new release the app itself will see it and I can update the browser inside of the app 2) move the entir"

| Request | Status | Evidence | Gap |
| :- | :- | :- | :- |
| M7.r (re-send) identical to M4, sent while the session was dead (no commits 08:50Z-14:50Z) | M4.1-M4.6 DONE; M4.7 PARTIAL | answered earlier by OUTBOX 11-11T06:49Z; alpha5 link first sent at OUTBOX 11-11T15:27Z ("I never sent you the alpha 5 link before") | the owner waited ~4.5 h for any reply |

## M8 - 2026-11-11T10:50:18Z
> "The goal for this browser is to be as lightweight and fast as possible, snappy. Research the hardware I have and make sure the browser is well optimized and tak"

| Request | Status | Evidence | Gap |
| :- | :- | :- | :- |
| M8.r (re-send) identical to M6, sent while the session was dead | M6.1 PARTIAL; M6.2-M6.5 DONE | alpha5 (BUILDLOG 08:50Z) and OUTBOX 11-11T15:27Z (mentions the blue neon theme) | no reply covering the hardware research (U2) |

## M9 - 2026-11-11T12:51:04Z
> "The goal for this browser is to be as lightweight and fast as possible, snappy. Research the hardware I have and make sure the browser is well optimized and tak"

| Request | Status | Evidence | Gap |
| :- | :- | :- | :- |
| M9.r (re-send) identical to M6, third time, still dead | M6.1 PARTIAL; M6.2-M6.5 DONE | as M8 | as M8 |

## M10 - 2026-11-11T14:50:48Z (TASKS "Owner request 5")
> "Tell me the pros and cons of using the https://github.com/uazo/cromite cromite engine for this browser instead of geckoview The scrolling up and down on website"

| Request | Status | Evidence | Gap |
| :- | :- | :- | :- |
| M10.1 "Tell me the pros and cons of using the cromite engine for this browser instead of geckoview" | DONE | Q1: OUTBOX 11-11T15:27Z, 16:17Z, 16:18Z; research/ENGINES.md | none |
| M10.2 "The scrolling up and down on websites is slow and stutters." | PARTIAL | Q2a-e alpha6; R2 alpha7 (Smoother default: display-port multipliers, touch wait 300 ms, 3 min expiry); config v4 alpha21; research/SCROLL.md | emulator only; Q2f `[ ]` needs the owner's numbers (O2); AA4 |
| M10.3 "Sometimes it maybe didn't cache properly and when I scroll up and down sometimes the screen goes white and has to reload." | PARTIAL | Q2a: killed page process comes back at the same scroll position behind a dimmed preview (emulator `kill -9`); Q2b decoded-image cache sizing; config v4 | the tablet's real cause is unknown (needs Diagnostics Copy after a white screen) |
| M10.4 "Make sure the browser takes full advantage of the hardware, decoding, and the GPU for anything that can make it faster." | PARTIAL | Q2c: WebRender/GPU process/MediaCodec are Android defaults, AV1 off, GPU strings in Diagnostics | the tablet's GPU/driver is still unread (O2); no hardware image decode exists in Gecko |
| M10.5 "Also consider using cromite engine instead of the Firefox engine" | DONE | Q3 verdict "do not swap" (OUTBOX 11-11T16:18Z) | the 15-minute A/B (P2b) and "Open in Cromite" (P2a) are the owner's call (O13) |

## M11 - 2026-11-11T16:17:31Z
> "Tell me the pros and cons of using the https://github.com/uazo/cromite cromite engine for this browser instead of geckoview"

| Request | Status | Evidence | Gap |
| :- | :- | :- | :- |
| M11.r (re-send) = first sentence of M10 | M10.1 DONE | answered 1 minute later, OUTBOX 11-11T16:18:17Z | none |

## M12 - 2026-11-11T16:18:33Z (TASKS "Owner request 6")
> "1) I don't see the neon pulsing animations anywhere. 2) the scrolling is still stuttering and laggy, and it looks like it reloads pictures after I scroll away f"

| Request | Status | Evidence | Gap |
| :- | :- | :- | :- |
| M12.1 "1) I don't see the neon pulsing animations anywhere." | DONE | R1 alpha7: two root causes found (animator-scale gate; "focused field with keyboard hidden counts as typing"), emulator pixel checks with animations off | repeated in M15.9 so alpha7 was not enough; see M6.5 / O1 |
| M12.2 "2) the scrolling is still stuttering and laggy ... reloads pictures ... unresponsive for a split second. Figure out how to efficiently use ram/cache/gpu/cpu and methods other browsers use for smooth scrolling and quick loading. Implement it." | PARTIAL | R2 alpha7 (root cause read from Gecko source: display port only ~60 px beyond the screen); Smoother default; Diagnostics "Compare 3 feels"; v4 config alpha21 (decode 2 screens ahead, 3 min image life, 64 MB cache) | sub-boxes R2a-R2e still `[ ]`; effect on the tablet unknown; AA4 |
| M12.3 "3) make a pull down to refresh page gesture that I can use." | DONE | R3 alpha7: KestrelGeckoView + core PullTracker (PullTrackerTest 14), PullChip, Settings switch; emulator-verified (fires at top, mid-page pull scrolls) | not confirmed on the tablet |
| M12.4 "Correction: it may not be reloading pictures during the scrolling, but it does stutter. Sometimes when I scroll down then back up there is just white space for a second" | PARTIAL | same fix set as M12.2 | same as M12.2 |

## M13 - 2026-11-11T16:26:31Z (TASKS "Owner request 7")
> "I made 900mb of space on my tablet but this message still shows. On the download manager, include a display of his many mb downloaded progress for each file in "

| Request | Status | Evidence | Gap |
| :- | :- | :- | :- |
| M13.1 "I made 900mb of space on my tablet but this message still shows." | DONE | S1 alpha7 (exact MB to free, Try again from the already downloaded file, stale message cleared), U1 alpha8/9 (Android's 10 % rule); M15.1 "The app can now update with enough storage space" | none |
| M13.2 "On the download manager, include a display of [how] many mb downloaded progress for each file in addition to the total file size" | DONE | S2 alpha7: PlannerTest.statusShowsMegabytesDownloaded; card text "27.4 MB of 115.2 MB - 23 % - 327 KB/s" | none |

## M14 - 2026-11-11T18:00:56Z (TASKS U1)
> "The newer versions never install over the old ones. I always have to uninstall the old one first I don't think free space was the issue because I installed it f"

| Request | Status | Evidence | Gap |
| :- | :- | :- | :- |
| M14.1 "The newer versions never install over the old ones. I always have to uninstall the old one first" | DONE | U1 alpha8/9: cause = Android refuses any install unless 10 % of the data partition stays free (research/UPDATE_STORAGE.md, emulator measurements); M15.1 confirms it works with room; Y1 alpha18 (M22.1) | uninstalling first still wipes bookmarks/history/settings and there is no Back up/Restore (U1's own condition "if in-place updates still fail" was met by M22.1) - G7 |
| M14.2 "I don't think free space was the issue because I installed it from github fine" | DONE | explained in OUTBOX 11-11T19:15Z (the uninstall freed ~1 GB first) | the Settings > Storage numbers asked for in that reply were never sent (O7, low value now) |

## M15 - 2026-11-11T19:15:24Z (TASKS "Owner request 8", V0-V7)
> "The app can now update with enough storage space. Make the app download and download and install updates even in the background, downloads should continue at fu"

| Request | Status | Evidence | Gap |
| :- | :- | :- | :- |
| M15.1 "The app can now update with enough storage space." (statement) | DONE | recorded as V0 (box still `[ ]`) | tick V0 |
| M15.2 "Make the app download and download and install updates even in the background, downloads should continue at full speed even if the tablet is on the lock screen and the screen is turned off" | DONE | V1 alpha10: update APK through the same engine in DownloadService (foreground service, partial wake lock, Wi-Fi lock); emulator screen-off run; AppSinksTest | tablet (doze, battery) unverified; Android 10 always shows the system Install screen (told to the owner); box V1 still `[ ]` |
| M15.3 "Put notifications for update status and download statuses." | DONE | V2 alpha10: UpdateNotifier, Notifier with Pause/Cancel/Retry | tablet unverified; box `[ ]` |
| M15.4 "Remove the 'quit' button from the menu. I will swipe to close the app to quit. If I swipe to close the app, keep downloads active, but if everything is inactive ... properly sleeps and doesn't waste battery or ram or processor" | DONE | V3 alpha10: no Quit in Kestrel G's menu (grep); emulator: idle swipe ended all 4 processes, running download carried on and the process exited ~6 s after the last byte (ProcessLife) | the unused WebView build still has Quit (BrowserActivity.kt:627); V3b download-only process (own idea) not built; box `[ ]` |
| M15.5 "I use an app called video downloader by inshot ... Figure out how it works and allow me to download videos from any web page online by long pressing it and an option for download video should appear." | DONE | V4: research/VIDEO_DOWNLOADER.md; sniffer + page scan + sheet (alpha11/12); long-press made reliable by X14 alpha17 after M16.8/M19.1 | real-site coverage only the owner can show |
| M15.6 "If the video is hosted in different resolutions, give me options ... Name the resolutions and their respective file size next to them." | DONE | quality sheet alpha11; header probe for bare files alpha12; X17 list rows (name, resolution, type, size, duration) alpha17; sizes of streams are labelled "about" | none |
| M15.7 "Figure out how to allow video downloads for as many sources as possible, even websites that don't allow downloads. The video downloader app can do this." | PARTIAL | HLS (TS, fMP4, AES-128), DASH merge, page scan; owner told that DRM, YouTube, live streams, expired links cannot be saved (OUTBOX 11-11T20:16Z, 21:38Z) | no site-specific extractors; whether the owner wants YouTube-class sources against the sites' terms is an open decision (O15) |
| M15.8 "Enable parallel downloading where available and download threading ... Make sure the downloads can be safely paused and resumed." | DONE | V5 alpha11: parallel Range segments, parallel HLS/DASH segment workers; StreamEngineTest and EngineTest kill-and-resume cases; X3 alpha16/17 | connection counts untuned on the tablet |
| M15.9 "The app's subtle neon pulsing animations still aren't showing." | DONE | V6 alpha10: holds cut to keyboard-up / first 3 s of load / finger drag / app away / full-screen page; Settings explains why it holds; "Test the pulse (10 s)" | not repeated by the owner afterwards; never confirmed (O1) |
| M15.10 "Review the app code and logic and improve speed, page loading, and stuttering as much as possible" | PARTIAL | V7: research/REVIEW-V7.md (two reviews, status per finding); alpha12 (refresh storms, touch path, suggestions debounce 70 ms), alpha13 (D1 blocker) | REVIEW-V7 still OPEN: P5 cold-start order, P9 onLoadRequest on the UI thread, P10 sniffer headers, S3 row reuse, S5 keyed tab-grid update, S7 neon layers; AA4 covers (G1) |

## M16 - 2026-11-12T00:07:18Z (TASKS "Owner request 9", X1-X10)
> "Alpha9 says it is up to date Make the app check once per day if there are any new versions of itself, of the browser engine it uses, and any ad blocking updates"

| Request | Status | Evidence | Gap |
| :- | :- | :- | :- |
| M16.1 "Alpha9 says it is up to date" / "The in app updater for the last couple versions says the app is up to date even when a new version was released" | DONE | X1 alpha14: GitHub lists releases by tag name and the app read only 8; fix = UpdateLogic.compareTags/newestTags + tag-list fetch; UpdateLogicTest, UpdateCheckTest (end-to-end, 304 path); rescue tag 0.3.0-alpha14; OUTBOX 11-12T00:32Z; M22.1 shows the owner reached the install step | none |
| M16.2 "Make the app check once per day if there are any new versions of itself, of the browser engine it uses, and any ad blocking updates. If any updates are available, send me a dialog ... and an update button, then download and update the app in the background." | DONE | X2 alpha16: ReleaseCheckJob (daily), UpdateManager.checkEngine, UpdateJob daily list refresh, BrowserActivity.maybeShowUpdateNotice (Update / Later / Details, Install now); emulator-proven | the engine half only informs: nobody bumps GeckoView (`geckoVersion = 160.0.20261104181552`; TASKS line 19 `[ ]`) and a scheduled watcher (B6) needs the owner's OK (G8, O11); dialog not seen on the tablet |
| M16.3 "Turn the download manager into a full fledged download manager optimized for maximum speed, multiple downloads, good pause and resume logic that doesn't break or corrupt files, parallel downloading and threading, etc. Research the technology and methods of popular and powerful download managers to implement." | PARTIAL | X3 `[~]`; research/DOWNLOAD_MANAGERS.md; shipped alpha16/17: auto-resume after kill, outage ladder, refused-connection retire, 2-connection fast start, Last-Modified/tail compare, web-page sanity check, Settings > Downloads (speed limit, downloads at once, connections); EngineTest/PlannerTest/StreamEngineTest | open: M5 rest (slow-connection recycle), M6 checksum (Planner has Reason.DIGEST and Repr-Digest parsing, not wired as a user feature), M8 priorities/reorder (no code), M9, M10, T0 (G5) |
| M16.4 "Make the app open as fast as possible, load web pages as fast as possible, and make everything snappy and the scrolling smooth and fast." | PARTIAL | X4 `[~]`: Page loads timing, Network lab, address-bar warm-up, cheaper UI refresh; D1; config v4 | same as M3.7; AA4 (G1) |
| M16.5 "Figure out if it is using the best ad and tracker blocking software and technology, and make it auto update the ad blocking and implement the best available ad blocking and tracker blocking technology." | PARTIAL | X5 `[~]`: assessment in research/AUDIT-2026-11.md section 5 and ADBLOCK_ENGINE.md (own engine kept); lists refresh daily (UpdateJob DAY); pop-up/redirect guard alpha15 | the verdict "is it the best?" was never told in chat (U1); scriptlets, redirect resources, removeparam, procedural cosmetics not built (G3); the CI list snapshot needs an owner yes (O11) |
| M16.6 "Sometimes when I go to movie web sites, it frequently redirects to an advertisement page or opens a new tab of spam ... Figure out a way for the browser to reject these redirects and spam tabs." | DONE | X6 alpha15: core JumpGuard + guard-main.js + refusePopup; web-tests/guard.test.js (20 tests), JumpGuardTest, emulator popups.html (counters guard.open/click/redirect); hardened by X16 alpha17 | emulator only; the owner reported another spoof case (M19.3); real-site check is the owner's (O14) |
| M16.7 "Sometimes I need to press play on a video in order to use the browser video Downloader function, and I don't want it to open spam sites, I want to download the video cleanly" | DONE | X7 alpha15: page scan in content.js (video, source, og:video, links, inline JSON) so the list exists before play; emulator domscan/disc pages | real sites unverified |
| M16.8 "I long pressed a video to download but instead it gave me the options if its own player, with no download button." | DONE | X8 alpha15 (always-visible chip), then X14 alpha17: content.js contextmenu capture opens Kestrel's menu (Download video / Copy video address); web-tests/content.test.js (4) | alpha15 did not fix it (owner repeated it as M19.1); alpha17 emulator only |
| M16.9 "I successfully started the download using the bottom menu, but it was downloading a .ts file ... Is it possible to force download mp4 or mkv?" | DONE | X9 alpha15 (TS remuxed to MP4 even with discontinuities), X18 alpha17 (MP4 from the first byte); answer given: MKV impossible (no muxer on Android), MP4 plays everywhere (OUTBOX 11-12T01:50Z) | residual .ts cases: M19.5 |
| M16.10 "Can it force a download button in addition to a videos own player menu" | SUPERSEDED | X10 alpha15 floating chip; replaced by the dock icon the owner asked for in M19.4 (X17 alpha17, chip removed) | none |

## M17 - 2026-11-12T00:37:05Z (TASKS "Owner request 10")
> "Do everything still on the list and then do an audit of the app and engine. It was very slow loading Proton mail and lagged when I was typing. Do your best effo"

| Request | Status | Evidence | Gap |
| :- | :- | :- | :- |
| M17.1 "Do everything still on the list" | PARTIAL | X11 `[ ]`: of X2-X10 only X3/X4/X5 stay `[~]` (rest DONE alpha14-17); P5, tab-grid swipe-to-close, Settings restyle, V3b untouched | G1, G3, G5, G6 |
| M17.2 "do an audit of the app and engine" | DONE | research/AUDIT-2026-11.md (verdict, findings table, asks) + GECKO_TUNING_2.md; OUTBOX 11-12T02:58Z | none |
| M17.3 "It was very slow loading Proton mail and lagged when I was typing." | PARTIAL | alpha16 tools: Diagnostics "Page loads", Settings "Network lab" (DoH, HTTP/3), address-bar warm-up; AUDIT section 1.4: no per-key code of ours in the typing path | root cause unknown and no fix shipped; the owner's numbers were requested 8 times and never came (O3, O4); independent options exist (G2) |
| M17.4 "Do your best effort at making the browser well coded and extremely efficient and optimized for my hardware" | PARTIAL | AUDIT-2026-11.md; alpha16-21 changes | continuing under AA4 (G1) |
| M17.5 "Consider looking at the Firefox engine it is using and modifying it for maximum speed on this hardware" | DONE | AUDIT section 1 + GECKO_TUNING_2.md (440 prefs checked): a custom Gecko build is infeasible (runner limits, no PGO), prefs near the ceiling; told in OUTBOX 11-12T02:58Z | none |

## M18 - 2026-11-12T02:56:07Z
> "Do everything still on the list and then do an audit of the app and engine. It was very slow loading Proton mail and lagged when I was typing. Do your best effo"

| Request | Status | Evidence | Gap |
| :- | :- | :- | :- |
| M18.r (re-send) identical to M17, 2 minutes before the alpha16 reply | M17.1 PARTIAL; M17.2 DONE; M17.3 PARTIAL; M17.4 PARTIAL; M17.5 DONE | OUTBOX 11-12T02:58Z ("You sent the same message twice, so here it is part by part") and 03:04Z | as M17 |

## M19 - 2026-11-12T03:04:40Z (TASKS "Owner message 11", X14-X18)
> "Also I long pressed a video and it still didn't show download even though I could download it from the regular menu. And the steam only showed a .ts file but on"

| Request | Status | Evidence | Gap |
| :- | :- | :- | :- |
| M19.1 "I long pressed a video and it still didn't show download even though I could download it from the regular menu." | DONE | X14 alpha17: long-press on `<video>` opens Kestrel's menu (content.js 'vmenu', BrowserActivity); web-tests/content.test.js | emulator only; the owner has not re-tested |
| M19.2 "the steam only showed a .ts file but on video Downloader app I could choose mp4 for the same video" | DONE | X15 alpha17: row says MP4 and the file is an MP4 from byte one (StreamEngineMp4Test 8, emulator 40 s stream, ffmpeg decodes clean) | edge cases in M19.5 |
| M19.3 "It also redirected me to a spam page, I think it is a spoof play button that redirects, but after I press play several times it finally plays the video" | DONE | X16 alpha17: tap on a transparent/odd overlay link over the player is blocked; guard knows the top host; guard.test.js +5; emulator spoof.html | real-world spoofs unverified (TASKS says so) |
| M19.4 "I like the save video button, but instead of floating over the webpage, put it in the navigation bar as a download icon. ... gray ... neon ... pulses ... pop up menu ... names ... file sizes ... resolutions ... file types" | DONE | X17 alpha17: ui/DownloadButton.kt in BottomDock; list rows name/resolution/type/size/duration; floating chip removed; X20 same-page-two-tabs fix alpha18 | emulator only |
| M19.5 "I want these to be mp4 downloads always" | PARTIAL | X18 alpha17 (MP4 written while downloading, byte-identical after kill and resume; research/MP4_MUX.md) + "Make MP4" button for old .ts files alpha18 | a .ts still survives for H.265, AC-3/E-AC-3/MP3 sound, free room below size+64 MB, a platform-muxer failure, unrecorded discontinuity runs; fMP4/CMAF/DASH end as fragmented MP4 (MP4_MUX.md sections 8-9; X19c/d/f open) - G4 |

## M20 - 2026-11-12T03:08:57Z
> "Start building the next step"

| Request | Status | Evidence | Gap |
| :- | :- | :- | :- |
| M20.1 "Start building the next step" | DONE | work continued at once and shipped as alpha17 (X14-X18, Settings > Downloads, warm-up) at 04:56Z; OUTBOX 11-12T05:30Z | ambiguous (which step) and never written into TASKS; no direct acknowledgement |

## M21 - 2026-11-12T04:27:21Z
> "Most videos don't need any converting. They are just remuxed into the mp4 container with audio"

| Request | Status | Evidence | Gap |
| :- | :- | :- | :- |
| M21.1 correction/guidance: only the wrapper changes, no re-encoding | DONE | adopted as the design of X18 (samples copied byte for byte); OUTBOX 11-12T05:30Z "You were right about the remux" | none |

## M22 - 2026-11-12T05:31:21Z (TASKS "Owner message 12", Y1-Y2)
> "Also, the update feature says I need to free 2gb storage, but I uninstalled the app and installed the new one fine without making the 2gb storage. Make the upda"

| Request | Status | Evidence | Gap |
| :- | :- | :- | :- |
| M22.1 "Make the update feature try to update no matter what" | DONE | Y1 alpha18: the updater no longer gates on its own estimate; refusal sheet with Try again / Storage settings / Install the other way; emulator with 650 MB free reached Android's own install screen | not re-tested by the owner; "Install the other way" discards app data and there is no Back up/Restore (G7) |
| M22.2 "Is it possible to make an option in the settings to have the phone itself download files/videos with the status for the download in the browser app on the tablet while everything looks and functions the same ... only outsourcing the work to the phone" | DONE | Y2: Kestrel Courier alpha1-3 + Kestrel G alpha19-21 (courier suite 124+ JVM tests, both-apps emulator proofs); question answered OUTBOX 11-12T06:06Z; work closed by M25.2 | Y2k real pair test never done (owner, now low value); Y2i stage 3 closed as "won't do" |

## M23 - 2026-11-12T09:37:25Z (TASKS "Owner message 13", Z0-Z10) - handled elsewhere, rows shown for completeness
> "My mobile hotspot is always unmetered. No data saving is necessary, but the option may remain in the settings. My download speeds range from around 8mbit/s in t"

| Request | Status | Evidence | Gap |
| :- | :- | :- | :- |
| M23.1 facts: Android 16 phone, app install OK, Shizuku + LinkMeter, speeds 8-150 Mbit/s, tablet 5 GHz set to 2.4 GHz | DONE | Z0 recorded; BRIEF.md 2026-11-12 bullet; SPEED2.md section 1 | none |
| M23.2 "My mobile hotspot is always unmetered. No data saving is necessary, but the option may remain in the settings." | DONE | Z1 alpha21: Prefs.dataSaver default OFF; PageProxyTest.theDataSaverIsOffByDefaultSoNothingActsMetered | CLAUDE.md/BRIEF.md/FILTER-LISTS.md still describe metered/weekly behaviour (G10) |
| M23.3 "My main goal for this browser is speed and snappiness. Make it as fast as possible. It may use ram liberally as long as the extra ram use can speed up browsing or page loading." | PARTIAL | Z2 `[~]`: research/SPEED2.md; gecko-prefs v4 (64 MB memory cache, 4 pages for 1 h, decoded images 3 min) | Z2 promised "a RAM budget": SPEED2.md has none; queued: max_total_viewers 6, size_factor 12 (CHECKPOINT); in progress elsewhere |
| M23.4 "Would it be possible and logical to offload any processing/downloading to the phone ... Think about if the phone hardware can be used in any way while it is supplying internet over the hotspot" | DONE | Z3: SPEED2.md sections 1-4; answered OUTBOX 11-12T15:35Z (only the phone opening the connections helps; carrier caps the hotspot, not the phone's own traffic) | none |
| M23.5 "Regardless, optimize this app for speed considering the tablet hardware." | PARTIAL | Z4 `[ ]` (same job as AA4) | in progress elsewhere |
| M23.6 "Smooth the scrolling." | PARTIAL | Z5 `[~]`: config v4 + Scroll feel levels | the owner judges; in progress elsewhere |
| M23.7 "Keep pages cached so if I press the back button it loads immediately." | PARTIAL | Z6 `[x]`: bfcache probe on the emulator (persisted=true), 4 viewers, 1 h timeout (gecko-prefs.txt) | pages sending Cache-Control: no-store (webmail, banks) are never cached, so Back reloads them; the `[x]` is generous; AA4 text covers it |
| M23.8 "Improve the caching logic to make scrolling faster and smoother ... preventing white unloaded or slow loading areas of web pages while scrolling." | PARTIAL | Z7 `[~]`: decode 2 screens ahead, lazy margin 1500 px, shader precache | tablet effect unmeasured; in progress elsewhere |
| M23.9 "make the phone handle only things that it can genuinely speed up ... include a toggle where I can turn this off and use hotspot only" | DONE | Z8: ConnectProxy (19 tests with real sockets), ProxyLink (8), PageProxyTest; main-menu "Phone helper" tile; emulator proof SPEED2.md 6.1; Courier alpha3 + alpha21 | no real-device numbers; now default OFF per M25.2 |
| M23.10 "For the ad block do whatever is logical" | DONE | Z9: stays in the engine (SPEED2.md 3 and 4.4); told in OUTBOX 11-12T15:35Z | the scheduled-workflow yes/no (asked 8 times) is still unanswered; M23.10 may or may not cover it (O11) |
| M23.11 "it has shizuku already running and paired with the linkmeter app Claude made in another repo" | DONE | Z10: LinkMeter's docs/FAST_WIFI.md read; port clash 8765 -> 8780 fixed; gateway lookup under VPN fixed | none |

## M24 - 2026-11-12T12:20:42Z
> "My mobile hotspot is always unmetered. No data saving is necessary, but the option may remain in the settings. My download speeds range from around 8mbit/s in t"

| Request | Status | Evidence | Gap |
| :- | :- | :- | :- |
| M24.r (re-send) = paragraph 1 of M23, sent while the session was dead (09:42Z-13:55Z) | M23.2 DONE; M23.3 PARTIAL; M23.4 DONE; M23.5 PARTIAL; M23.6 PARTIAL; M23.7 PARTIAL; M23.8 PARTIAL | answered OUTBOX 11-12T15:35Z | the owner waited 3 h 15 min |

## M25 - 2026-11-12T15:35:36Z (TASKS "Owner message 14", AA1-AA4) - handled elsewhere, rows shown for completeness
> "I will be using linkmeter fast wifi setting to get better hotspot speed. Optimize the browser to take advantage of this fast wifi feature and make sure websites"

| Request | Status | Evidence | Gap |
| :- | :- | :- | :- |
| M25.1 "Optimize the browser to take advantage of this fast wifi feature and make sure websites and services load correctly and as fast as possible." | PARTIAL | AA1 `[~]`; ckpt 1069: NetProfile (core, 5 tests), NetworkMonitor.vpn, GeckoEngine.applyNetwork, Diagnostics "Network:" line | in progress elsewhere; emulator proof of the VPN profile still queued |
| M25.2 "Finish and Close work on the kestrel courier method at the next logical opportunity and keep it turned off in the browser by default" | PARTIAL | AA2 `[ ]` at the snapshot; ckpt 1069 reports it done (defaults OFF, pairing no longer enables, tile only when paired, off under a VPN); PHONE_OFFLOAD.md section 17 CLOSED | in progress elsewhere; verify at the next ship |
| M25.3 "After you close the courier work, review my past prompts for any unfinished work and finish it. Do not ignore my prompts, work on each one of them, including past prompts not responded to yet." | PARTIAL | AA3 `[ ]`: this audit is its input | the finishing is the follow-up work |
| M25.4 "I especially want Claude to review and optimize as much as possible this browser app for speed and smooth scrolling and fast webpage loading and caching ... so the back button doesn't have to reload the last site ... Take advantage of the ram and hardware of the tablet" | NOT DONE | AA4 `[ ]`; CHECKPOINT: perf reviews launched, no code yet | in progress elsewhere; carry-over list in G1 |

---

# Closing lists

## (1) GAPS TO FINISH (not owner-only), most stressed first
1. **Speed, smooth scrolling, no white areas, instant Back, caching** (asked in M3, M4, M6, M10, M12, M15, M16, M17, M23, M25 = the single most repeated ask; status PARTIAL; AA4 is in progress elsewhere). Next step: hand AA4 the carry-over list it does not name: (a) P1e memory policy (geckoapp BrowserActivity.onTrimMemory / live-session limit use `ActivityManager` availMem minus threshold to hibernate earlier; plus the queued prefs `max_total_viewers` 6, `size_factor` 12); (b) REVIEW-V7 OPEN P9 (`onLoadRequest` work on the UI thread), P10, S3, S5, S7; (c) Back for `no-store` pages (Proton Mail): show the tab's last preview (ThumbStore/RestoreOverlay already exist) the instant Back is pressed, then reload; (d) A5: add per-process PSS and a page-load median to Diagnostics so the owner's Copy carries numbers; (e) P1d "Speed lab" A/B switches.
2. **Typing lag/freeze in pages (Proton Mail)** (M3.15, M17.3; the owner's numbers never came, so do not wait). Next step: add a Network-lab style A/B switch "Keyboard: resize page / keep page size" (AUDIT section 2: `adjustResize` in page fields relayouts a big SPA once), a Diagnostics "Typing lab" button using GeckoView `ProfilerController` on a plain textarea vs mail.proton.me, and move `NetworkMonitor.start` (BrowserActivity.kt:224) off the UI thread.
3. **Ad/tracker blocking depth** (M3.12, M16.5, M23.10). Next step: D2 first - class/id token index so all 29,204 generic cosmetic rules can apply instead of the first 1,200 (`CosmeticIndex.DEFAULT_GENERIC_LIMIT`; content.js reports ids/classes, background.js/ShieldsBridge answers), then `$removeparam` and `redirect=` resources, then scriptlets/procedural cosmetics (research/ADBLOCK_ENGINE.md plan P1, ~15 session-days), then D3 (annoyance list, heavy widgets); tick D4 (done under D1).
4. **"mp4 always"** (M16.9, M19.5). Next step: X19c/d/f in core/.../media: `hvcC` (H.265), `dac3`/`dec3` and `esds` 0x6B (AC-3/E-AC-3/MP3) in Mp4Mux with ffmpeg fixtures (tools/mux-corpus.sh), progressive finish for fMP4/CMAF/DASH, skip fetching removed ad runs; make the row note say which of the five .ts reasons applied.
5. **Download manager "full fledged"** (M3.6, M16.3). Next step: wire the existing digest support (Planner Reason.DIGEST, HttpSemantics.reprDigest) into Engine completion (M6), add queue priorities + "start now" + reorder in DownloadsScreen/DownloadController (M8), slow-connection recycle (M5 rest), then the raw-socket fake server T0.
6. **"I want to be impressed" UI polish** (M3.5, M4.5, M4.7). Next step: swipe-to-close in `ui/TabGrid.kt` (no swipe code exists), restyle `ui/SettingsView.kt` as tile rows like the menu, fix P5 (restored page tab shows the Home overlay while the engine starts), then C4: emulator screenshots of every screen at 600x973@170 and fix what looks off.
7. **Back up / Restore** (M14.1, M22.1). The condition TASKS U1 set ("if in-place updates still fail") is met: the owner still uninstalls first and "Install the other way" wipes data. Next step: Settings > "Back up" / "Restore" for bookmarks, settings and tab URLs as one file in Downloads (MediaStore sink; no `allowBackup`).
8. **Engine freshness** (M16.2). Next step: add the monthly GeckoView bump (TASKS line 19) to the release checklist and do one now if the in-app engine check reports newer than 160.0.20261104181552; the scheduled watcher B6 needs O11.
9. **Security hardening still open and owner-independent** (M3.11). Next step: `.github/workflows/android.yml` per-job `permissions:` and `persist-credentials: false` (B2), SHA-pin every `uses:` (B4), `gradle/verification-metadata.xml` + wrapper checksum (B5), `ExternalIntents` action allow-list (B10).
10. **Bookkeeping so the next audit is cheap** (M3.14). Next step: tick with evidence V0-V3, V5, V6 (code + emulator, tablet pending), B1, B2, C1, C2, D4, R2a-R2e, Y2a, U1 and TASKS lines 14 and 165 (duplicate X18b); leave V4, V7, C3, C4 open; add M20 to TASKS; fix stale text (CLAUDE.md and docs/FILTER-LISTS.md say "weekly" lists, BRIEF.md/HANDOFF.md say metered, TabGrid.kt:126 comment says yellow); save this audit's table as research/PROMPT_AUDIT.md (AA3).
11. Not owner-requested, low: V3b download-only process, A5 WebGL/codec matrix page, the WebView build (`:app`, frozen at 0.1.1, still has a Quit item) - leave unless asked.

## (2) OWNER-ONLY items (exactly what is needed)
- **O1 Neon pulse** (M6.5, M12.1, M15.9): say whether the blue glow pulses now; if not, Settings > Appearance, copy the "The glow right now" line and tap "Test the pulse (10 s)".
- **O2 Scroll / white areas / Back** (M10, M12, M23): on the newest build say whether stutter, white strips and Back are better; Menu > Diagnostics > "Compare 3 feels (90 s)" > Copy and paste it; Diagnostics > Copy right after any white screen (also reads the GPU line).
- **O3 Proton Mail numbers** (M17.3, asked 8 times): open mail.proton.me twice and paste the Diagnostics "Page loads" lines; repeat with Settings > Network lab > Encrypted DNS off, then HTTP/3 off (one change at a time); once with Shields off for proton.me.
- **O4 Tablet facts** (M17.3): keyboard app, whether an accessibility or autofill service is on (the 5 GHz fact was answered in M23).
- **O5 Typing freeze** (M3.15, TEST-1): has the Brave-style freeze ever happened in Kestrel G; if yes, Menu > Diagnostics > Refresh screen, then Copy/Share.
- **O6 Blocker numbers** (D1): after a heavy site, Diagnostics > Update > Copy and send the "Blocker" line (last asked 11-12T00:07Z).
- **O7 Settings > Storage** total/free (U1; low value now that the updater tries regardless).
- **O8 SEC-1 private release key**: create the four repo secrets (SIGNING_KEYSTORE_BASE64, SIGNING_STORE_PASSWORD, SIGNING_KEY_ALIAS, SIGNING_KEY_PASSWORD) and a reviewer-gated environment, then the pin in signing/CERT_SHA256 changes; until then the public dev key lets anyone sign an update.
- **O9 LIC-1**: pick the project licence (research suggests GPLv3-or-later) and the default of a YouTube-ads switch.
- **O10 UI-3** landscape tabs (strip on top vs 56 dp left rail); UI-2 look is de facto answered by M6 (blue REAPER) - confirm.
- **O11 Scheduled GitHub workflow, yes/no** (asked 8 times, last OUTBOX 11-12T15:35Z): one daily job to compile ad-block lists (cuts the tablet's list requests from seven to one) and a gecko-watch for new engine versions (B6). It spends Actions minutes automatically, so CLAUDE.md requires an explicit yes; M23.10 "do whatever is logical" may be meant as the answer - say so if it was.
- **O12 `main` as the default branch** and `tools/MIRROR_MAIN` on (optional, TASKS line 17).
- **O13 Cromite A/B** (P2b, 15-30 min) and whether to build "Open in Cromite" (P2a).
- **O14 Real-site video check** (M16.6-M16.8, M19): name any site where a .ts still appears, the Download icon stays gray, a long-press shows no Download, or a spam page still opens after tapping play.
- **O15 Site-specific extractors** (M15.7): do you want YouTube-class sources despite the sites' terms and the upkeep; default is no.
- **O16 LinkMeter** (M25.1): after the next build, run Fast Wi-Fi and read the Diagnostics "Network:" line; Courier pair test Y2k only if you still want the Courier.
- **O17 Owner-side security actions** (SECURITY.md B7): Android Private DNS = a strict DoT provider, GitHub two-step login, a screen lock.
- **O18 Optional adb measurements** (P1f): cpuset sizes, governor, GL_RENDERER, /proc/pressure/memory, storage speed, 5 cold-start runs; Diagnostics > Copy gives GL_RENDERER without adb.

## (3) UNANSWERED QUESTIONS
- **U1 (M16.5)** "Figure out if it is using the best ad and tracker blocking software and technology": OUTBOX never states the verdict (only "Not done: uBlock-style blocking scripts, about 15 days"). The answer exists in research/AUDIT-2026-11.md section 5: own engine kept; it lacks scriptlets, redirect resources, `$removeparam`, procedural cosmetics and the full generic-cosmetic set; lists refresh daily. Relay it.
- **U2 (M6.1, M8, M9)** "Research the hardware I have": the findings were never reported (HW_SPEED.md: the GPU is a Mali-G52 MC2, not the PowerVR the brief said; four in-order Cortex-A53 cores; nothing needs switching on in Gecko; the lever is less work, no starved threads and memory policy). Relay it, with the P1e plan.
- **U3 (M1.1-M1.2)** "Is the Firefox engine good ...? Can it be injected ...? Should it?": no reply exists in OUTBOX (the message pre-dates capture); the answer is in BRIEF.md "Two builds" and ENGINES.md. Relay in one line.
- **U4 (M20)** "Start building the next step": no direct acknowledgement and no TASKS entry; covered by the alpha17 reply, but say which step was meant.
