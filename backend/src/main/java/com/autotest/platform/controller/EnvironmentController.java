package com.autotest.platform.controller;

import com.autotest.platform.common.Result;
import com.autotest.platform.entity.EnvironmentVariable;
import com.autotest.platform.entity.TestEnvironment;
import com.autotest.platform.service.EnvironmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/env")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('env:manage')")
public class EnvironmentController {

    private final EnvironmentService environmentService;

    @GetMapping("/list")
    public Result<List<TestEnvironment>> list(@RequestParam Long projectId) {
        return Result.success(environmentService.list(projectId));
    }

    @PostMapping("/create")
    public Result<TestEnvironment> create(@RequestBody TestEnvironment env) {
        return Result.success(environmentService.create(env));
    }

    @PostMapping("/update")
    public Result<TestEnvironment> update(@RequestBody TestEnvironment env) {
        return Result.success(environmentService.update(env));
    }

    @PostMapping("/delete")
    public Result<Void> delete(@RequestParam Long id) {
        environmentService.delete(id);
        return Result.success();
    }

    @GetMapping("/variables")
    public Result<List<EnvironmentVariable>> variables(@RequestParam Long projectId, @RequestParam Long environmentId) {
        return Result.success(environmentService.variables(projectId, environmentId));
    }

    @PostMapping("/variable/save")
    public Result<EnvironmentVariable> saveVariable(@RequestBody EnvironmentVariable v) {
        return Result.success(environmentService.saveVariable(v));
    }

    @PostMapping("/variable/delete")
    public Result<Void> deleteVariable(@RequestParam Long id) {
        environmentService.deleteVariable(id);
        return Result.success();
    }
}
