package com.autotest.platform.controller;

import com.autotest.platform.common.Result;
import com.autotest.platform.entity.UiApplication;
import com.autotest.platform.entity.UiLocator;
import com.autotest.platform.entity.UiPageObject;
import com.autotest.platform.service.UiAssetService;
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
@RequestMapping("/api/ui")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ui:manage')")
public class UiController {

    private final UiAssetService uiAssetService;

    @GetMapping("/app/list")
    public Result<List<UiApplication>> appList(@RequestParam Long projectId) {
        return Result.success(uiAssetService.appList(projectId));
    }

    @PostMapping("/app/create")
    public Result<UiApplication> appCreate(@RequestBody UiApplication app) {
        return Result.success(uiAssetService.appCreate(app));
    }

    @PostMapping("/app/update")
    public Result<UiApplication> appUpdate(@RequestBody UiApplication app) {
        return Result.success(uiAssetService.appUpdate(app));
    }

    @PostMapping("/app/delete")
    public Result<Void> appDelete(@RequestParam Long id) {
        uiAssetService.appDelete(id);
        return Result.success();
    }

    @GetMapping("/page/list")
    public Result<List<UiPageObject>> pageList(@RequestParam Long projectId, @RequestParam(required = false) Long applicationId) {
        return Result.success(uiAssetService.pageList(projectId, applicationId));
    }

    @PostMapping("/page/create")
    public Result<UiPageObject> pageCreate(@RequestBody UiPageObject page) {
        return Result.success(uiAssetService.pageCreate(page));
    }

    @PostMapping("/page/update")
    public Result<UiPageObject> pageUpdate(@RequestBody UiPageObject page) {
        return Result.success(uiAssetService.pageUpdate(page));
    }

    @PostMapping("/page/delete")
    public Result<Void> pageDelete(@RequestParam Long id) {
        uiAssetService.pageDelete(id);
        return Result.success();
    }

    @GetMapping("/locator/list")
    public Result<List<UiLocator>> locatorList(@RequestParam Long pageObjectId) {
        return Result.success(uiAssetService.locatorList(pageObjectId));
    }

    @PostMapping("/locator/save")
    public Result<UiLocator> locatorSave(@RequestBody UiLocator locator) {
        return Result.success(uiAssetService.locatorSave(locator));
    }

    @PostMapping("/locator/delete")
    public Result<Void> locatorDelete(@RequestParam Long id) {
        uiAssetService.locatorDelete(id);
        return Result.success();
    }
}
