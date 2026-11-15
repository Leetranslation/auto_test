package com.autotest.platform.service;

import com.autotest.platform.dto.RunRequest;
import com.autotest.platform.entity.ExecutionLog;
import com.autotest.platform.entity.StepExecution;
import com.autotest.platform.entity.TestExecution;
import com.autotest.platform.entity.TestReport;
import com.autotest.platform.entity.TestTask;
import com.autotest.platform.mapper.ExecutionLogMapper;
import com.autotest.platform.mapper.StepExecutionMapper;
import com.autotest.platform.mapper.TestExecutionMapper;
import com.autotest.platform.mapper.TestReportMapper;
import com.autotest.platform.mapper.TestTaskMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExecutionService {

    private final SceneService sceneService;
    private final TestTaskMapper taskMapper;
    private final TestExecutionMapper executionMapper;
    private final StepExecutionMapper stepMapper;
    private final ExecutionLogMapper logMapper;
    private final TestReportMapper reportMapper;
    private final ObjectMapper objectMapper;
    private final ThreadPoolTaskExecutor executionExecutor;

    @Value("${platform.runner.python}")
    private String runnerPython;

    @Value("${platform.runner.ui-script}")
    private String uiScript;

    @Value("${platform.artifact.dir:artifacts}")
    private String artifactDir;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();

    /** 提交执行：创建任务+执行记录（QUEUED），异步执行 */
    public TestExecution submit(RunRequest req) {
        TestTask task = new TestTask();
        task.setProjectId(req.getProjectId());
        task.setTargetType(req.getTargetType());
        task.setTargetId(req.getTargetId());
        task.setEnvironmentId(req.getEnvironmentId());
        task.setTaskName(req.getTargetType() + ":" + req.getTargetId());
        task.setCreatedAt(LocalDateTime.now());
        taskMapper.insert(task);

        TestExecution exec = new TestExecution();
        exec.setTaskId(task.getId());
        exec.setProjectId(req.getProjectId());
        exec.setExecutionNo("EXE" + System.currentTimeMillis());
        exec.setStatus("QUEUED");
        exec.setCreatedAt(LocalDateTime.now());
        executionMapper.insert(exec);

        executionExecutor.execute(() -> runAsync(task, exec));
        return exec;
    }

    /**
     * DAG 流程编排执行引擎：
     * 1. 依据 edges 构建邻接表与入度；
     * 2. 从 START 节点 / 入度为 0 的节点出发，按连接关系遍历；
     * 3. CONDITION 节点按判断结果走 true/false 分支；
     * 4. 无 edges 时（旧数据）退化为顺序链执行，保证兼容。
     */
    private void runAsync(TestTask task, TestExecution exec) {
        try {
            exec.setStatus("RUNNING");
            exec.setStartedAt(LocalDateTime.now());
            executionMapper.updateById(exec);

            String graphJson = sceneService.graph(task.getTargetId(), null);
            JsonNode graph;
            try {
                graph = objectMapper.readTree(graphJson);
            } catch (Exception e) {
                fail(exec, "图解析失败: " + e.getMessage());
                return;
            }
            exec.setVersionSnapshotJson(graphJson);
            executionMapper.updateById(exec);

            // ---- 1. 节点索引（nodeKey -> node）----
            List<String> orderedKeys = new ArrayList<>();
            Map<String, JsonNode> nodeByKey = new HashMap<>();
            for (JsonNode n : graph.path("nodes")) {
                String key = n.path("nodeKey").asText();
                if (key.isEmpty()) key = "node_" + (orderedKeys.size() + 1);
                orderedKeys.add(key);
                nodeByKey.put(key, n);
            }

            // ---- 2. 边集合（旧数据无 edges 时顺序串链）----
            List<JsonNode> edgeList = new ArrayList<>();
            graph.path("edges").forEach(edgeList::add);
            if (edgeList.isEmpty() && orderedKeys.size() > 1) {
                for (int i = 0; i < orderedKeys.size() - 1; i++) {
                    JsonNode e = objectMapper.createObjectNode()
                            .put("source", orderedKeys.get(i))
                            .put("target", orderedKeys.get(i + 1))
                            .put("condition", "");
                    edgeList.add(e);
                }
            }

            // ---- 3. 邻接表 + 入度 ----
            Map<String, List<JsonNode>> successors = new HashMap<>();
            Map<String, Integer> inDegree = new HashMap<>();
            for (String k : orderedKeys) inDegree.put(k, 0);
            for (JsonNode e : edgeList) {
                String src = e.path("source").asText();
                String tgt = e.path("target").asText();
                if (!nodeByKey.containsKey(src) || !nodeByKey.containsKey(tgt)) continue;
                successors.computeIfAbsent(src, k -> new ArrayList<>()).add(e);
                inDegree.merge(tgt, 1, Integer::sum);
            }

            // ---- 4. 起点：START 节点或入度为 0 的节点 ----
            Deque<String> queue = new ArrayDeque<>();
            for (String k : orderedKeys) {
                JsonNode n = nodeByKey.get(k);
                if ("START".equals(n.path("nodeType").asText()) || inDegree.getOrDefault(k, 0) == 0) {
                    queue.offer(k);
                }
            }
            if (queue.isEmpty() && !orderedKeys.isEmpty()) queue.offer(orderedKeys.get(0));

            // ---- 5. 按 DAG 拓扑遍历执行 ----
            ExecContext ctx = new ExecContext();
            int sort = 0;
            int passed = 0;
            int failed = 0;
            Set<String> visited = new HashSet<>();

            while (!queue.isEmpty()) {
                String key = queue.poll();
                if (!visited.add(key)) continue; // 防环：每个节点只执行一次

                JsonNode node = nodeByKey.get(key);
                sort++;
                NodeResult nr = executeNode(exec, node, sort, ctx);
                if (nr.passed) passed++;
                else failed++;

                // 决定后继：CONDITION 按结果走分支，其余走全部出边
                for (JsonNode e : successors.getOrDefault(key, List.of())) {
                    if (nr.isCondition) {
                        String want = nr.conditionResult ? "true" : "false";
                        if (!want.equals(e.path("condition").asText(""))) continue;
                    }
                    String tgt = e.path("target").asText();
                    if (nodeByKey.containsKey(tgt)) queue.offer(tgt);
                }
            }

            // ---- 6. 完成 + 报告 ----
            try {
                log(exec.getId(), null, "INFO", "变量快照: " + objectMapper.writeValueAsString(ctx.vars));
            } catch (Exception ignored) {
            }
            exec.setStatus(failed == 0 ? "PASSED" : "FAILED");
            exec.setFinishedAt(LocalDateTime.now());
            exec.setDurationMs(java.time.Duration.between(exec.getStartedAt(), exec.getFinishedAt()).toMillis());
            exec.setSummaryJson(objectMapper.valueToTree(Map.of("total", passed + failed, "passed", passed, "failed", failed)).toString());
            executionMapper.updateById(exec);

            TestReport report = new TestReport();
            report.setExecutionId(exec.getId());
            report.setReportJson(exec.getSummaryJson());
            report.setCreatedAt(LocalDateTime.now());
            reportMapper.insert(report);
        } catch (Exception e) {
            log.error("执行异常", e);
            fail(exec, e.getMessage());
        }
    }

    /** 执行单个节点，记录 step_execution + 日志，返回结果与分支标志 */
    private NodeResult executeNode(TestExecution exec, JsonNode node, int sort, ExecContext ctx) {
        String type = node.path("nodeType").asText();
        String name = node.path("nodeName").asText(type);
        String key = node.path("nodeKey").asText("node_" + sort);
        JsonNode config = node.path("config");

        StepExecution step = new StepExecution();
        step.setExecutionId(exec.getId());
        step.setNodeKey(key);
        step.setStepName(name);
        step.setStepType(type);
        step.setSortNo(sort);
        step.setIterationNo(0);
        step.setAttemptNo(0);
        step.setStatus("RUNNING");
        step.setStartedAt(LocalDateTime.now());
        stepMapper.insert(step);
        log(exec.getId(), step.getId(), "INFO", "执行节点 " + type + " - " + name);

        NodeResult result = new NodeResult();
        try {
            switch (type) {
                case "START", "END" -> {
                    step.setStatus("PASSED");
                    step.setOutputSnapshot("{}");
                    result.passed = true;
                }
                case "API" -> {
                    Map<String, Object> r = execApi(config, ctx.vars);
                    step.setStatus((boolean) r.get("passed") ? "PASSED" : "FAILED");
                    step.setInputSnapshot(snapshot(r.get("input")));
                    step.setOutputSnapshot(snapshot(r.get("output")));
                    if (!(boolean) r.get("passed")) step.setErrorMessage(String.valueOf(r.get("error")));
                    ctx.lastBody = (String) r.get("body");
                    ctx.lastStatus = (int) r.get("status");
                    applyExtracts(config, ctx.vars, ctx.lastBody);
                    result.passed = (boolean) r.get("passed");
                }
                case "UI" -> {
                    Map<String, Object> r = execUi(config, ctx.vars, exec.getId());
                    step.setStatus((boolean) r.get("passed") ? "PASSED" : "FAILED");
                    step.setOutputSnapshot(snapshot(r.get("output")));
                    if (!(boolean) r.get("passed")) step.setErrorMessage(String.valueOf(r.get("error")));
                    result.passed = (boolean) r.get("passed");
                }
                case "ASSERT" -> {
                    boolean p = evaluateAssert(config, ctx.lastStatus, ctx.lastBody);
                    step.setStatus(p ? "PASSED" : "FAILED");
                    step.setOutputSnapshot("{\"passed\":" + p + "}");
                    if (!p) step.setErrorMessage("断言失败");
                    result.passed = p;
                }
                case "EXTRACT" -> {
                    applySingleExtract(config, ctx.vars, ctx.lastBody);
                    step.setStatus("PASSED");
                    result.passed = true;
                }
                case "CONDITION" -> {
                    boolean p = evaluateCondition(config, ctx.vars);
                    step.setStatus("PASSED"); // 条件节点本身是流程控制，不算失败
                    step.setOutputSnapshot("{\"result\":" + p + "}");
                    result.passed = true;
                    result.isCondition = true;
                    result.conditionResult = p;
                }
                default -> {
                    step.setStatus("FAILED");
                    step.setErrorMessage("未知节点类型: " + type);
                    result.passed = false;
                }
            }
        } catch (Exception e) {
            step.setStatus("FAILED");
            step.setErrorMessage(String.valueOf(e.getMessage()));
            result.passed = false;
        }

        step.setFinishedAt(LocalDateTime.now());
        step.setDurationMs(java.time.Duration.between(step.getStartedAt(), step.getFinishedAt()).toMillis());
        stepMapper.updateById(step);
        log(exec.getId(), step.getId(), step.getStatus().equals("PASSED") ? "INFO" : "ERROR",
                "节点 " + name + " " + step.getStatus() + (step.getErrorMessage() != null ? " - " + step.getErrorMessage() : ""));
        return result;
    }

    private Map<String, Object> execApi(JsonNode config, Map<String, Object> vars) throws Exception {
        String method = config.path("method").asText("GET").toUpperCase();
        String url = substitute(config.path("url").asText(), vars);
        String body = substitute(config.path("body").asText(""), vars);
        Map<String, Object> result = new HashMap<>();
        result.put("input", Map.of("method", method, "url", url, "body", body));

        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15));
        JsonNode headersNode = config.path("headers");
        boolean hasAuth = false;
        if (headersNode.isObject()) {
            headersNode.fields().forEachRemaining(e -> {
                String v = substitute(e.getValue().asText(), vars);
                b.header(e.getKey(), v);
            });
            hasAuth = headersNode.has("Authorization");
        }
        if (!hasAuth && vars.containsKey("token")) {
            b.header("Authorization", "Bearer " + vars.get("token"));
        }
        if (List.of("POST", "PUT", "PATCH").contains(method)) {
            b.header("Content-Type", "application/json");
            b.method(method, HttpRequest.BodyPublishers.ofString(body));
        } else {
            b.method(method, HttpRequest.BodyPublishers.noBody());
        }
        HttpResponse<String> resp = httpClient.send(b.build(), HttpResponse.BodyHandlers.ofString());
        result.put("status", resp.statusCode());
        result.put("body", resp.body());

        boolean passed = true;
        StringBuilder err = new StringBuilder();
        JsonNode assertions = config.path("assertions");
        if (assertions.isArray()) {
            for (JsonNode a : assertions) {
                String type = a.path("type").asText();
                String expect = a.path("expect").asText();
                boolean p = check(type, expect, resp.statusCode(), resp.body());
                if (!p) { passed = false; err.append("[").append(type).append("]期望").append(expect).append(" "); }
            }
        }
        result.put("passed", passed);
        result.put("error", err.toString());
        result.put("output", Map.of("status", resp.statusCode(), "body", truncate(resp.body())));
        return result;
    }

    private Map<String, Object> execUi(JsonNode config, Map<String, Object> vars, Long executionId) throws Exception {
        String baseUrl = config.path("baseUrl").asText();
        Map<String, Object> task = new HashMap<>();
        task.put("baseUrl", baseUrl);
        task.put("timeoutMs", config.path("timeoutMs").asInt(10000));
        task.put("browserType", config.path("browserType").asText("CHROMIUM"));
        task.put("steps", objectMapper.convertValue(config.path("steps"), List.class));
        if (config.has("retry")) task.put("retry", objectMapper.convertValue(config.path("retry"), Map.class));
        if (vars.containsKey("token")) {
            Map<String, Object> localStorage = new HashMap<>();
            localStorage.put("token", vars.get("token"));
            task.put("storageState", Map.of("localStorage", localStorage));
        }
        // 统一归档：artifacts/{executionId}/ui
        String outDir = new java.io.File(artifactDir, executionId + "/ui").getAbsolutePath();
        task.put("outputDir", outDir);

        Path tmp = Files.createTempFile("ui_task_", ".json");
        Files.writeString(tmp, objectMapper.writeValueAsString(task));

        ProcessBuilder pb = new ProcessBuilder(runnerPython, uiScript, "--task-file", tmp.toAbsolutePath().toString());
        pb.redirectErrorStream(true);
        Process p = pb.start();
        String out = new String(p.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        p.waitFor();

        Map<String, Object> result = new HashMap<>();
        try {
            JsonNode parsed = objectMapper.readTree(out);
            List<JsonNode> steps = new ArrayList<>();
            parsed.path("results").forEach(steps::add);
            boolean passed = steps.stream().allMatch(s -> "PASSED".equals(s.path("status").asText()));
            result.put("passed", passed);
            // 解析并归档 trace + 截图（转为可访问 URL）
            JsonNode artifacts = parsed.path("artifacts");
            Map<String, Object> arts = new HashMap<>();
            if (artifacts.isObject()) {
                String trace = artifacts.path("trace").asText("");
                if (!trace.isEmpty()) arts.put("trace", toArtifactUrl(trace));
                List<String> shots = new ArrayList<>();
                artifacts.path("screenshots").forEach(s -> shots.add(toArtifactUrl(s.asText())));
                arts.put("screenshots", shots);
            }
            Map<String, Object> output = new HashMap<>();
            output.put("passed", passed);
            output.put("artifacts", arts);
            output.put("output", truncate(out));
            result.put("output", output);
            if (!passed) result.put("error", out);
        } catch (Exception e) {
            result.put("passed", false);
            result.put("error", out);
            result.put("output", out);
        }
        return result;
    }

    /** 绝对路径 → /api/artifact/... 相对 URL（供前端查看/下载） */
    private String toArtifactUrl(String absPath) {
        try {
            String base = new java.io.File(artifactDir).getCanonicalPath().replace('\\', '/');
            String p = new java.io.File(absPath).getCanonicalPath().replace('\\', '/');
            if (p.startsWith(base)) {
                return "/api/artifact/" + p.substring(base.length() + 1);
            }
        } catch (Exception ignored) {
        }
        return absPath;
    }

    private boolean evaluateAssert(JsonNode config, int status, String body) {
        String type = config.path("type").asText();
        String expect = config.path("expect").asText();
        return check(type, expect, status, body);
    }

    private boolean evaluateCondition(JsonNode config, Map<String, Object> vars) {
        String left = String.valueOf(vars.getOrDefault(config.path("left").asText(), config.path("left").asText()));
        String op = config.path("op").asText("EQ");
        String right = config.path("right").asText();
        return switch (op) {
            case "EQ" -> left.equals(right);
            case "NE" -> !left.equals(right);
            case "CONTAINS" -> left.contains(right);
            case "GT" -> {
                try { yield Double.parseDouble(left) > Double.parseDouble(right); } catch (Exception e) { yield false; }
            }
            case "LT" -> {
                try { yield Double.parseDouble(left) < Double.parseDouble(right); } catch (Exception e) { yield false; }
            }
            case "EXISTS" -> !left.isBlank();
            default -> false;
        };
    }

    private boolean check(String type, String expect, int status, String body) {
        try {
            return switch (type) {
                case "httpStatus" -> String.valueOf(status).equals(expect);
                case "bodyCode" -> {
                    JsonNode n = objectMapper.readTree(body);
                    yield n.path("code").asInt() == Integer.parseInt(expect);
                }
                case "contains" -> body.contains(expect);
                default -> false;
            };
        } catch (Exception e) {
            return false;
        }
    }

    private void applyExtracts(JsonNode config, Map<String, Object> vars, String body) {
        if (body == null) return;
        JsonNode extracts = config.path("extracts");
        if (!extracts.isArray()) return;
        try {
            JsonNode root = objectMapper.readTree(body);
            for (JsonNode e : extracts) {
                String varName = e.path("varName").asText();
                String path = e.path("path").asText();
                JsonNode node = root;
                for (String seg : path.split("\\.")) {
                    if (node == null) break;
                    node = node.path(seg);
                }
                if (node != null && !node.isMissingNode()) {
                    vars.put(varName, node.isTextual() ? node.asText() : node.toString());
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void applySingleExtract(JsonNode config, Map<String, Object> vars, String body) {
        if (body == null) return;
        try {
            String varName = config.path("varName").asText();
            String path = config.path("path").asText();
            JsonNode root = objectMapper.readTree(body);
            JsonNode node = root;
            for (String seg : path.split("\\.")) {
                if (node == null) break;
                node = node.path(seg);
            }
            if (node != null && !node.isMissingNode()) {
                vars.put(varName, node.isTextual() ? node.asText() : node.toString());
            }
        } catch (Exception ignored) {
        }
    }

    private String substitute(String s, Map<String, Object> vars) {
        if (s == null) return "";
        for (Map.Entry<String, Object> e : vars.entrySet()) {
            s = s.replace("${" + e.getKey() + "}", String.valueOf(e.getValue()));
        }
        return s;
    }

    private String truncate(String s) {
        return s != null && s.length() > 500 ? s.substring(0, 500) : s;
    }

    private String snapshot(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (Exception e) {
            return "\"\"";
        }
    }

    private void log(Long executionId, Long stepId, String level, String message) {
        ExecutionLog l = new ExecutionLog();
        l.setExecutionId(executionId);
        l.setStepExecutionId(stepId);
        l.setLogLevel(level);
        l.setMessage(message);
        l.setCreatedAt(LocalDateTime.now());
        logMapper.insert(l);
    }

    private void fail(TestExecution exec, String message) {
        exec.setStatus("FAILED");
        exec.setFinishedAt(LocalDateTime.now());
        exec.setSummaryJson(objectMapper.valueToTree(Map.of("total", 0, "passed", 0, "failed", 1)).toString());
        executionMapper.updateById(exec);
    }

    /** 执行上下文：跨节点共享变量与最近一次响应 */
    private static class ExecContext {
        final Map<String, Object> vars = new HashMap<>();
        String lastBody;
        int lastStatus;
    }

    /** 节点执行结果 */
    private static class NodeResult {
        boolean passed;
        boolean isCondition;
        boolean conditionResult;
    }
}
