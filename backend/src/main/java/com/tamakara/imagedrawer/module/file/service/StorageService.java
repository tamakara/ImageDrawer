package com.tamakara.imagedrawer.module.file.service;

import jakarta.annotation.PostConstruct;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.stream.Stream;

@Service
public class StorageService {

    @Value("${app.storage.image-dir}")
    private String imageDir;

    @Value("${app.storage.temp-dir}")
    private String tempDir;

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(Paths.get(imageDir));
            Files.createDirectories(Paths.get(tempDir));
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage", e);
        }
    }

    public String store(MultipartFile file) {
        try {
            if (file.isEmpty()) {
                throw new RuntimeException("Failed to store empty file.");
            }
            String hash = calculateHash(file);
            Path destinationFile = Paths.get(imageDir).resolve(hash).normalize().toAbsolutePath();

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
            Path source = Paths.get(imageDir).resolve(hash);
            Path target = Paths.get(tempDir).resolve(hash + "_" + maxSize + "_" + quality + ".jpg");

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
        try (Stream<Path> files = Files.list(Paths.get(tempDir))) {
            files.forEach(file -> {
                try {
                    Files.delete(file);
                } catch (IOException e) {
                    // ignore
                }
            });
        } catch (IOException e) {
            throw new RuntimeException("Failed to clear cache", e);
        }
    }

    public Path getFilePath(String hash) {
        return Paths.get(imageDir).resolve(hash);
    }

    public Path createTempFile(String prefix, String suffix) {
        try {
            return Files.createTempFile(Paths.get(tempDir), prefix, suffix);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create temp file", e);
        }
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
