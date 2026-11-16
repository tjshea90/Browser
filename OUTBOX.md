# OUTBOX - what Claude told the owner (the last reply of every turn), newest last. Written by tools/capture_outbox.sh on the Stop hook; key-shaped strings are redacted and long replies cut in the middle. The mirror of INBOX.md. (older entries: git history)

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

## 2026-11-14T00:46:29Z
> Alpha33 is built and tested here, and its GitHub build is still running (the first checks passed). Test results:
> - Unit tests: 819 in `core`, 223 in `app`.
> - Lint and the Gecko compile: clean.
> - Page-script tests: 150 of 150 pass.
>
> When the run is green I'll record the release and send you the link and the full answer to your last few messages.
<!-- sha:234f9ace80d6 -->

## 2026-11-14T00:52:50Z
> Kestrel alpha33 is built, signed and published. The GitHub build passed all its checks, and the signing certificate on this exact APK is the one your tablet already trusts, so it installs over alpha32. The APK is 92.6 MB, about the same as alpha32. In the browser, Menu > Updates should offer it, or open this link on the tablet:
>
> https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.3.0-alpha33
>
> Everything below was checked on the emulator and with automated tests only. Nothing has run on your tablet yet.
>
> **Your requests (messages 29 to 33)**
>
> 1. **Opens on Home when Home was last (31):**
>    - **Cause:** the saved session remembered the page's web address but not that Home was in front, so the page loaded again.
>    - **Fix:** it now saves "Home was showing", and Home comes back with nothing loading behind it. Back or the tab list still takes you to the page.
>    - **Emulator:** I opened a page, pressed Home, closed and reopened, and got Home with no page load.
> 2. **"I accidentally pressed stop" (31):** I carried on from where it stopped. Everything had been saved, so nothing was lost.
> 3. **Blocked-page icon (32):**
>    - **What it does:** it replaces the bottom bar. A very small, quiet icon appears at the bottom middle and is gone one second after the latest block. Pressing it opens the blocked page in a new tab, with no question.
>    - **Forced redirects:** if a page tried to send the tab you are on somewhere else, that address opens in a new tab too, and your page stays put.
>    - **Emulator:** the icon showed and a press opened a new tab (3 tabs became 4). I stretched the one second for that test only, so the real one-second timing is untested.
> 4. **One-hand panel (32):**
>    - **Toggle:** a small tab sits at the top right. Pressing it slides a black panel down with Back, Home and Forward, and an address bar under them.
>    - **Address bar and keyboard:** pressing the bar selects the whole address and opens a small keyboard under the panel at the top right. The app draws that keyboard itself, because Android won't let an app move or shrink the normal one.
>    - **Keys:** letters, a 123 page with the address symbols, a .com key, shift, backspace (hold to repeat) and Go. Go opens the address, or searches Google like the normal bar.
>    - **Animation and feedback:** every button, the bar and each key lights neon blue for a split second. The slide uses its own timing, so it works with Android animations off.
>    - **Settings:** Settings > Appearance turns the tab off.
>    - **Limit:** the Kestrel Light build has no panel.
> 5. **Smoothness (29, item 1):**
>    - **Hardware is fine:** your tablet's hardware decoders and graphics were measured earlier and are fine. H.264, VP9 and H.265 decode in hardware with 0 of 150 pictures dropped. The screen draws about 65 frames a second.
>    - **Likely cause:** lag on real sites most likely comes from the page's own scripts. They run on one thread of a processor that is 3 to 5 times slower than a recent phone. The 5
> [... 4553 characters omitted ...]
> iagnostics > Copy and paste it. It will show the "App:" line (to confirm alpha33), the "Cold start" timings and the three new lines.
> 2. The exact words of the "incompatible browser / Cloudflare" site, a page report from a Doraby watch page after you press play, and whether the vidsrc movie saves now.
> 3. The choppy-video link, how scrolling and Back feel, and the Proton Mail numbers and keyboard test. For a laggy movie site, say whether Settings > Video & page animation > Calm pages helps.
> 4. Yes or no on the scheduled GitHub ad-list build, and your private signing key.
> 5. The network lab results, and the optional items: licence, landscape tabs, and the Cromite comparison.
> 6. New: your yes or no on the stored libraries, and how the one-second icon and the mini panel feel on the tablet.
>
> Next I'll add a memory-per-process line to Diagnostics. Then I'll wait for what you find on the tablet.
<!-- sha:f23ef84c6210 -->
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

## 2026-11-14T00:46:21Z
> There's no hidden switch that makes the engine itself faster. But there are a few ideas outside the engine that you could actually feel, listed best first. I haven't tested any of them on your tablet.
>
> 1. **Free up space on the tablet.** It's about 94% full. Cheap flash storage slows down as it fills, because it has to shuffle data around before it can write. That affects app start-up and the page cache. The mechanism is well known, but I couldn't find an exact threshold, and I haven't measured your tablet. It costs nothing to try: move movies and downloads off, and aim for a few GB more free.
> 2. **Turn off pre-installed apps you never use.** Only about 0.5 GB of the 2 GB is free, and the system and its apps use the rest. With your cable this is reversible. More free memory means fewer reloaded tabs and a smoother browser. I haven't seen what uses the memory, so I can't promise how much. A report from the cable would show it, and a session could tell you which apps are safe to turn off.
> 3. **A "no scripts" button per site, plus a Reader view.** A big part of heavy-page slowness is the page's own programs running on four slow cores. The engine you ship already has a per-site switch for that, and it includes the pieces of Firefox's Reader view. Article and news pages could load and scroll much faster. Some sites would break, so it would be your choice per site. It does nothing for video or mail. The project lists it as not built yet.
> 4. **Google results in "Web" mode.** Adding `&udm=14` to the search address drops the AI box and extra panels, and you don't want AI anyway. A blog test says it still worked in October 2026. I found no one who measured a speed gain. Google could remove it at any time, and it hides the image and shopping tabs.
> 5. **Small free things.**
>    - Set the three animation scales in Developer options to 0.5×. Don't set them to Off, because that freezes loading spinners on web pages.
>    - Keep battery saver off.
>    - Try 5 GHz when the phone is close.
>
> The big idea I don't recommend now is letting the phone do the heavy page work and sending the finished page to the tablet. The phone is much faster and has the uncapped connection. It would help heavy sites most, but it's a months-long project and would slow every tap and keystroke.
>
> Items 1, 2 and 5 you can try now. Items 3 and 4 would need the other session. Which would you like to start with?
>
> Sources: [Google "Web" filter guide](https://geotoolbox.ai/blog/how-to-turn-off-ai-overviews), [IONOS on Google without AI](https://www.ionos.com/digitalguide/online-marketing/search-engine-marketing/google-search-without-ai/), [Kioxia on flash garbage collection](https://kioxia.com/content/dam/kioxia/en-us/business/memory/mlc-nand/asset/KIOXIA_Efficient_Garbage_Collection_Operations_Tech_Brief.pdf), [Performance drop without discards](https://embeddedcomputing.com/technology/processing/performance-drop-without-discards)
<!-- sha:52487ae6a506 -->

## 2026-11-14T00:49:26Z
> **Not unless the tablet is plugged in and left alone.** Android runs that background compile only when it is charging and the screen has been off and untouched for a while, usually about an hour. It also only runs about once a day. It doesn't have to be overnight or fully charged, because any long idle charge counts. If the tablet is never plugged in with the screen off for that long, the compile basically never happens. It also stops as soon as you pick the tablet up or unplug it. This is how stock Android 10 works. Your tablet's maker may have changed it, and I haven't checked on the tablet itself.
>
> **Does that matter?** Very little. The compile only speeds up the app's own interface code. It doesn't touch the engine that loads and draws pages, which Mozilla already compiled. The project's own notes call the gain small. So there's no need to change your charging habits for this.
>
> If you ever want it, there are two ways:
> - Charge it with the screen off for an hour or two sometime, day or night.
> - With your cable, one command compiles the app right away. The result is lost every time the app updates.
>
> Want me to give you that command?
>
> Source: [Android's description of the background compile job](https://android.googlesource.com/platform/art/+show/refs/tags/android-security-14.0.0_r24/libartservice/service/README.md) (Android 14 version).
<!-- sha:6eadb43d5499 -->

## 2026-11-14T01:26:45Z
> Kestrel alpha33 is built, signed and published. The GitHub build passed all its checks, and the signing certificate on this exact APK is the one your tablet already trusts, so it installs over alpha32. The APK is 92.6 MB, about the same as alpha32. In the browser, Menu > Updates should offer it, or open this link on the tablet:
>
> https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.3.0-alpha33
>
> Everything below was checked on the emulator and with automated tests only. Nothing has run on your tablet yet.
>
> **Your requests (messages 29 to 33)**
>
> 1. **Opens on Home when Home was last (31):**
>    - **Cause:** the saved session remembered the page's web address but not that Home was in front, so the page loaded again.
>    - **Fix:** it now saves "Home was showing", and Home comes back with nothing loading behind it. Back or the tab list still takes you to the page.
>    - **Emulator:** I opened a page, pressed Home, closed and reopened, and got Home with no page load.
> 2. **"I accidentally pressed stop" (31):** I carried on from where it stopped. Everything had been saved, so nothing was lost.
> 3. **Blocked-page icon (32):**
>    - **What it does:** it replaces the bottom bar. A very small, quiet icon appears at the bottom middle and is gone one second after the latest block. Pressing it opens the blocked page in a new tab, with no question.
>    - **Forced redirects:** if a page tried to send the tab you are on somewhere else, that address opens in a new tab too, and your page stays put.
>    - **Emulator:** the icon showed and a press opened a new tab (3 tabs became 4). I stretched the one second for that test only, so the real one-second timing is untested.
> 4. **One-hand panel (32):**
>    - **Toggle:** a small tab sits at the top right. Pressing it slides a black panel down with Back, Home and Forward, and an address bar under them.
>    - **Address bar and keyboard:** pressing the bar selects the whole address and opens a small keyboard under the panel at the top right. The app draws that keyboard itself, because Android won't let an app move or shrink the normal one.
>    - **Keys:** letters, a 123 page with the address symbols, a .com key, shift, backspace (hold to repeat) and Go. Go opens the address, or searches Google like the normal bar.
>    - **Animation and feedback:** every button, the bar and each key lights neon blue for a split second. The slide uses its own timing, so it works with Android animations off.
>    - **Settings:** Settings > Appearance turns the tab off.
>    - **Limit:** the Kestrel Light build has no panel.
> 5. **Smoothness (29, item 1):**
>    - **Hardware is fine:** your tablet's hardware decoders and graphics were measured earlier and are fine. H.264, VP9 and H.265 decode in hardware with 0 of 150 pictures dropped. The screen draws about 65 frames a second.
>    - **Likely cause:** lag on real sites most likely comes from the page's own scripts. They run on one thread of a processor that is 3 to 5 times slower than a recent phone. The 5
> [... 4553 characters omitted ...]
> iagnostics > Copy and paste it. It will show the "App:" line (to confirm alpha33), the "Cold start" timings and the three new lines.
> 2. The exact words of the "incompatible browser / Cloudflare" site, a page report from a Doraby watch page after you press play, and whether the vidsrc movie saves now.
> 3. The choppy-video link, how scrolling and Back feel, and the Proton Mail numbers and keyboard test. For a laggy movie site, say whether Settings > Video & page animation > Calm pages helps.
> 4. Yes or no on the scheduled GitHub ad-list build, and your private signing key.
> 5. The network lab results, and the optional items: licence, landscape tabs, and the Cromite comparison.
> 6. New: your yes or no on the stored libraries, and how the one-second icon and the mini panel feel on the tablet.
>
> Next I'll add a memory-per-process line to Diagnostics. Then I'll wait for what you find on the tablet.
<!-- sha:f23ef84c6210 -->

## 2026-11-14T03:06:43Z
> Glide typing for the mini keyboard is built and tested, but it isn't on your tablet yet. Alpha34 is still building on GitHub (about 8 minutes). When the build is green I'll record the release and send you the download link.
>
> What I've checked so far, all in simulation or on the emulator, not on your tablet:
> - **Real finger on the emulator:** I injected a real touch path over the keys. Gliding "hello" typed `hello ` with guesses `hello | help | jello`, and "world" after it typed `hello world `. Tapping a guess swapped the word, one Backspace removed the whole word, and a glowing trail showed mid-glide.
> - **Simulated finger:** a typical thumb gets the right word first about 91% of the time and within the three guesses about 97%. A sloppy thumb gets about 81% and 93%. With the previous word known, a typical thumb gets about 97% first. That simulated thumb is my own model, not recorded data from a real thumb.
> - **Tests and lint:** all of them pass, including the page-script tests.
>
> How it behaves:
> - Slide over a word's letters and lift. The word is typed with a space, and the two next-best guesses show above the keys.
> - A plain tap still types a letter.
> - It learns how your thumb lands beside the keys, the words you type that it didn't know, and the names of sites you visit, so a name like doraby can be glided.
> - Settings > Appearance turns it off.
>
> One thing the tablet will settle: on the emulator each glide took 0.7 to 1.7 seconds, but that emulator has no hardware acceleration. My estimate for the tablet is 15 to 30 ms, which I haven't measured.
>
> Questions for you:
> - Spanish: the word list is English only, so a Spanish word will glide into the nearest English word. Do you type Spanish words (pelisjuanita.com is Spanish)? If yes, I'd add a Spanish list.
> - Bigger keyboard: widening the keyboard by a quarter gains about 2 points of first-guess accuracy for a loose finger. Want a "larger mini keyboard" setting?
>
> After a day of use, paste Menu > Diagnostics > Copy. Its new "Glide typing" line shows how often the first guess was kept, swapped or deleted.
<!-- sha:633372788832 -->

## 2026-11-14T03:12:55Z
> Glide typing is out as alpha34, and I've started on your three new requests (invisible corner, a learning keyboard, typing into a page field). Alpha34 built and published on GitHub with all checks green and the signing certificate verified, and it installs over alpha33. The APK is 93.9 MB (1.3 MB more than alpha33, mostly the word list).
>
> https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.3.0-alpha34
>
> **Your message 34 (glide typing)**
> - **How it works:** slide over a word's letters and lift. The word is typed with a space and two other guesses show above the keys. Tapping a guess swaps it in, and one Backspace removes the whole word.
> - **Learning so far:** it learns how your thumb lands beside the keys, the words you type that it didn't know, and the names of sites you visit.
> - **Tested:** I tried it on the emulator with a real multi-point touch path. "hello" then "world" typed correctly, and the swap and Backspace worked. A simulated thumb gets the right word first about 91% of the time and in the top three about 97%.
> - **Not tested on your tablet:** a real thumb and the real speed. The emulator took 0.7 to 1.7 seconds per glide because it has no hardware acceleration; I estimate 15 to 30 ms on the tablet.
>
> Questions from last time:
> - Spanish words: do you want a Spanish word list added?
> - Wider keyboard: do you want a setting for a wider mini keyboard?
>
> **Your message 35 (three new requests)**
> 1. **Invisible corner instead of the button:** I'm building this first. The visible tab is already gone in my working copy. A long press in an invisible 12 mm square at the top right opens the panel, and the panel hangs just below that square. A long press on the same square, which sits above the open panel, slides it closed. Short taps and drags there still go to the page. I read "long press above the open panel" as that same square; tell me if you meant something else.
> 2. **Learning from your choices:** the keyboard will learn from the words you pick when it guesses wrong, from the words you keep, and from the words and word pairs you type. It saves this only on your tablet.
> 3. **Typing into a page field:** when a text field on a website is already open, the mini keyboard will type into that field instead of searching Google. Your pasted Diagnostics (current page `google.com/search?q=hey`) shows the problem you meant.
>
> Your Diagnostics also showed a cold start reaching the engine in about 1 second, which is good.
>
> Still open from you:
> 1. After using the glide, paste Menu > Diagnostics > Copy. It will have a new "Glide typing" line.
> 2. The Spanish and wider-keyboard answers above.
> 3. The "incompatible browser / Cloudflare" site wording, a Doraby page report, the choppy-video link, and how scrolling and Back feel.
> 4. Your yes or no on the scheduled GitHub ad-list build, and your private signing key.
>
> Next I'll finish the corner long press, then the learning, then typing into page fields, and ship them together as alpha35.
<!-- sha:7c096b6c2f60 -->

## 2026-11-14T04:31:22Z
> Kestrel G alpha35 is released with all three things you asked for in message 35. I tested them on the emulator with real touches and a test web page, but **not on your tablet**, so your Gboard and real sites may behave differently.
>
> https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.3.0-alpha35
>
> Update through Menu > Updates, or open the link on the tablet and tap the APK.
>
> **1. No visible button; hold the top right corner**
> - The little tab is gone. Hold your finger on the top right corner of the page for about half a second (an invisible square about 12 mm wide) and the panel slides down. Hold the same corner again and it slides back up.
> - A quick tap or a drag there still goes to the page, so links under that corner still work.
> - On the emulator, a hold opened and closed the panel without touching the link under it.
> - I found one weakness and fixed it: when the screen was badly lagging, a hold could be mistaken for a tap. It now also uses the touch's own timestamps.
> - A quick tap that lands exactly while the page is frozen for about half a second could still toggle the panel. That only showed on the slow emulator.
>
> **2. The keyboard learns from you**
> - It learns words you keep, words you pick from the guess strip (counted three times), and words you type.
> - Your own words and word pairs rise to the top over time. A simulated person with habits went from about 86% right on the first guess to about 95%. That is a simulation, not a measurement of you.
> - It stays on the tablet in a small file and is never sent anywhere. Settings > Privacy & security > "Forget what the keyboard learned" wipes it (it asks first; I tested it).
> - Nothing is learned in private tabs or password fields. A guess you pick and then delete right away is not learned.
> - On the emulator the learned file was written, survived an app restart, and was wiped by the Forget button.
>
> **3. Typing goes into the page's own field**
> - If a web page's text field has the cursor, opening the mini keyboard now types into that field instead of searching Google. The system keyboard goes down, and the bar says "type into the page" with a small "page" tag.
> - It works for letters, glided words, picking a guess, and Backspace. Go presses Enter, so a search box submits; in a multi-line box Go starts a new line and the keyboard stays open. Passwords show as dots.
> - On the emulator, a search form got "hello world", a guess swap and a whole-word delete. Go submitted the form and Google was not searched. Tapping another field while the keyboard was open moved the typing there.
> - With no field open the bar works as before. To type an address while a field has the cursor, tap the page outside the field first, or use the main address bar.
>
> **Not tested on the tablet:** Gboard itself, the tablet's real speed, real sites with their own key handling, rich-text editors (like Proton Mail's message body), and password managers.
>
> **What I need from you:** use it for a day, then paste Menu > Diagnostics > Copy. There are two lines to look at: "Glide typing" (it now says what it learned) and the new "Mini keyboard in page fields".
>
> **Still open from before:**
> - Spanish word list and a larger-keyboard option (your yes or no).
> - The Doraby / "incompatible browser" Cloudflare wording and a page report.
> - The choppy-video link, scroll and Back feel, and the Proton Mail numbers and keyboard test.
> - Scheduled GitHub ad-list build (yes or no) and a private signing key.
> - Network lab results, and the optional items (licence, landscape tabs, Cromite comparison).
> - The stored-libraries decision, and the per-process memory line, which is still unbuilt.
>
> **Also:** one download-timing test failed once while the machine was busy and passed on its own and on GitHub, so it is flaky under load, not a real failure.
<!-- sha:ace49aaa2fbd -->

## 2026-11-14T06:01:29Z
> alpha36 is out. It has everything from your last message: a keyboard you can resize and move, the Gboard and Samsung research, more learning from your corrections, and the polish. I tested it on the emulator with real touches, but not on your tablet.
>
> https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.3.0-alpha36
>
> **Resize and move.**
> - Drag one of the small corner marks on the open keyboard to make it bigger or smaller. The letters grow with it, and glide typing follows the new key positions.
> - Drag the thin bar on top of the panel to move it anywhere on the screen.
> - Let go near the top right corner, or double tap that bar, and it docks back.
> - Size and place are remembered. Settings has a button to reset them.
> - The invisible long press in the top right corner still shows and hides the panel wherever it is.
>
> **Gboard and Samsung research.** Both match your swipe against a word list and pick the likeliest word using the words around it. The biggest published gain comes from learning your own words. Nothing published shows them learning from corrections, and Samsung publishes almost nothing, so I copied what is proven and added correction learning with limits. English only, no Spanish. The full report is in the repo at `research/GLIDE_RESEARCH.md`.
>
> **It now learns more from you.**
> - It remembers "I guessed one word, you meant another" and prefers your word next time the two look alike.
> - It learns your last two words, words you used a moment ago, and the titles of pages you visit and bookmark.
> - A word you delete and then glide again goes to second place instead of coming up first again.
> - Typed noise like "awwww" is never learned.
> - Nothing is learned in private tabs or password fields. Settings > "Forget what the keyboard learned" wipes it all.
>
> **Polish.**
> - While you glide, the strip already shows the word it thinks you mean.
> - The key under your finger shows large above it.
> - Holding a top-row letter types its number.
> - Double tap Shift for caps lock.
> - In a page's text field the bottom row has comma, apostrophe and question mark.
> - A dot, comma or ".com" right after a glided word joins it with no gap.
> - There is a key vibration setting, on by default.
>
> **Honest numbers.** In my simulated test the old learning already got about 95% right on the first guess for a person with set habits, so the new parts add less than one point there. Whether it really improves for you will show in your Diagnostics paste.
>
> **Tested and not tested.**
> - On the emulator with real touches I saw: the mid-glide guess, the big key preview, caps lock, held numbers, the deleted-word rule (it typed "too" with "to" second), and the page-field row.
> - Not tested: your tablet, a real thumb, real speed, and the vibration, which the emulator can't do.
> - Once the emulator stopped redrawing the panel for a while and I couldn't explain it. If the keyboard ever looks frozen but still types, tell me.
>
> **What I need.** After a few days, paste Menu > Diagnostics, the "Glide typing" and "Mini keyboard in page fields" lines.
>
> **Still open from before:**
> - Whether Doraby still shows "incompatible browser" or the Cloudflare message, plus a page report if it does.
> - The link of the choppy video.
> - How scrolling and Back feel.
> - Proton Mail numbers and a keyboard test on it.
> - Yes or no on a scheduled GitHub ad-list build.
> - Yes or no on stored libraries (+65 MB download, 73 MB storage).
> - Decisions on the private signing key, licence, landscape tabs and the Cromite comparison.
> - I have not built the per-process memory line in Diagnostics.
<!-- sha:048c560aeaa7 -->

## 2026-11-14T06:02:16Z
> Both notifications were old wake-ups from my own "Kestrel resume heartbeat" schedule. They fired at 02:17 and 04:17 UTC, while this session was already working. They were stale, because alpha36 was already built and recorded by the time I read them. Nothing needed redoing.
>
> Since the work is finished and nothing is left to resume, I disabled that heartbeat (`trig_01LLoVKsjuTUaDn61UGP1pBN`). Letting it keep waking me every two hours would only use up your usage. It is only disabled, not deleted, so a later session can turn it back on. The checkpoint at `CHECKPOINT.md` says all of this and says to wait for you.
>
> I have nothing more to build until you write. The alpha36 answer above still stands, with the release link and the open items. The next useful thing from you is the Diagnostics paste ("Glide typing" and "Mini keyboard in page fields" lines) after a few days of use on the tablet. If you send a new request, I write it into `TASKS.md` in your words before touching any code.
<!-- sha:059350ade97d -->

## 2026-11-14T16:17:16Z
> Alpha37 is out. It fixes the ad blocking I hadn't finished, but I tested it only on a lab page on the emulator, not on your tablet or any real site.
>
> Install from this page (it installs over the old Kestrel G):
>
> https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.3.0-alpha37
>
> **What changed**
> - **Fixes run before the page does.** Before, a site's fixes arrived after its own scripts had already shown the "turn off your ad blocker" wall, started the countdown or opened the pop-up. On the emulator the fixes took about a second to arrive and the page was already done by then. They now run first, and the lab page shows the effect: wall gone, 8-second countdown down to under 1 second, pop-up listener never ran, ad data removed from the video player's data.
> - **Blocked ad files are answered, not just dropped.** Where the lists say so, a blocked ad script, picture, clip or ad library gets a harmless empty stand-in. A video player that waits for its ad code then starts the video instead of hanging.
> - **More of the lists' fixes work.** I added fixes that cut the ad part out of what video players and feeds receive, the "ad blocker detected" scripts, and links on link-shortener pages (they now point at the real target). Rules for whole families of sites (one name with any ending, like yts.*) work too. The lists' "Quick fixes" list is added and refreshes daily.
> - **One Settings switch, "Fix ad and anti-adblock scripts", turns off everything above.** It is on by default, and it also pauses for any site where you switch Shields off. Its text now says what it does. Menu > Diagnostics > Blocker line shows how many pages ran fixes, how many before the page's own scripts, and how many blocked files got a stand-in.
>
> **What I could not test**
> - I did not try it on your tablet or on real sites such as YouTube. YouTube changes its ads often, and the lists use several tricks there; I run only the ones I understand, so ads may still appear.
> - The first page after the app starts, before the lists have loaded, and the very first visit to a new site can be a little late. A later copy of the fixes covers those.
> - The emulator's Diagnostics screen would not open for me once, so the counters were read twice, but the screen was not exercised further.
>
> **Two questions, yes or no**
> 1. Do you want a separate switch for the video-site fixes? I merged them into the one switch.
> 2. Do you want a scheduled GitHub job that builds the ad lists daily? It would use your GitHub minutes, so I have not built it.
>
> **Still open from you**
> - Diagnostics paste after some use (Glide typing and Mini keyboard in page fields lines).
> - Doraby / "incompatible browser" wording and the page report.
> - The choppy-video link.
> - How scrolling and Back feel.
> - Proton Mail numbers and the keyboard test.
> - Stored libraries yes or no, and the signing key.
> - The network lab results and the optional extras: licence, landscape tabs, Cromite comparison.
> - The per-process memory line is not built yet.
>
> You sent the same words again at 15:04, so I treated it as the same request.
<!-- sha:590cc0ddab8c -->

## 2026-11-14T18:01:02Z
> alpha39 is out. The keyboard now remembers what your glides look like, not just which words you use. I reproduced your "for"/"fir" problem on the emulator and then fixed it there. It is not tried on your tablet or with your real thumb.
>
> **What was wrong.** On the emulator, a glide for "for" whose turn lands on the "i" typed "fir", exactly your complaint. The old learning only remembered "guessed fir, you meant for" as a word pair, with a small capped nudge. With a sloppy hand the wrong word keeps changing (fur, fit, fire, due, fir), so each wrong word would need correcting separately. In my simulated-hand test the old keyboard stayed about 85% right, however often it was corrected.
>
> **What it does now.**
> - It remembers the shape of each glide together with the word you meant. The shape is the path over the keys, measured in key widths, so it doesn't depend on how big you drag the keyboard.
> - It learns from three things: tapping the right word in the strip, deleting the word and typing the right one (or gliding it again), and words you simply keep.
> - The next time you draw that shape, even sloppily, that word comes first. A shape it learned wrongly fades when you correct it.
> - It keeps counting the words you use most, as before. It now also always considers words you have drawn that way before. Previously a sloppy glide could drift so far that the right word never made the shortlist, and that was the main remaining mistake.
>
> **What I measured.**
> - **Emulator, real touches:** the first sloppy glide typed "fir". I tapped "for" in the strip once. The same sloppy glide then typed "for", and still did after restarting the app.
> - **Simulated hand that draws the same shape every time:** "for" is right about 90% of the time after one correction, and nearly every time from the fifth glide.
> - **3000 mixed glides:**
>   - A strong-habit hand went from about 60% to about 80% right.
>   - A hand with no habits went from about 94% to about 98%.
> - **Cost:** decoding time is unchanged.
>
> **Safety.**
> - Nothing is learned in private tabs or password fields.
> - "Forget what the keyboard learned" wipes everything, including the shapes.
> - It all stays on the tablet, size is capped, and old habits fade.
>
> **Export and import.**
> - **Settings > Export what the keyboard learned:** one tap saves `Kestrel keyboard learning <date>.txt` in Downloads. It is plain text with your words, word pairs, corrections, glide shapes and thumb habit.
> - **Import what the keyboard learned:** reads such a file through the file picker and adds it. Importing the same file twice changes nothing.
> - **Format:** written down in `docs/KEYBOARD-EXPORT.md`.
> - **Before you uninstall:** export first, because the keyboard's memory is deleted with the app.
> - **Privacy:** the file lists words you typed, so keep it private.
> - **On the emulator:** I tested export, forget, import through the system picker, and app restart.
>
> **Decision for you: do you want a system keyboard for all apps (yes/no)?** Making this your 
> [... 516 characters omitted ...]
>  Ally crash fix from alpha38.
>
> Release (tap `kestrel-g-0.3.0-alpha39.apk`; it installs over the old one):
> https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.3.0-alpha39
>
> **Still open for you**
> - Yes/no: system keyboard app (new).
> - Diagnostics paste after real use: the "Glide typing" and "Mini keyboard in page fields" lines, plus the EARLIER CRASH line if the app closes again.
> - Doraby / Cloudflare "incompatible browser" page: the wording and a page report.
> - The choppy-video link.
> - How scroll and Back feel.
> - Proton Mail numbers and a keyboard test there.
> - Yes/no: a scheduled GitHub ad-list build. It is not built, because it spends GitHub minutes.
> - Yes/no: a separate video-site ad switch.
> - Yes/no: stored libraries (AS1).
> - Your private signing key (SEC-1).
> - Network lab results.
> - Optional: the licence note (LIC-1), landscape tabs (UI-3), and the Cromite comparison (P2b).
<!-- sha:d535dc644105 -->

## 2026-11-14T18:59:42Z
> The review is finished and the findings file is attached to this chat. It's a read-only read of the code and the earlier research; I ran no builds, tests, emulator or tablet.
>
> I didn't change the repo, claim any `TASKS.md` item or run a checkpoint, so nothing touched the other session's branch. The repo's save hooks may log this chat to `INBOX.md` and `OUTBOX.md`, but only on my own branch.
>
> **File:** `/tmp/claude-0/-home-user-Browser/f5388cff-fd98-5e81-857a-8317253cf149/scratchpad/REVIEW-EXTERNAL-3.md` (64 KB, plain text). Upload it to the other session and tell it to save it as `research/REVIEW-EXTERNAL-3.md`, then work from section 12. It follows the same layout as the earlier outside reviews: each row has file and line evidence, a proof step and a status. Rows are marked new, already queued or already known. Section 12 is a block you can paste straight into `TASKS.md`.
>
> The most important findings:
>
> - **First press plays (ask 6):** the pop-up guard has three weaknesses.
>   - It isn't active on the first page after a cold start.
>   - A web page can fake the "real tap" evidence the redirect guard trusts.
>   - Any page that embeds another site can make the extension click inside that embedded frame.
>   - The current "press through the fake layer" fix only acts after an ad has already been refused, so the first press still fails.
> - **False videos and ads (ask 2):** any video that reports its length is treated as a real video, so ads, hover previews and muted loops get through. Entries found in page markup with unknown size always count towards the Save-video badge. Ad breaks inside saved HLS videos are only removed after downloading, and only when the codec changes.
> - **Download manager (ask 3):** an HLS video download fails after roughly 30 seconds to 2 minutes of dead hotspot upstream while Wi-Fi stays connected. The single-file engine only waits it out when it has one connection. Each row has a test to write first.
> - **Security and blocking (ask 5):**
>   - `trusted-*` scriptlets run from every list, including a single-maintainer host.
>   - AdGuard Tracking and Hagezi Light were recommended by earlier research but never added; the gain is mostly fewer tracker requests, with a small byte saving.
> - **Speed, RAM and hotspot (asks 1, 4, 7):**
>   - The tablet measured the ad blocker at 723 µs per request, against a design target of 50 µs.
>   - The glow animation keeps redrawing while a video plays.
>   - A page-wide mutation watcher has no time limit.
>   - The back/forward cache is fixed at 6 pages even though one kill takes down every tab.
>   - On the capped hotspot, several smoothness settings spend bandwidth on content you may never scroll to.
>
> **Limits:** every `[R]` item is code I read, not behaviour I observed. Three Gecko-specific ideas are marked as unverified in the file (the guard registration option, adjusting the back/forward cache live, the media-session hook), and each names the test that decides it. The tablet numbers I quote are from an earlier build (alpha27), and I didn't review the WebView build.
>
> **Your four yes/no questions (section 11 of the file):**
> - May the extra tracker lists be added with a Settings switch, even though logged-in pages weren't tested?
> - Should pop-ups be stopped more strictly, so a legitimate one sometimes needs a tap on the blocked icon?
> - May the glow pause while a video plays?
> - Should generic in-player ad skipping be on by default, with YouTube left out?
<!-- sha:bbdbc54c3701 -->

## 2026-11-14T19:08:23Z
> Understood: all four are approved, plus YouTube ad skipping and blocking. I'll also add the Back-button request and make the file say exactly what the other session must do and where to save things. I'll start with what the other session changed, since the start-up sync merged its newest checkpoint into my tree, so I can re-verify my line references against the current code.
<!-- sha:23c113a9061a -->

## 2026-11-14T19:24:07Z
> I've rewritten the file as v2, and it replaces the first one. It's still a read-only review: I changed nothing in the repo and didn't touch the other session's branch. I ran nothing on your tablet. I only read code, plus the public uBlock Origin list data, which I copied into a folder next to the repo, not inside it.
>
> **What the file now tells the other session to do:** the top of the file is a "START HERE" block with ten exact steps.
> - It saves the file as `research/REVIEW-EXTERNAL-3.md`, in the repo.
> - It pastes the task list at the end of `TASKS.md`, quoting your words, before any code.
> - It runs the checkpoint command with the exact text to use.
> - It finishes its current long-press panel work (AY1–AY5) and ships alpha40 first.
> - It then does your Back request, then YouTube, then everything else, in a set order.
> - It names which research file each kind of finding goes into.
> - It includes a short, plain-words message for the other session to send you.
>
> All you do is upload the file; there's nothing else to tell it.
>
> **Your decisions are recorded as approved:** the extra tracker lists, the stricter pop-up and redirect wall, pausing the glow during video, ad skipping in video players by default, and YouTube ad blocking and skipping. The YouTube answer also closes the YouTube part of the open licence question (LIC-1).
>
> **New Back-button section** (your request, in your words):
> - **How Back works today:** the engine keeps recent pages in memory, so Back is instant when it can use them. When it can't, the app puts a saved picture over the page until the page first paints. The app never checks that the scroll position came back.
> - **Why Back often reloads:** the engine can't keep pages that were still loading when you left, pages that tell the browser not to store them, or video-player pages. On a slow hotspot the first group is probably common, but that's a guess until it's measured.
> - **What the file asks for:** first, measure why Back reloads. Then remember the scroll position for each history entry. Then keep the picture up until the page is both painted and back at its place, and ask the page to scroll if the engine didn't.
> - **One honest limit:** pages that forbid storing can't be cached, so for those the best possible is the exact saved view instantly with the loaded page landing at the same place.
> - **One question I left open:** whether Back may be made instant for such pages on sites you pick. Doing so keeps a copy of them on the tablet. Nothing gets built until you answer.
>
> **New YouTube section:** I read the live YouTube rules from today's uBlock Origin lists and compared them with what the app can run.
> - **Desktop YouTube:** of 34 rules that apply, the app drops 9. Those include the parts that answer YouTube's "fake buffering" enforcement.
> - **Mobile YouTube (what this tablet gets by default):** of 11 rules, the app drops 1.
> - **What the file asks for:** finish the missing rule types (for trusted lists only), add a built-in ad skipper that works even when the lists lag, and build a test lab. Real YouTube was never reached from here, so the file says plainly that it may still warn or limit playback.
>
> I also corrected one description of how the cold-start picture hides, and updated every code reference to the latest code. The start-up sync had pulled the other session's newest work into my own branch.
>
> The one thing still waiting on you is the Back question above.
<!-- sha:713c03f96a39 -->

## 2026-11-14T19:56:07Z
> Kestrel G alpha40 is out. It has all four of your requests. I tried each one on the emulator with real touches; none has been tried on your tablet yet.
>
> https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.3.0-alpha40
>
> **What changed**
> 1. **No more "This page is slow" prompt.** Kestrel now waits by itself and shows nothing. I tested it with a page that runs a script for 50 seconds. The engine asked after about 30 seconds, Kestrel answered "wait" on its own, and the page finished. Menu > Diagnostics counts how often it waited. If a page is stuck too long, you close its tab.
> 2. **Long press an open spot and the panel appears under your finger.**
>    - It comes up anywhere on a page.
>    - A link still gives its own options, a picture gives the picture options, and text selects the word.
>    - Buttons, fields and clickable boxes are left alone.
>    - The top-right corner is no longer special.
>    - **To close the panel, tap the little bar on top of it.** Settings > Mini navigation panel still switches the whole feature off.
> 3. **The panel floats.** Long press it (buttons, address bar or frame, not the keys) and drag. It stays where you let go, with no jump back to the corner. The keys keep their own long press, so holding a top-row letter still types its digit.
> 4. **Slide on the space key to move the text cursor.** It works in the address bar and in a web page's text field. A quick tap still types a space.
>
> **Found and fixed on the emulator.**
> - A long press on blank space did nothing when a word was already selected on the page.
> - The neon glows could stay frozen after a long press that opened the panel.
>
> **Not tried on your tablet.**
> - How the long-press time (400 ms for picking up the panel) and the slide distance feel under your thumb.
> - A PDF.
> - A page inside a frame that has no address of its own. The panel won't open there.
>
> **Still waiting on you:**
> - Do you want the keyboard as a separate system keyboard app for all your apps? It would start from your exported file. Yes or no.
> - Tell me how the long press and the slide feel on the tablet, and I'll adjust the timings.
<!-- sha:852bd53074ff -->

## 2026-11-14T20:00:34Z
> The handoff save is running in the background. I'll confirm the saved paths once it finishes. The alpha40 answer above stands.
<!-- sha:e2264f7b91c8 -->

## 2026-11-14T20:00:52Z
> The handoff is saved, committed and pushed, so a new session can resume from it. The `generate_handoff_manifest` tool reported no warnings.
>
> - **Latest:** `.handoff/handoff.md` and `.handoff/handoff.json`
> - **Archive:** `.handoff/handoffs/2026-11-14/handoff-2026-11-14T20-00-32-265Z.md` and the matching `.json`
> - **Memory docs:** `CLAUDE.md` and `AGENTS.md`, each with a refreshed "Session Learnings" section
>
> The subagent's first call was rejected by the tool's validation, so nothing was saved then. It resent the call once and that one succeeded.
>
> **What the next session finds**
> - alpha38, alpha39 and alpha40 are released and recorded, and the owner has their answer with the Release link.
> - Still open: the owner's yes/no on a separate system keyboard app (AX1g), and their feedback from the tablet on the long-press feel.
> - One change is in the code but not in alpha40: the stuck-page window for the fallback long-press detector went from 60 to 30 seconds. It ships with the next release, which needs versionCode 142.
>
> A new session starts with `bash tools/resume.sh`, then reads `CHECKPOINT.md` "Do this next".
<!-- sha:cb3448a4dc9d -->

## 2026-11-14T22:01:34Z
> **Kestrel G alpha41 is built and published.** GitHub's tests and builds all passed, and the file is signed with your usual certificate, so it installs over alpha40.
>
> https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.3.0-alpha41
>
> **What changed**
> 1. **Back keeps your place.** Kestrel remembers where you were on each page and puts you back there when you press Back. Pages the engine still has in memory come back instantly, exactly as you left them. Pages it has to reload are shown as a picture of where you were until the real page is at the same spot. Menu > Diagnostics has a new "Back:" line counting how often Back was instant, came from the cache or came from the network.
> 2. **YouTube ad skipping.** Kestrel mutes and fast-forwards ads, presses "Skip", closes banner ads and removes the "ad blockers aren't allowed" box. It is one switch in Settings, "Skip ads inside video players", **on by default**. The lists' full YouTube rules also run now. Diagnostics shows a "YouTube ads" line, so you can see whether it skipped anything.
> 3. **Safer lists.** Only the uBlock lists may rewrite a page's contents. The other lists can't.
> 4. **Tap forging is blocked.** A page can no longer fake your tap or click inside frames it embeds.
> 5. **The pop-up guard is there from the first page.** That includes the first page after a cold start.
>
> **What I tested, and what I didn't**
> - **Emulator (real Firefox engine):** Back returned to the same place with kept, reloaded and re-fetched pages. The pop-up guard was present from the first page. The tap-through into a frame to start its video worked.
> - **Test browsers:** the tap-forging block, the list rewrites and the ad skipper (on a mock player built from YouTube's real page markup). The lists' YouTube rules ran cleanly on the real m.youtube.com and www.youtube.com, and the videos played.
> - **Not run on your tablet at all.** Speed and timings are unknown. The ad skipper has never seen a real YouTube ad, because my test browser gets none when signed out. Its button names are the long-standing public ones, but unconfirmed.
>
> **YouTube screenshot you sent:** the "Video unavailable, not available on your mobile browser" page was YouTube's own refusal. It was a Movies & TV "preview only" listing. Plain Chrome gets the same refusal, so Kestrel wasn't blocking it. If you meant something else by it, tell me.
>
> **Still waiting on you**
> 1. **Back for "no copy" pages (yes/no).** Banks, mail and some shops tell the browser not to keep a copy, so Back has to ask the site again, which is slow on the hotspot. I can make Back instant on sites you pick, but then a copy of those pages stays on the tablet. Do you want that, and for which sites?
> 2. **Separate keyboard app (yes/no).** Do you want the keyboard as a system keyboard app for all your apps?
> 3. **Tablet feedback.** How do the long press and slide from alpha40 feel? Does Back keep your place, and does YouTube skip ads, in alpha41?
>
> **Next:** skipping ads in other video players, then cleaning false videos out of the download list.
<!-- sha:c339a938d337 -->

## 2026-11-14T23:23:52Z
> **Kestrel G alpha42 is built and published.** GitHub's tests and builds all passed. It's signed with your usual certificate, so it installs over alpha41.
>
> https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.3.0-alpha42
>
> **What changed**
> 1. **Ad skipping now works in other video players too**, not only YouTube. It covers JW Player, Video.js, Plyr and the Google ad players that many sites use. It uses the same Settings switch, "Skip ads inside video players". Diagnostics has its own new line, "Other players' ads".
> 2. **The download list stops counting things that aren't videos.**
>    - Hover previews, silent looping banner videos, thumbnail-sized videos and ads no longer count toward the download badge.
>    - They stay in the list under "Other files on this page", and a long press on one still brings it up.
>    - A link to a video file that you can't see playing only counts once Kestrel has asked the site how big the file is. That is one tiny question for up to three links, and only on pages with no confirmed video.
>
> **A fix you should know about:** in alpha41 the Diagnostics line "YouTube ads" could stay at "none seen yet" even when ads were skipped. The numbers were being dropped, mostly on the first page after starting the app. The emulator test found it and alpha42 fixes it. The skipping itself was not affected.
>
> **What I tested, and what I didn't**
> - **Emulator (real Firefox engine):** five test players. Ads from the other players ran about 12 times faster (about 1 second instead of 12) and their skip button was pressed. A test page full of ads and previews ended up with 1 real video counted out of 4. Two links to video files ended up with 1 counted out of 2.
> - **Test browsers:** all the rules and the player class names (I read those from the players' own public source code), plus the full test suites.
> - **Not run on your tablet at all, and no real ad from a real site was available to me.** The new Diagnostics lines will show what actually happens.
>
> **Still waiting on you**
> 1. **Back for "no copy" pages (yes/no).** Banks, mail and some shops tell the browser not to keep a copy, so Back has to ask the site again. I can make Back instant on sites you pick, but then a copy of those pages stays on the tablet. Do you want that, and for which sites?
> 2. **Separate keyboard app (yes/no).** Do you want the keyboard as a system keyboard app for all your apps?
> 3. **Tablet feedback.** How do the long press and slide from alpha40 feel? Does Back keep your place, and do ads get skipped, in alpha41 and alpha42?
>
> **Next:** leaving advertising out of downloaded video streams before it is fetched. The first part is written and tested here, but it isn't in alpha42.
<!-- sha:c703eeb60dc6 -->

## 2026-11-15T00:20:56Z
> **Kestrel G alpha43 is built and published.** GitHub's tests and builds all passed, and it installs over alpha42.
>
> https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.3.0-alpha43
>
> **What changed**
> 1. **The glow holds still while a video with sound plays.** A muted video still lets it pulse, because the engine only reports videos that make sound.
> 2. **Two more tracker block lists.** These are AdGuard Tracking Protection and HaGeZi Light, with a new switch at Settings > Filter lists > Extra tracker lists, **on by default**. The first update downloads about 3 MB and rebuilds the lists, which roughly doubles the rule count (112k to 211k).
> 3. **A page jump to an advertising site is stopped even long after your last tap.** The blocked-page icon shows, and one tap on it opens the site anyway.
> 4. **A new window opened by a button now needs one tap on the small blocked-window icon.** This applies when the button opens a different site. Links you tap, windows on the same site and sign-in or payment windows work as before.
> 5. **Downloads wait out a lost connection instead of failing.** This covers video streams and files that use several connections. The wait starts at 10 seconds, doubles up to a minute, and gives up after 30 minutes with the part already downloaded kept.
> 6. **Video downloads skip advertising breaks.** Breaks that the stream's own list gives away are left out before they are downloaded, and the row says "Left out 0:30 of advertising". Live, copy-protected and clip-sized streams no longer count for the download badge.
>
> **What I tested, and what I didn't**
> - **Emulator (real Firefox engine):**
>   - The glow's busy state followed a real video's play and pause.
>   - A page that jumped to an ad site with no touch for hours was stopped.
>   - The playlist check found 12 seconds of advertising in a test film. A live stream and a 12-second clip did not count (1 of 3 counted).
> - **Real list files:** I compiled them and checked the rule counts and that the safety checks still pass. The extra lists add about 8 MB of working memory while building.
> - **Test suites only, not on the emulator:** the download wait, the new-window rule and leaving advertising out of a download.
> - **Not run on your tablet at all.** No real dropped connection was tried.
>
> **Please watch for this:** the first list update after installing compiles about 211,000 rules on the tablet. If lists stop updating, switch "Extra tracker lists" off and tell me. A page that stops working with the extra lists: turn Shields off for that site and tell me which site.
>
> **Still waiting on you**
> 1. **Back for "no copy" pages (yes/no).** Banks, mail and some shops tell the browser not to keep a copy, so Back has to ask the site again. I can make Back instant on sites you pick, but then a copy of those pages stays on the tablet. Do you want that, and for which sites?
> 2. **Separate keyboard app (yes/no).** Do you want the keyboard as a system keyboard app for all your apps?
> 3. **Tablet feedback on alpha40 to alpha43.** Back keeping your place, ad skipping, the download list, the glow, the new-window icon, and downloads after a connection drop.
>
> **Next:** making one tap play a video when an invisible ad layer sits on top of it. It is coded and tested, and goes in alpha44.
<!-- sha:4ff9b1a7fd63 -->

## 2026-11-15T01:03:40Z
> alpha44 is built, signed and published, and it's recorded in the build log.
>
> Install it over the old one (same signing certificate):
>
> https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.3.0-alpha44
>
> **What changed in alpha44**
> 1. **One tap plays a video even when an invisible ad layer lies over it.** The layer is switched off the moment your finger touches the player. An ad frame hidden over a player is found too.
> 2. **A short clip that plays first and is then replaced by a much longer film on the same player (a "pre-roll") is now treated as an ad.** It no longer counts toward the download badge, and in the download sheet the file that really plays comes first.
> 3. **The helper that hides ad boxes now stops after a short time budget.** A busy page can no longer slow itself down with it.
>
> **What was and wasn't proven**
> - **Emulator:** on the test pages, a tap plays the video under an invisible layer, and a tap on a real link under another layer is not swallowed. A pre-roll page counted 1 of its 2 video files.
> - **Test browser only:** the time budget on the ad-box helper.
> - **Automatic tests:** the web tests (362) and the Kotlin tests all pass, and GitHub's own build and signature check passed.
> - **Not run:** none of this has been on your tablet. I have also not seen a real ad from a real site, so how a real site's invisible layer behaves is still unconfirmed.
>
> **Not in alpha44, queued for the next release:** counters that show how long each of the extension's web-request checks holds the thread. They will appear on the Diagnostics "Blocker" line. I'm continuing with the download-list work on blob-based video players.
>
> **Still open for you**
> 1. **BK5:** may Back be instant on pages that tell the browser "don't keep me" (`Cache-Control: no-store`), on sites you pick? Yes or no.
> 2. **AX1g:** should the keyboard be a separate system keyboard app? Yes or no.
> 3. **Tablet feedback on alpha40–44:** especially Back (scroll place and speed), ad skipping, and the download list.
> 4. **One Diagnostics copy:** after a Proton Mail session, once the counters release is on the tablet.
>
> Next I'm running an independent review of the page-facing ad-layer code, then the blob-player row.
<!-- sha:29d252566b5c -->

## 2026-11-15T02:18:13Z
> alpha45 is built, signed and published, and it's recorded. Install it over the old one; the signing certificate is the same.
>
> https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.3.0-alpha45
>
> **Please update from alpha44.** I had an independent reviewer try to break alpha44's code that runs on every page, and it found real problems, all on test pages (you hadn't reported any):
> - **Ad-layer code:** it could switch off a page's own invisible button or menu lying over a video, for good.
> - **Ad skipper:** in some cases it could take a short film for an ad and speed it up.
>
> alpha45 fixes both:
> 1. **Invisible layers:** Kestrel no longer switches off invisible layers that belong to the page itself. It only switches off an invisible frame from another site lying over a video, and gives it back if it was just fading in. So when a page puts an invisible ad layer over a video, the first tap goes to that layer, as before alpha44. Kestrel refuses the ad it opens and then starts the video itself (as in alpha40 to 43).
> 2. **Links:** a big link to another site that has words on it, like a news card with a title, works normally again. Only picture-only or empty full-screen links to other sites, and empty links lying over a video, count as ad layers.
> 3. **Skipper for JW Player, Video.js and Plyr:** a short film is not taken for an ad, and when the film returns after an ad it is put back at once. The skip button is pressed once, not in turns.
>
> **Also in alpha45:**
> - **Blob players:** many embeds play through a hidden "blob" address. Kestrel now matches the playing video to the right playlist by length, so the film that really plays comes first in the download sheet. A clip under a minute next to a film no longer counts.
> - **Playlists:** DASH playlists are read early too.
> - **Words like trailer, intro, hero, loop:** they push a file down only when it is also much smaller than the biggest file, and never when it played or you pressed it.
> - **Diagnostics:** the Blocker line now shows how often each of the extension's request checks ran and how long it held the thread. That number decides whether I slim them down.
>
> **What was and wasn't proven**
> - **Tested:** the ad-layer, link and skipper fixes were tested in test browsers (all 373 web tests and the Kotlin suites pass), and GitHub's build and signature check passed.
> - **Emulator:** the blob-player matching ran in the real app on the emulator, on my own lab page.
> - **Not run:** none of it has been on your tablet or a real site.
> - **Unproven:** the new numbers in Diagnostics, and DASH matching, are not proven on the emulator.
>
> **YouTube ad skipping:** YouTube's terms forbid it, so YouTube may show a warning or limit playback, more likely when you're signed in. "Skip ads inside video players" in Settings turns it off. This is a moving target, and your reports decide what I fix next.
>
> **The file and screenshot you sent again:** the review file is byte-identical to the first one; the session restarted and delivere
> [... 121 characters omitted ...]
> let or your answers below. The screenshot is the same YouTube page as before ("Stolen from the Suburbs", Preview only). That "not available on your mobile browser" text comes from YouTube, which sends some Movies & TV titles to its app, and this tablet has none. I didn't test that exact title. "Desktop site" in the menu may or may not help, and I can't promise it.
>
> I deleted the scheduled auto-resume, and nothing is half-done.
>
> **Still open for you**
> 1. **BK5:** may Back be instant on pages that tell the browser "don't keep me" (`Cache-Control: no-store`), on sites you pick? Yes or no.
> 2. **AX1g:** should the keyboard be a separate system keyboard app? Yes or no.
> 3. **Tablet feedback on alpha40 to 45:** especially Back (scroll place and speed), ad skipping, and the download list.
> 4. **One Diagnostics copy** after a Proton Mail session, so I can read the new "Extension listeners" numbers.
<!-- sha:01e7e65fcbdf -->

## 2026-11-15T02:18:57Z
> Yes, this is the first time these have reached me. I'm checking whether an earlier mini-panel message was recorded that I missed, then writing all five requests into the task list in your words before touching any code.
<!-- sha:b3fc19885441 -->

## 2026-11-15T03:25:14Z
> alpha46 is out. Install it over the old one; the signing certificate is the same.
>
> https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.3.0-alpha46
>
> **Did I get your earlier mini panel and X message?** No. I searched my record of your messages and the first mention of the closing X is in the message you just sent. It's written down now and done.
>
> **Why the panel wouldn't close:** the empty spot straight above the panel was being counted as part of the panel's top bar. A long press there never reached the page, so it neither closed nor moved the panel. Presses above but off to the side worked, which is why it looked random. I found this on the emulator and fixed it.
>
> **What changed**
> 1. **Close by long press:** a long press on an empty spot above the open panel closes it. A long press beside or below it still moves it under your finger.
> 2. **Close by X:** long press the panel and drag it, and a large X appears at the bottom of the screen. Let go over the X and the panel closes and stays hidden until you long press an empty spot again. I didn't add a Settings switch for this; the existing "Mini navigation panel" switch still turns the whole feature off.
> 3. **Top bar tap:** tapping the little bar on top of the panel is more forgiving of slow taps and a drifting thumb. The Settings text now lists all three ways to close it.
> 4. **Tab left on Home:** it now comes back as the Home screen when you switch back to it.
> 5. **Private box:** it disappears when there are no private tabs, and the regular Tabs page shows. A private tab is still opened from the menu.
> 6. **Start-up picture:** the starting screen now shows the app's own symbol (the shield with the bird from the app drawer) instead of a plain shield. It glows for about a third of a second as the screen comes up, and nothing waits for it. The Home screen shows the same symbol next to "Kestrel". I didn't do spreading wings, just the glow.
>
> **What was and wasn't proven**
> - **Emulator, real touches:** a long press straight above the panel closed it. Carrying it onto the X showed the lit X, and lifting there removed the panel. A long press on an empty spot brought it back. A tap on the top bar closed it.
> - **Emulator, other changes:** the Home tab, Private box and start-up picture also worked there.
> - **Start-up glow:** I only saw it in slow motion on a debug build; at real speed it was not seen.
> - **Slow top-bar tap:** proven only by automatic tests; the emulator tooling is too slow to time it.
> - **Automatic tests:** all pass.
> - **Not run:** none of it has been on your tablet.
>
> **The YouTube screenshot again:** I tried to settle it from here, but YouTube served my test browser a bot-check page, so I couldn't tell whether Kestrel plays a part. Please try "Desktop site" in the menu on that page, and tell me if it plays.
>
> **Still open for you**
> 1. **BK5:** may Back be instant on pages that tell the browser "don't keep me" (`Cache-Control: no-store`), on sites you pick? Yes or no.
> 2. **AX1g:** should the keyboard be a separate system keyboard app? Yes or no.
> 3. **Tablet feedback on alpha40 to 46:** does a long press above the panel close it, the X, the Home tab, the Private box, and the start-up picture?
> 4. **One Diagnostics copy** after a Proton Mail session. Its "Extension listeners" numbers tell me whether to slim down the extension's request checks. Diagnostics also now logs what each long press did to the panel.
> 5. **YouTube:** the "Desktop site" check above.
<!-- sha:fcc3c0968633 -->

## 2026-11-15T03:25:32Z
> Nothing to do. Those two notifications are old firings of the auto-resume I created, which asked me to resume and finish the alpha41 release. I deleted that auto-resume earlier, and a lookup now confirms it no longer exists, so it can't fire again.
>
> The work it was set up for is all done:
> - **Releases:** alpha41 through alpha46 are released and recorded in the build log.
> - **Latest:** alpha46 is out, with the Release link in my previous message.
> - **Open items:** nothing is running or half-done. The only open items are the questions I listed for you (BK5 and AX1g), your tablet feedback, and one Diagnostics copy.
<!-- sha:60aef8d0ce21 -->

## 2026-11-15T04:52:35Z
> I've written the report as one file you can upload to the other Claude account as is. Nothing was run: no build, tests, emulator or tablet. My claims come from reading the code and a few public web pages, and each marks what is unverified and the check that would settle it.
>
> The repo was not changed. I wrote the file outside it, so the auto-save hooks couldn't commit anything. The project's own session-start hook did sync and merge other branches into my session's branch before I began, but I made no edits and touched no other account's branch.
>
> ## What's in the file
> The first section, "START HERE", tells the receiving Claude exactly what to do. It saves the file into the repo as `research/REVIEW-EXTERNAL-4.md`, adds a ready-made task block to `TASKS.md`, makes a checkpoint, and finishes the work already in flight first. The rest is a ranked list of rows, each with evidence, the change, the proof needed and the risk. It follows the same layout earlier outside reviews used, which that Claude already works from.
>
> ## What I found
> The engine settings, scrolling, caching and ad-blocking code are already tuned about as far as settings can take them, so the report doesn't re-tune them. The tablet already measured 64–65 scroll frames a second and 0 dropped frames on hardware-decoded video, with the first screen up 520 ms after launch. The report instead lists:
>
> - **Fullscreen video never rotates the screen.** Nothing in the code listens for a site's "lock landscape" request, so a film fills about a third of the portrait screen.
> - **A fake "Kestrel update" file could install over the app.** The signing key is public, and an APK downloaded in the browser gets only a generic warning.
> - **Downloads no one tapped for are silent** for ordinary file types.
> - **The About page says Safe Browsing doesn't work on this tablet,** while the earlier security research says it does. Nobody has tested it.
> - **Navigation to a site that has ad-fix scripts can wait up to 250 ms** from the second visit on.
> - **The real Firefox engine can't be tested automatically.** The repo is public, so GitHub's free runners could run an accelerated Android emulator, which would close most "not run" gaps.
> - **No way to measure real-site video smoothness.** The test pages were removed at your request, so the report proposes passive counters in Diagnostics.
> - **Safe order for the signing key.** The private key must not be added before the release approval gate, or any session branch could read it.
> - **Smaller items:** engine start timing, a pre-compiled startup profile, a cache-freshness experiment, and what "Shields off for this site" really turns off.
>
> ## One question for you
> Should sound keep playing when you leave the app or the screen turns off? It currently stops. The report flags this as the only open owner question.
>
> Files are in `/tmp/claude-0/-home-user-Browser/d9c0c79e-0ff7-5c2c-9ab9-daefb6ae1d02/scratchpad/`:
> - `REVIEW-EXTERNAL-4.md`
<!-- sha:564051041b6f -->

## 2026-11-15T05:27:54Z
> **alpha47 is out. Tapping the red bar under a video now jumps the video to the place you tapped.**
>
> Release (install over your current Kestrel G):
>
> https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.3.0-alpha47
>
> **What I found**
> - The light line you saw is YouTube's own hover line. YouTube's current bar is built for dragging, so the page most likely ignores a plain tap by design.
> - I could not load a real YouTube watch page here, so I could not prove this on your tablet.
>
> **What changed**
> - If you tap or slide along a video's bar and the page does not move the video within about half a second, Kestrel moves it to where your finger was. If the page jumps the video itself, Kestrel stays out, so there is never a double jump.
> - It leaves live streams, ads, volume sliders and hidden controls alone, so the first tap on hidden controls only shows them. A button or link over the bar is left alone too.
> - A paused video stays paused and a playing one keeps playing.
> - You can turn it off in Settings → "Tap the video bar to jump". It is on by default.
> - This is in Kestrel G only. The light Kestrel is unchanged.
>
> **Buffering from the new place**
> - I tested this in the real engine in the emulator with a 5-minute, 57 MB test film.
> - After the jump the engine made one new request starting at the new place. It kept the first 35 seconds it already had and then loaded forward from the new place. It asked for nothing in between or before.
> - On YouTube itself its own player decides what to fetch. I could not test that.
>
> **How it was tested**
> - All 405 tests pass, 32 of them new. They use real touch taps, including a copy of YouTube's current bar markup.
> - In the emulator's Firefox engine with real touches:
>   - A bar whose page ignores taps jumped to where I touched it.
>   - A page that jumps by itself got exactly one jump.
>   - A copy of YouTube's thin 3 px line also jumped.
> - An independent reviewer found a miss that would have hit YouTube itself: a tap exactly on the thin line did nothing. I fixed that before release.
> - Not tested: a real YouTube page, and your tablet.
>
> **What I need from you**
> Try YouTube. If it still does not jump, open Menu → Diagnostics and send me the line starting "Tap on a video bar". It says whether the page jumped, Kestrel jumped, or a bar Kestrel did not recognise was tapped.
>
> **Still open**
> 1. Back button: may Back show a page instantly even when the site says not to store it (`no-store` pages), on sites you pick? Yes or no.
> 2. Keyboard: do you want a separate system keyboard app? Yes or no.
> 3. Tablet feedback on alpha40–47: the panel closing and its X, the tab that stays on Home, the hidden Private box, and the launch symbol with its glow.
> 4. One Diagnostics copy after a Proton Mail session.
> 5. The YouTube Movies & TV screenshot you sent twice: please try "Desktop site" on that page and tell me what shows.
>
> 📌 **Chat checkpoint: 🔴 Red**
>
> **Goal:** Kestrel browser for the owner's Android 10 Go tablet. This turn: YS1, a tap on a v
> [... 476 characters omitted ...]
> run 43680608062, branch `claude/new-session-7d9rxz`.
> - Files: `seekbar.js`, `research/SEEKBAR.md`, tests `seekbar.test.js` and `seekbar-real.test.js`.
>
> **Done so far:**
> - alpha47 released and recorded in `BUILDLOG.md`.
> - Owner answered.
> - Open items listed above.
>
> **Open / next steps:**
> 1. Wait for the owner's tablet result.
> 2. Read the Diagnostics line with `TASKS.md` YS1d.
> 3. YS1a stays open until then.
>
> **Rules to remember:** Plain words, state what was not tested on the tablet, re-list open items, checkpoint with `tools/ckpt.sh`.
>
> 🔴 **Health:** The chat is long and was compacted once. A fresh session resumes losslessly from `CHECKPOINT.md`, so nothing is lost.
>
> 🔴 I'd move to a fresh chat now. Run `/amhandoff` if you want an extra handoff file; `CHECKPOINT.md` already holds the state.
>
> 💡 The code word is off in this chat. Type `/amcodeword` and I'll start every reply with "Yes Boss!".
<!-- sha:53bdeeadb01a -->

## 2026-11-15T16:00:31Z
> Alpha48 isn't released yet. I'm waiting on three things:
> - **Lab run 5** (20-minute check on the real engine): it ends around 16:15 container time. It will show whether Back leaves full screen and how the unrequested-download questions behave.
> - **The independent code review** of the alpha48 changes: it's still running.
> - **The one-hour hotspot run** (43732896160): it starts right after run 5. The lab's own test downloads and clips are now slowed to hotspot speed as well.
>
> Web tests (418) and the Kotlin unit tests are green. I'll ship alpha48 once run 5 and the review are in, and I'll answer you in plain words then.
<!-- sha:5a3683f0fb9e -->

## 2026-11-15T16:48:47Z
> Status: the last guard tests pass. The lab run for your video controls and long press is queued (the old hour run was cancelled so it doesn't block it). Next I read its screenshots and fix whatever the real Firefox engine shows.
>
> 📌 **Chat checkpoint: 🟡 Yellow**
>
> **Goal:** Kestrel, the owner's light Android browser for a 2 GB Android 10 Go tablet on a 5 Mbit/s hotspot. Current asks (owner messages 43 and 44): full-screen video rotation, an emulator lab with a one-hour soak, a Yahoo Finance video with no controls, and a long press on a video opening download options instead of the floating panel.
>
> **Decisions made:**
> - Kestrel draws its own bar on a tapped video that has no controls (Settings "Video controls": Automatic, Always, Off).
> - A long press on a video opens the Save video sheet, also for players inside shadow roots.
> - All nine review findings (RV1–RV9) are fixed before shipping alpha48 (code 149).
>
> **Changed / rejected:**
> - "Back does not leave full screen" was a lab artifact (Android's "Viewing full screen" box ate the key), not an app bug.
> - Run 109, the old one-hour run, was cancelled.
> - nytimes, espn, twitch and YouTube home were dropped from the soak (they killed the emulated phone).
>
> **Key details:**
> - Branch `ccr-b3c2484c-sbew36`, last ckpt 2399 (commit 4e233ebe).
> - Lab `vc` suite is queued via android.yml app=lab.
> - Heartbeat trigger `trig_01Y8CFHKVb6hmQmEEM78p6gv`.
> - Release note draft is in the scratchpad (`note48.txt`).
> - Release flow: `ship.sh gecko` → dispatch android.yml → `record-release.sh` after green.
>
> **Done so far:** web tests 430 and videobar 13 pass; Kotlin tests pass; HANDOFF and research notes updated.
>
> **Open / next steps:**
> 1. Read the `vc` lab run and fix what it shows.
> 2. Dispatch the hotspot hour run.
> 3. Ship alpha48 and send the Release link.
> 4. Answer the owner in plain words.
> 5. Delete the heartbeat trigger.
>
> **Rules to remember:** ckpt after every step; write owner requests into TASKS before code; no `push:` workflow trigger; no APKs or keys in the repo; ASCII only; say what was not run on the tablet; re-list open items (RX4-V2: keep sound playing when the screen is off?).
>
> ⚠️ **Health:** One compaction so far and many large logs; CHECKPOINT.md in the repo is the authoritative state.
>
> 💡 This chat is getting long. Consider `/amhandoff` at the next good stopping point.
> 💡 The code word is off in this chat. Type `/amcodeword` and I'll start every reply with 'Yes Boss!', so you'll notice if I start losing track.
<!-- sha:fa68c188dfb6 -->

## 2026-11-15T16:51:29Z
> Waiting for the lab run's result.
<!-- sha:755179e26901 -->

## 2026-11-15T16:52:28Z
> Still waiting for the lab run to finish; I'll read it when it does.
<!-- sha:58f3a58fbfa0 -->

## 2026-11-15T16:54:11Z
> Waiting for the video-controls lab run to finish (about 17:00 on the container clock).
<!-- sha:46e986a8cc85 -->

## 2026-11-15T16:59:47Z
> Waiting for the second video-controls lab run (about 17:12 on the container clock).
<!-- sha:128466e1f8e2 -->

## 2026-11-15T17:01:13Z
> Still waiting on the second video-controls run; nothing else is blocking.
<!-- sha:648e0b020076 -->

## 2026-11-15T17:04:17Z
> Waiting for the second video-controls run to finish.
<!-- sha:424131410d96 -->

## 2026-11-15T17:09:07Z
> Waiting on two things: the third video-controls lab run (now on Yahoo's video page) and the local ship gates for alpha48.
<!-- sha:7c260ffa049c -->

## 2026-11-15T17:12:12Z
> The ship gates are re-running after a one-line fix for a rare crash in the Courier server's shutdown code that kept failing the tests. The third lab run is also going.
<!-- sha:d171cba405f7 -->

## 2026-11-15T17:13:14Z
> Waiting for the ship gates and the third lab run; I'll read both as they finish.
<!-- sha:60a58225c5cf -->

## 2026-11-15T17:15:33Z
> Ship gates are green and pushed. I'm waiting for the third lab run before dispatching the release, so a bug it shows can still go into alpha48.
<!-- sha:dbe49cd6e7b3 -->

## 2026-11-15T17:19:52Z
> Waiting for the alpha48 build to finish; the next check is at 17:35 container time.
<!-- sha:1f1c1bf4dd51 -->

## 2026-11-15T17:36:20Z
> alpha48 is released. Your Yahoo problem is fixed and tested on the real Firefox engine, but not on your tablet.
>
> https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.3.0-alpha48
>
> **Your Yahoo Finance video**
> - **Why the long press showed the floating panel:** Yahoo's player is built as a hidden "web component". Kestrel's video code couldn't see inside it, so it thought you pressed on empty space and opened the panel. The same blindness affected the other video features.
> - **Why it had no controls:** in my check of the live page, the player draws almost no buttons while its ad plays. In my emulator run it did show its own buttons while the main video played, so the ad moment is my best explanation of what you saw.
> - **What changed:**
>   - **Controls:** if you tap a video that shows no buttons, Kestrel puts its own bar on it: play/pause, a track to tap or drag with the time, mute, and full screen. It hides itself and stays away where the page has its own buttons.
>   - **Setting:** Settings > "Video controls" has Automatic, Always and Off.
>   - **Long press:** this now opens the "Save video" options (download, copy the address) instead of the panel, including for players like Yahoo's.
> - **What I tested:** on the real Firefox engine in an Android 10 emulator on GitHub, on four test pages and on Yahoo's real video page. On Yahoo, the long press opened "Save video" with no panel, and the tap left the page's own buttons alone. In that run the player was a live stream, so "Save video" said live streams can't be saved.
> - **Not run on your tablet:** the emulator is a pretend tablet with software graphics.
>
> **Also in alpha48**
> - **Full-screen video** turns sideways with the tablet, follows it with Android's rotation lock on, has a turn button at the left edge, and Back leaves full screen first. Android's one-time "Viewing full screen" box takes the first Back press, which is normal.
> - **Downloads and installs:**
>   - A download nobody tapped asks first.
>   - A fake "Kestrel update" file is refused.
>   - The Safe Browsing text is corrected.
> - **Fixes from an independent check of this version:**
>   - A site's own screen lock was misread.
>   - Full screen could stay stuck after its tab was killed or closed.
>   - A web page could have posed as the updater.
>   - Redirecting download links were wrongly treated as started by themselves.
>   - Several questions could pile up.
>
> **The emulator hour:** the lab runs on GitHub. A full one-hour browsing, video and download run with your hotspot speed is going now and ends in about an hour. The shorter runs found that the pretend phone's own system dies under heavy pages. The lab now restarts it and I took the heaviest sites out of the mix. I'll write up what the hour shows and fix what I can.
>
> **Open items**
> 1. **Your tablet test:** play a Yahoo Finance video, long press it, tap a video that shows no buttons, play a film in full screen, turn the tablet, and press the turn button.
> 2. **Question for you:** should sound keep playing when you leave Kestrel or turn the screen off? I won't build that until you answer.
> 3. **Your GitHub steps:** a protected `release` environment, the "pin actions to a full commit" policy, and the private signing key last.
> 4. **Not started:** cold-start speed, startup profile, the 250 ms wait, repeated page checks, and what Shields-off really lifts. Several need your tablet's Diagnostics numbers.
<!-- sha:ac91cb85b1b3 -->

## 2026-11-15T18:08:31Z
> Status: alpha48 is released and the Release link is above. I'm now waiting on two things: the one-hour browsing soak in the lab (it ends around 18:35) and a second independent review of the new video-bar code. I'll fix anything real from either.
<!-- sha:56ffb546efcb -->

## 2026-11-15T18:21:24Z
> Waiting for the Kotlin compile, the hour soak (about 18:36) and the sound run that follows it; I'll read each as it lands.
<!-- sha:6392419da406 -->

## 2026-11-15T18:21:57Z
> Everything is pushed. Waiting for the hour soak (about 18:36) and then the sound run.
<!-- sha:f5c5295f9107 -->

## 2026-11-15T18:34:07Z
> Status: the one-hour soak finished, and its numbers are mostly unusable. The emulator's 5 Mbit/s cap made ordinary pages like Wikipedia and BBC time out. Since you say fast wifi is the normal case, the main lab runs are now fast-network. The soak also flagged three "crashes" that were only helper processes closing normally, which I'm fixing. Next are the sound check, then the big site-by-site compatibility run.
>
> 📌 **Chat checkpoint: 🔴 Red**
>
> **Goal:** Kestrel, the owner's private browser for a 2 GB Android 10 Go tablet, mostly on fast wifi. Current asks (messages 43–45):
> - full-screen rotation (shipped)
> - video controls and long-press options (shipped in alpha48)
> - sound must stop when not on the Kestrel tab (SND1)
> - a polished, compatible, fast daily browser (PD1)
> - fast-wifi emulator runs that log everything, then tune Kestrel from them (LAB2)
>
> **Decisions made:**
> - Lab runs use the runner's fast link by default; hotspot shaping is not used for the main runs.
> - The compat suite loads about 100 sites twice, Shields ON then OFF, from a cold cache.
> - No foreground service for sound (the answer to RX4-V2 is NO).
>
> **Changed / rejected:**
> - The hour soak under 5 Mbit/s shaping is not trustworthy for page speed.
> - Helper-process deaths (`:media`, tab processes) are not app crashes.
> - The earlier "Back does not leave full screen" report was a lab artifact.
>
> **Key details:**
> - Branch `ccr-b3c2484c-sbew36`.
> - alpha48 is released (tag `kestrel-g-v0.3.0-alpha48`, commit 281c48f4).
> - Hour soak run 43740398387 has been read.
> - Sound run 43745675957 is queued.
> - Video-bar review agent `af040def2527a6855` is still running.
> - Heartbeat trigger `trig_01Y8CFHKVb6hmQmEEM78p6gv`.
>
> **Done so far:** the lab harness is written and tested (page-health probe, scroll and memory measures, compat suite, report tables), and the Kotlin compile is green.
>
> **Open / next steps:**
> 1. Fix the lab's helper-process false "crash".
> 2. Read the sound run and fix SND1b if needed.
> 3. Dispatch the 90-minute fast-net compat run, then read it and tune.
> 4. Read the reviewer's result.
> 5. Delete the heartbeat when done.
>
> **Rules to remember:** ckpt after every step; owner requests go into TASKS first; no `push:` workflow trigger; no APKs or keys in the repo; say what was not run on the tablet; sound must not play away from Kestrel; answer in plain words and re-list open items.
>
> ⚠️ **Health:** very long session with one compaction and many large logs; CHECKPOINT.md in the repo is the real state.
>
> 🔴 I'd move to a fresh chat now. Run `/amhandoff` and I'll write a file that brings the new chat fully up to speed.
> 💡 The code word is off in this chat. Type `/amcodeword` and I'll start every reply with 'Yes Boss!', so you'll notice if I start losing track.
<!-- sha:3230fc3d5e8f -->

## 2026-11-15T18:35:34Z
> Waiting for the sound run (about 18:44) and the reviewer's report.
<!-- sha:69a7d11712e5 -->

## 2026-11-15T18:36:56Z
> The second independent review found twelve issues in the video-bar code, three of them major. They need fixing before alpha49. The worst is that a long press on a link sitting over a decorative video could wrongly open the Save sheet. I'm reading the files and fixing them now.
<!-- sha:da874f23e39c -->

## 2026-11-15T19:23:09Z
> alpha49 is ready for your tablet:
>
> https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.3.0-alpha49
>
> I measured your three screenshots, and all three problems come from the same two causes.
>
> **1. Controls under the navigation bar.**
> - On your tablet both system bars stay visible in full screen. alpha48 laid the page out edge to edge under them, so the player's bottom controls, and Kestrel's own bar, sat behind the navigation bar.
> - Now the page is laid out inside any bar the tablet keeps. Where a system really hides its bars, the picture is still full-bleed.
> - Menu > Diagnostics has a new line, "Full-screen system bars", that says what your tablet does.
>
> **2. Laggy, glitchy turning.**
> - I did what you suggested. While the screen turns under a full-screen or playing video, the video pauses and the page is covered with black until the screen has stopped changing (about a second). Then it plays again by itself.
> - A video you had paused stays paused, and so does one you touch during the turn.
> - The engine only lets me pause videos that have sound. A muted video is only covered, not paused.
> - Your third screenshot showed a stale layout with a grey strip after turning back. I don't yet know whether that is the page or the engine. The emulator run described below checks the live Yahoo player for it.
>
> **3. No controls in landscape.**
> - Part of it was the navigation bar covering them, which is fixed above.
> - Kestrel's own bar was also never drawn when the full-screen player is a web component like Yahoo's (a bug the independent check found). That is fixed. Tap the film in full screen and the bar appears if the page has none of its own.
>
> **Sound.**
> - In the emulator the sound already stops when you leave the app by Home or screen-off. The clip's own clock stood still and the engine reported it paused. My first lab verdict of "sound kept playing" was a bad measurement, and I corrected it.
> - I found one real gap by reading the code: Kestrel's own Home screen over a playing page left the page playing underneath. The app now pauses it, and also pauses any video left playing on another tab.
>
> **Also fixed**, from a second independent check of the video bar:
> - A link, button or picture over a decorative video no longer opens the video menu or the bar.
> - A long press released a moment late no longer brings the bar up behind the video menu.
> - Two ad frames of the same size no longer confuse it.
> - A web page can no longer switch the bar's menus off or cancel your taps.
> - The page's own controls are now found on pages with very many links.
>
> **Not run on your tablet.** Everything above ran in headless browsers and guard tests only. The emulator runs of these exact full-screen cases are still queued. They include a switch that makes the emulator keep its bars the way your tablet does, plus the live Yahoo player turned both ways.
>
> **Still running, not finished:**
> - The emulator run of the full-screen cases above, queued behind the big compatibility run.
> - The compatibility run on about a hundred sites at fast-wifi speed, due around 20:10 UTC.
> - An independent code check of the new code. If any of these finds something real I will ship alpha50.
>
> **Open for you:**
> - Please try alpha49: the Yahoo Finance video in full screen, turned both ways. If a bar still covers anything, send me the Diagnostics lines "Full-screen system bars" and "Turning under a video".
> - Owner-only GitHub steps still waiting: the protected `release` environment, the "pin actions to a full commit SHA" policy, and the private signing key last.
<!-- sha:5705f1b86c79 -->

## 2026-11-15T20:43:57Z
> alpha50 is out and replaces alpha49. Please install this one:
>
> https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.3.0-alpha50
>
> **Why alpha50.** An independent code check of alpha49 found two real problems, both fixed:
> - The "Full-screen system bars" line in Diagnostics could say "hidden" while both bars were showing on Android 10. That is the line I asked you to send me.
> - A tab that had been closed or put to sleep could still make a turn of the screen cover the page for nothing.
> - Turning the tablet a second time during a turn was ignored once the video had been paused. That is also fixed.
> - The code that handles turning now has twelve unit tests for exactly these cases.
>
> **Not proven yet.**
> - The emulator run of your full-screen and turning cases proved nothing. That emulator booted with a 16 MB memory limit for apps, so Kestrel kept crashing on start-up and no page action ever ran. That limit is a quirk of the emulator, because the later boot and every earlier run had 512 MB. I added a guard so the Shields background code can no longer take the whole browser down on a memory error. The app now also logs its memory limit at start, and I re-ran the emulator at about 20:50 UTC. Until that run is clean, the full-screen, turning and Yahoo-controls fixes are checked only by tests, not on a real engine, and not on your tablet.
> - The big compatibility run on about a hundred sites was also mostly void. The emulator's own system was killed three times, and only four sites loaded: Google, Bing, DuckDuckGo and Wikipedia. The two "crashes" it reported were not Kestrel. I changed the setup (more memory, one tab per site, a longer wait) and will re-run it after the full-screen run.
>
> **Still open:**
> 1. Your tablet test of alpha50: the Yahoo Finance video in full screen, turned both ways, and the two Diagnostics lines if anything is still covered.
> 2. The emulator results for full screen, turning and the Yahoo player. I will tell you what they show.
> 3. The compatibility run, then fixes from it.
> 4. Owner-only GitHub steps still waiting: the protected `release` environment, the "pin actions to a full commit SHA" policy, and the private signing key last.
<!-- sha:2848c2e14233 -->

## 2026-11-15T21:05:38Z
> alpha51 is ready, and you were right that alpha50 changed nothing for you. The link is here:
>
> https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.3.0-alpha51
>
> **Why alpha48 to alpha50 looked identical on your tablet.**
> - Those versions only changed how Kestrel reacts to the system bars, and your tablet doesn't behave the way I assumed.
> - My best explanation: it accepts the request to hide the bars and lays the page out as if they were gone, but still draws both bars on top. The app is told the bars are gone, so nothing it can read would have caught this. I can't prove it from here.
>
> **1. Controls under the navigation bar (and no controls in full screen).**
> - By default, Kestrel no longer asks the tablet to hide its bars in full screen.
> - The video now fills only the space between the status bar and the navigation bar, the same layout every normal page already gets. That is why the dock is never hidden behind the bar there.
> - The player's own controls, and Kestrel's own bar when you tap a film that has none, therefore sit above the navigation bar in both orientations.
> - Settings has a new row, "System bars in full-screen video". "Keep" is the default. "Hide" fills the whole screen and only makes sense on a tablet that really hides its bars.
> - **Please check that row exists.** If it is missing, the update did not install. Android keeps the old version without a clear message when the tablet is low on storage. That alone would explain "nothing changed".
>
> **2. Pausing while the screen turns.**
> - My earlier pause only worked for videos with sound, and the Yahoo video is muted. So it was never paused at all.
> - Now the page itself pauses every video when the screen turns, muted ones included. This works for videos inside web-component players like Yahoo's.
> - When the screen has stopped moving, the page is given one extra resize, so a player that measured itself from the old shape redoes its size. Then the video plays again by itself. A video you had paused stays paused, and so does one you touch during the turn.
> - The black cover during the turn is still there.
> - Diagnostics has a new line, "Turning, the page's own pause".
>
> **What I could not verify.**
> - Everything was tested in headless browsers (452 tests) and unit tests only. It was not tested on your tablet.
> - The emulator run for these cases has not given a clean result yet. Its first attempt died of the emulator's tiny memory limit. The second failed at start-up because of a mistake in my own workflow script, which is now fixed. A third run is in progress.
> - I have not seen the "wrong size for a few seconds" symptom reproduced anywhere. The extra resize is my best guess at the cause, not a proven fix. In your third screenshot the picture was 575 pixels tall, which is the landscape height. That suggests the page kept the old height after the width had already turned.
> - If the wrong size still happens, send the Diagnostics lines "Turning under a video" and "Turning, the page's own pause".
>
> **Still open:**
> - Your tablet test of alpha51, starting with the new Settings row.
> - The emulator results for the Yahoo player, which I will report.
> - The compatibility run, which was mostly void and is being redone.
> - The owner-only GitHub steps: the protected `release` environment, the "pin actions to a full commit SHA" policy, and the private signing key last.
<!-- sha:d4c598f2b2c8 -->

## 2026-11-15T21:35:47Z
> alpha52 is out and signed. Here is my answer to your message 47.
>
> **Release (install over the old one):**
>
> https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.3.0-alpha52
>
> You were right that alpha49 and alpha50 changed nothing for you. They rested on a guess about how your tablet treats the system bars, and I can't see your tablet.
>
> - **Bars (alpha51):** Kestrel no longer asks Android to hide the bars in full screen by default. The page now fills only the space between the status bar and the navigation bar, like every normal page, so a player's controls can't sit under the bar in either orientation. Settings has a new row, "System bars in full-screen video" (Keep is the default, Hide fills the whole screen).
> - **Turning (alpha51):** when the screen turns, the page itself now pauses every video, including silent ones like Yahoo's autoplay. Before, only videos with sound were paused. It waits until the screen has stopped moving, gives the page one extra resize so a player that measured itself from the old shape redoes its size, and then plays again by itself. A video you had paused stays paused.
> - **Wrong size for a few seconds (alpha52):** the emulator with the real Firefox engine showed that when a page goes full screen, the engine sizes it to the whole screen height (1024 px), not the space between the bars (947 px). It keeps that size until the next size change. That cuts off a player's bottom controls even with the bars kept, and it fits your third screenshot, a box left at the old landscape height. Now the page reports the size it really got, and if it is wrong the app gives the engine a real size change, at most three times per full screen.
>
> **What I have not checked:** none of this has run on your tablet. It was tested in headless browsers (453 tests), unit tests and the emulator, which is not your tablet's system. The clean emulator run of the alpha52 fix is still going, and I'll report what it shows.
>
> **Please try:** Yahoo Finance video, full screen, turn both ways. Then Menu > Diagnostics and send me these three lines:
> - Full-screen system bars
> - Turning, the page's own pause
> - Full-screen page size
>
> **Still open:**
> - your tablet test of alpha52
> - the live Yahoo player in the emulator
> - the 2-hour site-compatibility run (queued after the current emulator runs)
> - the owner-only GitHub steps (private signing key, protected `release` environment)
<!-- sha:d033a5fc3872 -->

## 2026-11-15T22:42:32Z
> alpha53 is out and recorded. Here is my answer to your message 48.
>
> **Release (install over the old one):**
>
> https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.3.0-alpha53
>
> **What your screenshots show (I measured them):**
> - **Portrait:** the page is laid out as if the window were still landscape. The film is at the landscape height and cut off at the right screen edge, with empty space under it.
> - **Landscape:** the page is laid out as if the window were still portrait. It is a 600 px wide box, the film is cut by the navigation bar, and there is empty space on the right.
> - So after a turn, the page keeps the old orientation's layout for a few seconds. The good news is that the page sits between the bars now, which was the alpha51 change.
>
> **Why it happened:** your tablet's engine hands the new window size to the page late. The black cover from alpha51 and alpha52 went away as soon as Kestrel's own window was still, which is before the page had caught up. The emulator is much faster than your tablet, so it never showed this for seconds. It did show it once, on a quick double turn.
>
> **What alpha53 does:**
> - The black cover and the video pause now stay until the page itself says it has the new size, for at most 7 seconds.
> - A small progress ring shows after about a second so the black doesn't look frozen.
> - A touch on the screen ends the wait.
> - If the page still hasn't caught up after 2.5 seconds, the engine is given one real size change.
> - I found and fixed a bug in my own first version: a page open in two tabs got mixed up with the other tab.
> - Diagnostics: the line "Turning under a video" now says how long the page took to have the new size, the worst case, and how many turns gave up. The line "Full-screen page size" now also counts the pages that report their size.
>
> **What I have not checked:**
> - None of this has run on your tablet.
> - It passed 453 headless-browser tests, the unit tests, and the emulator with the real Firefox engine. There, every waited-for turn logged "the page had the new size after 0.35 to 0.9 s" and the run found no issues.
> - I could not make the emulator's engine slow enough to reproduce your multi-second delay. So I can't promise the black screen won't last 3 to 7 s on your tablet. That is what the new Diagnostics line will tell me.
>
> **Please try:** the Yahoo Finance video in full screen, turned both ways. Tell me how long the black screen lasts, and send me three Diagnostics lines (Menu > Diagnostics):
> - Turning under a video
> - Full-screen page size
> - Full-screen system bars
>
> **Still open:**
> - your tablet test of alpha53
> - the 2-hour site-compatibility run, which is running on the emulator now
> - a stronger slow-engine emulator scene
> - the owner-only GitHub steps (private signing key, protected `release` environment)
<!-- sha:00fe0dafcdb1 -->

## 2026-11-15T22:46:36Z
> alpha53 is out and your message 48 is answered above: the cover and pause now wait for the page to confirm the new size, and an independent review of the new code is running. Nothing has run on your tablet, so how long the black wait lasts there is still open.
>
> **While that review runs:**
> - **Double rotation (ROT3, not released):** the emulator log showed the first manual rotation after full screen started turning the screen twice, with a portrait flash 0.45 s before landscape. Kestrel handed the screen back to the tablet's sensor before Android agreed on the new position. The fix waits 0.65 s for the tablet to stay in its position first. It is in the code with a test but not shipped, and I'll bundle it into alpha54 with the review fixes.
> - **Site-compatibility run:** it is running on the emulator now, about 2 hours, with Shields on and off for about a hundred sites. I'll write up what it finds and fix the cheap issues.
>
> **Open:**
> - your tablet test of alpha53 (the three Diagnostics lines: Turning under a video, Full-screen page size, Full-screen system bars)
> - the review findings
> - the compatibility-run findings
> - the stronger slow-engine emulator scene, which has to wait for the compatibility run to finish because they share the lab
> - the owner-only GitHub steps (private signing key, protected `release` environment)
<!-- sha:ed06d93199a5 -->

## 2026-11-15T23:17:41Z
> Here is an interim answer to your message 49. alpha54 is out, and the new video player is in progress.
>
> **alpha54 (install over the old one):**
>
> https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.3.0-alpha54
>
> **What the audit found, one cause per observation:**
> - **A tap doesn't hide the controls.** Kestrel's own bar only restarted its hide timer when you tapped, so it never toggled. Sites that draw their own controls (Yahoo's player) don't hide them on a tap by default either.
> - **Pulling up the navigation bar covered the controls (alpha50).** That was the bar sliding over the page. Since alpha51 the bars stay and the page sits between them, which is why the controls now show in both orientations.
> - **The "4 seconds" length.** Players show the length the page reports. For a streamed video that number grows as it downloads (4 s, 8 s, …), and an ad has its own short length. The new player will only show a length once it has stayed the same for 2 seconds and isn't smaller than what has been played or buffered. Until then it shows just the position.
> - **The stretched picture on first full screen.** The engine sized the full-screen page to the whole screen height instead of the space between the bars. Turning fixed it by accident. alpha52 added the correction, and the new player will also keep the picture whole.
> - **Rotation slow and buggy.** The engine hands the new size to the page seconds late on your tablet. alpha53 waits for the page, and alpha54 fixes three bugs an independent review found in that:
>   - On Yahoo Finance and YouTube the black cover could stay up for the full 7 seconds on every turn.
>   - The first manual turn after full screen began turned the screen twice.
>   - A touch ending the wait counted as a failure.
> - **The video is slow and laggy.** Kestrel's own scripts measured at no cost earlier. The usual causes are the page's own scripts and ads on a slow CPU, and the 5 Mbit/s hotspot. I can't see your tablet's CPU from here.
>
> **What I'm building now (alpha55):**
> - Kestrel's own full-screen controls, drawn by the app over the video instead of inside the page, so the page can't hide or lay them out wrongly.
> - **Tap:** each tap toggles the controls, and they hide by themselves 3.5 seconds after play starts.
> - **Double-tap:** double-tapping a side jumps 10 seconds, and double-tapping the middle plays or pauses.
> - **Controls:** a seek bar with times, mute, and an exit-full-screen button.
> - **Rotation:** the controls disappear during a rotation and come back after.
> - **Page buttons:** a site's own buttons, such as "Skip ad", keep working.
> - **Setting:** Settings has a new row, "Full-screen video controls": Kestrel's (default) or The page's own.
> - **Progress:** the page script has 12 browser tests, the rules have unit tests, and the Android view is written. I'm connecting it now and will prove it on the emulator with pictures before releasing.
>
> **What I have not run:** none of this has run on your tablet.
>
> **Still open:**
> - the player build above
> - your tablet test of alpha54 and then alpha55
> - the 2-hour site-compatibility run, which is still running on the emulator
> - the owner-only GitHub steps (private signing key, protected `release` environment)
<!-- sha:efd2cdceb2f7 -->

## 2026-11-16T01:13:55Z
> alpha55 is out, with Kestrel's own full-screen video controls. They passed all 22 emulator checks on the real Firefox engine, but I haven't run them on your tablet.
>
> https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.3.0-alpha55
>
> It installs over alpha54. Your points from message 49:
>
> - **Tap toggles the controls.** Each tap on the picture shows or hides them, and they hide on their own after 3.5 seconds while the film plays. They stay up while the film is paused, ended or buffering.
> - **No controls after rotating.** The app now draws the controls over the picture, so they are the same in portrait and landscape and don't depend on the page's layout.
> - **Seeking and buttons.** Double-tap the left or right side to jump 10 seconds, and double-tap the middle to play or pause. There are also play/pause and 10-second buttons, a seek bar you can drag with the time and length, mute, and a button to leave full screen.
> - **Navigation bar covering the controls.** With the default setting (bars kept) nothing can cover them. With "Hide", the controls keep room for the navigation bar you pull up.
> - **"4 seconds" length.** A streamed film's length only shows once it has stayed the same for 2 seconds. Live streams say LIVE.
> - **Stretched picture.** A film whose box has another shape than its picture is now shown whole.
> - **The site's own bar.** It is put away while Kestrel's controls show, so there is one set of controls. I added Yahoo Finance's player bar to that list.
> - **Slow and laggy.** I made the controls lighter (they redraw only when something shown changed, and the page reports once a second while they're hidden). I couldn't find or prove a cause, so I can't say it's fixed.
> - **Rotation.** The slow, glitchy rotation was addressed in alpha52–54, not in this release.
> - **Research.** The notes on how YouTube-style players, Chrome and Firefox do this are in `research/VIDEO_PLAYER_AUDIT.md`.
>
> The emulator found three real bugs, now fixed:
>
> - The page's address overwrote the film's "paused" flag, so the app thought every film was playing.
> - A second press on the play button could repeat the first.
> - Android 10 gives no signal when the navigation bar is pulled up over a film.
>
> An independent code review also found 12 more problems, and I fixed all of them. For testing: 479 browser-script tests and the Kotlin tests pass, and I probed the live Yahoo player in a headless browser.
>
> What I did not test on your tablet:
>
> - speed and smoothness, since the emulator's engine is much faster than yours
> - the real navigation bar
> - your hotspot
> - YouTube's own player
>
> On the emulator, Yahoo's bar was still visible after a turn in the last run that opened the live player. I then hid its player bar, and a headless-browser check on the live page confirmed that. The last two emulator runs didn't open that player, so it isn't re-confirmed in Firefox.
>
> One trade-off: caption and quality buttons that sit inside a site's own bar go away with it. "Settings > Full-screen video controls > the page's own" brings them back for those. I can add a captions button to Kestrel's controls if you want one.
>
> Please try a video in full screen (tap, double-tap, turn the tablet, pause, drag the seek bar), then send me these Diagnostics lines:
>
> - Full-screen video controls
> - Turning under a video
> - Full-screen page size
> - CPU by process
>
> Still open:
>
> - your tablet test of alpha55
> - your tablet report on alpha53/54 (Diagnostics lines "Turning under a video", "Full-screen page size" and "Full-screen system bars")
> - the captions-button question
> - the 2-hour compat emulator run, which I dispatched and will read when it ends
> - the three GitHub settings only you can change: a private signing key, a protected `release` environment, and the pin-actions-to-a-commit-SHA policy
<!-- sha:643319a2f82c -->

## 2026-11-16T01:23:42Z
> I already answered this in the alpha55 reply, which crossed with your repeat. Here is it sentence by sentence, with what was already fixed in earlier alphas so nothing is redone.
>
> https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.3.0-alpha55
>
> - **"Falsely reported 4 seconds."** Fixed in alpha55. The old bar printed whatever length the stream had received so far, and an ad has its own length. The new controls show a length only when it has stayed the same for 2 seconds and is never shorter than what has played or buffered. Live streams say LIVE. This is covered by unit and browser tests, but I haven't seen it on the page you used.
> - **"Stretched on first full screen, normal after landscape and back."** Fixed earlier, in alpha52. The engine sized the page to the whole display until the first real size change, so the app now gives it one. That was proven in the emulator then. Alpha55 adds a second guard that shows the whole picture when a page's box has the wrong shape. I haven't re-checked it on your tablet.
> - **"Rotation buggy and slow."** Fixed in steps:
>   - alpha51 paused the video and nudged the page.
>   - alpha53 keeps the black cover and the pause until the page really has the new size.
>   - alpha54 made one turn of the screen per move of the tablet.
>   - alpha55 hides the controls under the cover and shows them again for 2.5 seconds afterwards, and they no longer depend on the page's layout.
>   - In the emulator a turn settles in about 0.7 seconds. The live Yahoo page needed 1.7 to 3 seconds, and the cover waited for it. I haven't measured it on your tablet.
> - **"Slow and laggy."** Not proven. I removed what Kestrel itself added around a playing video: the old bar measured layout four times a second and wrote to the page, and the new controls redraw only when something shown changed. The numbers that decide it are yours: the Diagnostics lines "Real video" and "CPU by process".
> - **"Controls show up in both orientations."** Kept. Alpha55 draws them in the app, and the emulator pictures show both orientations.
> - **"Tap hides the controls and gives the whole screen to the video."** Done in alpha55. It passed all 22 emulator checks twice on the real Firefox engine, and the site's own bar is put away so nothing else sits on the picture.
> - **"Audit and research."** The three research files are in the repo. Your message made me check that the research and the code actually line up, so I did that now:
>   - `research/VIDEO_PLAYER_AUDIT.md` section 1 now gives each of your observations its real status.
>   - Section 5 compares 23 behaviours of the best players with Kestrel. Each is marked as matching, different on purpose, or a gap with a decision.
>
> The check found three small gaps, now fixed in code but not yet released, because I won't make you reinstall for them. I'll ship them with the next release.
>
> - **Failed video.** A video that fails (a flaky link) keeps the controls up and says "This video stopped", instead of a ring that spins forever.
> - **Fade length.** The fade is 200 ms, as the research suggests.
> - **Wake locks.** The emulator test now records whether the screen is kept awake while a film plays.
>
> Two things the big players have that Kestrel lacks: a captions button and a playback-speed button. Each adds a control to a small screen, so tell me if you want them. Captions matter most, because a site's own caption button goes away with its bar.
>
> I haven't run any of this on the tablet: speed and smoothness, rotation timing, the real navigation bar, your hotspot, or YouTube's own player.
>
> Still open:
> - your test of alpha55 and its four Diagnostics lines
> - your tablet report on alpha53/54 (the "Turning under a video", "Full-screen page size" and "Full-screen system bars" lines)
> - your captions/speed decision
> - the 2-hour compatibility emulator run, which I'll read when it ends
<!-- sha:fec521e24be8 -->

## 2026-11-16T01:27:20Z
> Those two heartbeat reminders were stale. Everything they list is already done, so I redid nothing.
>
> Where things stand:
>
> - **alpha55** is released and answered. Your repeated message is answered too, point by point.
> - **Compat emulator run** is still going. I set a reminder for 03:30 UTC to read it and write `research/COMPAT_RUN1.md`.
> - **Next release, not shipped yet.** While waiting I added a few small fixes for the next release, with tests passing:
>   - A video that fails now keeps its controls up and says "This video stopped", instead of a ring that spins forever.
>   - The controls fade in 200 ms instead of 160.
>   - The emulator test now records whether the screen is kept awake while a film plays.
>   - A stuck-screen safety net: if the app ever stops taking a page's reports, the page gets its own bar and taps back after four refusals instead of being left with no controls.
> - **Emulator check.** I queued one emulator run to prove these changes; it starts after the compat run finishes. I won't ask you to reinstall for these until that run passes.
>
> Still waiting on you: the tablet test of alpha55 with its four Diagnostics lines, and your decision on a captions or speed button.
<!-- sha:419542fca405 -->

## 2026-11-16T02:08:00Z
> Nothing new needs doing at this 02:07 UTC heartbeat. The compat emulator run is still going (55 minutes in) and I have nothing new from you. alpha55 is released, your message is answered, and everything is saved and pushed. I'll read the compat run when my 03:30 UTC reminder fires.
<!-- sha:d1c934c7cf53 -->

## 2026-11-16T02:37:59Z
> **Message 51, your tablet's Diagnostics of alpha55.** The new controls work on your tablet. Turning the tablet under a video is slow, and most of that slowness is not the controls.
>
> **What the numbers show**
> - **Works:** The controls started in 4 of 4 full screens. Each tap showed or hid them (7 taps). The pause button reached the page. Yahoo's own play bar was put away (3 elements). The page had the right size every time (5 checks, 0 wrong). The film paused and played again at both turns.
> - **Slow turns:** Each turn kept the black cover for about 3.1 s (3113 ms and 3086 ms).
>   - Right after the size changed, the app's own screen froze for 0.9–2 s. The app shows the same kind of 1 s freeze at start, so I suspect the engine's surface resize rather than our views.
>   - Android trimmed memory at those moments.
>   - The Yahoo page then went silent for 3.5–14 s and sent no reports. The film only came back about 19 s after the first turn.
> - **Automatic sideways turning never fired:** At the start of full screen the app did not know the film's shape (`video 0x0`), because Yahoo's player hides its video from the old finder. The player's own first report knew it 2.3 s later (1920x1080). You turned the tablet by hand both times, and each manual turn cost the 3.1 s cover.
> - **Controls restarting:** The 3.5 s watchdog (the limit for a silent page) ended and restarted the controls session 4 times in 160 s. Each restart popped the controls up on their own.
> - **Not measured at all:** Whether frames were dropped ("Real video" said nothing played) and what the page was busy with. "CPU by process" only sees the app's own process on Android 10.
>
> **Changed, coded and tested, shipping as alpha56 after one emulator check**
> 1. Automatic turning now takes the film's size from the player's own first report.
> 2. A busy page no longer ends the controls (limit 3.5 s → 12 s).
> 3. Diagnostics now count turns where the page stays silent, and time the page's first report after the cover lifts.
> 4. The controls script now reports numbers measured inside the page: how late its own timer runs, what the script itself costs, and frames shown and dropped (this also fills "Real video").
> 5. Log lines now say how long the engine's part and the app's part of a turn take.
>
> I deliberately did not change the 3 s wait for a silent page or the 1 px size nudge. Your data can't say which way is better; the new numbers will.
>
> A tap shows or hides the controls 300 ms after the tap by design, because that is the double-tap window for seeking. Tell me if it feels slow and I'll make the first tap act at once.
>
> **Not proven:** Speed on your tablet, and whether the late automatic turn works on Yahoo's real page. The emulator check proves it on a lab page only.
>
> **What I need from you:** When alpha56 is out, please repeat the test on a light video page as well as Yahoo, and send the same Diagnostics lines.
>
> **Still open**
> - The compat lab run is in progress.
> - The kp lab run on this bundle is queued behind the compat run.
> - VP12 (caption and quality buttons while Kestrel's controls own the film) still needs your decision.
>
> Next I'm having an independent reviewer read the alpha56 diff while the lab queue runs.
<!-- sha:fe7264e02123 -->

## 2026-11-16T02:41:56Z
> Correction to my interim answer: your paste shows one full screen, not four. The controls session started four times inside it, and three of those were restarts after the 3.5 s watchdog (the limit for a silent page). The fix for that is in the alpha56 code.
>
> **Still running**
> - The independent code review of the alpha56 changes.
> - The compat lab run, which ends around 03:25 UTC.
> - The kp lab run on this bundle, queued behind it. It now includes a check that the late automatic sideways turn works on the real engine.
>
> **One more fix from your log:** the page's "last look" picture was being read back twice while the film was full screen. That wasted CPU and saved the film's frame as the page's look. It no longer happens in full screen.
>
> **Next**
> - Once the lab run is green and the review is handled, I ship alpha56 and send you the Release link with the final answer to message 51.
> - A scheduled wake at 04:20 UTC picks this up in case this session is cut off.
>
> **Open from you**
> - Please repeat the video test when alpha56 arrives, on Yahoo and on one light video page, and send the same four Diagnostics lines.
> - Tell me if the 300 ms tap delay feels slow, so I can make the first tap act at once.
> - VP12 (caption and quality buttons while Kestrel's controls own the film) still needs your decision.
<!-- sha:c7a85d3a4736 -->

## 2026-11-16T02:42:10Z
> Everything is pushed. The branch on GitHub and the local branch are at the same commit (`fe7636f1`), so nothing is left only in this container.
>
> I'm still waiting on three things, and the session will be woken for each:
> - **Code review:** the independent reviewer of the alpha56 changes is still running.
> - **Compat lab run:** it ends around 03:25 UTC, and a scheduled wake at 03:30 UTC reads it.
> - **kp lab run:** the check of the late automatic turn runs behind the compat run, and a scheduled wake at 04:20 UTC picks it up and ships alpha56 if it passes.
<!-- sha:6dba3f1ff197 -->

## 2026-11-16T04:14:25Z
> **alpha56 is released.** It answers your Diagnostics paste (message 51) and your messages about the rotation lag and video lag (message 52, including the re-sent first paragraph). Nothing in it has been tried on your tablet yet.
>
> https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.3.0-alpha56
>
> **One correction to my earlier reply:** your paste showed one full screen, not four. Kestrel's controls started four times inside it, and three of those were restarts after a page that was only busy.
>
> **Why the status bar icons stuck and the video cropped**
> - When the tablet turns, Android freezes the screen until Kestrel has drawn its first frame at the new size.
> - Kestrel's screen thread was blocked about 2 seconds inside the Firefox engine's surface resize, so the wait ran into Android's 2-second limit.
> - Android then showed the old portrait pictures in the new landscape screen: icons at the old right edge, video cut at the old width.
> - **Fix:** the engine's view keeps its old size for the first frame. The black cover and the bars are drawn at the new size at once, and the engine resizes behind the cover. On the emulator the first frame came 76–480 ms after the turn.
>
> **"Wrong size for a long pause"**
> - That is a second, separate delay: your Yahoo page said nothing for 3.6 s to 14 s after a turn.
> - The cover now waits up to 6 s (it was 3 s).
> - The 1 px resize nudge now happens only for a page that answered with a wrong size. Both nudges in your log were for a page that was only busy.
>
> **Also in alpha56**
> - A film in a web component (Yahoo's player) now turns the screen sideways by itself, using the size the player reports.
> - A busy page no longer ends and restarts the controls (12 s limit instead of 3.5 s).
> - The page's saved look is no longer read back from the graphics chip while a film is full screen. That happened twice in your log.
>
> **What other browsers do**
> - Chrome puts the film on its own screen layer, so the screen hardware shows it without the graphics chip.
> - Firefox has no such path on Android.
> - Your tablet has hardware decoders for H.264, H.265 and VP9, so the lag is not the decoder.
> - The more likely causes are your page's busy script thread, and VP9 1080p60, which is beyond the 1080p30 the chip declares. The engine settings cannot hide VP9 on this tablet.
>
> **Third-party code**
> - Media3 (ExoPlayer) is already inside the app through the Firefox engine, so using it adds no new library.
> - libVLC, ijkplayer, mpv and external players are not worth it.
> - To try: the Save video sheet now has **"Play in Kestrel's player (test)"**. It plays plain video files and HLS streams on a SurfaceView with the hardware decoder and no page in the way. It is not offered for DASH or DRM, and it does not carry the page's cookies yet.
> - On the emulator, the page's player dropped 18.7 % of 134 frames and Kestrel's own player dropped 0.3 % of 351. That is a rough look, since both decoded in software, and it says nothing about your tablet's speed.
>
> **Tested:** web tests, unit tests, an independent review (6 small findings fixed), 26 emulator checks on the real Firefox engine, and 13 emulator checks of the new player. **Not on your tablet.**
>
> **What I need from you**
> - Turn the tablet under a film on the Yahoo page, and on a light video page too.
> - Try "Play in Kestrel's player (test)" on the Yahoo film.
> - Send these Diagnostics lines: Full-screen video controls, Turning under a video, Frames around turns, Real video, Video decoders, Heat and speed, and Kestrel's own player.
>
> **Still open**
> - Page codec and frame-rate policy (I'll build it next).
> - A 720p picture cap for HLS pages.
> - DASH, cookies, and freezing the page behind the native player.
> - Whether the first tap should show or hide the controls at once (it waits 300 ms for a double tap).
> - Captions and quality buttons while Kestrel's controls own the film.
<!-- sha:2290f1d9a234 -->

## 2026-11-16T04:58:22Z
> Alpha57 status: the page codec policy (ANS52g) and the HLS picture-size cap (ANS52h) are coded and unit-tested locally. The emulator proof run on the real Firefox engine (run 43806840560) is booting now. An independent review of the diff is running in parallel, and I'm woken at 05:08Z to read both.
>
> Nothing from alpha57 has been tried on the tablet yet, so I'm not asking you to test it.
>
> Open items I'm holding:
> - Your tablet report on alpha56: the Diagnostics lines, and the Save video sheet's "Play in Kestrel's player (test)".
> - After the lab read: alpha57 itself (codec policy + 720p HLS cap) and its release.
> - Older rows: VP10, VP12, ROT2e, ANS48, SOAK1d, LAB2f.
<!-- sha:92431e68186c -->

## 2026-11-16T04:59:24Z
> Waiting on two background jobs: the emulator proof run (43806840560, now running the lab suite) and the independent code review. I'll pick up when either finishes, or at the 05:08Z wake.
<!-- sha:407d8142df8f -->

## 2026-11-16T05:57:06Z
> Alpha57 is not shipped yet. The code is written and passes every test I can run here. I'm waiting on two things before shipping.
>
> - **Emulator run on the real Firefox engine (run 43812820865):** it covers the controls, Kestrel's own player and the press-to-play rule. I'll read its report when it finishes.
> - **Independent review of the press-to-play script:** it's read-only, and a bug there could stop videos playing on real sites.
>
> I also added a lab check for the Quality button in the page's full-screen controls. It needs a second lab run, since the one in flight started before I wrote it.
>
> What your message 53 gets so far, all coded:
> - **No auto-playing:** a video starts only when you press play.
> - **No video data before play:** only the picture the site shows for the video is downloaded.
> - **Instant controls:** a tap on the picture shows or hides the full-screen controls with no wait and no fade.
> - **Quality:** a Quality button in Kestrel's own player. The page's full-screen controls get the same button for films that offer two or more pictures, like the Yahoo one; picking one moves the film to Kestrel's player from the same second.
> - **Download badge:** the button no longer stays lit on the Home screen.
> - **Kestrel's own player:** it follows how you hold the tablet and has a rotate button.
>
> None of this has run on your tablet. The emulator proves behaviour, not how smooth it is on your hardware.
>
> Still open from earlier: your tablet test and Diagnostics numbers, a caption/speed/quality entry for films without a playlist address (VP12), and a few older lab-tuning rows (ROT2e, SOAK1d, LAB2f).
<!-- sha:6dbf79de49f7 -->

## 2026-11-16T05:57:26Z
> Nothing new from you. The queued notification was an old routine telling me to read the earlier codec/picture-size lab run (43806840560). I'd already read it and fixed what it found, so there's nothing left to do on it.
>
> Still waiting on the current lab run (43812820865, "Run the lab" step in progress) and the press-to-play review. I'll pick both up when they finish.
<!-- sha:102309bab3d7 -->

## 2026-11-16T06:01:50Z
> Still waiting on the emulator run, which is in its test phase, and on the press-to-play review. I'll check again when the next timer fires.
<!-- sha:74988d508057 -->

## 2026-11-16T06:42:16Z
> Lab run 3 (id 43818471174) is running on the exact build I plan to ship, and I'm waiting on it before releasing alpha57. Once it's clean I'll ship, wait for GitHub to build and sign it, then answer message 53 point by point with the Release link.
<!-- sha:544b4ded09db -->

## 2026-11-16T06:51:38Z
> The release note is drafted; I'm waiting for lab run 3 to finish before shipping.
<!-- sha:9c7ba13a7abd -->

## 2026-11-16T06:56:25Z
> Still running (about 11 minutes in; the previous run took 9). I'll check again in three minutes.
<!-- sha:9aaa2ee68c0f -->

## 2026-11-16T07:01:38Z
> Release build for alpha57 is running on GitHub (run 43820755532). I'll check it in about seven minutes.
<!-- sha:3c51fa54534d -->

## 2026-11-16T07:10:16Z
> Alpha57 is built and signed by GitHub, ready to install over alpha56.
>
> https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.3.0-alpha57
>
> I have not run it on your tablet. I tested it on the emulator running the real Firefox engine.
>
> **Your first list**
> 1. **No video starts by itself.** The engine blocks all autoplay, the app refuses every autoplay request, and a script in each page keeps every video closed until you press it. In the emulator, nine test pages never played without a press, including looping clips and script-driven players.
>    - A looping clip with no play button stays still, and tapping it does not start it.
>    - Music and sounds are not held, only video.
>    - The switch is Settings > "Videos start only when I press play" (on by default). Turn it off and sites behave as before.
> 2. **No video data before you press play.** Only the page's poster picture loads, and a video with no poster stays blank until you press play. The one exception is the app's own one-byte question about the file's size, used for the download list.
> 3. **Controls at once.** A tap on the picture now shows or hides the full-screen controls with no wait and no fade. The app answers on the screen itself, between 4 and 104 ms in the emulator. A double tap on the left or right side still jumps 10 seconds.
> 4. **Quality.**
>    - Kestrel's own player has a Quality button listing every picture the stream offers, with size, frames a second and bit rate. Your pick is remembered.
>    - The full-screen controls of a film in the page get the same button when it is an HLS stream with two or more pictures, which the Yahoo film should be. Picking one moves the film to Kestrel's player at the same second, and Back returns to the page.
>    - The emulator tested this with a stand-in film, not your Yahoo film.
>    - Films with no playlist address (YouTube-style players) get no list yet.
>
> **Your second list**
> 1. **Download button lit on Home:** the Home screen sits over a page that stays alive underneath, and the button, the list and the menu entry ignored that. They now do not count a page under Home.
> 2. **Kestrel's player in portrait:** it no longer forces landscape or stretches the first picture. It follows how you hold the tablet and has a rotate button at the top left. The emulator turned it both ways with the tablet and with the button.
> 3. **Smoother than the page's player:** noted. That is why the Quality button hands films over to it.
>
> I also shipped the open parts of message 52: sites are told "no" to video formats this tablet cannot play smoothly (VP9 above 1080p30), and HLS pages are held to a picture size that fits the screen. Both are settings.
>
> An independent review of the press-to-play script found 12 problems. I fixed 8, confirmed 1 on the emulator, and left 3 on purpose:
> - A player inside another site's frame may need a second tap inside the frame.
> - After one press, a site can still queue its next film itself, because ad players need that.
> - A Settings change reaches new pages within a minute.
>
> **Proof:** 551 web tests, all unit suites, and 243 emulator checks passed on the exact build. Nothing was run on your tablet.
>
> **Please send back:** the Diagnostics lines "Videos start only when pressed", "Full-screen video controls" and "Kestrel's own player". Also tell me about any video site that no longer plays after a press, or a film with no Quality button.
>
> **Still open:**
> - Your tablet test of alpha57.
> - A caption, speed and quality entry for films with no playlist address (VP12).
> - A few older rows: VP10, ROT2e, ANS48, SOAK1d, LAB2f, and the optional video-speed experiments (ANS52j).
<!-- sha:a6fec1c4bb30 -->

## 2026-11-16T07:53:56Z
> Hand-over lab run 43826954106 is running (about 8 minutes). Waiting on its results before the final full lab pass and the alpha58 release.
<!-- sha:1f5f3c41de01 -->

## 2026-11-16T08:01:22Z
> Still running; I'll check the result in about two and a half minutes.
<!-- sha:c3d6be92b07e -->

## 2026-11-16T08:43:13Z
> Status: the plain-video fix works on the real engine and I'm now in the verification stretch before alpha58.
>
> - **Running now:** the full emulator video run (about 12 more minutes) and an independent review of the new code.
> - **Found and fixed on the way:**
>   - A page script could have faked the "open Kestrel's player" question without you touching anything. Now only your own tap can start it.
>   - A film you press play on again continues where you stopped.
> - **Next:** read both results, fix anything they find, then ship alpha58 and answer your message 54 point by point with the download link.
> - **Not yet verified:** nothing has run on your tablet; every result so far is from the emulator.
<!-- sha:4303d0a887f5 -->

## 2026-11-16T08:50:12Z
> Waiting on the emulator run (ends about 08:58 UTC) and the code review; I'll pick up from whichever finishes first.
<!-- sha:9e711009be91 -->

## 2026-11-16T08:54:46Z
> Still waiting on the emulator run and the review (both running); I'll read each as soon as it lands.
<!-- sha:044cf0d04fd4 -->

## 2026-11-16T08:58:58Z
> Still waiting; the emulator run should finish within a few minutes.
<!-- sha:890b63694e05 -->

## 2026-11-16T09:02:07Z
> Still running; checking again in a few minutes.
<!-- sha:c70ff9ad45cd -->

## 2026-11-16T09:04:22Z
> The independent review found nine real problems in the new hand-over code, including a real security hole; I'm verifying each one against the code before changing anything. First, checking where the emulator run stands.
<!-- sha:28befa47970a -->

## 2026-11-16T09:14:29Z
> Test suites running; I'll read their results in a few minutes, then start the final full emulator run.
<!-- sha:db9111ca2216 -->

## 2026-11-16T09:19:11Z
> Final emulator run in progress (started 09:18 UTC, expected to end around 09:40). I'll check it in a few minutes.
<!-- sha:07846e82f952 -->

## 2026-11-16T09:24:08Z
> Waiting for the emulator run; the next check is at about 09:35 UTC.
<!-- sha:b9b2f925fb46 -->

## 2026-11-16T09:28:59Z
> Still waiting on the emulator run (no results yet).
<!-- sha:f13b276a2df8 -->

## 2026-11-16T09:33:51Z
> Emulator run still going (lab job started 09:23, now nearly 20 minutes in); I'll look again in four minutes.
<!-- sha:0d21988291c6 -->

## 2026-11-16T09:41:20Z
> Gates running; I'll read their result in a couple of minutes.
<!-- sha:496109449eef -->

## 2026-11-16T09:43:57Z
> The release gates passed (alpha58, build 159, is staged). I'm waiting for the last emulator re-run before telling GitHub to build and publish it.
<!-- sha:d4e081f1b632 -->

## 2026-11-16T09:50:31Z
> Waiting for the release build to finish; I'll verify the tag, all build jobs and the published APK before recording or announcing anything.
<!-- sha:441450ac36c8 -->

## 2026-11-16T09:58:07Z
> Waiting for the release run (about five minutes between checks).
<!-- sha:a0ffb8fc909d -->

## 2026-11-16T10:03:08Z
> Android build in progress; I'll confirm the tag and the published APK before recording or announcing anything.
<!-- sha:2cf57785815d -->

## 2026-11-16T10:09:42Z
> Your message 54 is answered in alpha58, which is built, signed and published. I haven't run any of it on your tablet. Everything was tested on a Firefox-engine emulator and in headless browsers, so how it feels on the tablet (smoothness, decoder) is still unchecked.
>
> Open this page on the tablet and tap the `.apk`; it installs over your current Kestrel G:
>
> https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.3.0-alpha58
>
> **Your points, one by one**
>
> - **(a) The web player was slow and laggy.** When you press play on a video in a web page, Kestrel's own player now takes the film over. It's the smooth one from the download button's "Play in Kestrel's player". Back returns you to the page. Press play on the page again and it continues where you stopped.
>   - **Works for:** a plain video file, a stream the page asked for (a player that loads a playlist), and a film that goes full screen.
>   - **Stays in the page, as before:** YouTube-style players that hide their stream, and protected (DRM) films. Those can still lag.
>   - **If Kestrel's player can't play a film,** it goes back to the page, which plays it for the next two minutes. A site that needs your login for the video file could do this.
>   - **Setting:** Settings > "Web videos play in": Kestrel's player whenever it can (default), only in full screen, or the page's own player. It only works while "Videos start only when I press play" is on.
>   - **Your tap only:** an independent review found that a page's script could have faked the request and opened the player without you. That is fixed. Only your own tap on the page can start it now.
> - **(b) Full screen turned sideways by itself.** "Turn the screen for full-screen video" now starts as "Follow the tablet", so portrait stays portrait. "Automatic" is still there if you want it.
> - **(c) The resolution box didn't close and froze.** That was the site's own box, and the freeze was the site's code on a busy page. Films that move to Kestrel's player use Kestrel's own Quality list instead, and it now has an x button that closes it.
> - **(d) Your feedback** that Kestrel's player is smooth and upright is why I modelled the web player on it. Thank you.
> - **(e) Your request** is what (a) delivers.
>
> **What was tested**
> - Unit tests and 573 web tests in headless browsers.
> - An independent code review: 9 real problems found, all fixed.
> - About 270 emulator checks. Everything about the new hand-over passed on the emulator, including the plain video with the engine's own play button, a play button over the picture, a script player with a stream, a player with nothing to take over, a broken address, pressing again, and the setting.
> - A few older emulator checks of Kestrel's controls missed on a timing detail in some runs (a different one each time). I widened their wait, and the one re-run still missed one, so those aren't fully clean.
> - I have not tried your own sites, only test pages.
>
> **What I need from you:** the Diagnostics line "Web videos in Kestrel's own player" after you've watched a few videos, and which sites still play in the page.
>
> **Still open**
> - Films with no playlist address (the YouTube-style ones: their quality and caption menus; VP12).
> - Improving from the long-run soak findings (SOAK1d).
> - The fast-wifi lab program (LAB2f).
> - Older items still open in TASKS.md: VP10, ROT2e, ANS48, ANS52j.
<!-- sha:75b6aab7ec25 -->

## 2026-11-16T10:12:43Z
> The owner-facing answer is already given; I'm saving the session handoff notes (done by a helper) and will confirm the saved paths when it reports back.
<!-- sha:d264dac5b6b0 -->

## 2026-11-16T15:31:19Z
> Alpha58 is released and your message 54 is answered above. The session handoff is also saved and pushed (last checkpoint 2516), so a new session can pick up from the files alone.
>
> - **What the handoff tool did:** it replaced the auto-managed "Session Learnings" block in `CLAUDE.md` and `AGENTS.md` with this session's decisions. That would have dropped the earlier mini-panel, open-space and slow-script decisions from `CLAUDE.md`, so I put them back by hand in a separate section above the auto block, along with the emulator line. They also remain in the repo's history.
> - **Not run on your tablet:** I only tested it on the emulator, so I'm waiting on your report for how it feels there.
> - **What I need from you:**
>   - the Diagnostics line "Web videos in Kestrel's own player" after you've watched a few videos
>   - which sites still play in the page
> - **Still open:**
>   - films with no playlist address, such as YouTube-style ones (VP12)
>   - improving from the long-run soak findings (SOAK1d)
>   - the fast-wifi lab program (LAB2f)
>   - VP10, ROT2e, ANS48 and ANS52j in `TASKS.md`
<!-- sha:414ea24b1c97 -->

## 2026-11-16T15:55:59Z
> Waiting for the emulator run of the Yahoo fix (it started 15:52 and takes about 11 minutes).
<!-- sha:b953e150cb5e -->

## 2026-11-16T15:58:47Z
> Still waiting on the emulator run.
<!-- sha:ae6021afe55e -->

## 2026-11-16T16:01:36Z
> Web tests running for the alpha59 release; the next step is the release gates.
<!-- sha:17f8ef987e65 -->

## 2026-11-16T16:04:00Z
> Waiting for the web tests and the emulator run.
<!-- sha:01525dbab226 -->

## 2026-11-16T16:05:21Z
> The web tests pass (573 of 573); the emulator run for "a different video gets its own stream" is in progress.
<!-- sha:e6e9f19fa4be -->

## 2026-11-16T16:33:54Z
> The report is done and sent as one Markdown file: `/tmp/claude-0/-home-user-Browser/12796fbc-bb0d-552d-b005-24654337a327/scratchpad/KEYBOARD-APP-PLAN.md`. It's about 20,000 words. I wrote no code and ran no tests, nothing was run on your tablet, and my web checks were search results only, not full pages.
>
> The new session should open with this: "Read KEYBOARD-APP-PLAN.md completely, then do section 11 (bootstrap) and continue with section 12 in order." Sections 0, 1, 2 and 12 are the minimum it needs.
>
> ## What I found
> - **Most of the hard part already exists.** The browser has a complete glide engine in pure Kotlin: the decoder, an 83k-word list with word pairs, and learning of your words, pairs, triples, corrections, glide shapes and thumb habit. It has 122 unit tests and a learning-file export. It has no Android dependencies, so it moves to a new app unchanged.
> - **The missing parts** are everything that makes it a system keyboard. That means the Android keyboard service, tap-typing prediction and autocorrect, suggestions, emoji, clipboard, and any layout beyond English QWERTY. I listed 12 specific gaps in the report, including that the code only handles letters a–z, which blocks Spanish accents and ñ until it's changed.
>
> ## What the plan recommends
> - **Build on the browser's engine** instead of forking HeliBoard, FlorisBoard or FUTO. HeliBoard is GPL-3.0 and its glide engine is a closed library it doesn't ship. FUTO's licence restricts forks, and FlorisBoard uses Jetpack Compose, which is too heavy for 2 GB.
> - **No internet permission in the keyboard.** A keyboard sees everything you type, so it can't send it anywhere. Translation therefore runs offline.
> - **Learning upgrades** to get the "better the more I type" behaviour:
>   - a per-key touch model;
>   - shape memory that carries over between similar words, so a habit learned on "for" also helps "four" and "form";
>   - word-pair and three-word context, plus a topic signal for the sentence so far;
>   - a way to measure accuracy on your real thumb without the data leaving the device.
> - **Translation to Mexican Spanish.** It uses an offline translation model in its own process, plus a Mexican-Spanish clean-up layer. A generic Latin-American model isn't Mexican by itself, and DeepL says so about its own. Whether such a model runs on a 32-bit ARM tablet is unverified, so the plan starts with a test on your tablet.
> - **Phased task list** of about 30–40 sessions, with five checkpoints where it stops for you to test.
>
> ## What you should know
> - **I didn't touch the Kestrel repo.** Its own hooks did write there. Its message-capture hook committed your message into `INBOX.md` on my session branch `claude/laughing-cori-u22au1`, and a similar hook will record this reply into `OUTBOX.md` on the same branch. That branch is mine, not the other account's, and you can delete it. The report itself was written outside the repo.
> - **Your Spanish decision.** In the browser project you once said "Do not include Spanish". I treated your new translate request as overriding that, but whether you want to type in Spanish too is an open question in the report.
> - **Safety and signing.** A broken keyboard on a tablet with no other keyboard locks you out of typing, so the plan builds the escape hatches first. The new app should be signed with a private key, not the browser's public one. Otherwise any APK could silently replace your keyboard, and changing keys later erases what it learned.
> - **Questions to answer early.** Q1 is the app name. Q2 is a private or public GitHub repo, where I recommend private. Q3 is whether you want to type in Spanish or only translate. The full list, with defaults, is section 15.
<!-- sha:03a0e0a3ae17 -->
## 2026-11-16T16:07:10Z
> Waiting for the emulator run (expected around 16:14 UTC).
<!-- sha:7735f2ff8dd0 -->

## 2026-11-16T16:10:09Z
> Short wait, then I read the emulator results.
<!-- sha:1cad998af89b -->

## 2026-11-16T16:11:37Z
> Almost done; I'll read the report as soon as it appears.
<!-- sha:cc7c19ef4ee8 -->

## 2026-11-16T16:11:56Z
> Still running; checking again in two minutes.
<!-- sha:ba39173a6388 -->

## 2026-11-16T16:14:41Z
> Waiting for the release checks to finish; the release build is dispatched right after.
<!-- sha:3292d1b5ec5b -->

## 2026-11-16T16:27:56Z
> Your messages 55 and 56 are fixed in alpha59, except for the player inside the page, which isn't built yet. It still opens full screen. I also haven't run any of this on your tablet.
>
> https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.3.0-alpha59
>
> **Message 55, the black Yahoo video: fixed.**
> - **Cause:** Yahoo hands the browser one main list for the film, plus separate lists for the picture, the sound and the subtitles. All of them are as long as the film. Kestrel's player was given the sound or subtitles list, so it showed the time and no picture. I found this by loading the real Yahoo page from my build machine.
> - **Fix:** Kestrel's player now always takes the main list, the one that offers pictures.
> - **Safety net:** if a film plays for 7 seconds with no picture, Kestrel's player hands it back to the page, which plays it with a short message.
>
> **Message 55, switching to another video: fixed.**
> - The newest main list the page asked for is now the film, so each video gets its own stream.
> - A new video starts at the beginning. "Resume where I stopped" only applies when you press play on the same video again.
>
> **Message 56, an old video showing when you pick a different one: same fix.** The old video's list was being reused for the new press. Each press now takes its own list.
>
> **Message 55, the player inside the page: not in alpha59.**
> - **What's coming:** the video will sit over the page's own video box, follow the page as you scroll, and have a full-screen button, so you still see the website around it.
> - **Where it stands:** I'm building it now for the next release. The position-tracking rules are written and their unit tests pass. The page-side measuring, the view itself and the controls are not.
> - **Until then:** it opens full screen as before. To keep the page's own player, use Settings > Web videos play in > The page's own player.
>
> **Tested:**
> - Unit tests, 573 web tests in headless browsers, and 53 emulator checks of the hand-over on the real Firefox engine.
> - The emulator copied Yahoo's list layout and covered the page moving on to another video, and a stream with sound but no picture.
>
> **Please do on the tablet:**
> - Try the same Yahoo video again.
> - Send the Diagnostics line "Web videos in Kestrel's own player". It now names the site of the list it took.
>
> **Still open:**
> - the player inside the page (in progress)
> - VP12: films with no playlist address, e.g. YouTube style
> - VP10
> - ROT2e
> - ANS48
> - SOAK1d
> - LAB2f
> - ANS52j
<!-- sha:50a847bda896 -->

## 2026-11-16T16:52:54Z
> Your answers are clear, and the translation-quality requirement changes the plan, so I'll research it properly before updating the report. First I'm checking the strongest offline translation models, whether they fit your tablet, GitHub's private-repo limits, and the keyboard name.
<!-- sha:a95e4167d154 -->

## 2026-11-16T17:00:32Z
> I've updated the plan with your three answers and sent it as `KEYBOARD-APP-PLAN.md`; use this one instead of the earlier file. I couldn't run any translation model here, so the translation quality claims are estimates and search results, and the plan tests quality before building anything.
>
> ## Your answers
>
> **Q1, the name: Swoop.** It fits glide typing, and kestrels swoop, so it echoes your browser. I found no Android keyboard with that name. The apps are `app.swoop.keyboard` and, if you want them, the translator add-ons `app.swoop.translate` and `app.swoop.online`. The repo name is `swoop-keyboard`. You can rename it later.
>
> **Q2, private repo.** Section 11.0 is a click-by-click list. The short version:
> 1. On GitHub tap **+**, then **New repository**. Name it `swoop-keyboard`, choose **Private**, and leave README, .gitignore and license all off.
> 2. Let Claude in at https://github.com/apps/claude/installations/select_target by choosing "Only select repositories" and adding it. Also check GitHub is connected at https://claude.ai/connect-github.
> 3. Start a new Claude Code session on that repo, attach the file, and say the start phrase.
>
> A private repo has two side effects:
> - **Downloading APKs:** a release link on a private repo only works while you're signed in to GitHub in that browser, so sign in once in the Kestrel browser. The plan describes a workaround if that's a nuisance.
> - **Build minutes:** private repos get 2,000 free build minutes a month, which is plenty for building the app. That figure is from older GitHub docs, so check your usage page.
>
> **Q3, no Spanish typing, translation only when you turn it on.**
> - I removed everything about Spanish layouts and dictionaries.
> - The setting is off by default. When it's off there is no button, nothing running, and no network use.
>
> ## Translation quality (this is the part to read)
>
> A modern translator doesn't translate word by word; it translates whole sentences. The real risks are idioms translated literally, wrong politeness level, losing context between sentences, and Spain-style words in Mexican text.
>
> - **Offline on your tablet.** With 2 GB of RAM and a slow processor, the best model that fits is about 75–240 million parameters. It would be a good translator for ordinary messages, but not as good as a large AI model on idioms, slang and context.
> - **Large AI models don't fit.** Google's new open TranslateGemma 4B needs about 2 GB on its own, so it doesn't fit.
> - **Online option.** The tablet's weak spot is the one thing a large model fixes. Claude Sonnet 5.5 costs about a third of a cent per message, and Haiku 5.5 about 1/70 of a cent. It would sit in a separate app so the keyboard itself stays free of internet access. The catch is that it needs an Anthropic account and key, and your text goes to Anthropic.
> - **Test before building.** The plan has no engine work until a blind test passes.
>   - A session writes 150 test texts plus correct Mexican answers, and you fix them.
>   - GitHub's servers 
> [... 469 characters omitted ...]
> lgar in Mexico) and handles "$" amounts and dates. It starts from a seed word list that you need to review.
>
> ## Not done
> - I couldn't test the models hands-on. The sandbox's safety check blocked a script that tried to reach model sites, and I didn't work around it. That's why the test runs on GitHub's servers.
> - Items I couldn't confirm are marked in the file's appendix.
>
> ## Open questions
> Q4–Q14 in section 15 all have defaults, so none blocks starting. The ones that matter most:
> - **Q4:** should translation use friendly "tú" or formal "usted" by default, and should it ever translate Spanish into English?
> - **Q5:** are you willing to add the online app after you've seen the test results? I'd ask again then.
> - **Q13:** what phone do you have, or how much RAM? A big-RAM phone could run a better offline translator than the tablet.
>
> Everything else from the first plan carries over unchanged.
<!-- sha:53dc0e3b90d3 -->
## 2026-11-16T16:53:27Z
> Still waiting on the emulator run and the review. I'll continue when they finish.
<!-- sha:af56b43a1f10 -->

## 2026-11-16T17:04:31Z
> Draft release note for alpha60 is written, with the test results still to be filled in; waiting for the emulator run.
<!-- sha:deed9114c4b6 -->

## 2026-11-16T17:26:30Z
> Waiting for the background tests and the emulator run to finish.
<!-- sha:bc2e2601950b -->

## 2026-11-16T17:29:15Z
> Waiting for the notification from the timer and the emulator run.
<!-- sha:cfdc66926d90 -->

## 2026-11-16T17:36:37Z
> Waiting for the timer, the web tests and the emulator run.
<!-- sha:dfbcf74a08d4 -->
## 2026-11-16T17:19:46Z
> I updated the plan with your answers and sent it as `KEYBOARD-APP-PLAN.md` (use this one; it replaces the earlier file). I read your `tjshea90/LinkMeter` repo read-only and changed nothing in it, and I didn't run any of its translation code or call the services.
>
> ## What LinkMeter uses, now in the plan
> - **MyMemory first:** it's a free service with no account, asked for Mexican Spanish (`es-MX`). It allows about 5,000 characters a day, and each request is capped at 500 bytes.
> - **Google Translate as the fallback:** the free endpoint behind Gboard's translation, used when MyMemory is used up or unreachable.
> - **A Mexican-Spanish safety layer on every result:**
>   - "coger" is vulgar in Mexico, so it becomes "agarrar".
>   - "coche" becomes "carro".
>   - "ordenador" becomes "computadora".
>   - "vosotros" forms become "ustedes".
> - **Plain texting words are spelled out before sending to Google:** "idk", "tbh" and so on.
> - The plan tells the new session to port this code and its roughly 56,000 tests to Kotlin and require the same results.
> - **Friendly "tú":** the services answer in "tú" for casual English. I added a test list and a guard that asks the other service if an answer comes back formal.
>
> ## "Enter translates, Enter again sends"
> LinkMeter's Send logic is ported as is. The first Enter replaces your English with the Spanish for you to check, and Undo and "Send my English" are there. The second Enter sends. It follows the same rules as LinkMeter, and it's off everywhere until you switch an app on.
>
> ## What a keyboard cannot do (the plan says so plainly)
> - **Apps with their own Send button:** most texting apps, including LinkMeter, have one. The keyboard can translate on Enter, but you tap the app's own Send yourself.
> - **LinkMeter itself:** it already translates on its own, so the keyboard skips it. Otherwise the text would be translated twice.
> - **Long-pressing any message in any app:** this is impossible for a keyboard. The plan gives three routes that do work:
>   - the text-selection menu, which shows "Translate to English" wherever an app allows selecting text;
>   - copy the message, then use the keyboard's translate button;
>   - select text in the box you're typing in.
>
>   LinkMeter's own long press stays as it is.
>
> ## What I removed
> All the offline-model, test-first, fine-tune, and large-AI-model translation material is gone. The old Spanish-typing material was already removed in the last round.
>
> ## Things to know
> - **Internet permission:** I kept the keyboard free of any internet permission, so it can't send what you type anywhere. Translation lives in a second tiny app, Swoop Translate, which is the only one that can use the internet. If you'd rather have one app like LinkMeter, say so and the plan changes.
> - **Service limits:** MyMemory's free allowance is small, and over a carrier hotspot the shared address may use it up faster. Google's endpoint is unofficial and could change or block you. The plan covers both with clear messages and tests, but I couldn't try live requests from here.
> - **LinkMeter's own notes:** they say the translation hadn't been tried on your tablet when written, so your real use is the only real test.
>
> ## Questions for you
> The new ones are in section 15, and the defaults let work start:
> - **Q15:** keep the internet permission in the separate tiny app (my default) or put it in the keyboard?
> - **Q16:** in which apps do you want translate-on-Enter? The default is none until you switch each one on.
> - **Q17:** is it OK that in apps with their own Send button you tap Send yourself?
> - **Q18:** is it OK to translate received messages through the selection menu or by copying, instead of long-pressing any bubble?
> - **Q19:** should LinkMeter's message box change so Enter sends there too? The default is no. That would be a change in the other repo, and I'd only do it if you ask.
>
> Still open from before: your repo setup (steps in the plan), then a new session with the file attached.
<!-- sha:998bcb1804a1 -->

## 2026-11-16T17:48:43Z
> Waiting for the emulator run to finish.
<!-- sha:117df21e9e41 -->

## 2026-11-16T18:08:06Z
> Lab gv run 43925029376 is still running on GitHub. The JVM unit suites passed; the web tests are still going. I'm waiting for the lab report, which will show whether the new finger-injection path (`input motionevent`) reaches the app and whether the private/regular-tab checks pass.
<!-- sha:631b07c60738 -->

## 2026-11-16T18:09:13Z
> Waiting for that timer; the lab run should finish around 18:14Z. I'll read its report as soon as the artifacts appear.
<!-- sha:624cef88341b -->

## 2026-11-16T18:09:42Z
> Still waiting: the lab run is about 5 minutes in, and the web tests are still going. I'll read the report when the timer fires.
<!-- sha:c47c9fb40b7a -->

## 2026-11-16T18:09:51Z
> Those were older timers firing; the lab run itself isn't finished. I'll hold until the 230-second timer completes and then read the artifacts.
<!-- sha:eab33a3a0fcd -->

## 2026-11-16T18:10:05Z
> Web tests: 576 of 576 pass, and the JVM suites passed earlier, both on the current head. Only the lab run remains; waiting for its timer.
<!-- sha:8095b65f3000 -->
