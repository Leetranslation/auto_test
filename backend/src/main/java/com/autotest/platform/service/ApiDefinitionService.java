package com.autotest.platform.service;

import com.autotest.platform.common.BizException;
import com.autotest.platform.entity.ApiDefinition;
import com.autotest.platform.entity.ApiDefinitionVersion;
import com.autotest.platform.entity.ApiService;
import com.autotest.platform.mapper.ApiDefinitionMapper;
import com.autotest.platform.mapper.ApiDefinitionVersionMapper;
import com.autotest.platform.mapper.ApiServiceMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ApiDefinitionService {

    private final ApiServiceMapper serviceMapper;
    private final ApiDefinitionMapper definitionMapper;
    private final ApiDefinitionVersionMapper versionMapper;

    // ---- 服务 ----
    public List<ApiService> serviceList(Long projectId) {
        return serviceMapper.selectList(new LambdaQueryWrapper<ApiService>()
                .eq(ApiService::getProjectId, projectId).orderByDesc(ApiService::getId));
    }

    public ApiService serviceCreate(ApiService service) {
        if (!StringUtils.hasText(service.getServiceName())) throw new BizException("服务名称不能为空");
        service.setId(null);
        serviceMapper.insert(service);
        return service;
    }

    public void serviceDelete(Long id) {
        serviceMapper.deleteById(id);
    }

    // ---- 接口定义 ----
    public List<ApiDefinition> definitionList(Long projectId, Long serviceId) {
        LambdaQueryWrapper<ApiDefinition> qw = new LambdaQueryWrapper<>();
        qw.eq(ApiDefinition::getProjectId, projectId);
        if (serviceId != null) qw.eq(ApiDefinition::getServiceId, serviceId);
        qw.orderByDesc(ApiDefinition::getId);
        return definitionMapper.selectList(qw);
    }

    @Transactional
    public ApiDefinition definitionCreate(ApiDefinition def) {
        if (!StringUtils.hasText(def.getPath())) throw new BizException("路径不能为空");
        def.setId(null);
        def.setHttpMethod(StringUtils.hasText(def.getHttpMethod()) ? def.getHttpMethod().toUpperCase() : "GET");
        def.setAuthType(StringUtils.hasText(def.getAuthType()) ? def.getAuthType() : "INHERIT");
        def.setTimeoutMs(def.getTimeoutMs() == null ? 10000 : def.getTimeoutMs());
        def.setStatus(def.getStatus() == null ? 1 : def.getStatus());
        definitionMapper.insert(def);

        ApiDefinitionVersion v = new ApiDefinitionVersion();
        v.setApiDefinitionId(def.getId());
        v.setVersionNo(1);
        versionMapper.insert(v);
        return def;
    }

    public ApiDefinition definitionUpdate(ApiDefinition def) {
        if (definitionMapper.selectById(def.getId()) == null) throw new BizException(404, "接口不存在");
        definitionMapper.updateById(def);
        return definitionMapper.selectById(def.getId());
    }

    public void definitionDelete(Long id) {
        definitionMapper.deleteById(id);
    }

    // ---- 版本 ----
    public List<ApiDefinitionVersion> versionList(Long definitionId) {
        return versionMapper.selectList(new LambdaQueryWrapper<ApiDefinitionVersion>()
                .eq(ApiDefinitionVersion::getApiDefinitionId, definitionId)
                .orderByDesc(ApiDefinitionVersion::getVersionNo));
    }

    public ApiDefinitionVersion saveVersion(ApiDefinitionVersion v) {
        if (v.getApiDefinitionId() == null) throw new BizException("接口 ID 不能为空");
        Integer max = versionMapper.selectList(new LambdaQueryWrapper<ApiDefinitionVersion>()
                        .eq(ApiDefinitionVersion::getApiDefinitionId, v.getApiDefinitionId()))
                .stream().map(ApiDefinitionVersion::getVersionNo).max(Integer::compareTo).orElse(0);
        v.setId(null);
        v.setVersionNo(max + 1);
        versionMapper.insert(v);
        return v;
    }
}
