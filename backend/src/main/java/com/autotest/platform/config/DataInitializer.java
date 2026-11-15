package com.autotest.platform.config;

import com.autotest.platform.entity.SysPermission;
import com.autotest.platform.entity.SysRole;
import com.autotest.platform.entity.SysRolePermission;
import com.autotest.platform.entity.SysUser;
import com.autotest.platform.entity.SysUserRole;
import com.autotest.platform.mapper.SysPermissionMapper;
import com.autotest.platform.mapper.SysRoleMapper;
import com.autotest.platform.mapper.SysRolePermissionMapper;
import com.autotest.platform.mapper.SysUserMapper;
import com.autotest.platform.mapper.SysUserRoleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysPermissionMapper permissionMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;

    private static final String[][] PERMISSIONS = {
            {"system:user", "用户管理", "MENU"},
            {"system:user:create", "用户新增", "BUTTON"},
            {"system:user:update", "用户编辑", "BUTTON"},
            {"system:user:reset", "重置密码", "BUTTON"},
            {"system:user:role", "分配角色", "BUTTON"},
            {"system:role", "角色管理", "MENU"},
            {"system:role:create", "角色新增", "BUTTON"},
            {"system:role:update", "角色编辑", "BUTTON"},
            {"system:role:assign", "角色授权", "BUTTON"},
            {"project:manage", "项目管理", "MENU"},
            {"project:create", "项目新增", "BUTTON"},
            {"project:update", "项目编辑", "BUTTON"},
            {"project:delete", "项目删除", "BUTTON"},
            {"env:manage", "环境管理", "MENU"},
            {"api:manage", "接口测试", "MENU"},
            {"ui:manage", "UI自动化", "MENU"},
            {"scene:manage", "场景编排", "MENU"},
            {"suite:manage", "测试套件", "MENU"},
            {"exec:run", "执行测试", "BUTTON"},
            {"report:view", "测试报告", "MENU"},
            {"schedule:manage", "定时任务", "MENU"},
    };

    @Override
    @Transactional
    public void run(String... args) {
        List<SysPermission> perms = initPermissions();
        SysRole adminRole = initAdminRole();
        assignAllPermissions(adminRole.getId(), perms);
        SysUser admin = initAdminUser();
        bindAdminRole(admin.getId(), adminRole.getId());
    }

    private List<SysPermission> initPermissions() {
        List<SysPermission> result = new ArrayList<>();
        for (String[] p : PERMISSIONS) {
            SysPermission existing = permissionMapper.selectOne(
                    new LambdaQueryWrapper<SysPermission>().eq(SysPermission::getPermissionCode, p[0]));
            if (existing == null) {
                SysPermission perm = new SysPermission();
                perm.setPermissionCode(p[0]);
                perm.setPermissionName(p[1]);
                perm.setResourceType(p[2]);
                permissionMapper.insert(perm);
                result.add(perm);
            } else {
                result.add(existing);
            }
        }
        return result;
    }

    private SysRole initAdminRole() {
        SysRole role = roleMapper.selectOne(
                new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, "ADMIN"));
        if (role == null) {
            role = new SysRole();
            role.setRoleCode("ADMIN");
            role.setRoleName("平台管理员");
            role.setScopeType("PLATFORM");
            role.setStatus(1);
            roleMapper.insert(role);
            log.info("初始化平台管理员角色 ADMIN");
        }
        return role;
    }

    private void assignAllPermissions(Long roleId, List<SysPermission> permissions) {
        for (SysPermission perm : permissions) {
            Long count = rolePermissionMapper.selectCount(
                    new LambdaQueryWrapper<SysRolePermission>()
                            .eq(SysRolePermission::getRoleId, roleId)
                            .eq(SysRolePermission::getPermissionId, perm.getId()));
            if (count == 0) {
                SysRolePermission rp = new SysRolePermission();
                rp.setRoleId(roleId);
                rp.setPermissionId(perm.getId());
                rolePermissionMapper.insert(rp);
            }
        }
    }

    private SysUser initAdminUser() {
        SysUser admin = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, "admin"));
        if (admin == null) {
            admin = new SysUser();
            admin.setUsername("admin");
            admin.setPasswordHash(passwordEncoder.encode("admin123"));
            admin.setDisplayName("平台管理员");
            admin.setStatus(1);
            userMapper.insert(admin);
            log.info("已初始化平台管理员 admin");
        }
        return admin;
    }

    private void bindAdminRole(Long userId, Long roleId) {
        Long count = userRoleMapper.selectCount(
                new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getUserId, userId)
                        .eq(SysUserRole::getRoleId, roleId));
        if (count == 0) {
            SysUserRole ur = new SysUserRole();
            ur.setUserId(userId);
            ur.setRoleId(roleId);
            userRoleMapper.insert(ur);
        }
    }
}
