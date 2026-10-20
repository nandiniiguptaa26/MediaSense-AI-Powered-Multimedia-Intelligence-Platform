# MediaSense — Setup Guide (macOS)

One-time environment setup: install tools and build the project. Once this is done, see
**run.md** to start and use the services.

MediaSense is a media conversion / transcription / Q&A platform built as a Maven multi-module
project of independent Spring Boot 3.5 microservices, backed by Kafka + MinIO, with a
Vite/React frontend. This guide targets **macOS** (Apple Silicon or Intel) — the config defaults
baked into `conversion-service` and `qna-service`'s `application.properties` already assume a
Homebrew-on-macOS layout. All commands below are **zsh/bash**.

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
  (uses `ffmpeg`/`ffprobe` via the `jaffree` wrapper, plus the Whisper CLI).
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
| Homebrew | everything below | https://brew.sh if you don't already have it |
| JDK 24 | root `pom.xml` (`java.version=24`) | `brew install openjdk@24`, then add it to your PATH (brew prints the exact `export PATH=...` line — add it to `~/.zshrc`) and set `JAVA_HOME="$(/usr/libexec/java_home -v 24)"` |
| Maven 3.9+ | build | `brew install maven`. **Don't rely on `mvnw`** — see gotcha #1 below. |
| Node.js 18+ / npm | frontend | `brew install node` (or use `nvm`) |
| Docker Desktop | Kafka + MinIO | https://www.docker.com/products/docker-desktop or `brew install --cask docker` |
| ffmpeg + ffprobe | conversion-service (convert, waveform, caption, transcribe) | `brew install ffmpeg` — on **Apple Silicon** this installs to `/opt/homebrew/bin` (matches the config default exactly); on **Intel Macs** Homebrew installs to `/usr/local/bin` instead, so you'll need to override `ffmpeg.path` (see RUN.md) |
| Python 3 | Whisper CLI + yt-dlp | `brew install python@3.12`, or use the Python 3 that ships with Xcode Command Line Tools |
| OpenAI Whisper CLI | conversion-service `/transcribe`, qna-service audio/video/YouTube ingest | `python3 -m pip install openai-whisper` |
| yt-dlp | qna-service YouTube ingest | `python3 -m pip install yt-dlp` (or `brew install yt-dlp`) |
| Ollama (optional) | qna-service `/ask` primary LLM | `brew install ollama` or https://ollama.com/download/mac |

## 3. Important gotchas in this codebase

1. **`mvnw`/`mvnw.cmd` are present but the wrapper is incomplete** — there is no `.mvn/wrapper/`
   folder (no `maven-wrapper.properties`, no wrapper jar), so `./mvnw` will fail to resolve a
   Maven distribution. **Install Maven yourself (`brew install maven`) and use the global `mvn`
   command** instead of `./mvnw`.
2. **Config defaults already target macOS/Homebrew**, specifically Apple Silicon's default
   Homebrew prefix. `conversion-service` and `qna-service` `application.properties` hardcode:
    - `ffmpeg.path=/opt/homebrew/bin`
    - `convert.temp.path=/tmp/mediasense-temp` (conversion-service) / `qna.temp.path=/tmp/mediasense-temp` (qna-service) — both services share the same temp directory, auto-created on first use
    - `whisper.path=whisper` — resolved via your shell's `PATH`, not an absolute path
    - `yt-dlp.path=yt-dlp` — same, resolved via `PATH`

   If you're on Apple Silicon and installed Whisper/yt-dlp's console scripts somewhere on your
   `PATH` (confirm with `which whisper` and `which yt-dlp` in a fresh terminal), you can run every
   service with **no command-line overrides at all**. If you're on an Intel Mac, or `pip install
   --user` put the scripts somewhere not on `PATH`, pass overrides (see RUN.md).
3. **No bucket auto-creation.** Nothing in the code calls `MakeBucketArgs`/`bucketExists`. You
   must manually create both MinIO buckets before uploading anything (step 4 below).
4. **`qna-service/application.properties` has `spring.application-name`** (missing the dot) —
   cosmetic typo only, doesn't block startup.

## 4. Build the project

```bash
cd /path/to/MediaSense-AI-Powered-Multimedia-Intelligence-Platform
mvn clean install
```

Maven resolves the reactor build order automatically (`shared-dto` builds before the services
that depend on it, regardless of `<modules>` listing order in the parent `pom.xml`).

## 5. Start infrastructure and create MinIO buckets

Make sure Docker Desktop is running, then from the project root:

```bash
cd /path/to/MediaSense-AI-Powered-Multimedia-Intelligence-Platform
docker compose up -d
```

This starts:
- **Kafka** on `localhost:9092` (topic `conversion-jobs` auto-creates on first publish)
- **MinIO** API on `localhost:9000`, console on `localhost:9001` (user `admin` / password `password123`)

Create the two required buckets via the MinIO console at `http://localhost:9001`:
Buckets → Create Bucket → enter each of:
- `audio-video-converter-files`
- `audio-video-converter-converted-files`

(Or with the `mc` CLI if you have it, `brew install minio-mc`:
`mc alias set local http://localhost:9000 admin password123`, then
`mc mb local/audio-video-converter-files` and `mc mb local/audio-video-converter-converted-files`.)

## 6. Verify your tool install paths

```bash
/opt/homebrew/bin/ffmpeg -version      # or: which ffmpeg
which whisper && whisper --help
which yt-dlp && yt-dlp --version
```

If any of these resolve to a different path than the config defaults above, you'll need to pass
`--ffmpeg.path=...` / `--whisper.path=...` / `--yt-dlp.path=...` overrides when starting the
relevant service — see RUN.md.

If Ollama is installed, pull the model referenced in `qna-service`'s config:

```bash
ollama pull llama3.2:1b
```
(The Homebrew/installer build of Ollama runs as a background service on `localhost:11434` — if
`ollama pull` can't connect, run `ollama serve` in its own terminal first.)

## Next step

Setup is complete. Go to **run.md** to start the services, the frontend, and see the full API
reference.