# Vehicle Wallpaper Backend

Java backend for the vehicle wallpaper website. The project is ready to open directly in IntelliJ IDEA with Maven Wrapper.

## Stack

- Java 8
- Spring Boot 2.7
- Spring Web
- Spring Data JPA
- Flyway
- H2 for local default development
- MySQL 8 for formal database deployment

## What The Backend Does

- Serves the existing frontend files from the repository root
- Serves wallpaper assets under `cars/`
- Exposes wallpaper catalog APIs
- Drives the public gallery from `/api/catalog` instead of hard-coded HTML blocks
- Stores feedback messages in the database
- Syncs wallpaper metadata from the filesystem into database tables on startup
- Protects the admin console with database-backed accounts, hashed passwords, session tokens, login throttling, and operation logs

## Database Design

Current core tables are created by Flyway migrations `V1__create_core_tables.sql` and `V2__add_admin_security_tables.sql`.

- `brands`: brand metadata
- `wallpapers`: wallpaper metadata and file URLs
- `feedback_messages`: feedback form submissions
- `admin_accounts`: administrator records, password hashes, and login-throttle state
- `admin_sessions`: issued bearer-token sessions
- `admin_operation_logs`: admin audit trail for content and moderation actions

The backend keeps `cars/` as the source of truth for image files, then writes brand and wallpaper metadata into the database during startup.

## Profiles

The backend defaults to the `h2` profile, so IDEA can run it immediately without extra setup.

- `h2`: local development profile
- `mysql`: MySQL 8 profile

Switch profiles in one of these ways:

```powershell
$env:APP_PROFILE="mysql"
```

or

```powershell
$env:SPRING_PROFILES_ACTIVE="mysql"
```

## Run In IDEA

1. Open `backend/pom.xml` in IntelliJ IDEA.
2. Set the project SDK to Java 8.
3. Run `com.vehiclewallpaper.backend.BackendApplication`.

If you want MySQL, add these environment variables to the run configuration:

```text
APP_PROFILE=mysql
MYSQL_URL=jdbc:mysql://localhost:3306/vehicle_wallpaper?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&characterEncoding=utf8
MYSQL_USER=your_mysql_user
MYSQL_PASSWORD=your_mysql_password
ADMIN_API_KEY=your_admin_api_key
ADMIN_USERNAME=your_admin_username
ADMIN_PASSWORD=your_admin_password
ADMIN_TOKEN_SECRET=your_admin_token_secret
```

## Run In PowerShell

Default H2 mode:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

MySQL mode:

```powershell
$env:APP_PROFILE="mysql"
$env:MYSQL_URL="jdbc:mysql://localhost:3306/vehicle_wallpaper?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&characterEncoding=utf8"
$env:MYSQL_USER="your_mysql_user"
$env:MYSQL_PASSWORD="your_mysql_password"
$env:ADMIN_API_KEY="your_admin_api_key"
$env:ADMIN_USERNAME="your_admin_username"
$env:ADMIN_PASSWORD="your_admin_password"
$env:ADMIN_TOKEN_SECRET="your_admin_token_secret"
cd backend
.\mvnw.cmd spring-boot:run
```

Run tests:

```powershell
cd backend
.\mvnw.cmd test
```

## H2 Data

- H2 file location: `backend/data/vehicle-wallpaper-dev*`
- H2 console: [http://localhost:8080/h2-console](http://localhost:8080/h2-console)
- JDBC URL: `jdbc:h2:file:./data/vehicle-wallpaper-dev`

## MySQL Setup

Create the database first:

```sql
CREATE DATABASE IF NOT EXISTS vehicle_wallpaper
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

After that, start the backend with the `mysql` profile. Flyway will create the tables automatically.

## Import Into MySQL Or Navicat

A verified MySQL dump of the current backend data is stored at:

- `backend/database/vehicle_wallpaper.sql`

What is inside:

- Table structure for `brands`, `wallpapers`, `feedback_messages`, and `flyway_schema_history`
- Current catalog metadata for 12 brands and 35 wallpapers
- Current sample feedback data

Import with MySQL command line:

```powershell
& "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u your_mysql_user -p vehicle_wallpaper
```

Run that command from the repository root, then inside the MySQL client:

```sql
SOURCE backend/database/vehicle_wallpaper.sql;
```

Use it in Navicat:

1. Create or open a MySQL connection.
2. Host: `127.0.0.1`
3. Port: `3306`
4. Username: your local MySQL user
5. Password: your local MySQL password
6. If `vehicle_wallpaper` already exists in MySQL, you can view it directly in Navicat without importing again.
7. If you want to import from file in Navicat, use `Run SQL File...` or `Import Wizard`, then select `backend/database/vehicle_wallpaper.sql`.

Important note:

- Navicat is a database client, not a second database engine.
- The real data is stored in MySQL.
- Navicat either connects to that existing MySQL database directly or imports the same SQL dump into MySQL for you.

## Main APIs

- `GET /api/catalog`
- `GET /api/catalog/brands/{brandSlug}`
- `GET /api/catalog/wallpapers`
- `GET /api/feedback/highlights`
- `POST /api/feedback`

## Admin APIs

These endpoints power the live admin console.

- `GET /api/admin/dashboard`
- `POST /api/admin/catalog/refresh`
- `GET /api/admin/logs`
- `POST /api/admin/brands`
- `PATCH /api/admin/brands/{brandId}`
- `DELETE /api/admin/brands/{brandId}`
- `GET /api/admin/wallpapers`
- `POST /api/admin/wallpapers`
- `PATCH /api/admin/wallpapers/{wallpaperId}`
- `DELETE /api/admin/wallpapers/{wallpaperId}`
- `GET /api/admin/feedback`
- `PATCH /api/admin/feedback/{feedbackId}`
- `GET /api/admin/auth/options`
- `POST /api/admin/auth/login`
- `POST /api/admin/auth/logout-all`

Authentication options:

- Admin APIs accept either a bearer token or the legacy API key fallback.
- Header name: `X-Admin-API-Key`
- Login endpoint: `POST /api/admin/auth/login`
- Bearer header: `Authorization: Bearer <accessToken>`
- Configure the fallback key with environment variable `ADMIN_API_KEY`
- Configure a dedicated login with `ADMIN_USERNAME` and `ADMIN_PASSWORD`
- Optionally configure token signing with `ADMIN_TOKEN_SECRET`
- Token lifetime defaults to `12` hours and can be changed with `ADMIN_TOKEN_TTL_HOURS`
- Passwords are stored as BCrypt hashes in `admin_accounts`
- Failed logins are counted per admin account and temporary lockouts default to `5` attempts and `15` minutes
- `POST /api/admin/auth/logout-all` revokes every active bearer session for the current admin account

Fallback behavior:

- If `ADMIN_USERNAME` is empty but `ADMIN_API_KEY` exists, login defaults to username `admin`
- If `ADMIN_PASSWORD` is empty but `ADMIN_API_KEY` exists, login defaults to the current API key
- If both login and API key are missing, protected admin APIs will return `503 Service Unavailable`
- If the token or API key is missing or invalid, protected admin APIs will return `401 Unauthorized`

## Admin Page

- Open [http://localhost:8080/admin](http://localhost:8080/admin) or [http://localhost:8080/admin.html](http://localhost:8080/admin.html)
- Sign in with the configured admin username and password
- Or expand the fallback section and enter `ADMIN_API_KEY`
- The page can remember the bearer token or fallback key locally in the browser
- The page talks directly to the protected `/api/admin/**` endpoints for dashboard stats, catalog refresh, brand CRUD, wallpaper upload and deletion, feedback moderation, and audit-log review
- The auth panel also supports `Log out all sessions`, which invalidates every active bearer token for the current admin account

## Cloud Deployment

- Split deployment guide: [DEPLOYMENT.md](D:\Codes\vscodecodes\Web前端课程设计\DEPLOYMENT.md:1)
- Root Docker image for Render: [Dockerfile](D:\Codes\vscodecodes\Web前端课程设计\Dockerfile:1)
- Frontend backend-origin switch: [api-config.js](D:\Codes\vscodecodes\Web前端课程设计\api-config.js:1)

## How To Use The Backend System

Recommended daily workflow:

1. Start MySQL service.
2. Start the backend with the `mysql` profile.
3. Open the public site at [http://localhost:8080/](http://localhost:8080/) or [http://localhost:8080/index.html](http://localhost:8080/index.html).
4. Open the admin console at [http://localhost:8080/admin](http://localhost:8080/admin).
5. Sign in with the username and password you configured for the admin console, or use the fallback API key section.

What each part is for:

- `/` and `index.html`: public wallpaper site
- `main.html`: compatibility redirect to the public homepage
- `/api/catalog/**`: public wallpaper data APIs
- `/api/feedback`: public feedback submission API
- `/admin`: admin management page
- `/api/admin/**`: protected admin APIs for stats, sync, wallpaper edits, and feedback moderation
- `/api/admin/auth/**`: admin login bootstrap and token issuance

Typical admin actions:

1. Sign in on the admin page, or use the fallback API key section if needed.
2. Check dashboard stats to confirm catalog and feedback counts.
3. Use `Brands` to create, rename, or remove brand records.
4. Use `Wallpaper management` to upload wallpapers, update titles and sort order, or delete obsolete entries.
5. Use `Feedback review` to approve, reject, or feature user feedback.
6. Use `Run catalog sync` after you add, remove, or rename image files under `cars/`.
7. Use `Operation log` to review who changed content or moderation state.

How data flows:

- Image files remain in `cars/`
- The backend scans those folders and syncs metadata into MySQL
- The public homepage now renders its brand navigation, carousel, and gallery sections from `/api/catalog`
- Public pages and admin pages both read data from backend APIs
- Feedback submitted from the frontend is stored in MySQL and then moderated in the admin page
