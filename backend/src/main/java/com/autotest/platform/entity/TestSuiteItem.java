package com.autotest.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("test_suite_item")
public class TestSuiteItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long suiteId;
    private String targetType;
    private Long targetId;
    private Integer targetVersion;
    private Integer sortNo;
    private Integer enabled;
}
