package com.autotest.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("schedule_job")
public class ScheduleJob {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private String jobName;
    private String targetType;
    private Long targetId;
    private Integer targetVersion;
    private Long environmentId;
    private String cronExpr;
    private String timezone;
    private String concurrencyPolicy;
    private LocalDateTime nextRunAt;
    private LocalDateTime lastRunAt;
    private Integer status;
}
