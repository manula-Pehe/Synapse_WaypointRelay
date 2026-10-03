# 3. PostgreSQL with Flyway, schema first

- Status: Accepted
- Date: 2026-10-03

## Context
Several modules add tables at the same time. Automatic schema generation from entities hides changes and drifts between environments.

## Decision
- PostgreSQL 16 is the only database.
- The schema is defined by Flyway migrations in `backend/src/main/resources/db/migration`.
- Hibernate runs with `ddl-auto=validate`: entities must match the migrated schema, and startup fails if they don't.
- Merged migrations are never edited. Each module uses its own version range (see `CONTRIBUTING.md`) to avoid version clashes.
- Enumerated columns use `CHECK` constraints.

## Consequences
- The schema is reviewable in pull requests and identical in every environment.
- A mismatch between entity and schema is caught at startup and in CI.
- Fixing a merged migration requires a new migration.
