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
