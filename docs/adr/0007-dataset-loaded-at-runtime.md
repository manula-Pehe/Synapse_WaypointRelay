# 7. Dataset loaded at runtime, never committed

- Status: Accepted
- Date: 2026-10-03

## Context
The system runs on a confidential competition dataset (outlets, vehicles, travel times, orders). The repository is public. The application still needs the real data to run the demo delivery day.

## Decision
- The dataset is never committed. `/data/` and `*.csv` are git-ignored.
- On first start, the backend loads reference data and the demo day from a mounted data directory (`./data`).
- If the directory is missing, startup reports a clear error instead of running with partial data.
- Development and tests use made-up mock data only. No values copied from the dataset appear in code, tests or documentation.
- On servers the dataset is copied directly to the host, not through git or CI.

## Consequences
- The public repository contains no confidential data.
- Anyone running the system must obtain the dataset separately and place it in `./data`.
- CI runs without the dataset; seed-dependent tests are skipped there.
