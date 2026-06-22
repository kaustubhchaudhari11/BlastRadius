# Dashboard

> Your daily command center. Pin this note in Obsidian (right-click → **Pin**).

## Today

- [ ] Create `application-local.properties` (gitignored) with PG18 password
- [ ] Run `scripts/setup-postgres.ps1` → DB `blastradius`
- [ ] App boots + Flyway creates 5 tables

## Project status

| Phase | Status | Notes |
|-------|--------|-------|
| [[Roadmap#Phase 0 — Skeleton\|0 Skeleton]] | ✅ Done | `/health` works |
| [[Phase 1 — Persistence\|1 Persistence]] | 🔄 Active | Needs Postgres to boot |
| 2 Maven ingestion | ⏳ | After P1 merges |
| 3 Advisories | ⏳ | After P2 |
| 4 Usage scan | ⏳ | After P2 |
| 5 Agents | ⏳ | After P3 + P4 |
| 6 Dashboard | ⏳ | After P5 |
| 7 Deploy | ⏳ | After P6 |
| 8 Python | ⏳ | After v0.1.0 |

## Phase 1 checklist

See [[Phase 1 — Persistence]] for details.

- [x] JPA + Flyway deps in pom.xml
- [x] 5 entities + repositories
- [x] Flyway V1 migration
- [ ] Postgres running (Docker **or** local install)
- [ ] App boots against Postgres
- [ ] Tables visible in IntelliJ DB tool
- [ ] PR merged to `main`

## Quick links

- [[Home]]
- [[Roadmap]]
- [[Architecture]]
- [[Data Model]]
- [[Git Workflow]]

## Session log

<!-- Add a line each time you work on the project -->

- _Example: 2026-06-17 — Phase 1 code done, need Postgres checkpoint_
