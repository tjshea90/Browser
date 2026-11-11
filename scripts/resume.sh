#!/usr/bin/env bash
# COMPAT SHIM: see scripts/autosave.sh. The briefing now comes from tools/resume.sh.
exec bash "$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/tools/resume.sh" "$@"
