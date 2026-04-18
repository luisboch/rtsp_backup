# Introduction
Make backups from your rtsp live streams (like wifi security cameras).

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

The new server entrypoint is in `src/main/kotlin/community/rtsp/Main.kt` and exposes:
- `GET /health`
- `GET /api/status`
- `GET /api/config`
- `GET /api/stats/sse` (SSE realtime disk/memory/cpu)

Build native binary (Linux host):

```bash
./gradlew linkReleaseExecutableNative
./build/bin/native/releaseExecutable/rtsp_backup.kexe
```

## Requirements
This set of scripts needs some linux libraries to work:
 - bash: Script interpreter. Is included in most of popular Linux distributions;
 - ffmpeg: tool-set used to read, encode and decode audio and video streams;
 - jq: manipulates json easily;
## Start 
### Configuration file

The config sample is [here](./conf/config.json.sample)

Where
- `properties`
  - `segment_time` seconds, is the time of splits between stream segments;
  - `auto_clean`
    - `enabled` Auto clean old backup history? (enable auto cleanup... see [Here](./cron/cleanup));
    - `keep_days` days to keep history;
- `streams` List of streams to backup, with the following properties:
  - `alias` Name to help identification;
  - `url` Rtsp url with full configuration to read from (like host, port,  user and password);
  - `directory` Used to identify inside DATA_DIR, the backups from this stream

### Backup
Want to start backup? Just start daemon script, passing to it, the built configuration, like this:

```bash
CONFIGURATION=$(jq -c . conf/config.json) ./daemon/daemon start &
```
Stop?
```bash
./daemon/daemon stop
```

## Cleaning up
Want to dele older backups? Just configure the cron job to execute daily
```bash
CONFIGURATION=$(jq -c . conf/config.json) /cron/cleanup
```
# Environments

- CONFIGURATION: json with configuration
- DATA_DIR: directory where store files