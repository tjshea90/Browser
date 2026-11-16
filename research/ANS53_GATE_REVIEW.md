# Independent review of the press-to-play page gate (gate-main.js + relay), 2026-11-16, read-only agent, 79 tool uses, scripts demonstrated in headless Chromium and a Node VM of background.js

Scope: gate-main.js, content.js / background.js relay, the callers of VF.gate. Nothing was run on Gecko. Decisions are mine; "FIX" = done in alpha57, "KEEP" = left on purpose (why), "LIMIT" = documented limit.

| # | Finding (severity) | Decision |
| - | - | - |
| 1 | HIGH: every blob: src was held, also a File the person chose (an upload form's duration / preview probe on a detached video never fires) | FIX: only blob: addresses made from a MediaSource are held (URL.createObjectURL is wrapped and remembers them) |
| 2 | HIGH: `<video autoplay>` that later gets `srcObject` (camera, call, QR scanner) stays black: the attribute was taken off and never given back | FIX: a stream opens the element AND gives its autoplay back, also when it was opened by a press before |
| 3a | HIGH: only the 5 s transient activation lets play() through: a play() 6 s after a real tap (manifest / token fetch on the hotspot) is refused where the engine accepts it | FIX: a tap that landed on the video or inside its player box (up to 5 ancestors, never body / html, shadow trees followed) counts for 30 s |
| 3b | HIGH: requestFullscreen() before play() in the same click consumes the activation | FIX: a trusted gesture counts for 1.2 s even when the activation was consumed |
| 4 | HIGH: the activation never reaches a cross-origin iframe (a parent button posting to a player frame, Kestrel's own 'Tap to load' placeholder frames) | LIMIT: the person presses play inside the frame (a second tap); delegation across frames would need a relay through the background page |
| 5a | MED: a play() the page makes BEFORE the app's answer arrives is refused for good, also when the answer turns out to be OFF | FIX: until the answer is there a play() without activation waits (4 s at most) and is carried out on OFF, refused on ON |
| 5b | MED: openAll only walks the first 60 elements | FIX: see 9 |
| 5c / 5d | MED: frames without the content script never hear OFF; a Settings change takes up to 60 s to reach a new page (cached flags) | LIMIT (documented in Settings: "within a minute") |
| 6 | MED: an app that does not answer (or answers badly) turned the gate OFF for that page (fail open) | FIX: no answer = closed (the person's rule is the default) |
| 7 | MED: a tap opened any held video whose rectangle contained the point, whatever was on top (a cookie button over a hero loop started an MSE player's fetch) | FIX: the tap must land on the video or inside its player box (same rule as 3a) |
| 8 | MED: an opened element never closes: 'up next' loads the next film into the same element with play() and no gesture | KEEP: ad players (IMA) feed ads and the film through ONE element; closing again would leave the film stuck after the ad |
| 9 | MED: only 60 elements enter the gate, scans stop at 60 per subtree / 400 per mutation: the rest keep autoplay; declarative shadow DOM is never seen | FIX: caps 400 / 500 / 2000 (dead elements are dropped); LIMIT: declarative shadow DOM |
| 10 | MED, Gecko unverified: presses on the engine's own control bar may never reach window listeners | CHECK: lab variant 'bar' taps the bar's play button. For plain files the engine's own play() resumes the held load itself; for MSE players the gate needs the press |
| 11 | MED: audio was gated like video (a music player's next track, an alarm, a notification sound, 6 s after a tap, was refused) | FIX: audio is not touched at all: the owner's rule is about video |
| 12 | LOW: a page can open its own gate (dispatch the event, fake navigator.userActivation); source children, setAttributeNS / Node, document.open() skip the gate; the wrappers are detectable | LIMIT: the gate is for cooperative sites, not a security boundary |

Checked by the reviewer and fine: no leak (10,000 elements churned, 0 KB kept), at most one observer / timer / 6 reports, a press before the app's answer is not lost, every Kestrel caller (videobar.js, player.js, vfind.js, content.js) calls VF.gate before play(), shadow DOM through attachShadow incl. closed roots, play() twice, srcObject + explicit play().
