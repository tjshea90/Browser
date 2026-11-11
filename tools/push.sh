#!/usr/bin/env bash
# push.sh — get commits to GitHub, and never decide "no" by accident.
#
# Called by autosave.sh (after every tool call), ckpt.sh and ship.sh. Rule: when
# "is there anything to push?" cannot be PROVEN to be no, push. A wasted push costs a
# second; a skipped one costs the session (the container is destroyed when it ends).
# `@{u}` is not trusted for this: a branch made from FETCH_HEAD has no remote-tracking
# ref, `@{u}` errors, and the old `|| echo 0` fallback read that as "nothing to push".
#
# main: tools/MIRROR_MAIN decides whether every successful push also fast-forwards
# origin/main (plain push, never forced, so it can only fast-forward). It is committed
# so every account sees the same setting. Default "off": the sessions are told which
# branch to develop on, so landing on main is the owner's call (see CLAUDE.md "Branches").
set -uo pipefail
D="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"; cd "$D" || exit 0
[ -e .git ] || exit 0

BR="$(git branch --show-current 2>/dev/null || true)"
[ -z "$BR" ] && exit 0   # detached HEAD — no branch to push to

UP="$(git rev-parse --abbrev-ref '@{u}' 2>/dev/null || true)"
[ -z "$UP" ] && UP="origin/$BR"

if git rev-parse --verify -q "$UP" >/dev/null 2>&1; then
  AHEAD="$(git rev-list --count "$UP"..HEAD 2>/dev/null || echo 1)"
else
  AHEAD=1
fi
[ "${AHEAD:-1}" -eq 0 ] && exit 0

# `timeout` keeps a hung connection well inside the hook's 60 s budget; the commit stays
# local and is retried on the next tool call.
if timeout 45 git push -q origin "HEAD:refs/heads/$BR" >/dev/null 2>&1; then
  # Self-heal the tracking setup so the cheap check above works next time (a shallow clone
  # of an empty repo leaves remote.origin.fetch unset).
  git config --get-all remote.origin.fetch >/dev/null 2>&1 || \
    git config remote.origin.fetch '+refs/heads/*:refs/remotes/origin/*' 2>/dev/null || true
  timeout 10 git fetch -q origin "$BR:refs/remotes/origin/$BR" >/dev/null 2>&1 || true
  git branch --set-upstream-to="origin/$BR" "$BR" >/dev/null 2>&1 || true

  if [ "$BR" != "main" ] && [ "$(tr -d ' \r\n' < tools/MIRROR_MAIN 2>/dev/null)" = "on" ]; then
    timeout 10 git push -q origin "HEAD:refs/heads/main" >/dev/null 2>&1 || true
  fi
  exit 0
fi
exit 1
