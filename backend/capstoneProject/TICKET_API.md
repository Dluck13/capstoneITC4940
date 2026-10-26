# Ticket API: complete ticket lifecycle and comments

Nine endpoints support creation, retrieval, assignment, starting work, comments,
resolution, and closure. User administration and statistics are not implemented.

## Authentication and CSRF

Base URL: http://localhost:8080. Allowed frontend origin: http://localhost:5173.

Use credentials: "include" for fetch, or withCredentials: true for Axios, on every
request. The browser sends the HttpOnly JSESSIONID cookie automatically.

1. GET /api/auth/csrf to obtain headerName and token.
2. POST /api/auth/login with email/password JSON, the cookie, and that CSRF header.
3. GET /api/auth/csrf again after login because login rotates the token.
4. Send X-CSRF-TOKEN with POST and PATCH requests. GET requests do not need that header.
5. After session expiry or logout, log in again and obtain a fresh CSRF token.

No bearer token or client-supplied user ID is used. Existing session behavior and
database configuration are unchanged.

## POST /api/tickets

Available to END_USER, TECHNICIAN, and ADMIN. Content-Type: application/json.

Request:

```json
{
  "title": "Cannot connect to campus Wi-Fi",
  "description": "My laptop cannot connect in the library.",
  "priority": "MEDIUM",
  "categoryId": 3
}
```

All four fields are required. Title must be nonblank and at most 150 characters.
Description must be nonblank and at most 10,000 characters. Priority must be one of
LOW, MEDIUM, HIGH (uppercase). categoryId must be positive and refer to an existing,
active category. There is no category-list endpoint in this step; use IDs from
existing category data.

The server determines requester from the session, sets status to OPEN, and leaves
assignedTechnician null. Additional JSON fields are ignored, including requesterId,
requester, status, assignedToId, and id. They cannot override server-owned fields.

Success: 201 Created with Location: /api/tickets/42 and a detail response:

```json
{
  "id": 42,
  "title": "Cannot connect to campus Wi-Fi",
  "description": "My laptop cannot connect in the library.",
  "priority": "MEDIUM",
  "status": "OPEN",
  "requester": { "id": 7, "name": "Example Student" },
  "assignedTechnician": null,
  "category": { "id": 3, "name": "Network" },
  "createdAt": "2026-10-18T10:30:00",
  "updatedAt": "2026-10-18T10:30:00",
  "resolvedAt": null,
  "resolutionNotes": null
}
```

## GET /api/tickets

Success: 200 OK with a JSON array, or [] if no visible tickets exist.

END_USER receives only their own tickets, selected by the authenticated ID in the
database query. TECHNICIAN and ADMIN receive all tickets for support operations.
There is no pagination or guaranteed ordering in this step.

```json
[
  {
    "id": 42,
    "title": "Cannot connect to campus Wi-Fi",
    "priority": "MEDIUM",
    "status": "OPEN",
    "requester": { "id": 7, "name": "Example Student" },
    "assignedTechnician": null,
    "category": { "id": 3, "name": "Network" },
    "createdAt": "2026-10-18T10:30:00"
  }
]
```

## GET /api/tickets/{id}

Success: 200 OK with the same detail structure shown for creation.
END_USER may read only their own tickets. TECHNICIAN and ADMIN may read any ticket.
Ownership is loaded from the database; changing the URL cannot bypass it.
Missing tickets return 404. Existing tickets owned by another end user return 403.

## Errors

| Status | Meaning |
| --- | --- |
| 400 | Invalid input/category/technician, disallowed transition, malformed JSON, or nonnumeric ticket ID |
| 401 | No authenticated session (with a valid CSRF token when required) |
| 403 | Another user's ticket, invalid/missing CSRF token, or insufficient permission |
| 404 | Requested ticket does not exist |

A request missing both authentication and CSRF may receive 403 because CSRF is
checked first. Error bodies use the existing status/message convention:

```json
{
  "status": 400,
  "message": "Invalid ticket request; check fields, active category, target technician, status transition, and comment text or closed status"
}
```

Denied access uses {"status":403,"message":"Access denied"}; missing tickets use
{"status":404,"message":"Ticket not found"}.

## Display fields for Nashon

For list rows, use id as the React key and detail-link identifier, title as the
subject, priority/status for badges, requester.name, assignedTechnician?.name
(or "Unassigned"), category?.name, and createdAt.

For the detail view, additionally display description, updatedAt, and, when present,
resolvedAt and resolutionNotes. Existing tickets can have a null category or assignee.
Account objects contain only id and name; passwords, password hashes, and emails are
never included. Timestamps follow the existing LocalDateTime model and have no UTC
offset; do not assume the strings represent UTC.


## PATCH /api/tickets/{id}/assign

Send the session cookie, Content-Type: application/json, and X-CSRF-TOKEN.
END_USER receives 403. TECHNICIAN may target only their own ID. ADMIN may target
any active technician. Legacy TECH roles are accepted as TECHNICIAN.

Request:

```json
{ "technicianId": 12 }
```

The target must exist, be active, and have the technician role. Missing, inactive,
or non-technician targets return 400. A technician targeting someone else receives
403. A missing ticket returns 404 for an otherwise authorized request.

OPEN becomes ASSIGNED. ASSIGNED and IN_PROGRESS tickets may be reassigned without
changing status. RESOLVED and CLOSED tickets cannot be assigned (400).
Unassignment is not supported.

Success: 200 OK with the complete detail DTO:

```json
{
  "id": 42,
  "title": "Cannot connect to campus Wi-Fi",
  "description": "My laptop cannot connect in the library.",
  "priority": "MEDIUM",
  "status": "ASSIGNED",
  "requester": { "id": 7, "name": "Example Student" },
  "assignedTechnician": { "id": 12, "name": "Example Technician" },
  "category": { "id": 3, "name": "Network" },
  "createdAt": "2026-10-18T10:30:00",
  "updatedAt": "2026-10-18T11:00:00",
  "resolvedAt": null,
  "resolutionNotes": null
}
```

## PATCH /api/tickets/{id}/status

Send the session cookie, Content-Type: application/json, and X-CSRF-TOKEN.
Only TECHNICIAN and ADMIN may call this endpoint.

Request:

```json
{ "status": "IN_PROGRESS" }
```

Only ASSIGNED -> IN_PROGRESS is supported, and assignedTechnician must be present.
Both support roles may start work on any assigned support ticket; the actor does
not have to be the assignee.

Success: 200 OK with the complete detail DTO, for example:

```json
{
  "id": 42,
  "title": "Cannot connect to campus Wi-Fi",
  "description": "My laptop cannot connect in the library.",
  "priority": "MEDIUM",
  "status": "IN_PROGRESS",
  "requester": { "id": 7, "name": "Example Student" },
  "assignedTechnician": { "id": 12, "name": "Example Technician" },
  "category": { "id": 3, "name": "Network" },
  "createdAt": "2026-10-18T10:30:00",
  "updatedAt": "2026-10-18T11:05:00",
  "resolvedAt": null,
  "resolutionNotes": null
}
```

| Transition | Operation |
| --- | --- |
| OPEN -> ASSIGNED | Assignment endpoint only |
| ASSIGNED -> IN_PROGRESS | Status endpoint; assignee required |
| IN_PROGRESS -> RESOLVED | PATCH /api/tickets/{id}/resolve; technician/admin; notes required |
| RESOLVED -> CLOSED | PATCH /api/tickets/{id}/close; admin only; stored notes and resolvedAt required |
| Backward, skipped, same-status, or unknown transitions | Rejected |

Invalid transitions return 400. A technician requesting CLOSED receives 403;
an administrator requesting CLOSED receives 400 because closure must use the
dedicated close endpoint. No role can resolve through this general status endpoint, even if
extra resolution notes are sent. RESOLVED and CLOSED tickets cannot change status
here, and CLOSED cannot reopen. Missing tickets return 404.

Both PATCH routes return 401 without authentication when a valid CSRF token is
present, and 403 for missing/invalid CSRF. PATCH is allowed by CORS only for the
existing frontend origin; credentials and allowed headers are unchanged.

## Nashon's assignment and status UI

Use the detail id in both URLs. For technician self-assignment, use the id from
/api/auth/me as technicianId. Administrators must choose an existing active
technician ID; no user-directory endpoint is added in this step.

Include credentials: "include" (fetch) or withCredentials: true (Axios), JSON
content type, and the fresh post-login CSRF token on each PATCH. Successful PATCH
requests do not rotate that token. Replace the displayed ticket with the returned
detail DTO, particularly assignedTechnician, status, and updatedAt.

Offer Start Work only for ASSIGNED tickets with an assignee. Hide assignment and
status controls for END_USER. Use the dedicated resolve and close controls described below. These UI rules
are conveniences; the service independently enforces permissions and transitions.

The secured service methods enforce permissions when called directly through
the Spring bean. Generic V1 save/update/delete methods remain available for trusted
internal calls without a security context, but reject callers with authentication
so they cannot bypass the workflow. No generic mutation route is exposed.
Ticket row locks serialize the new workflow writes without a schema change.


## GET /api/tickets/{id}/comments

Requires a session cookie. No CSRF header is needed for GET.
Ownership rules are identical to ticket details: END_USER may read only their own
tickets; TECHNICIAN and ADMIN may read all tickets under the current support policy.
A missing ticket returns 404; another end user's ticket returns 403.

Success: 200 OK with an array sorted by createdAt ascending, then id ascending
for equal timestamps. A ticket without comments returns [].
Comments on CLOSED tickets remain readable.

```json
[
  {
    "id": 81,
    "text": "I have tried restarting the laptop.",
    "authorId": 7,
    "authorName": "Example Student",
    "createdAt": "2026-10-18T12:00:00"
  }
]
```

## POST /api/tickets/{id}/comments

Requires a session cookie, Content-Type: application/json, and a valid X-CSRF-TOKEN.
END_USER may comment only on their own tickets; TECHNICIAN and ADMIN may comment
on tickets they can view. All roles are blocked from adding comments to CLOSED
tickets (400). RESOLVED tickets remain commentable until closed.

Request:

```json
{ "text": "I have tried restarting the laptop." }
```

Text is required, nonblank, and limited to 10,000 characters. The service validates
text too, so direct service calls cannot bypass validation. The author always comes
from the authenticated session and must still be an active database user. Additional
request fields such as userId, authorId, or user are ignored and cannot override it.

Success: 201 Created:

```json
{
  "id": 81,
  "text": "I have tried restarting the laptop.",
  "authorId": 7,
  "authorName": "Example Student",
  "createdAt": "2026-10-18T12:00:00"
}
```

Only these five fields are returned. No email, password, password hash, or full user
entity is exposed. Creating a comment does not change ticket status or updatedAt.

| Status | Comment endpoint meaning |
| --- | --- |
| 200 | Comment list returned |
| 201 | Comment created |
| 400 | Missing/blank/oversized text, malformed request or ticket ID, or ticket is CLOSED |
| 401 | Authentication required (a POST without CSRF may receive 403 first) |
| 403 | Ticket ownership/role denied, inactive author, or missing/invalid CSRF |
| 404 | Ticket does not exist |

Errors retain the existing JSON status/message format.

### Nashon's comment UI

Use the selected ticket's id to build the URL. Fetch the comment list with
credentials: "include" (or Axios withCredentials: true). Render text as plain text,
authorName as the label, createdAt as the timestamp, and id as the React key.
Do not use raw HTML rendering for user-entered text.

POST only {"text":"..."} with the same credentials option and the fresh post-login
CSRF token. Do not send the logged-in user's ID as the author. After a successful
POST, refetch the list to preserve server ordering and show other users' replies.
Show existing history but disable the composer for CLOSED tickets. The backend
enforces the same rule regardless of frontend controls.

The comment service checks database ticket ownership with the existing TicketAccess
policy. Comment creation takes the same ticket row lock as other workflow writes;
resolution and closure use that same locking discipline. Generic V1 comment mutations
remain available to trusted internal callers without authentication and reject
session callers, matching the existing ticket-service boundary.


## PATCH /api/tickets/{id}/resolve

Roles: TECHNICIAN or ADMIN. A technician may resolve any IN_PROGRESS support ticket
under the existing access policy; being the assigned technician is not required.
Requires the authenticated session cookie and a valid X-CSRF-TOKEN.

Request (Content-Type: application/json):

```json
{ "resolutionNotes": "Replaced the faulty cable and verified connectivity." }
```

Notes must be nonblank and at most 10,000 characters. The service also validates
them when invoked directly. Only IN_PROGRESS can become RESOLVED. The server
records resolvedAt using its current time, saves the notes, and updates updatedAt.
Repeated resolution is rejected without replacing the original resolution evidence.

Success: 200 OK with the complete ticket detail DTO:

```json
{
  "id": 42,
  "title": "Cannot connect to campus Wi-Fi",
  "description": "My laptop cannot connect in the library.",
  "priority": "MEDIUM",
  "status": "RESOLVED",
  "requester": { "id": 7, "name": "Example Student" },
  "assignedTechnician": { "id": 12, "name": "Example Technician" },
  "category": { "id": 3, "name": "Network" },
  "createdAt": "2026-10-18T10:30:00",
  "updatedAt": "2026-10-18T12:00:00",
  "resolvedAt": "2026-10-18T12:00:00",
  "resolutionNotes": "Replaced the faulty cable and verified connectivity."
}
```

## PATCH /api/tickets/{id}/close

Role: ADMIN only. Requires the session cookie and a valid X-CSRF-TOKEN.
No request body is required. If the frontend always sends JSON, it may send {}.
Request body fields are not used to set status, notes, or timestamps.

The ticket must already be RESOLVED, with nonblank stored resolutionNotes and a
nonnull resolvedAt. Closure changes only status to CLOSED and updates updatedAt;
resolutionNotes and resolvedAt are preserved. Repeated closure, closure from any
other state, and reopening are rejected.

Success: 200 OK with the complete ticket detail DTO:

```json
{
  "id": 42,
  "title": "Cannot connect to campus Wi-Fi",
  "description": "My laptop cannot connect in the library.",
  "priority": "MEDIUM",
  "status": "CLOSED",
  "requester": { "id": 7, "name": "Example Student" },
  "assignedTechnician": { "id": 12, "name": "Example Technician" },
  "category": { "id": 3, "name": "Network" },
  "createdAt": "2026-10-18T10:30:00",
  "updatedAt": "2026-10-18T12:15:00",
  "resolvedAt": "2026-10-18T12:00:00",
  "resolutionNotes": "Replaced the faulty cable and verified connectivity."
}
```

### Resolution and closure errors

| Status | Meaning |
| --- | --- |
| 200 | Operation completed; updated detail DTO returned |
| 400 | Invalid transition, repeated operation, invalid notes, invalid ticket ID, or missing stored resolution evidence |
| 401 | Authentication required (missing CSRF may produce 403 first) |
| 403 | Role denied or missing/invalid CSRF |
| 404 | Ticket does not exist for an authorized, valid request |

Errors use the existing {"status":400,"message":"..."} format.
The general /status endpoint still permits only ASSIGNED -> IN_PROGRESS. It cannot
resolve or close, including when extra notes are supplied. Assignment cannot alter
RESOLVED or CLOSED tickets. Existing comments remain readable after closure;
new comments are prohibited.

### Nashon's resolution and closure UI

Use credentials: "include" or Axios withCredentials: true, and the current
post-login X-CSRF-TOKEN on both PATCH requests. For resolution, also send JSON
content type and resolutionNotes. Do not send your own timestamps.

Offer Resolve only to TECHNICIAN/ADMIN on IN_PROGRESS tickets, with a required
notes input. Offer Close only to ADMIN on RESOLVED tickets with saved resolution
notes and resolvedAt. Replace local ticket data with the response and display
resolutionNotes as plain text, resolvedAt as resolution time, and updatedAt as
last ticket update time. These existing LocalDateTime values carry no UTC offset.
Keep the comments history visible for CLOSED tickets but disable new comments.

The enforced sequence is OPEN -> ASSIGNED -> IN_PROGRESS -> RESOLVED -> CLOSED.
Each operation checks its source state and role within the service transaction.
Resolution, closure, assignment, status changes, and comment creation share the
ticket row lock. Generic V1 mutations remain blocked for authenticated callers.

## Tests

[TicketApiIntegrationTest](src/test/java/com/itsupportoperationsdashboard/capstoneProject/ticket/TicketApiIntegrationTest.java)
uses real MySQL repositories, session login, security filters, and MVC endpoints.
Each test rolls back its database changes. Run .\\mvnw.cmd clean verify.
