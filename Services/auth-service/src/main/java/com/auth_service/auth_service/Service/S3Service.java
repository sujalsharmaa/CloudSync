package com.auth_service.auth_service.Service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

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


    public long getUserFolderSize(String userId) {
        long totalSize = 0;
        String prefix = userId + "/";

        ListObjectsV2Request listReq = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .prefix(prefix)
                .build();

        try {
            // Use the paginator to handle cases where a user has more than 1000 files
            for (S3Object s3Object : s3Client.listObjectsV2Paginator(listReq).contents()) {
                // Ensure we only count files directly under the prefix
                if (s3Object.key().startsWith(prefix)) {
                    totalSize += s3Object.size();
                }
            }
        } catch (Exception e) {
            log.error("Error calculating folder size for user {}: {}", userId, e.getMessage());
            // In case of an S3 error, conservatively assume 0 to allow the check to fail on the next step
            return 0;
        }

        log.info("User {} currently uses {} bytes of storage.", userId, totalSize);
        return totalSize;
    }


}
