package com.autotest.platform.service;

import com.autotest.platform.common.BizException;
import com.autotest.platform.entity.EnvironmentVariable;
import com.autotest.platform.entity.TestEnvironment;
import com.autotest.platform.mapper.EnvironmentVariableMapper;
import com.autotest.platform.mapper.TestEnvironmentMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EnvironmentService {

    private final TestEnvironmentMapper envMapper;
    private final EnvironmentVariableMapper varMapper;

    public List<TestEnvironment> list(Long projectId) {
        return envMapper.selectList(new LambdaQueryWrapper<TestEnvironment>()
                .eq(TestEnvironment::getProjectId, projectId)
                .orderByDesc(TestEnvironment::getId));
    }

    public TestEnvironment create(TestEnvironment env) {
        if (!StringUtils.hasText(env.getEnvCode())) throw new BizException("环境编码不能为空");
        if (!StringUtils.hasText(env.getBaseUrl())) throw new BizException("Base URL 不能为空");
        env.setId(null);
        env.setEnvType(StringUtils.hasText(env.getEnvType()) ? env.getEnvType() : "TEST");
        env.setEnvLevel(StringUtils.hasText(env.getEnvLevel()) ? env.getEnvLevel() : "TEST");
        env.setIsDefault(env.getIsDefault() == null ? 0 : env.getIsDefault());
        env.setStatus(env.getStatus() == null ? 1 : env.getStatus());
        envMapper.insert(env);
        return env;
    }

    public TestEnvironment update(TestEnvironment env) {
        if (envMapper.selectById(env.getId()) == null) throw new BizException(404, "环境不存在");
        envMapper.updateById(env);
        return envMapper.selectById(env.getId());
    }

    public void delete(Long id) {
        if (envMapper.selectById(id) == null) throw new BizException(404, "环境不存在");
        varMapper.delete(new LambdaQueryWrapper<EnvironmentVariable>().eq(EnvironmentVariable::getEnvironmentId, id));
        envMapper.deleteById(id);
    }

    public List<EnvironmentVariable> variables(Long projectId, Long environmentId) {
        return varMapper.selectList(new LambdaQueryWrapper<EnvironmentVariable>()
                .eq(EnvironmentVariable::getProjectId, projectId)
                .eq(EnvironmentVariable::getEnvironmentId, environmentId));
    }

    public EnvironmentVariable saveVariable(EnvironmentVariable v) {
        if (!StringUtils.hasText(v.getVarKey())) throw new BizException("变量名不能为空");
        EnvironmentVariable existing = varMapper.selectOne(new LambdaQueryWrapper<EnvironmentVariable>()
                .eq(EnvironmentVariable::getProjectId, v.getProjectId())
                .eq(EnvironmentVariable::getEnvironmentId, v.getEnvironmentId())
                .eq(EnvironmentVariable::getVarKey, v.getVarKey()));
        if (existing != null) {
            existing.setValueCiphertext(v.getValueCiphertext());
            existing.setValueType(StringUtils.hasText(v.getValueType()) ? v.getValueType() : "STRING");
            existing.setIsSecret(v.getIsSecret() == null ? 0 : v.getIsSecret());
            varMapper.updateById(existing);
            return existing;
        }
        v.setId(null);
        v.setValueType(StringUtils.hasText(v.getValueType()) ? v.getValueType() : "STRING");
        v.setIsSecret(v.getIsSecret() == null ? 0 : v.getIsSecret());
        varMapper.insert(v);
        return v;
    }

    public void deleteVariable(Long id) {
        varMapper.deleteById(id);
    }
}
