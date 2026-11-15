package com.autotest.platform.security;

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
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final SysPermissionMapper permissionMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }
        // 权限码作为 authority（供 @PreAuthorize("hasAuthority(...)") 使用）
        List<String> authorities = loadAuthorities(user.getId());
        return User.builder()
                .username(user.getUsername())
                .password(user.getPasswordHash())
                .disabled(user.getStatus() == null || user.getStatus() == 0)
                .authorities(authorities.toArray(String[]::new))
                .build();
    }

    private List<String> loadAuthorities(Long userId) {
        List<Long> roleIds = userRoleMapper.selectList(
                        new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId))
                .stream().map(SysUserRole::getRoleId).collect(Collectors.toList());
        if (roleIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<String> result = roleMapper.selectBatchIds(roleIds).stream()
                .map(SysRole::getRoleCode)
                .collect(Collectors.toList());
        List<Long> permIds = rolePermissionMapper.selectList(
                        new LambdaQueryWrapper<SysRolePermission>().in(SysRolePermission::getRoleId, roleIds))
                .stream().map(SysRolePermission::getPermissionId).distinct().collect(Collectors.toList());
        if (!permIds.isEmpty()) {
            permissionMapper.selectBatchIds(permIds).stream()
                    .map(SysPermission::getPermissionCode)
                    .forEach(result::add);
        }
        return result;
    }
}
