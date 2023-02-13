package com.naumov.dotnetscriptsworker.kafka;

import com.naumov.dotnetscriptsworker.dto.KafkaDtoMapper;
import com.naumov.dotnetscriptsworker.dto.cons.JobTaskMessage;
import com.naumov.dotnetscriptsworker.service.JobService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.Acknowledgment;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class JobMessagesConsumerTest {
    private JobService jobServiceMock;
    private final KafkaDtoMapper kafkaDtoMapper = new KafkaDtoMapper();
    private JobMessagesConsumer jobMessagesConsumer;

    @BeforeEach
    void setup() {
        this.jobServiceMock = mock(JobService.class);
        jobMessagesConsumer = new JobMessagesConsumer(jobServiceMock, kafkaDtoMapper);
    }

    @Test
    void onJobTaskMessageRegular() {
        JobTaskMessage jobTaskMessage = JobTaskMessage.builder().build();
        Acknowledgment ackMock = mock(Acknowledgment.class);

        jobMessagesConsumer.onJobTaskMessage(jobTaskMessage, ackMock);

        verify(ackMock, times(1)).acknowledge();
        verify(jobServiceMock, times(1)).runJob(any());
    }

    @Test
    void onJobTaskMessageJobServiceThrowsException() {
        JobTaskMessage jobTaskMessage = JobTaskMessage.builder().build();
        Acknowledgment ackMock = mock(Acknowledgment.class);
        doThrow(RuntimeException.class).when(jobServiceMock).runJob(any());

        assertThrows(RuntimeException.class, () -> jobMessagesConsumer.onJobTaskMessage(jobTaskMessage, ackMock));

        verify(ackMock, times(1)).acknowledge();
        verify(jobServiceMock, times(1)).runJob(any());
    }
}