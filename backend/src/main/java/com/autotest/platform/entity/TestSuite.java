package com.autotest.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("test_suite")
public class TestSuite {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private String suiteName;
    private String executionPolicy;
    private Integer status;
    private Integer currentVersion;
}
