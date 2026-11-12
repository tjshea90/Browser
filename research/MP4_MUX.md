# MP4_MUX - HLS downloads always end as MP4 (TASKS X15 / X18), design 2026-11-12

Owner (message 11): "the stream only showed a .ts file but on video Downloader app I could choose mp4 for the same video" and "I want these to be mp4 downloads always".
Evidence tags: [V] verified in this session, [E] estimate, [N] needs the tablet.

## 1. Why a `.ts` still happens today [V code read]
`StreamEngine` joins every TS segment into ONE file `<stem>.ts` in public Downloads, then `Remuxer.convert` (MediaExtractor + MediaMuxer) writes `<stem>.mp4` next to it and deletes the `.ts`.
The `.ts` survives when: (1) free room < size + 64 MB (the tablet is short of storage: likely the main cause), (2) no H.264/H.265 or the sound is not AAC, (3) the platform extractor/muxer throws, (4) a discontinuity stream whose runs were not recorded, (5) the finish ran while `job.stop`.
During the download and "Finishing" the row is named `<stem>.ts` (what the owner saw in the manager). The picker row already says MP4 (alpha15).
Cost of the two-step design: 2x disk at the end, a long "Finishing" on the A53, and a platform parser we cannot fix.

## 2. Decision: write the MP4 while downloading (own TS demux + own MP4 writer), keep the platform remux as the fallback
No `.ts` is ever written for a video stream the writer can handle: peak disk = the MP4 + the few staged segments, no "Finishing" wait, name `<stem>.mp4` from the first byte, resumable after a kill.
Layout = a plain progressive MP4, NOT fragmented (fMP4 without sidx cannot be scrubbed in ExoPlayer, and some share targets reject it): `ftyp`, `wide` + `mdat` (16-byte header patched at the end, 64-bit size when > 4 GB), samples in arrival order, `moov` at the END (fine for local files).
Out of scope for v1 (they keep the old path = raw `.ts` + platform remux + the reason shown): H.265, AC-3/E-AC-3/MP3 sound, SAMPLE-AES, audio-only TS, fMP4 HLS (already an `.mp4`).

## 3. Components (package `app.kestrel.core.media`, pure Kotlin, JVM-tested)
| Class | Job |
|---|---|
| `TsDemux` | 188-byte packets across arbitrary `feed()` chunk boundaries (carry < 188 bytes), PAT/PMT (sections may span packets), PES reassembly for the first H.264 (0x1B) and first AAC-ADTS (0x0F) PID, 33-bit PTS/DTS with unwrap, drop packets with TEI / scrambling; `endSegment()` flushes the open PES (segments end on PES boundaries) |
| `AvcSps` | SPS parse (scaling lists, cropping) -> width/height/profile/level; Annex-B -> 4-byte-length NALs; IDR detection; avcC builder |
| `Mp4Mux` | sample tables (primitive arrays), chunking, `ctts`/`elst` for B-frames, timeline rebasing, run rollback, journal, moov writer, `mdat` patch |
| `MuxJournal` | append-only record file in the staging folder: CONFIG, SAMPLES, MARK(next segment, output length), ROLLBACK; CRC per record; replay stops at the last MARK |
| `StreamEngine` (edit) | mode `MP4_DIRECT` chosen after the first segment is probed; `appendNext` feeds decrypted bytes to the mux instead of `pwrite`; checkpoint = sink.sync + journal MARK; finish = moov; ANY mux error before the first checkpoint -> restart as raw `.ts` (old path) |

## 4. Timeline rules
- 90 kHz video, audio timescale = sample rate; movie timescale 1000.
- Common zero = the earliest first kept sample (video DTS or audio PTS). A video start later than zero or a B-frame start delay is written as `elst` (empty edit + media_time) so lip-sync holds in players that honour edit lists.
- Leading non-IDR pictures are dropped; audio before the first kept picture is dropped.
- AAC frames: raw frames cut from ADTS, one sample each, exact 1024-sample grid (`AudioClock`), `esds` AudioSpecificConfig from the first header.
- `#EXT-X-DISCONTINUITY` (and any jump of more than 5 s or a step back beyond reorder tolerance) starts a new RUN: its stamps are rebased onto the end of the previous run (last picture time + one frame, or end of sound, whichever is later).
- Format key of a run = SPS bytes + PPS bytes + ASC bytes. A run whose key differs from the current main: if its playlist duration (sum of its segments) is longer than what is kept so far -> REPLACE (roll back to the file start; preroll advertisement case), else DROP (roll back to the run start and skip until the next discontinuity; midroll / postroll). When less than half of the playlist's time ends up kept the row carries a warning text. (Old rule was "decline and keep the .ts"; the owner asked for MP4 always.)

## 5. Resume safety
Checkpoint (every few seconds as today): `sink.sync()`, then the journal gets the samples since the last MARK plus `MARK(next segment, output length, timeline state)` and is synced, then the record is saved as today. Recovery: replay the journal up to its last good MARK, take `next` and `appended` from it (it can be one checkpoint AHEAD of the saved record; trust the journal), truncate the output to the MARK length. A PES split across a segment boundary at the resume point loses one picture (continuation packets without a start are ignored). Rollback records make replay deterministic.

## 6. Verification plan
1. Fixtures: tiny ffmpeg-made TS segments committed under `core/src/test/resources/ts/` (A/V with B-frames, no B-frames, 720p-ish 2 s, audio 44.1 kHz and 48 kHz, ad run with other size and 22 kHz audio, segments not aligned to 188, AES-128 copy).
2. Unit: demux of every fixture equals `ffprobe -show_packets` (sizes, pts, dts); Mp4Mux output re-read by an independent mini reader in the test (stsz, stts, ctts, stss, stco, durations).
3. If `ffmpeg` exists: `ffmpeg -v error -i out.mp4 -f null -` must print nothing, `ffprobe` duration within 0.1 s of the source (skipped when absent).
4. Engine: FakeServer end-to-end for plain, AES-128, discontinuity/ad drop, preroll replace, kill + resume (final file byte-identical to an uninterrupted run), unaligned segments.
5. Emulator: download `hls/720` through the app, pull the file, ffprobe/ffmpeg decode, Diagnostics `mp4 check` line from a MediaExtractor read-back.
6. NOT verifiable here: real sites' streams, the tablet's players. A Diagnostics line records every result so the owner's tablet can tell us.

## 7. Milestones
M-A core writer + tests; M-B engine integration + tests; M-C Android (Sink.truncate, Diagnostics check, "Make MP4" for existing `.ts`, decline reasons in the row); M-D emulator verification; ship with alpha17.
