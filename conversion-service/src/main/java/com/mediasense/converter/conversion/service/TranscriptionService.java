package com.mediasense.converter.conversion.service;

import com.mediasense.converter.conversion.config.ConvertConfig;
import com.mediasense.converter.conversion.config.FFmpegConfig;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.slf4j.Logger;

@Service
public class TranscriptionService {

    private static final Logger logger = LoggerFactory.getLogger(TranscriptionService.class);

    @Autowired
    private FFmpegConfig fFmpegConfig;

    @Autowired
    private ConvertConfig convertConfig;

    @Autowired
    private GcsHelper gcsHelper;

    @Value("${whisper.path}")
    private String whisperPath;

    public byte[] transcribe(String inputBucket, String outputBucket, String blobName) throws Exception {
        Path ffmpegExecutable = fFmpegConfig.getFFmpegPath().resolve(exeName("ffmpeg"));
        Path tempDir = convertConfig.getConvertTempPath();

        Path tempAudio = tempDir.resolve("transcribe_input.mp3");
        Path tempTxt = tempDir.resolve("transcribe_input.txt");

        logger.info("Starting transcription for: {}", blobName);

        try {
            Files.createDirectories(tempDir);
            Files.deleteIfExists(tempAudio);
            Files.deleteIfExists(tempTxt);

            try (InputStream inputStream = gcsHelper.fetchFromGCS(inputBucket, blobName)) {
                Files.copy(inputStream, tempAudio, StandardCopyOption.REPLACE_EXISTING);
            }
            logger.info("Input file downloaded to temp: {}", tempAudio);

            runWhisper(tempAudio, tempDir);
            logger.info("whisper transcription completed. TXT at: {}", tempTxt);

            if (!Files.exists(tempTxt)) {
                throw new RuntimeException("Whisper did not produce a .text file at: " + tempTxt);
            }

            byte[] txtContent = Files.readAllBytes(tempTxt);
            String outputBlobName = blobName + "_transcription.txt";
            try (InputStream resultStream = Files.newInputStream(tempTxt);
                OutputStream out = gcsHelper.prepareOutputStream(outputBucket, outputBlobName, "text/plain")) {
                resultStream.transferTo(out);
                out.flush();
                }
            logger.info("Transcription uploaded to MinIO: {}", outputBlobName);
            return txtContent;
            } finally {
            Files.deleteIfExists(tempAudio);
            Files.deleteIfExists(tempTxt);
        }
    }

    private void runWhisper(Path audioFile, Path outputDir) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(
                whisperPath,
                audioFile.toString(),
                "--model", "base",
                "--output_format", "txt",
                "--output_dir", outputDir.toString()
        );

        String ffmpegBinPath = fFmpegConfig.getFFmpegPath().toString();
        pb.environment().put("PATH", ffmpegBinPath + File.pathSeparator + System.getenv("PATH"));

        pb.redirectErrorStream(true);
        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                logger.info("[Whisper] {}", line);
            }
        }

        int exitCode = process.waitFor();
        if(exitCode != 0) {
            throw new RuntimeException("Whisper failed with exit code: " + exitCode);
        }
    }

    private static String exeName(String name) {
        return System.getProperty("os.name").toLowerCase().contains("win") ? name + ".exe" : name;
    }
}
