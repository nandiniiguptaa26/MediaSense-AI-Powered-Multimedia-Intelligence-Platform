package com.mediasense.converter.upload.exception;

public class FileValidationException extends RuntimeException {
    private final String fileName;
    private final String reason;

    public FileValidationException(String fileName, String reason) {
        super(reason);
        this.fileName = fileName;
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    public String getFileName() {
        return fileName;
    }
}
