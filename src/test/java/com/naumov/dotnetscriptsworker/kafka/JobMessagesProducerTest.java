package com.naumov.dotnetscriptsworker.kafka;

import com.naumov.dotnetscriptsworker.config.props.WorkerKafkaProperties;
import com.naumov.dotnetscriptsworker.dto.KafkaDtoMapper;
import com.naumov.dotnetscriptsworker.dto.prod.*;
import com.naumov.dotnetscriptsworker.model.JobResults;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class JobMessagesProducerTest {
    private static KafkaTemplate<String, JobStartedMessage> jobStartedKafkaTemplateMock;
    private static KafkaTemplate<String, JobFinishedMessage> jobFinishedKafkaTemplateMock;
    private static JobMessagesProducer jobMessagesProducer;

    @SuppressWarnings("unchecked")
    @BeforeAll
    static void setUp() {
        jobStartedKafkaTemplateMock = (KafkaTemplate<String, JobStartedMessage>) mock(KafkaTemplate.class);
        jobFinishedKafkaTemplateMock = (KafkaTemplate<String, JobFinishedMessage>) mock(KafkaTemplate.class);
        WorkerKafkaProperties workerKafkaPropertiesMock = mock(WorkerKafkaProperties.class);
        when(workerKafkaPropertiesMock.getRunningTopicName()).thenReturn("running-topic");
        when(workerKafkaPropertiesMock.getFinishedTopicName()).thenReturn("finished-topic");

        jobMessagesProducer = new JobMessagesProducer(
                workerKafkaPropertiesMock,
                jobStartedKafkaTemplateMock,
                jobFinishedKafkaTemplateMock,
                new KafkaDtoMapper()
        );

        when(jobStartedKafkaTemplateMock.send(any(), any(), any())).thenReturn(new CompletableFuture<>());
        when(jobFinishedKafkaTemplateMock.send(any(), any(), any())).thenReturn(new CompletableFuture<>());
    }

    @Test
    void sendJobStartedMessageAsync() {
        UUID jobId = UUID.randomUUID();

        jobMessagesProducer.sendJobStartedMessageAsync(jobId);

        ArgumentCaptor<JobStartedMessage> messageCaptor = ArgumentCaptor.forClass(JobStartedMessage.class);
        verify(jobStartedKafkaTemplateMock, times(1))
                .send(eq("running-topic"), eq(jobId.toString()), messageCaptor.capture());
        JobStartedMessage message = messageCaptor.getValue();
        assertNotNull(message);
        assertEquals(jobId, message.getJobId());
    }

    @Test
    void sendJobFinishedMessageAsync() {
        String stdout = "some stdout";
        String stderr = "some stderr";
        JobResults.ScriptResults scriptResults = JobResults.ScriptResults.builder()
                .finishedWith(JobResults.ScriptResults.JobCompletionStatus.SUCCEEDED)
                .stdout(stdout)
                .stderr(stderr)
                .build();

        UUID jobId = UUID.randomUUID();
        JobResults jobResults = JobResults.builder()
                .jobId(jobId)
                .status(JobResults.Status.ACCEPTED)
                .scriptResults(scriptResults)
                .build();

        jobMessagesProducer.sendJobFinishedMessageAsync(jobResults);

        ArgumentCaptor<JobFinishedMessage> jobMessageCaptor = ArgumentCaptor.forClass(JobFinishedMessage.class);
        verify(jobFinishedKafkaTemplateMock, times(1))
                .send(eq("finished-topic"), eq(jobId.toString()), jobMessageCaptor.capture());
        JobFinishedMessage jobFinishedMessage = jobMessageCaptor.getValue();
        assertNotNull(jobFinishedMessage);
        assertEquals(jobId, jobFinishedMessage.getJobId());
        assertEquals(JobStatus.ACCEPTED, jobFinishedMessage.getStatus());
        ScriptResults results = jobFinishedMessage.getScriptResults();
        assertNotNull(results);
        assertEquals(JobCompletionStatus.SUCCEEDED, results.getFinishedWith());
        assertEquals(stdout, results.getStdout());
        assertEquals(stderr, results.getStderr());
    }
}