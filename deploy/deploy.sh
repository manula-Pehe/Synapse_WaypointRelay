#!/usr/bin/env bash
# Deploys one environment on the server: pulls the branch, rebuilds and restarts the
# database and backend, then waits for the health check.
# Usage: deploy.sh dev|prod
set -euo pipefail

# Wrapped in a function so bash reads the whole script before `git reset`
# replaces this file on disk.
main() {
  ENVIRONMENT="${1:?usage: deploy.sh dev|prod}"
  case "$ENVIRONMENT" in
    dev)  BRANCH=develop ;;
    prod) BRANCH=main ;;
    *) echo "unknown environment: $ENVIRONMENT" >&2; exit 1 ;;
  esac

  DIR="/opt/waypoint/${ENVIRONMENT}"
  PROJECT="wp-${ENVIRONMENT}"
  cd "$DIR"

  git fetch --quiet origin "$BRANCH"
  git reset --quiet --hard "origin/${BRANCH}"
  echo "Deploying ${PROJECT} at $(git rev-parse --short HEAD)"

  API_PORT="$(grep -E '^API_PORT=' .env | cut -d= -f2)"
  docker compose -p "$PROJECT" up -d --build --remove-orphans db backend
  docker image prune -f >/dev/null

  for i in $(seq 1 40); do
    if curl -fsS "http://127.0.0.1:${API_PORT}/actuator/health" >/dev/null 2>&1; then
      echo "Healthy after $((i * 3))s"
      exit 0
    fi
    sleep 3
  done

  echo "Backend did not become healthy; last logs:" >&2
  docker compose -p "$PROJECT" logs --tail=80 backend >&2
  exit 1
}

main "$@"
