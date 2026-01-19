package com.tamakara.bakabooru.module.gallery.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tamakara.bakabooru.module.gallery.dto.ChatRequestDto;
import com.tamakara.bakabooru.module.gallery.dto.ChatResponseDto;
import com.tamakara.bakabooru.module.gallery.dto.SearchRequestDto;
import com.tamakara.bakabooru.module.system.service.SystemSettingService;
import com.tamakara.bakabooru.module.tag.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.StringJoiner;

@Service
@RequiredArgsConstructor
public class QueryFormLlmService {

    private final WebClient webClient;
    private final TagService tagService;
    private final SystemSettingService systemSettingService;
    private final ObjectMapper objectMapper;

    public SearchRequestDto parseSearchConfig(String userInput) {
        String llmUrl = systemSettingService.getSetting("llm.url", "");
        String llmModel = systemSettingService.getSetting("llm.model", "");
        String llmApiKey = systemSettingService.getSetting("llm.api-key", "");

        if ("".equals(llmUrl) || "".equals(llmModel) || "".equals(llmApiKey))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "未完成LLM配置，请在设置中配置 LLM");

        SearchRequestDto searchRequestDto = parseQuery(userInput, llmUrl, llmModel, llmApiKey);
        String tagSearch = filterTags(searchRequestDto.getTagSearch(), llmUrl, llmModel, llmApiKey);
        searchRequestDto.setTagSearch(tagSearch);
        return searchRequestDto;

    }

    public SearchRequestDto parseQuery(String userInput, String llmUrl, String llmModel, String llmApiKey) {
        String systemPrompt = """
                    你是一个查询条件解析器。
                    你的任务：把用户输入的自然语言查询条件转成特定格式的 JSON 对象。
                    生成 JSON 格式：
                     {
                        "tagSearch": string | null,
                         "keyword": string | null,
                        "randomSeed": string | null,
                        "widthMin": number | null,
                        "widthMax": number | null,
                        "heightMin": number | null,
                        "heightMax": number | null,
                        "sizeMin": number | null,
                        "sizeMax": number | null,
                        "page": number | null,
                        "size": number | null,
                        "sort": string | null
                     }
                    说明：
                    - tagSearch: 标签搜索字符串。多个标签用空格分隔，排除标签用前缀 '-'。例如 "blue_eyes -red_hair"。你需要从输入文本中提取所有描述图片特征的关键词提取出来，然后为每个关键词生成尽可能多的意思完全相同的标签供我与数据库进行匹配，不能生成与关键词不同意或者近义的标签，标签格式参考danbooru的标签系统。
                    - keyword: 标题或文件名关键字。只有当输入特别说明是标题或者文件名时才添加到这里。
                    - randomSeed: 随机种子字符串，如果用户想要随机结果时生成一个。
                    - widthMin/Max, heightMin/Max: 尺寸范围。
                    - sizeMin/Max: 文件大小范围（字节）。
                    - page: 分页查询页码。
                    - size: 分页查询每页大小。
                    - sort: 查询结果排序方式。结构"排序方式,排序方向"，排序方式只能是：RANDOM（随机）、viewCount（查看次数）、createdAt（创建日期）、updatedAt（修改日期）、size（文件大小）、title（标题、文件名），排序发现1只能是：ASC（升序）、DESC（降序）。
                    要求：
                    确保生成的JSON数据结构清晰、易于解析和使用。生成的JSON结构为JSON纯文本，不要输出代码块，JSON当中没有换行符。
                """;

        String userPrompt = "\n用户输入: " + userInput;

        try {
            ChatRequestDto request = new ChatRequestDto(systemPrompt, userPrompt, llmModel);

            ChatResponseDto response = webClient.post()
                    .uri(llmUrl)
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + llmApiKey)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(ChatResponseDto.class)
                    .block();

            String llmOutput = response.getChoices().get(0).getMessage().getContent();
            SearchRequestDto searchRequestDto = objectMapper.readValue(extractJson(llmOutput), SearchRequestDto.class);
            return searchRequestDto;

        } catch (Exception e) {
            throw new RuntimeException("调用模型失败", e);
        }
    }

    private String filterTags(String tagSearch, String llmUrl, String llmModel, String llmApiKey) {
        String systemPrompt = """
                    为每个该字符串内每个关键词生成一个标签数组，数组内是所有与该关键词同义的标签，标签风格可以是日语罗马字，也可以是全英语，可以参考Danbooru的标签风格。如果关键词前有-符号，则生成的每个标签前都要添加-符号。
                    例如对于输入关键词：关键词1 -关键词2
                    输出JSON：
                    [
                        ["同义词1","同义词2"...],
                        ["-同义词1","-同义词2"...],
                        ...
                    ]
                    要求：
                    确保生成的JSON数据结构清晰、易于解析和使用。生成的JSON结构为JSON纯文本，不要输出代码块，JSON当中没有换行符。
                """;

        String userPrompt = "\n用户输入: " + tagSearch;

        try {
            ChatRequestDto request = new ChatRequestDto(systemPrompt, userPrompt, llmModel);

            ChatResponseDto response = webClient.post()
                    .uri(llmUrl)
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + llmApiKey)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(ChatResponseDto.class)
                    .block();

            String llmOutput = response.getChoices().get(0).getMessage().getContent();
            List<List<String>> tagss = objectMapper.readValue(
                    extractJson(llmOutput),
                    new TypeReference<List<List<String>>>() {
                    }
            );

            // 筛选可用标签
            StringJoiner validTagSearch = new StringJoiner(" ");
            for (List<String> tags : tagss) {
                for (String tag : tags) {
                    if (tagService.existsTag(tag.startsWith("-") ? tag.substring(1) : tag)) {
                        validTagSearch.add(tag);
                        break;
                    }
                }
            }

            return validTagSearch.toString();

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
}
