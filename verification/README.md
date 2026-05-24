# Verification: how we prove 100% accuracy

> The Java port is "correct" when it produces the same output bytes the COBOL program would have produced, given the same input bytes.

## The accuracy contract

For every batch program ported in this repo, the contract is:

1. **Byte-equivalent file output** — every output dataset the COBOL program writes must be byte-for-byte identical to what the Java port writes, given the same input.
2. **Semantic-equivalent logs** — what the COBOL program writes to SYSOUT (the job log) must match in meaning. Exact byte equivalence is not required for log lines; presence of every expected message in the same order is.
3. **Exact return codes** — `RC=0` from COBOL must be `System.exit(0)` from Java. `RC=4` must be `4`. And so on.

Anything weaker than this is not "100% accuracy."

## Three ways to get a reference output

The Java port needs a reference output to diff against. There are three ways to get one.

### Option A — Run COBOL locally with GnuCOBOL

GnuCOBOL is an open-source COBOL compiler. If installed, every test can recompile the COBOL source, run it against the test input, and diff the output. This is the most rigorous option because the reference is regenerated every test run from the actual source.

Pros: zero drift between COBOL source and reference.
Cons: GnuCOBOL doesn't support every IBM extension. CardDemo programs that use mainframe-specific features (assembler CALLs, full CICS, full VSAM) may not compile or run identically.
Status: not yet probed on this machine. To be verified.

### Option B — Captured mainframe goldens

Run each COBOL program once on the real mainframe (or a UniKix runtime), capture the output files byte-for-byte, commit them to this repo as "golden" fixtures. Every Java test diffs against the captured golden.

Pros: the goldens come from the actual production runtime — no compiler-substitution risk.
Cons: regenerating the golden requires mainframe access. A change to the COBOL source has no automatic effect on goldens; humans must regenerate.
Status: not yet captured. Requires user action.

### Option C — Derived-from-copybook expected output

For pilot programs that are pure pass-throughs (read a file, write a transformed view of the same bytes — no signed math, no rounding, no date handling), the expected output can be derived analytically from the input plus the copybook layout. The test writes both the Java output and the analytically-computed expected output, then diffs.

Pros: requires nothing beyond what's already in this repo.
Cons: only works for trivial transformations. Breaks the moment a program does arithmetic, control breaks, or date math.
Status: usable for CBACT03C and CBACT02C (sequential read + display). Not usable for CBACT01C or CBACT04C.

## The plan

| Pilot | Verification approach |
|---|---|
| CBACT03C (XREFFILE seq read, DISPLAY only) | Option C — derived from copybook |
| CBACT02C (CARDFILE seq read, DISPLAY only) | Option C — derived from copybook |
| CBACT01C (adds COMP-3, OCCURS, external CALL) | Option A if GnuCOBOL available, else Option B |
| CBACT04C (interest calculation, multi-file) | Option A or Option B; **must escalate before this pilot lands** |
| CBTRN02C (transaction posting, multi-file write) | Option A or Option B |

The first two pilots intentionally don't depend on COBOL execution. They prove the file-reading codec and the Java→test plumbing work end-to-end. If Options A or B aren't available by the time we reach CBACT01C, that's a stop-the-line moment and the user is asked to choose.

## What lives in this directory (eventually)

- `goldens/` — captured reference output files, if using Option B. One subdirectory per program.
- `inputs/` — the input fixtures every test run loads. Some are copied from `app/data/`; some are synthetic (e.g., negative-balance test data, since `acctdata.txt` has none).
- `harness/` — a small Java module with byte-diff and structured-diff utilities, plus per-program test harnesses that wire the Java program to its input/output paths.

Nothing in this directory yet — it lands with Phase 1 (the codec module).

## Test-data gaps already identified

- `app/data/ASCII/acctdata.txt` contains 50 account records, **all with non-negative balances**. The negative-balance decode path is not exercised by any pilot that only reads ACCTFILE. The codec round-trip test on the larger fixture covers it; the per-program tests for CBACT01C and CBACT04C will need synthetic negative-balance fixtures.
- `app/data/ASCII/dailytran.txt` does exercise the negative-overpunch path (row 2: `0000009190}` = −919.00, verified). That covers the codec; not the account-balance posting paths.
