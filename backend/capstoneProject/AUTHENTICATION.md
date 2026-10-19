# V2 authentication

This document describes the authentication milestone. Ticket creation, listing,
and details are now implemented; see [TICKET_API.md](TICKET_API.md) for the current
ticket contract. Statements below about ticket routes being denied and predicates
awaiting integration describe the earlier milestone. The three new ticket service
entry points now enforce role and ownership checks; other ticket workflows remain
unimplemented and denied.

## Approach

Spring Security authenticates email/password against the existing UserRepository and
PasswordConfig BCrypt encoder. Authentication uses a server-side HTTP session and an
HttpOnly JSESSIONID cookie. No JWT secret, public registration, or new database tables
are required. Login rotates the session ID and clears the old CSRF token; logout
invalidates the session. Passwords are never returned, and the authenticated session
principal has its password hash erased.

The session expires after 30 minutes of inactivity. Sessions are in memory, so a
server restart logs users out. User details and roles are snapshots taken at login;
changes to a user's active flag, role, or password take effect at their next login.
Immediate revocation of existing sessions is not implemented in this step.

## Local configuration

Set DB_USERNAME and DB_PASSWORD in the backend process environment before starting
the application or running the tests. There are no credential defaults in
[application.properties](src/main/resources/application.properties).
Use the existing local MySQL credentials for it_ticket_management.

For Git Bash, enter credentials without putting the password in command history:

```bash
read -r -p "MySQL username: " DB_USERNAME
read -r -s -p "MySQL password: " DB_PASSWORD
echo
export DB_USERNAME DB_PASSWORD
./mvnw.cmd clean verify
```

Local HTTP uses a SameSite=Lax, HttpOnly cookie. For an HTTPS deployment set
SESSION_COOKIE_SECURE=true and configure the exact deployed frontend origin.
Use localhost consistently for local frontend and backend URLs.

## API contract

All requests from React must use credentials: "include". The allowed browser origin
is http://localhost:5173. Authentication responses use JSON and are not cacheable
through Spring Security's default cache-control headers.

| Method | Endpoint | Result |
| --- | --- | --- |
| GET | /api/auth/csrf | 200: {"headerName":"X-CSRF-TOKEN","token":"..."} |
| POST | /api/auth/login | 200: {"id":7,"name":"Example","email":"user@example.com","role":"END_USER"} |
| GET | /api/auth/me | 200: same public profile; 401 without authentication |
| POST | /api/auth/logout | 204, session invalidated |

1. Get /api/auth/csrf, retaining its session cookie.
2. Send JSON {"email":"...","password":"..."} to /api/auth/login with the token in
   the returned headerName. Login requires CSRF protection too.
3. After login, get /api/auth/csrf again because login invalidates the old token.
4. Get /api/auth/me to restore frontend identity after a page reload.
5. Send the current CSRF token with logout and future state-changing requests.
   After logout or expiry, fetch a new token before another login.

Wrong passwords, unknown email addresses, inactive users, and unsupported roles all
receive 401 with {"status":401,"message":"Invalid email or password"}.
Malformed login requests receive 400 with {"status":400,"message":"Invalid login request"}.
Missing authentication receives 401; denied access or invalid/missing CSRF tokens
receive 403. Error bodies contain only status and message. Logout has no response body.

## Authorization boundary for this step

[SecurityConfig](src/main/java/com/itsupportoperationsdashboard/capstoneProject/config/SecurityConfig.java)
allows only the authentication routes. All other routes are denied, including
ticket routes and registration. Method security is enabled.

[TicketAccess](src/main/java/com/itsupportoperationsdashboard/capstoneProject/security/TicketAccess.java)
provides tested authorization predicates for the next ticket implementation:

| Permission | END_USER | TECHNICIAN | ADMIN |
| --- | --- | --- | --- |
| Create | Yes | Yes | Yes |
| View/comment | Own tickets | All support tickets | All tickets |
| Assign | No | To self | To any technician |
| Update status/resolve | No | Yes | Yes |
| Close | No | No | Yes |

The existing string role field is retained. Legacy USER maps to END_USER and TECH
maps to TECHNICIAN at login; stored values are unchanged. Unknown roles fail login.

The V1 persistence services remain unchanged and are not exposed over HTTP. The
predicates do not yet secure ticket workflows because those workflows are outside
this step. When implementing them, apply method authorization at the new entry
points, obtain requester identity from TicketAccess.currentUserId(authentication),
and obtain ticket ownership from the database (never a client-supplied owner ID).
Filter end-user ticket lists to that identity. Assignment must additionally load
and validate an active technician target. Status validation must route closure
through canClose; canUpdateStatus alone must never authorize closure. Do not expose
the unrestricted V1 save/update methods directly as REST endpoints.

## Verification

Run ./mvnw.cmd clean verify with the MySQL environment configured. The original
11 tests remain unchanged and use the configured MySQL database.
[AuthenticationTest](src/test/java/com/itsupportoperationsdashboard/capstoneProject/security/AuthenticationTest.java)
adds 15 tests using the real Spring Security filter chain, authentication provider,
BCrypt encoder, MVC controllers, and method-security proxy with a mocked user repository.
Coverage includes valid and invalid logins, inactive accounts, unknown/legacy roles,
public-profile serialization, credential erasure, CSRF/session rotation, logout,
CORS, denied routes, ownership, and role enforcement.
