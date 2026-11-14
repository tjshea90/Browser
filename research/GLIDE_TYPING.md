# Glide typing on the mini keyboard (AT1, owner message 34, 2026-11-14)

> "Make the mini keyboard on the top right panel have glide typing like Gboard. Try to make the glide word predictions accurate."

## 1. What the owner gets

* A finger that goes down on a letter and slides off that key draws a word over the letters; a neon-blue trail follows it and fades after the lift. On lift the word goes into the bar with a space after it
  (the first glide replaces the selected address, like the first typed key does); a strip of three guesses sits above the keys (the typed one first, outlined); tapping another guess swaps it in.
* One Backspace right after a glide removes the whole word; after that Backspace works a letter at a time. Shift makes the glided word start with a capital. A plain tap still types its letter, also when the
  finger drifts a little inside the key. Slides that start on Shift / 123 / space / .com / Go / Backspace are not glides.
* A glide that fits nothing says "no word fits - try again" in the strip instead of typing the nearest word.
* The keyboard learns (all local, nothing leaves the tablet): how THIS thumb lands beside the key it aims at, the words typed letter by letter that the word list lacks (remembered when Go is pressed),
  and the names of the sites visited (host names such as doraby become glideable words). From alpha35 it also learns the person's own words and word pairs from what is kept, picked and typed (section 8).
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

### The decoder in four steps (no allocation per word, no Android types, about 2.6 ms per glide on a laptop core)
1. **Cut the path** into 48 equal steps (in key pitches, so the keyboard's size does not matter) and move it by the learned thumb habit.
2. **Quick pass** (one table of "how close does the path come to each of the 26 keys" answers it for every word): a word is a candidate when its first and last letter are near where the finger went down and
   lifted and every other letter is near the path. The best 200 by (fit + a light language weight) go on.
3. **Careful pass** (dynamic programming per word): put the letters on the path in order, the first at the start and the last at the end, minimising how far each step is from its letter plus how far the path
   strays from the straight line between two neighbouring letters (a letter the path merely passes costs nothing; a corner the word does not explain costs). Cost is summed over letters (more letters = more
   evidence). The 40 best are judged a second time with the whole path moved by 80 % of how far it sat off the word's letters (the thumb lands beside the key the same way along a whole path), the move charged.
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
| calm | 96.2 % | 99.0 % |
| normal | 90.5 % | 96.9 % |
| sloppy | 80.9 % | 92.9 % |
| bad | 66.3 % | 80.4 % |

| Harder / easier mixes (first / in 3) | normal | sloppy |
| :- | -: | -: |
| uniform over the 20 000 commonest words (as many rare words as common ones) | 79.8 / 93.3 | 62.9 / 79.2 |
| the word before is known (pairs drawn from the pair list: optimistic, same data) | 97.3 / 99.4 | 93.7 / 98.1 |

A thumb that lands 10 dp (3 mm) beside the key every time, normal aim: 77.0 / 88.9 without any correction, 85.2 / 94.3 with the second look, 90.5 / 96.7 after the decoder learned the habit (the same as
a finger without the habit: 90.5 / 96.9).

(An earlier setting, 250 candidates and 64 steps, was 0.6 points better on normal and twice as slow; the cheaper one was chosen because the tablet's first glides run while the runtime is still compiling
the decoder. The key-size and robustness figures below were measured with that earlier setting and are within about 1 point of today's.)

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

* Decode: about 2.6 ms on a laptop core. The tablet's A53 is 5-8 times slower and Android's runtime compiles the decoder's loops only after the first runs: about 15-30 ms once compiled, much more before (the
  emulator, which has no hardware acceleration, took 1.6 s per glide: NOT a tablet number). A throw-away decode of "the" runs on the glide thread when the keyboard opens to get the compiling over with; its time is in the
  Diagnostics line ("first decode (warm-up) N ms"). Everything runs on ONE background thread; the UI thread is never held. The word list is
  loaded on that thread the first time the keyboard opens (about 3 MB read, a few tens of ms here).
* Memory: the word list in flat arrays is about 3.5 MB of heap (82 971 words, 242 342 pairs, no object per word) plus the person's own words (at most 800). APK: +2.7 MB uncompressed (about 1.4 MB packed), Kestrel (GeckoView)
  build only.

## 5. What only the tablet can say (and the owner's smallest step)

1. Use the glide for a day, then Menu > Diagnostics > Copy: the **Glide typing** line reads like `Glide typing: 41 glides: best word kept 33 (80 %), another candidate picked 5, deleted right away 3; decode avg 31 ms,
   worst 120 ms; word list 82971 words ready after 180 ms, first decode (warm-up) 90 ms; thumb habit x 2.1 y -1.4 dp; own words 6`. "Kept" below about 75 % means the keys are too small for this thumb (see the size table) or a word is missing.
2. Words the list lacks (names, brands, Spanish) are glided into the nearest English word until they have been typed once (they are remembered when Go is pressed) or the site has been visited.

## 6. Limits and decisions

* English only. The owner decided (message 36, 2026-11-14): "Do not include Spanish." (TASKS AT2 is closed.)
* A live guess while the finger is still moving was added in AV3a (section 10); the final decode still happens on the lift.
* Space, symbols and Go are not part of a glide; a glide that starts on them is not a glide. Glide on the symbol page is off (the strip shows nothing there).
* Capital letters: only through Shift (an address bar is lower case).

## 7. Emulator proof (2026-11-14, AT1h) - what was and was NOT run

Run: the debug Kestrel build on the Android 10 x86_64 emulator (no hardware acceleration, software drawing), a REAL multi-point touch injected through the Linux input device (`sendevent`-style binary
events written to `/dev/input/event1`, 52 and 105 points for "hello" and "world", tool: scratchpad `glide_emu.py`, not kept in the repo; the recipe is: `getevent -p` for the touch device and its range, the keys view
bounds from a uiautomator dump of the view named "Keyboard", key centres from the same layout formula as `MiniKeyboardGeometry`, one `cat` of a chunk of `struct input_event` per few points):
* "hello" -> bar `hello ` + strip `hello | help | jello`; "world" after it -> `hello world ` + `world | wield | worlds`; a tap on the second guess swapped it (`hello wield `, strip led by `wield`); ONE Backspace
  removed the whole `wield ` (`hello `, strip back to the hint). A picture half-way through a glide shows the neon trail over the keys.
* Menu > Diagnostics: `Glide typing: 2 glides: best word kept 1 (50 %), another candidate picked 1, deleted right away 0; decode avg 1607 ms, worst 1880 ms; word list 82971 words ready after 743 ms; thumb habit x 0.7 y -0.0 dp`.
With the warm-up decode (second run of the build): `Glide typing: 1 glides; decode avg 676 ms, worst 676 ms; word list 82971 words ready after 1899 ms, first decode (warm-up) 5077 ms` - the first real glide took 0.68 s
instead of 1.6-1.9 s (the emulator's runtime compiled the decoder during the warm-up).
Not run: any real thumb, the real tablet's speed (the emulator's 1.6 s per decode is software emulation; see section 4), the owner's feel for the key size.

## 8. Learning from THIS person (AU2, owner message 35, 2026-11-14)

> "make the new glide type keyboard learn from my typing. I can choose words that I meant to type if it predicts incorrectly, and over time it will become more accurate by learning my more common word patterns"

### What it learns (all on the tablet, never sent anywhere: `files/glide_personal.txt`, one line per entry)
| Event | Word weight | Pair weight (word before -> this word) |
| :- | -: | -: |
| a glide that was kept (not deleted right away) | +1 | +1 |
| a guess picked from the strip instead of the first one | +3 | +3 |
| a glide deleted, then the right word glided or typed in its place | the replacement +3, the deleted word nothing | +3 |
| words typed letter by letter, counted when Go is pressed (glided words were counted when kept; an address such as `doraby.com/movie` is never learned) | +1 each | +1 for neighbours typed letter by letter |

`PersonalModel` (core): lowercase a-z and the apostrophe only, 2 to 24 letters; at most 4000 words and 8000 pairs (the least used tenth goes first); when the total weight passes 6000 everything halves and what fell under a fifth of a use is forgotten, so old habits fade.

### How it changes the answer
The decoder's language prior was `ln( (1 - mix) * P(word) + mix * P(word | word before) )` with the shipped list. With a person model:
* `P(word)` gets extra mass: every use is worth 2e7 occurrences on the list's scale (the list totals about 5.4e11), up to 200 uses. One use makes a word about as likely as the 10 000th commonest; ten uses the 1 500th; a hundred the 150th. A word the list lacks becomes glideable once it was typed (the older "own words" list does the same).
* `P(word | before)` is mixed with the person's own pair share, trusted more the more often the word before was seen: `n / (n + 4)`, never more than 70 %. So "best" -> "pizza" typed or picked a few times makes "pizza" the likely word after "best" even when its shape is close to another.
* Nothing can take over: both boosts have a ceiling; one pick cannot outweigh a clear gesture (the path cost still decides first; the language only weighs in when two words fit).

### Measured (simulated person; seeded; `GlideEval curve=1`, `PersonalModelTest`)
The person writes 70 % of what they glide from their own phrases of 2 to 4 words and has 150 favourite words outside the 20 000 commonest. 1800 glides in six stretches of 300; first guess right, %:

| finger | | stretch 1 | 2 | 3 | 4 | 5 | 6 |
| :- | :- | -: | -: | -: | -: | -: | -: |
| normal (aim 8 dp) | without learning | 86.3 | 90.7 | 84.0 | 85.7 | 87.7 | 85.7 |
| | with learning | 93.0 | 96.7 | 94.3 | 95.3 | 97.0 | 95.0 |
| | own favourites, with | 93.8 | 98.5 | 95.6 | 97.3 | 98.3 | 96.6 |
| sloppy (aim 11 dp) | without learning | 71.7 | 76.0 | 71.3 | 69.0 | 71.7 | 67.0 |
| | with learning | 81.0 | 86.7 | 87.7 | 85.3 | 90.3 | 89.3 |
| | own favourites, with | 81.9 | 89.1 | 88.9 | 89.6 | 92.1 | 90.2 |

Weaker habits (30 % own phrases, 60 favourites; the model knew 793 words and 637 pairs at the end): normal 87.7 .. 91.7 without against 90.0 .. 96.3 with learning; sloppy 73.7 .. 80.3 against 82.3 .. 89.0; the favourite words themselves 61.7 .. 75.6 without against 85.2 .. 95.1 with.

Reading: most of the gain arrives inside the first 300 glides (a pick counts three times), after that the model keeps the lead; a sloppier finger gains more because it leans on the language more. The test `learningMakesAPersonWithHabitsMoreAccurateOverTime` keeps at least +3 points overall and +5 points on the person's own words in the last third.
What the simulation cannot say: how much a REAL person repeats words and phrases (70 % own phrases is an assumption, 30 % a cautious one), and how a real thumb's mistakes cluster. The tablet's Diagnostics "Glide typing" line now ends with `learned N words and M word pairs from your typing, a learned word was the first guess K times`; THAT is the real count.

### Keeping and forgetting it
Saved at most every 30 seconds while typing, when the keyboard closes and when the browser stops (temp file, then rename, so a cut-off never leaves a half file). Settings > Privacy and security > "Forget what the keyboard learned" deletes the table, the thumb habit and the own-word list in one press.
Not part of Back up & restore: that file is plain text in Downloads which any app can read, and this table is a list of what the person typed. It re-learns in about 300 glides (above), so an uninstall costs little.

### Emulator proof of the learning (2026-11-14)
Typed "hello" letter by letter, glided "world", picked the second guess "wield" from the strip, deleted it with one Backspace, pressed Go in a page field. The learned file on the emulator then held `w wield 3.00`, `w hello 1.00`, `p hello wield 3.00` (a pick counts 3 with its pair; a word typed letter by letter counts 1; the glided "world" that was swapped away is not learned). After restarting the app, Menu > Diagnostics said `learned 2 words and 1 word pairs from your typing`: the table survives a restart. A guess that was picked and then deleted at once (Backspace right after the pick) is NOT learned (unit-tested: `MiniNavTest`). Not measured on the emulator: the ranking effect itself (simulated in section 8; the tablet's "a learned word was the first guess N times" counts it for real). Settings > Privacy and security > Forget what the keyboard learned (asks first: Cancel / Clear): after Clear the learned file and the stored word habit and thumb habit were gone on the emulator.

## 9. Research and the second round of learning (AV2, owner message 36, 2026-11-14)

> "research Google gboard and Samsung keyboard because i like how their glide typing works, but they often predict the wrong word and i want this app's keyboard to continuously learn and improve glide typing based on the words i actually type. Do not include Spanish."

The research is in `research/GLIDE_RESEARCH.md` (public sources only, 15 ranked ideas, each with the evidence quality; the table at its top says what was done with each). The short version:
* Gboard and Samsung decode a glide against the same kind of word list this keyboard uses and rely on a language model for the rest. Their published gains from learning the PERSON are in the language model (a personal word cache cut errors 19 % in a Google simulation) and in where a thumb lands on TAPS (small gains). Nothing published shows a glide ranker that learns from corrections: that part is ours to measure on the tablet.
* So the learning stays what it was (a bounded personal word and pair model) and gets five careful additions. Each one can only help decide between words whose paths fit about equally; none can overrule the finger.

| Addition | What it does | Guard |
| :- | :- | :- |
| Corrections | the keyboard remembers "guessed A, meant B" (a pick from the strip counts 1, a deletion followed by another word counts 0.5). When A is first again and B is among the others, B gets a lift of 0.9 x ln(1 + times) | the lift is at most 1.6 (`GlideParams.confCap`), and only for a word whose path fits within 0.9 pitch squared per letter of the first guess's (`confMaxGap`) |
| Last TWO words | "the whole" -> "wield" is learned as a three-word habit, mixed like the word pair; trust `n / (n + 3)`, never more than 80 % | needs the same two words before; one word before never uses it |
| Short memory | a word used in the last hundred or so words is likelier (every use of any word fades all the recent ones by 0.8 %); counts like 1.5e7 occurrences per use, at most 6 | fades by itself; off in the unit tests that need a plain model |
| Retry | a word deleted right after the glide that typed it is the wrong word for this person: if the very next glide (within 6 seconds) gives the same word first again, the next guess goes first and the deleted word stays second in the strip. The replacement then counts as a correction | only when the next guess fits the path about as well (`RETRY_GAP` 1.5); one tap on the strip brings the deleted word back; Diagnostics counts it |
| Page words | the words of the titles of pages visited and bookmarked, and the site names, join the glideable words (a name such as "doraby" can be glided) | local, bounded (1400), nothing from private tabs |

Nothing is learned in a private tab or in a password field (`IME_FLAG_NO_PERSONALIZED_LEARNING` and the field type are honoured), a guess that was picked and deleted at once is not learned, and a word with one letter three times in a row ("awwww", "sooooo") is never learned or added to the glideable words (typed noise polluted other keyboards' lists).

### Measured (simulated person as in section 8; `GlideEval curve=1`; first guess right, %, six stretches of 300 glides, normal finger)
| What is switched on | 1 | 2 | 3 | 4 | 5 | 6 |
| :- | -: | -: | -: | -: | -: | -: |
| nothing learned | 86.7 | 84.0 | 87.3 | 82.3 | 85.3 | 83.3 |
| words and pairs (section 8) | 90.3 | 94.0 | 94.7 | 93.7 | 95.0 | 96.3 |
| + corrections | 90.3 | 94.0 | 95.0 | 94.0 | 95.3 | 96.3 |
| + last two words | 90.3 | 94.0 | 95.0 | 94.0 | 95.3 | 96.3 |
| + short memory | 91.0 | 94.0 | 95.0 | 94.0 | 95.3 | 96.3 |
| (topic bursts, 80 glides on one subject) pairs only / + short memory | 94.7 .. 98.3 / 95.0 .. 98.3 | | | | | |

Honest reading: on this simulated person the word-and-pair model already reaches about 95 %, so the new parts add 0.3 to 0.7 points at most; capping the correction and guarding it by path fit changed nothing in the curve (same numbers with and without), so the guard costs no accuracy. They stay because a real person's language has structure the simulation lacks (the same two words after each other, one subject for a while, the same wrong neighbour word again and again) and because they are bounded. The honest measure is the tablet's Diagnostics line, which now also says how many corrections and three-word phrases were remembered and how often a deleted word came up first again.

### Candidate recall (does the right word reach the careful pass?) `GlideEval gaps=1`, 3000 words, 200 survivors of the quick pass
| finger | first guess right | right word in the first 3 | right word among the 200 careful candidates |
| :- | -: | -: | -: |
| calm (aim 5 dp) | 96.2 | 99.0 | 99.1 |
| normal (aim 8 dp) | 90.5 | 96.9 | 98.6 |
| sloppy (aim 11 dp) | 80.9 | 92.9 | 97.7 |
| bad (aim 14 dp) | 66.3 | 80.4 | 93.2 |

For a normal finger the right word is missing from the careful pass in 1.4 % of glides, so recall is not the main loss (the main loss is the order of near-equal words). Widening the list to 300 would give +0.3 points first guess for +1.1 ms per decode (laptop), 450: +0.5 points for +2.6 ms; kept at 200 until the tablet's "word shown N ms after the lift" says there is room. When the right word was second or third, its path fit the glide almost as well as the first guess's: extra cost per letter vs the first guess, normal finger: 10th percentile -0.81, median -0.12, 90th percentile 0.27, largest 1.02 (sloppy: median -0.05, 90th 0.75). That is why a near-tie guard of 0.9 keeps about 90-95 % of the cases a correction may settle.

### Not done, on purpose (see the table in `research/GLIDE_RESEARCH.md`)
A learned feature-weight re-ranker (no keyboard evidence, drift risk); changing the PREVIOUS word when the next one is typed (surprising edits); tap-offsets per key cluster for glides (no evidence for glides); a replay file of the owner's real glides (personal data in a public repository: the Diagnostics counters are the honest number); speed-aware weights (rare-word bias); case learning.

## 10. Polish and responsiveness (AV3, owner message 36, 2026-11-14)

> "Polish the keyboard and make it as responsive as possible and able to learn and constantly improve so that it accurately predicts the words I'm typing"

| Change | What the person sees | How it is kept cheap |
| :- | :- | :- |
| Live guess while gliding | the strip shows the best words for the path so far (the first one is what would be typed if the finger lifted now), updating as the finger moves | its own thread and its own decoder instance (a preview never touches the real decoder's state and never holds up the real decode on the lift); at most one preview at a time, not more often than every 110 ms and not for every touch point (4 new points at least) |
| Word on the lift | the word is typed as soon as the finger lifts; Diagnostics says how long that took ("word shown N ms after the lift") | the lift's decode goes first; live previews that are still running are simply not waited for |
| Key preview | the key under the finger is drawn large above it (the thumb covers the key itself); a top-row key has no room above, so its bubble sits over the strip | drawn in the keys view's own draw, no new view, no animation |
| Hold a top-row letter | after 0.42 s the large bubble turns into its digit (small digit hints sit in the corner of every top-row key); lifting types the digit | a timer, no extra view |
| Caps lock | double tap on Shift: the key fills with the accent colour, letters and glided words come out in capitals until Shift is tapped again | state in the core model, tested without a screen |
| Page-field row | typing into a page's text field: the bottom row is `123 , . space ' ? Go` (the address row with slash and .com is kept for the address bar) | one more layout in the core, tested |
| Smart spacing | a glide puts a space after its word; a dot, comma, slash, `.com`, apostrophe, hyphen ... right after it takes that space away again ("doraby" then ".com" = "doraby.com") | core model |
| Key vibration | short tick on each key press, a setting (default on, follows the system's touch feedback) | system haptics, no timer |
| Text size | the letters grow and shrink with the key (the keyboard can be resized, section AV1 in `research/MINI_NAV.md`) | one multiplication per key per draw |

### Emulator proof (2026-11-14, Android 10 x86_64 emulator without hardware acceleration, real touches through the Linux input device, lab page `tools/sitelab/fieldlab.py` whose server log shows what the page's field received)
* Page-field row, digit hints and the four corner marks: picture of the open keyboard in a focused page text field shows `123 , . space ' ? Go`, small digits on the top row, the bar says "type into the page".
* Live guess: a picture taken with the finger still down half-way through the glide for "hello" shows the strip already holding `her | he | hd`; after the lift the word `hello ` is in the page field (server log: `input id=free value=hello `) and the strip shows `hello | help | jello`. Diagnostics: `live preview 32 times` over 8 glides.
* Held digit: a picture during a 3 s hold on W shows the large "2" bubble over the strip above the key; the lift typed `2` into the page (log `... h2`); a hold on Q typed `1`.
* Caps lock: two quick taps on Shift fill the key with the accent colour and all letters are capitals; the glide for "world" typed `WORLD ` into the page; one tap on Shift unlocked it.
* Retry: glided "to", deleted it with one Backspace, glided the same path again within seconds: the page received `too` and the strip read `too | to | trio` (the deleted word second); Diagnostics: `a deleted word came up first again and went to second place 1 times`.
* Diagnostics now reads: `Glide typing: 8 glides: best word kept 3 (42 %), another candidate picked 0, deleted right away 4; decode avg 490 ms, worst 709 ms; word shown 524 ms after the lift (worst 718 ms); live preview 32 times, avg 670 ms; ...; learned 2 words and 1 word pairs from your typing ... ; a deleted word came up first again and went to second place 1 times` (these times are the emulator's software drawing and slow CPU, not the tablet's).
* Unit tests with real pixels (native graphics, `MiniNavLookTest`): the bubble is painted above a pressed key, through the panel's own touch dispatch too, and the digit bubble over the strip; it is gone after the lift.
* What was NOT run: a real thumb, the real tablet's speed and feel (the tablet's Diagnostics line "word shown N ms after the lift" and "live preview N times, avg M ms" are the real answer), the haptic tick (the emulator has no vibrator), a long session of learning from real words.
* One oddity, said plainly: on the very first run after installing, the emulator once stopped redrawing the panel for a while (its frame counter stood still while the page kept updating and keys kept typing); four later repeats of the same steps, and a restart, did not show it again. It is recorded here because it could not be explained, not because it was seen on the tablet.

