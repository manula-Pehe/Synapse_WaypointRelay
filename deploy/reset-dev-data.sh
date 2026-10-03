#!/usr/bin/env bash
# Wipes the DEV database so the seed data loads again on the next start.
# Production is never touched.
set -euo pipefail
cd /opt/waypoint/dev
read -r -p "Delete all DEV data and reload the seed? Type 'reset' to continue: " answer
[ "$answer" = "reset" ] || { echo "Cancelled."; exit 1; }
docker compose -p wp-dev down -v
/opt/waypoint/dev/deploy/deploy.sh dev
