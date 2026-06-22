# Blast Radius — Session Handoff (read this first in new chats)

> **Last updated:** 2026-06-22 · **Branch:** `feat/phase1-persistence` · **Phase:** 1 (checkpoint pending)

## One-liner
Dependency triage: filter CVE/advisory noise → show what affects **your** code → draft migrations. Java/Maven first, Python later. Spring Boot 3 + Postgres + Flyway + React (later).

## Done ✅
- Phase 0: Spring Boot skeleton, `/health`
- Phase 1 code: JPA entities, repos, Flyway V1, secure config (no password in git)
- PostgreSQL **18** installed, service running
- Headroom MCP in `.cursor/mcp.json`
- Obsidian vault: `docs/` · GitHub: https://github.com/kaustubhchaudhari11/BlastRadius
- Pushed: `1db9bf5` on `feat/phase1-persistence`

## Blocked / YOU do now 🔴
1. Create DB (once): `$env:PGPASSWORD='YOUR_PW'; .\scripts\setup-postgres.ps1`
2. Local secrets (gitignored):
   ```powershell
   copy src\main\resources\application-local.properties.example src\main\resources\application-local.properties
   # edit → spring.datasource.password=YOUR_PW
   ```
3. Run app → Flyway creates 5 tables
4. Merge PR → `main` → start Phase 2

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
