package com.autotest.platform.service;

import com.autotest.platform.dto.RunRequest;
import com.autotest.platform.entity.ScheduleJob;
import com.autotest.platform.mapper.ScheduleJobMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduleRunner {

    private final ScheduleJobService scheduleJobService;
    private final ScheduleJobMapper jobMapper;
    private final ExecutionService executionService;

    /** 每 60 秒扫描一次到期任务 */
    @Scheduled(fixedDelay = 60000, initialDelay = 10000)
    public void runDueJobs() {
        for (ScheduleJob job : scheduleJobService.dueJobs()) {
            try {
                RunRequest req = new RunRequest();
                req.setTargetType(job.getTargetType());
                req.setTargetId(job.getTargetId());
                req.setProjectId(job.getProjectId());
                req.setEnvironmentId(job.getEnvironmentId());
                executionService.submit(req);
                log.info("定时任务执行完成: {}", job.getJobName());
            } catch (Exception e) {
                log.error("定时任务执行失败: {}", job.getJobName(), e);
            } finally {
                job.setLastRunAt(LocalDateTime.now());
                job.setNextRunAt(LocalDateTime.now().plusDays(1)); // 简单日循环（cron 增强后续支持）
                jobMapper.updateById(job);
            }
        }
    }
}
