package com.autotest.platform.service;

import com.autotest.platform.common.BizException;
import com.autotest.platform.entity.ScheduleJob;
import com.autotest.platform.mapper.ScheduleJobMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScheduleJobService {

    private final ScheduleJobMapper jobMapper;

    public List<ScheduleJob> list(Long projectId) {
        return jobMapper.selectList(new LambdaQueryWrapper<ScheduleJob>()
                .eq(ScheduleJob::getProjectId, projectId).orderByDesc(ScheduleJob::getId));
    }

    public ScheduleJob create(ScheduleJob job) {
        if (!StringUtils.hasText(job.getJobName())) throw new BizException("任务名不能为空");
        if (job.getTargetId() == null) throw new BizException("目标不能为空");
        job.setId(null);
        job.setTargetType(StringUtils.hasText(job.getTargetType()) ? job.getTargetType() : "SCENE");
        job.setConcurrencyPolicy(StringUtils.hasText(job.getConcurrencyPolicy()) ? job.getConcurrencyPolicy() : "SKIP");
        job.setCronExpr(StringUtils.hasText(job.getCronExpr()) ? job.getCronExpr() : "0 0 0 * * ?");
        job.setTimezone(StringUtils.hasText(job.getTimezone()) ? job.getTimezone() : "Asia/Shanghai");
        job.setStatus(job.getStatus() == null ? 1 : job.getStatus());
        // 未指定下次运行时间则默认 1 分钟后
        job.setNextRunAt(job.getNextRunAt() == null ? LocalDateTime.now().plusMinutes(1) : job.getNextRunAt());
        jobMapper.insert(job);
        return job;
    }

    public ScheduleJob update(ScheduleJob job) {
        if (jobMapper.selectById(job.getId()) == null) throw new BizException(404, "任务不存在");
        jobMapper.updateById(job);
        return jobMapper.selectById(job.getId());
    }

    public void delete(Long id) {
        jobMapper.deleteById(id);
    }

    public void changeStatus(Long id, Integer status) {
        ScheduleJob job = jobMapper.selectById(id);
        if (job == null) throw new BizException(404, "任务不存在");
        job.setStatus(status);
        jobMapper.updateById(job);
    }

    /** 到期待执行的任务 */
    public List<ScheduleJob> dueJobs() {
        return jobMapper.selectList(new LambdaQueryWrapper<ScheduleJob>()
                .eq(ScheduleJob::getStatus, 1)
                .le(ScheduleJob::getNextRunAt, LocalDateTime.now()));
    }
}
