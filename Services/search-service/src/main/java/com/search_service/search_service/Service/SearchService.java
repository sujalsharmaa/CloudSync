package com.search_service.search_service.Service;

import com.search_service.search_service.Dto.FileMetadataMapper;
import com.search_service.search_service.Dto.UserFileMetadata;
import com.search_service.search_service.Dto.UserTagsAndCategories;
import com.search_service.search_service.Model.FileMetadata;
import com.search_service.search_service.Repository.FileMetadataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

    private final FileMetadataRepository repository;
    private final FileMetadataMapper fileMetadataMapper;


    @Caching(evict = {
            @CacheEvict(value = "userFiles", key = "#userId"),
            @CacheEvict(value = "recentFiles", key = "#userId"),
            @CacheEvict(value = "starredFiles", key = "#userId"),
            @CacheEvict(value = "recycledFiles", key = "#userId"),
            @CacheEvict(value = "userTags", key = "#userId")
    })
    public void evictUserFileCache(String userId) {
        log.info("Evicting all file caches for user: {}", userId);
    }

    @Cacheable(value = "userFiles", key = "#userId")
    public List<UserFileMetadata> searchByQuery(String query,String userId) {
        log.info("Performing semantic search: {}", query);
        List<FileMetadata> files = repository.searchAllByuserId(query,userId);
        return fileMetadataMapper.toUserFileMetadataList(files);
    }
    @Cacheable(value = "starredFiles", key = "#userId")
    public List<UserFileMetadata> getStarredFiles(String userId) {
        List<FileMetadata> files = repository.searchAllStarredByuserId(userId);
        return fileMetadataMapper.toUserFileMetadataList(files);
    }

    @Cacheable(value = "recentFiles", key = "#userId")
    public List<UserFileMetadata> searchRecentFilesByUserId(String userId) {

        List<FileMetadata> files = repository.searchAllRecentByuserId(userId);
        return fileMetadataMapper.toUserFileMetadataList(files);
    }

    @Cacheable(value = "recycledFiles", key = "#userId")
    public List<UserFileMetadata> searchRecycledFilesByQuery(String query,String userId) {
        log.info("Performing semantic search: {}", query);
        List<FileMetadata> files = repository.searchAllRecycledFilesByuserId(query,userId);
        return fileMetadataMapper.toUserFileMetadataList(files);
    }

    public List<UserFileMetadata> searchByUserId(String userId) {
        log.info("Performing semantic search: {}", userId);
        List<FileMetadata> files = repository.findByuserId(userId);
       System.out.println(files);
        return fileMetadataMapper.toUserFileMetadataList(files);
    }


    public List<UserFileMetadata> searchRecycledFilesByUserId(String userId) {
        log.info("Performing semantic search: {}", userId);
        List<FileMetadata> files = repository.findRecycledFilesByuserId(userId);
        return fileMetadataMapper.toUserFileMetadataList(files);
    }


    public List<FileMetadata> searchByTags(List<String> tags) {
        log.info("Searching files by tags: {}", tags);
        List<FileMetadata> results = new ArrayList<>();
        for (String tag : tags) {
            results.addAll(repository.findByTags(tag));
        }
        return results.stream().distinct().collect(Collectors.toList());
    }

    @Cacheable(value = "userTags", key = "#userId")
    public UserTagsAndCategories getAllUniqueTagsAndCategoriesByUserId(String userId) {
        List<FileMetadata> files = repository.findByuserId(userId);
        Map<String, String> tagsMap = new LinkedHashMap<>();
        files.stream()
                .flatMap(f -> Optional.ofNullable(f.getTags()).orElse(Collections.emptyList()).stream())
                .flatMap(this::splitToTokens)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .forEach(token -> tagsMap.putIfAbsent(token.toLowerCase(), token));

        Map<String, String> categoriesMap = new LinkedHashMap<>();
        files.stream()
                .flatMap(f -> Optional.ofNullable(f.getCategories()).orElse(Collections.emptyList()).stream())
                .flatMap(this::splitToTokens)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .forEach(token -> categoriesMap.putIfAbsent(token.toLowerCase(), token));

        UserTagsAndCategories result = new UserTagsAndCategories();
        result.setTags(new ArrayList<>(tagsMap.values()));
        result.setCategories(new ArrayList<>(categoriesMap.values()));
        return result;
    }


    private Stream<String> splitToTokens(String raw) {
        if (raw == null) return Stream.empty();
        raw = raw.trim();

        if (raw.startsWith("[") && raw.endsWith("]") && raw.length() >= 2) {
            raw = raw.substring(1, raw.length() - 1).trim();
        }
        if (raw.isEmpty()) return Stream.empty();

        if (raw.contains(",")) {
            return Arrays.stream(raw.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty());
        } else {
            return Stream.of(raw);
        }
    }


}