package com.mediasense.converter.conversion.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for mapping output formats to MIME types.
 */
public class MimeTypeUtil {
    private static final Map<String, String> FORMAT_TO_MIME = new HashMap<>();

    static {
        // Audio formats
        FORMAT_TO_MIME.put("3gp", "audio/3gpp");
        FORMAT_TO_MIME.put("3g2", "audio/3gpp2");
        FORMAT_TO_MIME.put("ac3", "audio/ac3");
        FORMAT_TO_MIME.put("ac4", "audio/ac4");
        FORMAT_TO_MIME.put("adts", "audio/aac"); // AAC in ADTS
        FORMAT_TO_MIME.put("adx", "audio/adx");
        FORMAT_TO_MIME.put("aea", "audio/x-aea");
        FORMAT_TO_MIME.put("aiff", "audio/aiff");
        FORMAT_TO_MIME.put("alaw", "audio/G.711"); // A-law PCM
        FORMAT_TO_MIME.put("amr", "audio/amr");
        FORMAT_TO_MIME.put("aptx", "audio/aptx");
        FORMAT_TO_MIME.put("aptx_hd", "audio/aptx-hd");
        FORMAT_TO_MIME.put("au", "audio/basic");
        FORMAT_TO_MIME.put("caf", "audio/x-caf");
        FORMAT_TO_MIME.put("daud", "audio/x-daud");
        FORMAT_TO_MIME.put("dfpwm", "audio/x-dfpwm");
        FORMAT_TO_MIME.put("dts", "audio/vnd.dts");
        FORMAT_TO_MIME.put("eac3", "audio/eac3");
        FORMAT_TO_MIME.put("flac", "audio/flac");
        FORMAT_TO_MIME.put("g722", "audio/G722");
        FORMAT_TO_MIME.put("g723_1", "audio/g723");
        FORMAT_TO_MIME.put("g726", "audio/G726");
        FORMAT_TO_MIME.put("g726le", "audio/G726");
        FORMAT_TO_MIME.put("gsm", "audio/gsm");
        FORMAT_TO_MIME.put("ircam", "audio/x-ircam");
        FORMAT_TO_MIME.put("lc3", "audio/lc3");
        FORMAT_TO_MIME.put("mp2", "audio/mpeg"); // MPEG Layer 2
        FORMAT_TO_MIME.put("mp3", "audio/mpeg");
        FORMAT_TO_MIME.put("mulaw", "audio/basic"); // μ-law PCM
        FORMAT_TO_MIME.put("oga", "audio/ogg");
        FORMAT_TO_MIME.put("ogg", "audio/ogg");
        FORMAT_TO_MIME.put("opus", "audio/opus");
        FORMAT_TO_MIME.put("oma", "audio/x-oma"); // ATRAC
        FORMAT_TO_MIME.put("sbc", "audio/sbc");
        FORMAT_TO_MIME.put("sox", "audio/x-sox");
        FORMAT_TO_MIME.put("spdif", "audio/spdif");
        FORMAT_TO_MIME.put("spx", "audio/x-speex");
        FORMAT_TO_MIME.put("tta", "audio/x-tta");
        FORMAT_TO_MIME.put("voc", "audio/x-voc");
        FORMAT_TO_MIME.put("wav", "audio/wav");
        FORMAT_TO_MIME.put("w64", "audio/w64");
        FORMAT_TO_MIME.put("wv", "audio/x-wavpack");
        FORMAT_TO_MIME.put("webm", "audio/webm");
        FORMAT_TO_MIME.put("webm_chunk", "audio/webm");
        FORMAT_TO_MIME.put("mp4", "audio/mp4");
        FORMAT_TO_MIME.put("m4v", "audio/mp4");
        FORMAT_TO_MIME.put("ismv", "audio/mp4");
        FORMAT_TO_MIME.put("psp", "audio/mp4");
        FORMAT_TO_MIME.put("mov", "audio/quicktime");
        FORMAT_TO_MIME.put("flv", "audio/x-flv");
        FORMAT_TO_MIME.put("f4v", "audio/x-f4v");
        FORMAT_TO_MIME.put("asf", "audio/x-ms-wma");
        FORMAT_TO_MIME.put("wtv", "audio/x-ms-wma");
        FORMAT_TO_MIME.put("mxf", "application/mxf");
        FORMAT_TO_MIME.put("mxf_d10", "application/mxf");
        FORMAT_TO_MIME.put("mxf_opatom", "application/mxf");
        FORMAT_TO_MIME.put("vob", "video/dvd"); // contains audio+video
        FORMAT_TO_MIME.put("dvd", "video/dvd");
        FORMAT_TO_MIME.put("svcd", "video/mpeg");
        FORMAT_TO_MIME.put("vcd", "video/mpeg");
        FORMAT_TO_MIME.put("mpeg", "audio/mpeg");

        // Video formats
        FORMAT_TO_MIME.put("3gp", "video/3gpp");
        FORMAT_TO_MIME.put("3g2", "video/3gpp2");
        FORMAT_TO_MIME.put("amv", "video/x-amv");
        FORMAT_TO_MIME.put("apng", "image/apng");
        FORMAT_TO_MIME.put("asf", "video/x-ms-asf");
        FORMAT_TO_MIME.put("wtv", "video/x-ms-wtv");
        FORMAT_TO_MIME.put("wmv", "video/x-ms-wmv");
        FORMAT_TO_MIME.put("avi", "video/x-msvideo");
        FORMAT_TO_MIME.put("avif", "image/avif");
        FORMAT_TO_MIME.put("dv", "video/x-dv");
        FORMAT_TO_MIME.put("f4v", "video/x-f4v");
        FORMAT_TO_MIME.put("flv", "video/x-flv");
        FORMAT_TO_MIME.put("gif", "image/gif");
        FORMAT_TO_MIME.put("h261", "video/h261");
        FORMAT_TO_MIME.put("h263", "video/h263");
        FORMAT_TO_MIME.put("h264", "video/h264");
        FORMAT_TO_MIME.put("mp4", "video/mp4");
        FORMAT_TO_MIME.put("ismv", "video/mp4");
        FORMAT_TO_MIME.put("psp", "video/mp4");
        FORMAT_TO_MIME.put("m4v", "video/x-m4v");
        FORMAT_TO_MIME.put("hevc", "video/h265");
        FORMAT_TO_MIME.put("ivf", "video/x-ivf");
        FORMAT_TO_MIME.put("webm", "video/webm");
        FORMAT_TO_MIME.put("mjpeg", "video/x-mjpeg");
        FORMAT_TO_MIME.put("mpjpeg", "video/x-mjpeg");
        FORMAT_TO_MIME.put("mkv", "video/x-matroska");
        FORMAT_TO_MIME.put("matroska", "video/x-matroska");
        FORMAT_TO_MIME.put("mov", "video/quicktime");
        FORMAT_TO_MIME.put("qt", "video/quicktime");
        FORMAT_TO_MIME.put("mpeg", "video/mpeg");
        FORMAT_TO_MIME.put("vcd", "video/mpeg");
        FORMAT_TO_MIME.put("svcd", "video/mpeg");
        FORMAT_TO_MIME.put("vob", "video/dvd");
        FORMAT_TO_MIME.put("dvd", "video/dvd");
        FORMAT_TO_MIME.put("nut", "video/x-nut");
        FORMAT_TO_MIME.put("ogv", "video/ogg");
        FORMAT_TO_MIME.put("ogg", "video/ogg");
        FORMAT_TO_MIME.put("rawvideo", "video/raw");
        FORMAT_TO_MIME.put("yuv4mpegpipe", "video/x-yuv4mpeg");
        FORMAT_TO_MIME.put("rm", "application/vnd.rn-realmedia");
        FORMAT_TO_MIME.put("roq", "video/x-roq");
        FORMAT_TO_MIME.put("swf", "application/x-shockwave-flash");
        FORMAT_TO_MIME.put("avm2", "application/x-shockwave-flash");
        FORMAT_TO_MIME.put("vc1", "video/vc1");
        FORMAT_TO_MIME.put("vc1test", "video/vc1");
        FORMAT_TO_MIME.put("webp", "image/webp");
        FORMAT_TO_MIME.put("vvc", "video/vvc"); // H.266/VVC
    }

    /**
     * Resolve MIME type from format string.
     *
     * @param mimeType The MIME type that is initially given, if any. If MIME type is empty/null, MIME type will be given by format
     * @param format The output format (e.g. "mp4", "mkv")
     * @return MIME type string, or "application/octet-stream" if unknown
     */
    public static String resolveMimeType(String format, String mimeType) {
        if (format == null) {
            return "application/octet-stream";
        }
        if (mimeType == null || mimeType.isEmpty()) {
            return FORMAT_TO_MIME.getOrDefault(format.toLowerCase(), "application/octet-stream");
        }
        return mimeType;
    }
}
