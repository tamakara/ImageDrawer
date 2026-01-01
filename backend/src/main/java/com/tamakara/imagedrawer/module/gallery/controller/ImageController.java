package com.tamakara.imagedrawer.module.gallery.controller;

import com.tamakara.imagedrawer.module.gallery.dto.ImageDto;
import com.tamakara.imagedrawer.module.gallery.service.ImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
@Tag(name = "图库", description = "图片库操作")
public class ImageController {

    private final ImageService imageService;

    @GetMapping
    @Operation(summary = "获取图片列表", description = "获取分页的图片列表")
    public Page<ImageDto> listImages(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return imageService.listImages(pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取图片详情")
    public ImageDto getImage(@PathVariable Long id) {
        return imageService.getImage(id);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除图片")
    public void deleteImage(@PathVariable Long id) {
        imageService.deleteImage(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新图片详情")
    public ImageDto updateImage(@PathVariable Long id, @RequestBody ImageDto dto) {
        return imageService.updateImage(id, dto);
    }

    @PostMapping(value = "/{id}/file", consumes = "multipart/form-data")
    @Operation(summary = "更新图片文件")
    public ImageDto updateImageFile(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "updateName", defaultValue = "false") boolean updateName) {
        return imageService.updateImageFile(id, file, updateName);
    }
}
