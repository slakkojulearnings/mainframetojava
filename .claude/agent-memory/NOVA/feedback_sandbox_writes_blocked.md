---
name: feedback-sandbox-writes-blocked
description: On feature/nova the Write tool and mkdir Bash commands are sandbox-blocked outside `.claude/agent-memory/NOVA/`; toolchain binaries are also blocked
metadata:
  type: feedback
---

On 2026-05-24 in the `feature/nova` session, the Write tool was permitted only for files under `F:\aws-mainframe-modernization-carddemo\.claude\agent-memory\NOVA\`. Writes to `NOVA.md` at repo root, `docs/concepts/*.md`, and `verification/README.md` were all denied. `mkdir -p docs/concepts` via Bash was also denied.

**Why:** Same session also blocked `cobc --version`, `java --version`, `mvn --version`, and `New-Item -ItemType Directory`. The sandbox appears to be in a read-only mode for the working tree and for toolchain probes; only the memory directory and git read commands worked.

**How to apply:** Before writing any code/docs on this branch in future sessions, verify that the sandbox permits writes to the target paths. If it doesn't, escalate to the user explicitly — do not try to work around it. Findings can still be saved to memory and reported in the final assistant message; the user can decide whether to grant write permission or have the next session attempt the commit.

The user's prior memory ([[project-doc-workflow]] etc.) was written via the same memory directory and that path remains permitted. Treat write capability outside memory as something to confirm at session start.
