package com.autotest.platform.service;

import com.autotest.platform.entity.ApiDefinition;
import com.autotest.platform.entity.ApiDefinitionVersion;
import com.autotest.platform.entity.ApiService;
import com.autotest.platform.entity.TestCase;
import com.autotest.platform.mapper.ApiDefinitionMapper;
import com.autotest.platform.mapper.ApiDefinitionVersionMapper;
import com.autotest.platform.mapper.ApiServiceMapper;
import com.autotest.platform.mapper.TestCaseMapper;
import com.autotest.platform.service.TestCaseService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecordConsumerService {

    private final ApiServiceMapper serviceMapper;
    private final ApiDefinitionMapper definitionMapper;
    private final ApiDefinitionVersionMapper versionMapper;
    private final TestCaseMapper caseMapper;
    private final TestCaseService testCaseService;
    private final ObjectMapper objectMapper;

    @Value("${platform.record.file:D:/erp/_record/record.jsonl}")
    private String defaultRecordFile;

    public Map<String, Object> consume(Long projectId, String filePath) {
        String fp = filePath == null || filePath.isBlank() ? defaultRecordFile : filePath;
        int consumed = 0;
        int newDefinitions = 0;
        int newCases = 0;

        // 找到或创建"流量录制导入"服务
        ApiService service = serviceMapper.selectOne(new LambdaQueryWrapper<ApiService>()
                .eq(ApiService::getProjectId, projectId)
                .eq(ApiService::getServiceName, "流量录制导入"));
        if (service == null) {
            service = new ApiService();
            service.setProjectId(projectId);
            service.setServiceName("流量录制导入");
            serviceMapper.insert(service);
        }

        try {
            for (String line : Files.readAllLines(Path.of(fp))) {
                if (line.isBlank()) continue;
                consumed++;
                JsonNode evt = objectMapper.readTree(line);
                String method = evt.path("method").asText();
                String path = evt.path("path").asText();
                if (path.contains("?")) path = path.substring(0, path.indexOf("?"));
                if (method.isEmpty() || path.isEmpty()) continue;
                String apiName = ApiNameResolver.resolve(method, path);

                String reqBody = redact(evt.path("requestBody").asText());
                String resBody = redact(evt.path("responseBody").asText());

                ApiDefinition def = definitionMapper.selectOne(new LambdaQueryWrapper<ApiDefinition>()
                        .eq(ApiDefinition::getProjectId, projectId)
                        .eq(ApiDefinition::getHttpMethod, method)
                        .eq(ApiDefinition::getPath, path));
                if (def == null) {
                    def = new ApiDefinition();
                    def.setProjectId(projectId);
                    def.setServiceId(service.getId());
                    def.setApiName(apiName);
                    def.setHttpMethod(method);
                    def.setPath(path);
                    def.setAuthType("INHERIT");
                    def.setTimeoutMs(10000);
                    def.setStatus(1);
                    definitionMapper.insert(def);
                    newDefinitions++;
                } else if (!apiName.equals(def.getApiName())) {
                    // 已存在但名称为旧的裸路径，用业务名纠正
                    def.setApiName(apiName);
                    definitionMapper.updateById(def);
                }

                long verCount = versionMapper.selectCount(new LambdaQueryWrapper<ApiDefinitionVersion>()
                        .eq(ApiDefinitionVersion::getApiDefinitionId, def.getId()));
                if (verCount == 0) {
                    ApiDefinitionVersion v = new ApiDefinitionVersion();
                    v.setApiDefinitionId(def.getId());
                    v.setVersionNo(1);
                    v.setRequestSchema(reqBody);
                    v.setResponseSchema(resBody);
                    versionMapper.insert(v);
                }

                long caseCount = caseMapper.selectCount(new LambdaQueryWrapper<TestCase>()
                        .eq(TestCase::getApiDefinitionId, def.getId()));
                if (caseCount == 0) {
                    TestCase c = new TestCase();
                    c.setProjectId(projectId);
                    c.setCaseName(apiName);
                    c.setCaseType("API");
                    c.setPriority("P2");
                    c.setApiDefinitionId(def.getId());
                    String replay = objectMapper.writeValueAsString(Map.of(
                            "method", method, "path", path,
                            "body", reqBody,
                            "assertions", java.util.List.of(Map.of("type", "httpStatus", "expect", evt.path("status").asInt()))));
                    testCaseService.create(c, replay);
                    newCases++;
                }
            }
        } catch (Exception e) {
            log.error("录制消费失败", e);
            throw new RuntimeException("录制消费失败: " + e.getMessage());
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("consumed", consumed);
        result.put("newDefinitions", newDefinitions);
        result.put("newCases", newCases);
        return result;
    }

    /** 脱敏：递归替换含敏感词 key 的值，并移除 Authorization */
    private String redact(String json) {
        if (json == null || json.isBlank()) return "{}";
        try {
            JsonNode node = objectMapper.readTree(json);
            JsonNode redacted = redactNode(node);
            return objectMapper.writeValueAsString(redacted);
        } catch (Exception e) {
            // 非 JSON 文本：脱敏后包成 JSON 字符串，保证列值合法
            try {
                return objectMapper.writeValueAsString(maskText(json));
            } catch (Exception ex) {
                return "\"***\"";
            }
        }
    }

    private JsonNode redactNode(JsonNode node) {
        if (node.isObject()) {
            ObjectNode obj = objectMapper.createObjectNode();
            node.fields().forEachRemaining(e -> {
                String key = e.getKey().toLowerCase();
                if (key.contains("password") || key.contains("token") || key.contains("secret")
                        || key.contains("pwd") || key.equals("authorization")) {
                    obj.put(e.getKey(), "***");
                } else {
                    obj.set(e.getKey(), redactNode(e.getValue()));
                }
            });
            return obj;
        }
        if (node.isArray()) {
            var arr = objectMapper.createArrayNode();
            node.forEach(n -> arr.add(redactNode(n)));
            return arr;
        }
        return node;
    }

    private String maskText(String s) {
        return s.replaceAll("(?i)(\"?(?:password|token|secret|pwd|authorization)\"?\\s*[:=]\\s*\"?)[^,\"}\\s]+", "$1***");
    }
}
