# Store UI integration and remaining issues

The store screens use the live API. Sign-in and notifications also use the live API. Demo records are disabled by default (`SEED_ENABLED=false`, `DEMO_MODE=false`); existing database rows are not deleted by this change.

## Review order

These branches are stacked; each later branch includes the earlier store work:

1. `feature/store-confirm`
2. `feature/store-new-order-ui`
3. `feature/store-arrival-ui`
4. `feature/issues-ui`
5. `feature/store-settings-ui`
6. `feature/store-receipt-ui`
7. `feature/store-moved-ui`
8. `feature/store-delivery-problems-ui`
9. `feature/store-production-integration` (also merges the current `develop` notification backend and jobs)

## Backend work required before production release

1. `GET /api/store/deliveries` currently returns `null` for arrival, deferral, delivery proof, and shortfall. F7 through F10 display real values only when those services publish them. The receipt screen does not permit confirmation without a delivery proof. Connect planning, driver proof, shortfall, and driver offline data to this endpoint.
2. The frontend calls `POST /api/store/deferrals/{id}/choice`, `/api/store/failed/{id}/choice`, and `/api/store/breakdown/{id}/choice` when the relevant record has an id. These endpoints are not present in the current backend. Implement them and validate outlet ownership and allowed transitions.
3. The new notification inbox works, but order, delivery, and issue actions currently do not all publish notifications. Alert preference endpoints were removed from `develop`; settings now explains that preferences are unavailable. Add notification events and preference persistence before offering switches again. Backend notification title/body are English only; localized notification content needs a server contract.
4. With demo seeding off, a new database needs operational provisioning of user accounts, outlets, and reference data. Supply a production import and credentials process before deployment. Do not turn on demo seeding to populate a production database.
5. Other dispatcher views still contain illustrative metrics and data outside the store feature area. Audit and connect those views before calling the entire application production ready.
6. The optional F13 style/technology variants in `shaanil.md` are not implemented.

## Verification limits

- Frontend lint, build, and live API client tests passed. Backend compiles and the real-time clock has isolated tests.
- A full integration run needs a PostgreSQL database with migrations and live planning/driver services. No production database was modified or purged.
