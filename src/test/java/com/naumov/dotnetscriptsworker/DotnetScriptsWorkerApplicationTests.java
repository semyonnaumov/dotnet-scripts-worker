package com.naumov.dotnetscriptsworker;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.test.context.EmbeddedKafka;

@SpringBootTest
@EmbeddedKafka(partitions = 1, brokerProperties = {"listeners=PLAINTEXT://localhost:8097", "port=8097"})
@Import(IntegrationTestConfig.class)
class DotnetScriptsWorkerApplicationTests {

    @Test
    void contextLoads() {
    }
}
