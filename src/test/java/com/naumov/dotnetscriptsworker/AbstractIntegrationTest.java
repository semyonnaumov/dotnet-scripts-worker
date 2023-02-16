package com.naumov.dotnetscriptsworker;

import com.github.dockerjava.api.model.Bind;
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

    @Container
    public static GenericContainer<?> dind = new GenericContainer<>(DockerImageName.parse("docker:20-dind"))
            .withExposedPorts(DOCKER_INTERNAL_PORT)
            .withPrivilegedMode(true)
            .withEnv("DOCKER_TLS_CERTDIR", "/certs")
            .withCreateContainerCmdModifier(cmd -> cmd.getHostConfig().withBinds(
                    Bind.parse("/Users/snaumov/Desktop/dockercerts/ca:/certs/ca"),
                    Bind.parse("/Users/snaumov/Desktop/dockercerts/client:/certs/client")
            ));

    @DynamicPropertySource
    public static void overrideProperties(DynamicPropertyRegistry registry) {
        Startables.deepStart(kafka);
        registry.add("scheduler.kafka.broker-url", kafka::getBootstrapServers);
        registry.add("worker.docker-client.docker-host", () -> "tcp://localhost:" + dind.getMappedPort(DOCKER_INTERNAL_PORT));
        registry.add("worker.docker-client.docker-cert-path", () -> "/Users/snaumov/Desktop/dockercerts/client");
    }
}