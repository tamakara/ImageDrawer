package com.tamakara.bakabooru.module.gallery.dto;

import lombok.Data;

@Data
public class SearchRequestDto {
    private String tagSearch;          // Tag search string (space separated, - for exclude)
    private String keyword;            // Title or Filename
}
