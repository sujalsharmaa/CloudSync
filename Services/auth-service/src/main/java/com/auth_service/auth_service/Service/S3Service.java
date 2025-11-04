package com.auth_service.auth_service.Service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

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

    public long getUserFolderSize(String userId) {
        long totalSize = 0;
        String prefix = userId + "/";

        ListObjectsV2Request listReq = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .prefix(prefix)
                .build();

        try {
            for (S3Object s3Object : s3Client.listObjectsV2Paginator(listReq).contents()) {
                if (s3Object.key().startsWith(prefix)) {
                    totalSize += s3Object.size();
                }
            }
        } catch (Exception e) {
            log.error("Error calculating folder size for user {}: {}", userId, e.getMessage());
            return 0;
        }

        log.info("User {} currently uses {} bytes of storage.", userId, totalSize);
        return totalSize;
    }
}