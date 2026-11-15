package com.autotest.platform.controller;

import com.autotest.platform.common.Result;
import com.autotest.platform.service.RecordConsumerService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/record")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('api:manage')")
public class RecordController {

    private final RecordConsumerService recordConsumerService;

    @PostMapping("/consume")
    public Result<Map<String, Object>> consume(@RequestBody Map<String, Object> body) {
        Long projectId = Long.valueOf(String.valueOf(body.get("projectId")));
        String filePath = body.get("filePath") == null ? null : String.valueOf(body.get("filePath"));
        return Result.success(recordConsumerService.consume(projectId, filePath));
    }
}
