# Waypoint Relay

Next-day delivery planning and execution for a multi-brand retail network.
Stores confirm what they need, dispatch turns confirmed orders into a fair, rule-checked delivery plan,
the depot loads trucks against it, and drivers deliver with proof — online or offline.

> Built for Tech-Triathlon 2026 (Hackathon phase) by Team Synapse.

---

## Features by role

| Role | What they do in Waypoint Relay |
|---|---|
| **Store manager** | Review and confirm tomorrow's order, edit or cancel before cut-off, see the delivery window and status, confirm receipt, raise issues |
| **Dispatcher** | Close orders at cut-off, confirm the fleet, generate and review the delivery plan, resolve deferrals, publish, monitor the day live |
| **Loader** | See trucks in departure order, check the fridge, load in reverse stop order, flag shortfalls, hand over to the driver |
| **Driver** | Accept the load, follow stops in order, record delivered / partial / failed with proof, keep working without signal |

Shared: role-based sign-in, notifications, English / Sinhala / Tamil, light and dark themes, and a demo clock to move through the order day and the run day.

## Tech stack

| Layer | Technology |
|---|---|
| Backend | Java 21, Spring Boot 4.1, Spring Security (JWT resource server), Spring Data JPA, Flyway |
| Database | PostgreSQL 16 |
| Frontend | React 19, TypeScript (strict), Vite, Tailwind CSS 4, React Router 7, TanStack Query |
| Offline | PWA (service worker) + IndexedDB outbox (Dexie) |
| Delivery | Docker Compose, nginx, GitHub Actions |

## Architecture

```
 Browser / PWA (React)
        │  HTTPS, JSON, Bearer JWT
        ▼
 nginx ──/api──► Spring Boot (modular monolith)
                   ├─ auth          sign-in, roles, current user
                   ├─ core          orders, reference data, fleet, demo clock, jobs
                   ├─ notification  in-app notifications
                   ├─ planning      plan generation, rules, deferrals
                   ├─ dispatch      live operations
                   ├─ store / issue store actions, receipts, issues
                   ├─ loader        loading, fridge checks, shortfalls, handover
                   └─ driver        trips, delivery outcomes, offline sync
                         │
                         ▼
                   PostgreSQL 16
```

Modules communicate only through service interfaces. API: [`docs/api.md`](docs/api.md) · Decisions: [`docs/adr/`](docs/adr/)

## Getting started

### Prerequisites
- Docker and Docker Compose
- The competition dataset (not included in this repository — see below)

### Run
```bash
git clone https://github.com/manula-Pehe/Synapse_WaypointRelay.git
cd Synapse_WaypointRelay
cp .env.example .env          # set DB_PASSWORD and JWT_SECRET
mkdir -p data                 # copy the dataset CSV files into ./data
docker compose up -d --build
```
Open http://localhost.

### Dataset
The competition dataset is confidential and is **never committed**. `./data` and all `*.csv` files are git-ignored.
On first start the backend loads reference data and the demo delivery day from `./data`.

### Local development
```bash
# backend (needs a local PostgreSQL 16)
cd backend && ./mvnw spring-boot:run

# frontend
cd frontend && npm ci && npm run dev
```

## Demo

| Role | Sign in with |
|---|---|
| Store manager | _added with the seed data_ |
| Dispatcher | _added with the seed data_ |
| Loader | _added with the seed data_ |
| Driver | _added with the seed data_ |

Live URL: _to be added_

A step-by-step walkthrough across all four roles will be added here.

## Project structure

```
backend/             Spring Boot application
  src/main/java/com/synapse/waypoint/<module>/
  src/main/resources/db/migration/   Flyway migrations
frontend/            React application
  src/features/<role>/   screens per role
  src/ui/                shared components
  src/lib/               API client, auth, offline
docs/                API contract, architecture, decision records
.github/workflows/   CI
```

## Contributing

See [`CONTRIBUTING.md`](CONTRIBUTING.md).

## License

[MIT](LICENSE)
