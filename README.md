# mainframetojava

Documentation and Java translation of the AWS CardDemo mainframe application, built for modern engineers who don't know COBOL.

## What this repo is

CardDemo is a credit card management system written in COBOL, CICS, VSAM, and JCL. Source code lives in [aws-samples/aws-mainframe-modernization-carddemo](https://github.com/aws-samples/aws-mainframe-modernization-carddemo). This repo translates it to Java with a verifiable 100%-accuracy migration path.

**Audience:** Java developers, AI engineers, and Cloud Architects evaluating mainframe modernization. No prior COBOL/CICS/VSAM knowledge assumed.

**Migration target:** byte-equivalent file output, semantic-equivalent logs, exact return codes.

## What's in here

- `docs/concepts/` — Mainframe primitives explained for engineers from a Java/cloud background.
- `docs/programs/` — Per-program walkthroughs of COBOL source paired with the Java port.
- `verification/` — How "100% accuracy" is measured.
- `java/` — Maven multi-module Java 17 source (added in a later commit).

## Status

| Phase | Status |
|---|---|
| Phase 0 — Foundation docs | **in progress** (this commit) |
| Phase 1 — Codec module (zoned-decimal, EBCDIC↔ASCII) | not started |
| Phase 2 — Pilot 1: CBACT03C (XREFFILE sequential read) | not started |
| Phase 3 — Pilot 2: CBACT02C (CARDFILE sequential read) | not started |
| Phase 4 — Pilot 3: CBACT01C (adds COMP-3, OCCURS, external CALL) | not started |
| Phase 5 — Pilot 4: CBACT04C (interest calculation, multi-file) | not started |
| Phase 6 — Pilot 5: CBTRN02C (daily transaction posting) | not started |
| Phase 7 — Remaining batch, online (CICS), optional modules | not started |

## Start here

1. [EBCDIC and zoned-decimal numbers](docs/concepts/01-ebcdic-and-zoned-decimal.md) — why a balance is stored as 12 ASCII-looking bytes ending in `{`.
2. [VSAM KSDS and Alternate Indexes](docs/concepts/02-vsam-ksds-and-aix.md) — the mainframe's equivalent of an indexed table.
3. [JCL and the daily batch](docs/concepts/03-jcl-and-the-daily-batch.md) — how a nightly job is wired together.
4. [Verification strategy](verification/README.md) — how we prove the Java port matches the COBOL.

## How this repo relates to the source

This repo never modifies the COBOL source. It treats the source repo as read-only input. Every claim about COBOL behavior in these docs cites a specific file and line range so it can be re-verified.
