# Hybrid SAST demo — runbook

One pull request, pushed from your terminal, that both scanners flag:

- **DryRun** flags the **IDOR** (authorization logic) and the SQL injection.
- **Veracode SAST** flags **SQL injection (High)** and **OS command injection
  (Very High)** as policy violations, plus 10 Medium findings in the raw
  results.

No fix step. The point is the side-by-side: what each tool sees on the same
diff.

## The pieces

- **`demo/idor`** — the live PR branch. The demo PR is `demo/idor` → `main`.
- **`demo/idor-vuln`** — save-point holding the update: the customer "view
  order by id" feature with the IDOR, plus `ReportController` / `NetworkHelper`
  with the SAST findings.
- **`demo.sh`** — `submit`, `reset`, `status`.

`main` never changes during the demo. Nothing vulnerable is ever merged.

| Branch | Keep because |
|---|---|
| `main` | trunk |
| `demo/idor` | the live demo PR branch |
| `demo/idor-vuln` | save-point: the update |
| `main-baseline` | clean snapshot of `main`; unused by the scripts now, safe to leave |

Anything else is a stale experiment.

## Run the demo

```
./demo.sh reset     # before the audience arrives: demo/idor back at main
./demo.sh submit    # on stage: "the developer pushes their update"
```

`submit` force-pushes `demo/idor-vuln` onto `demo/idor`. The PR
(`demo/idor` → `main`) picks up the commits and every check re-runs:

- DryRun — **IDOR Analyzer, SQL Injection Analyzer, General Security → red**,
  within a minute, with inline findings on `viewOrder` and `ReportController`.
- Veracode **Static Code Analysis – Pipeline → red** after ~5 minutes
  (build the JAR, upload, scan) with 2 policy violations.

Then talk through the checks. Afterwards `./demo.sh reset` so the next run is
a fresh push.

If the PR was closed, reopen it or create a new one from the link `submit`
prints. Leave it open between runs — after `reset` it just shows no changes.

## What Veracode is actually scanning

The org Veracode integration (the `veracode` repo) builds the PR head with
`veracode package`, which runs `./gradlew` and uploads the Spring Boot JAR
(~70 MB). If the build job's artifact is a few KB instead, the packager fell
back to zipping the JavaScript and the scan is meaningless — that was the
state of this repo until `gradlew` was made executable.

The pipeline scan is of the **whole JAR**, not the diff, and the check
reports only findings that violate the org policy
(`Veracode Recommended Medium + SCA` → High and Very High only).

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
   findings, so the check shows 2 violations while the raw scan has 12
   (1 Very High, 1 High, 10 Medium). The Mediums (path traversal, XXE, MD5,
   deserialization, trust-all TLS, XSS) are in the raw results and on the
   platform, just not gating.

So you're comparing three different numbers: org-policy-filtered Veracode,
raw Veracode, and DryRun. They *should* differ.

### "Are the Veracode findings even real?" (FP triage)

From the 12 raw findings on `demo/idor-vuln`:

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
| **Command injection / SQLi** (tainted input → dangerous sink) | SQLi ✅ | ✅ both |
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
