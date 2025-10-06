package com.tags_generation_service.tags_generation_service.Model;

import com.rometools.utils.Strings;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "file_metadata")
public class FileMetadataPostgres {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "file_type")
    private String fileType;

    @Column(name = "tags", columnDefinition = "jsonb")
    @Type(JsonBinaryType.class)
    private List<String> tags;

    @Column(name = "categories", columnDefinition = "jsonb")
    @Type(JsonBinaryType.class)
    private List<String> categories;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "security_status")
    private String securityStatus;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "processed_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date processedAt;

    @Column(name = "additional_metadata", columnDefinition = "jsonb")
    @Type(JsonBinaryType.class)
    private Map<String, Object> additionalMetadata;

    @Column(name = "s3_location", unique = true)
    private String s3Location;

    @Column(name = "thumbnail_s3_location", columnDefinition = "TEXT")
    private String thumbnailS3Location;

    @Column(name = "userId", columnDefinition = "TEXT")
    private String userId;

    @Column(name = "isMovedToRecycleBin",columnDefinition = "Bool")
    private Boolean isMovedToRecycleBin;

    @Column(name = "isStarred", columnDefinition = "Bool")
    private Boolean isStarred;

    @Column(name = "fileSize", columnDefinition = "BIGINT")
    private Long fileSize;

    @Column(name = "modifiedAt")
    @Temporal(TemporalType.TIMESTAMP)
    private Date modifiedAt;

}