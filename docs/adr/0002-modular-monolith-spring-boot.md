# 2. Modular monolith on Spring Boot

- Status: Accepted
- Date: 2026-10-03

## Context
The system serves four roles (store manager, dispatcher, loader, driver) that share orders, vehicles and plans. It must be built and deployed quickly by a small team and run on a single small server.

## Decision
Build one Spring Boot 4.1 application on Java 21, split into modules by business area (`auth`, `core`, `notification`, `planning`, `dispatch`, `store`, `issue`, `loader`, `driver`, `common`).
- Each module owns its package, tables and endpoints.
- Modules call each other only through public service interfaces (listed in `docs/api.md` §11), never another module's repositories or entities.
- There is no HTTP between modules.

## Consequences
- One deployable, one database, simple transactions across modules.
- Clear boundaries let modules be developed in parallel against stubbed interfaces.
- Boundaries are enforced by convention and review, not by the build; they could be extracted into services later if needed.
