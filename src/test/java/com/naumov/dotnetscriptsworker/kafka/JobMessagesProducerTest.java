package com.naumov.dotnetscriptsworker.kafka;

import com.naumov.dotnetscriptsworker.config.props.WorkerKafkaProperties;
import com.naumov.dotnetscriptsworker.dto.KafkaDtoMapper;
import com.naumov.dotnetscriptsworker.dto.prod.JobFinishedMessage;
import com.naumov.dotnetscriptsworker.dto.prod.JobStartedMessage;
import com.naumov.dotnetscriptsworker.model.JobResults;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class JobMessagesProducerTest {
    private static KafkaTemplate<String, JobStartedMessage> jobStartedKafkaTemplateMock;
    private static KafkaTemplate<String, JobFinishedMessage> jobFinishedKafkaTemplate;
    private static JobMessagesProducer jobMessagesProducer;

    @SuppressWarnings("unchecked")
    @BeforeAll
    static void setUp() {
        jobStartedKafkaTemplateMock = (KafkaTemplate<String, JobStartedMessage>) mock(KafkaTemplate.class);
        jobFinishedKafkaTemplate = (KafkaTemplate<String, JobFinishedMessage>) mock(KafkaTemplate.class);
        WorkerKafkaProperties workerKafkaPropertiesMock = mock(WorkerKafkaProperties.class);
        when(workerKafkaPropertiesMock.getRunningTopicName()).thenReturn("running-topic");
        when(workerKafkaPropertiesMock.getFinishedTopicName()).thenReturn("finished-topic");

        jobMessagesProducer = new JobMessagesProducer(
                workerKafkaPropertiesMock,
                jobStartedKafkaTemplateMock,
                jobFinishedKafkaTemplate,
                new KafkaDtoMapper()
        );

        when(jobStartedKafkaTemplateMock.send(any(), any())).thenReturn(CompletableFuture.completedFuture(null));
        when(jobFinishedKafkaTemplate.send(any(), any())).thenReturn(CompletableFuture.completedFuture(null));
    }

    @Test
    void sendJobStartedMessageAsync() {
        UUID uuid = UUID.randomUUID();
        jobMessagesProducer.sendJobStartedMessageAsync(uuid);
        verify(jobStartedKafkaTemplateMock, times(1)).send(eq("running-topic"), any());
    }

    @Test
    void sendJobFinishedMessageAsync() {
        JobResults jobResults = JobResults.builder().build();
        jobMessagesProducer.sendJobFinishedMessageAsync(jobResults);
        verify(jobFinishedKafkaTemplate, times(1)).send(eq("finished-topic"), any());
    }
}