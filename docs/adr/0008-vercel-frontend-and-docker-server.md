# 8. Frontend on Vercel, backend on a Docker server

- Status: Accepted
- Date: 2026-10-03

## Context
The team needs shared development and production environments early, with HTTPS (required for the camera and the service worker) and automatic deploys. The backend is a long-running Spring Boot application with PostgreSQL and needs the dataset on disk. Budget is limited to free tiers and credits.

## Decision
- **Frontend:** Vercel, built from `frontend/`. `main` is production, `develop` and pull requests get preview deployments. The API base URL is set per environment with `VITE_API_URL`.
- **Backend:** one Ubuntu VM running Docker Compose, with separate `wp-dev` (`develop`) and `wp-prod` (`main`) projects, each with its own database and secrets. Containers bind to localhost only.
- **HTTPS:** Caddy in front of both backends, with automatic Let's Encrypt certificates for DuckDNS hostnames.
- **Deploys:** a GitHub Actions workflow runs after CI succeeds and deploys over SSH with `deploy/deploy.sh`, which checks the health endpoint.
- **CORS:** allowed frontend origins are configured per environment (`CORS_ORIGINS`).

Alternatives considered: serverless/PaaS backends with free tiers (sleep after idle time, 512 MB memory, no persistent disk for the dataset) and nginx with certbot (more manual certificate setup).

## Consequences
- Frontend previews for every pull request; backend always on with no cold starts.
- The server is a single point of failure and must be maintained (updates, disk, credits).
- The frontend and API are on different origins, so CORS must be kept in sync with Vercel URLs.
