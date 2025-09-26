package com.tags_generation_service.tags_generation_service.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "file_sharing")
public class FileSharing {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Foreign key to the file that is being shared
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    private FileMetadataPostgres file;

    // Foreign key to the user the file is shared with (the recipient)
    // NOTE: This assumes your User entity's primary key is 'id' of type Long
    // If you are using a different service for Users, you may only store the ID string.
    @Column(name = "shared_with_user_id", nullable = false)
    private Long sharedWithUserId;

    @Column(name = "shared_at")
    @Temporal(TemporalType.TIMESTAMP)
    @Builder.Default
    private Date sharedAt = new Date();
}