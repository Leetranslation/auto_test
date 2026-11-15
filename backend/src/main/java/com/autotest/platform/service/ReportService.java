package com.autotest.platform.service;

import com.autotest.platform.entity.ExecutionLog;
import com.autotest.platform.entity.StepExecution;
import com.autotest.platform.entity.TestExecution;
import com.autotest.platform.mapper.ExecutionLogMapper;
import com.autotest.platform.mapper.StepExecutionMapper;
import com.autotest.platform.mapper.TestExecutionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final TestExecutionMapper executionMapper;
    private final StepExecutionMapper stepMapper;
    private final ExecutionLogMapper logMapper;
    private final ObjectMapper objectMapper;

    public String exportHtml(Long executionId) {
        TestExecution exec = executionMapper.selectById(executionId);
        if (exec == null) return "<html><body>执行不存在</body></html>";

        List<StepExecution> steps = stepMapper.selectList(new LambdaQueryWrapper<StepExecution>()
                .eq(StepExecution::getExecutionId, executionId).orderByAsc(StepExecution::getSortNo));
        List<ExecutionLog> logs = logMapper.selectList(new LambdaQueryWrapper<ExecutionLog>()
                .eq(ExecutionLog::getExecutionId, executionId).orderByAsc(ExecutionLog::getId));

        int passed = 0, failed = 0;
        for (StepExecution s : steps) {
            if ("PASSED".equals(s.getStatus())) passed++;
            else failed++;
        }
        String statusColor = "FAILED".equals(exec.getStatus()) ? "#dc2626" : "#15803d";

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html lang=\"zh-CN\"><head><meta charset=\"UTF-8\">");
        sb.append("<title>测试报告 ").append(esc(exec.getExecutionNo())).append("</title>");
        sb.append("<style>");
        sb.append("body{font-family:-apple-system,'Microsoft YaHei',sans-serif;margin:24px;color:#1f2937;}");
        sb.append("h1{font-size:22px;margin-bottom:4px;}");
        sb.append(".sub{color:#6b7280;margin-bottom:20px;}");
        sb.append(".cards{display:flex;gap:12px;margin-bottom:24px;flex-wrap:wrap;}");
        sb.append(".card{border:1px solid #e5e7eb;border-radius:8px;padding:12px 20px;min-width:110px;}");
        sb.append(".card .v{font-size:24px;font-weight:700;}");
        sb.append(".card .l{color:#6b7280;font-size:12px;}");
        sb.append("table{border-collapse:collapse;width:100%;margin-bottom:24px;}");
        sb.append("th,td{border:1px solid #e5e7eb;padding:8px 10px;font-size:13px;text-align:left;}");
        sb.append("th{background:#f9fafb;}");
        sb.append(".PASSED{color:#15803d;font-weight:600;}");
        sb.append(".FAILED{color:#dc2626;font-weight:600;}");
        sb.append(".RUNNING,.QUEUED{color:#b45309;font-weight:600;}");
        sb.append(".log{font-size:12px;color:#4b5563;padding:2px 0;}");
        sb.append(".err{color:#dc2626;}");
        sb.append("</style></head><body>");
        sb.append("<h1>测试报告</h1>");
        sb.append("<div class=\"sub\">执行编号：").append(esc(exec.getExecutionNo()))
          .append(" ｜ 完成时间：").append(exec.getFinishedAt() == null ? "-" : exec.getFinishedAt().toString()).append("</div>");

        sb.append("<div class=\"cards\">");
        sb.append(card("状态", exec.getStatus(), statusColor));
        sb.append(card("耗时(ms)", String.valueOf(exec.getDurationMs() == null ? 0 : exec.getDurationMs()), "#1f2937"));
        sb.append(card("总步骤", String.valueOf(passed + failed), "#1f2937"));
        sb.append(card("通过", String.valueOf(passed), "#15803d"));
        sb.append(card("失败", String.valueOf(failed), "#dc2626"));
        sb.append("</div>");

        sb.append("<h2>步骤明细</h2><table><tr><th>#</th><th>类型</th><th>名称</th><th>状态</th><th>耗时</th><th>错误</th></tr>");
        for (StepExecution s : steps) {
            sb.append("<tr><td>").append(s.getSortNo()).append("</td>")
              .append("<td>").append(esc(s.getStepType())).append("</td>")
              .append("<td>").append(esc(s.getStepName())).append("</td>")
              .append("<td class=\"").append(s.getStatus()).append("\">").append(s.getStatus()).append("</td>")
              .append("<td>").append(s.getDurationMs() == null ? 0 : s.getDurationMs()).append("</td>")
              .append("<td class=\"err\">").append(esc(s.getErrorMessage() == null ? "" : s.getErrorMessage())).append("</td>")
              .append("</tr>");
        }
        sb.append("</table>");

        sb.append("<h2>执行日志</h2>");
        if (logs.isEmpty()) sb.append("<div class=\"log\">无日志</div>");
        for (ExecutionLog l : logs) {
            sb.append("<div class=\"log ").append("ERROR".equals(l.getLogLevel()) ? "err" : "").append("\">")
              .append("[").append(l.getCreatedAt() == null ? "" : l.getCreatedAt().toString()).append("] ")
              .append("[").append(l.getLogLevel()).append("] ")
              .append(esc(l.getMessage() == null ? "" : l.getMessage())).append("</div>");
        }

        sb.append("</body></html>");
        return sb.toString();
    }

    private String card(String label, String value, String color) {
        return "<div class=\"card\"><div class=\"v\" style=\"color:" + color + "\">" + esc(value) + "</div><div class=\"l\">" + esc(label) + "</div></div>";
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private JsonNode parse(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            return null;
        }
    }
}
