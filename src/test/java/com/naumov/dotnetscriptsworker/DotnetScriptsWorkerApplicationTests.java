package com.naumov.dotnetscriptsworker;

import com.naumov.dotnetscriptsworker.config.props.SandboxContainerProperties;
import com.naumov.dotnetscriptsworker.service.ContainerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DirtiesContext
class DotnetScriptsWorkerApplicationTests extends AbstractIntegrationTest {
    @Autowired
    private ContainerService containerService;
    @Autowired
    private SandboxContainerProperties sandboxContainerProperties;

    @Test
    void contextLoads() {
    }

    @Test
    void containerCreatesAndDeletes() {
        String containerId = containerService.createContainer(
                "test-container",
                sandboxContainerProperties.getImage(),
                List.of(),
                Optional.empty()
        );

        assertEquals(1, containerService.getAllContainersIds().size());
        assertEquals(containerId, containerService.getAllContainersIds().get(0));
        containerService.removeContainer(containerId, true);
    }
}