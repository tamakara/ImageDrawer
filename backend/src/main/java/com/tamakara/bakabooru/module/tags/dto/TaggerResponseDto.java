package com.tamakara.bakabooru.module.tags.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class TaggerResponseDto {
    private boolean success;
    private Map<String, List<String>> data;
    private String error;
}

