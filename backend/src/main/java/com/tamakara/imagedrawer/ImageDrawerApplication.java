package com.tamakara.imagedrawer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@SpringBootApplication
@EnableJpaAuditing
public class ImageDrawerApplication {

    public static void main(String[] args) {
        String dataRoot = System.getProperty("app.data-dir", "data");

        Path dbDir = Paths.get(dataRoot, "db");
        try {
            Files.createDirectories(dbDir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create db dir", e);
        }
        SpringApplication.run(ImageDrawerApplication.class, args);
    }
}
