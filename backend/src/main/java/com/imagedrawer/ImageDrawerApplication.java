package com.imagedrawer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.StandardEnvironment;

import java.io.File;

@SpringBootApplication
public class ImageDrawerApplication {

    public static void main(String[] args) {
        initializeDataDirs();
        SpringApplication.run(ImageDrawerApplication.class, args);
    }

    private static void initializeDataDirs() {
        String dataDir = "./data";

        new File(dataDir).mkdirs();
        new File(dataDir + "/images").mkdirs();
        new File(dataDir + "/thumbnail").mkdirs();
        new File(dataDir + "/db").mkdirs();
    }
}
