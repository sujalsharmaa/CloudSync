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
    private final String bucketName = "your-rag-pipeline-bucket"; // Replace with your S3 bucket name

    public S3Service() {
        this.s3Client = S3Client.builder()
                .region(Region.US_EAST_1) // Replace with your region
                .build();
    }

    public InputStream downloadFile(String s3Location) {
        String key = s3Location.substring(s3Location.lastIndexOf("/") + 1);
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
        log.info("Downloading file from S3: {}", s3Location);
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

        return "https://" + bucketName + ".s3." + s3Client.serviceClientConfiguration().region().id() + ".amazonaws.com/" + key;
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
