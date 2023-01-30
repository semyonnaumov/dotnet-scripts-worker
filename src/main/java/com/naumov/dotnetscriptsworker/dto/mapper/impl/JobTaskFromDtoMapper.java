package com.naumov.dotnetscriptsworker.dto.mapper.impl;

import com.naumov.dotnetscriptsworker.dto.JobTaskDto;
import com.naumov.dotnetscriptsworker.dto.mapper.DtoMapper;
import com.naumov.dotnetscriptsworker.model.JobTask;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Component
public class JobTaskFromDtoMapper implements DtoMapper<JobTaskDto, JobTask> {

    @Override
    public JobTask map(JobTaskDto jobTaskDto) {
        Objects.requireNonNull(jobTaskDto, "Parameter jobTaskDto must not be null");
        JobTask jobTask = new JobTask(jobTaskDto.getJobId());
        jobTask.setJobScript(jobTaskDto.getScript());
        Map<String, String> dtoJobConfig = jobTaskDto.getJobConfig();
        if (dtoJobConfig != null) {
            HashMap<String, String> jobConfigs = new HashMap<>();
            dtoJobConfig.putAll(jobConfigs);
            jobTask.setJobConfig(jobConfigs);
        }

        return jobTask;
    }
}