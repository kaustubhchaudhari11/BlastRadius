# Roadmap — Phases & Dependencies

## Phase dependency graph

```mermaid
flowchart TD
    P0[Phase 0: Skeleton ✅]
    P1[Phase 1: Persistence]
    P2[Phase 2: Adapter + Maven parse]
    P3[Phase 3: Advisory ingestion]
    P4[Phase 4: Maven usage scan]
    P5[Phase 5: Multi-agent pipeline]
    P6[Phase 6: REST + React UI]
    P7[Phase 7: Telemetry + deploy v0.1.0]
    P8[Phase 8: Python adapter v0.2.0]
    P9[Phase 9: Stretch]

    P0 --> P1
    P1 --> P2
    P2 --> P3
    P2 --> P4
    P3 --> P5
    P4 --> P5
    P5 --> P6
    P6 --> P7
    P7 --> P8
    P8 --> P9
```

**Critical path:** P0 → P1 → P2 → (P3 + P4 in parallel) → P5 → P6 → P7 → P8

P3 and P4 are **independent** after P2 — build in either order or parallel sessions.

P5 is the **first convergence** — needs both advisories AND usage sites.

---

## All phases (summary)

| Phase | Name | Type | Needs | Enables | Tag |
|-------|------|------|-------|---------|-----|
| **0** | Spring Boot skeleton + `/health` | 🔧 | JDK 21 | Everything | ✅ |
| **1** | PostgreSQL + JPA + Flyway + entities | 🔧 | P0 | All ingestion/analysis | ✅ |
| **2** | `EcosystemAdapter` + Maven `parseDependencies` + **P2.5 version resolution** | 🧠🔧 | P1 | P3, P4, P8 | ✅ |
| **3** | OSV advisory ingestion (Maven + PyPI) | 🧠🔧 | P2 | P5 | ◀ next |
| **4** | Maven `scanUsage` (import/symbol) | 🧠 | P2 | P5 | — |
| **5** | Triage → Migration → Eval agents | 🧠 | P3 + P4 | P6 | — |
| **6** | REST API + React dashboard | 🔧 | P5 | P7 | — |
| **7** | Telemetry + Railway/Vercel deploy | 🔧 | P6 | P8 | **v0.1.0** |
| **8** | `PythonAdapter` (pypi) | 🧠 | P7 | Two-lang demo | **v0.2.0** |
| **9** | Stretch: GitHub clone, Kafka, AST | — | P8 | — | — |

🧠 = substance (review every line) · 🔧 = boilerplate (Cursor can drive)

---

## Three-week plan

| Week | Phases | Outcome |
|------|--------|---------|
| 1 | 1, 2, 3 | Adapter + Java parsing + advisories |
| 2 | 4, 5 | Usage detection + agent pipeline E2E |
| 3 | 6, 7 | Dashboard + deployed **v0.1.0** |
| 4 (buffer) | 8 | Python adapter → **v0.2.0** |

---

## Micro-steps checklist (Phase 3 — next)

- [ ] 3.1 `AdvisoryClient` → OSV.dev (`/v1/querybatch`) from distinct `(ecosystem, pkg, version)`
- [ ] 3.2 Map OSV JSON → `Advisory` entity (dedupe on `external_id`)
- [ ] 3.3 `AdvisoryService.refreshForProject(projectId)` upsert
- [ ] 3.4 Version-range check (`current_version` ∈ OSV affected range; `"unspecified"` → flag)
- [ ] 3.5 `POST /api/projects/{id}/advisories/refresh`
- [ ] 3.6 Unit test: OSV fixture → advisories + range true/false
- [ ] Checkpoint: refresh real project → `advisory` rows land, known CVE matches by range
- [ ] Push branch → PR → merge

> ✅ Phases 0–2 complete (incl. P2.5 version resolution). See [[PROGRESS]] for the live handoff and the Phase 2 → Phase 3 contract.
