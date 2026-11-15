package com.autotest.platform.controller;

import com.autotest.platform.common.PageResult;
import com.autotest.platform.common.Result;
import com.autotest.platform.entity.TestScene;
import com.autotest.platform.entity.TestSceneVersion;
import com.autotest.platform.service.SceneService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/scene")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('scene:manage')")
public class SceneController {

    private final SceneService sceneService;

    @GetMapping("/list")
    public Result<PageResult<TestScene>> list(
            @RequestParam Long projectId,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String keyword) {
        return Result.success(PageResult.of(sceneService.page(page, pageSize, projectId, keyword)));
    }

    @PostMapping("/create")
    public Result<TestScene> create(@RequestBody TestScene scene) {
        return Result.success(sceneService.create(scene));
    }

    @PostMapping("/update")
    public Result<TestScene> update(@RequestBody TestScene scene) {
        return Result.success(sceneService.update(scene));
    }

    @PostMapping("/delete")
    public Result<Void> delete(@RequestParam Long id) {
        sceneService.delete(id);
        return Result.success();
    }

    @PostMapping("/status")
    public Result<Void> status(@RequestParam Long id, @RequestParam String status) {
        sceneService.changeStatus(id, status);
        return Result.success();
    }

    @GetMapping("/version/list")
    public Result<List<TestSceneVersion>> versionList(@RequestParam Long sceneId) {
        return Result.success(sceneService.versionList(sceneId));
    }

    @GetMapping("/graph")
    public Result<String> graph(@RequestParam Long sceneId, @RequestParam(required = false) Integer versionNo) {
        return Result.success(sceneService.graph(sceneId, versionNo));
    }

    @PostMapping("/version/save")
    public Result<TestSceneVersion> saveVersion(@RequestBody Map<String, Object> body) {
        Long sceneId = Long.valueOf(String.valueOf(body.get("sceneId")));
        String graphJson = String.valueOf(body.get("graphJson"));
        boolean publish = Boolean.parseBoolean(String.valueOf(body.getOrDefault("publish", "false")));
        return Result.success(sceneService.saveVersion(sceneId, graphJson, publish));
    }
}
