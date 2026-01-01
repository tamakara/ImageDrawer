package com.tamakara.imagedrawer.module.search.dto;

import lombok.Data;

import java.util.List;

@Data
public class SearchRequestDto {
    private List<String> includedTags; // AND
    private List<String> excludedTags; // NOT
    private List<String> anyTags;      // OR (at least one of these)
}

