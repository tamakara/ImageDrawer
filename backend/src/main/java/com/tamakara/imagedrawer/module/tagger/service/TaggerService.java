package com.tamakara.imagedrawer.module.tagger.service;

import com.tamakara.imagedrawer.module.tagger.dto.TaggerResponseDto;
import com.tamakara.imagedrawer.module.tagger.entity.TaggerServerConfig;
import com.tamakara.imagedrawer.module.tagger.repository.TaggerServerConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TaggerService {

    private final TaggerServerConfigRepository taggerServerConfigRepository;
    private final RestClient.Builder restClientBuilder;

    public List<TaggerServerConfig> listServers() {
        return taggerServerConfigRepository.findAll();
    }

    public TaggerServerConfig addServer(TaggerServerConfig config) {
        return taggerServerConfigRepository.save(config);
    }

    public void deleteServer(Long id) {
        taggerServerConfigRepository.deleteById(id);
    }

    public List<TaggerResponseDto.TaggerTag> tagImage(Long serverId, Path imagePath) {
        TaggerServerConfig server = taggerServerConfigRepository.findById(serverId)
                .orElseThrow(() -> new RuntimeException("Tagger server not found"));

        if (!server.isActive()) {
            throw new RuntimeException("Tagger server is not active");
        }

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new FileSystemResource(imagePath));

        TaggerResponseDto response = restClientBuilder.build()
                .post()
                .uri(server.getUrl())
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .body(TaggerResponseDto.class);

        if (response != null && response.isSuccess() && response.getData() != null) {
            return response.getData().getTags();
        } else {
            String error = response != null ? response.getError() : "Unknown error";
            throw new RuntimeException("Tagging failed: " + error);
        }
    }
}
