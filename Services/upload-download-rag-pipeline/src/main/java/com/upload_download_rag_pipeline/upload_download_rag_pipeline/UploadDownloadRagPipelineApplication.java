package com.upload_download_rag_pipeline.upload_download_rag_pipeline;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class UploadDownloadRagPipelineApplication {

	public static void main(String[] args) {
		SpringApplication.run(UploadDownloadRagPipelineApplication.class, args);
	}

}
