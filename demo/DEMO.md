# IDOR demo — repeatable runbook

A reusable demo that shows DryRun Security catching an Insecure Direct Object
Reference (IDOR) that Veracode SAST does not flag, then the developer fixing it
and all checks going green.

## The pieces

- **Branch `demo/idor`** — the live PR branch: a customer "view order by id"
  feature that contains the IDOR. The demo PR is `demo/idor` → `main`.
- **Branch `demo/idor-vuln`** — save-point of the vulnerable state (IDOR present).
- **Branch `demo/idor-fixed`** — save-point of the same feature with the
  ownership-check fix applied.
- **Branch `main-baseline`** — the clean `main` snapshot to restore after a demo merge.
- **`demo/reset-demo.sh`** — rewinds everything back to the vulnerable start.
- **`demo/apply-fix.sh`** — advances `demo/idor` to the fix (the "developer fixes it" step).

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

## Talking points / Q&A

Speaker notes for the sharper questions a security audience tends to ask.
These are about the `test_2` vuln set (the SQLi/XXE/crypto bugs), which is a
richer cross-tool comparison than the single IDOR. None of this is shown to
the audience — it's your cheat sheet.

### "Why does DryRun show more findings than Veracode?"

Two independent reasons, not one:

1. **Different detection models.** Veracode's pipeline scan is data-flow /
   taint analysis on compiled bytecode — strong on injection and crypto
   (tainted input → dangerous sink), but it structurally does **not** flag
   authorization / business-logic flaws like IDOR, because there's no tainted
   sink to trace — it's a *missing ownership check*. DryRun's source-based
   analyzers (IDOR, SQLi, XSS, SSRF, Mass Assignment, Secrets, …) are built to
   reason about exactly that class.
2. **The org Veracode check applies a policy filter.** It reports only
   findings that violate org security policy ("Filtered pipeline findings: N"),
   so its count is lower than the raw scan. The repo-local JAR scan shows the
   full unfiltered set.

So you're comparing three different numbers: org-policy-filtered Veracode,
raw Veracode, and DryRun. They *should* differ.

### "Are the Veracode findings even real?" (FP triage)

From the 10 raw findings on `test_2`:

**6 clear true positives**

| Vulnerability | CWE | Location |
|---|---|---|
| SQL Injection | CWE-89 (High) | `ReportController.java` — `getCustomerReport`, string-concatenated SQL |
| Path Traversal | CWE-73 | `ReportController.java` — `exportReport`, `baseDir + filename` |
| XML External Entity (XXE) | CWE-611 | `ReportController.java` — `processInvoiceXml`, entities not disabled |
| Weak Cryptography | CWE-327 | `ReportController.java` — `verifyChecksum`, MD5 |
| Insecure Deserialization | CWE-502 | `ReportController.java` — `loadAnalyticsData`, `ObjectInputStream.readObject()` |
| Trust-all TLS | CWE-757* | `NetworkHelper.java` — empty `checkServerTrusted` + allow-all hostname verifier |

\* The issue is real; the CWE label is imprecise — this is really improper
cert validation (closer to CWE-295).

**2 debatable** — reflected XSS (CWE-80) in `UtilController` `/echo` and in the
invoice echo. Input *is* reflected, but the responses aren't `text/html`
(`/echo` raw bytes; `@RestController` String → `text/plain`), so a browser
won't execute script. Real reflection, weak real-world XSS.

**2 false-positive-leaning** — (a) CWE-798 "hard-coded credentials" in
`AppInitializer`: the password there is *randomly generated*; Veracode is
flagging the literal username `"admin"` next to password code — no secret is
hard-coded. (b) A CWE-80 XSS reported on the *same line* as the CWE-502
deserialize — overlapping/duplicate noise.

The point: a real scanner produces findings you triage and annotate. That's a
feature to show, not hide.

### "So why run both tools?" (the money slide)

The comparison is **bidirectional** — neither tool is a superset of the other:

| | DryRun caught | Veracode caught |
|---|---|---|
| **IDOR** (authorization logic, route-reachable) | ✅ | ❌ — no taint sink to trace |
| **Trust-all TLS** (insecure pattern in a utility class) | ❌ — unreachable + off-category | ✅ — pattern match ignores reachability |

Why DryRun misses the trust-all TLS: `NetworkHelper` is **dead code** — the
class is never imported or called, so there's no reachable attack surface, and
TLS/cert-validation isn't one of DryRun's analyzer categories. Veracode's
pattern/taint engine flags the insecure shape wherever it appears, reachable
or not.

Honest nuance for a security audience: because it's dead code, that TLS
finding isn't *currently* exploitable (nothing calls it). So Veracode flagging
it is both a **strength** (catches a latent bug before a dev wires it up) and
the kind of finding a reachability-first tool deliberately deprioritizes.
That tension is a good discussion to lean into — it's the real argument for
**hybrid SAST**: reachable business-logic coverage *and* whole-codebase
pattern coverage.
