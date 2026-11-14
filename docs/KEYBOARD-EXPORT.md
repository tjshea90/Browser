# Kestrel keyboard learning file (version 1)

What this is: the one file that holds everything the Kestrel keyboard learned about ONE person's typing. It is written by Settings > "Export what the keyboard learned" and read by
"Import what the keyboard learned" (owner message 38, AX1: "save this learned data in an exportable format because if the keyboard is good I will make it my system keyboard for all apps and I
want to import what it learned"). The reference implementation is `core/src/main/kotlin/app/kestrel/core/input/glide/LearnedExport.kt`; the tests in `LearnedExportTest.kt` pin this
page (the example below is read by a test, so it cannot go stale).

**Privacy.** The file lists words you typed and the shapes of your glides. It is plain text, anyone who has it can read it. It never leaves the tablet by itself: Export writes it to the
Downloads folder, Import reads a file you pick. Do not put an export in a public place (the Kestrel repository is public: never commit one).

## The file

ASCII text, one entry per line, lines separated by a line feed. Empty lines and lines that start with `#` are ignored. The first non-comment line is the name and the version. A reader skips
every line it does not know (a later version may add kinds of line) and refuses a file whose name or major version it does not know.

| Line | Meaning |
| :- | :- |
| `KESTREL-KEYBOARD-LEARNING 1` | name and version; must be the first line (a byte order mark before it is allowed) |
| `habit <x> <y>` | where this thumb lands beside the key it aims at, in key widths (x right, y down); a reader clamps both to -0.6 .. 0.6 |
| `own <word> <times>` | a word the shipped word list lacks that the person typed (a name, a site such as doraby); 1 to 1000 times; lowercase letters only, 3 to 24 long |
| `w <word> <weight>` | how much the person used the word (a kept glide counts 1, a word chosen over the first guess counts 3) |
| `p <before> <word> <weight>` | how often `<word>` followed `<before>` |
| `t <before2> <before> <word> <weight>` | ... and followed those two words |
| `c <guessed> <meant> <weight>` | the keyboard guessed `<guessed>` and the person meant `<meant>` (a correction) |
| `r <word> <recency>` | how lately the word was used; fades with every new word, a reader may ignore it |
| `s <word> <n> u0 v0 u1 v1 ... u15 v15` | the SHAPE of a glide the person drew for the word: 16 points, `n` = how often it was confirmed; see below |
| `end <entries>` | how many `habit`, `own`, `w`, `p`, `t`, `c`, `r` and `s` lines there are; a file without it, or with another number, was cut off or edited (a reader may still use it and says so) |

Words are lowercase `a` to `z` and the apostrophe of a contraction (`don't`), 2 to 24 long (the words in `p` and `t` as context may be 1 letter: `a`, `i`), never a letter three or more
times in a row. Numbers are written with a dot, never a comma, whatever language the device is in. Weights are positive and finite; a reader ignores a line whose number is not.

### The shape of a glide (`s` lines)

The path of a finger over the letter keys is cut into 16 points spaced evenly ALONG the path (not in time: a fast and a slow glide of the same word give the same shape). Each point is written
as two whole numbers, in tenths of a key:

* `u` = how many key widths the point is to the right of the CENTRE of the key Q (so the centre of O is 80, of P is 90),
* `v` = how many ROWS the point is below the centre of Q (the home row is 10, the bottom row 20).

Units are keys and rows, not pixels, so the same numbers mean the same thing on any keyboard size and the file can be read by another keyboard with the same QWERTY letter layout (three letter
rows; the top row Q..P is 10 keys wide, the home row is half a key to the right of it, the bottom row one key). `n` is how often the shape was confirmed (kept, picked from the strip, or retyped
after a deleted glide), at most 12; up to four shapes are kept per word.

How a keyboard uses them (what Kestrel does; another keyboard is free to do it differently):

1. Cut the new glide into the same 16 points. For every candidate word that has shapes, take the distance `d` to each shape: the root of the mean squared distance of the 16 points, in key units.
2. Evidence for the word = the sum over its shapes of `min(n, 12) * exp(-0.5 * (d / 0.35)^2)`.
3. Lift the word's score by `2.5 * ln(1 + evidence)`, at most 4.5, and only when the word's own fit to the path is within 2.2 (key pitch squared, per letter) of the best candidate's.
4. Learning: when a word is kept, add its glide's shape (confirmations +1); when the person picks a word from the strip or types a word in place of a deleted glide that the word could have
   been drawn as, add it with +3. A new shape within 0.3 of a stored one of the same word is merged into it (running mean), otherwise stored; the weakest shape of a word makes room. Every OTHER
   word whose stored shapes look like the glide (similarity above 0.3) then loses 2 x similarity confirmations on a correction (0.7 on a kept glide); a shape under a third of a confirmation goes.

## Bringing a file in (Import)

Import ADDS: for `w`, `p`, `t`, `c` and `r` lines the larger of the file's number and the number already learned is kept, so importing the same file twice changes nothing and a file made
on another device only brings what this one has not seen (or saw less often). Shapes within 0.3 of a stored shape of the same word keep the larger confirmation count; others are added. `own`
words are added when new. The `habit` is taken only while this keyboard has not learned a thumb habit of its own. The tables have the same limits as the learning itself (4000 words, 8000 pairs,
6000 triples, 3000 corrections, 1200 words with shapes, 3000 shapes, 800 own words): a file with more is read as far as the limits go. A file over 3 million characters is refused.
To start from the file instead of adding to what is learned: Settings > "Forget what the keyboard learned", then import.

## What is NOT in the file

The shipped word list and its word pairs (they come with the app), anything from private tabs or password fields (nothing is learned there), the text of anything you wrote beyond the single
words and pairs above, the pages you visited (the words of page titles and site names that join the glideable words are rebuilt from your own history on each device), and any identifier of
you or the device.

## Example

This is a complete, valid file (the shape is the straight line through f, o, r as a keyboard with this layout draws it):

```text kestrel-export-example
KESTREL-KEYBOARD-LEARNING 1
# What the Kestrel keyboard learned from your typing.
habit 0.120 -0.050
own doraby 3
w for 42.00
p so for 3.00
t go so for 1.00
c fir for 4.00
r for 1.500
s for 3.00 35 10 41 9 48 7 54 6 60 4 66 3 73 2 79 0 75 0 68 0 62 0 56 0 49 0 43 0 36 0 30 0
end 8
```
