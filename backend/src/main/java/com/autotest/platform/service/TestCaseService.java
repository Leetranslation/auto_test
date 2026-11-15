package com.autotest.platform.service;

import com.autotest.platform.common.BizException;
import com.autotest.platform.entity.TestCase;
import com.autotest.platform.entity.TestCaseVersion;
import com.autotest.platform.mapper.TestCaseMapper;
import com.autotest.platform.mapper.TestCaseVersionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TestCaseService {

    private final TestCaseMapper caseMapper;
    private final TestCaseVersionMapper versionMapper;

    public Page<TestCase> page(long page, long pageSize, Long projectId, String caseType, String keyword) {
        LambdaQueryWrapper<TestCase> qw = new LambdaQueryWrapper<>();
        qw.eq(TestCase::getProjectId, projectId);
        if (StringUtils.hasText(caseType)) qw.eq(TestCase::getCaseType, caseType);
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like(TestCase::getCaseName, keyword).or().like(TestCase::getCaseCode, keyword));
        }
        qw.orderByDesc(TestCase::getId);
        return caseMapper.selectPage(new Page<>(page, pageSize), qw);
    }

    @Transactional
    public TestCase create(TestCase c, String definitionJson) {
        if (!StringUtils.hasText(c.getCaseName())) throw new BizException("用例名称不能为空");
        c.setId(null);
        c.setCaseCode(StringUtils.hasText(c.getCaseCode()) ? c.getCaseCode() : "CASE_" + System.currentTimeMillis());
        c.setCaseType(StringUtils.hasText(c.getCaseType()) ? c.getCaseType() : "API");
        c.setPriority(StringUtils.hasText(c.getPriority()) ? c.getPriority() : "P1");
        c.setStatus(StringUtils.hasText(c.getStatus()) ? c.getStatus() : "DRAFT");
        c.setCurrentVersion(1);
        caseMapper.insert(c);

        TestCaseVersion v = new TestCaseVersion();
        v.setTestCaseId(c.getId());
        v.setVersionNo(1);
        v.setDefinitionJson(definitionJson);
        v.setPublished(0);
        versionMapper.insert(v);
        return c;
    }

    public TestCase update(TestCase c) {
        if (caseMapper.selectById(c.getId()) == null) throw new BizException(404, "用例不存在");
        caseMapper.updateById(c);
        return caseMapper.selectById(c.getId());
    }

    public void delete(Long id) {
        caseMapper.deleteById(id);
    }

    public void changeStatus(Long id, String status) {
        TestCase c = caseMapper.selectById(id);
        if (c == null) throw new BizException(404, "用例不存在");
        c.setStatus(status);
        caseMapper.updateById(c);
    }

    public List<TestCaseVersion> versionList(Long testCaseId) {
        return versionMapper.selectList(new LambdaQueryWrapper<TestCaseVersion>()
                .eq(TestCaseVersion::getTestCaseId, testCaseId)
                .orderByDesc(TestCaseVersion::getVersionNo));
    }

    @Transactional
    public TestCaseVersion saveVersion(Long testCaseId, String definitionJson, boolean publish) {
        TestCase c = caseMapper.selectById(testCaseId);
        if (c == null) throw new BizException(404, "用例不存在");
        Integer max = versionMapper.selectList(new LambdaQueryWrapper<TestCaseVersion>()
                        .eq(TestCaseVersion::getTestCaseId, testCaseId))
                .stream().map(TestCaseVersion::getVersionNo).max(Integer::compareTo).orElse(0);
        TestCaseVersion v = new TestCaseVersion();
        v.setTestCaseId(testCaseId);
        v.setVersionNo(max + 1);
        v.setDefinitionJson(definitionJson);
        v.setPublished(publish ? 1 : 0);
        versionMapper.insert(v);
        c.setCurrentVersion(v.getVersionNo());
        caseMapper.updateById(c);
        return v;
    }
}
