package com.naumov.dotnetscriptsworker.dto.mapper.impl;

import com.naumov.dotnetscriptsworker.dto.prod.JobFinishedMessage;
import com.naumov.dotnetscriptsworker.dto.mapper.DtoMapper;
import com.naumov.dotnetscriptsworker.model.JobResults;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class JobFinishedToDtoMapper implements DtoMapper<JobResults, JobFinishedMessage> {

    @Override
    public JobFinishedMessage map(JobResults jobResults) {
        Objects.requireNonNull(jobResults, "Parameter jobResults must not be null");
        JobFinishedMessage jobFinishedMessage = new JobFinishedMessage(jobResults.getJobId());
        jobFinishedMessage.setStatus(mapJobStatus(jobResults.getStatus()));
        jobFinishedMessage.setScriptResults(mapScriptResults(jobResults.getScriptResults()));

        return jobFinishedMessage;
    }

    private JobFinishedMessage.Status mapJobStatus(JobResults.Status status) {
        if (status == null) return null;
        return JobFinishedMessage.Status.valueOf(status.name());
    }

    private JobFinishedMessage.ScriptResults mapScriptResults(JobResults.ScriptResults scriptResults) {
        if (scriptResults == null) return null;
        JobFinishedMessage.ScriptResults results = new JobFinishedMessage.ScriptResults();
        results.setFinishedWith(mapScriptResultsStatus(scriptResults.getFinishedWith()));
        results.setStdout(scriptResults.getStdout());
        results.setStderr(scriptResults.getStderr());
        return results;
    }

    private JobFinishedMessage.ScriptResults.JobCompletionStatus mapScriptResultsStatus(JobResults.ScriptResults.JobCompletionStatus finishedWith) {
        if (finishedWith == null) return null;
        return JobFinishedMessage.ScriptResults.JobCompletionStatus.valueOf(finishedWith.name());
    }
}
