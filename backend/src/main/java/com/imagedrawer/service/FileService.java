package com.imagedrawer.service;

import com.imagedrawer.dto.FileResponse;
import com.imagedrawer.entity.Image;
import com.imagedrawer.repository.ImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
public class FileService {
    private final ImageRepository imageRepository;

    @Value("${image-drawer.data.dir}")
    private String dataDir;

    @Transactional(readOnly = true)
    public FileResponse getImageFile(String hash) {

        Path filePath = Paths.get(dataDir, "images").resolve(hash);
        if (!Files.exists(filePath)) return null;

        Image image = imageRepository.findByHash(hash)
                .orElseThrow(() -> new RuntimeException("图片不存在"));
        String mimetype = image.getMimetype();

        String originalFilename = image.getFilename();
        int dotIndex = originalFilename.lastIndexOf('.');

        String extension = getExtension(mimetype);
        String filename =
                (dotIndex == -1) ? originalFilename : originalFilename.substring(0, dotIndex)
                + "_"
                + hash
                + (extension != null ? extension : "");
        Resource resource = new FileSystemResource(filePath);

        return new FileResponse(filename, mimetype, resource);
    }

    @Transactional(readOnly = true)
    public byte[] getImageFileZip(List<String> hashes) {
        if (hashes == null || hashes.isEmpty()) {
            return new byte[0];
        }

        try (
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                ZipOutputStream zipOut = new ZipOutputStream(byteArrayOutputStream)
        ) {
            for (String hash : hashes) {
                Path imagePath = Paths.get(dataDir, "images").resolve(hash);

                if (Files.exists(imagePath)) {
                    Image image = imageRepository.findByHash(hash)
                            .orElseThrow(() -> new RuntimeException("图片不存在"));
                    String mimetype = image.getMimetype();
                    String extension = getExtension(mimetype);

                    String filenameInZip = extension != null
                            ? hash + extension
                            : hash;

                    zipOut.putNextEntry(new ZipEntry(filenameInZip));
                    Files.copy(imagePath, zipOut);
                    zipOut.closeEntry();
                }
            }
            zipOut.finish();
            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("生成 ZIP 文件失败", e);
        }
    }

    private String getExtension(String mimeType) {
        Map<String, String> mimeMap = new HashMap<>();
        mimeMap.put("image/jpeg", ".jpg");
        mimeMap.put("image/png", ".png");
        mimeMap.put("image/gif", ".gif");
        mimeMap.put("image/webp", ".webp");
        mimeMap.put("image/bmp", ".bmp");
        mimeMap.put("image/svg+xml", ".svg");

        return mimeMap.getOrDefault(mimeType, null);
    }

}
