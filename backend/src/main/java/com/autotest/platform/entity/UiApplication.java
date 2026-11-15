package com.autotest.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("ui_application")
public class UiApplication {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private String appName;
    private String baseUrl;
    private String browserType;
    private String viewportJson;
    private Integer defaultTimeoutMs;
    private Integer status;
}
