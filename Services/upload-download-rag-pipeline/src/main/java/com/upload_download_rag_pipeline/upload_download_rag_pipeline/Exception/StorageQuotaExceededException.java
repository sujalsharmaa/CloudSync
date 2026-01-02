package com.upload_download_rag_pipeline.upload_download_rag_pipeline.Exception;

/**
 * Storage quota exceeded exception
 */
public class StorageQuotaExceededException extends RuntimeException {
    public StorageQuotaExceededException(String message) {
        super(message);
    }
}
