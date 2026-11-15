package com.autotest.platform.controller;

import com.autotest.platform.common.PageResult;
import com.autotest.platform.common.Result;
import com.autotest.platform.entity.SysUser;
import com.autotest.platform.service.UserService;
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
@RequestMapping("/api/system/user")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('system:user')")
public class UserController {

    private final UserService userService;

    @GetMapping("/list")
    public Result<PageResult<SysUser>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        return Result.success(PageResult.of(userService.page(page, pageSize, keyword, status)));
    }

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('system:user:create')")
    public Result<SysUser> create(@RequestBody SysUser user) {
        return Result.success(userService.create(user));
    }

    @PostMapping("/update")
    @PreAuthorize("hasAuthority('system:user:update')")
    public Result<SysUser> update(@RequestBody SysUser user) {
        return Result.success(userService.update(user));
    }

    @PostMapping("/reset-password")
    @PreAuthorize("hasAuthority('system:user:reset')")
    public Result<Void> resetPassword(@RequestParam Long id, @RequestParam String newPassword) {
        userService.resetPassword(id, newPassword);
        return Result.success();
    }

    @PostMapping("/status")
    public Result<Void> status(@RequestParam Long id, @RequestParam Integer status) {
        userService.changeStatus(id, status);
        return Result.success();
    }

    @GetMapping("/roles")
    public Result<List<Long>> roles(@RequestParam Long userId) {
        return Result.success(userService.roles(userId));
    }

    @PostMapping("/assign-roles")
    @PreAuthorize("hasAuthority('system:user:role')")
    public Result<Void> assignRoles(@RequestParam Long userId, @RequestBody List<Long> roleIds) {
        userService.assignRoles(userId, roleIds);
        return Result.success();
    }
}
