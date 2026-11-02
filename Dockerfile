FROM node:22-alpine AS frontend-builder
WORKDIR /workspace/frontend
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

FROM maven:3.9.9-eclipse-temurin-21 AS backend-builder
WORKDIR /workspace/backend
COPY backend/capstoneProject/.mvn ./.mvn
COPY backend/capstoneProject/mvnw ./
COPY backend/capstoneProject/pom.xml ./
RUN chmod +x mvnw
RUN ./mvnw -q -DskipTests dependency:go-offline
COPY backend/capstoneProject/src ./src
COPY --from=frontend-builder /workspace/frontend/dist ./src/main/resources/static
RUN ./mvnw -q -DskipTests package

FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_TOOL_OPTIONS="-XX:InitialRAMPercentage=20.0 -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError"
COPY --from=backend-builder /workspace/backend/target/capstoneProject-0.0.1-SNAPSHOT.jar ./app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
