package com.autotest.platform.service;

import com.autotest.platform.entity.TestExecution;
import com.autotest.platform.mapper.ProjectMapper;
import com.autotest.platform.mapper.TestCaseMapper;
import com.autotest.platform.mapper.TestExecutionMapper;
import com.autotest.platform.mapper.TestSceneMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ProjectMapper projectMapper;
    private final TestCaseMapper caseMapper;
    private final TestSceneMapper sceneMapper;
    private final TestExecutionMapper executionMapper;

    public Map<String, Object> summary() {
        long projectCount = projectMapper.selectCount(null);
        long caseCount = caseMapper.selectCount(null);
        long sceneCount = sceneMapper.selectCount(null);

        List<TestExecution> all = executionMapper.selectList(new LambdaQueryWrapper<TestExecution>()
                .orderByDesc(TestExecution::getId));
        long total = all.size();
        long passed = all.stream().filter(e -> "PASSED".equals(e.getStatus())).count();

        Map<String, Object> result = new HashMap<>();
        result.put("projectCount", projectCount);
        result.put("caseCount", caseCount);
        result.put("sceneCount", sceneCount);
        result.put("executionCount", total);
        result.put("passCount", passed);
        result.put("passRate", total == 0 ? 0 : (int) Math.round(passed * 100.0 / total));
        result.put("recent", all.stream().limit(10).map(e -> Map.of(
                "id", e.getId(),
                "status", e.getStatus(),
                "executionNo", e.getExecutionNo(),
                "durationMs", e.getDurationMs() == null ? 0 : e.getDurationMs(),
                "finishedAt", e.getFinishedAt() == null ? "" : e.getFinishedAt().toString()
        )).toList());
        return result;
    }
}
