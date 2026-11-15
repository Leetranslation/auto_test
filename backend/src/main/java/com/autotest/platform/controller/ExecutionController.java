package com.autotest.platform.controller;

import com.autotest.platform.common.PageResult;
import com.autotest.platform.common.Result;
import com.autotest.platform.dto.RunRequest;
import com.autotest.platform.entity.ExecutionLog;
import com.autotest.platform.entity.StepExecution;
import com.autotest.platform.entity.TestExecution;
import com.autotest.platform.entity.TestReport;
import com.autotest.platform.mapper.ExecutionLogMapper;
import com.autotest.platform.mapper.StepExecutionMapper;
import com.autotest.platform.mapper.TestExecutionMapper;
import com.autotest.platform.mapper.TestReportMapper;
import com.autotest.platform.service.ExecutionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/exec")
@RequiredArgsConstructor
public class ExecutionController {

    private final ExecutionService executionService;
    private final TestExecutionMapper executionMapper;
    private final StepExecutionMapper stepMapper;
    private final ExecutionLogMapper logMapper;
    private final TestReportMapper reportMapper;

    @PostMapping("/run")
    @PreAuthorize("hasAuthority('exec:run')")
    public Result<TestExecution> run(@RequestBody RunRequest req) {
        return Result.success(executionService.submit(req));
    }

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('report:view')")
    public Result<PageResult<TestExecution>> list(
            @RequestParam Long projectId,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize) {
        Page<TestExecution> p = executionMapper.selectPage(new Page<>(page, pageSize),
                new LambdaQueryWrapper<TestExecution>()
                        .eq(TestExecution::getProjectId, projectId)
                        .orderByDesc(TestExecution::getId));
        return Result.success(PageResult.of(p));
    }

    @GetMapping("/detail")
    @PreAuthorize("hasAuthority('report:view')")
    public Result<Map<String, Object>> detail(@RequestParam Long executionId) {
        TestExecution exec = executionMapper.selectById(executionId);
        List<StepExecution> steps = stepMapper.selectList(new LambdaQueryWrapper<StepExecution>()
                .eq(StepExecution::getExecutionId, executionId)
                .orderByAsc(StepExecution::getSortNo));
        List<ExecutionLog> logs = logMapper.selectList(new LambdaQueryWrapper<ExecutionLog>()
                .eq(ExecutionLog::getExecutionId, executionId)
                .orderByAsc(ExecutionLog::getId));
        TestReport report = reportMapper.selectOne(new LambdaQueryWrapper<TestReport>()
                .eq(TestReport::getExecutionId, executionId));
        return Result.success(Map.of(
                "execution", exec,
                "steps", steps,
                "logs", logs,
                "report", report == null ? "" : report.getReportJson()));
    }
}
