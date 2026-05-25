---
name: project-verification-gap
description: 100% accuracy claim is unverifiable without a reference output; GnuCOBOL/Java/Maven probes are sandbox-blocked
metadata:
  type: project
---

As of 2026-05-24 on `feature/nova`, **no Java port can be declared "100% accurate"** because there is no reference output to diff against.

**Why:**
- `cobc --version`, `java --version`, and `mvn --version` calls were all blocked by the Bash/PowerShell sandbox.
- `Glob` searches for `C:\Program Files\Java\**\bin\java.exe` returned nothing (but didn't conclusively prove absence — directory may not be granted).
- Without GnuCOBOL the harness cannot recompile the COBOL and produce a fresh reference. Without captured mainframe goldens the harness has nothing to diff against.

**How to apply:** Three options the user must pick between before any pilot port is started:

1. **GnuCOBOL local** — user installs GnuCOBOL on Windows (or grants the agent permission to invoke `cobc`). Harness compiles each COBOL program, runs against ASCII fixtures, diffs Java output byte-for-byte. Highest fidelity.
2. **Captured goldens** — user provides per-program output files captured from the mainframe or from a prior known-good run. Harness only checks Java output against the golden; never re-runs COBOL.
3. **Derived-from-copybook** — for trivial pass-through programs (CBACT02C/CBACT03C: read VSAM record, DISPLAY to SYSOUT), the expected SYSOUT can be hand-derived from the input fixture using only the copybook layout. This works for the first two pilots but does NOT scale to CBACT04C (interest math + control break + multiple file outputs).

Recommended: option 3 for the first two pilots (CBACT03C and CBACT02C) as a confidence-building exercise that doesn't depend on the user, then escalate to option 1 or 2 before CBACT01C / CBACT04C.

**Surface this as the first open question in any "ready to start porting" report.** Don't proceed past Commit 1 until the user picks an option.
