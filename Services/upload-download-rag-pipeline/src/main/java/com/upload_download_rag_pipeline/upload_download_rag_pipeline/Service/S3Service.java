package com.upload_download_rag_pipeline.upload_download_rag_pipeline.Service;

import com.upload_download_rag_pipeline.upload_download_rag_pipeline.Dto.S3UploadResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
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
    private final String bucketName = "your-rag-pipeline-bucket"; // Replace with your S3 bucket name

    // Default storage quota: 1GB (1024 * 1024 * 1024 bytes)
    private static final long MAX_STORAGE_BYTES = 1024L * 1024 * 1024;

    public S3Service() {
        this.s3Client = S3Client.builder()
                .region(Region.US_EAST_1) // Replace with your region
                .build();
    }

    /**
     * Returns the maximum allowed storage quota in bytes.
     * @return The max storage quota (1GB).
     */
    public long getMaxStorageBytes() {
        return MAX_STORAGE_BYTES;
    }

    /**
     * Calculates the total size in bytes of all files stored under a specific user's folder (prefix).
     * @param userId The ID of the user whose storage usage to check.
     * @return The total size in bytes.
     */
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
            return Long.MAX_VALUE;
        }

        log.info("User {} currently uses {} bytes of storage.", userId, totalSize);
        return totalSize;
    }

    /**
     * Uploads a file to S3 using the provided key (which now includes the user ID folder).
     * @param key The full S3 key, e.g., "userId/fileName.txt"
     * @param inputStream The file data stream.
     * @return S3UploadResult containing the generated URL and file size.
     * @throws IOException
     */
    public S3UploadResult uploadFile(String key, InputStream inputStream) throws IOException {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        byte[] fileBytes = inputStream.readAllBytes();
        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(fileBytes));

        // Use headObject to get the file size
        HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
        HeadObjectResponse headObjectResponse = s3Client.headObject(headObjectRequest);
        long fileSize = headObjectResponse.contentLength();

        // Construct the URL (e.g., https://<bucket>.s3.<region>.amazonaws.com/userId/fileName.txt)
        String fileUrl = "https://" + bucketName + ".s3." + s3Client.serviceClientConfiguration().region().id() + ".amazonaws.com/" + key;

        log.info("Successfully uploaded file {} with size {} bytes to S3 bucket {}", key, fileSize, bucketName);

        // Return a new S3UploadResult object containing both values
        return new S3UploadResult(fileUrl, fileSize);
    }

    /**
     * Downloads a file from S3 and returns it as an InputStream.
     * Extracts the full S3 key (e.g., userId/fileName.txt) from the S3 URL.
     * @param s3Location The full S3 URI containing the user folder.
     * @return An InputStream of the downloaded file.
     */
    public InputStream downloadFile(String s3Location) {
        // Construct the expected base URL for parsing
        String bucketEndpoint = bucketName + ".s3." + s3Client.serviceClientConfiguration().region().id() + ".amazonaws.com/";

        // Find the index right after the bucket endpoint to get the full key
        int startIndex = s3Location.indexOf(bucketEndpoint);

        if (startIndex == -1) {
            log.error("S3 Location URL not in expected format: {}", s3Location);
            throw new IllegalArgumentException("Invalid S3 URL format. Could not parse bucket endpoint.");
        }

        // Extract the full key (e.g., "user123/document.pdf")
        String key = s3Location.substring(startIndex + bucketEndpoint.length());

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        log.info("Downloading file from S3 using key: {}", key);

        // The getObject method returns an SdkResponseInputStream
        return s3Client.getObject(getObjectRequest);
    }
}
