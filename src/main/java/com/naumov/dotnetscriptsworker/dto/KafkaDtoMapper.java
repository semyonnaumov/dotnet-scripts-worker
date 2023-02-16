package com.naumov.dotnetscriptsworker.dto;

import com.naumov.dotnetscriptsworker.dto.cons.JobConfig;
import com.naumov.dotnetscriptsworker.dto.cons.JobTaskMessage;
import com.naumov.dotnetscriptsworker.dto.prod.*;
import com.naumov.dotnetscriptsworker.model.JobResults;
import com.naumov.dotnetscriptsworker.model.JobTask;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public final class KafkaDtoMapper {

    // -------------------------------------------- "From" mappings ------------------------------------------------- //
    public JobTask fromJobTaskMessage(JobTaskMessage jobTaskMessage, UUID messageId) {
        if (jobTaskMessage == null) return null;

        return JobTask.builder()
                .jobId(jobTaskMessage.getJobId())
                .messageId(messageId)
                .jobScript(jobTaskMessage.getScript())
                .jobConfig(fromJobConfig(jobTaskMessage.getJobConfig()))
                .build();
    }

    private JobTask.JobConfig fromJobConfig(JobConfig jobConfigDto) {
        if (jobConfigDto == null) return null;

        return JobTask.JobConfig.builder()
                .nugetConfigXml(jobConfigDto.getNugetConfigXml())
                .build();
    }

    // -------------------------------------------- "To" mappings --------------------------------------------------- //
    public JobStartedMessage toJobStartedMessage(UUID jobId) {
        if (jobId == null) return null;

        return JobStartedMessage.builder()
                .jobId(jobId)
                .build();
    }

    public JobFinishedMessage toJobFinishedMessage(JobResults jobResults) {
        if (jobResults == null) return null;

        return JobFinishedMessage.builder()
                .jobId(jobResults.getJobId())
                .status(toJobFinishedMessageStatus(jobResults.getStatus()))
                .scriptResults(toJobFinishedMessageScriptResults(jobResults.getScriptResults()))
                .build();
    }

    private JobStatus toJobFinishedMessageStatus(JobResults.Status status) {
        if (status == null) return null;

        try {
            return JobStatus.valueOf(status.name());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Unable to map " + JobResults.Status.class.getName() + " to " +
                    JobStatus.class.getName() + " from value " + status);
        }
    }

    private ScriptResults toJobFinishedMessageScriptResults(JobResults.ScriptResults scriptResults) {
        if (scriptResults == null) return null;

        return ScriptResults.builder()
                .finishedWith(toJobFinishedMessageScriptResultsJobCompletionStatus(scriptResults.getFinishedWith()))
                .stdout(scriptResults.getStdout())
                .stderr(scriptResults.getStderr())
                .build();
    }

    private JobCompletionStatus toJobFinishedMessageScriptResultsJobCompletionStatus(
            JobResults.ScriptResults.JobCompletionStatus finishedWith
    ) {
        if (finishedWith == null) return null;

        try {
            return JobCompletionStatus.valueOf(finishedWith.name());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Unable to map " + JobResults.ScriptResults.JobCompletionStatus.class.getName() +
                    " to " +
                    JobCompletionStatus.class.getName() + " from value " + finishedWith);
        }
    }
}
