# Glide typing research: how Gboard and Samsung Keyboard decode and learn, and what to take from it (AV2a, owner message 36, 2026-11-14)

> "research Google gboard and Samsung keyboard because i like how their glide typing works, but they often predict the wrong word and i want this app's keyboard to continuously learn and improve glide typing based on the words i actually type. Do not include Spanish."

Public sources only (every claim cites a source number [G1]...; "inference" marks reasoning of the researcher, not a source claim). Researched by a delegated agent on 2026-11-14 (194 tool calls, 36 minutes), written here unchanged apart from layout; the section "What we do about it" is ours. English only: no Spanish anywhere.

## What we do about it (decisions, in the order of the report's ranked list)
| # | Idea (report) | What we did / will do | Why |
| :- | :- | :- | :- |
| 1 | Personal LM mixing (Fowler: 0.8 background + 0.2 personal cache, ramp with data) | ALREADY the shape of it: `PersonalModel` adds the person's own word mass to the shipped list's probability, bounded (200 uses), and pair / triple trust ramps with how often the first word was seen. Typed words that the list lacks become glideable at once (own words). | strong evidence [A6]; our boost is additive and capped, the simulation (research/GLIDE_TYPING.md section 8 and 9) shows the gain |
| 2 | Safe-learning rules | Done: nothing learned in private tabs or password fields (`IME_FLAG_NO_PERSONALIZED_LEARNING`), a picked guess that is deleted at once is not learned, a deleted word is never learned, "Forget what the keyboard learned" in Settings. New in alpha36: words with a letter repeated three times in a row ("awwww") are not learned. | avoids the harm seen for 3 of 45 users [A6] |
| 3 | Pairwise re-ranker from confirmed corrections | Done in its safest form (item 12): remembered corrections. A full learned feature-weight ranker is NOT built (no published keyboard evidence; risk of drifting). | weak evidence; confusion memory is bounded and explainable |
| 4 | Candidate-recall audit | Measured with `GlideEval` (recall of the quick pass). See research/GLIDE_TYPING.md section 9. | cheap check that the true word reaches the careful pass |
| 5 | Replay harness on real glides | NOT built: a log of the owner's words would be personal data and the repository is public. The Diagnostics counters (kept / picked / deleted, word shown N ms after the lift) are the honest real-use number. | privacy |
| 6 | Higher-order personal context | Done: the last TWO words (personal trigrams, `t` lines), mixed like the pair. | cheap; gain unproven in simulation, kept because real language has structure our simulated person lacks |
| 7 | Next-word post-correction | NOT built (surprising edits, "especially frustrating" when wrong [G2]). | risk |
| 8 | Position-dependent offsets | NOT built yet: evidence for glides is nil, for taps small [G3]; our global thumb habit is the best-supported part. Revisit if the tablet's counters show many neighbour-key misses. | weak evidence |
| 9 | Fit fusion constants on real data | Not possible without real data; the yardstick is simulated (honest limits in section 7 below). | |
| 10 | Speed-aware channel weights | NOT built (rare-word bias risk [A2]). | |
| 11 | Retry-aware demotion (deleted and re-glided: show the rejected word second) | Done in alpha36: right after a deleted glide, if the same word comes up first again it goes to the second place. | AOSP does it [O10]; stops the "same wrong word again" loop |
| 12 | Confusion-pair memory | Done: "guessed A, meant B" counts; a bonus only when A is first again, B is among the others and its shape fit is close; capped; a pick counts 1, a deletion followed by another word counts 0.5. | |
| 13 | Case handling | NOT built. | small |
| 14 | Latency and allocation budget | Done: live preview on its own thread at most every 110 ms and 4 touch points, one at a time, own decoder instance; the real decode on lift is not held up; "word shown N ms after the lift" counted. | strong support |
| 15 | Context vocabulary (page titles) | Done: words of the titles of visited and bookmarked pages join the glideable words (local, bounded, history only: nothing from private tabs). | names |

## Evidence quality (read first)
- Well supported (primary papers or source code; PDFs read as text): Gboard decoder structure and latency budget [G2][G4]. SHARK2 maths [A1][A3]. Tap-offset personalization gains [G3][A5][A7]. Personal language-model gains [A6]. AOSP behaviour (touch correction, throttling, unlearning, forgetting curve) [O1]-[O12].
- Moderate: causes of glide errors (collision counts, OOV data, a few forum threads); Grammarly blogs; FUTO results (a competitor's own benchmarks, not peer reviewed).
- Thin or absent: Samsung internals (one patent plus user-level help). SwiftKey Flow internals (nothing technical found). Any published measurement that corrections adapt glide ranking. Any study moving tap offsets to glides. its/it's and hello/jello (no source). Lore such as "Gboard learns a word after 3 swipes" (search snippet only). Reddit was not accessible to the fetcher and XDA gave nothing, so complaint evidence is a handful of forum threads.
- Method: HTML pages (AOSP, patents, blogs, FUTO) went through a summarising fetcher, so their exact numbers carry extra risk. PDFs were read directly.

## 1. DECODING
- SHARK2 / ShapeWriter [A1][A3]:
  - Each lexicon word is an ideal polyline through its key centers.
  - Pruning comes first: start-to-start and end-to-end distance after normalisation.
  - Shape channel: equidistant resampling, scale/translation normalised, mean point distance.
  - Location channel: absolute positions, a one-key-radius "tunnel" costing zero, and point weights lowest mid-path and rising linearly to both ends (people aim ends more carefully).
  - Each distance becomes a zero-mean Gaussian probability, candidates beyond 2 sigma are dropped, channels multiply (Bayes), then the N-best list is re-ranked by a smoothed bigram turned into a Gaussian.
  - Speed rule: gesture time versus a Fitts-law tracing time (a=83 ms, b=127, stylus values). Faster means recalled shape, so location sigma is inflated by 1+gamma*log2(tn/ta), gamma=2.
  - The paper reports no accuracy figure, only speed [A1].
- Bayes form: choose argmax P(gesture|word)P(word); "unigram frequencies in a lexicon alone provide significant power" [A3]. Gboard writes the same noisy-channel sum, spatial score plus LM score [G3].
- Gboard now [G1][G2][G4]:
  - Taps and glides become a "bi-key" sequence.
  - Spatial model since 2017: one LSTM trained with CTC, replacing a Gaussian plus rules. It was trained partly on reverted autocorrections and suggestion picks. Offline: 15% fewer bad autocorrects, 10% fewer wrongly decoded gestures, 6x faster, 10x smaller [G1].
  - Lexicon transducer with optional repeated keys and apostrophes; n-gram LM (64k words for n-grams, 170k unigrams) searched by beam [G2][G4].
  - Gesture input "typically does not allow OOV recognition" [G2].
  - A 6.4M-parameter neural LM gave only 0.26-1.19% fewer modified words for +17-28% latency [G4].
  - An older patent lists per-frame inputs x, y, key, time, up/down, speed, direction, curvature, with per-character probabilities plus a blank [G5].
- Features:
  - Google's incremental-decoding patent picks "alignment points" from curvature, direction and local/global speed (a slow point or pause suggests a key). It scores key distance plus n-gram cost and weights both by gesture length and speed [G6].
  - First/last keys matter: SHARK2 prunes on them; FlorisBoard keeps words from the 2 nearest keys at each end [O13].
  - AnySoftKeyboard measured candidate recall of 84.9% (first key only), 93.9% (2 nearest start/end keys) and 96.9% (3), on one typist's 61 sentences [O14].
  - Corner-only matching (AnySoftKeyboard's old method) discards most of the path and cannot tell feel from fell [O14].
- Other decoders: FUTO fuses a neural cost, log word frequency and a length term with data-tuned weights, plus an optional small context LM [F1][F2]. Grammarly feeds 33 features per touchpoint (position, 26 key-proximity values, angle, timing, velocity, acceleration) to a seq2seq model and lets the tap-typing autocorrect LM choose the word [I1].
- Candidates shown: patent figures show three slots [G5][G6]; FUTO shows 3 alternatives [F3].
- Published accuracy:
  - Google test (3,000+ gestured words, told to type fast and not fix): gesture WER 11.90% baseline, 11.51% FST, 9.20% with post-correction [G2].
  - Google Keyboard lab study, corrected character error: 2.04-2.34% glide versus 1.09-1.11% tap [A8].
  - FUTO benchmark (20k swipes, emulator, self-run): top-1 error 7.38% FUTO, 11.05% Gboard, 10.82% iOS, 13.12% HeliBoard with Google library. Top-4 error 4.19% versus Gboard 5.66% [F3].
  - FUTO's own SHARK2 re-implementation on real swipes (lexicon extended with the test words): top-1 80.05%, top-3 90.47% [F1].
- Samsung:
  - Help pages say predictive text "learns your typing habits" and offer "Erase personalized predictions"; neither documents swipe decoding [V1][V2].
  - Patent US11188158 (priority 2017) describes a GRU+CTC network over key and trajectory coordinates normalised by keyboard size, refined by the user's swipes and chosen words. A patent does not prove the product works so [V3].
  - AOSP has no gesture decoder (null factory pointer); HeliBoard says glide typing needs a closed library [O4][O15].
  - SwiftKey Flow: no technical description found.

## 2. PERSONALIZATION
- Gboard: on-device adaptation framework; touch data stored in unordered buckets so typed text cannot be rebuilt [G3]. Help pages: Dictionary > Delete learned words; Advanced > Personalization; saved words stay on device [G10]. Federated learning trains global LMs and discovers OOV words without uploading text (central DP epsilon 0.315 for en-US) [G7][G8][G9]. An offline keyboard cannot use that, so local learning must carry the load.
- SwiftKey: "remembers words that are important to you and how likely you are to use certain words together" and "adapts to the positions of your keystrokes"; nothing leaves the device by default [V7]. Its personalization page adds only that it learns from what you write across apps [V8]. Touchtype patent: per-key 2D Gaussian, Normal-inverse-Wishart prior, incremental MAP updates, forgetting by capping hyperparameters, updated when the user selects a prediction or the top one is auto-accepted [V9].
- Samsung: words saved as "learned words" as you type, tap-hold a suggestion to remove, "Erase personalized predictions" [V1]. Patent: user data (messages, contacts, SNS text, with app permission), repeated proper nouns weighted more [V3]. Whether glided words are learned: undocumented.
- AOSP open-source baseline [O8][O11][O9]:
  - User history stores unigrams and n-grams with timestamps.
  - Forgetting curve with levels 0-15, visible only from level 2 (roughly two uses, inference from constants), +1 per use, minus one level per 15 days, decay pass every 2 hours.
  - Unrecognised words are stored "invalid".
  - Contacts dictionary only with READ_CONTACTS.
  - Auto-capitalised words are stored lowercase unless only valid capitalised.
  - No gesture-specific condition in the commit code I could see.
- Do corrections feed back?
  - Gboard: aggregated and offline. Reverted autocorrects and picks train the global spatial model [G1]. Per-user spatial statistics use only tap-typed words that "match the final committed text", so corrected words are excluded. Glide personalization is future work needing "a materially different form of adaptation" [G3].
  - AOSP: backspace right after a glide unlearns the rejected word (removes it from user history). A reverted autocorrection unlearns the committed word [O9][O8]. If the same word tops the next glide it moves to slot 2 [O10].
  - Samsung patent: chosen words refine the model [V3].
  - No per-user glide re-ranking with measured gains is published. One forum user says Gboard "has learned" over time (anecdote) [V10].
- Known problems:
  - Misspellings get learned; users with under 1,500 words of history got worse (3 of 45) [A6].
  - Grammarly filters noisy input ("awwww", "cant"), learns every new word but defers suggesting it until seen enough, and saw "iTunes" learned [I2].
  - AOSP unlearning is blunt (deletes the word everywhere) [O8].
- Numbers [A6]: a unigram cache mixed with the background LM (0.8 background, 0.2 cache, unlimited window) cut WER 5.7% -> 4.6% (19.3% relative) and OOV 1.6% -> 0.9%. 42 of 45 users improved (2.9-30.9% relative). Exponential decay (half-weight 2,300 words) was no better than a uniform cache. PPM/RNN personal LMs beat a unigram cache and cut WER 36% relative versus a static LM [A11].

## 3. FAILURE MODES (cause - evidence - remedy, published or plausible)
- Path collisions. In a 20k lexicon on QWERTY, 1,461 pairs share a normalised shape and 537 also share start and end keys (tip/top, refuge/refugee, webb/web, traveled/travelled) [A1][A3]; pit/pot/put [G2]; god/good, food/good [I1]. Remedy: n-gram context and the N-best strip (published [A1][G2]); personal frequency (published [A6]).
- Neighbour-key errors at the ends. "'had' when I want 'has'; 'do' when I want 'so'" [V10]; hello/jello is the same pattern (h and j adjacent; no source). Remedy: let the LM arbitrate end errors (published [A1]); personal offset (published for taps [G3]); next-word post-correction (published [G2]).
- Short words and fast glides. SHARK2's author warns speed-driven location weighting can let a rare word beat a common one, e.g. "as" versus "cop", "coop", "co" on ATOMIK [A2]. Remedy: keep the LM prior strong for short words (published caution).
- LM too strong or too weak. False corrections such as "you're far" -> "you're fat" [G2]; always-correct gave 3.5% false corrections, under 1% at a 0.7 threshold [A6]. Glides have no literal fallback [G2]. Remedy: confidence thresholds and extra cost for changing an already valid word (published). Bigger neural LMs help little [G4].
- OOV and names. 44 OOV words touched 4.46% of test sentences, and glide suffered more than tap [A8]. Remedy: add typed words to the glide lexicon (published practice [A6]); generative OOV model or "interactive lexicon" (proposed [A8]); weight repeated proper nouns (patent [V3]).
- Double and collinear letters. Dropped repeats ("mashhuur" -> "mashuur"), letters on one row ("badhoge" -> "bahoge"), errors rising with length (Indic, simulated traces) [A13]; feel/fell share corners [O14]; stream/steam give low-confidence middle letters [F1]. Remedy: optional repeated keys (published [G2]); loop and dwell cues (AnySoftKeyboard added a loop rule [O14]).
- Homographs (its/it's): no source. Gboard's lexicon makes the apostrophe optional [G2]; plausible remedy: both forms plus a context bigram.
- Sloppy glides are "faster but also more error-prone" [A3]; remedy: speed-aware weights (published mechanism [A1]).
- Anecdotes only: Samsung 2020 thread, four tries for "sometime" and unrelated words (aubergine, dungeons, skein) [V4]; a 2025 thread has no detail [V5]; Android Central 2018 [V10]. A "One UI 8 fixed it" article cites nothing and uses doubtful setting names; ignore [V6].

## 4. SPATIAL ADAPTATION
- Gboard taps [G3]:
  - Per-key-cluster mean offsets (7 clusters from a greedy split), last 800 taps in 4 buckets, trained only on tap-typed words matching final text.
  - Against an unchanged control, personalized arms moved modified-word ratio by -0.05% to -1.9% (one locale +0.22%) and WPM by +0.2% to +0.6%.
  - Retuning sigma alone captured part of it, and personalization beat the best non-personalized arm on at least one metric in all but one locale.
  - More data helped with diminishing returns; decay and full covariance gave nothing; arms had 90M+ words.
  - "Gboard users do not generally type by pressing directly on physical centers of keys."
- Yin et al. [A5]:
  - Backoff chain general -> key -> posture+key -> user+key; 50 points per key-user minimum.
  - User+key models cut character error about 14% (intended key known).
  - A prototype with an 86.4%-accurate posture classifier cut it 8.64% -> 7.50% (13.2%), 30 right-handed participants, no LM.
  - Left-key offsets flip sign between one-finger and two-thumb.
- Henze et al. [A7]: 47.8M keystrokes, 72,945 installs; a fitted shift beat Android's native shift by 2.2% speed and 9.1% fewer errors. Buschek et al.: about 60 touches suffice, over 8% accuracy gain [A9]. Weir et al.: Gaussian-process touch model, up to 7% (versus a baseline) and 1.3% (versus a leading commercial keyboard) lower character error [A10].
- AOSP [O5][O6]: static per-row y offset and radius ("x offset is obsolete"); no learning in any file opened; no "key coordinate learning" found.
- Fit to our decoder: the existing global "second look" offset matches the best-supported part (Henze's device-wide skew); Gboard's finer 7-cluster offsets gave only small gains.
- Tap -> glide transfer: no published study. Gboard's authors expect glide error to depend on previous and next key locations and corner shapes [G3]. Inference: only a global (posture/device) component is likely to transfer; per-key parts will not.

## 5. LEARNING FROM FEEDBACK
- Pairwise preferences are "reliably inferred from implicit feedback" (dueling bandits) [A16]; perceptron training of structured decoders [A15]; learning from top-k-only feedback has O(T^(2/3)) regret bounds, relevant to a 3-slot strip [A17]. No keyboard learning-to-rank paper with measured gains found.
- In practice: reverts and picks as labels for the global model [G1]; touch-model update on selection or auto-accept [V9]; rejection demotion and unlearning [O9][O10]; selected words in Samsung's patent [V3].
- Safeguards seen: fall back to the base model until enough data (50 per key [A5]; about 60 touches [A9]; 250-800 taps [G3]); two-use rule [O11]; noise filters [I2]; capped hyperparameters [V9]; delay cache use until enough text [A6]. Regularising weights toward defaults and clipping drift: inference.
- Confusion pairs: no published result; AOSP's one-step demotion of a rejected word is the nearest analogue [O10].
- Recency and topic: the decaying cache was no better than a uniform cache [A6]; no evidence found for short-term topic boosts.
- Context: SHARK2 used bigrams, trigrams a "future extension" [A1]. Gboard's n-gram LM is "limited in context length" and neural LM gains were small [G4]. Using the next word to revise the previous one cut gesture WER 11.51% -> 9.20%, but false post-corrections are "especially frustrating" [G2].
- Context vocabulary and privacy:
  - Gboard rescoring supports typed n-grams and contact lists, with OOV words spliced in [G2].
  - AOSP's contacts dictionary needs READ_CONTACTS plus a setting [O8]; Samsung's patent requires app permission [V3]; SwiftKey keeps learning on device unless the user opts into an account [V7].
  - Android defines IME_FLAG_NO_PERSONALIZED_LEARNING (API 26), naming browser private modes, not guaranteed [D1].

## 6. LATENCY AND RESPONSIVENESS
- Targets:
  - Visible feedback within about 20 ms; LM size 5-10 MB [G2].
  - About 17 ms per gesture at 60 Hz [I1].
  - SHARK2 searched 20,000 templates in 20-40 ms on an 800 MHz PIII [A1]; under 20 ms with 50,000 words in 450 KB on a 168 MHz phone CPU [A3].
  - FUTO's 635k-parameter encoder: 1.5 ms (A76) to 7.6 ms (A55) p50, 10-14 ms end-to-end on A55 [F1].
  - Gboard accepted +17-28% decode latency for a better LM [G4].
- Incremental decoding:
  - Google shows the word "right at the fingertip", often before the gesture ends [G11]. Tokens walk a lexicon trie, keep only the top-n per step, and are revised at each alignment point [G6].
  - AOSP refreshes the preview only when new points exist and at least 100 ms passed, sampling at 1/6 key width (defaults "intended for testing") [O1][O2][O3].
  - FlorisBoard skips points under keyWidth/4, caps at 500 points, caches 5 results against "micro pauses" [O13].
  - FUTO: beam 10/100/400 gives 88.7%/92.9%/93.2% top-1 [F1].
- UI: AOSP shows pressed-key graphics and a key-preview popup on touch-down (suppressed during glides) and sends the key code on touch-up; haptics via KEYBOARD_TAP or a fixed vibration [O7][O12]. Android docs: allocations can trigger garbage collection and stutter; none during animation [D2].

## 7. EVALUATION
- Real data: FUTO set, MIT: 1,043,789 swipes, 12,278 donor sessions, prompted sentences on a web QWERTY, split by donor. Coordinates are normalised and key geometry is not described (how to align: unverified) [F1][F4]. "How We Swipe": 1,338 users, 11,318 words (per Grammarly) [I1]. Google's test sets are not public.
- Simulators:
  - Gaussian control points with interpolation (sigma 0.25*sqrt(key area), 50 points); the authors warn untrained models "can only be used for relative keyboard comparisons" [A12].
  - Minimum-jerk plus Gaussian endpoint and via-point noise [A13]; Quinn and Zhai's minimum-jerk model with statistical via-points [G12]; FUTO's layout search used minimum-jerk swipes [F5].
  - In a mid-air study, learned generators matched real micro-statistics better than minimum-jerk [A14].
  - No named "Kristensson-Zhai noise model" found in sources that could be opened.
- Metrics: top-1/3/10 [F1]; top-1 and top-4 error [F3]; sentence WER, false-correction rate [G2][A6]; corrected CER [A8]; live proxies WMR and WPM [G3].
- Honest versus misleading:
  - Report sensitivity to the noise level [A12].
  - Add no test words to the lexicon unless OOV is also reported (FUTO does, to isolate spatial decoding [F1]; real OOV hit 4.46% of sentences [A8]).
  - Weight words by frequency and use sentence context, as Fowler's closed-loop simulation does with real per-user text (45 users, primed on 30 days) [A6].
  - Show per-user curves, not means (3 of 45 worse) [A6].
  - Keep tuning and test sets apart (FUTO flags possible overlap) [F1].
  - Lab and field differ [A8][A4]. Effects of 0.2-1% needed 90M+ words per arm at Google [G3].
  - Inference: a paired replay on one user needs about 2,350 glides to see a 1-point gain at 90% when 3% of glides differ, versus about 13,500 per arm unpaired.

## WHAT THIS MEANS FOR OUR DECODER (the researcher's ranked list; our decisions are in the table at the top)
1. Personal-LM mixing to Fowler's recipe. Idea: P(w) = 0.8 background + 0.2 personal unigram cache, unlimited window, slow or no decay; personal weight ramps from 0 until about 1,500 learned words; typed OOV words become glideable at once; tune the mix on held-out likelihood of the user's own committed words. Benefit: 19.3% relative WER cut in a Google simulation; maybe 1-2 points of first-guess error (inference), less if our counts already do this. Cost/risk: low. Source: [A6]. Support: strong (simulation, tap decoding).
2. Safe-learning rules (protects #1 and #3). Idea: new word glideable at once, suggested after 2 uses; reverts and rejections drop this session's entries; filter repeated-letter noise; honour IME_FLAG_NO_PERSONALIZED_LEARNING and password fields; decay in months, not days; "forget this word" and "erase learned data" controls. Benefit: avoids the harm seen for 3 of 45 users and lexicon pollution. Cost/risk: low. Source: [O11], [D1], [I2]. Support: strong for the practices; thresholds are the researcher's inference.
3. Pairwise re-ranker from confirmed corrections. Idea: 8-12 features per candidate (shape, location, first/last-key distance, length, unigram, bigram, personal unigram/bigram, recency). When the user picks j over shown top i (or retypes a listed word) and does not undo, w += eta*(phi_j - phi_i); decay toward defaults; clip each weight to 0.5x-2x; cap drift per session. Benefit: the requested "learns from what I correct"; guess 5-10% relative error cut after a few hundred corrections (inference). Cost/risk: small code, noisy labels. Source: [A16], [G1], [O9]. Support: weak (no keyboard study) plus inference.
4. Candidate-recall audit. Idea: log whether the true word is among the ~200 candidates; if recall is under 99%, widen first/last-key sets to the 2-3 nearest keys, distance-weighted. Benefit: up to several points if recall is the bottleneck, none otherwise. Cost/risk: more CPU per glide. Source: [O14], [O13]. Support: weak (one typist).
5. Replay harness on real glides. Idea: with consent, store locally path, times, shown list, final word and kept/picked/retyped; replay every change; sanity-check on FUTO's MIT set. Benefit: stops tuning to a rosy simulator (a SHARK2-class decoder scored 80% top-1 on real swipes). Cost/risk: storage, privacy, mapping dataset coordinates (unverified). Source: [F1], [A12]. Support: strong for the need; gain is indirect.
6. Higher-order personal context. Idea: hashed personal trigram or PPM-style backoff from typed text, mixed like #1; its/it's and ive/I've share one key path with an optional apostrophe, context decides. Benefit: personal PPM/RNN cut WER 36% relative versus a static LM in simulation, but Google's longer global context gave only 0.26-1.19%. Cost/risk: memory, sparse data. Source: [A11], [G4]. Support: weak (simulation, taps).
7. Next-word post-correction. Idea: keep the last word's top-5 for a few seconds; when the next word commits, rescore the pair with the bigram; replace only the latest word, only with a large margin, with undo. Benefit: gesture WER 11.51% -> 9.20% at Google; it fixes the previous word, not the current first guess. Cost/risk: moderate; surprising edits. Source: [G2]. Support: strong (small test set).
8. Position-dependent offsets from accepted glides. Idea: from DP alignments of kept glides, collect per-key residuals, pool into 3-7 clusters, shrink to the global offset (about 50 samples), separate portrait/landscape, keep ~800 points in 4 buckets, no decay. Benefit: tap evidence is small (WPM +0.2-0.6%), Yin saw -13% character errors; for glides unknown (inference 0.5-1 point). Cost/risk: moderate; wrong alignments reinforce errors, so train only on accepted words. Source: [G3], [A5]. Support: weak (taps only, none for glides).
9. Fit fusion constants on real data, with a length term. Idea: black-box tune weights, per-letter normalisation and a length bonus (FUTO: s = -CTC/L^gamma + lambda*log f + beta*L). Benefit: unknown; guards short-word bias. Cost/risk: offline compute; overfits if data are few. Source: [F1]. Support: weak (competitor's report).
10. Speed-aware channel weights. Idea: estimate tracing time from the path, scale location sigma by 1+gamma*log2(tn/ta), refit a, b for thumbs on this tablet. Benefit: small-moderate for fast familiar words. Cost/risk: low; rare-word bias [A2]. Source: [A1]. Support: weak (mechanism published, no ablation).
11. Retry-aware demotion. Idea: if a glided word is deleted and re-glided within about 3 s, show the rejected word in slot 2 once and record (rejected, chosen). Benefit: fewer stuck loops, no first-attempt change. Cost/risk: tiny. Source: [O10]. Support: inference (AOSP does it; gain unmeasured).
12. Confusion-pair memory. Idea: counts n(A->B) for "shown A, chose B"; bonus min(cap, c*log(1+n)) when A is top and B in top 3 with a small shape gap; require 2 events on 2 days; decay. Benefit: habitual confusions only. Cost/risk: low; overfits one session. Source: [O10] analogue. Support: inference.
13. Case handling. Idea: learn the case form the user ends with; one-tap case toggle for the last glide (ShapeWriter "Case key"). Benefit: small. Cost/risk: low. Source: [A3], [O8]. Support: weak.
14. Latency and allocation budget. Idea: live preview from a coarse pass at most every 100 ms after 1/6 key of movement; full DP on lift; cap about 500 points; preallocate; 5-entry result cache. Benefit: smooth UI on a Cortex-A53, not accuracy. Cost/risk: low-moderate; profile on the tablet. Source: [O2], [O13], [D2]. Support: strong.
15. Context vocabulary (contacts, page titles). Idea: opt-in, local, bounded, off in private tabs. Benefit: small, names. Cost/risk: moderate; privacy, junk words. Source: [O8], [G2], [D1]. Support: weak.

## SOURCES
P = primary, S = secondary. "read" = PDF converted to text and read; "summ." = page passed through a summarising fetcher; "abstract" = only the abstract or first pages read.

Google / Gboard
- [G1] https://research.google/blog/the-machine-intelligence-behind-gboard/ - 2017 blog: LSTM+CTC spatial model, training signals, 15%/10% gains. P, summ.
- [G2] https://arxiv.org/pdf/1704.03987 - Ouyang et al., FST keyboard decoder: WER table, latency, dynamic models. P, read.
- [G3] https://arxiv.org/pdf/2209.11311 - Sivek and Riley, spatial model personalization in Gboard. P, read.
- [G4] https://arxiv.org/html/2410.15575v1 - Neural Search Space in Gboard Decoder: n-gram sizes, neural LM gains, latency. P, summ.
- [G5] https://patents.google.com/patent/US9678664B2/en - Google patent, neural network keyboard decoding. P (patent), summ.
- [G6] https://patents.google.com/patent/US8782549B2/en - Google patent, incremental feature-based gesture decoding. P (patent), summ.
- [G7] https://research.google/blog/federated-learning-collaborative-machine-learning-without-centralized-training-data/ - federated learning blog. P, summ.
- [G8] https://arxiv.org/abs/1903.10635 - federated learning of OOV words. P, abstract.
- [G9] https://arxiv.org/abs/2404.11607 - private federated OOV discovery for Gboard. P, abstract.
- [G10] https://support.google.com/gboard/answer/9058584 - Gboard help: Dictionary, Delete learned words, Personalization. P.
- [G11] https://research.google/blog/mobile-interaction-research-at-google/ - 2013 post: word shown at the fingertip. P.
- [G12] https://research.google/pubs/modeling-gesture-typing-movements/ - Quinn and Zhai, minimum-jerk gesture model, abstract page. P.

Academic
- [A1] https://pokristensson.com/pubs/KristenssonZhaiUIST2004.pdf - SHARK2 full paper. P, read.
- [A2] https://pokristensson.com/pubs/KristenssonUIST2004.pdf - Kristensson's short SHARK2 summary (short-word caveat). P, read.
- [A3] https://static.googleusercontent.com/media/research.google.com/en//pubs/archive/39190.pdf - Zhai and Kristensson, CACM word-gesture keyboard. P, read.
- [A4] https://www.shuminzhai.com/post/a-guided-tour-of-my-recent-papers-on-keyboard-research - Zhai's index of keyboard papers. S, summ.
- [A5] https://static.googleusercontent.com/media/research.google.com/en//pubs/archive/41930.pdf - Yin et al., hierarchical spatial backoff. P, read.
- [A6] https://static.googleusercontent.com/media/research.google.com/en//pubs/archive/43272.pdf - Fowler et al., LM personalization simulation. P, read.
- [A7] https://www.uni-ulm.de/fileadmin/website_uni_ulm/iui.inst.100/1-hci/hci-paper/2012/2012-henze_observational.pdf - Henze et al., 47.8M keystrokes. P, abstract.
- [A8] https://www.pokristensson.com/pubs/ReyalEtAlCHI2015.pdf - Reyal et al., Google Keyboard lab and field study. P, read.
- [A9] https://www.dcs.gla.ac.uk/~rod/publications/BusRogMur13.pdf - Buschek et al., user-specific offset models. P, abstract.
- [A10] https://www.keithv.com/pub/uncertain/WeirEtAlCHI2014.pdf - Weir et al., uncertain text entry. P, abstract.
- [A11] https://www.keithv.com/pub/personalization - Vertanen et al., PPM/RNN personalization. P, summ.
- [A12] https://ar5iv.arxiv.org/html/1503.06300 - Conway and Sangaline, Monte Carlo gesture evaluation. P, summ.
- [A13] https://arxiv.org/pdf/2203.14049 - Biju et al., Indic gesture typing, simulated traces. P, read.
- [A14] https://www.pokristensson.com/pubs/ShenEtAlISMAR2021.pdf - Shen et al., simulating mid-air trajectories. P, read.
- [A15] https://aclanthology.org/W02-1001/ - Collins 2002 perceptron paper, bibliographic page only. P.
- [A16] https://mlanthology.org/icml/2009/yue2009icml-interactively - Yue and Joachims, dueling bandits. P, abstract.
- [A17] https://arxiv.org/abs/1608.06408 - Chaudhuri and Tewari, ranking with top-k feedback. P, abstract.

Open-source keyboards (AOSP base: https://android.googlesource.com/platform/packages/inputmethods/LatinIME/+/refs/heads/main/ ; all P, summ.)
- [O1] java/src/com/android/inputmethod/keyboard/internal/ - directory listing of gesture classes (BatchInputArbiter, GestureStroke*, floating preview).
- [O2] .../keyboard/internal/GestureStrokeRecognitionParams.java - 100 ms, 1/6 key defaults.
- [O3] .../keyboard/internal/BatchInputArbiter.java - update trigger by elapsed time.
- [O4] native/jni/src/suggest/policyimpl/gesture/gesture_suggest_policy_factory.cpp - null factory; no gesture decoder.
- [O5] .../keyboard/internal/TouchPositionCorrection.java - static per-row correction.
- [O6] .../keyboard/ProximityInfo.java - sweet spots, no learning.
- [O7] .../keyboard/PointerTracker.java - press on down, commit on up.
- [O8] .../latin/DictionaryFacilitatorImpl.java - user history, unlearning, capitalisation, contacts.
- [O9] .../latin/inputlogic/InputLogic.java - revert and rejection unlearning (partly read).
- [O10] .../latin/Suggest.java - rejected suggestion demoted one slot.
- [O11] native/jni/src/dictionary/utils/forgetting_curve_utils.cpp (and .h) - levels and decay constants.
- [O12] .../latin/AudioAndHapticFeedbackManager.java - haptics.
- [O13] https://raw.githubusercontent.com/florisboard/florisboard/master/app/src/main/kotlin/dev/patrickgold/florisboard/ime/text/gestures/StatisticalGlideTypingClassifier.kt - FlorisBoard classifier; may be commented out on master. P, summ.
- [O14] https://github.com/AnySoftKeyboard/AnySoftKeyboard/pull/1870 - community gesture-typing write-up, one typist. S, summ.
- [O15] https://github.com/HeliBorg/HeliBoard - README: glide typing needs a closed library. P, summ.

FUTO and industry
- [F1] https://arxiv.org/html/2606.25247 (abstract: https://arxiv.org/abs/2606.25247) - FUTO Swipe report: dataset, SHARK2 baseline, latency. P (self-published), summ.
- [F2] https://swipe.futo.tech/ - FUTO product page; no extra facts used. Vendor, summ.
- [F3] https://github.com/futo-org/android-keyboard/releases/tag/0.1.29 - FUTO release notes with benchmark table. Vendor, summ.
- [F4] https://huggingface.co/datasets/futo-org/swipe.futo.org - dataset card. P, summ.
- [F5] https://futo.tech/blog/swipe-keyboard - layout search with synthetic swipes. Vendor, summ.
- [I1] https://www.grammarly.com/blog/engineering/deep-learning-swipe-typing/ - neural swipe decoder, 17 ms target, datasets. Industry, summ.
- [I2] https://www.grammarly.com/blog/engineering/personal-language-model/ - on-device personal LM: filters, deferral, decay. Industry, summ.

Vendors, forums, Android docs
- [V1] https://www.samsung.com/us/support/answer/ANS10013525/ - Samsung help: learned words, erase predictions. P.
- [V2] https://www.samsung.com/latin_en/support/mobile-devices/how-to-find-the-useful-features-of-the-samsung-keyboard-on-your-galaxy-phone/ - Samsung help: features, no swipe decoding. P.
- [V3] https://patents.google.com/patent/US11188158 - Samsung patent, swipe decoding with a trained model. P (patent), summ.
- [V4] https://community.samsung.com/ca/conversations/others/swipe-typing-word-prediction-needs-serious-work/r1_7212291 - 2020 forum thread. S.
- [V5] https://community.samsung.com/us-bp/conversations/Suggestions/Fix-swipe-to-type/us_3184314 - 2025 thread, no detail. S.
- [V6] https://geekchamp.com/?p=244087 - uncited One UI 8 claims; unreliable. S.
- [V7] https://support.microsoft.com/en-au/topic/microsoft-swiftkey-keyboard-sharing-your-typing-data-faq-d737059d-8810-448e-b376-9af56171a37d - SwiftKey learning and data FAQ. P, summ.
- [V8] https://support.microsoft.com/en-us/topic/f48b6267-7dc6-49c5-8794-8fa629f9748e - SwiftKey personalization page, thin. P.
- [V9] https://patents.google.com/patent/US9639266B2/en - Touchtype patent, touch-model learning. P (patent), summ.
- [V10] https://forums.androidcentral.com/threads/getting-tired-of-making-corrections-using-gboard-glide-typing.898124 - 2018 thread. S.
- [D1] https://developer.android.com/reference/android/view/inputmethod/EditorInfo - IME_FLAG_NO_PERSONALIZED_LEARNING. P.
- [D2] https://developer.android.com/develop/ui/views/layout/custom-views/optimizing-view - avoid allocations. P.

Opened with no usable content: the Google Research page for the LSTM keyboard gesture recognition paper (abstract blank), the St Andrews SHARK2 record. Tried but refused or failing: ACM Digital Library (WordGesture-GAN), Reddit mirrors, Taylor and Francis (Quinn and Zhai full text), SamMobile, a mirror of Deber et al. on touch-latency perception; so no latency-perception thresholds are claimed.
