package com.mediamind.converter.upload.exception;

public class UploadFailureException extends RuntimeException {
    private final String fileName;
    private final String reason;

    public UploadFailureException(String fileName, String reason) {
        super(reason);
        this.fileName = fileName;
        this.reason = reason;
    }

    public String getFileName() {
        return fileName;
    }

    public String getReason() {
        return reason;
    }
}
