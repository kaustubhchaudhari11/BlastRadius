# Git Workflow

## Branching

One branch per phase: `feat/phaseN-short-name`

Examples:
- `feat/phase1-persistence`
- `feat/phase2-maven-ingestion`

Cut each branch from `main` after the previous phase merges.

## Commits

Commit after **each numbered micro-step**, not at end of phase.

Conventional commits:
- `feat:` — new capability
- `fix:` — bug fix
- `test:` — tests
- `chore:` — tooling, config
- `docs:` — documentation
- `refactor:` — restructure

Examples:
```
chore(db): add jpa, postgres driver, flyway config
feat(model): add core entities
feat(ingestion): define EcosystemAdapter interface
```

## PR flow

1. Finish phase checkpoint
2. Push branch
3. Open PR into `main`
4. Self-review diff (interview prep)
5. Merge
6. Tag at milestones: `v0.1.0` (Phase 7), `v0.2.0` (Phase 8)

## Remote

```
https://github.com/kaustubhchaudhari11/BlastRadius
```
