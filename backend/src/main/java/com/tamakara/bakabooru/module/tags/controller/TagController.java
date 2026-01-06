package com.tamakara.bakabooru.module.tags.controller;

import com.tamakara.bakabooru.module.tags.dto.TagDto;
import com.tamakara.bakabooru.module.tags.service.TagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
@Tag(name = "标签", description = "标签管理")
public class TagController {

    private final TagService tagService;

    @GetMapping
    @Operation(summary = "获取标签列表", description = "获取所有标签或按查询搜索")
    public List<TagDto> listTags(@RequestParam(required = false) String query) {
        if (query != null && !query.isEmpty()) {
            return tagService.searchTags(query);
        }
        return tagService.listTags();
    }

    @PostMapping
    @Operation(summary = "创建标签")
    public TagDto createTag(@RequestBody TagDto tagDto) {
        return tagService.createTag(tagDto);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除标签")
    public void deleteTag(@PathVariable Long id) {
        tagService.deleteTag(id);
    }
}

