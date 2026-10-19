# MediaMind — Run Guide (Windows)

Assumes you've already completed **SETUP.md** (tools installed, project built, Docker
infrastructure up, MinIO buckets created). This guide covers starting the services and using
the APIs.

Run each `... spring-boot:run` command in its own PowerShell window (5 windows total: 4
services + frontend), with Docker Desktop running in the background.

## 1. Run each service

Open a separate PowerShell window per service. Confirm Kafka/MinIO are already up
(`docker compose ps` from the project root) before starting conversion-service.

**upload-service:**
```powershell
cd C:\path\to\MediaMind\upload-service
mvn spring-boot:run
```

**conversion-service** (no overrides needed if ffmpeg/temp paths match SETUP.md exactly):
```powershell
cd C:\path\to\MediaMind\conversion-service
mvn spring-boot:run
```
If your ffmpeg/whisper install lives elsewhere, override instead:
```powershell
mvn spring-boot:run "-Dspring-boot.run.arguments=--ffmpeg.path=D:/tools/ffmpeg/bin --convert.temp.path=D:/tmp/mediamind --whisper.path=D:/Python312/Scripts/whisper.exe"
```

**download-service:**
```powershell
cd C:\path\to\MediaMind\download-service
mvn spring-boot:run
```

**qna-service** (no overrides needed if paths match SETUP.md exactly):
```powershell
cd C:\path\to\MediaMind\qna-service
mvn spring-boot:run
```
Override example if installed elsewhere:
```powershell
mvn spring-boot:run "-Dspring-boot.run.arguments=--ffmpeg.path=D:/tools/ffmpeg/bin --qna.temp.path=D:/tmp/mediamind --whisper.path=D:/Python312/Scripts/whisper.exe --yt-dlp.path=D:/Python312/Scripts/yt-dlp.exe"
```

Confirm each is up (run in any PowerShell window, `curl.exe` ships with Windows 10/11):
```powershell
curl.exe -s -o NUL -w "upload:%{http_code}`n"     -X POST http://localhost:8081/upload
curl.exe -s -o NUL -w "conversion:%{http_code}`n" http://localhost:8080/convert
curl.exe -s -o NUL -w "download:%{http_code}`n"   -X POST http://localhost:8082/download
curl.exe -s -o NUL -w "qna:%{http_code}`n"        http://localhost:8083/qna/context/status
```
(400/405 responses are fine here — they just confirm the port is listening and routed.)

## 2. Run the frontend

```powershell
cd C:\path\to\MediaMind\frontend
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

```powershell
curl.exe -X POST http://localhost:8081/upload -F "file=@C:\path\to\sample.mp4" -F "outputFormat=mp3"
```

### conversion-service — `:8080`

| Method | Path | Body (multipart) | Response |
|---|---|---|---|
| POST | `/convert` | `blobName`, `format`, `mimeType?`, `codecs?`, `arguments?` | Converted file stream |
| POST | `/convert/waveform` | `blobName`, `backgroundImage?` | Waveform video (`*_waveform.mp4`) |
| POST | `/convert/caption` | `blobName` | Captioned video (`*_captioned.mp4`) — runs Whisper for subtitles |
| POST | `/convert/transcribe` | `blobName` | Plain-text transcript (Whisper) |

`blobName` is the object name returned by `/upload` (the stored file key in MinIO).

```powershell
curl.exe -X POST http://localhost:8080/convert/transcribe -F "blobName=sample.mp4" -o transcript.txt
```

Also listens on Kafka topic `conversion-jobs` — any message published by upload-service
triggers the same conversion pipeline as `/convert`, writing the result to bucket
`audio-video-converter-converted-files`.

### download-service — `:8082`

| Method | Path | Body (JSON) | Response |
|---|---|---|---|
| POST | `/download` | `{ "blobName": "...", "mimeType": "...", "outputFormat": "..." }` | File stream from the converted bucket |

```powershell
curl.exe -X POST http://localhost:8082/download `
  -H "Content-Type: application/json" `
  -d '{\"blobName\":\"sample.mp4\",\"mimeType\":\"audio/mpeg\",\"outputFormat\":\"mp3\"}' `
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

```powershell
curl.exe -X POST http://localhost:8083/qna/ingest-url `
  -H "Content-Type: application/json" `
  -d '{\"url\":\"https://en.wikipedia.org/wiki/Spring_Boot\"}'

curl.exe -X POST http://localhost:8083/qna/ask `
  -H "Content-Type: application/json" `
  -d '{\"question\":\"What is Spring Boot?\"}'
```

## 4. End-to-end smoke test

```powershell
# 1. Upload and request an mp3 conversion (fires a Kafka job automatically)
curl.exe -X POST http://localhost:8081/upload -F "file=@sample.mp4" -F "outputFormat=mp3"

# 2. Wait a few seconds for conversion-service's Kafka listener to process it, then download
curl.exe -X POST http://localhost:8082/download `
  -H "Content-Type: application/json" `
  -d '{\"blobName\":\"sample.mp4\",\"mimeType\":\"audio/mpeg\",\"outputFormat\":\"mp3\"}' `
  -o sample.mp3
```

## 5. Troubleshooting

| Symptom | Cause | Fix |
|---|---|---|
| `mvnw.cmd` fails with a wrapper/download error | `.mvn/wrapper` folder is missing from the repo | Use a globally installed `mvn` instead of `mvnw.cmd` |
| Maven build fails on `java.version=24` | JDK 24 not installed / `JAVA_HOME` points elsewhere | Install Temurin 24, set `JAVA_HOME` and `PATH`, open a new PowerShell window |
| `'mvn'`/`'ffmpeg'`/`'ollama'` not recognized | PATH not updated in the current shell | Close and reopen PowerShell after installing, or log out/in |
| Docker Desktop won't start / "WSL2 required" | WSL2 not enabled | `wsl --install` in an admin PowerShell, reboot, relaunch Docker Desktop |
| `Object not found in MinIO` on convert/download | Bucket doesn't exist, or upload didn't complete | Create both buckets (SETUP.md step 6); confirm the file is visible in the MinIO console |
| Conversion/caption/transcribe endpoints throw process errors | `ffmpeg.exe`/`ffprobe.exe` not at the configured `ffmpeg.path` | Verify `C:\ffmpeg\bin\ffmpeg.exe -version` works from cmd, and the path matches what the service was started with |
| Whisper-based endpoints fail with non-zero exit code | Whisper CLI not on the given path, or missing model download | Confirm `C:\Python312\Scripts\whisper.exe --help`; first Whisper run downloads the `tiny` model (needs internet) |
| `qna/ask` always answers via HuggingFace, never Ollama | Ollama not installed/running, or model not pulled | `ollama pull llama3.2:1b`, confirm `curl.exe http://localhost:11434` responds |
| Kafka consumer never fires | Kafka not up, or wrong `bootstrap-servers` | `docker compose ps`, confirm `localhost:9092` reachable |
| Frontend calls 404/502 | Backend service on that port isn't running | Check the four `mvn spring-boot:run` windows for errors |

## 6. Shutdown

```powershell
docker compose down          # stops Kafka + MinIO (add -v to also wipe volumes/data)
# Ctrl+C in each mvn spring-boot:run window and the npm run dev window
```
