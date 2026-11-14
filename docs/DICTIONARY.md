# The glide-typing word list: sources, licence, how it is made (AT1)

The mini keyboard's glide typing (owner message 34) needs a list of words with how common each one is, and which words follow which. It ships as ONE binary file,
`geckoapp/src/main/assets/glide/en.klx` (about 2.7 MB; only the Kestrel / GeckoView build has the mini keyboard, so only its APK carries the file).

## What is in it

| Part | Source | Licence |
| :- | :- | :- |
| 82,833 English words with counts (`frequency_dictionary_en_82_765.txt`) | SymSpell project (Wolf Garbe), shipped inside the `symspellpy` 6.10.2 Python package (a wheel from PyPI) | MIT |
| 242,342 word pairs with counts (`frequency_bigramdictionary_en_243_342.txt`) | same | MIT |
| about 300 hand-written extra words and the apostrophe words (`tools/glide/extra-words.txt`) | written for this project (web names, modern words, contractions such as `don't`) | none needed: our own list |

How the SymSpell project describes the data (their README, NOT re-checked here): counts taken from the Google Books Ngram data, limited to words that also appear in the SCOWL
spell-checker word lists. The files carry no licence of their own beyond the package's MIT licence, which is printed below; the package's metadata names the MIT licence as the licence
of the package that contains them.

SHA-256 of the two source files as downloaded: `68e9dc81c7e73bd7310b57e516ecaea0d8b6387ff71344a57c04174650a407a7` (words), `fd892a160184101dd7ae807ac5a302d01fcea1c47304181a8ed7ed9c94545bcd` (pairs).

## The licence text that has to travel with the copies (MIT, from the symspellpy 6.10.2 wheel)

```
MIT License

Copyright (c) 2025 mmb L (Python port https://github.com/mammothb/symspellpy)
Copyright (c) 2021 Wolf Garbe (Original C# implementation https://github.com/wolfgarbe/SymSpell)

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

(The same stance as `docs/FILTER-LISTS.md`: personal, non-commercial use; if the APK or the repository is ever redistributed, keep this notice with the list.)

## How the file is made (reproducible, nothing runs the downloaded code)

1. Fetch the package as DATA into a new empty folder OUTSIDE the repository: `python3 -m pip download symspellpy==6.10.2 --no-deps -d <empty folder>`.
2. Read only the two `.txt` files out of the wheel (it is a zip): `python3 -I -m zipfile -e <wheel> <another empty folder>` or Python's `zipfile` with `extract` on those two names. Never import or run anything from it.
3. From the repository root:
   `./gradlew :tools:glideLexicon --args="--out geckoapp/src/main/assets/glide/en.klx --words <folder>/symspellpy/frequency_dictionary_en_82_765.txt --pairs <folder>/symspellpy/frequency_bigramdictionary_en_243_342.txt --extra tools/glide/extra-words.txt"`
4. `./gradlew :core:test --tests 'app.kestrel.core.input.glide.*'` (the accuracy tests read the file and fail when it is missing or too small).

The tool is `tools/src/main/kotlin/app/kestrel/tools/GlideLexiconCompiler.kt`; the file format and its reader are `core/.../input/glide/Lexicon.kt` (`LexiconFile`), tested together in `LexiconFileTest`.
A word in `extra-words.txt` gets AT LEAST its count; one that is not in the frequency list is added. Words are lowercase a-z (an apostrophe is kept for typing and dropped for the finger's path).

## What it cannot do (so nobody assumes it)

* English only. The owner also visits Spanish-language sites (pelisjuanita.com): Spanish words are NOT in the list, so a Spanish word will be glided into the closest English one. A second list (a Spanish
  frequency dictionary with accents shown but typed on the plain letters) is the way, and needs the owner's word (see TASKS AT1).
* Names, brands and slang that are not in the list cannot be glided: the keyboard remembers words that are typed or picked (AT1f) and the host names of visited sites, so they join the vocabulary after a first use.
