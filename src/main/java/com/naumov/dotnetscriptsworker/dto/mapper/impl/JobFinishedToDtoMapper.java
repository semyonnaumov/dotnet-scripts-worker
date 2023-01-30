package com.naumov.dotnetscriptsworker.dto.mapper.impl;

import com.naumov.dotnetscriptsworker.dto.JobFinishedDto;
import com.naumov.dotnetscriptsworker.dto.mapper.DtoMapper;
import com.naumov.dotnetscriptsworker.model.JobResults;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class JobFinishedToDtoMapper implements DtoMapper<JobResults, JobFinishedDto> {

    @Override
    public JobFinishedDto map(JobResults jobResults) {
        Objects.requireNonNull(jobResults, "Parameter jobResults must not be null");
        JobFinishedDto jobFinishedDto = new JobFinishedDto(jobResults.getJobId());
        jobFinishedDto.setStatus(mapJobStatus(jobResults.getStatus()));
        jobFinishedDto.setScriptResults(mapScriptResults(jobResults.getScriptResults()));

        return jobFinishedDto;
    }

    private JobFinishedDto.Status mapJobStatus(JobResults.Status status) {
        if (status == null) return null;
        return JobFinishedDto.Status.valueOf(status.name());
    }

    private JobFinishedDto.ScriptResults mapScriptResults(JobResults.ScriptResults scriptResults) {
        if (scriptResults == null) return null;
        JobFinishedDto.ScriptResults results = new JobFinishedDto.ScriptResults();
        results.setFinishedWith(mapScriptResultsStatus(scriptResults.getFinishedWith()));
        results.setStdout(scriptResults.getStdout());
        results.setStderr(scriptResults.getStderr());
        return results;
    }

    private JobFinishedDto.ScriptResults.Status mapScriptResultsStatus(JobResults.ScriptResults.Status finishedWith) {
        if (finishedWith == null) return null;
        return JobFinishedDto.ScriptResults.Status.valueOf(finishedWith.name());
    }
}
