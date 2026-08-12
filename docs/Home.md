# Blast Radius — Project Hub

> Dependency-triage tool: filters advisory noise and shows which CVEs/version changes actually touch code you call — with migration drafts for your call sites.

## Quick links

- [[PROGRESS]] — **session handoff for new chats (read first)**
- [[Task Dependency Map]] — 🗺️ **what blocks what** across all remaining phases
- [[Dashboard]] — daily todos and status
- [[START HERE — Obsidian setup]] — if Obsidian looks empty or wrong
- [[Local PostgreSQL setup]] — connect without Docker
- [[Headroom + Cursor setup]] — LLM context compression in Cursor
- [[Roadmap]] — all phases, dependencies, timeline
- [[Architecture]] — system diagram and the `EcosystemAdapter` seam
- [[Data Model]] — PostgreSQL tables and relationships
- [[Git Workflow]] — branches, commits, PRs
- [[Phase 1 — Persistence]] · [[Phase 2 — Ingestion]] · [[Phase 3 — Advisories]] ← next

## Status

| Phase               | Status         | Branch                        |
| ------------------- | -------------- | ----------------------------- |
| 0 — Skeleton        | ✅ Done         | `main`                        |
| 1 — Persistence     | ✅ Done         | merged (PR #2)                |
| 2 — Maven ingestion | ✅ Done         | merged (PR #3)                |
| 3 — Advisories      | 🔜 Next         | `feat/phase3-advisories`      |
| 4 — Usage detection | ⏳ Unblocked    | `feat/phase4-usage-maven`     |
| 5 — Agents          | ⏳              | `feat/phase5-agents`          |
| 6 — Dashboard       | ⏳              | `feat/phase6-frontend`        |
| 7 — Deploy          | ⏳              | `feat/phase7-deploy`          |
| 8 — Python adapter  | ⏳              | `feat/phase8-python-adapter`  |

## Repo

- GitHub: https://github.com/kaustubhchaudhari11/BlastRadius
- Local: `C:\Users\kaust\projects\blastradius`
- Health check: http://localhost:8080/health

## One-liner (interview)

Import-level reachability triage + agentic migration layer, built behind an ecosystem-adapter abstraction so a new language is ~a day of work.



