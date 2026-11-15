# R2 - Touch video-player UIs: what proven players actually do

Researched 2026-11-15 for Kestrel (GeckoView, Android 10 tablet 600x973 @170 dpi, 5 Mbit/s hotspot).
Method: source files and official docs read through WebFetch (it returns a small-model digest with short quotes, so every
number below was returned with a quoted line; the ones that matter most were cross-checked in a second file).
Tags: [V] read in source / official doc this session. [O] third-party report or user-visible behaviour, not a primary doc.
[I] my inference. "x2" = confirmed in two independent places. Everything is ASCII; no code was run.

---------------------------------------------------------------------------------------------------------------------

## 1. AndroidX Media3 (PlayerView, PlayerControlView, DefaultTimeBar)
Files (branch `release`): github.com/androidx/media/blob/release/libraries/ui/src/main/java/androidx/media3/ui/
`PlayerView.java`, `PlayerControlView.java`, `PlayerControlViewLayoutManager.java`, `DefaultTimeBar.java`;
`.../res/values/dimens.xml`, `styles.xml`; `libraries/common/.../C.java`. Docs: developer.android.com/media/media3/ui/playerview,
/ui/surface, reference/androidx/media3/ui/PlayerView (docs page shows media3-ui 1.11.1).

PlayerView [V]
- Defaults: `controllerAutoShow=true` (attr auto_show), `controllerHideOnTouch=true` (hide_on_touch),
  `controllerShowTimeoutMs=PlayerControlView.DEFAULT_SHOW_TIMEOUT_MS=5000` (attr show_timeout; 0 = never auto-hide),
  `use_controller=true`, `hide_during_ads=true`, `show_buffering=never`, `keep_content_on_player_reset=false`.
- Tap: the surface and the PlayerView are plain click targets (`setClickable(true)`; surface forwards via click listener).
  `performClick()` -> `toggleControllerVisibility()`: not fully visible -> show; visible and hide_on_touch -> hide.
  There is NO gesture code at all (no GestureDetector, no onTouchEvent, no double-tap, no long-press).
- Stays visible: `shouldShowControllerIndefinitely()` = controllerAutoShow && timeline not empty && (state IDLE || ENDED ||
  !playWhenReady) -> `controller.setShowTimeoutMs(0)`. So paused / ended / idle(+failed) = shown until the user acts; playing = 5 s.
  Javadoc: "Controls are always shown during ads if they are enabled and the player is paused."
- Playback state hooks: onPlaybackStateChanged / onPlayWhenReadyChanged -> `updateControllerVisibility()` -> `maybeShowController(false)`.
  Buffering: no controller change, only the spinner (default never). Error: no controller branch; the state falls to IDLE so the
  controller is shown indefinitely (code reading, [I]). onPositionDiscontinuity into an ad + hide_during_ads -> hide.
- No WindowInsets / fitsSystemWindows / orientation / configuration code in PlayerView.java or PlayerControlView.java [V].
  Fullscreen is only a callback (`setFullscreenButtonClickListener`; the app does immersive mode itself).

Hide timer (PlayerControlViewLayoutManager) [V]
- show(): visible + `resetHideCallbacks()`. If showTimeoutMs > 0: after the timeout the MAIN bars fade out (250 ms), the thin
  progress bar stays (UX_STATE_ONLY_PROGRESS_VISIBLE) and hides 2000 ms later (ANIMATION_INTERVAL_MS=2000). Show anim 250 ms.
  So full hide = 5000 + 250 + 2000 ms. The timer itself ignores playing/scrubbing; callers handle it:
  `onScrubStart -> removeHideCallbacks()`, `onScrubStop -> resetHideCallbacks()`, every button `onClick -> resetHideCallbacks()`.
- Resize/orientation: an OnLayoutChangeListener recomputes "minimal mode" on every layout change (width <= max(centre-controls
  width, time+overflow width) or height <= centre-controls height + 2 x bottom-bar height; no fixed dp thresholds); minimal mode hides
  bottom bar, prev/next/rew/ffwd; controls that do not fit move to an overflow menu. Visible/hidden state is not touched by a resize.
  No explicit orientation check.
- Demo manifest: PlayerActivity has `configChanges="keyboard|keyboardHidden|orientation|screenSize|screenLayout|smallestScreenSize|uiMode"`,
  `launchMode=singleTop` (no recreation on rotation) [V] (demos/main/src/main/AndroidManifest.xml).

Seek increments [V] `C.java`: DEFAULT_SEEK_BACK_INCREMENT_MS=5_000, DEFAULT_SEEK_FORWARD_INCREMENT_MS=15_000 (ExoPlayer defaults; the
rewind/ffwd buttons call `player.seekBack()/seekForward()`; ffwd is skipped in STATE_ENDED), DEFAULT_MAX_SEEK_TO_PREVIOUS_POSITION_MS=3_000.
There are no rewind_increment / fastforward_increment attrs any more.

DefaultTimeBar [V]
- Class defaults: bar 4 dp, touch target 26 dp, scrubber 12 dp enabled / 0 dp disabled / 16 dp dragged, ad marker 4 dp,
  FINE_SCRUB_Y_THRESHOLD_DP=-50, FINE_SCRUB_RATIO=3, STOP_SCRUBBING_TIMEOUT_MS=1000 (d-pad only), DEFAULT_INCREMENT_COUNT=20.
- Shipped controller (style ExoStyledControls.TimeBar + dimens.xml): bar 2 dp, touch target 48 dp, layout height 48 dp, bar gravity
  bottom, thumb 10 dp enabled / 14 dp dragged, progress margin_bottom 52 dp, bottom bar 60 dp, centre buttons exo_icon_size 52 dp
  (padding 2 dp), small buttons 48x48 dp, centre-controls padding 24 dp.
- Touch: hit-test = whole `seekBounds` (touch-target strip, vertically centred/bottom per gravity), not just the thin bar.
  ACTION_DOWN inside it: scrubber jumps to the finger at once (no slop), `startScrubbing`, `requestDisallowInterceptTouchEvent(true)`,
  `onScrubStart`. MOVE: follows x; if finger y is more than 50 dp ABOVE the strip -> fine scrub (scrubber moves dx/3).
  UP -> `onScrubStop(canceled=false)`; CANCEL -> canceled=true. So a plain tap on the bar is a seek. Dragged thumb size applies
  at draw time (no animation). `onTouchEvent` returns false when disabled or duration <= 0 (unknown/live = not scrubbable).
- Controller side: default seek happens ONLY on release (`onScrubStop`); `time_bar_scrubbing_enabled` (default false) switches on
  ExoPlayer scrubbing mode = seek while dragging (buffered bar hidden meanwhile). The controller never pauses playback for a scrub.
  Position text follows the finger during the scrub. No thumbnail/preview API in DefaultTimeBar.
- Progress poll: min(timeBar.getPreferredUpdateDelay()=duration/barWidthDp, ms to next full second) / playbackSpeed, clamped to
  200..1000 ms (DEFAULT_TIME_BAR_MIN_UPDATE_INTERVAL_MS=200, MAX_UPDATE_INTERVAL_MS=1000); polling stops in ENDED/IDLE.
- Duration/live: `durationUs` starts 0; unknown content duration (C.TIME_UNSET) leaves it 0 -> duration text formats 0 ms, time bar
  duration 0 -> bar untouchable. Ad groups -> 4 dp markers; position/duration shown are CONTENT values (frozen at the ad cue).
- Surface doc: SurfaceView preferred (lower power, accurate frame timing); known API 34 problem "stretched/cropped/leaked Surface" when
  PlayerView sits in Compose AndroidView.

---------------------------------------------------------------------------------------------------------------------

## 2. Shaka Player UI (main)
Files: github.com/shaka-project/shaka-player/blob/main/ui/{ui.js,controls.js,seek_bar.js,presentation_time.js,hidden_seek_button.js,
hidden_fast_forward_button.js,externs/ui.js,less/containers.less,less/range_elements.less}, lib/media/streaming_engine.js.

Config defaults [V x2: externs/ui.js docs + ui.js defaultConfig_]
- `fadeDelay` 0 s (3 on cast) = "delay before fading out the controls once the user stops interacting"; the REAL idle time is the
  3 s `mouseStillTimer_` (restarted on touchend / mouse stop; touchstart/touchmove stop it) + fadeDelay. CSS comment: fade takes 600 ms.
- `doubleClickForFullscreen` true (false cast/TV); `singleClickForPlayAndPause` true but FALSE on mobile/cast/TV;
  `seekOnTaps` false, TRUE on mobile (elsewhere `navigator.maxTouchPoints > 0`); `tapSeekDistance` 10 s; `showUIOnPaused` true;
  `showUIAlways` false; `closeMenusDelay` 2 s; `showBufferingSpinner` true except mobile/TV; `enableTooltips` false on mobile;
  `enableFullscreenOnRotation` false -> true on mobile (not WEBKIT); `forceLandscapeOnFullscreen` true on mobile;
  `refreshTickInSeconds` 0.125; `keyboardSeekDistance` 5; `keyboardLargeSeekDistance` 60. Platform comes from DeviceFactory
  (isMobile/isCast/isSmartTV); there is no ontouchstart sniffing except maxTouchPoints.
- Touch vs mouse (`onMouseMove_`): touchstart/touchmove/touchend/keyup stamp `lastTouchEventTime_`; mouse events arriving within
  1000 ms of a touch are ignored (emulated mouse). `onMouseLeave_` ignored while a touch stamp exists.
- Keeps controls shown (`computeOpacity`): ad paused; video paused && showUIOnPaused && not seeking && no linear ad; recent movement;
  keyboard navigation; hover on show-controls elements. Otherwise `fadeControlsTimer_.tickAfter(fadeDelay)`.

Tap logic (`onContainerTouch`, `onContainerClick`) [V]
- Ignored entirely while `!video.duration` ("Can't play yet").
- Controls hidden -> a tap only SHOWS them (`onMouseMove_` + `resetLastTouchEventTime`; no play/pause). Controls shown -> `onContainerClick(true)`:
  second tap within `doubleTapTime = 1000 ms` toggles fullscreen (if enabled/supported); single tap: play/pause only if
  `singleClickForPlayAndPause` (false on mobile), else HIDE the UI after 1 s (waits for a possible double tap; 0 ms if no fullscreen).
  Touches on the bottom/top panels stopPropagation (never reach this path). Long press that opened a context menu: touchend swallowed.
- Tap seek (hidden_seek_button.js + containers.less): two invisible full-height containers, REWIND = left 0..40 % of the video width,
  FAST-FORWARD = 60..100 % (`width:40%; left:60%`); middle 20 % is not a seek zone. Listens touchstart/move/end only (stopPropagation).
  SCROLL_THRESHOLD_=10 px (moved more = ignored), DOUBLE_TAP_WINDOW_=0.5 s. Each tap inside the window ADDS tapSeekDistance (10 s) to a
  label ("10s", "20s"; no plus sign); when the window expires: `video.currentTime = getDisplayTime() + seconds`. A lone first tap
  (accumulator 0) falls back to `onContainerClick(true)`. Controls hidden -> forwarded to `onContainerTouch` (show first; comment:
  double tap with hidden controls should go fullscreen). Settings menu open -> hide menu only. Blocked if tapSeekDistance<=0 or a
  LINEAR AD is playing. No live/duration guard in this file.
- Rotation: listens `screen.orientation` "change"; returns if readyState==0, casting, disabled, fullscreen unsupported;
  landscape & not fullscreen -> enter fullscreen; portrait & fullscreen -> exit. After requestFullscreen with
  forceLandscapeOnFullscreen -> `screen.orientation.lock('landscape')` (errors ignored); exit -> `unlock()`. No delays.

Seek bar (seek_bar.js) [V]
- Hidden when: live && (seekRange < `MIN_SEEK_WINDOW_TO_SHOW_SEEKBAR_` = 5 s || not finite); linear ad playing (AD_STARTED hides,
  AD_STOPPED re-checks; also hidden at construction if an ad is already playing).
- Drag: `onChangeStart`: remember `wasPlaying_`, `controls.setSeeking(true)` (stops the 3 s timer), `video.pause()`.
  `onChange`: return if `!video.duration`; redraw now; actual seek 125 ms after the last change (`seekTimer_.tickAfter(0.125)`).
  `onChangeEnd`: seek immediately, `setSeeking(false)` (timer restarts at 3 s), resume if wasPlaying_; touch thumbnail hides after 0.25 s.
  VOD target == duration -> minus 0.001 s. Seek value is not clamped to seekRange (wheel path is).
- Sizes: THUMB_SIZE_PX_=12; Less (v5.1.7): thumb 12 px, track 4 px, touchable 40 px ("always greater than thumb"); newer main uses CSS vars
  `--shaka-thumb-size/--shaka-track-h/--shaka-touch-size` (values not read). Container height 5 px + 2x4 px transparent border.

Time / live / changing duration (presentation_time.js) [V]
- Driven by `player.seekRange()` (manifest/timeline based), NOT by `video.duration`; refreshed on `timeandseekrangeupdated`.
- seekRange size not finite -> "LIVE" (button disabled). Dynamic stream: `behindLive = floor(seekRange.end - displayTime)`; >= 1 s (or
  seeking) -> "- mm:ss" and the badge is clickable (jumps to seekRange.end); < 1 s -> "LIVE". VOD: "cur / total" (or "-remaining"),
  total shown only when seekRange size != 0; hours column only when >= 3600 s; hidden while a linear ad plays.
- Shaka writes the MediaSource duration itself (streaming_engine.js `updateDuration`): finite -> set; live -> Infinity if supported,
  else `Math.pow(2, 32)` ("Not all platforms support infinite durations, so set a finite duration so we can append segments and so
  the user agent can seek"). After endOfStream the timeline duration shrinks to MSE's.
- Ads: linear ad -> seek bar + time hidden, play/pause routed to the ad API, tap-seek and number-key seek disabled; controls stay while
  the ad is paused.

---------------------------------------------------------------------------------------------------------------------

## 3. video.js (v8.23.4 tag; `main` is now the v10 monorepo, v8 `src/js` exists only on tags)
Files: github.com/videojs/video.js/blob/v8.23.4/src/js/{player.js,component.js,tech/tech.js,live-tracker.js,utils/time.js,
control-bar/progress-control/seek-bar.js,control-bar/time-controls/*.js}, src/css/components/{_control-bar,_progress}.scss.

- `inactivityTimeout: 2000` ms (Player options_). `listenForUserActivity_`: mousedown/move/up/leave + key events; a
  `setInterval(activityCheck, 250)`; when no activity for inactivityTimeout -> `userActive(false)` (class `vjs-user-inactive`).
  Hover on the control bar sets the timeout to 0 (stays) except on iOS/Android [V].
- Touch: `Component.emitTapEvents` (touchstart/move/end/cancel/leave): ONE finger, moved <= `tapMovementThreshold = 10` px and
  touchend within `touchTimeThreshold = 200` ms -> `event.preventDefault()` + 'tap' [V]. `handleTechTap_`: `userActive(!userActive())`
  = a tap TOGGLES the controls. `handleTechTouchStart_` stores `userWasActive`; `handleTechTouchMove_` reports activity only if it was
  active. `handleTechClick_` toggles play/pause (userActions.click) [V]; it is the mouse path, on touch the emulated click is
  suppressed by the `preventDefault()` in the tap emitter [I]. (userActions.doubleClick not read.)
- CSS [V]: control bar hidden ONLY for `.vjs-has-started.vjs-user-inactive.vjs-playing` (opacity 0, pointer-events none, 1 s fade;
  fade-in 0.1 s) => stays while paused/ended. Bar height 3em (30 px if the 10 px base font applies [I]). `.vjs-progress-holder` 0.3em (3 px), x1.67 on hover
  and while `vjs-scrubbing.vjs-touch-enabled`; `.vjs-progress-control` min-width 4em, `touch-action:none`. vjs-error / native controls:
  bar display:none.
- Scrub (seek-bar.js) [V]: mousedown: `videoWasPlaying = !paused`; pause unless `shouldDisableSeekWhileScrubbing_`
  (`disableSeekWhileScrubbingOnMobile` default false, only iOS/Android); each move seeks via `userSeek_` (throttled redraw only);
  mouseup: scrubbing(false), apply pending seek, resume if it was playing. VOD target == duration -> duration - 0.1.
  `getPercent` has no NaN/0/Infinity guard (pendingSeekTime setter clamps to [0, duration]). Keyboard: stepSeconds 5, pageMultiplier 12.
- Duration [V]: getter returns cached value, initially NaN; setter parseFloat, NEGATIVE -> Infinity, Infinity adds `vjs-live`;
  "Do not fire durationchange unless the duration value is known" (NaN never fires). DurationDisplay refreshes on `durationchange`,
  `loadstart`, `loadedmetadata`. `formatTime(NaN|Infinity)` = "-:-" ("-:-:-" with hours guide); negative -> "0:00".
- Live (live-tracker.js) [V]: defaults `trackingThreshold: 20` s, `liveTolerance: 15` s; tracking only if `duration()===Infinity &&
  liveWindow() >= 20`; `liveui` option default false (adds seek-to-live + live seek bar); on `durationchange`/`canplay` -> toggleTracking;
  Infinity -> finite stops tracking and removes `vjs-liveui`; a user seek > 2 s behind marks "behind live edge"; seek bar live branch:
  distance >= 0.99 -> seek to live edge. Event polling when the tech lacks timeupdate: 250 ms (`trackCurrentTime`), progress 500 ms.
- Fullscreen: native API; else `enterFullWindow()` (class `vjs-full-window`, body overflow hidden); `fullscreen.options.navigationUI:'hide'`.
  No orientation code in the parts read.

---------------------------------------------------------------------------------------------------------------------

## 4. Plyr (master, v3.8.5)
Files: github.com/sampotts/plyr/blob/master/src/js/{config/defaults.js,listeners.js,ui.js,controls.js,plyr.js,fullscreen.js}, README.

- Defaults [V x2 defaults.js + README]: hideControls true ("Hide video controls automatically after 2s of no mouse or focus
  movement"), clickToPlay true ("Click (or tap) ... toggle play/pause"), seekTime 10, invertTime true, resetOnEnd false, displayDuration
  true, fullscreen {enabled true, fallback true, iosNative false}, tooltips {controls false, seek true}, duration null (faux duration),
  disableContextMenu true. CSS vars: `--plyr-range-thumb-height` 13 px, `--plyr-range-track-height` 5 px, icon 18 px, spacing 10 px.
- Hide logic (listeners.js) [V]: on container events `mousemove mouseleave touchstart touchmove enterfullscreen exitfullscreen`:
  touchstart/touchmove/mousemove -> show, then hide after `delay = player.touch ? 3000 : 2000` ms; others delay 0. Entering
  fullscreen resets pressed/hover.
- `ui.toggleControls` [V]: show if `force || loading || paused || controls.pressed || controls.hover || recentTouchSeek`
  (`touch && lastSeekTime + 2000 > now`). So: stays while paused/loading; stays >= 2 s after a touch seek. `checkLoading`: stalled/
  waiting sets `loading` after a 250 ms delay (anti-flicker) and shows controls.
- Tap (clickToPlay handler) [V]: `if (player.touch && player.config.hideControls) return;` -> on touch a tap NEVER toggles play/pause
  (it only reveals the controls through touchstart); desktop click toggles; ended -> restart then play. Double-click on the container
  toggles fullscreen (not on controls). No double-tap-seek code exists; `forward()/rewind()` = currentTime +/- seekTime (button only).
- Seek input [V]: range min 0, max 100, step 0.01. mousedown/touchstart: if playing set `play-on-seeked` and pause; mouseup/touchend:
  play again; `lastSeekTime = Date.now()`; `input` event sets currentTime = value/max * duration on EVERY change (no throttle).
- Duration rules [V]: getter = `parseFloat(config.duration)` || (`!is.number(real) || real === Infinity ? 0 : real`); setter
  `currentTime`: return if `!duration`, else `Math.min(input, duration)` (input <= 0 -> 0). `durationUpdate` runs on `durationchange
  loadeddata loadedmetadata`; if `duration >= 2**32` ("2**32 (shaka), Infinity (HLS), DASH-IF MAX_SAFE_INTEGER/MAX_VALUE indicating live")
  the current-time display and the progress bar are HIDDEN; `timeUpdate` ignores `timeupdate` while `media.seeking`;
  `updateTimeDisplay` bails unless the value `is.number`.
- Fullscreen [V]: native `requestFullscreen({navigationUI:'hide'})`, else fallback class + `body{overflow:hidden}` + scroll restore;
  iOS native only if iosNative. NO orientation handling anywhere in the file.

---------------------------------------------------------------------------------------------------------------------

## 5. YouTube (Android app / m.youtube.com)
Primary = official Help pages only; internals are closed.
- [V] support.google.com/youtube/answer/7509567 (Android): "To rewind, double-tap the left side of the video-screen." "To fast-forward,
  double-tap the right side of the screen." (heading: fast forward or rewind a video by 10 seconds). "Tap and hold anywhere on the video"
  = 2x fast forward; "To stop fast forwarding, lift your finger." "Tap the video once, then tap Settings" (implies one tap shows the controls).
- [V] answer/12825599 (navigation tips): seeking = "use the scrubber, the red dot"; precise seeking: "Click the screen to see the progress bar",
  "Drag the scrubber up", "a row of thumbnails below the progress bar", then drag the thumbnails/scrubber; "Release to cancel a seek"
  (tablet and phone app only): move the scrubber back toward where you left off and "wait until you feel a vibration or see the Release
  to cancel notification"; a "most replayed" graph appears above the bar when seeking starts.
- [V] answer/6087602 (accessibility): "set a timer to automatically hide the video player controls after X seconds" (Settings >
  Accessibility). [O] Beebom lists 3 / 5 / 10 / 30 s plus "use device settings" (= Android "Time to take action"). Default value NOT documented.
- [O] Android Police 2020-10-19 quoting a Google Product Expert: a single tap on the seek bar no longer jumps; "tap, hold, and slide the
  progress bar" and the video jumps on release.
- [O] third-party how-tos: double-tap seek amount configurable (Settings > General > Double-tap to seek, roughly 5-60 s), repeated
  double taps accumulate, "left half / right half" zones.
- [V] IFrame API (developers.google.com/youtube/iframe_api_reference): `getDuration()` "will return 0 until the video's metadata is
  loaded"; for a live event it returns "the elapsed time since the live video stream began"; states -1 unstarted, 0 ended, 1 playing,
  2 paused, 3 buffering, 5 cued.
- NOT documented anywhere I could read (treat as [O]/[I]): auto-hide default (commonly ~3 s), whether controls stay while paused
  (they do in everyday use), exact double-tap zone fractions, lock-screen button.

---------------------------------------------------------------------------------------------------------------------

## 6. MX Player, VLC for Android (+ three engine-level references that matter for Kestrel)

MX Player (closed source) [V official pages]: sites.google.com/site/mxvpen/features and sites.google.com/view/mx-player-contact
- "Drag Horizontally - Change playback position." "Drag Vertically on right half of the screen - Sound volume control." "... left half
  ... Brightness control." "Pinch to zoom and/or pan". Old page: "Double tap - Play/Pause toggle. (optional)". Support page: "Double-tap
  left or right to seek backward or forward", "Double-tap the center to play or pause", "Swipe horizontally to scrub", lock icon
  "to prevent accidental touches, then use the floating unlock control to restore the controls". Kids Lock: "Touch 4 corners clockwise
  to unlock". Speed: "swiping up/down with two fingers". Long press on lock/zoom button = options. Single-tap-toggles-controller is not
  stated on those pages [I: it is the default].

VLC for Android (GPL, source read) [V]: github.com/videolan/vlc-android/.../gui/video/{VideoTouchDelegate.kt,VideoPlayerOverlayDelegate.kt},
application/tools/.../Settings.kt
- Single tap: toggles the HUD (SHOW_INFO / HIDE_INFO); when double-tap play/seek is enabled the toggle is delayed by
  `getDoubleTapTimeout()`; only counts if movement < `scaledTouchSlop`.
- Double tap: left QUARTER (`x < width/4`) seek back `videoDoubleTapJumpDelay` (10 s default; videoJumpDelay 10, videoLongJumpDelay 20),
  right quarter (`x > 0.75 width`) seek forward, middle HALF -> play/pause. Each behind a flag (touchControls bitmask).
- Long press: no separate listener; after 250 ms with no other action -> `fastplaySpeed` (2.0x) until release, then restore the old rate.
- Swipes: horizontal = seek (jump = 600000 ms * (cm/8)^4 + 3000 ms, applied on release, min gesture 1 cm); vertical on the right
  (`x > 4w/7`) volume, left (`x < 3w/7`) brightness; vertical wins only past 5 % of range and `coef > 2` direction ratio; edge dead zone
  `gestureSafetyMargin = 24 dp`; pinch = zoom; locked = every gesture ignored, a tap only toggles the HUD.
- HUD timeout `Settings.videoHudDelay` default 4 s (prefs 1..15, -1 = never); HUD stays (infinite) when PAUSED, TalkBack on, stats on,
  bookmarks shown. `hideOverlay` also calls `dimStatusBar(true)` (system-UI flags follow the HUD); `lockScreen()` locks orientation,
  disables time/seekbar/prev/next, shows swipe-to-unlock, hides the HUD. SEEK_TIMEOUT 750 ms.

Gecko `toolkit/content/widgets/videocontrols.js` (what Firefox/GeckoView draws itself) [V]
- `isMobile = appVersion.includes("Android")` -> touch controls. Desktop `HIDE_CONTROLS_TIMEOUT_MS 2000`, SHOW 500, throbber 250 ms;
  TOUCH `controlsTimeout 5000`. Fades: control bar 200 ms, click-to-play 400 ms.
- Tap on the spacer toggles the control bar (not play/pause, except the first tap on an unstarted video). Scrubber touchstart clears
  the hide timer, touchend restarts it (5000). The play event not caused by the controls fades the bar out. Hide path does NOT check
  `paused` (controls fade 5 s after the last touch even when paused; they stay only if `dynamicControls` is false: audio, error before
  first frame). `_hideControlsFn` does nothing while `scrubber.isDragging`.
- Scrub: `input` event seeks on every change (no throttle), pauses the video during the drag (`pauseVideoDuringDragging`), resumes in
  `change`. If the drag ends outside the scrubber nothing resets `isDragging` (stays paused) - a known fragility, avoid copying.
- Duration: `showPosition()`: `maxCurrentTimeSeen` only rises; if `isNaN(duration) || Infinity` the slider max = maxCurrentTimeSeen and
  the duration label is "" for Infinity; finite duration < currentTime is NOT clamped; `durationchange` only refreshes the PiP button,
  `loadedmetadata` and `timeupdate` update the slider; labels re-set only when text changes; "long" layout when >= 3600 s. SEEK_TIME_SECS 5.

Chromium Blink `media_controls_impl.cc` (Chrome Android + desktop) [V]
- `kTimeWithoutMouseMovementBeforeHidingMediaControls = 2.5 s`; `OnPause` -> MakeOpaque + stop timer (stays while paused);
  `ResetHideMediaControlsTimer` restarts only if not paused; `ShouldHideMediaControls` false while seeking.
- Touch: tap toggles (`MaybeToggleControlsFromTap`, ignored while paused); `kDoubleTapDelay = 300 ms`; double-tap seek
  `kNumberOfSecondsToJump = 10`, halves split at the horizontal midpoint; target clamped to duration.
- Scrub: pauses (`is_paused_for_scrubbing_`) and resumes at end. Live: `IsLivePlayback()` = `seekable().length()==0 && state >= stopped`
  hides the timeline; duration label only when `isfinite(duration)`; speed button hidden when duration is infinite.
- Fullscreen orientation (blink-dev Intent to Implement "Fullscreen Media Orientation", Android only): Chrome "will automatically change
  the orientation of the video to match the aspect ratio"; "two thirds of the time, when a video goes fullscreen, the users rotate their phone".

NewPipe (open-source YouTube client; a proxy for YouTube feel, NOT Google code) [V]: VideoPlayerUi.java, gesture/*.kt, settings_keys.xml
- Controls hide `DEFAULT_CONTROLS_HIDE_TIME` 2000 ms (touch) / 7000 ms (d-pad), fade 300 ms; `onPaused -> showControls(400)` and
  `onCompleted -> showControls(500)` with NO hide scheduled; seekbar `onStartTrackingTouch -> showControls(0)` cancels the timer;
  `onStopTrackingTouch` seeks on release then `showControlsThenHide()`.
- `DOUBLE_TAP_DELAY = 550 ms`; zones by width: LEFT `x < w/3`, RIGHT `x > 2w/3`, MIDDLE = play/pause; default seek `seek_duration` 10000 ms
  (options 5000-30000); single tap toggles controls (hides after 150 ms), never play/pause; vertical scroll gestures only in fullscreen,
  ignored if they start in status/nav-bar area, `MOVEMENT_THRESHOLD = 40`.

---------------------------------------------------------------------------------------------------------------------

## 7. Duration problems (MSE, live-to-VOD, ads in the same <video>)

Spec facts [V]
- HTML Standard (html.spec.whatwg.org/multipage/media.html): `duration` = NaN when no media data; +Infinity when unbounded; a
  `durationchange` event fires "when the length of the media resource changes to a known value"; "If the duration is changed such that
  the current playback position ends up being greater than the time of the end ... the user agent must also seek to the time of the end".
- MSE (w3.org/TR/media-source-2): `MediaSource.duration` starts NaN; setter: negative/NaN -> TypeError, readyState != open or any
  SourceBuffer updating -> InvalidStateError; duration-change algorithm: lowering below the highest buffered presentation timestamp ->
  InvalidStateError, and a new value is raised to the highest buffered end time. First init segment: "Update the duration attribute if it
  currently equals NaN" to the init segment's duration, or +Infinity if it has none. Coded-frame processing: "If the media segment
  contains data beyond the current duration, then run the duration change algorithm" with max(current, group end timestamp).
  `endOfStream()` sets duration to the largest buffered end (spec example: duration 10 with 0..5 appended -> 5).
  `seekable`: NaN -> empty; +Infinity with no live seekable range -> [0, highest buffered end] (empty if nothing buffered); with
  `setLiveSeekableRange` -> union with buffered; otherwise [0, duration].
  => A short, GROWING duration (init says 30 s, 25 min arrives) and a duration that later SHRINKS at endOfStream are both legal.
- YouTube IFrame: getDuration() = 0 until metadata; live = elapsed since stream began (it is not an end time).

Rules the players actually implement [V unless tagged]
1. NaN / 0 = unknown: no total, no scrubber, no seeking. video.js: no `durationchange` for NaN, "-:-"; Plyr: duration getter 0, currentTime
   setter returns; Shaka UI: tap and seek-bar ignored while `!video.duration`; Media3: TIME_UNSET -> 0 -> bar disabled; Chromium: duration label
   only if finite; YouTube IFrame: 0.
2. Infinity (and >= 2^32) = live/unbounded: Plyr hides current time + progress at `duration >= 2**32`; Shaka sets 2^32 where Infinity is
   unsupported; video.js adds `vjs-live` and uses a live window; Chromium hides the timeline when `seekable().length()==0`; Gecko blanks the
   label and uses `maxCurrentTimeSeen`; Shaka shows "LIVE" / "- mm:ss" and hides the bar if the window < 5 s; video.js live tracking needs a
   window >= 20 s, "at live edge" tolerance 15 s.
3. Re-read on EVERY `durationchange` (+ `loadedmetadata`, `loadstart`/`emptied` for resets): video.js DurationDisplay (durationchange,
   loadstart, loadedmetadata), Plyr (durationchange, loadeddata, loadedmetadata), Chromium `OnDurationChange`. Gecko listens only to
   `timeupdate`/`loadedmetadata` for the slider: that is the pattern that shows a stale short duration on MSE sites - do not copy.
4. Raise-only inside one load: hls.js updates the VOD duration only when `playlistEnd > msDuration && playlistEnd > mediaDuration || !finite
   (mediaDuration)`; Gecko's `maxCurrentTimeSeen` never falls. Lower values only after the app resets (new src / `emptied`).
5. Clamp what you DISPLAY and SEND: position never > duration (Plyr clamps `Math.min`; video.js/Shaka avoid seeking to exactly
   `duration` via -0.1 / -0.001 s; Chromium `MaybeJump` clamps). The browser itself clamps currentTime when duration shrinks (HTML).
6. Invariant for MSE: `video.duration >= buffered.end(last)` always holds for a healthy MediaSource (spec raises it). [I] So
   "duration < max(currentTime, buffered end, seekable end)" means the page/player is mid-reconfiguration -> treat as unknown/unstable and
   show only elapsed time until it stabilises.
7. Live to VOD: video.js live tracker on Infinity -> finite removes live UI and stops tracking; Shaka re-derives from seekRange; spec says
   endOfStream turns duration into the buffered end.
8. Same-element ads: videojs-contrib-ads "redispatch" renames media events while an ad is in the content element
   (`playing`->`adplaying`, `ended`->`adended`, generic prefix `ad`/`content`; `loadstart/loadeddata/loadedmetadata` from a
   source change made BEFORE ad mode stay unprefixed) so UI listeners never see ad duration as content duration. IMA HTML5 docs:
   pass the content element, set `restoreCustomPlaybackStateOnAdBreakComplete`, remove the content `ended` listener on
   CONTENT_PAUSE_REQUESTED and re-add on CONTENT_RESUME_REQUESTED; "the SDK can't differentiate between content progressing normally
   and a user seeking" (hence a custom contentPlayback object for drag-seek). Shaka UI: hide seek bar + time, disable tap/number
   seeking during linear ads. Media3: controller hidden during ads (hide_during_ads) and time bar uses CONTENT position/duration with ad
   markers. [I] For a page-agnostic overlay: a `currentSrc` change, `emptied/loadstart`, or a duration that jumps (> ~2x or < current
   position) while playing => freeze scrubber/time (keep play/pause), unfreeze after the next `loadedmetadata` + one stable `durationchange`.

---------------------------------------------------------------------------------------------------------------------

## 8. Android specifics for a full-screen video overlay

System bars / insets
- [V] Media3 PlayerView/PlayerControlView contain no inset handling; apps pad the controller themselves. NewPipe applies an
  `OnApplyWindowInsetsListener` and pads its overlays; VLC toggles the system-UI flags together with its HUD (`dimStatusBar`).
- [V] developer.android.com/develop/ui/views/layout/immersive: `WindowInsetsControllerCompat.hide(systemBars())`;
  `BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE` = "transient system bars overlay your app's content, might have some degree of transparency,
  and are automatically hidden after a short timeout" (so they do NOT resize the layout; the controller is simply covered). The doc's
  own sample keeps its fullscreen-toggle button in sync with an `OnApplyWindowInsetsListener` + `windowInsets.isVisible(navigationBars()
  / statusBars())`. `View.OnSystemUiVisibilityChangeListener`, `setSystemUiVisibility`, SYSTEM_UI_FLAG_IMMERSIVE(_STICKY) are deprecated
  in API 30 (use `WindowInsets#isVisible` / `WindowInsetsController`); on API 29 the compat classes fall back to the old flags [I].
  Exact API-29 sticky-bar callback behaviour was not retrievable this session.
- [V] developer.android.com/develop/ui/views/layout/edge-to-edge: edge-to-edge is ENFORCED on Android 15+ when targetSdk >= 35 (Kestrel
  targets 36, so on the Android 16 phone the window draws behind the bars; on the API 29 tablet it does not unless requested); use
  `systemBars()` insets for tappable views, `displayCutout()` for cutouts, `systemGestures()` for swipeable views; `CONSUMED` insets are not
  passed to siblings on API <= 29 unless `ViewGroupCompat.installCompatInsetsDispatch` is used.
- [V] gesture navigation (API 29+): back = inward swipe from left/right edge; apps may opt out regions with
  `View.setSystemGestureExclusionRects()` (docs note SeekBar auto-opts-out); home/quick-switch at the bottom cannot be excluded
  (`getMandatorySystemGestureInsets`). Javadoc: "the system will put a limit of 200dp on the vertical extent of the exclusions ... The
  limit does not apply while the navigation bar is stickily hidden" (read via Microsoft Learn mirror of the Android Javadoc).
  [I] Keep the time bar >= ~24 dp away from the left/right edges and above the bottom gesture inset on gesture-nav devices.
- [V] GeckoView `ContentDelegate.onFullScreen(session, boolean)`: "Typically, the implementation would set the Activity containing the
  GeckoSession to full screen". GeckoView example does: hide action bar, `setDynamicToolbarMaxHeight(0)`, `WindowUtils.enterImmersiveMode`.
  `OrientationController.OrientationDelegate.onOrientationLock(int)` / `onOrientationUnlock()` deliver page `screen.orientation.lock()`
  calls (Shaka sites call it on fullscreen) to the app, which must call `setRequestedOrientation`.

Rotation
- [V] Android adaptive-app-quality Tier 3 (developer.android.com/docs/quality-guidelines/adaptive-app-quality/tier-3, test T-Config_State):
  "Playback resumes where it left off when the configuration change was initiated" (rotate, fold, resize). The documented bar is
  RESUME at the same position, not "pause". Media3's demo avoids the question with `configChanges` (no activity recreation).
- I found NO official recommendation to pause or freeze playback while the surface resizes. Documented tools: PlayerView
  `keep_content_on_player_reset` keeps the last frame visible when the player/surface is reset; Media3 notes SurfaceView resizing quirks
  (API 34 stretched surface). [I] Practical rule: handle the config change in place (do not recreate the engine view), keep the controller's
  shown/hidden state and hide timer, re-run layout + inset application, expect one frame of stretch on SurfaceView.
- [V] Web conventions: Shaka enters/exits fullscreen from `screen.orientation` 'change' and locks landscape on fullscreen (mobile only);
  web.dev "Mobile Web Video Playback": lock landscape on fullscreen only "when device is in portrait mode" and `max-device-width: 768px`
  because a lock "wouldn't be a great experience for users on tablet"; Plyr/video.js have no orientation code; Chromium auto-locks to
  the video's aspect ratio on fullscreen. In portrait our tablet is ~565 CSS px wide (600 px @170 dpi), under web.dev's 768 px
  "phone" rule, but ~916 CSS px in landscape, so that rule is ambiguous for this device; locking to the video's aspect ratio (Chromium
  style) is the safer convention [I].

Timeouts and touch targets (platform)
- [V] `AccessibilityManager.getRecommendedTimeoutMillis(int originalTimeout, int uiContentFlags)`, API 29 (minSdk 29 ok): "Controls should
  remain on the screen for at least this long to give users time to react"; flags FLAG_CONTENT_CONTROLS (4), _ICONS (1), _TEXT (2).
- [V] Touch targets: "at least 48x48dp, separated by 8dp of space" (support.google.com/accessibility/android/answer/7101858). Media3 time bar 48 dp.
- [V] API 29 `ViewConfiguration` (aosp-mirror android10-release): TAP_TIMEOUT 100 ms, DOUBLE_TAP_TIMEOUT 300 ms, DOUBLE_TAP_MIN_TIME 40 ms,
  DOUBLE_TAP_SLOP 100 dp, TOUCH_SLOP 8 dp, DOUBLE_TAP_TOUCH_SLOP 8 dp, PAGING_TOUCH_SLOP 16 dp, DEFAULT_LONG_PRESS_TIMEOUT 500 ms,
  MIN_SCROLLBAR_TOUCH_TARGET 48 dp.

---------------------------------------------------------------------------------------------------------------------

## 9. FINALLY: behaviour -> value used by most players
(V = verified in source/docs above, O = third-party/observed, I = inferred)

| Behaviour | Value used by most players | Notes / who |
| :- | :- | :- |
| Tap on video | TOGGLES the controller (show if hidden, hide if shown). Never play/pause on touch | Media3 [V], Shaka mobile (`singleClickForPlayAndPause`=false) [V], video.js (tap -> `userActive` toggle) [V], Gecko spacer [V], Chromium [V], NewPipe [V], VLC [V]. Outlier: Plyr touch = reveal only, no hide on tap [V]. YouTube: one tap shows controls [V help], hide on tap [O] |
| Single-tap latency | If double-tap is enabled the single tap is resolved after the double-tap window: 300 ms (Chromium, Android DOUBLE_TAP_TIMEOUT), 500 ms accumulate (Shaka), 550 ms (NewPipe), `getDoubleTapTimeout` (VLC); Shaka hides 1000 ms after a shown-controls tap | [V] |
| Auto-hide delay while playing | 2.0 - 5.0 s after the last touch; cluster at 3-5 s. Media3 5000 (+2000 for the thin progress bar), Gecko touch 5000, VLC 4000 (1-15 s), Plyr touch 3000 (mouse 2000), Shaka 3000, Chromium 2500, video.js 2000, NewPipe 2000. YouTube user-selectable (3/5/10/30 [O]) | [V]; default of YouTube unknown |
| Fade length | 200-300 ms (Media3 250, NewPipe 300, Gecko 200); Shaka 600; video.js 1000 out / 100 in | [V] |
| Timer rules | Any touch / button click / scrub restarts the full timeout; timer suspended while the finger is on the scrub bar (or a menu is open) and restarted at release | Media3 [V], Shaka [V], Gecko [V], NewPipe [V], Plyr (+2 s after touch seek) [V] |
| Stays while paused / ended / failed | YES (indefinite). Exception Gecko (fades 5 s even when paused) | Media3 (timeout 0), Plyr, Shaka `showUIOnPaused`, video.js (hide rule needs `.vjs-playing`), VLC (infinite), Chromium, NewPipe [V]; YouTube [O] |
| Buffering | Spinner only; controls state unchanged (Plyr: shows controls after 250 ms of stall) | Media3 spinner off by default, Shaka off on mobile [V] |
| Double-tap seek | +/-10 s per double tap (Shaka 10, VLC 10, NewPipe 10 (5-30), Chromium 10, YouTube 10 [V]); repeated taps accumulate while each tap lands within 0.5 s (Shaka 500 ms, NewPipe 550 ms; Chromium 300 ms single pair) ; Media3 buttons 5 back / 15 fwd, Plyr 10, Gecko keys 5 | [V]; YouTube configurable 5-60 s [O] |
| Double-tap zones | Left/right = seek, centre = play/pause. Splits: Shaka 40/20/40 (centre not a seek zone), NewPipe 33/33/33 (centre = play/pause), VLC 25/50/25 (centre = play/pause), MX L/C/R (fractions n/a), Chromium + YouTube halves [V Chromium; YouTube O] | [V] |
| Long press | Hold = 2x until release (YouTube [V help], VLC 250 ms hold -> 2.0x [V]); MX uses two-finger swipe for speed [V] | |
| Lock | Lock button disables all gestures and controls, leaves one unlock affordance (VLC swipe-to-unlock, MX floating unlock / 4-corner kids lock) | [V] |
| Scrub target (touch) | Full-width strip 40-48 dp tall around a 2-5 dp bar; thumb 10-14 dp (Media3), 12 px (Shaka), 13 px (Plyr), 16 dp dragged (Media3 class default); video.js 30 px bar | Media3 48 dp, Shaka 40 px, Android a11y 48 dp [V] |
| Scrub start | Media3/Plyr/Shaka/Gecko: touch-down jumps the thumb (a tap seeks). YouTube Android since 2020: hold-and-slide only [O] | [V] |
| Scrub semantics | Seek on RELEASE (Media3 default, YouTube) or live-seek while dragging with pause/resume: Shaka pauses, throttles to 125 ms, resumes; Chromium, video.js, Plyr, Gecko also pause during drag and resume (Gecko/Plyr unthrottled). Media3 never pauses | [V] |
| Fine scrub / cancel | Media3: finger > 50 dp above the bar = 1/3 speed; YouTube: drag up = thumbnail strip; "Release to cancel" back at the start point with a vibration | [V] |
| Duration display | Show total only when `isFinite(d) && d > 0 && d < 2^32` (after loadedmetadata, refreshed on every durationchange); NaN/0 = unknown, show nothing or "-:-"; Infinity or >= 2^32 = live: hide total and bar (or "LIVE"/"- mm:ss"), hide seek bar if seekable window < 5 s (Shaka) / < 20 s live tracking (video.js) | Shaka, video.js, Plyr, Chromium, Gecko, Media3 [V] |
| Duration that changes (MSE) | Spec-legal: init sets it, appended data raises it, endOfStream shrinks it. Players: re-read on durationchange, raise-only within one load (hls.js, Gecko maxCurrentTimeSeen), clamp displayed/seek position to <= duration, leave 0.1/0.001 s before the end | [V] |
| Ads in the same element | Hide seek bar + time, disable tap-seek, keep play/pause (Shaka, Media3 hide_during_ads); suppress/redirect `durationchange/ended/loadedmetadata` while an ad runs (contrib-ads redispatch, IMA remove/re-add `ended`) | [V]; page-agnostic detection is [I] |
| Rotation | Keep playing and resume at the same position (Android quality bar); no activity recreation (configChanges); controller keeps shown/hidden state and timer, re-lays out on size change; web players may enter/exit fullscreen or lock landscape on rotate (Shaka, Chromium by aspect ratio) | Media3 [V], Android guideline [V], Shaka [V]; "pause while resizing" is NOT a documented standard |
| System bars | Hide with transient-by-swipe behaviour; controls padded by `systemBars()|displayCutout()` insets; bars overlay (do not shift) the controller; keep scrubber off the gesture edges | Android docs [V]; "Chrome shifts?" not verified |

---------------------------------------------------------------------------------------------------------------------

## 10. Suggested Kestrel defaults (my synthesis, all [I])
1. Tap = toggle controls; decide it after 300 ms only if double-tap is on (else immediately).
2. Hide after 4 s while playing, or `AccessibilityManager.getRecommendedTimeoutMillis(4000, FLAG_CONTENT_CONTROLS)` if larger; suspend during scrub/menu,
   restart on release; no auto-hide while paused / ended / error / stalled.
3. Double tap: left 40 % and right 40 % = -/+10 s accumulating inside 500 ms (label "+10 s" / "+20 s"), middle 20 % = play/pause; off when the
   effective duration is unknown or an ad is suspected.
4. Time bar: 48 dp touch strip (full width minus a 24 dp side margin), 3-4 dp visible bar, 12 dp thumb growing to 16 dp; start the scrub after
   8 dp movement (touch slop) so a stray tap does not cost a rebuffer on the 5 Mbit/s link; seek on release (optionally one throttled
   preview seek 125 ms after the last move only if the target is inside `buffered`); fine-scrub when the finger is > 50 dp above the strip.
5. Effective duration = finite, > 0, < 2^32 ? max(duration, buffered end, currentTime) : unknown; re-evaluate on durationchange /
   loadedmetadata / progress / emptied; hold the previous value if a new one is smaller within the same `currentSrc`; unknown -> elapsed only,
   no scrubbing; Infinity -> "LIVE", scrub only inside `seekable`.
6. Rotation: handle in place, keep playing, keep controller state, re-apply insets; if you lock orientation on fullscreen, match the video's aspect ratio (Chromium) rather than a blanket landscape lock.
7. Fullscreen: transient-by-swipe bars, controls inside `systemBars()|displayCutout()` insets, bottom bar above the gesture inset.

## 11. Gaps (not verified)
YouTube auto-hide default, tap-hide, paused behaviour and zones; MX single-tap default; video.js `userActions.doubleClick` and iOS/Android
fullscreen internals; Shaka `--shaka-touch-size` current value; API-29 behaviour of `OnSystemUiVisibilityChangeListener` with sticky transient
bars; whether Chrome shifts its controls for transient bars; any doc recommending "pause while the surface resizes" (none found).
