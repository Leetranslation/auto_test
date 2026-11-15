package com.autotest.platform.service;

import com.autotest.platform.dto.DebugRequest;
import com.autotest.platform.dto.DebugResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DebugService {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public DebugResponse send(DebugRequest request) {
        DebugResponse resp = new DebugResponse();
        long t0 = System.currentTimeMillis();
        try {
            String url = buildUrl(request.getUrl(), request.getQueryParams());
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMillis(request.getTimeoutMs() == null ? 10000 : request.getTimeoutMs()));

            // headers
            if (request.getHeaders() != null) {
                request.getHeaders().forEach(builder::header);
            }
            // auth
            if ("BEARER".equalsIgnoreCase(request.getAuthType()) && StringUtils.hasText(request.getToken())) {
                builder.header("Authorization", "Bearer " + request.getToken());
            }

            String method = request.getMethod() == null ? "GET" : request.getMethod().toUpperCase();
            if (List.of("POST", "PUT", "PATCH").contains(method)) {
                String body = request.getBody() == null ? "" : request.getBody();
                builder.header("Content-Type", "application/json");
                builder.method(method, HttpRequest.BodyPublishers.ofString(body));
            } else {
                builder.method(method, HttpRequest.BodyPublishers.noBody());
            }

            HttpResponse<String> httpResp = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            resp.setHttpStatus(httpResp.statusCode());
            resp.setElapsedMs(System.currentTimeMillis() - t0);
            resp.setBodyText(httpResp.body());

            Map<String, String> headers = new HashMap<>();
            httpResp.headers().map().forEach((k, v) -> headers.put(k, String.join(",", v)));
            resp.setResponseHeaders(headers);

            resp.setBody(parseBody(httpResp.body()));
            resp.setAssertionResults(evaluate(request.getAssertions(), resp, httpResp.body()));
            resp.setExtractedVars(extract(request.getExtracts(), httpResp.body()));
        } catch (Exception e) {
            resp.setHttpStatus(0);
            resp.setElapsedMs(System.currentTimeMillis() - t0);
            resp.setBodyText("请求失败: " + e.getMessage());
        }
        return resp;
    }

    private String buildUrl(String url, Map<String, String> queryParams) {
        if (queryParams == null || queryParams.isEmpty()) {
            return url;
        }
        StringBuilder sb = new StringBuilder(url);
        sb.append(url.contains("?") ? "&" : "?");
        queryParams.forEach((k, v) -> sb.append(k).append("=").append(v).append("&"));
        return sb.substring(0, sb.length() - 1);
    }

    private Object parseBody(String body) {
        try {
            JsonNode node = objectMapper.readTree(body);
            return node;
        } catch (Exception e) {
            return null;
        }
    }

    private List<Map<String, Object>> evaluate(List<Map<String, Object>> assertions, DebugResponse resp, String bodyText) {
        List<Map<String, Object>> results = new ArrayList<>();
        if (assertions == null) {
            return results;
        }
        for (Map<String, Object> a : assertions) {
            String type = String.valueOf(a.get("type"));
            Object expect = a.get("expect");
            Object actual = null;
            boolean passed = false;
            try {
                switch (type) {
                    case "httpStatus" -> {
                        actual = resp.getHttpStatus();
                        passed = compareNumber(actual, expect);
                    }
                    case "bodyCode" -> {
                        JsonNode node = objectMapper.readTree(bodyText);
                        actual = node.path("code").asInt();
                        passed = compareNumber(actual, expect);
                    }
                    case "contains" -> {
                        actual = bodyText.contains(String.valueOf(expect));
                        passed = (boolean) actual;
                    }
                    case "elapsedMs" -> {
                        actual = resp.getElapsedMs();
                        passed = resp.getElapsedMs() <= toLong(expect);
                    }
                    default -> {
                        actual = "unsupported";
                    }
                }
            } catch (Exception e) {
                actual = "error: " + e.getMessage();
            }
            Map<String, Object> r = new HashMap<>();
            r.put("type", type);
            r.put("expect", expect);
            r.put("actual", actual);
            r.put("passed", passed);
            results.add(r);
        }
        return results;
    }

    private boolean compareNumber(Object actual, Object expect) {
        try {
            return Double.parseDouble(String.valueOf(actual)) == Double.parseDouble(String.valueOf(expect));
        } catch (Exception e) {
            return false;
        }
    }

    private long toLong(Object v) {
        return (long) Double.parseDouble(String.valueOf(v));
    }

    private Map<String, Object> extract(List<Map<String, Object>> extracts, String bodyText) {
        Map<String, Object> vars = new HashMap<>();
        if (extracts == null) {
            return vars;
        }
        try {
            JsonNode root = objectMapper.readTree(bodyText);
            for (Map<String, Object> e : extracts) {
                String varName = String.valueOf(e.get("varName"));
                String path = String.valueOf(e.get("path"));
                JsonNode node = root;
                for (String seg : path.split("\\.")) {
                    if (node == null) break;
                    node = node.path(seg);
                }
                vars.put(varName, node == null || node.isMissingNode() ? null : objectMapper.convertValue(node, Object.class));
            }
        } catch (Exception ignored) {
        }
        return vars;
    }
}
