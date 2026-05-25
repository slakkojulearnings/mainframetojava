---
name: project-carddemo-facts
description: Empirically verified facts about CardDemo encoding, file layouts, and batch path — established 2026-05-24 by reading bytes
metadata:
  type: project
---

Facts established by reading actual source bytes on 2026-05-24. Distinct from carried-over claims; these are re-verifiable from the current repo state.

**Counts in `app/` core (excludes `app/app-*` optional modules):**
- 29 COBOL programs (`*.cbl` + `*.CBL` — CBSTM03A/B use uppercase ext)
- 29 copybooks (`*.cpy`)
- 31 JCL jobs
- 17 BMS mapsets
- 2 procs

**Daily batch critical path (verified from `scripts/run_full_batch.sh`):**
CLOSEFIL → ACCTFILE → CARDFILE → XREFFILE → CUSTFILE → TRANBKP → DISCGRP → TCATBALF → TRANTYPE → DUSRSECJ → POSTTRAN (CBTRN02C) → INTCALC (CBACT04C, PARM='2022071800') → TRANBKP → COMBTRAN → TRANIDX → OPENFIL. Every referenced JCL exists in `app/jcl/`.

**No COMP-3 in core on-disk VSAM records.** Grep of `app/cpy/` for `COMP-3|PACKED-DECIMAL|USAGE COMP|USAGE BINARY` returns zero matches. COMP-3 appears in optional modules (`app/app-authorization-ims-db2-mq`, `app/app-transaction-type-db2`, `app/app-vsam-mq`) and in working-storage counters of online programs (COCRDLIC, COBIL00C, COACTUPC, CBSTM03A) and in CBACT01C's *output* file. The data flowing through the daily batch is all display zoned-decimal.

**Sample data file layouts (verified by byte count):**
- `app/data/EBCDIC/AWS.M2.CARDDEMO.ACCTDATA.PS` = 15000 bytes = 50 records × 300 bytes (RECFM=FB, no terminators, EBCDIC encoded — first byte `0xF0` = EBCDIC '0')
- `app/data/ASCII/acctdata.txt` = 15100 bytes = 50 records × 302 bytes (300 data + CRLF). Plain ASCII.
- `dailytran.txt` = 105600 bytes = 300 records × 352 bytes (350 + CRLF). Matches CVTRA05Y RECLN=350.

**Sign-overpunch table verified empirically:**
- `{`=+0, `A`=+1, `B`=+2, ..., `I`=+9
- `}`=-0, `J`=-1, `K`=-2, ..., `R`=-9
- `dailytran.txt` row 2 `TRAN-AMT` = `0000009190}` decodes to -919.00, and the description "Return item at Nitzsche..." is consistent with a refund. Negative-amount fixtures DO exist in dailytran but NOT in acctdata (all 50 accounts have positive balances, so the codec's negative-balance path is untested by acctdata alone).

**EBCDIC sign-overpunch carries through to EBCDIC bytes too:** `0xC7` (EBCDIC 'G') in `AWS.M2.CARDDEMO.DALYTRAN.PS` row 1 byte 35 corresponds to ASCII 'G' (+7) in `dailytran.txt` row 1 byte 132. Encoding-neutral.

**How to apply:** These are anchors for the verification harness and codec scope. The zoned-decimal codec MUST handle the full overpunch table. The Java port can use `BigDecimal` with implied scale from the COBOL `V99` clause — no packed-decimal/COMP-3 work is needed for the daily-batch path.
