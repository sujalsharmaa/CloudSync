package com.tags_generation_service.tags_generation_service.Service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.InputStream;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class S3Service {

    private final S3Client s3Client;
    // NOTE: In a production app, the bucket name should be loaded via @Value or properties.
    private final String bucketName = "your-rag-pipeline-bucket";
    private final Region region = Region.US_EAST_1; // Using a variable for consistency

    public S3Service() {
        this.s3Client = S3Client.builder()
                .region(region)
                .build();
    }

    public InputStream downloadFile(String s3Location) {

        // 1. Build the expected URL prefix for your bucket (e.g., "https://your-bucket.s3.us-east-1.amazonaws.com/")
        String regionId = s3Client.serviceClientConfiguration().region().id();
        String urlPrefix = String.format("https://%s.s3.%s.amazonaws.com/", bucketName, regionId);

        String key;

        if (s3Location.startsWith(urlPrefix)) {
            // 2. FIX: Extract the full S3 key, which is the path starting right after the URL prefix.
            // For URL "https://.../1/images (1).jpg", this extracts "1/images (1).jpg"
            key = s3Location.substring(urlPrefix.length());
        } else {
            // Fallback: Assume s3Location is already the raw key if the prefix doesn't match
            log.warn("S3 location does not start with expected URL prefix ({}). Assuming raw S3 key.", urlPrefix);
            key = s3Location;
        }

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key) // Use the correctly extracted key (e.g., "1/images (1).jpg")
                .build();

        log.info("Attempting to download file from S3 using key: {}", key);

        // This line will now use the correct key, which should resolve the 404 error
        return s3Client.getObject(getObjectRequest);
    }

    public String uploadFile(String key, InputStream inputStream) throws IOException {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        byte[] fileBytes = inputStream.readAllBytes();

        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(fileBytes));
        log.info("Successfully uploaded file {} to S3 bucket {}", key, bucketName);

        // Ensure the returned URL uses the configured region dynamically
        return String.format("https://%s.s3.%s.amazonaws.com/%s",
                bucketName, s3Client.serviceClientConfiguration().region().id(), key);
    }


    public InputStream downloadFileForUser(String key) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        log.info("Downloading file from S3: {}", key);
        return s3Client.getObject(getObjectRequest);
    }
    /**
     * Deletes multiple files from the S3 bucket.
     * @param keys The list of object keys (file names) to delete.
     */
    public void deleteFiles(List<String> keys) {
        try {
            List<ObjectIdentifier> objects = keys.stream()
                    .map(key -> ObjectIdentifier.builder().key(key).build())
                    .collect(Collectors.toList());

            Delete delete = Delete.builder()
                    .objects(objects)
                    .build();

            DeleteObjectsRequest deleteObjectsRequest = DeleteObjectsRequest.builder()
                    .bucket(bucketName)
                    .delete(delete)
                    .build();

            s3Client.deleteObjects(deleteObjectsRequest);
            log.info("Successfully deleted files from S3 bucket {}: {}", bucketName, keys);

        } catch (S3Exception e) {
            log.error("Failed to delete objects from S3: {}", e.awsErrorDetails().errorMessage());
            throw new RuntimeException("Error deleting files from S3", e);
        }
    }
}
