package com.naumov.dotnetscriptsworker;

import com.naumov.dotnetscriptsworker.service.ContainerService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class IntegrationTestConfig {

    @Primary
    @Bean
    ContainerService containerService() {
        return new SimpleContainerServiceMock();
    }
}