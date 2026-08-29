#!/usr/bin/env bash
#
# "The developer makes the fix."
#
# Pushes the ownership-check fix commit onto demo/idor by advancing the branch
# to the `idor-fixed` tag. This is a fast-forward from `idor-vuln`, so the PR
# picks up one new commit and its checks re-run and go green.
#
set -euo pipefail

REMOTE="${REMOTE:-origin}"

echo "Fetching tags from $REMOTE ..."
git fetch "$REMOTE" --tags

echo "Advancing demo/idor -> idor-fixed (adds the fix commit) ..."
git push "$REMOTE" idor-fixed:refs/heads/demo/idor

echo
echo "Done. The fix is pushed; checks will re-run and go green."
