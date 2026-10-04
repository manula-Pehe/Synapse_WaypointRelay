# 5. Single order service with an explicit state machine

- Status: Accepted
- Date: 2026-10-03

## Context
An order is touched by every role: the store confirms it, dispatch plans or moves it, the loader loads it, the driver delivers it. If modules updated order status directly, roles could see different states and history would be incomplete.

## Decision
- All order status changes go through `OrderService` in the `core` module.
- Valid transitions are defined once (see `docs/api.md`, "Order lifecycle"); any other change returns `409 INVALID_STATUS`.
- Every change writes an `order_events` row (time from the demo clock, actor, from/to status, details) in the same transaction.
- Orders use optimistic locking (`version` column).

## Consequences
- One consistent status for all roles and a complete audit history.
- Other modules depend on `OrderService` instead of the orders table.
- New transitions require a change to `OrderService` and the contract.
