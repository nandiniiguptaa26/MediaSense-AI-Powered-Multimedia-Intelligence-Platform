package com.mediasense.converter.conversion.service;

import com.mediasense.converter.conversion.config.ConvertConfig;
import com.mediasense.converter.conversion.config.FFmpegConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Service
public class CaptionVideoService {

    private static final Logger logger =
            LoggerFactory.getLogger(CaptionVideoService.class);

    @Autowired
    private FFmpegConfig fFmpegConfig;

    @Autowired
    private ConvertConfig convertConfig;

    @Autowired
    private GcsHelper gcsHelper;

    @Value("${whisper.path}")
    private String whisperPath;

    public void generateCaptionVideo(String inputBucket,
                                     String outputBucket,
                                     String blobName) throws Exception {

        Path ffmpegExecutable =
                fFmpegConfig.getFFmpegPath().resolve(exeName("ffmpeg"));

        Path tempDir = convertConfig.getConvertTempPath();

        Path tempAudio = tempDir.resolve("caption_input.mp3");
        Path tempSrt = tempDir.resolve("caption_input.srt");
        Path tempVideo = tempDir.resolve("caption.mp4");

        logger.info(
                "Starting caption video generation for: {}",
                blobName
        );

        try {

            Files.createDirectories(tempDir);

            Files.deleteIfExists(tempAudio);
            Files.deleteIfExists(tempSrt);
            Files.deleteIfExists(tempVideo);

            // Download audio from storage
            try (InputStream audioStream =
                         gcsHelper.fetchFromGCS(inputBucket, blobName)) {

                Files.copy(
                        audioStream,
                        tempAudio,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }

            logger.info(
                    "Audio downloaded to temp: {}",
                    tempAudio
            );

            // Run Whisper
            runWhisper(tempAudio, tempDir);

            logger.info(
                    "Whisper transcription completed. SRT at {}",
                    tempSrt
            );

            if (!Files.exists(tempSrt)) {
                throw new RuntimeException(
                        "Whisper did not produce SRT file at: " +
                                tempSrt
                );
            }

            // Get audio duration
            double duration =
                    getAudioDuration(ffmpegExecutable, tempAudio);

            logger.info(
                    "Audio duration: {} seconds",
                    duration
            );

            // Escape Windows path for FFmpeg subtitles filter
            String srtPath = tempSrt.toAbsolutePath()
                    .toString()
                    .replace("\\", "/")
                    .replace(":", "\\:");

            // SIMPLE subtitle-only filter (stable and compatible)
            String filterComplex =
                    "[0:v]geq=" +
                            "r='128+127*sin(2*PI*T/8+0)':" +
                            "g='128+127*sin(2*PI*T/8+2.094)':" +
                            "b='128+127*sin(2*PI*T/8+4.189)'," +
                            "subtitles='" + srtPath + "'" +
                            ":original_size=1280x720:" +
                            "force_style='FontName=Arial," +
                            "FontSize=16," +
                            "PrimaryColour=&H00FFFFFF," +
                            "OutlineColour=&H00000000," +
                            "Outline=1," +
                            "Shadow=0," +
                            "MarginV=40," +
                            "Alignment=2'[v]";

            logger.info(
                    "FFmpeg filter_complex: {}",
                    filterComplex
            );

            ProcessBuilder pb = new ProcessBuilder(

                    ffmpegExecutable.toString(),

                    // Black background video
                    "-f", "lavfi",
                    "-i",
                    "color=c=black:s=1280x720:r=25:d=" +
                            (int) Math.ceil(duration),

                    // Audio input
                    "-i",
                    tempAudio.toString(),

                    // Subtitles
                    "-filter_complex",
                    filterComplex,

                    // Maps
                    "-map", "[v]",
                    "-map", "1:a",

                    // Video codec
                    "-c:v", "libx264",

                    // Maximum compatibility
                    "-profile:v", "main",
                    "-level", "3.1",
                    "-pix_fmt", "yuv420p",

                    // Encoding preset
                    "-preset", "medium",

                    // Audio codec
                    "-c:a", "aac",
                    "-b:a", "192k",

                    // Streaming/playback optimization
                    "-movflags", "+faststart",

                    // Stop at shortest stream
                    "-shortest",

                    // Overwrite
                    "-y",

                    // Output
                    tempVideo.toString()
            );

            pb.redirectErrorStream(true);

            Process process = pb.start();

            try (BufferedReader reader =
                         new BufferedReader(
                                 new InputStreamReader(
                                         process.getInputStream()
                                 )
                         )) {

                String line;

                while ((line = reader.readLine()) != null) {
                    logger.info("[FFmpeg] {}", line);
                }
            }

            int exitCode = process.waitFor();

            if (exitCode != 0) {
                throw new RuntimeException(
                        "FFmpeg failed with exit code: " +
                                exitCode
                );
            }

            logger.info(
                    "Caption video generated at: {}",
                    tempVideo
            );

            // Upload generated video
            String outputBlobName =
                    blobName + "_captioned.mp4";

            try (
                    InputStream resultStream =
                            Files.newInputStream(tempVideo);

                    OutputStream out =
                            gcsHelper.prepareOutputStream(
                                    outputBucket,
                                    outputBlobName,
                                    "video/mp4"
                            )
            ) {

                resultStream.transferTo(out);

                out.flush();
            }

            logger.info(
                    "Caption video uploaded successfully: {}",
                    outputBlobName
            );

        } finally {

            Files.deleteIfExists(tempAudio);
            Files.deleteIfExists(tempSrt);
            Files.deleteIfExists(tempVideo);
        }
    }

    private void runWhisper(Path audioFile,
                            Path outputDir) throws Exception {

        ProcessBuilder pb = new ProcessBuilder(

                whisperPath,

                audioFile.toString(),

                "--model", "base",

                "--output_format", "srt",

                "--output_dir",
                outputDir.toString()
        );

        // Add FFmpeg binaries to PATH
        String ffmpegBinPath =
                fFmpegConfig.getFFmpegPath().toString();

        pb.environment().put(
                "PATH",
                ffmpegBinPath + File.pathSeparator +
                        System.getenv("PATH")
        );

        pb.redirectErrorStream(true);

        Process process = pb.start();

        try (BufferedReader reader =
                     new BufferedReader(
                             new InputStreamReader(
                                     process.getInputStream()
                             )
                     )) {

            String line;

            while ((line = reader.readLine()) != null) {
                logger.info("[Whisper] {}", line);
            }
        }

        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new RuntimeException(
                    "Whisper failed with exit code: " +
                            exitCode
            );
        }
    }

    private double getAudioDuration(Path ffmpegExecutable,
                                    Path audioFile) throws Exception {

        Path ffprobeExecutable =
                ffmpegExecutable.getParent()
                        .resolve(exeName("ffprobe"));

        ProcessBuilder pb = new ProcessBuilder(

                ffprobeExecutable.toString(),

                "-v", "error",

                "-show_entries",
                "format=duration",

                "-of",
                "default=noprint_wrappers=1:nokey=1",

                audioFile.toString()
        );

        pb.redirectErrorStream(false);

        Process process = pb.start();

        String durationStr;

        try (BufferedReader reader =
                     new BufferedReader(
                             new InputStreamReader(
                                     process.getInputStream()
                             )
                     )) {

            durationStr = reader.readLine();
        }

        int exitCode = process.waitFor();

        if (exitCode != 0) {

            logger.warn(
                    "ffprobe failed. Using fallback duration."
            );

            return 60.0;
        }

        if (durationStr == null ||
                durationStr.isBlank()) {

            return 60.0;
        }

        return Double.parseDouble(durationStr.trim());
    }

    private static String exeName(String name) {
        return System.getProperty("os.name").toLowerCase().contains("win") ? name + ".exe" : name;
    }
}