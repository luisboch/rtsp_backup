# Stage 1: Build the frontend
FROM node:20-alpine AS frontend-builder
WORKDIR /app/frontend
COPY frontend/package*.json ./
RUN npm install
COPY frontend/ ./
RUN npm run build

# Stage 2: Build the backend
FROM gradle:8.14-jdk21 AS backend-builder
WORKDIR /app

# 1. Copy only the files needed to download Gradle and plugins
COPY gradlew .
COPY gradle ./gradle
COPY build.gradle.kts settings.gradle.kts gradle.properties ./

# 2. Trigger the download of the Gradle distribution and plugins
RUN ./gradlew --version --no-daemon

# 3. Download dependencies
RUN ./gradlew help --no-daemon

# 4. Now copy the actual source code
COPY src ./src

# 5. Build the native executable
RUN ./gradlew nativeBinaries --no-daemon

# Stage 3: Final image
FROM alpine:latest
# Install Java, ffmpeg, and libc compat for the native binary
RUN apk add --no-cache openjdk21-jre-headless ffmpeg gcompat libstdc++

WORKDIR /app

# Copy the native executable from the backend-builder stage
# The path might vary depending on the Gradle configuration, but usually it's under build/bin/native/releaseExecutable/
COPY --from=backend-builder /app/build/bin/native/releaseExecutable/*.kexe ./rtsp-backup

# Copy the frontend build to a directory the app might use (e.g., static)
COPY --from=frontend-builder /app/frontend/dist ./static

# Expose the port (default 8080 from Main.kt)
EXPOSE 8080

# Environment variables
ENV PORT=8080
ENV HOST=0.0.0.0
ENV DATA_DIR=/data
ENV CONFIG_PATH=/app/conf/config.json

# Create directories
RUN mkdir -p /data /app/conf

CMD ["./rtsp-backup"]
