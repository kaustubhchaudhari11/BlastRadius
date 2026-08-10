# Blast Radius — Session Handoff (read this first in new chats)

> **Last updated:** 2026-08-09 · **Branch:** `feat/phase2-maven-ingestion` · **Phase:** 2 ✅ CODE COMPLETE (incl. P2.5), re-verified 14/14 tests green — **still awaiting merge** · Phase 1 ✅ merged (PR #2) · **Next:** merge P2, then Phase 3 (advisories)

> [!warning] Single blocker
> Phase 2 has been code-complete and pushed since 2026-06-24 but **was never merged**. `main` is still at Phase 1 (PR #2), 11 commits behind. Nothing else can proceed cleanly until this merges. No Phase 3 code exists yet.

## One-liner
Dependency triage: filter CVE/advisory noise → show what affects **your** code → draft migrations. Java/Maven first, Python later. Spring Boot 3 + Postgres + Flyway + React (later).

## Done ✅
- Phase 0: Spring Boot skeleton, `/health`
- Phase 1: JPA entities, repos, Flyway V1, secure config — **merged to `main`** (PR #2)
- PostgreSQL **18** installed, service running; DB `blastradius` created; app boots, 5 tables + `flyway_schema_history`
- Headroom MCP in `.cursor/mcp.json`
- Obsidian vault: `docs/` · GitHub: https://github.com/kaustubhchaudhari11/BlastRadius

## Phase 2 — ✅ CODE COMPLETE (branch `feat/phase2-maven-ingestion`, 14 tests green)
Goal: ingest Maven dependencies behind an `EcosystemAdapter` seam → fill `dependency` table.
Design: adapter returns `ParsedDependency` record (no JPA); `IngestionService` maps to entity + persists.
| Step | Component | Status |
|------|-----------|--------|
| 2.1 | `EcosystemAdapter` interface + `ParsedDependency` | ✅ |
| 2.2 | `MavenAdapter` (detect + parse; scanUsage stub) | ✅ |
| 2.3 | `AdapterRegistry` | ✅ |
| 2.4 | `IngestionService` (parse → persist) | ✅ |
| 2.5 | `POST /api/projects` + DTOs | ✅ |
| 2.6 | Unit test + fixture pom.xml | ✅ |
| **P2.5** | **Version resolution** (`${prop}` + parent + `dependencyManagement`) → `MavenVersionResolver` | ✅ |
| +IT | `ProjectControllerIntegrationTest` (POST→DB seam in CI) | ✅ |
Packages: `com.blastradius.ingestion`, `com.blastradius.api`. pom parsing via `org.apache.maven:maven-model`.

**Why P2.5 matters:** P3 filters advisories by whether *your* current version is in the vulnerable range. Without a concrete version (`"unspecified"`), every advisory for a package matches → false-positive noise — the exact thing the product removes. `MavenVersionResolver` resolves `${properties}`, on-disk parent POMs, and `<dependencyManagement>`. **Known boundary:** versions that live only in a *remote* BOM (e.g. `spring-boot-starter-parent`) are not downloaded → still `"unspecified"`; P3 must treat `"unspecified"` as "version-unknown, surface for manual review" rather than auto-matching all ranges.

## YOU do now 🔴
1. **Merge Phase 2** — open `https://github.com/kaustubhchaudhari11/BlastRadius/compare/main...feat/phase2-maven-ingestion` → Create PR → Merge. Fast-forward, **no conflicts expected** (`main` is an ancestor).
2. `git checkout main; git pull origin main; git checkout -b feat/phase3-advisories`
3. Delete stale merged branches: `git branch -d feat/phase1-persistence feat/phase2-maven-ingestion`
4. **Decide** on `origin/copilot/dependency-check-solution` (auto-created; adds Dependabot + dependency-review Action, touches only `.github/` + README) → merge as its own small PR, or delete.
5. Then start Phase 3 step 3.1 (`AdvisoryClient` → OSV).

### Housekeeping notes
- `git status` shows many files "modified" — these are **line-ending (CRLF) artifacts only**; `git diff` is empty for all but one stray blank line in `UsageSiteRepository.java`. Harmless; discard with `git checkout -- <file>` if it bothers you.
- Port 8080 was once blocked by `PEMHTTPD-x64` (Apache from the PG install) — stop that service if it returns.

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
P2 (done) → P3 advisories ─┐
         └→ P4 usage scan ─┴→ P5 agents → P6 UI → P7 deploy → P8 Python
```
P3 ∥ P4 after P2. Details: [[Roadmap]]

## Phase 2 → Phase 3 contract (what P3 consumes from P2)
| P2 produced | P3 uses it to… |
|-------------|----------------|
| `dependency` rows: `(ecosystem, group_or_pkg, artifact_or_name, current_version)` | Match advisories by `(ecosystem, pkg)`; **filter by `current_version` ∈ vulnerable range** |
| `MavenVersionResolver` → concrete versions | Make range filtering meaningful (the P2.5 payoff) |
| `EcosystemAdapter.ecosystemId()` = `"maven"` | Map to OSV ecosystem name `"Maven"` when querying |
| `"unspecified"` sentinel | Branch: cannot range-filter → mark finding `version-unknown` (manual review), don't auto-match all |

## Phase 3 — Advisory ingestion (next, branch `feat/phase3-advisories`)
Goal: pull advisories (OSV.dev) for the packages we already store → fill `advisory` table. Enables findings (P5).
| Step | Task |
|------|------|
| 3.1 | `AdvisoryClient` (OSV `POST /v1/querybatch` or `/v1/query`) — input: distinct `(ecosystem, pkg, version)` from `dependency` |
| 3.2 | Map OSV response → `Advisory` entity (`external_id` UNIQUE = OSV id; `affected_versions` = ranges JSON; `severity`) |
| 3.3 | `AdvisoryService.refreshForProject(projectId)` — query OSV per dependency, upsert advisories (dedupe on `external_id`) |
| 3.4 | Version-range check helper: is `current_version` inside an OSV affected range? (`"unspecified"` → skip/flag) |
| 3.5 | `POST /api/projects/{id}/advisories/refresh` → returns advisory count |
| 3.6 | Unit test: OSV JSON fixture → advisories parsed; range check true/false cases |
Checkpoint: refresh a real project → `advisory` rows land; a dep with a known CVE matches by range.

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
- 2026-06-24: Phase 2 code complete (2.1–2.6). Added **P2.5 `MavenVersionResolver`** (resolves `${props}`, on-disk parent, `dependencyManagement`) + **POST→DB integration test** + blank-input guard. 14 tests green. Confirmed the "extra" 6th table is `flyway_schema_history` (expected, not a bug). Ready to merge → Phase 3.
- 2026-07-02: Added Obsidian pages [[Phase 2 — Ingestion]] and [[Phase 3 — Advisories]]; refreshed [[Home]] + [[Dashboard]]. Pushed. Merge still pending.
- 2026-08-09: Resumed after ~5 weeks. **Audited repo:** Phase 2 still unmerged (11 commits ahead of `main`); no Phase 3 code started; only `V1__init.sql` migration. **Re-ran suite → 14/14 green.** Phase 2 confirmed still merge-ready. Working-tree "modifications" are CRLF noise only.
