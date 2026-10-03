# Deployment

## Environments

| Environment | Frontend | API | Deployed from |
|---|---|---|---|
| Production | Vercel production deployment | `https://waypoint-api.duckdns.org` | `main` |
| Development | Vercel `develop` branch deployment | `https://waypoint-api-dev.duckdns.org` | `develop` |
| Pull requests | Vercel preview per PR | development API | — |

```
Browser ──► Vercel (frontend/)            static React PWA
   │
   └─────► Caddy :443 (HTTPS, Let's Encrypt)
              ├─ waypoint-api.duckdns.org      → 127.0.0.1:8082  wp-prod (backend + PostgreSQL)
              └─ waypoint-api-dev.duckdns.org  → 127.0.0.1:8081  wp-dev  (backend + PostgreSQL)
```

Both backends run on one Ubuntu 24.04 server as separate Docker Compose projects with separate databases and secrets.

## Frontend (Vercel)

- Import the repository; **Root Directory** `frontend`; framework Vite (`frontend/vercel.json`).
- **Production Branch:** `main`.
- Environment variable `VITE_API_URL`:
  - Production → `https://waypoint-api.duckdns.org`
  - Preview → `https://waypoint-api-dev.duckdns.org`
- Ignored Build Step: `git diff --quiet HEAD^ HEAD -- .` (skips builds when only the backend changed).

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
   | `CORS_ORIGINS` | `https://*.vercel.app,http://localhost:5173` | the Vercel production URL |
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
