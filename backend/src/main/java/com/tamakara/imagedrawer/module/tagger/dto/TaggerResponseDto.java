package com.tamakara.imagedrawer.module.tagger.dto;

import lombok.Data;

import java.util.List;

@Data
public class TaggerResponseDto {
    private boolean success;
    private TaggerData data;
    private String error;

    @Data
    public static class TaggerData {
        private List<TaggerTag> tags;
    }

    @Data
    public static class TaggerTag {
        private String name;
        private Double score;
    }
}

