#!/usr/bin/env bash
# Regenerate the bundled filter snapshot (app/src/main/assets/filters/default.kfs) and the Public Suffix List
# (app/src/main/assets/psl.txt) from a local uBlockOrigin/uAssets checkout, then run the real-list regression test.
#
#   scripts/make-default-lists.sh [path-to-uassets-checkout]
#
# Getting the checkout (read-only, shallow; keep it OUTSIDE this repo - it is untrusted third-party data):
#   Claude session: call the add_repo tool for uBlockOrigin/uAssets, then
#     GIT_LFS_SKIP_SMUDGE=1 git clone --depth 1 https://github.com/uBlockOrigin/uassets <dir>
#   Default <dir> = ../ublockorigin/uassets (sibling of this repo's parent).
# List choice and licences: docs/FILTER-LISTS.md.  Peter Lowe's list is deliberately NOT included (noncommercial-only licence).
set -eu
cd "$(git rev-parse --show-toplevel)"
U="${1:-$(cd .. && pwd)/ublockorigin/uassets}"
[ -d "$U/filters" ] || { echo "uAssets checkout not found at $U (see header of this script)" >&2; exit 2; }
R="$(pwd)"
mkdir -p "$R/build/lists" "$R/app/src/main/assets/filters"

./gradlew :tools:run --console=plain -q --args="\
--out $R/build/lists/default.kfs \
--psl-in $U/thirdparties/publicsuffix.org/list/effective_tld_names.dat --psl-out $R/build/lists/psl.txt \
abp:ads:$U/thirdparties/easylist/easylist.txt \
abp:privacy:$U/thirdparties/easylist/easyprivacy.txt \
abp:ads:$U/filters/filters.txt \
abp:privacy:$U/filters/privacy.txt \
abp:privacy:$U/filters/privacy-removeparam.txt \
abp:malware:$U/filters/badware.txt \
abp:ads:$U/filters/unbreak.txt \
abp:ads:$U/filters/quick-fixes.txt \
abp:annoyance:$U/thirdparties/easylist/easylist-cookies.txt" 2>&1 | grep -v '^Picked up' | cut -c1-220 | tail -14

cp "$R/build/lists/default.kfs" "$R/app/src/main/assets/filters/default.kfs"
cp "$R/build/lists/psl.txt" "$R/app/src/main/assets/psl.txt"
echo "installed: $(( $(stat -c %s app/src/main/assets/filters/default.kfs) / 1024 )) KB snapshot, $(wc -l < app/src/main/assets/psl.txt) PSL rules"
./gradlew :core:test --tests '*DefaultListsRegressionTest*' --console=plain 2>&1 | grep -E 'BUILD|FAILED'
