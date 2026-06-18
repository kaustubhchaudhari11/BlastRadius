# Blast Radius

Dependency-triage tool for Java and Python projects: filters advisory noise and surfaces which CVEs/version changes actually touch code you call — with migration drafts for your call sites.

## Prerequisites

- JDK 21 (Temurin)
- Docker Desktop (for PostgreSQL)
- Maven (wrapper included)

## Run locally

```powershell
# Start PostgreSQL (requires Docker Desktop running)
docker run --name br-pg -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=blastradius -p 5432:5432 -d postgres:16

# Run the app
.\mvnw.cmd spring-boot:run
```

Health check: http://localhost:8080/health

## Documentation

Project roadmap, architecture, and phase checklists live in [`docs/`](docs/). Open `docs/` as an Obsidian vault or import into Notion — see [How to use this vault](docs/How%20to%20use%20this%20vault.md).

## Stack

- Spring Boot 3.x, Java 21, Maven
- PostgreSQL 16, Flyway, Spring Data JPA
- React + Vite (Phase 6)

## License

Private — portfolio / interview project.
