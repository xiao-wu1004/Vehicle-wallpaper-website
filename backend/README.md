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
- Stores feedback messages in the database
- Syncs wallpaper metadata from the filesystem into database tables on startup

## Database Design

Current core tables are created by Flyway migration `V1__create_core_tables.sql`.

- `brands`: brand metadata
- `wallpapers`: wallpaper metadata and file URLs
- `feedback_messages`: feedback form submissions

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

These endpoints are intended for the next backend management layer.

- `GET /api/admin/dashboard`
- `POST /api/admin/catalog/refresh`
- `GET /api/admin/wallpapers`
- `PATCH /api/admin/wallpapers/{wallpaperId}`
- `GET /api/admin/feedback`
- `PATCH /api/admin/feedback/{feedbackId}`

Current note:

- Admin APIs are protected by API key authentication.
- Header name: `X-Admin-API-Key`
- Configure the key with environment variable `ADMIN_API_KEY`
- If `ADMIN_API_KEY` is missing, admin APIs will return `503 Service Unavailable`
- If the header is missing or incorrect, admin APIs will return `401 Unauthorized`

## Admin Page

- Open [http://localhost:8080/admin](http://localhost:8080/admin) or [http://localhost:8080/admin.html](http://localhost:8080/admin.html)
- Enter the configured `ADMIN_API_KEY`
- The page talks directly to the protected `/api/admin/**` endpoints for dashboard stats, catalog refresh, wallpaper edits, and feedback moderation

## How To Use The Backend System

Recommended daily workflow:

1. Start MySQL service.
2. Start the backend with the `mysql` profile.
3. Open the public site at [http://localhost:8080/main.html](http://localhost:8080/main.html).
4. Open the admin console at [http://localhost:8080/admin](http://localhost:8080/admin).
5. Enter the same value you configured in `ADMIN_API_KEY`.

What each part is for:

- `main.html`: public wallpaper site
- `/api/catalog/**`: public wallpaper data APIs
- `/api/feedback`: public feedback submission API
- `/admin`: admin management page
- `/api/admin/**`: protected admin APIs for stats, sync, wallpaper edits, and feedback moderation

Typical admin actions:

1. Connect with the API key in the admin page.
2. Check dashboard stats to confirm catalog and feedback counts.
3. Use `Run catalog sync` after you add, remove, or rename images under `cars/`.
4. Use `Wallpaper management` to update wallpaper title, sort order, and active state.
5. Use `Feedback review` to approve, reject, or feature user feedback.

How data flows:

- Image files remain in `cars/`
- The backend scans those folders and syncs metadata into MySQL
- Public pages and admin pages both read data from backend APIs
- Feedback submitted from the frontend is stored in MySQL and then moderated in the admin page
