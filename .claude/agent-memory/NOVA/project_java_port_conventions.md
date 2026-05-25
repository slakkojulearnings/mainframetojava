---
name: project-java-port-conventions
description: Established conventions and trade-offs in the existing CBACT04C Java port — follow these when porting additional COBOL batch programs
metadata:
  type: project
---

The CBACT04C Java pilot (`app/java/batch_processing_workflow/`) establishes the conventions future ports should match unless there's a reason to diverge.

**Conventions:**
- Maven layout: `com.carddemo.batch.<feature>.{model,service,repository,util}`.
- **All money is `BigDecimal`** with `RoundingMode.HALF_UP` and scale 2 — preserves COBOL COMP-3 semantics. Anything else (double/float) will break byte-for-byte parity.
- IDs that are numeric in COBOL (PIC 9(11) account id, PIC 9(16) card number) are kept as `String` in Java to preserve leading zeros and avoid integer overflow for 16-digit card numbers.
- Repository interfaces in `repository/`, in-memory test impls in `src/test/.../repository/`, file-based impls live alongside the interface (only TCATBALF has one today).
- **Control-break logic** mirrors COBOL paragraph structure — see `InterestCalculator.java` while-loop pattern. Don't restructure into streams; debuggability and provability against the original matters more than idiom.
- Timestamps use DB2 format `YYYY-MM-DD-HH.MM.SS.MS0000` via `TimestampFormatter`.
- ABENDs translate to `RuntimeException` + `System.exit(999)`; tests skip the System.exit to keep the JVM alive (see test infrastructure).

**Why:** These choices were validated by the comparison in `cobol_test/BYTE_BY_BYTE_VERIFICATION.md` and `COMPARISON_RESULTS.md`. The first 290 bytes of the output transaction record hash-match COBOL output (formula, amount fields, IDs are identical). Remaining differences are timestamps, trailing padding, and GnuCOBOL's signed-decimal encoding for `LINE SEQUENTIAL` output — these are format edges, not business-logic differences. Deviating on `BigDecimal`, scale, or rounding mode would break the formula property that was proven.

**How to apply:**
- New COBOL batch ports should reuse `CobolDataConverter`, `TimestampFormatter`, and the same package structure rather than rolling their own.
- When proposing the next port, lean toward programs that already share copybooks with CBACT04C (CVACT01Y, CVTRA05Y, CVACT03Y) so the Java `Account`/`Transaction`/`CardXref` models can be reused — concretely, CBTRN02C and CBSTM03A/B.
- Online (CO*) programs are a different problem entirely (BMS screens, COMMAREA pseudo-conversation, CICS) and shouldn't be tackled with this batch template — they need a separate Spring MVC / REST approach decision before porting.
