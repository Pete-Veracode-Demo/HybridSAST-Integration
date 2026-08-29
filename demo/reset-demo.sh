#!/usr/bin/env bash
#
# Reset the IDOR demo back to its vulnerable starting state.
#
#   - restores `main` to the `main-baseline` tag (undoes a demo merge)
#   - rewinds `demo/idor` back to the `idor-vuln` tag (IDOR visible again)
#
# After running, reopen the PR (demo/idor -> main) if a previous run merged
# or closed it.
#
set -euo pipefail

REMOTE="${REMOTE:-origin}"

echo "Fetching tags and branches from $REMOTE ..."
git fetch "$REMOTE" --tags --prune

echo "Restoring main -> main-baseline ..."
git push -f "$REMOTE" main-baseline:main

echo "Rewinding demo/idor -> idor-vuln ..."
git push -f "$REMOTE" idor-vuln:refs/heads/demo/idor

echo
echo "Done. The demo is back to the vulnerable start."
echo "Reopen the PR (demo/idor -> main) if it was merged or closed."
