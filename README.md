# Blast Radius

Dependency-triage tool for Java and Python projects: filters advisory noise and surfaces which CVEs/version changes actually touch code you call — with migration drafts for your call sites.

## Prerequisites

- JDK 21 (Temurin)
- PostgreSQL 16+ (local install or Docker)
- Maven (wrapper included)

## Run locally

```powershell
# 1. Create the database (one time)
$env:PGPASSWORD = "your_postgres_password"
.\scripts\setup-postgres.ps1

# 2. Provide the DB password (gitignored local file)
copy src\main\resources\application-local.properties.example src\main\resources\application-local.properties
# then edit it: spring.datasource.password=your_postgres_password

# 3. Run the app
.\mvnw.cmd spring-boot:run
```

Health check: http://localhost:8080/health

## Documentation

Project roadmap, architecture, and phase checklists live in [`docs/`](docs/). Open `docs/` as an Obsidian vault or import into Notion — see [How to use this vault](docs/How%20to%20use%20this%20vault.md).

## Stack

- Spring Boot 3.x, Java 21, Maven
- PostgreSQL, Flyway, Spring Data JPA
- React + Vite (Phase 6)

## License

Private — portfolio / interview project.
