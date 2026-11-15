package com.autotest.platform.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class DebugRequest {
    private String method;                 // GET/POST/PUT/PATCH/DELETE
    private String url;
    private Map<String, String> headers;
    private Map<String, String> queryParams;
    private String body;                   // 请求体字符串
    private String authType;               // NONE/BEARER/BASIC
    private String token;                  // Bearer token
    private Integer timeoutMs;
    private List<Map<String, Object>> assertions;   // {type,target,expect}
    private List<Map<String, Object>> extracts;     // {varName,path}
}
