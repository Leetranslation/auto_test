package com.autotest.platform.service;

import com.autotest.platform.common.BizException;
import com.autotest.platform.entity.Project;
import com.autotest.platform.mapper.ProjectMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectMapper projectMapper;

    public Page<Project> page(long page, long pageSize, String keyword, Integer status) {
        LambdaQueryWrapper<Project> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like(Project::getProjectName, keyword).or().like(Project::getProjectCode, keyword));
        }
        if (status != null) qw.eq(Project::getStatus, status);
        qw.orderByDesc(Project::getId);
        return projectMapper.selectPage(new Page<>(page, pageSize), qw);
    }

    public Project create(Project project) {
        if (!StringUtils.hasText(project.getProjectCode())) throw new BizException("项目编码不能为空");
        if (!StringUtils.hasText(project.getProjectName())) throw new BizException("项目名称不能为空");
        Long count = projectMapper.selectCount(
                new LambdaQueryWrapper<Project>().eq(Project::getProjectCode, project.getProjectCode()));
        if (count > 0) throw new BizException("项目编码已存在");
        project.setId(null);
        project.setProjectType(StringUtils.hasText(project.getProjectType()) ? project.getProjectType() : "MIXED");
        project.setTimezone(StringUtils.hasText(project.getTimezone()) ? project.getTimezone() : "Asia/Shanghai");
        project.setStatus(project.getStatus() == null ? 1 : project.getStatus());
        projectMapper.insert(project);
        return project;
    }

    public Project update(Project project) {
        if (projectMapper.selectById(project.getId()) == null) throw new BizException(404, "项目不存在");
        projectMapper.updateById(project);
        return projectMapper.selectById(project.getId());
    }

    public void changeStatus(Long id, Integer status) {
        Project project = projectMapper.selectById(id);
        if (project == null) throw new BizException(404, "项目不存在");
        project.setStatus(status);
        projectMapper.updateById(project);
    }

    public void delete(Long id) {
        Project project = projectMapper.selectById(id);
        if (project == null) throw new BizException(404, "项目不存在");
        // 逻辑删除（@TableLogic）
        projectMapper.deleteById(id);
    }
}
