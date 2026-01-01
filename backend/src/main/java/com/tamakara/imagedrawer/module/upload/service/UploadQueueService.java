package com.tamakara.imagedrawer.module.upload.service;

import com.tamakara.imagedrawer.module.gallery.entity.Image;
import com.tamakara.imagedrawer.module.gallery.repository.ImageRepository;
import com.tamakara.imagedrawer.module.file.service.StorageService;
import com.tamakara.imagedrawer.module.search.entity.Tag;
import com.tamakara.imagedrawer.module.search.repository.TagRepository;
import com.tamakara.imagedrawer.module.system.service.SystemSettingService;
import com.tamakara.imagedrawer.module.tagger.dto.TaggerResponseDto;
import com.tamakara.imagedrawer.module.tagger.service.TaggerService;
import com.tamakara.imagedrawer.module.upload.model.UploadTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class UploadQueueService {

    private final StorageService storageService;
    private final TaggerService taggerService;
    private final ImageRepository imageRepository;
    private final TagRepository tagRepository;
    private final SystemSettingService systemSettingService;

    // In-memory queue storage
    private final Map<String, UploadTask> taskMap = new ConcurrentHashMap<>();

    public UploadTask createTask(MultipartFile file, Long taggerServerId) {
        // 1. Validate File
        validateFile(file);

        String taskId = UUID.randomUUID().toString();
        UploadTask task = new UploadTask();
        task.setId(taskId);
        task.setFilename(file.getOriginalFilename());
        task.setSize(file.getSize());
        task.setStatus(UploadTask.UploadStatus.PENDING);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        task.setTaggerServerId(taggerServerId);

        // Store file temporarily
        String tempFilename = storageService.store(file);
        task.setTempFilePath(tempFilename);

        taskMap.put(taskId, task);

        // Start processing asynchronously
        processTask(taskId);

        return task;
    }

    private void validateFile(MultipartFile file) {
        // Check size
        long maxSize = systemSettingService.getLongSetting("upload.max-file-size", 52428800); // Default 50MB
        if (file.getSize() > maxSize) {
            throw new RuntimeException("File too large. Max size: " + maxSize);
        }

        // Check extension
        String allowedExtensions = systemSettingService.getSetting("upload.allowed-extensions", "jpg,png,webp,gif,jpeg");
        String filename = file.getOriginalFilename();
        if (filename != null) {
            String ext = getExtension(filename).toLowerCase().replace(".", "");
            if (!Arrays.asList(allowedExtensions.split(",")).contains(ext)) {
                throw new RuntimeException("File type not allowed. Allowed: " + allowedExtensions);
            }
        }
    }

    private String getExtension(String filename) {
        if (filename == null) return "";
        int dotIndex = filename.lastIndexOf('.');
        return (dotIndex == -1) ? "" : filename.substring(dotIndex);
    }

    public List<UploadTask> getAllTasks() {
        return new ArrayList<>(taskMap.values());
    }

    public UploadTask getTask(String id) {
        return taskMap.get(id);
    }

    @Async
    public void processTask(String taskId) {
        UploadTask task = taskMap.get(taskId);
        if (task == null) return;

        try {
            updateStatus(task, UploadTask.UploadStatus.PROCESSING);

            // task.getTempFilePath() 现在是 storageService.store() 返回的哈希值
            String hash = task.getTempFilePath();
            Path filePath = storageService.getFilePath(hash);

            // 1. 检查重复
            Optional<Image> existingImage = imageRepository.findByHash(hash);
            if (existingImage.isPresent()) {
                // 文件已按哈希存储，因此我们不需要删除它（它是同一个文件）
                // 但我们可能希望上传失败或仅返回现有图像。
                // 当前逻辑失败。
                task.setErrorMessage("Duplicate image found: " + existingImage.get().getId());
                updateStatus(task, UploadTask.UploadStatus.FAILED);
                return;
            }

            Set<Tag> tags = new HashSet<>();

            // 2. 打标签
            if (task.getTaggerServerId() != null) {
                updateStatus(task, UploadTask.UploadStatus.TAGGING);
                try {
                    Path tagImageFile = filePath;
                    boolean isTemp = false;

                    // 如果需要，调整大小（例如 > 2MB）
                    // TODO: 这个阈值可以配置
                    if (Files.size(filePath) > 2 * 1024 * 1024) {
                        Path tempThumb = storageService.createTempFile("thumb_", ".jpg");
                        Thumbnails.of(filePath.toFile())
                                .size(1024, 1024)
                                .outputFormat("jpg")
                                .toFile(tempThumb.toFile());
                        tagImageFile = tempThumb;
                        isTemp = true;
                    }

                    List<TaggerResponseDto.TaggerTag> taggerTags = taggerService.tagImage(task.getTaggerServerId(), tagImageFile);

                    if (isTemp) {
                        Files.deleteIfExists(tagImageFile);
                    }

                    // 将 TaggerTags 转换为实体
                    for (TaggerResponseDto.TaggerTag tt : taggerTags) {
                        Tag tag = tagRepository.findByName(tt.getName())
                                .orElseGet(() -> {
                                    Tag newTag = new Tag();
                                    newTag.setName(tt.getName());
                                    newTag.setType("general"); // 默认类型
                                    return tagRepository.save(newTag);
                                });
                        tags.add(tag);
                    }
                } catch (Exception e) {
                    log.error("Tagging failed for task " + taskId, e);
                    throw new RuntimeException("Tagging failed: " + e.getMessage());
                }
            }

            // 3. 保存
            updateStatus(task, UploadTask.UploadStatus.SAVING);

            Image image = new Image();

            String originalFilename = task.getFilename();
            String title = originalFilename;
            String extension = "";
            int dotIndex = originalFilename.lastIndexOf('.');
            if (dotIndex != -1) {
                title = originalFilename.substring(0, dotIndex);
                extension = originalFilename.substring(dotIndex + 1);
            }

            image.setTitle(title);
            image.setFileName(originalFilename);
            image.setExtension(extension);

            image.setSize(task.getSize());
            image.setHash(hash);
            image.setTags(tags);
            image.setCreatedAt(LocalDateTime.now());
            image.setUpdatedAt(LocalDateTime.now());

            // 获取尺寸
            try (ImageInputStream in = ImageIO.createImageInputStream(filePath.toFile())) {
                final Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
                if (readers.hasNext()) {
                    ImageReader reader = readers.next();
                    try {
                        reader.setInput(in);
                        image.setWidth(reader.getWidth(0));
                        image.setHeight(reader.getHeight(0));
                    } finally {
                        reader.dispose();
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to read image dimensions for task " + taskId, e);
            }

            imageRepository.save(image);

            updateStatus(task, UploadTask.UploadStatus.COMPLETED);

        } catch (Exception e) {
            log.error("Task failed: " + taskId, e);
            task.setErrorMessage(e.getMessage());
            updateStatus(task, UploadTask.UploadStatus.FAILED);
        }
    }

    private void updateStatus(UploadTask task, UploadTask.UploadStatus status) {
        task.setStatus(status);
        task.setUpdatedAt(LocalDateTime.now());
    }
}
