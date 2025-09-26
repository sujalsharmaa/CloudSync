package com.tags_generation_service.tags_generation_service.Dto;

// tags-generation-service/Dto/ThumbnailResult.java
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ThumbnailResult {
    private String s3Url;
    private String thumbnailUrl;
}
