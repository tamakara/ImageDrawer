package com.tamakara.imagedrawer.module.system.service;

import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
public class BackupService {

    @Value("${app.storage.image-dir}")
    private String imageDir;

    @Value("${app.storage.temp-dir}")
    private String tempDir;

    @Value("${spring.datasource.url}")
    private String dbUrl;

    public File createBackup() throws IOException {
        // 从 URL 解析数据库路径 (jdbc:sqlite:data/db/data.sqlite)
        String dbPath = dbUrl.replace("jdbc:sqlite:", "");

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        File backupFile = new File(tempDir, "backup_" + timestamp + ".zip");

        try (FileOutputStream fos = new FileOutputStream(backupFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            // 备份数据库
            File dbFile = new File(dbPath);
            if (dbFile.exists()) {
                addToZip(dbFile, "db/" + dbFile.getName(), zos);
            }

            // 备份图片
            File images = new File(imageDir);
            if (images.exists() && images.isDirectory()) {
                addDirectoryToZip(images, "images", zos);
            }
        }

        return backupFile;
    }

    public void restoreBackup(MultipartFile file) throws IOException {
        // 警告：此操作很危险。它会覆盖数据。
        // 在实际应用中，我们应该先停止数据库连接。
        // 如果应用程序正在运行，SQLite 文件锁定可能会阻止覆盖。
        // 目前，我们假设它有效，或者我们可能需要重启。

        // 解压到临时目录
        Path tempExtractDir = Paths.get(tempDir, "restore_" + System.currentTimeMillis());
        Files.createDirectories(tempExtractDir);

        try (ZipInputStream zis = new ZipInputStream(file.getInputStream())) {
            ZipEntry zipEntry = zis.getNextEntry();
            while (zipEntry != null) {
                File newFile = newFile(tempExtractDir.toFile(), zipEntry);
                if (zipEntry.isDirectory()) {
                    if (!newFile.isDirectory() && !newFile.mkdirs()) {
                        throw new IOException("Failed to create directory " + newFile);
                    }
                } else {
                    // 修复 Windows 创建的归档文件
                    File parent = newFile.getParentFile();
                    if (!parent.isDirectory() && !parent.mkdirs()) {
                        throw new IOException("Failed to create directory " + parent);
                    }

                    try (FileOutputStream fos = new FileOutputStream(newFile)) {
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
                zipEntry = zis.getNextEntry();
            }
        }

        // 将文件移动到实际位置
        // 1. 恢复数据库
        String dbPath = dbUrl.replace("jdbc:sqlite:", "");
        File dbFile = new File(dbPath);
        File restoredDb = new File(tempExtractDir.toFile(), "db/" + dbFile.getName());
        if (restoredDb.exists()) {
            // 关闭数据库连接？SQLite 可能会被锁定。
            // 在运行的 Spring Boot 应用程序中，这很棘手。
            // 我们可能需要依赖操作系统文件替换或重启。
            // 目前，让我们尝试复制。
            FileUtils.copyFile(restoredDb, dbFile);
        }

        // 2. 恢复图片
        File imagesDir = new File(imageDir);
        File restoredImages = new File(tempExtractDir.toFile(), "images");
        if (restoredImages.exists()) {
            FileUtils.copyDirectory(restoredImages, imagesDir);
        }

        // 清理
        FileUtils.deleteDirectory(tempExtractDir.toFile());
    }

    private void addToZip(File file, String fileName, ZipOutputStream zos) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            ZipEntry zipEntry = new ZipEntry(fileName);
            zos.putNextEntry(zipEntry);

            byte[] bytes = new byte[1024];
            int length;
            while ((length = fis.read(bytes)) >= 0) {
                zos.write(bytes, 0, length);
            }
            zos.closeEntry();
        }
    }

    private void addDirectoryToZip(File folder, String parentFolder, ZipOutputStream zos) throws IOException {
        File[] files = folder.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isDirectory()) {
                addDirectoryToZip(file, parentFolder + "/" + file.getName(), zos);
                continue;
            }
            addToZip(file, parentFolder + "/" + file.getName(), zos);
        }
    }

    private File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir, zipEntry.getName());

        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }

        return destFile;
    }
}

