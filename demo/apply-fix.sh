#!/usr/bin/env bash
#
# "The developer makes the fix."
#
# Advances demo/idor to the demo/idor-fixed branch, which is demo/idor-vuln
# plus the ownership-check commit. This is a fast-forward, so the PR picks up
# one new commit and its checks re-run and go green.
#
set -euo pipefail

REMOTE="${REMOTE:-origin}"

echo "Fetching branches from $REMOTE ..."
git fetch "$REMOTE" --prune

echo "Advancing demo/idor -> demo/idor-fixed (adds the fix commit) ..."
git push "$REMOTE" refs/remotes/"$REMOTE"/demo/idor-fixed:refs/heads/demo/idor

echo
echo "Done. The fix is pushed; checks will re-run and go green."
