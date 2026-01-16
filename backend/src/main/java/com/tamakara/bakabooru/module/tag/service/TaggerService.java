package com.tamakara.bakabooru.module.tag.service;

import com.tamakara.bakabooru.module.system.service.SystemSettingService;
import com.tamakara.bakabooru.module.tag.dto.TaggerRequestDto;
import com.tamakara.bakabooru.module.tag.dto.TaggerResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TaggerService {

    private final WebClient webClient;
    private final SystemSettingService systemSettingService;

    @Value("${app.tagger.url}")
    private String taggerUrl;

    private void addSettingIfPresent(Map<String, Double> categoryThresholds, String category, String settingKey) {
        String value = systemSettingService.getSetting(settingKey, "");
        if (StringUtils.hasText(value)) {
            categoryThresholds.put(category, Double.parseDouble(value));
        }
    }

    public Map<String, List<String>> tagImage(String image_hash) {
        Map<String, Double> categoryThresholds = new HashMap<>();

        addSettingIfPresent(categoryThresholds, "artist", "tagger.category_thresholds.artist");
        addSettingIfPresent(categoryThresholds, "character", "tagger.category_thresholds.character");
        addSettingIfPresent(categoryThresholds, "copyright", "tagger.category_thresholds.copyright");
        addSettingIfPresent(categoryThresholds, "general", "tagger.category_thresholds.general");
        addSettingIfPresent(categoryThresholds, "meta", "tagger.category_thresholds.meta");
        addSettingIfPresent(categoryThresholds, "rating", "tagger.category_thresholds.rating");

        TaggerRequestDto body = new TaggerRequestDto();
        body.setImageHash(image_hash);
        body.setThreshold(Double.parseDouble(systemSettingService.getSetting("tagger.threshold", "0.6")));
        body.setMinConfidence(Double.parseDouble(systemSettingService.getSetting("tagger.minConfidence", "0.1")));
        body.setCategoryThresholds(categoryThresholds);

        TaggerResponseDto response = webClient
                .post()
                .uri(taggerUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .onStatus(
                        HttpStatusCode::isError,
                        r -> r.bodyToMono(String.class)
                                .map(msg -> new RuntimeException("Tagger error: " + msg))
                )
                .bodyToMono(TaggerResponseDto.class)
                .block(); // 同步等结果

        if (response != null && response.isSuccess() && response.getData() != null) {
            return response.getData();
        } else {
            String error = response != null ? response.getError() : "Unknown error";
            throw new RuntimeException("Tagging failed: " + error);
        }
    }


}
