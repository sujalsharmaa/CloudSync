package com.search_service.search_service.Controller;

import com.search_service.search_service.Dto.UserFileMetadata;
import com.search_service.search_service.Dto.UserTagsAndCategories;
import com.search_service.search_service.Service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Slf4j
@RequestMapping("/api")
@RequiredArgsConstructor
public class SearchController {
    private final SearchService searchService;

    @GetMapping("/metadata/search")
    public ResponseEntity<List<UserFileMetadata>> searchMetadata(@RequestParam String query, @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaims().get("userId").toString();
        List<UserFileMetadata> results = searchService.searchByQuery(query, userId);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/metadata/user/starred")
    public ResponseEntity<List<UserFileMetadata>> getStarredFiles(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaims().get("userId").toString();
        List<UserFileMetadata> results = searchService.getStarredFiles(userId);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/metadata/search/trash")
    public ResponseEntity<List<UserFileMetadata>> searchTrashMetadata(@RequestParam String query, @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaims().get("userId").toString();
        List<UserFileMetadata> results = searchService.searchRecycledFilesByQuery(query, userId);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/metadata/user/search")
    public ResponseEntity<List<UserFileMetadata>> getAllFiles(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaims().get("userId").toString();
        List<UserFileMetadata> results = searchService.searchByUserId(userId);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/metadata/user/trash")
    public ResponseEntity<List<UserFileMetadata>> getAllRecycledFiles(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaims().get("userId").toString();
        List<UserFileMetadata> results = searchService.searchRecycledFilesByUserId(userId);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/metadata/user/recentFiles")
    public ResponseEntity<List<UserFileMetadata>> getRecentFiles(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaims().get("userId").toString();
        List<UserFileMetadata> results = searchService.searchRecentFilesByUserId(userId);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/metadata/user/tagsAndCategories")
    public ResponseEntity<UserTagsAndCategories> getAllTagsAndCategories(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaims().get("userId").toString();
        UserTagsAndCategories results = searchService.getAllUniqueTagsAndCategoriesByUserId(userId);
        return ResponseEntity.ok(results);
    }
}