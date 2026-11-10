#!/usr/bin/env bash
# Shared helpers for the Kestrel checkpoint/resume scripts. Source this file; do not execute it.

kst_branch() { git rev-parse --abbrev-ref HEAD 2>/dev/null; }
kst_utc() { date -u +%Y-%m-%dT%H:%M:%SZ; }

# Never auto-commit on default branches (override with KESTREL_ALLOW_MAIN=1).
kst_guard_branch() {
  case "$1" in
    main|master|HEAD|"") [ "${KESTREL_ALLOW_MAIN:-0}" = 1 ] && return 0; return 1 ;;
    *) return 0 ;;
  esac
}

# Unstage anything that looks like private key material. Allow-list: signing/dev/ (public dev key).
kst_secret_guard() {
  local bad f
  bad="$(git diff --cached --name-only \
    | grep -E '(^|/)(keystore\.properties|\.env(\..*)?|id_rsa[^/]*|local\.properties)$|\.(pem|p12|key|jks|keystore)$' \
    | grep -v '^signing/dev/' || true)"
  [ -z "$bad" ] && return 0
  while IFS= read -r f; do
    git reset -q HEAD -- "$f" 2>/dev/null
    echo "[kestrel] secret-guard: refused to commit $f" >&2
  done <<< "$bad"
  return 0
}

# kst_push BRANCH [MAX_ATTEMPTS]  -> git push -u origin BRANCH with 2s/4s/8s/16s backoff.
kst_push() {
  local br="$1" max="${2:-5}" n=0 delay=2 err
  err="$(git rev-parse --git-dir)/kestrel-push.err"
  while :; do
    if timeout "${KESTREL_PUSH_TIMEOUT:-40}" git push -q -u origin "$br" 2>"$err"; then return 0; fi
    n=$((n + 1))
    if [ "$n" -ge "$max" ]; then cat "$err" >&2; return 1; fi
    sleep "$delay"; delay=$((delay * 2))
  done
}

# kst_remote_sha BRANCH -> sha of origin's branch (empty if absent/unreachable)
kst_remote_sha() { git ls-remote --heads origin "refs/heads/$1" 2>/dev/null | awk '{print $1}' | head -1; }

# kst_unpushed BRANCH -> number of local commits not on origin/BRANCH (999 if no tracking ref)
kst_unpushed() {
  if git rev-parse -q --verify "refs/remotes/origin/$1" >/dev/null 2>&1; then
    git rev-list --count "origin/$1..HEAD" 2>/dev/null || echo 999
  else
    echo 999
  fi
}
