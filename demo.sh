#!/usr/bin/env bash
#
# IDOR demo — one entry point. Three verbs:
#
#   ./demo.sh reset    Reset to the vulnerable start (run before each demo)
#   ./demo.sh fix      "The developer fixes it" — checks re-run and go green
#   ./demo.sh status   Show the current state and the PR link
#
# This is a thin wrapper over demo/reset-demo.sh and demo/apply-fix.sh; it adds
# the "what to do next" reminders so you don't have to remember them.
#
set -euo pipefail

REMOTE="${REMOTE:-origin}"
REPO_URL="https://github.com/Pete-Veracode-Demo/HybridSAST-Integration"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

cmd="${1:-help}"

case "$cmd" in
  reset)
    "$SCRIPT_DIR/demo/reset-demo.sh"
    echo
    echo "▶ Next:"
    echo "  1. Make sure the demo PR is OPEN (reopen it if a previous run merged/closed it):"
    echo "       $REPO_URL/pulls"
    echo "  2. Show the checks: DryRun IDOR Analyzer → RED, Veracode Pipeline → green."
    echo "  3. When ready, run:  ./demo.sh fix"
    ;;

  fix)
    "$SCRIPT_DIR/demo/apply-fix.sh"
    echo
    echo "▶ Next:"
    echo "  1. Watch the checks re-run and go ALL GREEN."
    echo "  2. Merge the PR in the GitHub UI to finish the story."
    echo "  To run the demo again later:  ./demo.sh reset"
    ;;

  status)
    git fetch "$REMOTE" --prune --quiet
    echo "Demo branch heads on $REMOTE:"
    for b in main demo/idor demo/idor-vuln demo/idor-fixed; do
      printf '  %-16s %s\n' "$b" "$(git rev-parse --short "$REMOTE/$b" 2>/dev/null || echo '(missing)')"
    done
    echo
    if git merge-base --is-ancestor "$REMOTE/demo/idor-fixed" "$REMOTE/demo/idor" 2>/dev/null; then
      echo "State: ✅ FIXED — demo/idor has the ownership-check fix (checks should be green)."
    else
      echo "State: ⚠  VULNERABLE — demo/idor shows the IDOR (run ./demo.sh fix to fix it)."
    fi
    echo "PRs:   $REPO_URL/pulls"
    ;;

  help|-h|--help|"")
    cat <<USAGE
IDOR demo — one command, three verbs:

  ./demo.sh reset    Reset to the vulnerable start (run before each demo)
  ./demo.sh fix      "The developer fixes it" — checks re-run and go green
  ./demo.sh status   Show the current state and the PR link

Typical run:  ./demo.sh reset  →  show the PR  →  ./demo.sh fix  →  merge

Full runbook + talking points: demo/DEMO.md
USAGE
    ;;

  *)
    echo "Unknown command: '$cmd'" >&2
    echo "Try:  ./demo.sh help" >&2
    exit 1
    ;;
esac
