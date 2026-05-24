# VSAM KSDS and Alternate Indexes

> The mainframe's equivalent of an indexed table — and the secondary-index gotcha that makes a port non-trivial.

## Why this matters for migration

CardDemo's primary data store is **VSAM** — IBM's record-oriented file system. Six files hold the application's state: ACCTFILE, CARDFILE, CUSTFILE, XREFFILE, TRANFILE, TCATBALF. There is no relational database in the core application.

A Java port has two reasonable choices:
1. **Keep the file format** — read and write the same byte layouts. Simplest path to byte-equivalent verification.
2. **Move to a database** — load VSAM into PostgreSQL/DynamoDB and re-author batch as SQL. Better long-term, but verification gets harder (you're proving "semantic equivalence" instead of "byte equivalence").

This repo starts with option 1 because it makes the 100%-accuracy claim falsifiable. Option 2 is a Phase 7+ topic.

## What VSAM actually is

VSAM (Virtual Storage Access Method) is a record-oriented file format. There are several types; CardDemo uses **KSDS** (Key-Sequenced Data Set) almost exclusively.

A KSDS is conceptually a B-tree-indexed sorted file:
- Every record has a **primary key** (one or more contiguous bytes at a fixed offset).
- Records are stored sorted by key.
- You can read by key (random) or in key order (sequential).
- Inserting a new record places it in key order automatically.

If you've used a single-table key-value store with sorted-range scan (DynamoDB, RocksDB, Berkeley DB) you already understand KSDS.

## CardDemo's KSDS files

| File | Primary key | Record length | Purpose |
|---|---|---|---|
| ACCTFILE | Account ID (11 bytes) | 300 | Account master |
| CARDFILE | Card number (16 bytes) | 150 | Card master |
| CUSTFILE | Customer ID (9 bytes) | 500 | Customer master |
| XREFFILE | Card number (16 bytes) | 50 | Card ↔ Account ↔ Customer cross-reference |
| TRANFILE | Transaction ID (16 bytes) | 350 | Transaction master |
| TCATBALF | Composite (11+4+2 bytes) | 50 | Transaction category balance |

Record lengths are fixed — every record in a file is exactly the same number of bytes. That's a structural difference from CSV or JSON line-delimited formats.

## Sequential read in COBOL

The simplest COBOL access pattern: open the file, read records in key order until EOF.

```cobol
PERFORM UNTIL END-OF-FILE
    READ XREFFILE-FILE NEXT RECORD
        AT END SET END-OF-FILE TO TRUE
    END-READ
    IF NOT END-OF-FILE
        DISPLAY XREF-CARD-NUM " -> " XREF-ACCT-ID
    END-IF
END-PERFORM
```

In Java, this is a `FileInputStream` + a loop that reads exactly `RECORD-LENGTH` bytes per iteration, returning when the stream is exhausted. No line breaks to find — just count bytes.

## Random read by key

When a batch program needs to look up an account by ID:

```cobol
MOVE INPUT-ACCT-ID TO ACCT-RECORD-KEY
READ ACCTFILE-FILE KEY IS ACCT-RECORD-KEY
    INVALID KEY DISPLAY "Account not found"
END-READ
```

In Java, this is either:
- A pre-loaded `Map<String, byte[]>` if the file fits in RAM (CardDemo's data does — ACCTFILE is ~15 KB).
- A B-tree on disk (a simple `MappedByteBuffer` over a sorted file, binary-searching the keys) if memory matters.

For pilot work we use the in-memory map. Real KSDS read latency would be 1–10 ms; in-memory is ~10 µs — but byte-equivalent output doesn't depend on read latency, only on what bytes get read.

## Alternate Index (AIX) — the secondary-index gotcha

Some queries want to find records by something other than the primary key. Example: find all transactions processed today, where the primary key is `TRAN-ID` but you want to range-scan by `TRAN-PROC-TS`.

VSAM solves this with an **Alternate Index** (AIX): a separate KSDS whose records are `(alternate-key → primary-key)` pairs. To read by alternate key, you open both the AIX and the base file, then the runtime joins them automatically.

CardDemo builds an AIX on TRANFILE keyed on the **transaction processed timestamp** (bytes 304–329 of the transaction record, 26 bytes — verifiable in `app/jcl/TRANIDX.jcl` with `KEYS(26 304)`). The COMBTRAN job uses this AIX to walk transactions in chronological order.

A Java port must replicate this access path. Two options:
- Build an in-memory `TreeMap<TimestampKey, TransactionRecord>` from the master file at startup.
- Materialize an actual secondary-index file at the same byte layout. Required only if a downstream COBOL job reads the AIX file directly (none currently do).

For this repo's pilot work, the in-memory `TreeMap` is sufficient.

## What the Java port owes

For every VSAM file:
1. A `Repository` interface (`AccountRepository`, `CardRepository`, …) with `findByKey`, `iterateInKeyOrder`, optionally `insert` and `update`.
2. A concrete implementation that reads/writes the exact byte layout described in the matching copybook (`app/cpy/CV*Y.cpy`).
3. For files with an AIX, a separate `iterateByAlternateKey` method.

Each repository is paired with a round-trip test: load a sample file, iterate all records, write them back out, byte-diff against the original. Zero diff is the pass condition. This catches both decoder and encoder bugs in one shot.

## References (re-verifiable)

- VSAM KSDS overview: IBM z/OS documentation, available publicly.
- File-to-key mapping: `app/csd/CARDDEMO.CSD` (CICS file definitions) and the copybooks in `app/cpy/`.
- AIX definition: `app/jcl/TRANIDX.jcl` (`DEFINE AIX … KEYS(26 304)` — 26-byte key starting at offset 304).
- AIX usage: `app/cbl/CBSTM03A.CBL` and `CBSTM03B.CBL` (statement generation reads transactions in chronological order via the AIX path).
