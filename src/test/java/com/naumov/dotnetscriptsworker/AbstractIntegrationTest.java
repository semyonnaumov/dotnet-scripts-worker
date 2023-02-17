package com.naumov.dotnetscriptsworker;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.utility.DockerImageName;

import java.net.URL;

@SpringBootTest
@Testcontainers
@DirtiesContext
public abstract class AbstractIntegrationTest {
    private static final int DOCKER_INTERNAL_PORT = 2376;

    @Container
    public static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.3.0"));

    @Container
    public static GenericContainer<?> dind = new GenericContainer<>(DockerImageName.parse("docker:20-dind"))
            .withExposedPorts(DOCKER_INTERNAL_PORT)
            .withPrivilegedMode(true)
            .withClasspathResourceMapping("dockercerts/ca", "/certs/ca", BindMode.READ_WRITE)
            .withClasspathResourceMapping("dockercerts/client", "/certs/client", BindMode.READ_WRITE);

    @DynamicPropertySource
    public static void overrideProperties(DynamicPropertyRegistry registry) {
        URL dockercertsUrl = AbstractIntegrationTest.class.getClassLoader().getResource("dockercerts");
        if (dockercertsUrl == null) throw new IllegalStateException("Folder 'dockercerts' must be on the test classpath");

        URL tempfilesUrl = AbstractIntegrationTest.class.getClassLoader().getResource("tempfiles");
        if (tempfilesUrl == null) throw new IllegalStateException("Folder 'tempfiles' must be on the test classpath");

        Startables.deepStart(kafka, dind);
        registry.add("worker.kafka.broker-url", kafka::getBootstrapServers);
        registry.add("worker.docker-client.docker-host", () -> "tcp://localhost:" + dind.getMappedPort(DOCKER_INTERNAL_PORT));
        registry.add("worker.docker-client.docker-cert-path", () -> dockercertsUrl.getPath() + "/client");
        registry.add("worker.sandbox.job-files-host-dir", tempfilesUrl::getPath);
    }
}