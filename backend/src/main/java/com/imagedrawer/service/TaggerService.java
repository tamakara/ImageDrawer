package com.imagedrawer.service;

import com.imagedrawer.dto.TaggerResponse;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class TaggerService {
    private final WebClient.Builder builder;

    private WebClient webClient;

    public void setTaggerUrl(String taggerUrl) {
        this.webClient = builder.baseUrl(taggerUrl).build();
    }

    public TaggerResponse tagger(MultipartFile image) {
        if (builder == null) {
            throw new IllegalStateException("Tagger URL not configured.");
        }

        Resource resource = image.getResource();

        return webClient.post()
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData("image", resource))
                .retrieve()
                .bodyToMono(TaggerResponse.class)
                .block();
    }
}
