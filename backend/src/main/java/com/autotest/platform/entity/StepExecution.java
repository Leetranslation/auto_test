package com.autotest.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("step_execution")
public class StepExecution {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long executionId;
    private String nodeKey;
    private String stepName;
    private String stepType;
    private Integer sortNo;
    private Integer iterationNo;
    private Integer attemptNo;
    private String status;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private Long durationMs;
    private String inputSnapshot;
    private String outputSnapshot;
    private String errorMessage;
}
