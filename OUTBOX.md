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
