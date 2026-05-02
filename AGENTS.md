# RTSP Backup - Project Guide

## Project Overview
RTSP Backup is a system designed to record RTSP streams, manage backups, and provide a web-based dashboard for monitoring and viewing the recorded video content.

## Architecture
Full-stack application: Kotlin Native (Backend) + React (Frontend).

### Backend (Kotlin/Native & Ktor)
- **Key Services**:
  - `StreamService`: Manages the recording lifecycle.
  - `CleanService`: Manages disk usage and cleanup of old segments.
  - `SystemStatsService`: Collects CPU, Memory, and Disk metrics.
  - `SessionService`: Manages user authentication and sessions.
- **Database**: SQLite with SQLDelight for persistence.
- **Interop**: Uses Posix C functions for filesystem access and process management.

### Frontend (React & Vite)
- **Tech Stack**: TypeScript, React, Lucide-icons, hls.js.
- **Communication**: REST API for management + SSE for real-time statistics.

## Directory Structure
- `src/nativeMain/kotlin/community/rtsp`: Backend source code.
- `src/nativeMain/sqldelight`: Database schemas and migrations.
- `frontend/src`: Frontend React application.
- `conf/`: Configuration templates (e.g., `config.json.sample`).
- `data/`: Default storage for video files (segments and HLS playlists).

## Database Schema Summary
The system uses the following main tables:
- `user`: Stores user credentials (id, username, password_hash).
- `stream`: Stores configured RTSP streams (id, owner_id, alias, rtsp_url, directory).
- `session`: Manages user sessions (id, user_id, token, created_at).
- `stream_share`: Manages shared access to streams.
- `stream_favorite`: Tracks user favorites for streams (own or shared).

## Environment Variables
- `HOST`: Server host address (default: `0.0.0.0`).
- `PORT`: Server port (default: `8080`).
- `CONFIG_PATH`: Path to the `config.json` file.
- `DATA_DIR`: Root directory for recorded video data (default: `/data`).

## Core API Endpoints

### Authentication
- `POST /api/auth/register`: Register a new user (Form params: `username`, `password`).
- `POST /api/auth/login`: Session authentication (Form params: `username`, `password`).
- `POST /api/auth/logout`: Invalidate current session.

### Streams & Management
- `GET /api/streams`: List all streams for the authenticated user.
- `POST /api/streams`: Add a new stream (JSON: `alias`, `rtspUrl`).
- `GET /api/status`: Overall system health and recording status.
- `GET /api/stats/sse`: Real-time system metrics via Server-Sent Events.

### Video & Live
- `GET /api/live/{alias}/live/index.m3u8`: HLS stream endpoint for a specific stream.
- `GET /api/video/{path}`: Access to recorded mp4 files.
- `GET /api/files/{alias}`: List recorded mp4 files for a specific stream.

## Development Workflow
- **Adding an API Endpoint**:
  1. Define the route in `src/nativeMain/kotlin/community/rtsp/routes/` or `plugins/Routing.kt`.
  2. Use DTOs in `src/nativeMain/kotlin/community/rtsp/dto/` for request/response bodies.
  3. Ensure the route is protected by `authenticate("auth-session")` if needed.
- **Database Changes**:
  1. Add/modify `.sq` files in `src/nativeMain/sqldelight`.
  2. Always use named parameters for SQL queries ( `:paramName` instead of `?`). 
  3. Add a migration file `<<version>>.sqm` in `src/nativeMain/sqldelight/migrations` if modifying existing structure.

## FFmpeg Process Management
Recording is managed by `StreamBackupService`.
- **Process**: One FFmpeg process is spawned per stream using `fork()` and `execvp()`.
- **Monitoring**: The system monitors the PID and restarts the process if it terminates.
- **Output**: FFmpeg is configured to produce both an HLS stream (for live view) and mp4 segments (for backup).
- **Storage**: Files are stored in `{DATA_DIR}/{user_id}/{stream_alias}/[live|backup]`.

## Tips for AI Assistants
- **C-Interop**: The project uses Kotlin/Native C-interop. Be mindful of `memScoped`, `alloc`, and C-pointers when working with filesystem or process functions.
- **DTOs**: Always use DTOs for frontend communication; never expose SQLDelight generated classes directly.
- **Memory**: Kotlin/Native memory management differs from JVM. Avoid leaking C-allocated memory.
- **Paths**: Many filesystem operations use Posix functions (`access`, `fopen`, `popen`). Ensure paths are correctly handled.