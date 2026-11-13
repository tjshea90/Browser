#!/usr/bin/env bash
# publish-release.sh <web|gecko|courier> - run by .github/workflows/android.yml AFTER the APK was built and verified.
# Creates (or refreshes) the GitHub Release for the app's current version with the APK + its sha256 attached.
# GitHub creates the tag itself (`--target <sha>`): a tag PUSH from the Claude sandbox is refused (403), so nothing ever pushes one.
# Needs GH_TOKEN (the workflow's token, contents: write). Optional env: NOTE (what changed).
set -euo pipefail
cd "$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
. tools/appinfo.sh

APP="${1:?usage: publish-release.sh <web|gecko|courier>}"
app_valid "$APP"
TAG="$(app_tag "$APP")"; ASSET="$(app_asset "$APP")"; VN="$(app_vname "$APP")"; VC="$(app_vcode "$APP")"; TITLE="$(app_title "$APP")"
cp "$(app_apk "$APP")" "$ASSET"
sha256sum "$ASSET" > "$ASSET.sha256"
SUM="$(cut -d' ' -f1 "$ASSET.sha256")"
CERT="$(tr -d ' \r\n' < signing/CERT_SHA256)"

case "$APP" in
  web)   ABOUT="Kestrel on the system WebView: very small, but the tablet's WebView is Chromium 74, so modern sites can refuse it." ;;
  gecko) ABOUT="Kestrel G on Firefox's engine (GeckoView): a modern engine, about 92 MB. It installs BESIDE Kestrel (own app, own data)." ;;
  courier) ABOUT="Kestrel Courier is the small app for your PHONE: it downloads files and videos for the tablet's Kestrel over the phone's own connection and sends them over the hotspot wifi." ;;
esac

{
  echo "${NOTE:-No change note was given.}"
  echo
  echo "**$TITLE $VN** (versionCode $VC). $ABOUT"
  echo
  if [ "$APP" = courier ]; then
    echo "- Install on your PHONE (not the tablet): open this page on the phone, tap \`$ASSET\`, and allow installing from this source if Android asks."
    echo "- Then open Kestrel Courier, switch on **Ready for the tablet**, turn on the phone's hotspot and join it with the tablet. On the tablet open Kestrel G > Settings > Download on my phone > Pair with the phone and type the code the Courier shows."
  else
    echo "- Install: open this page on the tablet, tap \`$ASSET\`, and allow installing from this source if Android asks. It installs over the previous $TITLE because the signing certificate is the same."
  fi
  echo "- If Android says \"App not installed\" or \"not enough storage\": an install only finishes while Android's own reserve stays free afterwards (5 % of the tablet's storage, at most 500 MB), on top of the room the update takes while it installs (the file and its unpacked libraries, each counted twice: about 0.45 GB for Kestrel G). On a 28 GB tablet that is about 1 GB free. Free some room (old installer files and watched videos in Downloads, Settings > Storage) and try again; Kestrel's Updates screen shows the exact numbers. Uninstalling first frees only what the old copy used (about 0.5 GB), so it helps only when you are that close."
  echo "- Built and signed by GitHub Actions from commit \`${GITHUB_SHA:-unknown}\`; the certificate was verified on this exact APK (SHA-256 \`$CERT\`)."
  echo "- APK SHA-256: \`$SUM\`"
} > release-notes.md

FLAGS=()
[ "$(app_prerelease "$APP")" = true ] && FLAGS+=(--prerelease)

if gh release view "$TAG" >/dev/null 2>&1; then
  # a rebuild of an existing release (or a Release the owner drafted by hand, which starts this run through its tag): replace the files,
  # and refresh the text only when a change note was given or the release has no text yet - never overwrite words someone wrote.
  gh release upload "$TAG" "$ASSET" "$ASSET.sha256" --clobber
  BODY="$(gh release view "$TAG" --json body -q .body)"
  if [ -n "${NOTE:-}" ] || [ -z "$BODY" ]; then gh release edit "$TAG" --notes-file release-notes.md; fi
else
  gh release create "$TAG" "$ASSET" "$ASSET.sha256" --target "${GITHUB_SHA:?}" --title "$TITLE $VN" --notes-file release-notes.md "${FLAGS[@]}"
fi
URL="$(gh release view "$TAG" --json url -q .url)"
echo "Published $TAG -> $URL"
[ -n "${GITHUB_STEP_SUMMARY:-}" ] && echo "Published **$TAG** -> $URL" >> "$GITHUB_STEP_SUMMARY"
exit 0
