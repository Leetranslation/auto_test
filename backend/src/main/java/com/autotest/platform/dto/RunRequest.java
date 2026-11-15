package com.autotest.platform.dto;

import lombok.Data;

@Data
public class RunRequest {
    private String targetType;   // SCENE / CASE
    private Long targetId;
    private Long projectId;
    private Long environmentId;
}
