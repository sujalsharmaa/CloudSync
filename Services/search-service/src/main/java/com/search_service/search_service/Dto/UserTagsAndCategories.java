package com.search_service.search_service.Dto;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

@Data
public class UserTagsAndCategories implements Serializable {

    // It is best practice to define a serialVersionUID to ensure compatibility during deserialization
    private static final long serialVersionUID = 1L;

    private List<String> tags;
    private List<String> categories;
}