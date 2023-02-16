package com.naumov.dotnetscriptsworker.dto;

import com.naumov.dotnetscriptsworker.dto.cons.JobConfig;
import com.naumov.dotnetscriptsworker.dto.cons.JobTaskMessage;
import com.naumov.dotnetscriptsworker.dto.prod.*;
import com.naumov.dotnetscriptsworker.model.JobResults;
import com.naumov.dotnetscriptsworker.model.JobTask;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class KafkaDtoMapperTest {
    private final KafkaDtoMapper kafkaDtoMapper = new KafkaDtoMapper();

    @Test
    void fromJobTaskMessageRegular() {
        JobConfig jobConfig = JobConfig.builder()
                .nugetConfigXml("<config />")
                .build();

        UUID jobId = UUID.randomUUID();
        JobTaskMessage jobTaskMessage = JobTaskMessage.builder()
                .jobId(jobId)
                .script("script")
                .jobConfig(jobConfig)
                .build();

        UUID messageId = UUID.randomUUID();

        JobTask jobTask = kafkaDtoMapper.fromJobTaskMessage(jobTaskMessage, messageId);
        assertNotNull(jobTask);
        assertEquals(jobId, jobTask.getJobId());
        assertEquals(messageId, jobTask.getMessageId());
        assertEquals("script", jobTask.getJobScript());
        assertNotNull(jobTask.getJobConfig());
        assertEquals("<config />", jobTask.getJobConfig().getNugetConfigXml());
    }

    @Test
    void fromJobTaskMessageIncomplete() {
        UUID jobId = UUID.randomUUID();
        JobTaskMessage jobTaskMessage = JobTaskMessage.builder()
                .jobId(jobId)
                .script("script")
                .jobConfig(null)
                .build();

        UUID messageId = UUID.randomUUID();

        JobTask jobTask = kafkaDtoMapper.fromJobTaskMessage(jobTaskMessage, messageId);
        assertNotNull(jobTask);
        assertEquals(jobId, jobTask.getJobId());
        assertEquals(messageId, jobTask.getMessageId());
        assertEquals("script", jobTask.getJobScript());
        assertNull(jobTask.getJobConfig());
    }

    @Test
    void fromJobTaskMessageNull() {
        assertNull(kafkaDtoMapper.fromJobTaskMessage(null, null));
    }

    @Test
    void toJobStartedMessageRegular() {
        UUID jobId = UUID.randomUUID();
        JobStartedMessage jobStartedMessage = kafkaDtoMapper.toJobStartedMessage(jobId);
        assertNotNull(jobStartedMessage);
        assertEquals(jobId, jobStartedMessage.getJobId());
    }

    @Test
    void toJobStartedMessageNull() {
        assertNull(kafkaDtoMapper.toJobStartedMessage(null));
    }

    @Test
    void toJobFinishedMessageRegular() {
        JobResults.ScriptResults scriptResults = JobResults.ScriptResults.builder()
                .finishedWith(JobResults.ScriptResults.JobCompletionStatus.SUCCEEDED)
                .stdout("some stdout")
                .stderr("some stderr")
                .build();

        UUID jobId = UUID.randomUUID();
        JobResults jobResults = JobResults.builder()
                .jobId(jobId)
                .status(JobResults.Status.ACCEPTED)
                .scriptResults(scriptResults)
                .build();

        JobFinishedMessage jobFinishedMessage = kafkaDtoMapper.toJobFinishedMessage(jobResults);
        assertNotNull(jobFinishedMessage);
        assertEquals(jobId, jobFinishedMessage.getJobId());
        assertEquals(JobStatus.ACCEPTED, jobFinishedMessage.getStatus());
        ScriptResults jobFinishedMessageScriptResults = jobFinishedMessage.getScriptResults();
        assertNotNull(jobFinishedMessageScriptResults);
        assertEquals(JobCompletionStatus.SUCCEEDED,
                jobFinishedMessageScriptResults.getFinishedWith());
        assertEquals("some stdout", jobFinishedMessageScriptResults.getStdout());
        assertEquals("some stderr", jobFinishedMessageScriptResults.getStderr());
    }

    @Test
    void toJobFinishedMessageIncomplete() {
        UUID jobId = UUID.randomUUID();
        JobResults jobResults = JobResults.builder()
                .jobId(jobId)
                .status(JobResults.Status.ACCEPTED)
                .scriptResults(null)
                .build();

        JobFinishedMessage jobFinishedMessage = kafkaDtoMapper.toJobFinishedMessage(jobResults);
        assertNotNull(jobFinishedMessage);
        assertEquals(jobId, jobFinishedMessage.getJobId());
        assertEquals(JobStatus.ACCEPTED, jobFinishedMessage.getStatus());
        ScriptResults jobFinishedMessageScriptResults = jobFinishedMessage.getScriptResults();
        assertNull(jobFinishedMessageScriptResults);
    }

    @Test
    void toJobFinishedMessageNull() {
        assertNull(kafkaDtoMapper.toJobFinishedMessage(null));
    }
}