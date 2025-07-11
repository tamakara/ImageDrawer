package com.imagedrawer.dto;

import lombok.Data;

import java.util.List;

@Data
public class ImageQueryParams {
    private List<String> tags;
    private String sort;
    private String rating;
    private Integer page;
    private Integer pageSize;
    private String seed;
}