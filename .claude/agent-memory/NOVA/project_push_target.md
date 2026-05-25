---
name: project-push-target
description: Final NOVA deliverable lands in F:\mainframetojava and pushes to the user's GitHub; F:\aws-mainframe-modernization-carddemo `feature/nova` is read-only (not user's branch)
metadata:
  type: project
---

User clarified on 2026-05-24:

- **`F:\aws-mainframe-modernization-carddemo`** is the source-of-truth COBOL repo. The `feature/nova` branch here belongs to someone else and the user does NOT have push permission to its remote. Treat this directory as **read-only** for the COBOL source we are analyzing. Do not push commits from here to any remote.
- **`F:\mainframetojava`** is the user's own working copy (git repo, has origin remote pointing at their GitHub account). The NOVA modernization output — documentation, Java code, verification harness, README — must be created in `F:\mainframetojava`, committed there, and pushed to their GitHub from there.

**Why:** Two distinct repos serve two distinct roles. The CardDemo repo is the input artifact (don't mutate). The mainframetojava repo is the output artifact (where everything we build lives).

**How to apply:**
- Read COBOL/JCL/copybook sources from `F:\aws-mainframe-modernization-carddemo\app\...`.
- Write all docs/Java/build files to `F:\mainframetojava\...`.
- Commits go in `F:\mainframetojava` only.
- `git push` happens from `F:\mainframetojava` (to user's GitHub) only after explicit user confirmation.
- Never push or attempt to push from `F:\aws-mainframe-modernization-carddemo`.

**Current sandbox status (2026-05-24 session):** Reads and writes inside `F:\mainframetojava` are sandbox-blocked in this session, same as writes to the CardDemo repo tree. Both must be unblocked before the next session can land Commit 1.

Related: [[project-feature-nova-branch]] (originally said push to slakkojulearnings/mainframetojava — this memory clarifies it's actually a *local* path the user controls, not a fork of the upstream branch).
