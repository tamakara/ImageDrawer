package com.tamakara.bakabooru.module.gallery.dto;

import lombok.Data;

@Data
public class SearchRequestDto {
    private String tagSearch;
    private String keyword;
    private String randomSeed;
}
