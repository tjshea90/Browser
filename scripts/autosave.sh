#!/usr/bin/env bash
# COMPAT SHIM. The save ladder moved to tools/ (the owner's system, same as his Launcher and Portfolio repos) on 2026-11-11.
# A Claude session that STARTED before the move still has the old hook command registered (scripts/autosave.sh --hook), and Claude Code
# keeps the hooks it started with, so this path must keep saving. It just hands over to the real script (hook JSON on stdin passes through).
# Remove this file together with scripts/resume.sh once no such session can be running (TASKS.md).
exec bash "$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/tools/autosave.sh" "$@"
