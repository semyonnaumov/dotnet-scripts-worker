package com.naumov.dotnetscriptsworker.kafka;

import com.naumov.dotnetscriptsworker.dto.KafkaDtoMapper;
import com.naumov.dotnetscriptsworker.dto.cons.JobConfig;
import com.naumov.dotnetscriptsworker.dto.cons.JobTaskMessage;
import com.naumov.dotnetscriptsworker.model.JobTask;
import com.naumov.dotnetscriptsworker.service.JobService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.support.Acknowledgment;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class JobMessagesConsumerTest {
    private final KafkaDtoMapper kafkaDtoMapper = new KafkaDtoMapper();
    private JobService jobServiceMock;
    private JobMessagesConsumer jobMessagesConsumer;

    @BeforeEach
    void setup() {
        jobServiceMock = mock(JobService.class);
        jobMessagesConsumer = new JobMessagesConsumer(jobServiceMock, kafkaDtoMapper, Optional.empty());
    }

    @Test
    void onJobTaskMessageRegular() {
        UUID jobId = UUID.randomUUID();
        String script = "script";
        String nugetConfigXml = "<config />";
        JobTaskMessage jobTaskMessage = JobTaskMessage.builder()
                .jobId(jobId)
                .script(script)
                .jobConfig(JobConfig.builder().nugetConfigXml(nugetConfigXml).build())
                .build();

        Acknowledgment ackMock = mock(Acknowledgment.class);

        jobMessagesConsumer.onJobTaskMessage(jobTaskMessage, ackMock);

        ArgumentCaptor<JobTask> jobTaskCaptor = ArgumentCaptor.forClass(JobTask.class);
        verify(ackMock, times(1)).acknowledge();
        verify(jobServiceMock, times(1)).runJob(jobTaskCaptor.capture());
        JobTask jobTask = jobTaskCaptor.getValue();
        assertNotNull(jobTask);
        assertNotNull(jobTask.getMessageId());
        assertEquals(jobId ,jobTask.getJobId());
        assertEquals(script ,jobTask.getJobScript());
        JobTask.JobConfig jobConfig = jobTask.getJobConfig();
        assertNotNull(jobConfig);
        assertEquals(nugetConfigXml, jobConfig.getNugetConfigXml());
    }

    @Test
    void onJobTaskMessageJobServiceThrowsException() {
        doThrow(RuntimeException.class).when(jobServiceMock).runJob(any());

        JobTaskMessage jobTaskMessage = JobTaskMessage.builder().build();
        Acknowledgment ackMock = mock(Acknowledgment.class);

        assertThrows(RuntimeException.class, () -> jobMessagesConsumer.onJobTaskMessage(jobTaskMessage, ackMock));

        verify(ackMock, times(1)).acknowledge();
        verify(jobServiceMock, times(1)).runJob(any());
    }
}