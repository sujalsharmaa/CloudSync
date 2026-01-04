package com.search_service.search_service.Dto;


import com.search_service.search_service.Model.FileMetadata;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring")
public interface FileMetadataMapper {

    FileMetadataMapper INSTANCE = Mappers.getMapper(FileMetadataMapper.class);
    @Mapping(target = "fileSize", source = "fileSize")
    UserFileMetadata toUserFileMetadata(FileMetadata fileMetadata);

    List<UserFileMetadata> toUserFileMetadataList(List<FileMetadata> fileMetadataList);
}
