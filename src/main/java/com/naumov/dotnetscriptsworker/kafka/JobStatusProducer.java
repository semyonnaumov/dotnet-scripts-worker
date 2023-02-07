package com.naumov.dotnetscriptsworker.kafka;

import com.naumov.dotnetscriptsworker.config.props.KafkaProperties;
import com.naumov.dotnetscriptsworker.dto.prod.JobFinishedMessage;
import com.naumov.dotnetscriptsworker.dto.prod.JobStartedMessage;
import com.naumov.dotnetscriptsworker.dto.mapper.DtoMapper;
import com.naumov.dotnetscriptsworker.dto.mapper.impl.JobFinishedToDtoMapper;
import com.naumov.dotnetscriptsworker.model.JobResults;
import jakarta.annotation.PreDestroy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class JobStatusProducer {
    private static final Logger LOGGER = LogManager.getLogger(JobStatusProducer.class);
    private final KafkaProperties kafkaProperties;
    private final KafkaTemplate<String, JobStartedMessage> jobStartedKafkaTemplate;
    private final KafkaTemplate<String, JobFinishedMessage> jobFinishedKafkaTemplate;
    private final DtoMapper<JobResults, JobFinishedMessage> jobFinishedDtoMapper;

    @Autowired
    public JobStatusProducer(KafkaProperties kafkaProperties,
                             KafkaTemplate<String, JobStartedMessage> jobStartedKafkaTemplate,
                             KafkaTemplate<String, JobFinishedMessage> jobFinishedKafkaTemplate,
                             JobFinishedToDtoMapper jobFinishedToDtoMapper) {
        this.kafkaProperties = kafkaProperties;
        this.jobStartedKafkaTemplate = jobStartedKafkaTemplate;
        this.jobFinishedKafkaTemplate = jobFinishedKafkaTemplate;
        this.jobFinishedDtoMapper = jobFinishedToDtoMapper;
    }

    public void reportJobStartedAsync(String jobId) {
        String runningTopic = kafkaProperties.getRunningTopicName();
        jobStartedKafkaTemplate.send(runningTopic, new JobStartedMessage(jobId))
                .thenAccept(res -> {
                    LOGGER.info("Reported job {} started to topic {}", jobId, runningTopic);
                }).exceptionally(e -> {
                    LOGGER.error("Failed to report job {} started to topic {}", jobId, runningTopic, e);
                    return null;
                });
    }

    public void reportJobFinishedAsync(JobResults jobResults) {
        JobFinishedMessage jobFinishedMessage = jobFinishedDtoMapper.map(jobResults);
        String finishedTopic = kafkaProperties.getFinishedTopicName();
        jobFinishedKafkaTemplate.send(finishedTopic, jobFinishedMessage)
                .thenAccept(res -> {
                    LOGGER.info("Reported job {} finished to topic {}", jobFinishedMessage.getJobId(), finishedTopic);
                }).exceptionally(e -> {
                    LOGGER.error("Failed to report job {} finished to topic {}", jobFinishedMessage.getJobId(), finishedTopic, e);
                    return null;
                });
    }

    @PreDestroy
    void shutdown() {
        // only needed if producer per thread enabled
        jobStartedKafkaTemplate.getProducerFactory().closeThreadBoundProducer();
        jobFinishedKafkaTemplate.getProducerFactory().closeThreadBoundProducer();
    }
}
