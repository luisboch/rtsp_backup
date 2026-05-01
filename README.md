# RTSP Backup

Make backups from your RTSP live streams (like WiFi security cameras) and view them via a web dashboard.

## Overview

RTSP Backup is a full-stack application that:
- Records RTSP streams into segmented video files using `ffmpeg`.
- Provides a web dashboard (React) to monitor system health and view recordings.
- Manages disk space with an automatic cleanup service.
- Features a Ktor-based backend compiled to a native binary.

# How it works
This repository is used to build Docker image available [Here](https://hub.docker.com/r/luisboch/rtsp-backup)

# Running
The recommended method is to use Docker image, the following guide is to run the standalone/manual mode.

## Dashboard (React + TypeScript)

A frontend dashboard is available in `frontend/` (pure React + TypeScript via Vite, no Next.js).

```bash
cd frontend
npm install
npm run dev
```

By default it proxies `/api/*` and `/health` to `http://localhost:8080`.

## Kotlin server (Ktor + Coroutines, Native)

The server entrypoint is in `src/nativeMain/kotlin/community/rtsp/Main.kt` and exposes:
- `GET /health` - System health check
- `POST /api/auth/login` - User authentication
- `GET /api/status` - Current recording status
- `GET /api/config` - Application configuration
- `GET /api/stats/sse` - SSE real-time system stats (Disk/Memory/CPU)

Build and run native binary:

```bash
./gradlew runDebugExecutableNative
```

Build release binary:
```bash
./gradlew linkReleaseExecutableNative
# Binary will be at build/bin/native/releaseExecutable/rtsp_backup.kexe
```

## Requirements
This application needs some linux libraries and tools to work:
 - ffmpeg: tool-set used to read, encode and decode audio and video streams;
 - libcurl: used by Ktor for networking;
 - sqlite3: for database support (if applicable).
## Start 
### Configuration file

The config sample is [here](./conf/config.json.sample). Copy it to `conf/config.json` and adjust as needed.

Where
- `properties`
  - `segment_time` seconds, is the time of splits between stream segments;
  - `auto_clean`
    - `enabled` Auto clean old backup history?
    - `keep_days` days to keep history;
- `streams` List of streams to backup, with the following properties:
  - `alias` Name to help identification;
  - `url` Rtsp url with full configuration to read from (like host, port,  user and password);
  - `directory` Used to identify inside DATA_DIR, the backups from this stream

### Running the application

The application is now a unified Kotlin Native binary that manages both recording and the web API.

```bash
./gradlew runDebugExecutableNative
```

The server will load configuration from `conf/config.json` by default.

## Legacy Scripts (Optional)
The following scripts were part of the original implementation and are still available for reference:
- `daemon/daemon`: Original bash daemon for managing ffmpeg.
- `cron/cleanup`: Original cleanup script.
# Environments

The following environment variables can be used to configure the application:
- `CONFIGURATION`: JSON string with configuration (overrides `conf/config.json`)
- `DATA_DIR`: directory where video files are stored (default: `./data`)
- `HTTP_PORT`: port for the web server (default: `8080`)