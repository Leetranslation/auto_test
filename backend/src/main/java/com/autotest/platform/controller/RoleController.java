package com.autotest.platform.controller;

import com.autotest.platform.common.Result;
import com.autotest.platform.entity.SysPermission;
import com.autotest.platform.entity.SysRole;
import com.autotest.platform.service.RoleService;
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
@RequestMapping("/api/system/role")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('system:role')")
public class RoleController {

    private final RoleService roleService;

    @GetMapping("/list")
    public Result<List<SysRole>> list() {
        return Result.success(roleService.list());
    }

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('system:role:create')")
    public Result<SysRole> create(@RequestBody SysRole role) {
        return Result.success(roleService.create(role));
    }

    @PostMapping("/update")
    @PreAuthorize("hasAuthority('system:role:update')")
    public Result<SysRole> update(@RequestBody SysRole role) {
        return Result.success(roleService.update(role));
    }

    @GetMapping("/permissions")
    public Result<List<Long>> permissions(@RequestParam Long roleId) {
        return Result.success(roleService.permissions(roleId));
    }

    @PostMapping("/assign")
    @PreAuthorize("hasAuthority('system:role:assign')")
    public Result<Void> assign(@RequestParam Long roleId, @RequestBody List<Long> permissionIds) {
        roleService.assignPermissions(roleId, permissionIds);
        return Result.success();
    }

    @GetMapping("/permission-list")
    public Result<List<SysPermission>> permissionList() {
        return Result.success(roleService.allPermissions());
    }
}
