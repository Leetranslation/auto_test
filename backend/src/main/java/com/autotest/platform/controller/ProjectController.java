package com.autotest.platform.controller;

import com.autotest.platform.common.PageResult;
import com.autotest.platform.common.Result;
import com.autotest.platform.entity.Project;
import com.autotest.platform.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/project")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('project:manage')")
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping("/list")
    public Result<PageResult<Project>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        return Result.success(PageResult.of(projectService.page(page, pageSize, keyword, status)));
    }

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('project:create')")
    public Result<Project> create(@RequestBody Project project) {
        return Result.success(projectService.create(project));
    }

    @PostMapping("/update")
    @PreAuthorize("hasAuthority('project:update')")
    public Result<Project> update(@RequestBody Project project) {
        return Result.success(projectService.update(project));
    }

    @PostMapping("/status")
    public Result<Void> status(@RequestParam Long id, @RequestParam Integer status) {
        projectService.changeStatus(id, status);
        return Result.success();
    }

    @PostMapping("/delete")
    @PreAuthorize("hasAuthority('project:delete')")
    public Result<Void> delete(@RequestParam Long id) {
        projectService.delete(id);
        return Result.success();
    }
}
