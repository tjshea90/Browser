# LAZY_RESTORE: "Do the lazy restore fix if it is logical and what major browsers do" (owner, 2026-11-24, PERF17b)
Owner, verbatim: "Look to improve speed. Animations on websites are laggy. Do the lazy restore fix if it is logical and what major browsers do. Cloudflare tests for anti bots are extremely slow."
Earlier note this answers: research/INSTANT_BACK.md section 4 item 5 ("a lazy restore of the last tab ... owner decision").
Privacy note: this repo is PUBLIC; site KINDS only.

## 1. What the major browsers do (sources read 2026-11-24)
| Behaviour | Who | Source |
| :- | :- | :- |
| Restore the whole tab list at once, load ONLY the tab in front, the others when they are opened ("restore on demand") | Firefox desktop (`browser.sessionstore.restore_on_demand`, default true); Chrome and Firefox for Android restore tabs without loading their pages until selected | Mozilla session restore wiki and bugs 648683 / 873771 (bugzilla.mozilla.org); Chromium tab state design |
| After a long break the tab in front is NOT reopened: the app opens on the homepage, the old tab is one tap away | **Firefox for Android: homepage after 4 hours or more of inactivity; the last tab for less than 4 hours** ("Choose your opening screen for Firefox for Android"); Chrome for Android: start surface with a card for the last tab (flag `StartSurfaceAndroid`; the last-tab card was tested for tabs from the last 24 hours) | support.mozilla.org/kb/set-opening-screen-firefox-android; chromium start_surface source; Android Central / MSPowerUser reports (exact Chrome timing not confirmed) |
| Restore a page picture first ("freeze-dried tabs": Chrome starts 13 % faster on Android) | Chrome | blog.chromium.org 2021/03 "advanced memory management" |

## 2. What Kestrel G did before this change
- Background tabs: already lazy (a restored tab has `pendingLoad` and no engine session until it is opened). That is the first row above: nothing to do.
- The tab in front: loaded at once at every start, behind the last-look picture (EY-1). The owner's tablet pays 12 to 27 s of the one content thread for a heavy web app at each start (digest: cold start to first paint median 19 s, almost all of it one heavy chat page restored behind the picture), during which every other tap is slow too.
- The mechanism to NOT load a tab already existed: AQ1 (a tab left on the Home screen comes back on Home, keeps its address and Back stack, nothing loads; Back, a new address or the Home search shows the page).

## 3. Decision: logical AND what the major browsers do: YES, in the Firefox-for-Android form
After a break of 4 hours or more, the tab in front comes back on Home with ONE extra line "Continue: <page title>" under the search field; a tap loads the page (Back and a new address work as before). Less than 4 hours: unchanged, the page loads at once behind its picture. Settings > "After a long break, wait with the last page" turns it off.
Why this and not "never load the front tab until touched": no major browser withholds the tab in front after a SHORT break (a person who left a page a minute ago expects it); every one that withholds it does so only after hours away, and shows its start screen with a way back to the page. The rule is the same.
Why Home and not a picture with "tap to load": the Home screen is the tested AQ1 path (no engine session is created, no cover, no timeouts to leak); a picture overlay would need a second state machine next to the start-up cover (`restoringTab`, `lookTab`, the 25 s timeout) and any bug there leaves a dead screen.

## 4. Rules (all in core `LazyStart`, tested in LazyStartTest)
- applies only when: the switch is on, away >= 4 h (`LazyStart.AWAY_MS`), the page is http(s), the app was NOT started by a link or a share, the start is a fresh one (no saved instance state: not the system re-creating a running app's screen), the tab was not already left on Home.
- "Away" = the wall clock at the next cold start minus the time stored when the app last paused (`Prefs.lastLeftAt`: a time only, no page, no address; never written by a page). Never stamped, or the clock went back: 0, the page loads as before.
- The start-up cover ("Opening <host>" / the last-look picture) is skipped for a waiting tab (`tabs early-cover-skipped lazy start`).
- Health lines (`tabs lazy-start away=...`, `tabs lazy-wake`, counters `lazy.start`, `lazy.wake`) and a Diagnostics line say what happened. Private tabs are never restored, so none is involved.

## 5. NOT proven on the tablet
JVM tests only (core rule; source guards in OwnerRulesTest; Gecko and WebView modules compile). The first start after 4 hours away on the tablet is the proof: expect Home with "Continue: <title>", cold start to a usable screen in about a second, and the page loading only after the tap. If the owner finds it annoying: the Settings switch.
