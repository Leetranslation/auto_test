package com.autotest.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("test_case_version")
public class TestCaseVersion {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long testCaseId;
    private Integer versionNo;
    private String definitionJson;
    private Integer published;
    private LocalDateTime createdAt;
}
