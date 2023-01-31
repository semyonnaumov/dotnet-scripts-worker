package com.naumov.dotnetscriptsworker.dto.mapper.impl;

import com.naumov.dotnetscriptsworker.dto.JobTaskDto;
import com.naumov.dotnetscriptsworker.dto.mapper.DtoMapper;
import com.naumov.dotnetscriptsworker.model.JobTask;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class JobTaskFromDtoMapper implements DtoMapper<JobTaskDto, JobTask> {

    @Override
    public JobTask map(JobTaskDto jobTaskDto) {
        Objects.requireNonNull(jobTaskDto, "Parameter jobTaskDto must not be null");
        JobTask jobTask = new JobTask(jobTaskDto.getJobId());
        jobTask.setJobScript(jobTaskDto.getScript());
        jobTask.setJobConfig(mapJobConfig(jobTaskDto.getJobConfig()));

        return jobTask;
    }

    private JobTask.JobConfig mapJobConfig(JobTaskDto.JobConfigDto jobConfigDto) {
        if (jobConfigDto == null) return null;
        JobTask.JobConfig jobConfig = new JobTask.JobConfig();
        jobConfig.setNugetConfig(jobConfigDto.getNugetConfig());
        return jobConfig;
    }
}