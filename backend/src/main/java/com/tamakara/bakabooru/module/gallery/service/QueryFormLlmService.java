package com.tamakara.bakabooru.module.gallery.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tamakara.bakabooru.module.gallery.dto.ChatRequestDto;
import com.tamakara.bakabooru.module.gallery.dto.ChatResponseDto;
import com.tamakara.bakabooru.module.gallery.dto.SearchRequestDto;
import com.tamakara.bakabooru.module.system.service.SystemSettingService;
import com.tamakara.bakabooru.module.tag.dto.TagDto;
import com.tamakara.bakabooru.module.tag.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QueryFormLlmService {

    private final WebClient webClient;
    private final TagService tagService;
    private final SystemSettingService systemSettingService;
    private final ObjectMapper objectMapper;

    public SearchRequestDto parseQuery(String userInput) {
        String llmUrl = systemSettingService.getSetting("llm.url", "");
        String llmModel = systemSettingService.getSetting("llm.model", "");
        String llmApiKey = systemSettingService.getSetting("llm.api-key", "");

        if ("".equals(llmUrl) || "".equals(llmModel) || "".equals(llmApiKey))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "未完成LLM配置，请在设置中配置 LLM");

        List<String> availableTags = tagService.listTags().stream().map(TagDto::getName).toList();

        ChatRequestDto request = getChatRequestDto(userInput, availableTags, llmModel);
        try {
            ChatResponseDto response = webClient.post()
                    .uri(llmUrl)
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + llmApiKey)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(ChatResponseDto.class)
                    .block();

            String llmOutput = response.getChoices().get(0).getMessage().getContent();
            return objectMapper.readValue(extractJson(llmOutput), SearchRequestDto.class);
        } catch (Exception e) {
            throw new RuntimeException("调用模型失败", e);
        }
    }

    private String extractJson(String content) {
        content = content.trim();
        if (content.startsWith("```json")) {
            content = content.substring(7);
        } else if (content.startsWith("```")) {
            content = content.substring(3);
        }
        if (content.endsWith("```")) {
            content = content.substring(0, content.length() - 3);
        }
        return content.trim();
    }

    private ChatRequestDto getChatRequestDto(String userInput, List<String> availableTags, String llmModel) {
        String systemPrompt = """
                    你是一个查询条件解析器。
                    你的任务：把用户自然语言查询转成严格的 SearchRequestDto JSON 对象。
                
                    SearchRequestDto 结构说明:
                    - tagSearch: 标签搜索字符串。多个标签用空格分隔，排除标签用前缀 '-'。例如 "blue_eyes -red_hair"。
                    - keyword: 标题或文件名关键字。
                    - randomSeed: 随机种子字符串，如果用户想要随机结果时生成一个。
                    - widthMin/Max, heightMin/Max: 尺寸范围。
                    - sizeMin/Max: 文件大小范围（字节）。
                
                    只输出 JSON，不要解释，不要使用 Markdown 代码块。
                """;

        String schemaPrompt = String.format("""
                JSON schema:
                {
                  "tagSearch": string | null,
                  "keyword": string | null,
                  "randomSeed": string | null,
                  "widthMin": number | null,
                  "widthMax": number | null,
                  "heightMin": number | null,
                  "heightMax": number | null,
                  "sizeMin": number | null,
                  "sizeMax": number | null
                }
                允许使用的标签全集: %s
                重要规则：
                1. tagSearch 字段中的标签 **必须严格** 来自上述列表。
                2. 如果用户描述了画面内容，请在列表中寻找对应标签。
                3. 如果列表中不存在对应的标签，绝对不要自己编造标签，请忽略该描述或作为 keyword 处理。
                """, availableTags != null && !availableTags.isEmpty() ? availableTags : "(暂无)");

        String userPrompt = schemaPrompt + "\n用户输入: " + userInput;

        ChatRequestDto request = new ChatRequestDto();
        request.setModel(llmModel);
        request.setMessages(List.of(
                new ChatRequestDto.Message("system", systemPrompt),
                new ChatRequestDto.Message("user", userPrompt)
        ));
        return request;
    }
}
