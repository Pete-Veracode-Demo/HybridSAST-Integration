# Hybrid SAST demo — repeatable runbook

A reusable demo of one pull request that carries two kinds of bug at once:

- an **IDOR** (authorization logic) that DryRun catches and Veracode SAST
  structurally does not, and
- a set of **injection / crypto / TLS** bugs — SQL injection (High) and OS
  command injection (Very High) among them — that Veracode catches.

The developer fixes it in two steps. After step 1 DryRun is green and Veracode
is still red; after step 2 everything is green and the PR merges.

## The pieces

- **Branch `demo/idor`** — the live PR branch. The demo PR is `demo/idor` → `main`.
- **Branch `demo/idor-vuln`** — save-point of the vulnerable state: the customer
  "view order by id" feature with the IDOR, plus `ReportController` /
  `NetworkHelper` with the SAST findings.
- **Branch `demo/idor-fixed`** — save-point after step 1: ownership check added,
  reporting API still vulnerable.
- **Branch `demo/all-fixed`** — save-point after step 2: reporting API and
  network helper remediated.
- **Branch `main-baseline`** — the clean `main` snapshot to restore after a demo merge.
- **`demo/reset-demo.sh`** — rewinds everything back to the vulnerable start.
- **`demo/apply-fix.sh idor|sast`** — advances `demo/idor` one step.

## Branches in this repo

These are the **only** branches that should exist. If you see others, they're
stale experiments and safe to delete.

| Branch | Keep because |
|---|---|
| `main` | trunk |
| `main-baseline` | demo reset save-point (`reset-demo.sh` restores `main` from it) |
| `demo/idor` | the live demo PR branch |
| `demo/idor-vuln` | save-point: vulnerable state |
| `demo/idor-fixed` | save-point: IDOR fixed, SAST findings still present |
| `demo/all-fixed` | save-point: everything fixed |

`main` and `main-baseline` must stay identical between demos. If you merge a
non-demo change to `main`, fast-forward `main-baseline` to match
(`git push origin main:main-baseline`) or the next reset will undo it.

## Quick start (the easy way)

One command, run from the repo root:

```
./demo.sh reset     # back to the vulnerable start (before each run)
./demo.sh fix       # step 1: ownership check   → DryRun green, Veracode red
./demo.sh fix       # step 2: remediate reports → all green
./demo.sh status    # show the current state + the PR link
```

`fix` applies whichever step is next; `fix-idor` / `fix-sast` pick one
explicitly. Each command prints what to do next.

## Run the demo

1. **Reset to the vulnerable start** (do this before each run):
   ```
   ./demo.sh reset
   ```
   Then make sure the PR `demo/idor` → `main` is **open** (reopen it if a
   previous run merged/closed it).

2. **Show the vulnerable PR.** On `demo/idor` → `main`:
   - DryRun **IDOR Analyzer → red** (inline finding on `viewOrder`).
   - Veracode **Static Code Analysis – Pipeline → red** with the policy
     violations: SQL injection (High) and OS command injection (Very High)
     in `ReportController`.

3. **"The developer fixes the IDOR."**
   ```
   ./demo.sh fix
   ```
   Pushes the ownership-check commit onto `demo/idor`. DryRun goes **green**;
   Veracode is **still red** — the reporting API hasn't been touched. This is
   the beat where you say neither tool alone would have let this merge.

4. **"The developer fixes the reporting API."**
   ```
   ./demo.sh fix
   ```
   Pushes the remediation commit. All checks go **green**.

5. **"They merge."** Merge the PR (`demo/idor` → `main`) in the GitHub UI.

## Re-run later

Run `./demo.sh reset` again. It restores `main` to `main-baseline` (undoing
the merge) and rewinds `demo/idor` back to `idor-vuln`. Reopen the PR if it
was merged/closed.

> Note: `reset-demo.sh` force-pushes `main` back to `main-baseline`. If `main`
> is branch-protected against force pushes, either temporarily disable that
> protection for the reset, or skip the merge in step 5 (just show the green
> checks) so `main` never changes and only `demo/idor` needs rewinding.

## What Veracode is actually scanning

The org Veracode integration (the `veracode` repo) builds the PR head with
`veracode package`, which runs `./gradlew` and uploads the Spring Boot JAR
(~70 MB). If the build job's artifact is a few KB instead, the packager fell
back to zipping the JavaScript and the scan is meaningless — that was the
state of this repo until `gradlew` was made executable.

The pipeline scan is of the **whole JAR**, not the diff, and the check
reports only findings that violate the org policy.

## Talking points / Q&A

Speaker notes for the sharper questions a security audience tends to ask.
None of this is shown to the audience — it's your cheat sheet.

### "Why does DryRun show more findings than Veracode?"

Two independent reasons, not one:

1. **Different detection models.** Veracode's pipeline scan is data-flow /
   taint analysis on compiled bytecode — strong on injection and crypto
   (tainted input → dangerous sink), but it structurally does **not** flag
   authorization / business-logic flaws like IDOR, because there's no tainted
   sink to trace — it's a *missing ownership check*. DryRun's source-based
   analyzers (IDOR, SQLi, XSS, SSRF, Mass Assignment, Secrets, …) are built to
   reason about exactly that class.
2. **The org Veracode check applies a policy filter.** With
   `Veracode Recommended Medium + SCA` it reports only High and Very High
   findings, so the check shows 2 violations while the raw scan has ~11. The
   Mediums (path traversal, XXE, MD5, deserialization, trust-all TLS, XSS) are
   in the raw results and on the platform, just not gating.

So you're comparing three different numbers: org-policy-filtered Veracode,
raw Veracode, and DryRun. They *should* differ.

### "Are the Veracode findings even real?" (FP triage)

From the raw findings on `demo/idor-vuln`:

**7 clear true positives**

| Vulnerability | CWE | Location |
|---|---|---|
| OS Command Injection | CWE-78 (Very High) | `ReportController.java` — `archiveReport`, `Runtime.exec` with a request param in the command string |
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
| **Command injection / SQLi** (tainted input → dangerous sink) | depends on analyzer | ✅ |
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

### "What does step 2 actually change?"

The remediation commit on `demo/all-fixed`, if someone asks to see the fix:

- SQL: parameterized `?` placeholders.
- Export / archive: report ids resolve through a fixed catalogue map, so no
  caller-supplied path fragment ever reaches the filesystem. Archives are
  built with `java.util.zip` — no shell.
- XML: DTDs and external entities disabled; echoed value HTML-escaped.
- MD5 → SHA-256.
- Analytics upload accepts JSON, not Java serialization.
- `NetworkHelper` uses the JVM default trust store and `SecureRandom`.
