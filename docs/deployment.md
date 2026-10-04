# Deployment

## Environments

| Environment | Frontend | API | Deployed from |
|---|---|---|---|
| Production | `https://waypoint-relay.vercel.app` | `https://waypoint-api.duckdns.org` | `main` |
| Development | run locally (see below) | `https://waypoint-api-dev.duckdns.org` | `develop` |

```
Browser ──► Vercel (frontend/)            static React PWA
   │
   └─────► Caddy :443 (HTTPS, Let's Encrypt)
              ├─ waypoint-api.duckdns.org      → 127.0.0.1:8082  wp-prod (backend + PostgreSQL)
              └─ waypoint-api-dev.duckdns.org  → 127.0.0.1:8081  wp-dev  (backend + PostgreSQL)
```

Both backends run on one Ubuntu 24.04 server as separate Docker Compose projects with separate databases and secrets.

## Frontend (Vercel)

The production frontend is `https://waypoint-relay.vercel.app`. The Vercel project is **not Git-connected**, so pull-request and `develop` branches are not built there. `.github/workflows/frontend-deploy.yml` ("Frontend deploy") runs after CI succeeds on a push to `main`, then runs `vercel pull`, `vercel build --prod` and `vercel deploy --prebuilt --prod` from `frontend/`.

- Root Directory `frontend`; framework Vite (`frontend/vercel.json`).
- Project environment variable `VITE_API_URL` (Production) → `https://waypoint-api.duckdns.org`.
- Branches are tested locally with `npm run dev` and `VITE_API_TARGET=https://waypoint-api-dev.duckdns.org` (see "Local frontend against the development API").

Repository secrets (environment `production`):

| Secret | Value |
|---|---|
| `VERCEL_TOKEN` | Vercel access token |
| `VERCEL_ORG_ID` | Vercel team / account ID |
| `VERCEL_PROJECT_ID` | Vercel project ID |

## Backend server

### One-time setup
```bash
scp deploy/server-setup.sh ubuntu@<server>:~
ssh ubuntu@<server> 'bash ~/server-setup.sh'
```
Installs Docker, Caddy, 2 GB swap and the firewall (22, 80, 443), and clones
`/opt/waypoint/dev` (`develop`) and `/opt/waypoint/prod` (`main`).

Then on the server:
1. Copy the dataset into `/opt/waypoint/data` (from your machine: `scp -r data/* ubuntu@<server>:/opt/waypoint/data/`).
2. Create `/opt/waypoint/dev/.env` and `/opt/waypoint/prod/.env` from `.env.example`, each with its own `DB_PASSWORD` and `JWT_SECRET`, and:

   | Setting | dev | prod |
   |---|---|---|
   | `API_PORT` | `8081` | `8082` |
   | `DATA_PATH` | `/opt/waypoint/data` | `/opt/waypoint/data` |
   | `CORS_ORIGINS` | `https://*.vercel.app,http://localhost:5173` | `https://waypoint-relay.vercel.app` |
   | `SEED_ENABLED` | `true` | `true` |
   | `DEMO_MODE` | `true` | `true` |
3. Install the proxy config: `sudo cp /opt/waypoint/prod/deploy/Caddyfile /etc/caddy/Caddyfile && sudo systemctl reload caddy`
4. First deploy: `/opt/waypoint/dev/deploy/deploy.sh dev` and `/opt/waypoint/prod/deploy/deploy.sh prod`

### Automatic deploys
`.github/workflows/deploy.yml` runs after CI succeeds on a push to `develop` or `main`,
connects over SSH and runs `deploy/deploy.sh dev|prod`, which pulls the branch, rebuilds the
database and backend containers and waits for `/actuator/health`.

Repository secrets:

| Secret | Value |
|---|---|
| `DEPLOY_HOST` | server hostname or IP |
| `DEPLOY_USER` | `ubuntu` |
| `DEPLOY_SSH_KEY` | private key of a key pair used only for deploys |
| `DEPLOY_KNOWN_HOSTS` | output of `ssh-keyscan <server>` |

### Operations
```bash
# Logs
docker compose -p wp-dev logs -f backend

# Reset development data (production is never touched)
/opt/waypoint/dev/deploy/reset-dev-data.sh
```

## Timed jobs

The backend runs time-driven work on the **demo clock**, not real time. A scheduler ticks every 30 seconds and each job decides from the demo clock whether it is due. `POST /api/settings/clock` also ticks immediately, so moving the clock past a job's time fires it before the request returns.

| Job | Due (Sri Lanka time) | What it does |
|---|---|---|
| `store-reminder` | 3:00 PM, day before the run | Warns each store with unconfirmed chilled / Style / Tech orders: "Confirm by 4 PM" |
| `dispatcher-alert` | 3:30 PM, day before the run | Warns the depot's dispatchers which stores have not confirmed; links to `/dispatch` (D1, where the unconfirmed panel is) |
| `close-orders` | 4:00 PM, day before the run | Closes the orders (same as the Close orders button) |
| `failed-delivery-retry` | 2:00 PM, run day | Re-plans failed deliveries with no store reply (driver module hook) |
| `receipt-auto-close` | 11:59 PM, run day | Closes unconfirmed receipts (store module hook) |

- Each job runs **once per run date and depot**. Completion is stored in `app_settings` as `job.<name>.<runDate>.<depot>`, so a restart does not repeat it.
- The two notification jobs send nothing, but still count as done, when there is nobody to report or the orders are already closed. A job whose hook has no implementation yet logs `skipped – no implementation` and counts as done.
- A failing job is logged (`WARN`, with job, run date and depot) and retried on the next tick; other jobs keep running. Each job runs in its own transaction.
- If the clock jumps past several jobs, they fire in time order.
- **Moving the clock backwards does not re-run jobs.** To start over, reset the data (`reset-dev-data.sh` on the dev server, or a fresh database volume locally).
- `JOBS_ENABLED` (default `true`) switches the scheduler and the clock-move trigger off; it is `false` in tests.

## Local full stack
```bash
cp .env.example .env
docker compose up -d --build     # http://localhost
```

## Local frontend against the development API
```bash
cd frontend
VITE_API_TARGET=https://waypoint-api-dev.duckdns.org npm run dev
```
