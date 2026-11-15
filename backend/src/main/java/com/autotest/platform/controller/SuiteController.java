package com.autotest.platform.controller;

import com.autotest.platform.common.Result;
import com.autotest.platform.entity.TestSuite;
import com.autotest.platform.entity.TestSuiteItem;
import com.autotest.platform.service.SuiteService;
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
@RequestMapping("/api/suite")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('suite:manage')")
public class SuiteController {

    private final SuiteService suiteService;

    @GetMapping("/list")
    public Result<List<TestSuite>> list(@RequestParam Long projectId) {
        return Result.success(suiteService.list(projectId));
    }

    @PostMapping("/create")
    public Result<TestSuite> create(@RequestBody TestSuite suite) {
        return Result.success(suiteService.create(suite));
    }

    @PostMapping("/update")
    public Result<TestSuite> update(@RequestBody TestSuite suite) {
        return Result.success(suiteService.update(suite));
    }

    @PostMapping("/delete")
    public Result<Void> delete(@RequestParam Long id) {
        suiteService.delete(id);
        return Result.success();
    }

    @GetMapping("/items")
    public Result<List<TestSuiteItem>> items(@RequestParam Long suiteId) {
        return Result.success(suiteService.items(suiteId));
    }

    @PostMapping("/item/add")
    public Result<TestSuiteItem> addItem(@RequestParam Long suiteId,
                                          @RequestParam String targetType,
                                          @RequestParam Long targetId) {
        return Result.success(suiteService.addItem(suiteId, targetType, targetId));
    }

    @PostMapping("/item/remove")
    public Result<Void> removeItem(@RequestParam Long itemId) {
        suiteService.removeItem(itemId);
        return Result.success();
    }

    @PostMapping("/run")
    public Result<List<Map<String, Object>>> run(@RequestParam Long suiteId) {
        return Result.success(suiteService.run(suiteId));
    }
}
