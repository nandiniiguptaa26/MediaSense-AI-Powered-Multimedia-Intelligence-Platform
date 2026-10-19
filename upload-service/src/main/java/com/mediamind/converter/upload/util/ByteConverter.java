package com.mediamind.converter.upload.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;

public class ByteConverter {
    private static final String[] UNITS = {"B", "KB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB"};
    private static final Logger logger = LoggerFactory.getLogger(ByteConverter.class);
    private static final BigInteger BASE = BigInteger.valueOf(1024);

    public static String formatBytes(long bytes) {
        return formatBytes(BigInteger.valueOf(bytes));
    }

    public static String formatBytes(BigInteger bytes) {
        if (bytes.compareTo(BASE) < 0) return bytes + " B";

        int unitIndex = 0;

        while (bytes.compareTo(BASE) >= 0 && unitIndex < UNITS.length - 1) {
            bytes = bytes.divide(BASE);
            unitIndex++;
        }

        if (unitIndex >= UNITS.length) {
            return String.format("%s %s", bytes, UNITS[UNITS.length - 1]);
        }
        return String.format("%s %s", bytes, UNITS[unitIndex]);
    }
}
