package com.autotest.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("ui_locator")
public class UiLocator {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long pageObjectId;
    private String locatorName;
    private String strategy;
    private String locatorValue;
    private String description;
}
