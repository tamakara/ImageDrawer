package com.tamakara.imagedrawer.module.tagger.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
public class TaggerRequestDto {

    @JsonProperty("image_hash")
    private String imageHash;

    @JsonProperty("threshold")
    private Double threshold;

    @JsonProperty("min_confidence")
    private Double minConfidence;

    @JsonProperty("category_thresholds")
    private Map<String, Double> categoryThresholds;
}
