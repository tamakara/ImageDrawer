package com.tamakara.imagedrawer.module.tagger.controller;

import com.tamakara.imagedrawer.module.tagger.entity.TaggerServerConfig;
import com.tamakara.imagedrawer.module.tagger.service.TaggerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tagger/servers")
@RequiredArgsConstructor
@Tag(name = "标签生成器", description = "标签生成服务器管理")
public class TaggerController {

    private final TaggerService taggerService;

    @GetMapping
    @Operation(summary = "获取标签生成服务器列表")
    public List<TaggerServerConfig> listServers() {
        return taggerService.listServers();
    }

    @PostMapping
    @Operation(summary = "添加标签生成服务器")
    public TaggerServerConfig addServer(@RequestBody TaggerServerConfig config) {
        return taggerService.addServer(config);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除标签生成服务器")
    public void deleteServer(@PathVariable Long id) {
        taggerService.deleteServer(id);
    }
}

