#!/usr/bin/env bash
#
# Reset the IDOR demo back to its vulnerable starting state.
#
#   - restores `main` to the `main-baseline` branch (undoes a demo merge)
#   - rewinds `demo/idor` back to the `demo/idor-vuln` branch (IDOR visible again)
#
# After running, reopen the PR (demo/idor -> main) if a previous run merged
# or closed it.
#
set -euo pipefail

REMOTE="${REMOTE:-origin}"

echo "Fetching branches from $REMOTE ..."
git fetch "$REMOTE" --prune

echo "Restoring main -> main-baseline ..."
git push -f "$REMOTE" refs/remotes/"$REMOTE"/main-baseline:refs/heads/main

echo "Rewinding demo/idor -> demo/idor-vuln ..."
git push -f "$REMOTE" refs/remotes/"$REMOTE"/demo/idor-vuln:refs/heads/demo/idor

echo
echo "Done. The demo is back to the vulnerable start."
echo "Reopen the PR (demo/idor -> main) if it was merged or closed."
