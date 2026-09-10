#!/usr/bin/env bash
#
# "The developer makes the fix." Two steps:
#
#   apply-fix.sh idor   Advances demo/idor -> demo/idor-fixed: adds the ownership
#                       check. DryRun goes green; Veracode is still red on the
#                       reporting-API findings.
#   apply-fix.sh sast   Advances demo/idor -> demo/all-fixed: remediates the
#                       reporting API and network helper. Everything goes green.
#
# Each step is a fast-forward, so the PR picks up the new commit(s) and its
# checks re-run.
#
set -euo pipefail

REMOTE="${REMOTE:-origin}"
step="${1:-}"

case "$step" in
  idor) target="demo/idor-fixed" ;;
  sast) target="demo/all-fixed" ;;
  *)
    echo "usage: $0 idor|sast" >&2
    exit 1
    ;;
esac

echo "Fetching branches from $REMOTE ..."
git fetch "$REMOTE" --prune

echo "Advancing demo/idor -> $target ..."
git push "$REMOTE" refs/remotes/"$REMOTE"/"$target":refs/heads/demo/idor

echo
echo "Done. The fix is pushed; checks will re-run."
