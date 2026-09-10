#!/usr/bin/env bash
#
# Hybrid SAST demo — one entry point.
#
#   ./demo.sh submit   Push the update: the PR refreshes, Veracode and DryRun
#                      both run and both go red.
#   ./demo.sh reset    Rewind demo/idor to main so the next submit is a fresh
#                      "developer pushed code" moment.
#   ./demo.sh status   Show the current state and the PR link.
#
# The update lives on the save-point branch demo/idor-vuln. `submit` copies it
# onto demo/idor; `reset` points demo/idor back at main. main never changes.
#
set -euo pipefail

REMOTE="${REMOTE:-origin}"
REPO="Pete-Veracode-Demo/HybridSAST-Integration"
REPO_URL="https://github.com/$REPO"
PR_BRANCH="demo/idor"
VULN_BRANCH="demo/idor-vuln"

fetch() { git fetch "$REMOTE" --prune --quiet; }

# Prints: submitted | reset
demo_state() {
  if git merge-base --is-ancestor "$REMOTE/$VULN_BRANCH" "$REMOTE/$PR_BRANCH" 2>/dev/null; then
    echo submitted
  else
    echo reset
  fi
}

pr_link() {
  if command -v gh >/dev/null 2>&1; then
    gh pr list --repo "$REPO" --head "$PR_BRANCH" --state open --json url --jq '.[0].url' 2>/dev/null || true
  fi
}

cmd="${1:-help}"

case "$cmd" in
  submit)
    fetch
    echo "Pushing the update: $VULN_BRANCH -> $PR_BRANCH ..."
    git push -f "$REMOTE" "refs/remotes/$REMOTE/$VULN_BRANCH:refs/heads/$PR_BRANCH"
    echo
    link="$(pr_link)"
    echo "▶ Next:"
    if [ -n "$link" ]; then
      echo "  1. Open the PR: $link"
    else
      echo "  1. Open the PR ($PR_BRANCH -> main). If none is open, create it:"
      echo "       $REPO_URL/compare/main...$PR_BRANCH?expand=1"
    fi
    echo "  2. Watch the checks run (~5 min for Veracode):"
    echo "       DryRun  IDOR Analyzer, SQL Injection Analyzer, General Security → RED"
    echo "       Veracode Static Code Analysis – Pipeline → RED (SQLi High, OS command injection Very High)"
    echo "  3. Afterwards:  ./demo.sh reset"
    ;;

  reset)
    fetch
    echo "Rewinding $PR_BRANCH -> main ..."
    git push -f "$REMOTE" "refs/remotes/$REMOTE/main:refs/heads/$PR_BRANCH"
    echo
    echo "Done. Leave the PR open (it now shows no changes)."
    echo "▶ Next:  ./demo.sh submit"
    ;;

  status)
    fetch
    echo "Branch heads on $REMOTE:"
    for b in main "$PR_BRANCH" "$VULN_BRANCH"; do
      printf '  %-16s %s\n' "$b" "$(git rev-parse --short "$REMOTE/$b" 2>/dev/null || echo '(missing)')"
    done
    echo
    case "$(demo_state)" in
      submitted) echo "State: 🔴 SUBMITTED — the update is on $PR_BRANCH; checks should be red." ;;
      reset)     echo "State: ⚪ RESET — $PR_BRANCH is at main; run ./demo.sh submit." ;;
    esac
    link="$(pr_link)"
    echo "PR:    ${link:-$REPO_URL/pulls}"
    ;;

  help|-h|--help|"")
    cat <<USAGE
Hybrid SAST demo — one command:

  ./demo.sh submit   Push the update — Veracode and DryRun both go red
  ./demo.sh reset    Rewind demo/idor to main for the next run
  ./demo.sh status   Show the current state and the PR link

Typical run:  ./demo.sh reset  →  ./demo.sh submit  →  show the PR checks

Runbook + talking points: demo/DEMO.md
USAGE
    ;;

  *)
    echo "Unknown command: '$cmd'" >&2
    echo "Try:  ./demo.sh help" >&2
    exit 1
    ;;
esac
