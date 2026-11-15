package com.autotest.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("test_environment")
public class TestEnvironment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private String envCode;
    private String envName;
    private String envType;
    private String envLevel;
    private String baseUrl;
    private String description;
    private Integer isDefault;
    private Integer status;
    private LocalDateTime createdAt;
}
