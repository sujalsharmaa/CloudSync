package com.search_service.search_service.Repository;

import com.search_service.search_service.Model.FileMetadata;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileMetadataRepository extends ElasticsearchRepository<FileMetadata, String>  {


    // Corrected to return a List<FileMetadata>
    List<FileMetadata> findByTags(String tag);

@Query("{\"bool\": {\"must\": [{\"match\": {\"userId\": \"?0\"}}, {\"term\": {\"isMovedToRecycleBin\": false}}]}}")
List<FileMetadata> findByuserId(String userId);

    @Query("{\"bool\": {\"must\": [{\"match\": {\"userId\": \"?0\"}}, {\"term\": {\"isMovedToRecycleBin\": false}]}}, \"sort\": [{\"modifiedAt\": \"desc\"}]}")
    List<FileMetadata> searchAllRecentByuserId(String userId);

    @Query("{\"bool\": {\"must\": [{\"match\": {\"userId\": \"?0\"}}, {\"term\": {\"isMovedToRecycleBin\": true}}]}}")
List<FileMetadata> findRecycledFilesByuserId(String userId);

    // Corrected to return a List<FileMetadata>
    List<FileMetadata> findByCategories(String category);

    // Corrected to return a List<FileMetadata>
    @Query("{\"bool\": {\"must\": [{\"match\": {\"summary\": \"?0\"}}, {\"term\": {\"isMovedToRecycleBin\": false}}]}}")
    List<FileMetadata> searchBySummary(String query);

    @Query("{\"bool\": {\"must\": [" +
            "{\"match\": {\"userId\": \"?1\"}}," +
            "{\"multi_match\": {" +
            "\"query\": \"?0\"," +
            "\"fields\": [\"fileName^3\", \"summary\", \"tags\"]," +
            "\"fuzziness\": \"AUTO\"" +
            "}}," +
            "{\"term\": {\"isMovedToRecycleBin\": false}}" +
            "]}}")
    List<FileMetadata> searchAllByuserId(String query, String userId);

    @Query("{\"bool\": {\"must\": [{\"match\": {\"userId\": \"?0\"}}, {\"term\": {\"isMovedToRecycleBin\": false}}, {\"term\": {\"isStarred\": true}}]}}")
    List<FileMetadata> searchAllStarredByuserId(String userId);

    @Query("{\"bool\": {\"must\": [" +
            "{\"match\": {\"userId\": \"?1\"}}," +
            "{\"multi_match\": {" +
            "\"query\": \"?0\"," +
            "\"fields\": [\"fileName^3\", \"summary\", \"tags\"]," +
            "\"fuzziness\": \"AUTO\"" +
            "}}," +
            "{\"term\": {\"isMovedToRecycleBin\": true}}" +
            "]}}")
    List<FileMetadata> searchAllRecycledFilesByuserId(String query,String userId);

    @Query("{\"bool\": {\"must\": [" +
            "{\"match\": {\"userId\": \"?1\"}}," +
            "{\"multi_match\": {" +
            "\"query\": \"?0\"," +
            "\"fields\": [ \"categories\", \"tags\"]," +
            "\"fuzziness\": \"AUTO\"" +
            "}}" +
            "]}}")
    List<FileMetadata> searchAllTagsAndCategoriesByUserId(String userId);
}
