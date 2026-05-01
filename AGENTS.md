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
  - Basic/Session Authentication for security.
  - Cleanup service for managing disk space.
- **Core Components:**
  - `community.rtsp.stream.StreamService`: Manages the recording of streams.
  - `community.rtsp.stream.CleanService`: Manages disk usage and cleanup.
  - `community.rtsp.system.FfmpegCliService`: Interacts with `ffmpeg`.
  - `community.rtsp.system.SystemStatsService`: Collects system information (CPU, Memory, Disk).
  - `community.rtsp.config.ConfigLoader`: Loads application configuration from `config.json`.
  - `community.rtsp.auth.SessionService`: Manages user authentication and sessions.

### Frontend (React)
- **Language:** TypeScript
- **Framework:** React
- **Build Tool:** Vite
- **Key Features:**
  - Dashboard for viewing active streams and system status.
  - Video player for reviewing recorded files (using `hls.js`).
  - Real-time monitoring via SSE.
  - Directory view for navigating backup files.
  - Authentication login page.
- **Key Components:**
  - `App.tsx`: Main application entry and routing.
  - `StreamCard.tsx`: Represents an individual stream and its status.
  - `DirectoryView.tsx`: Navigation and playback for recorded files.
  - `StatusHeader.tsx`: Displays system health and stats.
  - `Login.tsx`: User login interface.

## Development Commands

### Backend
The backend is built using Gradle as a Kotlin Native application.
- Build/Run (Debug): `./gradlew runDebugExecutableNative`
- Build (Release): `./gradlew linkReleaseExecutableNative`

### Frontend
Navigate to the `frontend` directory.
- Install dependencies: `npm install`
- Start development server: `npm run dev`
- Build for production: `npm run build`

## Directory Structure
- `src/nativeMain/kotlin/community/rtsp`: Backend source code.
  - `auth/`: Authentication logic and routes.
  - `config/`: Configuration loading and data classes.
  - `routes/`: API endpoint definitions (Live, Stream, Video).
  - `stream/`: Recording and cleanup services.
  - `system/`: System statistics and ffmpeg interaction.
- `frontend/src`: Frontend source code.
- `frontend/package.json`: Frontend dependencies and scripts.
- `conf/`: Configuration files (e.g., `config.json.sample`).
- `data/`: Default directory for recorded video segments.


## Code guidelines
- Do not expose the sqldelight generated classes to the frontend, use the DTOs (like StreamDto.kt) 