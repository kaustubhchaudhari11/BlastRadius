# Dashboard

> Your daily command center. Pin this note in Obsidian (right-click → **Pin**).

## Today

- [ ] Merge Phase 2 PR → `main` (compare: `main...feat/phase2-maven-ingestion`)
- [ ] `git checkout main && git pull` → cut `feat/phase3-advisories`
- [ ] Kick off [[Phase 3 — Advisories]] step 3.1 (`AdvisoryClient` → OSV)

## Project status

| Phase | Status | Notes |
|-------|--------|-------|
| [[Roadmap#Phase 0 — Skeleton\|0 Skeleton]] | ✅ Done | `/health` works |
| [[Phase 1 — Persistence\|1 Persistence]] | ✅ Done | merged (PR #2) |
| [[Phase 2 — Ingestion\|2 Maven ingestion]] | ✅ Done | ready to merge; 14/14 tests |
| [[Phase 3 — Advisories\|3 Advisories]] | 🔜 Next | OSV ingestion + range filter |
| 4 Usage scan | ⏳ | After P2 (parallel with P3) |
| 5 Agents | ⏳ | After P3 + P4 |
| 6 Dashboard | ⏳ | After P5 |
| 7 Deploy | ⏳ | After P6 |
| 8 Python | ⏳ | After v0.1.0 |

## Phase 3 checklist

See [[Phase 3 — Advisories]] for the full plan.

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
