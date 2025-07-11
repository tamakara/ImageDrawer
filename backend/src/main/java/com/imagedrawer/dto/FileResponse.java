package com.imagedrawer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.core.io.Resource;

@Data
@AllArgsConstructor
public class FileResponse {
    private String filename;
    private String mimeType;
    private Resource resource;
}
