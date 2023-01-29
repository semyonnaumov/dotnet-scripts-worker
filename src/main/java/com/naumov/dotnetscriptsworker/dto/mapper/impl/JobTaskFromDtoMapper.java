package com.naumov.dotnetscriptsworker.dto.mapper.impl;

import com.naumov.dotnetscriptsworker.dto.JobTaskDto;
import com.naumov.dotnetscriptsworker.dto.mapper.DtoMapper;
import com.naumov.dotnetscriptsworker.model.JobTask;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class JobTaskFromDtoMapper implements DtoMapper<JobTaskDto, JobTask> {

    @Override
    public JobTask map(JobTaskDto entity) {
        Objects.requireNonNull(entity, "Entity must not be null");
        JobTask jobTask = new JobTask(entity.getJobId());
        jobTask.setJobScript(entity.getScript());

        return jobTask;
    }
}