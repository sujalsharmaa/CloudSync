package com.search_service.search_service.Dto;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

@Data
public class UserTagsAndCategories implements Serializable {

    private static final long serialVersionUID = 1L;
    private List<String> tags;
    private List<String> categories;
}