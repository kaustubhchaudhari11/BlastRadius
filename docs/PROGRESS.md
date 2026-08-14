# Blast Radius — Session Handoff (read this first in new chats)

> **Last updated:** 2026-08-14 · **Phase:** 3 🔄 **code + CI complete** on `feat/phase3-advisories` · Phases 0–2 ✅ merged · Tests: **55/55 green** · Remaining: live-OSV checkpoint re-run + PR

> [!tip] Start here for planning
> **[[Task Dependency Map]]** — every remaining task, what blocks what, cross-functional contracts, and deferred debt with deadlines.

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

**Why P2.5 matters:** P3 filters advisories by whether *your* current version is in the vulnerable range. Without a concrete version (`"unspecified"`), every advisory for a package matches → false-positive noise — the exact thing the product removes. `MavenVersionResolver` resolves `${properties}`, parent POMs (**on disk *and* remote**), `<dependencyManagement>`, and **imported BOMs** (`<scope>import</scope>`). Anything still unresolved stays `"unspecified"` and is surfaced as `needs_review` rather than auto-matched.

## YOU do now 🔴
1. **Re-run the Phase 3 checkpoint** — see "Phase 3 checkpoint" below. The first run (2026-08-14) exposed the remote-BOM gap, which is now fixed; the re-run should show non-zero `affected`/`notAffected` instead of all-`unknown`.
2. PR `feat/phase3-advisories` → `main` once the checkpoint passes.
3. **Decide** on `origin/copilot/dependency-check-solution` (auto-created; adds Dependabot + dependency-review Action, touches only `.github/` + README) → merge as its own small PR, or delete.
4. Optional cleanup: `git push origin --delete feat/phase1-persistence` (stale; recovery SHA `b6b6bea`).

## Phase 3 checkpoint (manual — needs live OSV + Postgres)
> Port 8080 is occupied by `httpd` (`PEMHTTPD-x64`, from the PG/pgAdmin install), so run on **8081**. Stopping that service needs admin; using another port avoids the whole problem.
```powershell
# 1. start the app on 8081
$env:JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot"
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--server.port=8081"

# 2. register a project (any Maven repo path) — note the id it returns
$body = '{"name":"self","sourcePath":"C:/Users/kaust/projects/blastradius"}'
$p = (Invoke-WebRequest http://localhost:8081/api/projects -Method POST -Body $body `
        -ContentType "application/json" -UseBasicParsing).Content | ConvertFrom-Json
$p.dependencies | ForEach-Object { "$($_.artifactOrName) = $($_.currentVersion)" }

# 3. refresh advisories for it
(Invoke-WebRequest "http://localhost:8081/api/projects/$($p.id)/advisories/refresh" `
        -Method POST -UseBasicParsing).Content
```
**Pass criteria:** step 2 shows real versions (not a wall of `unspecified`), and step 3 returns non-zero `findingsCreated` with `affected` + `notAffected` > 0. Then confirm `advisory` and `finding` rows in Postgres.

### Housekeeping notes
- `git status` shows many files "modified" — these are **line-ending (CRLF) artifacts only**; `git diff` is empty for them. Harmless; discard with `git checkout -- <file>` if it bothers you.
- Advisory refresh hits the network twice: OSV for advisories, and `repo1.maven.org` for remote parent/BOM POMs. Both degrade gracefully when offline.

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

## Phase 3 — Advisory ingestion 🔄 (branch `feat/phase3-advisories`)
Goal: pull advisories (OSV.dev) for the packages we already store → fill `advisory` table. Enables findings (P5).
Full breakdown + blocking graph: **[[Task Dependency Map]]**
| Step | Task | Status |
|------|------|--------|
| 3.0 | `OsvProperties` + `OsvClientConfig` — dedicated `RestClient`, explicit connect/read timeouts | ✅ |
| 3.1 | `AdvisoryClient` → OSV `POST /v1/query`; ecosystem name mapping (`maven`→`Maven`) | ✅ |
| 3.2 | `AdvisoryMapper` — OSV vuln → `Advisory`; raw ranges kept as JSON; CVSS severity preferred | ✅ |
| 3.3 | `AdvisoryService.refreshForProject()` — dedupe on `external_id`, per-package error isolation | ✅ |
| 3.4 | `VersionRangeMatcher` + `VersionMatch` — tri-state, Maven `ComparableVersion` ordering | ✅ |
| 3.5 | `POST /api/projects/{id}/advisories/refresh` → `AdvisoryRefreshResult` | ✅ |
| 3.6 | Tests — 15 range, 5 client, 12 service, 3 integration | ✅ |
| **3.7** | **Remote version resolution** — `PomFetcher` + `MavenCentralPomFetcher`; remote parents, imported BOMs, per-JVM cache | ✅ |
| **3.8** | **Findings persistence** — verdict written to `finding` with `TriageStatus`; V2 unique `(dependency_id, advisory_id)` | ✅ |
| +IT | `AdvisoryRefreshIntegrationTest` — POST project → refresh → DB rows, all 3 verdicts, findings, idempotency (OSV stubbed) | ✅ |
| — | **Live-OSV checkpoint** re-run after 3.7/3.8 | ⏳ only remaining item |
| — | PR `feat/phase3-advisories` → `main` | ⏳ |

**Total: 55/55 green.**

### The 2026-08-14 checkpoint finding (why 3.7 and 3.8 exist)
The first live run passed half the checkpoint and failed the half that matters:
- ✅ 17 advisory rows landed from real OSV data, no errors.
- ❌ **All 17 were `unknown`** — 9 of 11 dependencies resolved to `"unspecified"` because this project inherits versions from `spring-boot-starter-parent`, a *remote* parent. Range filtering had nothing to compare against, so the noise filter did nothing.
- ❌ The verdict was **computed and discarded** — nothing wrote the dependency↔advisory link, so "which advisory affects what" was not queryable.

This was the load-bearing shortcut flagged during the P2 review, confirmed in practice. 3.7 fixes the input data; 3.8 fixes the output. Verified against real coordinates: `spring-boot-starter-parent:3.4.5` carries **0** managed entries and defers to `spring-boot-dependencies:3.4.5`, which has **412** — with `postgresql` as `${postgresql.version}` → `42.7.5`, i.e. the parent chain *and* a property indirection both have to be walked.

### Design decisions worth remembering
- **Remote POMs go through a `PomFetcher` seam.** Keeps resolution unit-testable offline and lets an air-gapped deploy swap in a mirror. Negative results are cached too, so an offline run doesn't retry every lookup.
- **BOM properties don't leak.** An imported BOM's `${properties}` resolve *its* managed versions, then only concrete versions are merged — matching Maven, which does not inherit BOM properties into the importing project.
- **Precedence is local > imported BOM > parent.** The pre-3.7 code used `putIfAbsent` while walking parents first, which silently let a parent's `dependencyManagement` win over the child's. Fixed.
- **Findings never overwrite a human decision.** A refresh skips any finding already marked `dismissed`.
- **`/v1/query`, not `/v1/querybatch`.** querybatch returns only vulnerability *ids*, so it would still need a detail fetch per id. Since we need summary/severity/ranges to build an `Advisory`, one `/v1/query` per package is the same round-trip count with simpler code.
- **Maven's own `ComparableVersion`** (from `maven-artifact`) does version ordering, not semver parsing — `2.0.0.RELEASE` and `1.0-RC1` must order the way Maven does.
- **Tri-state, not boolean.** `UNKNOWN` is what keeps the tool honest; `"unspecified"` deps query OSV *without* a version so we still learn what advisories exist, then flag them for manual review.
- **Raw OSV ranges stored as JSON** in `affected_versions` — OSV's schema is still evolving; keeping it verbatim avoids a lossy migration per field.
- **Partial-failure tolerant** — one package failing at OSV is collected into `errors[]` instead of aborting the refresh.

## Key paths
| What | Where |
|------|-------|
| Main class | `src/main/java/com/blastradius/blastradius/BlastradiusApplication.java` |
| Entities | `com.blastradius.model.*` |
| Flyway | `src/main/resources/db/migration/` (`V1__init.sql`, `V2__finding_unique_dependency_advisory.sql`) |
| Config | `application.properties` + `application-local.properties` |
| Obsidian start | `docs/Dashboard.md` |
| Cursor context rule | `.cursor/rules/blastradius-handoff.mdc` |

## Session log
- 2026-06-22: PG18 installed; secure config pushed; awaiting first successful Flyway boot + P1 merge
- 2026-06-23: Phase 1 checkpoint PASSED — app live, Flyway created 5 tables, /health OK. Next: merge PR → Phase 2.
- 2026-06-24: Phase 2 code complete (2.1–2.6). Added **P2.5 `MavenVersionResolver`** (resolves `${props}`, on-disk parent, `dependencyManagement`) + **POST→DB integration test** + blank-input guard. 14 tests green. Confirmed the "extra" 6th table is `flyway_schema_history` (expected, not a bug). Ready to merge → Phase 3.
- 2026-07-02: Added Obsidian pages [[Phase 2 — Ingestion]] and [[Phase 3 — Advisories]]; refreshed [[Home]] + [[Dashboard]]. Pushed. Merge still pending.
- 2026-08-09: Resumed after ~5 weeks. **Audited repo:** Phase 2 still unmerged (11 commits ahead of `main`); no Phase 3 code started; only `V1__init.sql` migration. **Re-ran suite → 14/14 green.** Phase 2 confirmed still merge-ready. Working-tree "modifications" are CRLF noise only.
- 2026-08-12: **Phase 3 code complete (3.0–3.6)** on `feat/phase3-advisories` — `AdvisoryClient`, `AdvisoryMapper`, `AdvisoryService`, `VersionRangeMatcher`, refresh endpoint. Suite grew **14 → 41 tests**, all green. Also synced all branches: `main` now holds every doc + code commit; deleted merged `feat/phase2-maven-ingestion`; fixed the recurring CRLF "phantom modified files" via `git add --renormalize`.
- 2026-08-14: **Ran the live checkpoint — it failed the important half, and that was valuable.** Advisory rows landed from real OSV, but all 17 came back `unknown` because remote-BOM versions were unresolved, and verdicts were never persisted. Added **3.7 remote version resolution** (`PomFetcher` + `MavenCentralPomFetcher`: remote parents, imported BOMs, caching, graceful offline) and **3.8 findings persistence** (`TriageStatus`, V2 unique constraint, dismissal-safe upsert). Also fixed a real precedence bug where a parent's `dependencyManagement` beat the child's. Suite **44 → 55 green**. Checkpoint needs a re-run.
- 2026-08-11: **Phase 2 MERGED (PR #3)** 🎉 — `main` @ `7f74821`. Found 2 doc commits stranded off the merge (fold into next PR). Added **[[Task Dependency Map]]** with the full P3–P8 blocking graph, cross-functional contracts, and deferred debt deadlines. Added task **3.0** (HTTP client) as a newly-identified prerequisite; flagged **3.4** range comparator as the critical long pole and marked it parallel-startable.
