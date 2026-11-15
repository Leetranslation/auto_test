package com.autotest.platform.service;

import com.autotest.platform.common.BizException;
import com.autotest.platform.entity.SysUser;
import com.autotest.platform.entity.SysUserRole;
import com.autotest.platform.mapper.SysUserMapper;
import com.autotest.platform.mapper.SysUserRoleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;

    public Page<SysUser> page(long page, long pageSize, String keyword, Integer status) {
        LambdaQueryWrapper<SysUser> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like(SysUser::getUsername, keyword).or().like(SysUser::getDisplayName, keyword));
        }
        if (status != null) qw.eq(SysUser::getStatus, status);
        qw.orderByDesc(SysUser::getId);
        return userMapper.selectPage(new Page<>(page, pageSize), qw);
    }

    @Transactional
    public SysUser create(SysUser user) {
        if (!StringUtils.hasText(user.getUsername())) throw new BizException("用户名不能为空");
        validatePassword(user.getPasswordHash());
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, user.getUsername()));
        if (count > 0) throw new BizException("用户名已存在");
        user.setId(null);
        user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
        user.setStatus(user.getStatus() == null ? 1 : user.getStatus());
        userMapper.insert(user);
        user.setPasswordHash(null);
        return user;
    }

    public SysUser update(SysUser user) {
        if (userMapper.selectById(user.getId()) == null) throw new BizException(404, "用户不存在");
        user.setPasswordHash(null);
        userMapper.updateById(user);
        return userMapper.selectById(user.getId());
    }

    public void resetPassword(Long id, String newPassword) {
        SysUser user = userMapper.selectById(id);
        if (user == null) throw new BizException(404, "用户不存在");
        validatePassword(newPassword);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
    }

    public void changeStatus(Long id, Integer status) {
        SysUser user = userMapper.selectById(id);
        if (user == null) throw new BizException(404, "用户不存在");
        user.setStatus(status);
        userMapper.updateById(user);
    }

    public List<Long> roles(Long userId) {
        return userRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId))
                .stream().map(SysUserRole::getRoleId).collect(Collectors.toList());
    }

    @Transactional
    public void assignRoles(Long userId, List<Long> roleIds) {
        if (userMapper.selectById(userId) == null) throw new BizException(404, "用户不存在");
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        if (roleIds != null) {
            for (Long rid : roleIds) {
                SysUserRole ur = new SysUserRole();
                ur.setUserId(userId);
                ur.setRoleId(rid);
                userRoleMapper.insert(ur);
            }
        }
    }

    private void validatePassword(String password) {
        if (!StringUtils.hasText(password)) throw new BizException("密码不能为空");
        if (password.length() < 6) throw new BizException("密码长度至少 6 位");
    }
}
