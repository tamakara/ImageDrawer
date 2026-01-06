package com.tamakara.bakabooru.module.file.service;

import com.tamakara.bakabooru.config.AppPaths;
import lombok.RequiredArgsConstructor;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class StorageService {

    private final AppPaths appPaths;

    public String store(MultipartFile file) {
        try {
            if (file.isEmpty()) {
                throw new RuntimeException("Failed to store empty file.");
            }
            String hash = calculateHash(file);
            Path destinationFile = appPaths.getImageDir().resolve(hash).normalize().toAbsolutePath();

            if (!Files.exists(destinationFile)) {
                try (InputStream inputStream = file.getInputStream()) {
                    Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
                }
            }
            return hash;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file.", e);
        }
    }

    public Path getThumbnailPath(String hash, int quality, int maxSize) {
        try {
            Path source = appPaths.getImageDir().resolve(hash);
            Path target = appPaths.getThumbnailDir().resolve(hash + "_" + maxSize + "_" + quality + ".jpg");

            if (Files.exists(target)) {
                return target;
            }

            Thumbnails.of(source.toFile())
                    .size(maxSize, maxSize)
                    .outputQuality(quality / 100.0)
                    .outputFormat("jpg")
                    .toFile(target.toFile());

            return target;
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate thumbnail", e);
        }
    }

    public void clearCache() {
        Path dir = appPaths.getTempDir();
        if (!Files.exists(dir)) return;

        try (Stream<Path> stream = Files.walk(dir)) {
            stream.filter(Files::isRegularFile)
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                        }
                    });
        } catch (IOException e) {
            throw new RuntimeException("Failed to clear files", e);
        }
    }

    public Path getFilePath(String hash) {
        return appPaths.getImageDir().resolve(hash);
    }

    public String calculateHash(MultipartFile file) {
        try {
            // TODO: 哈希算法可配置
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(file.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new RuntimeException("Failed to calculate hash", e);
        }
    }
}
