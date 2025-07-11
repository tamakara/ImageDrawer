package com.imagedrawer.service;

import com.imagedrawer.dto.ImageResponse;
import com.imagedrawer.dto.TaggerResponse;
import com.imagedrawer.entity.Image;
import com.imagedrawer.entity.Tag;
import com.imagedrawer.repository.ImageRepository;
import com.imagedrawer.repository.TagRepository;
import com.imagedrawer.util.ImageInfo;
import lombok.RequiredArgsConstructor;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.*;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
public class ImageService {
    private final ImageRepository imageRepository;
    private final TagService tagService;
    private final TaggerService taggerService;

    @Value("${image-drawer.data.dir}")
    private String dataDir;

    @Transactional
    public void addImage(MultipartFile file) {
        ImageInfo imageInfo;
        try {
            imageInfo = new ImageInfo(file);
        } catch (Exception e) {
            throw new RuntimeException("获取图片信息失败", e);
        }
        if (imageRepository.existsByHash(imageInfo.getHash())) {
            throw new RuntimeException("图片已存在");
        }

        TaggerResponse tagger = taggerService.tagger(file);
        String rating = tagger.getRating();
        List<Tag> tags = tagService.resolveTags(tagger.getTags());

        Image image = new Image(imageInfo, rating, tags);

        imageRepository.save(image);

        try {
            Files.copy(file.getInputStream(), Paths.get(dataDir, "images", imageInfo.getHash()), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            imageRepository.deleteByHash(imageInfo.getHash());
            throw new RuntimeException("保存图片文件失败", e);
        }

        try {
            Path thumbnailDir = Paths.get(dataDir, "thumbnail");
            Files.createDirectories(thumbnailDir);
            Path thumbnailPath = thumbnailDir.resolve(imageInfo.getHash());

            if (file.getSize() <= 1024 * 1024) {
                Files.copy(file.getInputStream(), thumbnailPath, StandardCopyOption.REPLACE_EXISTING);
            } else {
                byte[] bytes = file.getBytes();
                try (OutputStream os = Files.newOutputStream(thumbnailPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                    Thumbnails.of(new ByteArrayInputStream(bytes))
                            .size(1024, 1024)
                            .outputQuality(0.8)
                            .toOutputStream(os);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("保存缩略图失败", e);
        }
    }

    @Transactional
    public void deleteImage(String hash) {
        try {
            imageRepository.deleteByHash(hash);
            Files.delete(Paths.get(dataDir + "/images").resolve(hash));
            Files.delete(Paths.get(dataDir + "/thumbnail").resolve(hash));
        } catch (Exception e) {
            throw new RuntimeException("删除图片失败", e);
        }
    }

    @Transactional(readOnly = true)
    public ImageResponse getImage(String hash) {
        try {
            return imageRepository.findByHash(hash).orElseThrow(() -> new RuntimeException("图片不存在")).toResponse();
        } catch (Exception e) {
            throw new RuntimeException("获取图片失败", e);
        }
    }

}
