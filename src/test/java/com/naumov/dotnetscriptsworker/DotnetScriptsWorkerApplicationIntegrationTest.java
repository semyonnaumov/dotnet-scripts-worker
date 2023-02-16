package com.naumov.dotnetscriptsworker;

import com.naumov.dotnetscriptsworker.kafka.JobMessagesProducer;
import com.naumov.dotnetscriptsworker.service.ContainerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DirtiesContext
class DotnetScriptsWorkerApplicationIntegrationTest extends AbstractIntegrationTest {
    @Autowired
    private ContainerService containerService;
    @Autowired
    private JobMessagesProducer jobMessagesProducer;

    @Test
    void contextLoads() {
    }

    @Test
    void containerEnvironmentAvailable() {
        assertEquals(0, containerService.getAllContainersIds().size());
    }

    @Test
    void kafkaAvailable() {
        jobMessagesProducer.sendJobStartedMessageAsync(UUID.randomUUID());
    }
}