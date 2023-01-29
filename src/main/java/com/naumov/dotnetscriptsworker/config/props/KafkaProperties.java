package com.naumov.dotnetscriptsworker.config.props;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@ConfigurationProperties("worker.kafka")
@Component
public class KafkaProperties {
    private String brokerUrl;
    private String jobsTopic;
    private String runningTopic;
    private String finishedTopic;
    private String consumerGroup;
    private Integer consumerConcurrency;
    private String producerAcks;
}