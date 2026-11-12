#!/usr/bin/env bash
# appinfo.sh - the ONE place that knows the two apps this repo builds. Source it (. tools/appinfo.sh) or call it:
#   bash tools/appinfo.sh <web|gecko|courier> <field>        field = vname vcode gradle module dir prefix tag asset apk title prerelease
#   bash tools/appinfo.sh of-tag kestrel-g-v0.2.0-gecko-alpha1     -> gecko
#
#   web    "Kestrel"    :app      system WebView (Chromium 74 on the owner's tablet)  tag kestrel-v<versionName>     asset kestrel-<versionName>.apk
#   gecko  "Kestrel G"  :geckoapp Firefox GeckoView (modern engine, ~92 MB)           tag kestrel-g-v<versionName>  asset kestrel-g-<versionName>.apk
#   courier "Kestrel Courier" :courier the app for the owner's PHONE (downloads for the tablet, ~2 MB)  tag kestrel-courier-v<versionName>  asset kestrel-courier-<versionName>.apk
#
# ship.sh, record-release.sh, resume.sh, verify-apk.sh, test_handoff.sh and .github/workflows/android.yml all read versions through
# here, so the versionCode that is gated is always the one Gradle will really build.
APPINFO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

app_valid()  { case "${1:-}" in web|gecko|courier) return 0 ;; *) return 1 ;; esac; }
app_gradle() { case "$1" in web) echo app/build.gradle.kts ;; gecko) echo geckoapp/build.gradle.kts ;; courier) echo courier/build.gradle.kts ;; esac; }
app_dir()    { case "$1" in web) echo app ;; gecko) echo geckoapp ;; courier) echo courier ;; esac; }
app_module() { echo ":$(app_dir "$1")"; }
app_prefix() { case "$1" in web) echo kestrel-v ;; gecko) echo kestrel-g-v ;; courier) echo kestrel-courier-v ;; esac; }
app_title()  { case "$1" in web) echo "Kestrel" ;; gecko) echo "Kestrel G" ;; courier) echo "Kestrel Courier" ;; esac; }
app_apk()    { echo "$(app_dir "$1")/build/outputs/apk/release/$(app_dir "$1")-release.apk"; }
app_vcode()  { grep -m1 -oE 'versionCode *= *[0-9]+' "$APPINFO_ROOT/$(app_gradle "$1")" 2>/dev/null | grep -oE '[0-9]+'; }
app_vname()  { grep -m1 -oE 'versionName *= *"[^"]+"' "$APPINFO_ROOT/$(app_gradle "$1")" 2>/dev/null | grep -oE '"[^"]+"' | tr -d '"'; }
app_tag()    { echo "$(app_prefix "$1")$(app_vname "$1")"; }
app_asset()  { case "$1" in web) echo "kestrel-$(app_vname web).apk" ;; gecko) echo "kestrel-g-$(app_vname gecko).apk" ;; courier) echo "kestrel-courier-$(app_vname courier).apk" ;; esac; }
app_prerelease() { case "$(app_vname "$1")" in *-*) echo true ;; *) echo false ;; esac; }   # 0.2.0-gecko-alpha1 -> a pre-release
app_of_tag() { case "${1:-}" in kestrel-g-v*) echo gecko ;; kestrel-courier-v*) echo courier ;; kestrel-v*) echo web ;; *) return 1 ;; esac; }
# highest versionCode ever RECORDED for an app in BUILDLOG.md (0 when none)
app_last_code() {
  local n; n="$(grep -E "^\| [^|]+ \| $1 \| code [0-9]+ " "$APPINFO_ROOT/BUILDLOG.md" 2>/dev/null | grep -oE 'code [0-9]+' | grep -oE '[0-9]+' | sort -n | tail -1)"
  echo "${n:-0}"
}

# called as a program (not sourced): print one field
if [ "${BASH_SOURCE[0]}" = "$0" ]; then
  if [ "${1:-}" = "of-tag" ]; then app_of_tag "${2:-}"; exit $?; fi
  app_valid "${1:-}" || { echo "usage: bash tools/appinfo.sh <web|gecko|courier> <vname|vcode|gradle|module|dir|prefix|tag|asset|apk|title|prerelease|last_code>" >&2; exit 2; }
  case "${2:-}" in
    vname) app_vname "$1" ;; vcode) app_vcode "$1" ;; gradle) app_gradle "$1" ;; module) app_module "$1" ;; dir) app_dir "$1" ;;
    prefix) app_prefix "$1" ;; tag) app_tag "$1" ;; asset) app_asset "$1" ;; apk) app_apk "$1" ;; title) app_title "$1" ;;
    prerelease) app_prerelease "$1" ;; last_code) app_last_code "$1" ;;
    *) echo "unknown field '${2:-}'" >&2; exit 2 ;;
  esac
fi
