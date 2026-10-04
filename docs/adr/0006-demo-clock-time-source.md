# 6. Demo clock as the single time source

- Status: Accepted
- Date: 2026-10-03

## Context
The workflow spans two days: orders are confirmed and closed in the afternoon, and deliveries run the next morning. Scheduled steps (reminders, cut-off) depend on time of day. Reviewers need to move through the whole cycle without waiting, and devices may have wrong clocks.

## Decision
- A `DemoClock` service provides the current time as real time plus a stored offset.
- All business logic, scheduled jobs and timestamps use `DemoClock`; `LocalDate.now()` / `Instant.now()` are not used for business decisions.
- The frontend reads the current demo time and run date from `GET /api/settings` and never uses the device clock for business logic.
- A dispatcher can move the clock with `POST /api/settings/clock`.
- Scheduled jobs check the demo time and run once per run date.

## Consequences
- The full order-to-delivery cycle can be demonstrated in minutes.
- Time-dependent rules are testable by setting the clock.
- Every module must inject `DemoClock`; code review checks for direct clock use.
