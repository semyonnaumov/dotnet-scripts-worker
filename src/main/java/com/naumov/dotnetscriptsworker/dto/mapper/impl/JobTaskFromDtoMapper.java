package com.naumov.dotnetscriptsworker.dto.mapper.impl;

import com.naumov.dotnetscriptsworker.dto.cons.JobTaskMessage;
import com.naumov.dotnetscriptsworker.dto.mapper.DtoMapper;
import com.naumov.dotnetscriptsworker.model.JobTask;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class JobTaskFromDtoMapper implements DtoMapper<JobTaskMessage, JobTask> {

    @Override
    public JobTask map(JobTaskMessage jobTaskMessage) {
        Objects.requireNonNull(jobTaskMessage, "Parameter jobTaskMessage must not be null");
        JobTask jobTask = new JobTask(jobTaskMessage.getJobId());
        jobTask.setJobScript(jobTaskMessage.getScript());
        jobTask.setJobConfig(mapJobConfig(jobTaskMessage.getJobConfig()));

        return jobTask;
    }

    private JobTask.JobConfig mapJobConfig(JobTaskMessage.JobConfig jobConfigDto) {
        if (jobConfigDto == null) return null;
        JobTask.JobConfig jobConfig = new JobTask.JobConfig();
        jobConfig.setNugetConfigXml(jobConfigDto.getNugetConfigXml());
        return jobConfig;
    }
}