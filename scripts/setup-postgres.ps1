# Creates blastradius DB. Run once after PostgreSQL install:
#   $env:PGPASSWORD = "your_postgres_password"
#   .\scripts\setup-postgres.ps1

$psql = "C:\Program Files\PostgreSQL\18\bin\psql.exe"
if (-not (Test-Path $psql)) { $psql = "C:\Program Files\PostgreSQL\17\bin\psql.exe" }
if (-not $env:PGPASSWORD) { Write-Error "Set PGPASSWORD first: `$env:PGPASSWORD = 'your_password'"; exit 1 }

& $psql -U postgres -h localhost -p 5432 -tc "SELECT 1 FROM pg_database WHERE datname = 'blastradius'" | Out-Null
$exists = & $psql -U postgres -h localhost -p 5432 -tAc "SELECT 1 FROM pg_database WHERE datname = 'blastradius'"
if ($exists -eq "1") { Write-Host "Database blastradius already exists."; exit 0 }

& $psql -U postgres -h localhost -p 5432 -c "CREATE DATABASE blastradius OWNER postgres;"
Write-Host "Created database blastradius."
