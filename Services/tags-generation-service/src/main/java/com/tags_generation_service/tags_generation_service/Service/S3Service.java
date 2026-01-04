package com.tags_generation_service.tags_generation_service.Service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import org.springframework.beans.factory.annotation.Value;

import java.io.InputStream;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class S3Service {
    private final S3Client s3Client;
    private final String bucketName;

    public S3Service(
            @Value("${aws.region}") String region,
            @Value("${aws.credentials.access-key}") String accessKey,
            @Value("${aws.credentials.secret-key}") String secretKey,
            @Value("${aws.s3.bucket-name}") String bucketName
    ) {
        this.bucketName = bucketName;

        AwsBasicCredentials awsCreds = AwsBasicCredentials.create(accessKey, secretKey);

        this.s3Client = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                .build();

        log.info("S3Service initialized with bucket: {} in region: {}", bucketName, region);
    }

    public InputStream downloadFile(String s3Location) {


        String regionId = s3Client.serviceClientConfiguration().region().id();
        String urlPrefix = String.format("https://%s.s3.%s.amazonaws.com/", bucketName, regionId);

        String key;

        if (s3Location.startsWith(urlPrefix)) {
            key = s3Location.substring(urlPrefix.length());
        } else {
            log.warn("S3 location does not start with expected URL prefix ({}). Assuming raw S3 key.", urlPrefix);
            key = s3Location;
        }

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        log.info("Attempting to download file from S3 using key: {}", key);

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
            System.out.println(deleteObjectsRequest);
            log.info("Successfully deleted files from S3 bucket {}: {}", bucketName, keys);

        } catch (S3Exception e) {
            log.error("Failed to delete objects from S3: {}", e.awsErrorDetails().errorMessage());
            throw new RuntimeException("Error deleting files from S3", e);
        }
    }
}
