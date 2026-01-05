package com.tamakara.imagedrawer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

@SpringBootApplication
@EnableJpaAuditing
public class ImageDrawerApplication {

    public static void main(String[] args) {
        new File("data/db").mkdirs();
        SpringApplication.run(ImageDrawerApplication.class, args);
    }
}
