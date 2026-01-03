package com.upload_download_rag_pipeline.upload_download_rag_pipeline.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "ban")
public class BanProperties {

    private Redis redis;
    private List<Rule> rules;

    @Data
    public static class Redis {
        private String violationKeyPrefix;
        private String banKeyPrefix;
        private String lifetimeValue;
    }

    @Data
    public static class Rule {
        private int count;
        private String duration;
        private Duration ttl;
        private boolean lifetime = false;
        private String reason;
    }
}
