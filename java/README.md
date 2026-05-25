# Java source

Maven multi-module project. Java 17.

## Modules

| Module | Status | Purpose |
|---|---|---|
| `cobol-codec` | Phase 1 — initial | Zoned-decimal & overpunch encode/decode, EBCDIC↔ASCII translation, PIC clause parsing. Pure functions, no I/O. |
| `vsam-io`      | not started | KSDS sequential and random readers; AIX path reader. Reads the exact byte layouts in `app/data/`. |
| `batch-programs` | not started | One Java class per COBOL batch program (e.g. `CBACT03C`, `CBACT04C`). |
| `verification` | not started | Golden harness, byte-diff utility, per-program test wrappers. |

## Build

From the `java/` directory:

```
mvn clean verify
```

Requires Java 17 and Maven 3.9+. The build has no native dependencies.

## Why this layout

- **`cobol-codec` is a pure-function library** with no file I/O. That makes it
  trivially unit-testable and lets every other module depend on it without
  pulling in heavyweight transitive dependencies.
- **`vsam-io` depends only on `cobol-codec`.** It exposes `Repository`-style
  interfaces over the on-disk byte layouts. The first implementation reads
  the ASCII fixtures in `app/data/`; an EBCDIC implementation can be added
  without touching the public API.
- **`batch-programs` depends on `vsam-io` and `cobol-codec`.** Each batch
  program becomes a `public static void main(String[])` class. CLI flags map
  one-to-one with the DD statements of the corresponding JCL job.
- **`verification` depends on everything.** It hosts the harness that runs a
  Java batch program against a fixture and diffs the output against the
  reference (option C derived-from-copybook for the first two pilots; option
  A GnuCOBOL or option B captured-goldens once available).

See `../verification/README.md` for the verification strategy and the three
reference-output options.
