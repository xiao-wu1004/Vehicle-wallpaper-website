# Cloud Deployment Guide

This project is ready for a split deployment:

- Frontend on Vercel
- Backend on Koyeb
- Database on TiDB Cloud Starter

## Architecture

```text
Vercel static frontend
  -> calls configured backend API base
Koyeb Spring Boot backend
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

## 2. Deploy The Backend To Koyeb

This repository now includes a root [Dockerfile](D:\Codes\vscodecodes\Web前端课程设计\Dockerfile:1) that builds the Spring Boot backend and bundles the static site files plus `cars/`.

Create a Koyeb web service from this GitHub repository and use these environment variables:

```text
APP_PROFILE=mysql
MYSQL_URL=jdbc:mysql://your_tidb_host:4000/vehicle_wallpaper?sslMode=VERIFY_IDENTITY&enabledTLSProtocols=TLSv1.2&serverTimezone=Asia/Shanghai&characterEncoding=utf8
MYSQL_USER=your_tidb_user
MYSQL_PASSWORD=your_tidb_password
ADMIN_API_KEY=your_strong_admin_api_key
APP_FRONTEND_ROOT_PATH=/app/site
APP_CATALOG_ROOT_PATH=/app/site/cars
APP_CATALOG_SYNC_ON_STARTUP=true
```

Recommended Koyeb settings:

1. Runtime: Dockerfile from repository root
2. Service type: Web Service
3. Health check path: `/actuator/health`
4. Region: choose the closest region to your users

What the Docker image does:

- Builds `backend/target/backend-0.0.1-SNAPSHOT.jar`
- Copies the public site HTML, CSS, JS, and `cars/` assets into `/app/site`
- Starts Spring Boot with the Koyeb-provided `PORT`

## 3. Point The Frontend To The Backend

The frontend now reads its backend origin from [api-config.js](D:\Codes\vscodecodes\Web前端课程设计\api-config.js:1).

Before deploying to Vercel, update:

```js
window.VEHICLE_WALLPAPER_CONFIG = {
    apiBase: "https://your-koyeb-service.koyeb.app"
};
```

That one file controls both:

- public feedback APIs used by `main.html`
- admin APIs used by `admin.html`

If you leave `apiBase` empty:

- local file preview still falls back to `http://localhost:8080`
- deployed static pages will call same-origin `/api/...`

## 4. Deploy The Frontend To Vercel

1. Keep the project connected to Vercel as a static site.
2. Make sure [api-config.js](D:\Codes\vscodecodes\Web前端课程设计\api-config.js:1) contains the Koyeb backend URL.
3. Redeploy Vercel.

The current [vercel.json](D:\Codes\vscodecodes\Web前端课程设计\vercel.json:1) already disables aggressive caching for `api-config.js`, so backend-origin changes propagate more predictably.

## 5. Verify Production

Check these URLs after deployment:

1. `https://your-koyeb-service.koyeb.app/actuator/health`
2. `https://your-koyeb-service.koyeb.app/api/catalog`
3. `https://your-vercel-site.vercel.app/main.html`
4. `https://your-vercel-site.vercel.app/admin.html`

Admin verification steps:

1. Open the admin page on Vercel.
2. Enter the same `ADMIN_API_KEY` that you configured in Koyeb.
3. Confirm dashboard metrics load.
4. Confirm wallpaper list and feedback list load.
5. Submit a public feedback form on the frontend and confirm it appears in admin review.

## Daily Update Workflow

When you add or replace wallpaper files later:

1. Update the repository files under `cars/`
2. Redeploy Koyeb so the new image files are included in the backend container
3. Redeploy Vercel so the static frontend also has the same image assets
4. Open the admin page and click `Run catalog sync`

This keeps:

- frontend static assets
- backend-served assets
- database metadata

in sync with each other.
