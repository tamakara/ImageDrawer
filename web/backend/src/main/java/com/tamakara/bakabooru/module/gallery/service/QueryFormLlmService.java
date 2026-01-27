package com.tamakara.bakabooru.module.gallery.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tamakara.bakabooru.module.gallery.dto.ChatRequestDto;
import com.tamakara.bakabooru.module.gallery.dto.ChatResponseDto;
import com.tamakara.bakabooru.module.gallery.dto.SearchRequestDto;
import com.tamakara.bakabooru.module.system.service.SystemSettingService;
import com.tamakara.bakabooru.module.tag.service.TagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.StringJoiner;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueryFormLlmService {

    private final WebClient webClient;
    private final TagService tagService;
    private final SystemSettingService systemSettingService;
    private final ObjectMapper objectMapper;

    // 解析用户输入并生成标准的搜索配置对象
    public SearchRequestDto parseSearchConfig(String userInput) {
        String llmUrl = systemSettingService.getSetting("llm.url", "");
        String llmModel = systemSettingService.getSetting("llm.model", "");
        String llmApiKey = systemSettingService.getSetting("llm.api-key", "");

        if (!StringUtils.hasText(llmUrl) || !StringUtils.hasText(llmModel) || !StringUtils.hasText(llmApiKey)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "未完成LLM配置，请在设置中配置 LLM");
        }

        // 1. 解析查询条件
        SearchRequestDto searchRequestDto = parseQuery(userInput, llmUrl, llmModel, llmApiKey);

        // 2. 标签同义词扩展与过滤
        if (StringUtils.hasText(searchRequestDto.getTagSearch())) {
            String optimizedTags = expandAndFilterTags(searchRequestDto.getTagSearch(), llmUrl, llmModel, llmApiKey);
            searchRequestDto.setTagSearch(optimizedTags);
        }

        return searchRequestDto;
    }

    // 调用 LLM 将自然语言转换为 SearchRequestDto
    private SearchRequestDto parseQuery(String userInput, String llmUrl, String llmModel, String llmApiKey) {
        String systemPrompt = """
                你是一个专业的查询解析助手。你的任务是将用户的自然语言查询转换为以下JSON格式的结构化查询对象。
                
                目标 JSON 结构：
                {
                   "tagSearch": "string | null", // 提取图片视觉特征标签（如: 'blue_eyes -red_hair'），空格分隔，排除项加前缀'-'。去除冗余词，保留最精准的标签。
                   "keyword": "string | null",   // 仅当明确指定要匹配'标题'或'文件名'时填写。
                   "randomSeed": "string | null",// 若用户明确请求随机结果，则生成随机字符串种子。
                   "widthMin": number, "widthMax": number,    // 宽度范围
                   "heightMin": number, "heightMax": number,  // 高度范围
                   "sizeMin": number, "sizeMax": number,      // 文件大小范围（字节）
                   "page": number, "size": number,            // 分页参数
                   "sort": "string | null"       // 格式: "字段,方向"。字段: RANDOM, viewCount, createdAt, updatedAt, size, title。方向: ASC, DESC。
                }
                
                要求：
                1. 理解用户意图，准确提取数值范围、标签和排序要求。
                2. tagSearch中每个词应为独立的视觉标签，同义词只需保留一个最准确的。
                3. 仅输出纯JSON文本，严禁包含Markdown代码块或换行符。
                """;

        return callLlmApi(llmUrl, llmModel, llmApiKey, systemPrompt, "用户输入: " + userInput, new TypeReference<>() {
        });
    }

    // 调用 LLM 获取标签同义词，并根据数据库存在的标签进行过滤
    private String expandAndFilterTags(String tagSearch, String llmUrl, String llmModel, String llmApiKey) {
        String systemPrompt = """
                你是一个标签同义词扩展专家。请为输入的每个关键词（空格分隔）生成对应的 Danbooru 风格标签同义词列表。
                
                输入示例：关键词1 -关键词2
                输出格式（JSON二维数组）：
                [
                    ["同义词1A", "同义词1B", ...],    // 对应 关键词1
                    ["-同义词2A", "-同义词2B", ...]   // 对应 -关键词2 (必须保留排除前缀'-')
                ]
                
                要求：
                1. 能够识别多语言关键词并转换为标准的 Danbooru 风格标签（通常为英文）。
                2. 数组顺序必须严格对应输入的关键词顺序。
                3. 仅输出纯JSON文本，严禁包含Markdown代码块或换行符。
                """;

        try {
            List<List<String>> tagGroups = callLlmApi(llmUrl, llmModel, llmApiKey, systemPrompt, "用户输入: " + tagSearch, new TypeReference<>() {
            });

            if (tagGroups == null || tagGroups.isEmpty()) {
                return tagSearch;
            }

            log.debug("Original tags: [{}], Expanded groups: {}", tagSearch, tagGroups);

            StringJoiner validTagSearch = new StringJoiner(" ");
            for (List<String> synonyms : tagGroups) {
                for (String tag : synonyms) {
                    // 检查标签是否存在 (去掉可能的排除前缀 '-')
                    boolean isExcluded = tag.startsWith("-");
                    String cleanTag = isExcluded ? tag.substring(1) : tag;

                    if (tagService.existsTag(cleanTag)) {
                        validTagSearch.add(tag); // 添加完整标签（含前缀）
                        break; // 每个关键词只取第一个匹配到的有效标签
                    }
                }
            }
            return validTagSearch.toString();

        } catch (Exception e) {
            log.error("标签扩展失败，使用原始标签: {}", tagSearch, e);
            return tagSearch;
        }
    }

    // 通用 LLM 调用封装
    private <T> T callLlmApi(String url, String model, String apiKey, String systemPrompt, String userPrompt, TypeReference<T> responseType) {
        try {
            ChatRequestDto request = new ChatRequestDto(systemPrompt, userPrompt, model);

            ChatResponseDto response = webClient.post()
                    .uri(url)
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(ChatResponseDto.class)
                    .block();

            if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
                throw new RuntimeException("LLM 响应为空");
            }

            String content = response.getChoices().get(0).getMessage().getContent();
            return objectMapper.readValue(content, responseType);

        } catch (Exception e) {
            log.error("LLM 调用异常: {}", e.getMessage());
            throw new RuntimeException("调用模型失败: " + e.getMessage(), e);
        }
    }
}
