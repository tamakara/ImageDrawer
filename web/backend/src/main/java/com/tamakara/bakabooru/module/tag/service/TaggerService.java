package com.tamakara.bakabooru.module.tag.service;

import com.tamakara.bakabooru.module.system.service.SystemSettingService;
import com.tamakara.bakabooru.module.tag.dto.TaggerRequestDto;
import com.tamakara.bakabooru.module.tag.dto.TaggerResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
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

    @Value("${app.ai-service.url}")
    private String aiServiceUrl;

    public Map<String, List<String>> tagImage(String imagePath) {
        Map<String, Double> categoryThresholds = new HashMap<>();

        TaggerRequestDto body = new TaggerRequestDto();
        body.setImagePath(imagePath);
        body.setThreshold(Double.parseDouble(systemSettingService.getSetting("tag.threshold", "0.61")));
         body.setCategoryThresholds(categoryThresholds);

        TaggerResponseDto response = webClient
                .post()
                .uri(aiServiceUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .onStatus(
                        HttpStatusCode::isError,
                        r -> r.bodyToMono(String.class)
                                .map(msg -> new RuntimeException("AI Service error: " + msg))
                )
                .bodyToMono(TaggerResponseDto.class)
                .block(); // 同步等结果

        if (response != null && response.isSuccess() && response.getData() != null) {
            return response.getData();
        } else {
            String error = response != null ? response.getError() : "Unknown error";
            throw new RuntimeException("标签生成失败: " + error);
        }
    }


}
