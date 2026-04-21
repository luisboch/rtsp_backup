# Stage 1: Build the frontend
FROM node:20-alpine AS frontend-builder
WORKDIR /app/frontend
COPY frontend/package*.json ./
RUN npm install
COPY frontend/ ./
RUN npm run build

# Stage 2: Build the backend
FROM gradle:8.10-jdk21 AS backend-builder
WORKDIR /app
COPY . .
# We build the native executable
# We unset JAVA_HOME if it's pointing to a non-existent directory, or let Gradle use the one from the image
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
