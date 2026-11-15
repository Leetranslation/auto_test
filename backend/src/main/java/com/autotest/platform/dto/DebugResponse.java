package com.autotest.platform.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class DebugResponse {
    private int httpStatus;
    private long elapsedMs;
    private Map<String, String> responseHeaders;
    private String bodyText;
    private Object body;                   // 解析后的 JSON（若可解析）
    private List<Map<String, Object>> assertionResults;
    private Map<String, Object> extractedVars;
}
