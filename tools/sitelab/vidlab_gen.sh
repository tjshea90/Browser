#!/usr/bin/env bash
# Makes the two small real HLS streams (12 s, H.264 + AAC, three 4 s parts each, 360p and 240p) that tools/sitelab/vidlab_srv.py serves (encrypted, wearing a PNG header, behind a Referer gate).
# Usage: bash tools/sitelab/vidlab_gen.sh <output folder>   then   VIDLAB_GEN=<output folder> python3 -I tools/sitelab/vidlab_srv.py   (ports 8000, 8003, 8004; the emulator reaches them as 10.0.2.2)
# Needs ffmpeg and openssl. Nothing it writes belongs in the repository (keep the output folder outside it).
set -e
out="${1:?output folder}"; mkdir -p "$out"; cd "$out"
for q in 360 240; do
  w=$((q * 16 / 9 / 2 * 2))
  ffmpeg -loglevel error -y -f lavfi -i "testsrc=duration=12:size=${w}x${q}:rate=25" -f lavfi -i "sine=frequency=440:duration=12" \
    -c:v libx264 -preset ultrafast -pix_fmt yuv420p -g 50 -c:a aac -b:a 64k -f hls -hls_time 4 -hls_playlist_type vod -hls_segment_filename "v${q}_%d.ts" "v${q}.m3u8"
  echo "made ${q}p (${w}x${q})"
done
