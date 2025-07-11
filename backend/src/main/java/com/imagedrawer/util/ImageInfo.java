package com.imagedrawer.util;

import lombok.Data;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.tika.Tika;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;

@Data
public class ImageInfo {
    private String filename;
    private Long size;
    private String hash;
    private String mimetype;
    private Integer width;
    private Integer height;

    public ImageInfo(MultipartFile file) throws IOException {
        filename = file.getOriginalFilename();
        size = file.getSize();
        hash = DigestUtils.sha256Hex(file.getInputStream());

        Tika tika = new Tika();
        mimetype = tika.detect(file.getInputStream());

        BufferedImage bufferedImage = ImageIO.read(file.getInputStream());
        width = bufferedImage.getWidth();
        height = bufferedImage.getHeight();
    }
}
