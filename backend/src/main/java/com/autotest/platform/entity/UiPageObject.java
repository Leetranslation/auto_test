package com.autotest.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("ui_page_object")
public class UiPageObject {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private Long applicationId;
    private String pageName;
    private String urlPattern;
    private String description;
}
