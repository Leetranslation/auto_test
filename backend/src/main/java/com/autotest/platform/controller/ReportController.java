package com.autotest.platform.controller;

import com.autotest.platform.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/report")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('report:view')")
public class ReportController {

    private final ReportService reportService;

    /** 导出 HTML 报告（浏览器打开/打印即可转 PDF） */
    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@RequestParam Long executionId) {
        String html = reportService.exportHtml(executionId);
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=report_" + executionId + ".html")
                .contentType(new MediaType("text", "html", StandardCharsets.UTF_8))
                .body(bytes);
    }
}
