package com.autotest.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("test_execution")
public class TestExecution {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Long projectId;
    private String executionNo;
    private Long runnerId;
    private String status;
    private String versionSnapshotJson;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private Long durationMs;
    private String summaryJson;
    private LocalDateTime createdAt;
}
