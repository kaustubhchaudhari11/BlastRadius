# Dashboard

> Your daily command center. Pin this note in Obsidian (right-click → **Pin**).

## Today

- [x] ~~Merge Phase 2 PR → `main`~~ ✅ **done (PR #3)**
- [ ] PR the 2 stranded doc commits + [[Task Dependency Map]] → `main`
- [ ] `git checkout main && git pull` → cut `feat/phase3-advisories`
- [ ] Kick off [[Phase 3 — Advisories]]: **3.0** HTTP client → **3.1** `AdvisoryClient`
- [ ] Start **3.4** range comparator in parallel (longest pole)
- [ ] Decide: keep or delete `copilot/dependency-check-solution` branch

## Project status

| Phase | Status | Notes |
|-------|--------|-------|
| [[Roadmap#Phase 0 — Skeleton\|0 Skeleton]] | ✅ Done | `/health` works |
| [[Phase 1 — Persistence\|1 Persistence]] | ✅ Done | merged (PR #2) |
| [[Phase 2 — Ingestion\|2 Maven ingestion]] | ✅ Done | **merged PR #3**; 14/14 tests |
| [[Phase 3 — Advisories\|3 Advisories]] | 🔜 Next | OSV ingestion + range filter |
| 4 Usage scan | ⏳ Ready | Unblocked — parallel with P3; good fallback task |
| 5 Agents | ⏳ | After P3 + P4 |
| 6 Dashboard | ⏳ | After P5 |
| 7 Deploy | ⏳ | After P6 |
| 8 Python | ⏳ | After v0.1.0 |

## Phase 3 checklist

See [[Phase 3 — Advisories]] for the full plan and [[Task Dependency Map]] for what blocks what.

- [ ] 3.0 HTTP client bean + timeouts 🔒
- [ ] 3.1 `AdvisoryClient` → OSV `/v1/querybatch`
- [ ] 3.2 Map OSV JSON → `Advisory` (dedupe on `external_id`)
- [ ] 3.3 `AdvisoryService.refreshForProject()` upsert
- [ ] 3.4 Version-range check (`"unspecified"` → flag)
- [ ] 3.5 `POST /api/projects/{id}/advisories/refresh`
- [ ] 3.6 Unit tests (OSV fixture + range cases)
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
