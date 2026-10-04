## Summary
<!-- What does this change and why? -->

## Screen / area
<!-- e.g. S2 · Review and confirm, or backend module -->

## How it was tested
<!-- Tests added, manual steps, screenshots for UI changes -->

## Checklist
- [ ] Targets `develop`; CI passes
- [ ] Tests cover new business rules
- [ ] New tables use a new migration in the module's range; no existing migration edited
- [ ] Order status changes go through `OrderService`; time comes from `DemoClock`
- [ ] UI text uses i18n; shared API client and UI components used
- [ ] No dataset files or dataset values committed; no secrets
