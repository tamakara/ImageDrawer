package com.tamakara.imagedrawer.module.tagger.dto;

import lombok.Data;

import java.util.List;

@Data
public class TaggerResponseDto {
    private boolean success;
    private TagData data;
    private String error;

    @Data
    public static class TagData {
        private List<String> artist;
        private List<String> character;
        private List<String> copyright;
        private List<String> general;
        private List<String> meta;
        private String rating;
    }
}

