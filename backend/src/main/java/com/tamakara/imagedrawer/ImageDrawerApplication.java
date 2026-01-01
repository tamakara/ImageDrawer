package com.tamakara.imagedrawer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.io.File;

@SpringBootApplication
@EnableJpaAuditing
public class ImageDrawerApplication {

    public static void main(String[] args) {
        new File("data/db").mkdirs();
        new File("data/images").mkdirs();
        new File("data/temp").mkdirs();

        SpringApplication.run(ImageDrawerApplication.class, args);
    }
}
