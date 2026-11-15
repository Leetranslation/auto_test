package com.autotest.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("api_definition")
public class ApiDefinition {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private Long serviceId;
    private Long moduleId;
    private String apiName;
    private String httpMethod;
    private String path;
    private String authType;
    private Integer timeoutMs;
    private Integer status;
    private LocalDateTime createdAt;
}
