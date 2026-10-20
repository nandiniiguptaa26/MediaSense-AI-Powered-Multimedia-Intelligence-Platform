package com.mediasense.qna.service;

import com.mediasense.qna.model.ContextChunk;
import com.mediasense.qna.model.IngestResponse;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

@Service
public class IngestService {

    private static final Logger logger = LoggerFactory.getLogger(IngestService.class);

    @Value("${whisper.path}")
    private String whisperPath;

    @Value("${ffmpeg.path}")
    private String ffmpegBinPath;

    @Value("${qna.temp.path}")
    private String tempPath;

    @Value("${yt-dlp.path}")
    private String ytDlpPath;

    private final ContextStore contextStore;

    public IngestService(ContextStore contextStore) {
        this.contextStore = contextStore;
    }

    public IngestResponse ingestFile(MultipartFile file) throws Exception {
        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "uploaded_file";
        String ext = getExtension(originalName).toLowerCase();

        logger.info("Ingestion File: {} (ext: {})", originalName, ext);

        String text;
        if(isAudioVideo(ext)) {
            text = transcribeAudioVideo(file, originalName);
        } else if (ext.equals("pdf")) {
            text = extractPdf(file);
        } else if (ext.equals("docx")) {
            text = extractDocx(file);
        } else {
            text = new String(file.getBytes(), StandardCharsets.UTF_8);
        }

        contextStore.add(new ContextChunk(originalName, text));
        logger.info(" Ingested '{}' - {} characters", originalName, text.length());
        return new IngestResponse(originalName, "success", text.length());
    }

    public IngestResponse ingestUrl(String url) throws Exception {
        logger.info("Ingesting URL: {}", url);
        String text;
        String source;

        if (isYouTubeUrl(url)) {
            text = transcribeYouTube(url);
            source = url;
        } else {
            text = scrapeWebPage(url);
            source = url;
        }

        contextStore.add(new ContextChunk(source, text));
        logger.info(" Ingested '{}' - {} characters", url, text.length());
        return new IngestResponse(source, "success", text.length());
    }

    private String transcribeAudioVideo(MultipartFile file, String originalName) throws Exception {
        Path tempDir = Paths.get(tempPath);
        Files.createDirectories(tempDir);

        Path tempInput = tempDir.resolve("qna_input_" + originalName);
        Path tempTxt = tempDir.resolve("qna_input_" + getBaseName(originalName) + ".txt");

        try {
            Files.copy(file.getInputStream(), tempInput, StandardCopyOption.REPLACE_EXISTING);
            Files.deleteIfExists(tempTxt);

            runWhisper(tempInput, tempDir);

            if(!Files.exists(tempTxt)) {
                throw new RuntimeException("Whisper did not produce .txt for: " + originalName);
            }
            return Files.readString(tempTxt, StandardCharsets.UTF_8);
        } finally {
            Files.deleteIfExists(tempInput);
            Files.deleteIfExists(tempTxt);
        }
    }

    private String transcribeYouTube(String url) throws Exception {
        Path tempDir = Paths.get(tempPath);
        Files.createDirectories(tempDir);

        Path tempAudio = tempDir.resolve("yt_audio.mp3");
        Path tempTxt = tempDir.resolve("yt_audio.txt");

        try {
            Files.deleteIfExists(tempAudio);
            Files.deleteIfExists(tempTxt);

            ProcessBuilder dlPb = new ProcessBuilder(
                    ytDlpPath,
                    "-x",
                    "--audio-format", "mp3",
                    "--audio-quality", "0",
                    "-o", tempAudio.toString(),
                    url
            );
            dlPb.environment().put("PATH", ffmpegBinPath + File.pathSeparator + System.getenv("PATH"));
            dlPb.redirectErrorStream(true);
            runProcess(dlPb, "[yt-dlp]");

            if (!Files.exists(tempAudio)) {
                throw new RuntimeException("yt-dlp did not produce audio for: " + url);
            }

            runWhisper(tempAudio, tempDir);
            if(!Files.exists(tempTxt)) {
                throw new RuntimeException("Whisper did not produce .txt for YouTube audio");
            }
            return Files.readString(tempTxt, StandardCharsets.UTF_8);
        } finally {
            Files.deleteIfExists(tempAudio);
            Files.deleteIfExists(tempTxt);
        }
    }
    private static final int MAX_WEBPAGE_CHARS=8_000;
    private String scrapeWebPage(String url) throws Exception {
        org.jsoup.nodes.Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0")
                .timeout(15_000)
                .get();

        doc.select("nav, header, footer, script, style, iframe, aside, .reflist, .references, .mw-references-wrap").remove();
        String text = doc.body().text();
        if (text.isBlank()) {
            throw new RuntimeException(
                    "No text content found at URL: " + url
            );
        }
        return text.length() > MAX_WEBPAGE_CHARS ? text.substring(0,MAX_WEBPAGE_CHARS) : text;
    }

    private String extractPdf(MultipartFile file) throws Exception {
        try (PDDocument doc = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(doc);
        }
    }

    private String extractDocx(MultipartFile file) throws Exception {
        try (XWPFDocument doc = new XWPFDocument(file.getInputStream())) {
            StringBuilder sb = new StringBuilder();
            for(XWPFParagraph para : doc.getParagraphs()) {
                sb.append(para.getText()).append("\n");
            }
            return sb.toString();
        }
    }

    private void runWhisper(Path audioFile, Path outputDir) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(
                whisperPath,
                audioFile.toString(),
                "--model", "tiny",
                "--output_format", "txt",
                "--output_dir", outputDir.toString()
        );

        pb.environment().put("PATH", ffmpegBinPath + File.pathSeparator + System.getenv("PATH"));

        pb.redirectErrorStream(true);
        runProcess(pb, "[Whisper]");
    }

    private void runProcess(ProcessBuilder pb, String logPrefix) throws Exception {
        Process process = pb.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while((line = reader.readLine()) != null) {
                logger.info("{} {}", logPrefix, line);
            }
        }

        int exitCode = process.waitFor();
        if(exitCode != 0) {
            throw new RuntimeException(logPrefix + " process failed with exit code: " + exitCode);
        }
    }

    private boolean isAudioVideo(String ext) {
        return List.of("mp4", "mp3", "wav", "webm", "avi", "wmv", "mov", "flv", "m4a", "weba", "ogg", "opus", "flac").contains(ext);
    }

    private boolean isYouTubeUrl(String url) {
        return url.contains("youtube.com/watch") || url.contains("youtu.be/");
    }

    private String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot + 1) : "";
    }

    private String getBaseName(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(0, dot) : filename;
    }
}
