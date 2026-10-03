# 4. Built-in JWT authentication

- Status: Accepted
- Date: 2026-10-03

## Context
Four roles sign in differently: store managers and dispatchers with email and password, drivers with staff ID and PIN, loaders with depot and PIN on a shared tablet. Every request must be scoped to the user's outlet, depot or vehicle.
Hosted identity providers (Auth0, Firebase, Cognito) and a self-hosted Keycloak were considered. They add external setup, accounts or another container, and fit PIN sign-in on shared devices poorly.

## Decision
- The backend issues its own signed JWTs (HS256, secret from `JWT_SECRET`) from `POST /api/auth/login`.
- Requests are verified with Spring Security's OAuth2 resource server support.
- Secrets and PINs are stored as BCrypt hashes.
- Access is enforced by role per path, plus data-scope checks through a shared `CurrentUser` helper.
- Unauthenticated requests get 401, wrong roles 403; data from another outlet, depot or vehicle returns 404.

## Consequences
- No external dependency; works with offline-first clients and on any server.
- Sign-up, password reset, refresh tokens and SSO are out of scope.
- Token lifetime is long enough for a shift (12 h desk roles, 16 h drivers); revoking a token early is not supported.
