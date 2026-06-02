# Cloud Deployment Guide

This project is ready for a split deployment:

- Frontend on Vercel
- Backend on Render
- Database on TiDB Cloud Starter

## Architecture

```text
Vercel static frontend
  -> calls configured backend API base
Render Spring Boot backend
  -> connects with JDBC
TiDB Cloud Starter
```

## 1. Prepare TiDB Cloud Starter

1. Create a TiDB Cloud Starter cluster.
2. Create a database named `vehicle_wallpaper`.
3. Copy the public host, port, username, and password from the TiDB connection page.
4. Import the current project data if you want the same records as local development.

Import with the MySQL client from the repository root:

```powershell
& "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" `
  -h your_tidb_host `
  -P 4000 `
  -u your_tidb_user `
  -p `
  --ssl-mode=VERIFY_IDENTITY `
  vehicle_wallpaper
```

Then run:

```sql
SOURCE backend/database/vehicle_wallpaper.sql;
```

Notes:

- TiDB Cloud Starter is MySQL-compatible, so the existing Spring Boot MySQL driver works.
- If you skip the import, Flyway will still create the tables automatically and the backend will still sync brands and wallpapers from `cars/` at startup.
- The SQL import is mainly useful if you want to keep the current sample feedback records.

## 2. Deploy The Backend To Render

This repository now includes a root [Dockerfile](D:\Codes\vscodecodes\Web前端课程设计\Dockerfile:1) that builds the Spring Boot backend and bundles the static site files plus `cars/`.

Create a Render web service from this GitHub repository and use these environment variables:

```text
APP_PROFILE=mysql
MYSQL_URL=jdbc:mysql://your_tidb_host:4000/vehicle_wallpaper?sslMode=VERIFY_IDENTITY&enabledTLSProtocols=TLSv1.2&serverTimezone=Asia/Shanghai&characterEncoding=utf8
MYSQL_USER=your_tidb_user
MYSQL_PASSWORD=your_tidb_password
ADMIN_API_KEY=your_strong_admin_api_key
ADMIN_USERNAME=your_admin_username
ADMIN_PASSWORD=your_admin_password
ADMIN_TOKEN_SECRET=your_admin_token_secret
ADMIN_TOKEN_TTL_HOURS=12
ADMIN_MAX_FAILED_ATTEMPTS=5
ADMIN_LOCK_MINUTES=15
APP_FRONTEND_ROOT_PATH=/app/site
APP_CATALOG_ROOT_PATH=/app/site/cars
APP_CATALOG_SYNC_ON_STARTUP=true
```

Login fallback behavior:

- If `ADMIN_USERNAME` is omitted but `ADMIN_API_KEY` exists, the login username defaults to `admin`
- If `ADMIN_PASSWORD` is omitted but `ADMIN_API_KEY` exists, the login password defaults to the API key
- The admin page can use either bearer-token login or the legacy API key fallback
- Dedicated admin passwords are stored as BCrypt hashes in the database
- `ADMIN_MAX_FAILED_ATTEMPTS` and `ADMIN_LOCK_MINUTES` control temporary lockouts for repeated failures
- `ADMIN_TOKEN_SECRET` signs bearer-token sessions, and `Log out all sessions` invalidates all active tokens for that admin account

Recommended Render settings:

1. Service type: Web Service
2. Source: connect the GitHub repository
3. Language: `Docker`
4. Dockerfile Path: repository root `Dockerfile`
5. Health check path: `/actuator/health`
6. Instance type: `Free`
7. Region: choose the closest available region to your users and TiDB cluster

What the Docker image does:

- Builds `backend/target/backend-0.0.1-SNAPSHOT.jar`
- Copies the public site HTML, CSS, JS, and `cars/` assets into `/app/site`
- Starts Spring Boot with the Render-provided `PORT`

Important free-tier notes for Render:

- Free web services spin down after 15 minutes of inactivity.
- A cold start can take about one minute when the next request wakes the service up.
- Render provides 750 free instance hours per workspace each calendar month.

## 3. Point The Frontend To The Backend

The frontend now reads its backend origin from [api-config.js](D:\Codes\vscodecodes\Web前端课程设计\api-config.js:1).

Before deploying to Vercel, update:

```js
window.VEHICLE_WALLPAPER_CONFIG = {
    apiBase: "https://your-render-service.onrender.com"
};
```

That one file controls both:

- public catalog and feedback APIs used by `index.html`
- admin APIs used by `admin.html`

If you leave `apiBase` empty:

- local file preview still falls back to `http://localhost:8080`
- deployed static pages will call same-origin `/api/...`

## 4. Deploy The Frontend To Vercel

1. Keep the project connected to Vercel as a static site.
2. Make sure [api-config.js](D:\Codes\vscodecodes\Web前端课程设计\api-config.js:1) contains the Render backend URL.
3. Redeploy Vercel.

The current [vercel.json](D:\Codes\vscodecodes\Web前端课程设计\vercel.json:1) already disables aggressive caching for `api-config.js`, so backend-origin changes propagate more predictably.

## 5. Verify Production

Check these URLs after deployment:

1. `https://your-render-service.onrender.com/actuator/health`
2. `https://your-render-service.onrender.com/api/catalog`
3. `https://your-vercel-site.vercel.app/`
4. `https://your-vercel-site.vercel.app/admin.html`

Admin verification steps:

1. Open the admin page on Vercel.
2. Sign in with the username and password configured in Render.
3. Or expand the fallback section and enter the same `ADMIN_API_KEY` configured in Render.
4. Confirm dashboard metrics load.
5. Confirm the public homepage brands and wallpaper cards load from `/api/catalog`.
6. Confirm wallpaper list, feedback list, and operation logs load.
7. Use `Log out all sessions`, then verify the old bearer login is rejected.
8. Submit a public feedback form on the frontend and confirm it appears in admin review.

## Daily Update Workflow

When you add or replace wallpaper files later:

1. Update the repository files under `cars/`
2. Redeploy Render so the new image files are included in the backend container
3. Redeploy Vercel so the static frontend also has the same image assets
4. Open the admin page and click `Run catalog sync`

This keeps:

- frontend static assets
- frontend dynamic catalog rendering
- backend-served assets
- database metadata

in sync with each other.
