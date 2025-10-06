package com.upload_download_rag_pipeline.upload_download_rag_pipeline.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration // 👈 Tells Spring this class defines configuration/beans
public class RestClientConfig {

    /**
     * Defines the RestClient bean that can be injected anywhere in the application.
     */
    @Bean // 👈 Makes the return value of this method a Spring Bean
    public RestClient restClient() {
        // You can use the default builder or customize it here (e.g., set base URL, timeouts)
        return RestClient.create();
    }
}