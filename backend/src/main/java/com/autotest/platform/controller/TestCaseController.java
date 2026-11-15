package com.autotest.platform.controller;

import com.autotest.platform.common.PageResult;
import com.autotest.platform.common.Result;
import com.autotest.platform.entity.TestCase;
import com.autotest.platform.entity.TestCaseVersion;
import com.autotest.platform.service.TestCaseService;
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
@RequestMapping("/api/case")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('api:manage')")
public class TestCaseController {

    private final TestCaseService testCaseService;

    @GetMapping("/list")
    public Result<PageResult<TestCase>> list(
            @RequestParam Long projectId,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String caseType,
            @RequestParam(required = false) String keyword) {
        return Result.success(PageResult.of(testCaseService.page(page, pageSize, projectId, caseType, keyword)));
    }

    @PostMapping("/create")
    public Result<TestCase> create(@RequestBody TestCase c) {
        return Result.success(testCaseService.create(c, c.getDefinitionJson()));
    }

    @PostMapping("/update")
    public Result<TestCase> update(@RequestBody TestCase c) {
        return Result.success(testCaseService.update(c));
    }

    @PostMapping("/delete")
    public Result<Void> delete(@RequestParam Long id) {
        testCaseService.delete(id);
        return Result.success();
    }

    @PostMapping("/status")
    public Result<Void> status(@RequestParam Long id, @RequestParam String status) {
        testCaseService.changeStatus(id, status);
        return Result.success();
    }

    @GetMapping("/version/list")
    public Result<List<TestCaseVersion>> versionList(@RequestParam Long testCaseId) {
        return Result.success(testCaseService.versionList(testCaseId));
    }

    @PostMapping("/version/save")
    public Result<TestCaseVersion> saveVersion(@RequestBody Map<String, Object> body) {
        Long testCaseId = Long.valueOf(String.valueOf(body.get("testCaseId")));
        String definitionJson = String.valueOf(body.get("definitionJson"));
        boolean publish = Boolean.parseBoolean(String.valueOf(body.getOrDefault("publish", "false")));
        return Result.success(testCaseService.saveVersion(testCaseId, definitionJson, publish));
    }
}
