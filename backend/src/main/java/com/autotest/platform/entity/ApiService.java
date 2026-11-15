package com.autotest.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("api_service")
public class ApiService {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private String serviceName;
    private String description;
    private LocalDateTime createdAt;
}
