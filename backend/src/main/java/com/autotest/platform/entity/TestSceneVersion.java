package com.autotest.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("test_scene_version")
public class TestSceneVersion {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sceneId;
    private Integer versionNo;
    private String graphJson;
    private Integer published;
    private LocalDateTime createdAt;
}
