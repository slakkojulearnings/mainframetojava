---
name: project-feature-nova-branch
description: Fresh feature/nova branch (off main cf9d9d3) starting clean COBOL→Java port; experimentation-branch work is NOT being carried over
metadata:
  type: project
---

The user opened a fresh branch `feature/nova` off `main` (cf9d9d3) on 2026-05-24 to redo the CardDemo COBOL→Java modernization from scratch with stronger evidence discipline. The 42 docs and CBACT04C Java port that exist on `experimentation` are intentionally NOT being carried over.

**Why:** prior session on `experimentation` was tagged as not rigorous enough; the user wants every claim grounded in actual byte evidence, every architectural choice justified against the real source.

**How to apply:** When working in this branch, never reference or import work from `experimentation`. Re-verify findings from scratch. **CORRECTION (2026-05-24):** the user clarified that `feature/nova` here belongs to someone else and they have NO push permission to its remote. The deliverable does NOT land on this branch's remote. Output goes to `F:\mainframetojava` (the user's own repo, separate working tree) — see [[project-push-target]].

Related: [[project-modernization-progress]] (the older experimentation snapshot — DO NOT use as input for `feature/nova` work).
