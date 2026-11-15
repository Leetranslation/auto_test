package com.autotest.platform.controller;

import com.autotest.platform.common.Result;
import com.autotest.platform.entity.ScheduleJob;
import com.autotest.platform.service.ScheduleJobService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/schedule")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('schedule:manage')")
public class ScheduleJobController {

    private final ScheduleJobService scheduleJobService;

    @GetMapping("/list")
    public Result<List<ScheduleJob>> list(@RequestParam Long projectId) {
        return Result.success(scheduleJobService.list(projectId));
    }

    @PostMapping("/create")
    public Result<ScheduleJob> create(@RequestBody ScheduleJob job) {
        return Result.success(scheduleJobService.create(job));
    }

    @PostMapping("/update")
    public Result<ScheduleJob> update(@RequestBody ScheduleJob job) {
        return Result.success(scheduleJobService.update(job));
    }

    @PostMapping("/delete")
    public Result<Void> delete(@RequestParam Long id) {
        scheduleJobService.delete(id);
        return Result.success();
    }

    @PostMapping("/status")
    public Result<Void> status(@RequestParam Long id, @RequestParam Integer status) {
        scheduleJobService.changeStatus(id, status);
        return Result.success();
    }
}
