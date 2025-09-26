package com.search_service.search_service.Dto;

import lombok.Data; // Ensure you have getters and setters
import java.util.Date;
import java.util.List;

@Data
public class UserTagsAndCategories {
    private List<String> tags;
    private List<String> categories;
}
