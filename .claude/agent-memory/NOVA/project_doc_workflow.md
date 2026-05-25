---
name: project-doc-workflow
description: The catalog.txt / relationships.txt index workflow and the two helper agents the project uses for COBOL doc + diagram generation
metadata:
  type: project
---

This project has a deliberate analysis workflow built around two index files and two specialist subagents.

**The indexes (docs/memories/):**
- `catalog.txt` — `TYPE|ID|READABLE_NAME|CATEGORY|DOC_PATH` registry. Grep here before re-analyzing.
- `relationships.txt` — `RELATION_TYPE|FROM|TO|NOTES` dependency graph (USES_COPYBOOK, READS, UPDATES, WRITES, CALLS_PROGRAM, RUNS_BEFORE/AFTER, PASSES_COMMAREA, RECEIVES_FROM, DEFINES_STRUCTURE). Use for impact analysis.

**The agents (.claude/agents/):**
- `cobol-documentation-translator` (Sonnet) — translates .cbl / .cpy / .jcl into English docs and updates the indexes. Needs a to-do list, guidelines, and a memory dir.
- `diagram-generator` (Sonnet) — produces Mermaid diagrams (capability map, data flow, ER, impact analysis, batch sequence, program deps) reading from the indexes.

**Why:** The CLAUDE.md workflow says: before analyzing X, grep `catalog.txt`; during analysis, append to `relationships.txt`; after analysis, register in `catalog.txt`. This avoids re-doing work and keeps an authoritative dependency graph the diagram tool can consume.

**How to apply:**
- When the user asks to document or translate the "next" thing, propose batches that share copybooks (cheaper context reuse) and that close gaps in the relationships graph the diagram-generator will need.
- Use the existing diagram (`docs/diagrams/daily-batch-processing-capability-map.md`) as the model for new diagrams.
- Don't write COBOL-specific architectural facts into agent memory — they belong in docs/ and the indexes. Memory is for user/project/feedback context that wouldn't survive in those files.
