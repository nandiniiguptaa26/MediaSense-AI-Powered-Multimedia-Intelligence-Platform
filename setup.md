# MediaSense — Setup Guide (Windows)

One-time environment setup: install tools, prepare local folders/buckets, and build the
project. Once this is done, see **RUN.md** to start and use the services.

MediaSense is a media conversion / transcription / Q&A platform built as a Maven multi-module
project of independent Spring Boot 3.5 microservices, backed by Kafka + MinIO, with a
Vite/React frontend. This guide targets **Windows 10/11**, which is also the OS this codebase
was originally written for — the default config paths already assume Windows layouts.

All commands below are **PowerShell** unless noted.

## 1. Architecture

```
                         ┌─────────────┐
   Browser ────────────▶ │  Frontend    │  Vite dev server :3000
                         │ (React/TS)   │  proxies /upload /convert /download /qna
                         └──────┬───────┘
                                │
        ┌───────────────┬──────┼───────────────┬────────────────┐
        ▼               ▼      ▼               ▼                ▼
 upload-service   conversion-service   download-service    qna-service
    :8081               :8080              :8082              :8083
        │                  ▲                  │                  │
        │   Kafka topic    │                  │                  │
        └─"conversion-jobs"┘                  │                  │
        │                                     │                  │
        ▼                                     ▼                  │
   MinIO bucket                        MinIO bucket               │
 audio-video-converter-files   audio-video-converter-converted-files
                                                                    │
                                                    ffmpeg / whisper / yt-dlp
                                                    Ollama (local LLM) → HuggingFace (fallback)
```

- **upload-service** (`:8081`) — validates + stores files in MinIO, publishes a Kafka job
  (`conversion-jobs` topic) when an `outputFormat` is supplied.
- **conversion-service** (`:8080`) — consumes Kafka jobs *and* exposes direct REST endpoints
  for conversion, waveform-video generation, caption-video generation, and transcription
  (uses `ffmpeg.exe`/`ffprobe.exe` via the `jaffree` wrapper, plus the Whisper CLI).
- **download-service** (`:8082`) — streams a converted file back out of MinIO.
- **qna-service** (`:8083`) — ingests files/URLs/audio/video/YouTube links into an in-memory
  context store, then answers questions using a local Ollama model with a HuggingFace API
  fallback.
- **shared-dto** — the `JobPayload` class shared between upload-service and conversion-service.
- **frontend** — Vite + React, dev server on `:3000`, proxies API calls to the four services.

Note: `GcsHelper` inside `conversion-service` is misleadingly named — despite "GCS" in the name
it talks to **MinIO**, not Google Cloud Storage. There is no GCP dependency anywhere in this
project.

## 2. Prerequisites

| Tool | Required by | Notes |
|---|---|---|
| JDK 24 | root `pom.xml` (`java.version=24`) | Download from https://adoptium.net (Temurin 24) or `winget install --id EclipseAdoptium.Temurin.24.JDK` |
| Maven 3.9+ | build | Download from https://maven.apache.org/download.cgi or `winget install --id Apache.Maven`. **Don't rely on `mvnw.cmd`** — see gotcha #1 below. |
| Node.js 18+ / npm | frontend | https://nodejs.org (LTS) or `winget install --id OpenJS.NodeJS.LTS` |
| Docker Desktop (WSL2 backend) | Kafka + MinIO | https://www.docker.com/products/docker-desktop or `winget install --id Docker.DockerDesktop`. Requires WSL2 — Docker Desktop will prompt to enable it on first run. |
| ffmpeg + ffprobe | conversion-service (convert, waveform, caption, transcribe) | Download an "essentials" or "full" build from https://www.gyan.dev/ffmpeg/builds/ (linked from ffmpeg.org's own downloads page), unzip, copy the contents of its `bin\` folder to `C:\ffmpeg\bin` so `ffmpeg.exe`/`ffprobe.exe` live there |
| Python 3.12 | Whisper CLI + yt-dlp | https://www.python.org/downloads/ — **during install, click "Customize installation" and set the install path to `C:\Python312`** so it matches the config defaults |
| OpenAI Whisper CLI | conversion-service `/transcribe`, qna-service audio/video/YouTube ingest | `C:\Python312\python.exe -m pip install openai-whisper` |
| yt-dlp | qna-service YouTube ingest | `C:\Python312\python.exe -m pip install yt-dlp` |
| Ollama (optional) | qna-service `/ask` primary LLM | https://ollama.com/download/windows or `winget install --id Ollama.Ollama` |

## 3. Important gotchas in this codebase

1. **`mvnw`/`mvnw.cmd` are present but the wrapper is incomplete** — there is no `.mvn/wrapper/`
   folder (no `maven-wrapper.properties`, no wrapper jar), so `mvnw.cmd` will likely fail to
   resolve a Maven distribution. **Install Maven yourself and use the global `mvn` command**
   instead of `mvnw.cmd`.
2. **Config defaults already target Windows** — `conversion-service` and `qna-service`
   `application.properties` hardcode:
   - `ffmpeg.path=C:/ffmpeg/bin`
   - `convert.temp.path=C:/converter-convert-temp`
   - `whisper.path=C:/Python312/Scripts/whisper.exe`
   - `qna.temp.path=C:/converter-convert-temp`
   - `yt-dlp.path=C:/Python312/Scripts/yt-dlp.exe`

   If you install ffmpeg/Python at those exact paths (as instructed above), you can run every
   service with **no command-line overrides at all**. If you install elsewhere, pass overrides
   (see RUN.md).
3. **No bucket auto-creation.** Nothing in the code calls `MakeBucketArgs`/`bucketExists`. You
   must manually create both MinIO buckets before uploading anything (step 6 below).
4. **`qna-service/application.properties` has `spring.application-name`** (missing the dot) —
   cosmetic typo only, doesn't block startup.

## 4. Create required local folders

```powershell
New-Item -ItemType Directory -Force -Path C:\ffmpeg\bin
New-Item -ItemType Directory -Force -Path C:\Python312
New-Item -ItemType Directory -Force -Path C:\converter-convert-temp
```

Extract the ffmpeg zip you downloaded and copy `ffmpeg.exe`, `ffprobe.exe` (and `ffplay.exe`)
from its `bin\` folder into `C:\ffmpeg\bin`. Confirm:

```powershell
C:\ffmpeg\bin\ffmpeg.exe -version
C:\Python312\Scripts\whisper.exe --help
C:\Python312\Scripts\yt-dlp.exe --version
```

If Ollama is installed, pull the model referenced in `qna-service`'s config:

```powershell
ollama pull llama3.2:1b
```
(Ollama's Windows installer registers it as a background service on `localhost:11434` — no
separate `ollama serve` needed.)

## 5. Build the project

```powershell
cd C:\path\to\MediaSense
mvn clean install
```

Maven resolves the reactor build order automatically (`shared-dto` builds before the services
that depend on it, regardless of `<modules>` listing order in the parent `pom.xml`).

## 6. Start infrastructure and create MinIO buckets

Make sure Docker Desktop is running, then from the project root:

```powershell
cd C:\path\to\MediaSense
docker compose up -d
```

This starts:
- **Kafka** on `localhost:9092` (topic `conversion-jobs` auto-creates on first publish)
- **MinIO** API on `localhost:9000`, console on `localhost:9001` (user `admin` / password `password123`)

Create the two required buckets via the MinIO console at `http://localhost:9001`:
Buckets → Create Bucket → enter each of:
- `audio-video-converter-files`
- `audio-video-converter-converted-files`

(Or with the `mc` CLI if you have it on PATH: `mc alias set local http://localhost:9000 admin password123`, then `mc mb local/audio-video-converter-files` and `mc mb local/audio-video-converter-converted-files`.)

## Next step

Setup is complete. Go to **RUN.md** to start the services, the frontend, and see the full API
