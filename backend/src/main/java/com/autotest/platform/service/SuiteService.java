package com.autotest.platform.service;

import com.autotest.platform.common.BizException;
import com.autotest.platform.dto.RunRequest;
import com.autotest.platform.entity.TestExecution;
import com.autotest.platform.entity.TestSuite;
import com.autotest.platform.entity.TestSuiteItem;
import com.autotest.platform.entity.TestSuiteVersion;
import com.autotest.platform.mapper.TestSuiteItemMapper;
import com.autotest.platform.mapper.TestSuiteMapper;
import com.autotest.platform.mapper.TestSuiteVersionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SuiteService {

    private final TestSuiteMapper suiteMapper;
    private final TestSuiteItemMapper itemMapper;
    private final TestSuiteVersionMapper versionMapper;
    private final ExecutionService executionService;

    public List<TestSuite> list(Long projectId) {
        return suiteMapper.selectList(new LambdaQueryWrapper<TestSuite>()
                .eq(TestSuite::getProjectId, projectId).orderByDesc(TestSuite::getId));
    }

    @Transactional
    public TestSuite create(TestSuite suite) {
        if (!StringUtils.hasText(suite.getSuiteName())) throw new BizException("套件名称不能为空");
        suite.setId(null);
        suite.setExecutionPolicy(StringUtils.hasText(suite.getExecutionPolicy()) ? suite.getExecutionPolicy() : "SEQUENTIAL");
        suite.setStatus(suite.getStatus() == null ? 1 : suite.getStatus());
        suite.setCurrentVersion(1);
        suiteMapper.insert(suite);

        TestSuiteVersion v = new TestSuiteVersion();
        v.setSuiteId(suite.getId());
        v.setVersionNo(1);
        v.setDefinitionJson("{\"items\":[]}");
        v.setPublished(0);
        versionMapper.insert(v);
        return suite;
    }

    public TestSuite update(TestSuite suite) {
        if (suiteMapper.selectById(suite.getId()) == null) throw new BizException(404, "套件不存在");
        suiteMapper.updateById(suite);
        return suiteMapper.selectById(suite.getId());
    }

    public void delete(Long id) {
        suiteMapper.deleteById(id);
        itemMapper.delete(new LambdaQueryWrapper<TestSuiteItem>().eq(TestSuiteItem::getSuiteId, id));
    }

    public void changeStatus(Long id, Integer status) {
        TestSuite suite = suiteMapper.selectById(id);
        if (suite == null) throw new BizException(404, "套件不存在");
        suite.setStatus(status);
        suiteMapper.updateById(suite);
    }

    public List<TestSuiteItem> items(Long suiteId) {
        return itemMapper.selectList(new LambdaQueryWrapper<TestSuiteItem>()
                .eq(TestSuiteItem::getSuiteId, suiteId).orderByAsc(TestSuiteItem::getSortNo));
    }

    public TestSuiteItem addItem(Long suiteId, String targetType, Long targetId) {
        Integer max = itemMapper.selectList(new LambdaQueryWrapper<TestSuiteItem>()
                        .eq(TestSuiteItem::getSuiteId, suiteId))
                .stream().map(i -> i.getSortNo() == null ? 0 : i.getSortNo()).max(Integer::compareTo).orElse(0);
        TestSuiteItem item = new TestSuiteItem();
        item.setSuiteId(suiteId);
        item.setTargetType(StringUtils.hasText(targetType) ? targetType : "SCENE");
        item.setTargetId(targetId);
        item.setSortNo(max + 1);
        item.setEnabled(1);
        itemMapper.insert(item);
        return item;
    }

    public void removeItem(Long itemId) {
        itemMapper.deleteById(itemId);
    }

    /** 一键执行套件：逐个提交（异步） */
    public List<Map<String, Object>> run(Long suiteId) {
        List<TestSuiteItem> items = itemMapper.selectList(new LambdaQueryWrapper<TestSuiteItem>()
                .eq(TestSuiteItem::getSuiteId, suiteId)
                .eq(TestSuiteItem::getEnabled, 1)
                .orderByAsc(TestSuiteItem::getSortNo));
        List<Map<String, Object>> results = new ArrayList<>();
        for (TestSuiteItem item : items) {
            RunRequest req = new RunRequest();
            req.setTargetType(item.getTargetType());
            req.setTargetId(item.getTargetId());
            TestSuite suite = suiteMapper.selectById(suiteId);
            req.setProjectId(suite.getProjectId());
            TestExecution exec = executionService.submit(req);
            results.add(Map.of("itemId", item.getId(), "executionId", exec.getId(), "status", exec.getStatus()));
        }
        return results;
    }
}
