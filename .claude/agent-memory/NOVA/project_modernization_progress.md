---
name: project-modernization-progress
description: Snapshot of CardDemo modernization status as of 2026-05-24 — what's documented, what's translated, what's untouched
metadata:
  type: project
---

CardDemo modernization is in the **documentation + pilot-translation** phase. As of the snapshot date 2026-05-24:

- **Documentation coverage (TODO.md):** 42 of 94 files done. Batch programs are fully documented (CBACT01-04C, CBCUS01C, CBSTM03A/B, CBTRN01-03C, COBSWAIT). Online programs are half-done — sign-on, main menu, transaction screens, user-management screens, reports menu still TODO. Most copybooks (CVCRD01Y, CVACT02Y, CVTRA01-04Y, CVTRA06-07Y, CSUTL*, CSLKPCDY, CSSTRPFY, CSUSR01Y, CSMSG02Y) and most JCL (initialization, reports, security/admin, utilities, GDG defines) are not yet documented.
- **Java pilot:** Only CBACT04C (Interest Calculator) has been ported to Java. Lives in `app/java/batch_processing_workflow/`. Functional equivalence verified for the interest formula via 14 unit + 7 integration tests; a byte-by-byte run vs. GnuCOBOL CBACT04C_TEST is documented in `cobol_test/BYTE_BY_BYTE_VERIFICATION.md` (first 290 bytes hash-match, differences confined to timestamps and trailing padding).
- **Java pilot gaps:** Only one file-based repository (`FileBasedTransactionCategoryBalanceRepository`) is implemented; AccountRepository / CardXrefRepository / DiscountGroupRepository / TransactionRepository remain interfaces with only in-memory test implementations. No CI, no integration test running against full VSAM-sized data.
- **Optional modules** (`app/app-authorization-ims-db2-mq`, `app/app-transaction-type-db2`, `app/app-vsam-mq`) are untouched by both documentation and Java translation.

**Why:** Project is following the pattern documented in `.claude/agents/cobol-documentation-translator.md` and `COBOL_CODEBASE_UNDERSTANDING.md` — discover via documentation first, then pilot a translation to prove the approach, then scale.

**How to apply:**
- When the user asks "what next," prioritize closing the documentation gap on the daily-batch chain (so the next Java port has full upstream/downstream context) before broadening to other Java ports.
- The next sensible Java targets are the batch chain neighbors of CBACT04C: CBTRN02C (posting, runs immediately before) and CBSTM03A/B (statements, runs immediately after), because they share copybooks (CVACT01Y, CVTRA05Y, CVACT03Y) and the work to model those in Java is already partly done.
- File-based repositories are the blocker turning the existing Java port from "tests pass" into "you can actually run a real batch." Closing this is high leverage.
- Validate this snapshot against `TODO.md` and `git log` before reciting it; the picture moves fast.
