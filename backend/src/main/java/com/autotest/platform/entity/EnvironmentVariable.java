package com.autotest.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("environment_variable")
public class EnvironmentVariable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private Long environmentId;
    private String varKey;
    private String valueCiphertext;
    private String valueType;
    private Integer isSecret;
    private LocalDateTime updatedAt;
}
