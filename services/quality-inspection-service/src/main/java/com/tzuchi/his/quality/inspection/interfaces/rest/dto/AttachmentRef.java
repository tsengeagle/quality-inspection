package com.tzuchi.his.quality.inspection.interfaces.rest.dto;

public class AttachmentRef {

    private String fileId;
    private String fileName;
    private String fileType;

    public AttachmentRef() {}

    public String getFileId() { return fileId; }
    public void setFileId(String fileId) { this.fileId = fileId; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }
}
