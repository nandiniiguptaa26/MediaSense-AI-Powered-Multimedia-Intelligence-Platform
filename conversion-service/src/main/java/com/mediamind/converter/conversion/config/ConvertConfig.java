package com.mediamind.converter.conversion.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class ConvertConfig {
    @Value("${convert.temp.path}")
    private String convertTempPath;

    public Path getConvertTempPath() {
        return Paths.get(convertTempPath);
    }
}
