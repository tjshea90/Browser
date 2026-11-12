# TYPING - AA3-2: typing lag and the keyboard

The owner (M3.15, M17.3): Brave "freezes a lot especially when I'm typing", Proton Mail feels slow when typing. The numbers asked for (Diagnostics "Page loads", keyboard app, accessibility/autofill services) never came, so this work does not wait for them. Tags: **[O]** the owner said it, **[V]** verified here (Android 10 x86_64 emulator, GeckoView 160, debug build; no KVM, so absolute times mean nothing), **[N]** not verified (no real tablet).

## 1. Already done before this note
- Address bar: `adjustNothing` while the field is focused, `ui/KeyboardProbe` lifts the dock, so the page view never resizes under the address-bar keyboard (BrowserActivity `setUrlTyping`, docs/ARCHITECTURE.md). [V in alpha4]
- `NetworkMonitor.start` runs on the IO lane, not the UI thread (alpha23).
- No per-key code of ours sits in the typing path of a PAGE field: no `TextInputDelegate`, no `onCreateInputConnection` override, no key listener (research/AUDIT-2026-11.md section 1).

## 2. Hypothesis tested and closed: "the keyboard resizes the page, so the page is laid out again"
Probe page `kb.html` (kept in the scratchpad, not in the repo: it logs `innerHeight`, `documentElement.clientHeight`, `visualViewport.height/offsetTop`, `scrollY` on load / focus / resize / scroll). Result [V], keyboard opened by tapping a field:
- `innerHeight` stays 721 and there is NO window `resize` event: the layout viewport does not change, so the page is not laid out again when the keyboard opens or closes.
- Only the VISUAL viewport shrinks: 721 -> 375 -> 335 px (two steps: the keyboard, then its suggestion strip).
- So an A/B switch "Keyboard: resize page / keep page size" (adjustResize vs adjustNothing) would switch nothing the page can feel, and `adjustNothing` in a page would only leave the field hidden behind the keyboard. **Not built.**
- Firefox 160 contains the pref `dom.interactive_widget_default_resizes_visual` and the meta-viewport values `resizes-visual`, `resizes-content`, `overlays-content` (strings in libxul). Debug builds can flip the pref for an A/B on the emulator: `setprop debug.kestrel.iwvisual 0|1` + restart (applied in `GeckoEngine.create`); Diagnostics > "Engine prefs" reads it back as `resizes_visual=...`.

## 3. The real keyboard problem found: a tapped field stays hidden behind the keyboard until the first letter [V]
`kb2.html` has normal-flow fields at three heights. Tapping field B (its top at 406 px of the page, the visible area after the keyboard opens is 335 px): focus fires, the visible area shrinks, and **nothing scrolls** (`scrollY` 0, no scroll event): the field sits under the dock and the keyboard. Firefox follows the CARET, not the focus, so the page only moves once a key is typed (`kb.html`: after typing "abc" the field was visible above the dock). Anything in the lower ~45 % of a page (login forms, comment boxes, search fields) is affected.

**Fix (alpha24):** `content.js` listens to `visualViewport` `resize`; when the visible area shrinks by more than 60 px (a keyboard, not a toolbar), it waits 120 ms for the final size, and if the focused element is a text field / textarea / select / contenteditable that is no longer inside the visible area it calls `scrollIntoView({block:'center', behavior:'instant'})`. One listener per page, nothing runs unless the keyboard opened. Checkboxes, radios, buttons and other non-typing inputs are left alone.
- Tests: `web-tests/content.test.js` +5 (81 in all): covered field is scrolled, two-step keyboard uses the final size, visible field not moved, small change / button / checkbox not moved, contenteditable moved and a page without `visualViewport` untouched.
- Emulator [V]: before the fix `fieldTop` stayed 406 px with a 335 px visible area; after it the page scrolled by 260 px and the field sat at 146 px (inside the visible area, caret showing, field C below it).

## 4. What is still unknown, and what would settle it [N]
Per-key latency on the real tablet. Candidates, none measurable here:
1. **One shared content-process main thread.** `dom.ipc.processCount = 1` puts every web tab on one main thread; alpha22 keeps up to ~6 tabs live, so background pages' timers and workers compete with the page being typed in (background tabs are throttled, not stopped).
2. **The IME bridge on a large editor** (Proton's composer): every key makes the keyboard ask Gecko for text around the caret; cost grows with the document.
3. **An accessibility service switched on** makes Gecko build the accessibility tree for every page (a large CPU cost on a heavy page).
4. **The keyboard app itself** (prediction / clipboard features).
What would settle it, in one message from the owner: type 20 characters into (a) the Google search box, (b) the Proton Mail composer, and say which one lags; the keyboard app; whether any accessibility or autofill service is on (Settings > Accessibility). If (a) is smooth and (b) is not, the page's own scripts are the limit; if both lag, it is the engine/IME path and the live-tab count (Settings > Appearance > Tabs kept ready: Few) is the first thing to try.
A "Typing lab" in Diagnostics (a local page measuring key-to-paint latency) was considered and not built: it needs the owner to type for it, and the answer above costs less of their time.
