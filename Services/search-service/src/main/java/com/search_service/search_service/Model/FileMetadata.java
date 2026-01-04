// search_service/Model/FileMetadata.java
package com.search_service.search_service.Model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "file-metadata")
public class FileMetadata {
    @Id
    private String id;

    @Field(type = FieldType.Text)
    private String fileName;

    @Field(type = FieldType.Keyword)
    private String fileType;

    @Field(type = FieldType.Keyword)
    private List<String> tags;

    @Field(type = FieldType.Keyword)
    private List<String> categories;

    @Field(type = FieldType.Text)
    private String summary;

    @Field(type = FieldType.Keyword)
    private String securityStatus;

    @Field(type = FieldType.Text)
    private String rejectionReason;

    @Field(type = FieldType.Date)
    private Date processedAt;

    @Field(type = FieldType.Object)
    private Map<String, Object> additionalMetadata;

    @Field(type = FieldType.Text)
    private String s3Location;

    @Field(type= FieldType.Text)
    private String userId;

    @Field(type = FieldType.Boolean)
    private Boolean isMovedToRecycleBin;

    @Field(type = FieldType.Boolean)
    private Boolean isStarred = false;

    @Field(type = FieldType.Long)
    private Long fileSize;

    @Field(type = FieldType.Date)
    private Date modifiedAt;

    @Field(type = FieldType.Text)
    private String email;

}