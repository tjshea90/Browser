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

## 2026-11-11T16:17:28Z
> Alpha 6 is built, signed and published, and I tested it on the Android 10 emulator only, not on your tablet.
>
> https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.2.0-alpha6
>
> To update inside the app: Menu → Updates → Check now → Download.
>
> **White screen and reload.** When Android takes memory back from Firefox's page process, Kestrel used to reload the page from the top. Now it brings the page back at the same scroll position, behind a dimmed copy of the page with "Page freed to save memory - bringing it back". It never shows plain white. I tested it on the emulator by killing the page process mid-scroll: the dimmed preview appeared, then the page returned where it was.
>
> **Likely cause of the stutter.** An agent read Firefox's 160 source for me. Firefox's cache of decoded images could grow to about 475 MB on a 2 GB tablet. I had assumed it was already capped, and my old setting did nothing. A full cache makes Android signal "low memory", and Firefox answers by throwing away all decoded images and graphics caches. That gives hitches and blank areas when you scroll back up. I capped it at about 95 MB.
>
> **Our own overhead.** The neon glow now stays still while a finger is down, and page previews wait until 3 seconds after your last touch.
>
> **What I can't confirm:** how often Android sends "low memory" on your tablet, and whether the GPU driver plays a part.
>
> **New tools** so we stop guessing:
> - **Scroll feel** (Settings → Appearance): Standard, Smoother (draws more of the page ahead of your finger, aimed at white strips when you fling), or Smoothest (also a lower top fling speed). It defaults to Standard. Nothing proves either alternative helps here, so flip it and tell me.
> - **Scroll test** (Menu → Diagnostics, about 30 s): swipes a built-in page and prints one line like "Result: SMOOTH - about 55 frames a second…".
> - **GPU line** in Diagnostics: shows your GPU name and driver version.
>
> **Please send me any of these:**
> 1. Did alpha 3 scroll smoothly, or was it always like this?
> 2. The Scroll test Result line under Standard, under Smoother, and with Neon glow set to Off.
> 3. After a white screen: Menu → Diagnostics → Copy, then paste it here.
>
> **Cromite.** It can't be the engine inside Kestrel because it's a separate app; the full pros and cons are in my earlier message. To compare, install Cromite's `arm_ChromePublic.apk` (232 MB) beside Kestrel for 15 minutes and scroll the same three pages in both. If Cromite is dramatically smoother, I'll add "Open in Cromite" to the menu.
>
> **Still open:**
> - Your numbers above, which decide what I tune next.
> - The in-extension ad-blocking engine, which removes a per-request delay and could help blank areas when scrolling into lazy-loaded content.
> - A small startup glitch: a restored page briefly shows the Home screen with the page-style bottom bar.
> - Swipe-to-close in the tab grid.
> - Settings restyle.
> - Your pending decisions: signing key, licence and YouTube switch, and the landscape tab strip.
<!-- sha:e83205b8b0cf -->

## 2026-11-11T16:18:17Z
> **Short answer:** Cromite can't replace GeckoView inside Kestrel. It's a finished Chrome-style browser app, not a part another app can build on. So the real choice is Kestrel (Firefox engine) or Cromite as your browser, not an engine swap.
>
> **Why it can't be swapped in**
> - Cromite's embeddable form (Android WebView) exists only for 64-bit phones. Your tablet is 32-bit, and its WebView can't be replaced because the system image is locked.
> - Putting Cromite's engine under Kestrel's bottom bar, Home page, tabs, downloads and Shields would mean building Chromium ourselves. That needs about 100 GB of disk and 8–16 GB of RAM, and roughly 8 hours per build on a 4-core machine. It would have to be redone every 2 weeks for security fixes. GitHub's free builders (4 cores, 14 GB disk, 6-hour limit) can't do it.
> - What is possible: install Cromite next to Kestrel, or add an "Open in Cromite" menu item.
>
> **Cromite pros**
> - Chrome-style engines often scroll smoother on weak hardware, and sites are built and tested for them.
> - It has built-in ad blocking, privacy patches and no Google services.
> - It has a 32-bit build for Android 10 and up. I haven't tried installing it on your tablet.
> - Video uses the tablet's hardware decoder, same as Firefox.
>
> **Cromite cons**
> - You'd lose Kestrel's own features: bottom bar, Home page, private tabs kept separate, neon theme, tab previews, download manager, in-app updates and our Shields.
> - The APK is 232 MB (Kestrel G is 92 MB), and every update is another 232 MB over your hotspot.
> - JavaScript acceleration is off by default, which makes JavaScript about 40% slower. You can turn it on per site.
> - It gives every website its own process, which costs more RAM on a 1.9 GB tablet.
> - It has one maintainer. The last stable build was 15 Sep, which is about 8 weeks and 4 Chromium versions behind. For a browser, that means missing security fixes.
> - There are no extensions on Android.
> - Nobody has published speed numbers for your chip, so I can't promise it's faster.
>
> **GeckoView (what Kestrel G uses)**
> - **Pros:** it's built to be embedded, so everything in Kestrel works. It's small, updated every 2 weeks, and Mozilla says 32-bit ARM stays supported.
> - **Cons:** the stutter and white screens you're seeing, and more memory per page. Some sites are tuned for Chrome.
>
> **My recommendation:** stay on GeckoView and fix the real causes of the stutter, which is what alpha 6 does. To settle the engine question, install Cromite's `arm_ChromePublic.apk` beside Kestrel for 15 minutes and scroll the same 3 heavy pages in both. If Cromite is dramatically smoother, I'd add "Open in Cromite" to Kestrel's menu, which is about half a day of work. I'd also revisit if Mozilla drops 32-bit ARM, or if Developer options shows a second "WebView implementation" choice.
>
> Alpha 6 is here: https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.2.0-alpha6
>
> I'm still waiting on three things from you: did alpha 3 scroll smoothly, the Scroll test "Result:" line (Menu → Diagnostics) under Standard, Smoother and Neon Off, and a Diagnostics → Copy after the next white screen.
<!-- sha:3accbfaf5b7a -->

## 2026-11-11T16:18:30Z
> Those four notifications are stale wake-ups from the old auto-resume timer, which I deleted earlier today. Everything they pointed at is already done: alpha 4, 5 and 6 are built and recorded, and the download manager is finished. I took no action, and nothing more will fire from that timer.
>
> Still waiting on your three answers: did alpha 3 scroll smoothly, the Scroll test "Result:" lines (Menu → Diagnostics), and a Diagnostics → Copy after the next white screen.
<!-- sha:4b2628b2e072 -->

## 2026-11-11T18:00:53Z
> Alpha7 is built, signed by GitHub and published. I could only try it on the Android 10 emulator, not on your tablet.
>
> https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.2.0-alpha7
>
> **Installing it:** in the browser, open Menu → Updates → Check now → Download → Install, or open the link on the tablet and tap the APK. Keep about 800 MB free, because Android needs room to unpack the new version. If you still see the old "insufficient storage" message, tap Check now once. The old updater can't tell you how much to free, but alpha7's can.
>
> **Your requests**
>
> - **Storage failure:** The Updates sheet now says how many MB to free and why. If Android refuses the install, it asks for 150 MB more than Android said it had. After you free space, "Try again" installs the file it already downloaded. On the emulator, a simulated storage failure showed "Free up about 160.0 MB more, then tap Try again."
> - **Download progress:** Each download card now reads, for example, "27.4 MB of 115.2 MB · 23 % · 327 KB/s · 4 min left · 2 connections". A failed one shows "56.0 MB of 115.2 MB downloaded". I saw both on a real download.
> - **Neon:** I found two causes. One, the pulse stopped whenever Android's animation setting was off, and I'm assuming yours is. Two, it froze for good if you dismissed the keyboard with a field still focused. It now ignores the system animation setting, has a brighter ring plus glow, and also circles the address bar. To see it: open a new tab, hide the keyboard, and look at the big field. It holds still only while the keyboard is up, a finger is on the screen, or a full-screen page like Downloads or Settings is open. Diagnostics has a "Neon glow:" line saying why it's paused. On the emulator the brightness changed about 3× over time, with animations off.
> - **Scrolling and the white gap:** By default Firefox only pre-draws about 60 px beyond the screen on this tablet. That matches the white gap when you scroll back and the delay when you start scrolling after reading. **Smoother** is now the default: it pre-draws about twice the screen around your finger, keeps that for 3 minutes, and waits 300 ms instead of 600 for page touch handlers. It costs a little more memory and CPU per redraw. If it feels worse, set Settings → Appearance → Scroll feel to Standard. On the emulator the first frame after a touch dropped from 208 ms to 115 ms (95 ms with Smoothest), but the emulator is far slower than your tablet, so that says nothing about its real smoothness.
> - **Pull down to refresh:** Drag a page down from its top edge. A chip follows your finger and fills when it's armed (about 2 cm of drag). Let go to reload; a page that failed to load gets retried. It doesn't fire when the page can still scroll up, and there's a Settings switch. I saw it fire on the emulator, and a pull-down mid-page just scrolled.
>
> **I need from you** (none of this is verified on the tablet):
> 1. Diagnostics → "Compare 3 feels (90 s)", then Copy and paste the result to me.
> 2. Tell me whether the neon shows now and whether the white gaps are gone.
>
> **Still open:**
> - **Faster page loading (next big job):** moving ad blocking inside the browser, so each request no longer waits on the app.
> - **Cold-start glitch:** a restored page tab shows the Home screen for a moment while the engine starts.
> - **Tab grid:** swipe-to-close.
> - **Settings:** a restyle to match the menu.
> - **Your decisions:** the private signing key, the licence, the look, and the landscape tab layout.
> - **Cromite comparison:** the 15-minute side-by-side test is still yours to run.
<!-- sha:462eaea8001f -->

## 2026-11-11T18:02:58Z
> The owner's report points at the in-place update itself, not at free space. I'm checking whether consecutive releases are signed with the same certificate, which is the usual reason Android refuses an update but accepts a fresh install.
<!-- sha:61c18391d18e -->
