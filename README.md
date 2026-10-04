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
| Delivery | Vercel (frontend), Docker Compose + Caddy (backend), GitHub Actions |

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

Modules communicate only through service interfaces. API: [`docs/api.md`](docs/api.md) · Data model: [`docs/data-model.md`](docs/data-model.md) · Decisions: [`docs/adr/`](docs/adr/)

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
On first start the backend loads reference data, the demo delivery day (Thu 1 Oct 2026: 85 orders and the fleet) and the demo accounts from `./data`. It does this once; later starts skip it.
If the folder or a file is missing, start-up stops with a message naming the file. `docker compose` seeds and runs the demo clock by default (`SEED_ENABLED` and `DEMO_MODE` are `true`); set both to `false` in `.env` to start without demo data or accounts.

### Local development
```bash
# backend (needs a local PostgreSQL 16, e.g. `docker compose up -d db`)
cd backend && JWT_SECRET="$(openssl rand -base64 48)" ./mvnw spring-boot:run

# frontend
cd frontend && npm ci && npm run dev
```

## Live demo

- Frontend: https://waypoint-relay.vercel.app
- API: https://waypoint-api.duckdns.org
- Deployment guide: [`docs/deployment.md`](docs/deployment.md)

The demo day is **Thursday 1 October 2026** at Peliyagoda. The demo clock starts on **Wednesday 30 September 2026, 2:00 PM**, so the order day is still open.

### Demo accounts

| Role | Where to sign in | Sign in with |
|---|---|---|
| Store manager (Dilani, OUT001) | `/login` | `dilani@waypoint.lk` / `Relay@2026` |
| Dispatcher (Ruwan) | `/login` | `ruwan@waypoint.lk` / `Relay@2026` |
| Loader (Kasun, Peliyagoda dock) | `/loader/sign-in` | PIN `1234` (the depot is Peliyagoda) |
| Driver (Nuwan, VEH036) | `/login` | staff ID `DRV-0036` / PIN `3636` (type the PIN in the password field) |

### Demo clock

All business time comes from the demo clock, not the computer's date. A dispatcher can move it from the **Demo clock (Sri Lanka)** field in the top bar of the dispatcher screens (choose a date and time, then **Set**). Jobs that have become due run as soon as the clock moves.

| Demo time (Sri Lanka) | What happens |
|---|---|
| 3:00 PM, day before the run | Stores with unconfirmed chilled, Style or Tech orders get a reminder |
| 3:30 PM | Dispatch is alerted with the list of stores that have not confirmed |
| 4:00 PM | Orders close: Fresh ambient orders are confirmed automatically, other unconfirmed orders are left out |
| 2:00 PM, run day | Failed deliveries nobody answered are re-planned for the next day |
| 11:59 PM, run day | Unconfirmed receipts close automatically |

The walkthrough below does not need the clock to move: the dispatcher can close orders with a button.

## Judge walkthrough

Use two browser windows (or a desktop window and a phone-sized window): the loader and driver screens are built for phones.

1. **Store confirms its order.** Sign in at `/login` as Dilani. On Home, press **Review and confirm**, then **Confirm orders** for the two prepared orders. Change a quantity first to see the usual-quantity warning, or use **× Cancel order** on one to see cancellation.
2. **Dispatcher closes orders.** Sign in as Ruwan. On **Orders**, check the **Not confirmed** tab, optionally use **+ Add order for outlet** to enter a phone-in order, then press **Close orders**.
3. **Dispatcher confirms the fleet.** Open **Fleet**, optionally take a vehicle off the road, and press **Confirm fleet**.
4. **Dispatcher creates and publishes the plan.** Open **Plan** and press **Create plan**. Review the **Board** and the **Deferrals** tab, where each deferred order shows why it was moved. Press **Publish plan v1** and confirm. Stores, loaders and drivers are notified.
5. **Store sees the result.** As Dilani, open **Deliveries** to see the arrival window, or the moved-order notice and the choices offered for a deferred order.
6. **Loader loads the truck.** Open `/loader/sign-in` on a phone-sized screen and enter PIN `1234`. Pick the next trip with **Load now**, answer the fridge check and press **Start loading**, then tick each stop in the order shown. Use **⚠ Flag missing or damaged** to record a shortfall (a remainder order is created), then **Hand over to the driver** and **✓ Confirm handover**.
7. **Driver delivers.** Sign in at `/login` as `DRV-0036` with PIN `3636` on a phone-sized screen. Press **Check the load and accept**, then **I have arrived** at the first stop and **Record delivery** with a photo, a signature and the receiver's name. To try offline mode, switch the browser to offline: the status shows **Offline**, deliveries are saved on the phone, and they show **Synced** again when the connection returns. To try a failed delivery, record one with a reason such as **Store was closed**.
8. **Dispatcher watches and decides.** On **Live board** the delivery appears within seconds. **Driver decisions** lists failed deliveries and any sync conflict that needs a decision.
9. **Store confirms receipt.** As Dilani, open the delivered order under **Deliveries** and confirm that what arrived matches. For a failed delivery, the store chooses **Deliver tomorrow**, **Try later today** or **Cancel this order**, and dispatch is notified.

## Planning engine

The plan is built by a deterministic, rule-based engine; it uses no machine-learning model.

- **Rules checked:** one brand and one district per trip; chilled orders only on fridge vehicles; van-only outlets only on vans; a vehicle serves only its home depot; whole orders on one trip; weight and volume capacity; at most two trips per vehicle per day; trip time budgets; weekly fuel quota; delivery windows.
- **Priority:** chilled Fresh 3, ambient Fresh 2, Style and Tech 1, plus 2 for each extra day an order has waited and 3 if it was deferred yesterday.
- **Deferrals:** every deferred order records whether it was unavoidable (no vehicle can carry it) or chosen (it lost on priority), the rule that decided it, a plain-language reason and its new date. The store can keep, reduce or cancel it, or split it when it is too large.
- **Verification:** the finished plan is re-checked by the same rule checker, and any violation is counted in the plan summary; the **Publish** button is disabled while there is one.

More detail: [`docs/architecture.md`](docs/architecture.md).

## Changes from the Designathon design

- The store's skip action is labelled **Cancel order**.
- Order cut-off is a **Close orders** button on the dispatcher's order queue, in addition to the automatic 4:00 PM job on the demo clock.
- Arrival windows use the trip-time formula rather than a prediction model.
- Breakdown choices are not shown to the store; a breakdown shows a plain note that dispatch will make contact.

## Not included in this version

- Plan v2 (changing a published plan and notifying only the affected stops)
- Breakdown re-planning
- Manual trip edits with a rule check
- Capacity outlook shows sample data, not a forecast from the system's data
- Sign-up, password reset and account management; GPS tracking and navigation; payments and invoicing; any chatbot or LLM feature

## Documentation

- [`docs/architecture.md`](docs/architecture.md) — modules, security, order lifecycle, planning engine, offline sync
- [`docs/api.md`](docs/api.md) — API contract
- [`docs/data-model.md`](docs/data-model.md) — tables
- [`docs/ai-disclosure.md`](docs/ai-disclosure.md) — AI tools used to build the project

## Project structure

```
backend/             Spring Boot application
  src/main/java/com/synapse/waypoint/<module>/
  src/main/resources/db/migration/   Flyway migrations
frontend/            React application
  src/features/<role>/   screens per role
  src/ui/                shared components
  src/lib/               API client, auth, offline
deploy/              server setup, deploy script, Caddy config
docs/                API contract, data model, deployment, decision records
.github/workflows/   CI
```

## Contributing

See [`CONTRIBUTING.md`](CONTRIBUTING.md).

## License

[MIT](LICENSE)
