package com.mediamind.converter.conversion.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class FFmpegConfig {
    @Value("${ffmpeg.path}")
    private String ffmpegPath;

    public Path getFFmpegPath() {
        return Paths.get(ffmpegPath);
    }
}
