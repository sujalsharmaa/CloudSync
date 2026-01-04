package com.upload_download_rag_pipeline.upload_download_rag_pipeline.Service;

import com.upload_download_rag_pipeline.upload_download_rag_pipeline.Dto.S3UploadResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.InputStream;
import java.io.IOException;

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

    @Cacheable(value = "storage_usage", key = "#userId")
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
            return Long.MAX_VALUE;
        }

        log.info("User {} currently uses {} bytes of storage.", userId, totalSize);
        return totalSize;
    }


    @CacheEvict(value = "storage_usage", key = "#key.split('/')[0]")
    public S3UploadResult uploadFile(String key, InputStream inputStream) throws IOException {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        byte[] fileBytes = inputStream.readAllBytes();
        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(fileBytes));

        HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
        HeadObjectResponse headObjectResponse = s3Client.headObject(headObjectRequest);
        long fileSize = headObjectResponse.contentLength();

        String fileUrl = "https://" + bucketName + ".s3." + s3Client.serviceClientConfiguration().region().id() + ".amazonaws.com/" + key;

        log.info("Successfully uploaded file {} with size {} bytes to S3 bucket {}", key, fileSize, bucketName);

        return new S3UploadResult(fileUrl, fileSize);
    }

    public InputStream downloadFile(String s3Location) {

        String bucketEndpoint = bucketName + ".s3." + s3Client.serviceClientConfiguration().region().id() + ".amazonaws.com/";

        int startIndex = s3Location.indexOf(bucketEndpoint);

        if (startIndex == -1) {
            log.error("S3 Location URL not in expected format: {}", s3Location);
            throw new IllegalArgumentException("Invalid S3 URL format. Could not parse bucket endpoint.");
        }

        String key = s3Location.substring(startIndex + bucketEndpoint.length());

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        log.info("Downloading file from S3 using key: {}", key);

        return s3Client.getObject(getObjectRequest);
    }
}
