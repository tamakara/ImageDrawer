package com.tamakara.bakabooru.module.gallery.service;

import com.tamakara.bakabooru.module.file.service.StorageService;
import com.tamakara.bakabooru.module.gallery.dto.ImageDto;
import com.tamakara.bakabooru.module.gallery.entity.Image;
import com.tamakara.bakabooru.module.gallery.mapper.ImageMapper;
import com.tamakara.bakabooru.module.gallery.repository.ImageRepository;
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

@Service
@RequiredArgsConstructor
public class ImageService {

    private final ImageRepository imageRepository;
    private final ImageMapper imageMapper;
    private final StorageService storageService;

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
}
