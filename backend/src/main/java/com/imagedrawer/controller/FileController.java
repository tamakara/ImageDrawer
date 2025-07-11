package com.imagedrawer.controller;

import com.imagedrawer.dto.FileResponse;
import com.imagedrawer.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/file")
@RequiredArgsConstructor
public class FileController {
    private final FileService fileService;

    @GetMapping
    public ResponseEntity<Resource> getImageFile(@RequestParam String hash) {
        FileResponse response = fileService.getImageFile(hash);

        if (response == null) return ResponseEntity.notFound().build();

        String filename = response.getFilename();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(response.getMimeType()));
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
        headers.set("X-Filename", filename);
        headers.set("Access-Control-Expose-Headers", "X-Filename");

        return ResponseEntity.ok()
                .headers(headers)
                .body(response.getResource());
    }

    @PostMapping("/zip")
    public ResponseEntity<byte[]> getImageFileZip(@RequestBody List<String> hashes) {
        byte[] zipBytes = fileService.getImageFileZip(hashes);

        String timeStr = new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date());
        String filename = timeStr + ".zip";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDisposition(
                ContentDisposition.attachment().filename(filename).build()
        );
        headers.set("X-Filename", filename);
        headers.set("Access-Control-Expose-Headers", "X-Filename");

        return ResponseEntity.ok().headers(headers).body(zipBytes);
    }

}
