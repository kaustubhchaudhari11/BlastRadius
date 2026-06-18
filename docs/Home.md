# Blast Radius — Project Hub

> Dependency-triage tool: filters advisory noise and shows which CVEs/version changes actually touch code you call — with migration drafts for your call sites.

## Quick links

- [[Roadmap]] — all phases, dependencies, timeline
- [[Architecture]] — system diagram and the `EcosystemAdapter` seam
- [[Data Model]] — PostgreSQL tables and relationships
- [[Git Workflow]] — branches, commits, PRs
- [[Phase 1 — Persistence]] — current work

## Status

| Phase | Status | Branch |
|-------|--------|--------|
| 0 — Skeleton | ✅ Done | `main` |
| 1 — Persistence | 🔄 In progress | `feat/phase1-persistence` |
| 2 — Maven ingestion | ⏳ | `feat/phase2-maven-ingestion` |
| 3 — Advisories | ⏳ | `feat/phase3-advisories` |
| 4 — Usage detection | ⏳ | `feat/phase4-usage-maven` |
| 5 — Agents | ⏳ | `feat/phase5-agents` |
| 6 — Dashboard | ⏳ | `feat/phase6-frontend` |
| 7 — Deploy | ⏳ | `feat/phase7-deploy` |
| 8 — Python adapter | ⏳ | `feat/phase8-python-adapter` |

## Repo

- GitHub: https://github.com/kaustubhchaudhari11/BlastRadius
- Local: `C:\Users\kaust\projects\blastradius`
- Health check: http://localhost:8080/health

## One-liner (interview)

Import-level reachability triage + agentic migration layer, built behind an ecosystem-adapter abstraction so a new language is ~a day of work.
