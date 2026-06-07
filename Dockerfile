FROM maven:3.9.9-eclipse-temurin-8 AS build

WORKDIR /workspace

COPY backend/pom.xml backend/pom.xml
RUN mvn -f backend/pom.xml -B dependency:go-offline

COPY backend backend
RUN mvn -f backend/pom.xml -B clean package -DskipTests

FROM eclipse-temurin:8-jre

WORKDIR /app

ENV APP_PROFILE=mysql
ENV APP_FRONTEND_ROOT_PATH=/app/site
ENV APP_CATALOG_ROOT_PATH=/app/site/cars
ENV APP_CATALOG_SYNC_ON_STARTUP=false

COPY --from=build /workspace/backend/target/backend-0.0.1-SNAPSHOT.jar /app/backend.jar
COPY main.html /app/site/
COPY index.html /app/site/
COPY faq.html /app/site/
COPY privacy-policy.html /app/site/
COPY terms-of-service.html /app/site/
COPY admin.html /app/site/
COPY styles.css /app/site/
COPY script.js /app/site/
COPY admin.css /app/site/
COPY admin.js /app/site/
COPY api-config.js /app/site/
COPY robots.txt /app/site/
COPY sitemap.xml /app/site/
COPY cars /app/site/cars

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/backend.jar"]
