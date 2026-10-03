# 1. Record architecture decisions

- Status: Accepted
- Date: 2026-10-03

## Context
Several people build modules in parallel on a short timeline. Decisions about structure, data and tooling need to be visible and stable so that work does not diverge.

## Decision
Significant decisions are recorded as Architecture Decision Records in `docs/adr/`, numbered sequentially, using the format: Context, Decision, Consequences.
A decision is changed by adding a new ADR that supersedes the old one, not by editing it.

## Consequences
- New contributors can understand why the system looks the way it does.
- Changing a recorded decision requires a pull request and a new ADR.
