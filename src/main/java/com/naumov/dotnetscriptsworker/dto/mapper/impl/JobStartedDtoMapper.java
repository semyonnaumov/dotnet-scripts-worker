package com.naumov.dotnetscriptsworker.dto.mapper.impl;

import com.naumov.dotnetscriptsworker.dto.JobStartedDto;
import com.naumov.dotnetscriptsworker.dto.mapper.DtoMapper;
import com.naumov.dotnetscriptsworker.model.Job;
import org.springframework.stereotype.Component;

@Component
public class JobStartedDtoMapper implements DtoMapper<Job, JobStartedDto> {

    @Override
    public JobStartedDto map(Job job) {
        return new JobStartedDto(job.getJobId());
    }
}
