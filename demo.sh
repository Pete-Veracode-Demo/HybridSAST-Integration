#!/usr/bin/env bash
#
# Hybrid SAST demo — one entry point.
#
#   ./demo.sh reset      Reset to the vulnerable start (run before each demo)
#   ./demo.sh fix        Apply the next fix step (idor, then sast)
#   ./demo.sh fix-idor   Step 1: ownership check — DryRun green, Veracode red
#   ./demo.sh fix-sast   Step 2: remediate reporting API — everything green
#   ./demo.sh status     Show the current state and the PR link
#
# This is a thin wrapper over demo/reset-demo.sh and demo/apply-fix.sh; it adds
# the "what to do next" reminders so you don't have to remember them.
#
set -euo pipefail

REMOTE="${REMOTE:-origin}"
REPO_URL="https://github.com/Pete-Veracode-Demo/HybridSAST-Integration"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Prints one of: vulnerable | idor-fixed | all-fixed
demo_state() {
  git fetch "$REMOTE" --prune --quiet
  if git merge-base --is-ancestor "$REMOTE/demo/all-fixed" "$REMOTE/demo/idor" 2>/dev/null; then
    echo all-fixed
  elif git merge-base --is-ancestor "$REMOTE/demo/idor-fixed" "$REMOTE/demo/idor" 2>/dev/null; then
    echo idor-fixed
  else
    echo vulnerable
  fi
}

after_fix_idor() {
  echo
  echo "▶ Next:"
  echo "  1. Watch the checks re-run: DryRun IDOR Analyzer → GREEN, Veracode → still RED"
  echo "     (SQLi + command injection in the reporting API)."
  echo "  2. When ready, run:  ./demo.sh fix      (or ./demo.sh fix-sast)"
}

after_fix_sast() {
  echo
  echo "▶ Next:"
  echo "  1. Watch the checks re-run and go ALL GREEN."
  echo "  2. Merge the PR in the GitHub UI to finish the story."
  echo "  To run the demo again later:  ./demo.sh reset"
}

cmd="${1:-help}"

case "$cmd" in
  reset)
    "$SCRIPT_DIR/demo/reset-demo.sh"
    echo
    echo "▶ Next:"
    echo "  1. Make sure the demo PR is OPEN (reopen it if a previous run merged/closed it):"
    echo "       $REPO_URL/pulls"
    echo "  2. Show the checks: DryRun IDOR Analyzer → RED, Veracode → RED."
    echo "  3. When ready, run:  ./demo.sh fix"
    ;;

  fix)
    case "$(demo_state)" in
      vulnerable)
        "$SCRIPT_DIR/demo/apply-fix.sh" idor
        after_fix_idor
        ;;
      idor-fixed)
        "$SCRIPT_DIR/demo/apply-fix.sh" sast
        after_fix_sast
        ;;
      all-fixed)
        echo "Already fully fixed. Merge the PR, or run ./demo.sh reset to start over."
        ;;
    esac
    ;;

  fix-idor)
    "$SCRIPT_DIR/demo/apply-fix.sh" idor
    after_fix_idor
    ;;

  fix-sast)
    "$SCRIPT_DIR/demo/apply-fix.sh" sast
    after_fix_sast
    ;;

  status)
    state="$(demo_state)"
    echo "Demo branch heads on $REMOTE:"
    for b in main demo/idor demo/idor-vuln demo/idor-fixed demo/all-fixed; do
      printf '  %-16s %s\n' "$b" "$(git rev-parse --short "$REMOTE/$b" 2>/dev/null || echo '(missing)')"
    done
    echo
    case "$state" in
      vulnerable) echo "State: ⚠  VULNERABLE — IDOR + reporting-API vulns on demo/idor (run ./demo.sh fix)." ;;
      idor-fixed) echo "State: ◐  IDOR FIXED — DryRun green, Veracode still red (run ./demo.sh fix again)." ;;
      all-fixed)  echo "State: ✅ ALL FIXED — checks should be green; merge the PR." ;;
    esac
    echo "PRs:   $REPO_URL/pulls"
    ;;

  help|-h|--help|"")
    cat <<USAGE
Hybrid SAST demo — one command:

  ./demo.sh reset      Reset to the vulnerable start (run before each demo)
  ./demo.sh fix        Apply the next fix step (idor, then sast)
  ./demo.sh fix-idor   Step 1: ownership check — DryRun green, Veracode red
  ./demo.sh fix-sast   Step 2: remediate reporting API — everything green
  ./demo.sh status     Show the current state and the PR link

Typical run:  ./demo.sh reset  →  show the PR  →  ./demo.sh fix  →  ./demo.sh fix  →  merge

Full runbook + talking points: demo/DEMO.md
USAGE
    ;;

  *)
    echo "Unknown command: '$cmd'" >&2
    echo "Try:  ./demo.sh help" >&2
    exit 1
    ;;
esac
