package com.mediamind.converter.upload.util;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ByteConverterTest {

    @Test
    void testBytesLessThanOneKB() {
        assertEquals("512 B", ByteConverter.formatBytes(512));
    }

    @Test
    void testExactlyOneKB() {
        assertEquals("1 KB", ByteConverter.formatBytes(1024));
    }

    @Test
    void testOneMB() {
        assertEquals("1 MB", ByteConverter.formatBytes(1024 * 1024));
    }

    @Test
    void testFivePB() {
        long fivePB = 5L * 1024 * 1024 * 1024 * 1024 * 1024;
        assertEquals("5 PB", ByteConverter.formatBytes(fivePB));
    }

    @Test
    void testOneYottabyte() {
        BigInteger oneYB = BigInteger.valueOf(1024).pow(8);
        assertEquals("1 YB", ByteConverter.formatBytes(oneYB));
    }

    @Test
    void testBeyondYottabyte() {
        // 1 Brontobyte (unofficial)
        BigInteger beyondYB = BigInteger.valueOf(1024).pow(9);
        String result = ByteConverter.formatBytes(beyondYB);
        assertTrue(result.endsWith("YB"));
    }
}
