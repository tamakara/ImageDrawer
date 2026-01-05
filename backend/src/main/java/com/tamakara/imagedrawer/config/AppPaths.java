package com.tamakara.imagedrawer.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
@Getter
public class AppPaths {

    @Value("${app.data-dir}")
    private String dataRoot;

    private Path dataDir;
    private Path imageDir;
    private Path tempDir;
    private Path thumbnailDir;
    private Path dbDir;

    @PostConstruct
    public void init() {
        this.dataDir = Paths.get(dataRoot).toAbsolutePath().normalize();
        this.imageDir = this.dataDir.resolve("image");
        this.tempDir = this.dataDir.resolve("temp");
        this.thumbnailDir = this.tempDir.resolve("thumbnail");
        this.dbDir = this.dataDir.resolve("db");

        try {
            Files.createDirectories(this.dataDir);
            Files.createDirectories(this.imageDir);
            Files.createDirectories(this.tempDir);
            Files.createDirectories(this.thumbnailDir);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize data directories", e);
        }
    }
}

