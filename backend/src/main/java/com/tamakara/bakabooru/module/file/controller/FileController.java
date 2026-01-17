package com.tamakara.bakabooru.module.file.controller;

import com.tamakara.bakabooru.module.gallery.entity.Image;
import com.tamakara.bakabooru.module.gallery.repository.ImageRepository;
import com.tamakara.bakabooru.module.file.service.StorageService;
import com.tamakara.bakabooru.module.system.service.SystemSettingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.Optional;

@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
@Tag(name = "文件", description = "文件访问操作")
public class FileController {

    private final StorageService storageService;
    private final ImageRepository imageRepository;
    private final SystemSettingService systemSettingService;

    @GetMapping("/{hash}")
    @Operation(summary = "获取图片文件")
    public ResponseEntity<Resource> getFile(@PathVariable String hash) {
        try {
            Path file = storageService.getFilePath(hash);
            Resource resource = new UrlResource(file.toUri());

            if (resource.exists() || resource.isReadable()) {
                String filename = hash;
                Optional<Image> image = imageRepository.findByHash(hash);
                if (image.isPresent()) {
                    filename = image.get().getTitle() + "." + image.get().getExtension();
                }

                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/thumb/{hash}")
    @Operation(summary = "获取缩略图")
    public ResponseEntity<Resource> getThumbnail(@PathVariable String hash) {
        try {
            int quality = systemSettingService.getIntSetting("file.thumbnail.quality", 80);
            int maxSize = systemSettingService.getIntSetting("file.thumbnail.max-size", 800);
            Path file = storageService.getThumbnailPath(hash, quality, maxSize);
            Resource resource = new UrlResource(file.toUri());

            if (resource.exists() || resource.isReadable()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG)
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }

        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}

