package com.naumov.dotnetscriptsworker.config.props;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties("worker.kafka-admin")
@Component
public class KafkaAdminProperties {
    @NotBlank
    private String brokerUrl;
    @NotNull
    @Min(1)
    private Integer reconnectBackoffMs;
    @NotNull
    @Min(1)
    private Integer reconnectBackoffMaxMs;
    @NotNull
    @Min(1)
    private Short replicationFactor;
    @NotBlank
    private String jobsTopicName;
    @NotNull
    @Min(1)
    private Integer jobsTopicPartitions;
    @NotBlank
    private String runningTopicName;
    @NotNull
    @Min(1)
    private Integer runningTopicPartitions;
    @NotBlank
    private String finishedTopicName;
    @NotNull
    @Min(1)
    private Integer finishedTopicPartitions;
}
