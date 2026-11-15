package com.autotest.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("test_scene")
public class TestScene {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private Long moduleId;
    private String sceneCode;
    private String sceneName;
    private String description;
    private String status;
    private Integer currentVersion;
}
