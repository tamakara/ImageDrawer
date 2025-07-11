package com.imagedrawer.controller;

import com.imagedrawer.service.TagService;
import com.imagedrawer.service.TaggerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tag")
@RequiredArgsConstructor
public class TagController {
    private final TagService tagService;
    private final TaggerService taggerService;

    @PutMapping("/tagger")
    public ResponseEntity<?> setTaggerUrl(@RequestBody String url) {
        taggerService.setTaggerUrl(url);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/query")
    public ResponseEntity<List<String>> queryTags(
            @RequestParam(value = "keyword", required = false) String keyword) {
        List<String> tags = tagService.queryTags(keyword);
        return ResponseEntity.ok().body(tags);
    }
}
