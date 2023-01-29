package com.naumov.dotnetscriptsworker.dto.mapper.impl;

import com.naumov.dotnetscriptsworker.dto.JobFinishedDto;
import com.naumov.dotnetscriptsworker.dto.mapper.DtoMapper;
import com.naumov.dotnetscriptsworker.model.JobResults;
import org.springframework.stereotype.Component;

@Component
public class JobFinishedDtoMapper implements DtoMapper<JobResults, JobFinishedDto> {

    @Override
    public JobFinishedDto map(JobResults entity) {
        // TODO
        return new JobFinishedDto();
    }
}
