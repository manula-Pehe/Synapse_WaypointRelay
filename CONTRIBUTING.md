# Contributing

## Branches

| Branch | Purpose |
|---|---|
| `main` | Production. Updated only from `develop` by pull request. |
| `develop` | Integration. Default branch; all work merges here. |
| `feature/<area>-<name>` | New functionality, e.g. `feature/core-orders`, `feature/driver-sync` |
| `fix/<area>-<name>` | Bug fixes |
| `chore/<name>` / `docs/<name>` | Tooling, CI, documentation |

Branch from `develop`, keep branches short-lived, and rebase or merge `develop` in often.

## Pull requests

- Target `develop`.
- CI (`backend`, `frontend`, `docker`) must pass.
- At least one approval.
- Squash merge; the PR title becomes the commit message.
- One feature per PR. Link the screen ID it implements (e.g. `S2 · Review and confirm`).

## Commit messages

[Conventional Commits](https://www.conventionalcommits.org/):

```
feat(core): close orders at cut-off
fix(driver): retry sync after reconnect
docs: add ADR for demo clock
```

Types: `feat`, `fix`, `refactor`, `test`, `docs`, `chore`, `ci`.

## Backend rules

- Code lives in its own module package (`com.synapse.waypoint.<module>`). Other modules are used only through their service interfaces — see `docs/api.md` §11.
- Order status changes only through `OrderService`.
- Business time comes from `DemoClock`; never `LocalDate.now()` / `Instant.now()` in business logic.
- Every business rule has at least one test.

### Database migrations

- Write the migration first, then the entity (`spring.jpa.hibernate.ddl-auto=validate`).
- Never edit a migration that has been merged; add a new one.
- Version ranges per module avoid clashes:

| Range | Module |
|---|---|
| V1–V9 | auth, core, notification |
| V10–V19 | planning |
| V20–V29 | driver |
| V30–V39 | store, issue |
| V40–V49 | dispatch |
| V50–V59 | loader |

## Frontend rules

- Use the shared API client (`src/lib/api`), auth hook (`useAuth`), UI components (`src/ui`) and i18n — no parallel versions.
- TypeScript strict; `npm run lint` and `npm run build` must pass.
- All user-facing text goes through i18n (English, Sinhala, Tamil).
- Time on screen comes from `GET /api/settings`, never the device clock.
- Screens match their design reference; note the screen ID in a comment at the top of the component.

## Data

- **Never commit the competition dataset**, CSV files, or values copied from it.
- Mock data uses made-up values only.
- Never commit `.env`; add new settings to `.env.example`.
