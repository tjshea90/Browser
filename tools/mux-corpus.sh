#!/usr/bin/env bash
# mux-corpus.sh <outdir> - makes a corpus of small HLS transport-stream variants with ffmpeg, to stress the MP4 writer (research/MP4_MUX.md section 6).
# Each variant becomes <outdir>/<name>/seg0.ts, seg1.ts ... (2 s segments). Run the checks with:
#   KESTREL_CORPUS=<outdir> ./gradlew :core:test --tests 'app.kestrel.core.media.CorpusTest'
# Needs ffmpeg with libx264 / libx265 / libmp3lame. NOT run by CI (the repo keeps only the tiny fixtures in core/src/test/resources/mux).
set -euo pipefail
OUT="${1:?usage: mux-corpus.sh <outdir>}"
mkdir -p "$OUT"
mk() { # name size rate seconds "video opts" "audio opts"
  local name="$1" size="$2" rate="$3" secs="$4" vopts="$5" aopts="$6"
  mkdir -p "$OUT/$name"; rm -f "$OUT/$name"/seg*.ts
  ffmpeg -hide_banner -loglevel error -y -f lavfi -i "testsrc2=size=$size:rate=$rate" -f lavfi -i "sine=frequency=440:sample_rate=44100" -t "$secs" \
    $vopts $aopts -f hls -hls_time 2 -hls_segment_type mpegts -hls_flags independent_segments -hls_playlist_type vod -hls_segment_filename "$OUT/$name/seg%d.ts" "$OUT/$name/index.m3u8"
  echo "  $name: $(ls "$OUT/$name"/seg*.ts | wc -l) segments, $(du -cb "$OUT/$name"/seg*.ts | tail -1 | cut -f1) bytes"
}
A="-c:a aac -b:a 64k -ac 2"
mk plain_bf2        320x180 25 8 "-c:v libx264 -preset veryfast -crf 28 -g 50 -bf 2 -pix_fmt yuv420p" "$A"
mk no_aud           320x180 25 8 "-c:v libx264 -preset veryfast -crf 28 -g 50 -bf 2 -pix_fmt yuv420p -x264-params aud=0" "$A"
mk baseline_nobf    320x180 25 8 "-c:v libx264 -preset veryfast -crf 28 -g 50 -profile:v baseline -pix_fmt yuv420p" "$A"
mk pyramid_bf4      320x180 25 8 "-c:v libx264 -preset medium -crf 28 -g 50 -bf 4 -refs 4 -b_strategy 2 -pix_fmt yuv420p" "$A"
mk high10           320x180 25 8 "-c:v libx264 -preset veryfast -crf 28 -g 50 -profile:v high10 -pix_fmt yuv420p10le" "$A"
mk multislice       320x180 25 8 "-c:v libx264 -preset veryfast -crf 28 -g 50 -x264-params slices=4 -pix_fmt yuv420p" "$A"
mk interlaced       320x240 25 8 "-c:v libx264 -preset veryfast -crf 28 -g 50 -flags +ildct+ilme -x264-params tff=1 -pix_fmt yuv420p" "$A"
mk video_only       320x180 25 8 "-c:v libx264 -preset veryfast -crf 28 -g 50 -bf 2 -pix_fmt yuv420p" "-an"
mk odd_size         322x182 30 8 "-c:v libx264 -preset veryfast -crf 28 -g 60 -bf 2 -pix_fmt yuv420p" "-c:a aac -b:a 48k -ac 1"
mk fps_29_97        320x180 30000/1001 8 "-c:v libx264 -preset veryfast -crf 28 -g 60 -bf 2 -pix_fmt yuv420p" "$A"
mk audio_48k_5ch    320x180 25 8 "-c:v libx264 -preset veryfast -crf 28 -g 50 -bf 2 -pix_fmt yuv420p" "-c:a aac -b:a 128k -ac 6 -channel_layout 5.1"
mk hd_1080p         1920x1080 25 6 "-c:v libx264 -preset veryfast -crf 24 -g 50 -bf 2 -maxrate 6M -bufsize 12M -pix_fmt yuv420p" "$A"
mk long_gop_8s      320x180 25 20 "-c:v libx264 -preset veryfast -crf 28 -g 200 -keyint_min 200 -sc_threshold 0 -bf 2 -pix_fmt yuv420p" "$A"
# two sound tracks in one stream
mkdir -p "$OUT/two_audio"; rm -f "$OUT/two_audio"/seg*.ts
ffmpeg -hide_banner -loglevel error -y -f lavfi -i testsrc2=size=320x180:rate=25 -f lavfi -i sine=frequency=440:sample_rate=44100 -f lavfi -i sine=frequency=660:sample_rate=44100 -t 8 \
  -map 0:v -map 1:a -map 2:a -c:v libx264 -preset veryfast -crf 28 -g 50 -bf 2 -pix_fmt yuv420p -c:a aac -b:a 64k -f hls -hls_time 2 -hls_segment_type mpegts -hls_flags independent_segments -hls_playlist_type vod \
  -hls_segment_filename "$OUT/two_audio/seg%d.ts" "$OUT/two_audio/index.m3u8"
echo "  two_audio: $(ls "$OUT/two_audio"/seg*.ts | wc -l) segments"
# what the writer must REFUSE (and keep as .ts): H.265, AC-3 sound, MP3 sound, MPEG-2 video
mk refuse_hevc      320x180 25 6 "-c:v libx265 -preset ultrafast -crf 30 -g 50 -pix_fmt yuv420p" "$A"
mk refuse_ac3       320x180 25 6 "-c:v libx264 -preset veryfast -crf 28 -g 50 -pix_fmt yuv420p" "-c:a ac3 -b:a 128k -ac 2"
mk refuse_mp3       320x180 25 6 "-c:v libx264 -preset veryfast -crf 28 -g 50 -pix_fmt yuv420p" "-c:a libmp3lame -b:a 96k -ac 2"
mk refuse_mpeg2     320x180 25 6 "-c:v mpeg2video -b:v 800k -g 12 -pix_fmt yuv420p" "$A"
echo "corpus ready in $OUT"
