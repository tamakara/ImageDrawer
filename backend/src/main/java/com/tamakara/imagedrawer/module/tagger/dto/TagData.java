package com.tamakara.imagedrawer.module.tagger.dto;

import lombok.Data;

import java.util.List;

@Data
public class TagData {
    private List<String> artist;
    private List<String> character;
    private List<String> copyright;
    private List<String> general;
    private List<String> meta;
    private String rating;
}