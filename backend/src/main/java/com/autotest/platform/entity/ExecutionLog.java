package com.autotest.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("execution_log")
public class ExecutionLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long executionId;
    private Long stepExecutionId;
    private String logLevel;
    private String message;
    private LocalDateTime createdAt;
}
