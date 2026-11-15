package com.autotest.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("test_case")
public class TestCase {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private Long moduleId;
    private String caseCode;
    private String caseName;
    private String caseType;
    private String priority;
    private String status;
    private String tagsJson;
    private Integer currentVersion;
    private Long apiDefinitionId;
    private Long uiPageObjectId;
    private LocalDateTime createdAt;

    /** 创建时的首版定义，不落库 */
    @TableField(exist = false)
    private String definitionJson;
}
