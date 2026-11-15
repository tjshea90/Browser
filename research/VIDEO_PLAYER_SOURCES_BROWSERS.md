# R1 - Firefox and Chromium: touch video controls and full-screen video (input for VP1a)

Snapshot: 2026-11-15, `main` of both engines. Target: Kestrel G (GeckoView 160), Android 10 tablet (600x973 px @170 dpi = 564x916 dp), also Android 16.
Nothing was written to the repo. ASCII only so it can be pasted into research/VIDEO_PLAYER_AUDIT.md.

## Bottom line (details, numbers and tags below)
- Tap: Firefox toggles on EVERY tap on the picture (even paused), hide timer 5 s. Chromium toggles only while PLAYING, after a 300 ms double-tap wait, hide timer 2.5 s. In both, taps on the bar/buttons never toggle.
- Hold: Chromium never auto-hides while paused/ended/seeking/menu open/focus; Firefox touch has no such check (a paused video loses the bar after 5 s).
- Double tap: Chromium only: second tap within 300 ms, left half -10 s, right half +10 s, clamp, no chaining. Firefox has none.
- Duration: both just print `video.duration`; Infinity/NaN/0 are never shown as a number (Firefox: blank duration, scrubber follows max position; Chromium: duration label hidden, timeline hidden only if `seekable` is empty). A short MSE duration is legal per the MSE spec and neither engine filters it.
- Orientation: neither controls widget locks anything. Fenix app code (`MediaSessionFullscreenFeature`) locks by `portrait = height > width`: `USER_PORTRAIT` / `SENSOR_LANDSCAPE`, unknown `USER`, exit `USER`. Chromium (Blink delegate) locks `SENSOR_LANDSCAPE` / `SENSOR_PORTRAIT` by `videoWidth` vs `videoHeight`, relaxes to `FULL_USER` 500 ms after the device physically matches, unlocks to `USER` on exit; it excludes tablets (sw >= 600 dp) and this tablet is sw 564.
- Fit: both `object-fit: contain` in a fixed 100% x 100% box on a black backdrop, author-overridable.

## 0. How this was read, and the confidence tags

- All code was read through WebFetch (a summarising fetch: it returns quotes of at most ~125 chars and no line numbers), so references are file + function, never line numbers. Nothing was downloaded or run.
- Every key constant and function body was requested a second time as line-by-line quotes. Tags:
  - [V] verbatim lines were returned (short ones are quoted below).
  - [T] the function was transliterated in detail by the fetch tool, one read, no verbatim lines (high confidence, not quote-checked).
  - [D] from a bug, code review, spec or official docs page (not from the code itself).
  - [I] my inference. The "Kestrel should do" column is always [I].
- Chromium has no touch/gesture listener file (googlesource listings of `media_controls/` = 26 entries and `media_controls/elements/` = 85 entries show none). Touch, tap, double tap and the hide timer are all in `media_controls_impl.cc`.

## 1. Source index (all read on 2026-11-15)

Firefox (raw GitHub, repo mozilla-firefox/firefox, branch main):
- `toolkit/content/widgets/videocontrols.js` (118,608 chars; classes VideoControlsWidget, VideoControlsImplWidget with `Utils` and `TouchUtils`, NoControlsMobileImplWidget, ...)
- `toolkit/themes/shared/media/videocontrols.css`
- `layout/style/res/ua.css` (`:fullscreen:not(:root)`, `::backdrop`)
- `mobile/android/android-components/components/feature/media/src/main/java/mozilla/components/feature/media/fullscreen/MediaSessionFullscreenFeature.kt`
- `.../components/concept/engine/src/main/java/mozilla/components/concept/engine/mediasession/MediaSession.kt` (ElementMetadata.portrait)
- `.../components/feature/session/src/main/java/mozilla/components/feature/session/FullScreenFeature.kt`
- `mobile/android/fenix/app/src/main/java/org/mozilla/fenix/ext/Activity.kt` (enterToImmersiveMode), `mobile/android/fenix/app/src/main/AndroidManifest.xml`
- `dom/media/utils/MediaElementEventRunners.{h,cpp}` (timeupdate runner); bugzilla 571822, 1819593, 1819814, 1647522, 1623715 (commit 7609049a).
- GeckoView API: read with `javap` from the GeckoView 160 `classes.jar` already in the earlier session's scratchpad (MediaSession$Delegate, MediaSession$ElementMetadata, OrientationController, GeckoSession$ContentDelegate).

Chromium (raw GitHub chromium/chromium main and chromium.googlesource.com), all under `third_party/blink/renderer/` unless noted:
- `modules/media_controls/media_controls_impl.cc` (about 50,000 chars, whole file seen)
- `modules/media_controls/media_controls_orientation_lock_delegate.{cc,h}`, `media_controls_rotate_to_fullscreen_delegate.cc`, `media_controls_display_cutout_delegate.{cc,h}`, `media_controls_media_event_listener.cc`, `media_controls_shared_helper.cc`
- `modules/media_controls/elements/media_control_overlay_play_button_element.cc`, `media_control_timeline_element.cc`, `media_control_time_display_element.cc`, `media_control_animated_arrow_container_element.cc`
- `modules/media_controls/resources/mediaControls.css`, `mediaControls_Android.css`, `mediaControls_animated_arrow.css`
- `core/css/fullscreen.css`, `core/html/resources/html.css`, `core/html/media/html_media_element.cc`, `core/html/media/media_controls.cc`, `platform/runtime_enabled_features.json5`
- `content/public/android/java/src/org/chromium/content/browser/ScreenOrientationProviderImpl.java`, `ui/android/java/src/org/chromium/ui/base/DeviceFormFactor.java`
- [D] developer.chrome.com/blog/chrome-61-media-updates; blink-dev "Intent to Implement: Fullscreen Media Orientation"; codereview.chromium.org/2573983002 ("Media: do not lock the screen orientation when a video goes fullscreen on a tablet").

Other: W3C Media Source Extensions (media-source-2), developer.android.com (activity-element `screenOrientation`, device-compatibility-mode).

## 2. FIREFOX

### 2.1 Touch mode is decided by the user agent string [V]
- `let isMobile = this.window.navigator.appVersion.includes("Android");` then `this.isTouchControls = isMobile;` and classes `mobile` and `touch` on the controls container. TODO comment cites bug 1447547 (touch desktops).
- `controlsSpacer` `click` (play/pause) and `dblclick` (fullscreen) are registered with `nonTouchOnly: true`; the `mouseover/mouseout/mousemove` hide logic is skipped in touch mode [V for the two entries, T for the rest].
- [I] risk: a UA/appVersion override that loses "Android" (desktop-site mode) would give the page the mouse variant of the controls (no tap toggle). Not tested.

### 2.2 What a tap does (object `TouchUtils`, touch mode only) [V]
```
controlsTimeout: 5000
toggleControls() { if (!dynamicControls || !this.visible) showControls(); else delayHideControls(0); }
showControls()   { if (dynamicControls) { startFadeIn(controlBar); delayHideControls(this.controlsTimeout); } }
hideControls()   { if (!dynamicControls) return; startFadeOut(controlBar); }       // no paused / dragging / hover check
delayHideControls(t) { clearTimer(); controlsTimer = window.setTimeout(() => hideControls(), t); }
handleEvent:
  click   playButton : if (!video.paused) delayHideControls(0); else showControls();
  click   muteButton : delayHideControls(5000)
  touchstart (scrubber only): clearTimer()
  touchend   (scrubber only): delayHideControls(5000)
  mouseup : if (aEvent.originalTarget == Utils.controlsSpacer) { if (firstShow) { video.play(); firstShow = false; } toggleControls(); }
init():  firstShow = true only if (!autoplay && dynamicControls && paused && currentTime === 0);
         if (video.currentTime !== 0) delayHideControls(Utils.HIDE_CONTROLS_TIMEOUT_MS)   // 2000
         source comment: "we probably just transitioned into or out of fullscreen mode ... controlsTimeout is a full 5s, which feels too long after the transition."
```
- The "tap" is a `mouseup` whose target is `controlsSpacer`, the empty layer behind the bar (sibling of the big clickToPlay button inside `controlsOverlay`). [I] On Android Gecko synthesizes mouse events after a recognised tap; scroll and long press never reach it. There is no tap distance/time threshold in videocontrols.js: the engine decides tap vs scroll vs long press.
- Visible bar -> `setTimeout 0` then a 200 ms fade out. Hidden bar -> 200 ms fade in plus a 5000 ms hide timer. A tap on the bar or its buttons never toggles (originalTarget must be the spacer). `visible` is `!controlBar.hasAttribute("fadeout") && !controlBar.hidden`; the `hidden` getter is also true while the `fadeout` class is on, so a bar in mid fade-out counts as hidden [T].
- No double tap, no long press, no swipe, no pinch anywhere in the file [T, absence].
- `play` event [V]: `if (!this._triggeredByControls && this.dynamicControls && this.isTouchControls) { this.startFadeOut(this.controlBar); }` and `if (!this._triggeredByControls) { this.startFadeOut(this.clickToPlay, true); }`. So a video started by autoplay/script/page UI hides the bar at once; a video started with the bar's play button or the big play button (`startPlay()` sets `_triggeredByControls`) keeps it.
- [I] from handler order (Utils' click handler runs first and flips `paused`; the TouchUtils branch then reads `paused`): after pressing play the bar hides now; after pressing pause it shows with a 5 s timer (the source's branch names read backwards). After tapping the big clickToPlay no timer is armed, so the bar stays until the next tap.
- First tap on a never-started video (not autoplay, paused, t=0): plays it and then toggles [V code]; the bar was visible at that moment, so it is hidden [I].

### 2.3 Timers and animation numbers [V]
| constant | ms | used for |
| :- | -: | :- |
| `TouchUtils.controlsTimeout` | 5000 | touch: after tap-show, scrubber touchend, mute click, pause press |
| `Utils.HIDE_CONTROLS_TIMEOUT_MS` | 2000 | desktop: mouse idle; touch: after (re)init when currentTime != 0 (fullscreen transition); after `volumechange` |
| `Utils.SHOW_CONTROLS_TIMEOUT_MS` | 500 | desktop only: delay before re-showing a bar that a timeout hid |
| `Utils.SHOW_THROBBER_TIMEOUT_MS` | 250 | delay of the buffering throbber while resuming the video decoder |
| animationProps.controlBar | 200 | opacity 0<->1, Web Animations, `fill: "both"` |
| animationProps.clickToPlay | 400 | `scale(3) opacity 0` -> `scale(1) opacity .55` |
| animationProps.statusOverlay | 1050 | error / throbber overlay |
| `controlBarMinHeight` / `controlBarMinVisibleHeight` | 40 / 28 px | below this video height the bar is hidden (`hiddenByAdjustment`) |

### 2.4 When the bar does NOT hide / does hide
- Touch hide path checks only `dynamicControls` [V]. It does not look at paused, ended, error, buffering, seeking, `scrubber.isDragging` or hover. So a PAUSED video loses its bar 5 s after the last tap or pause press [V code, I effect].
- What keeps it up: no timer armed. Initial state (`setupNewLoadState`: `shouldShow = !dynamicControls || (video.paused && !video.autoplay)`) [V]; after `ended` (`startFadeIn(controlBar)` unconditional, no timer) [V]; after the big play button [I]; while a finger is on the scrubber (`touchstart` clears the timer, `touchend` re-arms 5 s) [V].
- `dynamicControls` is false for audio-only, attribute `mozNoDynamicControls`, and an error before the first frame [V]; then the bar is never auto-hidden (and after an early error it is hidden and not re-shown).
- Error: bar hidden only if `!firstFrameShown && !isAudioOnly`; status overlay shows the error [T]. Buffering/waiting: only the status overlay/throbber changes, never the bar [T].
- Desktop for contrast [T]: `onMouseMove` clears the timer, shows the bar (500 ms re-show delay if it was hidden by timeout) and arms a 2000 ms hide unless the pointer is over the bar; `_hideControlsFn` skips only when `scrubber.isDragging`; also no paused check.
- Entering fullscreen [V]: `onFullscreenChange` -> `if (this.isVideoInFullScreen) { this.startFadeOut(this.controlBar, true); }` (immediate, no animation).

### 2.5 Time and duration display
- Event list `videoEvents` [V]: play, pause, ended, volumechange, loadeddata, loadstart, timeupdate, progress, playing, waiting, canplay, canplaythrough, seeking, seeked, emptied, loadedmetadata, error, suspend, stalled, mozvideoonlyseekbegin/completed, durationchange.
- `durationchange` only calls `updatePictureInPictureToggleDisplay()` [T]. The bar re-reads `video.duration` on every `timeupdate`, `loadedmetadata`, `ended`; `emptied` calls `showPosition(0, 0)` [T].
- `showPosition(currentTimeMs, durationMs)` [V lines + T]:
  - `if (currentTimeMs > this.maxCurrentTimeSeen) ...` tracks the largest position.
  - `let durationIsInfinite = durationMs == Infinity;` `if (isNaN(durationMs) || durationIsInfinite) { durationMs = this.maxCurrentTimeSeen; }`
  - Label: `let duration = durationIsInfinite ? "" : this.formatTime(durationMs);` -> live (Infinity): position only, empty duration; NaN: the largest position seen is shown as the duration; 0 is kept ("0:00").
  - `let modifier = durationMs >= 3600000 ? "long" : "";` (wider boxes only).
  - Scrubber (range input) `max = durationMs`, `value = currentTimeMs` is written only when `!scrubber.max || isNaN(devPxProgress) || devPxProgress > 0.5` (thumb moved more than half a device pixel); label DOM is rewritten only when the text changed.
  - `showBuffered()` uses the same fallback to `maxCurrentTimeSeen` and binary-searches `video.buffered`.
- `formatTime`: ms -> `Math.round(ms/1000)`; `h:mm:ss` if hours, else `m:ss` (seconds always 2 digits, minutes unpadded without hours) [T].
- Absent: no "LIVE" text, scrubber not hidden or disabled for Infinity, seeks never check `video.seekable`, no rule for a very short duration (only the PiP toggle has `MIN_VIDEO_DIMENSION = 140` px and a `min-video-secs` pref) [T, absence].

### 2.6 Full screen
- Button/dblclick: `this.document.exitFullscreen()` or `this.video.requestFullscreen()` [T]; listener `fullscreenchange` on the document; `onFullscreenChange` sets attribute `inDOMFullscreen` (CSS effect: hides the PiP toggle only) [V].
- videocontrols.css has NO `:fullscreen` rule, NO `env(safe-area-inset-*)`, NO `object-fit` [V]. The bar is the last flex child of `.controlsOverlay` (column), under `.controlsSpacerStack {flex-grow:1}`; i.e. at the bottom of the video element box, not of the picture [V CSS, I for the letterbox consequence]. `.controlBar[hidden] { display:flex; opacity:0; pointer-events:none }`.
- The full-screen box comes from the UA sheet `layout/style/res/ua.css` [V]: `:fullscreen:not(:root) { -moz-top-layer:auto !important; position:fixed !important; inset:0 !important; width:100% !important; height:100% !important; margin:0 !important; min/max-width/height reset !important; box-sizing:border-box !important; object-fit: contain; transform:none !important }` and `:fullscreen:not(:root)::backdrop { background: black }`. `object-fit: contain` is not `!important` (page CSS can override).
- Orientation: nothing in videocontrols.js. The old pref `media.videocontrols.lock-video-orientation` used the deprecated `mozLockOrientation`, did nothing, and was set false on GeckoView in Firefox 113 [D bug 1819593].
- Android app layer (Fenix + android-components):
  - `MediaSessionFullscreenFeature` [V]: collects `tabs + customTabs`, picks the first with `mediaSessionState?.fullscreen == true`, `distinctUntilChanged` over (fullscreen, playbackState, elementMetadata, pictureInPictureEnabled). Orientation only when that tab is the active tab:
    `when (elementMetadata?.portrait) { true -> SCREEN_ORIENTATION_USER_PORTRAIT; false -> if (activity.isInPictureInPictureMode) SCREEN_ORIENTATION_UNSPECIFIED else SCREEN_ORIENTATION_SENSOR_LANDSCAPE; null -> SCREEN_ORIENTATION_USER }`.
    No fullscreen tab: `requestedOrientation = SCREEN_ORIENTATION_USER` and clear `FLAG_KEEP_SCREEN_ON`. While PLAYING in fullscreen: `FLAG_KEEP_SCREEN_ON`. No rotation listener, no pause, no delay.
  - `ElementMetadata.portrait` [V]: `get() = if (height == 0L && width == 0L) null else height > width` (square -> false -> landscape).
  - `FullScreenFeature` [V]: only observes `content.fullScreen` and the layoutInDisplayCutoutMode; callbacks `fullScreenChanged(Boolean)`, `viewportFitChanged(Int)`; `onBackPressed()` exits. The host app hides bars/toolbars.
  - Fenix `Activity.enterToImmersiveMode()` [V]: `FLAG_KEEP_SCREEN_ON` and legacy `systemUiVisibility = LAYOUT_HIDE_NAVIGATION | LAYOUT_FULLSCREEN | HIDE_NAVIGATION | FULLSCREEN | IMMERSIVE_STICKY` (its KDoc, as relayed by the fetch tool [T], calls it deprecated in favour of an android-components version that Fenix avoids because "the stable flag messes with the toolbar"). [I] Sticky immersive bars that the user swipes in are transient and overlay the content; nothing resizes.
  - `HomeActivity` manifest [V]: `configChanges="keyboard|keyboardHidden|mcc|mnc|orientation|screenSize|layoutDirection|smallestScreenSize|screenLayout"`, `windowSoftInputMode="adjustResize"`: rotation never recreates the activity.
  - GeckoView 160 API [V, javap]: `GeckoSession.ContentDelegate.onFullScreen(GeckoSession, boolean)`, `onMetaViewportFitChange(GeckoSession, String)`; `MediaSession.Delegate.onFullscreen(GeckoSession, MediaSession, boolean, MediaSession.ElementMetadata{source,duration,width,height,audioTrackCount,videoTrackCount})`; `OrientationController.OrientationDelegate.onOrientationLock(int)` / `onOrientationUnlock()` (page-level `screen.orientation.lock`).
  - Pitfalls [D]: GeckoView derives the media-session fullscreen event from `document.fullscreenElement` through `MediaUtils.findMediaElement`; if no media element is found it sends nothing ("Non-media element fullscreen", 2020 commit for bug 1623715; whether descendants are searched was not seen). It is also skipped when the media controller is not yet "activated" (bug 1819814, fixed in Fx 113, follow-up 1827583). A wrapper-div fullscreen (Shaka/evp-player) may therefore never reach `onFullscreen`.

### 2.7 Performance choices
- UA-widget shadow DOM, created once per `<video controls>`, never removed on hide; fades use Web Animations with one cached `Animation` per element (`animationMap`); the CSS has no transition/animation/`will-change` [V]. (The TouchUtils comment suggests the widget is re-initialised around fullscreen transitions [I].)
- No timeupdate throttle in the widget; the engine already limits `timeupdate`: `nsTimeupdateRunner` drops non-mandatory events younger than `TIMEUPDATE_MS` (250 ms, bug 571822 patch; constant's current definition line not re-read) [D/T]. In the handler `timeUpdateCount <= 2` gates `setupStatusFader()`, and `scrubber.isDragging` returns before any update [V].
- No per-frame work. One `requestAnimationFrame` + `setTimeout` exists, in the resize path only; resize uses the `resizevideocontrols` event, not ResizeObserver (a FIXME says "ResizeObserver, probably") [T].
- Not done: no skipping of `showPosition/showBuffered` while the bar is hidden [T, absence].
- Scrubbing [V]: every `input` event sets `video.currentTime` at once (no throttle, no `fastSeek`); the video is paused while dragging (`pauseVideoDuringDragging`) and resumed on `change`/`mouseup`.

### 2.8 Touch sizes (videocontrols.css) [V]
`.controlsContainer.touch`: `--clickToPlay-size:64px; --button-size:40px; --timer-size:52px; --timer-long-size:78px; --track-size:7px; --thumb-size:16px; --label-font-size:16px`. `.controlBar` height 40px, `.touch .controlBar` 52px, padding `0 9px`, background `rgba(26,26,26,0.8)`; `--scrubberStack-width:84px` and `--volumeStack-width:64px` on touch. `.clickToPlay`: round, `#1a1a1a`, opacity .8, `top:20px` (20 px below centre), play icon 40%.

## 3. CHROMIUM (Blink media controls)

### 3.1 Tap, double tap, overlay button [V]
Constants in `media_controls_impl.cc`:
`kMinWidthForOverlayPlayButton = 72`, `kMinScrubbingMessageWidth = 300`, `kAudioButtonPadding = 20`, `kVideoButtonPadding = 26`, `kDoubleTapDelay = base::Milliseconds(300)`, `kTimeToShowVolumeSlider = 200 ms`, `kNumberOfSecondsToJump = 10`, `kTimeWithoutMouseMovementBeforeHidingMediaControls = base::Seconds(2.5)`.
```
HandleTouchEvent(event):
  is_mouse_over_controls_ = false; is_touch_interaction_ = true;
  if (event->type() == kGesturetap && !ContainsRelatedTarget(event)) {
    event->SetDefaultHandled();
    if (tap_timer_.IsActive()) {            // second tap inside 300 ms
      tap_timer_.Stop();                    // "Cancel the visibility toggle event."
      if (IsOnLeftSide(event)) MaybeJump(-10); else MaybeJump(+10);
    } else tap_timer_.StartOneShot(kDoubleTapDelay);
  }
TapTimerFired(): if (is_touch_interaction_) MaybeToggleControlsFromTap(); else if (paused) {mouse path}
MaybeToggleControlsFromTap():
  if (paused) return;                       // taps do nothing while paused
  if (IsVisible()) MakeTransparent();
  else { MakeOpaque();
         if (ShouldHideMediaControls(kIgnoreWaitForTimer | kIgnoreFocus)) { keep_showing_until_timer_fires_ = true; StartHideMediaControlsTimer(); } }
MaybeJump(s): new = max(0, currentTime + s); new = min(new, duration); if (!CanSeekTo(new)) return;
              setCurrentTime(new); EnsureAnimatedArrowContainer(); arrow left/right animation
```
- Touch input is the compositor `gesturetap` event (not touchend/click). `IsTouchEvent` = PointerEvent touch/pen, TouchEvent, GestureEvent, or a mouse event `FromTouch()`. `is_touch_interaction_` flips back to false when a native mouse/pointer event arrives (hybrid devices).
- Zones: only left half / right half: `IsOnLeftSide` is `tap_x < rect.x + rect.width/2` (exact 50/50, no centre zone, no fractions). Seek is 10 s, clamped to [0, duration] and to `CanSeekTo`. A pair of taps = one jump; tap-tap-tap does not chain: the third tap finds the timer stopped, starts a new 300 ms timer and resolves as a toggle [I, read off the V code]. No check for paused, fullscreen or controls-visible before the seek [V].
- Every single tap is delayed by 300 ms before the toggle (it has to rule out a double tap).
- Mouse path (not touch): `click` -> single click pauses at once, second click inside 300 ms toggles fullscreen and resumes (`is_paused_for_double_tap_`).
- Long press: none in the controls code [T, absence].
- Overlay (big centre) play button [V `media_control_overlay_play_button_element.cc`, CSS]: handles `click` and `gesturetap`, `SetDefaultHandled()`, `TogglePlayState()` (reloads first if errored plain `src`), then `if (!paused) SetIsDisplayed(false)`. Inner size default 56 px, CSS `72px x 72px`, padding 20px, centred with `left/top:50%` and margins -56/-68 px, fades `opacity .25s`. Shown by `MakeOpaque`, hidden by `.state-scrubbing` (display:none), dimmed `.3` when no source. It is drawn only if it fits (`width >= 72`, height left after bar/timeline >= its height) [T]; when it fits, the bar's own play button is dropped (`play_button.IsWanted = !overlay || !overlay.DoesFit`) [T]: touch layout = big centre button + bar with time, timeline, mute, fullscreen.
- Double-tap feedback [V CSS/T]: arrows 54x36 px, `700ms arrow-pulse cubic-bezier(.9,0,.2,1)`, three arrows staggered 67 ms, opacity 0 -> .6 (at 42%) -> 0; repeated `Show()` increments a counter that drains one per animation iteration; no "10 seconds" text.
- Taps on the panel buttons: `ContainsRelatedTarget` only inspects `PointerEvent.relatedTarget`, so for a gesture event it never blocks; whatever keeps a bar-button tap from toggling must be in the button classes (overlay button marks the event handled [V]; the other buttons were not read).

### 3.2 Auto-hide and what keeps the controls up
- Delay: 2.5 s [V]; `GetTimeWithoutMouseMovementBeforeHidingMediaControls()` has no branches (no a11y/test override); a comment says to keep web_tests/media/media-controls.js in sync.
- Timer is started by [T]: `OnPlaying`, `OnEnteredFullscreen`, `OnExitedFullscreen` (stop+start), tap-show, `OnAccessibleFocus`, `OnSeeked` (`StartHideMediaControlsIfNecessary`), `ResetHideMediaControlsTimer` (focusin/input/panel keypress; restarts only if not paused). Stopped by `OnPause` and `OnSeeking` [V].
- `HideMediaControlsTimerFired` [T]: clears `keep_showing_until_timer_fires_`; `if (paused) return; if (!ShouldHideMediaControls(flags)) return; MakeTransparent();`.
- `ShouldHideMediaControls` returns false (keep visible) [T] for: not a video / no video track; remote playback active; `keep_showing_until_timer_fires_` (unless ignored); pointer over bar/timeline (ignored for touch); pointer over the video; focus on the element or inside the controls (keyboard focus cannot be ignored); text-track, playback-speed or overflow menu open; accessibility keep-displayed; `MediaElement().seeking()`.
- Paused [V]: `OnPause` -> `MakeOpaque(); StopHideMediaControlsTimer();`; `OnTimeUpdate`: "'timeupdate' might be called in a paused state. The controls should not become transparent in that case." -> `if (paused) { MakeOpaque(); return; }`. Ended: `pause` fires before `ended` per HTML spec, so the same path [I]. Error: `OnError -> Reset()` pauses the element on error [T]. Buffering: only CSS class `state-buffering` and the loading panel; no effect on hiding except `seeking()`.
- `OnTimeUpdate` (every 250 ms while playing) [V]: `if (IsVisible() && ShouldHideMediaControls()) MakeTransparent();` -> visible controls that nothing holds are hidden at the next timeupdate; the tap-show flag and the 2.5 s timer are what hold a tap-shown bar.
- `OnSeeking` [V]: `MaybeShow(); StopHideMediaControlsTimer();` (seeks, including double-tap seeks, show the controls); `OnSeeked`: `StartHideMediaControlsIfNecessary()`.
- Entering fullscreen keeps the controls and restarts the 2.5 s timer [V] (Firefox hides at once).
- Fade CSS [V]: panel `opacity .25s cubic-bezier(.25,.1,.25,1)`; hidden panel (`.transparent`) `opacity:0` with `transition: opacity 1s`; button panel and timeline `.5s` shown / `.75s` hidden; `.transparent` has no `pointer-events:none`.

### 3.3 Orientation lock, rotate-to-fullscreen, display cutout
- Gates in `MediaControlsImpl::Create` [V]: `RuntimeEnabledFeatures::VideoFullscreenOrientationLockEnabled()`, `VideoRotateToFullscreenEnabled()`, and `MediaControlsDisplayCutoutDelegate::IsEnabled()` (= `DisplayCutoutAPIEnabled() && MediaControlsExpandGestureEnabled()`), each only for `HTMLVideoElement`. The two Video* flags have no status in `runtime_enabled_features.json5` (switched on from the content layer) [V]. Android only [D blink-dev intent]. Tablets: CL 2573983002 (Dec 2016) "do not lock the screen orientation when a video goes fullscreen on a tablet" (`render_view_host_impl.cc`, "Ignore the feature being enabled if the Android device form factor is a tablet") [D, not re-verified in today's tree]; Chrome 61 post says rotate-to-fullscreen is phone only [D]. Chromium's tablet test = smallest width >= 600 dp (`DeviceFormFactor.java`, res `values-sw600dp`) [V]; the Kestrel tablet is sw 564 dp, so Chromium would treat it as a PHONE [I].
- Lock on entering fullscreen [`media_controls_orientation_lock_delegate.cc`; `MaybeLockOrientation` and `ComputeOrientationLock` are V, the event wiring is T]: runs on `fullscreenchange` when `VideoElement().IsFullscreen()` (the `<video>` itself must be the fullscreen element; wrapper-div fullscreen is not covered). State `kPendingFullscreen`; if `readyState == HAVE_NOTHING` -> `kPendingMetadata` and retry on `loadedmetadata`. Skipped when the page already holds a lock (`controller->MaybeHasActiveLock()`).
  - Rule (`ComputeOrientationLock`): `videoWidth > videoHeight` -> LANDSCAPE; `videoHeight > videoWidth` -> PORTRAIT; square -> current screen orientation (portrait types -> PORTRAIT, landscape types -> LANDSCAPE, undefined -> LANDSCAPE). No aspect-ratio threshold.
  - Android mapping [V `ScreenOrientationProviderImpl.java`]: LANDSCAPE -> `SCREEN_ORIENTATION_SENSOR_LANDSCAPE`, PORTRAIT -> `SENSOR_PORTRAIT`, ANY -> `FULL_USER`, DEFAULT/unlock -> `SCREEN_ORIENTATION_USER` (falls back to the manifest value). Requests are queued while the activity is in a delayed state (`mDelayedRequests`), no timer.
- Release [V/T]: unlock only when fullscreen exits (`MaybeUnlockOrientation`: `controller.unlock()`, state back to `kPendingFullscreen`). User rotation does not unlock. In between: if the user's auto-rotate is ON (Android only: `IsAutoRotateEnabledByUser` over mojo; other OS assume true) and `VideoRotateToFullscreen` is enabled, it listens to `deviceorientation`; when the PHYSICAL orientation equals the video's it waits `kLockToAnyDelay = base::Milliseconds(500)` [V, .h] and relocks to ANY (`FULL_USER`) so the user can rotate back (the code comment, as relayed by the fetch tool, says DEFAULT is avoided because a manifest-set orientation could exit fullscreen at once) [T]. Physical orientation from beta/gamma: `kMinElevationAngle = 24` deg from the horizontal plane (flatter = unknown), portrait/landscape zones are within 23 deg of 0/90/180/270 (46 deg wide, 44 deg diagonal dead zones) [V]. If auto-rotate is OFF the lock simply holds until fullscreen exit.
- Rotate-to-fullscreen [T, `media_controls_rotate_to_fullscreen_delegate.cc`]: constants `kMinVideoSize = 200` (px, both sides) and `kIntersectionThreshold = 0.75` (strict >, IntersectionObserver, only while playing and not fullscreen). Gates: not PiP, native controls shown, no `nofullscreen` controlslist, a `deviceorientation` probe worked (beta and gamma not both 0), no other fullscreen element, metadata loaded, screen orientation changed between two known values (180 deg flips ignored). Rule: screen orientation equals the video's class (width >= height = landscape) -> `EnterFullscreen()`; no longer equal while fullscreen -> `ExitFullscreen()`. Neither delegate pauses the video.
- Display cutout [V lines + T]: on entering fullscreen `SetExpandIntoDisplayCutout(true)`, on exit `(false)`; exactly-two-finger touch listeners in fullscreen: finger distance growing -> expand true, shrinking -> false (raw pixel distance, no threshold). CSS [V]: `video::-webkit-media-controls { margin-top/left/right/bottom: env(safe-area-inset-top/left/right/bottom) }`, plus a cutout button at `top:4px; right:4px`.
- Controls in fullscreen without `controls`: `HTMLMediaElement::ShouldShowControls()` is true for `controls` attribute OR `IsFullscreen()` [V]: a `<video>` that is itself fullscreen gets native controls even if the page set none.

### 3.4 Duration, live, MSE
- `OnDurationChange` [V]: `duration_display_->SetCurrentValue(duration); duration_display_->SetIsWanted(std::isfinite(duration) || (ShouldShowAudioControls() && State() == kNoSource)); UpdateCurrentTimeDisplay(); timeline_->SetDuration(duration);` The duration LABEL is event-driven (only `durationchange` updates it); the timeline bar reads `MediaElement().duration()` again on every render, i.e. each timeupdate [T].
- `IsLivePlayback()` [V]: `return MediaElement().seekable()->length() == 0 && (State() >= kStopped);` `UpdateCurrentTimeDisplay`: `timeline_->SetIsWanted(!IsLivePlayback());` So: no duration number unless finite; no timeline only when there is nothing seekable.
- Timeline `SetDuration` [T]: `is_live_ = isinf(duration)`, `max = is_live ? 0 : duration`. For infinite duration WITH a seekable window it builds a DVR window: min = `seekable.start(last)`, max = anchored `seekable.end(last)` (anchor = wall clock + media time), re-rendered by a 1 s repeating timer only while paused (`kRenderTimelineInterval = 1 s`). `RenderBarSegments` draws nothing if `!isfinite(duration) || duration == 0 || isnan(current)`. NaN is not special-cased in `SetDuration`.
- `FormatTime` [V]: non-finite -> 0; `m:ss` or `h:mm:ss`, seconds 2 digits, fraction truncated, "-" sign for negatives. No "LIVE" string was seen in the files read.
- Seek clamps `min(new_time, duration)` and `CanSeekTo`; nothing detects a transient short MSE duration. No short-duration rule.
- Update rate: `timeupdate` periodic every `kMaxTimeupdateEventFrequency = 250 ms` ("the spec says 15 to 250ms, we choose the slowest") [V `html_media_element.cc`].

### 3.5 Fit / stretch in fullscreen [V]
- `html.css`: `video { object-fit: contain; }`.
- `core/css/fullscreen.css` `:not(:root):fullscreen, :xr-overlay { position:fixed !important; width:100% !important; height:100% !important; transform:none !important; object-fit: contain; /* intentionally not !important */ ...}`, `:not(:root):fullscreen::backdrop { position:fixed; background: black; }`; `:-internal-video-persistent { position:fixed !important; width/height:100% !important; transform:none !important; background-color:black !important; z-index:2147483647 !important }`.
- Result: letterbox on black, author CSS may change `object-fit`. The only "fill the screen" gesture is the cutout pinch (it changes the cutout inset, not the crop).

### 3.6 Performance choices
- `MediaControlsMediaEventListener` subscribes to volumechange, focusin, timeupdate, play, playing, pause, durationchange, seeking, seeked, error, loadedmetadata, waiting, progress, loadeddata, key events, pointer move/out/enter, (webkit)fullscreenchange, PiP events and track lists. No timers, no `requestAnimationFrame` [V].
- Size changes: `ResizeObserver` on the media element -> `NotifyElementSizeChanged` -> `UpdateSizingCSSClass()` + a ZERO-delay one-shot timer -> `ComputeWhichControlsFit()` + `timeline_->RenderBarSegments()`; skipped if the size is unchanged [V]. Source comment: "this code permits a bad frame on resize, since it is run after the relayout / paint happens" [V].
- Sizing classes `MediaControls::GetSizingClass`: width < 741 -> small, < 1441 -> medium, else large (`kMediaControlsSizingMediumThreshold = 741`, `Large = 1441`) [V]; video treats small and medium alike.
- No work skipped while the panel is merely transparent: `OnTimeUpdate` always calls `UpdateTimeIndicators` (timeline + time text); the only gate is `UpdateCurrentTimeDisplay`: `if (panel_->IsWanted())`, i.e. controls enabled, not faded out [V]; `Hide()` (panel not wanted) is the state that stops it [T]. Fades are CSS transitions (compositor); `contain: paint !important` on the controls root; no `will-change`.
- Scrubbing [T]: `BeginScrubbing` pauses a playing video (`is_paused_for_scrubbing_`) and resumes in `EndScrubbing`; seeks are issued on every `input`/`touchmove` (no throttle, no seek timer); touchmove maps `clientX / TrackWidth()` of the first of exactly one touch to a time, so a drag still works when the finger leaves the track vertically.

### 3.7 Touch sizes (`mediaControls.css`, small class = width < 741) [V]
`.sizing-small` buttons 48x48 (min-width 48), button panel height 48 with `padding: 0 0 0 16px`, timeline track 4px high with `padding: 0 16px 20px 16px`, slider thumb 12px (Android CSS forces the thumb always visible, `opacity:1`), time text 14px, base font 12px, overlay button inner 72px + 20px padding. `.sizing-large` (>= 1441): 64px buttons.

## 4. MSE facts behind "the film is 0:04 long" [D, W3C media-source-2]
- `MediaSource.duration` is NaN when the object is created; an initialization segment without a duration sets +Infinity (5.5.7); coded-frame processing step 5: "If the media segment contains data beyond the current duration, then run the duration change algorithm with new duration set to the maximum of the current duration and [[group end timestamp]]"; the setter note says the duration is raised if buffered data ends later than the value set.
- So `video.duration` of an MSE player legitimately passes NaN -> Infinity -> a small value that only equals what has been appended (for example one 4 s segment) -> the real value once the page sets `mediaSource.duration`. Neither engine filters this: Chromium re-renders on every `durationchange`, Firefox re-reads on every `timeupdate`; both print what the element reports. A guard has to be Kestrel's own [I].

## 5. Android platform facts that matter for the lock [D, developer.android.com]
- `sensorLandscape` / `sensorPortrait`: "The sensor is used even if the user has locked sensor-based rotation." `user`: the user's current preferred orientation. `userPortrait`/`userLandscape`: sensor and the user's preference. `fullUser`: same as `user` if the user locked rotation, else same as `fullSensor`. `locked`: current rotation.
- Android 16 (API 36): for apps targeting 36 on screens with smallest width >= 600 dp the system ignores `screenOrientation` values (portrait, landscape, reverse*, sensor*, user*) and `setRequestedOrientation()`; an opt-out property (`PROPERTY_COMPAT_ALLOW_RESTRICTED_RESIZABILITY`) exists for API 36 only and is removed for apps targeting API 37. The Android 10 tablet (sw 564) and an Android 16 phone are not affected; an Android 16 device with sw >= 600 dp would be.

## 6. Behaviour table

| behaviour | Firefox (videocontrols.js touch mode + Fenix) | Chromium (Blink media controls) | Kestrel should do [I] |
| :- | :- | :- | :- |
| Tap toggles controls | YES [V]: `mouseup` on the empty spacer; visible -> hide (timer 0 + 200 ms fade), hidden -> fade in 200 ms + 5000 ms timer; no paused check; tap on bar never toggles; first tap on a never-started video also plays | YES [V]: `gesturetap`, resolved after 300 ms; playing: visible -> transparent (1 s CSS fade), hidden -> opaque + `keep_showing_until_timer_fires_` + 2.5 s timer; PAUSED: tap ignored (bar stays) | Toggle on every tap on the picture, also while paused (owner wants it); taps on bar/buttons/slider never toggle; show at once when hidden; if double-tap seek exists, delay only the HIDE by 300 ms |
| Auto-hide delay | 5000 ms touch [V]; 2000 ms after a fullscreen transition and on desktop | 2500 ms [V], also after `playing`, entering/leaving fullscreen, `seeked` | About 3000 ms while playing (VP2); re-arm on any bar touch; hold during seeking |
| Paused keeps controls | NO in touch [V code]: hides 5 s after the last tap even when paused; `ended` fades the bar in and arms nothing | YES [V]: timer callback returns when paused; `OnPause` -> opaque; timeupdate while paused -> opaque; ended = paused | Never AUTO-hide while paused, ended, error, scrubbing, seeking (Chromium); show on pause/ended/error; a user tap may still hide |
| Double-tap seek | none [T]: dblclick/click on spacer are desktop only; no gesture code | YES [V]: 2nd tap within 300 ms, left half -10 s, right half +10 s, no centre zone, clamp [0,duration] + `CanSeekTo`, arrow pulse 700 ms, no paused/fullscreen/visibility gate, no chaining | Same rule: halves, 10 s, 300 ms window, `seekable` clamp, brief arrow/"10 s" feedback; show bar after the seek (Chromium does via `OnSeeking`) |
| Duration / live | Infinity: position only, empty duration, scrubber max grows with the max position seen; NaN: shows max position as duration; 0 kept; no LIVE text; `durationchange` ignored (re-read every timeupdate) [V/T] | Duration shown only if `isfinite`; timeline hidden only when `seekable.length == 0` (live) else DVR window `seekable.start..end`; `FormatTime` non-finite -> 0:00; re-render on `durationchange` [V]; no LIVE text seen | Never print a duration that is not finite and > 0; Infinity without seekable window -> "LIVE" (own badge); Infinity with window -> seekable range; re-read on `durationchange` AND timeupdate; MSE transient (NaN/Infinity/first segment) needs own guard: show "--:--" until readyState >= 1 and the value stayed unchanged for a short settle time (about 1 s) |
| Orientation on fullscreen entry | Not in videocontrols.js [V]. Fenix `MediaSessionFullscreenFeature`: `portrait` (h > w) -> `USER_PORTRAIT`; not portrait -> `SENSOR_LANDSCAPE`; unknown (0x0) -> `USER`; exit -> `USER`; keep screen on while playing [V]. Only if GeckoView sends `onFullscreen` (not for non-media/wrapper fullscreen, race bug 1819814) [D] | `<video>`-is-fullscreen only [V/T]: lock when metadata known: w > h landscape, h > w portrait, square = current screen; `SENSOR_LANDSCAPE` / `SENSOR_PORTRAIT`; skipped if page has a lock; after physical orientation matches (24/23 deg rule) wait 500 ms, relock `FULL_USER`; unlock -> `USER` on exit; phones only [D] | Own detection (do not rely on `MediaSession.onFullscreen`): on `onFullScreen(true)` find the playing/fullscreen video, read `videoWidth/videoHeight` (not the box); h > w -> `USER_PORTRAIT`, else `SENSOR_LANDSCAPE`; unknown size -> `USER`, decide on `loadedmetadata`; once the screen matches relax to `FULL_USER` (Chromium, 500 ms); exit -> `USER`; rotate button = toggle `SENSOR_LANDSCAPE` <-> `USER_PORTRAIT`; no pause is done by either engine, so Kestrel's pause/cover during the turn is extra; Activity must keep `configChanges` (Fenix does) |
| Fit (contain vs fill) | `contain` [V]: UA `:fullscreen:not(:root)` fixed, inset 0, 100% x 100% `!important`, `object-fit: contain` NOT important, `::backdrop` black; controls CSS adds no fit rule | `contain` [V]: `html.css video {object-fit: contain}` + fullscreen.css same box, `object-fit: contain` "intentionally not !important", backdrop black | `contain`, letterboxed on black; do not size the video in Kestrel code; the box must be the whole window before the first frame; if the page forces `fill`/`cover` and the aspect differs, override with `contain !important` only for the fullscreen video; check the stretched-until-rotate case against the page-vs-view viewport lag (cause not established here) |
| Bar position / insets | bottom of the video box; 52 px touch, buttons 40, big play 64; no `env()` | bottom of the video box; 48 px small class; root margins `env(safe-area-inset-*)` | Bottom of the visible picture box inside the safe insets of the window; 48 px targets; recompute on `ResizeObserver` + `fullscreenchange`, not by 250 ms polling |
| Work while hidden | not skipped [T] | not skipped [V/T] | Detach timeupdate/resize listeners and timers while hidden (better than both) |

## 7. Mapping to the open tasks
- VP2: tap toggles (Firefox rule for the toggle, Chromium rule for the hold conditions), 3 s while playing, double-tap 10 s (Chromium numbers), controls return on pause/ended/error.
- VP3/VP4: both engines keep the controls inside the video box and rely on the system bars staying hidden (sticky immersive overlays them transiently) [I]; Chromium pads by `env(safe-area-inset-*)` [V]. Neither engine has a "pull the nav bar up" remedy that I found [I]. Chromium's controls are not rebuilt on orientation change: one shadow tree, `ResizeObserver` recompute [T].
- VP5: see section 4 and the Duration row; both engines print `video.duration` as is.
- VP6: both engines use `object-fit: contain` on a fixed 100% x 100% box; neither sets size from script.
- VP7: neither engine pauses on rotation; Fenix avoids activity recreation with `configChanges`, Chromium accepts one stale frame ("permits a bad frame on resize").
- VP8/VP9: engines do 4 Hz event-driven updates, no per-frame work, CSS/Web Animations fades; they do not stop work while faded out.

## 8. Not verified / watch out
- Chromium panel buttons: how a tap on them avoids the toggle (only the overlay button was read). Chromium tablet exclusion is from a 2016 CL, not re-read in current code. `findMediaElement` descendant search in GeckoView not seen. `ElementMetadata.width/height` source (videoWidth vs layout size) not seen; the Javadoc says "device pixels".
- Firefox `TIMEUPDATE_MS` value is from the 2010 bug patch (250); only its use was read in current code.
- Nothing here was run on a device or emulator.
