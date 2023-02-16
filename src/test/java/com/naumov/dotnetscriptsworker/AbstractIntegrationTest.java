package com.naumov.dotnetscriptsworker;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@Testcontainers
public abstract class AbstractIntegrationTest {
    private static final int DOCKER_INTERNAL_PORT = 2376;

    @Container
    public static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.3.0"));

    // TODO dind doesn't allow external http requests and requires https - setup TLS properly
    //  any requests to dind docker fail now with worker.docker-client.docker-tls-verify=true
    @Container
    public static GenericContainer<?> dind = new GenericContainer<>(DockerImageName.parse("docker:20-dind"))
            .withExposedPorts(DOCKER_INTERNAL_PORT)
            .withPrivilegedMode(true);

    @DynamicPropertySource
    public static void overrideProperties(DynamicPropertyRegistry registry) {
        Startables.deepStart(kafka);
        registry.add("scheduler.kafka.broker-url", kafka::getBootstrapServers);
        registry.add("worker.docker-client.docker-host", () -> "tcp://localhost:" + dind.getMappedPort(DOCKER_INTERNAL_PORT));
    }
}