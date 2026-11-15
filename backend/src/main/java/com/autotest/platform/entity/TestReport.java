package com.autotest.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("test_report")
public class TestReport {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long executionId;
    private String reportJson;
    private String htmlUri;
    private LocalDateTime createdAt;
}
