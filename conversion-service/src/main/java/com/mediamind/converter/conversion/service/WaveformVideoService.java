package com.mediamind.converter.conversion.service;
import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffmpeg.PipeInput;
import com.github.kokorin.jaffree.ffmpeg.UrlInput;
import com.github.kokorin.jaffree.ffmpeg.UrlOutput;
import com.mediamind.converter.conversion.config.ConvertConfig;
import com.mediamind.converter.conversion.config.FFmpegConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class WaveformVideoService {
    private static final Logger logger = LoggerFactory.getLogger(WaveformVideoService.class);


    @Autowired
    private FFmpegConfig fFmpegConfig;

    @Autowired
    private ConvertConfig convertConfig;

    @Autowired
    private GcsHelper gcsHelper;

    public void generateWaveformVideo(String inputBucket, String outputBucket,
                                      String blobName, MultipartFile backgroundImage) throws Exception {
        Path ffmpegExecutable = fFmpegConfig.getFFmpegPath().resolve(exeName("ffmpeg"));
        Path tempOutput = convertConfig.getConvertTempPath().resolve(blobName + "_waveform.mp4");
        Path tempImage = null;

        logger.info("Generating waveform video for: {}", blobName);

        try (InputStream audioStream = gcsHelper.fetchFromGCS(inputBucket, blobName)) {
            FFmpeg ffmpeg = FFmpeg.atPath(ffmpegExecutable.getParent());

            if (backgroundImage != null && !backgroundImage.isEmpty()) {
                String originalFileName = backgroundImage.getOriginalFilename();
                String ext = (originalFileName != null && originalFileName.contains("."))
                        ? originalFileName.substring(originalFileName.lastIndexOf('.'))
                        : ".jpg";

                tempImage = convertConfig.getConvertTempPath().resolve("bg_" + blobName + ext);
                Files.write(tempImage, backgroundImage.getBytes());

                ffmpeg.addArguments("-loop", "1")
                        .addInput(UrlInput.fromPath(tempImage))
                        .addInput((PipeInput.pumpFrom(audioStream)))
                        .setComplexFilter(
                                "[0:v]scale=1280:720,setsar=1[bg];" +
                                "[1:a]showwaves=s=1280x720:mode=line:rate=25:colors=00FF00|00FF00:scale=sqrt[waves];" +
                                "[bg][waves]overlay=0:H/2-h/2[v]"
                        )
                        .addArguments("-map", "[v]")
                        .addArguments("-map", "1:a")
                        .addArguments("-c:v", "libx264")
                        .addArguments("-c:a", "aac")
                        .addArgument("-shortest")
                        .addOutput(UrlOutput.toPath(tempOutput))
                        .execute();
            } else {
                ffmpeg.addInput(PipeInput.pumpFrom(audioStream))
                        .setComplexFilter("[0:a]showwaves=s=1280x720:mode=line:rate=25:colors=00FF00[v]")
                        .addArguments("-map", "[v]")
                        .addArguments("-map", "0:a")
                        .addArguments("-c:v", "libx264")
                        .addArguments("-c:a", "aac")
                        .addArgument("-shortest")
                        .addOutput(UrlOutput.toPath(tempOutput))
                        .execute();
            }

            logger.info("Waveform video generated at temp path: {}", tempOutput);

            String outputBlobName = blobName + "_waveform.mp4";

            try (InputStream resultStream = Files.newInputStream(tempOutput);
            OutputStream out = gcsHelper.prepareOutputStream(outputBucket, outputBlobName, "video/mp4")) {
                resultStream.transferTo(out);
                out.flush();
            }
            logger.info("Waveform video uploaded to MINIO: {}", outputBlobName);
        } finally {
            Files.deleteIfExists(tempOutput);
            if(tempImage != null) {
                Files.deleteIfExists(tempImage);
            }
        }
    }

    private static String exeName(String name) {
        return System.getProperty("os.name").toLowerCase().contains("win") ? name + ".exe" : name;
    }
}
