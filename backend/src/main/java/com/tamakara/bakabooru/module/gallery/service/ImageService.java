package com.tamakara.bakabooru.module.gallery.service;

import com.tamakara.bakabooru.module.file.service.SignatureService;
import com.tamakara.bakabooru.module.file.service.StorageService;
import com.tamakara.bakabooru.module.gallery.dto.ImageDto;
import com.tamakara.bakabooru.module.gallery.entity.Image;
import com.tamakara.bakabooru.module.gallery.mapper.ImageMapper;
import com.tamakara.bakabooru.module.gallery.repository.ImageRepository;
import com.tamakara.bakabooru.module.tag.dto.TagDto;
import com.tamakara.bakabooru.module.tag.entity.Tag;
import com.tamakara.bakabooru.module.tag.service.TagService;
import com.tamakara.bakabooru.module.tag.service.TaggerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ImageService {

    private final ImageRepository imageRepository;
    private final ImageMapper imageMapper;
    private final StorageService storageService;
    private final TagService tagService;
    private final TaggerService taggerService;
    private final SignatureService signatureService;

    @Transactional(readOnly = true)
    public Page<ImageDto> listImages(Pageable pageable) {
        return imageRepository.findAll(pageable).map(image -> imageMapper.toDto(image, signatureService));
    }

    @Transactional(readOnly = true)
    public ImageDto getImage(Long id) {
        return imageRepository.findById(id)
                .map(image -> imageMapper.toDto(image, signatureService))
                .orElseThrow(() -> new RuntimeException("找不到图片"));
    }

    @Transactional
    public void deleteImage(Long id) {
        Image image = imageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("找不到图片"));

        // storageService.delete(image.getPath()); // 不要删除文件，因为它可能被其他人使用（去重）
        // 或者检查是否被使用。目前，让我们保留文件。

        imageRepository.delete(image);
    }

    @Transactional
    public ImageDto updateImage(Long id, ImageDto dto) {
        Image image = imageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("找不到图片"));

        if (dto.getTitle() != null) {
            image.setTitle(dto.getTitle());
        }
        image.setUpdatedAt(LocalDateTime.now());
        return imageMapper.toDto(imageRepository.save(image), signatureService);
    }

    @Transactional
    public ImageDto regenerateTags(Long id) {
        Image image = imageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("找不到图片"));

        // 生成标签
        Map<String, List<String>> newTagsMap = taggerService.tagImage(image.getHash());

        // 保留自定义标签
        Set<Tag> tagsToKeep = image.getTags().stream()
                .filter(tag -> "custom".equals(tag.getType()))
                .collect(Collectors.toSet());

        // 处理新标签
        for (Map.Entry<String, List<String>> entry : newTagsMap.entrySet()) {
            String type = entry.getKey();
            List<String> names = entry.getValue();
            for (String name : names) {
                Tag tag = tagService.findOrCreateTag(name, type);
                tagsToKeep.add(tag);
            }
        }

        image.setTags(tagsToKeep);
        image.setUpdatedAt(LocalDateTime.now());
        return imageMapper.toDto(imageRepository.save(image), signatureService);
    }

    @Transactional
    public ImageDto addTag(Long id, TagDto tagDto) {
        Image image = imageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("找不到图片"));

        Tag tag = tagService.findOrCreateTag(tagDto.getName(), tagDto.getType() != null ? tagDto.getType() : "custom");
        image.getTags().add(tag);
        image.setUpdatedAt(LocalDateTime.now());

        return imageMapper.toDto(imageRepository.save(image), signatureService);
    }

    @Transactional
    public ImageDto removeTag(Long id, Long tagId) {
        Image image = imageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("找不到图片"));

        image.getTags().removeIf(tag -> tag.getId().equals(tagId));
        image.setUpdatedAt(LocalDateTime.now());

        return imageMapper.toDto(imageRepository.save(image), signatureService);
    }

    @Transactional
    public void deleteImages(List<Long> ids) {
        ids.forEach(id -> {
            try {
                deleteImage(id);
            } catch (Exception e) {
                // ignore
            }
        });
    }

    public void downloadImages(List<Long> ids, java.io.OutputStream outputStream) throws java.io.IOException {
        List<Image> images = imageRepository.findAllById(ids);
        if (images.isEmpty()) {
            return;
        }

        try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(outputStream)) {
            for (Image image : images) {
                java.nio.file.Path file = storageService.getFilePath(image.getHash());
                if (java.nio.file.Files.exists(file)) {
                    // 文件名: title_id.extension
                    String entryName = String.format("%s_%d.%s",
                            image.getTitle().replaceAll("[\\\\/:*?\"<>|]", "_"),
                            image.getId(),
                            image.getExtension());

                    java.util.zip.ZipEntry zipEntry = new java.util.zip.ZipEntry(entryName);
                    zos.putNextEntry(zipEntry);
                    java.nio.file.Files.copy(file, zos);
                    zos.closeEntry();
                }
            }
        }
    }
}
