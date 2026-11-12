# CHECKPOINT 804 — read me first, then TASKS.md

**Written:** 2026-11-12T04:06:37Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `c9aeb6b` (this checkpoint is the commit after it)

## Just done
X18a DONE (core, JVM-tested, ffmpeg-verified): Avc.kt (SPS/NAL), TsDemux.kt (streaming PAT/PMT/PES), Mp4Mux.kt (progressive MP4: stts/ctts/stss/stsc/stco|co64/stsz, ffmpeg-style elst, journal+replay, rollback, wide/mdat 64-bit), MuxJournal.kt (CRC blocks), TsToMp4.kt (runs, ad drop/replace, implicit clock restart, persisted clock offset, probe). Fixtures core/src/test/resources/mux/*.ts; tests TsDemuxTest, Mp4MuxTest, TsToMp4Test (kill+resume gives byte-identical file; ffmpeg -f null decode clean; ffprobe frames 150/150 + lip sync 23ms). Design: research/MP4_MUX.md.

## Do this next
X18b engine integration in core/.../media/StreamEngine.kt: (1) StreamRec.mux Int (0 undecided,1 mp4,2 raw) + StreamStoreFiles json field; (2) Sink.truncate default no-op in core Model.kt + Os.ftruncate in app FileSinks/MediaStoreSinks; (3) prepare(): for container ts probe segment 0 (decrypt prefix if AES) with TsToMp4.probe -> mode; MP4 mode: name stem.mp4 mime video/mp4, TsToMp4(MuxOut over sink.pwrite, FileMuxJournal(dir/mux.jnl), sink::sync, media.durationMs), recover() or startFresh(); (4) appendNext(): beginSegment(idx, seg.discontinuity, runMs)/feed decrypted bytes/endSegment, job.written=mux.position; (5) checkpoint(): under job.drain lock call mux.checkpoint(rec.next); (6) finish(): mux.finish(), no Remuxer, warning to row; (7) mux exception -> restart job raw. Then StreamEngineTest with fixtures (plain, AES-128, disc/ad, kill+resume), then app wiring (X18c), emulator proof (X18d). Heartbeat trig_01LLoVKsjuTUaDn61UGP1pBN.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  1b3baa2 ckpt 756: X17 dock download icon VERIFIED on emulator after a real redraw (home+relaunch
  51942ec ckpt 744: owner message 11 written into TASKS as X14-X18 in their words (long-press down
  e8ca3f6 ckpt 741: M8-lite: Settings > Downloads (speed limit 2 MB/s..250 KB/s applied at once, d
  06945e0 ckpt 733: Owner told (alpha16 reply: Release link, repeated message 10 answered part by 
  b87f19d ckpt 732: alpha16 SHIPPED + RECORDED (run 43143728373 green; the two earlier runs failed
  dc57dde ckpt 729: stream engine: a dropped job (merged-away sound of a pair, cancelled, cleared)
  9226e37 ckpt 726: TASKS X4/X5/X12 statuses with evidence; HLS auto-resume test; lintRelease clea
  d41caca ckpt 724: M7 content sanity (web page where a file/video was expected -> BAD_CONTENT, HL
  61e1894 ckpt 721: M1b validator-less resume: Sink.readable/pread (Os.pread on both Android sinks
  f17fcb1 ckpt 719: web guard tests fixed (tap reports) + 2 new; CI test-js now gates the Gecko bu
```

(47 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
