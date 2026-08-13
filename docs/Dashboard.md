# Dashboard

> Your daily command center. Pin this note in Obsidian (right-click → **Pin**).

## Today

- [x] ~~Merge Phase 2 → `main`~~ ✅ (PR #3)
- [x] ~~Sync all branches; `main` fully up to date~~ ✅
- [x] ~~Phase 3 code: 3.0–3.6, 41/41 tests green~~ ✅
- [ ] **Phase 3 checkpoint** — run app → POST project → POST advisories/refresh → check `advisory` rows
- [ ] PR `feat/phase3-advisories` → `main`
- [ ] Decide: keep or delete `copilot/dependency-check-solution` branch
- [ ] Then Phase 4 — `MavenAdapter.scanUsage()`

## Project status

| Phase | Status | Notes |
|-------|--------|-------|
| [[Roadmap#Phase 0 — Skeleton\|0 Skeleton]] | ✅ Done | `/health` works |
| [[Phase 1 — Persistence\|1 Persistence]] | ✅ Done | merged (PR #2) |
| [[Phase 2 — Ingestion\|2 Maven ingestion]] | ✅ Done | **merged PR #3**; 14/14 tests |
| [[Phase 3 — Advisories\|3 Advisories]] | 🔄 Code done | 41/41 tests; needs manual checkpoint + PR |
| 4 Usage scan | 🔜 Next | `scanUsage()` slot already on the interface |
| 5 Agents | ⏳ | After P3 + P4 |
| 6 Dashboard | ⏳ | After P5 |
| 7 Deploy | ⏳ | After P6 |
| 8 Python | ⏳ | After v0.1.0 |

## Phase 3 checklist

See [[Phase 3 — Advisories]] for the full plan and [[Task Dependency Map]] for what blocks what.

- [x] 3.0 `OsvClientConfig` — `RestClient` + timeouts
- [x] 3.1 `AdvisoryClient` → OSV `/v1/query`
- [x] 3.2 `AdvisoryMapper` — OSV JSON → `Advisory`
- [x] 3.3 `AdvisoryService.refreshForProject()` upsert + error isolation
- [x] 3.4 `VersionRangeMatcher` — tri-state, Maven version ordering
- [x] 3.5 `POST /api/projects/{id}/advisories/refresh`
- [x] 3.6 Unit tests — 27 new, 41/41 total
- [ ] Checkpoint: real project → advisories land, known CVE matches

## Quick links

- [[Home]]
- [[Roadmap]]
- [[Architecture]]
- [[Data Model]]
- [[Git Workflow]]

## Session log

<!-- Add a line each time you work on the project -->

- 2026-06-24 — Phase 2 code complete (2.1–2.6 + P2.5 version resolution + POST→DB IT), 14/14 tests, pushed
- 2026-07-02 — Resumed after a week; Phase 2 confirmed ready to merge; Obsidian updated for P2 done + P3 plan
- 2026-08-09 — Repo audit: P2 still unmerged, suite re-verified 14/14 green
- 2026-08-11 — **Phase 2 merged (PR #3)**; added [[Task Dependency Map]]; Phase 3 broken down with blocking order
- 2026-08-12 — All branches synced to `main`; **Phase 3 implemented (3.0–3.6)**, suite 14 → 41 tests green
