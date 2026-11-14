# Glide typing on the mini keyboard (AT1, owner message 34, 2026-11-14)

> "Make the mini keyboard on the top right panel have glide typing like Gboard. Try to make the glide word predictions accurate."

## 1. What the owner gets

* A finger that goes down on a letter and slides off that key draws a word over the letters; a neon-blue trail follows it and fades after the lift. On lift the word goes into the bar with a space after it
  (the first glide replaces the selected address, like the first typed key does); a strip of three guesses sits above the keys (the typed one first, outlined); tapping another guess swaps it in.
* One Backspace right after a glide removes the whole word; after that Backspace works a letter at a time. Shift makes the glided word start with a capital. A plain tap still types its letter, also when the
  finger drifts a little inside the key. Slides that start on Shift / 123 / space / .com / Go / Backspace are not glides.
* A glide that fits nothing says "no word fits - try again" in the strip instead of typing the nearest word.
* The keyboard learns (all local, nothing leaves the tablet): how THIS thumb lands beside the key it aims at, the words typed letter by letter that the word list lacks (remembered when Go is pressed),
  and the names of the sites visited (host names such as doraby become glideable words).
* Settings > Appearance > "Glide typing on the mini keyboard" (default on). Menu > Diagnostics has a "Glide typing:" line: glides, how often the best word was kept / another guess was picked / the word was deleted
  right away, decode time, the learned thumb habit in dp, own-word count. THAT line is what turns "feels accurate" into a number on the tablet.

## 2. How it works (files)

| Part | File |
| :- | :- |
| key boxes, one function for drawing AND for the decoder | `core/.../input/MiniKeyboardGeometry.kt` |
| word list format + reader, in-memory list for personal words | `core/.../input/glide/Lexicon.kt` (`LexiconFile`, `BinaryLexicon`, `ArrayLexicon`) |
| the decoder | `core/.../input/glide/GlideDecoder.kt` (`GlideParams` holds every tuning number) |
| engine on ONE background thread, own words, counters | `core/.../input/glide/GlideSupport.kt` (`GlideTyping`, `UserWords`, `GlideStats`) |
| what a glide does to the text, Backspace, swap | `core/.../input/MiniKeyboard.kt` (`commitGlide`, `swapGlide`, `previousWord`) |
| the finger: glide vs tap, trail, strip | `app/.../ui/MiniNav.kt` (`KeysView`) |
| the word list | `geckoapp/src/main/assets/glide/en.klx`, made by `:tools:glideLexicon` (`docs/DICTIONARY.md`) |
| the accuracy yardstick | `core/src/test/.../glide/GestureSim.kt`, `GlideEval.kt`; tests `GlideDecoderTest`, `LexiconFileTest`, `GlideSupportTest`; keyboard flow `MiniNavTest` |

### The decoder in four steps (no allocation per word, no Android types, about 5 ms per glide on a laptop core)
1. **Cut the path** into 64 equal steps (in key pitches, so the keyboard's size does not matter) and move it by the learned thumb habit.
2. **Quick pass** (one table of "how close does the path come to each of the 26 keys" answers it for every word): a word is a candidate when its first and last letter are near where the finger went down and
   lifted and every other letter is near the path. The best 250 by (fit + a light language weight) go on.
3. **Careful pass** (dynamic programming per word): put the letters on the path in order, the first at the start and the last at the end, minimising how far each step is from its letter plus how far the path
   strays from the straight line between two neighbouring letters (a letter the path merely passes costs nothing; a corner the word does not explain costs). Cost is summed over letters (more letters = more
   evidence). The 60 best are judged a second time with the whole path moved by 80 % of how far it sat off the word's letters (the thumb lands beside the key the same way along a whole path), the move charged.
4. **Language**: score = -cost + w * ln P(word | word before). P is the word's frequency, mixed 40/60 with the pair frequency when the pair is known. **w is not fixed**: it is 1.5 times the best fit's cost per
   letter (limited to 0.12 .. 1.2), so a clean glide trusts the path and a sloppy one trusts the language. A word whose cost is above 3.4 per letter is dropped ("no word fits").

Learning: when a word is kept (it was not deleted right away, or it was picked from the strip) the decoder moves its stored thumb habit 12 % of the way towards how far that glide sat off the word's letters
(limited to 0.6 pitch); the habit is saved in Settings storage (`glide_bx`, `glide_by`).

## 3. Accuracy, MEASURED (simulated finger; seeded; `GlideEval`)

There is no recorded glide data of a real thumb here, so a seeded simulator draws paths over the REAL key layout (the keyboard as drawn: 292 x 161 dp, key pitch 29.5 dp, about 4.7 mm on the tablet): the finger
aims at every letter with scatter (a normal distribution), rounds each corner, drifts, is shifted as a whole, and touches the glass at uneven steps. A "calm" finger aims within 5 dp (0.8 mm), "normal" 8 dp (1.3 mm),
"sloppy" 11 dp (1.75 mm), "bad" 14 dp (2.2 mm). The numbers are only as honest as that finger; they are the best available guide, and the Diagnostics line is the real check.

| Words drawn as often as people use them (3000 glides each) | first guess right | right word in the 3 guesses |
| :- | -: | -: |
| calm | 96.5 % | 99.1 % |
| normal | 91.1 % | 97.2 % |
| sloppy | 81.7 % | 93.4 % |
| bad | 67.1 % | 81.1 % |

| Harder / easier mixes (first / in 3) | normal | sloppy |
| :- | -: | -: |
| uniform over the 20 000 commonest words (as many rare words as common ones) | 81.8 / 94.5 | 64.2 / 80.6 |
| the word before is known (pairs drawn from the pair list: optimistic, same data) | 97.8 / 99.5 | 94.0 / 98.5 |

A thumb that lands 10 dp (3 mm) beside the key every time, normal aim: 78.2 / 91.1 without any correction, 85.9 / 94.9 with the second look, 91.2 / 97.4 after the decoder learned the habit (the same as
a finger without the habit: 91.1 / 97.2).

**Key size** (same finger noise in dp, only the keyboard's width changes; first / in 3):

| keyboard width (key pitch) | aim 6 dp | aim 8 dp | aim 11 dp |
| :- | -: | -: | -: |
| 240 dp (24.3 dp) | 93.5 / 98.8 | 87.6 / 96.5 | 72.7 / 87.1 |
| 292 dp (29.5 dp, today) | 94.8 / 98.8 | 90.5 / 97.6 | 79.0 / 91.0 |
| 340 dp (34.3 dp) | 95.6 / 99.0 | 91.0 / 97.6 | 81.4 / 92.6 |
| 380 dp (38.3 dp) | 95.5 / 98.5 | 92.8 / 98.1 | 83.5 / 94.2 |

Reading: today's size is on the knee of the curve for a careful finger; a looser finger gains about 2 points of first-guess accuracy for every 25 % of width. If the tablet's Diagnostics line shows many deletions,
a "larger mini keyboard" setting (PANEL_KEYS_W_DP in `MiniNav.kt`) is the first lever, before any decoder change.

**Robustness** (weights were tuned on the four fingers above): a corner rounded much more or much less, much more per-touch noise: 88.8 .. 91.3 first; a drift of the whole path twice as strong: 84.7; a whole-path
offset of 10 dp WITHOUT the second look / learning: 73.5 (this is why both exist); a very loose finger with a big offset (aim 12 dp, offset 8 dp): 65.8.

Where the tuning went (kept so nobody repeats it): the cost must be a SUM over letters, not an average (an average let short words beat long ones: 65 % -> 88 % on calm paths); the path-shape term must be scaled by
the path's length; forcing the first / last letter to the very ends of the path beats allowing slack (+3 points) at one step of tolerance; 64 steps beat 48 (+1); a corner modelled as a smoothing spline (an early
guess) cut reversals far too much and made the yardstick unrealistically hard; the adaptive language weight gave +2 .. +4 points on loose fingers at no cost on clean ones.

## 4. Speed and memory

* Decode: about 5 ms on a laptop core (first call about 15-30 ms while the JIT warms up). The tablet's A53 is 5-8 times slower: about 25-40 ms, on ONE background thread; the UI thread is never held. The word list is
  loaded on that thread the first time the keyboard opens (about 3 MB read, a few tens of ms here).
* Memory: the word list in flat arrays is about 3.5 MB of heap (82 971 words, 242 342 pairs, no object per word) plus the person's own words (at most 800). APK: +2.7 MB uncompressed (about 1.4 MB packed), Kestrel (GeckoView)
  build only.

## 5. What only the tablet can say (and the owner's smallest step)

1. Use the glide for a day, then Menu > Diagnostics > Copy: the **Glide typing** line reads like `Glide typing: 41 glides: best word kept 33 (80 %), another candidate picked 5, deleted right away 3; decode avg 31 ms,
   worst 120 ms; word list 82971 words ready after 180 ms; thumb habit x 2.1 y -1.4 dp; own words 6`. "Kept" below about 75 % means the keys are too small for this thumb (see the size table) or a word is missing.
2. Words the list lacks (names, brands, Spanish) are glided into the nearest English word until they have been typed once (they are remembered when Go is pressed) or the site has been visited.

## 6. Limits and decisions

* English only. The owner also visits Spanish-language sites; a Spanish list (accents shown, typed on plain letters) is a follow-up that needs the owner's yes (asked in the answer; TASKS AT2).
* No live preview while the finger is still moving (Gboard shows one); decoding on lift keeps the decoder simple and cheap on the A53.
* Space, symbols and Go are not part of a glide; a glide that starts on them is not a glide. Glide on the symbol page is off (the strip shows nothing there).
* Capital letters: only through Shift (an address bar is lower case).
