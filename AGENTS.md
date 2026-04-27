# Project Overview
RTSP Backup is a system designed to record RTSP streams, manage backups, and provide a web-based dashboard for monitoring and viewing the recorded video content.

## Architecture
The project is a full-stack application consisting of:

### Backend (Kotlin Multiplatform / Ktor)
- **Language:** Kotlin
- **Framework:** Ktor (Server-side)
- **Key Features:**
  - RTSP stream recording using `ffmpeg`.
  - Stream proxying and video serving.
  - API for system statistics, configuration, and file management.
  - SSE (Server-Sent Events) for real-time stats updates.
  - Basic Authentication for security.
  - Cleanup service for managing disk space.
- **Core Components:**
  - `StreamService`: Manages the recording of streams.
  - `CleanService`: Manages disk usage and cleanup.
  - `FfmpegCliService`: Interacts with `ffmpeg`.
  - `SystemStatsService`: Collects system information.
  - `ConfigLoader`: Loads application configuration.

### Frontend (React)
- **Language:** TypeScript
- **Framework:** React
- **Build Tool:** Vite
- **Key Features:**
  - Dashboard for viewing active streams and system status.
  - Video player for reviewing recorded files (using `hls.js`).
  - Real-time monitoring via SSE.
  - Directory view for navigating backup files.
- **Key Components:**
  - `App.tsx`: Main application entry.
  - `StreamCard.tsx`: Represents an individual stream.
  - `DirectoryView.tsx`: Navigation for recorded files.
  - `StatusHeader.tsx`: Displays system health.

## Development Commands

### Backend
The backend is built using Gradle.
- Build/Run: `./gradlew run` (or equivalent Gradle command)

### Frontend
Navigate to the `frontend` directory.
- Install dependencies: `npm install`
- Start development server: `npm run dev`
- Build for production: `npm run build`

## Directory Structure
- `src/nativeMain/kotlin/community/rtsp`: Backend source code.
- `frontend/src`: Frontend source code.
- `frontend/package.json`: Frontend dependencies and scripts.
