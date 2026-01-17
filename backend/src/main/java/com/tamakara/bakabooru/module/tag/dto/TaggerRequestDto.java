package com.tamakara.bakabooru.module.tag.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
public class TaggerRequestDto {

    @JsonProperty("image_path")
    private String imagePath;

    @JsonProperty("threshold")
    private Double threshold;

    @JsonProperty("category_thresholds")
    private Map<String, Double> categoryThresholds;
}
