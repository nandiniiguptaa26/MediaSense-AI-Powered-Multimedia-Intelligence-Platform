package com.mediasense.converter.dto;

public class JobPayload {
    private String blobName;
    private String mimeType;
    private String outputFormat;

    // Default constructor (needed by Jackson)
    public JobPayload() {}

    public JobPayload(String blobName, String mimeType, String outputFormat) {
        this.blobName = blobName;
        this.mimeType = mimeType;
        this.outputFormat = outputFormat;
    }

    public String getBlobName() {
        return blobName;
    }

    public void setBlobName(String blobName) {
        this.blobName = blobName;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getOutputFormat() {
        return outputFormat;
    }

    public void setOutputFormat(String outputFormat) {
        this.outputFormat = outputFormat;
    }

    @Override
    public String toString() {
        return "JobPayload:{" +
                "blobName='" + blobName + '\'' +
                ", mimeType='" + mimeType + '\'' +
                ", outputFormat='" + outputFormat + '\'' +
                '}';
    }
}
