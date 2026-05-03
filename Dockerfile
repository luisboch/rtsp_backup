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

# Install dependencies for Kotlin/Native build
RUN apt-get update && apt-get install -y libsqlite3-dev

# 1. Copy only the files needed to download Gradle and plugins
COPY gradlew .
COPY gradle ./gradle
COPY build.gradle.kts settings.gradle.kts gradle.properties ./

# 2. Trigger the download of the Gradle distribution and plugins
RUN ./gradlew --version --no-daemon

# 3. Download dependencies
RUN mkdir -p src/nativeMain/kotlin && \
    echo "fun main() {}" > src/nativeMain/kotlin/dummy.kt && \
    ./gradlew nativeBinaries --no-daemon || true && \
    rm -rf src

# 4. Now copy the actual source code
COPY src ./src

# 5. Build the native executable
RUN ./gradlew nativeBinaries --no-daemon

# Stage 3: Final image
FROM debian:trixie-slim
# Install runtime dependencies: libsqlite3, ffmpeg, and JRE (if needed)
RUN apt-get update && apt-get install -y \
    libsqlite3-0 \
    ffmpeg \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy the native executable from the backend-builder stage
COPY --from=backend-builder /app/build/bin/native/releaseExecutable/rtsp_backup.kexe ./rtsp-backup

# Copy the frontend build
COPY --from=frontend-builder /app/frontend/dist ./static

# Expose the port
EXPOSE 8080

# Environment variables
ENV PORT=8080
ENV HOST=0.0.0.0
ENV DATA_DIR=/data
ENV CONFIG_PATH=/app/conf/config.json

# Create directories
RUN mkdir -p /data /app/conf

# Ensure the binary is executable
RUN chmod +x ./rtsp-backup

CMD ["./rtsp-backup"]
