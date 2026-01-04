
package com.search_service.search_service.Dto;

import lombok.Data;
import java.util.Date;

@Data
public class UserFileMetadata {
    private String id;
    private String fileName;
    private String fileType;
    private Date processedAt;
    private Boolean isStarred;
    private Long fileSize;
}