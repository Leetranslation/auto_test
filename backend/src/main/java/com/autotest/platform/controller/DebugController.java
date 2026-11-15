package com.autotest.platform.controller;

import com.autotest.platform.common.Result;
import com.autotest.platform.dto.DebugRequest;
import com.autotest.platform.dto.DebugResponse;
import com.autotest.platform.service.DebugService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/debug")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('api:manage')")
public class DebugController {

    private final DebugService debugService;

    @PostMapping("/send")
    public Result<DebugResponse> send(@RequestBody DebugRequest request) {
        return Result.success(debugService.send(request));
    }
}
