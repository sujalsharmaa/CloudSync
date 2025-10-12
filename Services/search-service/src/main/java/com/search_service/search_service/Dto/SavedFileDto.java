package com.search_service.search_service.Dto;

import lombok.*;

@Data
@Getter
@Setter
@RequiredArgsConstructor
@AllArgsConstructor
public class SavedFileDto {
    private String userId;
    private String fileName;
}
