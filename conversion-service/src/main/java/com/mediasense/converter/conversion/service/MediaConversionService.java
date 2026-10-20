package com.mediasense.converter.conversion.service;

import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffmpeg.Output;
import com.github.kokorin.jaffree.ffmpeg.PipeInput;
import com.github.kokorin.jaffree.ffprobe.FFprobe;
import com.github.kokorin.jaffree.ffprobe.FFprobeResult;
import com.github.kokorin.jaffree.ffprobe.Stream;
import com.mediasense.converter.conversion.config.ConvertConfig;
import com.mediasense.converter.conversion.config.FFmpegConfig;
import com.mediasense.converter.conversion.util.SmartOutputStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

@Service
public class MediaConversionService {
    @Autowired
    private FFmpegConfig ffmpegConfig;
    @Autowired
    private ConvertConfig convertConfig;
    @Autowired
    private GcsHelper gcsHelper;
    private static final Logger logger = LoggerFactory.getLogger(MediaConversionService.class);

    public void convertMedia(long estimatedSize, String inputBucket, String outputBucket,
                             String blobName, String mimeType, String outputFormat,
                             Map<String, String> codecs, Map<String, String> arguments) throws IOException {
        Path ffmpegExecutable = ffmpegConfig.getFFmpegPath().resolve(exeName("ffmpeg"));
        Path ffprobeExecutable = ffmpegConfig.getFFmpegPath().resolve(exeName("ffprobe"));
        double durationSeconds = 0;
        long bitrateKbps = 0;
        InputStream probeStream = gcsHelper.fetchFromGCS(inputBucket, blobName);
        InputStream ffmpegStream = gcsHelper.fetchFromGCS(inputBucket, blobName);
        if (ffmpegStream == null) {
            throw new IllegalArgumentException("InputStream ffmpegStream not found");
        }

        if (Objects.equals(outputFormat, "wmv")) {
            outputFormat = "asf";
        }

        Path tempFilePath = convertConfig.getConvertTempPath().resolve("converted." + outputFormat);

        FFprobeResult result = FFprobe.atPath(ffprobeExecutable.getParent())
                .setShowStreams(true)
                .setInput(probeStream)
                .execute();

        for (Stream stream : result.getStreams()) {
            if (stream.getDuration() != null && stream.getDuration() > durationSeconds) {
                durationSeconds = stream.getDuration(); // longest stream duration
            }

            if (stream.getBitRate() != null && stream.getBitRate() > bitrateKbps) {
                bitrateKbps = stream.getBitRate() / 1000; // convert to kbps
            }
        }

        long estimatedSizeBytes = getEstimatedSize(estimatedSize, bitrateKbps, durationSeconds);

        //Prepare GCS output
        String convertedBlobName = blobName + "_converted." + outputFormat;
        OutputStream gcsOutputStream = gcsHelper.prepareOutputStream(outputBucket,
                convertedBlobName, mimeType);

        Output output = SmartOutputStrategy.chooseOutput(outputFormat, gcsOutputStream, tempFilePath, estimatedSizeBytes);

        FFmpeg ffmpeg = FFmpeg.atPath(ffmpegExecutable.getParent())
                .addInput(PipeInput.pumpFrom(ffmpegStream))
                .addOutput(output);

        logger.info("Duration: {} seconds", durationSeconds);
        logger.info("Bitrate: {} kbps", bitrateKbps);

        // Video and audio codec names.
        String videoCodec = resolveVideoCodec(outputFormat, codecs);
        String audioCodec = resolveAudioCodec(outputFormat, codecs);

        if (videoCodec != null) {
            ffmpeg.addArguments("-c:v", videoCodec);
        }
        if (audioCodec != null) {
            ffmpeg.addArguments("-c:a", audioCodec);
        }

        // Add extra arguments.
        if (arguments != null && !arguments.isEmpty()) {
            arguments.forEach(ffmpeg::addArguments);
        }

        ffmpeg.execute();

        gcsOutputStream.flush();
        gcsOutputStream.close();
        // If SmartOutputStrategy used ChannelOutput, stream the temp file
        if (SmartOutputStrategy.FORMATS_REQUIRING_SEEK.contains(outputFormat.toLowerCase())) {
            long size = Files.size(tempFilePath);
            logger.info("Temp file size after FFmpeg: {} bytes", size);

            try (InputStream resultStream = Files.newInputStream(tempFilePath)) {
                OutputStream gcsOutputStream2 = gcsHelper.prepareOutputStream(outputBucket, convertedBlobName, mimeType);
                resultStream.transferTo(gcsOutputStream2);
                gcsOutputStream2.flush();
                gcsOutputStream2.close();
            }
            Files.deleteIfExists(tempFilePath); // Clean the temp file
        } else {
            // We used PipeOutput → FFmpeg already wrote to gcsOutputStream
            logger.info("Conversion via PipeOutput completed, no temp file used");
        }
    }

    private String resolveVideoCodec(String format, Map<String, String> codecs) {
        if (codecs != null && codecs.containsKey("video")) {
            return codecs.get("video");
        }

        return switch (format.toLowerCase()) {
            case "3gp", "3g2"       -> "h263";         // Mobile formats
            case "amv"              -> "amv";          // AMV-specific codec
            case "apng"             -> "apng";         // Animated PNG
            case "asf", "wtv", "wmv" -> "msmpeg4";      // Windows formats
            case "avi"              -> "mpeg4";        // Legacy but safe
            case "avif"             -> "libaom-av1";   // AV1 image format
            case "dv"               -> "dvvideo";      // Digital Video
            case "f4v", "flv"       -> "flv";          // Flash Video
            case "gif"              -> "gif";          // Animated GIF
            case "h261"             -> "h261";         // Legacy
            case "h263"             -> "h263";         // Mobile
            case "h264", "mp4", "ismv", "psp", "m4v" -> "libx264"; // H.264 family
            case "hevc"             -> "libx265";      // H.265
            case "ivf", "webm"      -> "libvpx";       // VP8 for WebM
            case "mjpeg", "mpjpeg"  -> "mjpeg";        // MJPEG
            case "mkv", "matroska"  -> "libx264";      // Matroska
            case "mov", "qt"        -> "libx264";      // QuickTime
            case "mpeg", "vcd", "svcd", "vob", "dvd" -> "mpeg2video"; // MPEG family
            case "nut"              -> "libx264";      // Experimental
            case "ogv", "ogg"       -> "libtheora";    // Ogg Video
            case "rawvideo", "yuv4mpegpipe" -> "rawvideo"; // Uncompressed
            case "rm"               -> "rv10";         // RealMedia
            case "roq"              -> "roqvideo";     // RoQ format
            case "swf", "avm2"      -> "flv";          // Flash
            case "vc1", "vc1test"   -> "vc1";          // VC-1
            case "webp"             -> "libwebp";      // Animated WebP
            case "vvc"              -> "libvvc";       // H.266/VVC (if compiled)
            default                 -> null;
        };
    }

    private String resolveAudioCodec(String format, Map<String, String> codecs) {
        if (codecs != null && codecs.containsKey("audio")) {
            return codecs.get("audio");
        }

        return switch (format.toLowerCase()) {
            case "3gp", "3g2"       -> "aac";           // Mobile formats
            case "ac3"              -> "ac3";           // Dolby AC-3
            case "ac4"              -> "ac4";           // Dolby AC-4
            case "adts"             -> "aac";           // AAC in ADTS
            case "adx"              -> "adx";           // CRI ADX
            case "aea"              -> "pcm_s16le";     // Studio audio
            case "aiff"             -> "pcm_s16be";     // AIFF
            case "alaw"             -> "pcm_alaw";      // A-law
            case "amr"              -> "libopencore_amrnb"; // Narrowband AMR
            case "aptx"             -> "aptx";          // Bluetooth audio
            case "aptx_hd"          -> "aptx_hd";       // Bluetooth HD
            case "au"               -> "pcm_mulaw";     // Sun AU
            case "caf"              -> "aac";           // Apple CAF
            case "daud"             -> "pcm_s24be";     // D-Cinema audio
            case "dfpwm"            -> "dfpwm";         // Compressed waveform
            case "dts"              -> "dts";           // DTS
            case "eac3"             -> "eac3";          // Enhanced AC-3
            case "flac"             -> "flac";          // FLAC
            case "g722"             -> "g722";          // VoIP
            case "g723_1"           -> "g723_1";        // VoIP
            case "g726", "g726le"   -> "g726";          // VoIP
            case "gsm"              -> "gsm";           // GSM
            case "ircam"            -> "pcm_s16be";     // IRCAM
            case "lc3"              -> "lc3";           // Low Complexity Comm
            case "mp2"              -> "mp2";           // MPEG Layer 2
            case "mp3"              -> "libmp3lame";    // MPEG Layer 3
            case "mulaw"            -> "pcm_mulaw";     // μ-law
            case "oga", "ogg", "opus" -> "libopus";     // Ogg Opus
            case "oma"              -> "atrac3";        // Sony ATRAC
            case "sbc"              -> "sbc";           // Bluetooth
            case "sox"              -> "pcm_s16le";     // SoX
            case "spdif"            -> "ac3";           // IEC 61937
            case "spx"              -> "libspeex";      // Speex
            case "tta"              -> "tta";           // True Audio
            case "voc"              -> "pcm_u8";        // Creative Voice
            case "wav", "w64"       -> "pcm_s16le";     // WAV family
            case "wv"               -> "wavpack";       // WavPack
            case "webm", "webm_chunk" -> "libopus";     // WebM audio
            case "mp4", "m4v", "ismv", "psp" -> "aac";  // MP4 family
            case "mov"              -> "aac";           // QuickTime
            case "flv", "f4v"       -> "aac";           // Flash
            case "asf", "wtv"       -> "wmav2";         // Windows formats
            case "mxf", "mxf_d10", "mxf_opatom" -> "pcm_s16le"; // MXF
            case "vob", "dvd", "svcd", "vcd", "mpeg" -> "mp2"; // MPEG family
            default                 -> null;
        };
    }

    private long getEstimatedSize(long tempEstimatedSize, long bitrateKbps, double durationSeconds) {
        // fiveGB's(5GB) real value in "long" variable type, is 5_368_709_120L
        long fiveGB = 5L * 1024 * 1024 * 1024;
        if (tempEstimatedSize > fiveGB) {
            tempEstimatedSize = (bitrateKbps * 1000 / 8) * (long) durationSeconds;
        }
        return tempEstimatedSize;
    }

    private static String exeName(String name) {
        return System.getProperty("os.name").toLowerCase().contains("win") ? name + ".exe" : name;
    }
}
