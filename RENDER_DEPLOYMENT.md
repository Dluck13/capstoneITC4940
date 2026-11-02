# Render Deployment (Docker) — IT Support Operations Dashboard

This project is prepared to deploy as **one Render Web Service** from the repository root, running both frontend and backend from a single Spring Boot application.

## Architecture

- React/Vite frontend is built in Docker and copied into Spring Boot static resources.
- Spring Boot serves both:
  - frontend assets and SPA routes
  - backend APIs under `/api/**`
- Session auth (`JSESSIONID`), CSRF protection, BCrypt password hashing, and role authorization are preserved.

## Required Render Service Settings

- **Environment**: Docker
- **Root Directory**: repository root (`C:\Users\dimit\capstone` in local path terms)
- **Dockerfile Path**: `./Dockerfile`
- **Auto Deploy**: optional, per your workflow
- **Health Check Path**: `/login` (or `/`)

## Required Environment Variables

Set these in Render for the web service:

- `SPRING_PROFILES_ACTIVE=prod`
- `SPRING_DATASOURCE_URL=<your-mysql-jdbc-url>`
- `SPRING_DATASOURCE_USERNAME=<your-mysql-username>`
- `SPRING_DATASOURCE_PASSWORD=<your-mysql-password>`

Optional:

- `PORT` (Render usually injects this automatically)
- `APP_CORS_ALLOWED_ORIGINS` only if you need additional cross-origin access; default is local dev origin.

## MySQL Connection Requirements

- MySQL must be reachable from Render network.
- JDBC URL should reference your hosted MySQL instance and database (`it_ticket_management`).
- Ensure database user has required read/write permissions.
- Keep the existing schema; no schema changes are required for deployment prep.

## Deployment Steps

1. Push current repository state to your Git provider.
2. In Render, create a new **Web Service** from the repository.
3. Select Docker deploy mode and confirm Dockerfile path is `./Dockerfile`.
4. Add required environment variables.
5. Deploy.
6. After deploy, open service URL and test:
   - `/login`
   - `/register`
   - `/`
   - `/tickets/new`
   - `/admin/users/new` (as ADMIN)

## Verification Checklist

After deployment:

1. Login succeeds and sets `JSESSIONID` cookie.
2. CSRF flow works (`GET /api/auth/csrf` + state-changing requests).
3. Dashboard and ticket routes refresh directly without 404.
4. `/api/**` endpoints remain functional.
5. Existing ticket lifecycle behavior is unchanged.
6. Theme toggle (light/dark mode) still works.

## Local Development Impact

- Existing local development still works:
  - Frontend dev server on `http://localhost:5173`
  - Backend on `http://localhost:8080`
- In dev mode, frontend continues using `http://localhost:8080` as API base fallback.
- In production build/runtime, frontend uses same-origin `/api` calls.
