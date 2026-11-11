#!/usr/bin/env bash
# bootstrap.sh - the session briefing in plain text. tools/resume.sh is the real thing (the SessionStart hook runs it); this exists so
# `bash bootstrap.sh` works from habit in any of the owner's repos.
exec bash "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/tools/resume.sh" --text
