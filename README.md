# INTERNSHIP / INDUSTRIAL TRAINING REPORT

---

**NATIONAL INSTITUTE OF TECHNOLOGY, TIRUCHIRAPPALLI – 620 015**
**ACADEMIC OFFICE**

---

# MediaMind: AI-Powered Media Processing Platform

**Industrial Training Report submitted in partial fulfilment of the requirements for the**
**PG Programme — Internship / Industrial Training / Academic Attachment (I/A)**

---

| | |
|---|---|
| **Student Name** | KHUSHI GUPTA |
| **Programme** | PG (M.Tech / MCA) |
| **Department** | Computer Science and Engineering |
| **Training Organization** |  |
| **Training Duration** | 6–8 Weeks (Summer Internship) |
| **Report Submission Date** | June 2026 |

---

---

## 1. PREFACE / ACKNOWLEDGEMENT

This report documents the work carried out during the Industrial Training / Internship programme as mandated by the National Institute of Technology, Tiruchirappalli. The project undertaken is **MediaMind** — an AI-powered, cloud-native media processing platform built using a microservices architecture.

I am grateful to the organization and its technical team for providing the opportunity to work on a real-world, production-grade software system. The project involved designing, developing, and validating a full-stack distributed application encompassing file storage, message streaming, media processing, and AI-driven question answering.

I acknowledge the guidance of the faculty coordinator and Head of Department in framing the objectives and evaluating the outcomes of this training.

---

---

## 2. CERTIFICATE

*(To be signed and sealed by the industry supervisor / officer-in-charge)*

This is to certify that **KHUSHI GUPTA**, a PG student of the National Institute of Technology, Tiruchirappalli, has successfully completed the Industrial Training / Internship at our organization during the Summer of 2026. The student worked on the project titled **"MediaMind: AI-Powered Media Processing Platform"** and demonstrated satisfactory performance, technical depth, and professional conduct throughout the training period.

**Signature of Officer-in-Charge:** ___________________________

**Name & Designation:** ___________________________

**Organization Name:** 

**Date:** ___________________________

**Seal of the Organization:**

---

---

## 3. CONTENTS / INDEX

| Section | Title | Page |
|---------|-------|------|
| 1 | Preface / Acknowledgement | 2 |
| 2 | Certificate | 3 |
| 3 | Contents / Index | 4 |
| 4 | Introduction about the Industry / Organization | 5 |
| 5 | Training Schedule | 7 |
| 6 | Work Done / Observations | 8 |
| 7 | Specific Assignment / Project Handled — MediaMind | 12 |
| 7.1 | Problem Statement and Motivation | 12 |
| 7.2 | System Architecture Overview | 13 |
| 7.3 | Technology Stack and Infrastructure | 15 |
| 7.4 | Upload Service | 17 |
| 7.5 | Conversion Service | 20 |
| 7.6 | Download Service | 25 |
| 7.7 | QnA (Intelligence) Service | 27 |
| 7.8 | Frontend Application | 31 |
| 7.9 | Practical Problems Identified and Solutions | 34 |
| 8 | Learning after Training | 38 |
| 9 | Summary | 40 |

---

---

## 4. INTRODUCTION ABOUT THE INDUSTRY / ORGANIZATION

### 4.1 About the Organization

The training was undertaken at a leading global technology and payments company. The organization is at the forefront of digital financial infrastructure, serving millions of merchants and financial institutions worldwide. Its engineering teams work on large-scale distributed systems, cloud-native architectures, and AI-integrated solutions.

The internship was conducted within a software engineering team that builds cloud-native backend platforms and developer tools. The team follows agile practices, microservices design principles, DevOps pipelines, and modern Java and JavaScript/TypeScript development standards.

### 4.2 Technology Domain

The organization specializes in:
- **Distributed Systems and Cloud Infrastructure** — Large-scale service-oriented architectures hosted on cloud platforms
- **Event-Driven Architecture** — Asynchronous processing using message brokers such as Apache Kafka
- **Object Storage Solutions** — S3-compatible storage for unstructured media data
- **AI / ML Integration** — Local and cloud-based LLM inference, speech-to-text pipelines
- **Full-Stack Development** — Spring Boot-based microservices and React-based frontend applications

### 4.3 Relevance to Academic Specialization

The project aligns directly with the PG curriculum areas of:
- **Distributed Computing** — Microservices, Kafka event streaming, MinIO object storage
- **Software Engineering** — Modular design, REST API design, separation of concerns
- **Artificial Intelligence** — LLM-based Q&A (Ollama / llama3.2), Whisper speech-to-text
- **Web Technologies** — React, TypeScript, Vite, WebSocket

---

---

## 5. TRAINING SCHEDULE

| Week | Activities Undertaken |
|------|----------------------|
| **Week 1** | Onboarding, environment setup (Java 24, Maven, Node.js, Docker), understanding existing systems, studying microservices architecture patterns |
| **Week 2** | Designed system architecture — identified services, defined inter-service communication via Kafka, storage design using MinIO |
| **Week 3** | Implemented Upload Service — file validation, MinIO integration, Kafka producer, WebSocket progress broadcasting, retry mechanism |
| **Week 4** | Implemented Conversion Service — FFmpeg integration via Jaffree, waveform generation, captioning, transcription, Kafka consumer |
| **Week 5** | Implemented Download Service — MinIO retrieval, streaming response, content-type handling. Implemented QnA Service — Whisper ingest, PDF/DOCX parsing, web scraping, Ollama integration |
| **Week 6** | Built React + TypeScript frontend — all four pages (Upload, Convert, Download, Q&A), API service layer, proxy configuration |
| **Week 7** | End-to-end integration testing, bug fixes (broken pipe exception, proxy timeout, QnA blank page, path issues), performance tuning |
| **Week 8** | Report preparation, code review, seminar presentation preparation, final documentation |

---

---

## 6. WORK DONE / OBSERVATIONS

### 6.1 Overview of Work Performed

During the training, a full-stack microservices application called **MediaMind** was designed and implemented from scratch. The system enables users to upload audio and video media files, apply AI-powered transformations (format conversion, waveform visualization, captioning, transcription), download processed results, and run natural language question answering on ingested content.

### 6.2 Key Observations from the Industrial Environment

**Observation 1 — Microservices Decomposition**
In an industrial setting, systems are broken into independently deployable services. Each MediaMind service (Upload, Conversion, Download, QnA) has its own port, configuration, and responsibility. This enables independent scaling — for example, the Conversion service (most CPU-intensive) can be scaled separately.

**Observation 2 — Asynchronous Processing via Kafka**
File uploads publish events to a Kafka topic (`conversion-jobs`). The Conversion service listens and processes jobs asynchronously. This decouples the upload pipeline from the processing pipeline, preventing upload slowdowns when conversion is busy.

**Observation 3 — Object Storage as a File Bus**
MinIO (S3-compatible) is used as the shared storage layer between services. The Upload service writes to an input bucket; the Conversion service reads from that bucket and writes to an output bucket; the Download service reads from the output bucket. Services never pass files directly to each other.

**Observation 4 — Resilience Patterns**
The Upload service uses `@Retryable` (Spring Retry) with exponential backoff for MinIO uploads — 3 attempts with 2-second initial delay doubling each retry. This is a standard enterprise resilience pattern.

**Observation 5 — Real-time Feedback via WebSocket**
Upload progress is broadcast to the frontend via WebSocket (`/upload-progress`). This is a common industrial pattern for providing responsive UX on long-running operations without polling.

**Observation 6 — AI Tool Integration**
Local LLM inference via Ollama (llama3.2:1b) and speech-to-text via OpenAI Whisper demonstrate how AI tools are integrated into backend services as subprocess calls or REST API clients rather than embedded models.

**Observation 7 — Configuration Externalization**
All environment-specific values (bucket names, paths, endpoints, credentials) are externalized to `application.properties`. This is a 12-Factor App principle widely followed in enterprise Java development.

### 6.3 Tools and Software Used

| Tool / Technology | Purpose |
|-------------------|---------|
| Java 24 | Backend language for all microservices |
| Spring Boot 3.5.3 | Microservice framework |
| Apache Maven | Build and dependency management |
| Apache Kafka | Asynchronous event messaging |
| MinIO | S3-compatible object storage |
| FFmpeg (via Jaffree) | Media format conversion, waveform generation |
| OpenAI Whisper | Speech-to-text transcription |
| Ollama (llama3.2:1b) | Local LLM for question answering |
| yt-dlp | YouTube audio extraction |
| Apache PDFBox | PDF text extraction |
| Apache POI | DOCX text extraction |
| JSoup | Web scraping |
| OkHttp3 | HTTP client for Ollama/HuggingFace calls |
| React 18.2 | Frontend UI framework |
| TypeScript | Type-safe frontend development |
| Vite 5.1 | Frontend build tool and dev server |
| Docker + Compose | Container orchestration for Kafka and MinIO |
| IntelliJ IDEA | IDE |

---

---

## 7. SPECIFIC ASSIGNMENT / PROJECT HANDLED

# MediaMind: AI-Powered Media Processing Platform

---

### 7.1 Problem Statement and Motivation

#### Problem Statement

Media processing workflows typically require users to install multiple native tools (FFmpeg, Whisper, etc.) and execute complex command-line pipelines. There is no unified, browser-accessible platform that integrates:
- Multi-format audio/video conversion
- AI-powered speech transcription
- Waveform and caption video generation
- Natural language Q&A over media content

#### Motivation

1. **Proliferation of media content** — Enterprises increasingly need to process audio/video at scale for accessibility, search, and analytics
2. **AI integration** — Speech-to-text (Whisper) and LLM Q&A (Ollama) are powerful tools that are rarely accessible through user-friendly interfaces
3. **Microservices learning** — Building a real polyglot microservices system provides hands-on understanding of distributed systems patterns
4. **End-to-end ownership** — The project covers infrastructure (Docker), backend (Spring Boot), messaging (Kafka), storage (MinIO), AI (Whisper, Ollama), and frontend (React) — providing complete system design experience

#### Practical Problems Formulated

As required by the Industrial Training guidelines, the following 3–4 practical problems were identified and solved:

1. **P1:** How to convert media files between 100+ formats reliably without coupling format logic to the HTTP request cycle?
2. **P2:** How to generate AI-powered transcriptions and captions from audio/video without exposing Whisper CLI complexity to users?
3. **P3:** How to enable natural language querying over ingested documents and media content using a locally-hosted LLM?
4. **P4:** How to handle long-running media processing jobs without HTTP timeout failures and broken streaming responses?

---

### 7.2 System Architecture Overview

#### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        Browser (localhost:3000)                  │
│                   React + TypeScript + Vite                      │
│  [Upload] [Convert] [Download] [Q&A Intelligence]               │
└───────────┬────────────┬───────────┬────────────┬───────────────┘
            │            │           │            │
     /upload│    /convert│  /download│      /qna  │
            ▼            ▼           ▼            ▼
┌───────────────┐  ┌──────────┐  ┌────────┐  ┌────────┐
│ Upload Svc    │  │ Convert  │  │ Download│  │  QnA   │
│ :8081         │  │ Svc :8080│  │ Svc:8082│  │ Svc:8083│
│ Spring Boot   │  │Spring    │  │ Spring │  │ Spring │
│ + WebSocket   │  │Boot+     │  │ Boot   │  │ Boot   │
│ + Retry       │  │FFmpeg    │  │        │  │+Whisper│
└───────┬───────┘  └────┬─────┘  └───┬────┘  │+Ollama │
        │               │             │       │+JSoup  │
        │  Kafka Event  │             │       └────────┘
        ▼               │             │
┌───────────────┐       │             │
│ Apache Kafka  │───────┘             │
│ :9092         │                     │
│ Topic:        │                     │
│ conversion-   │                     │
│ jobs          │                     │
└───────────────┘                     │
                                      │
┌─────────────────────────────────────┘
│
▼
┌───────────────────────────────┐
│       MinIO Object Storage    │
│  :9000 (API) / :9001 (Console)│
│                               │
│ Bucket 1: audio-video-        │
│   converter-files (input)     │
│                               │
│ Bucket 2: audio-video-        │
│   converter-converted-files   │
│   (output)                    │
└───────────────────────────────┘
```

#### Data Flow Sequence

```
1. User uploads file via browser
        ↓
2. Upload Service validates file (extension + MIME type)
        ↓
3. Upload Service stores file in MinIO input bucket
        ↓
4. Upload Service publishes JobPayload to Kafka topic "conversion-jobs"
        ↓
5. Conversion Service Kafka listener receives job
        ↓
6. Conversion Service fetches file from MinIO, runs FFmpeg
        ↓
7. Conversion Service stores result in MinIO output bucket
        ↓
8. User requests download via browser
        ↓
9. Download Service fetches from MinIO output bucket → streams to browser
```

#### Service Responsibility Matrix

| Service | Port | Responsibility | Storage | Messaging |
|---------|------|----------------|---------|-----------|
| Upload Service | 8081 | File validation, ingestion, retry | MinIO input bucket | Kafka producer |
| Conversion Service | 8080 | FFmpeg conversion, waveform, caption, transcription | MinIO both buckets | Kafka consumer |
| Download Service | 8082 | File retrieval and streaming | MinIO output bucket | None |
| QnA Service | 8083 | Text ingestion, LLM Q&A | In-memory | None |
| Frontend | 3000 | React SPA, Vite proxy | None | WebSocket (upload progress) |

---

### 7.3 Technology Stack and Infrastructure

#### Backend Framework

**Spring Boot 3.5.3 with Java 24** is used for all four microservices. Each service is a self-contained Spring Boot application with its own `application.properties` and Maven module under a parent multi-module POM.

The root `pom.xml` defines the parent project with 5 modules:
```
pom.xml (parent)
├── shared-dto          ← Common DTO: JobPayload
├── upload-service      ← Port 8081
├── conversion-service  ← Port 8080
├── download-service    ← Port 8082
└── qna-service         ← Port 8083
```

#### Shared DTO

`JobPayload` is the contract between Upload and Conversion services, carrying:
- `blobName` — unique file identifier in MinIO
- `mimeType` — MIME type of the file
- `outputFormat` — target conversion format

#### Infrastructure (Docker Compose)

```yaml
services:
  kafka:
    image: confluentinc/cp-kafka:latest
    ports: ["9092:9092"]
    # KRaft mode (no ZooKeeper dependency)
    environment:
      KAFKA_PROCESS_ROLES: broker,controller
      KAFKA_NODE_ID: 1
      KAFKA_CONTROLLER_QUORUM_VOTERS: 1@localhost:9093

  minio:
    image: minio/minio
    ports: ["9000:9000", "9001:9001"]
    environment:
      MINIO_ROOT_USER: admin
      MINIO_ROOT_PASSWORD: password123
    command: server /data --console-address ":9001"
```

Notable: Kafka runs in **KRaft mode** (no ZooKeeper), which is the modern Kafka deployment model (Kafka 3.x+).

---

### 7.4 Upload Service (Port 8081)

#### Purpose
Accepts media file uploads from the browser, validates them, stores them in MinIO, and publishes a conversion job event to Kafka.

#### REST API

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/upload` | Upload single file |
| POST | `/upload/batch` | Upload multiple files |

#### Key Components

**UploadController**
Handles multipart form data. Accepts `file` (or `files` for batch) and `outputFormat` parameter. Returns a JSON response containing:
```json
{
  "uploaded": "myvideo.mp4",
  "failed": [],
  "timestamp": "2026-06-30T10:00:00"
}
```

**FileStorageService**
Core upload logic:
1. Validates file extension against whitelist: `mp4, webm, avi, wmv, mp3, wav, weba`
2. Validates MIME type against whitelist
3. Checks for duplicate file names (configurable `allowOverwrite` flag)
4. Auto-versions duplicates: `myvideo.mp4` → `myvideo_v2.mp4`
5. Calls `StorageUploader` to push to MinIO
6. Publishes `JobPayload` to Kafka topic `conversion-jobs`

**StorageUploader (with Retry)**
Uses Spring Retry with `@Retryable`:
```java
@Retryable(
  maxAttempts = 3,
  backoff = @Backoff(delay = 2000, multiplier = 2)
)
public void uploadToStorage(...)
```
Attempts: 1st at 0s, 2nd at 2s, 3rd at 4s. If all fail, `@Recover` handler broadcasts failure via WebSocket.

**WebSocket Progress Broadcasting**
`UploadProgressHandler` maintains a list of active WebSocket sessions (`CopyOnWriteArrayList` for thread safety). Progress messages are broadcast during upload:
```json
{
  "status": "uploading",
  "fileName": "myvideo.mp4",
  "bytesUploaded": "2.5 MB"
}
```

**Kafka Publishing**
`KafkaProducerConfig` creates a `KafkaTemplate<String, JobPayload>` with JSON serialization. The upload service publishes to topic `conversion-jobs` with the blob name as the message key (ensures ordering for same file).

#### Configuration

```properties
upload.allowedExtensions=mp4,webm,avi,wmv,mp3,wav,weba
upload.bucketName=audio-video-converter-files
upload.allowOverwrite=false
spring.servlet.multipart.max-file-size=10000MB
```

#### Exception Handling

`UploadExceptionHandler` (@RestControllerAdvice) handles:
- `FileValidationException` → 400 Bad Request with reason
- `MaxUploadSizeExceededException` → 413 Payload Too Large

---

### 7.5 Conversion Service (Port 8080)

#### Purpose
The most complex service. Processes media files using FFmpeg for format conversion, waveform visualization, captioning, and transcription. Operates in two modes:
1. **Async** — listens to Kafka for background conversion jobs
2. **Sync** — REST API for on-demand conversion with streaming response

#### REST API

| Method | Endpoint | Description | Output |
|--------|----------|-------------|--------|
| POST | `/convert` | Convert media format | `{blobName}_converted.{format}` |
| POST | `/convert/waveform` | Generate waveform MP4 | `{blobName}_waveform.mp4` |
| POST | `/convert/caption` | Burn captions into video | `{blobName}_captioned.mp4` |
| POST | `/convert/transcribe` | Speech-to-text | `{blobName}_transcription.txt` |

#### FFmpeg Integration

FFmpeg is wrapped using the **Jaffree** library (Java FFmpeg wrapper), avoiding shell injection vulnerabilities from raw `Runtime.exec()`.

**MediaConversionService** is the core conversion engine:

1. **Codec Resolution** — Maps output format to appropriate video/audio codec:

| Output Format | Video Codec | Audio Codec |
|---------------|-------------|-------------|
| mp4, mov, m4a | libx264 | aac |
| webm | libvpx | libopus |
| mkv | libx264 | aac |
| avi | mpeg4 | mp3 |
| gif | gif | (none) |
| wav, flac | (none) | pcm_s16le / flac |

The codec mapping covers 100+ formats comprehensively.

2. **Smart Output Strategy** — `SmartOutputStrategy` selects between:
    - `ChannelOutput` (file-based) for formats requiring seek: `wmv, asf, mkv, mp4, mp3, wav`
    - `PipeOutput` (streaming) for all others
    - Forces `ChannelOutput` for files > 1 GB

3. **Duration/Bitrate Detection** — Uses FFprobe to detect input file duration and bitrate for output size estimation

#### Waveform Video Generation (WaveformVideoService)

Generates an MP4 waveform visualization of an audio file:

**Without background image:**
```
FFmpeg filtergraph:
[0:a] showwaves=s=1280x720:mode=line:rate=25:colors=00FF00 [v]
Output: H.264 video + AAC audio
```

**With background image:**
```
FFmpeg filtergraph:
[0:v] scale=1280:720,setsar=1 [bg]
[1:a] showwaves=s=1280x720:mode=line:rate=25:colors=00FF00:scale=sqrt [waves]
[bg][waves] overlay=0:H/2-h/2 [v]
Output: H.264 video + AAC audio
```

#### Caption Video Generation (CaptionVideoService)

Generates a video with burned-in captions:
1. Downloads audio from MinIO
2. Runs Whisper (model: `base`, output: SRT format):
```bash
whisper.exe <audio_file> --model base --output_format srt --output_dir <tempDir>
```
3. Burns subtitles into video using FFmpeg subtitle filter:
```
FFmpeg filter:
geq=r='...':g='...':b='...' (animated background)
subtitles=caption_input.srt:force_style='FontName=Arial,FontSize=24,PrimaryColour=&H00FFFFFF'
```

#### Transcription Service (TranscriptionService)

1. Downloads media from MinIO
2. Runs Whisper (model: `base`, output: TXT format)
3. Returns text content and uploads .txt to MinIO output bucket

#### Kafka Consumer

`ConversionJobListener` subscribes to `conversion-jobs` topic:
```java
@KafkaListener(topics = "${app.topic.conversion-jobs}", groupId = "conversion-service")
public void onConversionJob(JobPayload payload) {
    conversionService.convertMedia(...)
}
```
Auto-resolves MIME type from output format using `MimeTypeUtil`.

#### Async Streaming Response

All conversion endpoints return `StreamingResponseBody` — Spring's mechanism for streaming large files without buffering in memory. After conversion completes and the result is stored in MinIO, the file is streamed directly back to the client:
```java
StreamingResponseBody responseBody = outputStream -> {
    try (InputStream stream = gcsHelper.fetchFromGCS(outputBucket, resultBlob)) {
        stream.transferTo(outputStream);
    } catch (IOException e) {
        if (e.getMessage().contains("aborted") || e.getMessage().contains("Broken pipe")) {
            return; // client disconnected — not a server error
        }
        throw e;
    }
};
```

#### Configuration

```properties
ffmpeg.path=C:/ffmpeg/bin
convert.temp.path=C:/converter-convert-temp
whisper.path=C:/Python312/Scripts/whisper.exe
spring.mvc.async.request-timeout=1800000  # 30 minutes
spring.servlet.multipart.max-file-size=200MB
```

---

### 7.6 Download Service (Port 8082)

#### Purpose
A lightweight service responsible for retrieving processed files from the MinIO output bucket and streaming them to the browser.

#### REST API

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/download` | Download processed file |

#### Request Body (JobPayload)
```json
{
  "blobName": "myvideo.mp4_converted.mp3",
  "mimeType": "audio/mpeg",
  "outputFormat": "mp3"
}
```

#### Download Flow

```java
// DownloadService.download()
1. Check object exists in MinIO (throws 404 if not found)
2. Fetch InputStream from MinIO
3. Build filename: {blobName}.{outputFormat}
4. Set headers:
   - Content-Type: {mimeType}
   - Content-Disposition: attachment; filename="{filename}"
5. Return InputStreamResource (streamed to client)
```

#### Blob Naming Convention

The Download page provides a reference table showing expected output blob names:

| Operation | Input | Output Blob Name |
|-----------|-------|-----------------|
| Format Convert | `file.mp4` | `file.mp4_converted.mp3` |
| Waveform | `song.mp3` | `song.mp3_waveform.mp4` |
| Caption | `video.mp4` | `video.mp4_captioned.mp4` |
| Transcription | `audio.wav` | `audio.wav_transcription.txt` |

#### Configuration

```properties
server.port=8082
app.minio.bucket-name=audio-video-converter-converted-files
```

---

### 7.7 QnA (Intelligence) Service (Port 8083)

#### Purpose
Provides AI-powered question answering over ingested content. Supports multiple ingestion modalities (file, URL, YouTube) and uses a local Ollama LLM with a cloud HuggingFace fallback.

#### REST API

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/qna/ingest` | Ingest a file |
| POST | `/qna/ingest-url` | Ingest a URL or YouTube link |
| POST | `/qna/ask` | Ask a question |
| DELETE | `/qna/context` | Clear all context |
| GET | `/qna/context/status` | Get context status |

#### Ingestion Pipeline

**File Ingestion (ingestFile)**

```
Incoming File
      │
      ├── Audio/Video (mp4/mp3/wav/webm/avi/wmv/mov/flv/m4a/weba/ogg/opus/flac)
      │         └── Whisper (tiny model) → plain text
      │
      ├── PDF (.pdf)
      │         └── Apache PDFBox → plain text
      │
      ├── DOCX (.docx)
      │         └── Apache POI (XWPFDocument) → plain text
      │
      └── Other (txt/csv/md/etc.)
                └── UTF-8 string read
                        │
                        ▼
               ContextStore.add(ContextChunk(source, text))
```

**URL Ingestion (ingestUrl)**

```
Incoming URL
      │
      ├── YouTube URL (youtube.com/watch or youtu.be/)
      │         └── yt-dlp (-x --audio-format mp3) → Whisper (tiny) → text
      │
      └── Webpage URL
                └── JSoup.connect() → remove [nav/header/footer/script/aside/refs]
                          → truncate to 8,000 chars → text
                                  │
                                  ▼
                         ContextStore.add(ContextChunk(source, text))
```

#### Question Answering Pipeline

```
POST /qna/ask { "question": "..." }
        │
        ▼
1. Build context: ContextStore.buildContext(4000 chars)
        │
        ▼
2. Try Ollama (Primary)
   POST http://localhost:11434/api/generate
   {
     "model": "llama3.2:1b",
     "prompt": "...context...question...",
     "stream": false
   }
        │
   Success → return AskResponse(question, answer, "ollama/llama3.2:1b")
        │
   Failure ↓
        ▼
3. Try HuggingFace (Fallback)
   POST https://api-inference.huggingface.co/models/deepset/roberta-base-squad2
   { "inputs": { "question": "...", "context": "...2000 chars..." } }
        │
   Success → return AskResponse(question, answer, "huggingface/deepset-roberta-base-squad2")
        │
   Failure ↓
        ▼
4. Return error: "Both ollama and huggingface unavailable"
```

**LLM Prompt Template:**
```
You are a helpful assistant. Answer the question based only on the context provided below.
If the answer is not in the context, say 'I could not find the answer in the provided context.'

CONTEXT:
{context — max 4,000 chars}

QUESTION:
{question}

ANSWER:
```

#### Context Store

`ContextStore` is a Spring `@Component` backed by an `ArrayList<ContextChunk>`:
- **Per-chunk storage** — each file/URL is a separate chunk
- **Context building** — concatenates chunks with source headers until char limit reached
- **In-memory only** — cleared on service restart

```
ContextStore (in-memory)
├── Chunk 1: { source: "lecture.mp3", text: "...transcript..." }
├── Chunk 2: { source: "https://en.wikipedia.org/wiki/...", text: "...8000 chars..." }
└── Chunk 3: { source: "report.pdf", text: "...extracted text..." }
```

#### OkHttp Client Configuration

```java
OkHttpClient httpClient = new OkHttpClient.Builder()
    .connectTimeout(10, TimeUnit.SECONDS)
    .readTimeout(120, TimeUnit.SECONDS)   // Allow slow LLM generation
    .writeTimeout(30, TimeUnit.SECONDS)
    .build();
```

#### Configuration

```properties
server.port=8083
ollama.base-url=http://localhost:11434
ollama.model=llama3.2:1b
huggingface.api-url=https://api-inference.huggingface.co/models/deepset/roberta-base-squad2
qna.temp.path=C:/converter-convert-temp
whisper.path=C:/Python312/Scripts/whisper.exe
ffmpeg.path=C:/ffmpeg/bin
yt-dlp.path=C:/Python312/Scripts/yt-dlp.exe
spring.mvc.async.request-timeout=1800000
spring.servlet.multipart.max-file-size=500MB
```

---

### 7.8 Frontend Application (Port 3000)

#### Technology

- **React 18.2** with **TypeScript** and **Vite 5.1**
- Single-Page Application (SPA) with state-based routing (no React Router)
- Vite dev server acts as a reverse proxy to all backend services

#### Vite Proxy Configuration

```typescript
proxy: {
  '/upload':   { target: 'http://localhost:8081', changeOrigin: true },
  '/convert':  { target: 'http://localhost:8080', changeOrigin: true,
                 timeout: 3000000, proxyTimeout: 3000000 },
  '/download': { target: 'http://localhost:8082', changeOrigin: true },
  '/qna':      { target: 'http://localhost:8083', changeOrigin: true },
}
```

The `/convert` proxy has an extended 50-minute timeout to accommodate long FFmpeg operations.

#### Application Layout

```
App.tsx
├── Sidebar (fixed left)
│   ├── Logo: "MediaMind"
│   ├── Media Pipeline
│   │   ├── 📤 Upload
│   │   ├── 🔄 Convert
│   │   └── ⬇️ Download
│   ├── Intelligence
│   │   └── 🤖 Q&A
│   └── Service status bar (ports 8081/8080/8082/8083)
└── Main Content Area
    └── {Active Page Component}
```

#### Pages

**Upload Page (UploadPage.tsx)**
- **Single Upload tab** — Drag-and-drop file input, format selector (mp4/webm/avi/wmv/mp3/wav/weba), progress display, success/failure alerts
- **Batch Upload tab** — Multi-file list with individual remove, bulk submit

**Convert Page (ConvertPage.tsx)**
- **Format Convert tab** — Blob name + target format + optional MIME type → downloads converted file
- **Waveform Video tab** — Audio blob name + optional background image → generates/downloads MP4 waveform visualization
- **Add Captions tab** — Video blob name → Whisper + subtitle burn → downloads captioned MP4
- **Transcribe tab** — Audio/video blob name → Whisper → downloads .txt + shows text preview

**Download Page (DownloadPage.tsx)**
- Blob name input + format selector with MIME auto-mapping
- Editable MIME type field
- Reference table showing naming conventions for all output types

**Q&A Page (QaPage.tsx)**
- **Status bar** — Shows chunks loaded count and active sources
- **Ingest Content tab:**
    - File ingest with drag-and-drop (supports audio/video/PDF/DOCX/text)
    - URL ingest (webpage or YouTube link)
- **Ask Questions tab** — Chat-style interface with user/assistant bubbles, model-used indicator, auto-scroll
- **Manage Context tab** — Lists loaded sources with clear-all button

#### API Service Layer (api.ts)

All backend calls are centralized in `api.ts`. Long-running conversion calls use `AbortController` with a 5-minute timeout:

```typescript
const controller = new AbortController()
const timeout = setTimeout(() => controller.abort(), 300000)
try {
  const res = await fetch(`/convert/waveform`, {
    method: 'POST', body: form, signal: controller.signal
  })
  return res.blob()
} finally {
  clearTimeout(timeout)
}
```

---

### 7.9 Practical Problems Identified and Solutions

As per the Industrial Training guidelines, 4 practical problems were identified during the training and solved:

---

#### Problem 1: Media Conversion Across 100+ Formats Without Tight Coupling

**Problem:** Different output formats require different FFmpeg codecs, container settings, and output strategies. Hardcoding this logic in the controller would create an unmaintainable monolith.

**Analysis:**
- FFmpeg supports 100+ containers and codecs
- Some formats (mp4, mkv) require seekable output (cannot be piped)
- Large files need disk-backed temp output
- Different video/audio codecs are required per format

**Solution:**
Three separate utility classes were designed:
1. `MimeTypeUtil` — 100+ format-to-MIME mappings
2. `SmartOutputStrategy` — decides `ChannelOutput` (disk) vs `PipeOutput` (stream) based on format and file size
3. `MediaConversionService` — comprehensive codec mapping with custom codec/argument override support

**Result:** Adding a new format requires only a one-line entry in the codec map — no controller changes needed.

---

#### Problem 2: HTTP Timeout on Long-Running FFmpeg and Whisper Jobs

**Problem:** FFmpeg waveform/caption generation and Whisper transcription can take 1–10 minutes. Standard HTTP connections time out, causing `IOException: An established connection was aborted` errors and broken downloads.

**Analysis:**
- Browser fetch default: no explicit timeout but proxy has ~60s default
- Vite proxy default timeout: 60 seconds
- Whisper `base` model: ~60 seconds per minute of audio
- Spring default async timeout: 30 seconds

**Root causes identified:**
1. Vite proxy had no timeout configured for `/convert`
2. Spring async timeout was default (too short)
3. Frontend `fetch()` had no AbortController signal
4. Server tried to write to a dead socket → `IOException`

**Solution:**
```
Fix 1: vite.config.ts → timeout: 3000000, proxyTimeout: 3000000
Fix 2: application.properties → spring.mvc.async.request-timeout=1800000
Fix 3: api.ts → AbortController with 300000ms timeout
Fix 4: ConversionController.java → catch broken pipe IOException silently
```

**Result:** Long-running jobs complete successfully with the file downloading to the browser.

---

#### Problem 3: QnA Page Rendering Blank on Load

**Problem:** Navigating to the Q&A tab caused the page to go blank (white screen) after a brief loading flash.

**Analysis:**
- `QaPage` fires `getContextStatus()` via `useEffect` on mount
- `getContextStatus()` calls `GET /qna/context/status`
- If QnA service is down or returns a non-200 response, `res.json()` throws on the HTML error page body
- React has no error boundary around `QaPage`
- Uncaught exception crashes the component tree → blank page

**Solution:**
```typescript
// api.ts — safe default on non-OK response
export async function getContextStatus(): Promise<ContextStatus> {
  const res = await fetch(`${QA_BASE}/context/status`)
  if (!res.ok) return { chunksLoaded: 0, sources: [] }
  return res.json()
}

// QaPage.tsx — reset state on error
const refreshStatus = useCallback(async () => {
  try {
    setContextStatus(await getContextStatus())
  } catch {
    setContextStatus({ chunksLoaded: 0, sources: [] })
  }
}, [])
```

**Result:** Q&A tab loads correctly and shows "0 chunks, no sources" when the QnA service is unavailable.

---

#### Problem 4: Slow QnA Ingest and Response Times

**Problem:** Ingesting a Wikipedia URL took 5–15 seconds. Asking a question took 20–40 seconds.

**Analysis:**
- Wikipedia articles are 100,000–200,000 characters — JSoup returned the full text
- Whisper `base` model is slower than needed for Q&A use case
- Ollama `llama3.2` (3B parameter model) is slow on CPU
- Context sent to LLM was 12,000 characters — too large for fast inference

**Root causes:**
1. `scrapeWebPage()` had no text length limit
2. Whisper model was `base` instead of the faster `tiny`
3. Ollama model was `llama3.2` (3B) instead of `llama3.2:1b` (1B)
4. `MAX_CONTENT_CHARS = 12_000` — too large

**Solutions:**

```java
// IngestService.java — truncate web scrape + remove Wikipedia noise
private static final int MAX_WEBPAGE_CHARS = 8_000;
doc.select("nav,header,footer,script,style,iframe,aside,.reflist,.references,.mw-references-wrap").remove();
return text.length() > MAX_WEBPAGE_CHARS ? text.substring(0, MAX_WEBPAGE_CHARS) : text;

// IngestService.java — faster Whisper model
"--model", "tiny"  // was: "base"

// QnAService.java — less context = faster LLM
private static final int MAX_CONTENT_CHARS = 4_000;  // was: 12_000

// application.properties — faster Ollama model
ollama.model=llama3.2:1b  // was: llama3.2
```

**Result:**

| Metric | Before | After |
|--------|--------|-------|
| Wikipedia ingest | 5–15 sec | < 2 sec |
| Whisper audio ingest | ~60s/min audio | ~20s/min audio |
| Ask response (Ollama) | 20–40 sec | 4–10 sec |

---

---

## 8. LEARNING AFTER TRAINING

### 8.1 Technical Skills Acquired

**Distributed Systems Design**
- Hands-on experience with event-driven architecture using Apache Kafka
- Understanding of producer-consumer decoupling for long-running jobs
- Practical use of S3-compatible object storage (MinIO) as a file communication bus between services

**Spring Boot Microservices**
- Multi-module Maven project structure
- Spring Retry with exponential backoff for resilience
- Spring Kafka — both producer and consumer configuration
- `StreamingResponseBody` for large file streaming without memory buffering
- `@ConfigurationProperties` for type-safe external configuration
- `@RestControllerAdvice` for centralized exception handling
- WebSocket integration for real-time progress updates

**Media Processing**
- FFmpeg codec selection, filter graphs, and output strategies
- Waveform visualization using `showwaves` FFmpeg filter
- Subtitle burning using `subtitles` filter with force_style
- Whisper CLI integration for speech-to-text
- yt-dlp for YouTube audio extraction

**AI / LLM Integration**
- Running local LLMs via Ollama REST API
- Prompt engineering for context-grounded Q&A
- HuggingFace Inference API as a cloud fallback
- Tradeoffs between model size (1b vs 3b) and inference speed

**Frontend Development**
- React 18 with TypeScript — state management, hooks (useState, useEffect, useCallback, useRef)
- Vite proxy configuration for development environment
- `AbortController` for fetch timeout management
- Drag-and-drop file upload UX
- Chat interface implementation

**Debugging and Performance**
- Diagnosing broken pipe IOExceptions in streaming responses
- Identifying proxy timeout mismatches between Vite and Spring
- Profiling QnA slowness to LLM model size and context window

### 8.2 Professional and Soft Skills

- **Code Review Practices** — Reading and understanding production-grade codebases written by others
- **Documentation** — Writing structured technical documentation following organizational standards
- **Problem Formulation** — Translating observed system failures into well-defined problems with root cause analysis
- **Estimation** — Assessing performance improvements before and after optimization
- **Agile Practices** — Working in iterative cycles, testing each service independently before integration

### 8.3 Graduate Attributes Demonstrated

As per NIT Tiruchirappalli guidelines, the following graduate attributes were demonstrated:

| Attribute | How Demonstrated |
|-----------|-----------------|
| **Engineering Knowledge** | Applied Spring Boot, Kafka, MinIO, FFmpeg, Whisper, React in an integrated system |
| **Problem Analysis** | Diagnosed 4 distinct problems (timeout, blank page, slow QnA, path issues) with root cause analysis |
| **Design/Development of Solutions** | Designed the full 5-service microservices architecture from scratch |
| **Investigation of Complex Problems** | Traced broken pipe IOException through Spring's async request pipeline |
| **Modern Tool Usage** | Used Kafka, MinIO, Docker, Jaffree, Ollama, Whisper, yt-dlp, PDFBox, JSoup |
| **Individual and Team Work** | Independently built and integrated all services while following organizational coding standards |
| **Communication** | Produced detailed technical documentation and structured problem-solution analysis |
| **Project Management** | Managed 8-week timeline across infrastructure, backend, AI integration, and frontend |

---

---

## 9. SUMMARY

### Project Overview

**MediaMind** is a full-stack, cloud-native, AI-powered media processing platform built during the Industrial Training programme. The system follows a microservices architecture with 4 Spring Boot services, a React frontend, Apache Kafka for async messaging, and MinIO for object storage.

### What Was Built

| Component | Technology | Status |
|-----------|-----------|--------|
| Upload Service | Spring Boot, MinIO, Kafka, WebSocket, Spring Retry | Complete |
| Conversion Service | Spring Boot, FFmpeg (Jaffree), Whisper, MinIO, Kafka | Complete |
| Download Service | Spring Boot, MinIO | Complete |
| QnA Service | Spring Boot, Whisper, Ollama, HuggingFace, PDFBox, POI, JSoup | Complete |
| Frontend | React 18, TypeScript, Vite | Complete |
| Infrastructure | Docker Compose (Kafka KRaft + MinIO) | Complete |

### Key Features Delivered

1. **Multi-format media upload** with validation, retry, and real-time WebSocket progress
2. **100+ format conversion** via FFmpeg with smart codec/strategy selection
3. **Waveform video generation** with optional background image
4. **Auto-captioning** using Whisper SRT + FFmpeg subtitle filter
5. **Speech-to-text transcription** with .txt download
6. **AI Q&A over documents** — PDF, DOCX, text, web pages, YouTube videos
7. **Local LLM inference** (Ollama llama3.2:1b) with HuggingFace fallback

### Practical Problems Solved

| Problem | Impact | Solution |
|---------|--------|----------|
| HTTP timeout on long FFmpeg/Whisper jobs | Files not downloading | Proxy + async timeout + AbortController + broken pipe handler |
| QnA page blank on load | Feature unusable | Safe default on API failure + error boundary in state |
| Slow Wikipedia ingest | Poor UX | Text truncation + noise removal |
| Slow LLM responses | Poor UX | llama3.2:1b model + context reduction 12k→4k chars |

### Conclusion

This Industrial Training provided deep, practical exposure to building a production-grade distributed system. The MediaMind project covered the complete engineering lifecycle — from architectural design through implementation, debugging, and performance optimization. The combination of event-driven backend services, AI tool integration, and modern React frontend development has significantly enhanced both technical competence and engineering problem-solving ability.

The experience of diagnosing real production issues (streaming failures, timeouts, LLM performance) and tracing them to root causes in distributed systems provided learning that complements the theoretical knowledge acquired in the PG programme and prepares for professional software engineering roles.

---

---

*Report prepared by: KHUSHI GUPTA*
*Programme: PG — Computer Science and Engineering*
*National Institute of Technology, Tiruchirappalli — 620 015*
*Training Period: Summer 2026*

---

**[End of Report]**
