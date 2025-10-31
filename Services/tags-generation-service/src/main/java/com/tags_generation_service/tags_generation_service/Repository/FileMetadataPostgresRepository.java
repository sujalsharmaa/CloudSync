// FileMetadataPostgresRepository.java
package com.tags_generation_service.tags_generation_service.Repository;

import com.tags_generation_service.tags_generation_service.Model.FileMetadataPostgres;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FileMetadataPostgresRepository extends JpaRepository<FileMetadataPostgres, UUID> {
    Optional<FileMetadataPostgres> findByS3Location(String s3Location);
    @Modifying
    @Query("UPDATE FileMetadataPostgres f SET f.thumbnailS3Location = :thumbnailUrl WHERE f.s3Location = :s3Location")
    int updateThumbnailUrlByS3Location(@Param("s3Location") String s3Location, @Param("thumbnailUrl") String thumbnailUrl);

    // Use a native query with a database-specific function for case-insensitive search.
    // This is for PostgreSQL's ILIKE operator.
    @Query(value = "SELECT * FROM file_metadata WHERE tags LIKE %?1%", nativeQuery = true)
    List<FileMetadataPostgres> findByTagsContaining(String tag);

    @Query(value = "SELECT * FROM file_metadata WHERE categories LIKE %?1%", nativeQuery = true)
    List<FileMetadataPostgres> findByCategoriesContaining(String category);

    // This derived query method still works as it maps to a standard `LIKE`
    FileMetadataPostgres findByFileName(String fileName);
}