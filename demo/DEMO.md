# IDOR demo — repeatable runbook

A reusable demo that shows DryRun Security catching an Insecure Direct Object
Reference (IDOR) that Veracode SAST does not flag, then the developer fixing it
and all checks going green.

## The pieces

- **Branch `demo/idor`** — a customer "view order by id" feature that contains
  the IDOR. The demo PR is `demo/idor` → `main`.
- **Tag `idor-vuln`** — the vulnerable commit (IDOR present). The demo starts here.
- **Tag `idor-fixed`** — the same feature with the ownership-check fix applied.
- **Tag `main-baseline`** — the clean `main` snapshot to restore after a demo merge.
- **`demo/reset-demo.sh`** — rewinds everything back to the vulnerable start.
- **`demo/apply-fix.sh`** — pushes the fix commit onto `demo/idor` (the "developer fixes it" step).

## Run the demo

1. **Reset to the vulnerable start** (do this before each run):
   ```
   ./demo/reset-demo.sh
   ```
   Then make sure the PR `demo/idor` → `main` is **open** (reopen it if a
   previous run merged/closed it).

2. **Show the vulnerable PR.** On `demo/idor` → `main`:
   - DryRun **IDOR Analyzer → red** (with an inline finding on `viewOrder`).
   - Veracode **Static Code Analysis – Pipeline → green** (SAST does not flag IDOR).

3. **"The developer makes the fix."**
   ```
   ./demo/apply-fix.sh
   ```
   This pushes the ownership-check commit onto `demo/idor`; the checks re-run
   and go **all green**.

4. **"They merge the fix."** Merge the PR (`demo/idor` → `main`) in the GitHub UI.

## Re-run later

Just run `./demo/reset-demo.sh` again. It restores `main` to `main-baseline`
(undoing the merge) and rewinds `demo/idor` back to `idor-vuln`, so the PR shows
the IDOR again. Reopen the PR if it was merged/closed.

> Note: `reset-demo.sh` force-pushes `main` back to `main-baseline`. If `main`
> is branch-protected against force pushes, either temporarily disable that
> protection for the reset, or skip the merge in step 4 (just show the green
> checks) so `main` never changes and only `demo/idor` needs rewinding.
