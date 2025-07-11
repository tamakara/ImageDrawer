package com.imagedrawer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ImageResponse {
    private String filename;
    private String mimetype;
    private String hash;
    private Integer width;
    private Integer height;
    private Long size;
    private String rating;
    private List<String> tags;
}
