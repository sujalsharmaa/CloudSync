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

import java.io.InputStream;
import java.io.IOException;

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

        // Construct the URL
        String fileUrl = "https://" + bucketName + ".s3." + s3Client.serviceClientConfiguration().region().id() + ".amazonaws.com/" + key;

        log.info("Successfully uploaded file {} with size {} bytes to S3 bucket {}", key, fileSize, bucketName);

        // Return a new S3UploadResult object containing both values
        return new S3UploadResult(fileUrl, fileSize);
    }

    /**
     * Downloads a file from S3 and returns it as an InputStream.
     * @param s3Location The full S3 URI or object key.
     * @return An InputStream of the downloaded file.
     */
    public InputStream downloadFile(String s3Location) {
        // Extract the key from the S3 location URL
        String key = s3Location.substring(s3Location.lastIndexOf("/") + 1);

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        log.info("Downloading file from S3: {}", s3Location);

        // The getObject method returns an SdkResponseInputStream
        return s3Client.getObject(getObjectRequest);
    }
}