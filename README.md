# MediaSense — AI-Powered Multimedia Intelligence Platform

MediaSense is a microservices-based media processing platform: upload audio/video, convert
between 100+ formats, generate waveform/caption videos, transcribe speech, and run natural
language Q&A over ingested documents, media, and web pages — backed by Kafka, MinIO, FFmpeg,
Whisper, and a local LLM (Ollama, with a HuggingFace fallback).

> **New to this repo?** Start with **[setup.md](setup.md)** (one-time environment setup) and
> then **[run.md](run.md)** (starting services + full API reference). This file is a project
> overview only.

## Architecture

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

| Service | Port | Responsibility | Storage | Messaging |
|---|---|---|---|---|
| **upload-service** | 8081 | Validates (extension + MIME type) and stores files in MinIO; publishes a Kafka job when an `outputFormat` is given; broadcasts upload progress over WebSocket (`/upload-progress`); retries MinIO writes 3× with exponential backoff | MinIO input bucket | Kafka producer |
| **conversion-service** | 8080 | FFmpeg-powered format conversion, waveform-video generation, caption-burning, and Whisper transcription — both as direct REST endpoints and as a Kafka consumer of `conversion-jobs` | MinIO both buckets | Kafka consumer |
| **download-service** | 8082 | Streams a converted file back out of MinIO | MinIO output bucket | — |
| **qna-service** | 8083 | Ingests files (audio/video via Whisper, PDF via PDFBox, DOCX via Apache POI, plain text), URLs (JSoup) and YouTube links (yt-dlp + Whisper) into an **in-memory** context store; answers questions via a local Ollama model, falling back to the HuggingFace Inference API | In-memory (lost on restart) | — |
| **shared-dto** | — | `JobPayload` — the contract shared between upload-service and conversion-service (`blobName`, `mimeType`, `outputFormat`) | — | — |
| **frontend** | 3000 | React + TypeScript SPA (Vite dev server), one page per service, proxies all API calls to the backend ports | — | WebSocket client |

`GcsHelper` inside `conversion-service` is a legacy/misleading name — it talks to **MinIO**, not
Google Cloud Storage. There is no GCP dependency anywhere in this project (though the Kafka
consumer/producer tests that remain, see below, were written against an even earlier GCS-based
design and are commented out).

## Tech stack

| Layer | Technology |
|---|---|
| Backend language/runtime | Java 24 |
| Backend framework | Spring Boot 3.5.3 (multi-module Maven reactor) |
| Messaging | Apache Kafka (KRaft mode, no ZooKeeper) — topic `conversion-jobs` |
| Object storage | MinIO (S3-compatible) — buckets `audio-video-converter-files` (input) and `audio-video-converter-converted-files` (output) |
| Media processing | FFmpeg/FFprobe via the [Jaffree]\([https://github.com/kokorin/Jaffree](https://github.com/kokorin/Jaffree)) Java wrapper |\
| Speech-to-text | OpenAI Whisper CLI (invoked as a subprocess) |
| YouTube ingestion | yt-dlp (invoked as a subprocess) |
| Local LLM | Ollama (`llama3.2:1b`) via its REST API, with HuggingFace Inference API (`deepset/roberta-base-squad2`) as a fallback |
| Document parsing | Apache PDFBox (PDF), Apache POI (DOCX), JSoup (HTML scraping) |
| HTTP client (qna-service → Ollama/HuggingFace) | OkHttp3 |
| Resilience | Spring Retry (`@Retryable`/`@Recover`, exponential backoff) on MinIO uploads |
| Frontend | React 18 + TypeScript + Vite 5, no router (state-based page switching) |
| Infrastructure | Docker Compose (Kafka + MinIO) |

## Prerequisites & setup

This repo was built on **macOS** and that's what the current `application.properties` defaults
target (`/opt/homebrew/bin`, `/tmp/mediasense-temp`, bare `whisper`/`yt-dlp` resolved via `PATH`).
Full tool list, install commands, and gotchas: **[setup.md](setup.md)**.

## Running the system

Starting each service, the frontend, and the full REST API reference (with example `curl`
commands) is in **[run.md](run.md)**.

## Known gaps / issues (as of this writing)

These are worth knowing before relying on this codebase, and before writing any report about it:

- **`AskResponse` JSON field name bug** — `qna-service`'s `AskResponse` serializes the model
  identifier as `moduleUsed` (typo: "module" vs "model"), but the frontend (`api.ts`, `QaPage.tsx`)
  reads `modelUsed`. The model-used badge in the Q&A chat UI is therefore always blank.
- **No bucket auto-creation** — nothing calls `MakeBucketArgs`/`bucketExists`; both MinIO buckets
  must be created manually before any upload/convert/download will work.
- **Dead, commented-out tests** — `upload-service`'s `FileStorageServiceRetryTest` and
  `FileStorageServiceIntegrationTest`, and `conversion-service`'s `ConversionJobListenerTest` and
  `ConversionJobListenerIntegrationTest`, are entirely commented out and reference an even earlier
  Google-Cloud-Storage-based design (`com.google.cloud.storage.Storage`, `GCSConfig`) that no
  longer exists. Only `UploadControllerTest` and `ByteConverterTest` (upload-service) are live,
  real tests, plus the three Spring Initializr `*ApplicationTests` stubs. **`qna-service` has no
  tests at all**, and `download-service` has only its stub.
- **Stale compiled build artifacts committed to git** — `shared-dto/target/` and
  `qna-service/target/` contain `.class`/`.jar` files checked into source control from before the
  package rename, still referencing the old `com.mediamind` package name.
- **`qna-service` has no module-level `.gitignore`** — every other module does.
- Config typo: `qna-service/application.properties` uses `spring.application-name` (missing the
  dot) instead of `spring.application.name` — cosmetic only, doesn't block startup.

## Repository layout

```
pom.xml                 ← parent Maven reactor (5 modules)
shared-dto/             ← JobPayload DTO shared by upload-service and conversion-service
upload-service/         ← :8081
conversion-service/     ← :8080
download-service/       ← :8082
qna-service/            ← :8083
frontend/               ← React/TS/Vite SPA, :3000
docker-compose.yml       ← Kafka + MinIO
setup.md                 ← one-time environment setup
run.md                   ← starting services + API reference
```