# MediaSense — Run Guide (macOS)

Assumes you've already completed **setup.md** (tools installed, project built, Docker
infrastructure up, MinIO buckets created). This guide covers starting the services and using
the APIs.

Run each `... spring-boot:run` command in its own terminal tab/window (5 total: 4 services +
frontend), with Docker Desktop running in the background.

## 1. Run each service

Open a separate terminal per service. Confirm Kafka/MinIO are already up
(`docker compose ps` from the project root) before starting conversion-service.

**upload-service:**
```bash
cd /path/to/MediaSense-AI-Powered-Multimedia-Intelligence-Platform/upload-service
mvn spring-boot:run
```

**conversion-service** (no overrides needed if ffmpeg/temp paths match setup.md exactly —
i.e. you're on Apple Silicon with `ffmpeg` at `/opt/homebrew/bin` and `whisper` on your `PATH`):
```bash
cd /path/to/MediaSense-AI-Powered-Multimedia-Intelligence-Platform/conversion-service
mvn spring-boot:run
```
If your ffmpeg/whisper install lives elsewhere (e.g. Intel Mac Homebrew prefix), override instead:
```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--ffmpeg.path=/usr/local/bin --convert.temp.path=/tmp/mediasense-temp --whisper.path=/usr/local/bin/whisper"
```

**download-service:**
```bash
cd /path/to/MediaSense-AI-Powered-Multimedia-Intelligence-Platform/download-service
mvn spring-boot:run
```

**qna-service** (no overrides needed if paths match setup.md exactly):
```bash
cd /path/to/MediaSense-AI-Powered-Multimedia-Intelligence-Platform/qna-service
mvn spring-boot:run
```
Override example if installed elsewhere:
```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--ffmpeg.path=/usr/local/bin --qna.temp.path=/tmp/mediasense-temp --whisper.path=/usr/local/bin/whisper --yt-dlp.path=/usr/local/bin/yt-dlp"
```

Confirm each is up (run in any terminal):
```bash
curl -s -o /dev/null -w "upload:%{http_code}\n"     -X POST http://localhost:8081/upload
curl -s -o /dev/null -w "conversion:%{http_code}\n" http://localhost:8080/convert
curl -s -o /dev/null -w "download:%{http_code}\n"   -X POST http://localhost:8082/download
curl -s -o /dev/null -w "qna:%{http_code}\n"        http://localhost:8083/qna/context/status
```
(400/405 responses are fine here — they just confirm the port is listening and routed.)

## 2. Run the frontend

```bash
cd /path/to/MediaSense-AI-Powered-Multimedia-Intelligence-Platform/frontend
npm install
npm run dev
```

Open **http://localhost:3000**. Vite's dev server proxies `/upload`, `/convert`, `/download`,
`/qna` to the four backend ports (see `frontend/vite.config.ts`).

## 3. Full API reference

### upload-service — `:8081`

| Method | Path | Body | Notes |
|---|---|---|---|
| POST | `/upload` | multipart: `file`, `outputFormat` | Validates extension (`mp4,webm,avi,wmv,mp3,wav,weba`) + MIME type, stores in MinIO bucket `audio-video-converter-files`, publishes a Kafka `conversion-jobs` event if `outputFormat` is set |
| POST | `/upload/batch` | multipart: `files` (multiple), `outputFormat` | Same as above for multiple files |

A WebSocket endpoint is also exposed at `ws://localhost:8081/upload-progress` — connected clients
receive plain-text progress/failure broadcasts during an upload (the current frontend does not
consume this; it's polled only by whatever connects directly).

```bash
curl -X POST http://localhost:8081/upload -F "file=@/path/to/sample.mp4" -F "outputFormat=mp3"
```

### conversion-service — `:8080`

| Method | Path | Body (multipart) | Response |
|---|---|---|---|
| POST | `/convert` | `blobName`, `format`, `mimeType?`, `codecs?`, `arguments?` | Converted file stream |
| POST | `/convert/waveform` | `blobName`, `backgroundImage?` | Waveform video (`*_waveform.mp4`) |
| POST | `/convert/caption` | `blobName` | Captioned video (`*_captioned.mp4`) — runs Whisper for subtitles |
| POST | `/convert/transcribe` | `blobName` | Plain-text transcript (Whisper) |

`blobName` is the object name returned by `/upload` (the stored file key in MinIO).

```bash
curl -X POST http://localhost:8080/convert/transcribe -F "blobName=sample.mp4" -o transcript.txt
```

Also listens on Kafka topic `conversion-jobs` — any message published by upload-service
triggers the same conversion pipeline as `/convert`, writing the result to bucket
`audio-video-converter-converted-files`.

### download-service — `:8082`

| Method | Path | Body (JSON) | Response |
|---|---|---|---|
| POST | `/download` | `{ "blobName": "...", "mimeType": "...", "outputFormat": "..." }` | File stream from the converted bucket |

```bash
curl -X POST http://localhost:8082/download \
  -H "Content-Type: application/json" \
  -d '{"blobName":"sample.mp4","mimeType":"audio/mpeg","outputFormat":"mp3"}' \
  -o sample.mp3
```

### qna-service — `:8083`

| Method | Path | Body | Notes |
|---|---|---|---|
| POST | `/qna/ingest` | multipart `file` | Accepts audio/video (Whisper), `.pdf`, `.docx`, or plain text |
| POST | `/qna/ingest-url` | JSON `{ "url": "..." }` | YouTube URL → yt-dlp + Whisper; any other URL → scraped via Jsoup |
| POST | `/qna/ask` | JSON `{ "question": "..." }` | Answers from ingested context via Ollama, falls back to HuggingFace `deepset/roberta-base-squad2` |
| DELETE | `/qna/context` | — | Clears all ingested context |
| GET | `/qna/context/status` | — | `{ chunksLoaded, sources }` |

Note: the `/qna/ask` response field is `moduleUsed` (not `modelUsed`) due to a naming bug in
`AskResponse.java` — the frontend's "model used" chat badge reads `modelUsed` and will always
come back blank. Call the endpoint directly (below) to see the real value.

```bash
curl -X POST http://localhost:8083/qna/ingest-url \
  -H "Content-Type: application/json" \
  -d '{"url":"https://en.wikipedia.org/wiki/Spring_Boot"}'

curl -X POST http://localhost:8083/qna/ask \
  -H "Content-Type: application/json" \
  -d '{"question":"What is Spring Boot?"}'
```

## 4. End-to-end smoke test

```bash
# 1. Upload and request an mp3 conversion (fires a Kafka job automatically)
curl -X POST http://localhost:8081/upload -F "file=@sample.mp4" -F "outputFormat=mp3"

# 2. Wait a few seconds for conversion-service's Kafka listener to process it, then download
curl -X POST http://localhost:8082/download \
  -H "Content-Type: application/json" \
  -d '{"blobName":"sample.mp4","mimeType":"audio/mpeg","outputFormat":"mp3"}' \
  -o sample.mp3
```

## 5. Troubleshooting

| Symptom | Cause | Fix |
|---|---|---|
| `./mvnw` fails with a wrapper/download error | `.mvn/wrapper` folder is missing from the repo | Use a globally installed `mvn` instead of `./mvnw` |
| Maven build fails on `java.version=24` | JDK 24 not installed / `JAVA_HOME` points elsewhere | `brew install openjdk@24`, `export JAVA_HOME="$(/usr/libexec/java_home -v 24)"`, open a new terminal |
| `mvn`/`ffmpeg`/`ollama` not found | Homebrew's bin dir not on `PATH` in this shell | Open a new terminal (or `source ~/.zshrc`) after installing |
| Docker Desktop won't start | Not enough resources allocated, or it's not actually running | Open Docker Desktop manually and wait for the whale icon to settle before `docker compose up` |
| `Object not found in MinIO` on convert/download | Bucket doesn't exist, or upload didn't complete | Create both buckets (setup.md step 5); confirm the file is visible in the MinIO console |
| Conversion/caption/transcribe endpoints throw process errors | `ffmpeg`/`ffprobe` not at the configured `ffmpeg.path` | Run `which ffmpeg` and compare to the configured path; override `--ffmpeg.path` if they differ (common on Intel Macs: `/usr/local/bin` vs the default `/opt/homebrew/bin`) |
| Whisper-based endpoints fail with non-zero exit code | Whisper CLI not on the given path, or missing model download | Confirm `which whisper && whisper --help`; first Whisper run downloads the model (needs internet) |
| `qna/ask` always answers via HuggingFace, never Ollama | Ollama not installed/running, or model not pulled | `ollama pull llama3.2:1b`, confirm `curl http://localhost:11434` responds |
| Kafka consumer never fires | Kafka not up, or wrong `bootstrap-servers` | `docker compose ps`, confirm `localhost:9092` reachable |
| Frontend calls 404/502 | Backend service on that port isn't running | Check the four `mvn spring-boot:run` terminals for errors |

## 6. Shutdown

```bash
docker compose down          # stops Kafka + MinIO (add -v to also wipe volumes/data)
# Ctrl+C in each mvn spring-boot:run terminal and the npm run dev terminal
```