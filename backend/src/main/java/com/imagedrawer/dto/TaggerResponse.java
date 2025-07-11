package com.imagedrawer.dto;

import lombok.Data;

import java.util.List;

@Data
public class TaggerResponse {
    private String rating;
    private List<String> tags;
}
