# Phase 1 — Persistence

**Branch:** `feat/phase1-persistence`  
**Needs:** Phase 0 ✅  
**Enables:** All ingestion and analysis

## Steps

### 1.1 Start Postgres

```powershell
docker run --name br-pg -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=blastradius -p 5432:5432 -d postgres:16
```

Requires Docker Desktop running.

### 1.2 Dependencies + config

- `pom.xml`: `spring-boot-starter-data-jpa`, `postgresql`, `flyway-core`
- `application.properties`: datasource, `ddl-auto=validate`, flyway enabled

### 1.3 Entities

Under `com.blastradius.model`: Project, Dependency, UsageSite, Advisory, Finding

### 1.4 Repositories

One Spring Data JPA repository per entity.

### 1.5 Flyway

`src/main/resources/db/migration/V1__init.sql` matching entities.

## Checkpoint

- [x] App boots without error
- [x] Flyway creates 5 tables
- [x] Tables visible in IntelliJ Database tool (localhost:5432, blastradius/postgres)

## IntelliJ DB connection

1. Database tool window → **+** → Data Source → PostgreSQL
2. Host: `localhost`, Port: `5432`, Database: `blastradius`, User/Password: `postgres`
3. Test connection → OK
