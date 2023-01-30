package com.naumov.dotnetscriptsworker.dto.mapper.impl;

import com.naumov.dotnetscriptsworker.dto.JobStartedDto;
import com.naumov.dotnetscriptsworker.dto.mapper.DtoMapper;
import com.naumov.dotnetscriptsworker.model.Job;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class JobStartedDtoMapper implements DtoMapper<Job, JobStartedDto> {

    @Override
    public JobStartedDto map(Job job) {
        Objects.requireNonNull(job, "Parameter job must not be null");
        return new JobStartedDto(job.getJobId());
    }
}
