package com.autotest.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("api_definition_version")
public class ApiDefinitionVersion {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long apiDefinitionId;
    private Integer versionNo;
    private String requestSchema;
    private String responseSchema;
    private String headersJson;
    private String queryParamsJson;
    private String pathParamsJson;
    private String bodyTemplate;
    private LocalDateTime createdAt;
}
