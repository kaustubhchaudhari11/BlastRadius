# BlastRadius

DependencyReportProject

## Dependency check setup

This repository now includes an automated dependency check for GitHub projects:

- **Dependency Review workflow**: `.github/workflows/dependency-check.yml`
  - Runs on every pull request
  - Runs weekly on a schedule
  - Can be run manually
  - Fails on newly introduced dependencies with **moderate or higher** severity vulnerabilities
- **Dependabot updates**: `.github/dependabot.yml`
  - Checks GitHub Actions dependencies weekly
