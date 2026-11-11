# OUTBOX - what Claude told the owner (the last reply of every turn), newest last. Written by tools/capture_outbox.sh on the Stop hook; key-shaped strings are redacted and long replies cut in the middle. The mirror of INBOX.md.

## 2026-11-11T03:24:17Z
> Both APKs now build, sign and publish on GitHub, and your save system from Launcher and Portfolio is running in Browser. I haven't run either APK on your tablet.
>
> **Downloads**
>
> Kestrel G, the Firefox-engine build (pre-release):
> https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.2.0-alpha2
>
> Kestrel, the WebView build:
> https://github.com/tjshea90/Browser/releases/tag/kestrel-v0.1.1
>
> Open the page on the tablet and tap the `.apk`. Both are signed with the same key as the APKs I sent before, so they should install over them.
>
> **How the GitHub build works now**
> - The `Build APK` workflow tests, builds, signs and checks the certificate. It then creates the Release, with the APK and its sha256 attached.
> - The first full run took about 5 minutes for both apps.
> - You can start it yourself from the Actions tab (Run workflow). Pick `gecko`, `web` or `both`, tick `full_build`, and add a note.
> - It never runs on ordinary pushes, only when started manually or when a `kestrel-v*` / `kestrel-g-v*` tag is pushed. This follows your Launcher rule.
> - GitHub creates the release tag itself, because tag pushes from the Claude sandbox are refused.
> - I won't send APKs as chat attachments anymore. Releases are the delivery channel.
>
> **Save system**
> - It follows your Launcher and Portfolio setup: every tool call is autosaved and pushed, `ckpt.sh` records "did / next", and `resume.sh` briefs a cold start. `ship.sh` and `record-release.sh` handle releases.
> - It also saves what you type and what I reply (`INBOX.md`, `OUTBOX.md`), as in your other repos. This repo is public, so those are visible to anyone.
> - The hooks only start in the next session. I wrote your two messages from this session into `INBOX.md` by hand.
> - `TASKS.md` is the job list, `HANDOFF.md` is the page for a session with no context, and `BUILDLOG.md` records every release.
> - I left out the compaction-veto guards, since that plugin isn't in this environment.
>
> **Things to know**
> - Kestrel G alpha2 has the same code as alpha1, rebuilt by GitHub.
> - Kestrel 0.1.1 is 0.1.0 plus an internal refactor. The unit tests pass, but I haven't re-run it on an emulator.
> - The signing key is still the public dev key in the repo, so anyone could sign an APK that updates yours. A private key from repo secrets is supported but needs your say-so, because the pinned certificate would have to change.
> - The default branch is the current session branch, and `tools/MIRROR_MAIN` is off, so nothing goes to `main`. A later session starts from the latest work unless it works on its own new branch. Making `main` the default branch and mirroring to it would fix that, but that's your call.
>
> Next, please install Kestrel G alpha2 over alpha1 and tell me whether it installs in place. Then tell me how claude.ai, Google, speed and memory feel on the tablet. Until you do, I'm holding the next big step, the cosmetic ad-hiding index.
<!-- sha:aeed195790b6 -->

## 2026-11-11T04:54:40Z
> Alpha 3 of Kestrel G is built, signed and released on GitHub, and it is ready for you to test. I've only run it on an emulator, not on your tablet. Thanks for confirming Kestrel G loads Claude.ai; I've recorded it as the first real-tablet success.
>
> https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.2.0-alpha3
>
> Install it over your current Kestrel G, which works because the signing key is the same. It is a pre-release.
>
> **The freeze you get in Brave**
> - I found real causes in our own code and fixed them:
>   - Every ad-blocker decision ran on the app's main thread, the one that draws the screen and handles typing.
>   - The page view was being torn out and re-attached on every navigation.
>   - The toolbar redrew constantly.
>   - Typing in the address bar resized the page under the keyboard.
> - The emulator log caught a real two-second main-thread freeze inside the ad-block code on the first page load. That work now happens in the background.
> - There is a new Menu → Diagnostics screen with a big Refresh screen button, which does what switching apps and coming back does. If the screen freezes, tap it, tell me whether it helped, and use Copy or Share on the report.
> - I can't reproduce your tablet's freeze here, so please type a lot (address bar, Claude.ai, forms) and tell me if it still happens.
>
> **Your four to-dos**
> 1. **Design and downloads:** I researched Brave, Chrome, Firefox, Samsung Internet, Edge, Opera and Vivaldi. The plan is 56–64 dp touch targets, an address row on top with a big button bar at the bottom in portrait, no animations, and a fresh look called "CERE" (slate greys, one yellow accent, flat, angled corners). The download manager design is done too: adaptive parallel pieces, true pause and resume, saving straight to Downloads. Neither is built yet; that is next.
> 2. **Speed:**
>    - There are no animations in the app or on pages.
>    - I tuned the engine for your 2 GB tablet: AV1 video off so sites pick formats the chip can decode, one content process, bounded caches, and no push or notifications.
>    - The research says Firefox's engine already uses the GPU by default, so there is nothing to switch on. I'll add a GPU readout to Diagnostics so you can check.
> 3. **Security:** I added fingerprinting protection, local-network blocking and a locked-down ad-block bridge. The biggest hole needs you: the app signing key is public, so anyone could sign a fake update. I'll walk you through the fix at github.com when you're ready. Meanwhile, set Android Private DNS to `dns.quad9.net`, turn on GitHub two-step login, and keep a screen lock.
> 4. **Ad and tracker engine:** the best design is running the blocker inside the browser engine itself. A prototype matched our current engine on 156,731 test requests with zero differences. On 13 real news and shopping pages in a desktop test, blocking cut requests 78%, data 49%, and median load time from 14 s to 2 s. The in-engine version, element hiding for all ~29k rules, and YouTube-style ad scripts (a
> [... 1946 characters omitted ...]
> /`.
>
> **Done so far:** research R1–R6; sync, cards and claims tooling; A1–A5 mostly; alpha 3 released and recorded.
>
> **Open / next steps:**
> 1. You test alpha 3.
> 2. Download engine (B1).
> 3. UI redesign (C).
> 4. In-engine blocker (D1).
> 5. Your decisions: signing key, licence, UI choices.
>
> **Rules to remember:** answer every prompt; re-send a denied call once; save constantly; no animations; big buttons; no Google services; be honest about what wasn't run on the tablet.
>
> ⚠️ **Health:** very long chat with many large outputs; early details may blur.
>
> 🔴 I'd move to a fresh chat now. Run `/amhandoff` and I'll write a file that brings the new chat fully up to speed. The project's own handoff in `CHECKPOINT.md` also lets a new session resume automatically.
>
> 💡 The code word is off in this chat. Type `/amcodeword` and I'll start every reply with 'Yes Boss!', so you'll notice if I start losing track.
<!-- sha:cbcb7440e6d4 -->

## 2026-11-11T06:49:36Z
> Alpha 4 is built, signed and published by GitHub Actions. I've re-checked your list, and items 1–5 plus part of 7 are done. I'm starting the download manager (item 6) now.
>
> **Alpha 4 download (tap on the tablet, it installs over alpha 3):**
>
> https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.2.0-alpha4
>
> What's in it, matching your numbered list:
> 1. **In-app updates:** Menu > Updates. It checks GitHub Releases once a day when the app opens, plus a "Check now" button. You then tap Download (it shows the size and asks first because the hotspot is metered). The download can pause and resume. It's checked against the release's checksum and installed through Android's installer. I ran the whole flow on an Android 10 emulator against a local test feed, going from version 102 to 103, but not against the real GitHub API or your hotspot.
> 2. **Bottom navigation bar:** address bar, reload, back, forward, Home, new tab, tabs and menu are all at the bottom. When you type, it lifts above the keyboard and the page is not resized, which matters for your typing-freeze issue.
> 3. **Home button and black Home screen:** one large stylized field in the middle. Words search Google and addresses open directly.
> 4. **Private tabs:** separate from regular tabs, with a violet look, a mask in the address bar, and a violet dot on the tabs button.
> 5. **Tab switcher:** a card grid with a page preview on every tab, and a Tabs | Private switch at the top.
> 7. **First pass on the redesign:** the new look (black and yellow, cut corners, no animations), plus a menu and Shields panel that rise from the bottom.
>
> **Not done yet (6):** Downloads still opens the old system screen. Building the real download manager now, with pause/resume, parallel segments and its own Downloads screen. It will ship as alpha 5.
>
> **Not verified:** all of this was checked on the emulator in portrait and landscape, not on your tablet. Please try it and tell me about any freeze while typing, or anything that feels slow or awkward.
>
> **Still waiting on you (defaults apply if you don't answer):**
> - Private release signing key (SEC-1).
> - Project licence and the YouTube setting (LIC-1).
> - Whether landscape should keep the tab strip on top (UI-3).
<!-- sha:65c2dd8d41b3 -->

## 2026-11-11T15:27:06Z
> Your Cromite question, answered now. The scroll and white-screen work continues below.
>
> **Short answer: I can't swap Cromite in as Kestrel's engine, and I don't think I should.**
> Cromite is a whole Chrome-style browser app, not a component other apps can use. Its Android WebView exists only for 64-bit phones. Your tablet's WebView can't be replaced (locked, 32-bit). Running Cromite's engine under Kestrel's bottom bar, neon theme, download manager and Shields would mean building Chromium ourselves. That needs about 100 GB of disk and many hours per build, plus a rebuild every 2 weeks. GitHub's free builders can't do it.
>
> So the real choices are: keep Kestrel G, or run Cromite as a separate browser beside it (or add an "Open in Cromite" menu item).
>
> **Cromite, pros**
> - Chrome's engine often scrolls smoother on weak hardware. Sites are tested against it, so fewer "this site breaks".
> - Built-in ad blocking, privacy patches, no Google services.
> - Video uses the tablet's hardware decoder (H.264), same as Firefox.
> - One person with a similar 2 GB phone says it feels smooth. That's one anecdote.
>
> **Cromite, cons**
> - You lose everything that makes Kestrel yours: bottom bar, Home page, separated private tabs, neon theme, tab previews, download manager, in-app updates, our Shields.
> - The APK is 232 MB (Kestrel G is 92 MB). Every update is 232 MB over your hotspot.
> - JavaScript acceleration is switched off by default, which makes JavaScript about 40% slower. You can turn it on per site.
> - It forces one process per website, which costs more RAM on a 1.9 GB tablet.
> - It has one maintainer. The last stable build was 15 Sep (Chromium 153) while Chromium 157 is out, so about 8 weeks of security fixes are missing.
> - No extensions on Android.
> - Nobody has published speed numbers for your exact chip, so I can't promise it's faster.
>
> **GeckoView (Kestrel G), pros:** it can be embedded, so everything in Kestrel works. It's small, updated every 2 weeks, and Mozilla says 32-bit ARM stays supported.
> **Cons:** the scroll stutter you're describing, and it uses more memory per page.
>
> **My recommendation:** stay on GeckoView and fix the real causes of the stutter. Then settle the engine question with a 15-minute test on your tablet. Install Cromite's `arm_ChromePublic.apk` from its GitHub releases and scroll the same 3 heavy pages in both. If Cromite is dramatically smoother, I'll add "Open in Cromite" to Kestrel's menu. It's about half a day of work.
>
> **Your scrolling and white-screen report.** I found one cause in the code. When Android kills Firefox's page process to free memory (likely on a 2 GB tablet), Kestrel throws away where you were and reloads the page from the top, with a white screen in between. That matches "goes white and has to reload". I can't see your tablet, so I'm adding a log of every kill with free memory, and fixing it so the page returns at the same scroll position with its last preview shown instead of white.
>
> I never sent you the alpha 5 link before. It has the blue neon theme, the download manager and some speed fixes:
>
> https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.2.0-alpha5
>
> Two questions that would save me hours:
> 1. Did alpha 3 scroll smoothly, or was it always like this?
> 2. Does scrolling feel better with Settings → Appearance → Neon glow → Off?
>
> All of this is checked on an emulator only, not on your tablet.
<!-- sha:e1e451c1f32d -->
