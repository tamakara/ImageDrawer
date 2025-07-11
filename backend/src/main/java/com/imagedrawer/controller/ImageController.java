package com.imagedrawer.controller;

import com.imagedrawer.dto.ImageResponse;
import com.imagedrawer.entity.Image;
import com.imagedrawer.service.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/image")
@RequiredArgsConstructor
public class ImageController {
    private final ImageService imageService;

    @PostMapping
    public ResponseEntity<Void> addImage(@RequestParam("file") MultipartFile file) {
        imageService.addImage(file);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{hash}")
    public ResponseEntity<Void> deleteImage(@PathVariable String hash) {
        imageService.deleteImage(hash);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{hash}")
    public ResponseEntity<ImageResponse> getImage(@PathVariable String hash) {
        ImageResponse response = imageService.getImage(hash);
        return ResponseEntity.ok().body(response);
    }

}
