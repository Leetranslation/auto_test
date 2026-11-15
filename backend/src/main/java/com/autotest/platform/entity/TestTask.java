package com.autotest.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("test_task")
public class TestTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private String taskName;
    private String targetType;
    private Long targetId;
    private Integer targetVersion;
    private Long environmentId;
    private String paramsJson;
    private Long createdBy;
    private LocalDateTime createdAt;
}
