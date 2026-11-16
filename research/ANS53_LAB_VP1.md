# Lab run 43812820865 (android.yml app=lab lab_suite=vp, head 51374ebb, 2026-11-16 05:50Z, 9 minutes): ANS53 on the real engine

Scenes: ok 4, failed 3 (np, np-hls, ap). 34 issues, all in np and ap: one REAL bug in the page gate and six lab-side mistakes. The emulator has no MediaTek decoders: 'plays' counts, smoothness does not.

## Proven on the real Firefox engine (GeckoView 160, Android 10 emulator)
- kp (Kestrel's own full-screen controls, 27 of 27): ANS53c taps answered on the glass itself 5 to 15 ms after the finger lifted (one 104 ms); the first tap of a double tap toggles at once and the second only seeks; two quick taps in the middle are two toggles and no command; the page never got those taps; the exit button, mute, Hide mode inset all fine.
- cp / hls (ANS52g codec policy, ANS52h HLS picture cap): every check ok, also the gzip master list, also "the cap was already there at the page's first requests".
- np (plain file in Kestrel's own player): decoder, first picture, controls by taps, pause / play / skip, screen stays upright when a wide film starts in portrait, turns sideways with the tablet and back (config orientation landscape then portrait), Back, Diagnostics line. NOT reached: the rotate button (see 3).
- ap (press to play): preload-auto / preload-meta / no-attr (native controls): 0 bytes before a press, nothing played by itself, after the press the film played; mse (when its script ran) kept from sourceopen; Diagnostics counts "held back 6, refused 1, opened by the person 7".

## Found, and what was done
1. REAL BUG, fixed (gate-main.js openEl, web-tests/gate.test.js): the page's `autoplay` was never given back when the rule is switched OFF. The OFF case loaded the muted hero loop (2,025,001 bytes) but never played it. Now `why === 'off'` restores the attribute / property; a press, the page's own play() and Kestrel's controls do not (a tap on a hero loop with no play button starts nothing).
2. Lab: the np HLS scene gave the debug intent `http://localhost:8002/...`; the intent takes only 127.0.0.1 / 10.0.2.2, so NativePlayerActivity never started and 17 HLS checks failed. Fixed in soak.py (localhost -> 127.0.0.1).
3. Lab: the debug commands to the player screen (rotate, qopen, qpick) are `am start` from the shell and the activity is exported=false (no AM event at all in logcat). Fixed for DEBUG builds only: geckoapp/src/debug/AndroidManifest.xml (exported=true, tools:replace); the release manifest is unchanged. (Merged debug manifest checked.)
4. Lab: the press pages' own scripts ran BEFORE AP_JS defined apHook / apRep (ReferenceError), so script / mse / button never ran their script. AP_JS now comes first in soaklab.page_ap.
5. Lab: the engine's HTTP cache answered the later variants from the first one's bytes ("0 bytes after the press" although it played). The film address now carries ?c=<tag>.
6. Lab expectations: the app's own one-byte Range request (the download list's size question) is not video data (film_bytes counts requests above 1 byte); a hero loop with no play button must stay still after a tap (no data, no play) instead of being pressed into playing; the script variant now has a play button of its own and the 3 timer plays must be refused with NotAllowedError.
7. Lab: ap tap placement when the engine reports mozInnerScreenY = 0 (+25 px status bar); layout rows only from pages that have a film or a button.

## Added for the next run
- kp pass 'ANS53d.2': a page that also asks for the lab's HLS master list (fsbox hls=1) -> the full-screen controls draw the Quality button (log 'player quality-button') -> a REAL TAP on the button (top right, left of the exit button) opens the list ('video quality-open 3 row(s)') -> debug intent `pagepick 2` hands the film to Kestrel's own player ('video native-quality', 'native start-quality ... picked in the page's list', 'native pick-applied') -> Back.
- np HLS: the same hand-over launched with a pick (debug intent nativeurl + kestrel_pick).

# Lab run 2 (43816308648, head 99c99a77 = ckpt 2489, 06:35Z, 9 minutes): 7 of 7 scenes ok, 0 issues

Everything of run 1 that was open is now proven on the real engine (GeckoView 160, Android 10 emulator):
- ANS53f / ANS53d (Kestrel's own player, plain file and HLS): started on the lab's film, decoder, first picture, controls by taps, pause / play / skip, the screen STAYS upright when a wide film starts in portrait, follows the tablet sideways and back, the ROTATE BUTTON turns it and turns it back (the debug-only exported player screen works), the quality list opens with Automatic + the two pictures, a pick is carried out, Automatic gives the choice back.
- ANS53d.2: a page that asks for an HLS master list (like hls.js) -> the full-screen controls draw the Quality button -> a REAL TAP on the button (top right, left of the exit button) opens the list over the controls with Automatic and the two pictures -> a pick hands the film to Kestrel's own player ('video native-quality', 'native start-quality ... picked in the page's list', 'native pick-applied') -> Back. Also the launch with a pick from the np scene (180p picked, 180p found and played).
- ANS53a / ANS53b (the gate, of that head): 0 bytes before a press in all 8 pages (besides the app's own one-byte size question), nothing played by itself, the hero loop (no controls) stays still after a tap (0 bytes), the page's own play() timers refused 3 times with NotAllowedError, then its own play() inside the press went through, the MediaSource player kept from sourceopen until the button (2,017,295 bytes then), native controls pages played after the press (2,025,001 bytes), the rule OFF plays the muted autoplay loop again (the autoplay-back fix works), Diagnostics: held back 6, 1 script player kept from fetching, refused 4 plays that nobody pressed, opened by the person 1.
- ANS53c (kp): taps answered on the glass in 4 to 52 ms, double tap logic, the page never got the taps.

NOT covered by run 2: the gate as changed by the independent review (research/ANS53_GATE_REVIEW.md) - audio exempt, MediaSource-only blob hold (URL.createObjectURL wrapper: the mse page must still be held), tap scoping + 30 s, fail-closed relay, the late-answer wait, and the engine's own control bar (variant 'bar') - needs lab run 3.
