# Blast Radius — Session Handoff (read this first in new chats)

> **Last updated:** 2026-06-23 · **Branch:** `feat/phase2-maven-ingestion` · **Phase:** 2 IN PROGRESS · Phase 1 ✅ merged to `main` (PR #2)

## One-liner
Dependency triage: filter CVE/advisory noise → show what affects **your** code → draft migrations. Java/Maven first, Python later. Spring Boot 3 + Postgres + Flyway + React (later).

## Done ✅
- Phase 0: Spring Boot skeleton, `/health`
- Phase 1: JPA entities, repos, Flyway V1, secure config — **merged to `main`** (PR #2)
- PostgreSQL **18** installed, service running; DB `blastradius` created; app boots, 5 tables
- Headroom MCP in `.cursor/mcp.json`
- Obsidian vault: `docs/` · GitHub: https://github.com/kaustubhchaudhari11/BlastRadius

## Phase 2 — IN PROGRESS (branch `feat/phase2-maven-ingestion`)
Goal: ingest Maven dependencies behind an `EcosystemAdapter` seam → fill `dependency` table.
Design: adapter returns `ParsedDependency` record (no JPA); `IngestionService` maps to entity + persists.
| Step | Component | Status |
|------|-----------|--------|
| 2.1 | `EcosystemAdapter` interface + `ParsedDependency` | pending |
| 2.2 | `MavenAdapter` (detect + parse; scanUsage stub) | pending |
| 2.3 | `AdapterRegistry` | pending |
| 2.4 | `IngestionService` (parse → persist) | pending |
| 2.5 | `POST /api/projects` + DTOs | pending |
| 2.6 | Unit test + fixture pom.xml | pending |
Packages: `com.blastradius.ingestion`, `com.blastradius.api`. pom parsing via `org.apache.maven:maven-model`.

## YOU do now 🔴
1. Merge PR `feat/phase1-persistence` → `main` (https://github.com/kaustubhchaudhari11/BlastRadius)
2. `git checkout main && git pull` → start Phase 2 branch
3. Note: port 8080 was blocked by `PEMHTTPD-x64` (Apache from PG install) — stop that service if it returns

## Phase 1 setup (DONE — reference)
- DB created: `$env:PGPASSWORD='...'; .\scripts\setup-postgres.ps1`
- Secret in `application-local.properties` (gitignored)
- App boots, Flyway applied V1, 5 tables + flyway_schema_history exist

## Run commands
```powershell
cd C:\Users\kaust\projects\blastradius
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot"
.\mvnw.cmd spring-boot:run
# OR IntelliJ: run BlastradiusApplication (not HelloController)
```
Verify: http://localhost:8080/health

## Secrets — never commit
- `application-local.properties`, `.env` → gitignored
- Use env: `SPRING_DATASOURCE_PASSWORD`

## Next phases (dependency order)
```
P1 checkpoint → P2 Maven adapter → P3 advisories ─┐
                         └→ P4 usage scan ────────┴→ P5 agents → P6 UI → P7 deploy → P8 Python
```
P3 ∥ P4 after P2. Details: [[Roadmap]]

## Phase 2 micro-steps (after P1 merge)
| Step | Task |
|------|------|
| 2.1 | `EcosystemAdapter` interface |
| 2.2 | `MavenAdapter.parseDependencies()` |
| 2.3 | `AdapterRegistry` |
| 2.4 | `IngestionService` + persist |
| 2.5 | `POST /api/projects` |
| 2.6 | Unit test fixture pom |

Branch: `feat/phase2-maven-ingestion`

## Key paths
| What | Where |
|------|-------|
| Main class | `src/main/java/com/blastradius/blastradius/BlastradiusApplication.java` |
| Entities | `com.blastradius.model.*` |
| Flyway | `src/main/resources/db/migration/V1__init.sql` |
| Config | `application.properties` + `application-local.properties` |
| Obsidian start | `docs/Dashboard.md` |
| Cursor context rule | `.cursor/rules/blastradius-handoff.mdc` |

## Session log
- 2026-06-22: PG18 installed; secure config pushed; awaiting first successful Flyway boot + P1 merge
- 2026-06-23: Phase 1 checkpoint PASSED — app live, Flyway created 5 tables, /health OK. Next: merge PR → Phase 2.
