# Video download sizes: why they were off, what changed (AO2, owner message 29, 2026-11-13)

Owner: "the video downloader is inaccurate at estimating file size. Is there any way to improve it?"

## Where a size comes from (each kind of video)

| Source | Before alpha33 | Now |
| :- | :- | :- |
| A plain file (MP4 etc.) | exact: `Content-Length` / `Content-Range` of a one-byte request | unchanged (already exact) |
| HLS where every part names its byte range (`EXT-X-BYTERANGE`) | exact: sum of the ranges | unchanged |
| HLS **master** playlist (the usual one: several qualities) | `BANDWIDTH` x length. `BANDWIDTH` is the PEAK rate of any part, so a variable-rate video comes out 20 to 50 % too big | 8 parts of the lowest AND of the sharpest quality are measured (a one-byte `Range` request each: the reply says how long the part is, no video data is fetched); every quality is scaled by the measured/declared ratio, interpolated between the two measured ones (`SizeEstimate.factorFor`); list is redrawn when the answers are in |
| HLS **media** playlist alone (no master) | 2 parts measured (first, middle), count-weighted | 8 parts spread over the timeline, weighted by each part's length |
| DASH (`.mpd`) | `bandwidth` x length (+ sound's) | unchanged: the DASH `bandwidth` is usually the average already; not measured |
| HLS with the sound in a list of its own | declared rate x length (the declared rate covers picture + sound) | unchanged: the measured parts would only be the picture, so no scaling is applied |
| While downloading | (finished parts' bytes / number of finished parts) x number of parts | bytes of finished parts per SECOND of video they cover x the whole length, blended with the figure shown at the start until 12.5 % of the video is in (`SizeEstimate.running`) |

The final MP4 of a transport-stream (`.ts`) stream is a little smaller than the stream itself; `SizeEstimate.total` uses 0.95.

## Measured, not assumed

1. **TS to MP4 ratio**, real ffmpeg H.264+AAC HLS output (3 synthetic sources x 2 quality levels, 60 s) through Kestrel's own writer (`TsToMp4`): the MP4 is 0.925 to 0.974 of the stream (0.95 in the middle). With 0.95, the estimate from measured parts was off by -2.5 % .. +2.7 % on those six files (all parts measured, so that is the factor's error alone).
2. **Peak versus average** (synthetic ladder, 300 random variable-rate encodes of 1 hour, parts +-35 % around the average, declared peak 1.35 to 1.50 x the average, as on real sites; `SizeEstimateTest`):

| Quality | Before (declared x length) | After (8 parts of 360p and 1080p measured, others interpolated) | Worst of 300 |
| :- | :- | :- | :- |
| 360p | 35 % too big | 5.6 % off | 19 % |
| 480p | 40 % | 4.5 % | 17 % |
| 720p | 45 % | 4.2 % | 15 % |
| 1080p | 50 % | 6.0 % | 18 % |

   These are MODEL numbers (my assumption about how peaks sit above averages), not measurements of a real site. What was measured is item 1 and the engine behaviour below.
3. **While it downloads** (`StreamEngineTest`): a 56 % too high first guess is within 10 % once 40 % of the file is in; a video whose first parts are short and later ones long (same rate) is sized correctly from the first two parts (counting parts would have halved it).

## Not done, on purpose

- No measurement for DASH or for HLS with separate sound: a wrong correction there would be worse than the declared figure.
- No "size as a range" display: the row still says "about 210 MB". Sizes already say "about" when they are not exact.

## How to see the real error on the tablet (Diagnostics, copyable)

- `Video size estimates:` lists the last 8 finished stream downloads as `#n shown 210 MB, finished 188 MB (+12%)` (no names, so pasting it in chat does not publish what the owner watches). Positive = the list was too big.
- The Health log also has `media size-calibrated 1080p samples=8 measured=... declared=... factor=0.71` per measured quality.

Files: `core/.../media/SizeEstimate.kt`, `StreamEngine.view()/noteSize()/sizeReport()`, `geckoapp/.../web/VideoFlow.kt` (`calibrateMaster`, `measureQuality`, `sizeBareStream`), `DownloadController.sizeLine()`.
