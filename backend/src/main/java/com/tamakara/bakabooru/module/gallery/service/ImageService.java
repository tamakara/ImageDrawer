package com.tamakara.bakabooru.module.gallery.service;

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

    @Transactional(readOnly = true)
    public Page<ImageDto> listImages(Pageable pageable) {
        return imageRepository.findAll(pageable).map(imageMapper::toDto);
    }

    @Transactional(readOnly = true)
    public ImageDto getImage(Long id) {
        return imageRepository.findById(id)
                .map(imageMapper::toDto)
                .orElseThrow(() -> new RuntimeException("Image not found"));
    }

    @Transactional
    public void deleteImage(Long id) {
        Image image = imageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Image not found"));

        // storageService.delete(image.getPath()); // 不要删除文件，因为它可能被其他人使用（去重）
        // 或者检查是否被使用。目前，让我们保留文件。

        imageRepository.delete(image);
    }

    @Transactional
    public ImageDto updateImage(Long id, ImageDto dto) {
        Image image = imageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Image not found"));

        if (dto.getTitle() != null) {
            image.setTitle(dto.getTitle());
        }
        image.setUpdatedAt(LocalDateTime.now());
        return imageMapper.toDto(imageRepository.save(image));
    }

    @Transactional
    public ImageDto updateImageFile(Long id, MultipartFile file, boolean updateName) {
        Image image = imageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Image not found"));

        // 存储新文件
        String newHash = storageService.store(file);

        // 更新图像实体
        image.setHash(newHash);
        image.setSize(file.getSize());
        image.setUpdatedAt(LocalDateTime.now());

        String originalFilename = file.getOriginalFilename();
        image.setFileName(originalFilename);

        String extension = "";
        if (originalFilename != null) {
            int dotIndex = originalFilename.lastIndexOf('.');
            if (dotIndex != -1) {
                extension = originalFilename.substring(dotIndex + 1);
            }
        }
        image.setExtension(extension);

        if (updateName && originalFilename != null) {
             String title = originalFilename;
             int dotIndex = originalFilename.lastIndexOf('.');
             if (dotIndex != -1) {
                title = originalFilename.substring(0, dotIndex);
             }
             image.setTitle(title);
        }

        // 更新尺寸
        try (ImageInputStream in = ImageIO.createImageInputStream(storageService.getFilePath(newHash).toFile())) {
            final Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
            if (readers.hasNext()) {
                ImageReader reader = readers.next();
                try {
                    reader.setInput(in);
                    image.setWidth(reader.getWidth(0));
                    image.setHeight(reader.getHeight(0));
                } finally {
                    reader.dispose();
                }
            }
        } catch (Exception e) {
            // 记录日志
        }

        return imageMapper.toDto(imageRepository.save(image));
    }

    @Transactional
    public ImageDto regenerateTags(Long id) {
        Image image = imageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Image not found"));

        // Call tagger service
        Map<String, List<String>> newTagsMap = taggerService.tagImage(image.getHash());

        // Keep custom tags
        Set<Tag> tagsToKeep = image.getTags().stream()
                .filter(tag -> "custom".equals(tag.getType()))
                .collect(Collectors.toSet());

        // Process new tags
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
        return imageMapper.toDto(imageRepository.save(image));
    }

    @Transactional
    public ImageDto addTag(Long id, TagDto tagDto) {
        Image image = imageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Image not found"));

        Tag tag = tagService.findOrCreateTag(tagDto.getName(), tagDto.getType() != null ? tagDto.getType() : "custom");
        image.getTags().add(tag);
        image.setUpdatedAt(LocalDateTime.now());

        return imageMapper.toDto(imageRepository.save(image));
    }

    @Transactional
    public ImageDto removeTag(Long id, Long tagId) {
        Image image = imageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Image not found"));

        image.getTags().removeIf(tag -> tag.getId().equals(tagId));
        image.setUpdatedAt(LocalDateTime.now());

        return imageMapper.toDto(imageRepository.save(image));
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
                    // Unique entry name: title + id + extension
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
