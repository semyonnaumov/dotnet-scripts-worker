package com.naumov.dotnetscriptsworker;

import com.naumov.dotnetscriptsworker.service.ContainerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.annotation.DirtiesContext;

import static org.junit.jupiter.api.Assertions.assertTrue;

@DirtiesContext
class DotnetScriptsWorkerApplicationTests extends AbstractIntegrationTest {
    @Autowired
    private ContainerService containerService;

    @Test
    void contextLoads() {
        assertTrue(containerService.getAllContainersIds().isEmpty());
    }

    @TestConfiguration
    public static class MockOverridingContainerServiceConfig {

//        @Bean
//        ContainerService containerService() {
//            return new SimpleContainerServiceMock();
//        }
    }
}