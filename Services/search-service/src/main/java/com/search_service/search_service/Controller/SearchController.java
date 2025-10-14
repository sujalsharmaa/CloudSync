package com.search_service.search_service.Controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.search_service.search_service.Dto.UserFileMetadata;
import com.search_service.search_service.Dto.UserTagsAndCategories;
import com.search_service.search_service.Service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

@RestController
@Slf4j
@RequestMapping("/api")
@RequiredArgsConstructor
public class SearchController {
    private final SearchService searchService;
    private final RedisTemplate<String, String> redisTemplate;
    // Inject ObjectMapper for serialization/deserialization
    private final ObjectMapper objectMapper;

    // Cache key prefix for user search results. This should match the invalidation logic.
    private static final String SEARCH_CACHE_PREFIX = "search:files:user:";
    // Key format from RedisFileService: 'pending_files:USER_ID'
    private static final String PENDING_FILE_KEY_PREFIX = "pending_files:"; // ADDED
    // Cache TTL (Time To Live) set to 5 minutes
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);


    @GetMapping("/metadata/search")
    public ResponseEntity<List<UserFileMetadata>> searchMetadata(
            @RequestParam String query,
            @AuthenticationPrincipal Jwt jwt) {

        String userId = jwt.getClaims().get("userId").toString();
        // 1. Define a unique cache key for this user and query
        // We hash the query to keep the key length manageable.
        String cacheKey = SEARCH_CACHE_PREFIX + userId + ":" + query.hashCode();

        try {
            // --- 2. CHECK CACHE ---
            String cachedResultsJson = redisTemplate.opsForValue().get(cacheKey);

            if (cachedResultsJson != null) {
                log.info("Cache hit for user {} with query: {}", userId, query);
                // Deserialize the JSON string back into a List of DTOs
                List<UserFileMetadata> cachedResults = objectMapper.readValue(
                        cachedResultsJson, new TypeReference<List<UserFileMetadata>>() {});
                return ResponseEntity.ok(cachedResults);
            }

            log.info("Cache miss for user {} with query: {}", userId, query);

            // --- 3. CACHE MISS: QUERY DATABASE ---
            List<UserFileMetadata> results = searchService.searchByQuery(query, userId);

            // --- 4. STORE RESULTS IN CACHE ---
            if (!results.isEmpty()) {
                String resultsJson = objectMapper.writeValueAsString(results);
                redisTemplate.opsForValue().set(cacheKey, resultsJson, CACHE_TTL);
                log.info("Results cached successfully for user {}. TTL: {} minutes", userId, CACHE_TTL.toMinutes());
            }

            // --- 5. Retrieve Pending File List (As requested by user) ---
            // NOTE: The current return type List<UserFileMetadata> does not allow returning
            // the pending list (List<String>) alongside search results.
            // For now, we will retrieve and log the pending list.

            String pendingFileKey = PENDING_FILE_KEY_PREFIX + userId;
            String pendingFilesJson = redisTemplate.opsForValue().get(pendingFileKey);

            if (pendingFilesJson != null) {
                List<String> pendingFiles = objectMapper.readValue(pendingFilesJson, new TypeReference<List<String>>() {});
                log.info("Retrieved {} pending files for user {}: {}", pendingFiles.size(), userId, pendingFiles);
                // If you need to send this list to the front-end, you must change the return type
                // of this method to a new wrapper DTO containing both the search results and the pending list.
            }
            // -------------------------------------------------------------

            return ResponseEntity.ok(results);

        } catch (IOException e) {
            log.error("Error reading/writing JSON for Redis/ObjectMapper for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            log.error("Error processing search or fetching data for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/metadata/user/starred")
    public ResponseEntity<List<UserFileMetadata>> getStarredFiles(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaims().get("userId").toString();
        try {
            // Correctly calling a method that returns List<FileMetadata>
            List<UserFileMetadata> results = searchService.getStarredFiles(userId);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            log.error("Error searching metadata", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/metadata/search/trash")
    public ResponseEntity<List<UserFileMetadata>> searchTrashMetadata(@RequestParam String query,@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaims().get("userId").toString();
        try {
            // Correctly calling a method that returns List<FileMetadata>
            List<UserFileMetadata> results = searchService.searchRecycledFilesByQuery(query,userId);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            log.error("Error searching metadata", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/metadata/user/search")
    public ResponseEntity<List<UserFileMetadata>> getAllFiles(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaims().get("userId").toString();
        try {
            // Correctly calling a method that returns List<FileMetadata>
            List<UserFileMetadata> results = searchService.searchByUserId(userId);
            System.out.println(results);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            log.error("Error searching metadata", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }



    @GetMapping("/metadata/user/trash")
    public ResponseEntity<List<UserFileMetadata>> getAllRecycledFiles(@AuthenticationPrincipal Jwt jwt){
        String userId = jwt.getClaims().get("userId").toString();
        try {
            // Correctly calling a method that returns List<FileMetadata>
            List<UserFileMetadata> results = searchService.searchRecycledFilesByUserId(userId);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            log.error("Error searching metadata", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    @GetMapping("/metadata/user/recentFiles")
    public ResponseEntity<List<UserFileMetadata>> getRecentFiles(@AuthenticationPrincipal Jwt jwt){
        String userId = jwt.getClaims().get("userId").toString();
        try {
            // Correctly calling a method that returns List<FileMetadata>
            List<UserFileMetadata> results = searchService.searchRecentFilesByUserId(userId);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            log.error("Error searching metadata", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/metadata/user/tagsAndCategories")
    public ResponseEntity<UserTagsAndCategories> getAllTagsAndCategories(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaims().get("userId").toString();
        try {
            // Correctly calling a method that returns List<FileMetadata>
            UserTagsAndCategories results = searchService.getAllUniqueTagsAndCategoriesByUserId(userId);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            log.error("Error searching metadata", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

}
