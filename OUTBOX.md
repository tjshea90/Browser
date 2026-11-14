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

## 2026-11-11T19:15:21Z
> The cause is Android's install storage rule, not the signing key, and alpha9 now accounts for it. I tested this on the emulator, not on your tablet.
>
> https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.2.0-alpha9
>
> **What I checked and found**
> - **Signing key:** All seven releases (0.1.1 and alpha2 through alpha7) carry the same certificate, so Android has no reason to refuse an in-place update on that ground.
> - **The rule:** Android refuses to finish any install, update or fresh, unless 10% of the tablet's storage is still free afterwards. While installing it also holds the new app twice (the download plus the installed copy), and unpacks the Firefox engine. That comes to a tenth of your storage plus about 0.37 GB. If your tablet has about 16 GB of storage, you need roughly 1.5 GB free.
> - **Why uninstalling first worked:** Deleting the old app and its data freed about a gigabyte at once, so the same APK suddenly fit. Installing from GitHub "fine" was always after that uninstall, so it didn't prove there was enough space.
> - **How I tested it:** On the emulator the same APK installs with 1010 MB free and fails with 1000 MB. A fresh install fails at the same point, and Android's log says it wants 10% of the disk. Making the APK 60 MB bigger raised the bar by about 120 MB.
>
> **What alpha9 changes**
> - **Updates screen:** Menu → Updates now works out the real number before it downloads anything, so you don't waste hotspot data. It shows something like "Free up about 480 MB more, then tap Try again".
> - **New button:** "Delete old installers" removes the Kestrel APKs the browser downloaded for earlier updates (92 MB each).
> - **Diagnostics and release page:** Diagnostics has a Storage line with the real numbers, and the GitHub release page explains the rule.
> - **Everything from alpha7** (neon, scrolling, pull-to-refresh, download sizes) is in alpha9 too.
>
> **What to do now**
> 1. In Android Settings → Storage, free about 1.5 GB. Start with old Kestrel `.apk` files in Downloads, then unused apps or videos.
> 2. Install alpha9 from the link over your current version, which keeps your logins, tabs and bookmarks.
> 3. If Android still says "App not installed", it needs more room; uninstalling first still works as a fallback.
> 4. For future updates, use Menu → Updates rather than tapping a downloaded file. It deletes its own copy once Android has taken it, which needs about 90 MB less space.
>
> **One question:** what do Settings → Storage show for total and free? That tells me whether the 10% figure holds on your tablet. If the real numbers differ, the updater corrects itself by asking for 150 MB more after any refusal.
>
> **Still waiting on you from alpha7**
> - The Diagnostics → "Compare 3 feels" numbers, copied and pasted to me.
> - Whether the neon glow shows now, and whether the white gaps are gone when you scroll back.
>
> **Still open**
> - Faster page loading by moving ad blocking inside the browser.
> - The cold-start glitch.
> - Swipe-to-close in the tab grid.
> - The Settings restyle.
> - Your decisions on the private signing key, the licence, the look, and landscape tabs.
> - The Cromite side-by-side test.
> - A backup/restore feature, only if updates still fail after alpha9.
<!-- sha:5bca6ce07171 -->

## 2026-11-11T20:16:30Z
> Alpha10 is out. Here's where things stand, then I carry on with the video downloader.
>
> **Kestrel G alpha10** — built and signed by GitHub Actions, run green:
>
> https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.2.0-alpha10
>
> What's in it, and what I did and didn't check (all testing was on the Android 10 emulator, not your tablet):
>
> - **Background downloads at full speed.** The update now downloads through the same engine as files. A foreground service plus a wake lock and a Wi-Fi lock keep it moving with the screen off. I saw it keep going with the screen off, using several connections at once.
> - **Notifications.** There are notifications for update available, downloading, ready to install (with an Install button), failed, and not enough space. Every download gets Pause/Cancel, and Retry on failure.
> - **Android 10 limit.** It cannot install silently, so the system "Install?" screen is mandatory. The "ready to install" notification takes you straight to it, so it's one tap.
> - **Quit is gone from the menu; swipe the app away instead.**
>   - With nothing running, the swipe ended all four processes and none came back.
>   - With a download running, the app stayed alive, finished the file, then exited by itself about 7 seconds later.
>   - While a download runs after the swipe, the browser engine stays in memory. That is a real RAM cost. A download-only process would fix it; I wrote that down as V3b.
> - **Neon pulse.** I can't confirm it shows on your tablet. If it still doesn't, Settings → Appearance now shows "The glow right now" with the reason it is holding, plus a "Test the pulse (10 s)" button.
> - **Notification tap.** Tapping a notification now closes Settings first, so the Updates sheet isn't hidden behind it.
>
> The first alpha10 build failed in CI. A flaky test caught a race in the test itself, not in the app. I fixed it, ran the full suite under heavy CPU load, and the second run was green.
>
> **Video downloader (in progress, alpha11).** I couldn't look inside InShot's app, only its store descriptions, so its internals are unconfirmed. The standard technique is to watch the page's network traffic for video files and playlists and offer them. I'm building that, with the research saved in `research/VIDEO_DOWNLOADER.md`. Long-press a video, tap "Download video", and pick a quality listed with its name and size. Plain MP4/WebM files come first, then HLS streams.
>
> What will not work, so you aren't surprised:
> - DRM video (Netflix, Disney+, Prime).
> - YouTube.
> - Live streams.
> - Links the site has already expired.
>
> **Still open:**
> - Speed and stutter review (alpha12).
> - The in-extension blocking engine (D1).
> - Your tablet checks: neon pulse, the three scroll feels (Compare 3 feels), and Settings → Storage.
> - Decisions SEC-1, LIC-1, UI-2 and UI-3.
>
> Now wiring the sniffer into the browser: tab state, long-press, menu entry and the quality sheet.
<!-- sha:2d37a9419a01 -->

## 2026-11-11T21:38:53Z
> Alpha11 is out. The release is built and signed by GitHub Actions:
>
> https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.2.0-alpha11
>
> **What it does: Download video.** Play a video for a few seconds, then long-press it (or open Menu → Download video). A sheet lists what the page offers, with each quality's name and size next to it. Tap one to download:
>
> - **Direct files** (MP4, WebM, MP3 and similar) show a row like `720p · MP4 · 48 MB · 1:02`. The quality name comes from the player or the page; if neither says, the row just says "Video".
> - **HLS streams** (the kind most video sites use) show `1080p · HLS · about 210 MB · 12:34`. The size is estimated from the bit rate, so it says "about". This includes streams with standard AES-128 encryption.
> - Downloads use several connections at once and can be paused and resumed. Files are named like `Page title - 720p.mp4`.
>
> **What I checked, and what I didn't.** I tested on the Android 10 emulator with test streams I made with ffmpeg: plain pieces, AES-128-encrypted pieces, and fragmented MP4. Each saved file was identical to the original pieces joined together. Pause stopped all traffic, and resume carried on. I have not tried it on real websites or on your tablet. Some sites will behave differently: expired links, sites that check cookies, and players that hide their stream.
>
> **What will not work:**
> - Copy-protected (DRM) video such as Netflix, Disney+ and Prime.
> - YouTube.
> - Live streams.
> - Sites that offer only DASH, a second streaming format. Kestrel will say so; I'm working on it.
>
> **How InShot does it.** I couldn't see inside the InShot app, only its store pages. Apps like it work the same way as this: watch the page's network traffic for video files and playlists, then list them. That is what Kestrel now does.
>
> **One limit in alpha11.** Streams made of `.ts` pieces are saved as `.ts`, which plays in VLC or MX Player. I'm testing the automatic conversion to MP4 now. It already works on the emulator for the picture but still has an audio-timing problem I'm fixing.
>
> I also fixed a real bug in the download engine. A finished download could be recorded as unfinished after an unlucky moment of timing.
>
> Still open: the speed and stutter review (alpha12). Your tablet checks are still waiting too: neon pulse, the three scroll feels, and Settings → Storage.
>
> Continuing with the MP4 conversion and the speed review.
<!-- sha:f4410ccf2f64 -->

## 2026-11-11T22:29:38Z
> **alpha12 is out.** It installs over your current Kestrel G because the signing key is the same. Android 10 will still show its own "Install?" screen, because it can't update silently.
>
> https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.2.0-alpha12
>
> **What changed**
> - **Download video works on more sites.**
>   - Long-press a video, or use Menu > Download video, to get the quality list with sizes.
>   - New: DASH streams, the Reddit-style kind with separate picture and sound. Kestrel downloads both and joins them into one MP4.
>   - New: `.ts` streams are converted to MP4, and HLS streams with separate sound are joined.
>   - New: plain video files get their resolution from their header, so rows read like "720p - MP4 - 48 MB".
> - **Smoother, faster pages (first round):**
>   - Ad blocking no longer restarts at every launch, so the first page after starting is blocked and scanned for videos straight away.
>   - Its answers skip an extra detour through the interface thread, and blocked requests are counted in batches.
>   - Download notifications refresh about once a second instead of up to five times.
>   - Touch handling, the tab grid, suggestions and keyboard tracking do less work.
>
> **What I could and couldn't verify**
> - Everything was tested on the Android 10 emulator with generated test videos: plain files, HLS with AES-128 and fMP4, `.ts` to MP4, and the DASH merge. Nothing ran on your tablet or on real sites yet. If a site refuses, tell me which one and I'll look at what it does.
> - Not possible in any app: DRM video (Netflix, Disney+, Prime), YouTube, and live streams.
> - I couldn't open InShot's internals, only its store pages. This uses the standard method for such apps: watching the page's network traffic and reading its video elements.
> - The neon pulse and the scroll feel are still unconfirmed on your tablet.
>
> **Next, already started:** moving the ad-block decisions fully inside the browser engine, so there is no per-request round trip to the app. That is the big page-load fix. It will ship as alpha13 with timing numbers in Diagnostics, so we can see the difference on your tablet.
>
> **Open on my side:** that ad-block change, the cold-start glitch where a restored tab briefly shows Home, tab-grid swipe-to-close, the Settings restyle, and a download-only process after swipe-away. Back up/Restore only comes into play if updates fail again.
>
> **Open on your side:**
> - Tablet numbers: Compare-3-feels and Settings > Storage.
> - Whether the neon pulse and the white-gap/scroll fixes show.
> - The Cromite A/B, if you want to run it.
> - Decisions:
>   - SEC-1: a private signing key.
>   - LIC-1: the licence.
>   - UI-2: the look.
>   - UI-3: landscape tabs.
>
> Now D1 step 1: the image encoder in `:core`.
<!-- sha:3a01500ddbdc -->

## 2026-11-12T00:07:16Z
> **alpha13 is out.** It installs over alpha12 and updates in place. Android 10 will still show its own "Install?" screen.
>
> https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.2.0-alpha13
>
> **What changed: the main page-load fix**
> - **Ad and tracker blocking now runs inside the browser engine.** Before, every request on a page made the engine ask the app "block this?" and wait for the app's screen thread to answer. A busy page makes hundreds of those requests, which is what made loading and typing stall. Now the engine decides for itself and the app is asked once per page.
> - **The first page after you open the browser is protected sooner.** The engine keeps a saved copy of the lists. Before, requests went through unfiltered while the app was still loading them.
> - **Diagnostics has a new "Blocker" line.** It shows timing numbers, and a button next to it switches back to the old way if a site misbehaves. Both take effect from the next page.
> - **Smaller extras:** embedded frames get smaller element-hiding styles, and the filter-list loader starts a bit quicker.
>
> **What I checked**
> - **Same answers as before.** The new blocker gave identical answers to the old one on 63,378 test requests built from the real filter lists, and on 2,200 more from a small test list. A deliberate-bug check caught every one of 10 bugs I injected.
> - **Emulator.** It blocks the test ad page 3 of 3. A restarted app judged its first page from the saved copy without asking the app. Page timing on the emulator was too noisy to prove anything: a 300-request test page took 38–44 s in the new mode and 45–51 s in the old one, with two runs each.
> - **Not measured on your tablet.** After browsing a heavy site, open Diagnostics, tap Update, then Copy, and send me the Blocker line. That gives me real numbers.
>
> **Still open on my side:**
> - The cold-start glitch where a restored tab briefly shows Home.
> - Tab-grid swipe-to-close.
> - The Settings restyle.
> - A download-only process after swipe-away.
> - Back up/Restore, only if updates fail again.
>
> **Still open on your side:**
> - The Blocker line from your tablet, as above.
> - Tablet numbers: Compare-3-feels and Settings > Storage.
> - Whether the neon pulse and the scroll/white-gap fixes show.
> - The Cromite A/B, if you want to run it.
> - Decisions:
>   - SEC-1: a private signing key.
>   - LIC-1: the licence.
>   - UI-2: the look.
>   - UI-3: landscape tabs.
<!-- sha:b8a3739fe815 -->

## 2026-11-12T00:32:05Z
> **alpha14 is out, and it fixes the "up to date" bug.**
>
> https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.3.0-alpha14
>
> **Why it said "up to date":** GitHub hands back this repo's releases in an odd order, not newest first. The app only read the first eight, and those stopped at alpha9, so it never saw alpha10 or anything newer. That is why every version from alpha9 up looked current.
>
> **What I changed**
> - **The check:** the app now asks for the newest versions directly and works out the order itself. The new check can't be fooled by GitHub's order.
> - **Older versions:** I numbered this release 0.3.0 on purpose. I checked that GitHub now lists it first, which is inside the first eight entries that the old broken check reads.
>
> **What you do:** in alpha9–alpha13, open Menu > Updates and tap Check now. It should offer alpha14. If it still says "up to date", download the APK from the link above once, and every later update works.
>
> **Tested:** the real GitHub release order is reproduced in tests. The old check misses alpha13 for every installed version, and the new one finds it. An end-to-end test against a fake GitHub server also passes, including the cheap "nothing new" case. alpha14 itself was built and signed by GitHub.
>
> **Not verified:** I haven't installed alpha14 on the tablet or seen the update offer on a real device.
>
> **Now building, from your message**
> - **The pop-up and redirect guard** for movie sites: it blocks new tabs and page jumps you didn't ask for, and shows a "Blocked a pop-up – Open" notice in case you wanted one. It's written and passes 15 browser-behaviour tests, but I haven't run it in the app's engine yet.
> - **A Download video button** that shows whenever a page has video, plus a long-press option that works even on custom players.
> - **MP4 instead of `.ts`.**
> - **Finding the video without pressing play.**
>
> **Still open from your message:**
> - The daily check for app, engine and ad-block updates, with an Update dialog.
> - The download manager upgrade and research.
> - Faster startup, page loads and scrolling.
> - The ad-blocking technology assessment, plus auto-updating lists.
>
> MKV needs a muxer Android doesn't include, and MP4 plays everywhere, so I'll explain that rather than force it.
<!-- sha:7790820b3774 -->

## 2026-11-12T01:50:19Z
> **Alpha15 is out.** Open this on the tablet and tap the APK (it installs over alpha14, or use Menu > Updates):
>
> https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.3.0-alpha15
>
> I tested all of this on the Android 10 emulator, not on your tablet, so please check it on your movie sites.
>
> - **Spam tabs and redirects:**
>   - A new tab or page jump is allowed only after a real tap of yours.
>   - It is refused when a timer, a hidden link, an invisible layer over the player or a script triggers it.
>   - On the test page, the window.open ad, the hidden link, the redirect and the timer pop-up were all blocked. A real link and a same-site window opened normally.
>   - A small notice with an "Open" button appears when something is stopped.
>   - You can switch it off under Settings > "Block pop-ups & forced redirects", or per site with Shields off.
>   - Some sign-in or share pop-ups from sites I haven't listed may get blocked; the "Open" button is the way through.
> - **Videos without pressing play:**
>   - The browser now finds videos in the page itself, so there's nothing to play first.
>   - It covers video tags, og:video tags, links to media files and player settings hidden in scripts.
>   - Ad networks are filtered out of the list.
> - **Your long-press problem:**
>   - A site's own player swallows the long press, so there is now a "Save video" button floating at the bottom right of any page that has something to save.
>   - Tap it to open the download list. The ✕ hides it for that page.
> - **.ts files:**
>   - HLS streams are now converted to MP4 even when the stream has the "discontinuity" markers that sites use to splice in ads. Previously those stayed .ts.
>   - Parts with a different picture or sound format, which is what a spliced-in ad usually is, are dropped.
>   - On my test stream (movie, ad, movie) the result was a clean 16-second MP4 with the ad gone.
>   - **MKV: no.** Android has no MKV writer, and MP4 plays everywhere.
>   - If a stream can't be converted, the reason now shows in Diagnostics.
>
> **Still open from your list:**
> - **Daily update dialog:** coded and compiling. The updater and its new engine check pass their tests. I haven't tried the dialog on the emulator yet, so it's not in alpha15. It covers the app, a newer Firefox engine and the ad-block lists, with an Update button that downloads in the background and then offers Install.
> - **Ad-block lists:** will refresh every day instead of weekly.
> - **Download manager upgrades:** still to do.
> - **Speed and Proton Mail audit:** I have the engine-tuning research now (written by a background agent, not yet checked against the code), and the audit is next.
> - **Decision for you:** I'd like a GitHub schedule that rebuilds the ad-block lists daily so the tablet downloads one small file instead of seven. That adds a scheduled workflow, which your rules say needs your OK. Say yes or no.
<!-- sha:fa6ef4fb01a4 -->

## 2026-11-12T02:58:34Z
> **Alpha16 is out.** Tap this on the tablet, or use Menu > Updates > Check now:
>
> https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.3.0-alpha16
>
> Everything below was tested on the Android 10 emulator, none of it on your tablet. You sent the same message twice, so here it is part by part.
>
> **"Do everything still on the list"**
> - **Shipped earlier:** the updater fix (alpha14), and the spam-tab/redirect guard, finding videos without pressing play, the Save video button and MP4 conversion (alpha15).
> - **Daily update dialog (alpha16):** it checks the app, the Firefox engine version and the ad-block lists. Its Update button downloads in the background, then offers Install now.
> - **Ad-block lists:** they now refresh every day by themselves.
> - **App download is now off by default:** a 92 MB download over your hotspot is your call. Settings > App updates > "Download updates in the background" turns it on.
> - **Download manager:**
>   - A download resumes by itself after the app is killed.
>   - It waits out a dead hotspot for up to 30 minutes instead of failing.
>   - An extra connection the server refuses no longer fails the whole download.
>   - Files of 8 MB or more start with 2 connections at once.
>   - Resume checks the file on the server is still the same one.
>   - A web page is never saved as your video or file; it says "The site sent a web page instead of the file."
> - **Not done:** checksum verification, queue and speed-limit settings, and uBlock-style blocking scripts (about 15 days of work).
>
> **"Audit of the app and engine"**
> It's written up in the repo as `research/AUDIT-2026-11.md`.
> - There is no code of ours in the typing path. It runs through the keyboard, the engine and the page's own JavaScript.
> - I made the UI refresh cheaper and moved the daily list refresh out of the first minute of use. I also fixed a race in the download code.
> - None of that is claimed to speed up Proton Mail.
>
> **"Slow Proton Mail, lag when typing"**
> I can't fix what I can't measure, so alpha16 adds the tools:
> - Menu > Diagnostics > "Page loads" shows time to first paint and to finished, per site.
> - Settings > Network lab has switches for encrypted DNS and HTTP/3, which are the likeliest hotspot slowdowns.
> - "Warm up the top suggestion" opens the connection to the first address-bar suggestion while you type.
>
> What I need from you, about 5 minutes:
> 1. Open mail.proton.me twice and paste the "Page loads" lines here.
> 2. Switch Encrypted DNS off, restart the browser and repeat.
> 3. Do the same with HTTP/3 off. Change one switch at a time.
> 4. Tell me which keyboard app you use, whether an accessibility or autofill service is on, and whether the hotspot is on 5 GHz.
>
> **"Modify the Firefox engine"**
> I had the bundled Firefox 160 checked (about 440 settings). Recompiling it isn't feasible: GitHub's builders can't build Firefox in their time limit, and Mozilla's own build is already optimized, so mine would be slower. Its settings already sit near what this tablet allows. I added one and will tune the rest on your numbers.
>
> **Open and waiting on you**
> - **Your OK:** may I add a GitHub schedule that builds the ad-block lists once a day? The tablet would download one small file instead of seven. Your rules say no scheduled workflows without your OK.
> - **Your numbers:** the Proton Mail page-load times above.
> - **Still to do:** checksum verification, queue and speed-limit settings, the blocking upgrade, and the keyboard-resize idea once I have your numbers.
>
> The first two alpha16 builds failed (a transient GitHub permission error at the last step, then a test race I fixed). The third is what's published.
<!-- sha:a4d73dd35a02 -->

## 2026-11-12T03:04:37Z
> Alpha16 is out and recorded, and everything is pushed to GitHub. Your message was a repeat of the earlier one. I answered it part by part above, including what I need from you for Proton Mail.
>
> https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.3.0-alpha16
>
> Since that message, I added a **Settings > Downloads** section. It has a speed limit for all downloads together, how many downloads run at once, and connections per download. It's tested but not in alpha16 on purpose: each release is a 92 MB download for you, so it will ride with the next alpha.
>
> **Waiting on you:**
> 1. **Proton Mail numbers:** open mail.proton.me twice and paste the "Page loads" lines from Menu > Diagnostics. Then repeat with Encrypted DNS off, then with HTTP/3 off, one switch at a time. Also tell me your keyboard app, whether an accessibility or autofill service is on, and whether the hotspot is on 5 GHz.
> 2. **A yes or no:** may I add a GitHub schedule that builds the ad-block lists once a day? Your rules say no scheduled workflows without your OK.
>
> **Still on my side:**
> - **Download manager:** checksum verification, queue priorities, and a few smaller items.
> - **Ad blocking:** the uBlock-style scripts, which is about 15 days of work.
> - **Typing and keyboard resize:** I won't touch this until I have your Proton numbers.
>
> I set a wake-up every 4 hours that only does small safe work and otherwise just says it's standing by, to save your usage. It deletes itself if you're quiet for a day.
<!-- sha:a9d5921e1f1f -->

## 2026-11-12T05:30:56Z
> alpha17 is out and built. Install it over your current Kestrel G:
>
> https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.3.0-alpha17
>
> **Your points from the last message**
> 1. **Long-press on a video:** it now opens Kestrel's own menu ("Download video", "Copy video address") instead of the site's player menu. Links and images on a page with a video also get "Download video from this page".
> 2. **`.ts` instead of mp4:** HLS streams are now written straight into an `.mp4` while they download, so the row says `.mp4` from the first second and no `.ts` file ever exists.
>    - **You were right about the remux:** nothing is re-encoded. The picture and sound bytes are copied unchanged into an MP4 wrapper, so it is fast and loses no quality.
>    - **Interrupted downloads:** if the download is killed half way, it carries on from where it stopped.
>    - **Not yet wrapped:** H.265 video and AC-3/MP3 sound stay as `.ts`, and the row says why.
>    - **Size in the list:** a stream's list entry now shows an estimated size, e.g. "about 6.0 MB".
> 3. **Fake play button that opens spam:** a tap on a hidden link laid over a player is blocked ("Blocked a pop-up", with Open / Dismiss). Real links with words still open. Fakes come in many shapes, so tell me if one gets through.
> 4. **Download icon in the nav bar:** it is gray when the page has no video, and a pulsing neon blue when it has one. Tap it for the list with name, resolution, type, size and length, then pick one. The floating button is gone.
>
> Settings > Downloads (speed limit, downloads at once, connections) is also in this release.
>
> **How sure I am:** I tested all of this on an Android 10 emulator, not on your tablet or on real video sites.
> - A 40-second 720p stream downloaded to an mp4 that decodes cleanly.
> - I killed the app half way through and the download finished by itself, with the same file.
> - Android's own video reader opens the result.
> - Real streams vary. If one still ends as `.ts`, send me the line under the row.
>
> **Already in the code, shipping with the next release:**
> - A "Make MP4" button for `.ts` files you already have.
> - A fix for the icon staying gray when the same page is open in two tabs.
> - Spotting ad inserts that no one marked in a stream.
>
> **Still waiting on you:**
> 1. The Proton Mail "Page loads" numbers from Diagnostics, taken twice. Then repeat with encrypted DNS off, then HTTP/3 off.
> 2. Which keyboard app you use, whether an accessibility or autofill service is on, and whether the hotspot is 5 GHz or 2.4 GHz.
> 3. Yes or no: may I add a scheduled GitHub job that rebuilds the ad-block lists once a day?
>
> **Open on my side:** download queue priorities and checksums, ad-block scriptlets (needs your yes on item 3), the typing lag (needs your numbers), the cold-start glitch, swipe-to-close in the tab grid, and backup/restore.
<!-- sha:aacdaeab552e -->

## 2026-11-12T05:31:19Z
> Standing by. The heartbeat reached this session late, and this session is the one that did the work. alpha17 is released and recorded in `BUILDLOG.md`. Everything else is committed and pushed (checkpoint 856), with no uncommitted changes.
>
> What remains either waits on you (your Proton Mail numbers, the tablet facts, and the yes/no on the scheduled list job) or is backlog that doesn't justify another 92 MB release yet. I'm leaving the heartbeat in place, since you wrote to me a few minutes ago.
<!-- sha:dd4f7e3e0d27 -->

## 2026-11-12T06:06:54Z
> **Alpha18 is out.** It is built, signed and checked by GitHub, and the certificate matches your installed version. The only testing so far was on the emulator, not your tablet.
>
> https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.3.0-alpha18
>
> **Y1: update no matter what (done)**
> - The updater no longer stops on its own "free 2 GB" estimate. It always downloads the update and hands it to Android, and Android decides.
> - If Android still refuses, the sheet says why in plain words and offers three buttons:
>   - **Try again**.
>   - **Storage settings**.
>   - **Install the other way**: it saves the update into Downloads and opens Android's uninstall screen. That is the uninstall-and-reinstall you did by hand. Bookmarks, history and settings are not carried over, and the sheet says so.
> - I can't make Android accept an update it has no room for. When your uninstall worked, the old app's space was freed first. That is why the two cases differ.
> - **Update by hand once:** alpha17 on your tablet still has the old gate. Open the link above on the tablet and tap the APK. From alpha18 on, the in-app update tries every time.
> - On the emulator, with less free space than Android wants, the update downloaded and Android's own "install an update?" screen came up. The refusal sheet was also checked there.
>
> Also in alpha18:
> - A **Make MP4** button on finished `.ts` downloads.
> - Ads spliced into a stream without any marker are spotted and left out.
> - The download icon now works when the same page is open in two tabs.
>
> **Y2: phone does the downloading (possible, but it needs a small app on the phone)**
> - The tablet can't make the phone download anything on its own, so there would be a second tiny app on the phone, "Kestrel Courier" (about 1–2 MB).
> - You pair it once by typing a code shown on the phone.
> - With the setting on, the tablet hands the download to the phone. The phone fetches it and turns videos into MP4. The tablet shows the same download row with live progress and the same Pause, Resume and Cancel buttons, then copies the finished file over wifi.
> - If the phone isn't reachable, the tablet downloads it itself and the row says why.
> - **It only helps if the slowness is on the hotspot path** (for example the carrier slowing tethered data), or if the tablet's processor is the limit. If the tablet's wifi radio is the limit, the finished file still has to cross that same radio, so the gain would be small.
> - So I'm adding a **Test the phone link** button. It measures three speeds for the same file: tablet to phone, the phone's internet, and the tablet's internet. It then tells you in one sentence whether it's worth it.
> - Data the phone downloads counts on the phone's plan, which may be a different bucket from your hotspot allowance. The phone also works harder (battery, heat).
> - I'll build it in three stages:
>   1. The Courier app, pairing, the speed test and plain file downloads.
>   2. Videos (HLS, DASH, MP4 remux) done on the phone.
>   3. Polish: overlapping the copy with the download, notifications on both devices, and so on.
> - The full design is in `research/PHONE_OFFLOAD.md`.
>
> I'm starting stage 1 now on the assumption that your phone is an Android phone and that installing the Courier on it is fine. Tell me if either is wrong and I'll stop.
>
> **Questions for you (Y2):**
> 1. Is your phone an Android 16 phone, not an iPhone?
> 2. Is installing a second small app on the phone OK?
> 3. When you say the phone is faster, what do you see? A rough MB/s, or minutes for the same file on each, would be enough.
> 4. Should it handle everything, or only videos and big files?
>
> **Still waiting on you:**
> - The Proton Mail "Page loads" numbers.
> - Your keyboard app, accessibility and autofill details, and whether the tablet does 5 GHz wifi.
> - A yes or no on a scheduled build of the ad-block lists. It would use GitHub minutes automatically, so I won't add it without your OK.
<!-- sha:24065bf7921c -->

## 2026-11-12T07:59:42Z
> The phone downloads are ready to try. The two APKs below are built and signed by GitHub. I tested them on the Android 10 emulator, not on your real phone and tablet.
>
> **1. Kestrel Courier 0.1.0-alpha1 — install this on your phone** (about 150 KB)
>
> https://github.com/tjshea90/Browser/releases/tag/kestrel-courier-v0.1.0-alpha1
>
> **2. Kestrel G 0.3.0-alpha19 — install this on your tablet**
>
> https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.3.0-alpha19
>
> **How to set it up**
> 1. On the phone, open the first link, tap the APK and allow installing from your browser if Android asks.
> 2. Open Kestrel Courier. Switch on **Ready for the tablet**. Tap **Allow it to run with the screen off**, and allow its notification if asked.
> 3. Turn on the phone's hotspot and join it with the tablet.
> 4. On the tablet, install alpha19. Alpha18 will also offer it as an update inside the app. Then open Menu > Settings and scroll to **Download on my phone**.
> 5. Tap **Pair with the phone** and type the 16-character code the Courier shows. This is done once.
> 6. Set **Use the phone** to "When the phone is there".
> 7. Tap **Test the phone link**. It measures the wifi between the two, the phone's internet and the tablet's internet. Then it says in one sentence whether the phone is worth using. That answers your question about whether the phone is really faster.
>
> **What happens when you download a file**
> - Your phone downloads it over its own connection.
> - The tablet's Downloads row shows it live ("On your phone", percent, speed, Pause and Cancel buttons).
> - When the phone has the whole file, the tablet copies it over the wifi, and the phone then deletes its copy.
> - If the phone isn't there or can't do a download, the tablet downloads it by itself and the row says why.
>
> **What I checked**
> - Both apps in one emulator: a 12 MB file went through the phone and the copy on the tablet matched the original byte for byte.
> - With the Courier off, the tablet downloaded by itself and showed the note.
> - Over 120 automated tests cover the rest of the logic.
>
> **What is not done yet**
> - Videos (HLS and DASH streams, and making them MP4) are still downloaded by the tablet itself. Doing that on the phone is the next stage. The design is written and the file downloads are the foundation for it.
> - Private tabs and app updates always stay on the tablet.
> - I could not test your real phone's rules (Android 16 background limits, battery saver) or the real hotspot speeds. The Diagnostics screen has a new "Phone link" line to send me if something doesn't work.
>
> **Questions for you (the Y2 ones)**
> 1. Is your phone an Android 16 phone, not an iPhone?
> 2. What do you see when you say the phone is faster, in MB/s or in minutes for the same file? The link test will give me real numbers if you send them.
> 3. Should it handle everything, or only big files and videos? Right now it takes every file the tablet itself would download.
>
> **Still waiting on you**
> - The Proton Mail "Page loads" numbers.
> - Your keyboard app, accessibility and autofill details, and whether the tablet does 5 GHz wifi.
> - A yes or no on a scheduled build of the ad-block lists. It would use GitHub minutes automatically, so I won't add it without your OK.
>
> Next I'm making the phone more patient on its first answer, since the emulator showed a slow first reply makes the tablet give up early, and checking what happens when Android kills the Courier in the middle of a download.
<!-- sha:d05cefc2c207 -->

## 2026-11-12T09:37:23Z
> Videos now go through your phone too. I tested it on the emulator, not on your real phone and tablet (details below).
>
> **How to try it:**
> 1. **On the phone:** install Courier alpha2 over alpha1 (same signing key) and leave "Ready for the tablet" on.
> 2. **On the tablet:** update to Kestrel G alpha20. Your pairing and the "Download on my phone" setting stay as they are.
> 3. **Download a video:** open a page with a video, tap the download icon, pick the video, then open Menu > Downloads.
> 4. **What you should see:** the row says "On your phone" with live progress, speed, Pause and Cancel. The finished MP4 then lands on the tablet.
>
> **What the phone does:**
> - It downloads HLS videos and videos whose picture and sound come apart, and it merges them with its own converter.
> - If the phone can't do a video or isn't reachable, the tablet downloads it itself and the row says why.
> - With an old Courier the tablet just downloads by itself.
> - Private tabs and app updates always stay on the tablet.
>
> **What I tested:**
> - On the emulator, with both apps running together, a video went through the phone and the MP4 came out identical in content to what the tablet makes on its own.
> - It survived Pause/Resume and the Courier being killed mid-video, and the fallback worked.
> - Testing caught a bug where the phone would have handed over a video without its sound. That's fixed.
> - Not checked: your real hotspot, real speeds, Android 16, and H.265 videos on your phone. Tap "Test the phone link" in Settings (about 40 MB) to see whether the phone is actually faster.
> - If the phone's data plan is the limit, the same plan pays for these downloads.
>
> https://github.com/tjshea90/Browser/releases/tag/kestrel-courier-v0.1.0-alpha2
>
> https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.3.0-alpha20
>
> **Still open, waiting on you:**
> - Is the phone an Android 16 phone?
> - When you say the phone is faster, what do you see (MB/s, or minutes for the same file)?
> - Should it handle everything, or only big files and videos?
> - Your real pair test of the two apps.
> - The Proton Mail "Page loads" numbers.
> - Your keyboard app, accessibility, autofill and 5 GHz facts.
> - A yes or no on a scheduled CI list-build workflow.
<!-- sha:8a11c0553a4e -->

## 2026-11-12T15:35:32Z
> Yes, the phone can speed up page loading, but only in one way. I built it, and I tested it on the emulator only, not on your real phone and tablet.
>
> Your LinkMeter notes say the carrier caps the **hotspot** at 5 Mbit/s, while the phone's own traffic isn't capped. So the phone helps when it opens the connections itself, not when it does "processing". It already did that for files and videos. Now it does it for web pages too.
>
> **What's new (Courier alpha3 on the phone, Kestrel G alpha21 on the tablet):**
> - **Pages through the phone.** The tablet's https pages go through a small proxy in the Courier, and the phone makes the real connections. The phone sees site names, never page contents. If the phone stops answering, pages load directly by themselves.
> - **One switch for hotspot only.** There's a new "Phone helper" tile in the main menu. Off means the tablet stops using the phone and the phone's proxy closes. The Courier's notification has "Turn off" if you want it fully stopped.
> - **Unmetered.** The data saver is now off by default, so there's no "metered connection" label and no big-download questions. The levels are still in Settings.
> - **Back and scrolling.**
>   - **Back:** 4 pages are kept for an hour (it was 2 pages for 6 minutes).
>   - **Memory:** 64 MB page cache, and decoded images kept 3 minutes.
>   - **Loading ahead:** images decoded two screens ahead, and lazy images start loading earlier.
>   - **One limit no setting can lift:** pages that send `Cache-Control: no-store` (webmail, banks) are never kept by any browser, so Back reloads those.
> - **Two clashes with LinkMeter fixed.** The Courier used LinkMeter's port 8765 (now 8780). The tablet also lost the phone while LinkMeter's VPN was on, which is fixed in code but untested with the VPN.
>
> **What the phone can't speed up:**
> - **Rendering, JavaScript and image decoding** stay on the tablet. Streaming a rendered page would make scrolling wait on the wifi.
> - **Prefetching or transcoding https** would need a fake certificate on the tablet.
> - **Ad block** stays in the browser. It needs to see the page, and at the proxy only the site name of https traffic is visible.
>
> **Wifi note:** 2.4 GHz tops out around 30–50 Mbit/s, so at night (150 Mbit/s) the wifi is the limit. With the phone close to the tablet, try 5 GHz.
>
> **What I tested:**
> - **Emulator:** a page loaded through the proxy from a name only the phone could resolve. Loading fell back to direct when the proxy was dead or refused the tablet. Back restored a page from the cache.
> - **Not tested:**
>   - your real hotspot
>   - real speeds and battery
>   - Android 16 background limits
>   - how scrolling feels
>
> To see whether it's worth it: install both, then Settings > Phone helper > Test the phone link. The last sentence says whether pages through the phone are faster, about the same, or slower. If not faster, switch it off from the menu. With an older Courier, the tablet just loads pages itself.
>
> https://github.com/tjshea90/Browser/releases/tag/kestrel-courier-v0.1.0-alpha3
>
> https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.3.0-alpha21
>
> **Still open, waiting on you:**
> - Your real pair test, and that "Pages through the phone" sentence.
> - How scrolling, white areas and Back feel now.
> - The Proton Mail "Page loads" numbers.
> - Your keyboard app, accessibility and autofill facts.
> - A yes or no on a scheduled CI list-build workflow.
<!-- sha:f780843ede9d -->

## 2026-11-12T19:33:49Z
> **Kestrel G alpha22 is out.** It also carries the Fast Wi-Fi and Courier changes from your last message.
>
> https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.3.0-alpha22
>
> Everything below was checked on the Android 10 emulator only. The emulator has no hardware acceleration, so its speed means nothing. I checked what happens, not how fast. Nothing here has been tried on your tablet or on real Fast Wi-Fi.
>
> ## Your four requests
>
> **1. Fast Wi-Fi.** The browser now notices when LinkMeter's Fast Wi-Fi is the active connection and switches itself to match:
> - **Lookups** go through the phone's own resolver instead of the encrypted-DNS route, which would have to squeeze through the tunnel.
> - **HTTP/3** is off, since Fast Wi-Fi refuses it and the tries would only fail.
> - **Phone helper** rests.
>
> When Fast Wi-Fi ends, your own settings come back exactly as you had them. Menu > Diagnostics has a "Network:" line saying which profile is active.
>
> **2. Courier closed.** Every Courier option is off by default. The "Phone helper" tile in the main menu only appears once the phone is paired, and it rests by itself while Fast Wi-Fi is on. No new Courier release was needed, so alpha3 stays as it is.
>
> **3. Your past prompts.** An independent reviewer went through all 25 of your messages (102 requests): 63 done, 33 partly done. In the order you stressed them, the open items are:
> 1. speed, scrolling and Back
> 2. typing lag in pages
> 3. ad-blocking depth
> 4. "mp4 always" leftovers
> 5. download manager
> 6. UI polish
> 7. Back up / Restore
> 8. security
>
> Finished now:
> - the speed items (alpha22 below)
> - engine freshness: Firefox's engine 160.0.20261104181552 is still the newest, so there is nothing to update
> - workflow security: GitHub's token is read-only by default, and links from web pages can only ask other apps for open, write, dial or share
> - bookkeeping: 18 old task items ticked, and the docs now say daily lists and an unmetered hotspot
>
> Already in the code, not in alpha22, coming in the next alpha:
> - swiping a tab card sideways closes it (promised three times)
> - a cold start opens straight onto "Opening <site>" instead of flashing Home
> - the network check no longer runs on the screen thread
>
> Next, in order: typing lag, ad-block depth, Back up / Restore, the Settings restyle.
>
> **4. Speed, what changed in alpha22:**
> - **Tabs stay ready by free memory.** Before, only 2 pages stayed alive, so with 3+ tabs every switch reloaded. Now it follows the memory the tablet reports: about 6 tabs at your usual 800 MB–1 GB free. Leaving the app no longer closes your tabs. Settings > Appearance > "Tabs kept ready" lets you force Few or Many.
> - **Back.** Firefox's own cache now keeps 6 pages for an hour (it was 4), and those Backs are instant. Pages that forbid caching (Proton Mail, banking) can't be kept by any browser and must reload. Kestrel now takes a picture of the page as you leave it and shows it the moment you press Back while the real page reloads. That hides the wait bu
> [... 2904 characters omitted ...]
> ny problem, Menu > Diagnostics > Copy.
> 2. For the Proton Mail slowness: open mail.proton.me twice and paste the "Page loads" lines from Diagnostics. Also tell me your keyboard app and whether an accessibility or autofill service is on.
> 3. A scheduled GitHub job that compiles the ad-block lists once a day (yes or no). I've asked eight times. If your "do whatever is logical" was meant as yes, say so.
> 4. The private signing key: only you can create those four secrets. Until then anyone holding the public dev key could sign an update.
> 5. With Fast Wi-Fi on, read the Diagnostics "Network:" line and tell me what it says.
> 6. Any site where a `.ts` file still appears, the download icon stays grey, or a spam page opens after play.
> 7. Optional: project licence, landscape tab layout, Cromite comparison. The full list is in `research/PROMPT_AUDIT.md`.
>
> I'm continuing down the list in the background.
<!-- sha:c815f5a97dc8 -->

## 2026-11-12T21:11:02Z
> **Kestrel G alpha23 is out.** The first part of your message repeats your last four requests; those were answered with alpha22, and their open items are listed at the bottom.
>
> https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.3.0-alpha23
>
> Everything below was checked on the Android 10 emulator only. Its speed means nothing, so I checked what happens, not how fast. Nothing here has been tried on your tablet or on a real LinkMeter connection.
>
> ## Your three new requests
>
> **1. One switch for the Phone helper.**
> - The menu tile and Settings now control the same switch.
> - **Off:** downloads, videos and pages through the phone are all off. Settings no longer shows anything switched on under an off helper, which was the "Load pages through the phone" you found still on.
> - **On:** all of it turns on. A stronger choice you made earlier ("always wait for the phone") is kept.
> - It stays off by default.
>
> **2. Long-press video: the new "Save video" sheet.**
> - **Exact video first:** the file under your finger is preselected and marked **THIS VIDEO**.
> - **Small or odd files:** the little .php files, previews and trackers sit under a closed group, "Other files on this page (3)". Each row there shows its file name (dl.php, getit.php, track.php) so you can tell them apart.
> - **How it decides:** it compares each file's size against the pressed one, and keeps other qualities of the same video in the main list. Tracker-looking addresses are tucked away.
> - **Name:** the NAME card shows the name the site gives the file (from its download header), otherwise the page title, with a **RENAME** button to edit it before saving.
> - **Formats:** one row per quality, such as 1080p, 720p or 360p, with the file type, size and length. A big **Download** button shows the size of the chosen row.
> - **Tested on a page I built** with three qualities and three .php-style files:
>   - the .php files stayed hidden
>   - a .php file whose server name is "Summer Trip 2026.mp4" showed that name
>   - I typed "My Holiday Clip", picked 720p and pressed Download; **My Holiday Clip.mp4** (1.8 MB) appeared in Downloads
> - **Bug found and fixed on the way:** a long press was ignored when two tabs held the same page.
>
> **3. Fast Wi-Fi.**
> - **Browser side:** it already switched to the phone's own lookups and turned HTTP/3 off. Now, when it recognises LinkMeter's own tunnel, it also:
>   - turns IPv6 off, because the tunnel blocks it and those lookups only dead-end
>   - lets downloads give a loading page about 1 MB/s instead of 150 KB/s. The 150 KB/s was set for the capped hotspot and would slow a big download for nothing on a link this much faster
> - **Other VPNs and no VPN:** another VPN keeps the careful values, and with no VPN nothing changes.
> - **On screen:** Home says "Fast Wi-Fi", and Menu > Diagnostics > "Network:" says whether LinkMeter's tunnel was recognised.
> - **LinkMeter side:** I read its tunnel and relay code and found no defect: no-delay sockets, 256 KB windows, sensible limits. I chan
> [... 850 characters omitted ...]
> arlier may keep the slow path.
> 2. How tabs, Back and scrolling feel now (Menu > Diagnostics > Copy for any problem).
> 3. For the Proton Mail slowness: the "Page loads" lines from Diagnostics after opening mail.proton.me twice, plus your keyboard app and whether an accessibility or autofill service is on.
> 4. A yes or no on a scheduled GitHub job that compiles the ad-block lists daily.
> 5. The private signing key. Only you can create those four secrets.
> 6. Any site where a .ts file still appears, the download icon stays grey, or a spam page opens after play.
> 7. Optional: project licence, landscape tab layout, Cromite comparison.
>
> **Next, in order:** typing lag, ad-block depth, Back up / Restore, then the Settings restyle. I already have a lead on typing: Firefox 160 has a setting that makes the keyboard shrink only the visible area instead of re-laying out the whole page. I'm testing it now.
<!-- sha:5811858b1cdd -->

## 2026-11-12T22:49:46Z
> Kestrel G alpha24 is built, signed and published. I tested it on the Android 10 emulator and with the unit tests, but not on your real tablet.
>
> https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.3.0-alpha24
>
> To install, open that page on the tablet and tap `kestrel-g-0.3.0-alpha24.apk`. It installs over the current Kestrel G because the signing certificate is the same. If Android says "not enough storage", free some room first. An install needs about a tenth of the tablet's storage free afterwards.
>
> **What's new**
> 1. **Back up & restore** (Menu > Settings > Back up & restore). "Back up now" writes one file, `Kestrel backup <date>.json`, into your Downloads folder with no picker and no permission prompt. It holds your bookmarks, history, settings, per-site Shields choices and open tabs. The file survives uninstalling Kestrel. "Restore" opens the file picker. It shows what the file contains and asks before changing anything. Bookmarks, history and tabs are added to yours, and your settings and per-site Shields choices are replaced by the backup's. The phone pairing code is deliberately left out, so you pair again after a restore. On the emulator I backed up, wiped the app, and restored. The settings, per-site Shields, bookmarks, history and tabs all came back.
> 2. **Much deeper ad hiding.** All 29,204 generic hiding rules from the lists now work; only the first 1,200 did before. Each one applies only on pages that actually contain a matching class or id, so pages aren't slowed. I also fixed two bugs in how the hiding rules were written that would have stopped some of them working. I checked this on test pages built with real names from the lists, including an element added late and an exception rule. I haven't seen it on real websites.
> 3. **Keyboard fix.** Tapping a text field in the lower half of the screen now scrolls it above the keyboard instead of leaving it hidden until you type. On the emulator the field moved from 406 px to 146 px, inside the 335 px of screen the keyboard leaves visible.
>
> **Still open: the typing lag.** The keyboard fix removes one cause, but the lag in Proton Mail and the Google search box needs numbers from your tablet. Type 20 characters in each and tell me roughly how long they take to appear. Also tell me which keyboard app you use and whether any accessibility or autofill service is on.
>
> **Next for alpha25.** The Firefox engine now answers ad-blocking requests on its own worker thread, so hiding rules arrive sooner after a cold start. I'm also adding list-driven stripping of tracking parameters from links you open, such as `?si=` on YouTube share links. The core part passes its 21 unit tests and the rest isn't connected yet. After that come the remaining ad-blocking items, "mp4 always", the download-manager queue and UI polish.
>
> **What I need from you**
> - Open Diagnostics with Fast Wi-Fi on. The Network line should say "LinkMeter tunnel recognised: true" and show 1000 KB/s.
> - Tell me how tabs, Back and scrolling feel.
> - Send the Proton Mail numbers and the keyboard and accessibility facts above.
> - Yes or no on a scheduled GitHub job that rebuilds the ad lists automatically.
> - The private signing key (four repo secrets), if you want releases signed with your own key.
> - Report any video-site problems: `.ts` files, a grey download icon, spam after pressing play.
> - Optional: the licence choice, landscape tabs, and the Cromite comparison.
<!-- sha:15de348e3486 -->

## 2026-11-13T00:48:18Z
> Alpha25 is built, signed and published; the Release has the 93 MB APK. It should install over your current Kestrel G, and the in-app Updates sheet will offer it.
>
> https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.3.0-alpha25
>
> Your message 16 reached me twice after a session restart, and I handled it once. The review is saved as `research/REVIEW-EXTERNAL-1.md`, and its 18 rows are in TASKS as EX-1 to EX-18. I worked them top to bottom, and alpha25 is the result.
>
> **Changes you will notice, and how to switch each off**
> - **Taps no longer wait about a quarter of a second on wide pages.** Double-tap zoom is now off by default; pinching still works. On the emulator a tap took 18 ms instead of 253 ms. To get double-tap zoom back, use Settings > Appearance.
> - **YouTube, Vimeo, Facebook, Instagram, TikTok, Spotify, SoundCloud, Disqus and Google Maps players now show a "Tap to load" box.** Nothing in them runs until you tap, so pages open faster. Settings > Shields turns it off, or you can turn it off per site in the Shields panel. On the emulator the box replaced a test player, and one tap loaded the real address.
> - **2,441 more rules clean tracking parts out of links you open.**
> - **Three fixes.**
>   - The Wi-Fi lock for background downloads really works on Android 10 now. Android logged it being taken when a download started and released at the last byte.
>   - "Warm up the top suggestion" really opens the connection while you type. The Firefox engine silently ignored it before.
>   - JPEG XL pictures are no longer requested; this tablet decodes them slowly.
> - **Settings > Network lab has six new switches, all off.** Each says what it costs, for example battery or lookups for links you never tap. Diagnostics has a Tap lab and a Decode lab to measure the tablet.
> - **Smaller fixes.**
>   - Ad-hiding rules arrive sooner after a cold start.
>   - The dock follows the keyboard.
>   - A swiped-away tab keeps its scroll position.
>   - A download whose file name Android refuses now tries the next name instead of failing. I found this while testing.
>
> **Not built:** I skipped the idea of pre-connecting to search results and to the link under your finger. It would contact sites you haven't chosen, for a head start of about a tenth of a second.
>
> **What was and wasn't tested:** The checks were 825 unit tests, 99 browser-script tests and the Android 10 emulator. Nothing was run on your tablet, and the emulator numbers only show the before/after difference.
>
> **Tip:** About once a week, leave the tablet on the charger overnight with the screen off. Android only does its storage clean-up and app optimising then.
>
> **Still open, and only you can do these**
> 1. Check the "Network:" line with LinkMeter on (the tunnel should be recognised, about 1000 KB/s).
> 2. Tell me how tabs, Back and scrolling feel.
> 3. Send Proton Mail numbers and your keyboard app. For typing lag, try Settings > Appearance, "Zoom to the text box you tap", off.
> 4. Answer yes or no on the scheduled GitHub list-build workflow.
> 5. Decide on a private signing key (four secrets).
> 6. Report video-site problems: `.ts` files, the grey icon, spam after play.
> 7. Send the Tap lab and Decode lab numbers from the tablet.
> 8. Try the Network lab switches one at a time and tell me the results.
> 9. Optional: the licence, landscape tabs, and the Cromite comparison.
>
> Next I'll finish the rest of the ad-hiding work and then the download manager queue.
<!-- sha:95e28771348b -->

## 2026-11-13T02:47:28Z
> Alpha26 is built, signed and published. Nothing in it has been tried on your tablet: the checks were 857 JVM tests, 114 browser-script tests, ffmpeg-made video test files, and runs on the Android 10 emulator.
>
> https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.3.0-alpha26
>
> It installs over your current Kestrel G, and the Updates sheet will offer it.
>
> **Fixes and optimizations from the review**
> - **Link warm-up:** the browser opens the connection to a link the moment your finger touches it, and to the first three search results after a search. On the emulator, holding a finger on a cross-site link made the browser connect while the finger was still down. The search-result part is covered by web tests only, because the emulator can't reach a search engine.
> - **Two optimizations are now on by default, not left as switches:**
>   - Firefox looks up the addresses of links ahead of your tap. The emulator can only load plain-http pages, where this already happens, so the https effect needs your tablet to measure.
>   - Web-font text appears in a plain font after half a second instead of staying invisible for up to 3 seconds. This was proven on the emulator.
> - **Privacy costs:** the warm-up makes the site see a connection from your phone's address even if you never open it. The link look-ups show your DNS resolver the addresses of links you never tap. Both stay off in private tabs and with Data saver. Settings > Network lab has "Warm up connections" and "Look up links ahead" to turn them off.
> - **New switch, off by default:** "No AVIF pictures on Fast Wi-Fi". Turn it on if the Decode lab shows AVIF is slow on your tablet.
> - **Proven on the emulator:** the Wi-Fi lock for downloads and the "no JPEG XL pictures" header.
> - **Not built:** I decided not to raise the download window size for Fast Wi-Fi. Two download connections already cover each other's gaps, and bigger windows would double the worst-case memory.
>
> **Work I was doing before the review**
> - **Download manager:**
>   - A waiting download says "Waiting in line" with Start now, Move up and Move down. A paused one also has Start now. I tried these on the emulator and they work.
>   - A file whose server states a checksum is checked at the end.
>   - Files are written in bigger pieces.
>   - A file name Android refuses makes the download try the next name instead of failing.
> - **Videos:** H.265 pictures and AC-3, E-AC-3 and MP3 sound now end as one MP4 instead of a `.ts` file. The rest of an advertisement the MP4 writer leaves out is no longer downloaded. ffmpeg decodes the files I produced without errors; whether your tablet's player does is untested.
> - **Ad blocking:** the bundled lists now include rules that restyle a page and rules that hide a box containing a word like "Sponsored". 522 of 1,172 such rules in the real lists are understood.
> - **Look:** Settings sections are cards like the menu and the Downloads list. The app also shows a shield on black the moment you tap its icon, instead of an emp
> [... 499 characters omitted ...]
> helper under Fast Wi-Fi, keep Wi-Fi awake (battery), zoom to the text box you tap, and fetching the next page early.
>
> **Open items for you**
> 1. Check the "Network:" line with LinkMeter on (the tunnel should be recognised, about 1000 KB/s).
> 2. Tell me how tabs, Back and scrolling feel.
> 3. Send Proton Mail numbers and your keyboard app. For typing lag, try Settings > Appearance, "Zoom to the text box you tap", off.
> 4. Answer yes or no on the scheduled GitHub list-build workflow.
> 5. Decide on a private signing key (four secrets).
> 6. Report video-site problems: `.ts` files, the grey icon, spam after play.
> 7. Send the Tap lab and Decode lab numbers from the tablet.
> 8. Try the Network lab switches one at a time and tell me the results.
> 9. Optional: the licence, landscape tabs, and the Cromite comparison.
> 10. Tip: about once a week, leave the tablet on the charger overnight with the screen off.
<!-- sha:639c7e60cb22 -->

## 2026-11-13T03:47:31Z
> **Alpha27 is out.** I found why the loading symbol stays on the video, and fixed it. I could not explain the choppiness: I have no access to your tablet, so the new Video lab (below) is how we find out.
>
> https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.3.0-alpha27
>
> **The stuck loading symbol**
> - **Cause:** the browser refused every request by a page to start a video by itself, even a muted one. Most players start muted and only hide their loading symbol once the video starts, so it never went away.
> - **Reproduced:** on the emulator with alpha26, muted autoplay stayed paused and a muted `play()` call got a "not allowed" error. With alpha27 both play and the symbol clears.
> - **Still blocked:** a video with sound still waits for your tap, as in every browser. **Settings > Video** turns muted autoplay off.
> - **Second suspect:** since alpha3 every site was told "reduce motion", and GIFs played once. That can freeze a site's own loading symbol. Pages now animate normally. **Settings > Video > Calm pages** brings the old behaviour back.
>
> **Your questions**
> - **Hardware:** H.264 and AAC (what most sites use) go through the tablet's own video decoders. The engine decodes VP9, AV1, Opus and similar itself, on the processor.
> - **VP9:** the engine already keeps it away from streaming sites.
> - **AV1 and H.265:** alpha27 now keeps them away too, unless the tablet has a hardware decoder for them. Sites then send H.264. The choice is **Settings > Video > Video formats offered to sites**.
> - **More codecs:** none needed. The few missing ones (Dolby, DTS) are licensed formats the tablet's own decoders cover if the maker paid for them.
> - **"Updated java":** I took this to mean JavaScript. It is Firefox 160's engine with its fast compilers and WebAssembly on. Java applets and Flash no longer exist in any browser.
> - **Protected (Netflix-style) video:** it was refused outright. It now asks "wants to play protected video" if the tablet supports it. The emulator does, and I proved the prompt there; your tablet is untested.
>
> **Choppy video**
> There are three possible causes, and the new tools tell them apart:
> - **Menu > Diagnostics > Video lab** plays short clips stored in the app and says smooth or choppy for each, with dropped pictures.
> - **All clips smooth:** the site's stream is probably more than your 5 Mbit/s hotspot carries. Pick a lower quality in its player.
> - **Only VP9 or H.265 choppy:** the processor is doing the decoding.
> - **H.264 choppy too:** the problem is the tablet or the page itself.
> - **Diagnostics** also lists the tablet's real video decoders, hardware or software.
>
> **Paid or licensed items** (none needed today, full list in `research/VIDEO_AUDIT.md`):
> - **Widevine L1** (HD protected video): Google, through the tablet's maker.
> - **Patent pools**, only if we ever shipped our own decoders: Via LA (H.264, AAC) and Access Advance (H.265).
> - **Dolby and DTS:** for their soundtracks.
>
> I declined three free options. Ruffle (a Flash emulator) is 
> [... 550 characters omitted ...]
> e of the test clips.
>
> **Please send:**
> 1. The site or link of the choppy video, and whether the symbol shows when the page opens or after you press play.
> 2. The Video lab result (Run, then Copy the results).
>
> **Still open from you:**
> - **LinkMeter Fast Wi-Fi:** the "Network:" line in Diagnostics, and the speed (~1000 KB/s).
> - **Feel:** tabs, Back and scroll.
> - **Proton Mail:** the numbers, and whether turning off "Zoom to the text box you tap" helps the keyboard.
> - **Ad-lists build:** yes or no to a scheduled GitHub build of the lists.
> - **Signing key:** the private signing key (four secrets).
> - **Earlier video problems:** the `.ts` files, grey icon and spam after play.
> - **Lab numbers:** Tap lab and Decode lab from the tablet.
> - **Network lab:** the switch comparisons.
> - **Optional decisions:** licence, landscape tabs, Cromite comparison.
> - **Charging:** the weekly overnight-charge tip.
<!-- sha:efc99f48089f -->

## 2026-11-13T03:47:59Z
> Two late heartbeat wake-ups arrived: scheduled prompts from the 2-hourly "Kestrel resume heartbeat" that I set up earlier. They fired while I was busy, and their text still describes the alpha22 work list. I'm standing down on both, per the rule they carry ("stand down if another session's real work is under 60 minutes old"). This session finished alpha27 minutes ago.
>
> - **State:** nothing is uncommitted. Local and GitHub are both at `c689906`, which includes alpha27 recorded in BUILDLOG.
> - **Heartbeat:** it stays armed (`trig_01LLoVKsjuTUaDn61UGP1pBN`). Its delete condition, 24 hours of owner silence with nothing safe left, is not met, since you wrote about two hours ago.
> - **Nothing started:** the remaining items either need your tablet data or are speculative without it. These are the ad-player stand-ins, the fragmented-MP4 downloader, background audio and picture-in-picture. I'll pick them up once the Video lab numbers or the choppy video's site arrive.
>
> My reply above is current: the same Release link, and the same questions still open for you.
>
> https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.3.0-alpha27
<!-- sha:29ddb7e9c7de -->

## 2026-11-13T04:59:32Z
> **Alpha28 is out. I found why doraby.com loses its photos and the search icon, and fixed it.** Everything was tested on the emulator, but one link in the chain is not seen on your tablet yet.
>
> https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.3.0-alpha28
>
> **doraby.com**
> - **What the site needs:** its photos, menu and search icons all load through one helper host, `cdn.jsdelivr.net`. If those files can't arrive, the page looks exactly as you described. I reproduced that on the emulator.
> - **The cause:** Kestrel asked Firefox's built-in tracking protection for its strictest set. That set includes a "Content" list that names jsDelivr and many video-player hosts (JW Player, Brightcove, Kaltura, Mux, Bitmovin, Vimeo and more).
> - **The fix:** alpha28 leaves that list out. Ads and trackers are still blocked by Kestrel's own lists.
> - **Likely the video player too:** the loading symbol stuck on your video player is probably this same cause. The muted-autoplay problem from alpha27 was real as well.
>
> **Proof, and what's missing**
> - I checked that Firefox blocks hosts on that list (a stand-in host was blocked), that Kestrel's old setting included it, and that the new one drops it.
> - jsDelivr itself being refused on your tablet is not observed, because the emulator can't download Mozilla's lists. So please check doraby.com yourself.
>
> **New tools**
> - **Menu > Diagnostics > Page report:** for each page it lists which files failed and why, script errors, and what Firefox's own protection blocked. It is never kept for private tabs.
> - **Menu > Reload without cache:** loads the page and all its files fresh.
>
> **Your lab numbers**
> - **Video:** all four clips (H.264 720p and 1080p, VP9, H.265) played with 0 of 150 pictures dropped, decoded by the video chip. The screen drew at 65 frames per second. So the tablet is not what makes video choppy; the cause is the site's stream versus the 5 Mbit/s hotspot, or the page itself.
> - **JavaScript:** about 3 to 5 times slower than a phone, as expected. WebAssembly "fast math" (SIMD) is not available on this tablet's 32-bit mode, which is rarely needed.
> - **Protected video:** Widevine works.
> - **Lab fix:** the "24.8 fps, mostly smooth" lines were my arithmetic error (fixed).
> - **Tap lab:** taps are answered in 3 ms, so the double-tap-zoom fix works on the tablet.
> - **Decode lab:** AVIF takes 108 ms to decode against WebP's 50 ms, but its files are three times smaller. On your hotspot AVIF still wins, so "No AVIF" stays off.
>
> **Please check**
> 1. After installing, does doraby.com show the photos and the menu and search icons? If anything is still missing, tap Menu > Diagnostics > Copy and paste it to me. The Page report names the failed files.
> 2. If a video still stutters, send me the site or link.
>
> **Still open from you**
> - **LinkMeter:** the "Network:" line in Diagnostics.
> - **Feel:** tabs, Back and scroll.
> - **Proton Mail:** the numbers and keyboard test.
> - **Ad lists:** yes or no to the scheduled GitHub list build.
> - **Signing key:** the private signing key.
> - **Earlier video problems:** the `.ts` files, grey icon and spam after play.
> - **Network lab:** the switch comparisons.
> - **Optional:** licence, landscape tabs, Cromite comparison.
> - **Charging:** the overnight-charge tip.
<!-- sha:6db802838522 -->

## 2026-11-13T05:02:23Z
> I saved a handoff for this session, and the work is committed and pushed to GitHub.
>
> The handoff files are `.handoff/handoff.md` and `.handoff/handoff.json`, plus a timestamped copy under `.handoff/handoffs/2026-11-13/`. It also refreshed the learnings section in `CLAUDE.md` and `AGENTS.md`. I checked the files for the owner's email and found nothing.
>
> The next session picks up from there. The main thing it is waiting on is the owner confirming that alpha28 fixes doraby.com and the video player on the tablet. If it is still broken, they should send a Diagnostics copy so the Page report can name the failed files.
>
> The two doraby.com fix links I couldn't test on the tablet remain open. I tested the Firefox-tracking-protection cause on the emulator, but not jsDelivr being refused on the owner's tablet itself.
>
> The 2-hourly wake-up trigger (`trig_01LLoVKsjuTUaDn61UGP1pBN`) is still active. It stands down while work is less than an hour old.
<!-- sha:da89b6f0586a -->

## 2026-11-13T07:18:45Z
> Your report shows the tablet is still on alpha27. alpha28 never installed, so the doraby.com fix hasn't reached it. The tablet only has 1.62 GB free of 27.75 GB, and Android refuses an update unless about 3.1 GB would still be free afterwards. It insists on keeping a tenth of the storage free, plus room to unpack the new version.
>
> I made alpha29, which includes alpha28 (full list below). I haven't run any of it on your tablet. I tested it on an emulator and with unit tests.
>
> **What to do**
> 1. Free about 1.5 GB, more if you can.
>    - Kestrel's menu, then Downloads, lists every file with its size. Delete movies you've already watched.
>    - Android's Settings, then Storage, shows what else is big. If the tablet has a microSD slot, move videos there.
>    - Android's Settings, then Apps, then Kestrel G, then Storage, then Clear cache gives back about 228 MB right now.
> 2. Install the newest Release below. It covers everything since alpha27.
> 3. Don't uninstall first. You'd still be about 1.1 GB short and would be left with no browser.
>
> The 3.1 GB figure comes from a rule I measured on a test tablet, so your tablet's own message may differ. If Android refuses, Kestrel's Updates screen shows its exact number.
>
> **What else your report showed**
> - **doraby.com:** 18 requests were blocked, but only 7 were ours. Firefox's own tracking protection blocked about 11 more, which confirms the cause.
> - **Video hardware:** your tablet has real hardware decoders for H.264, H.265, VP9 and VP8, and a Mali-G52 GPU. AV1 is software only, so Kestrel never offers it. Protected video (Widevine L3) is standard-definition only.
> - **Scrolling:** all three scroll modes measured smooth, 62–65 frames a second.
> - **Fast Wi-Fi:** the Fast Wi-Fi tunnel is recognised, and the keyboard detection works.
> - **pelisjuanita.com:** the part I can see is a login page, so I can't tell whether its video pages have a problem.
>
> **What alpha29 changes**
> - **Shields off:** it now also turns Firefox's own tracker blocking off for that site. If a page is missing photos, icons or its player, tap the shield icon and switch Shields off for the site.
> - **Updates screen:**
>   - It shows the real numbers, warns before a download the installer would refuse, and has a Clear cache button.
>   - It lists your biggest downloads.
>   - It only offers "install the other way" when uninstalling would actually free enough.
> - **Diagnostics:** the Video line was stuck on "decoders unknown" and now shows the real ones. The Storage line says how far short an update would be.
>
> **What I tested**
> - **Emulator:** Shields off made a blocked test script load. The warning and Clear cache button worked with the real engine (cache 24 MB to 0.1 MB, 23.5 MB freed).
> - **Not reproduced:** Android's real "not enough storage" refusal after a download. Only unit tests cover that screen.
>
> https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.3.0-alpha29
>
> **Still open, from you**
> - Once alpha29 is installed: do doraby.com's photos, icons and movie player work? If not, open Menu, Diagnostics, Page report right after the page loads and send me the copy.
> - Which site or link has the choppy video, and on which page does the loading symbol stick?
> - Does scrolling and Back feel smooth to you, given the measured numbers?
> - Proton Mail speed, and the keyboard app, with "Zoom to the text box you tap" switched off.
> - Yes or no: build the ad-block lists on a GitHub schedule?
> - The private signing key (four repo secrets).
> - Earlier video-site problems: .ts files, the grey icon, spam after play.
> - Network lab switch results.
> - Optional: a licence, landscape tabs, the Cromite comparison.
> - Weekly tip: charge the tablet overnight with the screen off.
<!-- sha:77e28b19adf3 -->

## 2026-11-13T12:21:06Z
> All three requests are done and in alpha30, but I haven't tested any of it on your tablet. It is still on alpha27, so none of the fixes have reached it.
>
> **1. Settings in private mode.** Settings now stays in the menu in private windows (both apps) and opens in the private colours. On the emulator the private menu lists Settings and it opens.
>
> **2. "Open in new tab" in the background.** After a long-press on a link, the page now opens in a tab behind the one you're on. The foreground tab stays, the tab counter goes up, and a small "Opened in a new tab" notice appears. On the emulator the second page loaded and the front tab stayed put. "Open in private tab" and "Open image in new tab" work as before.
>
> **3. Doraby movies and the Cloudflare message.** I found the likely cause, but I haven't seen it on a device.
> - Doraby's movie is a frame from another site (vidfast.vc, vidy.st and others). Cloudflare's check runs from `challenges.cloudflare.com`.
> - Firefox's own tracking protection, as alpha27 set it up, lists `cloudflare.com` and `cdn.jsdelivr.net` as "content" and refuses them. That matches Doraby's missing photos and "can't access Cloudflare".
> - alpha28 removed that. Shields off for a site (alpha29) now also switches Firefox's own protection off for that site.
> - Kestrel's own lists don't block the six Doraby players or Cloudflare's check; a new test confirms it.
> - **Two questions for you:** which version does Menu > Diagnostics show on its "App:" line? And what were the exact words on the site that said "incompatible browser"?
>
> **Your complaint that prompts went unanswered.** I compared your messages with my replies. Each one had a reply, but several came hours late, were folded into a release message, or answered only part of a multi-part prompt. Your Diagnostics paste took until 07:18 to get an answer. I can't tell which ones you meant. Which earlier prompts did you mean? Meanwhile the list is in `TASKS.md` (AI1d) and in `.handoff/handoff.md`. The handoff tool needed an approval that nobody answered, so I wrote that file by hand.
>
> **Install:** free about 1.5 GB first; the tablet has 1.62 GB free and needs about 3.1 GB. Use Menu > Downloads to delete movies you've watched, and Android's Settings > Storage. Then install alpha30.
>
> https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.3.0-alpha30
>
> **Needs your approval:** the heartbeat (a 2-hourly self-wake-up) is still on. It fires with an old message every 2 hours and cancels any approval prompt I have open. Please approve my "disable heartbeat" request next time it appears, or tell me to delete it.
>
> **Still open from you**
> - Doraby: after installing alpha30, open a watch page, tap play, wait 10 s, then send Menu > Diagnostics > Page report.
> - The site or link with the choppy video.
> - Does scrolling and Back feel smooth?
> - Proton Mail speed, and the keyboard app with "Zoom to the text box you tap" off.
> - Yes or no: scheduled GitHub ad-list builds.
> - The private signing key.
> - Earlier video problems: `.ts` files, grey icon, spam after play.
> - Network lab results.
> - Optional: licence, landscape tabs, Cromite comparison.
> - Weekly tip: charge the tablet overnight with the screen off.
<!-- sha:5dca78d6d931 -->

## 2026-11-13T14:23:40Z
> Standing down: the heartbeat's old work list is finished, and your three requests are shipped in alpha30 and answered. I'm waiting for your reply on the version shown in Menu > Diagnostics, the exact words of the Cloudflare site, and which earlier prompts you meant.
<!-- sha:c63a9a6b61e6 -->

## 2026-11-13T16:19:08Z
> Standing down again. This is the same 2-hourly heartbeat with the old alpha22-era text, and its work list is finished. I'm waiting for your reply on alpha30. The heartbeat is still on and costs a wake-up every 2 hours; say "disable it" and approve the request, or tell me to delete it.
<!-- sha:b5f77a9002d8 -->

## 2026-11-13T17:44:22Z
> Yes, adb helps. It also exposed a mistake of mine: your tablet very likely has enough room to update already.
>
> **What I got wrong.** I told you Android keeps a tenth of the storage free after an install, so you'd need about 3.1 GB free. I checked the Android 10 source and tested on the emulator, and that was wrong. Android keeps 5 % of storage free, but never more than 500 MB, and an update needs about 0.45 GB on top of that. On your tablet (500 MB + 0.45 GB, about 1 GB) the update should already fit in your 1.6 GB free. That is tested on the emulator only, not on your tablet, which may have different settings. I fixed the numbers and wording in the app's code, but the fix is not released yet. You don't need it to update.
>
> **Setup once.** On the tablet, go to Settings > About tablet and tap Build number 7 times. Then open Developer options and turn on USB debugging. Plug in the cable and accept the prompt on the tablet. On the computer, install Android "platform-tools". `adb devices` should then show `device`. Turn USB debugging off when you're done.
>
> **First, three read-only commands. Paste the output back.**
> ```
> adb shell df /data
> adb shell settings get global sys_storage_threshold_percentage
> adb shell settings get global sys_storage_threshold_max_bytes
> ```
> `null` means Android's normal settings.
>
> **Then install over the cable.** Download the alpha30 APK on the computer, which is faster than the 5 Mbit/s hotspot. Run this in the folder where you saved it, with the APK's file name:
> ```
> adb install -r <the APK file>
> ```
> It prints `Success` and keeps your data. If it fails, the error line says exactly why.
>
> https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.3.0-alpha30
>
> **Commands that help speed or function.** I ran these on the Android 10 emulator. The commands worked, but I did not measure a speed-up from any of them.
> - **Animations off** (undo by setting each back to 1). This makes the whole tablet feel snappier:
>   ```
>   adb shell settings put global window_animation_scale 0
>   adb shell settings put global transition_animation_scale 0
>   adb shell settings put global animator_duration_scale 0
>   ```
> - **Free memory before browsing:** `adb shell am kill-all`.
> - **Find what eats the 2 GB:** `adb shell dumpsys meminfo`. Paste the "Total PSS by process" part. I can then tell you which preinstalled apps are safe to switch off. That is reversible with `pm enable`.
> - **In-app updates without the Settings detour:** `adb shell appops set app.kestrel.browser.gecko REQUEST_INSTALL_PACKAGES allow`
> - **Downloads keep going on battery with the screen off:** `adb shell cmd deviceidle whitelist +app.kestrel.browser.gecko`. Undo it with `-` instead of `+`. I only checked that the command runs.
> - **More room:** `adb shell pm trim-caches 2G` clears other apps' caches. `adb pull` copies big files to the computer, and then you can delete them on the tablet.
> - **Only if an install ever says "not enough storage":** `adb shell settings put global sys_storage
> [... 1310 characters omitted ...]
> nything to this repo") reached a second session started around 17:17, not this one. This reply answers both. Nothing was written for this answer except the automatic chat log. My test notes and the unreleased fix were saved before that message.
>
> **Still open from you:**
> - The adb outputs above, or just try the in-app update now.
> - Menu > Diagnostics "App:" version line.
> - Doraby: after updating, open a watch page, tap play, wait 10 s, then send Diagnostics > Page report. Also the exact words of the "incompatible browser / Cloudflare" site.
> - Which earlier prompts felt unanswered.
> - The choppy-video link, scroll and Back feel, Proton Mail numbers, and keyboard test.
> - Yes or no on the scheduled GitHub ad-list build.
> - Your private signing key.
> - Earlier video problems (.ts files, grey icon, spam after play), and network lab results.
> - Optional: licence, landscape tabs, Cromite comparison.
<!-- sha:67ae69a79246 -->
## 2026-11-13T17:35:45Z
> No permission will make Kestrel faster: it asks Android for almost nothing, and none of it affects speed. Below is what adb can do, best first.
>
> I can't reach your cable from the cloud, so none of this ran on your tablet. "(tested)" means I ran it on the Android 10 emulator last session (`research/ADB_FOR_THE_OWNER.md`). The rest are standard Android 10 commands I haven't run anywhere. None need root. Commands with a `|` are in quotes so they run on the tablet and work on any computer.
>
> Names: Kestrel G is `app.kestrel.browser.gecko`, Kestrel (WebView) is `app.kestrel.browser`, and Courier on the phone is `app.kestrel.courier`. Start with `adb devices`; the tablet should show as `device`. If it says `unauthorized`, tap Allow on the tablet.
>
> **1. Install updates over the cable (tested)**
> ```
> adb install -r the-downloaded-file.apk
> ```
> This updates in place, keeps your tabs and settings, and skips the 99 MB download over the hotspot. If it fails, adb prints Android's exact reason.
>
> - **Space:** last session's emulator maths says alpha30 needs about 0.95 GB free, and your last report showed 1.62 GB. The app's "needs 3.1 GB" message was a wrong estimate (fix AJ1d).
> - **`INSTALL_FAILED_INSUFFICIENT_STORAGE`:** run `adb shell pm trim-caches 900G` (tested) and retry.
> - **`INSTALL_FAILED_UPDATE_INCOMPATIBLE`:** the installed copy was signed with another key. Don't uninstall, because that wipes your tabs. Tell me first.
>
> Newest build per the checkpoint:
> https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.3.0-alpha30
>
> **2. Pre-compile Kestrel's own code**
> ```
> adb shell cmd package compile -m speed -f app.kestrel.browser.gecko
> adb shell "dumpsys package dexopt | grep -A4 app.kestrel.browser.gecko"
> ```
> Android normally does this overnight on the charger, and this does it now. The second command should show `status=speed`. It helps Kestrel's own start-up and menus, not Firefox's engine or page loading, so expect a modest gain. Repeat it after every update. Undo with `adb shell cmd package compile --reset app.kestrel.browser.gecko`.
>
> **3. Stop a false "no internet" on the hotspot**
> Kestrel treats any Wi-Fi that Android hasn't "validated" as no-internet. That changes the error-page wording and, per `docs/ARCHITECTURE.md`, the auto-retry when the link returns. A carrier hotspot can fail Android's Google check while working fine. Pages load either way.
>
> Check first:
> ```
> adb shell "dumpsys connectivity | grep -o 'Transports: WIFI Capabilities: [A-Z_&]*'"
> ```
> If the Wi-Fi line has no `VALIDATED`, try:
> ```
> adb shell settings put global captive_portal_mode 0
> adb shell svc wifi disable
> adb shell svc wifi enable
> ```
> Undo with `captive_portal_mode 1`. As a side benefit, Android stops pinging Google to test the link. The cost is no sign-in pop-up on hotel-style Wi-Fi, and no Android warning when the phone really has no data.
>
> **4. The permissions that exist**
> ```
> adb shell cmd appops set app.kestrel.browser.gecko REQUEST_INSTALL_PACKAGES allow
> adb shell cmd devi
> [... 4224 characters omitted ...]
> latform-tools" folder, and I'll adjust the commands.
>
> I wrote nothing to the repo; I only read files. Its own hooks still copy chat into `INBOX.md` and `OUTBOX.md` (`INBOX.md` already has your message), and I can't turn that off from here.
>
> **Still open, left untouched:**
> - AJ1d, the updater's storage-maths fix: the last session was cut off mid-change.
> - AJ1b, this answer: not ticked in `TASKS.md` because you said to write nothing. Tell me when to record it.
> - AJ1c, optional: a hidden "Debug pages from a computer (USB)" switch so desktop Firefox can show Kestrel G's console and network for the Doraby/Cloudflare problem. Say yes if you want it.
> - Waiting on you: the version shown in Menu > Diagnostics, the exact Cloudflare-site message, and which earlier prompts felt unanswered.
>
> Want this as a one-page cheat sheet to keep next to the terminal? I can publish it as a page, not in the repo.
<!-- sha:ff5510698cba -->

## 2026-11-13T18:32:00Z
> Kestrel G alpha31 is out. I ran it on the emulator, not on your tablet. The Doraby/vidsrc fix was checked against a test site I built to behave like that player, not on doraby.com itself.
>
> https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.3.0-alpha31
>
> **1. adb permissions to give it now** (tested on the emulator; the package is `app.kestrel.browser.gecko`):
> ```
> adb shell cmd appops set app.kestrel.browser.gecko REQUEST_INSTALL_PACKAGES allow
> adb shell cmd appops set app.kestrel.browser.gecko RUN_IN_BACKGROUND allow
> adb shell cmd appops set app.kestrel.browser.gecko RUN_ANY_IN_BACKGROUND allow
> adb shell cmd deviceidle whitelist +app.kestrel.browser.gecko
> ```
> These help updates and keep downloads going with the screen off. None of them makes pages load faster, because the browser asks Android for nothing that affects speed.
>
> **2. Test videos and Video lab:** removed. The update file is 93.4 MB instead of 99.0 MB, about 5.3 MB smaller. The Tap lab, the Decode lab and the one-line decoder readout stay; say if you want those gone too.
>
> **3. Home screen in the tab list:** fixed. A tab on the Home screen now shows as "Home" with a picture of the Home screen, not the last site. I checked this on the emulator with a screenshot.
>
> **4. Videos from doraby.com's vidsrc server:**
> - **Cause:** the video server wants the headers of the player the movie sits in. Kestrel sent the movie page's headers, so the server said no.
> - **What changed:**
>   - Kestrel now repeats the player's own headers and tries other header combinations before giving up.
>   - It finds playlists that sites hide under names like `.txt`.
>   - It handles video parts disguised as pictures.
>   - When a site refuses, it tells you why, with the HTTP number.
> - **Test:** on my test site, with the player on a different address than the movie page, an encrypted stream with disguised playlists, and a stream server that refused any other player, Kestrel found it and saved a valid 12-second MP4 with picture and sound.
> - **Still not possible:** players that keep the stream out of the network, copy-protected (DRM) video, and live streams.
> - **Try it:** open the movie, press play, wait a few seconds, then tap the download icon. If it still fails, send me the Diagnostics lines that mention "media" (Menu > Diagnostics > Copy); they say exactly why.
>
> **Also in alpha31:** the update-space numbers are fixed. Android only keeps about 0.5 GB free after an install, so your update needs about 1 GB, not 3 GB, and your 1.6 GB free should be enough.
>
> **Still open from you:**
> - Does the vidsrc movie save now?
> - The "App:" version line in Menu > Diagnostics.
> - The exact words of the "incompatible browser / Cloudflare" site, plus a Page report from a Doraby watch page after you press play.
> - Which earlier prompts felt unanswered.
> - The choppy-video site or link.
> - How scrolling and Back feel.
> - Proton Mail numbers, and the keyboard test.
> - Yes or no on the scheduled GitHub ad-list build.
> - Your private signing key.
> - Optional: the hidden USB debug switch, the licence, landscape tabs, and a Cromite comparison.
<!-- sha:57c2ddc0d012 -->

## 2026-11-13T18:32:17Z
> Standing down on the 2-hourly heartbeat. Its alpha22-era work list is long finished, and alpha31 shipped about 15 minutes ago. The owner wrote today at 17:44, so I'm leaving the trigger in place. I'm waiting for the owner's report on whether alpha31 saves the vidsrc movie and for the Diagnostics "media" lines if it doesn't.
<!-- sha:b664c2e9ed81 -->

## 2026-11-13T21:58:02Z
> Kestrel G alpha32 is built, signed and published. The GitHub build passed and the release page has the APK (92.5 MB, about 0.9 MB smaller than alpha31).
>
> https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.3.0-alpha32
>
> **Your requests (messages 26 and 27)**
> 1. **Video list:** it only lists videos of the page you are on now. When you move to another site the old list is dropped, and the dock icon goes grey on a page with no video.
> 2. **Download pop-ups:** every download (video or file) shows a "Download started" message at the top. A small window in the top right lists all downloads, each with pause or resume, a progress bar and an X. The X on a row cancels and deletes the unfinished file. The X on the window closes it without stopping anything.
> 3. **Download manager:** the bar is about 4 times taller, with MB done, MB left, percent and speed written inside it. The bar is transparent and fills with neon blue, and the text changes colour where the blue crosses it so it stays readable.
> 4. **Cancel:** cancelling any download now deletes the partly downloaded file, whether it started from the manager, the new window or the notification.
> 5. **Switching servers:** when the site swaps the player for another server's, the old download choices are removed and the new server's appear. An ad that plays first and then the real video is not mistaken for a server switch.
>    - **Limit:** a site that swaps servers inside the same page, without loading another player, looks the same as an ad followed by the real video. Kestrel can't tell those apart yet.
> 6. **Names:** when the page says what a video is, the file name offered is like "Ozark S02E05".
> 7. **Diagnosis tools:** I removed the ones whose questions are already answered. The list is in `research/DIAG_TOOLS.md`.
>    - **Removed:** the tap lab, decode lab, scroll test and "compare 3 feels", the blocker mode switch, the neon pulse test, and the GPU, video-decoder, keyboard and warm-up lines.
>    - **Kept:** the Diagnostics screen itself and the lines for questions that are still open.
>
> **From the review report (message 28)**
> - **Link expired:** for videos that come as a playlist of parts (most streaming sites), a refused part is now tried again three times, and then with other Referer/Origin headers, before the download says "expired". The other parts wait while one is retried.
> - **Video list:** a page that loads many previews can no longer push the real video out of the list.
> - **Start-up timing:** Menu > Diagnostics now shows a copyable "Cold start" line with milliseconds for each stage.
> - **Not yet in alpha32:** pause and resume for files from servers that send no change marker, and a check that stops a cut-off partial file from being continued. They are written and tested but not shipped.
> - **In progress:** your last ask, showing the last page instantly when you reopen the browser. The picture-saving and display parts are written; I'm wiring them in now.
>
> **Not tested on the tablet:** all of the above was checked on the emulator and with 940 JVM tests plus 133 extension tests. The "link expired" retry was tested only against test servers, not a real link that expires.
>
> **Open items for you**
> 1. After a fresh start (swipe the browser away, then open it), tap Menu > Diagnostics > Copy and paste the report. The "Cold start" line is the number I need to judge the instant-last-page work.
> 2. Menu > Diagnostics > "App:" line, to confirm alpha32 installed.
> 3. The exact words of the "incompatible browser / Cloudflare" site, and a page report from doraby.com.
> 4. The choppy-video link, how scrolling and Back feel, and the Proton Mail numbers and keyboard test.
> 5. Your decision on a scheduled GitHub ad-list build (yes or no), and the private signing key.
> 6. The network lab results and the optional items (licence, landscape tabs, Cromite comparison).
>
> Now continuing with the last-page feature.
<!-- sha:70a78e79e274 -->

## 2026-11-13T23:37:05Z
> Storage tidy (AO4) and the video size estimates (AO2) are done and tested. The CPU-by-process Diagnostics line (AO1) is built, but its on-screen check isn't finished. That emulator check is waiting on the rebuilt app to install, and the full test suite plus lint are running in the background. When they finish I'll check the CPU line on the emulator, tick AO1, and ship alpha33.
<!-- sha:3abec246d40f -->

## 2026-11-13T23:37:23Z
> Both jobs are still running: the emulator CPU check and the full test suite with lint. I'll pick up as soon as either reports back.
<!-- sha:986a213631e9 -->

## 2026-11-13T23:42:09Z
> Home-restore fix status: the session file now records "Home was showing", and a restored Home tab loads nothing behind it. It compiles and its session tests pass. I'm building it into the emulator now to prove the open page → Home → close → reopen sequence, then I'll re-run the full suite and ship alpha33.
<!-- sha:e98976976859 -->

## 2026-11-13T23:42:41Z
> Waiting on the install and the emulator run. Nothing else is running that could disturb it.
<!-- sha:ee9deda89b8e -->

## 2026-11-13T23:57:58Z
> The Home-restore fix and the two new features (blocked-page icon, top-right panel) are built and unit-tested. I'm now waiting on the emulator Home-restore check to finish, then I'll run the visual check of the icon and panel.
<!-- sha:9417c957f59c -->

## 2026-11-14T00:02:42Z
> Waiting for the emulator run to reach the panel steps; I'll read its screenshots when it finishes.
<!-- sha:76ba27628259 -->

## 2026-11-14T00:09:28Z
> Waiting for the last steps of the emulator script (the pop-up icon part); then I'll debug the panel toggle.
<!-- sha:9a2e76bbda7e -->

## 2026-11-14T00:13:04Z
> Waiting for the restarted app to finish loading its page; then I'll retest the panel toggle with screenshots taken inside the device.
<!-- sha:4261e1513eb4 -->

## 2026-11-14T00:18:16Z
> Waiting for the lab page to finish loading on the emulator; then I'll press its fake play button and tap the new blocked-page icon within its one second.
<!-- sha:2465a4bc5e7b -->

## 2026-11-14T00:20:39Z
> Waiting on the install and the icon check; I'll look at its screenshots when it finishes.
<!-- sha:f171a37c08ac -->

## 2026-11-14T00:21:08Z
> Waiting for the icon check to finish; nothing else is running on the emulator.
<!-- sha:e3bf565c626f -->

## 2026-11-14T00:24:44Z
> The icon check is rerunning with the app already warm, because the first run came right after a cold start, when Shields is not yet active on the page. Waiting for it to finish.
<!-- sha:77f6538cc7a0 -->

## 2026-11-14T00:27:42Z
> Status: the blocked-page icon and the top-right panel are both proven on the emulator and ticked. I'm doing one last emulator check that the new Diagnostics lines show on screen, then I'll run the full test suite and ship alpha33.
<!-- sha:b8c2b206b2a0 -->

## 2026-11-14T00:33:03Z
> **It would only help storage. It would not make the browser faster.**
>
> - **Speed:** Pages would load and scroll exactly the same.
>   - **The change:** Android currently keeps two copies of the browser engine's biggest files on your tablet, one packed and one unpacked. The change would drop the second copy, and the engine itself would be identical.
>   - **The gain:** about 73 MB freed, roughly 4% of your 1.6 GB free.
>   - **The cost:** each update download would be about 65 MB bigger, a couple of extra minutes on your hotspot.
>   - **Start-up:** the one thing I can't predict is how fast the browser opens right after an update. It could be slightly faster or slower.
>   - **Other options:** I found nothing that would make the engine itself faster on your tablet. Mozilla's own version is already tuned for it, and one I built would run slower. The remaining speed work is in settings and memory handling, which the other session is already doing.
> - **Safety:** your bookmarks, history and settings aren't touched by this. The risk is that Kestrel G fails to start on your tablet, and you'd then reinstall the previous version. Mozilla uses this method for Firefox on most phones, as far as I can tell from their bug tracker. Your tablet is unusual, though (32-bit, Android 10 Go).
> - **Confidence:** no, not yet. I think it will probably work, but I haven't tested it anywhere. I can't test it here, because there's no tablet or emulator in this sandbox. The way to find out is to test it on a computer-simulated tablet first, and only then on yours.
>
> **Nothing has been changed.** I checked both versions of the code, and the setting is untouched. The other session merged my notes and filed them as a finished question plus one waiting to-do, "AS1". That item says it won't be tried on your tablet until you say yes. It also marked my message copies as automatic, so there's nothing for you to clean up.
>
> My advice is to skip it unless running out of storage becomes a problem again. If you do want it, the next step is to have the other session test it on the simulated tablet first. Do you want to skip it, or have it tested there first?
<!-- sha:971e8f5b0b20 -->
## 2026-11-13T23:49:29Z
> **Yes, it's possible, but the useful part isn't rebuilding Gecko.** Mozilla's build for your tablet's chip is already profile-guided and link-time optimized. A home build would be slower and would trim only a few percent. The real win is in how the engine is packaged: about 35% less installed storage, with no engine changes.
>
> I checked this in the real engine package and Mozilla's source. Nothing ran on your tablet or an emulator, because this sandbox has neither. The size numbers are computed from the real package. The speed and RAM effects are estimates.
>
> - **Rebuilding Gecko: not worth it.** The shipped build already uses profile-guided and link-time optimization, `-O2` and the "lite" variant. To match it you'd need an instrumented build plus a training run. That can't run on GitHub's free builders or here. It would need a rented 16–32-core machine and a redo every Firefox release.
> - **Features removed in a rebuild:** I estimate 5–10 MB off the 118 MB engine library.
> - **Stored libraries: the real saving.**
>   - The app currently unpacks the engine's libraries on install, which costs 92.5 MB for the APK plus 138 MB unpacked. Keeping them uncompressed inside the APK takes the installed size from 230 to 158 MB, about 73 MB less.
>   - The catch is a bigger update download, +65 MB to about 158 MB.
>   - The repo's note "GeckoView needs the libraries on disk" has no source and looks wrong. GeckoView has supported in-APK libraries since version 96. Mozilla's bug tracker shows Firefox for Android stopped forcing extraction in version 143.
>   - It's a one-line change at `geckoapp/build.gradle.kts:68`. It needs a test on an emulator first, then the tablet, covering start-up time and an in-place update.
> - **Trimming `omni.ja`:** the repo already researched it. It saves about 9 MB more, so about 149 MB installed in total.
> - **The `:crashhelper` process:** it looks removable but isn't. Gecko 160 starts it unconditionally and child processes wait for it, so switching it off would probably hang the engine.
> - **Settings:** they're already near their ceiling. The open question is what the GPU and utility processes cost in RAM. I'd measure that on the tablet first.
>
> I'd do it in this order: per-process memory in Diagnostics, then the stored-libraries test, then the `omni.ja` trim. A custom build comes last or never.
>
> **Heads-up about the other session.** I wrote nothing to the repo, but its hooks copied your message and my helpers' reports into `INBOX.md` on my branch `claude/dazzling-bohr-qjnxhe` and pushed them, about 480 lines. This reply will add `OUTBOX.md`. The other session merges any branch idle for 20+ minutes, so those lines could show up in its INBOX and look like a request it should act on.
>
> To avoid that, delete my branch on GitHub after this reply, or tell the other session to ignore INBOX entries from about 23:23 UTC today. I can't delete it myself, because the hooks would push it back.
>
> The full report with sources is at `/tmp/claude-0/-home-user-Browser/3ffc23c1-bccc-579d-b684-db627ad5b4a9/scratchpad/GECKO_ENGINE_OPTIMIZATION.md`. If you want, I can turn it into a private page you can paste to the other session, or commit it to `research/` once that's safe.
<!-- sha:532d7f6a075d -->

## 2026-11-14T00:28:06Z
> Status: the blocked-page icon and the top-right panel are both proven on the emulator and ticked. I'm doing one last emulator check that the new Diagnostics lines show on screen, then I'll run the full test suite and ship alpha33.
<!-- sha:b8c2b206b2a0 -->

## 2026-11-14T00:29:22Z
> Waiting for the Diagnostics screen check to finish on the emulator; then the full test run and the release.
<!-- sha:5da169992d6b -->

## 2026-11-14T00:31:13Z
> Waiting for the clean-start Diagnostics check; the emulator is the only thing running.
<!-- sha:5229903bd36e -->

## 2026-11-14T00:43:37Z
> **It would only help storage. It would not make the browser faster.**
>
> - **Speed:** Pages would load and scroll exactly the same.
>   - **The change:** Android currently keeps two copies of the browser engine's biggest files on your tablet, one packed and one unpacked. The change would drop the second copy, and the engine itself would be identical.
>   - **The gain:** about 73 MB freed, roughly 4% of your 1.6 GB free.
>   - **The cost:** each update download would be about 65 MB bigger, a couple of extra minutes on your hotspot.
>   - **Start-up:** the one thing I can't predict is how fast the browser opens right after an update. It could be slightly faster or slower.
>   - **Other options:** I found nothing that would make the engine itself faster on your tablet. Mozilla's own version is already tuned for it, and one I built would run slower. The remaining speed work is in settings and memory handling, which the other session is already doing.
> - **Safety:** your bookmarks, history and settings aren't touched by this. The risk is that Kestrel G fails to start on your tablet, and you'd then reinstall the previous version. Mozilla uses this method for Firefox on most phones, as far as I can tell from their bug tracker. Your tablet is unusual, though (32-bit, Android 10 Go).
> - **Confidence:** no, not yet. I think it will probably work, but I haven't tested it anywhere. I can't test it here, because there's no tablet or emulator in this sandbox. The way to find out is to test it on a computer-simulated tablet first, and only then on yours.
>
> **Nothing has been changed.** I checked both versions of the code, and the setting is untouched. The other session merged my notes and filed them as a finished question plus one waiting to-do, "AS1". That item says it won't be tried on your tablet until you say yes. It also marked my message copies as automatic, so there's nothing for you to clean up.
>
> My advice is to skip it unless running out of storage becomes a problem again. If you do want it, the next step is to have the other session test it on the simulated tablet first. Do you want to skip it, or have it tested there first?
<!-- sha:971e8f5b0b20 -->

## 2026-11-14T00:43:40Z
> The earlier answer only covered what the engine itself can do. Before I list unconventional ideas, I'm checking the repo and the engine package, so I don't suggest something that already exists or can't work.
<!-- sha:b31c3d72f7c8 -->
