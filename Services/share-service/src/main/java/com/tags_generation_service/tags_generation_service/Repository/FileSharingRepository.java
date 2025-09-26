package com.tags_generation_service.tags_generation_service.Repository;

import com.tags_generation_service.tags_generation_service.Model.FileSharing;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FileSharingRepository extends JpaRepository<FileSharing, UUID> {
}
