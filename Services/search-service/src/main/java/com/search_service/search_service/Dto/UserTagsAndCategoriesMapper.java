package com.search_service.search_service.Dto;

import com.search_service.search_service.Model.FileMetadata;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import java.util.List;

@Mapper(componentModel = "spring")
public interface UserTagsAndCategoriesMapper {
    FileMetadataMapper INSTANCE = Mappers.getMapper(FileMetadataMapper.class);
    UserTagsAndCategories toUserTagsAndCategories(FileMetadata fileMetadata);
    List<UserTagsAndCategories> toUserFileMetadataList(List<FileMetadata> fileMetadataList);
}

