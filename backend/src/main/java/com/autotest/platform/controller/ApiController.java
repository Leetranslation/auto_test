package com.autotest.platform.controller;

import com.autotest.platform.common.Result;
import com.autotest.platform.entity.ApiDefinition;
import com.autotest.platform.entity.ApiDefinitionVersion;
import com.autotest.platform.entity.ApiService;
import com.autotest.platform.service.ApiDefinitionService;
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
@RequestMapping("/api/api")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('api:manage')")
public class ApiController {

    private final ApiDefinitionService apiDefinitionService;

    @GetMapping("/service/list")
    public Result<List<ApiService>> serviceList(@RequestParam Long projectId) {
        return Result.success(apiDefinitionService.serviceList(projectId));
    }

    @PostMapping("/service/create")
    public Result<ApiService> serviceCreate(@RequestBody ApiService service) {
        return Result.success(apiDefinitionService.serviceCreate(service));
    }

    @PostMapping("/service/delete")
    public Result<Void> serviceDelete(@RequestParam Long id) {
        apiDefinitionService.serviceDelete(id);
        return Result.success();
    }

    @GetMapping("/definition/list")
    public Result<List<ApiDefinition>> definitionList(@RequestParam Long projectId,
                                                       @RequestParam(required = false) Long serviceId) {
        return Result.success(apiDefinitionService.definitionList(projectId, serviceId));
    }

    @PostMapping("/definition/create")
    public Result<ApiDefinition> definitionCreate(@RequestBody ApiDefinition def) {
        return Result.success(apiDefinitionService.definitionCreate(def));
    }

    @PostMapping("/definition/update")
    public Result<ApiDefinition> definitionUpdate(@RequestBody ApiDefinition def) {
        return Result.success(apiDefinitionService.definitionUpdate(def));
    }

    @PostMapping("/definition/delete")
    public Result<Void> definitionDelete(@RequestParam Long id) {
        apiDefinitionService.definitionDelete(id);
        return Result.success();
    }

    @GetMapping("/definition/version/list")
    public Result<List<ApiDefinitionVersion>> versionList(@RequestParam Long definitionId) {
        return Result.success(apiDefinitionService.versionList(definitionId));
    }

    @PostMapping("/definition/version/save")
    public Result<ApiDefinitionVersion> saveVersion(@RequestBody ApiDefinitionVersion v) {
        return Result.success(apiDefinitionService.saveVersion(v));
    }
}
