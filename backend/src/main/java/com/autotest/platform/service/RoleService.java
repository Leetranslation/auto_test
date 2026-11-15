package com.autotest.platform.service;

import com.autotest.platform.common.BizException;
import com.autotest.platform.entity.SysPermission;
import com.autotest.platform.entity.SysRole;
import com.autotest.platform.entity.SysRolePermission;
import com.autotest.platform.mapper.SysPermissionMapper;
import com.autotest.platform.mapper.SysRoleMapper;
import com.autotest.platform.mapper.SysRolePermissionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final SysRoleMapper roleMapper;
    private final SysPermissionMapper permissionMapper;
    private final SysRolePermissionMapper rolePermissionMapper;

    public List<SysRole> list() {
        return roleMapper.selectList(new LambdaQueryWrapper<SysRole>().orderByAsc(SysRole::getId));
    }

    public SysRole create(SysRole role) {
        if (!StringUtils.hasText(role.getRoleCode())) throw new BizException("角色编码不能为空");
        if (!StringUtils.hasText(role.getRoleName())) throw new BizException("角色名称不能为空");
        role.setId(null);
        role.setScopeType(StringUtils.hasText(role.getScopeType()) ? role.getScopeType() : "PLATFORM");
        role.setStatus(role.getStatus() == null ? 1 : role.getStatus());
        roleMapper.insert(role);
        return role;
    }

    public SysRole update(SysRole role) {
        if (roleMapper.selectById(role.getId()) == null) throw new BizException(404, "角色不存在");
        roleMapper.updateById(role);
        return roleMapper.selectById(role.getId());
    }

    public List<Long> permissions(Long roleId) {
        return rolePermissionMapper.selectList(
                new LambdaQueryWrapper<SysRolePermission>().eq(SysRolePermission::getRoleId, roleId))
                .stream().map(SysRolePermission::getPermissionId).collect(Collectors.toList());
    }

    @Transactional
    public void assignPermissions(Long roleId, List<Long> permissionIds) {
        if (roleMapper.selectById(roleId) == null) throw new BizException(404, "角色不存在");
        rolePermissionMapper.delete(
                new LambdaQueryWrapper<SysRolePermission>().eq(SysRolePermission::getRoleId, roleId));
        if (permissionIds != null) {
            for (Long pid : permissionIds) {
                SysRolePermission rp = new SysRolePermission();
                rp.setRoleId(roleId);
                rp.setPermissionId(pid);
                rolePermissionMapper.insert(rp);
            }
        }
    }

    public List<SysPermission> allPermissions() {
        return permissionMapper.selectList(
                new LambdaQueryWrapper<SysPermission>().orderByAsc(SysPermission::getId));
    }
}
